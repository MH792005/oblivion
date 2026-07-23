#include "syscalls.h"
#include "oblivion_crypto.h"
#include <pthread.h>
#include <unistd.h>
#include <cstring>
#include <atomic>

namespace oblivion {
namespace daemon {

static std::atomic<bool> g_daemon_running(false);

static bool contains_signature(const char* buffer, size_t buf_len, const char* sig) {
    size_t sig_len = strlen(sig);
    if (sig_len == 0 || buf_len < sig_len) return false;

    for (size_t i = 0; i <= buf_len - sig_len; ++i) {
        bool match = true;
        for (size_t j = 0; j < sig_len; ++j) {
            char c1 = buffer[i + j];
            char c2 = sig[j];
            // Case-insensitive comparison
            if (c1 >= 'A' && c1 <= 'Z') c1 += 32;
            if (c2 >= 'A' && c2 <= 'Z') c2 += 32;
            if (c1 != c2) {
                match = false;
                break;
            }
        }
        if (match) return true;
    }
    return false;
}

static void scan_proc_self_maps() {
    int fd = syscalls::sys_openat(AT_FDCWD, "/proc/self/maps", O_RDONLY, 0);
    if (fd < 0) {
        // Handle SELinux restricted environment gracefully without crashing
        return;
    }

    char buffer[4096];
    ssize_t bytes_read;

    static const char* targets[] = {
        "frida",
        "xposed",
        "gum-js",
        "substrate"
    };

    while ((bytes_read = syscalls::sys_read(fd, buffer, sizeof(buffer) - 1)) > 0) {
        buffer[bytes_read] = '\0';
        for (const char* sig : targets) {
            if (contains_signature(buffer, (size_t)bytes_read, sig)) {
                syscalls::sys_close(fd);
                // Nuclear exit bypassing libc / runtime hooks
                syscalls::sys_exit_group(137);
            }
        }
    }

    syscalls::sys_close(fd);
}

static void* daemon_loop(void* arg) {
    (void)arg;
    
    // Verify hardware crypto binding on startup
    const uint8_t salt[] = "OBLIVION_DAEMON_SALT";
    uint8_t* key = derive_hardware_key(salt, sizeof(salt) - 1);
    if (key) {
        oblivion_free_key(key, 32);
    }

    while (g_daemon_running.load()) {
        scan_proc_self_maps();
        sleep(3);
    }
    return nullptr;
}

bool start_security_daemon() {
    bool expected = false;
    if (!g_daemon_running.compare_exchange_strong(expected, true)) {
        return true; // Already running
    }

    pthread_t thread_id;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

    int res = pthread_create(&thread_id, &attr, daemon_loop, nullptr);
    pthread_attr_destroy(&attr);

    return res == 0;
}

// Automatically launch memory scanner on library load (__attribute__((constructor)))
__attribute__((constructor))
static void auto_start_on_load() {
    start_security_daemon();
}

} // namespace daemon
} // namespace oblivion
