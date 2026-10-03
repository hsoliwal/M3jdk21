/* SPDX-License-Identifier: Apache-2.0 */
#include "com_m3_fixture_NativeProbe.h"
JNIEXPORT jint JNICALL Java_com_m3_fixture_NativeProbe_runtimeContract(JNIEnv *env, jclass type) {
    (void)env;
    (void)type;
    return 21;
}
