<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3SDK JMOD current-master receiver — 2026-10-13

## What this actually delivers

This receiver restores the exact 52 source/evidence/descriptor Git blobs introduced by
[M3JDK21 PR #788](https://github.com/hsoliwal/M3jdk21/pull/788), head
`3c7f331a65e31fdbe4c198d79c7bfcb4c226cda2`, to a new branch based on
`master@3799df0b88d169064b610481400ec4edacec1d2e`.

The earlier PR was merged into the `m3/dependency-source-inline-20261010` feature
stack, not directly into the current master product tree. Its recipes/descriptor files
were not present on master at this receiver's inventory time.

The receiver intentionally uses each existing Git **blob SHA**, without regenerating or
editing the previously reviewed descriptor or recipe. All files remain under `m3/`;
OpenJDK `src/`, `make/` and the default Maven reactor are unchanged.

## Proof boundaries

1. `RECEIVED_SDK_JMOD_BLOBS.tsv` binds the 52 original Git blobs to PR #788 and
   the exact donor commit.
2. `tools/verify_sdk_receiver.py` recomputes Git's NUL-framed blob SHA-1 from
   every current byte, refuses missing/drifted/escaped/duplicate targets, and returns a
   separate SHA-256 receipt. This is exact custody **only**.
3. `tools/sdk_image_probe.py` is a distinct target-runtime proof. Its input is a
   newly built linked SDK image and matching `JMOD_PACK.tsv`, not the historical
   Windows evidence alone. It requires at least the original 87 JMOD targets,
   a Java 21 release marker, all standard development tools, a module resolver pass,
   and **actual execution** of RE2J and Guava API calls inside the linked image.
4. The smoke compiles `fixtures/M3SdkImageSmoke.java` using the candidate image's
   own Java 21 `javac --release 21 -Xlint:all -Werror` and runs it with
   `-Xcheck:jni`. It writes receipts only to a fresh external proof directory.
5. Neither a source mirror nor the SDK runtime smoke bypasses OpenJDK configure/make,
   relevant jtreg, compiler/GC/JNI/regression proof, first-party license attribution,
   or the independent release-admission contract. Product promotion stays disabled.

## Run

From repository root:

```bash
python3 m3/tooling/jmod-recipes/tools/verify_sdk_receiver.py --repo .
python3 -m unittest discover -s m3/tooling/jmod-recipes/tools -p 'test_*.py' -v
mvn -B -ntp -f m3/tooling/jmod-recipes/pom.xml test
```

For a **new** developer/JMOD SDK image built with the selected Java 21 tools and a
current matching JMOD pack TSV (not a stale historical receipt):

```bash
python3 m3/tooling/jmod-recipes/tools/sdk_image_probe.py \
  --image /path/to/new/m3sdk/image \
  --pack /path/to/new/m3sdk/JMOD_PACK.tsv \
  --out /fresh/external/sdk-runtime-proof
```

**Expected outcome against the historical PR #788 image:** the new SDK probe refuses
it unless that image includes `javac`, `jlink`, `jpackage` and the other development
tools. The PR #788 image linked `java.se` plus donor modules and proved module
resolution; it did not prove an SDK developer toolchain or execute application APIs.

The existing safe developer-pack machinery under `m3/tooling/module-packs/` remains
the canonical developer-tools/image owner. Composing its developer closure with the
vendored JMOD closure is the following **PENDING** SDK recipe/DAG pass, not a reason
to create a second image assembler or alter current Java 21 contracts.

## Non-overlap with backport residue

The JEP 423, 458, 474 and 484 current-master packets continue in residue priority
order. This SDK receiver is an independent opt-in tooling/packaging feature and
does not change their status or claim any JEP backport complete.

## License and authorship

- OpenJDK-derived code retains its original GPLv2 with Classpath Exception terms.
- The restored first-party M3 descriptor recipe remains Apache-2.0 as recorded in
  its original source and project notices.
- Vendored third-party libraries retain each original license; the existence of
  an SDK image does not relicense them.
