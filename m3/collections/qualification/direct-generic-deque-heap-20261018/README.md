# Native M3 generic ArrayDeque and PriorityQueue

Canonical source: Synexia pull request 12258 at commit 48dbdfc6bbe5a140f72f7dfe43a8f1a8e52689d9. Product: hsoliwal/M3jdk21. These are actual M3-owned Java21 collection implementations, not interop wrappers over external vendors.

M3ArrayDeque<E> implements JDK Deque directly over a GC-traced Object[] and aligned byte[] flags, circular logical-to-physical offsets and a checked growable capacity. M3PriorityQueue<E> implements JDK Queue directly over a primitive-indexed Object[] heap, byte flags and long[] occurrence IDs used for correct duplicate iterator removal. Both retain their own contracts, exception safety, clone/serialization and primitive state export. No per-element Entry or Node is stored.

The existing M3IndexMaskCompactor does optional native-required primitive index selection; no Java objects, comparisons, predicates or pointers cross JNI and no native symbol is introduced.

Recipe ownership is Synexia M3HashPinnedJavaSnapshotRecipe, in Java AST mode, with a 4-target ABSENT-to-hash manifest. The first product commit installs exact templates, YAML, donor catalogue, source proof and CI. The second installs four exact generated postimages, byte-identical to the canonical donor. Future fixes must modify/execute the recipe, not hand-edit generated Java.

Donor reports strict Java21 and 51,972 randomized JDK comparison checks with temporary API-shaped stand-ins. The donor's GitHub workflow did not execute job steps and is not an actual Maven/receiver/native acceptance result. The new product focused workflow must execute actual Maven/OpenRewrite SDK replay and refusal, complete current M3 core javac, normal/interpreted differential, original C17 JNI UBSan and checked JNI run. Until those run on exact head, all product-wide JNI and full module compatibility remain HOLD.

This native M3 substrate is useful for the JDK, Guava, Eclipse, Spring and Apache Commons SDK program without pretending those five libraries' full public contracts are already implemented. No donor source, JDK source or challenge solutions are copied. Preserve upstream licences and Synexia/M3 Apache-2.0 first-party attribution. STRING -> arrays -> collections -> AST/compiler receiving remains WAITING_FOR_STRING.
