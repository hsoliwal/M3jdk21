<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Fresh selected-source M3JDK21 native verification

The exact selected-source receiver at `repository/` was materialized from the
qualified native-only Synexia recipe and freshly tested. It is a source subset
of `hsoliwal/M3JDK21@7c0d2493edbf93ac5cb726667ce83046d3519e3e`, not a full
checkout or an installed JDK image. `preimage-custody.json` pins the unchanged
production Java owner, native preimage and two existing regression owners.

## Actual execution

Twelve commands passed: the existing qualified copier's check, apply and replay;
Java, javac and C compiler version observations; strict Java21 and C17 builds;
the nine-export check; and three actual JVM runs with `-Xcheck:jni`. The Java
build uses actual receiving paths, `--release 21`, `-Xlint:all`, `-Werror`, no
annotation processing and a fresh isolated classpath. The C17 build uses
`-O3 -Wall -Wextra -Werror -pedantic` and the freshly generated JNI declaration
header. No substitute Java owner, synthetic JNI execution or new behavioral
oracle is used.

The new mask-position oracle passed 16,777,216 truth cases, 66,196 vectors and
17,241,106 checks. The unchanged range regression passed 7,680 cases and 53,782
checks; the unchanged required/excluded regression passed 512 checks. Each
fresh stdout is byte-identical to its sealed baseline output and each JVM
stderr is empty. The original nine exported JNI symbols are unchanged.

Check and replay left their complete observed product snapshots unchanged.
Apply modified only the native owner and added the exact Java regression. The
unchanged Java owner and old regressions retained their bytes, modes, mtimes
and inodes. The stock copier writes receipts into its own `target/`; it was
therefore invoked from an exact isolated copy of its qualified inputs under
`evidence/build/qualified-copier`. The frozen canonical C packet was not edited.

The prior 899-case schedule tape is bound by exact hash together with the prior
PASS receipt and matching C/test bytes. This execution ran zero schedule
instrumentation commands. That result is historical evidence, separate from
the three fresh target JVM runs. Full JDK, whole-module, jtreg, other native
platform and performance qualifications remain unperformed here.

## Portable reproduction

Prepare a fresh selected-source root containing the four exact preimages in
`preimage-custody.json`, with the new test absent. Use the qualified
`m3-mask-native-atoms-runtime-20261021` crate after its documented Maven run and
the sealed sibling `m3-mask-range-atoms-20261021` evidence packet. Then run:

```sh
python3 verification/verify_target_native.py \
  --repository /path/to/fresh-selected-source-M3JDK21 \
  --crate /path/to/synexia-openrewrite-recipes/recipe-crates/m3-mask-native-atoms-runtime-20261021 \
  --source-crate /path/to/synexia-openrewrite-recipes/recipe-crates/m3-mask-range-atoms-20261021 \
  --preimage-custody /path/to/preimage-custody.json \
  --java-home /path/to/jdk-21 \
  --output /path/to/fresh-evidence-directory
```

The driver requires an exact initial four-file subset; it deliberately refuses
a current receiver that already contains the added test. Its three copier
invocations include the replay check internally. Choose a new output directory.
A GNU/Linux toolchain providing `gcc` and `nm` is required; explicit `--cc` and
`--nm` paths are supported. Actual historical paths and argv are retained in
`evidence/commands.json` rather than substituted in the original logs.

`evidence/input-custody.json` binds all source, oracle, proof and template inputs;
`toolchain-custody.json` identifies tools and JNI headers;
`generated-build-artifacts.json` records the fresh library, generated header and
class identities. `results.json` separates actual execution from reused proof.
Build products and the isolated copier working copy are reproducible and are
excluded from the handoff. The driver is a narrow adaptation of the already
qualified Synexia emitted-output verification workflow, preserving its tests
and expected outputs. Source file-level licenses remain unchanged; the authored
driver and this document use Apache-2.0.

Target metadata, receiving admission, PR publication and promotion are parent
work and were not performed by this verification task.
