# univocity-parsers 2.9.1 source reference

This directory compiles the unchanged univocity-parsers 2.9.1 sources from the published sources jar.
Donor-superset lane: build kind `java`, status `PASS`.


```text
mvn -f m3/vendor/third_party/univocity-parsers/pom.xml clean install
python m3/vendor/third_party/univocity-parsers/verify_source.py m3/vendor/third_party/univocity-parsers/target/univocity-parsers-2.9.1-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
