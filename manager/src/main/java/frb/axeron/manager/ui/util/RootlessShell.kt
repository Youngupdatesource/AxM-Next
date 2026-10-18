package frb.axeron.manager.ui.util

/**
 * "Rootless debugging": lowers a QuickShell command from root to the shell user (uid 2000), so
 * it behaves like `adb shell` while the Axeron server itself keeps running as root.
 *
 * Built on toybox `setpriv` (a single exec, no extra process left behind):
 *  - uid/gid and supplementary groups become adbd's shell identity;
 *  - the capability bounding set and inheritable caps are emptied, and `no_new_privs` stops it
 *    from getting anything back through setuid binaries;
 *  - the SELinux domain is switched to u:r:shell:s0 with `runcon` when the system allows it.
 *
 * If `setpriv` is missing the command is NOT run: silently staying root would defeat the point.
 */
object RootlessShell {

    private const val SHELL_UID = 2000

    // adbd's supplementary groups for the shell user: shell, log, adb, input, inet, sdcard_*,
    // mount, net_*, readproc, bluetooth, usb, ext_data_rw, ext_obb_rw, readtracefs, graphics.
    private const val SHELL_GROUPS =
        "2000,1004,1007,1011,1015,1028,1009,3001,3002,3003,3006,3009,1002,1018,1078,1079,3012,1003"

    private const val SHELL_CONTEXT = "u:r:shell:s0"

    /** Wraps [command] so it runs as the shell user. Output and stdin stay attached (exec). */
    fun wrap(command: String): String {
        val quoted = "'" + command.replace("'", "'\\''") + "'"
        return buildString {
            append("RC=''; ")
            append("command -v runcon >/dev/null 2>&1 && runcon $SHELL_CONTEXT true >/dev/null 2>&1 ")
            append("&& RC=\"runcon $SHELL_CONTEXT\"; ")
            append("if command -v setpriv >/dev/null 2>&1; then ")
            append("exec \$RC setpriv --reuid=$SHELL_UID --regid=$SHELL_UID --groups=$SHELL_GROUPS ")
            append("--bounding-set=-all --inh-caps=-all --no-new-privs sh -c $quoted; ")
            append("else echo 'rootless: setpriv not found, command not run' >&2; exit 127; fi")
        }
    }
}
