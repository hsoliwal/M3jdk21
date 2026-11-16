/*
 * Copyright (c) 2003, 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2011, 2015, Red Hat, Inc.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#ifndef OS_CPU_LINUX_ZERO_ATOMIC_LINUX_ZERO_HPP
#define OS_CPU_LINUX_ZERO_ATOMIC_LINUX_ZERO_HPP

#include "orderAccess_linux_zero.hpp"

#if defined(__ANDROID__) && defined(__i386__) && defined(__clang__)
// Android x86 permits four-byte-aligned 64-bit storage. These compiler-rt
// entry points select an atomic implementation using the actual address.
extern "C" {
  void zero_atomic_load(int size, const volatile void* src, void* dest, int order)
    __asm__("__atomic_load");
  void zero_atomic_store(int size, volatile void* dest, const void* src, int order)
    __asm__("__atomic_store");
  int zero_atomic_compare_exchange(int size, volatile void* dest, void* expected,
                                  const void* desired, int success, int failure)
    __asm__("__atomic_compare_exchange");
  void zero_atomic_exchange(int size, volatile void* dest, const void* value,
                            void* previous, int order)
    __asm__("__atomic_exchange");
  uint64_t zero_atomic_fetch_add_8(volatile void* dest, uint64_t value, int order)
    __asm__("__atomic_fetch_add_8");
}
#endif

// Implementation of class atomic

template<size_t byte_size>
struct Atomic::PlatformAdd {
  template<typename D, typename I>
  D add_then_fetch(D volatile* dest, I add_value, atomic_memory_order order) const;

  template<typename D, typename I>
  D fetch_then_add(D volatile* dest, I add_value, atomic_memory_order order) const {
    return add_then_fetch(dest, add_value, order) - add_value;
  }
};

template<>
template<typename D, typename I>
inline D Atomic::PlatformAdd<4>::add_then_fetch(D volatile* dest, I add_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(I));
  STATIC_ASSERT(4 == sizeof(D));

  D res = __atomic_add_fetch(dest, add_value, __ATOMIC_RELEASE);
  FULL_MEM_BARRIER;
  return res;
}

template<>
template<typename D, typename I>
inline D Atomic::PlatformAdd<8>::add_then_fetch(D volatile* dest, I add_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(I));
  STATIC_ASSERT(8 == sizeof(D));

#if defined(__ANDROID__) && defined(__i386__) && defined(__clang__)
  uint64_t bits = PrimitiveConversions::cast<uint64_t>(add_value);
  uint64_t previous = zero_atomic_fetch_add_8(dest, bits, __ATOMIC_RELEASE);
  D res = PrimitiveConversions::cast<D>(previous + bits);
#else
  D res = __atomic_add_fetch(dest, add_value, __ATOMIC_RELEASE);
#endif
  FULL_MEM_BARRIER;
  return res;
}

template<>
template<typename T>
inline T Atomic::PlatformXchg<4>::operator()(T volatile* dest,
                                             T exchange_value,
                                             atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(T));
  FULL_MEM_BARRIER;
  T result = __atomic_exchange_n(dest, exchange_value, __ATOMIC_RELAXED);
  FULL_MEM_BARRIER;
  return result;
}

template<>
template<typename T>
inline T Atomic::PlatformXchg<8>::operator()(T volatile* dest,
                                             T exchange_value,
                                             atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(T));
  FULL_MEM_BARRIER;
#if defined(__ANDROID__) && defined(__i386__) && defined(__clang__)
  T result;
  zero_atomic_exchange(8, dest, &exchange_value, &result, __ATOMIC_RELAXED);
#else
  T result = __atomic_exchange_n(dest, exchange_value, __ATOMIC_RELAXED);
#endif
  FULL_MEM_BARRIER;
  return result;
}

// No direct support for cmpxchg of bytes; emulate using int.
template<>
struct Atomic::PlatformCmpxchg<1> : Atomic::CmpxchgByteUsingInt {};

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<4>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(4 == sizeof(T));

  T value = compare_value;
  FULL_MEM_BARRIER;
  __atomic_compare_exchange(dest, &value, &exchange_value, /*weak*/false,
                            __ATOMIC_RELAXED, __ATOMIC_RELAXED);
  FULL_MEM_BARRIER;
  return value;
}

template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<8>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order order) const {
  STATIC_ASSERT(8 == sizeof(T));

  FULL_MEM_BARRIER;
  T value = compare_value;
#if defined(__ANDROID__) && defined(__i386__) && defined(__clang__)
  zero_atomic_compare_exchange(8, dest, &value, &exchange_value,
                               __ATOMIC_RELAXED, __ATOMIC_RELAXED);
#else
  __atomic_compare_exchange(dest, &value, &exchange_value, /*weak*/false,
                            __ATOMIC_RELAXED, __ATOMIC_RELAXED);
#endif
  FULL_MEM_BARRIER;
  return value;
}

// Atomically copy 64 bits of data
inline void atomic_copy64(const volatile void *src, volatile void *dst) {
  // Copy's jlong entry points require both addresses to be 8-byte aligned.
  typedef int64_t aligned_int64_t ATTRIBUTE_ALIGNED(8);
  aligned_int64_t tmp;
  __atomic_load(reinterpret_cast<const volatile aligned_int64_t*>(src), &tmp, __ATOMIC_RELAXED);
  __atomic_store(reinterpret_cast<volatile aligned_int64_t*>(dst), &tmp, __ATOMIC_RELAXED);
}

template<>
template<typename T>
inline T Atomic::PlatformLoad<8>::operator()(T const volatile* src) const {
  STATIC_ASSERT(8 == sizeof(T));
  T dest;
#if defined(__ANDROID__) && defined(__i386__) && defined(__clang__)
  zero_atomic_load(8, src, &dest, __ATOMIC_RELAXED);
#else
  __atomic_load(const_cast<T*>(src), &dest, __ATOMIC_RELAXED);
#endif
  return dest;
}

template<>
template<typename T>
inline void Atomic::PlatformStore<8>::operator()(T volatile* dest,
                                                 T store_value) const {
  STATIC_ASSERT(8 == sizeof(T));
#if defined(__ANDROID__) && defined(__i386__) && defined(__clang__)
  zero_atomic_store(8, dest, &store_value, __ATOMIC_RELAXED);
#else
  __atomic_store(dest, &store_value, __ATOMIC_RELAXED);
#endif
}

#endif // OS_CPU_LINUX_ZERO_ATOMIC_LINUX_ZERO_HPP
