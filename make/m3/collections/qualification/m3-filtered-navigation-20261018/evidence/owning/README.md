# Source dependency bootstrap and owning collection Maven gate

The received collection implementation passed the unchanged owning Maven POM: **234 tests across 79 suites, zero failures, errors, or skipped tests**. The build compiled all **134 production Java sources and 161 test Java sources** and completed in **171.191 seconds**. The copied owning inputs, including the original POM, remained unchanged during the gate. See the original [owning result receipt](collections/RESULTS.json), [Maven output](collections/maven.log), and [complete executed test inventory](collections/JUNIT_SUMMARY.json).

## Evidence identity and disclosure

Every existing receipt and log in this packet is copied byte-for-byte from the completed run. [PUBLICATION_MANIFEST.json](PUBLICATION_MANIFEST.json) identifies each original artifact, its exact SHA256, byte count, and evidence type. This README and that manifest are authored indexes. `JUNIT_SUMMARY.json` is the gate-generated extraction of suite counts and case outcomes; it is not raw Surefire XML. No run output is silently edited or redacted.

Original command vectors retain the harmless local Maven settings filename needed to describe the actual invocation. The settings document, environment configuration, truststore, credentials, caches, binary dependencies, and full Surefire XML are excluded. The published runtime property inventory is restricted to Java identity and the native library path. The selected text was checked for credential-bearing URLs, credentials, settings document content, and proxy or truststore configuration; none was present. Initial environment setup failures are not acceptance evidence and are not included.

## Genuine source dependency prerequisite

The repository declares source-owned JUnit coordinates such as `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`. The prerequisite used the exact existing source implementations and POMs at M3JDK21 commit `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. All **ten source modules** compiled, packaged, and installed successfully. No released binary was renamed to the source-owned coordinate, and no source or dependency version was changed. The original [source build receipt](source-bootstrap/RESULTS.json) records each executed argument vector, exit code, duration, installed artifact SHA256, installed POM SHA256, and packaged legal resource hashes.

| Source module / original log | Installed coordinate | Maven exit |
| --- | --- | --- |
| [apiguardian-api](source-bootstrap/apiguardian-api.log) | `org.apiguardian:apiguardian-api:1.1.2-m3-source-1` | 0 |
| [opentest4j](source-bootstrap/opentest4j.log) | `org.opentest4j:opentest4j:1.3.0-m3-source-1` | 0 |
| [junit-platform-commons-1.10.2](source-bootstrap/junit-platform-commons-1.10.2.log) | `org.junit.platform:junit-platform-commons:1.10.2-m3-source-1` | 0 |
| [junit-platform-engine-1.10.2](source-bootstrap/junit-platform-engine-1.10.2.log) | `org.junit.platform:junit-platform-engine:1.10.2-m3-source-1` | 0 |
| [junit-platform-launcher-1.10.2](source-bootstrap/junit-platform-launcher-1.10.2.log) | `org.junit.platform:junit-platform-launcher:1.10.2-m3-source-1` | 0 |
| [univocity-parsers](source-bootstrap/univocity-parsers.log) | `com.univocity:univocity-parsers:2.9.1-m3-source-1` | 0 |
| [junit-jupiter-api-5.10.2](source-bootstrap/junit-jupiter-api-5.10.2.log) | `org.junit.jupiter:junit-jupiter-api:5.10.2-m3-source-1` | 0 |
| [junit-jupiter-engine-5.10.2](source-bootstrap/junit-jupiter-engine-5.10.2.log) | `org.junit.jupiter:junit-jupiter-engine:5.10.2-m3-source-1` | 0 |
| [junit-jupiter-params-5.10.2](source-bootstrap/junit-jupiter-params-5.10.2.log) | `org.junit.jupiter:junit-jupiter-params:5.10.2-m3-source-1` | 0 |
| [junit-jupiter-5.10.2](source-bootstrap/junit-jupiter-5.10.2.log) | `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1` | 0 |

The [source intake receipt](source-intake.json) records all **886 tracked files / 4,336,521 bytes**, exact Git blob identities, repository module tree pins, source archive provenance, and **826 additional provenance manifest checks**. The `all_files` member is the complete final inventory; the earlier `files` and `connector_completed` members retain intake history. The final status is `EXACT_INTAKE_COMPLETE` with zero missing files. Source archives were used only when their individual file Git blob identities matched the pinned repository; remaining files came from the pinned repository blobs. Input seals were checked across the builds.

The artifact audit verified **47 packaged license, notice, and provenance resources** against their actual tracked inputs. JUnit retains EPL-2.0; apiguardian, opentest4j, and Univocity retain Apache-2.0. Apiguardian's canonical tracked license is packaged at `META-INF/LICENSE`. Its source tree has no module-root `LICENSE`; the audit was corrected to that real tracked resource without changing any source or artifact. Integration notices do not relicense the donor implementations.

The dependency bootstrap used each original standalone POM in dependency order because the local intake was finite. The source dependency POMs' test skip policy was preserved, so bootstrap success is compilation, packaging, installation, source integrity, and legal resource evidence. It does not assert that donor test suites ran. The owning collection gate itself skipped zero tests.

## Actual owning gate and Java / JNI scope

After the ten installs, the owning gate used a separate cache containing the genuinely built artifacts and an isolated copy of the received collection module. The result receipt identifies the received owner hashes, original POM hash, Java 21 runtime, exact command, and all eight source-built JUnit dependency artifacts found in the reported test classpath. Ten modules were built for the source closure; optional Univocity and the platform launcher are not listed in that test classpath. Standard Maven build and test provider dependencies were resolved normally.

The owning command enabled `-Xcheck:jni` and supplied the already built `libm3_collections_mask.so` and `libm3_collections_pathmask.so`; both exact library hashes are recorded. This establishes the owning suite with those two native libraries available. It does not assert that every possible native component is present or that every test invokes JNI. The separate focused native qualification remains the evidence for its own dispatch and runtime assertions.

[INPUTS.json](collections/INPUTS.json) seals 457 owning input files. [CLASS_FILES.json](collections/CLASS_FILES.json) seals 574 emitted class files. [JAVA_VERSION.txt](collections/JAVA_VERSION.txt) preserves the actual Java version output. The 79-suite inventory contains all 234 observed test case outcomes.

## Reproduction and finite admission boundary

The repository's existing source prerequisite is documented at `m3/docs/a3-vendored-build-prerequisite-20261015.md` (Git blob `4939e7c2148a8db90f2336d1df364e4b86757efe`). On a full checkout, its reactor selection for this closure is:

```sh
mvn -B -ntp -f m3/vendor/third_party/pom.xml \
  -pl junit-jupiter-5.10.2,junit-platform-launcher-1.10.2 \
  -am -DskipTests install
mvn -B -ntp -DfailIfNoTests=true -f m3/collections/pom.xml clean verify
```

The commands above are portable reproduction instructions, not substituted command transcripts. The exact locally executed commands remain in the original result receipts and logs; the owning run additionally supplied its native library path and JNI checks. Use the same dependency cache for the bootstrap and owning build.

The actual local prerequisite installed the same statically reviewed ten-module dependency closure through the original standalone POMs. The full vendor root reactor, including parsing unrelated module POMs, was not executed locally. Full-checkout CI retains that normal reactor gate. This packet qualifies the received owning collection module and finite source closure; it does not establish all repository modules, whole-JDK builds, universal callback identity, universal strict map endpoints, or unbounded deep-map behavior.
