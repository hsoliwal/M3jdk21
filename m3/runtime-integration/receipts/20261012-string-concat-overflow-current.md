# M3JDK21 current String concat overflow receiver — candidate

## Documentation before code and inventory

- **Canonical implementation/recipe source:** [Synexia #10560](https://github.com/hsoliwal/com.synexia/pull/10560)
  at commit `3ae820846d76f9bf32d7c10f5610f3f562952052`.
- **Product receiver:** `hsoliwal/M3jdk21`, inventory `master@63bef2829d762110ad9322504acc0d3b3e391a94`.
- **Canonical recipe:** `com.synexia.rewrite.M3Jdk21StringConcatOverflowCurrentReceive`,
  using the existing Java-LST and text-source hash-pinned engines.
- **Actual pool preimage:** `bcff46c8b32a3fd2a45f13fd4466e2cc9f9693a6`.
  This is byte-identical to the older Synexia #10158 source preimage,
  including native cache budgets and weak tuple retention.
- **Current workflow preimage:** `1f3c7d140ed7eb9ceca6530ff003e06d16ef5d45`.

The JDK runtime has real `java.lang.String` integration with canonical
`M3String` owner/coordinate storage. This is not a standalone CharSequence
wrapper. `M3StringPool.concat` allows joining two valid logical strings
whose combined length exceeds `Integer.MAX_VALUE`; the old code
reached `Math.addExact(int,int)`, leaking `ArithmeticException` instead
of the JDK String logical-size-limit `OutOfMemoryError`.

## Product candidate boundaries

- Exactly one existing `src/java.base/share/classes/java/lang/M3StringPool.java`
  source changes, inserting the six-line long-width limit check after
  the zero-length guards and before adjacent-coordinate or tuple construction.
- One **create-only corrected** `M3StringConcatLengthOverflowTest.java`,
  which checks large logical no-flattening String.concat/join and the
  separate StringJoiner overflow at its **second add**, both
  `+CompactStrings` and `-CompactStrings`.
- One *existing* `mindex-string-backing.yml` receives one prebuild
  source/refusal gate and **one additional selector**, bringing 23
  existing jtreg selectors to 24 while retaining current images,
  JNI/HotSpot, regex, resources and other proof steps.
- Two create-only read-only Python source/negative tests. All five
  source files are materialized **byte-for-byte** from Synexia
  canonical resource templates (no duplicate recipe implementation).

## Source pins

| Product path | Complete postimage SHA-256 |
| --- | --- |
| `M3StringPool.java` | `f8bac45d1623d25470e1b79beffa276f271db10e297ef5b434d5335c9d4c908b` |
| `M3StringConcatLengthOverflowTest.java` | `587f7f63fb419970ffdc889bca05335eb3f49ebcc97d3810906b3587ff9e7ab4` |
| `mindex-string-backing.yml` | `927ba49c51709896a4610d89daf4d669217e27713153cd46f863aafb0dd63989` |
| `check-m3string-concat-overflow-current.py` | `e16460bd56377ca59c395e3336dad5914cab1d46c8423b22ee7a0c4366e38cf8` |
| `test_m3string_concat_overflow_current.py` | `5a28b85c7f6e5af794acf6663d32ac5a5c82d31e67fc6f22e1a1dc688925eab4` |

## Verification authority

The historical corrected jtreg and stock-JDK AST guard proof (their exact
Git blobs) were **compiled under Java21 with -Xlint:all -Werror**. Stock
`StringJoiner.checkAddLength` and the AST guard passed; hostile
`joiner::toString` was correctly rejected. This does not exercise
a modified JDK image.

The target's source checker and 10 mutation tests must verify the
**active delivered tree** before configure/build. Full JDK `make images`
and the 24-test jtreg run including both CompactStrings modes are
mandatory, followed by existing JNI, HotSpot, GC, CDS and full
reactor/platform gates. Source SHA matching is *necessary, not sufficient*.

Do not set a PASS for source Maven/OpenRewrite SDK, JDK image, jtreg,
JNI, full reactor or performance without completed logs. No root-branch
writes, rebase, squash or force push. JDK derivative files retain
GPLv2 with Classpath Exception; canonical Synexia recipes and
new helper source are Apache-2.0. LeetCode/HackerRank/GfG donor
algorithms remain in their separate contract-review lanes.
