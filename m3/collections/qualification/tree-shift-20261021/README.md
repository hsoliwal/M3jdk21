<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 native tree viewport sibling-shift correction — direct Java 21 owner

M3JDK21 is the **integrated JDK21/HotSpot/Java/JNI product**.
This is not `M3 + JDK`, a compatibility layer, a second tree model or a
JNI/GPU callback path. Original source:
`m3/collections/src/main/java/com/m3/collections/M3TreeWindowPlan.java`.

Canonical source-first authoring: [Synexia #12886](https://github.com/hsoliwal/com.synexia/pull/12886)
at `a168cebd5c95b3c9b4c47e9db5d44b8aca324f1a`, using the **unchanged existing**
`com.synexia.m3jdk.AstBuildReverse20261017` /
`M3HashPinnedJavaSnapshotRecipe`. The source-sealed dataset installs
one modified original Java owner and two new Java tests.

Original source: Git blob `8ca008289633710d9581d45ca93fb4c825d7ee67`,
SHA-256 `939c71664bda0330d30cbf88532226ec601c8908e12cf9a6a42a013bd3624368`.
New source: blob `f4f064f370a38bd8363b1e6309e4b151fb987b1c`,
SHA-256 `837aeb592e797902f9c50bcdbf4f85db3f9dbf97ee7537b5d6e85a9fa4a4e41a`.
Standalone oracle: `M3TreeWindowShiftContract.java`,
blob `81afe4e5d86062e91a3c20c749b150abb67a48fc`;
JUnit5 discovery: `M3TreeWindowShiftTest.java`,
blob `de6137afe743c003336a40d3c23a2c42668e9092`.

## Behavioral atom

Existing `M3TreeWindowPlan.shiftSiblings(parentId,fromIndex,delta)`
previously mutated earlier keys before discovering a later signed-index
overflow; a failure could leave the `long[] keys` lane inconsistent with
the current `M3LongLongHashMap slotByKey`. Left shifts could also collide
with still-resident sibling coordinates.

The same method now scans the relevant keys once to validate all final
int index bounds in long arithmetic and reject collisions against the
existing primitive map. Then it mutates original `keys[]` and invokes
the original reindex once. No object-per-row nodes or new store, no new
public method, ABI, source format, JNI symbol or external service.
O(n) preflight + O(n) original mutation/reindex, O(1) auxiliary space.
Valid insert/remove shifts, stable native handle identity, selection and
focus semantics remain in the original owner.

## Evidence and exact-head acceptance

Author local OpenJDK21.0.12.1 **method-level reconstruction**, not the full
product source: `javac --release 21 -Xlint:all -Werror` passes; prior
method fails at assertion **12** (partial lane mutation), candidate
passes **2,591,918 checks** in each default, interpreted, C1 and non-tiered
VM modes. The canonical source test blob is byte-identical to that locally
compiled oracle.

The new exact-head CI source job compiles the **real original owner source**
together with the existing `M3LongLongHashMap`, using only a test-time
`M3TreeProjection.Residency` *type stub* because it is not invoked by
the focused test. It runs the same oracle on the actual owner under four
JVM modes and checks exact postimage source SHA256. That hosted job must
execute and pass before claiming a full-source qualified behavior.

Still **NOT_RUN** here: actual Maven/OpenRewrite SDK `Recipe.run` and
real Result receiving/fixed point; JUnit on the owning Maven module;
complete M3JDK21 JDK/HotSpot/JNI build; original SWT/JFace/Nebula native
viewer, accessibility, event and pixel differentials; CPU/GPU and heap
benchmarks. Do not merge until exact-head acceptance proves them.

Original SWT `VirtualTreeResidentPlan` donor attribution/elected
Apache-2.0 OR EPL-2.0 notice remains unchanged. No challenge solution
source is copied. Synexia `develop` and M3JDK21 `master` were not
rebased, squashed, force-pushed or directly edited.

### Local reproduction of focused full source on Java 21

Use `M3TreeWindowPlan.java`, `M3LongLongHashMap.java`, the
standalone contract and the transient type-only `M3TreeProjection`
stub shown in `.github/workflows/m3-tree-shift-20261021.yml`.
For complete module execution use:
```sh
mvn -B -ntp -f m3/collections/pom.xml \
  -Dtest=M3TreeWindowPlanTest,M3TreeWindowShiftTest test
```
