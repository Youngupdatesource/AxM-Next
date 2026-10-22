package frb.axeron.server.shell

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.Process
import android.os.SystemClock
import android.util.Log
import frb.axeron.api.Axeron
import frb.axeron.server.ServerConstants
import frb.axeron.shared.AxeronApiConstant
import java.util.concurrent.atomic.AtomicBoolean

object ShellBinderRequestHandler {

    private const val TAG = "ShellBinderRequest"

    private const val BINDER_WAIT_MS = 2_500L

    private const val FRESH_PROCESS_MS = 3_000L

    private val mainHandler = Handler(Looper.getMainLooper())

    fun handleRequest(context: Context, intent: Intent, onDone: () -> Unit = {}): Boolean {
        if (intent.action != ServerConstants.REQUEST_BINDER_AXRUNTIME) {
            onDone()
            return false
        }

        val receiver = intent.getBundleExtra("data")?.getBinder("binder")
        if (receiver == null) {
            onDone()
            return false
        }

        Axeron.getBinder()?.takeIf { it.pingBinder() }?.let {
            reply(context, receiver, it)
            onDone()
            return true
        }

        val age = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()
        if (age > FRESH_PROCESS_MS) {
            reply(context, receiver, null)
            onDone()
            return true
        }

        val done = AtomicBoolean(false)
        val listener = object : Axeron.OnBinderReceivedListener {
            override fun onBinderReceived() {
                if (done.getAndSet(true)) return
                Axeron.removeBinderReceivedListener(this)
                reply(context, receiver, Axeron.getBinder())
                onDone()
            }
        }
        Axeron.addBinderReceivedListenerSticky(listener, mainHandler)
        mainHandler.postDelayed({
            if (done.getAndSet(true)) return@postDelayed
            Axeron.removeBinderReceivedListener(listener)
            Log.w(TAG, "no binder from the server within ${BINDER_WAIT_MS}ms")
            reply(context, receiver, null)
            onDone()
        }, BINDER_WAIT_MS)
        return true
    }

    private fun reply(context: Context, receiver: IBinder, axeronBinder: IBinder?) {
        val data = Parcel.obtain()
        try {
            data.writeStrongBinder(axeronBinder)
            data.writeLong(AxeronApiConstant.server.VERSION_CODE)
            data.writeString(context.applicationInfo.nativeLibraryDir)
            receiver.transact(1, data, null, IBinder.FLAG_ONEWAY)
        } catch (e: Throwable) {
            Log.w(TAG, "reply failed", e)
        } finally {
            data.recycle()
        }
    }
}
