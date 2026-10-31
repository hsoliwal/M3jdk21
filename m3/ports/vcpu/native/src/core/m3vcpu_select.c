/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_select.c - selector parsing, scoring and m3vcpu_open.
 * Policies: AUTO (highest probe score, or the M3VCPU_BACKEND list when set), GPU (best GPU
 * device of any backend), explicit ordered list "name[:device],...". Pattern donor (no code
 * copied): OpenVINO AUTO meta-device; ggml backend scoring.
 */
#include "m3vcpu_internal.h"

#include <stdlib.h>
#include <string.h>

#define M3_PICK_MAX 16

typedef struct m3_pick {
    char name[32];
    int32_t device;
} m3_pick;

typedef struct m3_why {
    char text[480];
    int32_t status; /* first failure status */
} m3_why;

static void m3_why_add(m3_why *why, int32_t status, const char *name, const char *detail)
{
    size_t used = strlen(why->text);
    if (why->status == M3VCPU_OK) {
        why->status = status;
    }
    if (used + 4u < sizeof why->text) {
        m3_format(why->text + used, sizeof why->text - used, "%s%s: %s", used == 0 ? "" : "; ", name, detail);
    }
}

/* Parses "name[:device],..." into picks; returns count or -1 on a syntax error. */
static int m3_parse_list(const char *list, m3_pick *picks, int cap)
{
    int count = 0;
    const char *p = list;
    while (*p != '\0') {
        const char *end = strchr(p, ',');
        size_t len = end == NULL ? strlen(p) : (size_t)(end - p);
        char item[64];
        char *colon;
        size_t first = 0;
        while (first < len && p[first] == ' ') {
            ++first;
        }
        while (len > first && p[len - 1u] == ' ') {
            --len;
        }
        if (len > first) {
            if (count >= cap || len - first >= sizeof item) {
                return -1;
            }
            memcpy(item, p + first, len - first);
            item[len - first] = '\0';
            picks[count].device = -1;
            colon = strchr(item, ':');
            if (colon != NULL) {
                char *stop = NULL;
                long dev = strtol(colon + 1, &stop, 10);
                if (stop == colon + 1 || *stop != '\0' || dev < 0 || dev > 4096) {
                    return -1;
                }
                picks[count].device = (int32_t)dev;
                *colon = '\0';
            }
            if (item[0] == '\0' || strlen(item) >= sizeof picks[0].name) {
                return -1;
            }
            m3_strlcpy(picks[count].name, item, sizeof picks[0].name);
            ++count;
        }
        if (end == NULL) {
            break;
        }
        p = end + 1;
    }
    return count;
}

static int m3_is_gpu(uint32_t kind)
{
    return kind == M3VCPU_DEVICE_GPU_DISCRETE || kind == M3VCPU_DEVICE_GPU_INTEGRATED;
}

/* Best device of a backend (highest score, lowest index on ties); -1 when none matches. */
static int32_t m3_best_device(const m3vcpu_backend_v1 *b, int gpu_only, int32_t *score)
{
    int32_t n = b->device_count();
    int32_t best = -1;
    int32_t i;
    *score = 0;
    for (i = 0; i < n; ++i) {
        m3vcpu_device_info info;
        memset(&info, 0, sizeof info);
        info.struct_size = sizeof info;
        if (b->device_info(i, &info) != M3VCPU_OK || (gpu_only && !m3_is_gpu(info.kind))) {
            continue;
        }
        if (best < 0 || info.score > *score) {
            best = i;
            *score = info.score;
        }
    }
    return best;
}

static int32_t m3_try_open(int32_t index, int32_t device, int gpu_only, const m3vcpu_ctx_opts *opts,
                           m3vcpu_session **out, m3_why *why)
{
    const m3vcpu_backend_v1 *b = m3vcpu_backend_at(index);
    m3vcpu_probe probe;
    int32_t score;
    int32_t rc;
    if (b == NULL || m3vcpu_backend_probe(index, &probe) != M3VCPU_OK) {
        m3_why_add(why, M3VCPU_E_NOT_FOUND, "?", "not registered");
        return M3VCPU_E_NOT_FOUND;
    }
    if (!probe.available) {
        m3_why_add(why, M3VCPU_E_UNAVAILABLE, b->name, probe.reason);
        return M3VCPU_E_UNAVAILABLE;
    }
    if (device < 0) {
        device = m3_best_device(b, gpu_only, &score);
        if (device < 0) {
            m3_why_add(why, M3VCPU_E_NO_DEVICE, b->name, gpu_only ? "no GPU device" : "no device");
            return M3VCPU_E_NO_DEVICE;
        }
    } else if (device >= b->device_count()) {
        m3_why_add(why, M3VCPU_E_NO_DEVICE, b->name, "device index out of range");
        return M3VCPU_E_NO_DEVICE;
    }
    rc = m3vcpu_session_create(b, device, opts, out);
    if (rc != M3VCPU_OK) {
        m3_why_add(why, rc, b->name, m3vcpu_tls_error());
    }
    return rc;
}

static int32_t m3_open_list(const char *list, const char *source, int32_t device, const m3vcpu_ctx_opts *opts,
                            m3vcpu_session **out, m3_why *why)
{
    m3_pick picks[M3_PICK_MAX];
    int count = m3_parse_list(list, picks, M3_PICK_MAX);
    int i;
    if (count <= 0) {
        m3_why_add(why, M3VCPU_E_INVALID_ARG, source, "malformed backend list");
        return M3VCPU_E_INVALID_ARG;
    }
    for (i = 0; i < count; ++i) {
        int32_t index = m3vcpu_backend_index(picks[i].name);
        if (index < 0) {
            m3_why_add(why, M3VCPU_E_NOT_FOUND, picks[i].name, "not registered");
            continue;
        }
        if (m3_try_open(index, device >= 0 ? device : picks[i].device, 0, opts, out, why) == M3VCPU_OK) {
            return M3VCPU_OK;
        }
    }
    return why->status == M3VCPU_OK ? M3VCPU_E_UNAVAILABLE : why->status;
}

/* AUTO / GPU: candidates ordered by score (stable on registration order). */
static int32_t m3_open_scored(int gpu_only, int32_t device, const m3vcpu_ctx_opts *opts, m3vcpu_session **out,
                              m3_why *why)
{
    int32_t n = m3vcpu_backend_count();
    int32_t order[32];
    int32_t scores[32];
    int32_t count = 0;
    int32_t i;
    for (i = 0; i < n && count < 32; ++i) {
        const m3vcpu_backend_v1 *b = m3vcpu_backend_at(i);
        m3vcpu_probe probe;
        int32_t score = 0;
        int32_t j;
        if (b == NULL || m3vcpu_backend_probe(i, &probe) != M3VCPU_OK || !probe.available) {
            continue;
        }
        if (gpu_only) {
            if (m3_best_device(b, 1, &score) < 0) {
                continue;
            }
        } else {
            score = probe.score;
        }
        for (j = count; j > 0 && scores[j - 1] < score; --j) {
            order[j] = order[j - 1];
            scores[j] = scores[j - 1];
        }
        order[j] = i;
        scores[j] = score;
        ++count;
    }
    for (i = 0; i < count; ++i) {
        if (m3_try_open(order[i], device, gpu_only, opts, out, why) == M3VCPU_OK) {
            return M3VCPU_OK;
        }
    }
    if (count == 0) {
        m3_why_add(why, M3VCPU_E_NO_DEVICE, gpu_only ? "gpu" : "auto", "no available backend");
    }
    return why->status == M3VCPU_OK ? M3VCPU_E_UNAVAILABLE : why->status;
}

int32_t m3vcpu_open(const char *selector, int32_t device, const m3vcpu_ctx_opts *opts, m3vcpu_session **out)
{
    char env[256];
    m3_why why;
    int32_t rc;
    memset(&why, 0, sizeof why);
    if (out == NULL) {
        m3vcpu_tls_error_set("m3vcpu_open: out is NULL");
        return M3VCPU_E_INVALID_ARG;
    }
    *out = NULL;
    if (selector == NULL || selector[0] == '\0' || m3_strieq(selector, "auto")) {
        if (m3_getenv("M3VCPU_BACKEND", env, sizeof env) != NULL && !m3_strieq(env, "auto")) {
            rc = m3_strieq(env, "gpu") ? m3_open_scored(1, device, opts, out, &why)
                                       : m3_open_list(env, "M3VCPU_BACKEND", device, opts, out, &why);
        } else {
            rc = m3_open_scored(0, device, opts, out, &why);
        }
    } else if (m3_strieq(selector, "gpu")) {
        rc = m3_open_scored(1, device, opts, out, &why);
    } else {
        rc = m3_open_list(selector, "selector", device, opts, out, &why);
    }
    if (rc != M3VCPU_OK) {
        m3vcpu_tls_error_set("m3vcpu_open(%s): %s: %s", selector == NULL ? "auto" : selector,
                             m3vcpu_status_name(rc), why.text);
    }
    return rc;
}
