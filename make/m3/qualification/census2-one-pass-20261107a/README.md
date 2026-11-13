<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# census2 one-pass family 20261107a: decision record

This directory records decisions for the six census2 `LEAVES_PROPOSED.tsv` rows of crate `m3-one-pass-20261020`:
P2-026, P2-027, P2-028, P2-029, P2-030 and P2-093. The census marked P2-026, P2-027, P2-029 and P2-030
LOWCONF_NOPRE, meaning no preimage was found. The delta was therefore rebuilt from:

- the crate's own `PASS_LEDGER.tsv` and `RECIPE_PLAN.json`
- its hash-pinned snapshots on com.synexia `develop` at `78cf2ad4c7e5`, read with `git show` from the blobless clone
- the owner on M3jdk21 `master` at `e1d0d8450ff4`

Each crate file was compared with master's owner history by blob identity, then through census pass 1's normaliser.

No row needs an edit in this repository, so this PR changes no source.

| decision | rows | edit |
|---|---|---|
| SUPERSEDED | P2-027 | none: the crate's MFiles postimage and MFilesParallelTest landed byte-exact in `c6e0d9a26a7c`. `4d40df38b5b3` (`FileTask.andThen`) and `a081d0199cc0` (borrowed NIO paths) account for all 43 residual lines |
| SUPERSEDED | P2-026 | none: the snapshot is MFileContent at `c6e0d9a26a7c`. `a081d0199cc0` replaced its `input.transferTo` with the no-flush copy loop |
| SUPERSEDED | P2-029, P2-030 | none: the snapshots are the compiletime owners at `46ee4ad33e90`. `98483791030c` (`fromCaptured`) and `836bdbb9d0cc` (#2547, lazy and frozen-input source seals) changed them later |
| NEC | P2-028 | none: a name-only pairing of Synexia's recipe-memory tool with the different `jdk.m3.mop.M3RecipeMemory`. Master `m3/mop/MOP_PILLARS.md` says the M3 owner is not involved |
| ALREADY | P2-093 | none: #2670 compiles the vendored mindex AST into m3/ports/compiletime (lane com-synexia-4d) |

- `DECISION.tsv` gives each row's crate source, master owner, evidence and action.
- `CHAIN.tsv` gives the delta chain: blob identities and normalised steps per commit.
- `PROBES.tsv` is the mechanical form of every decision.
- `GATES.tsv` lists what was run.
- `contradict.py` puts the crate's superseded m3/fs lines back into copies of the owners for the contradiction gate.
  Master tests fail on those copies: 2 for P2-026 and 10 for P2-027.

`verify.py` re-checks the probes. It reuses the probe atom of `../census2-small-20261107a/verify.py`:

```text
python -I m3/qualification/census2-one-pass-20261107a/verify.py [--rev REV]
```

A FAIL means master moved under a decision, and that row needs a new decision.
