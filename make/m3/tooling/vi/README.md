# VI — Version Index

This recipe crate inlines the relevant Synexia implementation into two internal java.base owners:
`jdk.internal.mindex.M3VI` and `jdk.internal.mindex.M3Release`. VI is the short pattern name;
Release is already a concise domain name. The canonical crosswalk is `m3/docs/name-mapping.json`.

## Source and adaptation

`pins.json` binds the exact Synexia commit, source blobs, template SHA-256 values and existing
OpenRewrite recipe. `donor/` retains the two originals byte-for-byte with their Apache-2.0 license
and NOTICE. Generated targets retain Apache-2.0; the distribution notice lives in java.base/legal.

The source MIndexVersionTable algorithm parses stored versions once, retains raw labels and a
primitive sorted row lane, then uses binary bounds for range queries. The port preserves that
algorithm, stable row order and budgets. It replaces application MIndexString coordinates with
references to the JDK's existing String owner and supplies a small JDK-owned progress callback.
No second interner, payload dictionary, class map, resolver or Maven version grammar is introduced.
Preparation now also rejects an observed scheme-revision change before publishing its snapshot.
Scheme implementations must remain immutable and deterministic; revision checks are not a lock.

M3Release preserves exact origin/coordinates/raw release spelling/classifier/content seal. A seal
is caller-declared provenance, not a signature or proof of actual loaded bytes. Neither kernel
selects a class implementation or alters the public Java API. No JNI accelerator is needed for
this metadata port; the source VM precompute JNI owner remains a separate pending adaptation.

## Reproduce

From the repository root with JDK 21 and the pinned dependencies available offline:

```sh
mvn -o -f m3/tooling/vi/pom.xml test
python m3/tooling/vi/test_install.py
```

The existing `M3HashPinnedJavaSnapshotRecipe` creates typed Java candidates under `target/generated`.
The three manifest targets have explicit ABSENT preimages. Replay of the exact output is a fixed
point; conflicting Java, a wrong parser or a changed template is refused. The harness compiles
the product with `--patch-module java.base`, `-Xlint:all -Werror`, then compiles/runs an independent
JPMS oracle probe in `-Xint` and `-Xmixed`. `jdeps` must report only `java.base`.
The retained bootstrap task-crate gate verifies the manifest seal and performs a read-only
atom inventory over this packet's Java candidates. Exact donor bytes and license/NOTICE seals
are verified as well. This scoped inventory does not claim a full-tree semantic audit.

`install/plan.json` uses the retained `m3/migration/recipe.py` sealed installer for reviewed outputs,
including mapping, notices and generated Java. It preserves exact preimages for rollback. The
installer is not a second Java transformation engine: Java postimages come from OpenRewrite.
All template and output changes must rerun the recipe proof and refresh the sealed plan.

This is a bounded MODULE-scope port of two classes, not a general FILE refactoring. No 100-file
general-recipe promotion or complete JDK conformance is claimed. Local evidence is in `evidence/`;
full configure/make, jtreg, platform coverage, benchmarks and automatic runtime consumers remain
unexecuted. Maven and OpenRewrite are tool-plane dependencies only.
