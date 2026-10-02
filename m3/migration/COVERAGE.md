# Generated migration coverage

Generated from `m3/docs/name-mapping.json`. **Partial inventory; no global completeness claim.**

26 mapping entries: 8 implemented/tested production mappings, 1 unverified CI mapping, 13 pending capabilities and 4 retained legacy entries.

| Mapping | State | Disposition / boundary |
|---|---|---|
| legacy.prototype.1 | separate provisional byte-format prototype; NOT an API-compatible replacement | retain established Synexia owner; adapter/migration deferred |
| legacy.prototype.2 | provisional semantic direction only | provisional semantic direction only |
| legacy.prototype.3 | requires per-type source/API/ownership mapping | requires per-type source/API/ownership mapping |
| legacy.prototype.4 | requires per-type source/API/ownership mapping | requires per-type source/API/ownership mapping |
| text.FrozenBytes | implemented_tested | Existing public signatures preserved; added methods are additive. No serialization or native ABI is declared by these classes. Do not install this module alongside the original same-package classes. |
| text.FrozenChars | implemented_tested | Existing public signatures preserved; added methods are additive. No serialization or native ABI is declared by these classes. Do not install this module alongside the original same-package classes. |
| text.MIndexJoinedBytes | implemented_tested | Existing public signatures preserved; added methods are additive. No serialization or native ABI is declared by these classes. Do not install this module alongside the original same-package classes. |
| text.MIndexJoinedChars | implemented_tested | Existing public signatures preserved; added methods are additive. No serialization or native ABI is declared by these classes. Do not install this module alongside the original same-package classes. |
| text.MIndexJoinedStorageIntern | implemented_tested | Existing public signatures preserved; added methods are additive. No serialization or native ABI is declared by these classes. Do not install this module alongside the original same-package classes. |
| text.MIndexJoinedStreams | implemented_tested | Existing public signatures preserved; added methods are additive. No serialization or native ABI is declared by these classes. Do not install this module alongside the original same-package classes. |
| m3.M3Text | implemented_tested | Additive explicit API only; no implicit compiler or runtime substitution |
| m3.module-info | implemented_tested | Additive explicit API only; no implicit compiler or runtime substitution |
| structural.canonical | pending | Preserve root canonical owners; collections/TableTree/runtime are specialized projections. Only MIndexAtomStore code read; dependency closure not yet fetched. |
| bridge.runtime | pending | Reuse existing bridge; exact UTF-16 content conversion, never numeric-ID translation. README inspected, bridge code/test gate pending. |
| matindex.family | pending | Source ledger labels artifacts MERGED; those historical labels are not independently verified source/CI acceptance. Review BitWords, compiler, Maven, OpenRewrite, optional accelerators and candidate-only advisor dependencies. |
| word.signals | pending | Word functionality is documented without a standalone MIndexWord requirement. Reuse symbol dictionary, exact search cascade, facts, affix/n-gram owners; code/test/license review remains pending. |
| family.remaining | pending | Tuples, collections, Object, Path, Algorithm, DataStructure, Tool, Library, Framework, structural indexes, generated sources/resources and non-prefix dependency closure remain unsurveyed. |
| dictionary.lifetime | pending | OS-shared immutable generations, corrupt/truncated image rejection, cross-process Windows/Linux lifecycle, lazy loading/single-flight, eviction/close and native lifetime remain unimplemented in this slice. |
| compiler.route-b | pending | No source lowering or invokedynamic transformation implemented. Existing compiler/bridge owners require typed eligibility, negative and boundary tests before any mutation. |
| jdk.route-c | pending | No java.lang.String/HotSpot modifications in this port. PR #6 merged into its feature-base branch; the current master tree still equals the original foundation tree. Interpreter-only receipts and enabled StringJoiner OOME failures remain historical, separate gates. |
| string.full-surface | pending | Explicit facade covers a subset only. Full Java 21 String surface, intern/identity, constructors, case/strip/format/replace/split/decoding, full API differential matrix remain pending. |
| substring.compatibility | pending | Arbitrary UTF-16 versus surrogate-rejecting runtime SubMIndexString contracts are distinct; source-specific bridge code and regression tests not yet ported. |
| precompute.backends | pending | Full bounded facts/regex preparation/search-index/native/GPU backends remain pending. Current regex calls java.util.regex directly; no RE2J/GPU equivalence claim. |
| native.acceptance | pending | JNI ownership/release/modified UTF-8, JVMTI, JIT, GC, CDS, deduplication, serialization and serviceability are not accepted by stock-view tests. |
| provenance.catalogues | pending | Existing source donor/problem-category catalogues discovered by directory metadata; content/license review and measured algorithm-selection catalogue remain pending. No challenge-site code imported. |
| migration.exact-head-ci | implemented_unverified | Additive workflow only; existing foundation and private-source download gates are unchanged. No secrets or write permission. |
