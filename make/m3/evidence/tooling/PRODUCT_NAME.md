# The product name is one token: M3JDK (2026-11-09)

The user's instruction: M3JDK is one unit, not m3 + jdk. Census of live text (m3/, .github/, make/, src/, test/):
- the split spellings are `M3 JDK` (81), `M3-JDK` (82), `m3-jdk` (42), `M3_JDK` (28) and `m3_jdk` (9);
- evidence holds a further 7,575, which stay.

`m3/tooling/jdk-naming/product_name.py` (PRODUCT_NAME_UNIFY v1, 5 fixture tests) rewrote 31 live files
(`PRODUCT_NAME.tsv`) to the per-kind product name of NAMING_STANDARD:
- `M3 JDK` becomes `M3JDK`: workflow names (M3JDK naming standard, M3JDK modules, M3JDK proof kit, M3JDK source
  naming), step names, docs, a recipe display name;
- the environment name `M3_JDK` becomes `M3JDK`: every script already reads `M3JDK` (m3/build.sh,
  m3/optane/verify.sh, the lexicon and release runners, the qualify scripts); only the docs still said `M3_JDK`;
- `--m3-jdk[-x]` becomes `--m3jdk[-x]`, and `m3_jdk[_x]` becomes `m3jdk[_x]`. Master declares `--m3jdk` and
  `--m3jdk-receipt` but reads `args.m3_jdk` and `self.a.m3_jdk_receipt`, attributes argparse never sets. So
  verify_progress_job.py and tornado/build_runtime.py raise AttributeError on master; with this change they read
  what they declare (checked by AST: every m3* attribute they read is a declared destination).

These keep their bytes:
- invariant ids pinned by ledgers (`M3-JDK-PORT-1`, `M3-JDK-STRING-1`) and com.synexia donor paths (`m3-jdk-*`);
- TSV/JSON ledgers, evidence segments, NAMING_KEEP files and the naming recipe's own files;
- three dated release records that cite the old names on purpose: RELEASE_PREPARATION.md (it describes the old
  confusion between `M3_JDK` and `M3JDK`), CI_FAILURE_TRIAGE_20261107.md and NAMING_FIXED_POINT_EVIDENCE.md.

Gate: all 4 edited Python files compile; the recipe's second run is a fixed point; test_product_name 5/5. Workflow
job names are unchanged, so no required-check context changes; only the workflow and step display names do.
