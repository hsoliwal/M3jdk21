/* SPDX-License-Identifier: Apache-2.0 */
/* m3vcpu_sys.c - platform layer (see m3vcpu_sys.h). Win32 links kernel32 + CRT only. */
#if defined(_WIN32)
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0A00
#endif
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <windows.h>
#include <malloc.h>
#else
#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <dirent.h>
#include <dlfcn.h>
#include <errno.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>
#endif

#include "m3vcpu_sys.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct m3_thread_start_block {
    m3_thread_fn fn;
    void *arg;
} m3_thread_start_block;

/* ------------------------------------------------------------------------------------- */
#if defined(_WIN32)

_Static_assert(sizeof(SRWLOCK) == sizeof(m3_mutex), "m3_mutex must hold an SRWLOCK");
_Static_assert(sizeof(CONDITION_VARIABLE) == sizeof(m3_cond), "m3_cond must hold a CONDITION_VARIABLE");

#define M3_SRW(m) ((PSRWLOCK)(void *)(m))
#define M3_CV(c) ((PCONDITION_VARIABLE)(void *)(c))

struct m3_thread {
    HANDLE handle;
};

static int m3_to_wide(const char *utf8, wchar_t *out, int cap)
{
    int n = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, utf8, -1, out, cap);
    return n > 0 ? 0 : -1;
}

static int m3_to_utf8(const wchar_t *wide, char *out, size_t cap)
{
    int n = WideCharToMultiByte(CP_UTF8, 0, wide, -1, out, (int)cap, NULL, NULL);
    return n > 0 ? 0 : -1;
}

void m3_mutex_init(m3_mutex *m) { InitializeSRWLock(M3_SRW(m)); }
void m3_mutex_destroy(m3_mutex *m) { (void)m; }
void m3_mutex_lock(m3_mutex *m) { AcquireSRWLockExclusive(M3_SRW(m)); }
void m3_mutex_unlock(m3_mutex *m) { ReleaseSRWLockExclusive(M3_SRW(m)); }
void m3_cond_init(m3_cond *c) { InitializeConditionVariable(M3_CV(c)); }
void m3_cond_destroy(m3_cond *c) { (void)c; }
void m3_cond_wait(m3_cond *c, m3_mutex *m) { (void)SleepConditionVariableSRW(M3_CV(c), M3_SRW(m), INFINITE, 0); }
void m3_cond_signal(m3_cond *c) { WakeConditionVariable(M3_CV(c)); }
void m3_cond_broadcast(m3_cond *c) { WakeAllConditionVariable(M3_CV(c)); }

int m3_cond_timedwait(m3_cond *c, m3_mutex *m, uint32_t timeout_ms)
{
    if (SleepConditionVariableSRW(M3_CV(c), M3_SRW(m), (DWORD)timeout_ms, 0)) {
        return 0;
    }
    return GetLastError() == ERROR_TIMEOUT ? 1 : 0;
}

static DWORD WINAPI m3_thread_main(LPVOID raw)
{
    m3_thread_start_block block = *(m3_thread_start_block *)raw;
    free(raw);
    block.fn(block.arg);
    return 0;
}

int m3_thread_start(m3_thread **out, m3_thread_fn fn, void *arg)
{
    m3_thread *t = (m3_thread *)calloc(1, sizeof *t);
    m3_thread_start_block *block = (m3_thread_start_block *)malloc(sizeof *block);
    if (t == NULL || block == NULL) {
        free(t);
        free(block);
        return -1;
    }
    block->fn = fn;
    block->arg = arg;
    t->handle = CreateThread(NULL, 0, m3_thread_main, block, 0, NULL);
    if (t->handle == NULL) {
        free(t);
        free(block);
        return -1;
    }
    *out = t;
    return 0;
}

void m3_thread_join(m3_thread *t)
{
    (void)WaitForSingleObject(t->handle, INFINITE);
    (void)CloseHandle(t->handle);
    free(t);
}

void m3_thread_detach(m3_thread *t)
{
    (void)CloseHandle(t->handle);
    free(t);
}

int64_t m3_atomic_fetch_add64(volatile int64_t *p, int64_t v)
{
    return (int64_t)InterlockedExchangeAdd64((volatile LONG64 *)p, (LONG64)v);
}

uint64_t m3_now_ms(void) { return (uint64_t)GetTickCount64(); }

uint32_t m3_cpu_count(void)
{
    DWORD n = GetActiveProcessorCount(ALL_PROCESSOR_GROUPS);
    return n == 0 ? 1u : (uint32_t)n;
}

uint64_t m3_physical_memory(void)
{
    MEMORYSTATUSEX status;
    status.dwLength = sizeof status;
    return GlobalMemoryStatusEx(&status) ? (uint64_t)status.ullTotalPhys : 0u;
}

void *m3_aligned_alloc(size_t alignment, size_t bytes)
{
    void *p = _aligned_malloc(bytes, alignment);
    if (p != NULL) {
        memset(p, 0, bytes);
    }
    return p;
}

void m3_aligned_free(void *p) { _aligned_free(p); }

static m3_lib *m3_load_wide(const wchar_t *name, DWORD flags)
{
    DWORD old_mode = 0;
    HMODULE h;
    (void)SetThreadErrorMode(SEM_FAILCRITICALERRORS | SEM_NOOPENFILEERRORBOX, &old_mode);
    h = LoadLibraryExW(name, NULL, flags);
    (void)SetThreadErrorMode(old_mode, NULL);
    return (m3_lib *)(void *)h;
}

m3_lib *m3_lib_open_system(const char *const *names, size_t count)
{
    size_t i;
    for (i = 0; i < count; ++i) {
        wchar_t wide[M3_PATH_MAX];
        m3_lib *lib;
        if (m3_to_wide(names[i], wide, M3_PATH_MAX) != 0) {
            continue;
        }
        lib = m3_load_wide(wide, LOAD_LIBRARY_SEARCH_SYSTEM32);
        if (lib != NULL) {
            return lib;
        }
    }
    return NULL;
}

m3_lib *m3_lib_open_path(const char *utf8_path)
{
    wchar_t wide[M3_PATH_MAX];
    if (m3_to_wide(utf8_path, wide, M3_PATH_MAX) != 0) {
        return NULL;
    }
    return m3_load_wide(wide, LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR | LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);
}

m3_fnptr m3_lib_sym(m3_lib *lib, const char *name)
{
    FARPROC p = GetProcAddress((HMODULE)(void *)lib, name);
    return (m3_fnptr)(void (*)(void))p;
}

void m3_lib_close(m3_lib *lib)
{
    if (lib != NULL) {
        (void)FreeLibrary((HMODULE)(void *)lib);
    }
}

int m3_module_dir(const void *address, char *out, size_t cap)
{
    HMODULE module = NULL;
    wchar_t wide[M3_PATH_MAX];
    DWORD n;
    wchar_t *cut;
    if (!GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                            (LPCWSTR)address, &module)) {
        return -1;
    }
    n = GetModuleFileNameW(module, wide, M3_PATH_MAX);
    if (n == 0 || n >= M3_PATH_MAX) {
        return -1;
    }
    cut = wcsrchr(wide, L'\\');
    if (cut == NULL) {
        cut = wcsrchr(wide, L'/');
    }
    if (cut == NULL) {
        return -1;
    }
    *cut = L'\0';
    return m3_to_utf8(wide, out, cap);
}

static int m3_has_suffix(const char *name, const char *suffix)
{
    size_t n = strlen(name);
    size_t s = strlen(suffix);
    return n >= s && m3_strieq(name + (n - s), suffix);
}

int m3_dir_list(const char *dir, const char *prefix, const char *suffix, m3_dir_cb cb, void *user)
{
    char pattern[M3_PATH_MAX];
    wchar_t wide[M3_PATH_MAX];
    WIN32_FIND_DATAW data;
    HANDLE find;
    m3_format(pattern, sizeof pattern, "%s\\%s*%s", dir, prefix, suffix);
    if (m3_to_wide(pattern, wide, M3_PATH_MAX) != 0) {
        return -1;
    }
    find = FindFirstFileW(wide, &data);
    if (find == INVALID_HANDLE_VALUE) {
        return 0;
    }
    do {
        char name[M3_PATH_MAX];
        if ((data.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0) {
            continue;
        }
        if (m3_to_utf8(data.cFileName, name, sizeof name) == 0 && m3_has_suffix(name, suffix)) {
            cb(user, dir, name);
        }
    } while (FindNextFileW(find, &data));
    (void)FindClose(find);
    return 0;
}

const char *m3_getenv(const char *name, char *buf, size_t cap)
{
    wchar_t wname[128];
    wchar_t wide[M3_PATH_MAX * 2];
    DWORD n;
    if (m3_to_wide(name, wname, 128) != 0) {
        return NULL;
    }
    n = GetEnvironmentVariableW(wname, wide, (DWORD)(sizeof wide / sizeof wide[0]));
    if (n == 0 || n >= sizeof wide / sizeof wide[0]) {
        return NULL;
    }
    if (m3_to_utf8(wide, buf, cap) != 0 || buf[0] == '\0') {
        return NULL;
    }
    return buf;
}

int m3_setenv(const char *name, const char *value)
{
    wchar_t wname[128];
    wchar_t wvalue[M3_PATH_MAX];
    if (m3_to_wide(name, wname, 128) != 0) {
        return -1;
    }
    if (value == NULL) {
        return SetEnvironmentVariableW(wname, NULL) ? 0 : -1;
    }
    if (m3_to_wide(value, wvalue, M3_PATH_MAX) != 0) {
        return -1;
    }
    return SetEnvironmentVariableW(wname, wvalue) ? 0 : -1;
}

/* ------------------------------------------------------------------------------------- */
#else /* POSIX */

struct m3_thread {
    pthread_t id;
};

void m3_mutex_init(m3_mutex *m) { (void)pthread_mutex_init(&m->m, NULL); }
void m3_mutex_destroy(m3_mutex *m) { (void)pthread_mutex_destroy(&m->m); }
void m3_mutex_lock(m3_mutex *m) { (void)pthread_mutex_lock(&m->m); }
void m3_mutex_unlock(m3_mutex *m) { (void)pthread_mutex_unlock(&m->m); }
void m3_cond_init(m3_cond *c) { (void)pthread_cond_init(&c->c, NULL); }
void m3_cond_destroy(m3_cond *c) { (void)pthread_cond_destroy(&c->c); }
void m3_cond_wait(m3_cond *c, m3_mutex *m) { (void)pthread_cond_wait(&c->c, &m->m); }
void m3_cond_signal(m3_cond *c) { (void)pthread_cond_signal(&c->c); }
void m3_cond_broadcast(m3_cond *c) { (void)pthread_cond_broadcast(&c->c); }

int m3_cond_timedwait(m3_cond *c, m3_mutex *m, uint32_t timeout_ms)
{
    struct timespec ts;
    (void)clock_gettime(CLOCK_REALTIME, &ts);
    ts.tv_sec += (time_t)(timeout_ms / 1000u);
    ts.tv_nsec += (long)(timeout_ms % 1000u) * 1000000L;
    if (ts.tv_nsec >= 1000000000L) {
        ts.tv_sec += 1;
        ts.tv_nsec -= 1000000000L;
    }
    return pthread_cond_timedwait(&c->c, &m->m, &ts) == ETIMEDOUT ? 1 : 0;
}

static void *m3_thread_main(void *raw)
{
    m3_thread_start_block block = *(m3_thread_start_block *)raw;
    free(raw);
    block.fn(block.arg);
    return NULL;
}

int m3_thread_start(m3_thread **out, m3_thread_fn fn, void *arg)
{
    m3_thread *t = (m3_thread *)calloc(1, sizeof *t);
    m3_thread_start_block *block = (m3_thread_start_block *)malloc(sizeof *block);
    if (t == NULL || block == NULL) {
        free(t);
        free(block);
        return -1;
    }
    block->fn = fn;
    block->arg = arg;
    if (pthread_create(&t->id, NULL, m3_thread_main, block) != 0) {
        free(t);
        free(block);
        return -1;
    }
    *out = t;
    return 0;
}

void m3_thread_join(m3_thread *t)
{
    (void)pthread_join(t->id, NULL);
    free(t);
}

void m3_thread_detach(m3_thread *t)
{
    (void)pthread_detach(t->id);
    free(t);
}

int64_t m3_atomic_fetch_add64(volatile int64_t *p, int64_t v)
{
    return __atomic_fetch_add(p, v, __ATOMIC_SEQ_CST);
}

uint64_t m3_now_ms(void)
{
    struct timespec ts;
    (void)clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint64_t)ts.tv_sec * 1000u + (uint64_t)ts.tv_nsec / 1000000u;
}

uint32_t m3_cpu_count(void)
{
    long n = sysconf(_SC_NPROCESSORS_ONLN);
    return n < 1 ? 1u : (uint32_t)n;
}

uint64_t m3_physical_memory(void)
{
    long pages = sysconf(_SC_PHYS_PAGES);
    long size = sysconf(_SC_PAGESIZE);
    return pages < 0 || size < 0 ? 0u : (uint64_t)pages * (uint64_t)size;
}

void *m3_aligned_alloc(size_t alignment, size_t bytes)
{
    void *p = NULL;
    if (posix_memalign(&p, alignment, bytes) != 0) {
        return NULL;
    }
    memset(p, 0, bytes);
    return p;
}

void m3_aligned_free(void *p) { free(p); }

m3_lib *m3_lib_open_system(const char *const *names, size_t count)
{
    size_t i;
    for (i = 0; i < count; ++i) {
        void *h = dlopen(names[i], RTLD_NOW | RTLD_LOCAL);
        if (h != NULL) {
            return (m3_lib *)h;
        }
    }
    return NULL;
}

m3_lib *m3_lib_open_path(const char *utf8_path) { return (m3_lib *)dlopen(utf8_path, RTLD_NOW | RTLD_LOCAL); }

m3_fnptr m3_lib_sym(m3_lib *lib, const char *name)
{
    void *p = dlsym((void *)lib, name);
    m3_fnptr f;
    memcpy(&f, &p, sizeof f);
    return f;
}

void m3_lib_close(m3_lib *lib)
{
    if (lib != NULL) {
        (void)dlclose((void *)lib);
    }
}

int m3_module_dir(const void *address, char *out, size_t cap)
{
    Dl_info info;
    char *cut;
    if (dladdr(address, &info) == 0 || info.dli_fname == NULL) {
        return -1;
    }
    m3_strlcpy(out, info.dli_fname, cap);
    cut = strrchr(out, '/');
    if (cut == NULL) {
        m3_strlcpy(out, ".", cap);
        return 0;
    }
    *cut = '\0';
    return 0;
}

int m3_dir_list(const char *dir, const char *prefix, const char *suffix, m3_dir_cb cb, void *user)
{
    DIR *d = opendir(dir);
    struct dirent *e;
    size_t plen = strlen(prefix);
    size_t slen = strlen(suffix);
    if (d == NULL) {
        return 0;
    }
    while ((e = readdir(d)) != NULL) {
        size_t n = strlen(e->d_name);
        char path[M3_PATH_MAX];
        struct stat st;
        if (n < plen + slen || strncmp(e->d_name, prefix, plen) != 0
            || strcmp(e->d_name + (n - slen), suffix) != 0) {
            continue;
        }
        m3_format(path, sizeof path, "%s/%s", dir, e->d_name);
        if (stat(path, &st) == 0 && S_ISREG(st.st_mode)) {
            cb(user, dir, e->d_name);
        }
    }
    (void)closedir(d);
    return 0;
}

const char *m3_getenv(const char *name, char *buf, size_t cap)
{
    const char *v = getenv(name);
    if (v == NULL || v[0] == '\0') {
        return NULL;
    }
    m3_strlcpy(buf, v, cap);
    return buf;
}

int m3_setenv(const char *name, const char *value)
{
    return value == NULL ? unsetenv(name) : setenv(name, value, 1);
}

#endif

/* ------------------------------------------------------------------------------------- */

void m3_strlcpy(char *dst, const char *src, size_t cap)
{
    size_t n;
    if (cap == 0) {
        return;
    }
    n = src == NULL ? 0 : strlen(src);
    if (n >= cap) {
        n = cap - 1;
    }
    if (n > 0) {
        memcpy(dst, src, n);
    }
    dst[n] = '\0';
}

void m3_vformat(char *dst, size_t cap, const char *fmt, va_list ap)
{
    if (cap == 0) {
        return;
    }
    if (vsnprintf(dst, cap, fmt, ap) < 0) {
        dst[0] = '\0';
    }
    dst[cap - 1] = '\0';
}

void m3_format(char *dst, size_t cap, const char *fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    m3_vformat(dst, cap, fmt, ap);
    va_end(ap);
}

int m3_strieq(const char *a, const char *b)
{
    for (;; ++a, ++b) {
        char ca = *a;
        char cb = *b;
        if (ca >= 'A' && ca <= 'Z') {
            ca = (char)(ca - 'A' + 'a');
        }
        if (cb >= 'A' && cb <= 'Z') {
            cb = (char)(cb - 'A' + 'a');
        }
        if (ca != cb) {
            return 0;
        }
        if (ca == '\0') {
            return 1;
        }
    }
}
