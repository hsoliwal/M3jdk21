// SPDX-License-Identifier: Apache-2.0
#include <jni.h>
#include <cstdint>
#include <limits>

namespace {
void fail(JNIEnv* env, const char* type, const char* message) {
  jclass exception = env->FindClass(type);
  if (exception != nullptr) {
    env->ThrowNew(exception, message);
    env->DeleteLocalRef(exception);
  }
}
void invalid(JNIEnv* env, const char* message) {
  fail(env, "java/lang/IllegalArgumentException", message);
}

bool direct_segment(JNIEnv* env, jobject owner, const std::uint8_t*& bytes, jlong& size) {
  if (owner == nullptr) { invalid(env, "null UTF-16 segment"); return false; }
  size = env->GetDirectBufferCapacity(owner);
  bytes = static_cast<const std::uint8_t*>(env->GetDirectBufferAddress(owner));
  if (size < 0 || size % 2 != 0 || (size > 0 && bytes == nullptr)) {
    invalid(env, "UTF-16 segment must be a direct ByteBuffer with even capacity");
    return false;
  }
  return true;
}

// Validate BOTH full descriptor arrays before reading text, including tails after an early mismatch.
bool validate(JNIEnv* env, jobjectArray segments, jsize& count, jint& length) {
  if (segments == nullptr) { invalid(env, "segments required"); return false; }
  count = env->GetArrayLength(segments);
  if (env->ExceptionCheck()) return false;
  if (count > 4096) { invalid(env, "segment budget exceeded"); return false; }
  std::int64_t total = 0;
  for (jsize i = 0; i < count; ++i) {
    jobject owner = env->GetObjectArrayElement(segments, i);
    if (env->ExceptionCheck()) return false;
    const std::uint8_t* bytes = nullptr;
    jlong size = 0;
    const bool valid = direct_segment(env, owner, bytes, size);
    if (owner != nullptr) env->DeleteLocalRef(owner);
    if (!valid) return false;
    // Subtraction checks prevent overflow even for adversarial direct-buffer capacities.
    if (size / 2 > std::numeric_limits<jint>::max() - total) {
      fail(env, "java/lang/ArithmeticException", "UTF-16 length exceeds Java int range");
      return false;
    }
    total += size / 2;
  }
  length = static_cast<jint>(total);
  return true;
}

// At most one local buffer reference per cursor; all pointers die before this JNI call returns.
class Cursor {
  JNIEnv* env_;
  jobjectArray segments_;
  jsize count_;
  jsize next_ = 0;
  jobject owner_ = nullptr;
  const std::uint8_t* bytes_ = nullptr;
  jlong size_ = 0;
  jlong position_ = 0;
 public:
  Cursor(JNIEnv* env, jobjectArray segments, jsize count)
      : env_(env), segments_(segments), count_(count) {}
  Cursor(const Cursor&) = delete;
  Cursor& operator=(const Cursor&) = delete;
  ~Cursor() { if (owner_ != nullptr) env_->DeleteLocalRef(owner_); }
  bool read(jint& unit) {
    while (position_ == size_) {
      if (owner_ != nullptr) { env_->DeleteLocalRef(owner_); owner_ = nullptr; }
      if (next_ == count_) { invalid(env_, "descriptor array changed during comparison"); return false; }
      owner_ = env_->GetObjectArrayElement(segments_, next_++);
      if (env_->ExceptionCheck()) return false;
      if (!direct_segment(env_, owner_, bytes_, size_)) return false;
      position_ = 0;
    }
    unit = static_cast<jint>((std::uint32_t(bytes_[position_]) << 8U)
        | std::uint32_t(bytes_[position_ + 1]));
    position_ += 2;
    return true;
  }
};
}

extern "C" JNIEXPORT jint JNICALL
Java_com_synexia_indexstring_shared_SharedArrayNative_compareUtf16Segments(
    JNIEnv* env, jclass, jobjectArray left, jobjectArray right) {
  jsize left_count = 0, right_count = 0;
  jint left_length = 0, right_length = 0;
  if (!validate(env, left, left_count, left_length)
      || !validate(env, right, right_count, right_length)) return 0;
  Cursor a(env, left, left_count), b(env, right, right_count);
  const jint common = left_length < right_length ? left_length : right_length;
  for (jint i = 0; i < common; ++i) {
    jint x = 0, y = 0;
    if (!a.read(x) || !b.read(y)) return 0;
    if (x != y) return x - y;
  }
  return left_length - right_length;
}
