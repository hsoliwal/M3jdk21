<!-- SPDX-License-Identifier: Apache-2.0 -->
# Lane28 restoration and M3 SZF handoff

Synexia is the convergence workspace and code contributor. M3JDK21 is an
independent project and the sole owner of the adapted JDK runtime. The target
contains its Java/JNI implementation, recipes, source/licence provenance and
test fixtures; `java.base` has no Maven, OpenRewrite or Synexia runtime dependency.

| Convergence contribution | Target runtime owner |
| --- | --- |
| `com.synexia.common.collections.SegmentedIntLane28` qualified by M3 SZF | `jdk.internal.mindex.M3IntLane28` |
| `com.synexia.common.collections.SegmentedLongLane28` qualified by M3 SZF | `jdk.internal.mindex.M3LongLane28` |

Existing donor public names are compatibility surfaces, not target ABI names.
New convergence tooling is `synexia-openrewrite-recipes/crates/m3-szf`.
The target retains its established compact `M3*` internal names and current
`M3String`/backing/tuple/fact naming. No public Java SE contract is renamed.

## Why restoration is necessary

GitHub reports PR #146 merged, but target master
`87590cb96fb0e2dc0f88fae8e01957f7179cd255` contains none of the five primitive
owners, the production bit-lane C peer, or the Lane28 recipe crate. These paths
were present at `9af35b30528940a6aa83e710a4fc0d1b76d79799`. This is a LOST source
capability despite retained PR history. The version 2 recipe restores the source
additively. It preserves current master metadata and adds five versioned records;
it does not replay an old repository tree. Historical receipts remain unchanged.
Original mapping records, int/long postimages, manifest, pins and plan are retained
under `history/9af35b3` as exact historical evidence.

The initial lazy/bulk/rank/select/JNI capabilities survive in this candidate.
Only the private int/long zero-fill atom is superseded by the qualified M3 SZF
contribution at Synexia commit `aabe02e51718922c486fb04590af8818ccc7e653`
([PR #9428](https://github.com/hsoliwal/com.synexia/pull/9428)). `pins.json`,
`inputs.tsv`, `donor/szf`, and the five mapping records bind source and target
identities. A target test compares every byte after the explicit package/name
adaptation, while API descriptor tests protect callers.

## Qualification and limits

`mvn -B -ntp -f m3/tooling/lane28/pom.xml verify` runs the restored 12 checks plus
compiler-boundary evidence and seven sparse-fill checks (20 total). Java 21.0.8+9
and Maven 3.9.9 were run offline locally. The original 12 Java/JNI checks also
passed with nonrecovering UBSan. Eleven Python tests protect the strict XML
compiler checker, including missing/wrong/truncated evidence and the M3 guard.

Each baseline/candidate sparse runtime completes 286,214 checks and 3,000 seeded
fills. Both primitive fill methods must have actual C1 and C2 nmethods in the
normal-mode compilation logs. Interpreter and mixed-mode parity, equal public/
protected descriptors, allocation stability, empty/end/page/region bounds,
overlap and overflow are required. Deliberately premature bounds shortcuts and
region skips must fail. The original bit-lane native-mask and stale-rank mutants
also remain required. The new probe is included in rebuilt-image qualification.

Synexia's frozen instrumentation measured 65,536 -> 0 directory visits for an
empty full-domain clear and 65,536 -> 512 for one allocated page. Its two timing
forks observed lower sparse times; dense timings varied. Those observations are
bounded qualification, not a general performance claim or JDK admission.

The exact output plan passes apply/fixed-point/rollback and three no-write refusal
checks. Current master's 47 records and 20 gates remain intact. The overall
mapping remains INCOMPLETE; the restored records remain implemented-unverified.

Current master independently has TQ source/template drift: the live `M3TQ.java`
blob is `ca23741ecd8cd24593ba5c260aa0d939c53d6766`, while its recipe template is
`4f92643166133b0f527c88db65b8c48c4cf00402`. The live owner adds `containsAll`.
Its existing shared-metadata custody also predates additive records. This PR
preserves those newer owners and keeps the predecessor TQ gate in CI. Separate
reconciliation is required; no TQ or full-reactor pass is claimed here.

The current target must still pass full fastdebug-image build, image-local libjava,
jtreg, the full recipe reactor, target-wide custody and cross-platform gates.
Local execution used stock Java 21 with the exact product classes patched into
java.base and a test-only native loader. Historical pre-restoration image receipts
are not current-head image proof. `UseM3StringStorage` still forces interpretation;
its C1/C2 requests must prove interpreted mode and zero nmethods. The HotSpot
guard is unchanged. Automatic java.util consumer replacement remains separate.
