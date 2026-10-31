/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Test-only JNI registration of the real product mapping functions.
 * Fixture methods allocate a file and an occupied range; they do not implement
 * or replace the product's mapping algorithm.
 */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#ifndef M3_POOL_SOURCE
#error M3_POOL_SOURCE must identify the actual production M3StringPool.c
#endif
#include M3_POOL_SOURCE

static void fixture_error(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/io/IOException");
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static jint JNICALL open_fixture(JNIEnv *env, jclass type, jint length) {
    char name[] = "/tmp/m3-jni-lexicon-XXXXXX";
    unsigned char block[4096];
    int fd;
    size_t written = 0;
    (void)type;
    if (length != 65536) { fixture_error(env, "Invalid test fixture length"); return -1; }
    fd = mkstemp(name);
    if (fd < 0) { fixture_error(env, "Cannot create test lexicon"); return -1; }
    if (unlink(name) != 0) {
        (void)close(fd); fixture_error(env, "Cannot unlink test lexicon"); return -1;
    }
    for (size_t i = 0; i < sizeof(block); i++) block[i] = (unsigned char)(i * 17 + 3);
    while (written < (size_t)length) {
        ssize_t n = write(fd, block, sizeof(block));
        if (n < 0 && errno == EINTR) continue;
        if (n <= 0) { (void)close(fd); fixture_error(env, "Cannot fill test lexicon"); return -1; }
        /* Ordinary regular files are expected to take each block; preserve the
         * exact pattern if the host returns a short write. */
        written += (size_t)n;
        if (n != (ssize_t)sizeof(block)) {
            size_t remain = sizeof(block) - (size_t)n;
            while (remain != 0) {
                ssize_t extra = write(fd, block + sizeof(block) - remain, remain);
                if (extra < 0 && errno == EINTR) continue;
                if (extra <= 0) { (void)close(fd); fixture_error(env, "Short test lexicon write"); return -1; }
                written += (size_t)extra;
                remain -= (size_t)extra;
            }
        }
    }
    return fd;
}
static void JNICALL close_fixture(JNIEnv *env, jclass type, jint fd) {
    (void)type;
    if (close(fd) != 0) fixture_error(env, "Cannot close test lexicon");
}
static jlong JNICALL reserve_sentinel(JNIEnv *env, jclass type, jint length) {
    const size_t alignment = 65536;
    size_t total = (size_t)length + alignment;
    unsigned char *raw;
    uintptr_t aligned;
    size_t prefix, suffix;
    (void)type;
    if (length != 65536) { fixture_error(env, "Invalid sentinel length"); return 0; }
    raw = mmap(NULL, total, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (raw == MAP_FAILED) { fixture_error(env, "Cannot reserve sentinel"); return 0; }
    aligned = ((uintptr_t)raw + alignment - 1) & ~(uintptr_t)(alignment - 1);
    prefix = (size_t)(aligned - (uintptr_t)raw);
    suffix = total - prefix - (size_t)length;
    if (prefix != 0 && munmap(raw, prefix) != 0) {
        (void)munmap(raw, total); fixture_error(env, "Cannot trim sentinel prefix"); return 0;
    }
    if (suffix != 0 && munmap((void *)(aligned + (size_t)length), suffix) != 0) {
        (void)munmap((void *)aligned, total - prefix);
        fixture_error(env, "Cannot trim sentinel suffix"); return 0;
    }
    memset((void *)aligned, 0x5a, (size_t)length);
    return (jlong)aligned;
}
static jboolean JNICALL sentinel_intact(JNIEnv *env, jclass type, jlong address, jint length) {
    const unsigned char *p = (const unsigned char *)(uintptr_t)address;
    (void)env; (void)type;
    for (jint i = 0; i < length; i++) if (p[i] != 0x5a) return JNI_FALSE;
    return JNI_TRUE;
}
static void JNICALL release_sentinel(JNIEnv *env, jclass type, jlong address, jint length) {
    (void)type;
    if (munmap((void *)(uintptr_t)address, (size_t)length) != 0)
        fixture_error(env, "Cannot release sentinel");
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    jclass type;
    jint result;
    static const JNINativeMethod methods[] = {
        {"mapLexicon0", "(IJJJ)Ljava/nio/ByteBuffer;", (void *)Java_java_lang_M3StringPool_mapLexicon0},
        {"unmapLexicon0", "(JJ)Z", (void *)Java_java_lang_M3StringPool_unmapLexicon0},
        {"openFixture0", "(I)I", (void *)open_fixture},
        {"closeFixture0", "(I)V", (void *)close_fixture},
        {"reserveSentinel0", "(I)J", (void *)reserve_sentinel},
        {"sentinelIntact0", "(JI)Z", (void *)sentinel_intact},
        {"releaseSentinel0", "(JI)V", (void *)release_sentinel}
    };
    (void)reserved;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;
    type = (*env)->FindClass(env, "M3StringPoolNativeTest");
    if (type == NULL) return JNI_ERR;
    result = (*env)->RegisterNatives(env, type, methods,
            (jint)(sizeof(methods) / sizeof(methods[0])));
    (*env)->DeleteLocalRef(env, type);
    return result == JNI_OK ? JNI_VERSION_1_6 : JNI_ERR;
}
