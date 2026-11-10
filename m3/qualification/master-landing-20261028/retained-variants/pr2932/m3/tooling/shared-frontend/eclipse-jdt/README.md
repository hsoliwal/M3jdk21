# Real Eclipse JDT qualification for M3JDK21

This module executes the existing Synexia selective-AST implementation against
actual Eclipse JDT 3.47.0 and Eclipse Text 3.14.700. It also feeds projections for
all six parser scopes into the existing `M3ASTLeafIndex`: atom handles and state
words remain primitive lanes, and source arrays cannot mutate retained facts.
No parser substitute or successful test skip is allowed.

Use a local checkout of `hsoliwal/com.synexia` at
`08e84ed4430894e07dea272758401fd7202d2187`, and the receiving M3JDK21 checkout.
The 36 source files in `sources.json` must match their SHA-256 seals. The build
copies their exact bytes only into its build directory. No donor runtime is
installed in the JDK image by this qualification module.

With Java 21, Python 3 and a populated Maven repository:

```sh
mvn -o -f m3/tooling/shared-frontend/eclipse-jdt/pom.xml \
  -Dsynexia.root=/absolute/path/to/com.synexia test
```

`target/SOURCE_RECEIPT.json` identifies the original source closure;
`target/surefire-reports` contains the six JUnit outcomes. The original donor
suites cover UTF-8 BOM/UTF-16 boundaries, fragment isolation, whole-file/focal/
declaration modes, bindings, recovery, cancellation, limits, cache custody,
rewrite normalization and an actual Java compiler gate. The additional product
fixture checks primitive M3AST facts for every scope and stale-source refusal.

This establishes finite standalone JDT parsing and projection evidence. It does
not qualify Eclipse workspace reconciliation, OSGi deployment, the separate
MIndex master-cache bridge, every Java language release, or a full source-built
M3JDK21 image. The existing `com.sun.source.util.M3AST` interface still exposes
javac-owned trees. No claim of universal parser support follows from this suite.

The original sources retain their Apache-2.0 notices. Eclipse JDT/Text and their
dependencies retain their own licenses; they are Maven test-build dependencies,
not copied into this repository. Dependency identities and binary hashes for the
recorded run are in `qualification.json`.

Canonical transformation custody remains in Synexia's existing
`m3-collections-contract-refinement-20261017` crate, dataset
`jdt-contracts-20261019`. The crate name identifies its reusable owner; this task
does not create another transformation engine or change any parser public API.
