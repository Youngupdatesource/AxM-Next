## fOxygen v1.2

fOxygen (formerly AxM-Next) is a modified fork of [AxManager](https://github.com/fahrez182/AxManager) by fahrez182.

### What's new (since AxM-Next v1.1)
- Renamed from AxM-Next to fOxygen with a new application id, `com.foxygen.manager`, new launcher icons, and the update check now follows fOxygen releases
- Terminal (axsh): a native client gives any terminal app a full interactive shell through the server (real PTY, `vi`, `top`, job control, window resizing), the same way in Root and ADB mode. Start it from the Privilege Manager for apps that bundle `libtermux.so`
- Rootless debugging (root mode only, per app): run an app's axsh sessions as the shell user (uid 2000, like `adb shell`) instead of root
- Privilege Manager: filter by Shizuku API apps, terminal apps, or both. Granted apps stay on top and are remembered between launches, so the list no longer flickers, and apps that are already allowed skip the permission prompt
- WebUI follows the KernelSU native API (`ksu.exec`, `ksu.spawn`, `ksu.listPackages` and more). `exec` and `spawn` callbacks no longer block the page, every plugin process carries `AXERON_PLUGIN`, and plugin installs get their own temporary folder
- New Bold text option in Appearance, a softer Material You light theme, stat tile titles that shrink to fit, and smoother plugin cards (press animation, animated description)
- Restart that comes back: the starter runs detached and logs to `/data/local/tmp/axeron_restart.log`, and the manager starts the server again if it does not return within about 20 seconds
- More reliable permission requests and binder delivery, and swiping between tabs no longer triggers accidental taps

### Install
Download the APK below and verify it against the attached `.sha256` file. Because the application id changed, fOxygen installs as a separate app and does not update AxM-Next in place. If Android refuses to install over an existing fOxygen build, uninstall the old one first, because it was signed with a different key.

See the [README](https://github.com/Youngupdatesource/fOxygen#readme) for details and credits.
