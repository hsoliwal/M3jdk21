# M3JDK21 zero-key off-lane primitive storage — public receiver

Canonical algorithm/recipe owner: hsoliwal/com.synexia,
`com.synexia.rewrite.M3Jdk21ZeroSlotGrowth20261017` (pinned donor PR in RECEIVER.tsv).
This is a target-owned refinement of three existing Apache-2.0 primitive Java types,
not a Synexia runtime dependency or java.util/JDK backend replacement.

Only `M3IntHashSet`, `M3IntIntHashMap`, `M3IntLongHashMap` are changed. Their public JVM
descriptors and value/ordering/iterator contracts are unchanged; diagnostics capacity()
and payloadBytes() intentionally reflect fewer unnecessary allocations for off-lane zero.
No retained Entry/Node objects, boxed keys, new reference-array store or JNI method.

Run the selected-source product admission on a Java21 checkout:

```
python3 m3/collections/verification/zero-slot-growth-20261017/verify.py --root .
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3ZeroSlotCapacityTest test
```

The first command source-hash checks the three exact postimages, compiles those sources
with the four existing target-owned interfaces, asserts public descriptors match the
preimage reference tapes, and executes a JDK HashMap/HashSet differential plus capacity
checks. It never mutates source or the full module. The second command is an owning
Maven/JUnit gate and is not covered by the first's PASS. Full JDK matched-image,
jtreg, JNI/GC/platform/throughput evidence and independent review remain NOT_RUN.

The target keeps the existing M3JDK21 String→arrays→collections receiving order.
Do not claim completion of all collection families or transfer canonical algorithm
ownership away from Synexia on merge.
