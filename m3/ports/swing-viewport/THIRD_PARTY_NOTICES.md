<!-- SPDX-License-Identifier: Apache-2.0 -->
# Third-party notices: m3/ports/swing-viewport

| Path | Origin | Licence used here |
|---|---|---|
| `src/main/java/com/m3/swing/viewport/engine/residency/**` | hsoliwal/eclipse.platform.swt PR #122, commit `136df9c62ba5`, renamed by `m3/tooling/residency-rename` (M3jdk21 #826) | Apache-2.0 (the files are `EPL-2.0 OR Apache-2.0`) |
| `src/main/java/com/m3/swing/viewport/engine/swap/**` | Synexia swap owners, ported in SWT PR #122 | Apache-2.0 |
| `src/main/java/com/m3/swing/viewport/engine/PackedHeapGrowth.java` | Synexia `M3PreGc` (#10879), specialized in SWT PR #122 | Apache-2.0 |

`ItemBitSet` adapts the container idea of RoaringBitmap (Apache-2.0); no RoaringBitmap code is copied.

The engine files keep their original headers. Do not edit them by hand: re-run the recipe, which refuses on
drift (exit 3) and on source mismatch (exit 4).
