# Collection and linked M3JDK21 SDK evidence

The admitted collection sources pass their complete owning Java/JNI gate. The existing repository packager also links those sources' published `com.m3.collections` module and ten product JNI libraries into the source-built M3JDK21 SDK. The linked SDK passes the owning suite, native contracts, public `java.util.CollectionViews` contracts, and installed-class ownership checks.

## Exact checkpoint

These results describe **E1**, the 49-file composed source checkpoint published in [M3JDK21 PR 1657](https://github.com/hsoliwal/M3jdk21/pull/1657), commit `f35342202149b98465bb4e9e6be807eb0cf3f762`, tree `1f86f1244e4d2c218a3dc29bc101cfbdf89339fc`. Its canonical source custody is [Synexia PR 12680](https://github.com/hsoliwal/com.synexia/pull/12680), commit `0ee3e62241cb1252f2d018a33969f64e66c2b2fe`, tree `aa4434b052ef8541275cabad86eaf298cd27532c`. `EPOCHS.json` records the canonical composition hash, all 49 final postimage hashes, source-JDK binary hashes, module JAR hash, and linked module-store hash. The verifier ran before publication on origin `a115db5d8d9a53c20bf6f6880286a967a80a8dda` with the admitted changes present; the later published commit has those exact 49 postimages.

The 491-test linked result remains an E1 result. Any later String-only successor needs a rebuilt source SDK and a new linked-image receipt before that successor's installed-image boundary is claimed. The unchanged collection JAR and native inputs may be reused only after their seals match. A subsequent relink receipt must distinguish fresh checks from these earlier suite results.

## Executed gates

| Gate | Recorded result | Evidence |
| --- | --- | --- |
| Complete owning module, Java fallback | 491 tests, zero failures/errors, two native-only skips | `OWNING_PROOF.json` |
| Complete owning module, required JNI | 491 tests, zero failures/errors/skips | `OWNING_PROOF.json` |
| Source and package closure | 270 main Java files, 282 Java test files, 11 product C files; 512 packaged class files; runtime dependency `java.base` | `OWNING_PROOF.json` |
| Native build and old explicit contracts | Ten libraries; generated headers; strict C17 and fatal UBSan; checked JNI; 19 explicit contracts and two missing-native runs | `OWNING_PROOF.json`, `OWNING_RUN.json` |
| Newly admitted native addenda | Catalogue 8,438 checks; radix 417,038; missing-native two; zero-survivor fixture covers four exports with zero payload reads and retained drift detection | `NATIVE_ADDENDA.json` |
| Linked SDK owning suite | 491 tests, zero failures/errors/skips; all reports identify the linked `java.home` | `LINKED_SDK_RESULTS.json` |
| Linked SDK explicit contracts | 22 named-module main/probe runs and seven public-core runs pass | `LINKED_SDK_RESULTS.json` |
| Installed ownership | All 512 module class files included; all 370 production collection classes observed in JUnit originate in `jrt:/com.m3.collections` | `LINKED_SDK_OWNERSHIP.json` |
| JNI, legal and metadata packaging | Ten release JNI libraries, 21 generated exports, four legal notices, nine metadata resources | `LINKED_SDK_INPUTS.json`, `LINKED_SDK_OWNERSHIP.json` |

The source-built SDK contains `String`, `M3String`, and `CollectionViews` in bootstrap `java.base`. The module classes resolve from the installed `jrt:` image. Only external **test classes** are patched into `com.m3.collections`; the patch inventory has no production overlap, and there is no `java.base` patch. JUnit and the test-only bit-lane JNI fixture are external harness dependencies. All ten product JNI libraries are embedded in the image.

The seven public-core runs cover the current owner contract (14,694 checks), retained and additional identity contracts (46 and 540), refinement (1,828), two size-saturation assertions over more than `Integer.MAX_VALUE` callbacks, copy-on-write handling (120), and bootstrap ownership (77). The one-million-row packed-selection allocation probe observes 125,882 thread-allocated bytes per call on the boot JDK and 125,890 in the source-built linked SDK. Both satisfy the fixture's budget; they are different VM observations.

The linked SDK contains 29 selected modules. The source JDK's full-image build, CDS, and broader jtreg results are separate gates owned by the containing integration packet.

## Resource comparison

The unchanged `M3SignalRadixUniformBench` ran in eight isolated JVM processes: two processes for each baseline/candidate and Java/JNI combination, with pair order reversed in the second round. `RESOURCE_SAMPLES.csv` contains all **96 workload rows**. `RESOURCE_COMPARISON.json` retains all 24 paired summaries and exact source seals; `RESOURCE_PROCESS_USAGE.json` contains all eight process CPU/RSS observations.

For 65,536 rows, the ratios below compare the mean of each variant's two process-local median thread-CPU observations. Values above one are slower in this diagnostic.

| Backend | Repeated values | Narrow values | Varying values |
| --- | ---: | ---: | ---: |
| Java | 0.523 | 0.626 | 0.755 |
| JNI | 0.407 | 0.575 | **1.137** |

The JNI varying-input regression is retained. Two processes per variant do not establish a general speedup or statistical confidence. JIT, GC, and concurrent build load can affect timings. The benchmark's exact seed, warmup, repetitions, nine-sample median protocol, flags, and source hash are retained in the comparison receipt.

Allocation, retained-payload, and modeled-scratch fields match in every paired workload. At 65,536 rows, measured Java thread allocation is 1,573,992 bytes per call; measured JNI thread allocation is 786,488; retained index payload is 262,144 bytes; modeled JNI scratch is 1,572,864 bytes. The payload figure excludes headers and other reachable objects. The native figure is the unchanged source formula `24 * n`, not a measured native peak. OS maximum RSS covers the whole JVM process and all its workloads.

## What is published

The packet contains source and binary hashes, bounded results, normalized commands, the small external ownership fixture, and raw benchmark observations. `OWNING_PROOF.json` is byte-identical to the successful owning verifier's original proof and seals 567 source/fixture paths. Other receipts replace private absolute paths with portable variables. `RAW_EVIDENCE_SHA256.tsv` binds the original receipts and logs by logical identifier; it is not a promise that those private raw logs are distributed here.

`REPORT_INDEX_DIGESTS.json` binds the complete sorted Surefire report indexes without publishing XML system properties. No binaries, Maven settings, credentials, full environment dumps, or complete class-load/build logs are included. `PACKET_MANIFEST.json` seals the published files other than itself.

## Portable rerun

Use Linux x86_64, Java 21, Maven 3.9.9 or a compatible installation, a C17 compiler, `nm`, `objcopy`, and Python 3. The executed boot JDK was Eclipse Temurin 21.0.12.1+1 and the compiler was GCC 13.3. Source-owned JUnit dependencies are in `m3/vendor/third_party`; the existing verifier checks their source provenance and builds them. Maven plugins must be available in the chosen cache before an offline run.

Set absolute paths for your workspace. These examples are parameterized commands, not claims of new execution:

```sh
export REPO=/absolute/path/to/M3jdk21
export JAVA_HOME=/absolute/path/to/boot-jdk21
export MVN=/absolute/path/to/mvn
export M2_REPO=/absolute/path/to/maven-cache
export SOURCE_SDK="$REPO/build/m3-composed-release/images/jdk"
export PACKET="$REPO/m3/docs/composed-integration-20261021/evidence/collections"
export LINKED_WORK=/absolute/path/to/new-linked-evidence
export PACK_OUT="$LINKED_WORK/pack"
export IMAGE="$PACK_OUT/image"
export ADDENDA_WORK="$LINKED_WORK/addenda"
python3 "$REPO/m3/collections/qualification/full-java-jni-20261018/verify.py"
```

Set `M3_MAVEN_OFFLINE=1` only when the dependency/plugin cache is ready. If local Maven settings are needed, set `MVN` to a wrapper which supplies your settings to the existing verifier. The recorded linked-Surefire command has a separate optional `${MAVEN_SETTINGS}` argument; no settings file is supplied by this evidence packet.

Build the source JDK through the containing integration's unchanged build owner before linking. To reproduce E1, receive the exact canonical 49-file checkpoint and use its image. To qualify a later checkpoint, retain a new epoch and fresh source/image hashes. The release JNI compilation used boot-JDK headers; both header hashes are byte-identical to the E1 source SDK headers in `LINKED_SDK_INPUTS.json`.

Stage only the existing module, test inputs, legal notices, and external fixture:

```sh
python3 - <<'PY'
import json, os, shutil, zipfile
from pathlib import Path
r = Path(os.environ['REPO'])
w = Path(os.environ['LINKED_WORK'])
p = Path(os.environ['PACKET'])
m = r / 'm3/collections'
for name in ('inputs/jars', 'inputs/legal', 'inputs/core-sources',
             'inputs/test-native', 'inputs/ownership-probe', 'native-objects',
             'native-release', 'native-stripped-reference', 'core-test-classes', 'logs'):
    (w / name).mkdir(parents=True, exist_ok=True)
Path(os.environ['ADDENDA_WORK']).mkdir(parents=True, exist_ok=True)
jar = w / 'inputs/jars/com.m3.collections.jar'
shutil.copyfile(m / 'target/m3-collections-0.1.0-SNAPSHOT.jar', jar)
shutil.copytree(m / 'target/test-classes', w / 'inputs/test-classes')
with zipfile.ZipFile(jar) as z:
    production = {n for n in z.namelist() if n.endswith('.class')}
test = {str(f.relative_to(w / 'inputs/test-classes'))
        for f in (w / 'inputs/test-classes').rglob('*.class')}
assert not production.intersection(test), 'Test patch overlaps production'
for name in ('LICENSE', 'NOTICE', 'SYNEXIA-DONATION-NOTICE.txt', 'NETBEANS-NOTICE.txt'):
    shutil.copyfile(m / name, w / 'inputs/legal' / name)
for name in ('BootstrapViewsTest', 'CollectionViewsCoreContract',
             'CollectionViewsIdentityTest', 'CollectionViewsCowBulkTest',
             'CollectionViewsIdentityContract', 'CollectionViewsRefinement'):
    shutil.copyfile(r / 'test/jdk/java/util/CollectionViews' / (name + '.java'),
                    w / 'inputs/core-sources' / (name + '.java'))
shutil.copyfile(m / 'target/full-java-jni/native/libm3_bitlane_jump_test.so',
                w / 'inputs/test-native/libm3_bitlane_jump_test.so')
shutil.copyfile(p / 'LinkedM3SdkOwnership.java', w / 'LinkedM3SdkOwnership.java')
(w / 'INPUT_JMOD_IMAGE.tsv').write_text(
    'artifact\tversion\tjar\tmodule\trequires\tdisposition\n'
    + 'com.m3.collections\t0.1.0-SNAPSHOT\t' + str(jar)
    + '\tcom.m3.collections\tjava.base\tJMOD\n')
(w / 'EMPTY_DESCRIPTOR_MANIFEST.tsv').write_text(
    'module\tartifacts\tunit_jars\ttarget\tsha256\tgenerated\n')
PY
```

`RERUN_COMMANDS.json` contains argument arrays for every executed release-native compile/link/export/strip command, the unchanged packager, static ownership checks, linked JUnit invocation, and all explicit contracts. Its native mapping was read from the existing owning verifier's `LIBRARIES` constant. Run those groups in order after expanding variables; keep each recorded argument as one argument, particularly `-DargLine=...`.

The following replay example uses `subprocess.run` without shell evaluation. It deliberately uses a fresh pack output, and keeps the test-only JNI fixture outside the image. With no `MAVEN_SETTINGS`, it omits only the optional settings pair and uses normal Maven configuration.

```sh
python3 - <<'PY'
import json, os, re, subprocess
from pathlib import Path
p = Path(os.environ['PACKET'])
commands = json.loads((p / 'RERUN_COMMANDS.json').read_text())
def run(record):
    args = list(record['command'])
    if '-s' in args and not os.environ.get('MAVEN_SETTINGS'):
        at = args.index('-s')
        del args[at:at+2]
    args = [os.path.expandvars(a) for a in args]
    assert not any(re.search(r'\$\{[^}]+\}', a) for a in args), args
    subprocess.run(args, cwd=os.environ['REPO'], check=True)
for library in commands['releaseNative']:
    for command in library['commands']:
        run(command)
run(commands['compileOwnershipProbe'])
run(commands['pack'])
for command in commands['staticOwnership']:
    run(command)
run(commands['owningJUnit'])
for command in commands['contracts']:
    run(command)
addenda = json.loads((p / 'NATIVE_ADDENDA.json').read_text())
for command in addenda['runs']:
    run(command)
PY
```

A passing subprocess exit alone does not recreate all evidence. Recheck the JAR/test disjointness, all 512 class-file inventory entries, ordinary class-byte equality between JAR and JMOD, all ten native strip-debug hashes, legal/metadata bytes, 491 report totals with zero skips, `java.home`, and observed production class origins. Verify input/source hashes before and after. Publish new receipts and their hashes; do not overwrite E1 results with new measurements.

For the resource diagnostic, obtain complete separate baseline and candidate source trees. The baseline is `a115db5d8d9a53c20bf6f6880286a967a80a8dda`; the candidate radix atom and unchanged benchmark have exact hashes in `RESOURCE_COMPARISON.json`. Compile each main-source tree separately with `javac --release 21 -Xlint:all,-module -Werror -Xpkginfo:always -h <headers> -d <classes> @<sources>`. Set `BASELINE_REPO`, `CANDIDATE_REPO`, `BASELINE_CLASSES`, `CANDIDATE_CLASSES`, `RADIX_HEADERS`, and `RESOURCE_WORK` to these isolated inputs. Replay the three preparation commands in `RESOURCE_COMPARISON.json`, then the eight process commands in `RESOURCE_PROCESS_USAGE.json` in their recorded order. Preserve stdout, stderr, exit status, and `os.wait4` process usage for each process. The existing `-Dm3.collections.signal.native=true` opt-in is required for the JNI half.

## Scope and failures

`FAILURES_AND_SCOPE.md` distinguishes repaired baseline defects, harness refusals, obsolete donor tests, stopped recipe preparations, retained performance regressions, and remaining exclusions. These finite gates support the admitted per-file changes and the E1 installed SDK boundary; they do not imply every collection/provider contract, every platform, full JCK/jtreg, all JMM/GC behavior, or whole-heap/native-peak resource qualification.
