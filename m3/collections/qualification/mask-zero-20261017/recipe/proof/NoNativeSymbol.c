/* SPDX-License-Identifier: Apache-2.0 */
/* Negative fixture: loads successfully but deliberately has no selector JNI symbol. */
#include <jni.h>
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)vm;
    (void)reserved;
    return JNI_VERSION_1_8;
}
