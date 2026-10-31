# JDT source qualification, 2026-10-19

## Source and runtime gates

The donor is `hsoliwal/eclipse.jdt.core` commit `623f17c961840a8c2b48672e0b71689106ba5050`. The canonical Synexia run `sdk-jdt-04` emitted the four Java LST outputs and the narrowly admitted unused-parameter syntax bootstrap recorded in `SDK_EXECUTION.tsv`. The original underscore preimage failed OpenRewrite Java 21 print fidelity; the bootstrap preserves its failure witness, refuses another path/crate/hash, and verifies the resulting Java LST. It does not admit arbitrary Java through the text host.

The generated owner files used for compilation were copied byte-for-byte from that actual SDK output. Their SHA-256 checksums agree with the source transform table.

| Gate | Result |
| --- | --- |
| Original pinned compiler and core, compiled together on Java 25 | All 1,624 Java files compile; the 15 child-list/DOM contracts pass. |
| Original compiler built from source with its upstream compile API archive | 552 Java files compile with `--release 17`. The compiler runs the two record/annotation-processing contracts on Java 21 and Java 25. |
| Generated receiving source on actual Java 21 | 552 compiler files target Java 17; 1,072 core files compile with genuine `--release 21`; all 17 contracts pass. No runtime `--patch-module` is used. |
| Generated receiving source on actual Java 25 | 552 compiler files target Java 17; 1,072 core files target Java 25; all 17 contracts pass. |
| Original array-list owner on Java 21 | The original `ASTNode.java` replaces the generated owner only in the verification compilation inputs. The same canonical unused-lambda compatibility fix is retained. All 1,072 core files compile with `--release 21`; all 17 contracts pass. |
| Integrated source-built M3JDK21 image | All 17 generated JDT contracts pass on the actual fastdebug `21-internal-adhoc.root.M3jdk21` image without runtime patch flags. Its `lib/modules` SHA-256 is `d852163d8569ccfe6887615d0be84b6803cb5f463fc4f2f9e0638daf5a62d84d`; see `generated-receiving-m3jdk21.json`. |
| Public/protected declarations, descriptors and constant values | `javap -protected -s -constants` output for `ASTNode` and `JavaBuilder` is identical for original Java 25, generated Java 25 and generated Java 21. See `public-abi-comparison.json`. |

Java 21 is Temurin `21.0.12.1+1`; Java 25 is Temurin `25.0.4.1+1`. The upstream core's JavaSE-25 manifest remains a provenance record; a passing receiving gate does not relabel that upstream release.

## Original upstream DOM suites

The unchanged `ASTTest` and `ASTVisitorTest` sources, their actual JDT test support classes, and the original Eclipse performance test harness were compiled and executed. The harness source comes from `eclipse-platform/eclipse.platform.releng.aggregator` commit `90ccc0a05665bf3ed0b9e8c9bcf906ccfa355550`. No test superclass or platform API was stubbed.

`ASTVisitorTest` passes all 384 tests on original Java 25, generated Java 25 and generated Java 21. `ASTTest` executes 190 tests in each of those configurations: 188 pass and the same two `testAST` cases fail because the standalone process has no OSGi default compiler-source preference. The initial combined run therefore reports 572 passing tests and two failures out of 574. A direct call to the real preference initializer also requires an active Equinox configuration location, so the platform startup boundary is retained rather than replaced by test-only behavior.

The Java 21 original-owner baseline also executes all 574 combined upstream tests with exactly the same two default-preference failures.

These results establish no new failure in those finite DOM suites. They do not establish a passing full Eclipse/Tycho/PDE regression run, working-copy reconciliation, or an incremental workspace build.

## Physical memory measurements

The paired Java 25 run uses 10,000 retained block child lists per workload, `-Xms256m -Xmx256m`, ten warmup iterations, and seven measured samples. Instrumentation counts each distinct directly owned list/store/backing-array object once. It excludes the AST nodes stored in those lists.

| Children per list | Original retained bytes | Generated retained bytes | Reduction | Fewer objects |
| ---: | ---: | ---: | ---: | ---: |
| 0 | 560,016 | 400,016 | 160,000 (28.57%) | 10,000 |
| 1 | 800,000 | 640,000 | 160,000 (20.00%) | 10,000 |
| 4 | 880,000 | 720,000 | 160,000 (18.18%) | 10,000 |
| 16 | 1,520,000 | 1,360,000 | 160,000 (10.53%) | 10,000 |

The paired Java 21 empty-list and four-child runs reproduce those same respective retained totals; their raw samples are in `nodelist-storage-paired-21.jsonl`.

The measured net saving is 16 bytes and one object per list on this JVM configuration. The additional logical-size field changes the list object's aligned size, so the net byte reduction is smaller than the removed wrapper object's size. Median allocated bytes fall by 160,000 per workload as well.

The raw paired samples and summary retain CPU time, elapsed time and RSS observations. Short timing samples show substantial variation, and RSS is slightly higher for the generated four-child workload. These observations are not a universal speed or process-memory claim. Public `subtreeBytes()` values intentionally remain at the historic estimate and are not used as physical-memory evidence.

The finite contracts do not allocate arrays near the VM's maximum array length or establish identical allocation-failure thresholds under heap exhaustion.
