# UI label interning and tree path ids (design)

Owner: ui-interning agent. Branches: hsoliwal/M3jdk21 `m3/ui-interning-20261019` (proving ground,
`m3/collections`), hsoliwal/eclipse.platform.swt `m3/swt-interning-20261019` (package
`org.eclipse.swt.internal.m3.collections`). Status: section 8.

## 1. Why

User, 2026-10-19: "text labels in most UIs are repeats, so M3String-like local interning in the UI SDK makes sense";
"how path can be useful for tree-like widgets". Measured on real data (`REPEAT_RATIOS.tsv`):

| Data | Rows | Distinct labels | Distinct per row |
|---|---|---|---|
| S: drive, 4 Explorer columns (name, type, size, date) | 8,000,000 cells | 174,448 | 0.0218 |
| S: drive, Name column alone | 2,000,000 | 169,618 | 0.0848 |
| JSON viewer sample, key column | 637,594 | 11,520 | 0.0181 |
| JSON viewer sample, key: value labels | 637,594 | 221,648 | 0.3476 |
| SWT Table differential (worst case: unique per row) | 900 | 900 | 1.0 |
| Path components, S: drive / JSON | 22.6 M / 2.65 M | 169,618 / 11,520 | 0.0075 / 0.0043 |

Full path strings are all distinct (one per node), but the segments they are made of repeat 133x (S:) and 230x
(JSON). Storing a String per row stores every repeat again; storing a String path per node stores every prefix again.

## 2. One model (memory `feedback-one-model-delta-processing`)

Label ids and path ids live in the ONE model's lanes. A widget stack (SWT Table or Tree, its JFace viewer, data
binding, or a Swing JTable/JTree with its model) owns exactly one `M3LabelPool`, one `M3TextExtentCache` and, for
trees, one `M3TreePathTrie`. SWT items, JFace label providers and Swing renderers read and write the same int ids:

- a JFace label provider's text is interned once into the model's pool; SWT `TableItem.getText` answers the pool's
  String facade for the row's id; a Swing renderer reads the same id;
- no layer keeps its own String per row or its own path map.

The pool is LOCAL (per model), not global: its lifetime is the widget's, it needs no locks (one owner thread), and
dropping the widget drops every label at once. On M3JDK the pool can key on the JDK's canonical M3String ids instead
of hashing text (section 4.4).

## 3. Lanes and facades (brief rule 18)

| Class | Lanes (per id) | Facades | Bytes per entry |
|---|---|---|---|
| `M3LabelPool` | `byte[]` arena (Latin-1 1 byte/char, UTF-16 2 bytes/char per label, as JDK compact strings); `long` span (offset, length, coder); `long` key (String.hashCode + length, or canonical id); `int` refcount; `int` prev/next (LRU of released labels, free list); `int[]` slot table | `String` per id, created only by `text(id)`, dropped on eviction or `trimFacades()` | 24 + text bytes + 8 table |
| `M3TextExtentCache` | `long` key (labelId << 32 \| fontId); `long` extent (width << 32 \| height); `byte` CLOCK bit | none | 17 per slot |
| `M3TreePathTrie` | `int` parent, segment, depth, firstChild, nextSibling, prevSibling; `long` flags; edge table `long` key (parentPathId << 32 \| segment) + `int` id | none (`segments(id, int[])` fills a caller array) | 36 + 12 per edge slot |
| `M3LabelLanes` | one `int[]` label-id lane per column | String facades from the pool | 4 per cell |

No Entry objects, no per-row allocation on any hot path, no `String.intern()` (TIL law).

## 4. M3LabelPool

### 4.1 Contract
- `intern(String)` / `intern(char[], int, int)` return an id and add one reference; equal text gives the same id
  while the label is live or cached. Id 0 is the empty string, permanent.
- `release(id)` drops a reference. At zero the label moves to the MRU end of the released list; it stays findable
  until more than `maxCached` labels are released, then the least recently released label is evicted: its slot is
  deleted, its arena bytes become garbage, its id goes to the free list and the eviction listener hears the id (the
  extent cache drops that id's widths).
- Referenced labels are never evicted: the bound applies to released labels only, so rows never lose text.
- `text(id)` returns the String facade (created once, then reused); `length`, `charAt`, `getChars`, `contentEquals`,
  `compare` and `hash` answer from the arena without creating a String.
- Not thread-safe: one model owner thread.

### 4.2 Algorithms (rule 21; rows in `ALGORITHM_CATALOGUE.tsv`)
- Open addressing, linear probing, `int` slot = id + 1, power-of-two table at most half full; slot index from a
  Fibonacci multiplicative mix of `String.hashCode()` (cached by the JDK, so a repeated label costs no rehash).
- Deletion by backward shift (Knuth algorithm R): no tombstones under churn.
- Released labels form an LRU list in `int` lanes (LeetCode 146 shape without nodes).
- The arena is compacted in O(live bytes) when garbage exceeds the live bytes.

### 4.3 Measured effect
See section 8 and `BENCHMARKS.tsv`.

### 4.4 M3JDK in place
`M3LabelPool.CanonicalIds` maps a String to a 64-bit id that is unique per content (contract: injective, never 0).
With it the pool keys on that id and compares ids instead of text. In java.base the implementation is the M3String
canonical atom id from `java.lang.M3StringPool` (package-private there, reached through the M3JdkHandoff packet when
the class moves into `jdk.internal.util`). The proving ground tests it with an injective test map.

## 5. M3TextExtentCache

- Key `(labelId, fontId)` packed in a `long`; value width and height packed in a `long`.
- Open addressing with backward-shift delete; bounded by `maxEntries`; CLOCK second-chance eviction when full.
- `invalidateFont(fontId)` (font change), `invalidateAll()` (DPI/zoom change), `invalidateLabel(labelId)` (wired as
  the pool's eviction listener, since evicted ids are reused).
- `widest(labelIds, from, to, fontId, measurer)` measures each distinct label once; the measurer is called only on
  a miss, so `misses()` counts the measurement calls the toolkit had to make.

## 6. M3TreePathTrie

A path is the sequence of segments from the root: child indices (SWT/JFace index paths), element ids, or label ids of
names (file systems, JSON keys). Each node path gets one `int` path id; a child shares its parent's prefix by
pointing at the parent id. Children are found by one probe of the edge table keyed by `(parentPathId, segment)`.
Radix compression is rejected because a compressed node would cover several paths (no id per path).

Uses:
1. **Expansion that survives refresh**: `flag(pathId, EXPANDED)`; after a full rebuild the viewer re-derives each
   node's path id from its segments (names or element ids) and reads the flag. No String paths, no element map.
2. **Selection and caret persistence**: the same flags (`SELECTED`) and one caret path id.
3. **FilteredTree ancestor marking**: `markAncestors(match, MATCH_ANCESTOR)` walks up the parent lane and stops at
   the first node already marked, so marking all matches costs O(marked nodes) in total.
4. **Lazy loading by path**: `segments(pathId, int[])` gives an `ILazyTreePathContentProvider` its TreePath
   segments; `appendPath(pathId, sb, separator)` gives a file path or a JSONPath (`$.a.b[3]`) from pooled names.
5. **Path to row**: `M3TreePathRows.row(topology, trie, pathId)` and `pathOfRow(...)` over `M3TreeTopology` (the
   order-statistic forest): O(depth x log k).

With an `M3LabelPool` the trie owns one reference per name segment and releases it when the path is removed.

## 7. Adoption

`ADOPTION.tsv` lists, per owner (Tree, Table, List, Grid, JFace, Swing caches), the call sites that switch to label
ids and path ids and the expected savings. TreeViewport, TableViewport and ListViewport are not edited by this leaf.

## 8. Status

Filled in as leaves land (tests, coverage, benchmarks, differential).
