# M3 String ROOT-ASCII cold-path repair

This packet owns one product transformation: for an already M3-backed String and
`Locale.ROOT`, prepare the fixed canonical fact bundle before deciding whether the ASCII-only
canonical case mapper applies.

Why this exists:

- the previous `factsIfPrepared()` check made the optimization depend on unrelated prior work;
- a cold M3 ASCII String therefore fell through to `String.value()` materialization;
- case conversion is already O(n), so preparing reusable fixed facts is appropriate;
- non-ASCII and non-ROOT locale semantics remain under the stock JDK case engine.

The executable patch is restricted to `src/java.base/share/classes/java/lang/String.java`.
The jtreg differential and source-invariant files are checksum-pinned as verification artifacts and
are checked before any product write.

Run:

`mvn -f m3/runtime-integration/fix-root-ascii-case/pom.xml verify`

Opt-in apply on an exact preimage:

`mvn -f m3/runtime-integration/fix-root-ascii-case/pom.xml -Papply verify`

The retained runtime patch engine refuses drift, mixed states, unsafe paths and symlinks.
