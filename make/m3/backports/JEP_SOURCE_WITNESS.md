# Exact source witnesses for released-JEP borrowing

This is a **read-only evidence join**, not a new backport engine, a replacement
for the canonical Maven/OpenRewrite recipe system, or an authorization to copy
every OpenJDK feature wholesale.

M3JDK21 derives from OpenJDK 21. Its inherited JDK 21 JEPs remain part of the
baseline and must be protected by existing JDK/jtreg regression gates. The
post-21 authority is the existing 85-row, JDK 22–27
[JEP catalogue](JEP_CATALOGUE.tsv) and its [official release authority](RELEASE_JEP_AUTHORITY.tsv).
The wider upstream commit denominator also includes non-JEP JBS fixes; use
[inventory.py](inventory.py) and the [compatibility policy](COMPATIBILITY_ADMISSION.md)
rather than treating the JEP catalogue as all OpenJDK source changes.

## What the new witness actually proves

[jep_source_witness.py](jep_source_witness.py) joins **every catalogue row** with
the existing per-JEP recipe directories and their
CURRENT_TREE_RECEIPT.tsv files. If a receipt includes the specific
product_path/product_sha256 pair, it checks the current target bytes against
that SHA-256. It also checks for invalid receipts, duplicate receipt keys,
noncanonical paths and symlink aliases.

The exact states are:

| Witness state | Meaning |
| --- | --- |
| NO_PACKET | No recognized recipe packet directory exists |
| PACKET_WITHOUT_RECEIPT | A recipe packet exists, but has no current-tree receipt |
| RECEIPT_WITHOUT_PRODUCT_HASH | A receipt exists, but provides no exact product-path/hash pair |
| RECORDED_PRODUCT_HASH_MATCHED | One or more receipt-named product files match the recorded SHA-256; no acceptance is inferred |
| SOURCE_DRIFT | A receipt-named product file is absent or differs from the recorded hash |
| INVALID_RECEIPT | The receipt/path/hash input is malformed or unsafe |

The last two states can be made fatal with --fail-on-drift. The default
mode emits observations even when older, historical receipts are stale:
a failed or incomplete receipt must stay visible, not disappear from an
ostensibly successful backport inventory.

The output deliberately hard-codes
java21_compatibility=NOT_PROVEN_BY_WITNESS and
promotion=NOT_AUTHORIZED_BY_WITNESS for every row, including hash matches.
A product-path hash is **not** an upstream licensing/provenance check, full
JEP code-coverage assertion, complete dependency-closure proof, compiler
result, jtreg pass, runtime image, JNI/VM qualification, or benchmark.

This prevents a common false promotion:

    packet/receipt exists -> source bytes are present -> JEP is implemented

Only the middle statement is sometimes established, and then only for
explicitly witnessed files.

## Run and verify

From repository root:

```bash
python3 m3/backports/jep_source_witness.py --root . \
  --out m3/backports/target/jep-source-witness.tsv
python3 -m unittest discover -s m3/backports \
  -p test_jep_source_witness.py -v
```

The Maven backport reactor also emits this TSV in its verify phase, and the
existing test_*.py discovery executes the focused regression tests in its test
phase. Output writes are confined to m3/backports/target/ and reject symlinks
and hard-linked output aliases; product source is never modified.

## To actually borrow implementation code

Keep the already-established sequence:

1. **Inventory** the full relevant upstream OpenJDK commit/JBS/JEP lineage,
   including later follow-up fixes, with immutable commits and exact paths.
2. **Classify** Java 21 semantics, default behavior, toolchain, API,
   class-file, JNI/HotSpot/GC/JIT and license boundaries before selecting code.
3. **Reuse** an approved canonical Synexia Maven/OpenRewrite recipe or add
   the smallest needed recipe there, with preimage, donor and postimage seals.
4. **Materialize** exact eligible Java, native and build-file leaves in
   M3JDK21 through the existing recipe/DAG machinery; do not paste a newer
   entire source tree over the modified JDK 21 owners.
5. **Prove** recipe tests and fixed point; configure/make; matched jtreg,
   JUnit, HotSpot/JNI/JVMTI, GC, security, platform, and compatibility
   regressions; then before/after CPU/heap/RSS where performance is claimed.
6. **Submit** narrow reviewable PRs; keep holds and exclusions explicit and
   never infer completion merely from copying.

OpenJDK files retain their individual **GPLv2 / Classpath-exception status
where expressly applicable**, plus upstream copyright and modifications.
There is no blanket permission to relicense HotSpot or JDK source into the
Apache-2.0 Synexia programme. Respect the existing
[SYNEXIA borrowing policy](../../SYNEXIA_APACHE_BORROWING_POLICY.md).

This audit does not change source import order
(String -> arrays -> collections -> AST/compiler -> remaining families)
or override the current String-first product-acceptance stage.
