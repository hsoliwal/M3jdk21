/* SPDX-License-Identifier: Apache-2.0 */
/*
 * m3vcpu_jni.c - thin JNI glue for com.m3.vcpu.internal.NativeVcpu. Six entry points:
 *   long open(String selector, int device)        selector: "auto" | "gpu" | "name[:dev],..."
 *   long alloc(long session, long bytes)           zero-filled device buffer
 *   void upload(long buffer, long byteOffset, Object src, int srcOffset, int length)
 *        src = int[] (offset/length in ints) or a direct ByteBuffer (offset/length in bytes)
 *   void launch(long session, String kernel, long global, int local, long[] buffers)   synchronous
 *   void download(long buffer, long byteOffset, int[] dst, int dstOffset, int length)
 *   void close(long handle)                         session or buffer; idempotent
 * No policy and no vendor code: marshalling, a generation-checked handle table (a stale or
 * double close from a Cleaner is a no-op, never a use-after-free), per-session locking, and
 * status -> exception mapping:
 *   INVALID_ARG -> IllegalArgumentException      OUT_OF_RANGE -> IndexOutOfBoundsException
 *   OUT_OF_MEMORY -> OutOfMemoryError            UNAVAILABLE / NOT_FOUND / NO_DEVICE /
 *   stale handle, anything else -> IllegalStateException     NO_KERNEL_FORMAT -> UnsupportedOperationException
 */
#include <jni.h>

#include "m3vcpu.h"
#include "m3vcpu_sys.h"

#include <stdlib.h>
#include <string.h>

#define M3J_SESSION 1u
#define M3J_BUFFER 2u
#define M3J_MAX_SLOTS (1u << 24)
#define M3J_NONE 0xFFFFFFFFu

typedef struct m3j_session {
    m3_mutex lock;      /* serializes every call on the session and its buffers */
    m3vcpu_session *s;
    int closed;         /* guarded by lock */
    uint32_t refs;      /* guarded by the table lock: slots + in-flight calls */
} m3j_session;

typedef struct m3j_slot {
    uint32_t gen;
    uint32_t kind; /* 0 = free */
    uint32_t next_free;
    m3j_session *owner;
    m3vcpu_buf *buf;
} m3j_slot;

static m3_mutex m3j_table_lock = M3_MUTEX_INITIALIZER;
static m3j_slot *m3j_slots;
static uint32_t m3j_used;
static uint32_t m3j_cap;
static uint32_t m3j_free_head = M3J_NONE;
static jclass m3j_int_array;

/* ---- exceptions ---------------------------------------------------------------------------- */
static void m3j_throw_class(JNIEnv *env, const char *cls, const char *msg)
{
    jclass type;
    if ((*env)->ExceptionCheck(env)) {
        return;
    }
    type = (*env)->FindClass(env, cls);
    if (type != NULL) {
        (void)(*env)->ThrowNew(env, type, msg);
        (*env)->DeleteLocalRef(env, type);
    }
}

static void m3j_throw(JNIEnv *env, int32_t status, const char *op, const char *detail)
{
    char msg[768];
    const char *cls;
    switch (status) {
    case M3VCPU_E_INVALID_ARG:
        cls = "java/lang/IllegalArgumentException";
        break;
    case M3VCPU_E_OUT_OF_RANGE:
        cls = "java/lang/IndexOutOfBoundsException";
        break;
    case M3VCPU_E_OUT_OF_MEMORY:
        cls = "java/lang/OutOfMemoryError";
        break;
    case M3VCPU_E_UNAVAILABLE:
    case M3VCPU_E_NOT_FOUND:
    case M3VCPU_E_NO_DEVICE:
    case M3VCPU_E_NO_KERNEL_FORMAT:
        cls = "java/lang/UnsupportedOperationException";
        break;
    default:
        cls = "java/lang/IllegalStateException";
        break;
    }
    m3_format(msg, sizeof msg, "m3vcpu %s: %s: %s", op, m3vcpu_status_name(status), detail == NULL ? "" : detail);
    m3j_throw_class(env, cls, msg);
}

/* ---- handle table ------------------------------------------------------------------------- */
static void m3j_unref_locked(m3j_session *owner, m3j_session **dead)
{
    if (--owner->refs == 0u) {
        *dead = owner;
    }
}

static void m3j_destroy(m3j_session *dead)
{
    if (dead != NULL) {
        m3_mutex_destroy(&dead->lock);
        free(dead);
    }
}

static void m3j_release(m3j_session *owner)
{
    m3j_session *dead = NULL;
    m3_mutex_lock(&m3j_table_lock);
    m3j_unref_locked(owner, &dead);
    m3_mutex_unlock(&m3j_table_lock);
    m3j_destroy(dead);
}

static jlong m3j_slot_new(uint32_t kind, m3j_session *owner, m3vcpu_buf *buf)
{
    uint32_t index;
    m3j_slot *slot;
    jlong handle = 0;
    m3_mutex_lock(&m3j_table_lock);
    if (m3j_free_head != M3J_NONE) {
        index = m3j_free_head;
        m3j_free_head = m3j_slots[index].next_free;
    } else {
        if (m3j_used == m3j_cap) {
            uint32_t cap = m3j_cap == 0u ? 64u : m3j_cap * 2u;
            m3j_slot *grown = cap > M3J_MAX_SLOTS ? NULL : (m3j_slot *)realloc(m3j_slots, cap * sizeof *grown);
            if (grown == NULL) {
                m3_mutex_unlock(&m3j_table_lock);
                return 0;
            }
            memset(grown + m3j_cap, 0, (size_t)(cap - m3j_cap) * sizeof *grown);
            m3j_slots = grown;
            m3j_cap = cap;
        }
        index = m3j_used++;
    }
    slot = &m3j_slots[index];
    slot->gen = slot->gen + 1u == 0u ? 1u : slot->gen + 1u;
    slot->kind = kind;
    slot->owner = owner;
    slot->buf = buf;
    slot->next_free = M3J_NONE;
    owner->refs++;
    handle = (jlong)(((uint64_t)slot->gen << 32) | (uint64_t)(index + 1u));
    m3_mutex_unlock(&m3j_table_lock);
    return handle;
}

static m3j_slot *m3j_lookup_locked(jlong handle, uint32_t kind)
{
    uint64_t h = (uint64_t)handle;
    uint32_t index = (uint32_t)(h & 0xFFFFFFFFu);
    uint32_t gen = (uint32_t)(h >> 32);
    m3j_slot *slot;
    if (index == 0u || index > m3j_used) {
        return NULL;
    }
    slot = &m3j_slots[index - 1u];
    if (slot->kind == 0u || slot->gen != gen || (kind != 0u && slot->kind != kind)) {
        return NULL;
    }
    return slot;
}

/* Takes a call reference on the owning session; NULL when the handle is stale or of another kind. */
static m3j_session *m3j_acquire(jlong handle, uint32_t kind, m3vcpu_buf **buf)
{
    m3j_session *owner = NULL;
    m3j_slot *slot;
    m3_mutex_lock(&m3j_table_lock);
    slot = m3j_lookup_locked(handle, kind);
    if (slot != NULL) {
        owner = slot->owner;
        owner->refs++;
        if (buf != NULL) {
            *buf = slot->buf;
        }
    }
    m3_mutex_unlock(&m3j_table_lock);
    return owner;
}

/* Locks an acquired session; throws and returns 0 when it is already closed. */
static int m3j_enter(JNIEnv *env, m3j_session *owner, const char *op)
{
    m3_mutex_lock(&owner->lock);
    if (owner->closed) {
        m3_mutex_unlock(&owner->lock);
        m3j_throw_class(env, "java/lang/IllegalStateException", op);
        return 0;
    }
    return 1;
}

/* ---- entry points ------------------------------------------------------------------------- */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env = NULL;
    jclass local;
    (void)reserved;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }
    local = (*env)->FindClass(env, "[I");
    if (local == NULL) {
        return JNI_ERR;
    }
    m3j_int_array = (jclass)(*env)->NewGlobalRef(env, local);
    (*env)->DeleteLocalRef(env, local);
    return m3j_int_array == NULL ? JNI_ERR : JNI_VERSION_1_8;
}

JNIEXPORT jlong JNICALL Java_com_m3_vcpu_internal_NativeVcpu_open(JNIEnv *env, jclass type, jstring selector,
                                                                   jint device)
{
    const char *sel = NULL;
    m3vcpu_session *s = NULL;
    m3j_session *owner;
    int32_t rc;
    jlong handle;
    (void)type;
    if (selector != NULL) {
        sel = (*env)->GetStringUTFChars(env, selector, NULL);
        if (sel == NULL) {
            return 0;
        }
    }
    rc = m3vcpu_open(sel, (int32_t)device, NULL, &s);
    if (sel != NULL) {
        (*env)->ReleaseStringUTFChars(env, selector, sel);
    }
    if (rc != M3VCPU_OK) {
        m3j_throw(env, rc, "open", m3vcpu_last_error(NULL));
        return 0;
    }
    owner = (m3j_session *)calloc(1, sizeof *owner);
    if (owner == NULL) {
        m3vcpu_close(s);
        m3j_throw(env, M3VCPU_E_OUT_OF_MEMORY, "open", "session wrapper");
        return 0;
    }
    m3_mutex_init(&owner->lock);
    owner->s = s;
    handle = m3j_slot_new(M3J_SESSION, owner, NULL);
    if (handle == 0) {
        m3vcpu_close(s);
        m3j_destroy(owner);
        m3j_throw(env, M3VCPU_E_OUT_OF_MEMORY, "open", "handle table");
    }
    return handle;
}

JNIEXPORT jlong JNICALL Java_com_m3_vcpu_internal_NativeVcpu_alloc(JNIEnv *env, jclass type, jlong session,
                                                                    jlong bytes)
{
    m3j_session *owner;
    m3vcpu_buf *buf = NULL;
    int32_t rc;
    jlong handle = 0;
    (void)type;
    if (bytes <= 0) {
        m3j_throw(env, M3VCPU_E_INVALID_ARG, "alloc", "bytes must be > 0");
        return 0;
    }
    owner = m3j_acquire(session, M3J_SESSION, NULL);
    if (owner == NULL) {
        m3j_throw_class(env, "java/lang/IllegalStateException", "m3vcpu alloc: stale or closed session handle");
        return 0;
    }
    if (m3j_enter(env, owner, "m3vcpu alloc: session closed")) {
        rc = m3vcpu_buffer_alloc(owner->s, (size_t)bytes, M3VCPU_BUF_READ_WRITE, &buf);
        if (rc != M3VCPU_OK) {
            m3j_throw(env, rc, "alloc", m3vcpu_last_error(owner->s));
        } else {
            handle = m3j_slot_new(M3J_BUFFER, owner, buf);
            if (handle == 0) {
                m3vcpu_buffer_free(owner->s, buf);
                m3j_throw(env, M3VCPU_E_OUT_OF_MEMORY, "alloc", "handle table");
            }
        }
        m3_mutex_unlock(&owner->lock);
    }
    m3j_release(owner);
    return handle;
}

static int m3j_range(JNIEnv *env, const char *op, jlong byte_offset, jlong offset, jlong length, jlong limit)
{
    if (byte_offset < 0 || offset < 0 || length < 0 || offset > limit || length > limit - offset) {
        char msg[160];
        m3_format(msg, sizeof msg, "m3vcpu %s: offset %lld + length %lld outside [0, %lld) or negative device offset",
                  op, (long long)offset, (long long)length, (long long)limit);
        m3j_throw_class(env, "java/lang/IndexOutOfBoundsException", msg);
        return 0;
    }
    return 1;
}

JNIEXPORT void JNICALL Java_com_m3_vcpu_internal_NativeVcpu_upload(JNIEnv *env, jclass type, jlong buffer,
                                                                    jlong byte_offset, jobject src, jint src_offset,
                                                                    jint length)
{
    m3vcpu_buf *buf = NULL;
    m3j_session *owner;
    int32_t rc = M3VCPU_OK;
    (void)type;
    if (src == NULL) {
        m3j_throw_class(env, "java/lang/NullPointerException", "m3vcpu upload: null source");
        return;
    }
    owner = m3j_acquire(buffer, M3J_BUFFER, &buf);
    if (owner == NULL) {
        m3j_throw_class(env, "java/lang/IllegalStateException", "m3vcpu upload: stale or closed buffer handle");
        return;
    }
    if ((*env)->IsInstanceOf(env, src, m3j_int_array)) {
        jsize n = (*env)->GetArrayLength(env, (jintArray)src);
        if (m3j_range(env, "upload", byte_offset, src_offset, length, n)
            && m3j_enter(env, owner, "m3vcpu upload: session closed")) {
            jint *data = (jint *)(*env)->GetPrimitiveArrayCritical(env, (jarray)src, NULL);
            if (data != NULL) {
                rc = m3vcpu_buffer_write(owner->s, buf, (size_t)byte_offset, data + src_offset,
                                         (size_t)length * sizeof(jint));
                (*env)->ReleasePrimitiveArrayCritical(env, (jarray)src, data, JNI_ABORT);
            }
            if (rc != M3VCPU_OK) {
                m3j_throw(env, rc, "upload", m3vcpu_last_error(owner->s));
            }
            m3_mutex_unlock(&owner->lock);
        }
    } else {
        uint8_t *base = (uint8_t *)(*env)->GetDirectBufferAddress(env, src);
        jlong capacity = base == NULL ? -1 : (*env)->GetDirectBufferCapacity(env, src);
        if (base == NULL || capacity < 0) {
            m3j_throw(env, M3VCPU_E_INVALID_ARG, "upload", "source must be int[] or a direct ByteBuffer");
        } else if (m3j_range(env, "upload", byte_offset, src_offset, length, capacity)
                   && m3j_enter(env, owner, "m3vcpu upload: session closed")) {
            rc = m3vcpu_buffer_write(owner->s, buf, (size_t)byte_offset, base + src_offset, (size_t)length);
            if (rc != M3VCPU_OK) {
                m3j_throw(env, rc, "upload", m3vcpu_last_error(owner->s));
            }
            m3_mutex_unlock(&owner->lock);
        }
    }
    m3j_release(owner);
}

JNIEXPORT void JNICALL Java_com_m3_vcpu_internal_NativeVcpu_download(JNIEnv *env, jclass type, jlong buffer,
                                                                      jlong byte_offset, jintArray dst,
                                                                      jint dst_offset, jint length)
{
    m3vcpu_buf *buf = NULL;
    m3j_session *owner;
    int32_t rc = M3VCPU_OK;
    (void)type;
    if (dst == NULL) {
        m3j_throw_class(env, "java/lang/NullPointerException", "m3vcpu download: null destination");
        return;
    }
    owner = m3j_acquire(buffer, M3J_BUFFER, &buf);
    if (owner == NULL) {
        m3j_throw_class(env, "java/lang/IllegalStateException", "m3vcpu download: stale or closed buffer handle");
        return;
    }
    if (m3j_range(env, "download", byte_offset, dst_offset, length, (*env)->GetArrayLength(env, dst))
        && m3j_enter(env, owner, "m3vcpu download: session closed")) {
        jint *data = (jint *)(*env)->GetPrimitiveArrayCritical(env, dst, NULL);
        if (data != NULL) {
            rc = m3vcpu_buffer_read(owner->s, buf, (size_t)byte_offset, data + dst_offset,
                                    (size_t)length * sizeof(jint));
            (*env)->ReleasePrimitiveArrayCritical(env, dst, data, rc == M3VCPU_OK ? 0 : JNI_ABORT);
        }
        if (rc != M3VCPU_OK) {
            m3j_throw(env, rc, "download", m3vcpu_last_error(owner->s));
        }
        m3_mutex_unlock(&owner->lock);
    }
    m3j_release(owner);
}

JNIEXPORT void JNICALL Java_com_m3_vcpu_internal_NativeVcpu_launch(JNIEnv *env, jclass type, jlong session,
                                                                    jstring kernel, jlong global, jint local,
                                                                    jlongArray buffers)
{
    m3vcpu_arg args[M3VCPU_MAX_ARGS];
    m3j_session *refs[M3VCPU_MAX_ARGS];
    jlong handles[M3VCPU_MAX_ARGS];
    m3j_session *owner;
    m3vcpu_kernel *k = NULL;
    const char *name;
    jsize count, i, taken = 0;
    int32_t rc;
    (void)type;
    if (kernel == NULL || buffers == NULL) {
        m3j_throw_class(env, "java/lang/NullPointerException", "m3vcpu launch: null kernel or buffers");
        return;
    }
    count = (*env)->GetArrayLength(env, buffers);
    if (count < 0 || count > (jsize)M3VCPU_MAX_ARGS || global < 0 || local < 0) {
        m3j_throw(env, M3VCPU_E_INVALID_ARG, "launch", "bad argument count, global or local");
        return;
    }
    (*env)->GetLongArrayRegion(env, buffers, 0, count, handles);
    owner = m3j_acquire(session, M3J_SESSION, NULL);
    if (owner == NULL) {
        m3j_throw_class(env, "java/lang/IllegalStateException", "m3vcpu launch: stale or closed session handle");
        return;
    }
    for (i = 0; i < count; ++i) {
        args[i].kind = M3VCPU_ARG_BUFFER;
        args[i].i32 = 0;
        args[i].buf = NULL;
        refs[i] = m3j_acquire(handles[i], M3J_BUFFER, &args[i].buf);
        if (refs[i] == NULL) {
            break;
        }
        ++taken;
        if (refs[i] != owner) {
            break;
        }
    }
    name = (*env)->GetStringUTFChars(env, kernel, NULL);
    if (taken != count || (count > 0 && refs[count - 1] != owner)) {
        m3j_throw(env, M3VCPU_E_INVALID_ARG, "launch", "stale buffer handle or buffer of another session");
    } else if (name != NULL && m3j_enter(env, owner, "m3vcpu launch: session closed")) {
        rc = m3vcpu_kernel_builtin(owner->s, name, &k);
        if (rc == M3VCPU_OK) {
            rc = m3vcpu_launch_1d(owner->s, k, (uint64_t)global, (uint32_t)local, args, (uint32_t)count);
        }
        if (rc == M3VCPU_OK) {
            rc = m3vcpu_finish(owner->s);
        }
        if (rc != M3VCPU_OK) {
            m3j_throw(env, rc, "launch", m3vcpu_last_error(owner->s));
        }
        m3_mutex_unlock(&owner->lock);
    }
    if (name != NULL) {
        (*env)->ReleaseStringUTFChars(env, kernel, name);
    }
    for (i = 0; i < taken; ++i) {
        m3j_release(refs[i]);
    }
    m3j_release(owner);
}

JNIEXPORT void JNICALL Java_com_m3_vcpu_internal_NativeVcpu_close(JNIEnv *env, jclass type, jlong handle)
{
    m3j_session *owner = NULL;
    m3vcpu_buf *buf = NULL;
    uint32_t kind = 0;
    m3j_slot *slot;
    (void)env;
    (void)type;
    if (handle == 0) {
        return;
    }
    m3_mutex_lock(&m3j_table_lock);
    slot = m3j_lookup_locked(handle, 0u);
    if (slot != NULL) {
        uint32_t index = (uint32_t)(slot - m3j_slots);
        owner = slot->owner; /* the slot's reference moves to this call */
        buf = slot->buf;
        kind = slot->kind;
        slot->kind = 0u;
        slot->owner = NULL;
        slot->buf = NULL;
        slot->next_free = m3j_free_head;
        m3j_free_head = index;
    }
    m3_mutex_unlock(&m3j_table_lock);
    if (owner == NULL) {
        return; /* stale or repeated close: no-op */
    }
    m3_mutex_lock(&owner->lock);
    if (!owner->closed) {
        if (kind == M3J_SESSION) {
            m3vcpu_close(owner->s);
            owner->s = NULL;
            owner->closed = 1;
        } else {
            m3vcpu_buffer_free(owner->s, buf);
        }
    }
    m3_mutex_unlock(&owner->lock);
    m3j_release(owner);
}
