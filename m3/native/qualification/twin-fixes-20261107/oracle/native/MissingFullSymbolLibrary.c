/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/bee80508c334ec11d9f82ad05106b556f6036546/synexia-openrewrite-recipes/recipe-crates/m3-jni-fullscan-missing-symbol-20261107/oracle/MissingFullSymbolLibrary.c blob d7d057d42c8d; recipe m3/native/qualification/twin-fixes-20261107/apply.py v1
#include <jni.h>

/* Loadable JNI peer intentionally lacking the full and window scan symbols. */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) vm;
    (void) reserved;
    return JNI_VERSION_1_6;
}
