<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3Grid native sparse collapsible model-row groups

## Motivation and owner

The canonical `src/java.desktop/share/classes/jdk/m3/swing/widgets/M3Grid.java`
inherits real `JTable`, `TableModel`, `TableColumnModel`, editor,
cell renderer and native selection/LookAndFeel behavior. This leaf
adds opt-in collapsible row groups inside **that owner**, not an SWT
bridge, a second JTable or a parallel value store.

A group is a contiguous **model-order** range
`[firstModelRow, endModelRowExclusive)` with the existing first model
row as its header, never a synthetic header row. Child rows are hidden
when collapsed, with their original model values untouched.

## Sparse algorithm, contracts and limits

- Only group metadata is retained: three parallel primitive int lanes
  per configured group (first model row, exclusive end, collapse flag)
  plus one String title. Groups are kept in ascending, non-overlapping
  model order. Capacity is explicitly bounded to 4,096 groups, not rows.
- When the first group is added, a private opt-in `GroupRowSorter`
  presents the collapsed model ranges as an indexed view. Its arrays
  `hiddenStart[]`, `hiddenEnd[]`, `viewThreshold[]` and
  `prefixHidden[]` contain only collapsed ranges. Model-to-view and
  view-to-model rank/select queries use O(log G) binary search.
- No `TableRowSorter` per-row Entries, dense visible map, per-child
  widgets or duplicated model cell values. A million-row model with
  two huge collapsed groups stays sparse. Rebuilding after group
  mutation is O(G); adding a group can shift O(G) metadata. This is
  not yet a mutable AVL/O(log G) update implementation.
- A caller-owned external `RowSorter` is not replaced implicitly:
  adding groups while a foreign sorter is installed fails with
  `IllegalStateException`. Explicitly setting an external sorter
  clears groups and takes normal JTable ownership.
- The sparse sorter does not reorder or compare model rows.
  `toggleSortOrder` on an ordinary JTableHeader click is harmless
  (maintains original model order) instead of crashing the EDT;
  nonempty direct `setSortKeys` is explicitly unsupported.
- The first group row is retained and painted as a disclosure header.
  A left-edge native mouse action, or Alt+Space on a selected group
  header, toggles its children. The painter queries only group headers
  intersecting the resident viewport and visible pinned bands.
- Existing frozen-column headers, pinned top/bottom rows, merged-cell
  spans, sorter-coordinate hit testing and original JTable editors
  remain in the same component. A span with hidden/nonconsecutive
  model rows becomes inactive via the existing fail-closed mapping.
- Model INSERT/DELETE/structure events clear unkeyed group ordinals
  before Swing processes the structural change. A model-specific
  stable-row-key/group feature remains out of scope.
- Serialization reconstructs only the transient sparse RowSorter
  after validating the saved group lanes. It preserves saved collapse
  flags, view-row counts and group labels. Previous M3Grid streams
  missing these new fields are accepted, with empty group lanes
  initialized. No serialVersionUID change.

### Explicitly not claimed

The grouping support is **flat** and model-order only. Nested group
hierarchies, synthesized summary rows, per-group sorting/filtering,
cross-group merged spans, tree icons beyond the disclosure, accessibility
hierarchy, alternative LookAndFeel and complete Nebula Grid event
parity require separate owner-specific work.

## Donor and source provenance

| Source | Immutable Git blob | Disposition |
|---|---|---|
| Nebula GridRowTree.java | `7db36cf1d82136fb9bca23354aff6f321ebd323e` | Apache-2.0 OR EPL-2.0, rank/select behavior donor only |
| Nebula GridRowIndex.java | `1e37c7615b999072cc62b374dd691dcb2b001dd0` | Apache-2.0 OR EPL-2.0, interface requirement donor only |
| Canonical M3Grid prior grouped extension | `774225874c6c35f1d733b17ba087e4a1ae6af9a2` | Existing owner's direct preimage |
| New M3Grid.java | `cfec30aba24110841a1a617fd45f67db34ec2c9e` | Exact compiled/tested first-party source |
| M3GridGroupsSmoke.java | `470dd331f06c3c8c428249e11638ef7b361ca089` | Basic behavior/sorter/header/1M gate |
| M3GridGroupsMappingSmoke.java | `164a57c802fb354c9fc4e6b018513ef7cb687320` | 24K-row, 600-group deterministic random dense oracle |
| M3GridGroupsSerializationSmoke.java | `871515f0c3433d05341cd2237cf0ab2f6f5d1876` | Serialization correctness and reconfiguration |
| M3GridGroupsHeadfulSmoke.java | `356b69ae6999d529fd1037ae99619152284a8761` | Native Xvfb AWT Robot disclosure/mouse proof |

No EPL/SwingX LGPL implementation source was copied into this
Apache-2.0 `java.desktop` owner.

## Executed qualification

**Stock OpenJDK 21.0.12.1, Linux, 128 MiB test heap:**

- Strict `javac --release 21 -Xlint:all,-serial,-this-escape
  -Werror`: PASS.
- Actual `--patch-module java.desktop` strict compile and named-module
  execution: PASS.
- Six headless programs PASS: inherited `M3GridContractSmoke`,
  `M3GridPinnedRowsSmoke`, `M3GridSpansSmoke`,
  new `M3GridGroupsSmoke`, `M3GridGroupsMappingSmoke`,
  `M3GridGroupsSerializationSmoke`.
- Four Linux/Xvfb Robot programs PASS: inherited
  `M3GridHeadfulSmoke`, `M3GridPinnedRowsHeadfulSmoke`,
  `M3GridSpansHeadfulSmoke`, and new
  `M3GridGroupsHeadfulSmoke` (two successive disclosure clicks).
- 1,000,000-row model, two large collapsed ranges, 3,000 sparse
  groups and 128 MiB heap: PASS.
- An independent 24K-row dense oracle checked **every** model/view
  index for 600 sparse groups over 45 seeded collapse toggles: PASS.
- Group serialization round-trip initially failed (group metadata
  persisted but 100 model rows reappeared); after the `readObject`
  atom restored the sorter, both collapsed group counts persist: PASS.
- Old pre-group Grid serialization from blob
  `774225874c6c35f1d733b17ba087e4a1ae6af9a2`
  read with this new class and its grouping APIs: PASS.
- An ordinary stock JTableHeader sorter toggle cannot throw or
  change model ordering under grouping: PASS.
- Public `javap -public` signature-set comparison against the
  pre-group M3Grid: **0 removed signatures**, additive methods only.

## Reproduction

From repository root, with Java 21 and Xvfb available:

```sh
ROOT=src/java.desktop/share/classes
TEST=m3/ports/swing-viewport/src/test/java
OUT=/tmp/m3-grid-groups
mkdir -p "$OUT"
javac -Xlint:all,-serial,-this-escape -Werror \
  --patch-module java.desktop="$ROOT:$TEST" -d "$OUT" \
  "$ROOT/jdk/m3/swing/widgets/M3Grid.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridContractSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridPinnedRowsSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridSpansSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridGroupsSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridGroupsMappingSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridGroupsSerializationSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridHeadfulSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridPinnedRowsHeadfulSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridSpansHeadfulSmoke.java" \
  "$TEST/jdk/m3/swing/widgets/M3GridGroupsHeadfulSmoke.java"
java -Xmx128m -Djava.awt.headless=true \
  --patch-module java.desktop="$OUT" \
  -m java.desktop/jdk.m3.swing.widgets.M3GridGroupsMappingSmoke
xvfb-run -a java -Xmx128m -Djava.awt.headless=false \
  --patch-module java.desktop="$OUT" \
  -m java.desktop/jdk.m3.swing.widgets.M3GridGroupsHeadfulSmoke
```

## HOLD gates

Complete source-built M3JDK and jtreg, full Maven/JUnit reactor,
Win32/Cocoa/GTK4/HiDPI/LAF, keyboard and screen-reader differentials,
native input ergonomics, tree-style nested groups, user-specified
external sorter composition, large-N performance comparisons,
canonical Synexia OpenRewrite recipe source-seal/fixed-point,
API/serialization JDK-wide qualification remain open. This is an
incremental functional widget feature, not complete Nebula Grid parity.
