#ifndef OBLIVION_SYSCALLS_H
#define OBLIVION_SYSCALLS_H

#include <sys/types.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <sys/syscall.h>

/**
 * Direct Linux Assembly Syscalls for ARM64 with portable syscall() fallback
 * for non-64-bit ARM architectures (x86_64, armv7, i686) to prevent SIGILL crashes.
 */

namespace oblivion {
namespace syscalls {

inline int sys_openat(int dirfd, const char* pathname, int flags, mode_t mode) {
#if defined(__aarch64__)
    register long x0 __asm__("x0") = dirfd;
    register long x1 __asm__("x1") = (long)pathname;
    register long x2 __asm__("x2") = flags;
    register long x3 __asm__("x3") = mode;
    register long x8 __asm__("x8") = 56; // __NR_openat
    __asm__ __volatile__(
        "svc #0"
        : "=r"(x0)
        : "r"(x0), "r"(x1), "r"(x2), "r"(x3), "r"(x8)
        : "memory"
    );
    return (int)x0;
#else
    return (int)syscall(SYS_openat, dirfd, pathname, flags, mode);
#endif
}

inline ssize_t sys_read(int fd, void* buf, size_t count) {
#if defined(__aarch64__)
    register long x0 __asm__("x0") = fd;
    register long x1 __asm__("x1") = (long)buf;
    register long x2 __asm__("x2") = count;
    register long x8 __asm__("x8") = 63; // __NR_read
    __asm__ __volatile__(
        "svc #0"
        : "=r"(x0)
        : "r"(x0), "r"(x1), "r"(x2), "r"(x8)
        : "memory"
    );
    return (ssize_t)x0;
#else
    return syscall(SYS_read, fd, buf, count);
#endif
}

inline int sys_close(int fd) {
#if defined(__aarch64__)
    register long x0 __asm__("x0") = fd;
    register long x8 __asm__("x8") = 57; // __NR_close
    __asm__ __volatile__(
        "svc #0"
        : "=r"(x0)
        : "r"(x0), "r"(x8)
        : "memory"
    );
    return (int)x0;
#else
    return (int)syscall(SYS_close, fd);
#endif
}

[[noreturn]] inline void sys_exit_group(int status) {
#if defined(__aarch64__)
    register long x0 __asm__("x0") = status;
    register long x8 __asm__("x8") = 94; // __NR_exit_group
    __asm__ __volatile__(
        "svc #0"
        :
        : "r"(x0), "r"(x8)
        : "memory"
    );
#else
    syscall(SYS_exit_group, status);
#endif
    while (true) {} // Unreachable safeguard
}

} // namespace syscalls
} // namespace oblivion

#endif // OBLIVION_SYSCALLS_H
