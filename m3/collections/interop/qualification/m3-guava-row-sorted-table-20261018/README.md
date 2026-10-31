# M3JDK21: actual Guava RowSortedTable on indexed M3 array owners

Canonical Synexia convergence Maven/OpenRewrite crate for a **new** \`com.google.common.collect.RowSortedTable<R,C,V>\` implementation. The Guava public interface retains its proper name. Payloads reside in the existing M3TreeMap outer-row slots and M3LinkedHashMap or M3TreeMap inner-column slots. Guava \`Tables.newCustomTable\` provides cell/column/row API behavior without copying Guava source. \`SortedRowMap\` wraps the original M3 NavigableMap range index and preserves live sub/head/tail removal, but rejects direct map.put like Guava. Public Map.Entry facades are allocated only at API boundaries. No new M3 per-cell Entry/Node or native ABI.

Two constructors: comparator-sorted row keys, with either insertion-order M3 columns or independently comparator-sorted M3 columns. \`RowSortedTable\` does not promise sorted \`columnKeySet\` iteration, unlike Guava's concrete \`TreeBasedTable\`; compare that outcome as a set, not an ordering requirement.

Recipe-first: existing \`M3HashPinnedJavaSnapshotRecipe\` in Java LST mode; named \`com.synexia.m3.Jdk21GuavaRowSortedTable20261018\`. Three ABSENT-before/sha256-checked Java runtime/test outputs, plus actual recipe fixed-point/negative JUnit. Correct canonical recipe/templates and rerun rather than hand-patch generated product files. Synexia develop is never rebased and current JDK/Eclipse/Spring/Apache owners are unchanged.

Actually executed Linux Java21.0.12.1: strict \`javac --release 21 -proc:none -Xlint:all -Werror\` against previously packaged M3 core jar and **real Guava 33.4.8-jre** SHA256 \`f3d7f57f67fd622f4d468dfdd692b3a5e3909246c28017ac3263405f0fe617ed\`; PASS **153,557 assertions / 3,200 seeded operations** in both normal and -Xint. It covers row/cell mutations, sorted order, live rowMap removes, range bounds, original Guava equality, null-free data and unsupported map put. Not an installed JDK/current-master reactor or full Guava API proof.

Local Maven/OpenRewrite, full current target Maven/JUnit, JNI/UBSan, installed M3JDK21/jtreg/JCK/JMM and performance not run; exact-head hosted CI must qualify. The unchanged C17/JNI mask selector remains authoritative for primitive masks, never Java cell references or comparator callbacks.

First-party Apache-2.0 ©2026 Hitesh Soliwal and contributors. Original Guava binary Apache-2.0; vendor implementation code not copied. LeetCode/HackerRank/GeeksforGeeks are category evidence only. This extends concrete SDK coverage but does not complete all collection types/methods in the five ecosystems.
