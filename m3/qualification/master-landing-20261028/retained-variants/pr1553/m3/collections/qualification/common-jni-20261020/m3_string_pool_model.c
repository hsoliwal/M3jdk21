/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Deterministic JNI/syscall model around the actual production C source.
 * Forced Apple preprocessing below does not test an Apple SDK or kernel.
 */
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <jni.h>
#include <errno.h>
#include <stdint.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>

#ifndef M3_POOL_SOURCE
#error M3_POOL_SOURCE must identify the actual production M3StringPool.c
#endif

enum { EXACT, WRONG_ADDRESS, MAP_ERROR };
static struct {
    int find_calls, throw_calls, delete_calls, buffer_calls;
    int stat_calls, page_calls, map_calls, unmap_calls;
    int lookup_failure, throw_failure, buffer_failure, pending;
    int stat_failure, map_result, unmap_failure;
    mode_t file_mode;
    off_t file_size;
    long page_size;
    void *map_address, *unmap_address, *buffer_address;
    size_t map_length, unmap_length;
    jlong buffer_length;
    int map_prot, map_flags, map_fd;
    off_t map_offset;
    char type[128], message[256];
} state;

static const uintptr_t target_address = UINT64_C(0x10000000);
static const jlong target_length = 65536;
static const uintptr_t alternate_address = UINT64_C(0x20000000);
static unsigned checks;

#define REQUIRE(test) do { \
    if (!(test)) { \
        fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #test); \
        exit(1); \
    } \
} while (0)

static jclass JNICALL model_find(JNIEnv *env, const char *name) {
    (void)env;
    state.find_calls++;
    snprintf(state.type, sizeof(state.type), "%s", name);
    if (state.lookup_failure) { state.pending = 2; return NULL; }
    return (jclass)(uintptr_t)1;
}
static jint JNICALL model_throw(JNIEnv *env, jclass type, const char *message) {
    (void)env;
    REQUIRE(type == (jclass)(uintptr_t)1);
    state.throw_calls++;
    snprintf(state.message, sizeof(state.message), "%s", message);
    state.pending = state.throw_failure ? 3 : 1;
    return state.throw_failure ? JNI_ERR : JNI_OK;
}
static void JNICALL model_delete(JNIEnv *env, jobject ref) {
    (void)env;
    REQUIRE(ref == (jobject)(uintptr_t)1);
    state.delete_calls++;
}
static jobject JNICALL model_buffer(JNIEnv *env, void *address, jlong length) {
    (void)env;
    state.buffer_calls++;
    state.buffer_address = address;
    state.buffer_length = length;
    if (state.buffer_failure) { state.pending = 4; return NULL; }
    return (jobject)(uintptr_t)2;
}
static int model_fstat(int fd, struct stat *info) {
    REQUIRE(fd == 7);
    state.stat_calls++;
    if (state.stat_failure) { errno = EBADF; return -1; }
    memset(info, 0, sizeof(*info));
    info->st_mode = state.file_mode;
    info->st_size = state.file_size;
    return 0;
}
static long model_sysconf(int name) {
    REQUIRE(name == _SC_PAGESIZE);
    state.page_calls++;
    return state.page_size;
}
static void *model_mmap(void *address, size_t length, int prot, int flags,
        int fd, off_t offset) {
    state.map_calls++;
    state.map_address = address;
    state.map_length = length;
    state.map_prot = prot;
    state.map_flags = flags;
    state.map_fd = fd;
    state.map_offset = offset;
    if (state.map_result == MAP_ERROR) { errno = EEXIST; return MAP_FAILED; }
    if (state.map_result == WRONG_ADDRESS) return (void *)alternate_address;
    return address;
}
static int model_munmap(void *address, size_t length) {
    state.unmap_calls++;
    state.unmap_address = address;
    state.unmap_length = length;
    return state.unmap_failure ? -1 : 0;
}

/* System headers have already been parsed for the host. Only the production
 * source's conditional branch is changed; syscall results are model inputs. */
#if defined(M3_MODEL_APPLE)
#undef __linux__
#define __APPLE__ 1
#define __MACH__ 1
#endif
#if defined(M3_MODEL_NO_NOREPLACE)
#undef MAP_FIXED_NOREPLACE
#endif
#define fstat model_fstat
#define sysconf model_sysconf
#define mmap model_mmap
#define munmap model_munmap
#include M3_POOL_SOURCE
#undef fstat
#undef sysconf
#undef mmap
#undef munmap

static const struct JNINativeInterface_ functions = {
    .FindClass = model_find,
    .ThrowNew = model_throw,
    .DeleteLocalRef = model_delete,
    .NewDirectByteBuffer = model_buffer
};
static JNIEnv env_value = &functions;

static void reset(void) {
    memset(&state, 0, sizeof(state));
    state.file_mode = S_IFREG;
    state.file_size = target_length;
    state.page_size = 4096;
}
static jobject map(jint descriptor, jlong address, jlong length) {
    return Java_java_lang_M3StringPool_mapLexicon0(
            &env_value, NULL, descriptor, 0, address, length);
}
static void error_is(const char *message) {
    REQUIRE(state.pending == 1);
    REQUIRE(state.find_calls == 1 && state.throw_calls == 1 && state.delete_calls == 1);
    REQUIRE(strcmp(state.type, "java/io/IOException") == 0);
    REQUIRE(strcmp(state.message, message) == 0);
    REQUIRE(state.buffer_calls == 0);
}
static void passed(const char *name) {
    checks++;
    printf("PASS %s find=%d throw=%d delete=%d map=%d unmap=%d buffer=%d pending=%d %s\n",
            name, state.find_calls, state.throw_calls, state.delete_calls,
            state.map_calls, state.unmap_calls, state.buffer_calls,
            state.pending, state.message);
}

int main(void) {
    reset();
    REQUIRE(map(7, (jlong)target_address, target_length) == (jobject)(uintptr_t)2);
    REQUIRE(state.map_calls == 1 && state.unmap_calls == 0 && state.pending == 0);
    REQUIRE(state.map_address == (void *)target_address);
    REQUIRE(state.map_length == (size_t)target_length);
    REQUIRE(state.map_prot == PROT_READ && state.map_fd == 7 && state.map_offset == 0);
    REQUIRE((state.map_flags & MAP_SHARED) != 0);
    REQUIRE((state.map_flags & MAP_FIXED) == 0);
#if defined(__linux__) && defined(MAP_FIXED_NOREPLACE)
    REQUIRE(state.map_flags == (MAP_SHARED | MAP_FIXED_NOREPLACE));
#else
    REQUIRE(state.map_flags == MAP_SHARED);
#endif
    REQUIRE(state.buffer_address == (void *)target_address && state.buffer_length == target_length);
    passed("exact-map");

    reset(); state.page_size = 16384;
    REQUIRE(map(7, (jlong)target_address, target_length) != NULL);
    REQUIRE(state.map_calls == 1 && state.pending == 0);
    passed("16k-pages");

    reset(); state.map_result = WRONG_ADDRESS;
    REQUIRE(map(7, (jlong)target_address, target_length) == NULL);
    error_is("Kernel did not honor the fixed M3 lexicon address");
    REQUIRE(state.unmap_calls == 1 && state.unmap_address == (void *)alternate_address);
    REQUIRE(state.unmap_length == (size_t)target_length);
    passed("wrong-address-cleanup");

    reset(); state.map_result = MAP_ERROR;
    REQUIRE(map(7, (jlong)target_address, target_length) == NULL);
    error_is("Fixed M3 lexicon address unavailable");
    REQUIRE(state.unmap_calls == 0);
    passed("mapping-collision");

    reset(); state.buffer_failure = 1;
    REQUIRE(map(7, (jlong)target_address, target_length) == NULL);
    REQUIRE(state.pending == 4 && state.find_calls == 0 && state.throw_calls == 0);
    REQUIRE(state.unmap_calls == 1 && state.unmap_address == (void *)target_address);
    REQUIRE(state.unmap_length == (size_t)target_length);
    passed("direct-buffer-failure-cleanup");

    const jlong bad_addresses[] = {0, -1, (jlong)target_address + 1};
    for (unsigned i = 0; i < sizeof(bad_addresses) / sizeof(bad_addresses[0]); i++) {
        reset(); REQUIRE(map(7, bad_addresses[i], target_length) == NULL);
        error_is("Invalid fixed M3 lexicon address or size");
        REQUIRE(state.stat_calls == 0 && state.map_calls == 0);
        passed("invalid-address");
    }
    const jlong bad_lengths[] = {0, -1, (jlong)INT_MAX + 1, INT64_MAX};
    for (unsigned i = 0; i < sizeof(bad_lengths) / sizeof(bad_lengths[0]); i++) {
        reset(); REQUIRE(map(7, (jlong)target_address, bad_lengths[i]) == NULL);
        error_is("Invalid fixed M3 lexicon address or size");
        REQUIRE(state.stat_calls == 0 && state.map_calls == 0);
        passed("invalid-length");
    }
    reset(); REQUIRE(map(-1, (jlong)target_address, target_length) == NULL);
    error_is("Invalid or changed fixed M3 lexicon descriptor");
    REQUIRE(state.stat_calls == 0 && state.map_calls == 0);
    passed("negative-descriptor");

    reset(); state.stat_failure = 1;
    REQUIRE(map(7, (jlong)target_address, target_length) == NULL);
    error_is("Invalid or changed fixed M3 lexicon descriptor");
    REQUIRE(state.stat_calls == 1 && state.map_calls == 0);
    passed("descriptor-stat-failure");

    reset(); state.file_mode = S_IFDIR;
    REQUIRE(map(7, (jlong)target_address, target_length) == NULL);
    error_is("Invalid or changed fixed M3 lexicon descriptor");
    REQUIRE(state.map_calls == 0);
    passed("nonregular-file");

    reset(); state.file_size = target_length - 1;
    REQUIRE(map(7, (jlong)target_address, target_length) == NULL);
    error_is("Invalid or changed fixed M3 lexicon descriptor");
    REQUIRE(state.map_calls == 0);
    passed("changed-file-size");

    const long bad_pages[] = {-1, 0, 98304};
    for (unsigned i = 0; i < sizeof(bad_pages) / sizeof(bad_pages[0]); i++) {
        reset(); state.page_size = bad_pages[i];
        REQUIRE(map(7, (jlong)target_address, target_length) == NULL);
        error_is("Unaligned fixed M3 lexicon address");
        REQUIRE(state.map_calls == 0);
        passed("invalid-page-size-or-alignment");
    }

    reset(); state.lookup_failure = 1;
    REQUIRE(map(7, 0, target_length) == NULL);
    REQUIRE(state.pending == 2 && state.find_calls == 1 && state.throw_calls == 0 && state.delete_calls == 0);
    passed("class-lookup-failure-preserved");

    reset(); state.throw_failure = 1;
    REQUIRE(map(7, 0, target_length) == NULL);
    REQUIRE(state.pending == 3 && state.find_calls == 1 && state.throw_calls == 1 && state.delete_calls == 1);
    passed("exception-allocation-failure-preserved");

    reset();
    REQUIRE(Java_java_lang_M3StringPool_unmapLexicon0(&env_value, NULL,
            (jlong)target_address, target_length) == JNI_TRUE);
    REQUIRE(state.unmap_calls == 1 && state.unmap_address == (void *)target_address);
    passed("unmap-success");
    reset(); state.unmap_failure = 1;
    REQUIRE(Java_java_lang_M3StringPool_unmapLexicon0(&env_value, NULL,
            (jlong)target_address, target_length) == JNI_FALSE);
    REQUIRE(state.unmap_calls == 1);
    passed("unmap-failure");
    reset();
    REQUIRE(Java_java_lang_M3StringPool_unmapLexicon0(&env_value, NULL, 0, target_length) == JNI_FALSE);
    REQUIRE(state.unmap_calls == 0);
    passed("unmap-invalid-range");
    printf("MODEL_CHECKS=%u\n", checks);
    return 0;
}
