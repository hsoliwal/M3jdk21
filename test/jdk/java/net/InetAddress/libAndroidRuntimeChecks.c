/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
#include "jni.h"

#ifndef ERROR_LIBRARY
#define ERROR_LIBRARY "libjava.so"
#endif

#if defined(__linux__)
#include <dlfcn.h>
#include <errno.h>
#include <limits.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>
#endif

JNIEXPORT jboolean JNICALL
Java_AndroidRuntimeChecks_hasIpv6Loopback(JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
#if defined(__linux__)
    int fd = socket(AF_INET6, SOCK_DGRAM, 0);
    if (fd < 0) {
        return JNI_FALSE;
    }
    struct sockaddr_in6 address = {0};
    address.sin6_family = AF_INET6;
    address.sin6_addr = in6addr_loopback;
    int result = bind(fd, (struct sockaddr *)&address, sizeof(address));
    close(fd);
    if (result != 0) {
        return JNI_FALSE;
    }
#if !defined(__ANDROID__)
    /* Preserve the existing desktop Linux policy when procfs is restricted. */
    FILE *interfaces = fopen("/proc/net/if_inet6", "r");
    if (interfaces == NULL) {
        return JNI_FALSE;
    }
    fclose(interfaces);
#endif
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

JNIEXPORT jint JNICALL
Java_AndroidRuntimeChecks_errorStringFailures(JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
#if defined(__linux__)
    typedef int (*error_function)(int, char *, size_t);
    /* The JVM may have loaded libjava with local symbol visibility. */
    void *library = dlopen(ERROR_LIBRARY, RTLD_NOW);
    if (library == NULL) {
        return 1;
    }
    void *symbol = dlsym(library, "getErrorString");
    error_function get_error;
    _Static_assert(sizeof(get_error) == sizeof(symbol), "POSIX function pointer size");
    if (symbol == NULL) {
        dlclose(library);
        return 1;
    }
    memcpy(&get_error, &symbol, sizeof(get_error));
    char buf[256];
    memset(buf, 'X', sizeof(buf));
    int failures = 0;
    if (get_error(0, buf, sizeof(buf)) != 0 || buf[0] != 'X') failures |= 2;
    if (get_error(EACCES, buf, 0) != 0 || buf[0] != 'X') failures |= 4;
    if (get_error(EACCES, buf, sizeof(buf)) != 0 || buf[0] == '\0' ||
        memchr(buf, '\0', sizeof(buf)) == NULL) failures |= 8;
    if (get_error(EACCES, buf, 1) != ERANGE || buf[0] != '\0') failures |= 16;
    int unknown = get_error(INT_MAX, buf, sizeof(buf));
    if ((unknown != 0 && unknown != EINVAL) || buf[0] == '\0' ||
        memchr(buf, '\0', sizeof(buf)) == NULL) failures |= 32;
    dlclose(library);
    return failures;
#else
    return 0;
#endif
}
