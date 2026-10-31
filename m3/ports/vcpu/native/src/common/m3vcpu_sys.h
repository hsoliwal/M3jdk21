/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_sys.h - internal platform layer: mutex/condvar, threads, atomics, dynamic libraries,
 * module directory, directory listing, environment and bounded string helpers.
 * Win32 (kernel32 only) or POSIX (pthreads + dl). Compiled into every m3vcpu shared library
 * that needs it; never exported.
 */
#ifndef M3VCPU_SYS_H
#define M3VCPU_SYS_H

#include <stdarg.h>
#include <stddef.h>
#include <stdint.h>

#if defined(_WIN32)
/* SRWLOCK and CONDITION_VARIABLE are one pointer each; zero-init is their static initializer. */
typedef struct m3_mutex {
    void *opaque;
} m3_mutex;
typedef struct m3_cond {
    void *opaque;
} m3_cond;
#define M3_MUTEX_INITIALIZER {0}
#else
#include <pthread.h>
typedef struct m3_mutex {
    pthread_mutex_t m;
} m3_mutex;
typedef struct m3_cond {
    pthread_cond_t c;
} m3_cond;
#define M3_MUTEX_INITIALIZER {PTHREAD_MUTEX_INITIALIZER}
#endif

#if defined(_MSC_VER)
#define M3_THREAD_LOCAL __declspec(thread)
#else
#define M3_THREAD_LOCAL _Thread_local
#endif

#if defined(_WIN32)
#define M3_PATH_SEP '\\'
#define M3_LIST_SEP ';'
#else
#define M3_PATH_SEP '/'
#define M3_LIST_SEP ':'
#endif

#define M3_PATH_MAX 1024

void m3_mutex_init(m3_mutex *m);
void m3_mutex_destroy(m3_mutex *m);
void m3_mutex_lock(m3_mutex *m);
void m3_mutex_unlock(m3_mutex *m);

void m3_cond_init(m3_cond *c);
void m3_cond_destroy(m3_cond *c);
void m3_cond_wait(m3_cond *c, m3_mutex *m);
/* Returns 0 when woken (possibly spuriously), 1 on timeout. */
int m3_cond_timedwait(m3_cond *c, m3_mutex *m, uint32_t timeout_ms);
void m3_cond_signal(m3_cond *c);
void m3_cond_broadcast(m3_cond *c);

typedef struct m3_thread m3_thread;
typedef void (*m3_thread_fn)(void *arg);
int m3_thread_start(m3_thread **out, m3_thread_fn fn, void *arg); /* 0 on success */
void m3_thread_join(m3_thread *t);                                /* waits and frees */
void m3_thread_detach(m3_thread *t);                              /* frees the handle only */

int64_t m3_atomic_fetch_add64(volatile int64_t *p, int64_t v);
uint64_t m3_now_ms(void);

uint32_t m3_cpu_count(void);
uint64_t m3_physical_memory(void);

void *m3_aligned_alloc(size_t alignment, size_t bytes); /* zero-filled */
void m3_aligned_free(void *p);

typedef void (*m3_fnptr)(void);
typedef struct m3_lib m3_lib;
/* Opens a vendor runtime by name from the system directory only (System32 / SONAME). */
m3_lib *m3_lib_open_system(const char *const *names, size_t count);
/* Opens a library by absolute path; its own directory is searched for its dependencies. */
m3_lib *m3_lib_open_path(const char *utf8_path);
m3_fnptr m3_lib_sym(m3_lib *lib, const char *name);
void m3_lib_close(m3_lib *lib);

/* Directory (UTF-8, no trailing separator) of the module containing `address`. */
int m3_module_dir(const void *address, char *out, size_t cap);
typedef void (*m3_dir_cb)(void *user, const char *dir, const char *name);
/* Calls cb for every regular file in dir whose name starts with prefix and ends with suffix. */
int m3_dir_list(const char *dir, const char *prefix, const char *suffix, m3_dir_cb cb, void *user);

/* Copies the variable into buf; returns buf, or NULL when unset or empty. */
const char *m3_getenv(const char *name, char *buf, size_t cap);
int m3_setenv(const char *name, const char *value); /* value NULL unsets */

void m3_strlcpy(char *dst, const char *src, size_t cap);
void m3_vformat(char *dst, size_t cap, const char *fmt, va_list ap);
#if defined(__GNUC__) || defined(__clang__)
#define M3_PRINTF(f, a) __attribute__((format(printf, f, a)))
#else
#define M3_PRINTF(f, a)
#endif
void m3_format(char *dst, size_t cap, const char *fmt, ...) M3_PRINTF(3, 4);
int m3_strieq(const char *a, const char *b);

#endif /* M3VCPU_SYS_H */
