<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 five-family SDK receiver: real Guava SetMultimap

Canonical Synexia source-first recipe PR: https://github.com/hsoliwal/com.synexia/pull/12224
Exact canonical recipe head: bef9f4a80db53ed36b25b5675f6a91005bf77807
Public receiver original master: 0efc1719ca5766aead09f4f7c49f5067975bd946
One existing independent M3 map owner + per-key existing M3 set owner serve the real Guava SetMultimap contract via Guava's public view machinery. The companion M3-specific selectedByStates() uses the existing M3IndexPathMasks Java/C17 optional JNI primitive kernel. No imported vendor source, second M3 map implementation, changed JDK/HotSpot/JNI production symbol, default-branch edit, or source rename.

Serial commits: (1) exact Synexia hash-sealed Maven/OpenRewrite recipe and verification workflow; (2) exact source/template Java and actual Guava donor JUnit oracle. The original M3 factory guavaSetMultimap() is deliberately retained untouched for source/binary compatibility, while new M3GuavaSetMultimap.create() returns the specifically named implementation. Any future existing-owner factory rewrite needs its own canonical source-sealed recipe and tests.

Evidence already obtained: local Java21 selected-source strict compilation and primitive-mask smoke with clearly disclosed Guava/M3 TEST-ONLY stand-ins. NOT a real provider binary or full M3 owner test. Full Maven SDK/JUnit against actual Guava 33.4.8-jre and core owners plus JNI -Xcheck:jni runs in exact-head CI. Performance, JDK image/JTReg/JCK, concurrency and whole-five-provider library breadth remain separate acceptance gates.
