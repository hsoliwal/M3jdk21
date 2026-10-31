# Direct JDT child-list storage

This receiving source retains the original `org.eclipse.jdt.core.dom.ASTNode` owner and public JDT API. Its private `NodeList` stores child references in a growable `ASTNode[]` and a logical size. Empty lists share an empty array; a populated list no longer owns a separate `ArrayList` wrapper. Parent links, property descriptors, change events, visitors, bindings, and AST node identity remain with their existing JDT owners.

The source donor is [hsoliwal/eclipse.jdt.core at 623f17c961840a8c2b48672e0b71689106ba5050](https://github.com/hsoliwal/eclipse.jdt.core/tree/623f17c961840a8c2b48672e0b71689106ba5050). `SOURCE.json`, `UPSTREAM_SOURCE_SHA256SUMS`, and `OWNER_SOURCE_TRANSFORMS.tsv` identify the complete compiler/core source closure and the exact source changes. Eclipse copyright notices and EPL-2.0 remain in the source. The upstream bundle manifests are retained as provenance files.

## Build the receiving source

Use Maven 3.9.11, Python 3, and a Java 21 or newer JDK. Check out the exact source donor and supply its absolute path:

```sh
mvn -f m3/eclipse/jdt/receiving/array-nodelist-20261019/pom.xml \
  -Djdt.source=/absolute/path/to/eclipse.jdt.core \
  -Dproof.build=/tmp/jdt-array-nodelist-receiving \
  clean verify
```

The verifier checks all 1,786 locked files, including 1,624 Java sources, parser resources, and the upstream compiler API archive. It refuses altered or additional source inputs. A donor file may already have the exact recipe-generated postimage recorded in the transform table; that case is reported explicitly. Both generated owner files and the two shared fork tests must match their recorded postimage checksums.

The Maven reactor builds the original compiler and core source separately:

| Module | Source and compilation contract |
| --- | --- |
| `compiler` | All original `org.eclipse.jdt.core.compiler.batch/src` files, compiled with `--release 17`. The committed `lib/javax23api.jar` supplies the newer `java.compiler` API declarations already named by upstream `build.properties`. This archive is a compile input and is not patched into, or packaged with, the runtime. |
| `core` | All six original core source roots, compiled with `--release 21`. Maven stages unchanged source and the two exact recipe-generated owner files. The compiler dependency is the source-built reactor output. |

The current upstream core manifest requires JavaSE-25. Compiling that original source directly with `--release 21` fails on the unnamed lambda parameter in `JavaBuilder.collectDependents`. The receiving source changes only that unused parameter spelling from `_` to `unused`. The upstream compiler also refers to API declarations introduced after Java 21; its committed compile API archive is retained explicitly. Newer platform API methods do not become available on a Java 21 runtime through this build.

For the Java 25 source gate, use `-Djdt.core.release=25` and a Java 25 JDK. Runtime qualification must actually execute the tests on each claimed runtime. A successful Java 25 build alone does not establish Java 21 runtime compatibility.

## Preserved contracts

The direct array change preserves the ordering of null, type, ownership, cycle, protection, and bounds checks. It preserves pre/post change callbacks, same-child replacement behavior, per-child bulk mutation behavior, parent detachment, and independent live visitor cursors. Removed array slots are cleared. The inherited public list implementation and original AST occurrence identity remain intact.

`ASTNode.subtreeBytes()` continues to use its historical cost estimate, including the former wrapper cost and logical child count. This observable estimate is deliberately independent of the new physical storage. Memory measurements must inspect actual objects rather than interpret that compatibility estimate as heap occupancy.

`ASTNodeListArrayTest` covers 15 finite contracts: seeded mutation sequences against an identity oracle; list bounds and failure ordering; owner/type/cycle and protected-node rejection; partial bulk failure; iterator/sublist edits; nested visitor mutation and cursor release; ASTRewrite comment preservation and undo; recorded changes and reparsing; binding identity after mutation; copied fragment parent ownership; and the historical public memory estimate.

`JdtSourceCompatibilityTest` checks the source-built compiler's classfile target and compiles a Java 21 record while executing a real annotation processor that reads record components and documentation. These are bounded runtime checks. They do not qualify the full Eclipse workspace, working-copy reconciliation, incremental builder, annotation API versions beyond the running JDK, or all upstream regression suites.

The fork's existing DOM `RunAllTests` receives the new child-list suite through its own canonical recipe. The standalone receiving build runs the shared contracts without replacing the Eclipse test harness or presenting a partial run as the full upstream suite.

## Measure physical storage

`ASTNodeListStorageProbe` uses the JVM instrumentation API to measure the `NodeList`, its direct store, and any backing array. It also records per-thread allocated bytes, CPU time, elapsed time, and process RSS where available. It retains the created ASTs and deduplicates shared empty arrays. AST nodes themselves are outside its reported child-list storage total.

Run identical workloads in separate original and generated JVMs. Use the generated test JAR as a Java agent and open `java.util` for the original `ArrayList` backing-array measurement:

```sh
java -Xms256m -Xmx256m \
  --add-opens java.base/java.util=ALL-UNNAMED \
  -javaagent:/absolute/path/to/jdt-exact-core-source-20261019-tests.jar \
  -cp '/absolute/path/to/core/classes:/absolute/path/to/core/test-classes:/absolute/path/to/compiler/classes:/absolute/path/to/dependencies/*' \
  org.eclipse.jdt.core.tests.dom.ASTNodeListStorageProbe generated 10000 4
```

Workload size, JDK version, heap settings, and raw samples must accompany results. Process RSS and short timing samples include JVM and host effects; storage measurements do not establish a universal speedup or a complete AST heap reduction.
