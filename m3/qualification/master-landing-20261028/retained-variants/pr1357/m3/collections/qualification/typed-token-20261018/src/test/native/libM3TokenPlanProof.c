/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Host qualification only; production symbols remain in libjava.
 */
#include "jdk_internal_mindex_M3RegexDpPlan.h"
static jlong calls;
static jlongArray compose(JNIEnv *e, jclass c, jlongArray a, jlongArray b) {
    calls++;
    return Java_jdk_internal_mindex_M3RegexDpPlan_compose0(e, c, a, b);
}
static jlongArray structured(JNIEnv *e, jclass c, jlongArray a, jlongArray b) {
    calls++;
    return Java_jdk_internal_mindex_M3RegexDpPlan_composeStructured0(e, c, a, b);
}
static jlongArray repeated(JNIEnv *e, jclass c, jlongArray a, jlong n, jboolean s) {
    calls++;
    return Java_jdk_internal_mindex_M3RegexDpPlan_repeat0(e, c, a, n, s);
}
JNIEXPORT void JNICALL Java_M3TokenRegexPlanContract_registerKernel(JNIEnv *e, jclass ignored) {
    (void) ignored;
    jclass owner = (*e)->FindClass(e, "jdk/internal/mindex/M3RegexDpPlan");
    if (owner == NULL) return;
    JNINativeMethod methods[] = {
        {"compose0", "([J[J)[J", (void *) compose},
        {"composeStructured0", "([J[J)[J", (void *) structured},
        {"repeat0", "([JJZ)[J", (void *) repeated}
    };
    if ((*e)->RegisterNatives(e, owner, methods, 3) != 0 && !(*e)->ExceptionCheck(e)) {
        jclass failure = (*e)->FindClass(e, "java/lang/AssertionError");
        if (failure != NULL) {
            (*e)->ThrowNew(e, failure, "registration failed");
            (*e)->DeleteLocalRef(e, failure);
        }
    }
    (*e)->DeleteLocalRef(e, owner);
}
JNIEXPORT jlong JNICALL Java_M3TokenRegexPlanContract_nativeCalls(JNIEnv *e, jclass ignored) {
    (void) e; (void) ignored;
    return calls;
}
