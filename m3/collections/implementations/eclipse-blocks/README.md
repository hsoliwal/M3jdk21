# Eclipse Collections source-closed callbacks — standalone M3JDK21 module

This is a **bounded real Eclipse API slice**, not an Eclipse Collections Map/MutableList/Bag/Multimap replacement. Original Eclipse Collections **11.1.0** public functional APIs `Function`, `Function2`, `Predicate`, `Predicate2` and `Procedure` are inlined byte-for-byte, under their original Java package names and original Eclipse EPL-v1.0/EDL-v1.0 file copyright/NOTICE. First-party `M3EclipseFunctionalAdapters` delegates callback invocations to JDK21 `java.util.function` method references, without retaining a duplicate M3 payload, entry, node or index store.

Runtime dependency closure: **`java.base` only**. JUnit is TEST scope. No external Eclipse API or implementation JAR is needed by consumers of these five provider interfaces. M3 array/index/mask collection owners remain the authoritative data structures; this module supplies only functional contracts so those owners need not import an Eclipse implementation superclass or helper when callbacks cross the API.

The canonical exact-source recipe and fixed-point/refusal tests are stored solely in Synexia as `com.synexia.m3.InlineEclipseFunctionalApi20261027`. The product holds actual Java/test postimages, original dual licenses, and a data-only recipe receipt. Tested by strict Java21 compiler, Maven/JUnit, `jdeps` and dependency tree at exact PR head.

This cannot co-load incompatible duplicates of the five FQCNs from `eclipse-collections-api.jar` without a JPMS/classloader policy. Other Eclipse API hierarchies are not yet source-closed. The whole five-provider SDK remains incomplete.
