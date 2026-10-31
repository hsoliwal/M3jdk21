# Lane28 — primitive collection kernels in java.base

This is the next serial Synexia-to-JDK port after TQ. It brings the complete
current segmented int/long/bit lanes into the unexported `jdk.internal.mindex`
package. OpenJDK 21 public APIs, collection contracts, bytecode and linkage
behavior remain the compatibility baseline. Automatic `java.util` replacement
and wider runtime consumers remain separate admission work.

| Synexia source | JDK counterpart |
| --- | --- |
| SegmentedAddress28 | M3Address28 |
| SegmentedIntLane28 | M3IntLane28 |
| SegmentedLongLane28 | M3LongLane28 |
| SegmentedBitLane28 | M3BitLane28 |
| PackedBitAtoms | M3Bits (package-private arithmetic) |

The established `M3*Lane28` names preserve the earlier collection port lineage.
The older `com.m3.collections` proof module is retained. Product classes do not
depend on that module, Synexia artifacts, Maven or OpenRewrite.

Each owner uses a 28-bit logical slot, split 8/8/12. Absent pages read as zero;
zero writes do not allocate pages. Primitive pages stay stable. Range copies
are bounds-checked, support self-overlap and allocate no flattened scratch
payload. Entries do not have per-row wrappers; page arrays and directories
still allocate. These mutable owners are single-threaded.

The current bit lane includes Synexia's lazy rank/select precompute. It derives
exact page and region counts on first nontrivial use, then maintains them on
successful mutations. It retains no second bitmap. The JNI range-count peer is
ported into `src/java.base/share/native/libjava/M3BitLane28.c`, with the actual
JDK symbol and javac-generated header. Existing native discovery/header rules
are pinned in `inputs.tsv`; no native build rule is weakened. Java remains the
default path. The explicit native call is not automatically selected or claimed
to be faster.

## Recipe and proof

Use Java 21, Maven 3.9.9 and Linux GCC/JNI headers:

```sh
mvn -B -ntp -f m3/tooling/lane28/pom.xml verify
mvn -B -ntp -f m3/tooling/lane28/pom.xml -Dlane28.sanitize=true verify
mvn -B -ntp -f m3/tooling/tq/pom.xml verify
python3 m3/tooling/lane28/verify-plan.py
python3 m3/tooling/tq/verify-plan.py
python3 m3/migration/migration.py validate .
```

The mapping validator uses `m3/migration/requirements.txt`. The existing Java
and text snapshot engines execute generation, exact postimage/fixed-point,
occupied-target, parser, duplicate, missing-input, drift and tamper gates. The
existing bootstrap task recipe receives the exact crate path/root. The legacy
`jdk22-m3-lane28` resource key is a recipe grammar requirement, not a JDK22 donor.

`plan.json` uses the existing sealed installer for exact apply/rollback in a
matching, exclusively owned worktree. The verifier exercises historical replay
and no-write refusals in a temporary fixture. Shared mapping/naming/notice files
may gain additive entries in subsequent ports; all this packet's own records
and product bytes must stay exact. The existing TQ predecessor gate remains mandatory. Current master already has
independent TQ source/custody drift; this restoration does not replace those newer owners. No prior record or gate is removed or promoted.

The 12-test suite compares seeded operations with primitive JDK arrays,
`System.arraycopy`, `BitSet` and a sorted reference set. It exercises sparse,
word/page/region/end boundaries, zero allocation, overflow/refusal atomicity,
overlap, stable pages, cold/warm counts and writes after count preparation.
Malformed native directories and ranges must be rejected. Every correctness
JVM enables `-Xcheck:jni`. Interpreter, mixed, C1 and C2 modes each require full
parity; C1/C2 require compiled bit-lane kernel evidence. Deliberately missing
native masks and stale Java region counts must fail. UBSan is nonrecovering.

Local execution patches the exact five product classes plus a **test-only**
bootstrap native loader into stock Java 21. The loader opens the identical C
candidate as an isolated library; it is not a product class. That proves the
bounded Java/JNI behavior, not linkage in a rebuilt JDK. The separate CI image
job builds the full fastdebug fork, requires the symbol in its own `libjava`, and
runs seven modes without patch-module or an external library, including
C1/C2 requests with M3 enabled. The existing VM guard forces those requests
to interpreted mode. Normal C1/C2 require compiled kernels; M3 requests
require interpreted mode and no compiled methods. See `RESTORATION.md`
for captured full-image evidence. M3-enabled compilation remains unsupported.
Complete jtreg/TCK, broader lifecycle/platform,
memory/performance, remaining collections and automatic consumer gates remain
open. Existing reactor/99% coverage gates are not replaced by this crate.

## Upstream feedback

`feedback/` contains recipe-generated, revision-bound findings intended for
`com.synexia`. The cost probe uses two standalone warmed C2 forks without JNI
checking; correctness runs retain the checker. It reports construction, first
rank, warm rank/select and Java/JNI counts for the exact 32-page fixture.
These are descriptive measurements on one stock-JDK patch setup, not JMH,
cross-platform evidence, or a speed guarantee. Suggested improvements distinguish
measured costs from source-review opportunities and unmeasured work.

The authoritative map gains five records and preserves the previous 47 records
and 20 gates. Source/target hashes, adaptations, tests, dependencies and pending
consumers let later Synexia improvements update the same owners. Feedback is a
proposal to upstream; it does not authorize blind reverse-copying JDK adaptations.

## Provenance

Source commit: `123b49647bd3f1f7d5962a72a3e5bbb3cbc57038` in
`hsoliwal/com.synexia`. Complete originals, native peer, Apache-2.0 LICENSE and
NOTICE are under `donor/`. Port changes are package/names, the native symbol and
generated header, and JDK ownership documentation. Existing JDK/other-donor
licenses keep their scope. No external challenge-site solution was copied.

Sparse zero-fill now validates the range before the empty-owner shortcut and skips
absent region directories. See `RESTORATION.md` and `fixtures/Probe.java` for the
separate convergence and self-contained target qualification.
