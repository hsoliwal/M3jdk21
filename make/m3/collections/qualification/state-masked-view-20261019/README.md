<!-- SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0 -->
# M3JDK21 native state-mask live views — 2026-10-19

**M3JDK21 is the implementation and product, not an M3 plus JDK wrapper.**

## Scope and exact source custody

This PR changes the original `src/java.base/share/classes/jdk/internal/m3/M3IndexedArray.java`
and `M3IndexedView.java` behavioral atoms in their existing M3JDK21 java.base owner. It
adds one jtreg contract file; no new collection hierarchy, payload owner, native exports,
or dependency on a private Synexia runtime is introduced.

The canonical reusable **OpenRewrite 8.17.1** Java snapshot recipe is the existing
`M3HashPinnedJavaSnapshotRecipe` under
`m3/tooling/shared-frontend/recipe-custody/sdk-owner`, *reused unchanged*.
The new `m3-state-masked-views-20261019` dataset includes exact old/new Java snapshots,
source SHA-256 seals, a named OpenRewrite recipe and mode metadata. The resulting product
sources match the target snapshots. Transformations are admitted serially, per file;
divergent sources are refused. The existing SDK's five tests include actual Java LST
parsing, replay, fixed point, reversed-order admission, mixed states and drift rejection.
This is the same M3JDK21 recipe authority already used by the compiler frontend.

## Data invariants

- The only value owner stays `Object[] values`. `int[] order`, `int[] states`,
  `int[] generations`, `int[] leaseCounts`, and slot offsets remain authoritative.
- Required/forbidden tests use complete signed **32-bit** masks. Every selected row
  is a physical slot index in an ordered `int[]`; no per-entry object or second value copy.
- The existing `stableSort` compares Java payloads but moves *only* primitive
  slot indices. Equal keys keep encounter order.
- The new `stateVersion` changes on append, replace, remove, retain and release.
  Legacy filtered/sorted views keep their original value-only `version`.
  In particular, a cooperative lease does not invalidate a value-only cursor.
- A state-sensitive bound iterator, subrange or spliterator refuses later state
  changes. Its unbound root/view can refresh lazily.
- Contradictory masks are rejected before a view is made; unknown mask bits
  simply match no required rows and do not exclude absent state bits.
- `M3IndexedArray` handles are generation-qualified; slot reuse never revalidates
  a removed handle. These are M3 **lease counters**, not JVM-wide object reference counts.
- A Java comparator never crosses a JNI boundary. Existing native mask selectors
  and their ABIs remain unchanged in this slice.

## Reproduction

```sh
root="$PWD/m3/tooling/shared-frontend/recipe-custody"
mvn -B -ntp -f "$root/pom.xml" \
  -Dm3.dataset=m3-state-masked-views-20261019 \
  "-Drecipe.owner.root=$root/sdk-owner" -Pcontract verify
python3 "$root/receive.py" --dataset m3-state-masked-views-20261019 \
  --recipe-owner "$root/sdk-owner" --target "$PWD" --check
```

The PR workflow then strictly compiles the **actual java.base owners** with Java 21,
patches them into the test VM, and executes the old indexed-array and epoch suites plus
the new randomized state-mask oracle, in both normal and interpreted VM modes.

## Review state and further convergence

Submitted as **draft** pending actual GitHub-hosted SDK and product test results.
No successful Maven, JDK image or JNI run is claimed merely from preparing blobs.
This slice is only one qualified behavioral family, not completion of all collections,
Guava/Eclipse Collections/Spring/Apache providers, every parser, or the Eclipse JDT frontend.
The next direct M3JDK21 compiler change should reuse the merged Synexia selective-JDT
`MIndexSelectiveAstMasterAdapter` with actual Eclipse JDT/parser bindings and preserve
native compiler ownership; *do not* build another independent parser runtime or wrapper.
Keep product and recipe changes reviewable as one PR and update donors by category.
