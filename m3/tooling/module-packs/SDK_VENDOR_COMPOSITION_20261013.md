<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3SDK developer + vendor JMOD image composition

Status: **opt-in SDK candidate**, not a qualified M3JDK21 product release.

## Reused canonical owners

This operation deliberately reuses existing substrate instead of introducing a second JDK image
assembler or a second OpenRewrite engine:

- `m3/tooling/module-packs/build_pack.py`: the existing Linux Java 21 developer image, toolchain
  fixture, jpackage launcher, linked-app, JMOD link-kit and diagnostics gate.
- `m3/tooling/module-packs/developer_tools.py`: the canonical developer module roots.
- `m3/tooling/jmod-recipes/`: the exact received JMOD descriptor/OpenRewrite recipe and its
  original 87 vendored-module build evidence, protected by PR #810's 52-blob receiver.
- `m3/tooling/jmod-recipes/tools/sdk_image_probe.py`: linked Java 21 image / development-tool
  inventory and actual RE2J + Guava API smoke.

`sdk_compose.py` is only the deterministic DAG fan-in, operating on immutable JMOD inputs.

```text
[Synexia recipe + donor custody]
              |
      [JMOD descriptor recipe]
              |                         [Original M3JDK21 developer pack]
        [87 JMOD images]                         |
              +------------------+---------------+
                                 |
                      [candidate jlink image]
                                 |
                        [actual vendor APIs]
                                 |
                      [SDK candidate receipt]
                                 |
          [OpenJDK build / jtreg / JNI / release proof PENDING]
```

OpenRewrite/JUnit is the recipe admission authority. This Python runner does not replace it.

## Build

Build JMODs from the already pinned and licensed source artifacts using the original recipe tools.
Use a reviewed `JMOD_PACK.tsv` and its matching directory of 87 separately emitted `.jmod`
files. These are **inputs**, not downloaded or generated implicitly by the SDK composer.

From repository root, using a real Java 21 development JDK:

```bash
python3 m3/tooling/module-packs/sdk_compose.py \
  --jdk /path/to/reviewed/jdk21 \
  --vendor-jmods /path/to/reviewed/jmod-pack/jmods \
  --vendor-pack /path/to/reviewed/jmod-pack/JMOD_PACK.tsv \
  --out /fresh/external/m3sdk-candidate
```

The script validates identity and all 87 named modules *before writing output*. It invokes the
existing developer pack (which builds its own fresh image and JMOD link inputs), links the combined
developer + 87-vendor module closure using that JDK 21's `jlink`, then runs a real Java 21
compiler/API execution smoke in the new image.

The output contains `developer-stage/`, `image/`, `logs/`, `runtime-proof/` and
`SDK_CANDIDATE_RECEIPT.json` on success. Occupied outputs, output under `JAVA_HOME` or
the source repository, incomplete/extra vendor JMODs, changed inputs, collisions with platform
modules, uncontrolled VM flags, Java versions other than 21, non-Linux developer builds and
runtime failures are refused.

## Tests / independent proof

```bash
python3 -m unittest discover -s m3/tooling/module-packs -p 'test_sdk_compose.py' -v
python3 -m unittest discover -s m3/tooling/jmod-recipes/tools -p 'test_*.py' -v
mvn -B -ntp -f m3/tooling/module-packs/pom.xml verify
mvn -B -ntp -f m3/tooling/jmod-recipes/pom.xml test
```

The unit tests use synthetic bytes **only for preflight/refusal**. They are never called a valid
JMOD, an application launch, or a runtime success. Only the explicit `sdk_compose.py` command on
a real candidate image can create the local runtime proof receipt.

The result always has `product_accepted=false` and
`java21_spec_compatibility=NOT_PROVEN_BY_THIS_CANDIDATE`. No SDK image should be advertised
as production-ready until native OpenJDK `make images`, applicable jtreg, ABI/JNI/GC/CDS checks,
rights/attribution, platform matrix and the existing release gate are satisfied.

## Backport independence

JDK 22–27 compatible backport residue is not waived by SDK packaging. Keep the JEP 423, 458,
474, 484 and following packets in the existing `JEP_RESIDUE_QUEUE.tsv` order, each with
source-pinned OpenRewrite atoms, Java 21 behavior proof, second-pass fixed point, product image
tests and serial promotion. This SDK DAG grants no JEP compatibility or mutation authority.
