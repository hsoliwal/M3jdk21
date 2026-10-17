# M3JDK21 generation-safe object-state index projections — 2026-10-17

## Target and canonical source ownership

Synexia holds the reusable recipe `com.synexia.m3.ObjectStateOffset`, reusing existing `M3HashPinnedJavaSnapshotRecipe` (two Java source/test atoms) and `M3HashPinnedTextSnapshotRecipe` (the exact source receipt/CI atoms). Downstream `hsoliwal/M3jdk21` owns Java21 collection behavior and JNI runtime acceptance. Every modified current Java, Python or CI file is guarded by an exact previous SHA-256 and reviewed successor SHA-256; the historic source manifest is never rewritten.

The existing `M3ObjectStateSlots28` remains the only per-object state authority. It already stores **one byte of flags per touched physical slot in lazy segmented byte[] lanes**, and delegates liveness/stale-generation checks to the existing `M3SlotAllocator28`. There is no new Java object/Entry per state and no change to HotSpot object headers or GC reference counts.

## Added behavioral atoms

- `matchingPositions(long[] handles, int from, int to, int required, int excluded, Backend)`: half-open original **input index positions** (not handles, slot payloads, or native pointers), each handle prevalidated before output. Stable order and duplicates preserved; signed/unsigned eight-bit state masks retain semantics. Positions are emitted in ascending encounter order even if caller's handle sequence is arbitrary/sorted/filtered. Java produces one exact O(k) `int[]`, with two O(n) scans and **no extra index array**.
- Explicit `Backend.NATIVE_REQUIRED` first validates every handle and native-library availability, then constructs aligned primitive `int[]` physical input positions and `byte[]` predicate eligibility. It reuses the existing `M3IndexMaskCompactor.select(int[],byte[],mask,Backend)` C17 JNI kernel for one stable select. JNI never receives object references, per-slot application state or generations. This native route has O(n) temporary index+mask storage; it is **opt-in only** and not assumed faster than Java.
- `updateHandles(long[] handles, int from, int to, int setMask, int clearMask)`: validates all generation handles and disjoint unsigned byte masks **before any state write**, updates the existing per-slot flag lane in one sequential pass, and counts distinct words that actually changed. Duplicate handle occurrences are idempotent. Invalid handles, masks or ranges refuse without modifying any flags; non-transactional allocation failures and concurrency are outside this single-thread owner contract.

The old `allocate`, `release`, `state`, `update`, `compactHandles`, `snapshotHandles`, `exportStates`, flags and existing JNI ABI remain unchanged. No new JNI C symbol is needed, because this is a **compositional** bridge to an already qualified primitive mask operation.

## Algorithm/correctness catalogue and tests

Canonical category ledger: `synexia-openrewrite-recipes/catalogs/m3jdk21-object-state-offset-jni-20261017.tsv`.

Selected: existing M3 lazy byte-state lane, generation-safe slot allocator and original native int-mask compactor; original source license Apache-2.0. Not selected: Guava/Commons entry/object wrappers (additional object graph and different mutable-view contracts), copied challenge-site code, per-object JNI calls or arbitrary GC-reachability approximation. LeetCode/HackerRank/GeeksforGeeks are problem-taxonomy/adversarial-input references only.

Tests extend **the original** `M3ObjectStateSlots28SelfTest`: source-position filtering, duplicate handles, 3,072 seeded projection rounds, state updates and no-op idempotence, stale-handle reuse/ABA, fail-before-write, invalid ranges, empty windows, null/invalid backend and actual JNI parity when requested. The previously written 28-bit slot state, random 98k operations and sparse-memory oracle remain present.

## Admission

1. Canonical Synexia OpenRewrite SDK preimage verification, exact target postimage, missing/occupied/refused stale path and second-pass fixed point.
2. Target source-receipt SHA256 and historical inventory untouched; negative test must reject symlinks, malformed/duplicate/tampered receipts and cross-generation predecessor drift.
3. Strict Java21 `javac --release 21 -Xlint:all -Werror`; original JUnit and state self-test; actual checked JNI under `-Xcheck:jni` with the existing C17 bounded kernel.
4. Full JDK, HotSpot, jtreg/JCK and cross-platform performance comparison remain separate gates; this collection module change does not imply all-JEP completion or `java.util` replacement.

**No completion or speed claim from source hash matches alone.**
