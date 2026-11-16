/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Read-only Linux native capability probes. No persistent fd, mapping or ring.
 */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE 1
#endif
#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdint.h>
#include <sys/syscall.h>
#include <unistd.h>
/* New syscall headers are optional: old build sysroots must still compile. */
#if defined(__has_include)
#  if __has_include(<linux/io_uring.h>)
#    include <linux/io_uring.h>
#    define M3_HAS_URING_H 1
#  endif
#  if __has_include(<linux/openat2.h>)
#    include <linux/openat2.h>
#    define M3_HAS_OPENAT2_H 1
#  endif
#  if __has_include(<linux/stat.h>)
#    include <linux/stat.h>
#    define M3_HAS_STAT_H 1
#  endif
#endif
#include "jdk_m3_os_NativeProbe.h"

/* Stable ABI status low byte: available=0, unsupported=1, denied=2,
 * transient resource limit=3, other error=4. errno occupies upper bits.
 */
static jint encode_errno(int error) {
    int kind;
    if (error == ENOSYS || error == EOPNOTSUPP || error == EINVAL) kind = 1;
    else if (error == EPERM || error == EACCES) kind = 2;
    else if (error == EMFILE || error == ENFILE || error == ENOMEM || error == EAGAIN) kind = 3;
    else kind = 4;
    if (error < 0 || error > 4095) return 4;
    return (jint)(((unsigned)error << 8) | (unsigned)kind);
}

static jint fd_result(long result) {
    if (result >= 0) {
        if (close((int)result) == 0) return 0;
        return encode_errno(errno);
    }
    return encode_errno(errno);
}

JNIEXPORT jint JNICALL Java_jdk_m3_os_NativeProbe_abiVersion0(JNIEnv *env, jclass type) {
    (void)env; (void)type;
    return 1;
}

JNIEXPORT jint JNICALL Java_jdk_m3_os_NativeProbe_pageSize0(JNIEnv *env, jclass type) {
    (void)env; (void)type;
    long page = sysconf(_SC_PAGESIZE);
    return page > 0 && page <= INT_MAX ? (jint)page : 0;
}

JNIEXPORT jint JNICALL Java_jdk_m3_os_NativeProbe_probe0(JNIEnv *env, jclass type,
                                                           jint feature) {
    (void)env; (void)type;
    switch (feature) {
        case 0: { /* io_uring_setup: ring opened only for probing, then closed */
#if defined(__NR_io_uring_setup) && defined(M3_HAS_URING_H)
            struct io_uring_params options = {0};
            errno = 0;
            long fd = syscall(__NR_io_uring_setup, 2u, &options);
            return fd_result(fd);
#else
            return encode_errno(ENOSYS);
#endif
        }
        case 1: { /* pidfd_open: own PID, no signal and no process mutation */
#ifdef __NR_pidfd_open
            errno = 0;
            long fd = syscall(__NR_pidfd_open, getpid(), 0u);
            return fd_result(fd);
#else
            return encode_errno(ENOSYS);
#endif
        }
        case 2: { /* openat2: read-only current directory */
#if defined(__NR_openat2) && defined(M3_HAS_OPENAT2_H)
            struct open_how how = {0};
            how.flags = O_RDONLY | O_CLOEXEC;
            errno = 0;
            long fd = syscall(__NR_openat2, AT_FDCWD, ".", &how, sizeof(how));
            return fd_result(fd);
#else
            return encode_errno(ENOSYS);
#endif
        }
        case 3: { /* statx: read-only metadata query */
#if defined(__NR_statx) && defined(M3_HAS_STAT_H)
            struct statx info = {0};
            errno = 0;
            long ret = syscall(__NR_statx, AT_FDCWD, ".", AT_SYMLINK_NOFOLLOW,
                               STATX_BASIC_STATS, &info);
            return ret == 0 ? 0 : encode_errno(errno);
#else
            return encode_errno(ENOSYS);
#endif
        }
        default:
            return encode_errno(EINVAL);
    }
}
