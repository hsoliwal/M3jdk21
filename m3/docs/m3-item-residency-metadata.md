<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 item-residency reference metadata (compile-time half)

Status: draft, stacked on PR #814 (`m3/compiletime-string-precompute-20261013`); #814's files are not
modified. Runtime half: SWT fork PR hsoliwal/eclipse.platform.swt#122 (`ItemResidency`,
`docs/m3/M3_ITEM_RESIDENCY.md`). Governing invariant: Synexia M3-LAZY-OBJECT-PRESSURE-1 (#10889);
siblings #10891 and #10892.

## Question answered

"Half the time the user wants just the behaviour." For every call site that obtains a transient
item handle (an SWT `TableItem`, `TreeItem`, Nebula `GridItem`, Swing `TreePath` and so on), does the
reference escape, or is it used locally for behaviour only? Behaviour-only sites may receive a
recyclable flyweight handle; every other site keeps a pinned handle with stock identity.

## Annotations (`com.m3.residency`, CLASS retention, Apache-2.0)

| Annotation | Target | Meaning |
|---|---|---|
| `@M3Transient` | type | instances are parent-owned thin handles; obtaining sites are classified |
| `@M3Retains` | method, parameter | the callee stores its transient argument (for example `TableEditor.setEditor`); passing it is RETAINS |
| `@M3BehaviourOnly` | method, parameter | the callee uses the transient argument only during the call (for example `Table.indexOf`); passing it does not escape |

CLASS retention: visible to the ASM pass as invisible annotations, absent at run time. Libraries that
cannot be annotated (stock SWT, Nebula, Swing) are covered by a seed list in the pass with the same meaning.

## Pass

`M3ItemReferenceScan` reuses PR #814's jar and `jrt:/` walk order and its `SourceInterpreter` frame
budget (4,000,000 cells). Two passes over the class files:

1. Hierarchy: collect `super` links and `@M3Transient`; close the transient set over subclasses of the seeds.
2. Sites: for every method, every instruction that obtains a transient value (an `INVOKE*` returning `T` or
   `T[]`, or `NEW T` followed by its constructor) seeds forward def-use tracking over `SourceInterpreter`
   frames. Copies (`DUP*`, `CHECKCAST`, `ASTORE`/`ALOAD`, `AALOAD` of a tracked array) extend the tracked set.

Escape kinds recorded per site (bit mask):

| Bit | Kind | Trigger on a tracked value |
|---:|---|---|
| 0 | FIELD | `PUTFIELD`/`PUTSTATIC` value operand |
| 1 | ARRAY | `AASTORE` value operand |
| 2 | COLLECTION | argument of a `java/util` collection or map mutator |
| 3 | RETURN | `ARETURN` |
| 4 | LAMBDA | argument of `INVOKEDYNAMIC` other than string concatenation |
| 5 | IDENTITY_COMPARE | `IF_ACMPEQ`/`IF_ACMPNE` operand |
| 6 | IDENTITY_HASH | `hashCode()` on the handle or `System.identityHashCode` |
| 7 | MONITOR | `MONITORENTER` |
| 8 | ARG_UNKNOWN | argument of any other call (not receiver, not a behaviour-only callee) |
| 9 | ANALYSIS_FAILED | frame budget exceeded or analyzer exception |
| 10 | RETAINS_CALLEE | argument of an `@M3Retains` callee or a seeded retaining callee |

Class per site: `BEHAVIOUR_ONLY` (mask 0), `RETAINS` (any of bits 0-7 or 10), `UNKNOWN` (only bits 8-9).
The runtime pins for both RETAINS and UNKNOWN (pinned by default).

Behaviour-only callees (seed): receiver calls on the handle itself, `String.valueOf`, `StringBuilder.append`,
`Objects.equals`/`requireNonNull`, and on SWT owners `indexOf`, `showItem`, `setTopItem`, `select`, `deselect`,
`setSelection`, `setInsertMark`. Retaining callees (seed): `TableEditor.setEditor`, `TreeEditor.setEditor`,
`ControlEditor` subclasses, `Widget.setData` with a handle value.

## Sidecar

- TSV `ITEM_REFERENCE_SITES.tsv`: `corpus, unit, class, method, desc, bci, obtain, kind, mask, site_key`.
- Binary `M3IRS01`, big-endian ints: `0x4D334952`, `1`, `count`, then sorted `(site_key, kind_word)` pairs,
  `kind_word = mask << 2 | class`. Key: FNV-1a 32 over the UTF-16 units of `owner.name+desc@bci`, the same
  function as `ItemResidency.siteKey`. Colliding keys merge conservatively (any non-behaviour site wins).

## Run and counts

Filled from executed runs only; see the PR body.
