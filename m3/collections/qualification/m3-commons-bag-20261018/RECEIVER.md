<!-- SPDX-License-Identifier: Apache-2.0 -->
# Product receiving scope (M3JDK21)
Canonical recipe: https://github.com/hsoliwal/com.synexia/pull/12211
Canonical commit: aafeae36925741088b5a8baa299db2e2c054ec07
Original target preimage: master@4b24824c3aa09d802121447f8970d442a855f064.
Exactly two ABSENT-before product files; keep existing core M3CountedBag, M3LinkedHashMap, five-provider interop, Maven/JUnit owners and C17 JNI symbol unchanged.
Recipe + SHA/JUnit CI commit precedes generated product source commit. Public API remains original Apache org.apache.commons.collections4.Bag<E>.
Do not claim whole-JDK replacement, guaranteed native speedup or all-provider exhaustive coverage. Merge only after actual hosted Maven, donor API and JNI gates.
