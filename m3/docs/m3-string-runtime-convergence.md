# M3 String runtime convergence

**Ownership invariant:** Synexia is donor/provenance. M3JDK21 is the product and runtime owner.

This source-sealed packet replaces the stale VM-visible `java.lang.MIndexString` owner with
`java.lang.M3String`, moves the private `String` field and HotSpot field lookup from `mindex`
to `m3`, and updates Java, HotSpot and JNI together. The old source filenames remain only as
explicit package-only tombstones so the migration is reviewable and does not leave a second runtime
owner.

Fixed-size text facts are retained once on canonical M3 String storage: UTF-16/code-point geometry,
ASCII/LATIN1/surrogate flags, ASCII case hashes, prefix/suffix windows, and conservative unit,
bigram and trigram signals. Length-proportional analyses are not stored on every String; they remain
bounded side caches keyed by canonical identity.

Exact `indexOf`, `lastIndexOf`, `startsWith`, exact `regionMatches`, code-point counting,
deprecated byte-copy compatibility, and `toCharArray/getChars` paths can operate over canonical
storage without forcing a Compact-String compatibility projection.

The legacy `jdk.mindex.lexicon` property is retained as a launch compatibility input in this slice.
It is not the runtime owner name and does not authorize a Synexia dependency.
