# M3 atom JIT gate (per-atom JIT retest gate)

`m3/tooling/atom-jit-gate` is qualification tooling: it is never product code and never lives under `src/`. It holds
the per-atom JIT retest gate that the other convergence leaves use to show that an atom behaves the same when the JVM
interprets it and when it compiles it. Maven `com.m3:m3-tooling-atom-jit-gate`, package `jdk.m3.tooling.atomjit`,
automatic module `org.m3jdk.tooling.atomjit`. Like its sibling `m3/tooling/atom-census`, it is a standalone pom with
its own workflow (`.github/workflows/m3-tooling-atom-jit-gate-20261106.yml`). It is not a module of the `m3/pom.xml`
reactor.

All of it was relocated by recipe from hsoliwal/com.synexia develop `b65df53330e3`, never edited by hand. The recipe
is `recipe/atom-jit-gate-20261106/relocate.py` v4. `RELOCATION_MAP.tsv` lists the rows and `PROVENANCE.tsv` pins the
donor and target hashes.

| Owner | What it does |
|---|---|
| `OpenJdkOrderedPasses` | Runs four passes in order. **ATOMIZE**: the M3 atom census reads one OpenJDK source file (`src/<module>/<os>/classes/...`). **JIT**: child JVMs run under `-Xint` and with forced compilation (`-Xbatch -XX:-TieredCompilation -XX:CompileThreshold=100 -XX:+PrintCompilation`). The 80,000-call `hotLoop` digest must be equal in both modes, and the compiled run must show a `::hotLoop` compilation. **PATTERNIZE**: records each atom's census catalogue pattern and exact digest. **RETEST**: runs the same child JVMs again. The source must not drift between passes, and nothing is ever promoted. |
| `OpenJdkAtomJitPatternGate` | Reviews the serial per-atom phase ledger: ATOMIZE, JAVA_COMPILE, JIT_XINT, JIT_C2, JNI_PARITY, PATTERNIZE, DIFFERENTIAL, TARGET_JTREG. The whole plan must use one JDK runtime root, and Xint and C2 must behave the same. JNI may be N/A only for Java atoms, and phases run in prerequisite order. A `Plan` has a sealed root and its ordered targets. |
| `OpenJdkAtomPatternGate`, `OpenJdkWorkOrderCli` | Reads a sealed `SOURCE_WORK_QUEUE.tsv` (one snapshot, checked by SHA-256) and gives each path a source lane, an atomizer, a patternizer and pre- and post-pattern proofs. It refuses forged rows. |
| `OpenJdkMaskJitProbe` | Compares `jdk.internal.mindex.M3CollectionMaskCompactor.compact` interpreted vs C2 inside an installed M3JDK image. The result is bound to the image root: launcher, libjvm, `lib/modules` and `release`. |
| `OpenJdkIndexMaskJniProbe` | Checks parity of `jdk.m3.collections.M3IndexMaskCompactor` across the Java path, `NATIVE_REQUIRED` and the direct C exports, under `-Xcheck:jni`, on the same image root. |
| `fixture-gate/` | The fail-closed Java/JNI fixture gate (`verify.py`). It checks source custody, then runs `javac -h`, compiles C17 and C++20, links, checks `nm -D` exports and runs `javap`. It requires exact exit, stdout and stderr parity across interpreted, C1, C2 and default tiered runs. A negative control must refuse an injected drift. |
| `longset-proof/` | The LC705 long-set add-atom proof: the single-probe oracle, the derived baseline and the JIT warmup. It runs against `jdk.m3.collections.M3LongHashSet`. |

## Atomize with the census (Phase A NEC)

The donor atomized with its own `M3FileAtomizer` and patternized with `M3AtomPatternIndex`. That index keys every
pattern by contract, structure and logic roots. `M3AtomDescriptor` carries none of these roots, so the index cannot be
re-bound onto the census without a second atomizer. That re-binding is a NEC (`recipe/atom-jit-gate-20261106/PHASE_A_NEC.tsv`,
`phase-a-nec.sh`). The gate therefore uses the census as it is:

- **atoms:** the executable Java atoms that `M3AtomCensus --semantic` finds in the file. They are bound to the exact
  bytes by the census FILE_CONTAINER digest.
- **patterns:** the census catalogue pattern plus the exact source SHA-256 of each atom. Exact recurrence is reproduced
  (identical to the donor's on the measured corpus). Projection recurrence is not: the donor's `M3P1-*` ids and its
  `UNPROVEN_LEAF` state have no census equivalent.
- **limit:** the census refuses any source that declares enum constants ("unbound semantic source span"). That is 33
  of the 353 m3/collections and m3/algorithms files, and `atomize` inherits the refusal.

## Run

```text
mvn -f m3/tooling/atom-census/pom.xml -DskipTests install     # the atomizer the gate binds
mvn -f m3/tooling/atom-jit-gate/pom.xml package                # -Xlint:all -Werror, JUnit 5
JAVA_HOME=<JDK 21> bash m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/verify.sh <donor-git> <donor-rev> <scratch> [m2]
python3 m3/tooling/atom-jit-gate/fixture-gate/verify.py --evidence <dir> [--negative-test]   # Linux: gcc, g++, nm, JDK 21
```

The measured figures of every gate are in `recipe/atom-jit-gate-20261106/GATES.tsv`. Lessons learned are in
`RECIPE_MEMORY.tsv`.

**Not proven by this module:** a full OpenJDK image, jtreg, the installed-image mask probe and the live JNI parity of
`libm3_collections_mask`. These need an M3JDK image and the m3/collections native library.
