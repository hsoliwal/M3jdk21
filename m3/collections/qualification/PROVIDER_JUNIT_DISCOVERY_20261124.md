# M3 provider JUnit discovery — named-module test fix

## Observed failure

Combined source-built GINQ + countByMany run [45134282192](https://github.com/hsoliwal/M3jdk21/actions/runs/45134282192) completed original JNI CTests, both operators on full and linked images, all selected jtreg files, and compilation of the provider JUnit sources. Its JUnit ConsoleLauncher then reported **zero discovered tests** and terminated with exit code **2**, before the original fatal collections Maven stage could execute. A second GINQ run [45148359472](https://github.com/hsoliwal/M3jdk21/actions/runs/45148359472) had the same discovery failure.

## Class-loader boundary

The provider test Java classes are patched into installed named `jdk.m3.collections`; the standalone JUnit Platform Console jar was only on the child application classpath. Some installed JDK modules are loaded by bootstrap/platform class loaders and cannot resolve Jupiter annotations from a child application class loader even when `--add-reads ...=ALL-UNNAMED` and `--add-opens` are present. Annotations may therefore disappear from reflection without an ordinary Java compilation failure, leaving a discoverable class with no Jupiter tests.

The separate Java 21 synthetic fixture in `make/m3/collections/test-provider-junit-discovery-bootstrap.sh` reproduces this failure with a `java.base`-patched class and a tiny JUnit `@Test` annotation stub, then proves that a scoped `-Xbootclasspath/a:` addition makes the annotation visible. It requires both the negative and positive outcomes and does **not** run any third-party tests by itself.

## Receiving correction

The original `make/m3/collections/test-provider-junit.sh` still:

- verifies all 54 relocated provider test/helper original Git blob identities and the additional three original shared helpers;
- compiles all 57 original provider sources into the installed module using the source-built JDK;
- selects exactly 15 original JUnit test classes and retains their test methods unchanged;
- supplies the existing module, native JNI path, `--add-reads` and `--add-opens`;
- requires JUnit 1.12.2 as the original pinned standalone jar, with its SHA-256 verified in the workflow;
- invokes the original ConsoleLauncher with `--fail-if-no-tests` and retains the fatal Maven/JUnit stage after this provider lane.

For **only the two provider JUnit execution subprocesses**, the existing jar is also appended to bootstrap lookup with `-Xbootclasspath/a:`. Before ConsoleLauncher, a new dedicated Java 21 `ProviderJUnitDiscoveryProbe` checks that each of the 15 exact test selectors belongs to the installed `jdk.m3.collections` module and exposes runtime-visible Jupiter method annotations. It emits an auditable class/method count and refuses missing/duplicate annotations, wrong modules and incomplete class sets.

The CI source-only job validates both scripts' Bash syntax. The actual-image job runs the reproducible Java 21 negative/positive synthetic proof **before** compiling the full JDK; native JUnit and its logs remain required later. The synthetic test and probe were compiled and executed locally under OpenJDK 21, with missing-bootclasspath annotation visibility rejected and bootstrap visibility accepted.

## Qualification

A successful synthetic bootstrap visibility test does **not** qualify the original 15 provider tests. A full exact-head hosted rerun must pass the source-built full JDK, original native CTests, combined GINQ/count tests, all selected jtreg, the 15 provider JUnit classes, and the separate fatal collections Maven bridge. Any assertions or runtime failures from the original tests remain **unwaived**. Keep the repair PR **DRAFT/HOLD** until those results are actually observed. This changes neither production Java, Groovy semantics, the original Maven POM nor historical source-sealed Java postimages.
