<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# census2 small batch 20261107a: decision record

This directory records decisions for 10 rows of the census2 `LEAVES_PROPOSED.tsv` small batch: P2-016, P2-067, P2-068,
P2-097, P2-104, P2-113, P2-118, P2-139, P2-147 and P2-148. Each row was checked in two places:

- the donor on com.synexia `develop` at `78cf2ad4c7e5`, walked with `git log --follow`
- the owner on M3jdk21 `master` at `2274e18709b8`

No row needs an edit in this repository, so this PR changes no source.

| decision | rows | edit |
|---|---|---|
| ALREADY | P2-016 | none: after the JDK naming rename, the donor is byte-identical to `m3/core/test/FoundationTest.java` (blob `6074d4cc060c`) |
| SUPERSEDED | P2-067, P2-068 | none: `6311c7c8bb3` replaced the crate's transcript-hash contract (also recorded in `make/m3/collections/qualification/census2-c2-20261106`) |
| HANDOFF | P2-104 | lane com-synexia-4d: the owner would be java.base `jdk.internal.m3.classictools` |
| HELD | P2-113 | needs a remote-display owner (Sequoyah WRAP row, not landed) and the `synexia_recipe_native` JNI library |
| HELD | P2-118 | needs `com.synexia.visual.PixelFrame`, which has no owner on master. NEC with `M3ImageFilter.makeDilate`/`makeErode` |
| OMIT | P2-097, P2-139, P2-147, P2-148 | none: a recipe fixture, a test-only JNI probe, and default-package harnesses whose subject is a crate-compiled before/after pair |

- `DECISION.tsv` gives each row's donor blobs, master owner, evidence (cited commits and paths) and action.
- `PROBES.tsv` is the mechanical form of every decision.
- `GATES.tsv` lists what was run.
- `DEVELOP_HEADS.tsv` gives the `git log --follow` head of every donor path on develop.

`verify.py` re-checks the probes:

```text
python -I make/m3/qualification/census2-small-20261107a/verify.py [--rev REV]
```

On `2274e18709b8` the result is probes=27, failures=0. A FAIL means master moved under a decision, and that row needs a
new decision.

Findings:

- No LOSS. The tests already on master pass:
  - FoundationTest under four VM modes
  - M3FirstVisibleAtomTest under jit and `-Xint`
- Coverage gap (not a LOSS): no m3/core test exercises two `SharedLexiconCatalog` contracts:
  - `close()` releases cached payloads (`2e5af99316b`)
  - a warm lookup does not re-enter `computeIfAbsent` (`b31bda6823f`)

  The only tests for them are the P2-148 crate harnesses.
