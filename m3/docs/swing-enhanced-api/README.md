# Enhanced Swing API (M3JDK)

User, 2026-10-19: "make the SWT Swing API richer". The enhanced-API map (SWT fork `docs/m3/ENHANCED_API_MAP.tsv`, branch
`m3/enhanced-api-20261017`) lists capabilities with a Swing applicability column. This track ships the rows marked
for Swing in `java.desktop` itself: M3JDK21 is the JDK, so the additions are made in place, not in a side library.

## Rules

- **Additive.** Nothing that exists changes behaviour (BCP). An application that does not call the new API sees
  stock behaviour; the differential test in `m3/ports/swing-enhanced-api` runs one probe of `JTable`, `JList` and
  `JTree` against master's classes and against this branch's, and the outputs must be byte-equal.
- **Natural Swing shape.** New behaviour goes onto the existing classes as methods where a Swing developer would look
  for it: `JTable` / `JList` / `JTree` `getSelectionRanges`, `getVisibleRange`, and `scrollRowToVisible(row,
  alignment)`. Small new types in `javax.swing` are added only for values and for capabilities that span the
  three components (`M3ItemRange`, `M3ScrollInfo`, `M3RowMath`, `M3Emptiness`, `M3TypeAhead`, `M3DiffApply`).
- **Parallel names.** A concept has the same name in SWT and Swing (`M3ItemRange`, `getSelectionRanges`,
  `M3Scrolling.TOP`, ...). The pairs are recorded in the SWT map (column `proposed_m3_surface`, a `Swing:` part)
  and in `API_DECISIONS.tsv` (column `swt_twin`).
- **Review.** Every public addition is one row of `API_DECISIONS.tsv`: class, member, rationale, SWT twin,
  compatibility note. The user reviews that table.
- **DOOP.** The additions read the existing lanes rather than building objects:
  - the selection model's bit set, walked as runs;
  - the M3 tree layout caches (`sun.swing.tree.M3VariableHeightLayoutCache`, `M3FixedHeightLayoutCache`), through
    `JTree`'s row API;
  - the `JList` row offsets of the Swing viewport work.
  The M3JDK Swing viewport branches are reused and not edited.
- **Tests.** JUnit on JDK 21 with `--patch-module java.desktop=...`. The changed classes are compiled alone against
  the JDK's own module. Coverage of the new code is at least 99%. Build output is written to `S:/m3-scratch`.

## Lineage

These classes and methods are the Swing counterparts of the SWT enhanced API written by Hitesh Soliwal (Synexia):
SWT fork `hsoliwal/eclipse.platform.swt`, branch `m3/enhanced-api-20261017`, licensed `Apache-2.0 OR EPL-2.0`. The
Swing code is written fresh against Swing's types. It is not a copy, and it is licensed in `java.desktop` under
GPLv2 with the Classpath exception by the same rights holder. New files carry the Synexia GPLv2+CPE header. Edits to
Oracle files keep Oracle's header and are listed in `API_DECISIONS.tsv`.

## Leaves

| leaf | content | status |
|---|---|---|
| S1 | `M3ItemRange`; `getSelectionRanges` / `setSelectionRanges` / `getVisibleRange` on JList, JTable, JTree | in progress |
| S2 | `M3Scrolling` alignments, `scrollRowToVisible(row, alignment)`, `M3ScrollInfo` | planned |
| S3 | `M3RowMath` (JTree visible-row arithmetic over the layout cache) | planned |
| S4 | `M3Emptiness` (row count crossing zero, for all three) | planned |
| S5 | `M3TypeAhead` (`indexOf` with prefix/exact and wrap; JTable included) | planned |
| S6 | `M3DiffApply` (identity diff onto DefaultListModel / DefaultTableModel / DefaultTreeModel) | planned |
| S7 | sort and filter binding of the sort-filter design (multi-key permutation index, bitmask filters) | planned |

Examples: `examples/` has the JTable, JList and JTree demos of the shared scenario (SWT twin snippets in
`org.eclipse.swt.snippets.m3`).
