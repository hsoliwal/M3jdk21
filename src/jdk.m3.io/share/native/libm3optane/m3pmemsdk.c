/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/36827ee8a798ef6ce07f6fc55a2ac371b1382c06/m3-pmem-sdk/src/main/c/m3pmemsdk.c blob a60c01553ff9; recipe make/m3/optane/recipe/pmem-region-core-20261106/relocate.py v2
#define _GNU_SOURCE
#define _FILE_OFFSET_BITS 64
#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <float.h>
#include <limits.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#ifdef M3_HAVE_LIBPMEM2
#include <libpmem2.h>
#endif

/* Private ownership only: region pointers never escape to public Java callers.
   Java-side exclusive close/read-write leases cover every native operation. */
typedef struct {
    int fd;
    uint64_t file_origin;
    dev_t device;
    ino_t inode;
    int writable;
    int backend;  /* 1=POSIX, 2=PMDK */
    int granularity; /* 0=BYTE, 1=CACHE_LINE, 2=PAGE */
    unsigned char *data;
    void *base;
    size_t map_length;
    size_t length;
#ifdef M3_HAVE_LIBPMEM2
    struct pmem2_map *pmem_map;
    struct pmem2_source *pmem_source;
    struct pmem2_config *pmem_config;
    pmem2_persist_fn persist_fn;
#endif
} region;

static void throw_named(JNIEnv *env, const char *cls, const char *msg) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type=(*env)->FindClass(env,cls);
    if (type) (*env)->ThrowNew(env,type,msg);
}
static void throw_errno(JNIEnv *env, const char *action, int err) {
    char msg[320];
    (void)snprintf(msg,sizeof(msg),"PMEMSDK %s: %s",action,strerror(err));
    throw_named(env,"java/io/IOException",msg);
}
static void cleanup(region *r) {
    if (!r) return;
#ifdef M3_HAVE_LIBPMEM2
    if (r->pmem_map) pmem2_map_delete(&r->pmem_map);
    if (r->pmem_source) pmem2_source_delete(&r->pmem_source);
    if (r->pmem_config) pmem2_config_delete(&r->pmem_config);
#endif
    if (r->base) (void)munmap(r->base,r->map_length);
    if (r->fd>=0) (void)close(r->fd);
    free(r);
}
static region *validated(JNIEnv *env, jlong handle) {
    if (handle == 0) {
        throw_named(env,"java/lang/IllegalStateException","PMem region closed");
        return NULL;
    }
    return (region *)(uintptr_t)(uint64_t)handle;
}
static int bounded(JNIEnv *env, region *r, jlong offset, jlong amount) {
    if (offset < 0 || amount < 0 || (uint64_t)offset > r->length ||
        (uint64_t)amount > r->length-(uint64_t)offset) {
        throw_named(env,"java/lang/IndexOutOfBoundsException","native mapping bounds");
        return 0;
    }
    return 1;
}
static int array_bounded(JNIEnv *env, jbyteArray bytes, jint array_offset, jint length) {
    if (!bytes) { throw_named(env,"java/lang/NullPointerException","array"); return 0; }
    jsize n=(*env)->GetArrayLength(env,bytes);
    if (array_offset < 0 || length < 0 || array_offset > n || length > n-array_offset) {
        throw_named(env,"java/lang/IndexOutOfBoundsException","native byte array bounds");
        return 0;
    }
    return 1;
}

JNIEXPORT jint JNICALL Java_jdk_m3_optane_NativePmem_capabilities
(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
#ifdef M3_HAVE_LIBPMEM2
    return 1;
#else
    return 0;
#endif
}
JNIEXPORT jint JNICALL Java_jdk_m3_optane_NativePmem_pageSize
(JNIEnv *env, jclass cls) {
    (void)env; (void)cls;
    long p=sysconf(_SC_PAGESIZE);
    return p > 0 && p <= INT_MAX ? (jint)p : 0;
}

JNIEXPORT jlong JNICALL Java_jdk_m3_optane_NativePmem_open
(JNIEnv *env, jclass cls, jbyteArray path_bytes, jlong offset, jlong length,
 jint access, jint requested_backend) {
    (void)cls;
    if (!path_bytes || offset < 0 || length <= 0 || offset > INT64_MAX-length ||
        (access!=0 && access!=1) || requested_backend<0 || requested_backend>2 ||
        (uint64_t)length > SIZE_MAX) {
        throw_named(env,"java/lang/IllegalArgumentException","invalid native mapping request");
        return 0;
    }
    jsize n=(*env)->GetArrayLength(env,path_bytes);
    if (n<=0 || n>65536) {
        throw_named(env,"java/lang/IllegalArgumentException","invalid UTF-8 path length");
        return 0;
    }
    char *path=malloc((size_t)n+1);
    if (!path) { throw_named(env,"java/lang/OutOfMemoryError","path allocation"); return 0; }
    (*env)->GetByteArrayRegion(env,path_bytes,0,n,(jbyte*)path);
    if ((*env)->ExceptionCheck(env)) { free(path); return 0; }
    if (memchr(path,0,(size_t)n)) {
        free(path);
        throw_named(env,"java/lang/IllegalArgumentException","embedded NUL in path");
        return 0;
    }
    path[n]='\0';
    region *r=calloc(1,sizeof(*r));
    if (!r) { free(path); throw_named(env,"java/lang/OutOfMemoryError","region allocation"); return 0; }
    r->fd=-1;
    r->writable=(access==1);
    r->length=(size_t)length;
    int flags=(r->writable ? O_RDWR : O_RDONLY) | O_CLOEXEC | O_NOFOLLOW;
    r->fd=open(path,flags);
    free(path);
    if (r->fd<0) { int err=errno;cleanup(r);throw_errno(env,"open",err);return 0; }
    struct stat sb;
    if (fstat(r->fd,&sb)!=0) {
        int err=errno;cleanup(r);throw_errno(env,"fstat",err);return 0;
    }
    r->file_origin=(uint64_t)offset;
    r->device=sb.st_dev;
    r->inode=sb.st_ino;
    if (!S_ISREG(sb.st_mode) || sb.st_size < 0 || offset > sb.st_size ||
        length > sb.st_size-offset) {
        cleanup(r);
        throw_named(env,"java/io/IOException","mapping must fit an existing regular file");
        return 0;
    }
#ifdef M3_HAVE_LIBPMEM2
    if (requested_backend!=1) {
        int rc=pmem2_config_new(&r->pmem_config);
        if (!rc) rc=pmem2_config_set_protection(r->pmem_config,
                PMEM2_PROT_READ | (r->writable ? PMEM2_PROT_WRITE : 0));
        if (!rc) rc=pmem2_source_from_fd(&r->pmem_source,r->fd);
        if (!rc) rc=pmem2_config_set_required_store_granularity(r->pmem_config,PMEM2_GRANULARITY_PAGE);
        if (!rc) rc=pmem2_config_set_offset(r->pmem_config,(size_t)offset);
        if (!rc) rc=pmem2_config_set_length(r->pmem_config,(size_t)length);
        if (!rc) rc=pmem2_map_new(&r->pmem_map,r->pmem_config,r->pmem_source);
        if (!rc) {
            r->data=pmem2_map_get_address(r->pmem_map);
            size_t mapped=pmem2_map_get_size(r->pmem_map);
            if (!r->data || mapped!=(size_t)length) rc=-1;
        }
        if (!rc) {
            r->persist_fn=pmem2_get_persist_fn(r->pmem_map);
            r->granularity=(int)pmem2_map_get_store_granularity(r->pmem_map);
            r->backend=2;
            return (jlong)(uintptr_t)r;
        }
        /* Never retain any half-constructed PMDK state on POSIX fallback. */
        if (r->pmem_map) pmem2_map_delete(&r->pmem_map);
        if (r->pmem_source) pmem2_source_delete(&r->pmem_source);
        if (r->pmem_config) pmem2_config_delete(&r->pmem_config);
        r->data=NULL;
        if (requested_backend==2) {
            cleanup(r);
            throw_named(env,"java/io/IOException","libpmem2 mapping unsupported for requested file span");
            return 0;
        }
    }
#else
    if (requested_backend==2) {
        cleanup(r);
        throw_named(env,"java/lang/UnsupportedOperationException","libpmem2 was not compiled into this SDK");
        return 0;
    }
#endif
    long page=sysconf(_SC_PAGESIZE);
    if (page<=0) { cleanup(r);throw_named(env,"java/io/IOException","invalid OS page size");return 0; }
    uint64_t delta=(uint64_t)offset%(uint64_t)page;
    if ((uint64_t)length > SIZE_MAX-delta) {
        cleanup(r);throw_named(env,"java/lang/IllegalArgumentException","mapped length overflow");return 0;
    }
    off_t aligned=(off_t)((uint64_t)offset-delta);
    r->map_length=(size_t)length+(size_t)delta;
    int prot=PROT_READ | (r->writable?PROT_WRITE:0);
    r->base=mmap(NULL,r->map_length,prot,MAP_SHARED,r->fd,aligned);
    if (r->base==MAP_FAILED) {
        int err=errno;r->base=NULL;cleanup(r);throw_errno(env,"mmap",err);return 0;
    }
    r->data=(unsigned char*)r->base+delta;
    r->backend=1;
    r->granularity=2;
    return (jlong)(uintptr_t)r;
}
JNIEXPORT jint JNICALL Java_jdk_m3_optane_NativePmem_selectedBackend
(JNIEnv *env, jclass cls, jlong handle) {
    (void)cls;region *r=validated(env,handle);return r?r->backend:0;
}
JNIEXPORT jint JNICALL Java_jdk_m3_optane_NativePmem_granularity
(JNIEnv *env, jclass cls, jlong handle) {
    (void)cls;region *r=validated(env,handle);return r?r->granularity:0;
}
JNIEXPORT void JNICALL Java_jdk_m3_optane_NativePmem_read
(JNIEnv *env,jclass cls,jlong handle,jlong offset,jbyteArray out,jint start,jint length) {
    (void)cls;region *r=validated(env,handle);
    if (!r || !array_bounded(env,out,start,length) || !bounded(env,r,offset,length)) return;
    if (length) (*env)->SetByteArrayRegion(env,out,start,length,(jbyte*)(r->data+(size_t)offset));
}
JNIEXPORT void JNICALL Java_jdk_m3_optane_NativePmem_write
(JNIEnv *env,jclass cls,jlong handle,jlong offset,jbyteArray data,jint start,jint length) {
    (void)cls;region *r=validated(env,handle);
    if (!r || !array_bounded(env,data,start,length) || !bounded(env,r,offset,length)) return;
    if (!r->writable) {throw_named(env,"java/lang/IllegalStateException","mapping read-only");return;}
    if (length) (*env)->GetByteArrayRegion(env,data,start,length,(jbyte*)(r->data+(size_t)offset));
}
JNIEXPORT void JNICALL Java_jdk_m3_optane_NativePmem_persist
(JNIEnv *env,jclass cls,jlong handle,jlong offset,jlong length) {
    (void)cls;region *r=validated(env,handle);
    if (!r || !bounded(env,r,offset,length)) return;
    if (!r->writable) {throw_named(env,"java/lang/IllegalStateException","mapping read-only");return;}
    if (!length) return;
#ifdef M3_HAVE_LIBPMEM2
    if (r->pmem_map) {
        r->persist_fn(r->data+(size_t)offset,(size_t)length);
        return;
    }
#endif
    long pagesz=sysconf(_SC_PAGESIZE);
    if (pagesz<=0) {throw_named(env,"java/io/IOException","invalid OS page size");return;}
    uintptr_t addr=(uintptr_t)(r->data+(size_t)offset);
    uintptr_t lower=addr-(addr%(uintptr_t)pagesz);
    size_t span=(size_t)length+(size_t)(addr-lower);
    if (msync((void *)lower,span,MS_SYNC)<0) {throw_errno(env,"msync",errno);return;}
    if (fdatasync(r->fd)<0) {throw_errno(env,"fdatasync",errno);return;}
}
JNIEXPORT void JNICALL Java_jdk_m3_optane_NativePmem_close
(JNIEnv *env,jclass cls,jlong handle) {
    (void)cls;region *r=validated(env,handle);
    if (!r) return;
    int err=0;
#ifdef M3_HAVE_LIBPMEM2
    if (r->pmem_map && pmem2_map_delete(&r->pmem_map)!=0) err=EIO;
    if (r->pmem_source) pmem2_source_delete(&r->pmem_source);
    if (r->pmem_config) pmem2_config_delete(&r->pmem_config);
#else
    (void)r;
#endif
    if (r->base && munmap(r->base,r->map_length)<0) err=errno;
    if (r->fd>=0 && close(r->fd)<0) err=errno;
    free(r);
    if (err) throw_errno(env,"close",err);
}

/* M3_FLOAT_ROWS_C_BEGIN: additive typed-copy atom; existing JNI ABI is retained. */
_Static_assert(CHAR_BIT == 8 && sizeof(jfloat) == sizeof(uint32_t)
        && FLT_RADIX == 2 && FLT_MANT_DIG == 24 && FLT_MAX_EXP == 128,
        "PMem float rows require IEEE 754 binary32 JNI floats");

/* Byte loads avoid alignment/aliasing assumptions; memcpy performs no FP arithmetic. */
static void m3_float_rows_decode(const unsigned char *bytes, jfloat *values, size_t count) {
    for (size_t i = 0; i < count; i++) {
        const unsigned char *p = bytes + i * sizeof(uint32_t);
        uint32_t bits = (uint32_t)p[0] | ((uint32_t)p[1] << 8)
                | ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
        memcpy(values + i, &bits, sizeof(bits));
    }
}

/* Null selection means consecutive rows. Non-null selection is a private Java
   snapshot. Validate the entire request before the first output store; bounded
   JNI regions avoid critical pins and any native pointer/heap pointer escape. */
JNIEXPORT void JNICALL Java_jdk_m3_optane_NativePmem_floatRowsLE
(JNIEnv *env, jclass cls, jlong handle, jlong offset, jlong rows, jint columns,
 jlong first_row, jlongArray row_ids, jint count, jfloatArray target, jint target_offset) {
    (void)cls;
    region *r = validated(env, handle);
    if (!r) return;
    if (columns <= 0) {
        throw_named(env, "java/lang/IllegalArgumentException", "columns must be positive");
        return;
    }
    uint64_t row_bytes = (uint64_t)columns * sizeof(uint32_t);
    if (rows < 0 || (uint64_t)rows > (uint64_t)INT64_MAX / row_bytes || count < 0) {
        throw_named(env, "java/lang/IndexOutOfBoundsException", "native float table shape");
        return;
    }
    if (!bounded(env, r, offset, rows * (jlong)row_bytes)) return;
    if (!target) {
        throw_named(env, "java/lang/NullPointerException", "target");
        return;
    }
    jsize target_length = (*env)->GetArrayLength(env, target);
    if (target_offset < 0 || target_offset > target_length
            || count > (target_length - target_offset) / columns) {
        throw_named(env, "java/lang/IndexOutOfBoundsException", "native float output bounds");
        return;
    }
    enum { ID_WINDOW = 512, VALUE_WINDOW = 4096 };
    jlong selected[ID_WINDOW];
    if (row_ids) {
        jsize ids_length = (*env)->GetArrayLength(env, row_ids);
        if (count > ids_length) {
            throw_named(env, "java/lang/IndexOutOfBoundsException", "native row selection bounds");
            return;
        }
        for (jint done = 0; done < count;) {
            jint amount = count - done < ID_WINDOW ? count - done : ID_WINDOW;
            (*env)->GetLongArrayRegion(env, row_ids, done, amount, selected);
            if ((*env)->ExceptionCheck(env)) return;
            for (jint i = 0; i < amount; i++) {
                if (selected[i] < 0 || selected[i] >= rows) {
                    throw_named(env, "java/lang/IndexOutOfBoundsException", "native float row ID");
                    return;
                }
            }
            done += amount;
        }
    } else if (first_row < 0 || first_row > rows || (jlong)count > rows - first_row) {
        throw_named(env, "java/lang/IndexOutOfBoundsException", "native consecutive row range");
        return;
    }
    jfloat values[VALUE_WINDOW];
    jint output = target_offset;
    for (jint done = 0; done < count;) {
        jint amount = count - done < ID_WINDOW ? count - done : ID_WINDOW;
        if (row_ids) {
            (*env)->GetLongArrayRegion(env, row_ids, done, amount, selected);
            if ((*env)->ExceptionCheck(env)) return;
        }
        for (jint i = 0; i < amount; i++) {
            jlong row = row_ids ? selected[i] : first_row + done + i;
            size_t base = (size_t)offset + (size_t)((uint64_t)row * row_bytes);
            for (jint column = 0; column < columns;) {
                jint width = columns - column < VALUE_WINDOW ? columns - column : VALUE_WINDOW;
                m3_float_rows_decode(r->data + base + (size_t)column * sizeof(uint32_t),
                        values, (size_t)width);
                (*env)->SetFloatArrayRegion(env, target, output, width, values);
                if ((*env)->ExceptionCheck(env)) return;
                column += width;
                output += width;
            }
        }
        done += amount;
    }
}
/* M3_FLOAT_ROWS_C_END */

/* M3_TRANSFER_C_BEGIN: recipe-owned JNI copy and compare atom. */
static int m3_same_file_overlap(const region *src, jlong src_offset,
                                 const region *dst, jlong dst_offset, jlong bytes) {
    if (src == dst || bytes == 0 || src->device != dst->device || src->inode != dst->inode)
        return 0;
    /* All operands are known bounded by the mapped file; differences avoid overflow. */
    uint64_t a=src->file_origin+(uint64_t)src_offset;
    uint64_t b=dst->file_origin+(uint64_t)dst_offset;
    uint64_t size=(uint64_t)bytes;
    return a<=b ? b-a < size : a-b < size;
}

JNIEXPORT void JNICALL Java_jdk_m3_optane_NativePmem_copy
(JNIEnv *env, jclass cls, jlong src_handle, jlong src_offset,
 jlong dst_handle, jlong dst_offset, jlong amount) {
    (void)cls;
    region *src=validated(env,src_handle);
    if (!src) return;
    region *dst=validated(env,dst_handle);
    if (!dst || !bounded(env,src,src_offset,amount) ||
        !bounded(env,dst,dst_offset,amount)) return;
    if (!dst->writable) {
        throw_named(env,"java/lang/IllegalStateException","mapping read-only");
        return;
    }
    if (m3_same_file_overlap(src,src_offset,dst,dst_offset,amount)) {
        throw_named(env,"java/lang/IllegalArgumentException",
                    "overlapping file ranges across distinct mappings");
        return;
    }
    if (amount) memmove(dst->data+(size_t)dst_offset,
                        src->data+(size_t)src_offset,(size_t)amount);
}

/* Compare-and-copy in one native lease, avoiding writes for identical weights. */
JNIEXPORT jboolean JNICALL Java_jdk_m3_optane_NativePmem_copyIfDifferent
(JNIEnv *env, jclass cls, jlong src_handle, jlong src_offset,
 jlong dst_handle, jlong dst_offset, jlong amount) {
    (void)cls;
    region *src=validated(env,src_handle);
    if (!src) return JNI_FALSE;
    region *dst=validated(env,dst_handle);
    if (!dst || !bounded(env,src,src_offset,amount) ||
        !bounded(env,dst,dst_offset,amount)) return JNI_FALSE;
    if (!dst->writable) {
        throw_named(env,"java/lang/IllegalStateException","mapping read-only");
        return JNI_FALSE;
    }
    if (m3_same_file_overlap(src,src_offset,dst,dst_offset,amount)) {
        throw_named(env,"java/lang/IllegalArgumentException",
                    "overlapping file ranges across distinct mappings");
        return JNI_FALSE;
    }
    if (!amount || memcmp(src->data+(size_t)src_offset,
                          dst->data+(size_t)dst_offset,(size_t)amount)==0)
        return JNI_FALSE;
    memmove(dst->data+(size_t)dst_offset,
            src->data+(size_t)src_offset,(size_t)amount);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_jdk_m3_optane_NativePmem_equalRange
(JNIEnv *env, jclass cls, jlong a_handle, jlong a_offset,
 jlong b_handle, jlong b_offset, jlong amount) {
    (void)cls;
    region *a=validated(env,a_handle);
    if (!a) return JNI_FALSE;
    region *b=validated(env,b_handle);
    if (!b || !bounded(env,a,a_offset,amount) ||
        !bounded(env,b,b_offset,amount)) return JNI_FALSE;
    if (!amount) return JNI_TRUE;
    return memcmp(a->data+(size_t)a_offset,b->data+(size_t)b_offset,
                  (size_t)amount)==0 ? JNI_TRUE : JNI_FALSE;
}
/* KMP over a single leased mapping; no Java heap arrays or native pointers escape. */
JNIEXPORT jlong JNICALL Java_jdk_m3_optane_NativePmem_indexOf
(JNIEnv *env, jclass cls, jlong handle, jlong offset, jlong count, jbyteArray pattern) {
    (void)cls;
    region *r=validated(env,handle);
    if (!r || !bounded(env,r,offset,count)) return -1;
    if (!pattern) {
        throw_named(env,"java/lang/NullPointerException","pattern");
        return -1;
    }
    jsize needle_size=(*env)->GetArrayLength(env,pattern);
    if (needle_size > (1<<20)) {
        throw_named(env,"java/lang/IllegalArgumentException","PMem search pattern exceeds 1 MiB");
        return -1;
    }
    if (needle_size == 0) return offset;
    if ((jlong)needle_size > count) return -1;
    const unsigned char *haystack=r->data+(size_t)offset;
    size_t n=(size_t)count;
    size_t m=(size_t)needle_size;
    unsigned char *needle=(unsigned char *)malloc(m);
    if (!needle) {
        throw_named(env,"java/lang/OutOfMemoryError","PMem search pattern allocation");
        return -1;
    }
    (*env)->GetByteArrayRegion(env,pattern,0,needle_size,(jbyte *)needle);
    if ((*env)->ExceptionCheck(env)) { free(needle); return -1; }
    if (m==1) {
        const unsigned char *match=(const unsigned char *)memchr(haystack,needle[0],n);
        free(needle);
        return match ? offset+(jlong)(match-haystack) : -1;
    }
    /* MAX_PATTERN_BYTES is 1 MiB: every prefix link fits uint32_t. On
       LP64 this halves the temporary KMP prefix bytes without changing
       first-hit, binary-input, or worst-case linear search behavior. */
    if (m>SIZE_MAX/sizeof(uint32_t)) {
        free(needle);
        throw_named(env,"java/lang/OutOfMemoryError","PMem search prefix overflow");
        return -1;
    }
    uint32_t *prefix=(uint32_t *)malloc(m*sizeof(uint32_t));
    if (!prefix) {
        free(needle);
        throw_named(env,"java/lang/OutOfMemoryError","PMem search prefix allocation");
        return -1;
    }
    prefix[0]=0;
    size_t matched=0;
    for (size_t i=1;i<m;i++) {
        while (matched && needle[i]!=needle[matched]) matched=prefix[matched-1];
        if (needle[i]==needle[matched]) matched++;
        prefix[i]=matched;
    }
    matched=0;
    jlong found=-1;
    for (size_t i=0;i<n;i++) {
        while (matched && haystack[i]!=needle[matched]) matched=prefix[matched-1];
        if (haystack[i]==needle[matched]) matched++;
        if (matched==m) {
            found=offset+(jlong)(i+1-m);
            break;
        }
    }
    free(prefix);
    free(needle);
    return found;
}

/* Sorted signed int64 LE lookup, independent of host alignment/endianness.
   Comparison uses a sign-bit flip to preserve signed sort in uint64_t order.
   The Java owner holds a mapping read lease throughout this JNI call. */
JNIEXPORT jlong JNICALL Java_jdk_m3_optane_NativePmem_boundLongLE
(JNIEnv *env, jclass cls, jlong handle, jlong offset, jlong elements,
 jlong key, jboolean strict_upper) {
    (void)cls;
    region *r=validated(env,handle);
    if (!r) return -1;
    if (elements<0 || (uint64_t)elements > (uint64_t)INT64_MAX / sizeof(uint64_t)) {
        throw_named(env,"java/lang/IndexOutOfBoundsException","native long element count out of bounds");
        return -1;
    }
    jlong bytes=elements*(jlong)sizeof(uint64_t);
    if (!bounded(env,r,offset,bytes)) return -1;
    const size_t first=(size_t)offset;
    const uint64_t sign=UINT64_C(0x8000000000000000);
    const uint64_t ordered_key=((uint64_t)key)^sign;
    size_t lo=0,hi=(size_t)elements;
    while (lo<hi) {
        size_t mid=lo+(hi-lo)/2;
        const unsigned char *p=r->data+first+mid*sizeof(uint64_t);
        uint64_t bits=0;
        for (size_t i=0;i<sizeof(uint64_t);i++) bits|=(uint64_t)p[i]<<(i*8);
        uint64_t ordered_value=bits^sign;
        if (ordered_value<ordered_key ||
                (strict_upper==JNI_TRUE && ordered_value==ordered_key)) lo=mid+1;
        else hi=mid;
    }
    return (jlong)lo;
}
/* M3_TRANSFER_C_END */
