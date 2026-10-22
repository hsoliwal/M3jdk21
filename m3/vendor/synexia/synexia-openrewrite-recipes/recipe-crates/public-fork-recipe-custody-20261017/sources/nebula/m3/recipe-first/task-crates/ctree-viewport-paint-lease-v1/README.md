<!-- SPDX-License-Identifier: Apache-2.0 -->
# CTree original paint-region resource convergence

**Scope:** one behavior-preserving FILE atom on the original
`org.eclipse.nebula.widgets.ctree.CTree` (EPL-2.0), not a replacement
SWT widget and not a declaration that the full CTree backend is qualified.

## Mechanically executable Maven recipe

```sh
mvn -B -ntp -f m3/recipe-first/task-crates/ctree-viewport-paint-lease-v1/pom.xml verify
```

At **validate**, `CTreePaintLeaseRecipe.java` verifies the exact live EPL
postimage and reconstructs the **original** `CTree.java` from Git commit
`9943511a112e25c9fdaeb1e92442674fa0d19443`. It applies only the
two-hunk, one-target `CTreePaintLease.patch` to a separate temporary folder;
checks identical SHA-256 to the reviewed output; refuses a second application;
and reverses to the original source. This is a **replay of the source-changing
recipe**, not a hand-waved source hash or a second implementation.

At **verify**, `CTreePaintLeaseContractTest.java` checks original paint
callback ordering, closing both outer and scratch GC/Image pairs on paint
exceptions, viewport-empty fast-path placement before scratch allocation,
and negative mutations that remove those fixes. The SWT widget source stays
EPL-2.0; the patch of its source also stays EPL-2.0. Only independently
authored Maven orchestration, tests and documentation are Apache-2.0.

## Explicit gates

| Gate | Status |
| --- | --- |
| Pinned single-file original/postimage SHA-256 | Source pins recorded |
| Two-hunk patch replay + reverse + fixed point | Maven executable created; CI pending |
| Java21 source-shape and mutation refusal | Maven executable created; CI pending |
| Native SWT `Image` and `GC` exceptional disposal | Code added; real SWT UI tests pending |
| Original paint order and `EventTable` listener semantics | Static-order check present; GUI differential pending |
| Text/IME/accessibility/focus/keyboard/selection/disposal | Existing SWT/Nebula owners unchanged; not qualified here |
| Windows, macOS and Linux GUI rendering parity | Not yet executed/qualified |
| Skija GPU/software backend substitution | Out of scope; no runtime introduced |

The patch is intentionally EPL-2.0 as a derivative of EPL widget source;
it is not imported into Synexia Apache-2.0 runtime modules.

The original `CTree` retains its public API and existing painter entry
points. The scratch canvas is created only when painted items exist. This
candidate avoids allocation for an empty *viewport projection*, not all
offscreen geometry cases. No new renderer or event dispatcher is introduced.

The broader catalogue and contract admission matrix belong to
`hsoliwal/com.synexia`, whose canonical Maven/OpenRewrite recipe machinery
is reused for new first-party Java/JNI algorithm atoms. It will not
automatically replace other original Nebula or SWT widgets merely because
their algorithmic shapes appear similar.

To run, use JDK 21, Git containing the pinned base commit
(`fetch-depth: 0` in CI), and Maven. The Maven crate must not be listed as a
runtime dependency of SWT/CTree. If the original CTree source changed
concurrently, this source-sealed recipe **must fail closed** pending review.
