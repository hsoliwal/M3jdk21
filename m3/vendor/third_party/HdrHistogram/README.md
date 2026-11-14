# HdrHistogram 2.1.12 source reference

This directory compiles the HdrHistogram 2.1.12 sources from the published sources jar with one M3 overlay,
`BASE64_TO_JDK v1` (`m3/tooling/jdk-superset/retarget/tools/base64_to_jdk.py`): `Base64Helper` calls `java.util.Base64`
directly instead of finding it by reflection (with a `javax.xml.bind` fallback for Java SE 6 and 7). The upstream
file stays byte-identical under `upstream-excluded/`; `verify_source.py` re-derives the overlay from it.
Donor-superset lane: build kind `java` (NOT_IN_LOCK), status `PASS`.


```text
mvn -f m3/vendor/third_party/HdrHistogram/pom.xml clean install
python m3/vendor/third_party/HdrHistogram/verify_source.py m3/vendor/third_party/HdrHistogram/target/HdrHistogram-2.1.12-m3-source-1.jar
```

The overlay's proof is `m3/evidence/java-base/BASE64_HDRHISTOGRAM.md`. Outside it no speed, allocation or
behaviour change is claimed. The published sources jar carries no test suite; the compile from source and
the consumers' own tests are the proof lane. Upstream tests remain a follow-up with their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
