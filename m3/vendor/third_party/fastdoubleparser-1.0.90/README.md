# fastdoubleparser 1.0.90 source reference

This directory compiles the fastdoubleparser 1.0.90 sources from the published sources jar, unchanged except
for the two recorded M3 overlays below.
Donor-superset lane: build kind `commands` (MODULE_DESCRIPTOR_OR_MULTI_RELEASE), status `PASS`.
Excluded from compilation (retained under upstream-excluded/): ch.randelshofer.fastdoubleparser/module-info.java

M3 overlays (`OVERLAY_SOURCES.json` key `m3_overlays`; recipe `m3/tooling/fdp-delegate/tools/fdp_delegate_overlay.py`;
gates `m3/tooling/fdp-delegate/GATES.tsv`): the parse methods of `JavaDoubleParser` and `JavaFloatParser` delegate
to `Double.parseDouble` / `Float.parseFloat`, whose M3JDK implementation is this library's algorithm absorbed into
java.base. Their unchanged upstream files stay pinned under upstream-excluded/. The BigDecimal, BigInteger and
JSON parsers remain upstream bytes.

```text
mvn -f m3/vendor/third_party/fastdoubleparser-1.0.90/pom.xml clean install
python m3/vendor/third_party/fastdoubleparser-1.0.90/verify_source.py m3/vendor/third_party/fastdoubleparser-1.0.90/target/fastdoubleparser-1.0.90-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed for the unchanged files. The overlays return java.base's
result. Differential gate b in `m3/tooling/fdp-delegate/GATES.tsv` compares them with the upstream facades over
its corpus:

- Decimal input gives the same values, and every input gets the same accept/reject result.
- All value differences are hex literals with 16 or more significant hex digits, each by one ulp. Upstream
  FastDoubleParser 1.0.90 misrounds these literals. On a stock JDK 21 the overlay returns the correctly rounded
  value, so it differs from upstream. On #2215's java.base the overlay returns upstream's value, because #2215
  absorbed the same misrounding.
- Rejected input reports the JDK's NumberFormatException messages. Some offset/length pairs whose sum overflows
  an int pass the upstream bounds check, which the overlay keeps. For `byte[]` and `char[]` input, upstream then
  throws ArrayIndexOutOfBoundsException where the overlay throws StringIndexOutOfBoundsException.

The published sources jar carries no test suite; the compile from source and the consumers' own tests are the
proof lane. Upstream tests remain a follow-up with their own recipe. See `PROVENANCE.json` for the file-level pins
and the runner receipt.
