package frb.axeron.server;

import static frb.axeron.server.ServerConstants.PERMISSION;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.system.Os;
import android.util.AtomicFile;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import frb.axeron.ktx.HandlerKt;
import frb.axeron.shared.AxeronApiConstant;
import frb.axeron.shared.PathHelper;
import kotlin.collections.ArraysKt;
import rikka.hidden.compat.PackageManagerApis;
import rikka.hidden.compat.UserManagerApis;

public class AxeronConfigManager extends ConfigManager {

    private static final Gson GSON_IN = new GsonBuilder()
            .create();
    private static final Gson GSON_OUT = new GsonBuilder()
            .setVersion(AxeronConfig.LATEST_VERSION)
            .create();

    // Short on purpose: a restart/kill within the delay used to throw away the pending write,
    // which reset every checked app in the Privileges list (root daemon is killed hard).
    private static final long WRITE_DELAY = 500;

    private static final File FILE = PathHelper.getWorkingPath(Os.getuid() == 0,AxeronApiConstant.folder.PARENT + "ax_permission.json");
    private static final AtomicFile ATOMIC_FILE = new AtomicFile(FILE);
    private final AxeronConfig config;
    private final Runnable mWriteRunner = new Runnable() {

        @Override
        public void run() {
            synchronized (AxeronConfigManager.this) {
                write(config);
            }
        }
    };

    public AxeronConfigManager() {
        this.config = load();

        boolean changed = false;

        // When the package manager is not answering yet (root daemon started early at boot,
        // or while the system is busy), "uid has no packages" does NOT mean the app is gone.
        // Never wipe the saved permissions in that case.
        // One scan for the whole constructor. It used to be three (a readiness probe, then the
        // GET_PERMISSIONS scan here, then another one in sendBinderToClient), which is a big part
        // of the slow start on devices with many apps.
        List<PackageInfo> scanned = new ArrayList<>();
        boolean pmReady = false;
        try {
            for (int userId : UserManagerApis.getUserIdsNoThrow()) {
                List<PackageInfo> list = PackageManagerApis.getInstalledPackagesNoThrow(PackageManager.GET_PERMISSIONS, userId);
                if (list != null && !list.isEmpty()) {
                    pmReady = true;
                    scanned.addAll(list);
                }
            }
        } catch (Throwable tr) {
            LOGGER.w(tr, "scan installed packages");
        }

        if (config.packages == null) {
            config.packages = new ArrayList<>();
            changed = true;
        }

        for (AxeronConfig.PackageEntry entry : new ArrayList<>(config.packages)) {
            if (entry == null) {
                config.packages.remove(null);
                changed = true;
                continue;
            }
            if (entry.packages == null) {
                entry.packages = new ArrayList<>();
            }

            List<String> packages = PackageManagerApis.getPackagesForUidNoThrow(entry.uid);
            if (packages.isEmpty()) {
                if (!pmReady) {
                    LOGGER.w("package manager not ready, keep config for uid %d", entry.uid);
                    continue;
                }
                LOGGER.i("remove config for uid %d since it has gone", entry.uid);
                config.packages.remove(entry);
                changed = true;
                continue;
            }

            // Entries written by updateFlagsForUid() used to be saved with an empty package
            // list. They were then treated as "packages changed" and deleted on the next start.
            if (entry.packages.isEmpty()) {
                entry.packages.addAll(packages);
                changed = true;
            }

            boolean packagesChanged = true;

            for (String packageName : entry.packages) {
                if (packages.contains(packageName)) {
                    packagesChanged = false;
                    break;
                }
            }

            final int rawSize = entry.packages.size();
            Set<String> s = new LinkedHashSet<>(entry.packages);
            entry.packages.clear();
            entry.packages.addAll(s);
            final int shrunkSize = entry.packages.size();
            if (shrunkSize < rawSize) {
                LOGGER.w("entry.packages has duplicate! Shrunk. (%d -> %d)", rawSize, shrunkSize);
            }

            if (packagesChanged) {
                LOGGER.i("remove config for uid %d since the packages for it changed", entry.uid);
                config.packages.remove(entry);
                changed = true;
            }
        }

        {
            for (PackageInfo pi : scanned) {
                if (pi == null
                        || pi.applicationInfo == null
                        || pi.requestedPermissions == null
                        || !ArraysKt.contains(pi.requestedPermissions, PERMISSION)) {
                    continue;
                }

                int uid = pi.applicationInfo.uid;
                String pkg = pi.packageName;
                AxeronConfig.PackageEntry entry = findLocked(uid);
                boolean allowed = false;

                if (entry != null) {
                    allowed = entry.packages.contains(pkg) && entry.isAllowed();
                }

                List<String> packages = new ArrayList<>();
                packages.add(pkg);

                updateLocked(uid, packages, ConfigManager.MASK_PERMISSION, allowed ? ConfigManager.FLAG_ALLOWED : ConfigManager.FLAG_DENIED);
            }
        }

        if (changed) {
            scheduleWriteLocked();
        }

        // The daemon can be killed without running any exit hook (restart from the manager,
        // SIGTERM). Flush whatever is still pending.
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(this::flush, "ax-config-flush"));
        } catch (Throwable tr) {
            LOGGER.w(tr, "add shutdown hook");
        }
    }

    /** Writes pending changes now instead of waiting for the delayed write. */
    public void flush() {
        synchronized (this) {
            HandlerKt.getWorkerHandler().removeCallbacks(mWriteRunner);
            write(config);
        }
    }

    public static AxeronConfig load() {
        FileInputStream stream;
        try {
            stream = ATOMIC_FILE.openRead();
        } catch (FileNotFoundException e) {
            LOGGER.i("no existing config file " + ATOMIC_FILE.getBaseFile() + "; starting empty");
            return new AxeronConfig();
        }

        AxeronConfig config = null;
        try {
            config = GSON_IN.fromJson(new InputStreamReader(stream), AxeronConfig.class);
        } catch (Throwable tr) {
            LOGGER.w(tr, "load config");
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                LOGGER.w("failed to close: " + e);
            }
        }
        if (config != null) return config;
        quarantine();
        return new AxeronConfig();
    }

    /** Unreadable config: keep it for debugging and start empty instead of failing on every start. */
    private static void quarantine() {
        try {
            File base = ATOMIC_FILE.getBaseFile();
            if (base.exists()) {
                File bad = new File(base.getPath() + ".corrupt");
                if (bad.exists()) bad.delete();
                if (!base.renameTo(bad)) base.delete();
                LOGGER.w("config unreadable, moved to " + bad);
            }
        } catch (Throwable tr) {
            LOGGER.w(tr, "quarantine config");
        }
    }

    public static void write(AxeronConfig config) {
        synchronized (ATOMIC_FILE) {
            FileOutputStream stream;
            try {
                stream = ATOMIC_FILE.startWrite();
            } catch (IOException e) {
                LOGGER.w("failed to write state: " + e);
                return;
            }

            try {
                String json = GSON_OUT.toJson(config);
                stream.write(json.getBytes());

                ATOMIC_FILE.finishWrite(stream);
                LOGGER.v("config saved");
            } catch (Throwable tr) {
                LOGGER.w(tr, "can't save %s, restoring backup.", ATOMIC_FILE.getBaseFile());
                ATOMIC_FILE.failWrite(stream);
            }
        }
    }

    private void scheduleWriteLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (HandlerKt.getWorkerHandler().hasCallbacks(mWriteRunner)) {
                return;
            }
        } else {
            HandlerKt.getWorkerHandler().removeCallbacks(mWriteRunner);
        }
        HandlerKt.getWorkerHandler().postDelayed(mWriteRunner, WRITE_DELAY);
    }

    private AxeronConfig.PackageEntry findLocked(int uid) {
        for (AxeronConfig.PackageEntry entry : config.packages) {
            if (uid == entry.uid) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public AxeronConfig.PackageEntry find(int uid) {
        synchronized (this) {
            return findLocked(uid);
        }
    }

    @Nullable
    public AxeronConfig.PackageEntry findOrUpdate(int uid) {
        synchronized (this) {
            AxeronConfig.PackageEntry entry = findLocked(uid);
            if (entry == null) {
                entry = new AxeronConfig.PackageEntry(uid, ConfigManager.MASK_PERMISSION & ConfigManager.FLAG_DENIED);
                entry.packages.addAll(PackageManagerApis.getPackagesForUidNoThrow(uid));
                config.packages.add(entry);
            }
            return entry;
        }
    }

    private void updateLocked(int uid, List<String> packages, int mask, int values) {
        // updateFlagsForUid() passes no package list. Without one the entry is saved with
        // empty packages and gets deleted on the next daemon start, so resolve it here.
        if (packages == null) {
            packages = PackageManagerApis.getPackagesForUidNoThrow(uid);
        }

        boolean changed = false;
        AxeronConfig.PackageEntry entry = findLocked(uid);
        if (entry == null) {
            entry = new AxeronConfig.PackageEntry(uid, mask & values);
            config.packages.add(entry);
            changed = true;
        } else {
            if (entry.packages == null) {
                entry.packages = new ArrayList<>();
                changed = true;
            }
            int newValue = (entry.flags & ~mask) | (mask & values);
            if (newValue != entry.flags) {
                entry.flags = newValue;
                changed = true;
            }
        }
        if (packages != null) {
            for (String packageName : packages) {
                if (entry.packages.contains(packageName)) {
                    continue;
                }
                entry.packages.add(packageName);
                changed = true;
            }
        }
        if (changed) {
            scheduleWriteLocked();
        }
    }

    public void update(int uid, List<String> packages, int mask, int values) {
        synchronized (this) {
            updateLocked(uid, packages, mask, values);
        }
    }

    private void removeLocked(int uid) {
        AxeronConfig.PackageEntry entry = findLocked(uid);
        if (entry == null) {
            return;
        }
        config.packages.remove(entry);
        scheduleWriteLocked();
    }

    public void remove(int uid) {
        synchronized (this) {
            removeLocked(uid);
        }
    }
}
