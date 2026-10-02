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

[Draft PR #19](https://github.com/hsoliwal/M3jdk21/pull/19), inspected at
`584932a5f631bf8eb40b9525d18184085be1a404`, independently ports six of the same
Frozen/Joined owners into `m3/ports/indexstring`, adds an `M3Text` facade, and has
source-recipe/native-boundary/lineage evidence. This branch additionally ports
the bounded byte interner and adds owner-local UTF16 admission facts and a typed
compiler admission recipe. Its source pin and adaptation hashes differ.

These are overlapping draft implementations, not two admitted storage owners.
They must not be put on one classpath with duplicate `com.synexia.indexstring`
classes. The next consolidation must compare their pinned source baselines and
target deltas, select one physical Frozen/Joined closure, retain both public API
contracts through adapters where needed, and execute both differential corpora
plus replay/rollback tests. No blind overwrite or prefix rename is authorized by
this overlap. Each independent candidate's receipt remains bound to its own code.

[Draft PR #20](https://github.com/hsoliwal/M3jdk21/pull/20) records a history audit
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
