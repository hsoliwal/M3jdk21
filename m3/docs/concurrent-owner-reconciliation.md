# Concurrent repository changes and owner reconciliation

The initial target pin was `8bb6215372e07712f1fdf5a0cb912af495007b19`. A fresh
2026-10-02 check found master at `4a81f3b3050fa5572ec1ff9368e2c383231db2e6`,
24 commits ahead. The new master includes a naming-manifest schema and validator,
prefix-Z port, mapped kernel, sealed tooling and partial migration evidence.

The public naming authority remains `m3/docs/name-mapping.json` and its existing
schema-1 `migration.records` extension. The initial local schema-2 projection is
translated into that authority before publication. Incoming records and gates
must remain visible. The selected coverage check wraps the existing migration
validator; it does not replace its lineage or receipt rules.

[PR #19](https://github.com/hsoliwal/M3jdk21/pull/19), inspected at
`584932a5f631bf8eb40b9525d18184085be1a404`, independently ports six of the same
Frozen/Joined owners into `m3/ports/indexstring`, adds an `M3Text` facade, and has
source-recipe/native-boundary/lineage evidence. This branch additionally ports
the bounded byte interner and adds owner-local UTF16 admission facts and a typed
compiler admission recipe. Its source pin and adaptation hashes differ.

Master advanced to `45f546ff5bcb06a1b2604f14baf998785d98d9a1` and merged
PRs #19 and #20 during this verification. The current integration preserves their
files and historical receipts. Code commit
`ef33a8cceff4848551f32ec302f8940ec6609591` consolidates the seven physical owners
in the established `m3/ports/indexstring/src/main/java` module and removes the
duplicate text-module copies. It retains both facades and the additive concat,
copy, uncached compaction and UTF16-fact contracts. The original surface corpus
passes 502,762 checks with an identical baseline/candidate digest; M3Text passes
1,014,911 checks in both modes and M3String passes 64,450 checks in four modes.
The baseline recipe proves upgrade, refusal, idempotence and reverse replay; the
v2 installer and original-source donor replay emit the same single owner closure.
Public descriptor comparisons and unchanged baseline test bytecode also pass
against the candidate. `shared-owner-code-binding.json` records exact code and
receipt hashes. Each earlier receipt remains bound to its original code; scoped
consolidation does not prove complete family, compiler, native or String parity.

[PR #20](https://github.com/hsoliwal/M3jdk21/pull/20) records a history audit
which distinguishes preserved ancestry from preserved runtime contents. Its
historical build qualification agrees with this branch's baseline investigation.

The separate runtime restoration candidate is
`69667e3f1ce673aaded2ad93e4ae2ecf6c59668b`, tree
`406f4bd973d40c5c8ce1782e01745307aab008ff`. It has a matched incremental complete
fastdebug image build and scoped acceptance receipts. Incoming master leaves
the restored String/HotSpot production paths unchanged. The exact image passes
86 selected default-mode upstream tests and 16 selected enabled tests. The two
original 4 GiB StringJoiner tests remain pending on this candidate, and their
historical donor failures remain recorded. Full jtreg/JCK, enabled JIT storage
and remaining VM gates remain separate obligations.

[Draft #24](https://github.com/hsoliwal/M3jdk21/pull/24) contains this selected
text/compiler candidate; [draft #23](https://github.com/hsoliwal/M3jdk21/pull/23)
contains the separately tested runtime restoration. Publication leaves the
continual optimization goal active. The exact combined master/runtime source at
`06c2ee74dfd6d243e305e4c12e67f44862b3904e` failed the warning-as-error compilation
gate on the inherited AutoCloseable close contract. The narrow interface fix is
frozen at `ebdafc4876717eb7e43e21827b260e50f63b6a00`; its matched image rebuild
and acceptance remain pending. The previous image remains bound to 696.
