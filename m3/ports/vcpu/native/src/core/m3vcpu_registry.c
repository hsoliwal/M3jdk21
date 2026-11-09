/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_registry.c - backend registry: built-in CPU backend, discovery of backend shared
 * libraries next to the core (and in M3VCPU_BACKEND_PATH), table validation, and lazy,
 * cached, time-bounded probing. Pattern donors (no code copied): ggml-backend-reg.cpp
 * (dlopen + score), Khronos / Vulkan loaders (struct_size-versioned dispatch tables).
 */
#include "m3vcpu_internal.h"

#include <stdlib.h>
#include <string.h>

#define M3_REG_MAX 32
#define M3_DISCOVER_MAX 64
#define M3_PROBE_TIMEOUT_DEFAULT_MS 15000u

#if defined(_WIN32)
#define M3_LIB_PREFIX "m3vcpu_"
#define M3_LIB_SUFFIX ".dll"
#elif defined(__APPLE__)
#define M3_LIB_PREFIX "libm3vcpu_"
#define M3_LIB_SUFFIX ".dylib"
#else
#define M3_LIB_PREFIX "libm3vcpu_"
#define M3_LIB_SUFFIX ".so"
#endif

typedef struct m3_reg_entry {
    const m3vcpu_backend_v1 *backend;
    m3_lib *lib;
    char origin[M3_PATH_MAX];
    int probed;
    m3vcpu_probe probe;
} m3_reg_entry;

static m3_mutex m3_reg_lock = M3_MUTEX_INITIALIZER;
static m3_reg_entry m3_reg[M3_REG_MAX];
static int32_t m3_reg_count;
static int m3_reg_ready;
static const char m3_reg_anchor = 0; /* an address inside this module */

int32_t m3vcpu_validate_backend(const m3vcpu_backend_v1 *b, char *why, size_t cap)
{
    if (b == NULL) {
        m3_strlcpy(why, "null backend table", cap);
        return M3VCPU_E_ABI;
    }
    if (b->abi != M3VCPU_ABI_V1 || b->struct_size < sizeof(m3vcpu_backend_v1)) {
        m3_format(why, cap, "abi %u / struct_size %u not accepted (want abi %u, size >= %u)",
                  (unsigned)b->abi, (unsigned)b->struct_size, (unsigned)M3VCPU_ABI_V1,
                  (unsigned)sizeof(m3vcpu_backend_v1));
        return M3VCPU_E_ABI;
    }
    if (b->name == NULL || b->name[0] == '\0' || strlen(b->name) > 31u) {
        m3_strlcpy(why, "backend name missing or longer than 31 bytes", cap);
        return M3VCPU_E_ABI;
    }
    if (b->probe == NULL || b->device_count == NULL || b->device_info == NULL || b->ctx_create == NULL
        || b->ctx_destroy == NULL || b->buf_alloc == NULL || b->buf_write == NULL || b->buf_read == NULL
        || b->buf_free == NULL || b->kernel_load == NULL || b->kernel_free == NULL || b->launch_1d == NULL
        || b->finish == NULL || b->last_error == NULL) {
        m3_format(why, cap, "backend '%s' has a null function pointer", b->name);
        return M3VCPU_E_ABI;
    }
    return M3VCPU_OK;
}

static int32_t m3_reg_find_locked(const char *name)
{
    int32_t i;
    for (i = 0; i < m3_reg_count; ++i) {
        if (m3_strieq(m3_reg[i].backend->name, name)) {
            return i;
        }
    }
    return -1;
}

static int32_t m3_reg_add_locked(const m3vcpu_backend_v1 *b, m3_lib *lib, const char *origin)
{
    char why[192];
    int32_t rc = m3vcpu_validate_backend(b, why, sizeof why);
    m3_reg_entry *e;
    if (rc != M3VCPU_OK) {
        m3vcpu_tls_error_set("%s: %s", origin, why);
        return rc;
    }
    if (m3_reg_find_locked(b->name) >= 0) {
        m3vcpu_tls_error_set("%s: backend '%s' already registered", origin, b->name);
        return M3VCPU_E_INVALID_ARG;
    }
    if (m3_reg_count >= M3_REG_MAX) {
        m3vcpu_tls_error_set("%s: registry full (%d backends)", origin, M3_REG_MAX);
        return M3VCPU_E_OUT_OF_MEMORY;
    }
    e = &m3_reg[m3_reg_count++];
    memset(e, 0, sizeof *e);
    e->backend = b;
    e->lib = lib;
    m3_strlcpy(e->origin, origin, sizeof e->origin);
    return M3VCPU_OK;
}

typedef struct m3_found {
    size_t count;
    char names[M3_DISCOVER_MAX][128];
    char dir[M3_PATH_MAX];
} m3_found;

static void m3_collect(void *user, const char *dir, const char *name)
{
    m3_found *found = (m3_found *)user;
    (void)dir;
    if (found->count < M3_DISCOVER_MAX && strlen(name) < sizeof found->names[0] && strstr(name, "jni") == NULL) {
        m3_strlcpy(found->names[found->count++], name, sizeof found->names[0]);
    }
}

static int m3_name_cmp(const void *a, const void *b) { return strcmp((const char *)a, (const char *)b); }

static void m3_discover_dir_locked(const char *dir)
{
    m3_found *found = (m3_found *)calloc(1, sizeof *found);
    size_t i;
    if (found == NULL) {
        return;
    }
    m3_strlcpy(found->dir, dir, sizeof found->dir);
    (void)m3_dir_list(dir, M3_LIB_PREFIX, M3_LIB_SUFFIX, m3_collect, found);
    qsort(found->names, found->count, sizeof found->names[0], m3_name_cmp);
    for (i = 0; i < found->count; ++i) {
        char path[M3_PATH_MAX];
        m3_lib *lib;
        m3vcpu_backend_entry_v1_fn entry;
        m3_format(path, sizeof path, "%s%c%s", dir, M3_PATH_SEP, found->names[i]);
        lib = m3_lib_open_path(path);
        if (lib == NULL) {
            continue;
        }
        entry = (m3vcpu_backend_entry_v1_fn)m3_lib_sym(lib, M3VCPU_BACKEND_ENTRY_V1_SYMBOL);
        if (entry == NULL || m3_reg_add_locked(entry(), lib, path) != M3VCPU_OK) {
            m3_lib_close(lib);
        }
    }
    free(found);
}

static void m3_reg_init_locked(void)
{
    char dir[M3_PATH_MAX];
    char list[M3_PATH_MAX * 2];
    if (m3_reg_ready) {
        return;
    }
    m3_reg_ready = 1;
    (void)m3_reg_add_locked(m3vcpu_cpu_backend_v1(), NULL, "builtin");
    if (m3_module_dir(&m3_reg_anchor, dir, sizeof dir) == 0) {
        m3_discover_dir_locked(dir);
    }
    if (m3_getenv("M3VCPU_BACKEND_PATH", list, sizeof list) != NULL) {
        char *cursor = list;
        while (cursor != NULL && *cursor != '\0') {
            char *sep = strchr(cursor, M3_LIST_SEP);
            if (sep != NULL) {
                *sep = '\0';
            }
            if (*cursor != '\0') {
                m3_discover_dir_locked(cursor);
            }
            cursor = sep == NULL ? NULL : sep + 1;
        }
    }
}

/* ---- bounded probe ------------------------------------------------------------------- */
typedef struct m3_probe_job {
    m3_mutex lock;
    m3_cond cv;
    int done;
    int refs;
    int32_t rc;
    const m3vcpu_backend_v1 *backend;
    m3vcpu_probe out;
} m3_probe_job;

static void m3_probe_job_release(m3_probe_job *job)
{
    m3_cond_destroy(&job->cv);
    m3_mutex_destroy(&job->lock);
    free(job);
}

static void m3_probe_thread(void *arg)
{
    m3_probe_job *job = (m3_probe_job *)arg;
    m3vcpu_probe local;
    int32_t rc;
    int last;
    memset(&local, 0, sizeof local);
    local.struct_size = sizeof local;
    rc = job->backend->probe(&local);
    m3_mutex_lock(&job->lock);
    job->out = local;
    job->rc = rc;
    job->done = 1;
    last = --job->refs == 0;
    m3_cond_broadcast(&job->cv);
    m3_mutex_unlock(&job->lock);
    if (last) {
        m3_probe_job_release(job);
    }
}

static uint32_t m3_probe_timeout_ms(void)
{
    char buf[32];
    if (m3_getenv("M3VCPU_PROBE_TIMEOUT_MS", buf, sizeof buf) != NULL) {
        long v = strtol(buf, NULL, 10);
        if (v > 0 && v < 600000L) {
            return (uint32_t)v;
        }
    }
    return M3_PROBE_TIMEOUT_DEFAULT_MS;
}

static int32_t m3_probe_bounded(const m3vcpu_backend_v1 *b, int builtin, m3vcpu_probe *out)
{
    m3_probe_job *job;
    m3_thread *thread = NULL;
    uint64_t deadline;
    int32_t rc;
    int last;
    memset(out, 0, sizeof *out);
    out->struct_size = sizeof *out;
    job = builtin ? NULL : (m3_probe_job *)calloc(1, sizeof *job);
    if (job == NULL) {
        return b->probe(out);
    }
    m3_mutex_init(&job->lock);
    m3_cond_init(&job->cv);
    job->refs = 2;
    job->backend = b;
    if (m3_thread_start(&thread, m3_probe_thread, job) != 0) {
        m3_probe_job_release(job);
        return b->probe(out);
    }
    m3_thread_detach(thread);
    deadline = m3_now_ms() + m3_probe_timeout_ms();
    m3_mutex_lock(&job->lock);
    while (!job->done) {
        uint64_t now = m3_now_ms();
        if (now >= deadline) {
            break;
        }
        (void)m3_cond_timedwait(&job->cv, &job->lock, (uint32_t)(deadline - now));
    }
    if (job->done) {
        *out = job->out;
        rc = job->rc;
    } else {
        m3_format(out->reason, sizeof out->reason, "probe timed out after %u ms", (unsigned)m3_probe_timeout_ms());
        rc = M3VCPU_E_UNAVAILABLE;
    }
    last = --job->refs == 0;
    m3_mutex_unlock(&job->lock);
    if (last) {
        m3_probe_job_release(job);
    }
    return rc;
}

static void m3_probe_locked(m3_reg_entry *e)
{
    int32_t rc;
    if (e->probed) {
        return;
    }
    rc = m3_probe_bounded(e->backend, e->lib == NULL, &e->probe);
    e->probe.struct_size = sizeof e->probe;
    e->probe.reason[sizeof e->probe.reason - 1u] = '\0';
    if (rc != M3VCPU_OK) {
        e->probe.available = 0;
    }
    if (e->probe.available && e->probe.device_count <= 0) {
        e->probe.available = 0;
        m3_strlcpy(e->probe.reason, "no devices", sizeof e->probe.reason);
    }
    if (!e->probe.available) {
        e->probe.score = 0;
        if (e->probe.reason[0] == '\0') {
            m3_format(e->probe.reason, sizeof e->probe.reason, "unavailable (%s)", m3vcpu_status_name(rc));
        }
    }
    e->probed = 1;
}

/* ---- public ------------------------------------------------------------------------- */
int32_t m3vcpu_backend_count(void)
{
    int32_t n;
    m3_mutex_lock(&m3_reg_lock);
    m3_reg_init_locked();
    n = m3_reg_count;
    m3_mutex_unlock(&m3_reg_lock);
    return n;
}

const m3vcpu_backend_v1 *m3vcpu_backend_at(int32_t index)
{
    const m3vcpu_backend_v1 *b = NULL;
    m3_mutex_lock(&m3_reg_lock);
    m3_reg_init_locked();
    if (index >= 0 && index < m3_reg_count) {
        b = m3_reg[index].backend;
    }
    m3_mutex_unlock(&m3_reg_lock);
    return b;
}

int32_t m3vcpu_backend_index(const char *name)
{
    int32_t i;
    if (name == NULL) {
        return -1;
    }
    m3_mutex_lock(&m3_reg_lock);
    m3_reg_init_locked();
    i = m3_reg_find_locked(name);
    m3_mutex_unlock(&m3_reg_lock);
    return i;
}

const char *m3vcpu_backend_origin(int32_t index)
{
    const char *origin = "";
    m3_mutex_lock(&m3_reg_lock);
    m3_reg_init_locked();
    if (index >= 0 && index < m3_reg_count) {
        origin = m3_reg[index].origin;
    }
    m3_mutex_unlock(&m3_reg_lock);
    return origin;
}

int32_t m3vcpu_backend_probe(int32_t index, m3vcpu_probe *out)
{
    int32_t rc = M3VCPU_OK;
    if (out == NULL) {
        return M3VCPU_E_INVALID_ARG;
    }
    m3_mutex_lock(&m3_reg_lock);
    m3_reg_init_locked();
    if (index < 0 || index >= m3_reg_count) {
        rc = M3VCPU_E_NOT_FOUND;
    } else {
        m3_probe_locked(&m3_reg[index]);
        *out = m3_reg[index].probe;
    }
    m3_mutex_unlock(&m3_reg_lock);
    return rc;
}

int32_t m3vcpu_register_backend(const m3vcpu_backend_v1 *backend)
{
    int32_t rc;
    m3_mutex_lock(&m3_reg_lock);
    m3_reg_init_locked();
    rc = m3_reg_add_locked(backend, NULL, "registered");
    if (rc == M3VCPU_OK) {
        m3_reg[m3_reg_count - 1].lib = NULL;
    }
    m3_mutex_unlock(&m3_reg_lock);
    return rc;
}
