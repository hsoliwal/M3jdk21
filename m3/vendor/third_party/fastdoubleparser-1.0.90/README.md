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

No speed, allocation or behaviour change is claimed for the unchanged files. The overlays return the same values
as the upstream facades (differential gate) and report rejected input with the JDK's NumberFormatException
messages. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
