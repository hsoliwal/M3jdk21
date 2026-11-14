# Exact SDK source-binding successor

The Apache multivalue workflow stopped in its historical source verifier before compilation: the SDK receipt pinned 7,020 bytes, while master contains the legitimate 7,807-byte append successor. This change gives CI a separately sealed qualification for that exact successor.

The old 33-row provenance, verifier, V2 qualification, 32 owned implementation/test sources and historical transformation resources remain unchanged. The new guard proves the complete old SDK prefix, all 26 old public static declarations, the exact 787-byte append and the four added factories (`m3ArrayList`, `indexedTable`, `indexedMultiValueMap`, `longRangeSet`). It checks the current array owners and JNI source. It accepts one exact current SDK image; the old image is retained as a precursor, never as another accepted current hash. Drift, source aliases, receipt reseeding and wrong provider bytes still fail closed.

The optional argument file retains the old build contract: 33 source bindings are checked, and the 32 owned implementation/test paths are emitted. Core compilation separately includes the SDK. No production Java source or native ABI changes in this PR.

The new source-only OpenRewrite receiving host reuses the canonical hash-pinned engine unchanged. It installs the exact 7,020-to-7,807 successor on the qualified historical image, preserves the already current image and unrelated files, and refuses missing, drifted and duplicate targets before installer output. Its fresh activation/engine tests prove fixed points after removing transformation markers. The previous installers and their before/after resources are preserved.

Validation is split by purpose. The original Windows source-blind plan produced 98 refusals and two final positive controls. A portable, explicit-work-directory probe independently exercises 48 drift and encoding/optimization refusals plus two positives; it copies only 45 required source inputs (339,796 bytes), restores them, and leaves the checkout untouched. These corpora are different and their counts must not be added as unique behavior coverage. The portable subset runs in CI.

Existing runtime replay passed: SDK smoke (1,780 checks, Java and actual JNI), the owner-factory fixture (10,224 checks, normal JVM and `-Xint`), the existing JUnit factory entry point, the owned Apache multivalue test (558,923 checks and 10,000 random operations), and its existing 65-case comparison (609 independent laws, zero failures). The JNI-sensitive SDK smoke verifies the existing native mask kernel; owner-factory multivalue operations explicitly select Java. These are finite regression corpora, not a performance proof or completion of the five-library superset.

The receiving host uses Rewrite 8.17.1 and Jupiter 5.10.2. Local tests use their hash-verified existing offline jars through the official JUnit launcher; local Maven is not run because its C: cache is held read-only. Hosted Maven and GCC execution are reported separately in the PR.

To reproduce from a checkout with the exact current source:

```sh
python -I -S -B m3/collections/qualification/owned-apache-multivalue-20261018/verify_source_binding_successor_v3.py --jar /scratch/commons-collections4-4.5.0.jar --args /scratch/owned.args
python -I -S -B m3/collections/qualification/owned-apache-multivalue-20261018/probe_source_binding_successor_v3.py --jar /scratch/commons-collections4-4.5.0.jar --work /scratch/new-guard-probe
mvn -f m3/collections/recipe/sdk-binding-successor-v3-20261018/pom.xml -Dmaven.repo.local=/scratch/m2 -Dm3.recipe.output=/scratch/recipe-target test
```

Use an existing explicit scratch parent outside the checkout with at least 8 GiB free after bounded output. The provider is the pinned Commons Collections 4.5.0 artifact, not a renamed compatible jar. Paths and custody are in the sealed receipt; retained historical resources and their raw-byte inventory permit subsequent passes to reuse this qualification instead of reseeding old evidence.
