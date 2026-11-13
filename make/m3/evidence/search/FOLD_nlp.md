# jdk.m3.nlp folded into jdk.m3.search (2026-11-08)

This is the approved layout's first family fold: jdk.m3.search = search + indexdb + nlp. The module root m3/nlp, its
package jdk.m3.nlp and its descriptor `module org.m3jdk.nlp` are gone. The 30 types now sit in jdk.m3.search without
the M3 prefix: 4 main, 26 test.

The fold ran as one recipe command (`FOLD_nlp.tsv` holds the rows):

    python -I make/m3/tooling/jdk-naming/fold_package.py --root . --from-root m3/nlp --from-package jdk.m3.nlp \
      --to-root m3/search --to-package jdk.m3.search --exclude make/m3/tooling/jdk-naming/ \
      --retire-to make/m3/evidence/search/nlp --aggregator make/m3/pom.xml
    python -I make/m3/tooling/jdk-naming/rename_types.py --root . --package jdk.m3.search \
      --exclude make/m3/tooling/jdk-naming/ --doc m3/search/ --string-words --comment-words

`fold_package.py` (FOLD_PACKAGE v1, new, 7 fixture tests) did the following:
- moved the package's sources;
- moved the module's records byte for byte to `make/m3/evidence/search/nlp/`: GATES, PROVENANCE, RELOCATION_MAP,
  RECIPE_MEMORY, README, recipe/ with the three relocation leaves, verify.sh, pom.xml and module-info.java;
- removed `<module>nlp</module>` from make/m3/pom.xml.

`rename_types.py` v1.7 then dropped the prefix. A hand-sequenced run (retire, fold, rename) produced the same tree,
byte for byte, apart from the two declared edits in `DECLARED_EDITS_fold_nlp.tsv`: the search README (stale names
from #2818, plus a section on the fold) and the baseline (the nlp ROOT, the PACKAGE row and 30 PREFIXED rows removed).

Inventory before the move:
- `pom_dependents.py`: m3-nlp has 0 dependents.
- A scan of m3/, .github/ and make/ in every form (`jdk.m3.nlp.X`, `jdk/m3/nlp/X`, `m3/nlp/`, artifact,
  `org.m3jdk.nlp`, `<module>nlp</module>`) found only the reactor line and the baseline outside the module.
  `jdk.m3.nlp.search`, a package of the nlp-search module pack, is another package and stays.
- No simple name of nlp, with or without M3, exists in jdk.m3.search.

## Gate (`gate_fold_nlp.sh`, WSL OpenJDK 21; javac -Xlint:all,-module -Werror as the poms; JUnit console)

| | Before: m3/search + m3/nlp | After: m3/search |
|---|---|---|
| javac main / test | rc=0 / rc=0 (both modules) | rc=0 / rc=0, 0 diagnostics |
| JUnit found | 164 + 31 = 195 | 195 |
| successful / failed / skipped | 119 + 31 = 150 / 25 / 14 | 150 / 25 / 14 |
| per-test status (names normalised) | - | identical |
| package_gate (post-change path list) | roots 727, product packages 384 | violations=0, roots 726, packages 383 |

The 25 failures and 14 skips on both sides are the native-library tests, run here without the CMake-built JNI
libraries ("actual native library path is required"). They are not part of this fold.

Next in the family: m3/indexdb. Its recipe-home policy declares `m3/indexdb/**` as MIGRATION_RESIDUE_NOT_CANONICAL,
owned by com.synexia's synexia-m3index-db, and `check_m3_m3indexdb_v1.py` pins a live file's blob. So it needs an
owner decision against `jdk.internal.m3.mindex` (one owner per concept) before it can move.
