<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 rewrites original Eclipse SWT internals — EventTable first

**The M3JDK21 product is one integrated Java 21 SDK/JDK. SWT is not a wrapper
calling a separate M3 application library.** Its own original `org.eclipse.swt.widgets.EventTable`
source is mechanically refined to use an M3 physical-slot/active-range
representation, while the SWT API and event semantics remain authoritative.

The actual compilable Eclipse production source belongs to
`hsoliwal/eclipse.platform.swt`, path
`bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/widgets/EventTable.java`.
The M3JDK21-owned Maven/OpenRewrite recipe under
`m3/tooling/shared-frontend/recipe-custody/datasets/swt-event-table-m3-20261021`
is the source-custody/replay authority, **not a second EventTable runtime**.
This is an additive new reviewable source-refinement slice after existing
JDT NodeList array adoption and SWT's indexed VirtualTree/VirtualItemStorage work.

## Source source/behavioral atoms

- **Original Eclipse owner:** `EventTable` already uses one parallel `int[] types`
  and `Listener[] listeners`. They remain the **only** event payload storage;
  original `SWT.None == 0` denotes tombstones/unused slots, and dispatch still
  defers compaction while nested.
- **New single primitive coordinate:** `int highWater` means one past the
  last nonzero event-type slot. It replaces the full-capacity backward
  `hook` scan and bounds ordinary `hooks`, `getListeners`, `size`,
  `unhook` and dispatch iterations to the admitted range, not array
  capacity. No HashMap, listener entry/node wrappers, per-event indices or
  JNI symbols are introduced.
- **Callback/reentrancy correctness:** listeners may hook/unhook themselves,
  append listeners, trigger nested events, alter `Event.type` or throw
  exceptions. The original `ExceptionStash` dispatch and deferred tombstone
  compaction remain the owners; newly hooked listeners stay visible in an
  ongoing dispatch, and removed tail slots can be reused.
- **Rare sentinel behavior:** original same-package `getListeners(0)`,
  `hooks(0)` and `unhook(0,...)` inspect all allocated zero-type slots,
  including empty ones. The M3 optimization keeps those exact exceptional
  code paths. Do not silently replace `SWT.None` with absence.
- **Source licensing:** original `EventTable` carries EPL-2.0; the direct SWT
  source keeps that header and upstream attribution, plus any additional
  Synexia copyright in new proof files. No LeetCode/HackerRank/GfG solution
  body was copied.

## Qualification

Original SWT source Git blob:
`c04fd7a51dd64350540f316e14e79dcaebd9d59d`.
M3-style postimage Git blob:
`efd31dc0d4acd1c088295e861b1d6b3a44313a43`.
Exact SHA-256 UTF-8 source pre/post:
`dd4c0c31385024cfc8d652a562a07a81aefea8e8b291f6e473e1d53d6d7403ce`
→ `82201e5f512e8ff2f9f0e8f2187b59535861d2c6a8b8a7b9a9f32da2e65ab10c`.

The named existing class-backed
`com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` is reused with
one serial file-local pass and exact preimage/postimage. The isolated proof in
the SWT source fork compiles **both actual Java sources** on JDK 21 by
renaming only the immutable original baseline class via `git cat-file`,
plus minimal *test-only* SWT/ExceptionStash stand-ins. It runs original
versus candidate randomized and reentrant callback scripts in default
and `-Xint` modes, comparing all event-type cells, listener identity
positions, query results and callback order.

Run the source recipe in the M3JDK21 checkout:

```sh
mvn -B -ntp -f m3/tooling/shared-frontend/recipe-custody/pom.xml \
  -Dm3.dataset=swt-event-table-m3-20261021 \
  "-Drecipe.owner.root=$PWD/m3/tooling/shared-frontend/recipe-custody/sdk-owner" \
  -Pcontract verify
```

Then check a **separate** SWT fork checkout:

```sh
python3 m3/tooling/shared-frontend/recipe-custody/receive.py \
  --dataset swt-event-table-m3-20261021 \
  --recipe-owner "$PWD/m3/tooling/shared-frontend/recipe-custody/sdk-owner" \
  --target /path/to/eclipse.platform.swt --check

cd /path/to/eclipse.platform.swt
bash docs/m3/eventtable-20261021/prove.sh
```

**Status DRAFT / HOLD:** these authored gates must actually execute
successfully. The test-only stand-ins do **not** prove native GTK/Win32/Cocoa
SWT event-loop equivalence or full Eclipse/Tycho/PDE, UI-thread/disposal,
accessibility, plugin lifecycle, UI latency/retained heap and complete
M3JDK21 JDK/HotSpot/JNI compatibility. Keep every public SWT method, event
ordering and handler exception contract until those original-owner tests pass.

## Successors, no duplicate Eclipse

Eclipse should progressively use M3 primitive, indexed and lazy shapes
**inside its actual modules**: SWT widget children, labels and native
viewport rows; JFace/Platform UI viewer state; JDT parser/binding facts;
Resources trees/markers/deltas; Equinox extension/bundle registries.
Every successive file starts with its original API/event/ABI facts, consults
the shared algorithm catalogue, performs a source-sealed recipe pass, and
adds a differential proof. Do not make Eclipse depend on an application
wrapper or build another resource/AST tree next to the original owner.
