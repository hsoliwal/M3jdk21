# M3JDK stage-2 OpenRewrite gate

This module packages the reusable OpenRewrite recipe
`com.m3.openrewrite.M3SegmentedStringProjectionBoundaries`.

The recipe captures the mechanical projection-boundary rule: segmented
`java.lang.String` storage is canonical, while contiguous arrays are created
only at compatibility boundaries such as legacy String helpers and JNI.

## Verification

From the repository root:

```bash
python3 m3/runtime-stage2/rewrite/verify_source_pins.py
mvn -f m3/runtime-stage2/rewrite/pom.xml test
```

The source-pin gate accepts only the exact pre-stage-2 or exact current
stage-2 Git blob set and rejects mixed/drifted source. The RewriteTest suite
checks transformation output and idempotence.

The OpenRewrite recipe is intentionally a reusable mechanical hardening pass;
it is not presented as a substitute for the HotSpot/C1/C2/JNI semantic tests.
