/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include "m3_ncm_kernel.h"

#include <cstdint>
#include <new>
#include <stdexcept>
#include <vector>

namespace {
void throw_java(JNIEnv *env, const char *type, const char *message) {
    if (env->ExceptionCheck()) {
        return;
    }
    jclass clazz = env->FindClass(type);
    if (clazz != nullptr) {
        env->ThrowNew(clazz, message);
        env->DeleteLocalRef(clazz);
    }
}
}

extern "C" JNIEXPORT jint JNICALL
Java_jdk_m3_llm_model_M3NcmNative_abiVersion0(JNIEnv *, jclass) {
    return 1;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_jdk_m3_llm_model_M3NcmNative_nearest0(
        JNIEnv *env, jclass, jfloatArray vectors, jint rows, jint channels,
        jfloatArray codebooks, jint offset, jint size) {
    if (vectors == nullptr || codebooks == nullptr) {
        throw_java(env, "java/lang/NullPointerException", "null nearest-code array");
        return nullptr;
    }
    const jsize vector_count = env->GetArrayLength(vectors);
    const jsize book_count = env->GetArrayLength(codebooks);
    if (env->ExceptionCheck()) {
        return nullptr;
    }
    const std::int64_t expected_vectors = static_cast<std::int64_t>(rows) * channels;
    const std::int64_t used_books = static_cast<std::int64_t>(size) * channels;
    if (rows < 0 || channels <= 0 || size <= 0 || offset < 0
            || expected_vectors != vector_count
            || static_cast<std::int64_t>(offset) + used_books > book_count) {
        throw_java(env, "java/lang/IllegalArgumentException", "invalid nearest-code dimensions or range");
        return nullptr;
    }
    try {
        std::vector<jfloat> input(static_cast<size_t>(vector_count));
        std::vector<jfloat> books(static_cast<size_t>(used_books));
        std::vector<int> selected(static_cast<size_t>(rows));
        std::vector<jint> transfer(static_cast<size_t>(rows));
        if (vector_count != 0) {
            env->GetFloatArrayRegion(vectors, 0, vector_count, input.data());
            if (env->ExceptionCheck()) {
                return nullptr;
            }
        }
        env->GetFloatArrayRegion(codebooks, offset, static_cast<jsize>(used_books), books.data());
        if (env->ExceptionCheck()) {
            return nullptr;
        }
        const int status = m3_ncm_nearest(input.data(), input.size(),
                static_cast<size_t>(rows), static_cast<size_t>(channels),
                books.data(), books.size(), static_cast<size_t>(size),
                selected.data(), selected.size());
        if (status != 0) {
            throw_java(env, "java/lang/IllegalArgumentException",
                    status == 2 ? "nonfinite nearest-code input" : "invalid nearest-code shape");
            return nullptr;
        }
        jintArray result = env->NewIntArray(rows);
        if (result == nullptr || env->ExceptionCheck()) {
            return nullptr;
        }
        if (rows != 0) {
            for (size_t i = 0; i < selected.size(); ++i) {
                transfer[i] = static_cast<jint>(selected[i]);
            }
            env->SetIntArrayRegion(result, 0, rows, transfer.data());
            if (env->ExceptionCheck()) {
                env->DeleteLocalRef(result);
                return nullptr;
            }
        }
        return result;
    } catch (const std::bad_alloc &) {
        throw_java(env, "java/lang/OutOfMemoryError", "NCM native workspace allocation failed");
        return nullptr;
    } catch (const std::length_error &) {
        throw_java(env, "java/lang/IllegalArgumentException", "NCM native workspace exceeds capacity");
        return nullptr;
    }
}
