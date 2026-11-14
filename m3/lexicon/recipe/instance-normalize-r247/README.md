Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0

Reduce validation and normalization allocation in the existing M3 instance-name index. The current default owner (blob6f9f14312e18e8ffa0a310801278bed846cd0d34, originating commit9ee83d68fdcc3023c037cef5de2120dd447037af) already contains a qualified scalar-prefix UTF-8 comparator with the original byte fallback. This refinement preserves that comparator byte-for-byte and changes only name normalization/blank scanning.

Already-normalized inputs still return a fresh String through the public String(String) constructor. It can retain immutable backing without rebuilding character buffers. Inputs needing ASCII folding keep StringBuilder, copying the unchanged prefix once. The existing SPACE/TAB/CR/LF blank predicate is scanned without stream objects. Full UTF-16 validation still precedes ASCII-control checks, preserving exact mixed-error precedence.

Name fields, metadata keys, metadata values and source scope keep their separate contracts. No trimming, Unicode case folding/normalization, broader blankness, canonical interning or fixed-address behavior is added. Returned-map lookup behavior and immutable views stay unchanged.

The current default SDK contains R245's mapping registration. The first recipe reconciles it with the existing instance-owner registration, preserving both exact paths. The second recipe applies this normalization delta to the current default instance owner. No broad source path or new runtime owner is admitted.

Four executed Maven/RAM stages: current-owner behavior baseline; SDK reconciliation; normalization-owner replay; generated-owner behavior. Results:
- Six recipe JUnit tests across the two serial recipes: exact output, fixed points, missing/duplicate/foreign/path refusals and preservation of the mapping registration.
- Eleven behavior JUnit tests in each before/after stage: six normalization tests and the five retained UTF-8 tests.
- 159,555 normalization/refusal cases and 1,408,801 encoding comparisons per stage, checking exact exceptions, mixed-error precedence, fresh result identity, content/hash, field distinctions and views.
- Both unchanged contract mains report 25 + 59 checks per stage.

Five fixed 20,000-call rounds on Java21.0.9+7-LTS-338 observed:
- Public lookup allocation: [6262472,6262472,6262472,6262472,6262472] before; [480000,480000,480000,480000,480000] after.
- Escaping already-normalized results show the same counts, with the result stored to a volatile field and freshness checked.
- Escaping uppercase normalization: [6262472,6262472,6262472,6262472,6262472] before; [4022472,4022472,4022472,4022472,4022472] after.
- Valid-input comparator rounds allocate zero before and after. Timing varies; these are local measurements, not a portable speedup or zero-allocation public API claim.

The behavior reference is the current default owner with only its class identifier renamed into a test-only class. The hash-bound runner derives and records that projection. Exact generated output is compiled/tested separately. OpenJDK21 source inspection supports the fresh immutable-copy choice; no JDK implementation body is copied and no OS-level interner/address guarantee follows.

Replay:
```text
python -X utf8 -I -S -B qualification/run.py
```
This reuses the offline Maven/RAM compiler and 47 pinned JARs. No cache is downloaded/copied. Readable inputs are SHA-bound and assembled in RAM; pinned Windows toolchain/cache paths are prerequisites. Full logs, packet/source/projection identities and counters are in evidence/qualification.json (zlib/hex; verify expanded length and SHA-256).

Canonical: synexia-openrewrite-recipes/crates/m3-instance-normalize-r247/.
M3JDK mirror: m3/lexicon/recipe/instance-normalize-r247/.
The unchanged R246 packet remains historical comparator evidence. Its older comparator proposal is not replayed over the incoming default comparator. Earlier R247 five-stage proof remains in the prior review commits; evidence/master-reconciliation.json binds that history. Current active replay is synexia-instance-reconcile-r247 followed by synexia-instance-normalize-r247.

C: is below8GiB reserve, so C writes remain held. K: is the verified output lane; F: inputs stay read-only. The545MiB total cap and8GiB K reserve were preserved; one transient POM was removed after child reaping and zero task files remain. Native/current-ABI/platform, full-reactor, complete-corpus and complete LangDex admission-bridge proof remain outstanding. The full goal stays active.
