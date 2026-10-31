<!--
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
-->
# fdp-delegate: one number parser for Jackson and the JDK

Jackson 2.19.2 parses floating-point numbers with FastDoubleParser 1.0.90 when
`StreamReadFeature.USE_FAST_DOUBLE_PARSER` is on, and with `Double.parseDouble` /
`Float.parseFloat` when it is off. M3JDK's java.base absorbs FastDoubleParser 1.0.90
as `jdk.internal.math.M3*` (#2215). This lane makes the vendored FastDoubleParser
1.0.90 facades delegate to java.base, so both Jackson paths use the JDK's parser.

## What changed

- `m3/vendor/third_party/fastdoubleparser-1.0.90`: `JavaDoubleParser` and
  `JavaFloatParser` are recorded M3 overlays. The parse methods for `CharSequence`,
  `byte[]` and `char[]` with offset and length keep upstream's bounds check
  (`AbstractNumberParser.checkBounds`, which throws `IllegalArgumentException`)
  and then call `Double.parseDouble` / `Float.parseFloat`. Bytes are decoded as
  ISO-8859-1, the encoding the upstream byte parser accepts. The overloads that
  take no offset already called these methods. The unused bit-parser fields are
  removed.
- The BigDecimal, BigInteger and JSON parsers stay as upstream bytes. The JDK
  absorbs the BigDecimal and BigInteger parsers in a later leaf.
- Custody uses the donor's existing overlay mechanism:
  - the upstream originals stay byte-identical under `upstream-excluded/`;
  - `PROVENANCE.json` pins the originals, the overlays and the recipe hash;
  - `OVERLAY_SOURCES.json` lists the overlays under `m3_overlays`;
  - `verify_source.py` re-derives each overlay from its retained original
    through the pinned recipe;
  - the jackson module pack's `SOURCE_LOCK.json` is relocked, and the previous
    lock is kept as `qualification/fdp-delegate/SOURCE_LOCK_PREDECESSOR.json`.
- `verify_source.py` also gets two fixes. On master it resolved the repository
  root to `m3/vendor` and read a missing `revision` key, so it failed before
  checking anything. The same template is copied in every vendored donor, and
  those copies still have both bugs.

## Tools

| Tool | Role |
|---|---|
| `tools/fdp_delegate_overlay.py` | The recipe (FDP_DELEGATES_TO_JDK v1). It applies the overlay and its custody records. Running it again changes nothing; `--check` writes nothing. |
| `tools/run_gates.py` | Gates (a) to (c). It builds upstream and overlay multi-release jars, compiles jackson-core 2.19.2 against each, and runs the differentials on stock java.base and, with `--m3-src`, on M3 java.base. |
| `tools/M3FdpParseCorpus.java` | The deterministic corpus: Java literals and JSON numbers, from a fixed seed, identified by SHA-256. |
| `tools/M3FdpDelegateDifferential.java` | Gate (b). Upstream and overlay run side by side through all 12 facade methods. |
| `tools/verify_rounding.py` | An exact-arithmetic referee (`fractions.Fraction`, round-half-even) for raw-bits differences. |
| `tools/M3JacksonFastDoubleDifferential.java` | Gate (c). Jackson runs with `USE_FAST_DOUBLE_PARSER` on and off, through char, byte and async parsers. |
| `tools/custody_gate.py` | Gate (d). It runs `verify_source.py`, the recipe fixed point, the pack lock check and the central crate rule, plus 7 tamper probes. |
| `tools/class_load_trace.py` | Gate (c) evidence. It lists the FastDoubleParser classes each build loads while Jackson parses. |
| `tools/gates_tsv.py` | Builds `GATES.tsv` from one run's `RESULT.jsonl`, `custody.json` and `classload.txt`. |

```text
python -I m3/tooling/fdp-delegate/tools/fdp_delegate_overlay.py --check
python -I m3/tooling/fdp-delegate/tools/run_gates.py --jdk <jdk21> --out <dir> --m3-src <M3JDK tree>
python -I m3/tooling/fdp-delegate/tools/custody_gate.py --out <dir>/custody
python -I m3/tooling/fdp-delegate/tools/class_load_trace.py --jdk <jdk21> --run <dir>
python -I m3/tooling/fdp-delegate/tools/gates_tsv.py <dir> <dir>/classload.txt "<public-names result>"
```

Results are in `GATES.tsv`. Corrections are in `RECIPE_MEMORY.tsv`.

## Finding: upstream FastDoubleParser misrounds long hex significands

Gate (b) on stock java.base found 1,213 double and 96 float hex literals where the
upstream facades and the JDK disagree by one ulp. Each literal has 16 or more
significant hex digits. The exact-arithmetic referee shows the JDK result is
correctly rounded in every case and upstream FastDoubleParser 1.0.90 in none.
One example is `0x8Db1Ffd473B83a5fp196`:

- upstream returns `0x5021b63ffa8e7708`;
- `Double.parseDouble` and Python's `float.fromhex` both return `0x5021b63ffa8e7707`.

On a stock JDK the overlay therefore fixes this case for Jackson. On #2215's java.base
the gate shows no difference, because the absorbed parser has the same
misrounding: there, `Double.parseDouble("0x8Db1Ffd473B83a5fp196")` returns
`...7708`. #2215 needs to route such hex literals to `FloatingDecimal`, or fix the
hex conversion, before it lands. Its 600k corpus contained no long hex
significands; `M3FdpParseCorpus` includes them.

## Speed

On M3JDK, the overlay runs the same algorithm through java.base. On a stock or
current-master JDK, `USE_FAST_DOUBLE_PARSER` runs the JDK's `FloatingDecimal`, so
it is not faster than the off path until #2215, with the fix above, is merged.
