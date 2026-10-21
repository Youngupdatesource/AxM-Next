package frb.axeron.manager.service

import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import frb.axeron.adb.util.AdbEnvironment
import frb.axeron.api.Axeron
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.core.Starter
import frb.axeron.manager.adb.AdbStarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Status "server seharusnya hidup" + logika restart bersama.
 *
 * Flag disimpan lewat AxeronSettings.getPreferences() (store yang sama dengan getStartOnBoot),
 * jadi tidak perlu mengubah submodule api/.
 */
object ServerGuard {
    private const val TAG = "ServerGuard"
    private const val KEY_WAS_RUNNING = "server_guard_was_running"
    private const val KEY_AUTO_RESTART = "server_guard_auto_restart"

    private const val RESTART_GRACE_MS = 20_000L

    private val prefs get() = AxeronSettings.getPreferences()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var restartWatch: Job? = null

    /**
     * Call right after asking the running server to restart itself.
     *
     * The restart command runs as a child of the server it is about to kill, so it can be taken
     * down with it before the new server is forked: the server just disappears. And since a
     * restart is "intentional", the auto-restart stays off, so nothing brings it back. If the
     * binder isn't back after [graceMs], start it again from the manager with the last launch mode.
     */
    fun watchRestart(context: Context, graceMs: Long = RESTART_GRACE_MS) {
        val app = context.applicationContext
        restartWatch?.cancel()
        restartWatch = scope.launch {
            delay(graceMs)
            if (Axeron.pingBinder()) return@launch
            Log.w(TAG, "server did not come back after restart, starting it again")
            restart(app)
        }
    }

    /** Server is up: no need to wait for the restart fallback anymore. */
    fun cancelRestartWatch() {
        restartWatch?.cancel()
        restartWatch = null
    }

    /** true selama server terakhir diketahui jalan dan bukan dimatikan oleh user. */
    var wasRunning: Boolean
        get() = prefs.getBoolean(KEY_WAS_RUNNING, false)
        set(value) {
            prefs.edit().putBoolean(KEY_WAS_RUNNING, value).apply()
        }

    var autoRestartEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RESTART, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_RESTART, value).apply()
        }

    /**
     * Menyalakan ulang server memakai mode peluncuran terakhir.
     * Return true bila perintah start berhasil dikirim (bukan jaminan server sudah hidup).
     */
    suspend fun restart(context: Context): Boolean =
        if (AxeronSettings.getLastLaunchMode() == AxeronSettings.LaunchMethod.ROOT) {
            restartViaRoot()
        } else {
            restartViaAdbTcp(context)
        }

    private suspend fun restartViaRoot(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            if (Shell.getShell().isRoot) {
                Shell.cmd(Starter.internalCommand).exec()
                Log.i(TAG, "restart via root: command sent")
                true
            } else {
                Shell.getCachedShell()?.close()
                Log.w(TAG, "restart via root: no root access")
                false
            }
        }.getOrElse {
            Log.e(TAG, "restart via root failed", it)
            false
        }
    }

    private suspend fun restartViaAdbTcp(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val port = AdbEnvironment.getAdbTcpPort().takeIf { it > 0 }
                ?: AxeronSettings.getTcpPort()
            if (port > 0) {
                AdbStarter.startAdbClient(context.applicationContext, port)
                Log.i(TAG, "restart via ADB TCP: command sent to port $port")
                true
            } else {
                Log.w(TAG, "restart via ADB: no TCP port available")
                false
            }
        }.getOrElse {
            Log.e(TAG, "restart via ADB failed", it)
            false
        }
    }
}
