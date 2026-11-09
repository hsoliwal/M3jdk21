/* SPDX-License-Identifier: Apache-2.0 */
/* m3vcpu_status.c - status names and the per-thread error string for session-less calls. */
#include "m3vcpu_internal.h"

static M3_THREAD_LOCAL char m3_tls_error[512];

void m3vcpu_tls_error_set(const char *fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    m3_vformat(m3_tls_error, sizeof m3_tls_error, fmt, ap);
    va_end(ap);
}

const char *m3vcpu_tls_error(void) { return m3_tls_error; }

uint32_t m3vcpu_abi_version(void) { return M3VCPU_ABI_V1; }

const char *m3vcpu_status_name(int32_t status)
{
    switch (status) {
    case M3VCPU_OK:
        return "M3VCPU_OK";
    case M3VCPU_E_INVALID_ARG:
        return "M3VCPU_E_INVALID_ARG";
    case M3VCPU_E_UNAVAILABLE:
        return "M3VCPU_E_UNAVAILABLE";
    case M3VCPU_E_NO_DEVICE:
        return "M3VCPU_E_NO_DEVICE";
    case M3VCPU_E_OUT_OF_MEMORY:
        return "M3VCPU_E_OUT_OF_MEMORY";
    case M3VCPU_E_OUT_OF_RANGE:
        return "M3VCPU_E_OUT_OF_RANGE";
    case M3VCPU_E_NO_KERNEL_FORMAT:
        return "M3VCPU_E_NO_KERNEL_FORMAT";
    case M3VCPU_E_KERNEL_BUILD:
        return "M3VCPU_E_KERNEL_BUILD";
    case M3VCPU_E_LAUNCH:
        return "M3VCPU_E_LAUNCH";
    case M3VCPU_E_DRIVER:
        return "M3VCPU_E_DRIVER";
    case M3VCPU_E_ABI:
        return "M3VCPU_E_ABI";
    case M3VCPU_E_NOT_FOUND:
        return "M3VCPU_E_NOT_FOUND";
    case M3VCPU_E_INTEGRITY:
        return "M3VCPU_E_INTEGRITY";
    case M3VCPU_E_INTERNAL:
        return "M3VCPU_E_INTERNAL";
    default:
        return "M3VCPU_E_UNKNOWN";
    }
}
