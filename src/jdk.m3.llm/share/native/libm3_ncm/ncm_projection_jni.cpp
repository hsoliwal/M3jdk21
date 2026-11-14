/*
 * SPDX-FileCopyrightText: 2025 hermann kumbong
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: MIT AND Apache-2.0
 *
 * JNI primitives for jdk.m3.llm.projection.M3NcmNativeOps in the single jdk.m3.llm
 * NCM native owner (library m3_ncm). Resize, Phi and tied-logit batches translate
 * HazyResearch/ncm models/quant.py and models/next_concept.py at
 * c70274273f489abc73832b2b98584a8483b6b203; see NCM_DONOR_NOTICE.md for the MIT
 * notice. Nearest-code selection calls the shared FP32 expanded-norm entry in
 * ncm_kernel.cpp; the F64 and offset-codebook numerical contracts are unchanged.
 * Flat FP32 CPU batches; no retained JNI references or native allocations.
 * ABI 1 retains its default allocation ceiling. Controlled capability 1 supplies
 * a per-call ceiling and an optional checkpoint() callback on the calling thread.
 */
#include <jni.h>
#include "m3_ncm_kernel.h"
#include <math.h>
#include <stdint.h>
#include <stdlib.h>

#define JCALL(name, ...) env->name(__VA_ARGS__)
#define JNI_NAME(name) Java_jdk_m3_llm_projection_M3NcmNativeOps_##name
#define MAX_ELEMENTS UINT64_C(16777216)
#define MAX_ARRAY_ELEMENTS UINT64_C(2147483639)

typedef struct { jfloatArray array; jfloat *data; jsize size; } Floats;
typedef struct {
    uint64_t max_elements;
    jobject cancellation;
    jmethodID checkpoint;
} Control;

static int pending(JNIEnv *env) {
    return env->ExceptionCheck() != JNI_FALSE;
}
static void fail(JNIEnv *env, const char *type, const char *message) {
    /* In particular, never replace the exception thrown by a Java checkpoint. */
    if (pending(env)) return;
    jclass exception = JCALL(FindClass, type);
    if (exception != NULL) {
        (void) JCALL(ThrowNew, exception, message);
        JCALL(DeleteLocalRef, exception);
    }
}
static void invalid(JNIEnv *env) {
    fail(env,"java/lang/IllegalArgumentException","invalid NCM native shape, value, budget or overflow");
}
static int control_init(JNIEnv *env,jlong max_elements,jobject cancellation,Control *out) {
    if (max_elements <= 0) { invalid(env); return 0; }
    out->max_elements = (uint64_t)max_elements;
    out->cancellation = cancellation;
    out->checkpoint = NULL;
    if (cancellation == NULL) return 1;
    jclass type = JCALL(GetObjectClass,cancellation);
    if (type == NULL || pending(env)) return 0;
    out->checkpoint = JCALL(GetMethodID,type,"checkpoint","()V");
    int valid = !pending(env) && out->checkpoint != NULL;
    JCALL(DeleteLocalRef,type);
    return valid;
}
static int checkpoint(JNIEnv *env,const Control *control) {
    if (control->cancellation == NULL) return 1;
    JCALL(CallVoidMethod,control->cancellation,control->checkpoint);
    /* No JNI call may intervene before checking a callback exception. */
    return !pending(env);
}
static int acquire(JNIEnv *env,jfloatArray array,Floats *out) {
    if (array == NULL) { fail(env,"java/lang/NullPointerException","native input"); return 0; }
    out->array = array;
    out->size = JCALL(GetArrayLength,array);
    if (pending(env)) return 0;
    out->data = JCALL(GetFloatArrayElements,array,NULL);
    if (out->data == NULL) {
        fail(env,"java/lang/OutOfMemoryError","NCM native input");
        return 0;
    }
    return 1;
}
static void release(JNIEnv *env,Floats *input) {
    if (input->data != NULL) JCALL(ReleaseFloatArrayElements,input->array,input->data,JNI_ABORT);
    input->data = NULL;
}
static int finite_data(JNIEnv *env,const Control *control,const float *data,size_t size) {
    if (!checkpoint(env,control)) return 0;
    for (size_t i=0;i<size;i++) {
        if ((i & (size_t)65535) == (size_t)65535 && !checkpoint(env,control)) return 0;
        if (!isfinite(data[i])) { invalid(env); return 0; }
    }
    return 1;
}
static int finite_input(JNIEnv *env,const Control *control,const Floats *input) {
    return finite_data(env,control,input->data,(size_t)input->size);
}
static int admits(JNIEnv *env,const Control *control,uint64_t count) {
    if (count > control->max_elements) { invalid(env); return 0; }
    return 1;
}
static void *allocate(JNIEnv *env,const Control *control,uint64_t count,size_t width) {
    if (count > MAX_ARRAY_ELEMENTS || count > control->max_elements || count > SIZE_MAX/width) {
        invalid(env); return NULL;
    }
    void *memory = calloc((size_t)(count == 0 ? 1 : count),width);
    if (memory == NULL) fail(env,"java/lang/OutOfMemoryError","NCM native batch");
    return memory;
}
static jfloatArray float_result(JNIEnv *env,const Control *control,const float *data,size_t size) {
    if (!finite_data(env,control,data,size) || !checkpoint(env,control)) return NULL;
    jfloatArray result = JCALL(NewFloatArray,(jsize)size);
    if (result != NULL && size != 0) JCALL(SetFloatArrayRegion,result,0,(jsize)size,data);
    if (pending(env)) {
        if (result != NULL) JCALL(DeleteLocalRef,result);
        return NULL;
    }
    return result;
}
static jintArray int_result(JNIEnv *env,const Control *control,const jint *data,size_t size) {
    if (!checkpoint(env,control)) return NULL;
    jintArray result = JCALL(NewIntArray,(jsize)size);
    if (result != NULL && size != 0) JCALL(SetIntArrayRegion,result,0,(jsize)size,data);
    if (pending(env)) {
        if (result != NULL) JCALL(DeleteLocalRef,result);
        return NULL;
    }
    return result;
}
static int shape3(jint batch,jint length,jint channels,jsize actual) {
    if (batch <= 0 || length <= 0 || channels <= 0 || actual <= 0) return 0;
    /* Two positive jint factors fit uint64_t; divide before the third product. */
    return actual%channels == 0
            && (uint64_t)batch*(uint64_t)length == (uint64_t)(actual/channels);
}

typedef struct {
    JNIEnv *env;
    const Control *control;
} KernelControl;

static int kernel_checkpoint(void *context) {
    const KernelControl *call = (const KernelControl *)context;
    return checkpoint(call->env, call->control) ? 0 : 1;
}

static jintArray nearest(JNIEnv *env,jfloatArray rows,jint channels,jfloatArray codebook,
        const Control *control) {
    Floats x = {NULL,NULL,0}, e = {NULL,NULL,0};
    if (!acquire(env,rows,&x)) return NULL;
    if (!acquire(env,codebook,&e)) { release(env,&x); return NULL; }
    if (channels <= 0 || x.size <= 0 || x.size%channels != 0 || e.size <= 0 || e.size%channels != 0) {
        invalid(env); release(env,&e); release(env,&x); return NULL;
    }
    size_t c=(size_t)channels,r=(size_t)x.size/c,v=(size_t)e.size/c;
    if (!admits(env,control,(uint64_t)r+(uint64_t)v)) {
        release(env,&e); release(env,&x); return NULL;
    }
    int *selected=(int *)allocate(env,control,(uint64_t)r,sizeof(int));
    float *norms=selected == NULL ? NULL : (float *)allocate(env,control,(uint64_t)v,sizeof(float));
    jint *transfer=norms == NULL ? NULL : (jint *)allocate(env,control,(uint64_t)r,sizeof(jint));
    jintArray result=NULL;
    if (transfer != NULL) {
        KernelControl call={env,control};
        /* Both original and controlled JNI use the one current FP32 numerical owner. */
        int status=m3_ncm_nearest_expanded_f32_controlled(x.data,(size_t)x.size,r,c,
                e.data,(size_t)e.size,v,norms,v,selected,r,
                control->cancellation == NULL ? NULL : kernel_checkpoint,&call);
        if (status != 0) invalid(env); /* fail() preserves a pending callback exception. */
        else {
            int valid=1;
            for (size_t i=0;i<r;i++) {
                if ((i & (size_t)65535) == (size_t)65535 && !checkpoint(env,control)) {
                    valid=0; break;
                }
                transfer[i]=(jint)selected[i];
            }
            if (valid) result=int_result(env,control,transfer,r);
        }
    }
    free(transfer); free(norms); free(selected); release(env,&e); release(env,&x); return result;
}

static jfloatArray resize(JNIEnv *env,jfloatArray values,jint batch,jint from_length,
        jint channels,jint to_length,jboolean linear,const Control *control) {
    Floats x = {NULL,NULL,0};
    if (!acquire(env,values,&x)) return NULL;
    if (!shape3(batch,from_length,channels,x.size) || to_length <= 0
            || !finite_input(env,control,&x)) {
        invalid(env); release(env,&x); return NULL;
    }
    size_t b=(size_t)batch,n=(size_t)from_length,c=(size_t)channels,m=(size_t)to_length;
    /* Input shape bounds batch*channels to a Java array length. */
    uint64_t output_count=(uint64_t)b*(uint64_t)c*(uint64_t)m;
    float *out=(float *)allocate(env,control,output_count,sizeof(float));
    jfloatArray result=NULL;
    int valid=1;
    if (out != NULL) {
        size_t count=(size_t)output_count;
        for (size_t bi=0;bi<b;bi++) {
            if (!checkpoint(env,control)) { valid=0; break; }
            for (size_t j=0;j<m;j++) {
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
        }
        if (valid) result=float_result(env,control,out,count);
    }
    free(out); release(env,&x); return result;
}

static jfloatArray phi(JNIEnv *env,jfloatArray values,jint batch,jint length,jint channels,
        jfloatArray weights,jfloatArray bias,jfloat ratio,const Control *control) {
    Floats x={NULL,NULL,0},w={NULL,NULL,0},a={NULL,NULL,0};
    if (!acquire(env,values,&x)) return NULL;
    if (!acquire(env,weights,&w)) { release(env,&x); return NULL; }
    if (!acquire(env,bias,&a)) { release(env,&w); release(env,&x); return NULL; }
    if (!shape3(batch,length,channels,x.size)
            || (uint64_t)channels*(uint64_t)channels*UINT64_C(3) != (uint64_t)w.size
            || a.size != channels || !isfinite(ratio) || ratio < 0.0f || ratio > 1.0f
            || !finite_input(env,control,&x) || !finite_input(env,control,&w)
            || !finite_input(env,control,&a)) {
        invalid(env); release(env,&a); release(env,&w); release(env,&x); return NULL;
    }
    size_t b=(size_t)batch,n=(size_t)length,c=(size_t)channels,count=(size_t)x.size;
    float *out=(float *)allocate(env,control,(uint64_t)count,sizeof(float));
    jfloatArray result=NULL;
    int valid=1;
    if (out != NULL) {
        for (size_t bi=0;bi<b;bi++) {
            if (!checkpoint(env,control)) { valid=0; break; }
            for (size_t pos=0;pos<n;pos++) for (size_t oc=0;oc<c;oc++) {
                float sum=0.0f;
                for (size_t ic=0;ic<c;ic++) for (size_t tap=0;tap<3;tap++) {
                    if ((pos == 0 && tap == 0) || (pos+1 == n && tap == 2)) continue;
                    size_t at=pos+tap-1;
                    sum+=x.data[(bi*n+at)*c+ic]*w.data[(oc*c+ic)*3+tap];
                }
                float convolution=sum+a.data[oc];
                out[(bi*n+pos)*c+oc]=x.data[(bi*n+pos)*c+oc]*(1.0f-ratio)+convolution*ratio;
            }
        }
        if (valid) result=float_result(env,control,out,count);
    }
    free(out); release(env,&a); release(env,&w); release(env,&x); return result;
}

static jfloatArray tied_logits(JNIEnv *env,jfloatArray rows,jint channels,jfloatArray codebook,
        jfloat log_inverse_temperature,jfloatArray bias,const Control *control) {
    Floats x={NULL,NULL,0},e={NULL,NULL,0},a={NULL,NULL,0};
    if (!acquire(env,rows,&x)) return NULL;
    if (!acquire(env,codebook,&e)) { release(env,&x); return NULL; }
    if (bias != NULL && !acquire(env,bias,&a)) { release(env,&e); release(env,&x); return NULL; }
    if (channels <= 0 || x.size <= 0 || x.size%channels != 0 || e.size <= 0 || e.size%channels != 0
            || (bias != NULL && a.size != e.size/channels) || !isfinite(log_inverse_temperature)
            || !finite_input(env,control,&x) || !finite_input(env,control,&e)
            || (bias != NULL && !finite_input(env,control,&a))) {
        invalid(env); release(env,&a); release(env,&e); release(env,&x); return NULL;
    }
    size_t c=(size_t)channels,r=(size_t)x.size/c,v=(size_t)e.size/c;
    uint64_t count=(uint64_t)r*(uint64_t)v;
    if (!admits(env,control,count+(uint64_t)v)) {
        release(env,&a); release(env,&e); release(env,&x); return NULL;
    }
    float *norms=(float *)allocate(env,control,(uint64_t)v,sizeof(float));
    float *out=norms == NULL ? NULL : (float *)allocate(env,control,count,sizeof(float));
    jfloatArray result=NULL;
    int valid=1;
    if (out != NULL) {
        float norm_sum=0.0f;
        for (size_t j=0;j<v;j++) {
            if (!checkpoint(env,control)) { valid=0; break; }
            float sum=0.0f;
            for (size_t k=0;k<c;k++) sum+=e.data[j*c+k]*e.data[j*c+k];
            norms[j]=sum; norm_sum+=sum;
        }
        if (valid) {
            float denominator=norm_sum/(float)v;
            if (denominator < 1.0e-12f) denominator=1.0e-12f;
            float inverse=(float)exp((double)log_inverse_temperature);
            if (!isfinite(norm_sum) || !isfinite(inverse)) invalid(env);
            else {
                for (size_t i=0;i<r;i++) {
                    if (!checkpoint(env,control)) { valid=0; break; }
                    for (size_t j=0;j<v;j++) {
                        float dot=0.0f;
                        for (size_t k=0;k<c;k++) dot+=x.data[i*c+k]*e.data[j*c+k];
                        float score=((2.0f*dot-norms[j])/denominator)*inverse;
                        if (bias != NULL) score+=a.data[j];
                        out[i*v+j]=score;
                    }
                }
                if (valid) result=float_result(env,control,out,(size_t)count);
            }
        }
    }
    free(out); free(norms); release(env,&a); release(env,&e); release(env,&x); return result;
}

extern "C" {

JNIEXPORT jint JNICALL JNI_NAME(abiVersionNative)(JNIEnv *env,jclass type) {
    (void)env; (void)type; return 1;
}
JNIEXPORT jint JNICALL JNI_NAME(controlledAbiVersionNative)(JNIEnv *env,jclass type) {
    (void)env; (void)type; return 1;
}
JNIEXPORT jintArray JNICALL JNI_NAME(nearestNative)(
        JNIEnv *env,jclass type,jfloatArray rows,jint channels,jfloatArray codebook) {
    (void)type;
    const Control control={MAX_ELEMENTS,NULL,NULL};
    return nearest(env,rows,channels,codebook,&control);
}
JNIEXPORT jintArray JNICALL JNI_NAME(nearestControlledNative)(
        JNIEnv *env,jclass type,jfloatArray rows,jint channels,jfloatArray codebook,
        jlong max_elements,jobject cancellation) {
    (void)type;
    Control control;
    if (!control_init(env,max_elements,cancellation,&control)) return NULL;
    return nearest(env,rows,channels,codebook,&control);
}
JNIEXPORT jfloatArray JNICALL JNI_NAME(resizeNative)(
        JNIEnv *env,jclass type,jfloatArray values,jint batch,jint from_length,
        jint channels,jint to_length,jboolean linear) {
    (void)type;
    const Control control={MAX_ELEMENTS,NULL,NULL};
    return resize(env,values,batch,from_length,channels,to_length,linear,&control);
}
JNIEXPORT jfloatArray JNICALL JNI_NAME(resizeControlledNative)(
        JNIEnv *env,jclass type,jfloatArray values,jint batch,jint from_length,
        jint channels,jint to_length,jboolean linear,jlong max_elements,jobject cancellation) {
    (void)type;
    Control control;
    if (!control_init(env,max_elements,cancellation,&control)) return NULL;
    return resize(env,values,batch,from_length,channels,to_length,linear,&control);
}
JNIEXPORT jfloatArray JNICALL JNI_NAME(phiNative)(
        JNIEnv *env,jclass type,jfloatArray values,jint batch,jint length,jint channels,
        jfloatArray weights,jfloatArray bias,jfloat ratio) {
    (void)type;
    const Control control={MAX_ELEMENTS,NULL,NULL};
    return phi(env,values,batch,length,channels,weights,bias,ratio,&control);
}
JNIEXPORT jfloatArray JNICALL JNI_NAME(phiControlledNative)(
        JNIEnv *env,jclass type,jfloatArray values,jint batch,jint length,jint channels,
        jfloatArray weights,jfloatArray bias,jfloat ratio,jlong max_elements,jobject cancellation) {
    (void)type;
    Control control;
    if (!control_init(env,max_elements,cancellation,&control)) return NULL;
    return phi(env,values,batch,length,channels,weights,bias,ratio,&control);
}
JNIEXPORT jfloatArray JNICALL JNI_NAME(tiedLogitsNative)(
        JNIEnv *env,jclass type,jfloatArray rows,jint channels,jfloatArray codebook,
        jfloat log_inverse_temperature,jfloatArray bias) {
    (void)type;
    const Control control={MAX_ELEMENTS,NULL,NULL};
    return tied_logits(env,rows,channels,codebook,log_inverse_temperature,bias,&control);
}
JNIEXPORT jfloatArray JNICALL JNI_NAME(tiedLogitsControlledNative)(
        JNIEnv *env,jclass type,jfloatArray rows,jint channels,jfloatArray codebook,
        jfloat log_inverse_temperature,jfloatArray bias,jlong max_elements,jobject cancellation) {
    (void)type;
    Control control;
    if (!control_init(env,max_elements,cancellation,&control)) return NULL;
    return tied_logits(env,rows,channels,codebook,log_inverse_temperature,bias,&control);
}

}
