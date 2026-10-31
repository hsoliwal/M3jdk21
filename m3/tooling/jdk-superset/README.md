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
