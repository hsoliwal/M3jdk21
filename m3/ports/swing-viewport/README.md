<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/ports/swing-viewport: `com.m3.swing.viewport`

Packed Swing models that follow the M3 viewport laws on a stock JDK 21:

- `TreeModel`, `TableModel`, `ListModel` and `TreeSelectionModel` implementations;
- state in engine bit lanes and patterns, data in columnar primitive arrays;
- every node or row object handed out is a proxy `(model, key, generation)`.

Design: [`m3/docs/m3-swing-awt-viewport.md`](../../docs/m3-swing-awt-viewport.md). The residency engine under
`engine/` comes from the hash-pinned rename recipe of M3jdk21 #826; see `THIRD_PARTY_NOTICES.md`.

```
bash m3/ports/swing-viewport/run-tests.sh            # javac + vendored JUnit Platform/Jupiter + M3JUnitLauncher, headless
mvn -B -ntp -f m3/ports/swing-viewport/pom.xml test  # Maven, M3 JUnit coordinates
```

Self-contained build: yes. `bash m3/ports/swing-viewport/run-tests.sh` (or `run-tests.sh test --select-package
com.m3.swing.web`) reads only files of this repository and the JDK (`M3_JDK21`): JUnit Platform 1.11.4, Jupiter 5.11.4,
opentest4j and apiguardian are compiled once from `m3/vendor/third_party` sources (classpath mode, `--release 17`) into
`$OUT.junit`, and `com.m3.swing.testkit.M3JUnitLauncher` (in `src/test/java`) runs them; no `~/.m2`, SWT or Nebula
artifact is used. The Maven line above is the optional reactor route.

The inventory of Swing/AWT sites is in `inventory/` (`PATTERNS.tsv`, `inventory.sh`, `INVENTORY.tsv`).
What has run, and what has not, is in `TESTS_TO_RUN.md`.
