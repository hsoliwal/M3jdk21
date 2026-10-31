// SPDX-License-Identifier: Apache-2.0
#include <jni.h>
#include <jni/ownership.hpp>
#include <cstdint>
#include <cstring>
#include <limits>
#include <utility>

namespace {
// The Java range plan identifies the participating window. Keep full metadata preflight,
// then avoid revisiting unrelated owners during the copy pass.

// Own only references acquired in this native frame. Borrowed input arguments are never adopted.
// Mapbox's deleter performs DeleteLocalRef on scope exit, including pending-Java-exception exits.
// No NewGlobalRef, heap allocation, thread attachment, array pin or Java object field is added.
template<class T, class R>
jni::UniqueLocalRef<T> local(JNIEnv* env, R acquired) {
  return {jni::Wrap<T*>(acquired), {*env}};
}

void invalid(JNIEnv* env, const char* message) {
  auto type = local<jni::jclass>(env, env->FindClass("java/lang/IllegalArgumentException"));
  if (type != nullptr) {
    env->ThrowNew(jni::Unwrap(type.get()), message);
  }
}
}

extern "C" JNIEXPORT jint JNICALL
Java_com_synexia_indexstring_shared_SharedArrayNative_hash32Segments(
    JNIEnv* env, jclass, jobjectArray segments, jint width) {
  if (segments == nullptr || (width != 1 && width != 2)) {
    invalid(env, "segments required; width must be 1 or 2");
    return 0;
  }
  jsize count = env->GetArrayLength(segments);
  if (env->ExceptionCheck()) return 0;
  if (count > 4096) {
    invalid(env, "segment budget exceeded");
    return 0;
  }
  std::uint32_t hash = width == 1 ? 1U : 0U;
  for (jsize s = 0; s < count; ++s) {
    auto buffer = local<jni::jobject>(env, env->GetObjectArrayElement(segments, s));
    if (env->ExceptionCheck()) return 0;
    if (buffer == nullptr) {
      invalid(env, "null segment");
      return 0;
    }
    jlong size = env->GetDirectBufferCapacity(jni::Unwrap(buffer.get()));
    const auto* bytes = static_cast<const std::uint8_t*>(env->GetDirectBufferAddress(jni::Unwrap(buffer.get())));
    if (size < 0 || size % width != 0 || (size > 0 && bytes == nullptr)) {
      invalid(env, "segment must be a direct, aligned-width ByteBuffer slice");
      return 0;
    }
    // ByteBuffer references remain rooted throughout this call. No pointers are retained or written.
    for (jlong i = 0; i < size; i += width) {
      std::uint32_t unit;
      if (width == 1) {
        const std::int32_t signed_byte = bytes[i] < 128 ? bytes[i] : std::int32_t(bytes[i]) - 256;
        unit = static_cast<std::uint32_t>(signed_byte);
      } else {
        unit = (std::uint32_t(bytes[i]) << 8U) | std::uint32_t(bytes[i + 1]);
      }
      hash = 31U * hash + unit; // Unsigned overflow is defined, matching Java's low 32 bits.
    }
  }
  static_assert(sizeof(jint) == sizeof(hash));
  jint result;
  std::memcpy(&result, &hash, sizeof(result));
  return result;
}


namespace {
void metric_overflow(JNIEnv* env) {
  auto type = local<jni::jclass>(env, env->FindClass("java/lang/ArithmeticException"));
  if (type != nullptr) {
    env->ThrowNew(jni::Unwrap(type.get()), "UTF-16 metric exceeds Java int range");
  }
}

bool utf16_segment(JNIEnv* env, const jni::UniqueLocalRef<jni::jobject>& owner,
                   const std::uint8_t*& bytes, jlong& size) {
  if (env->ExceptionCheck()) return false;
  if (owner == nullptr) {
    invalid(env, "null UTF-16 segment");
    return false;
  }
  size = env->GetDirectBufferCapacity(jni::Unwrap(owner.get()));
  bytes = static_cast<const std::uint8_t*>(env->GetDirectBufferAddress(jni::Unwrap(owner.get())));
  if (size < 0 || size % 2 != 0 || (size > 0 && bytes == nullptr)) {
    invalid(env, "UTF-16 segment must be a direct ByteBuffer with even capacity");
    return false;
  }
  return true;
}

jint signed_bits(std::uint32_t bits) {
  jint result;
  static_assert(sizeof(result) == sizeof(bits));
  std::memcpy(&result, &bits, sizeof(result));
  return result;
}
}

namespace {
// Invocation-local facts, following IndexTextMetrics.combine; no character payload is retained.
struct utf16_summary {
  std::int64_t utf8 = 0, points = 0, unpaired = 0;
  std::uint32_t hash = 0, power = 1;
  jint first = -1, last = -1;
};

utf16_summary scan_utf16_summary(const std::uint8_t* bytes, jlong size, jint replacement_bytes) {
  utf16_summary result;
  bool previous_high = false;
  for (jlong p = 0; p < size; p += 2) {
    const std::uint32_t unit = (std::uint32_t(bytes[p]) << 8U) | std::uint32_t(bytes[p + 1]);
    const bool high = unit >= 0xd800U && unit <= 0xdbffU;
    const bool low = unit >= 0xdc00U && unit <= 0xdfffU;
    if (result.first < 0) result.first = static_cast<jint>(unit);
    result.last = static_cast<jint>(unit);
    result.hash = 31U * result.hash + unit;
    result.power *= 31U;
    ++result.points;
    if (high || low) { result.utf8 += replacement_bytes; ++result.unpaired; }
    else result.utf8 += unit <= 0x7fU ? 1 : unit <= 0x7ffU ? 2 : 3;
    if (previous_high && low) {
      --result.points; result.unpaired -= 2; result.utf8 += 4 - 2 * replacement_bytes;
    }
    previous_high = high;
  }
  return result;
}

void append_utf16_summary(utf16_summary& total, const utf16_summary& part, jint replacement_bytes) {
  if (part.first < 0) return;
  if (total.first < 0) { total = part; return; }
  total.utf8 += part.utf8; total.points += part.points; total.unpaired += part.unpaired;
  if (total.last >= 0xd800 && total.last <= 0xdbff && part.first >= 0xdc00 && part.first <= 0xdfff) {
    --total.points; total.unpaired -= 2; total.utf8 += 4 - 2 * replacement_bytes;
  }
  total.hash = total.hash * part.power + part.hash;
  total.power *= part.power;
  total.last = part.last;
}

// Eight exact, invocation-local range summaries. FIFO eviction bounds retained metadata;
// a most-recent fast path preserves the earlier adjacent-repeat optimization.
// Keys are borrowed payload address AND byte length, never a hash or jobject address.
// Owners keep each address live. No entry, pointer, reference or fact survives this call.
struct utf16_cache {
  static constexpr jsize capacity = 8;
  struct entry {
    const std::uint8_t* bytes = nullptr;
    jlong size = -1;
    utf16_summary facts;
    jni::UniqueLocalRef<jni::jobject> owner;
  } entries[capacity];
  const jint replacement_bytes; // Encoding policy is fixed for this complete invocation.
  jsize used = 0, next = 0, recent = 0;

  explicit utf16_cache(jint replacement) : replacement_bytes(replacement) {}

  const utf16_summary& get(jni::UniqueLocalRef<jni::jobject>& owner,
                           const std::uint8_t* bytes, jlong size) {
    if (used != 0 && entries[recent].bytes == bytes && entries[recent].size == size) {
      return entries[recent].facts;
    }
    for (jsize index = 0; index < used; ++index) {
      if (entries[index].bytes == bytes && entries[index].size == size) {
        recent = index;
        return entries[index].facts;
      }
    }
    const jsize slot = used < capacity ? used++ : next;
    next = (slot + 1) % capacity;
    entry& saved = entries[slot];
    saved.facts = scan_utf16_summary(bytes, size, replacement_bytes);
    saved.owner = std::move(owner);
    saved.bytes = bytes;
    saved.size = size;
    recent = slot;
    return saved.facts;
  }
};
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_synexia_indexstring_shared_SharedArrayNative_scanUtf16Segments0(
    JNIEnv* env, jclass, jobjectArray segments, jint replacement_bytes) {
  if (segments == nullptr || replacement_bytes < 1 || replacement_bytes > 4) {
    invalid(env, "segments and a valid UTF-8 replacement width are required");
    return nullptr;
  }
  const jsize count = env->GetArrayLength(segments);
  if (env->ExceptionCheck()) return nullptr;
  if (count > 4096) {
    invalid(env, "segment budget exceeded");
    return nullptr;
  }
  constexpr std::int64_t max_int = std::numeric_limits<jint>::max();
  std::int64_t total_units = 0;
  // Validate every descriptor and total length BEFORE scanning possibly repeated large regions.
  for (jsize s = 0; s < count; ++s) {
    auto owner = local<jni::jobject>(env, env->GetObjectArrayElement(segments, s));
    const std::uint8_t* bytes = nullptr;
    jlong size = 0;
    if (!utf16_segment(env, owner, bytes, size)) return nullptr;
    total_units += size / 2;
    if (total_units > max_int) { metric_overflow(env); return nullptr; }
  }
  // Reserve before retaining extra locals. Failure preserves the pending JVM exception.
  if (env->EnsureLocalCapacity(utf16_cache::capacity + 4) < 0) return nullptr;
  utf16_summary total;
  utf16_cache cached(replacement_bytes);
  for (jsize s = 0; s < count; ++s) {
    auto owner = local<jni::jobject>(env, env->GetObjectArrayElement(segments, s));
    const std::uint8_t* bytes = nullptr;
    jlong size = 0;
    if (!utf16_segment(env, owner, bytes, size)) return nullptr;
    if (size == 0) continue; // Empty components neither break surrogate seams nor evict useful facts.
    // Complete summaries compose exactly; append still repairs every surrogate seam.
    append_utf16_summary(total, cached.get(owner, bytes, size), replacement_bytes);
  }
  // Keep wide local summaries until seam repair is complete; replacement widths 3/4 can subtract.
  if (total.utf8 > max_int || total.points > max_int || total.unpaired > max_int) {
    metric_overflow(env); return nullptr;
  }
  const jint result[] = {
      static_cast<jint>(total_units), static_cast<jint>(total.utf8), static_cast<jint>(total.points),
      static_cast<jint>(total.unpaired), signed_bits(total.hash), signed_bits(total.power), total.first, total.last};
  jintArray array = env->NewIntArray(8);
  if (array == nullptr) return nullptr;
  env->SetIntArrayRegion(array, 0, 8, result);
  return env->ExceptionCheck() ? nullptr : array;
}

// Explicit compatibility export, NOT the representation of a joined immutable value.
// Reuses utf16_segment and the same library/UTF-16BE contract as the existing hash/metric kernels.
extern "C" JNIEXPORT void JNICALL
Java_com_synexia_indexstring_shared_SharedArrayNative_copyUtf16SegmentsTo0(
    JNIEnv* env, jclass api, jobjectArray segments, jcharArray target,
    jint target_start, jint count, jobject monitor) {
  if (segments == nullptr || target == nullptr) {
    invalid(env, "segments and target required"); return;
  }
  const jsize target_length = env->GetArrayLength(target);
  if (env->ExceptionCheck()) return;
  if (target_start < 0 || count < 0 || target_start > target_length
      || count > target_length - target_start) {
    invalid(env, "invalid output range"); return;
  }
  const jsize segment_count = env->GetArrayLength(segments);
  if (env->ExceptionCheck()) return;
  if (segment_count > 4096) { invalid(env, "segment budget exceeded"); return; }

  // Complete descriptor/length preflight before the first write, even for zero requested units.
  jlong units = 0;
  for (jsize s = 0; s < segment_count; ++s) {
    auto owner = local<jni::jobject>(env, env->GetObjectArrayElement(segments, s));
    const std::uint8_t* bytes = nullptr;
    jlong size = 0;
    if (!utf16_segment(env, owner, bytes, size)) return;
    if (size / 2 > std::numeric_limits<jint>::max() - units) { metric_overflow(env); return; }
    units += size / 2;
  }
  if (units != count) { invalid(env, "descriptor lengths must equal the requested export"); return; }
  jmethodID checkpoint = env->GetStaticMethodID(api, "copyCheckpoint", "(Lcom/synexia/job/IProgressMonitor;)V");
  if (checkpoint == nullptr || env->ExceptionCheck()) return;
  env->CallStaticVoidMethod(api, checkpoint, monitor);
  if (env->ExceptionCheck()) return;

  // A fixed 2 KiB stack transfer block; no vector growth or array-elements pin/copy is needed.
  constexpr jint block_units = 1024;
  jchar block[block_units];
  jint written = 0;
  for (jsize s = 0; s < segment_count; ++s) {
    auto owner = local<jni::jobject>(env, env->GetObjectArrayElement(segments, s));
    const std::uint8_t* bytes = nullptr;
    jlong size = 0;
    if (!utf16_segment(env, owner, bytes, size)) return;
    for (jlong offset = 0; offset < size;) {
      env->CallStaticVoidMethod(api, checkpoint, monitor);
      if (env->ExceptionCheck()) { return; }
      const jlong remaining = (size - offset) / 2;
      const jint take = static_cast<jint>(remaining < block_units ? remaining : block_units);
      if (take > count - written) {
        invalid(env, "descriptor lengths changed during export"); return;
      }
      for (jint i = 0; i < take; ++i) {
        const jlong p = offset + 2 * static_cast<jlong>(i);
        block[i] = static_cast<jchar>((std::uint32_t(bytes[p]) << 8U) | std::uint32_t(bytes[p + 1]));
      }
      env->SetCharArrayRegion(target, target_start + written, take, block);
      if (env->ExceptionCheck()) { return; }
      written += take; offset += 2 * static_cast<jlong>(take);
    }
  }
  if (written != count) invalid(env, "descriptor lengths changed during export");
}


namespace {
class jint_array_view {
 public:
  jint_array_view(JNIEnv* env, jintArray array) : env_(env), array_(array) {
    values_ = env_->GetIntArrayElements(array_, nullptr);
  }
  ~jint_array_view() {
    if (values_ != nullptr) env_->ReleaseIntArrayElements(array_, values_, JNI_ABORT);
  }
  jint* values() const { return values_; }

 private:
  JNIEnv* env_;
  jintArray array_;
  jint* values_ = nullptr;
};
}

// Range export over the immutable SharedSegments directory. The metadata arrays are borrowed
// for this synchronous call only; no Java array, direct-buffer pointer, or owner is retained.
extern "C" JNIEXPORT void JNICALL
Java_com_synexia_indexstring_shared_SharedArrayNative_copyUtf16RangeTo0(
    JNIEnv* env, jclass api, jobjectArray owners, jintArray offsets, jintArray lengths,
    jintArray ends, jint first_segment, jint last_segment_exclusive, jint source_start,
    jcharArray target, jint target_start, jint count,
    jobject monitor) {
  if (owners == nullptr || offsets == nullptr || lengths == nullptr || ends == nullptr
      || target == nullptr) {
    invalid(env, "owners, metadata, and target required");
    return;
  }

  const jsize segment_count = env->GetArrayLength(owners);
  const jsize offset_count = env->GetArrayLength(offsets);
  const jsize length_count = env->GetArrayLength(lengths);
  const jsize end_count = env->GetArrayLength(ends);
  if (env->ExceptionCheck()) return;
  if (segment_count != offset_count || segment_count != length_count || segment_count != end_count) {
    invalid(env, "metadata lengths must match owners");
    return;
  }
  if (segment_count > 4096) {
    invalid(env, "segment budget exceeded");
    return;
  }
  if (first_segment < 0 || last_segment_exclusive < first_segment
      || last_segment_exclusive > segment_count) {
    invalid(env, "invalid participating segment window");
    return;
  }

  const jsize target_length = env->GetArrayLength(target);
  if (env->ExceptionCheck()) return;
  if (target_start < 0 || count < 0 || target_start > target_length
      || count > target_length - target_start || source_start < 0) {
    invalid(env, "invalid export range");
    return;
  }

  jint_array_view offset_values(env, offsets);
  jint_array_view length_values(env, lengths);
  jint_array_view end_values(env, ends);
  if (env->ExceptionCheck()) return;
  if ((segment_count > 0 && (offset_values.values() == nullptr
      || length_values.values() == nullptr || end_values.values() == nullptr))) {
    invalid(env, "metadata arrays unavailable");
    return;
  }

  jlong total_units = 0;
  for (jsize s = 0; s < segment_count; ++s) {
    const jlong previous_end = s == 0 ? 0 : end_values.values()[s - 1];
    const jlong end = end_values.values()[s];
    const jlong length = length_values.values()[s];
    const jlong offset = offset_values.values()[s];
    if (length <= 0 || offset < 0 || end <= previous_end || end - previous_end != length) {
      invalid(env, "invalid UTF-16 descriptor metadata");
      return;
    }
    auto owner = local<jni::jobject>(env, env->GetObjectArrayElement(owners, s));
    const std::uint8_t* bytes = nullptr;
    jlong capacity = 0;
    if (!utf16_segment(env, owner, bytes, capacity)) return;
    if ((offset + length) * 2 > capacity) {
      invalid(env, "UTF-16 descriptor exceeds owner capacity");
      return;
    }
    total_units = end;
  }

  if (static_cast<jlong>(source_start) > total_units
      || static_cast<jlong>(count) > total_units - source_start) {
    invalid(env, "source range exceeds descriptor length");
    return;
  }
  if (count > 0 && (first_segment >= last_segment_exclusive || first_segment >= segment_count)) {
    invalid(env, "participating segment window is empty or out of bounds");
    return;
  }
  if (count > 0) {
    const jlong range_end = static_cast<jlong>(source_start) + count;
    const jlong first_start = first_segment == 0 ? 0 : end_values.values()[first_segment - 1];
    const jlong first_end = end_values.values()[first_segment];
    const jlong last_start = end_values.values()[last_segment_exclusive - 1];
    const jlong prior_last_end = last_segment_exclusive <= 1 ? 0
        : end_values.values()[last_segment_exclusive - 2];
    if (first_segment >= last_segment_exclusive || source_start < first_start
        || source_start >= first_end || range_end <= prior_last_end || range_end > last_start) {
      invalid(env, "participating segment window does not cover source range");
      return;
    }
  }

  jmethodID checkpoint =
      env->GetStaticMethodID(api, "copyCheckpoint",
          "(Lcom/synexia/job/IProgressMonitor;)V");
  if (checkpoint == nullptr || env->ExceptionCheck()) return;
  env->CallStaticVoidMethod(api, checkpoint, monitor);
  if (env->ExceptionCheck()) return;

  constexpr jint block_units = 1024;
  jchar block[block_units];
  const jlong range_end = static_cast<jlong>(source_start) + count;
  jint written = 0;
  for (jsize s = first_segment; s < last_segment_exclusive && written < count; ++s) {
    const jlong previous_end = s == 0 ? 0 : end_values.values()[s - 1];
    const jlong from = source_start > previous_end ? source_start : previous_end;
    const jlong to = range_end < end_values.values()[s] ? range_end : end_values.values()[s];
    if (from >= to) continue;

    auto owner = local<jni::jobject>(env, env->GetObjectArrayElement(owners, s));
    const std::uint8_t* bytes = nullptr;
    jlong capacity = 0;
    if (!utf16_segment(env, owner, bytes, capacity)) return;
    jlong byte_offset =
        (static_cast<jlong>(offset_values.values()[s]) + from - previous_end) * 2;
    jlong remaining = to - from;
    while (remaining > 0) {
      env->CallStaticVoidMethod(api, checkpoint, monitor);
      if (env->ExceptionCheck()) return;
      const jint take = static_cast<jint>(remaining < block_units ? remaining : block_units);
      for (jint i = 0; i < take; ++i) {
        const jlong p = byte_offset + 2 * static_cast<jlong>(i);
        block[i] = static_cast<jchar>(
            (std::uint32_t(bytes[p]) << 8U) | std::uint32_t(bytes[p + 1]));
      }
      env->SetCharArrayRegion(target, target_start + written, take, block);
      if (env->ExceptionCheck()) return;
      written += take;
      byte_offset += 2 * static_cast<jlong>(take);
      remaining -= take;
    }
  }
  if (written != count) invalid(env, "descriptor metadata changed during export");
}
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) return JNI_ERR;
    jclass cls = env->FindClass("com/synexia/indexstring/shared/SharedArrayNative");
    if (cls == nullptr) return JNI_ERR;
    JNINativeMethod method = {
        const_cast<char*>("copyUtf16RangeTo0"),
        const_cast<char*>("([Ljava/nio/ByteBuffer;[I[I[I[IIII[CIILcom/synexia/job/IProgressMonitor;)V"),
        reinterpret_cast<void*>(Java_com_synexia_indexstring_shared_SharedArrayNative_copyUtf16RangeTo0)};
    const jint result = env->RegisterNatives(cls, &method, 1);
    env->DeleteLocalRef(cls);
    return result == JNI_OK ? JNI_VERSION_1_8 : JNI_ERR;
}