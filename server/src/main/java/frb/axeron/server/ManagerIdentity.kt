package frb.axeron.server

import frb.axeron.axsh.AxshConstants

object ManagerIdentity {

    const val MANAGER_APPLICATION_ID = AxshConstants.MANAGER_PACKAGE

    const val PERMISSION_MANAGER = "$MANAGER_APPLICATION_ID.permission.MANAGER"

    const val REQUEST_PERMISSION_ACTION = "$MANAGER_APPLICATION_ID.intent.action.REQUEST_PERMISSION"
}
