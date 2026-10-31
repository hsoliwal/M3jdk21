<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/ports/swing-viewport: `com.m3.swing.viewport`

Packed Swing models that follow the M3 viewport laws on a stock JDK 21:

- `TreeModel`, `TableModel`, `ListModel` and `TreeSelectionModel` implementations;
- state in engine bit lanes and patterns, data in columnar primitive arrays;
- every node or row object handed out is a proxy `(model, key, generation)`.

Design: [`m3/docs/m3-swing-awt-viewport.md`](../../docs/m3-swing-awt-viewport.md). The residency engine under
`engine/` comes from the hash-pinned rename recipe of M3jdk21 #826; see `THIRD_PARTY_NOTICES.md`.

```
bash m3/ports/swing-viewport/run-tests.sh            # javac + JUnit console, headless
mvn -B -ntp -f m3/ports/swing-viewport/pom.xml test  # Maven, M3 JUnit coordinates
```

The inventory of Swing/AWT sites is in `inventory/` (`PATTERNS.tsv`, `inventory.sh`, `INVENTORY.tsv`).
What has run, and what has not, is in `TESTS_TO_RUN.md`.
