<!-- SPDX-License-Identifier: Apache-2.0 -->
# retained-port-20261107: merge-loss restore of `m3/llm` (PR #2108) as `jdk.m3.llm`

PR #2108 (head `58bc0d2d297e`, the second parent of the retain merge `a92a5765d54a`) added the Chrome LLM and
summarization module `m3/llm` (44 production files, module `com.m3.llm`). A later fold of master dropped the whole
module; `S:/m3-scratch/eb-conv/folddrop/retained/m3__llm.tsv` lists the 44 files with `merge_kind RETAIN`. Master
`c7be9541dbad` has no `m3/llm` at all, so this leaf is a whole-module port: every file of the module is written back
from its git blob at `58bc0d2d297e`, the exported package is relocated `com.m3.llm -> jdk.m3.llm` by the
`m3/tooling/jdk-naming` rules, and the module is registered in `m3/pom.xml` after `nlp`.

Chrome rule (user, 2026-10-31): Chrome-generated production classes are held pending a per-file Chrome work order.
These 44 classes were authored and reviewed in #2108 and later landed as a retained variant; this leaf restores that
reviewed code after a merge loss. It generates nothing.

## Decisions (`DECISION.tsv`)

| Decision | Listed files (44) | Note |
|---|---|---|
| RESTORE_EXACT | 43 | compile as restored after naming; `javap -p` identical to the donor class; differential identical |
| ADAPT | 1 | `ChromeMlTaskService`: `jdk_naming` CONST_KEBAB renamed the capabilities schema id `SYNEXIA-CHROMEML-CAPABILITIES/1 -> M3-CHROMEML-CAPABILITIES/1` (the only runtime string this restore changes; `//FLLDCIM: MRL` anchor); no API change |
| SUPERSEDED | 0 | master has no owner of any restored type (`M3LuhnSummarizer` exists only under `m3/qualification/master-landing-20261028/retained-variants`, not a source owner) |
| HOLD | 0 | |

Beyond the list, the module's own files are restored too (33): `pom.xml` (relocated `targetPath`), `README.md`
(relocated names), `LICENSE`, `NOTICE`, `SOURCE_PROVENANCE.tsv`, `PENDING_FAMILIES.tsv`, `RECEIVER_RECEIPT.json`
(lineage, byte for byte), 7 browser `.mjs` resources (byte for byte) and the 19 test sources of #2108 (8 JUnit classes,
11 `main`-based self tests they wrap). API drift: none; the module imports only `java.*`, `com.sun.net.httpserver`
and itself (no `jdk.m3.collections`/`arrays`/`core`, no progress monitor), so no API adaptation was needed. The
module keeps `requires transitive jdk.httpserver` exactly as #2108's `module-info.java` (`ChromeMlTaskService` serves
`HttpExchange`); nothing outside `java.base` and `jdk.httpserver`.

## Recipe

`python -I apply.py [--check] [--donor-dir DIR]` over `RESTORE.tsv` (77 rows: target path, donor blob at
`58bc0d2d297e`, donor sha256, target sha256, decision, rule). Each blob is read with `git cat-file`, relocated
(`PKG_COM_M3`, `PKG_PATH_COM_M3` on `.java`, `pom.xml`, `.mjs`, `README.md`), edited (`EDITS`: the one CONST_KEBAB
rename) and written only when the target is absent; a target holding other bytes than the postimage, a donor blob
that does not hash to its recorded sha256, or a produced target that does not hash to its recorded sha256 refuses
with exit 2 before any write. `EDITS.tsv` records before/after sha256 per written file (and the `m3/pom.xml`
registration from the base blob `eadf233183d4`). Second run: `changed=0`; `jdk_naming.py plan|apply --only m3/llm/`:
`moves=0 rewrites=0 refusals=0` (fixed point).

## Gates

`GATES.tsv`; scripts `differential.sh` (seeded donor-vs-restored dumps over the summarizers, canonical JSON, BM25
projection and task planning; identical except the declared `capabilities` line), `javap_parity.sh` (103 classes
`javap -p` identical; the ADAPT class differs in one `ldc_w`), `mutants.py` (`MUTANTS.tsv`: M1 drops the surrogate
guard of `plan()`, killed by `ChromeMlTaskServiceMigrationTest`: expected 7999, was 8000).

Branch: `m3/llm-retained-port-20261107c` (worktree `wt-llm-c`, from commit `9c3c8259ae2`); the earlier `-a` worktree had a second writer, so its branch (and any PR opened from it) is a duplicate of this one and is left untouched.
