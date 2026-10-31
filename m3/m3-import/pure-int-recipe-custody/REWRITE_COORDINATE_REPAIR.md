# Source-built OpenRewrite coordinate recovery

This is a focused continuation of the M3JDK21 bulk executor/TornadoVM receiver
gates. The source-built vendor POMs publish `rewrite-core`, `rewrite-java`, and
`rewrite-java-21` at `8.17.1-m3-source-1`; the bounded Synexia custody POM
requested `8.17.1`. The latter is a different Maven coordinate.

The reusable OpenRewrite recipe `com.m3.rewrite.custody.AlignSourceBuiltRewrite`
owns the one-property change. The coordinate checker verifies the three actual
vendor module POMs and all three declared custody dependencies. The normal
Maven build and JUnit/fixed-point gates remain mandatory and are not implied
by this static check.

Reproduce the static gate:

```sh
python3 -S m3/synexia-import/pure-int-recipe-custody/verify-source-rewrite-coordinate.py
```

No runtime, String, HotSpot, public Java API or Tornado backend source is modified.
The RV32IM installation failure remains an independent open gate.
