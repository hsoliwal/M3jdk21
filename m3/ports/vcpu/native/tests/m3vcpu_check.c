/* SPDX-License-Identifier: Apache-2.0 */
/* m3vcpu_check.c - see m3vcpu_check.h. */
#include "m3vcpu_check.h"

#include <stdio.h>

long m3t_checks;
long m3t_failures;

void m3t_check(int ok, const char *file, int line, const char *fmt, ...)
{
    ++m3t_checks;
    if (!ok) {
        ++m3t_failures;
        if (m3t_failures <= 50) {
            char text[512];
            va_list ap;
            va_start(ap, fmt);
            m3_vformat(text, sizeof text, fmt, ap);
            va_end(ap);
            fprintf(stderr, "FAIL %s:%d: %s\n", file, line, text);
        }
    }
}

int m3t_summary(const char *suite)
{
    printf("%s: %ld checks, %ld failures -> %s\n", suite, m3t_checks, m3t_failures,
           m3t_failures == 0 ? "PASS" : "FAIL");
    fflush(stdout);
    return m3t_failures == 0 ? 0 : 1;
}
