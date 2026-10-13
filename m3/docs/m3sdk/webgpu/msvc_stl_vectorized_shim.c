// Probe-only toolchain shim (Apache-2.0). The prebuilt Dawn release (GitHub windows-latest) was compiled with a newer
// MSVC STL than the local 14.44 toolset and references six vectorized algorithm helpers that 14.44's msvcprt.lib
// does not export. These scalar versions implement the same contracts (std::unique, std::rotate, first min/max
// element over unsigned 4/8-byte keys). A production m3vcpu webgpu backend builds Dawn from source with the pinned
// toolchain instead and drops this file.
#include <stdint.h>
#include <string.h>

#define M3_UNIQUE(name, T)                                                   \
    void *__stdcall name(void *first, void *last) {                          \
        T *f = (T *)first, *l = (T *)last;                                   \
        if (f == l) return last;                                             \
        T *out = f;                                                          \
        for (T *p = f + 1; p != l; ++p) if (*p != *out) *++out = *p;         \
        return out + 1;                                                      \
    }

#define M3_EXTREME(name, T, better)                                          \
    const void *__stdcall name(const void *first, const void *last) {        \
        const T *f = (const T *)first, *l = (const T *)last, *best = f;      \
        if (f == l) return last;                                             \
        for (const T *p = f + 1; p != l; ++p) if (*p better *best) best = p; \
        return best;                                                         \
    }

M3_UNIQUE(__std_unique_4, uint32_t)
M3_UNIQUE(__std_unique_8, uint64_t)
M3_EXTREME(__std_min_element_4u, uint32_t, <)
M3_EXTREME(__std_min_element_8u, uint64_t, <)
M3_EXTREME(__std_max_element_4u, uint32_t, >)

static void reverse_bytes(unsigned char *f, unsigned char *l) {
    while (f < l && f < --l) { unsigned char t = *f; *f++ = *l; *l = t; }
}

void __stdcall __std_rotate(void *first, void *mid, void *last) {
    reverse_bytes((unsigned char *)first, (unsigned char *)mid);
    reverse_bytes((unsigned char *)mid, (unsigned char *)last);
    reverse_bytes((unsigned char *)first, (unsigned char *)last);
}
