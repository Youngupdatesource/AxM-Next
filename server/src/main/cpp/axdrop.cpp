// axdrop: runs a command as the adb "shell" user while the caller is root.
//
//   libaxdrop.so [-c <selinux context>] [-n] [-u <uid>] [-g <gid>] [-G <gid,gid,...>] -- prog [args...]
//
// Used by QuickShell's "Rootless debugging". Order matters:
//   1. SELinux exec context (written to /proc/self/attr/exec, applied on the next exec)
//   2. capability bounding set emptied   (needs CAP_SETPCAP, so before the uid change)
//   3. supplementary groups, gid, uid    (permitted/effective caps are lost with uid != 0)
//   4. ambient caps cleared, optional no_new_privs
//   5. exec
// If exec is refused while a context is set (policy forbids that transition), it is retried
// once without the context rather than failing the whole command.

#include <errno.h>
#include <fcntl.h>
#include <linux/prctl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/prctl.h>
#include <unistd.h>
#include <grp.h>

#ifndef PR_CAP_AMBIENT
#define PR_CAP_AMBIENT 47
#define PR_CAP_AMBIENT_CLEAR_ALL 4
#endif

static const int kShellId = 2000;
static const int kMaxGroups = 64;

static int last_cap() {
    int fd = open("/proc/sys/kernel/cap_last_cap", O_RDONLY | O_CLOEXEC);
    int last = 40;
    if (fd >= 0) {
        char buf[16] = {0};
        if (read(fd, buf, sizeof(buf) - 1) > 0) last = atoi(buf);
        close(fd);
    }
    return last;
}

static bool write_exec_context(const char *context) {
    int fd = open("/proc/self/attr/exec", O_WRONLY | O_CLOEXEC);
    if (fd < 0) return false;
    size_t len = context ? strlen(context) : 0;
    ssize_t n = write(fd, context ? context : "", len);
    close(fd);
    return n == (ssize_t) len;
}

static int parse_groups(char *list, gid_t *out) {
    int count = 0;
    for (char *tok = strtok(list, ","); tok && count < kMaxGroups; tok = strtok(nullptr, ",")) {
        out[count++] = (gid_t) atoi(tok);
    }
    return count;
}

int main(int argc, char **argv) {
    const char *context = nullptr;
    bool nnp = false;
    int uid = kShellId, gid = kShellId;
    gid_t groups[kMaxGroups];
    int group_count = 0;

    int i = 1;
    for (; i < argc; i++) {
        const char *a = argv[i];
        if (strcmp(a, "--") == 0) { i++; break; }
        if (strcmp(a, "-n") == 0) nnp = true;
        else if (strcmp(a, "-c") == 0 && i + 1 < argc) context = argv[++i];
        else if (strcmp(a, "-u") == 0 && i + 1 < argc) uid = atoi(argv[++i]);
        else if (strcmp(a, "-g") == 0 && i + 1 < argc) gid = atoi(argv[++i]);
        else if (strcmp(a, "-G") == 0 && i + 1 < argc) group_count = parse_groups(argv[++i], groups);
        else break;
    }
    if (i >= argc) {
        fprintf(stderr, "axdrop: no command\n");
        return 2;
    }
    if (group_count == 0) {
        groups[0] = (gid_t) gid;
        group_count = 1;
    }

    bool context_set = context && write_exec_context(context);

    int last = last_cap();
    for (int cap = 0; cap <= last; cap++) prctl(PR_CAPBSET_DROP, cap, 0, 0, 0);

    if (setgroups(group_count, groups) != 0 ||
        setresgid(gid, gid, gid) != 0 ||
        setresuid(uid, uid, uid) != 0) {
        fprintf(stderr, "axdrop: cannot switch to uid %d: %s\n", uid, strerror(errno));
        return 126;
    }

    prctl(PR_CAP_AMBIENT, PR_CAP_AMBIENT_CLEAR_ALL, 0, 0, 0);
    if (nnp) prctl(PR_SET_NO_NEW_PRIVS, 1, 0, 0, 0);

    execvp(argv[i], &argv[i]);
    if (context_set && (errno == EACCES || errno == EPERM)) {
        write_exec_context(nullptr);
        execvp(argv[i], &argv[i]);
    }
    fprintf(stderr, "axdrop: exec %s: %s\n", argv[i], strerror(errno));
    return 127;
}
