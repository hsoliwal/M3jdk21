<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# GVAS native pathname qualification

The existing Linux mapper used JNI modified UTF-8 for a filesystem name. A valid
supplementary Unicode path therefore failed in `mapExact0` even though Java had
already resolved the real file. The same image passed under ASCII and BMP paths.
This change calls M3JDK21's existing `sun.nio.fs.Util.toBytes(String)` charset owner
and releases its temporary references and native buffer before returning.

The production change is in `m3/lexicon/native/m3_fixed_address_mapping_jni.c`.
The Java owner remains `com.m3.text.M3FixedAddressMapping`. The image ABI, four JNI
descriptors and exported names, Java descriptors, strict address flags, digest
validation, read-only shared mapping, and process-lifetime ownership are preserved.
The Windows mapping body and the other three native method bodies are byte-identical.

## Reuse and reproducibility

Baseline: `hsoliwal/M3jdk21@672e29f20bd039acaaeeb97ea17eee2939fcc8e4`.
Canonical transformation:
`hsoliwal/com.synexia/synexia-openrewrite-recipes/crates/m3jdk21-20261019/string-native`.
Recipe: `com.synexia.m3.GvasNativePath20261019`.

The recipe reuses the existing class-backed Synexia Java and native/text snapshot
recipes, SDK qualification test, and receiving adapter at
`com.synexia@1124f5b72744b306884beedebafd8558d49d4c4c`. Maven/OpenRewrite 8.17.1 executes
the transformations. Exact input and output hashes admit each file; altered,
missing, duplicated and post-scan-modified inputs are refused. Repeated application
produces zero changes. The product has no Synexia runtime dependency.

The JDK encoder is invoked, without copying or relicensing its implementation.
Its existing source is `src/java.base/share/classes/sun/nio/fs/Util.java`, Git blob
`94a5def1e853d5406f3defa24f0455b977095e0c`. A handwritten UTF-16 converter was rejected
because it would duplicate an established charset owner and lose native charset
policy. JNI modified UTF-8 was rejected by the actual pathname regression.
LeetCode 393, HackerRank Java Strings Introduction and GeeksforGeeks character
encoding were category references; their statements and solutions were not copied.
The canonical catalogue records these alternatives and their rights disposition.

## Executed evidence

The adjacent `evidence.json` binds source hashes, fixture digests, native ABI
results, baseline failure, and the individual resource observations.

| Check | Observed result |
| --- | --- |
| Original native library with the added Java pathname test | Exit 1: `java.io.IOException: cannot open canonical image read-only` at `mapExact0` |
| Generated native library, actual Java 21 with `-Xcheck:jni` | Eight filename families; 336 checks pass |
| Actual native source included by C17 fault fixture | Ten cases; 102 checks pass; JNI outcomes mocked only for this cleanup fixture |
| Retained lookup corpus | All five fixtures; 43,015 original checks pass |
| Strict cross-process mapping | Three JVMs; 15,433 checks each; base `0x600000000000`; literal `0x600000014e9a` |
| Process-identity oracle | 25 tests pass, including a live child witnessed independently |
| Native/Java ABI | Generated JNI header and Java descriptors identical; four JNI exports unchanged |
| Recipe SDK | Five tests pass, zero failures, errors or skips; source-drift refusal and fixed point retained |

The three-process proof executes `-Xint`, `-Xbatch` and `-XX:-CompactStrings` JVMs,
checks the same inode and a read-only shared mapping through procfs, and retains
collision, wrong-digest and corruption refusal. The exact seven-file source closure
used for that proof matches the delivered sources. No address relocation or relaxed
namespace association was used to obtain a passing result.

On Linux with a Java 21 JDK and C compiler, the product regressions need no private
files or Maven dependencies:

```bash
export M3_JDK=/path/to/jdk21
export JAVA_TOOL_OPTIONS='-Xmx256m -XX:ActiveProcessorCount=2'
python3 m3/verify-gvas-lookup-index.py
python3 m3/verify-gvas-literal-precompute.py
python3 -m unittest discover -s m3/tests -p test_gvas_literal_process_identity.py -v
```

## Bounded lookup observations

The existing test's `bench` mode now separates initial native mapping/digest work,
image validation/index preparation, hits, misses, and reverse address reads. Each
fixture runs in three fresh JVMs, with 20 warmup passes followed by 100 measured
passes per workload. The forward workload consumes verified row checksums; reverse
addresses feed a volatile checksum. Inputs are prepared outside the timed loops.

For the 8,203-row normal fixture, each measured workload contains 820,300 operations:

| Measurement | Median across three JVMs | Observed range |
| --- | --- | --- |
| Process-cold mapping, native load and digest | 320.61 ms | 164.16–372.29 ms |
| Image validation and index preparation | 89.35 ms | 60.85–168.93 ms |
| Forward hit | 182.15 ns/operation | 131.26–210.50 ns/operation |
| Forward miss | 15.68 ns/operation | 12.12–21.00 ns/operation |
| Reverse address read | 10.35 ns/operation | 5.55–11.63 ns/operation |

The normal fixture retains 131,072 bytes of primitive index payload and maps 768,284
bytes. Thread allocation counters report zero bytes in each of its three measured
workloads. The tiny fixture records 288 hit bytes and 728 miss bytes across 1,100
operations; its short warmup does not establish a steady-state allocation claim.

These are diagnostic observations on a contended shared host. The OS page cache
was uncontrolled, and the small fixture can include JIT/warmup effects. No universal
RAM-speed, latency threshold or hot-lookup improvement is attributed to the pathname
repair. The hot lookup and reverse-address implementations are inherited unchanged.

The measurements can also be reproduced from this public checkout by using the
existing compiler/probe helpers directly:

```bash
python3 - <<'PY'
import hashlib, importlib.util, os, tempfile
from pathlib import Path
root = Path.cwd()
jdk = Path(os.environ['M3_JDK'])
spec = importlib.util.spec_from_file_location('proof', root / 'm3/verify-gvas-literal-precompute.py')
proof = importlib.util.module_from_spec(spec)
spec.loader.exec_module(proof)
with tempfile.TemporaryDirectory(prefix='m3-gvas-cost-') as temporary:
    work = Path(temporary)
    library = proof.compile_sources(work, jdk)
    test = 'com.m3.text.M3GvasLookupIndexContractTest'
    proof.run(str(jdk / 'bin/javac'), '--release', '21', '-Xlint:all', '-Werror',
              '-cp', str(work), '-d', str(work),
              str(root / 'm3/core/test/com/m3/text/M3GvasLookupIndexContractTest.java'))
    for kind in ('small', 'normal', 'collision64', 'collision65', 'collision'):
        image = work / (kind + '.m3gvas')
        proof.run(str(jdk / 'bin/java'), '-ea', '-cp', str(work), test, 'emit', str(image), kind)
        digest = hashlib.sha256(image.read_bytes()).hexdigest()
        for fork in range(3):
            proof.run(str(jdk / 'bin/java'), '-Xbatch', '-ea', '-esa', '-cp', str(work),
                      test, kind, str(library), str(image), digest, 'bench')
PY
```

Windows runtime, iOS, full M3JDK21 images, HotSpot/GC/CDS/jtreg and full reactor
qualification are outside this bounded Linux Java/JNI receipt. This receipt does
not advance a family phase or declare all M3String work complete.
