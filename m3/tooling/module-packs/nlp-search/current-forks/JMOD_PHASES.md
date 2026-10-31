<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0 -->
# Current-fork JMOD admission and phase contract

This contract precedes the corresponding packer guard implementation. It extends
the existing `jmod_pack.py`; the original `jmod_image.flatten_release` remains the
only class-tree materializer. The compatibility lane's default release remains
21. A source/recipe/test pass does not imply matched-M3JDK product acceptance.

## Preflight before output

Before creating any output directory or invoking a JDK tool, the packer rejects:

* `--jmods-only` combined with `--root-module`;
* a release below 9 or greater than the supplied JDK's release-file feature;
* repeated active published module names, repeated generated module names, or a
  name appearing in both descriptor lanes;
* module names that cannot be safe Java-style dotted output names;
* malformed TSV headers/rows, missing input JARs, or occupied/symlink outputs;
* duplicate ZIP paths, absolute/backslash/drive/control/parent-traversal paths,
  file/directory path conflicts, encrypted entries, or Unix special-file entries;
* JAR signature files, which the existing flattening step cannot preserve as
  authenticated release-view signatures;
* malformed versioned paths or versioned entries without a main-manifest
  `Multi-Release: true` declaration;
* an invalid class header or an effective selected class whose major version is
  greater than `44 + --release`.

The manifest guard unfolds continuation lines in the main section only. It
rejects ambiguous duplicate main attributes. Versioned JAR paths use canonical
decimal release names at least 9, and cannot version `META-INF` resources. The
guard selects the highest eligible release only for inspection; it writes no
alternate flattened payload. A future version's class can be ignored for a lower
release while its archive path is still validated. Malformed/signed inputs need
their own explicit upstream repair or admission; no entry is silently removed to
make a rejected input pass.

The requested release sets the effective JAR view, the upper bound on selected
class-file major versions, and generated module-info bytecode. It does not lower
newer classes, rewrite their APIs, or establish semantic compatibility. Packaging
a class compatible with an older VM does not certify its external dependencies.

## Packaging, testing and runtime are separate

`--jmods-only` creates the admitted JMODs and a packaging report. It invokes no
`java` application or `jlink` image stage and reports runtime as `NOT_EXECUTED`.
It does not resolve or certify a complete executable dependency closure.
Descriptor compilation and JMOD creation still use the actual supplied JDK.

`--date` forwards an explicit ISO-8601 timestamp to the existing JDK `jmod create`
owner. Reproducibility is proven only by comparing two independent actual JMOD
outputs from the same pinned inputs and timestamp. A configured date alone is
not a determinism receipt. Without the flag, existing timestamp behavior remains.

`test_jmod_pack.py` divides fixture preparation from assertions:

```sh
python3 test_jmod_pack.py --phase prepare --jdk21-home /path/to/jdk21 \
  --jdk25-home /path/to/jdk25 --out /explicit/fresh/proof
python3 test_jmod_pack.py --phase test --jdk21-home /path/to/jdk21 \
  --jdk25-home /path/to/jdk25 --out /explicit/fresh/proof
```

All four arguments are required; no phase or JDK is inferred. Preparation
requires an absent output root; testing requires that exact prepared root.
The importable `validate_jar(jar: Path, release: int) -> None` performs the same
read-only JAR preflight for callers that must inspect inputs before `jdeps`.

Place fixture preparation in the COMPILE gate and the second command in TEST,
after PATCH and LINT. Preparation writes and compiles small genuine modular Java
fixtures at releases 21 and 25 and creates exact archive refusal cases. Tests
wrap the existing subprocess dispatcher only to record its actual commands and
complete outputs; they do not replace javac/jmod with success stubs.

Assertions cover early refusal, both module lanes, effective 21/25 resources and
class versions, generated descriptor class versions, lack of application/image
commands in packaging-only mode, and independent timestamp-pinned JMOD hashes.
No tests are claimed executed by this contract. The enclosing task records exact
command logs, exits, source/target hashes, failed gates and any unexecuted stage.

## Existing descriptor normalization receives the same release

The existing `descriptor_gen.class_names(jar, release=21)` and
`descriptor_gen.normalize(text, jar, all_classes, release=21)` accept an optional
release argument. Normalization passes that release into its existing binary-name
inventory. A Java 25-only implementation package therefore remains available to
the Java 25 descriptor receiver. Their default remains 21, and the existing
descriptor generator CLI remains unchanged at 21. The current-fork caller must
pass its release explicitly to both the class-name union and normalization.

This is parameterization of the existing owners. It adds no descriptor engine,
class-tree materializer, package-name transformation or new default admission.
The archive must pass `validate_jar` before these permissive legacy inventory
helpers run. The fixture suite includes a real Java 25-only class and checks both
the default/explicit-21 exclusion and the explicit-25 class/export retention.
