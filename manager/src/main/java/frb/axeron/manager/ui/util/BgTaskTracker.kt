package frb.axeron.manager.ui.util

import android.util.Log
import frb.axeron.api.Axeron
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds the processes a QuickShell command leaves running behind it (`cmd &`, `nohup ...`,
 * daemons) and follows them until they end, reporting each change through [onEvent] as
 * `[bgtask] pid=...`.
 *
 * Detection combines two sources, because a command can finish faster than it can be polled:
 *  1. while the command runs, its descendants are remembered (pid + name, from the ppid tree);
 *  2. right after it exits, anything alive that is remembered, or that still carries the
 *     command's process group / session id, is a background task.
 *
 * Process lists come from `ps` run through the Axeron server, so root and shell mode both work.
 */
class BgTaskTracker(
    private val scope: CoroutineScope,
    private val onEvent: (String) -> Unit,
) {
    private data class Proc(val pid: Int, val ppid: Int, val pgid: Int, val sid: Int, val name: String)

    private val seen = ConcurrentHashMap<Int, String>()
    private val tracked = ConcurrentHashMap<Int, String>()

    private var pollJob: Job? = null
    private var watchJob: Job? = null

    /** A command started: remember its descendants while it runs. */
    fun onStarted(pid: Int) {
        pollJob?.cancel()
        seen.clear()
        pollJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(POLL_MS)
                val all = snapshot() ?: continue
                descendants(all, pid).forEach { seen[it.pid] = it.name }
            }
        }
    }

    /**
     * The command's own process ended: whatever of its tree is still alive went background.
     * [hint] are pids the session already knows were started (the shell's `$!`); they count even
     * when the process tree no longer shows how they are related to the command.
     */
    fun onFinished(pid: Int, hint: IntArray = IntArray(0)) {
        pollJob?.cancel()
        scope.launch(Dispatchers.IO) {
            delay(SETTLE_MS)
            val all = snapshot() ?: return@launch
            val fresh = all
                .filter { it.pid != pid }
                .filter { seen[it.pid] == it.name || it.pgid == pid || it.sid == pid || it.pid in hint }
                .filter { tracked.putIfAbsent(it.pid, it.name) == null }
            seen.clear()
            if (fresh.isNotEmpty()) {
                onEvent("[bgtask] pid=${fresh.joinToString(",") { it.pid.toString() }}")
                watch()
            }
        }
    }

    /** The command's background jobs were stopped on purpose: stop looking for them. */
    fun onStopped() {
        pollJob?.cancel()
        seen.clear()
    }

    /** Tells when each background task is gone. Runs only while something is tracked. */
    private fun watch() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch(Dispatchers.IO) {
            while (isActive && tracked.isNotEmpty()) {
                delay(WATCH_MS)
                val all = snapshot() ?: continue
                val alive = all.associate { it.pid to it.name }
                val gone = tracked.filter { (pid, name) -> alive[pid] != name }.keys
                if (gone.isNotEmpty()) {
                    gone.forEach { tracked.remove(it) }
                    onEvent("[bgtask] exited pid=${gone.sorted().joinToString(",")}")
                }
            }
        }
    }

    fun close() {
        pollJob?.cancel()
        watchJob?.cancel()
        tracked.clear()
        seen.clear()
    }

    private fun descendants(all: List<Proc>, root: Int): List<Proc> {
        val children = all.groupBy { it.ppid }
        val out = ArrayList<Proc>()
        val queue = ArrayDeque<Int>().apply { add(root) }
        val visited = HashSet<Int>().apply { add(root) }
        while (queue.isNotEmpty()) {
            for (child in children[queue.removeFirst()].orEmpty()) {
                if (!visited.add(child.pid)) continue
                out += child
                queue += child.pid
            }
        }
        return out
    }

    /** One `ps` run; null when the server is unreachable or the output can't be read. */
    private fun snapshot(): List<Proc>? {
        if (!Axeron.pingBinder()) return null
        return try {
            val process = Axeron.newProcess(
                arrayOf("sh", "-c", "ps -A -o PID,PPID,PGID,SID,NAME 2>/dev/null"),
                null,
                null
            )
            val text = try {
                process.inputStream.bufferedReader().readText()
            } finally {
                runCatching { process.destroy() }
            }
            text.lineSequence().mapNotNull(::parse).toList().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "ps failed", e)
            null
        }
    }

    private fun parse(line: String): Proc? {
        val parts = line.trim().split(Regex("\\s+"), limit = 5)
        if (parts.size < 4) return null
        val pid = parts[0].toIntOrNull() ?: return null // also skips the header
        val ppid = parts[1].toIntOrNull() ?: return null
        val pgid = parts[2].toIntOrNull() ?: return null
        val sid = parts[3].toIntOrNull() ?: return null
        return Proc(pid, ppid, pgid, sid, parts.getOrElse(4) { "" })
    }

    private companion object {
        const val TAG = "BgTaskTracker"
        const val POLL_MS = 1500L
        const val SETTLE_MS = 300L
        const val WATCH_MS = 2000L
    }
}
