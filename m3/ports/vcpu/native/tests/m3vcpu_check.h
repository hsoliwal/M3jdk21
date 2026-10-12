/* SPDX-License-Identifier: Apache-2.0 */
/* m3vcpu_check.h - minimal check counters for the C test harnesses. */
#ifndef M3VCPU_CHECK_H
#define M3VCPU_CHECK_H

#include "m3vcpu_sys.h"

extern long m3t_checks;
extern long m3t_failures;

/* Records one check; prints the first 50 failures with file:line and a printf message. */
void m3t_check(int ok, const char *file, int line, const char *fmt, ...) M3_PRINTF(4, 5);
#define CHECK(cond, ...) m3t_check((cond) ? 1 : 0, __FILE__, __LINE__, __VA_ARGS__)
int m3t_summary(const char *suite);

#endif /* M3VCPU_CHECK_H */
