<!-- SPDX-License-Identifier: Apache-2.0 -->
# Residency engine package rename (SWT #122 to m3/ports/swing-viewport)

This recipe moves the toolkit-neutral residency engine from SWT fork PR hsoliwal/eclipse.platform.swt#122
into the Apache module `m3/ports/swing-viewport`. It is hash-pinned: no file is copied by hand.

**Source.** hsoliwal/eclipse.platform.swt, branch `m3/item-residency-20261013`, commit
`136df9c62ba5555bfb33e7906f2760dc171ac9db`. The rename covers 21 files under
`bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt/internal/`:

- 12 files in `residency/`;
- 9 files in `swap/`;
- `PackedHeapGrowth.java`.

**Target packages.**

| From | To |
|---|---|
| `org.eclipse.swt.internal.residency` | `com.m3.swing.viewport.engine.residency` |
| `org.eclipse.swt.internal.swap` | `com.m3.swing.viewport.engine.swap` |
| `org.eclipse.swt.internal.PackedHeapGrowth` | `com.m3.swing.viewport.engine.PackedHeapGrowth` |

After the rename no `org.eclipse.swt` reference remains in any file; see the `residual` column.

**Transform.** Five ordered literal replacements over UTF-8 text, listed in `RULES` of both tools.

**Licences.** Headers are kept unchanged:

- the engine files are `EPL-2.0 OR Apache-2.0`, and the Apache module takes the Apache-2.0 option;
- the ported Synexia swap files stay Apache-2.0.

Copies into `java.desktop` internals are an explicit relicence, recorded in
[`m3/docs/m3-item-residency-explicit-relicense.md`](../../docs/m3-item-residency-explicit-relicense.md).

| File | Role |
|---|---|
| `RENAME_MANIFEST.tsv` (sha256 `1a5abf31dc07743f97a10af934982b1f03c938d1ef51a97f01e57277a81f1a57`) | source path and SHA-256, target path and package, expected post SHA-256, licence |
| `gen_rename_manifest.py` | CPU sealer: git blobs, then the transform, then hashes (`python -I gen_rename_manifest.py SWT_REPO COMMIT OUT`) |
| `ResidencyRename.java` | applier (`java ResidencyRename.java RENAME_MANIFEST.tsv SWT_WORKTREE M3JDK21_ROOT`) |

**Applier behaviour.**

- It validates every row before writing anything.
- A target already at its postimage is left alone, so a second run changes nothing.
- An existing target that differs is drift: exit 3.
- A source that does not match its pinned hash exits 4.
- A transform that does not reproduce the expected bytes exits 5.

**Run status.**

- The sealer ran (CPU).
- The Java applier has not been run yet; it is held by the test pause (`TESTS_TO_RUN.md` #8 on SWT #122).
- The Swing/AWT agent applies the rename into `m3/ports/swing-viewport` and owns that module.
