<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Target receiver: typed filtered/sorted collection contracts

This successor follows merged M3JDK21 PR1038 on receiving branch aix/m3jdk-purpose-wrapper-receiver-20261017,
base bed96f73a74bfe146bf05346f3a260036093ba89. Its four exact source inputs match the sealed proof
base; other merged files are retained. It refines the existing java.util.CollectionViews
owner; the seven-factory legacy facade and all other owners stay unchanged. The JNI change is
in the existing verification-only RunSearchJni test, not a production accelerator.

Synexia PR12038, commit556b7a28722b459a9bb63183273e1b12f720623a, owns the reusable recipe.
SOURCE_RECEIPT.json pins its exact28-file task tree. The recipe/ directory is a byte-identical,
independently verifiable authoring/proof mirror, not a second canonical implementation.
It adds no Synexia dependency to java.base or native runtime. Modify the canonical recipe and
rerun it rather than hand-editing the mirror or generated production sources.

## Reproduce on this checkout

Supply the approved offline Maven and dependency cache. From the repository root, use a new
absolute output directory (proof requires it not to contain previous outputs):

```sh
TASK=m3/runtime-integration/java-util-views/typed-contracts/recipe
mvn -o -B -ntp -Dmaven.repo.local=/approved/repository -Dproof.root=/absolute/new-proof -f "$TASK/pom.xml" verify
python3 "$TASK/check-receiver.py" --root . --sdk /absolute/new-proof/sdk-results
```

The first command executes five real OpenRewrite JUnit methods and serial Java/JNI target
verification. The second read-only command confirms that this checkout has the exact three
SDK result bytes and two unchanged dependency owners. Neither command auto-promotes a PR.
The frozen historical parent task ledger remains unchanged; use this successor for its
intentionally corrected source hashes. Existing hosted and wider product gates remain required.

## Review scope

Filtered Set/SortedSet cursor metadata and comparator identity propagate through splits;
size saturates without early predicate termination. The C verifier uses bounded regions,
checks all runs after locating an answer and preserves pending exceptions. No source payload,
per-element node, automatic sorting cache or interner is added. Existing public descriptors and
JNI symbol remain. Two new private fields belong to each cursor, not each element.

See recipe/README.md, CATEGORY_REVIEW.tsv, FILE_PASSES.tsv and evidence/SUMMARY.json for exact
contracts and finite results. Original/completed raw logs are hash-bound by RAW_RECEIPTS.json
and delivered in the conversation archive, not asserted to be in Git. No universal speedup,
all-collections completion or source-built matched M3JDK21 success is claimed. Keep draft until
source-built JDK/jtreg/JCK, full reactor/provider/platform/performance/CI and inherited java.base
source-license admission gates are resolved.
