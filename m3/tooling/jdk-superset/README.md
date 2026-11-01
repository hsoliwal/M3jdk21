<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
<!-- Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others -->
# JDK superset: one fast implementation in the JDK per duplicated capability

Rule (user, 2026-10-31): when libraries each carry their own fast implementation of something, M3jdk owns one fast
implementation inline in the JDK, and the libraries use it. The M3 API of a domain is the superset of the donors' APIs.
Upstream library APIs stay available (Jackson, Lucene, OpenNLP keep their packages); see the memory rule
"keep upstream + M3 facades". Every step is atomize, then patternize, then a recipe, then gates.

## number-parse: FastDoubleParser inlined into java.base (done)

The duplication: java.base `FloatingDecimal`, plus three vendored FastDoubleParser copies (0.9.0, 1.0.0 and 1.0.90,
one per jackson-core version).

1. Atomize FastDoubleParser 1.0.90 and `jdk.internal.math` together: 48 files, 511 leaves, 277 structural classes,
   3 of them shared (`number-parse/ATOMS_PROOF.json`, `PATTERNS_STRUCTURAL.tsv`).
2. Patternize: NUMBER-PARSE-PATTERN v1 places every type on the axes role, grammar, target and carrier
   (`number-parse/PATTERN_MATRIX.tsv`, `tools/pattern_matrix.py`). For `Double.parseDouble` and `Float.parseFloat`,
   java.base needs 13 of the 39 types:
   - the Java grammar, for the CharSequence and byte[] carriers;
   - the double/float instances;
   - the kernels.
   15 more belong to a BigDecimal/BigInteger leaf. 10 are not needed: the JSON grammar, and the char[] carrier.
3. Recipe:
   - `tools/absorb_rename_generic.py` renames `ch.randelshofer.fastdoubleparser.T` to `jdk.internal.math.M3T`
     (`TYPE_RENAME.tsv`). The classes stay package-private, next to `FloatingDecimal`.
   - `tools/jdk_self_call.py` (JDK_SELF_CALL v1): the donor's slow paths call `Double.parseDouble` /
     `Float.parseFloat`. Inside java.base those calls recurse, so they now call
     `FloatingDecimal.readJavaFormatString`. The gate's first run found this as a StackOverflowError.
4. Wiring: `FloatingDecimal.parseDouble` / `parseFloat` try the absorbed parser first. A null input, or any input it
   rejects, takes the original path, so results and exceptions are exactly the JDK's.
5. Gates (`number-parse/GATES.tsv`):
   - The M3jdk `FloatingDecimal` code body is identical to stock 21.0.9, so the patch runs on stock JDK 21 through
     `--patch-module`.
   - Differential: 600,000 inputs, each through `Double.parseDouble` and `Float.parseFloat`, byte-identical results
     and exceptions.
   - Benchmark on the mixed corpus: about 1.5x faster.
   - Licence and notices: `src/java.base/share/legal/fastdoubleparser.md` (MIT; fast_float MIT; bigint BSD-2).
6. Constants (user rule: no magic numbers, no floating strings). Recipe LITERAL_CONSTANTS v1,
   `tools/literal_constants.py`:
   - Inventory (`LITERALS.tsv`): 488 literals to name in the 13 types (409 numeric, 74 char, 5 string). 0, 1 and -1
     stay, and so do the five final tables (1,316 elements). The wiring in `FloatingDecimal` has none.
   - Each literal became a `private static final` constant in its class, named by its role (`CONSTANT_NAMES.tsv`;
     one row per occurrence in `CONSTANTS.tsv`). That is 205 constants. A value with two roles gets two names.
     Rerunning the tool makes 0 edits.
   - Each constant keeps the literal's own type and spelling, so javac folds it back into the code: all 24 classes
     have instruction-identical methods (`tools/bytecode_identity.py`). The differential, the JUnit atoms (11/11) and
     a same-host perf A/B (`tools/perf_ab_high.ps1`, `tools/perf_gate.py`) agree.
   - Strings: no M3 interner is reachable from `jdk.internal.math`, so the 4 strings are compile-time constants, which
     the JVM interns. The gap is recorded in `RECIPE_MEMORY.tsv`.
   - The custody record `src/java.base/share/legal/fastdoubleparser.md` names this change too.
     `tools/custody_check.py` fails when a commit changes absorbed files and no commit up to head updates the record.
7. Commits. The M3jdk worktree is blobless and sparse, with a 25 MB index on a nearly full disk, so commits go through
   `tools/index_free_commit.py`, which writes no index. The index stays at the old HEAD until `git reset -q` runs, and
   until then a porcelain commit there (`git add --sparse`, `git commit`) would revert the committed paths. The tool
   reports INDEX_STALE after each commit (`tools/index_free_commit_check.py` checks that), and the GATES.tsv row
   `index_sync` holds the reset: HOLD while H: is under the 350 MB floor.

Next:
- Point jackson-core's fast path at the JDK (`Double.parseDouble`) by recipe, then drop the three vendored
  FastDoubleParser copies.
- A LATIN1 byte path straight over String's bytes. Only the JDK can do this; a library cannot.
- BigDecimal and BigInteger.
- Try an M3 kernel variant against this baseline.

## csv: three CSV implementations (atomized; superset in progress)

Donors: univocity-parsers (216 files), commons-csv (12) and jackson-dataformat-csv (19). junit-jupiter-params uses
univocity for `@CsvSource`.

- Atomize: 247 files, 3,648 leaves, 1,262 structural classes; 35 exact and 50 structural classes shared across donors
  (`csv/ATOMS_PROOF.json`, `PATTERNS_*.tsv`).
- API superset v1 (`csv/CSV_API_SUPERSET.tsv`, `tools/api_superset.py`): 242 capability keys from the donors'
  configuration surfaces. Only 8 match across donors by name, because each library names the same concept
  differently (delimiter / separator / columnSeparator, quote / quoteChar, ...).
- Next: a reviewed synonym table (CSV-CAPABILITY v1), then the jdk.m3 CSV API as the superset, implemented once.
  Per the generation rule, new production classes go through Chrome local AI per file; they are on HOLD while it is
  unavailable.
