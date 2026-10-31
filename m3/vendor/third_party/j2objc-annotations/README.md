# j2objc-annotations 2.8 source reference

This directory compiles the unchanged j2objc-annotations 2.8 sources from the published sources jar.
Donor-superset lane: build kind `java`, status `PASS`.


```text
mvn -f m3/vendor/third_party/j2objc-annotations/pom.xml clean install
python m3/vendor/third_party/j2objc-annotations/verify_source.py m3/vendor/third_party/j2objc-annotations/target/j2objc-annotations-2.8-m3-source-1.jar
```

No speed, allocation or behaviour change is claimed. The published sources jar carries no test suite; the
compile from source and the consumers' own tests are the proof lane. Upstream tests remain a follow-up with
their own recipe. See `PROVENANCE.json` for the file-level pins and the runner receipt.
