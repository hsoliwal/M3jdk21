<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
# LFU JNI lanes and exhausted-clock repair

The existing M3LFUCache retains its five JNI-created array lanes. This increment accepts the first occupied victim at saturated comparator sentinels and preserves timestamp order/ties when a zero-minimum lane reaches Long.MAX_VALUE. Dense ranks use scalar negative markers in the existing long[] lane, with no scratch array or structural Entry/Node allocation. Only victim() and nextStamp() differ from the qualified constructor postimage.

The received source is byte-for-byte the canonical recipe output: 6,455 bytes, SHA256 8e404bcd1e6a0efc94e8f1828feaf4d2d3da1375f07b8a64d238390e09d36f27. All five required JNI allocator expressions, other source bytes and API declarations remain unchanged. Existing constructor qualification remains in its original packet.

Validation: five canonical recipe SDK cases; strict Java 21 compilation; six expected old-source REDs; five boundary modes with 91 assertions; six fresh ordinary/miss/native/fault regression modes with 98,778 assertions. Total: 98,869 positive runtime assertions. The ordinary 98,602-assertion trace is unchanged, and accepted JNI runs have no diagnostics. All eight field and eleven method metadata signatures/access flags compare equal within the recorded class-file scope. Code, annotations, Exceptions and other reflective attributes were excluded.

[Canonical recipes, donor notices, raw results, logs, seals, exact source custody and qualification matrix](https://github.com/hsoliwal/com.synexia/tree/ca1dd139762fe15bb792a4b58eff0d3170ddad06/synexia-openrewrite-recipes/recipe-crates/m3-jni-array-storage-20261021/qualification/lfu-clock-rebase-v1) were published before applying this target source. ACTUAL_MATRIX_V1.json binds their immutable commit. Earlier failed attempts remain failures; verified archive state does not turn the V8 post-RAM-gate failure into a helper PASS.

Formal autoreview remains NO_VERDICT; independent source/proof advisories are separate. Rank transition assumes nonnegative occupied stamps and single-threaded operation with no callback/reentry inside it. Worst-case work is O(capacity * distinct), without a peak-performance claim. Later-allocation faults, full reactor/jtreg/installed-image/C++20, API projection ownership/liveness integration, reentry/concurrency/serialization, Unicode loading, performance and complete family coverage remain open.

The full original collection superset and both regex requirements remain ACTIVE/UNFINISHED. Policy M3-COLLECTION-JNI-ARRAY-STORAGE-1, Apache-2.0 attribution and all storage controls remain in force. No unrelated POM/native/source overlay or merge is part of this increment.
