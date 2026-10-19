// SPDX-License-Identifier: Apache-2.0
#include <jni.h>

#include "m3fs.h"
#include "m3fs_semantic.h"

#include <limits.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#if defined(_WIN32)
#include <windows.h>
static SRWLOCK g_registry_lock = SRWLOCK_INIT;
static void registry_lock(void) { AcquireSRWLockExclusive(&g_registry_lock); }
static void registry_unlock(void) { ReleaseSRWLockExclusive(&g_registry_lock); }
#else
#include <pthread.h>
static pthread_mutex_t g_registry_lock = PTHREAD_MUTEX_INITIALIZER;
static void registry_lock(void) { (void)pthread_mutex_lock(&g_registry_lock); }
static void registry_unlock(void) { (void)pthread_mutex_unlock(&g_registry_lock); }
#endif

#define M3_JNI_HANDLE_SLOTS 64
#define M3_JNI_MAX_DB_PATH_BYTES 32768U

typedef struct m3_jni_slot {
  m3fs *fs;
  uint32_t generation;
  uint32_t busy;
} m3_jni_slot;

static m3_jni_slot g_slots[M3_JNI_HANDLE_SLOTS];

typedef struct m3_jni_lease {
  m3fs *fs;
  uint32_t slot;
} m3_jni_lease;

static void throw_class(JNIEnv *env, const char *class_name, const char *message) {
  jclass type = (*env)->FindClass(env, class_name);
  if (type != NULL) {
    (void)(*env)->ThrowNew(env, type, message == NULL ? "" : message);
    (*env)->DeleteLocalRef(env, type);
  }
}

static void throw_status(JNIEnv *env, int status, const char *path) {
  const char *message = path == NULL ? m3fs_status_string(status) : path;
  switch (status) {
    case M3FS_NOT_FOUND:
      throw_class(env, "java/nio/file/NoSuchFileException", message);
      break;
    case M3FS_EXISTS:
      throw_class(env, "java/nio/file/FileAlreadyExistsException", message);
      break;
    case M3FS_NOT_DIRECTORY:
      throw_class(env, "java/nio/file/NotDirectoryException", message);
      break;
    case M3FS_NOT_EMPTY:
      throw_class(env, "java/nio/file/DirectoryNotEmptyException", message);
      break;
    case M3FS_STALE:
      throw_class(env, "java/io/IOException", "M3FS stale inode: path was replaced");
      break;
    case M3FS_NOMEM:
      throw_class(env, "java/lang/OutOfMemoryError", message);
      break;
    case M3FS_INVALID:
      throw_class(env, "java/lang/IllegalArgumentException", message);
      break;
    case M3FS_READ_ONLY:
      throw_class(env, "java/io/IOException", "M3FS is read-only");
      break;
    case M3FS_IS_DIRECTORY:
      throw_class(env, "java/io/IOException", "M3FS path is a directory");
      break;
    case M3FS_BUSY:
      throw_class(env, "java/io/IOException", "M3FS is busy");
      break;
    case M3FS_CORRUPT:
      throw_class(env, "java/io/IOException", "M3FS integrity failure");
      break;
    case M3FS_NOSPACE:
      throw_class(env, "java/io/IOException", "M3FS storage is full");
      break;
    default:
      throw_class(env, "java/io/IOException", m3fs_status_string(status));
      break;
  }
}

static int utf8_encode(
    JNIEnv *env,
    jstring value,
    uint32_t max_bytes,
    char **out) {
  if (value == NULL || out == NULL) {
    throw_class(env, "java/lang/NullPointerException", "string");
    return 0;
  }
  *out = NULL;
  jsize units = (*env)->GetStringLength(env, value);
  const jchar *chars = (*env)->GetStringChars(env, value, NULL);
  if (chars == NULL) return 0;

  size_t bytes = 0U;
  int valid = 1;
  for (jsize i = 0; i < units; ++i) {
    uint32_t cp = chars[i];
    if (cp == 0U) {
      valid = 0;
      break;
    }
    if (cp >= UINT32_C(0xd800) && cp <= UINT32_C(0xdbff)) {
      if (i + 1 >= units
          || chars[i + 1] < UINT16_C(0xdc00)
          || chars[i + 1] > UINT16_C(0xdfff)) {
        valid = 0;
        break;
      }
      cp = UINT32_C(0x10000)
          + (((uint32_t)chars[i] - UINT32_C(0xd800)) << 10)
          + ((uint32_t)chars[i + 1] - UINT32_C(0xdc00));
      ++i;
    } else if (cp >= UINT32_C(0xdc00) && cp <= UINT32_C(0xdfff)) {
      valid = 0;
      break;
    }
    size_t needed = cp <= UINT32_C(0x7f)
        ? 1U
        : cp <= UINT32_C(0x7ff)
            ? 2U
            : cp <= UINT32_C(0xffff) ? 3U : 4U;
    if (bytes > max_bytes || needed > (size_t)max_bytes - bytes) {
      valid = 0;
      break;
    }
    bytes += needed;
  }

  char *encoded = NULL;
  if (valid) {
    encoded = (char *)malloc(bytes + 1U);
    if (encoded == NULL) {
      (*env)->ReleaseStringChars(env, value, chars);
      throw_class(env, "java/lang/OutOfMemoryError", "UTF-8 path");
      return 0;
    }
    size_t at = 0U;
    for (jsize i = 0; i < units; ++i) {
      uint32_t cp = chars[i];
      if (cp >= UINT32_C(0xd800) && cp <= UINT32_C(0xdbff)) {
        cp = UINT32_C(0x10000)
            + (((uint32_t)chars[i] - UINT32_C(0xd800)) << 10)
            + ((uint32_t)chars[i + 1] - UINT32_C(0xdc00));
        ++i;
      }
      if (cp <= UINT32_C(0x7f)) {
        encoded[at++] = (char)cp;
      } else if (cp <= UINT32_C(0x7ff)) {
        encoded[at++] = (char)(UINT32_C(0xc0) | (cp >> 6));
        encoded[at++] = (char)(UINT32_C(0x80) | (cp & UINT32_C(0x3f)));
      } else if (cp <= UINT32_C(0xffff)) {
        encoded[at++] = (char)(UINT32_C(0xe0) | (cp >> 12));
        encoded[at++] = (char)(UINT32_C(0x80) | ((cp >> 6) & UINT32_C(0x3f)));
        encoded[at++] = (char)(UINT32_C(0x80) | (cp & UINT32_C(0x3f)));
      } else {
        encoded[at++] = (char)(UINT32_C(0xf0) | (cp >> 18));
        encoded[at++] = (char)(UINT32_C(0x80) | ((cp >> 12) & UINT32_C(0x3f)));
        encoded[at++] = (char)(UINT32_C(0x80) | ((cp >> 6) & UINT32_C(0x3f)));
        encoded[at++] = (char)(UINT32_C(0x80) | (cp & UINT32_C(0x3f)));
      }
    }
    encoded[at] = '\0';
  }
  (*env)->ReleaseStringChars(env, value, chars);
  if (!valid) {
    free(encoded);
    throw_class(env, "java/lang/IllegalArgumentException", "invalid or oversized UTF-16/UTF-8 path");
    return 0;
  }
  *out = encoded;
  return 1;
}

static uint64_t token_for(uint32_t slot, uint32_t generation) {
  return ((uint64_t)generation << 32) | (uint64_t)(slot + 1U);
}

static int decode_token(uint64_t token, uint32_t *slot, uint32_t *generation) {
  uint32_t encoded_slot = (uint32_t)token;
  uint32_t gen = (uint32_t)(token >> 32);
  if (encoded_slot == 0U || encoded_slot > M3_JNI_HANDLE_SLOTS
      || gen == 0U || gen > UINT32_C(0x7fffffff)) {
    return 0;
  }
  *slot = encoded_slot - 1U;
  *generation = gen;
  return 1;
}

static jlong register_fs(JNIEnv *env, m3fs *fs) {
  registry_lock();
  for (uint32_t slot = 0U; slot < M3_JNI_HANDLE_SLOTS; ++slot) {
    if (g_slots[slot].fs == NULL && g_slots[slot].busy == 0U) {
      uint32_t generation = g_slots[slot].generation + 1U;
      if (generation == 0U || generation > UINT32_C(0x7fffffff)) generation = 1U;
      g_slots[slot].generation = generation;
      g_slots[slot].fs = fs;
      uint64_t token = token_for(slot, generation);
      registry_unlock();
      return (jlong)token;
    }
  }
  registry_unlock();
  throw_class(env, "java/lang/IllegalStateException", "M3FS native handle capacity exhausted");
  return 0;
}

static int acquire_fs(JNIEnv *env, jlong raw, m3_jni_lease *lease) {
  if (raw <= 0 || lease == NULL) {
    throw_class(env, "java/lang/IllegalStateException", "invalid M3FS native handle");
    return 0;
  }
  uint32_t slot = 0U, generation = 0U;
  if (!decode_token((uint64_t)raw, &slot, &generation)) {
    throw_class(env, "java/lang/IllegalStateException", "malformed M3FS native handle");
    return 0;
  }
  registry_lock();
  m3_jni_slot *entry = &g_slots[slot];
  if (entry->fs == NULL || entry->generation != generation || entry->busy == UINT32_MAX) {
    registry_unlock();
    throw_class(env, "java/lang/IllegalStateException", "stale or busy M3FS native handle");
    return 0;
  }
  entry->busy++;
  lease->fs = entry->fs;
  lease->slot = slot;
  registry_unlock();
  return 1;
}

static void release_fs(m3_jni_lease *lease) {
  registry_lock();
  if (lease->slot < M3_JNI_HANDLE_SLOTS && g_slots[lease->slot].busy > 0U) {
    g_slots[lease->slot].busy--;
  }
  registry_unlock();
}

static int with_path(
    JNIEnv *env,
    jlong handle,
    jstring path_value,
    m3_jni_lease *lease,
    char **path) {
  if (!utf8_encode(env, path_value, M3FS_MAX_PATH_BYTES, path)) return 0;
  if (!acquire_fs(env, handle, lease)) {
    free(*path);
    *path = NULL;
    return 0;
  }
  return 1;
}

JNIEXPORT jint JNICALL
Java_com_m3_fs_M3FsNative_nativeAbiVersion(
    JNIEnv *env, jclass type) {
  (void)env;
  (void)type;
  return m3fs_abi_version();
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeOpen(
    JNIEnv *env, jclass type, jstring database, jint flags) {
  (void)type;
  char *path = NULL;
  if (!utf8_encode(env, database, M3_JNI_MAX_DB_PATH_BYTES, &path)) return 0;
  m3fs *fs = NULL;
  int status = m3fs_open(path, (uint32_t)flags, &fs);
  free(path);
  if (status != M3FS_OK) {
    throw_status(env, status, "M3FS database");
    return 0;
  }
  jlong token = register_fs(env, fs);
  if (token == 0) m3fs_close(fs);
  return token;
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeClose(
    JNIEnv *env, jclass type, jlong raw) {
  (void)type;
  if (raw <= 0) return;
  uint32_t slot = 0U, generation = 0U;
  if (!decode_token((uint64_t)raw, &slot, &generation)) {
    throw_class(env, "java/lang/IllegalStateException", "malformed M3FS native handle");
    return;
  }
  m3fs *fs = NULL;
  registry_lock();
  m3_jni_slot *entry = &g_slots[slot];
  if (entry->fs == NULL || entry->generation != generation) {
    registry_unlock();
    return;
  }
  if (entry->busy != 0U) {
    registry_unlock();
    throw_class(env, "java/lang/IllegalStateException", "M3FS native handle is in use");
    return;
  }
  fs = entry->fs;
  entry->fs = NULL;
  registry_unlock();
  m3fs_close(fs);
}

static void unary_path_mutation(
    JNIEnv *env,
    jlong handle,
    jstring path_value,
    uint32_t argument,
    int operation) {
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return;
  int status;
  if (operation == 1) status = m3fs_mkdir(lease.fs, path, argument);
  else if (operation == 2) status = m3fs_create(lease.fs, path, argument);
  else if (operation == 3) status = m3fs_truncate(lease.fs, path, argument);
  else if (operation == 4) status = m3fs_unlink(lease.fs, path);
  else status = m3fs_rmdir(lease.fs, path);
  release_fs(&lease);
  if (status != M3FS_OK) throw_status(env, status, path);
  free(path);
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeMkdir(
    JNIEnv *env, jclass type, jlong handle, jstring path, jint mode) {
  (void)type;
  if (mode < 0) {
    throw_class(env, "java/lang/IllegalArgumentException", "mode");
    return;
  }
  unary_path_mutation(env, handle, path, (uint32_t)mode, 1);
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeCreate(
    JNIEnv *env, jclass type, jlong handle, jstring path, jint mode) {
  (void)type;
  if (mode < 0) {
    throw_class(env, "java/lang/IllegalArgumentException", "mode");
    return;
  }
  unary_path_mutation(env, handle, path, (uint32_t)mode, 2);
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeTruncate(
    JNIEnv *env, jclass type, jlong handle, jstring path_value, jlong size) {
  (void)type;
  if (size < 0) {
    throw_class(env, "java/lang/IllegalArgumentException", "size");
    return;
  }
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return;
  int status = m3fs_truncate(lease.fs, path, (uint64_t)size);
  release_fs(&lease);
  if (status != M3FS_OK) throw_status(env, status, path);
  free(path);
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeUnlink(
    JNIEnv *env, jclass type, jlong handle, jstring path) {
  (void)type;
  unary_path_mutation(env, handle, path, 0U, 4);
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeRmdir(
    JNIEnv *env, jclass type, jlong handle, jstring path) {
  (void)type;
  unary_path_mutation(env, handle, path, 0U, 5);
}

static void binary_path_operation(
    JNIEnv *env,
    jlong handle,
    jstring left_value,
    jstring right_value,
    int operation) {
  char *left = NULL;
  char *right = NULL;
  if (!utf8_encode(env, left_value, M3FS_MAX_PATH_BYTES, &left)) return;
  if (!utf8_encode(env, right_value, M3FS_MAX_PATH_BYTES, &right)) {
    free(left);
    return;
  }
  m3_jni_lease lease;
  if (!acquire_fs(env, handle, &lease)) {
    free(left);
    free(right);
    return;
  }
  int status = operation == 4
      ? m3fs_rename_noreplace(lease.fs, left, right)
      : operation == 1
      ? m3fs_rename(lease.fs, left, right)
      : operation == 2
          ? m3fs_link(lease.fs, left, right)
          : m3fs_symlink(lease.fs, left, right);
  release_fs(&lease);
  if (status != M3FS_OK) throw_status(env, status, right);
  free(left);
  free(right);
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeRename(
    JNIEnv *env, jclass type, jlong handle, jstring from, jstring to) {
  (void)type;
  binary_path_operation(env, handle, from, to, 1);
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeLink(
    JNIEnv *env, jclass type, jlong handle, jstring existing, jstring new_path) {
  (void)type;
  binary_path_operation(env, handle, existing, new_path, 2);
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeSymlink(
    JNIEnv *env, jclass type, jlong handle, jstring target, jstring link_path) {
  (void)type;
  binary_path_operation(env, handle, target, link_path, 3);
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeWrite(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong offset,
    jbyteArray data,
    jint data_offset,
    jint length) {
  (void)type;
  if (offset < 0 || data == NULL || data_offset < 0 || length < 0
      || data_offset > (*env)->GetArrayLength(env, data) - length) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "write geometry");
    return 0;
  }
  unsigned char *copy = NULL;
  if (length != 0) {
    copy = (unsigned char *)malloc((size_t)length);
    if (copy == NULL) {
      throw_class(env, "java/lang/OutOfMemoryError", "write copy");
      return 0;
    }
    (*env)->GetByteArrayRegion(env, data, data_offset, length, (jbyte *)copy);
    if ((*env)->ExceptionCheck(env)) {
      free(copy);
      return 0;
    }
  }

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) {
    free(copy);
    return 0;
  }
  uint64_t size = 0U;
  int status =
      m3fs_write(
          lease.fs, path, (uint64_t)offset, copy, (uint32_t)length, &size);
  release_fs(&lease);
  free(copy);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jlong)size;
}

JNIEXPORT jint JNICALL
Java_com_m3_fs_M3FsNative_nativeRead(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong offset,
    jbyteArray output,
    jint output_offset,
    jint length) {
  (void)type;
  if (offset < 0 || output == NULL || output_offset < 0 || length < 0
      || output_offset > (*env)->GetArrayLength(env, output) - length) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "read geometry");
    return 0;
  }
  unsigned char *copy = NULL;
  if (length != 0) {
    copy = (unsigned char *)malloc((size_t)length);
    if (copy == NULL) {
      throw_class(env, "java/lang/OutOfMemoryError", "read copy");
      return 0;
    }
  }

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) {
    free(copy);
    return 0;
  }
  uint32_t read = 0U;
  int status =
      m3fs_read(
          lease.fs, path, (uint64_t)offset, copy, (uint32_t)length, &read);
  release_fs(&lease);
  if (status == M3FS_OK && read != 0U) {
    (*env)->SetByteArrayRegion(env, output, output_offset, (jsize)read, (const jbyte *)copy);
  }
  free(copy);
  if ((*env)->ExceptionCheck(env)) {
    free(path);
    return 0;
  }
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jint)read;
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeWriteDirect(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong offset,
    jobject buffer,
    jint buffer_offset,
    jint length) {
  (void)type;
  if (offset < 0 || buffer == NULL || buffer_offset < 0 || length < 0) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "direct write geometry");
    return 0;
  }
  void *address = (*env)->GetDirectBufferAddress(env, buffer);
  jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
  if (address == NULL || capacity < 0
      || (jlong)buffer_offset > capacity - (jlong)length) {
    throw_class(env, "java/lang/IllegalArgumentException", "direct write buffer");
    return 0;
  }

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return 0;
  uint64_t size = 0U;
  int status =
      m3fs_write(
          lease.fs,
          path,
          (uint64_t)offset,
          (const unsigned char *)address + (size_t)(uint32_t)buffer_offset,
          (uint32_t)length,
          &size);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jlong)size;
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeWriteDirectSegments(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong offset,
    jobjectArray buffers) {
  (void)type;
  if (offset < 0 || buffers == NULL) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "scatter/gather write geometry");
    return 0;
  }
  jsize count = (*env)->GetArrayLength(env, buffers);
  if ((*env)->ExceptionCheck(env)) return 0;
  if (count < 0 || count > (jsize)M3FS_MAX_WRITE_SEGMENTS) {
    throw_class(env, "java/lang/IllegalArgumentException", "scatter/gather segment budget");
    return 0;
  }

  m3fs_iovec *segments = count == 0
      ? NULL
      : (m3fs_iovec *)calloc((size_t)count, sizeof(*segments));
  if (count != 0 && segments == NULL) {
    throw_class(env, "java/lang/OutOfMemoryError", "scatter/gather descriptor allocation");
    return 0;
  }

  for (jsize index = 0; index < count; ++index) {
    jobject buffer = (*env)->GetObjectArrayElement(env, buffers, index);
    if ((*env)->ExceptionCheck(env)) {
      free(segments);
      return 0;
    }
    if (buffer == NULL) {
      free(segments);
      throw_class(env, "java/lang/NullPointerException", "scatter/gather segment");
      return 0;
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    void *address = (*env)->GetDirectBufferAddress(env, buffer);
    (*env)->DeleteLocalRef(env, buffer);
    if (capacity < 0 || capacity > (jlong)UINT32_MAX
        || (capacity != 0 && address == NULL)) {
      free(segments);
      throw_class(env, "java/lang/IllegalArgumentException",
          "scatter/gather requires exact direct-buffer slices");
      return 0;
    }
    segments[index].data = address;
    segments[index].length = (uint32_t)capacity;
  }

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) {
    free(segments);
    return 0;
  }
  uint64_t size = 0U;
  int status = m3fs_writev(
      lease.fs, path, (uint64_t)offset, segments, (uint32_t)count, &size);
  release_fs(&lease);
  free(segments);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jlong)size;
}

/* Throw the no-argument NIO exception; ThrowNew would require a String constructor. */
static int require_writable_buffer(JNIEnv *env, jobject buffer) {
  jclass type = (*env)->GetObjectClass(env, buffer);
  if (type == NULL) return 0;
  jmethodID method = (*env)->GetMethodID(env, type, "isReadOnly", "()Z");
  if (method == NULL) {
    (*env)->DeleteLocalRef(env, type);
    return 0;
  }
  jboolean read_only = (*env)->CallBooleanMethod(env, buffer, method);
  (*env)->DeleteLocalRef(env, type);
  if ((*env)->ExceptionCheck(env)) return 0;
  if (!read_only) return 1;
  jclass error = (*env)->FindClass(env, "java/nio/ReadOnlyBufferException");
  if (error == NULL) return 0;
  jmethodID constructor = (*env)->GetMethodID(env, error, "<init>", "()V");
  if (constructor != NULL) {
    jthrowable failure = (jthrowable)(*env)->NewObject(env, error, constructor);
    if (failure != NULL) {
      (void)(*env)->Throw(env, failure);
      (*env)->DeleteLocalRef(env, failure);
    }
  }
  (*env)->DeleteLocalRef(env, error);
  return 0;
}

JNIEXPORT jint JNICALL
Java_com_m3_fs_M3FsNative_nativeReadDirect(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong offset,
    jobject buffer,
    jint buffer_offset,
    jint length) {
  (void)type;
  if (offset < 0 || buffer == NULL || buffer_offset < 0 || length < 0) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "direct read geometry");
    return 0;
  }
  if (!require_writable_buffer(env, buffer)) return 0;
  void *address = (*env)->GetDirectBufferAddress(env, buffer);
  jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
  if (address == NULL || capacity < 0
      || (jlong)buffer_offset > capacity - (jlong)length) {
    throw_class(env, "java/lang/IllegalArgumentException", "direct read buffer");
    return 0;
  }

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return 0;
  uint32_t read = 0U;
  int status =
      m3fs_read(
          lease.fs,
          path,
          (uint64_t)offset,
          (unsigned char *)address + (size_t)(uint32_t)buffer_offset,
          (uint32_t)length,
          &read);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jint)read;
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeReadDirectSegments(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong offset,
    jobjectArray buffers) {
  (void)type;
  if (offset < 0 || buffers == NULL) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "scatter/gather read geometry");
    return 0;
  }
  jsize count = (*env)->GetArrayLength(env, buffers);
  if ((*env)->ExceptionCheck(env)) return 0;
  if (count < 0 || count > (jsize)M3FS_MAX_READ_SEGMENTS) {
    throw_class(env, "java/lang/IllegalArgumentException", "scatter/gather segment budget");
    return 0;
  }

  m3fs_mut_iovec *segments = count == 0
      ? NULL
      : (m3fs_mut_iovec *)calloc((size_t)count, sizeof(*segments));
  if (count != 0 && segments == NULL) {
    throw_class(env, "java/lang/OutOfMemoryError", "scatter/gather descriptor allocation");
    return 0;
  }

  for (jsize index = 0; index < count; ++index) {
    jobject buffer = (*env)->GetObjectArrayElement(env, buffers, index);
    if ((*env)->ExceptionCheck(env)) {
      free(segments);
      return 0;
    }
    if (buffer == NULL) {
      free(segments);
      throw_class(env, "java/lang/NullPointerException", "scatter/gather segment");
      return 0;
    }
    if (!require_writable_buffer(env, buffer)) {
      (*env)->DeleteLocalRef(env, buffer);
      free(segments);
      return 0;
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    void *address = (*env)->GetDirectBufferAddress(env, buffer);
    (*env)->DeleteLocalRef(env, buffer);
    if (capacity < 0 || capacity > (jlong)UINT32_MAX
        || (capacity != 0 && address == NULL)) {
      free(segments);
      throw_class(env, "java/lang/IllegalArgumentException",
          "scatter/gather requires exact writable direct-buffer slices");
      return 0;
    }
    segments[index].data = address;
    segments[index].length = (uint32_t)capacity;
  }

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) {
    free(segments);
    return 0;
  }
  uint64_t read = 0U;
  int status = m3fs_readv(
      lease.fs, path, (uint64_t)offset, segments, (uint32_t)count, &read);
  release_fs(&lease);
  free(segments);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jlong)read;
}

static jlongArray make_stat(JNIEnv *env, const m3fs_stat *stat) {
  jlong values[8];
  values[0] = (jlong)stat->inode;
  values[1] = (jlong)stat->size;
  values[2] = (jlong)stat->generation;
  values[3] = (jlong)stat->mtime_ns;
  values[4] = (jlong)stat->kind;
  values[5] = (jlong)stat->mode;
  values[6] = (jlong)stat->nlink;
  values[7] = (jlong)stat->block_size;
  jlongArray result = (*env)->NewLongArray(env, 8);
  if (result != NULL) (*env)->SetLongArrayRegion(env, result, 0, 8, values);
  return result;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_fs_M3FsNative_nativeStat(
    JNIEnv *env, jclass type, jlong handle, jstring path_value) {
  (void)type;
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return NULL;
  m3fs_stat stat;
  int status = m3fs_stat_path(lease.fs, path, &stat);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return NULL;
  }
  free(path);
  return make_stat(env, &stat);
}

typedef struct list_buffer {
  unsigned char *data;
  size_t length;
  size_t capacity;
  int status;
} list_buffer;

static void put_u32(unsigned char *target, uint32_t value) {
  target[0] = (unsigned char)(value & UINT32_C(0xff));
  target[1] = (unsigned char)((value >> 8) & UINT32_C(0xff));
  target[2] = (unsigned char)((value >> 16) & UINT32_C(0xff));
  target[3] = (unsigned char)((value >> 24) & UINT32_C(0xff));
}

static void put_u64(unsigned char *target, uint64_t value) {
  for (uint32_t index = 0U; index < 8U; ++index) {
    target[index] = (unsigned char)((value >> (index * 8U)) & UINT64_C(0xff));
  }
}

static int list_collect(
    void *context,
    const char *name,
    uint32_t name_bytes,
    const m3fs_stat *stat) {
  list_buffer *buffer = (list_buffer *)context;
  const size_t fixed = 4U + 4U * 4U + 8U * 4U;
  size_t needed = fixed + name_bytes;
  if (needed > (size_t)INT_MAX || buffer->length > (size_t)INT_MAX - needed) {
    buffer->status = M3FS_NOSPACE;
    return buffer->status;
  }
  size_t required = buffer->length + needed;
  if (required > buffer->capacity) {
    size_t next = buffer->capacity == 0U ? 1024U : buffer->capacity;
    while (next < required) {
      if (next > (size_t)INT_MAX / 2U) {
        next = required;
        break;
      }
      next *= 2U;
    }
    unsigned char *grown = (unsigned char *)realloc(buffer->data, next);
    if (grown == NULL) {
      buffer->status = M3FS_NOMEM;
      return buffer->status;
    }
    buffer->data = grown;
    buffer->capacity = next;
  }
  unsigned char *out = buffer->data + buffer->length;
  put_u32(out, name_bytes);
  out += 4U;
  memcpy(out, name, name_bytes);
  out += name_bytes;
  put_u32(out, stat->kind); out += 4U;
  put_u32(out, stat->mode); out += 4U;
  put_u32(out, stat->nlink); out += 4U;
  put_u32(out, stat->block_size); out += 4U;
  put_u64(out, stat->inode); out += 8U;
  put_u64(out, stat->size); out += 8U;
  put_u64(out, stat->generation); out += 8U;
  put_u64(out, stat->mtime_ns);
  buffer->length = required;
  return M3FS_OK;
}

JNIEXPORT jbyteArray JNICALL
Java_com_m3_fs_M3FsNative_nativeList(
    JNIEnv *env, jclass type, jlong handle, jstring path_value) {
  (void)type;
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return NULL;
  list_buffer buffer = {NULL, 0U, 0U, M3FS_OK};
  int status = m3fs_list(lease.fs, path, list_collect, &buffer);
  release_fs(&lease);
  if (status == M3FS_OK && buffer.status != M3FS_OK) status = buffer.status;
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    free(buffer.data);
    return NULL;
  }
  free(path);
  jbyteArray result = (*env)->NewByteArray(env, (jsize)buffer.length);
  if (result != NULL && buffer.length != 0U) {
    (*env)->SetByteArrayRegion(
        env, result, 0, (jsize)buffer.length, (const jbyte *)buffer.data);
  }
  free(buffer.data);
  return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_m3_fs_M3FsNative_nativeReadlink(
    JNIEnv *env, jclass type, jlong handle, jstring path_value) {
  (void)type;
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return NULL;
  char target[M3FS_MAX_PATH_BYTES];
  uint32_t bytes = 0U;
  int status =
      m3fs_readlink(
          lease.fs, path, target, M3FS_MAX_PATH_BYTES, &bytes);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return NULL;
  }
  free(path);
  jbyteArray result = (*env)->NewByteArray(env, (jsize)bytes);
  if (result != NULL && bytes != 0U) {
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)bytes, (const jbyte *)target);
  }
  return result;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_fs_M3FsNative_nativeStats(
    JNIEnv *env, jclass type, jlong handle) {
  (void)type;
  m3_jni_lease lease;
  if (!acquire_fs(env, handle, &lease)) return NULL;
  m3fs_stats stats;
  int status = m3fs_get_stats(lease.fs, &stats);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, NULL);
    return NULL;
  }
  jlong values[6] = {
      (jlong)stats.inode_count,
      (jlong)stats.dirent_count,
      (jlong)stats.logical_bytes,
      (jlong)stats.unique_blocks,
      (jlong)stats.block_references,
      (jlong)stats.physical_block_bytes};
  jlongArray result = (*env)->NewLongArray(env, 6);
  if (result != NULL) (*env)->SetLongArrayRegion(env, result, 0, 6, values);
  return result;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_fs_M3FsNative_nativeScrub(
    JNIEnv *env, jclass type, jlong handle, jboolean backfill) {
  (void)type;
  m3_jni_lease lease;
  if (!acquire_fs(env, handle, &lease)) return NULL;
  m3fs_scrub_report report;
  uint32_t flags =
      backfill == JNI_TRUE ? M3FS_SCRUB_BACKFILL_MISSING : 0U;
  int status = m3fs_scrub(lease.fs, flags, &report);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, NULL);
    return NULL;
  }
  jlong values[5] = {
      (jlong)report.blocks_scanned,
      (jlong)report.checksums_verified,
      (jlong)report.checksums_missing,
      (jlong)report.checksums_mismatched,
      (jlong)report.checksums_backfilled};
  jlongArray result = (*env)->NewLongArray(env, 5);
  if (result != NULL) (*env)->SetLongArrayRegion(env, result, 0, 5, values);
  return result;
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeVerify(
    JNIEnv *env, jclass type, jlong handle) {
  (void)type;
  m3_jni_lease lease;
  if (!acquire_fs(env, handle, &lease)) return;
  int status = m3fs_verify(lease.fs);
  release_fs(&lease);
  if (status != M3FS_OK) throw_status(env, status, NULL);
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeCheckpoint(
    JNIEnv *env, jclass type, jlong handle) {
  (void)type;
  m3_jni_lease lease;
  if (!acquire_fs(env, handle, &lease)) return;
  int status = m3fs_checkpoint(lease.fs);
  release_fs(&lease);
  if (status != M3FS_OK) throw_status(env, status, NULL);
}


JNIEXPORT jint JNICALL
Java_com_m3_fs_M3FsNative_nativeCapabilities(
    JNIEnv *env, jclass type) {
  (void)env;
  (void)type;
  return (jint)m3fs_capabilities();
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeCloneFile(
    JNIEnv *env, jclass type, jlong handle, jstring from_value, jstring to_value) {
  (void)type;
  char *from = NULL, *to = NULL;
  if (!utf8_encode(env, from_value, M3FS_MAX_PATH_BYTES, &from)) return 0;
  if (!utf8_encode(env, to_value, M3FS_MAX_PATH_BYTES, &to)) {
    free(from);
    return 0;
  }
  m3_jni_lease lease;
  if (!acquire_fs(env, handle, &lease)) {
    free(from);
    free(to);
    return 0;
  }
  uint64_t inode = 0U;
  int status = m3fs_clone_file(lease.fs, from, to, &inode);
  release_fs(&lease);
  if (status != M3FS_OK) throw_status(env, status, to);
  free(from);
  free(to);
  return (jlong)inode;
}

JNIEXPORT jbyteArray JNICALL
Java_com_m3_fs_M3FsNative_nativeListPage(
    JNIEnv *env, jclass type, jlong handle, jstring path_value,
    jstring after_value, jint limit) {
  (void)type;
  if (limit < 1 || limit > (jint)M3FS_MAX_PAGE_ENTRIES) {
    throw_class(env, "java/lang/IllegalArgumentException", "directory page limit");
    return NULL;
  }
  char *after = NULL;
  if (after_value != NULL
      && !utf8_encode(env, after_value, M3FS_MAX_NAME_BYTES, &after)) return NULL;
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) {
    free(after);
    return NULL;
  }
  /* Reserve exactly the worst-case wire size. The row callback cannot exceed this bound. */
  size_t capacity = 8U + (size_t)(uint32_t)limit * (52U + M3FS_MAX_NAME_BYTES);
  list_buffer buffer = {NULL, 8U, capacity, M3FS_OK};
  buffer.data = (unsigned char *)malloc(capacity);
  uint32_t count = 0U, more = 0U;
  int status = buffer.data == NULL ? M3FS_NOMEM
      : m3fs_list_page(lease.fs, path, after, (uint32_t)limit,
          list_collect, &buffer, &count, &more);
  release_fs(&lease);
  free(after);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    free(buffer.data);
    return NULL;
  }
  free(path);
  put_u32(buffer.data, count);
  put_u32(buffer.data + 4U, more);
  jbyteArray result = (*env)->NewByteArray(env, (jsize)buffer.length);
  if (result != NULL) {
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)buffer.length,
        (const jbyte *)buffer.data);
  }
  free(buffer.data);
  return result;
}

static jlongArray semantic_long_array(JNIEnv *env, const jlong *values, jsize count) {
  jlongArray result = (*env)->NewLongArray(env, count);
  if (result != NULL && count != 0) {
    (*env)->SetLongArrayRegion(env, result, 0, count, values);
  }
  return result;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_fs_M3FsNative_nativeMIndexInfo(
    JNIEnv *env, jclass type, jlong handle, jstring path_value) {
  (void)type;
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return NULL;
  m3fs_mindex_info_data info;
  int status = m3fs_mindex_info(lease.fs, path, &info);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return NULL;
  }
  free(path);
  jlong values[4];
  values[0] = (jlong)info.value_count;
  values[1] = (jlong)info.token_count;
  values[2] = (jlong)info.utf16_count;
  values[3] = (jlong)info.image_bytes;
  return semantic_long_array(env, values, 4);
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_fs_M3FsNative_nativeMIndexValueInfo(
    JNIEnv *env, jclass type, jlong handle, jstring path_value, jint value) {
  (void)type;
  if (value < 0) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "MIndex value");
    return NULL;
  }
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return NULL;
  m3fs_mindex_value info;
  int status =
      m3fs_mindex_value_info(
          lease.fs, path, (uint32_t)value, &info);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return NULL;
  }
  free(path);
  jlong values[4];
  values[0] = (jlong)info.token_count;
  values[1] = (jlong)info.utf16_length;
  values[2] = (jlong)info.java_hash;
  values[3] = (jlong)info.signal64;
  return semantic_long_array(env, values, 4);
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeMIndexToken(
    JNIEnv *env, jclass type, jlong handle, jstring path_value,
    jint value, jint ordinal) {
  (void)type;
  if (value < 0 || ordinal < 0) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "MIndex token coordinate");
    return 0;
  }
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return 0;
  uint64_t token = UINT64_C(0);
  int status =
      m3fs_mindex_token(
          lease.fs, path, (uint32_t)value, (uint32_t)ordinal, &token);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jlong)token;
}

JNIEXPORT jint JNICALL
Java_com_m3_fs_M3FsNative_nativeMIndexChar(
    JNIEnv *env, jclass type, jlong handle, jstring path_value,
    jint value, jint ordinal) {
  (void)type;
  if (value < 0 || ordinal < 0) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "MIndex UTF-16 coordinate");
    return 0;
  }
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return 0;
  uint16_t unit = 0U;
  int status =
      m3fs_mindex_char(
          lease.fs, path, (uint32_t)value, (uint32_t)ordinal, &unit);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jint)unit;
}

JNIEXPORT jint JNICALL
Java_com_m3_fs_M3FsNative_nativeMIndexCompare(
    JNIEnv *env, jclass type, jlong handle, jstring path_value,
    jint left, jint right) {
  (void)type;
  if (left < 0 || right < 0) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "MIndex compare coordinate");
    return 0;
  }
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return 0;
  int32_t comparison = 0;
  int status =
      m3fs_mindex_compare(
          lease.fs, path, (uint32_t)left, (uint32_t)right, &comparison);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jint)comparison;
}

JNIEXPORT jint JNICALL
Java_com_m3_fs_M3FsNative_nativeMIndexTokenIndexOf(
    JNIEnv *env, jclass type, jlong handle, jstring path_value,
    jint value, jlongArray needle_array, jint from_token) {
  (void)type;
  if (value < 0 || from_token < 0 || needle_array == NULL) {
    throw_class(env, "java/lang/IllegalArgumentException", "MIndex token search");
    return -1;
  }
  jsize count = (*env)->GetArrayLength(env, needle_array);
  uint64_t *needle = NULL;
  if (count != 0) {
    needle = (uint64_t *)malloc((size_t)count * sizeof(uint64_t));
    if (needle == NULL) {
      throw_class(env, "java/lang/OutOfMemoryError", "MIndex token needle");
      return -1;
    }
    (*env)->GetLongArrayRegion(env, needle_array, 0, count, (jlong *)needle);
    if ((*env)->ExceptionCheck(env)) {
      free(needle);
      return -1;
    }
  }

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) {
    free(needle);
    return -1;
  }
  int32_t index = -1;
  int status =
      m3fs_mindex_token_index_of(
          lease.fs, path, (uint32_t)value, needle, (uint32_t)count,
          (uint32_t)from_token, &index);
  release_fs(&lease);
  free(needle);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return -1;
  }
  free(path);
  return (jint)index;
}

static void *semantic_copy_bytes(JNIEnv *env, jbyteArray input, jsize expected) {
  if (input == NULL || (*env)->GetArrayLength(env, input) != expected) return NULL;
  if (expected == 0) return NULL;
  void *copy = malloc((size_t)expected);
  if (copy == NULL) return NULL;
  (*env)->GetByteArrayRegion(env, input, 0, expected, (jbyte *)copy);
  if ((*env)->ExceptionCheck(env)) {
    free(copy);
    return NULL;
  }
  return copy;
}

static int32_t *semantic_copy_ints(JNIEnv *env, jintArray input, jsize expected) {
  if (input == NULL || (*env)->GetArrayLength(env, input) != expected) return NULL;
  if (expected == 0) return NULL;
  int32_t *copy = (int32_t *)malloc((size_t)expected * sizeof(int32_t));
  if (copy == NULL) return NULL;
  (*env)->GetIntArrayRegion(env, input, 0, expected, (jint *)copy);
  if ((*env)->ExceptionCheck(env)) {
    free(copy);
    return NULL;
  }
  return copy;
}

static uint64_t *semantic_copy_longs(JNIEnv *env, jlongArray input, jsize expected) {
  if (input == NULL || (*env)->GetArrayLength(env, input) != expected) return NULL;
  if (expected == 0) return NULL;
  uint64_t *copy = (uint64_t *)malloc((size_t)expected * sizeof(uint64_t));
  if (copy == NULL) return NULL;
  (*env)->GetLongArrayRegion(env, input, 0, expected, (jlong *)copy);
  if ((*env)->ExceptionCheck(env)) {
    free(copy);
    return NULL;
  }
  return copy;
}

static uint16_t *semantic_copy_chars(JNIEnv *env, jcharArray input, jsize expected) {
  if (input == NULL || (*env)->GetArrayLength(env, input) != expected) return NULL;
  if (expected == 0) return NULL;
  uint16_t *copy = (uint16_t *)malloc((size_t)expected * sizeof(uint16_t));
  if (copy == NULL) return NULL;
  (*env)->GetCharArrayRegion(env, input, 0, expected, (jchar *)copy);
  if ((*env)->ExceptionCheck(env)) {
    free(copy);
    return NULL;
  }
  return copy;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_fs_M3FsNative_nativeRegexMatchAll(
    JNIEnv *env, jclass type, jlong handle, jstring path_value, jint mode,
    jbyteArray op_array, jintArray out1_array, jintArray out2_array,
    jintArray class_array, jlongArray ascii_low_array, jlongArray ascii_high_array,
    jintArray range_offset_array, jintArray range_count_array,
    jcharArray range_low_array, jcharArray range_high_array,
    jint start_state, jint match_state, jint min_length, jint max_length,
    jlong required_presence64, jboolean may_consume_surrogate) {
  (void)type;
  if (op_array == NULL || out1_array == NULL || out2_array == NULL
      || class_array == NULL || ascii_low_array == NULL || ascii_high_array == NULL
      || range_offset_array == NULL || range_count_array == NULL
      || range_low_array == NULL || range_high_array == NULL) {
    throw_class(env, "java/lang/NullPointerException", "MIndex regex program");
    return NULL;
  }

  jsize states = (*env)->GetArrayLength(env, op_array);
  jsize classes = (*env)->GetArrayLength(env, ascii_low_array);
  jsize ranges = (*env)->GetArrayLength(env, range_low_array);
  if (states <= 0 || states > 1000000
      || classes < 0 || classes > 1000000
      || ranges < 0 || ranges > 16000000
      || (*env)->GetArrayLength(env, out1_array) != states
      || (*env)->GetArrayLength(env, out2_array) != states
      || (*env)->GetArrayLength(env, class_array) != states
      || (*env)->GetArrayLength(env, ascii_high_array) != classes
      || (*env)->GetArrayLength(env, range_offset_array) != classes
      || (*env)->GetArrayLength(env, range_count_array) != classes
      || (*env)->GetArrayLength(env, range_high_array) != ranges) {
    throw_class(env, "java/lang/IllegalArgumentException", "MIndex regex geometry");
    return NULL;
  }

  uint8_t *op = (uint8_t *)semantic_copy_bytes(env, op_array, states);
  int32_t *out1 = semantic_copy_ints(env, out1_array, states);
  int32_t *out2 = semantic_copy_ints(env, out2_array, states);
  int32_t *class_id = semantic_copy_ints(env, class_array, states);
  uint64_t *ascii_low = semantic_copy_longs(env, ascii_low_array, classes);
  uint64_t *ascii_high = semantic_copy_longs(env, ascii_high_array, classes);
  int32_t *range_offset = semantic_copy_ints(env, range_offset_array, classes);
  int32_t *range_count = semantic_copy_ints(env, range_count_array, classes);
  uint16_t *range_low = semantic_copy_chars(env, range_low_array, ranges);
  uint16_t *range_high = semantic_copy_chars(env, range_high_array, ranges);
  if ((*env)->ExceptionCheck(env)
      || op == NULL || out1 == NULL || out2 == NULL || class_id == NULL
      || (classes != 0 && (ascii_low == NULL || ascii_high == NULL
          || range_offset == NULL || range_count == NULL))
      || (ranges != 0 && (range_low == NULL || range_high == NULL))) {
    free(op); free(out1); free(out2); free(class_id);
    free(ascii_low); free(ascii_high); free(range_offset); free(range_count);
    free(range_low); free(range_high);
    if (!(*env)->ExceptionCheck(env)) {
      throw_class(env, "java/lang/OutOfMemoryError", "MIndex regex program");
    }
    return NULL;
  }

  m3fs_regex_program program;
  program.op = op;
  program.out1 = out1;
  program.out2 = out2;
  program.class_id = class_id;
  program.state_count = (uint32_t)states;
  program.ascii_low = ascii_low;
  program.ascii_high = ascii_high;
  program.range_offset = range_offset;
  program.range_count = range_count;
  program.class_count = (uint32_t)classes;
  program.range_low = range_low;
  program.range_high = range_high;
  program.range_count_total = (uint32_t)ranges;
  program.start_state = (uint32_t)start_state;
  program.match_state = (uint32_t)match_state;
  program.min_length = (int32_t)min_length;
  program.max_length = (int32_t)max_length;
  program.required_presence64 = (uint64_t)required_presence64;
  program.may_consume_surrogate_unit = may_consume_surrogate != JNI_FALSE ? 1U : 0U;

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) {
    free(op); free(out1); free(out2); free(class_id);
    free(ascii_low); free(ascii_high); free(range_offset); free(range_count);
    free(range_low); free(range_high);
    return NULL;
  }

  uint32_t value_count = 0U;
  uint32_t words = 0U;
  int status =
      m3fs_regex_match_all(
          lease.fs, path, &program, (uint32_t)mode,
          NULL, NULL, 0U, &value_count, &words);
  uint64_t *match_words = NULL;
  uint64_t *unsupported_words = NULL;
  for (int attempt = 0; status == M3FS_NOSPACE && attempt < 4; ++attempt) {
    if (words == 0U) {
      status =
          m3fs_regex_match_all(
              lease.fs, path, &program, (uint32_t)mode,
              NULL, NULL, 0U, &value_count, &words);
      continue;
    }
#if SIZE_MAX < UINT64_MAX
    if ((size_t)words > SIZE_MAX / sizeof(uint64_t)) {
      status = M3FS_NOSPACE;
      break;
    }
#endif
    uint64_t *next_match = (uint64_t *)realloc(
        match_words, (size_t)words * sizeof(uint64_t));
    uint64_t *next_unsupported = (uint64_t *)realloc(
        unsupported_words, (size_t)words * sizeof(uint64_t));
    if (next_match == NULL || next_unsupported == NULL) {
      if (next_match != NULL) match_words = next_match;
      if (next_unsupported != NULL) unsupported_words = next_unsupported;
      status = M3FS_NOMEM;
      break;
    }
    match_words = next_match;
    unsupported_words = next_unsupported;
    uint32_t capacity = words;
    status =
        m3fs_regex_match_all(
            lease.fs, path, &program, (uint32_t)mode,
            match_words, unsupported_words, capacity, &value_count, &words);
  }
  release_fs(&lease);

  free(op); free(out1); free(out2); free(class_id);
  free(ascii_low); free(ascii_high); free(range_offset); free(range_count);
  free(range_low); free(range_high);

  if (status != M3FS_OK) {
    free(match_words); free(unsupported_words);
    throw_status(env, status, path);
    free(path);
    return NULL;
  }
  free(path);

  if (words > (uint32_t)(INT_MAX / 2 - 2)) {
    free(match_words); free(unsupported_words);
    throw_class(env, "java/lang/IllegalStateException", "MIndex regex bitmap too large");
    return NULL;
  }
  jsize result_count = (jsize)(2U + words * 2U);
  jlongArray result = (*env)->NewLongArray(env, result_count);
  if (result != NULL) {
    jlong *encoded = (jlong *)calloc((size_t)result_count, sizeof(jlong));
    if (encoded == NULL) {
      free(match_words); free(unsupported_words);
      throw_class(env, "java/lang/OutOfMemoryError", "MIndex regex result");
      return NULL;
    }
    encoded[0] = (jlong)value_count;
    encoded[1] = (jlong)words;
    for (uint32_t word = 0U; word < words; ++word) {
      encoded[2U + word] = (jlong)match_words[word];
      encoded[2U + words + word] = (jlong)unsupported_words[word];
    }
    (*env)->SetLongArrayRegion(env, result, 0, result_count, encoded);
    free(encoded);
  }
  free(match_words); free(unsupported_words);
  return result;
}

typedef struct m3_jni_ranges {
  jlong *values;
  uint32_t pairs;
  uint32_t capacity;
  int failed;
} m3_jni_ranges;

static int collect_range(
    void *context,
    uint64_t offset,
    uint64_t length) {
  m3_jni_ranges *ranges = (m3_jni_ranges *)context;
  if (ranges == NULL || length == 0U || ranges->failed) return M3FS_INVALID;
  if (offset > (uint64_t)INT64_MAX || length > (uint64_t)INT64_MAX) {
    ranges->failed = 1;
    return M3FS_INVALID;
  }
  if (ranges->pairs == ranges->capacity) {
    uint32_t next = ranges->capacity == 0U ? 16U : ranges->capacity * 2U;
    if (next < ranges->capacity || next > (uint32_t)INT_MAX / 2U) {
      ranges->failed = 1;
      return M3FS_NOSPACE;
    }
    jlong *grown =
        (jlong *)realloc(ranges->values, (size_t)next * 2U * sizeof(jlong));
    if (grown == NULL) {
      ranges->failed = 1;
      return M3FS_NOMEM;
    }
    ranges->values = grown;
    ranges->capacity = next;
  }
  ranges->values[(size_t)ranges->pairs * 2U] = (jlong)offset;
  ranges->values[(size_t)ranges->pairs * 2U + 1U] = (jlong)length;
  ranges->pairs++;
  return M3FS_OK;
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativePunchHole(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong offset,
    jlong length) {
  (void)type;
  if (offset < 0 || length < 0) {
    throw_class(env, "java/lang/IllegalArgumentException", "hole range");
    return;
  }
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return;
  int status =
      m3fs_punch_hole(
          lease.fs, path, (uint64_t)offset, (uint64_t)length);
  release_fs(&lease);
  if (status != M3FS_OK) throw_status(env, status, path);
  free(path);
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_fs_M3FsNative_nativeAllocatedRanges(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong offset,
    jlong length) {
  (void)type;
  if (offset < 0 || length < 0) {
    throw_class(env, "java/lang/IllegalArgumentException", "allocated range");
    return NULL;
  }
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) return NULL;

  m3_jni_ranges ranges = {0};
  int status =
      m3fs_allocated_ranges(
          lease.fs,
          path,
          (uint64_t)offset,
          (uint64_t)length,
          collect_range,
          &ranges);
  release_fs(&lease);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    free(ranges.values);
    return NULL;
  }
  free(path);
  if (ranges.failed || ranges.pairs > (uint32_t)INT_MAX / 2U) {
    free(ranges.values);
    throw_class(env, "java/lang/IllegalStateException", "invalid allocated-range result");
    return NULL;
  }

  jsize cells = (jsize)(ranges.pairs * 2U);
  jlongArray result = (*env)->NewLongArray(env, cells);
  if (result != NULL && cells != 0) {
    (*env)->SetLongArrayRegion(env, result, 0, cells, ranges.values);
  }
  free(ranges.values);
  return result;
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeFeatureBits(JNIEnv *env, jclass type) {
  (void)env; (void)type;
  return (jlong)m3fs_feature_bits();
}

JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeSetMetadata(
    JNIEnv *env, jclass type, jlong handle, jstring value, jint mode, jlong mtime,
    jboolean set_mode, jboolean set_time) {
  (void)type;
  if ((set_mode != JNI_FALSE && (mode < 0 || mode > 07777))
      || (set_time != JNI_FALSE && mtime < 0)) {
    throw_class(env, "java/lang/IllegalArgumentException", "metadata geometry");
    return;
  }
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, value, &lease, &path)) return;
  uint32_t flags = (set_mode != JNI_FALSE ? M3FS_METADATA_MODE : 0U)
      | (set_time != JNI_FALSE ? M3FS_METADATA_MTIME : 0U);
  int status = m3fs_set_metadata(lease.fs, path, (uint32_t)mode, (uint64_t)mtime, flags);
  release_fs(&lease);
  if (status != M3FS_OK) throw_status(env, status, path);
  free(path);
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeReplaceImage(
    JNIEnv *env, jclass type, jlong handle, jstring value, jobject data) {
  (void)type;
  if (data == NULL) {
    throw_class(env, "java/lang/NullPointerException", "image");
    return 0;
  }
  jlong length = (*env)->GetDirectBufferCapacity(env, data);
  const void *bytes = (*env)->GetDirectBufferAddress(env, data);
  if (length < 0 || (uint64_t)length > UINT32_MAX || (length != 0 && bytes == NULL)) {
    throw_class(env, "java/lang/IllegalArgumentException", "direct image geometry");
    return 0;
  }
  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, value, &lease, &path)) return 0;
  uint64_t size = 0U;
  int status = m3fs_replace(lease.fs, path, bytes, (uint32_t)length, &size);
  release_fs(&lease);
  if (status != M3FS_OK) throw_status(env, status, path);
  free(path);
  return status == M3FS_OK ? (jlong)size : 0;
}

JNIEXPORT jlong JNICALL
Java_com_m3_fs_M3FsNative_nativeWriteIfInode(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong expected_inode,
    jlong offset,
    jbyteArray data,
    jint data_offset,
    jint length,
    jint flags) {
  (void)type;
  if (expected_inode <= 0 || flags < 0 || flags > 2 || offset < 0 || data == NULL || data_offset < 0 || length < 0
      || data_offset > (*env)->GetArrayLength(env, data) - length) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "write geometry");
    return 0;
  }
  unsigned char *copy = NULL;
  if (length != 0) {
    copy = (unsigned char *)malloc((size_t)length);
    if (copy == NULL) {
      throw_class(env, "java/lang/OutOfMemoryError", "write copy");
      return 0;
    }
    (*env)->GetByteArrayRegion(env, data, data_offset, length, (jbyte *)copy);
    if ((*env)->ExceptionCheck(env)) {
      free(copy);
      return 0;
    }
  }

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) {
    free(copy);
    return 0;
  }
  uint64_t size = 0U;
  int status =
      m3fs_write_if_inode(
          lease.fs, path, (uint64_t)expected_inode, (uint64_t)offset, copy,
          (uint32_t)length, (uint32_t)flags, &size);
  release_fs(&lease);
  free(copy);
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jlong)size;
}

JNIEXPORT jint JNICALL
Java_com_m3_fs_M3FsNative_nativeReadIfInode(
    JNIEnv *env,
    jclass type,
    jlong handle,
    jstring path_value,
    jlong expected_inode,
    jlong offset,
    jbyteArray output,
    jint output_offset,
    jint length) {
  (void)type;
  if (expected_inode <= 0 || offset < 0 || output == NULL || output_offset < 0 || length < 0
      || output_offset > (*env)->GetArrayLength(env, output) - length) {
    throw_class(env, "java/lang/IndexOutOfBoundsException", "read geometry");
    return 0;
  }
  unsigned char *copy = NULL;
  if (length != 0) {
    copy = (unsigned char *)malloc((size_t)length);
    if (copy == NULL) {
      throw_class(env, "java/lang/OutOfMemoryError", "read copy");
      return 0;
    }
  }

  m3_jni_lease lease;
  char *path = NULL;
  if (!with_path(env, handle, path_value, &lease, &path)) {
    free(copy);
    return 0;
  }
  uint32_t read = 0U;
  int status =
      m3fs_read_if_inode(
          lease.fs, path, (uint64_t)expected_inode, (uint64_t)offset, copy,
          (uint32_t)length, &read);
  release_fs(&lease);
  if (status == M3FS_OK && read != 0U) {
    (*env)->SetByteArrayRegion(env, output, output_offset, (jsize)read, (const jbyte *)copy);
  }
  free(copy);
  if ((*env)->ExceptionCheck(env)) {
    free(path);
    return 0;
  }
  if (status != M3FS_OK) {
    throw_status(env, status, path);
    free(path);
    return 0;
  }
  free(path);
  return (jint)read;
}


JNIEXPORT void JNICALL
Java_com_m3_fs_M3FsNative_nativeRenameNoReplace(
    JNIEnv *env, jclass type, jlong handle, jstring from, jstring to) {
  (void)type;
  binary_path_operation(env, handle, from, to, 4);
}
