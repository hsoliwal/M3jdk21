# JEP 474 — ZGC: Generational Mode by Default

Status: capability-equivalence / compatibility-split proof. No product source mutation.

## Upstream authority

- JEP: 474
- issue: JDK-8326957
- implementation commit: `4ba74475d44831c1fe49359458163cd1567e9619`
- target: JDK 23
- Java21 oracle: `890adb6410dab4606a4f26a942aed02fb2f55387`

The implementation commit touches seven paths: four HotSpot product paths and three tests.

## Java21 current capability

Current M3JDK21/JDK21 source already defines:

`ZGenerational = false`

and ships the generational ZGC implementation behind the explicit flag.

Therefore JEP 474 does **not** represent a missing collector capability for M3JDK21. Its central
change is policy:

- make generational ZGC the default when ZGC is selected;
- deprecate explicit non-generational selection;
- emit deprecation/log wording.

## Compatibility split

M3JDK21 keeps the Java21 runtime contract sealed:

- `-XX:+UseZGC` continues to mean the Java21 default mode;
- `-XX:+UseZGC -XX:+ZGenerational` remains the explicit generational opt-in;
- `-XX:+UseZGC -XX:-ZGenerational` remains available;
- the default flip is not imported into stock M3JDK21 behavior.

The upstream deprecation warning/metadata/log wording is a separate opt-in diagnostics candidate,
not part of the capability equivalence proof.

## Proof target

The hosted proof must:

1. verify the exact seven-path upstream denominator;
2. verify current source still has `ZGenerational, false`;
3. configure/build the current M3JDK21 image using Java 21;
4. run `-XX:+UseZGC -XX:+ZGenerational -Xlog:gc+init` successfully;
5. run `-XX:+UseZGC -XX:-ZGenerational -Xlog:gc+init` successfully;
6. run default `-XX:+UseZGC -Xlog:gc+init` and prove it does not silently select the JEP474
   generational default;
7. retain no source delta;
8. only then classify the collector capability as `ALREADY_PRESENT_BY_EQUIVALENCE`.

This does not authorize the JEP474 default-policy change.
