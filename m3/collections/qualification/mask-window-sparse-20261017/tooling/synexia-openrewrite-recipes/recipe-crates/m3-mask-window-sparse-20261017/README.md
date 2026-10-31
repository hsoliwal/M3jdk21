# M3 mask-window sparse refinement — review candidate

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

The existing bounded primitive index window read states and copied row chunks even when no row was selected. This recipe resolves constant-mask expressions after the original validation and avoids JNI row reads for rejected state chunks. The normal Java scan atom is byte-preserved. Existing SDK namespaces and all public/native descriptors remain unchanged.

Canonical source: `hsoliwal/com.synexia`, this recipe crate. Target baseline: `hsoliwal/M3jdk21@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`. Kernel baseline: Synexia `892e3f6c51d78bc0948e8a8cf736055b4c70eafa`. First-party Apache-2.0 source is reused; no challenge/editorial or third-party implementation body is copied. Catalogue categories reuse the existing receiving record; no new deep research was performed.

Named recipe: `com.synexia.m3jdk.MaskWindowSparse20261017`. Its existing class-backed Java/text kernels emit four exact source/test files from SHA-sealed preimages or explicit ABSENT admissions. Five actual Maven/OpenRewrite SDK tests cover named execution, fixed point, missing/drift/duplicate/occupied refusals, permutations, independent recipe order, partial replay and unrelated text. Production outputs are SDK materializations, not hand-edited copies.

Executed selected gates: text/Checkstyle lint; Java21 strict compilation; C17 warnings-as-errors and fatal UBSan; generated JNI headers used as compiler prototypes; public descriptor parity; 357,094 new scalar-oracle checks in Java and 357,095 with actual checked JNI; existing 30,596 window checks and 11,520 compactor parity checks; missing-library behavior; independent portable reproduction. The instrumented native-table test proves 0/8 row reads for rejection and 1/8 for a sparse chunk fixture; the baseline fails that work-count assertion as expected.

Cost evidence is in `evidence/COSTS.json` and `COST_SUMMARY.tsv`. These are two-process fixed-workload probes, not JMH/statistical qualification. Rows=262,144; output limit=4,096; 500 measured calls. All early-warm and longer-warm results are retained, including slower early-warm observations. Timing depends on JVM compilation and environment. There is no universal speedup claim, automatic native backend selection, retained-heap proof, or whole SDK acceptance. Allocation counters include probe overhead; process RSS is a process-level observation, not retained/native live-allocation attribution. Product adds no retained fields, heap scratch or native heap allocation; fixed JNI buffers remain 128 elements.

Recipe revisions are retained in `history`: v1 used a per-selected-row load flag; v2 removed that branch; v3 kept the ordinary Java scan atom unchanged and moved mask identities to the validated dispatch. A proposed read-only filtered List iterator optimization was rejected because it changes callback exception timing. See `CONTRACT.md`, `CATALOGUE.tsv`, and `NAME_MAPPING.tsv` for the boundary.

Reproduce selected proof with Java21, GCC and the approved offline Maven cache:

```sh
export JAVA_HOME=/path/to/jdk21
python3 proof/verify.py --output /new/empty/proof-directory --maven /path/to/mvn --m2 /path/to/offline-m2
```

Run from this crate or pass the full script path. The script executes real SDK generation before compiling/running its output, preserves logs and refuses failures. In the target mirror the existing kernels are copied with exact source lineage; they are authoring tooling only.

HOLD: full owning Maven reactor/JUnit suite, built M3JDK image/jtreg/JCK, GC/JIT matrix, non-Linux JNI, statistical performance and precise retained/native memory attribution have not been completed. The system bridge was unavailable. This independent `m3/collections` increment does not advance the repository's active STRING receiving phase and does not claim complete Guava/Eclipse/Spring/Apache/JDK SDK coverage. Keep both PRs draft for review.
