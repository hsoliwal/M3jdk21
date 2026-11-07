<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/core: repair the JDK-naming rename damage that stopped m3/build.sh

`m3/build.sh` stopped at `M3NumberSpaceContractTest` on master. Replaying it fix by fix (GATES.tsv row 2) found three
kinds of damage from the rename fe35ea98841. No production source changes.

1. **Donor facts renamed in test literals.** Three tests compare manifest rows against the donor source path in
   hsoliwal/com.synexia (`dictlang`, `interned-language` with its `com/<donor>/language` package, `unicodex`). The rename
   rewrote the literals to `m3-...`; the manifest TSV, which the rename skipped, kept the true donor path.
   The tests now compose the donor path from parts, with a comment marking it as a donor fact, so no future rename
   rewrites it (the naming fixed point holds).
2. **An escaped span.** `"0\tsynexia.m3lex..."` kept the old fixture name while the same test writes `m3.m3lex`.
   SEGMENT_DOT refuses a letter before the segment, and the `t` of the `\t` escape is a letter.
3. **Our own names, stale in data.** The code reads `m3.records.tsv` and `m3.<family>.tsv` sidecars and the owner
   type `jdk.m3.text.M3NumberSpace`. The field map and target map still named `synexia.*` and `com.m3.text.*`.
   76 cells now follow the code: 56 records, 19 family and 1 owner. Donor columns are untouched.

After the fixes, `m3/build.sh` passes 72 of 72 runs (18 programs × 4 modes). The core-contract manifest is re-pinned.
The lesson is recorded in m3/tooling/jdk-naming/RECIPE_MEMORY.tsv (PATTERN:DONOR_FACT_AND_ESCAPED_SPANS).
