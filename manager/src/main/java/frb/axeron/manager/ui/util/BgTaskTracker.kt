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
 * daemons) and follows them until they end, reporting each change through [onEvent]:
 *  - `[bgtask] pid=a,b`         tasks that outlived the command, or children they forked later
 *  - `[bgtask] exited pid=a,b`  tasks that are gone
 *
 * Detection combines two sources, because a command can finish faster than it can be polled:
 *  1. while the command runs, its family is remembered (pid + start time): descendants through
 *     the ppid tree, plus members of the command's process group / session;
 *  2. right after it exits, anything alive that is remembered, or that still carries the
 *     command's process group / session id, is a background task, and so is everything
 *     descending from those (repeated until nothing new joins, a task's children are tasks too).
 *
 * While something is tracked, a watch loop reports exits and adopts children the tasks fork
 * later, once they have lived [ADOPT_TICKS] polls (a loop that runs `sleep 1` stays quiet).
 *
 * Identity is pid + start time (field 22 of /proc/<pid>/stat), so a recycled pid is never
 * mistaken for the task it replaced, and zombies (state Z) never count as alive.
 * The process table comes from one `cat /proc/[0-9]*` run through the Axeron server, so root
 * and shell mode both work and nothing depends on the flags of the device's `ps`.
 */
class BgTaskTracker(
    private val scope: CoroutineScope,
    private val onEvent: (String) -> Unit,
) {
    private class Proc(
        val pid: Int,
        val ppid: Int,
        val pgrp: Int,
        val sid: Int,
        val start: Long,
        val state: Char,
    )

    private class Candidate(val start: Long, var ticks: Int)

    /** pid -> start time of what the running command spawned, seen while it ran. */
    private val seen = ConcurrentHashMap<Int, Long>()

    /** pid -> start time of the background tasks being followed. */
    private val tracked = ConcurrentHashMap<Int, Long>()

    private var pollJob: Job? = null
    private var watchJob: Job? = null

    /** A command started: remember its family while it runs. */
    fun onStarted(pid: Int) {
        pollJob?.cancel()
        seen.clear()
        pollJob = scope.launch(Dispatchers.IO) {
            var tick = 0
            while (isActive) {
                // Quick at first: `cmd &` and short scripts are over within a second.
                delay(if (tick < FAST_TICKS) FAST_MS else POLL_MS)
                tick++
                val alive = snapshot()?.filter { it.state != 'Z' && it.pid != pid } ?: continue
                family(alive, pid).forEach { seen[it.pid] = it.start }
            }
        }
    }

    /** The command's own process ended: whatever of its tree is still alive went background. */
    fun onFinished(pid: Int) {
        pollJob?.cancel()
        // Taken now: the next command may start (and clear [seen]) before the settle delay ends.
        val remembered = HashMap(seen)
        seen.clear()
        scope.launch(Dispatchers.IO) {
            delay(SETTLE_MS)
            val alive = snapshot()?.filter { it.state != 'Z' && it.pid != pid } ?: return@launch
            val keep = LinkedHashMap<Int, Proc>()
            for (p in alive) {
                if (remembered[p.pid] == p.start || p.pgrp == pid || p.sid == pid) keep[p.pid] = p
            }
            val fresh = grow(alive, keep, setOf(pid))
                .filter { tracked.putIfAbsent(it.pid, it.start) == null }
            if (fresh.isNotEmpty()) {
                onEvent("[bgtask] pid=${fresh.map { it.pid }.sorted().joinToString(",")}")
                watch()
            }
        }
    }

    /** Reports exits and late children of the tracked tasks. Runs only while something is tracked. */
    private fun watch() {
        if (watchJob?.isActive == true) return
        watchJob = scope.launch(Dispatchers.IO) {
            val candidates = HashMap<Int, Candidate>()
            while (isActive && tracked.isNotEmpty()) {
                delay(WATCH_MS)
                val alive = snapshot()?.filter { it.state != 'Z' } ?: continue
                val live = alive.associate { it.pid to it.start }

                val gone = tracked.filter { (pid, start) -> live[pid] != start }.keys.toList()
                if (gone.isNotEmpty()) {
                    gone.forEach { tracked.remove(it) }
                    onEvent("[bgtask] exited pid=${gone.sorted().joinToString(",")}")
                }
                if (tracked.isEmpty()) break

                // Descendants of the tasks that are not tracked yet.
                val kids = grow(
                    alive.filter { it.pid !in tracked },
                    LinkedHashMap(),
                    tracked.keys.toSet()
                )
                val current = kids.associate { it.pid to it.start }
                candidates.entries.removeAll { (pid, c) -> current[pid] != c.start }

                val adopted = ArrayList<Int>()
                for (p in kids) {
                    val c = candidates.getOrPut(p.pid) { Candidate(p.start, 0) }
                    c.ticks++
                    if (c.ticks >= ADOPT_TICKS && tracked.putIfAbsent(p.pid, p.start) == null) {
                        adopted += p.pid
                    }
                }
                adopted.forEach { candidates.remove(it) }
                if (adopted.isNotEmpty()) {
                    onEvent("[bgtask] pid=${adopted.sorted().joinToString(",")}")
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

    /** What belongs to [root] while it runs: its group and session, and all that descends from it. */
    private fun family(alive: List<Proc>, root: Int): List<Proc> {
        val keep = LinkedHashMap<Int, Proc>()
        for (p in alive) if (p.pgrp == root || p.sid == root) keep[p.pid] = p
        return grow(alive, keep, setOf(root))
    }

    /**
     * Adds to [keep] every process whose parent is in [roots] or already in [keep], repeated
     * until nothing new joins (children of children, however deep).
     */
    private fun grow(alive: List<Proc>, keep: MutableMap<Int, Proc>, roots: Set<Int>): List<Proc> {
        do {
            var grew = false
            for (p in alive) {
                if (p.pid in keep) continue
                if (p.ppid in roots || p.ppid in keep) {
                    keep[p.pid] = p
                    grew = true
                }
            }
        } while (grew)
        return keep.values.toList()
    }

    /** One run over /proc; null when the server is unreachable or the output can't be read. */
    private fun snapshot(): List<Proc>? {
        if (!Axeron.pingBinder()) return null
        return try {
            val process = Axeron.newProcess(
                arrayOf("sh", "-c", "cat /proc/[0-9]*/stat 2>/dev/null"),
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
            Log.w(TAG, "reading /proc failed", e)
            null
        }
    }

    /**
     * `pid (name) S ppid pgrp session ... starttime ...`. The name can hold spaces and ')', so
     * the fields are counted from the last ')'. After it: state, ppid, pgrp, session are the
     * first four; starttime is field 22 of the file, the 20th after the name.
     */
    private fun parse(line: String): Proc? {
        val open = line.indexOf('(')
        val close = line.lastIndexOf(')')
        if (open <= 0 || close < open || close + 2 >= line.length) return null
        val pid = line.substring(0, open).trim().toIntOrNull() ?: return null
        val f = line.substring(close + 2).trim().split(' ')
        if (f.size < 20 || f[0].isEmpty()) return null
        return Proc(
            pid = pid,
            ppid = f[1].toIntOrNull() ?: return null,
            pgrp = f[2].toIntOrNull() ?: return null,
            sid = f[3].toIntOrNull() ?: return null,
            start = f[19].toLongOrNull() ?: return null,
            state = f[0][0],
        )
    }

    private companion object {
        const val TAG = "BgTaskTracker"
        const val FAST_TICKS = 4
        const val FAST_MS = 250L
        const val POLL_MS = 1500L
        const val SETTLE_MS = 300L
        const val WATCH_MS = 2000L
        const val ADOPT_TICKS = 3
    }
}
