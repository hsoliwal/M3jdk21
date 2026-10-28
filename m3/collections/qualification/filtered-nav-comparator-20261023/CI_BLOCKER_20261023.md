# Original Java21 test seed grammar repair

The first actual M3JDK21 [source-owning workflow run](https://github.com/hsoliwal/M3jdk21/actions/runs/39895556649) built source-vendored Jupiter/Launcher and the 20-source m3-arrays dependency. It compiled **291 actual m3-collections production sources** successfully; then failed while compiling 372 test sources at `M3LongTreeSetMaskCompactionContract.java:12` with `integer number too large`.

This is the original `M3LongTreeSet` test seed, not the new filtered navigation code. The single character `L` suffix makes its hexadecimal spelling legal as a Java long constant; it does not alter the intended numerical seed. Local Java21 red/green:
```
RED: javac --release 21 M3LongSeedRed.java
     integer number too large; exit 1
GREEN: javac --release 21 -Xlint:all -Werror M3LongSeedGreen.java
       4d33534f52544c; exit 0
```
Canonical source-sealed producer [Synexia #13198](https://github.com/hsoliwal/com.synexia/pull/13198) uses `M3HashPinnedTextSnapshotRecipe` with one exact path + named crate + old/new SHA allowance because the preimage is invalid Java. The original Java parser remains mandatory for other Java sources; no general Java text recipe bypass.

No unrelated product production sources, public API, native ABI or array/index/mask owners are changed by this additional compilation repair. The actual owning JUnit and full M3JDK21 image remain separate gates. **Do not merge until exact-head CI success is inspected.**
