package frb.axeron.axsh

import android.content.Intent
import android.net.LocalSocket
import android.os.ParcelFileDescriptor
import android.system.Os
import rikka.hidden.compat.ActivityManagerApis
import java.io.FileDescriptor
import kotlin.system.exitProcess

/**
 * `app_process` entry started by the native client with one end of a socketpair as its first
 * argument. It runs as the caller's uid, serves that single session through [Broker] and exits.
 */
object Bootstrap {

    @JvmStatic
    fun main(args: Array<String>) {
        val fdNumber = args.getOrNull(0)?.toIntOrNull()
        if (fdNumber == null) {
            System.err.println("axsh: bootstrap needs a socket fd")
            exitProcess(2)
        }

        val socket = try {
            connectedSocket(ParcelFileDescriptor.adoptFd(fdNumber).fileDescriptor)
        } catch (e: Throwable) {
            System.err.println("axsh: bootstrap can't use fd $fdNumber: $e")
            exitProcess(2)
        }

        Broker.serveOne(socket, ::broadcast)
        exitProcess(0)
    }

    private fun broadcast(intent: Intent) {
        ActivityManagerApis.broadcastIntent(
            intent, null, null, 0, null, null, null, -1, null,
            false, false, Os.getuid() / 100000
        )
    }

    private fun connectedSocket(fd: FileDescriptor): LocalSocket =
        LocalSocket::class.java.getDeclaredConstructor(FileDescriptor::class.java)
            .apply { isAccessible = true }
            .newInstance(fd)
}
