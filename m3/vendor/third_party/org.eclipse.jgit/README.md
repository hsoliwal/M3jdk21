# org.eclipse.jgit 5.13.3.202401111512-r source reference

This directory compiles the org.eclipse.jgit 5.13.3.202401111512-r sources from the published sources jar with one
M3 overlay, `JGIT_BASE64_TO_JDK v1` (`m3/tooling/jdk-superset/retarget/tools/jgit_base64_to_jdk.py`):
`util.Base64` encodes with `java.util.Base64`, and decodes with it when the input is in the canonical form; other
input keeps JGit's lenient decoding loop. The upstream file stays byte-identical under `upstream-excluded/`;
`verify_source.py` re-derives the overlay from it.
Donor-superset lane: build kind `commands` (NOT_IN_LOCK), status `PASS`.


```text
mvn -f m3/vendor/third_party/org.eclipse.jgit/pom.xml clean install
python m3/vendor/third_party/org.eclipse.jgit/verify_source.py m3/vendor/third_party/org.eclipse.jgit/target/org.eclipse.jgit-5.13.3.202401111512-r-m3-source-1.jar
```

The overlay's proof is `m3/evidence/java-base/BASE64_JGIT.md`. Outside it no speed, allocation or behaviour
change is claimed. The published sources jar carries no test suite; the compile from source and the consumers'
own tests are the proof lane. Upstream tests remain a follow-up with their own recipe. See `PROVENANCE.json` for
the file-level pins and the runner receipt.
