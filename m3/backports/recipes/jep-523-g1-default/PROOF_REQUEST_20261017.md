# JEP 523 G1/default fallback verification — 2026-10-17

Status: **source candidate present, proof requested; promotion NOT_AUTHORIZED**.

## Product source already in current M3JDK21
- Existing HotSpot implementation: `src/hotspot/share/gc/shared/gcConfig.cpp`
- Existing focused JDK jtreg: `test/hotspot/jtreg/gc/arguments/TestSelectDefaultGC.java`
- Source-based canonical receiver: `M3Jep523BackportRecipe`, with
  JEP 523 upstream implementation `86637704fd01493ddb57e455dbd1e35e4798237d`.
- The existing `CURRENT_TREE_RECEIPT.tsv` remains historical candidate
  evidence. Do not turn its open `PENDING_HOSTED_PROOF` gates into PASS
  before actual CI artifacts exist.

## This pass
The existing JEP 523 workflow was missing the repository's required pinned
source-built OpenRewrite/JUnit bootstrap. Use
`m3/synexia-import/pure-int-recipe-custody/install.sh` and the existing
migration-recipe reactor before the unchanged JEP 523 JUnit fixed-point proof.
No JVM/GC product/test source is modified by this proof repair.

The original workflow continues to build **two actual OpenJDK images**,
one G1 enabled and one without G1, and runs the focused GC jtreg plus
runtime checks for default and explicit collector selection.

Stop and report the first failed step; never mark a candidate accepted
because source files match or because only the recipe executes.
