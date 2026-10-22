package frb.axeron.manager.ui.util

import frb.axeron.api.Axeron
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.core.Starter

object ServerRestart {

    const val LOG_PATH = "/data/local/tmp/axeron_restart.log"

    fun command(): String = "{ ${Starter.internalCommand}; } >$LOG_PATH 2>&1 </dev/null"

    fun launch() {
        Axeron.newProcess(
            AxeronCommandSession.getQuickCmd(command(), true, false),
            null,
            null
        )
    }
}
