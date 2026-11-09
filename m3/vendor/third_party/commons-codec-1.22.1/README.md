# commons-codec 1.22.1 source reference

This directory compiles the commons-codec 1.22.1 sources from the published sources jar with one M3 overlay,
`CHECKSUMS_TO_JDK v1` (`m3/tooling/jdk-superset/hashing/tools/checksums_to_jdk.py`): `digest.PureJavaCrc32` and
`PureJavaCrc32C` keep their public members and delegate to java.base (`java.util.zip.CRC32`, `CRC32C`),
the JDK's one CRC implementation, instead of carrying their own lookup tables. The upstream files stay
byte-identical under `upstream-excluded/`; `verify_source.py` re-derives each overlay from them.
Donor-superset lane: build kind `java`, status `PASS`.


```text
mvn -f m3/vendor/third_party/commons-codec-1.22.1/pom.xml clean install
python m3/vendor/third_party/commons-codec-1.22.1/verify_source.py m3/vendor/third_party/commons-codec-1.22.1/target/commons-codec-1.22.1-m3-source-1.jar
```

The overlay's proof is `m3/evidence/java-base/CHECKSUMS_CODEC.md`. Outside it no speed, allocation or
behaviour change is claimed. The published sources jar carries no test suite; the compile from source and
the consumers' own tests are the proof lane. Upstream tests remain a follow-up with their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
