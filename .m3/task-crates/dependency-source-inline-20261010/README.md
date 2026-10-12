# dependency-source-inline-20261010 (M3jdk21 receiver)

User directives (2026-10-10/11): "pull all thriepart code and inline ands refactor it", "inline and refactor all
third party code ... jdk cannot have downstream dependency", "commit to github using pr".

The Synexia crate (`.m3/task-crates/dependency-source-inline-20261010` in hsoliwal/com.synexia, PR hsoliwal/com.synexia#10339)
is canonical: inventories, locks, donor-superset receipts, FOSS ledger, the CPU atomize/patternize projection.
This receiver vendors every third-party artifact the M3jdk21 build-time tooling resolves, including the complete
OpenRewrite 8.17.1 runtime closure, into `m3/vendor/third_party/` (120 modules, `<version>-m3-source-1`,
standalone Maven gate 120/120) and `m3/vendor/third_party-images/` (11 images).

Refactor applied: the OpenRewrite modules are delomboked (269 files are the Lombok 1.18.30 expansion
with the upstream lombok.config; each carries a notice line; the unchanged upstream files stay under upstream-lombok/).
Every module is proven against the published binary jar by `API_SURFACE.tsv` (ASM: classes, members, signatures,
constants, annotations, plus resources by SHA-256): {"EQUIVALENT_MODULO_COMPILER": 57, "IDENTICAL": 63}. Resources the
published sources jars omit (SPI registrations, jar-level legal files, build-filtered properties, native libraries:
128 files) are completed byte-for-byte from the published jar and pinned in PROVENANCE.json.

`DOWNSTREAM_DEPENDENCY_LEDGER.tsv` lists every remaining downstream dependency of the tooling poms
(304 rows: 0 still resolve a published binary,
0 not vendored, 129 Maven build plugins). Nothing here enters
`src/**` (GPLv2+CPE); the JDK runtime itself has no third-party dependency.

Shaded jar (maven-shade-plugin): `m3/vendor/third_party/m3-tooling-shaded` merges the vendored OpenRewrite stack into one
artifact, `com.m3.vendor:m3-tooling-third-party:8.17.1-m3-shaded-1`, built only from in-repo source modules. It merges
services and legal texts, strips jar signatures, and applies no relocation (OpenRewrite resolves recipes and Jackson
type ids by class name). `SHADE_CLOSURE.tsv` proves every merged artifact is a vendored source module.

Verify: `python -I .m3/task-crates/dependency-source-inline-20261010/verify_crate.py`; build: `mvn -B -ntp -f m3/vendor/third_party/pom.xml install` (aggregator).
