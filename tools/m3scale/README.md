# M3JDK21 — exhaustive tracked-file M3Scale entrance

**M3JDK21 is the product/runtime owner**, not M3 wrapped around another JDK.
The first-party canonical Java 21 all-file controller comes unchanged from
[hsoliwal/synexia-m3scale-apache2#214](https://github.com/hsoliwal/synexia-m3scale-apache2/pull/214),
exact Git blob `5f3cb6a13cb9bfced77be4b47e103196d7d263fc`. This is a standalone
developer tool with **no runtime dependency** on Synexia or vendor donors.

The controller scans **every Git-tracked source, test, resource, documentation
and binary file** in this product checkout, checks SHA256 identity and emits
a searchable Java problem-category ledger. It distinguishes symlinks and
submodules; submodule content needs a separate pass.

```sh
javac --release 21 -Xlint:all -Werror tools/m3scale/M3ForkFilePass.java
java -cp tools/m3scale M3ForkFilePass scan . /tmp/m3jdk-scale
java -cp tools/m3scale M3ForkFilePass verify . /tmp/m3jdk-scale
```

This first-stage all-file scan **does not rewrite or rename** existing
`java.base`, HotSpot, `M3 String`, M3 Collections, AST, JNI, or other
JDK sources. Those changes are admitted separately through the original
canonical Maven/OpenRewrite/JDT recipe owners with compiler and JDK
tests, plus binary/JNI compatibility evidence where applicable.

Ordered pass: Git/history inventory → dated additive reconciliation →
donor/category catalogue → Inline → Atomize → Patternize → Comment →
Java21/jtreg/JNI and contract gates → PR. Physical storage for M3 Collections
remains dynamic arrays, logical-to-physical indices, offsets, masks and
generation-state flags. No per-entry replacement hierarchy is introduced.

Separate donor workspaces:
- `hsoliwal/guava` PR #3 (all-file inventory);
- `hsoliwal/commons-collections` PR #3 (inventory) and #4 (source-sealed
  dynamic-array bulk-capture refinement);
- `hsoliwal/eclipse-collections` PR #3 (inventory and phase separation).

**Not a claim of all-files semantic conversion.** This executable phase
creates a complete inventory and refuses drift. Promotion must observe
actual source-generated modifications and passing source/ABI/runtime gates.
