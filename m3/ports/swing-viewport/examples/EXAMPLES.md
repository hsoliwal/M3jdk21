# M3JDK Swing viewport examples

`M3SwingViewportDemo.java` is one file with three demos, each at 10^6 items:

| Mode | View | M3 paths it exercises |
|---|---|---|
| `tree-variable` | `JTree` of 1,001,001 nodes (root, 1,000 groups of 1,000 items), every group expanded, row height 0 (measured per row), a two-line HTML label every 97th node | leaf 1 (`M3VariableHeightLayoutCache`), leaf 5 (`M3ExpandedState`) |
| `tree-fixed` | the same tree with `setRowHeight(18)` and `setLargeModel(true)` | leaf 2 (`M3FixedHeightLayoutCache`), leaf 5 |
| `list` | `JList` of 10^6 rows, a two-line HTML row every 50th | leaf 3 (`M3ListRows`), leaf 4 (`M3ListMeasure`) |

**Overlay.** In the top-right corner of every view, the overlay shows:
- whether `-Dm3.swing.viewport` is on (green: the M3 paths) or off (red: the stock paths);
- the item count;
- the setup time (build the model, expand, first layout);
- p50 and p95 of the last 120 frame times.

**Switching the property.** It is read once when `java.desktop` loads, so it cannot change inside a running JVM.
- In the window, **Relaunch with -Dm3.swing.viewport=...** starts the same demo in a new JVM, with the same JVM
  options (`--patch-module` included) and the property flipped.
- `run_examples.sh` runs every demo both ways.

## Run

Interactive (needs a display), on JDK 21 with the patched `java.desktop`:

```
javac -d classes M3SwingViewportDemo.java
java --patch-module java.desktop=<patched classes> -Dm3.swing.viewport=true -cp classes M3SwingViewportDemo tree-variable
```

The window also has **Auto-scroll** (one step every 16 ms) and **Jump to the middle**.

Headless, every demo with the property off and on:

```
JAVA_HOME=<jdk21> PATCH=<patched classes> OUT=<dir> bash run_examples.sh
```

With `--capture <dir>`, each run:
1. builds the view;
2. scrolls 60 frames of 800x600 offscreen from the middle of the view (`setViewPosition` + `paint`, after 10 warm-up frames);
3. writes `<mode>-<off|on>.png` with the overlay;
4. appends a row to `examples.tsv`.

Messages go through `System.Logger`, not System.out.

## Results (2026-10-19, JDK 21.0.9, headless, `-XX:+UseSerialGC -Xmx1500m`, shared machine)

Screenshots and `examples.tsv` are in `S:\m3-scratch\swing\examples\` (`tree-variable-off.png`, `tree-variable-on.png`,
`tree-fixed-off.png`, `tree-fixed-on.png`, `list-off.png`, `list-on.png`).

| Demo | Setup, stock | Setup, M3 | Frame p50 / p95, stock | Frame p50 / p95, M3 |
|---|---:|---:|---:|---:|
| `tree-variable` (1,001,001 nodes) | 5,553 ms | 4,692 ms | 1.79 / 4.18 ms | 1.25 / 3.67 ms |
| `tree-fixed` (1,001,001 nodes) | 4,459 ms | 593 ms | 11.60 / 27.86 ms | 1.70 / 10.97 ms |
| `list` (10^6 rows) | 7,301 ms | 7,286 ms | 2.25 / 4.14 ms | 1.78 / 11.19 ms |

How to read the table:
- **Setup** includes building the model, which is the same in both columns.
- **The list's first layout measures every row either way.** Leaf 4 speeds up the layouts after model edits, not the
  first one. The edit numbers are in `m3/docs/swing-viewport/bench/LIST_MEASURE_20261019.tsv`.
- **The p95 values are single samples of 60 frames on a shared machine** and move from run to run. The benches under
  `m3/docs/swing-viewport/bench/` repeat the measurements.
