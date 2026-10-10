# dependency-source-inline-20261010 (M3jdk21 receiver)

User directives (2026-10-10): "pull all thriepart code and inline ands refactor it", then "inline and refactor all
third party code ... jdk cannot have downstream dependency".

The Synexia crate (`.m3/task-crates/dependency-source-inline-20261010` in hsoliwal/com.synexia, PR hsoliwal/com.synexia#10193)
is canonical: inventories, locks, donor-superset receipts, FOSS ledger, the CPU atomize/patternize projection.
This receiver vendors the third-party artifacts the M3jdk21 tooling pins directly into `m3/vendor/third_party/`
(9 source-built modules, `<version>-m3-source-1`, standalone Maven gate 9/9) and
`m3/vendor/third_party-images/` (5 byte-for-byte images that cannot compile without their platform:
OpenRewrite needs Lombok at source level plus a dependency tree that is not yet inlined).

`DOWNSTREAM_DEPENDENCY_LEDGER.tsv` lists every remaining third-party dependency of the tooling poms with its vendored
status and removal path (52 rows; 34 still resolve a published binary,
0 not yet vendored). Nothing here enters `src/**` (GPLv2+CPE); the JDK runtime has no
third-party dependency. The remaining downstream dependency of the *tooling* is the OpenRewrite 8.17.1 engine and its
tree; its source inline is the next leaf.

Verify: `python -I .m3/task-crates/dependency-source-inline-20261010/verify_crate.py`; build: `mvn -B -ntp -f m3/vendor/third_party/pom.xml install` (aggregator).
