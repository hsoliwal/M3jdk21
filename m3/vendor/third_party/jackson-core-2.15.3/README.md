# jackson-core 2.15.3 source reference

This directory compiles the jackson-core 2.15.3 sources from the published sources jar with one M3 overlay,
`JACKSON_NUMBERS_TO_JDK v1` (`m3/tooling/jdk-superset/number-parse/tools/jackson_numbers_to_jdk.py`): `NumberInput`, `BigDecimalParser` and
`BigIntegerParser` call `Double.parseDouble`, `Float.parseFloat`, `new BigDecimal` and `new BigInteger`
in java.base, the JDK's one number parser, instead of the shaded FastDoubleParser. The upstream files
stay byte-identical under `upstream-excluded/`; `verify_source.py` re-derives each overlay from them.
Donor-superset lane: build kind `java`, status `PASS`.


```text
mvn -f m3/vendor/third_party/jackson-core-2.15.3/pom.xml clean install
python m3/vendor/third_party/jackson-core-2.15.3/verify_source.py m3/vendor/third_party/jackson-core-2.15.3/target/jackson-core-2.15.3-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
