# BigInteger and BigDecimal parse through the JDK's one fast parser (2026-11-09, NUMBER_PARSE_ABSORB v2)

User rules: M3JDK is one JDK, so M3 integrates in place in java.base and is never a layer; and the JDK owns one fast
implementation that every consumer uses. `Double.parseDouble` and `Float.parseFloat` already ran through
FastDoubleParser 1.0.90, absorbed in jdk.internal.math (2026-10-31). Big numbers did not: java.math had no M3 path.
jackson therefore kept its own FastDoubleParser copy for BigDecimal and BigInteger, and four copies of the parser
stayed in m3/vendor.

## Change

- `number-parse-big/absorb_big.py` adds 7 donor classes to java.base as jdk.internal.math.M3*: AbstractBigDecimalParser,
  AbstractBigIntegerParser, JavaBigDecimalFromCharArray, JavaBigIntegerFromCharSequence, BigSignificand,
  ParseDigitsTaskCharSequence, ParseDigitsTaskCharArray. The type map continues TYPE_RENAME.tsv (+7 rows). Every helper
  they call (M3FastIntegerMath, M3FftMultiplier, M3FastDoubleSwar, M3AbstractNumberParser) was already in java.base.
  JavaBigDecimalFromCharSequence is not absorbed, because java.math parses BigDecimal from char[].
- `jdk.internal.math.M3BigNumbers` is the one entry point. It returns null for a literal shorter than 400 characters and
  for any literal it rejects.
- `number-parse-big/wire_math.py` wires the parser in place:
  - `BigInteger(String, int)`: for radix 10, the public constructor asks M3BigNumbers. A private constructor copies the
    result's final fields, or runs the original body unchanged.
  - `BigDecimal(char[], int, int, MathContext)`: the primary constructor that every String and char[] path reaches.
    It does the same at unlimited precision; a null MathContext leaves the original path to decide.
- The donor builds its results only through BigDecimal(long), BigDecimal(BigInteger, int), BigInteger(byte[]) and
  BigInteger.valueOf. Its one String call, new BigInteger(String, radix), is for radixes other than 10 and 16, so the
  wiring cannot recurse.

## Gate (stock OpenJDK 21.0.12 with the patched java.base: wired java.math plus all 21 jdk.internal.math.M3* classes, -implicit:none)

| Check | Result |
|---|---|
| javac of the patch | rc=0, 33 classes; 1 warning, the existing lossy-conversions in M3FftMultiplier |
| class loading | java.math.BigInteger and jdk.internal.math.M3BigNumbers load from the patch in every run |
| `BigParseDifferential`: 3 seeds x 200,000 literals; per literal new BigInteger(s), new BigInteger(s, 10), new BigDecimal(s), new BigDecimal(char[], 2, len) give value (unscaled, scale, precision, hashCode, toString) or exception class and message | stock and patched outputs byte-identical (`SHA256.txt`) |
| corpus | 1 to 5,000 digits, signs, leading zeros, fractions, exponents to the int-scale limits, the threshold lengths, Arabic-Indic and fullwidth digits (the JDK accepts them; the fast parser declines and the JDK path decides), invalid mutations |
| fast path exercised | of 20,000 corpus-shaped literals: 5,994 BigInteger and 6,276 BigDecimal taken by the fast path |

## Performance (`BENCH.txt`; warm medians of 2,001 rounds up to 10,000 digits, else 9)

| digits | BigInteger stock -> M3 | BigDecimal stock -> M3 |
|---|---|---|
| 32 to 399 | the JDK path for both: same within noise | same |
| 1,000 | 13.0 -> 5.8 us | 15.7 -> 7.0 us |
| 5,000 | 298 -> 44 us | 348 -> 53 us |
| 100,000 | 114.5 -> 4.3 ms | 138.4 -> 4.4 ms |
| 1,000,000 | 11.47 s -> 62.5 ms | 13.24 s -> 64.1 ms |

The 400-character threshold is the measured crossover. With the donor's own threshold of 32, BigInteger was up to
2x slower between 32 and 200 digits.

## Next in this lane

1. Point jackson's NumberInput fast paths at the JDK API: new BigDecimal / new BigInteger / Double.parseDouble /
   Float.parseFloat, all now fast in java.base.
2. Re-point the module packs, tq-sync and the fdp-delegate tools.
3. Remove the four vendored FastDoubleParser copies from m3/vendor: one implementation, in the JDK.
