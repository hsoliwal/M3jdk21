/*
 * SPDX-FileCopyrightText: 2025 hermann kumbong
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: MIT AND Apache-2.0
 *
 * JNI primitives for jdk.m3.llm.projection.M3NcmNativeOps in the single jdk.m3.llm
 * NCM native owner (library m3_ncm). Resize, Phi and tied-logit batches translate
 * HazyResearch/ncm models/quant.py and models/next_concept.py at
 * c70274273f489abc73832b2b98584a8483b6b203; see NCM_DONOR_NOTICE.md for the MIT
 * notice. Nearest-code selection calls the owner kernel's FP32 expanded-norm entry
 * m3_ncm_nearest_expanded_f32 (ncm_kernel.cpp), bit-identical to M3NcmOps.nearest.
 * Flat FP32 CPU batches; no retained JNI references or native allocations.
 */
#include <jni.h>
#include "m3_ncm_kernel.h"

#include <cmath>
#include <cstdint>
#include <cstdlib>

#define JNI_NAME(name) Java_jdk_m3_llm_projection_M3NcmNativeOps_##name

namespace {
constexpr std::uint64_t MAX_ELEMENTS = UINT64_C(16777216);

struct Floats { jfloatArray array; jfloat *data; jsize size; };

void fail(JNIEnv *env, const char *type, const char *message) {
    if (env->ExceptionCheck()) return;
    jclass exception = env->FindClass(type);
    if (exception != nullptr) { (void) env->ThrowNew(exception, message); env->DeleteLocalRef(exception); }
}
void invalid(JNIEnv *env) {
    fail(env, "java/lang/IllegalArgumentException", "invalid NCM native shape, value, budget or overflow");
}
bool acquire(JNIEnv *env, jfloatArray array, Floats *out) {
    if (array == nullptr) { fail(env, "java/lang/NullPointerException", "native input"); return false; }
    out->array = array;
    out->size = env->GetArrayLength(array);
    out->data = env->GetFloatArrayElements(array, nullptr);
    return out->data != nullptr;
}
void release(JNIEnv *env, Floats *input) {
    if (input->data != nullptr) env->ReleaseFloatArrayElements(input->array, input->data, JNI_ABORT);
    input->data = nullptr;
}
bool finite_input(const Floats *input) {
    for (jsize i = 0; i < input->size; i++) if (!std::isfinite(input->data[i])) return false;
    return true;
}
void *allocate(JNIEnv *env, std::uint64_t count, size_t width) {
    if (count > MAX_ELEMENTS || count > SIZE_MAX / width) { invalid(env); return nullptr; }
    void *memory = std::calloc(static_cast<size_t>(count == 0 ? 1 : count), width);
    if (memory == nullptr) fail(env, "java/lang/OutOfMemoryError", "NCM native batch");
    return memory;
}
jfloatArray float_result(JNIEnv *env, const float *data, size_t size) {
    for (size_t i = 0; i < size; i++) if (!std::isfinite(data[i])) { invalid(env); return nullptr; }
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(size));
    if (result != nullptr && size != 0) env->SetFloatArrayRegion(result, 0, static_cast<jsize>(size), data);
    return result;
}
bool shape3(jint batch, jint length, jint channels, jsize actual) {
    if (batch <= 0 || length <= 0 || channels <= 0) return false;
    return static_cast<double>(batch) * static_cast<double>(length) * static_cast<double>(channels)
            == static_cast<double>(actual);
}
}

extern "C" {

JNIEXPORT jint JNICALL JNI_NAME(abiVersionNative)(JNIEnv *, jclass) {
    return 1;
}

JNIEXPORT jintArray JNICALL JNI_NAME(nearestNative)(
        JNIEnv *env, jclass, jfloatArray rows, jint channels, jfloatArray codebook) {
    Floats x = {nullptr, nullptr, 0}, e = {nullptr, nullptr, 0};
    if (!acquire(env, rows, &x)) return nullptr;
    if (!acquire(env, codebook, &e)) { release(env, &x); return nullptr; }
    jintArray result = nullptr;
    if (channels <= 0 || x.size <= 0 || x.size % channels != 0 || e.size <= 0 || e.size % channels != 0) {
        invalid(env); release(env, &e); release(env, &x); return nullptr;
    }
    const size_t c = static_cast<size_t>(channels), r = static_cast<size_t>(x.size) / c;
    const size_t v = static_cast<size_t>(e.size) / c;
    if (static_cast<std::uint64_t>(r) + static_cast<std::uint64_t>(v) > MAX_ELEMENTS) {
        invalid(env); release(env, &e); release(env, &x); return nullptr;
    }
    int *selected = static_cast<int *>(allocate(env, r, sizeof(int)));
    float *norms = selected == nullptr ? nullptr : static_cast<float *>(allocate(env, v, sizeof(float)));
    jint *transfer = norms == nullptr ? nullptr : static_cast<jint *>(allocate(env, r, sizeof(jint)));
    if (transfer != nullptr) {
        // FP32 expanded-norm entry of the owner kernel: bit-identical to M3NcmOps.nearest.
        const int status = m3_ncm_nearest_expanded_f32(x.data, static_cast<size_t>(x.size), r, c,
                e.data, static_cast<size_t>(e.size), v, norms, v, selected, r);
        if (status != 0) invalid(env);
        else {
            for (size_t i = 0; i < r; i++) transfer[i] = static_cast<jint>(selected[i]);
            result = env->NewIntArray(static_cast<jsize>(r));
            if (result != nullptr) env->SetIntArrayRegion(result, 0, static_cast<jsize>(r), transfer);
        }
    }
    std::free(norms);
    std::free(transfer); std::free(selected); release(env, &e); release(env, &x); return result;
}

JNIEXPORT jfloatArray JNICALL JNI_NAME(resizeNative)(
        JNIEnv *env, jclass, jfloatArray values, jint batch, jint from_length,
        jint channels, jint to_length, jboolean linear) {
    Floats x = {nullptr, nullptr, 0};
    if (!acquire(env, values, &x)) return nullptr;
    const double output_count = static_cast<double>(batch) * static_cast<double>(to_length)
            * static_cast<double>(channels);
    if (!shape3(batch, from_length, channels, x.size) || to_length <= 0
            || output_count > static_cast<double>(MAX_ELEMENTS) || !finite_input(&x)) {
        invalid(env); release(env, &x); return nullptr;
    }
    const size_t b = static_cast<size_t>(batch), n = static_cast<size_t>(from_length);
    const size_t c = static_cast<size_t>(channels), m = static_cast<size_t>(to_length);
    const size_t count = b * m * c;
    float *out = static_cast<float *>(allocate(env, count, sizeof(float)));
    jfloatArray result = nullptr;
    if (out != nullptr) {
        for (size_t bi = 0; bi < b; bi++) for (size_t j = 0; j < m; j++) {
            if (linear == JNI_TRUE) {
                const float scale = static_cast<float>(n) / static_cast<float>(m);
                float source = scale * (static_cast<float>(j) + 0.5f) - 0.5f;
                if (source < 0.0f) source = 0.0f;
                size_t lo = static_cast<size_t>(source); if (lo >= n) lo = n - 1;
                size_t hi = lo + 1; if (hi >= n) hi = n - 1;
                float alpha = source - static_cast<float>(lo);
                if (alpha < 0.0f) alpha = 0.0f;
                if (alpha > 1.0f) alpha = 1.0f;
                for (size_t k = 0; k < c; k++)
                    out[(bi * m + j) * c + k] = (1.0f - alpha) * x.data[(bi * n + lo) * c + k]
                            + alpha * x.data[(bi * n + hi) * c + k];
            } else {
                const size_t start = static_cast<size_t>(static_cast<std::uint64_t>(j)
                        * static_cast<std::uint64_t>(n) / static_cast<std::uint64_t>(m));
                const size_t end = static_cast<size_t>((static_cast<std::uint64_t>(j + 1)
                        * static_cast<std::uint64_t>(n) + static_cast<std::uint64_t>(m) - 1)
                        / static_cast<std::uint64_t>(m));
                for (size_t k = 0; k < c; k++) {
                    float sum = 0.0f;
                    for (size_t q = start; q < end; q++) sum += x.data[(bi * n + q) * c + k];
                    out[(bi * m + j) * c + k] = sum / static_cast<float>(end - start);
                }
            }
        }
        result = float_result(env, out, count);
    }
    std::free(out); release(env, &x); return result;
}

JNIEXPORT jfloatArray JNICALL JNI_NAME(phiNative)(
        JNIEnv *env, jclass, jfloatArray values, jint batch, jint length, jint channels,
        jfloatArray weights, jfloatArray bias, jfloat ratio) {
    Floats x = {nullptr, nullptr, 0}, w = {nullptr, nullptr, 0}, a = {nullptr, nullptr, 0};
    if (!acquire(env, values, &x)) return nullptr;
    if (!acquire(env, weights, &w)) { release(env, &x); return nullptr; }
    if (!acquire(env, bias, &a)) { release(env, &w); release(env, &x); return nullptr; }
    if (!shape3(batch, length, channels, x.size)
            || static_cast<double>(channels) * static_cast<double>(channels) * 3.0 != static_cast<double>(w.size)
            || a.size != channels || !std::isfinite(ratio) || ratio < 0.0f || ratio > 1.0f
            || !finite_input(&x) || !finite_input(&w) || !finite_input(&a)) {
        invalid(env); release(env, &a); release(env, &w); release(env, &x); return nullptr;
    }
    const size_t b = static_cast<size_t>(batch), n = static_cast<size_t>(length);
    const size_t c = static_cast<size_t>(channels), count = static_cast<size_t>(x.size);
    float *out = static_cast<float *>(allocate(env, count, sizeof(float)));
    jfloatArray result = nullptr;
    if (out != nullptr) {
        for (size_t bi = 0; bi < b; bi++) for (size_t pos = 0; pos < n; pos++)
            for (size_t oc = 0; oc < c; oc++) {
                float sum = 0.0f;
                for (size_t ic = 0; ic < c; ic++) for (size_t tap = 0; tap < 3; tap++) {
                    if ((pos == 0 && tap == 0) || (pos + 1 == n && tap == 2)) continue;
                    const size_t at = pos + tap - 1;
                    sum += x.data[(bi * n + at) * c + ic] * w.data[(oc * c + ic) * 3 + tap];
                }
                const float convolution = sum + a.data[oc];
                out[(bi * n + pos) * c + oc] = x.data[(bi * n + pos) * c + oc] * (1.0f - ratio)
                        + convolution * ratio;
            }
        result = float_result(env, out, count);
    }
    std::free(out); release(env, &a); release(env, &w); release(env, &x); return result;
}

JNIEXPORT jfloatArray JNICALL JNI_NAME(tiedLogitsNative)(
        JNIEnv *env, jclass, jfloatArray rows, jint channels, jfloatArray codebook,
        jfloat log_inverse_temperature, jfloatArray bias) {
    Floats x = {nullptr, nullptr, 0}, e = {nullptr, nullptr, 0}, a = {nullptr, nullptr, 0};
    if (!acquire(env, rows, &x)) return nullptr;
    if (!acquire(env, codebook, &e)) { release(env, &x); return nullptr; }
    if (bias != nullptr && !acquire(env, bias, &a)) { release(env, &e); release(env, &x); return nullptr; }
    if (channels <= 0 || x.size <= 0 || x.size % channels != 0 || e.size <= 0 || e.size % channels != 0
            || (bias != nullptr && a.size != e.size / channels) || !std::isfinite(log_inverse_temperature)
            || !finite_input(&x) || !finite_input(&e) || !finite_input(&a)) {
        invalid(env); release(env, &a); release(env, &e); release(env, &x); return nullptr;
    }
    const size_t c = static_cast<size_t>(channels), r = static_cast<size_t>(x.size) / c;
    const size_t v = static_cast<size_t>(e.size) / c;
    const std::uint64_t count = static_cast<std::uint64_t>(r) * static_cast<std::uint64_t>(v);
    if (count + static_cast<std::uint64_t>(v) > MAX_ELEMENTS) {
        invalid(env); release(env, &a); release(env, &e); release(env, &x); return nullptr;
    }
    float *norms = static_cast<float *>(allocate(env, v, sizeof(float)));
    float *out = norms == nullptr ? nullptr : static_cast<float *>(allocate(env, count, sizeof(float)));
    jfloatArray result = nullptr;
    if (out != nullptr) {
        float norm_sum = 0.0f;
        for (size_t j = 0; j < v; j++) {
            float sum = 0.0f;
            for (size_t k = 0; k < c; k++) sum += e.data[j * c + k] * e.data[j * c + k];
            norms[j] = sum; norm_sum += sum;
        }
        float denominator = norm_sum / static_cast<float>(v);
        if (denominator < 1.0e-12f) denominator = 1.0e-12f;
        const float inverse = static_cast<float>(std::exp(static_cast<double>(log_inverse_temperature)));
        if (!std::isfinite(norm_sum) || !std::isfinite(inverse)) invalid(env);
        else {
            for (size_t i = 0; i < r; i++) for (size_t j = 0; j < v; j++) {
                float dot = 0.0f;
                for (size_t k = 0; k < c; k++) dot += x.data[i * c + k] * e.data[j * c + k];
                float score = ((2.0f * dot - norms[j]) / denominator) * inverse;
                if (bias != nullptr) score += a.data[j];
                out[i * v + j] = score;
            }
            result = float_result(env, out, static_cast<size_t>(count));
        }
    }
    std::free(out); std::free(norms); release(env, &a); release(env, &e); release(env, &x); return result;
}

}
