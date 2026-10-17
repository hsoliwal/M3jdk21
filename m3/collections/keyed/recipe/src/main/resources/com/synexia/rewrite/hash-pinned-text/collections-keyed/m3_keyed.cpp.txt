// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
#include <jni.h>
#include <cstdint>
#include <limits>
#include <new>
#include <stdexcept>
#include <vector>

static void fail(JNIEnv* env, const char* type, const char* message) {
    if (env->ExceptionCheck()) return;
    jclass klass = env->FindClass(type);
    if (klass != nullptr) { env->ThrowNew(klass, message); env->DeleteLocalRef(klass); }
}
// No Java callbacks, pinned critical arrays, retained handles, or cross-call native state.
extern "C" JNIEXPORT jintArray JNICALL
Java_com_m3_collections_parallel_M3PartitionLayout_00024Native_scatter(
        JNIEnv* env, jclass, jintArray input, jint groups) {
    if (input == nullptr) { fail(env, "java/lang/NullPointerException", "ids"); return nullptr; }
    const jsize n = env->GetArrayLength(input);
    const int64_t total = static_cast<int64_t>(n) + groups + 1;
    if (groups < 0 || groups > n || total > std::numeric_limits<jint>::max() - 8LL) {
        fail(env, "java/lang/IllegalArgumentException", "invalid group count or layout size"); return nullptr;
    }
    try {
        std::vector<jint> ids(static_cast<size_t>(n));
        if (n != 0) env->GetIntArrayRegion(input, 0, n, ids.data());
        if (env->ExceptionCheck()) return nullptr;
        std::vector<jint> out(static_cast<size_t>(total), 0);
        for (jint id : ids) {
            if (id < 0 || id >= groups) {
                fail(env, "java/lang/IllegalArgumentException", "invalid group id"); return nullptr;
            }
            ++out[static_cast<size_t>(id) + 1];
        }
        for (jint g = 0; g < groups; ++g) out[g + 1] += out[g];
        std::vector<jint> cursor(out.begin(), out.begin() + groups);
        for (jsize row = 0; row < n; ++row) {
            const size_t position = static_cast<size_t>(groups) + 1 + cursor[ids[row]]++;
            out[position] = row;
        }
        jintArray result = env->NewIntArray(static_cast<jsize>(total));
        if (result == nullptr) return nullptr;
        env->SetIntArrayRegion(result, 0, static_cast<jsize>(total), out.data());
        return env->ExceptionCheck() ? nullptr : result;
    } catch (const std::bad_alloc&) {
        fail(env, "java/lang/OutOfMemoryError", "native partition allocation");
    } catch (const std::length_error&) {
        fail(env, "java/lang/OutOfMemoryError", "native partition capacity");
    } catch (...) {
        fail(env, "java/lang/InternalError", "native partition construction");
    }
    return nullptr;
}

// Full-window extrema share the admitted monotonic ring-deque atom; no signed negation.
static jlongArray window_extrema(JNIEnv* env, jlongArray input, jint width, bool maximum) {
    if (input == nullptr) { fail(env, "java/lang/NullPointerException", "values"); return nullptr; }
    if (width <= 0) { fail(env, "java/lang/IllegalArgumentException", "nonpositive window width"); return nullptr; }
    const jsize n = env->GetArrayLength(input);
    if (width > n) return env->NewLongArray(0);
    try {
        std::vector<jlong> values(static_cast<size_t>(n));
        env->GetLongArrayRegion(input, 0, n, values.data());
        if (env->ExceptionCheck()) return nullptr;
        const jsize length = n - width + 1;
        std::vector<jlong> out(static_cast<size_t>(length));
        std::vector<jint> queue(static_cast<size_t>(width));
        jint head = 0, tail = 0, size = 0;
        const auto next = [width](jint position) { return position == width - 1 ? 0 : position + 1; };
        for (jint i = 0; i < n; ++i) {
            if (size > 0 && queue[head] <= i - width) { head = next(head); --size; }
            while (size > 0) {
                const jint last = tail == 0 ? width - 1 : tail - 1;
                if (maximum ? values[queue[last]] >= values[i] : values[queue[last]] <= values[i]) break;
                tail = last; --size;
            }
            queue[tail] = i; tail = next(tail); ++size;
            if (i >= width - 1) out[i - width + 1] = values[queue[head]];
        }
        jlongArray result = env->NewLongArray(length);
        if (result == nullptr) return nullptr;
        env->SetLongArrayRegion(result, 0, length, out.data());
        return env->ExceptionCheck() ? nullptr : result;
    } catch (const std::bad_alloc&) {
        fail(env, "java/lang/OutOfMemoryError", "native window allocation");
    } catch (const std::length_error&) {
        fail(env, "java/lang/OutOfMemoryError", "native window capacity");
    } catch (...) {
        fail(env, "java/lang/InternalError", "native window extrema");
    }
    return nullptr;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_parallel_M3PartitionLayout_00024Native_windowMax(
        JNIEnv* env, jclass, jlongArray input, jint width) {
    return window_extrema(env, input, width, true);
}
extern "C" JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_parallel_M3PartitionLayout_00024Native_windowMin(
        JNIEnv* env, jclass, jlongArray input, jint width) {
    return window_extrema(env, input, width, false);
}
