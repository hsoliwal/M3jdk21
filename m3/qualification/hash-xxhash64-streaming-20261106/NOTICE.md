<!--
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
-->
# Notices of the relocation leaf hash-xxhash64-streaming-20261106

- `src/java.base/share/classes/jdk/internal/m3/hash/M3XxHash64Streaming.java` and the four
  `test/jdk/jdk/internal/m3/hash/M3XxHash64Streaming*.java` files are relocated from `hsoliwal/com.synexia`
  (`synexia-fileindex` `XXHash64`, its tests, and recipe crate `m3-xxhash64-block-reuse-20261031`, PR #14848), Apache
  License 2.0, Copyright 2026 Synexia <hsoliwal@gmail.com> (the donor's file header). As the crate's
  `qualification/NOTICE.md` asks: the Java production change and the Synexia qualification code remain Apache-2.0,
  Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project. In M3JDK they carry the SPDX form of the sibling
  `jdk.internal.m3.hash` files (`Apache-2.0 OR EPL-2.0`, Copyright (c) 2026 Synexia, Hitesh Soliwal and others) and one
  `//FLLDCIM: RELOCATE` anchor naming the donor blob at donor commit `ca1aafc4d94f9dd1ee288226dcca070cc60991c9`.
- The crate's independent test oracle (Cyan4973/xxHash v0.8.3, BSD-2-Clause, Copyright (c) 2012-2021 Yann Collet) is
  NOT relocated: no C source, header or binary of it enters M3JDK. The oracle of this leaf is the vendored
  zero-allocation-hashing source already in `m3/vendor/third_party` (through the lane's existing tests) and the donor
  Java itself (the differential).
- `M3XxHash.java` is zero-allocation-hashing 0.16 (Apache License 2.0, Copyright 2015 Higher Frequency Trading); its
  2026-11-06 change (MRL helper extraction, recipe `recipe/owner-patches/M3XxHash.java.diff`) is stated in the file's
  own change notice above its package line, next to the 2026-10-31 one listed in
  `src/java.base/share/legal/zero-allocation-hashing.md`.
- The donor's Lombok `@Slf4j` logger (unused) is dropped; Lombok and SLF4J are used only by the differential gate to
  compile the unchanged donor bytes on the scratch side, never by M3JDK.
