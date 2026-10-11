# slf4j-api 1.7.36 source reference

This directory compiles the unchanged slf4j-api 1.7.36 sources from the published sources jar.
Donor-superset lane: build kind `java`, status `PASS`.


```text
mvn -f m3/vendor/third_party/slf4j-api-1.7.36/pom.xml clean install
python m3/vendor/third_party/slf4j-api-1.7.36/verify_source.py m3/vendor/third_party/slf4j-api-1.7.36/target/slf4j-api-1.7.36-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
