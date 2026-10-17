// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
_Static_assert(sizeof(jlong)==sizeof(uint64_t),"JNI long width");
static void fail(JNIEnv *e,const char *name,const char *message) {
    jclass c=(*e)->FindClass(e,name);
    if(c!=NULL) {
        (*e)->ThrowNew(e,c,message);
        (*e)->DeleteLocalRef(e,c);
    }
}
static int checkpoint(JNIEnv *e,jclass type,jmethodID method,jobject monitor,jint work) {
    (*e)->CallStaticVoidMethod(e,type,method,monitor,work);
    return !(*e)->ExceptionCheck(e);
}
static jlongArray sum_common(JNIEnv *e,jclass type,jintArray keys,jlongArray values,jint groups,jobject monitor) {
    if(keys==NULL||values==NULL) {
        fail(e,"java/lang/NullPointerException","null native array");
        return NULL;
    }
    jsize length=(*e)->GetArrayLength(e,keys);
    if(length!=(*e)->GetArrayLength(e,values)||groups<0||groups>1048576) {
        fail(e,"java/lang/IllegalArgumentException","length/group bound");
        return NULL;
    }
    jmethodID check=(*e)->GetStaticMethodID(e,type,"checkpoint","(Lcom/m3/collections/M3ProgressMonitor;I)V");
    if(check==NULL||!checkpoint(e,type,check,monitor,0))return NULL;
    uint64_t *sums=calloc(groups==0?1U:(size_t)groups,sizeof(uint64_t));
    if(sums==NULL) {
        fail(e,"java/lang/OutOfMemoryError","aggregate allocation");
        return NULL;
    }
    jint k[1024];
    jlong v[1024];
    for(jsize start=0;start<length;) {
        if(!checkpoint(e,type,check,monitor,0)) {
            free(sums);
            return NULL;
        }
        jsize n=length-start>1024?1024:length-start;
        (*e)->GetIntArrayRegion(e,keys,start,n,k);
        if((*e)->ExceptionCheck(e)) {
            free(sums);
            return NULL;
        }
        (*e)->GetLongArrayRegion(e,values,start,n,v);
        if((*e)->ExceptionCheck(e)) {
            free(sums);
            return NULL;
        }
        for(jsize j=0;j<n;j++) {
            if(k[j]<0||k[j]>=groups) {
                free(sums);
                fail(e,"java/lang/IllegalArgumentException","key outside group domain");
                return NULL;
            }
            uint64_t bits;
            memcpy(&bits,&v[j],sizeof(bits));
            sums[k[j]]+=bits;
        }
        if(!checkpoint(e,type,check,monitor,n)) {
            free(sums);
            return NULL;
        }
        start+=n;
    }
    jlongArray output=(*e)->NewLongArray(e,groups);
    if(output==NULL) {
        free(sums);
        return NULL;
    }
    for(jsize start=0;start<groups;) {
        if(!checkpoint(e,type,check,monitor,0)) {
            free(sums);
            return NULL;
        }
        jsize n=groups-start>1024?1024:groups-start;
        for(jsize j=0;j<n;j++)memcpy(&v[j],&sums[start+j],sizeof(jlong));
        (*e)->SetLongArrayRegion(e,output,start,n,v);
        if((*e)->ExceptionCheck(e)) {
            free(sums);
            return NULL;
        }
        start+=n;
    }
    free(sums);
    return checkpoint(e,type,check,monitor,0)?output:NULL;
}
JNIEXPORT jlongArray JNICALL Java_com_m3_collections_M3NativeLongs_sumNative(JNIEnv *e,jclass type,jintArray keys,jlongArray values,jint groups) {
    return sum_common(e,type,keys,values,groups,NULL);
}
JNIEXPORT jlongArray JNICALL Java_com_m3_collections_M3NativeLongs_sumNativeMonitored(JNIEnv *e,jclass type,jintArray keys,jlongArray values,jint groups,jobject monitor) {
    return sum_common(e,type,keys,values,groups,monitor);
}
JNIEXPORT jlongArray JNICALL Java_com_m3_collections_M3NativeLongs_scanNative(JNIEnv *e,jclass type,jlongArray values,jlong identity,jobject monitor) {
    if(values==NULL) {
        fail(e,"java/lang/NullPointerException","null native array");
        return NULL;
    }
    jmethodID check=(*e)->GetStaticMethodID(e,type,"checkpoint","(Lcom/m3/collections/M3ProgressMonitor;I)V");
    if(check==NULL||!checkpoint(e,type,check,monitor,0))return NULL;
    jsize length=(*e)->GetArrayLength(e,values);
    jlongArray output=(*e)->NewLongArray(e,length);
    if(output==NULL)return NULL;
    uint64_t total;
    memcpy(&total,&identity,sizeof(total));
    jlong v[1024];
    for(jsize start=0;start<length;) {
        if(!checkpoint(e,type,check,monitor,0))return NULL;
        jsize n=length-start>1024?1024:length-start;
        (*e)->GetLongArrayRegion(e,values,start,n,v);
        if((*e)->ExceptionCheck(e))return NULL;
        for(jsize j=0;j<n;j++) {
            uint64_t bits;
            memcpy(&bits,&v[j],sizeof(bits));
            total+=bits;
            memcpy(&v[j],&total,sizeof(jlong));
        }
        (*e)->SetLongArrayRegion(e,output,start,n,v);
        if((*e)->ExceptionCheck(e))return NULL;
        if(!checkpoint(e,type,check,monitor,n)) {
            return NULL;
        }
        start+=n;
    }
    return checkpoint(e,type,check,monitor,0)?output:NULL;
}

/* V3 append-only rolling sum kernel. All previous native definitions stay byte-exact. */
static int window_emit(JNIEnv *e, jlongArray output, jlong *buffer,
        jsize *used, jsize *written, uint64_t total) {
    memcpy(&buffer[*used], &total, sizeof(total));
    (*used)++;
    if (*used == 1024) {
        (*e)->SetLongArrayRegion(e, output, *written, *used, buffer);
        if ((*e)->ExceptionCheck(e)) return 0;
        *written += *used;
        *used = 0;
    }
    return 1;
}

JNIEXPORT jlongArray JNICALL Java_com_m3_collections_M3NativeLongs_windowsNative(
        JNIEnv *e, jclass type, jlongArray values, jint width, jint step,
        jboolean partial, jobject monitor) {
    if (values == NULL) {
        fail(e, "java/lang/NullPointerException", "null native array");
        return NULL;
    }
    if (width <= 0 || step <= 0) {
        fail(e, "java/lang/IllegalArgumentException", "positive width/step required");
        return NULL;
    }
    jmethodID check = (*e)->GetStaticMethodID(e, type, "checkpoint",
            "(Lcom/m3/collections/M3ProgressMonitor;I)V");
    if (check == NULL || !checkpoint(e, type, check, monitor, 0)) return NULL;
    jsize length = (*e)->GetArrayLength(e, values);
    jsize output_length = partial
            ? (length == 0 ? 0 : 1 + (length - 1) / step)
            : (length < width ? 0 : 1 + (length - width) / step);
    jlongArray output = (*e)->NewLongArray(e, output_length);
    if (output == NULL) return NULL;
    if (length == 0) return checkpoint(e, type, check, monitor, 0) ? output : NULL;
    size_t capacity = (size_t) (width < length ? width : length);
    if (capacity > SIZE_MAX / sizeof(uint64_t)) {
        fail(e, "java/lang/OutOfMemoryError", "window size overflow");
        return NULL;
    }
    uint64_t *ring = malloc(capacity * sizeof(uint64_t));
    if (ring == NULL) {
        fail(e, "java/lang/OutOfMemoryError", "window scratch allocation");
        return NULL;
    }
    size_t head = 0, count = 0;
    uint64_t total = 0;
    int64_t seen = 0, next = 0;
    jlong input[1024], buffer[1024];
    jsize used = 0, written = 0;
    for (jsize start = 0; start < length;) {
        if (!checkpoint(e, type, check, monitor, 0)) goto refused;
        jsize n = length - start > 1024 ? 1024 : length - start;
        (*e)->GetLongArrayRegion(e, values, start, n, input);
        if ((*e)->ExceptionCheck(e)) goto refused;
        for (jsize i = 0; i < n; i++) {
            if (count == capacity) {
                total -= ring[head];
                head = (head + 1) % capacity;
                count--;
            }
            uint64_t bits;
            memcpy(&bits, &input[i], sizeof(bits));
            ring[(head + count) % capacity] = bits;
            count++;
            total += bits;
            seen++;
            if (seen == next + width) {
                if (!window_emit(e, output, buffer, &used, &written, total)) goto refused;
                next += step;
            }
        }
        if (!checkpoint(e, type, check, monitor, n)) goto refused;
        start += n;
    }
    if (partial) {
        while (next < seen) {
            if (!checkpoint(e, type, check, monitor, 0)) goto refused;
            int64_t first = seen - (int64_t) count;
            size_t removed = 0;
            while (count > 0 && first < next) {
                if ((removed++ & 1023U) == 0 && !checkpoint(e, type, check, monitor, 0)) goto refused;
                total -= ring[head];
                head = (head + 1) % capacity;
                count--;
                first++;
            }
            if (!window_emit(e, output, buffer, &used, &written, total)) goto refused;
            next += step;
        }
    }
    if (used > 0) {
        (*e)->SetLongArrayRegion(e, output, written, used, buffer);
        if ((*e)->ExceptionCheck(e)) goto refused;
        written += used;
    }
    if (written != output_length) {
        fail(e, "java/lang/IllegalStateException", "window result count mismatch");
        goto refused;
    }
    free(ring);
    return checkpoint(e, type, check, monitor, 0) ? output : NULL;
refused:
    free(ring);
    return NULL;
}
