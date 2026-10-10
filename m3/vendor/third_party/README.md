# m3/vendor/third_party

Unchanged published sources of the third-party artifacts the M3jdk21 build-time tooling pins, vendored so the JDK
repository carries the code itself ("jdk cannot have downstream dependency"). Each module keeps its upstream license,
PROVENANCE.json and notices; the Maven descriptors are M3jdk21 composition (Apache-2.0). Coordinates:
`<group>:<artifact>:<version>-m3-source-1`. Build all: `mvn -B -ntp -f m3/vendor/third_party/pom.xml install`.
Images that cannot compile from their sources jar live in `m3/vendor/third_party-images/`. Ledger and verifier:
`.m3/task-crates/dependency-source-inline-20261010/`. Nothing here enters src/**.
