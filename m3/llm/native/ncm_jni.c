/* SPDX-FileCopyrightText: 2025 hermann kumbong
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: MIT AND Apache-2.0
 * Numerical translation of HazyResearch/ncm models/quant.py and models/next_concept.py
 * at c70274273f489abc73832b2b98584a8483b6b203. See NCM-MIT-LICENSE.txt.
 * Flat FP32 CPU batches; no retained JNI references or native allocations.
 */
#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>

#ifdef __cplusplus
#define JCALL(name, ...) env->name(__VA_ARGS__)
#else
#define JCALL(name, ...) (*env)->name(env, __VA_ARGS__)
#endif
#define JNI_NAME(name) Java_jdk_m3_llm_projection_M3NcmNativeOps_##name
#define MAX_ELEMENTS UINT64_C(16777216)

typedef struct { jfloatArray array; jfloat *data; jsize size; } Floats;

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass exception = JCALL(FindClass, type);
    if (exception != NULL) { (void) JCALL(ThrowNew, exception, message); JCALL(DeleteLocalRef, exception); }
}
static void invalid(JNIEnv *env) {
    fail(env,"java/lang/IllegalArgumentException","invalid NCM native shape, value, budget or overflow");
}
static int acquire(JNIEnv *env,jfloatArray array,Floats *out) {
    if (array == NULL) { fail(env,"java/lang/NullPointerException","native input"); return 0; }
    out->array = array;
    out->size = JCALL(GetArrayLength,array);
    out->data = JCALL(GetFloatArrayElements,array,NULL);
    return out->data != NULL;
}
static void release(JNIEnv *env,Floats *input) {
    if (input->data != NULL) JCALL(ReleaseFloatArrayElements,input->array,input->data,JNI_ABORT);
    input->data = NULL;
}
static int finite_input(const Floats *input) {
    for (jsize i=0;i<input->size;i++) if (!isfinite(input->data[i])) return 0;
    return 1;
}
static void *allocate(JNIEnv *env,uint64_t count,size_t width) {
    if (count > MAX_ELEMENTS || count > SIZE_MAX/width) { invalid(env); return NULL; }
    void *memory = calloc((size_t)(count == 0 ? 1 : count),width);
    if (memory == NULL) fail(env,"java/lang/OutOfMemoryError","NCM native batch");
    return memory;
}
static jfloatArray float_result(JNIEnv *env,const float *data,size_t size) {
    for (size_t i=0;i<size;i++) if (!isfinite(data[i])) { invalid(env); return NULL; }
    jfloatArray result = JCALL(NewFloatArray,(jsize)size);
    if (result != NULL && size != 0) JCALL(SetFloatArrayRegion,result,0,(jsize)size,data);
    return result;
}
static int shape3(jint batch,jint length,jint channels,jsize actual) {
    if (batch <= 0 || length <= 0 || channels <= 0) return 0;
    return (double)batch*(double)length*(double)channels == (double)actual;
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL JNI_NAME(abiVersionNative)(JNIEnv *env,jclass type) {
    (void)env; (void)type; return 1;
}

JNIEXPORT jintArray JNICALL JNI_NAME(nearestNative)(
        JNIEnv *env,jclass type,jfloatArray rows,jint channels,jfloatArray codebook) {
    (void)type;
    Floats x = {NULL,NULL,0}, e = {NULL,NULL,0};
    jintArray result = NULL;
    float *norms = NULL;
    jint *ids = NULL;
    int valid = 1;
    if (!acquire(env,rows,&x)) return NULL;
    if (!acquire(env,codebook,&e)) { release(env,&x); return NULL; }
    if (channels <= 0 || x.size <= 0 || x.size%channels != 0 || e.size <= 0 || e.size%channels != 0
            || !finite_input(&x) || !finite_input(&e)) {
        invalid(env); release(env,&e); release(env,&x); return NULL;
    }
    size_t c = (size_t)channels, r = (size_t)x.size/c, v = (size_t)e.size/c;
    if ((uint64_t)r+(uint64_t)v > MAX_ELEMENTS) {
        invalid(env); release(env,&e); release(env,&x); return NULL;
    }
    norms = (float *)allocate(env,(uint64_t)v,sizeof(float));
    if (norms != NULL) ids = (jint *)allocate(env,(uint64_t)r,sizeof(jint));
    if (norms != NULL && ids != NULL) {
        for (size_t j=0;j<v;j++) {
            float sum = 0.0f;
            for (size_t k=0;k<c;k++) sum += e.data[j*c+k]*e.data[j*c+k];
            norms[j] = sum;
            if (!isfinite(sum)) valid = 0;
        }
        for (size_t i=0;valid && i<r;i++) {
            float row_norm = 0.0f, best = INFINITY;
            for (size_t k=0;k<c;k++) row_norm += x.data[i*c+k]*x.data[i*c+k];
            if (!isfinite(row_norm)) { valid = 0; break; }
            for (size_t j=0;j<v;j++) {
                float dot = 0.0f;
                for (size_t k=0;k<c;k++) dot += x.data[i*c+k]*e.data[j*c+k];
                float distance = (row_norm+norms[j])-2.0f*dot;
                if (!isfinite(distance)) { valid = 0; break; }
                if (distance < best) { best=distance; ids[i]=(jint)j; }
            }
        }
        if (!valid) invalid(env);
        else {
            result = JCALL(NewIntArray,(jsize)r);
            if (result != NULL && r != 0) JCALL(SetIntArrayRegion,result,0,(jsize)r,ids);
        }
    }
    free(ids); free(norms); release(env,&e); release(env,&x); return result;
}

JNIEXPORT jfloatArray JNICALL JNI_NAME(resizeNative)(
        JNIEnv *env,jclass type,jfloatArray values,jint batch,jint from_length,
        jint channels,jint to_length,jboolean linear) {
    (void)type;
    Floats x = {NULL,NULL,0};
    if (!acquire(env,values,&x)) return NULL;
    double output_count = (double)batch*(double)to_length*(double)channels;
    if (!shape3(batch,from_length,channels,x.size) || to_length <= 0
            || output_count > (double)MAX_ELEMENTS || !finite_input(&x)) {
        invalid(env); release(env,&x); return NULL;
    }
    size_t b=(size_t)batch,n=(size_t)from_length,c=(size_t)channels,m=(size_t)to_length;
    size_t count=b*m*c;
    float *out=(float *)allocate(env,(uint64_t)count,sizeof(float));
    jfloatArray result=NULL;
    if (out != NULL) {
        for (size_t bi=0;bi<b;bi++) for (size_t j=0;j<m;j++) {
            if (linear == JNI_TRUE) {
                float scale=(float)n/(float)m;
                float source=scale*((float)j+0.5f)-0.5f;
                if (source < 0.0f) source=0.0f;
                size_t lo=(size_t)source; if (lo >= n) lo=n-1;
                size_t hi=lo+1; if (hi >= n) hi=n-1;
                float alpha=source-(float)lo;
                if (alpha < 0.0f) alpha=0.0f;
                if (alpha > 1.0f) alpha=1.0f;
                for (size_t k=0;k<c;k++)
                    out[(bi*m+j)*c+k]=(1.0f-alpha)*x.data[(bi*n+lo)*c+k]
                                            +alpha*x.data[(bi*n+hi)*c+k];
            } else {
                size_t start=(size_t)((uint64_t)j*(uint64_t)n/(uint64_t)m);
                size_t end=(size_t)(((uint64_t)(j+1)*(uint64_t)n+(uint64_t)m-1)/(uint64_t)m);
                for (size_t k=0;k<c;k++) {
                    float sum=0.0f;
                    for (size_t q=start;q<end;q++) sum+=x.data[(bi*n+q)*c+k];
                    out[(bi*m+j)*c+k]=sum/(float)(end-start);
                }
            }
        }
        result=float_result(env,out,count);
    }
    free(out); release(env,&x); return result;
}

JNIEXPORT jfloatArray JNICALL JNI_NAME(phiNative)(
        JNIEnv *env,jclass type,jfloatArray values,jint batch,jint length,jint channels,
        jfloatArray weights,jfloatArray bias,jfloat ratio) {
    (void)type;
    Floats x={NULL,NULL,0},w={NULL,NULL,0},a={NULL,NULL,0};
    if (!acquire(env,values,&x)) return NULL;
    if (!acquire(env,weights,&w)) { release(env,&x); return NULL; }
    if (!acquire(env,bias,&a)) { release(env,&w); release(env,&x); return NULL; }
    if (!shape3(batch,length,channels,x.size) || (double)channels*(double)channels*3.0 != w.size
            || a.size != channels || !isfinite(ratio) || ratio < 0.0f || ratio > 1.0f
            || !finite_input(&x) || !finite_input(&w) || !finite_input(&a)) {
        invalid(env); release(env,&a); release(env,&w); release(env,&x); return NULL;
    }
    size_t b=(size_t)batch,n=(size_t)length,c=(size_t)channels,count=(size_t)x.size;
    float *out=(float *)allocate(env,(uint64_t)count,sizeof(float));
    jfloatArray result=NULL;
    if (out != NULL) {
        for (size_t bi=0;bi<b;bi++) for (size_t pos=0;pos<n;pos++)
            for (size_t oc=0;oc<c;oc++) {
                float sum=0.0f;
                for (size_t ic=0;ic<c;ic++) for (size_t tap=0;tap<3;tap++) {
                    if ((pos == 0 && tap == 0) || (pos+1 == n && tap == 2)) continue;
                    size_t at=pos+tap-1;
                    sum+=x.data[(bi*n+at)*c+ic]*w.data[(oc*c+ic)*3+tap];
                }
                float convolution=sum+a.data[oc];
                out[(bi*n+pos)*c+oc]=x.data[(bi*n+pos)*c+oc]*(1.0f-ratio)+convolution*ratio;
            }
        result=float_result(env,out,count);
    }
    free(out); release(env,&a); release(env,&w); release(env,&x); return result;
}

JNIEXPORT jfloatArray JNICALL JNI_NAME(tiedLogitsNative)(
        JNIEnv *env,jclass type,jfloatArray rows,jint channels,jfloatArray codebook,
        jfloat log_inverse_temperature,jfloatArray bias) {
    (void)type;
    Floats x={NULL,NULL,0},e={NULL,NULL,0},a={NULL,NULL,0};
    if (!acquire(env,rows,&x)) return NULL;
    if (!acquire(env,codebook,&e)) { release(env,&x); return NULL; }
    if (bias != NULL && !acquire(env,bias,&a)) { release(env,&e); release(env,&x); return NULL; }
    if (channels <= 0 || x.size <= 0 || x.size%channels != 0 || e.size <= 0 || e.size%channels != 0
            || (bias != NULL && a.size != e.size/channels) || !isfinite(log_inverse_temperature)
            || !finite_input(&x) || !finite_input(&e) || !finite_input(&a)) {
        invalid(env); release(env,&a); release(env,&e); release(env,&x); return NULL;
    }
    size_t c=(size_t)channels,r=(size_t)x.size/c,v=(size_t)e.size/c;
    uint64_t count=(uint64_t)r*(uint64_t)v;
    if (count+(uint64_t)v > MAX_ELEMENTS) {
        invalid(env); release(env,&a); release(env,&e); release(env,&x); return NULL;
    }
    float *norms=(float *)allocate(env,(uint64_t)v,sizeof(float));
    float *out=norms == NULL ? NULL : (float *)allocate(env,count,sizeof(float));
    jfloatArray result=NULL;
    if (out != NULL) {
        float norm_sum=0.0f;
        for (size_t j=0;j<v;j++) {
            float sum=0.0f;
            for (size_t k=0;k<c;k++) sum+=e.data[j*c+k]*e.data[j*c+k];
            norms[j]=sum; norm_sum+=sum;
        }
        float denominator=norm_sum/(float)v;
        if (denominator < 1.0e-12f) denominator=1.0e-12f;
        float inverse=(float)exp((double)log_inverse_temperature);
        if (!isfinite(norm_sum) || !isfinite(inverse)) invalid(env);
        else {
            for (size_t i=0;i<r;i++) for (size_t j=0;j<v;j++) {
                float dot=0.0f;
                for (size_t k=0;k<c;k++) dot+=x.data[i*c+k]*e.data[j*c+k];
                float score=((2.0f*dot-norms[j])/denominator)*inverse;
                if (bias != NULL) score+=a.data[j];
                out[i*v+j]=score;
            }
            result=float_result(env,out,(size_t)count);
        }
    }
    free(out); free(norms); release(env,&a); release(env,&e); release(env,&x); return result;
}
#ifdef __cplusplus
}
#endif
