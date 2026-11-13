# Preserve Spatial4j optional contracts in the full current-fork pack

This design precedes the receiver changes. Read-only original JDK 25 `jdeps`
inspection found actual Spatial4j 0.8 references missing from candidate 01. Keep
all original classes and use real input JARs for descriptor generation. The
original POM's three optional suppliers map to four directly referenced modules:
Noggit, JTS, Jackson core and Jackson databind. Original public/protected
signatures expose each supplier, corroborating `requires static transitive`.
Jackson annotations remains the actual databind descriptor's dependency.

## Receiving dependency selection

| Supplier | Original Spatial4j declaration | Selected receiving artifact | Authority and limit |
| --- | --- | --- | --- |
| Noggit | 0.8, optional | 0.8 | Exact original POM/version; official artifact checksum checked. |
| JTS | 1.17.0, optional | 1.20.0 | The pinned Lucene Gradle source and lock explicitly select 1.20.0 for its spatial tests. |
| Jackson databind | 2.9.10.5, optional | 2.22.3 | Explicit receiving 2.x maintenance selection; all 25 original Spatial4j Jackson member references resolve in the actual JARs. Finite JSON runtime probes remain required. |
| Jackson core | Transitive databind supplier | 2.22.3 | Actual named databind descriptor requires this module transitively. |
| Jackson annotations | Transitive databind supplier | 2.22 | Actual named databind descriptor requires this module transitively. |

The user's Jackson core/databind forks expose a 3.x default and no inspected 2.x
or 2.22 branch. Their package family is not substituted for this original 2.x
contract. This selection does not claim that the supplied Jackson/JTS binaries
were built from those forks or passed their full upstream suites.

## Receiver changes

Add optional lock field `spatial4j_pom` containing one absolute regular POM path
and its exact SHA-256. The input seal includes it whenever supplied; the full
Spatial4j owner requires it. Verify original GAV 0.8 and all three declared
optional dependency coordinates/versions. Require the four actual module owners
to be available to `jdeps`. After actual descriptor generation, change precisely
the four observed `requires transitive` clauses to `requires static transitive`.
Any missing or unexpected clause refuses the candidate; do not drop a clause.
Write original/received dependency facts into generated descriptor evidence.

The complete bundle now supplies five additional JMODs and the validation image
includes them. Each application still chooses its resolved roots. A named core
fixture requires only the original Lucene spatial/core and Spatial4j modules and
asserts that all five optional suppliers are absent from its boot layer. It then
checks real WKT coordinates and positive/negative Lucene spatial queries. A
separate feature fixture explicitly resolves Noggit/JTS/Jackson and checks
original GeoJSON, JTS containment and Jackson shape round-trip behavior.

Compile both exact fixtures in COMPILE and run them in TEST and RUNTIME. Seal
their outputs alongside the other compiled probes. Preserve the complete
original POM, JAR identities, rights, class/resource payloads and every failed
candidate. This changes no original algorithm, native M3String owner, M3 compiler
owner, or receiver phase. Target changes are materialized only through the
qualified existing canonical recipes and receive a fresh ordered candidate.
