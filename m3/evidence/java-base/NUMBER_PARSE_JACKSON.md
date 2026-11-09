# Jackson parses and writes numbers with java.base; the vendored FastDoubleParser copies are gone (2026-11-09, JACKSON_NUMBERS_TO_JDK v1, v2)

User rules: M3JDK is one JDK, so M3 integrates in place and is never a side layer; and the JDK owns one fast
implementation that every consumer uses. NUMBER_PARSE_ABSORB v1 (2026-10-31) put FastDoubleParser 1.0.90's double
and float parsers in java.base, and v2 (`NUMBER_PARSE_BIG.md`) its BigDecimal and BigInteger parsers. Jackson core
still called its own shaded FastDoubleParser, so `m3/vendor` kept four copies: `third_party/fastdoubleparser-0.9.0`,
`-1.0.0`, `-1.0.90`, and `third_party-m3/relocated/fastdoubleparser-1.0.90` (module `jdk.m3.fastdoubleparser`).
The fdp-delegate lane made the 1.0.90 double/float facades call java.base: a layer beside the JDK, not in it.

## Change

- Recipe `m3/tooling/jdk-superset/number-parse/tools/jackson_numbers_to_jdk.py` (re-runs to a fixed point; `--check`
  writes nothing). In jackson-core 2.15.3, 2.16.1 and 2.19.2 it retargets the FastDoubleParser calls:
  - `NumberInput`: `JavaDoubleParser.parseDouble(s)` -> `Double.parseDouble(s)`, `parseDouble(array, offset, len)` ->
    `Double.parseDouble(new String(array, offset, len))` (the expression of Jackson's own non-fast branch); the same
    for `JavaFloatParser` and `Float.parseFloat`;
  - `BigDecimalParser`: `JavaBigDecimalParser.parseBigDecimal(...)` -> `new BigDecimal(...)`;
  - `BigIntegerParser`: `JavaBigIntegerParser.parseBigInteger(...)` -> `new BigInteger(...)`;
  - the FastDoubleParser imports go; the first becomes a `//FLLDCIM` anchor. Nothing else in the sources changes.
- Custody uses the donors' overlay mechanism (introduced by FDP_DELEGATES_TO_JDK v1): each upstream file stays
  byte-identical under `upstream-excluded/`, `PROVENANCE.json` pins it, the overlay and the recipe, the new
  `OVERLAY_SOURCES.json` lists the overlays, and `verify_source.py` re-derives every overlay through the recipe. The
  regenerated `verify_source.py` also fixes the two bugs of the old template (repository root, `revision` key) and
  checks only the legal files the donor directory has.
- Each pom drops the FastDoubleParser dependency, its shade filter and its four package relocations; the
  LICENSE/NOTICE relocations stay. The relocated jackson-core gets the same overlay through the lib-intake name
  mapping and loses `requires jdk.m3.fastdoubleparser`.
- Jackson pack (`m3/tooling/module-packs/jackson`): the FastDoubleParser donor leaves `build.py` DONORS,
  `source-build/pom.xml` and `SOURCE_LOCK.json`; the recipe relocks jackson-core (the previous lock is
  `number-parse-jackson/SOURCE_LOCK_PREDECESSOR.json`); the built-jar contract now refuses any FastDoubleParser class;
  `legal/fastdoubleparser-1.0.90/` and its `legal_copies` rows go; NOTICE, README, INTEGRATION and
  FOSS_REUSE_DECISION say what is built now. The published Jackson core jar's own resources
  (`META-INF/FastDoubleParser-LICENSE`, `-NOTICE`, `thirdparty-LICENSE`) stay upstream bytes.
- Removed: the four FastDoubleParser trees (244 files), their three `third_party/pom.xml` modules, the
  `m3sdk-jmod-receiver.yml` path filter and their four VENDOR rows in `PACKAGE_BASELINE.tsv`.
- `m3/tooling/fdp-delegate` is retired to `m3/evidence/java-base/fdp-delegate` (its overlay target is gone).
- `src/java.base/share/legal/fastdoubleparser.md`: the origin line says the vendored copy was removed.
- One-off edits: `number-parse-jackson/land_edits.py` (asserts each anchor; a second run changes nothing).
- Left as records: lib-intake runs, catalogue, BATCH and COMBINED_IMAGE receipts; `m3/docs/name-mapping.d`;
  compiletime inventories (jar SHA-256 receipts of a past run); retained variants; jmod-recipes evidence; the tq-sync
  oracle, which pins receiver base a7cfe4931f22 and whose 122 model pins already all differ from master.

## Gates

| Gate | Result |
|---|---|
| recipe | apply, then `--check`: fixed point on Windows and on Linux |
| `verify_source.py` | 2.15.3, 2.16.1, 2.19.2: PASS, 3 overlays each with v1 and 6 with v2; PASS with each Maven-built jar (`MAVEN_BUILD.txt`, v2) |
| javac (`run_gate.sh`) | rc=0, 0 errors: master jackson-core 2.19.2 + FastDoubleParser 1.0.90 (206 classes); branch jackson-core 2.15.3 (140), 2.16.1 (161), 2.19.2 (165) without FastDoubleParser; relocated module (166) without `jdk.m3.fastdoubleparser`; M3 java.base patch (42: FloatingDecimal, java.math, jdk.internal.math.M3* of #2864) |
| Maven | the three jackson-core poms build without FastDoubleParser; 0 FastDoubleParser classes in each jar; the 2.19.2 jar is the published `jackson-core-2.19.2.jar` minus its 42 shaded FastDoubleParser files, plus the M3 provenance records (`JsonToken$1.class`, a compiler artifact, also differs) |
| differential (`JacksonNumbersDifferential`, `GATE.txt`) | 3 seeds x 20,000 JSON numbers (1 to 1,600 digits, fractions, exponents to the int limits) through the char and byte parsers, 4 getters each with the fast double and big-number features on, plus the 8 NumberInput fast methods (a quarter with Java literals: hex, NaN, Infinity, whitespace, suffixes, invalid). Master vs branch, each on stock JDK 21.0.12 and on M3 java.base: master is identical on both JDKs, the branch is identical on both JDKs, and every valid value is identical between master and branch |
| value differences (`VALUE_DIFFS.tsv`) | two, both fixes: `parseBigInteger("", true)` and `parseBigIntegerWithRadix("", 16, true)` threw `StringIndexOutOfBoundsException` (outside NumberInput's `NumberFormatException` contract) and now throw `NumberFormatException` (255 inputs each); a BigDecimal whose scale is exactly `Integer.MIN_VALUE` was refused ("value exceeds limits") and is now the value `new BigDecimal` defines (2 numbers) |
| exception messages (`MESSAGES_DIFF_TOP.txt`) | 33,654 / 34,043 / 34,263 of about 45,400 per seed differ: FastDoubleParser's "illegal syntax" and "value exceeds limits" become the JDK's reasons ("Character x is neither a decimal digit number...", "Exponent overflow.") |
| jackson pack (`PACK_TEST.txt`) | `build.py --stage test` on Linux (JDK 21.0.12, Maven 3.9.14): 3 donor source builds, 11 host contracts PASS (JacksonContract 299 checks, JacksonStreamingContract 962, M3FileContract 20,595, ...); 0 FastDoubleParser classes in the 4 jars. It ran on a scratch lock (see below) |
| package gate | 0 violations; roots 718 -> 714, vendor 157 -> 153 |

## v2: Jackson renders doubles and floats with java.base

JDK 19 and later render doubles and floats with Raffaello Giulietti's Schubfach (`jdk.internal.math.DoubleToDecimal`,
`FloatToDecimal`). jackson-core carries its own copy of the algorithm in `io.schubfach` (DoubleToDecimal,
FloatToDecimal, MathUtils: 1,992 lines per version), which `NumberOutput` uses when `USE_FAST_DOUBLE_WRITER` is on.

- `SchubfachVsJdk` (`SCHUBFACH_VS_JDK.txt`): Jackson's copy against `Double.toString` and `Float.toString` on JDK
  21.0.12, 3 seeds x 30,000,016 samples (random raw bits for both types, decimal-shaped doubles, edge values):
  0 differences. The copy is a pure duplicate.
- The recipe (v2) retargets `NumberOutput` to `Double.toString` and `Float.toString`. `io.schubfach.DoubleToDecimal`
  and `FloatToDecimal` keep their public members (`toString`, the instance field `MAX_CHARS`, private constructor)
  and delegate to java.base, so Jackson's exported API is unchanged. The package-private `MathUtils` tables go. All
  originals stay under `upstream-excluded/`; the v1 overlays and their anchors are unchanged.
- `land_edits.py` updates the pack README, NOTICE and INTEGRATION; replaying it from master reproduces the branch
  files byte for byte.

| Gate (`WRITE_GATE.txt`) | Result |
|---|---|
| recipe | fixed point; `verify_source.py` 6 overlays per donor |
| javac | v1 2.19.2 (165 classes); v2 2.15.3 (139), 2.16.1 (160), 2.19.2 (164), relocated module (165); 0 errors |
| write differential (`JacksonNumberWriteDifferential`) | 3 seeds x 1,000,000 samples through the char and byte generators (`USE_FAST_DOUBLE_WRITER` on), `NumberOutput.toString(v, true)` and the facades: before and after v2 byte-identical (SHA-256 in `WRITE_GATE.txt`) |
| Maven (`MAVEN_BUILD.txt`) | 3 builds pass; each jar keeps the two facade classes, drops `MathUtils` and has 0 FastDoubleParser classes |
| jackson pack (`PACK_TEST.txt`) | `build.py --stage test` on the v2 tree (Linux, scratch lock as above): 3 donor source builds, 11 host contracts PASS |

On a JDK older than 19, `Double.toString` is not Schubfach, so output there could differ; M3JDK is JDK 21.

## Found, not changed here

- The pack's `SOURCE_LOCK.json` disagrees with all four donors on master 5c47b0edf08d (`LOCKCHECK.txt`, Linux): the
  JDK naming v2 merge c9a1d41baaf5 edited the donors without relocking them, so `build.py` refuses at its first gate
  on master. This change relocks jackson-core only; jackson-annotations and jackson-databind need their owner. The
  pack gate above used a scratch copy of the lock with those two relocked, never committed.
- `build.py` orders paths by the platform's Path sort, which is case-insensitive on Windows, so a lock computed on
  Windows never matches the Linux build. The recipe orders by case-sensitive path parts on every platform.
- The other vendored donors' `verify_source.py` copies keep the old template's bugs.

## Speed

Double and float: unchanged, because fdp-delegate already routed Jackson's fast path to java.base. BigDecimal and
BigInteger: Jackson used FastDoubleParser's big parsers for literals of 500 characters or more (and whenever the fast
big-number feature was on); it now uses `new BigDecimal` and `new BigInteger`, which run the same algorithm in
java.base from 400 characters with #2864 (1,000 digits: BigDecimal 15.7 -> 7.0 us, BigInteger 13.0 -> 5.8 us). On a
JDK without #2864, numbers of 500 to 1,000 characters (Jackson's default maximum number length is 1,000) parse at the
JDK's own speed, about 2x slower than FastDoubleParser. This change therefore stacks on #2864.
