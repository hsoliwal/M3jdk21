# M3JDK21 prepared String lookup and fixed-address refinement

<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->

## Exact source and target ownership

Base: hsoliwal/M3jdk21@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49. This namespace was absent before inventory. Sources and modes are sealed in SOURCE_SEALS.json. Product sources are received only from the qualified OpenRewrite Results. Reusable recipe custody is hsoliwal/com.synexia@289e32f130c9c8e4a1cf0edc1066327d699393bf.

## Frozen contracts

M3GvasLiteralImage remains the existing target-owned image reader. All public/protected declarations, M3GVAS01 wire bytes, lexicographic row IDs, exact UTF-16 equality including unpaired surrogates, validated facts, rejection checks, native base+offset payload address and process-lifetime mapping stay authoritative. There is no Unicode normalization, spelling duplication or concept-ID allocation. A sorted literal row is not a permanent semantic concept identity.

## Behavioral atom

Prepare an optional primitive int[] row+1 lookup directory from already-validated per-row hash/length metadata. Zero denotes an empty slot. Half occupancy, power-of-two mask, a fixed 64-probe admission bound and at most 2^22 slots (16 MiB) limit cost. Images below 32 rows use current binary lookup; allocation refusal, oversize or an overlong cluster retain current exact binary lookup. A hash only nominates a candidate: length, stored hash and exact UTF-16 comparison decide a hit. Reverse row-to-address and fact access remain direct mapped offset reads. Preparation never changes the mapped image or numeric pointers. The directory is derived process-local metadata, not an OS-shared payload or a promise of constant time on unseen text.

## Candidate comparison

Existing M3 binary search remains the exact fallback and small-image choice. Synexia MappedCompiledLanguageImage donates the primitive row+1 candidate-directory pattern; its UTF-8 comparison is unsuitable for Java UTF-16 and is not copied. MIndexStringSearchIndex double-array trie gives bounded transitions but requires a new two-byte-unit representation and a separate footprint decision; defer that wider format change. Agrona primitive open addressing is a compatibility/performance reference, not an additional runtime dependency. LeetCode 706/704, HackerRank Sparse Arrays, and GeeksforGeeks open addressing nominate categories only; no challenge submission/editorial code is copied.

## Verification order

Inventory/document -> source-sealed Maven/OpenRewrite candidate -> lint -> Java21/native compilation -> exact API and behavior oracles -> Java/JNI runtime -> deterministic replay/fixed point/refusal -> PR. Compile the actual complete M3Gvas owners and writer, not stand-ins. Test small, normal, same-hash collision, lone-surrogate, NUL, empty, miss, corrupt and foreign-row cases. Run the existing three-process Linux JNI proof and collision/digest/corruption refusals with Xcheck:jni, interpreter, compiled and CompactStrings-disabled modes. Test fallback explicitly. Record cold preparation, warm lookup, reverse lookup, allocations/retained metadata and process/native memory separately. Windows and matched full M3String/HotSpot qualification stay pending unless actually executed.

## Repeatable target checks

Set `M3_JDK` to the Java21 toolchain, then run `python3 m3/verify-gvas-literal-precompute.py`. It compiles the real owners with `-Xlint:all -Werror`, generates JNI headers, compiles C, and proves same fixed address/shared inode in three processes plus digest/collision/corruption refusal.

Compile `M3GvasLookupIndexContractTest` with the same owner closure to exercise `small`, `normal` and `collision` fixtures. `emit <file> <kind>` uses the existing deterministic writer. Run `<kind> <JNI-library> <image> <sha256> checks` in an independent JVM with `-Xcheck:jni`; replace `checks` with `bench` for explicitly diagnostic preparation/lookup/reverse/retained-index measurements. The test source supports an unmodified baseline owner for comparison and consumes results to prevent dead-code removal. Measured speed is workload/toolchain-specific.
