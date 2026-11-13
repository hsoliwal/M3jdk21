<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Array recipe source custody

This directory preserves the exact canonical Synexia checkpoint used to author
the array refinement, together with the four existing recipe-engine and scope
sources it compiles. The retained datasets are `fixture` and `m3jdk21-arrays`.
[SOURCE.json](../SOURCE.json) pins the canonical publication, and
[OWNER_CUSTODY.json](OWNER_CUSTODY.json) maps every original source path to its
public mirror with byte counts, SHA-256 and Git blob identities.

Synexia remains the owner of the reusable transformations. These files are
pinned authoring evidence for the receiving array module. They add no runtime
dependency, recipe authority or alternate implementation owner. Future changes
must update the canonical recipe inputs and pass qualification before receiving
new source results.

## Preserved sources and licenses

The 96 canonical crate files retain their original bytes, including its Maven
POM, Python sealing and receiving tools, SDK test, contract tests, fixture/array
datasets, evidence, licence, notices and lineage. `sdk-owner/src/main/java`
contains the exact existing `M3HashPinnedJavaSnapshotRecipe`,
`M3HashPinnedTextSnapshotRecipe`, `M3ScopeInference` and `M3EditScope` sources
pinned by [ENGINE_SOURCES.tsv](ENGINE_SOURCES.tsv).

The canonical [README.md](README.md) is preserved as source evidence. This
document supplies the concrete commands for the two datasets retained here.
[HARNESS_LINEAGE.json](HARNESS_LINEAGE.json) and
[NOTICE.md](NOTICE.md) preserve the existing reuse decisions. First-party source
headers and [LICENSE.txt](LICENSE.txt) remain in place; receiving source and
Maven dependencies retain their applicable licenses.

Archived evidence paths ending in `.log` have `.txt` appended because the
existing text recipe admits that extension. Their bytes are unchanged, and the
source-to-mirror mapping is recorded in `OWNER_CUSTODY.json`. The frozen harness
itself retains its normal output names. Fresh executions generate independent
reports in `target/<dataset>`.

## Reproduce the source transformations

Use Java 21, Maven and Python 3. The frozen POM pins OpenRewrite 8.17.1 and JUnit
5.10.2 for this authoring proof. Its default owner path remains unchanged;
override it with the retained `sdk-owner` directory. From this directory, run:

```sh
mvn -B -ntp -Dm3.dataset=fixture \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify

mvn -B -ntp -Dm3.dataset=m3jdk21-arrays \
  -Drecipe.owner.root="$PWD/sdk-owner" -Pcontract verify
```

Use Maven's `-o` option when these dependencies are already cached. Each command
executes five actual SDK tests, eight input-sealing tests and nine isolated
receiver contract cases. SDK output is retained under
`target/<dataset>/candidate`, including `SDK_INPUTS.tsv`, `SDK_RESULTS.tsv` and
the emitted source files. A fresh output directory retains a new qualification
without replacing earlier evidence.

The tests cover typed/lossless Java and exact text/native output, named recipe
composition, changed input order, mixed pre/postimages, absent-before targets,
fixed point, source drift, tampered inputs/results, symlink refusal, and
file-mode/timestamp preservation. The fixture qualifies the reusable receiving
machinery. The array dataset binds the four array source/test outputs. Product
behavior is established separately by the actual received-module evidence in
[the array qualification README](../README.md).

## Receive qualified array results

Set `M3_ARRAYS_TARGET` to an isolated M3JDK21 checkout containing the recorded
preimages or independently converged postimages, then run from this directory:

```sh
python3 receive.py --dataset m3jdk21-arrays --target "$M3_ARRAYS_TARGET" \
  --recipe-owner "$PWD/sdk-owner" --check

python3 receive.py --dataset m3jdk21-arrays --target "$M3_ARRAYS_TARGET" \
  --recipe-owner "$PWD/sdk-owner"

python3 receive.py --dataset m3jdk21-arrays --target "$M3_ARRAYS_TARGET" \
  --recipe-owner "$PWD/sdk-owner"
```

The receiver checks the qualified engine/input/result bindings before writing
the retained SDK Results. A repeat application at the fixed point writes zero
files and preserves timestamps. Different source bytes are refused for review.
Serial atomic file replacement does not provide a cross-file transaction under
concurrent modification or interrupted I/O.

The embedded datasets do not target this custody directory or the surrounding
review evidence. A separate qualified custody dataset receives those files,
keeping the source-receipt chain finite. The array module's original Maven POM
and native profile remain the product build and runtime acceptance entry points.
