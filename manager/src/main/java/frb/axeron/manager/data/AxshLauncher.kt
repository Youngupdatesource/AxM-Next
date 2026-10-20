package frb.axeron.manager.data

import android.content.Context
import android.util.Log
import frb.axeron.api.Axeron
import java.io.File

object AxshLauncher {

    private const val TAG = "AxshLauncher"

    const val PATH = "/sdcard/axeron/axsh"

    const val RUN_COMMAND = "sh $PATH"

    fun binaryPath(context: Context): String =
        File(context.applicationInfo.nativeLibraryDir, "libaxsh.so").absolutePath

    fun script(context: Context): String {
        val binary = binaryPath(context)
        val abi = File(context.applicationInfo.nativeLibraryDir).name
        val pkg = context.packageName
        return listOf(
            "#!/system/bin/sh",
            "AXSH='$binary'",
            "[ -x \"\$AXSH\" ] || AXSH=\"\$(pm path $pkg | sed -n 's/^package://;s/base\\.apk\$//p')lib/$abi/libaxsh.so\"",
            "exec \"\$AXSH\" \"\$@\"",
            ""
        ).joinToString("\n")
    }

    fun ensure(context: Context): Boolean {
        return try {
            Axeron.newFileService().getStreamSession(PATH, true, false).outputStream.use {
                it.write(script(context).toByteArray())
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "can't write $PATH", e)
            false
        }
    }
}
