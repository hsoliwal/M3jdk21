<!-- SPDX-License-Identifier: Apache-2.0 -->
# Contract-preserving Java21 ctor warning repair for M3 object-state slots

**Convergence source:** hsoliwal/com.synexia. **Product receiver:** hsoliwal/M3JDK21.
This is a separate tiny **recipe-first** repair, not a new implementation of
object-state bitmasks, primitive arrays, residency, JNI or HotSpot.

The current M3JDK21 public `M3ObjectStateSlots28` class deliberately
uses existing `M3SlotAllocator28` generations, lazy byte state lanes,
read/modify/clear masks, in-place stable two-pointer handle compaction
and explicit per-object-state flags without retaining Map.Entry nodes.

**Actual hosted failure:** M3JDK21 PR #1099 inherited
[M3 four-library receiver admission run #39030218035](https://github.com/hsoliwal/M3jdk21/actions/runs/39030218035);
javac `-Xlint:all -Werror` warns because a public class in an exported
module package exposes an *implicit* public no-arg constructor.
This is the existing core owner's issue, not the new Java/JNI selection
algorithm's behavior.

**Exact preserving atom:** insert
`public M3ObjectStateSlots28() { }` with Javadoc.
Original source Git blob `c497ef68f62c86814ac6e04eecf73e57496c7209`
→ generated Git blob `ba17b0a264e8781b6f0a4ed33c2d067f6d53daaf`.
The constructor is already publicly callable via Java's implicit rule;
making it explicit does not add/remove any previously callable descriptor
or change initialization. It fixes the compiler diagnostic without
suppressing warnings, relaxing the gate, or replacing state owners.

**Recipe:** `com.synexia.m3.M3Jdk21StateSlotsConstructor20261017`
reuses Maven/OpenRewrite `M3HashPinnedJavaSnapshotRecipe` with a
SHA-256-pinned original source and exact generated output. The canonical
Maven JUnit test runs `Recipe.run`, fixed point, strict tampered/missing
original refusal and verifies that source text differs by precisely
the explicit constructor declaration. Repair source recipe, not the
generated product.

The M3JDK21 receiver should compile the exact old/new products, compare
`javap -public -s` contracts and rerun strict Java21 and existing
JNI/collection tests. Current installed source-manifest controls must
be requalified with the canonical inventory recipe if its source hash
changes; do not hand-update historical manifest records.

No full JDK/jtreg/JCK, GC/runtime allocation, LeetCode donor source
or performance improvement is claimed by this bounded diagnostic fix.
