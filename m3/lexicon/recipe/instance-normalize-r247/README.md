Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0

This follow-on recipe reduces the remaining normalization/validation allocation in the existing M3 instance-name index. It applies after R246's comparator recipe. The default branch gained R245's mapping registration during qualification, so a preceding exact SDK reconciliation preserves that registration and adds R246's already-qualified instance registration. The text change retains the existing API, source-identity, storage and lifetime contract; no runtime owner or broad path admission is added.

Already-normalized inputs still produce a fresh String, using the public String(String) constructor to retain immutable backing when the runtime permits it. Inputs needing ASCII folding keep the StringBuilder path, copying the unchanged prefix once. The existing four-character ASCII blank predicate is scanned directly without a stream. Validation still scans all UTF-16 before ASCII controls, so a malformed surrogate wins even when a control occurs earlier.

The separate contracts of name fields, metadata keys, metadata values and source scope are preserved. No trimming, Unicode normalization, non-ASCII folding, broader blank classification, canonical interning or fixed-address claim is introduced. The returned-map path retains its pre-existing direct lookup behavior.

Executed qualification passed:
- Nine recipe JUnit tests across three serial stages: current-default SDK reconciliation, original comparator replay and normalization replay. Exact output/fixed point and missing/duplicate/foreign/path refusals are checked; the mapping registration remains admitted.
- Eleven behavior JUnit tests in both before and generated-after stages: the six new normalization tests plus the five retained R246 UTF-8 tests.
- 159,555 normalization/refusal cases per stage, checking exact exception class/message, mixed-error precedence, UTF-16 content, fresh identity, field distinctions and immutable views.
- 1,408,801 encoding comparisons per stage; both unchanged contract mains report 25 + 59 checks in each stage.

Five fixed 20,000-call rounds on Java21.0.9+7-LTS-338 observed:
- Public lookup allocated bytes: [6262472,6262472,6262472,6262472,6262472] before, [480000,480000,480000,480000,480000] after.
- Escaping already-normalized results: the same before/after byte counts. The result is deliberately stored to a volatile field, and freshness is checked.
- Escaping uppercase-input normalization: [6262472,6262472,6262472,6262472,6262472] before, [4022472,4022472,4022472,4022472,4022472] after.
- Direct comparator allocation remains zero in these rounds. Timing varies; these measurements do not establish a portable speedup or zero-allocation public API.

The reference is the exact R246 postimage, with only the class identifier renamed into a test-only class. The hash-bound runner derives that projection mechanically and records its source identity. Actual generated output is compiled and tested separately. OpenJDK21 source inspection supports the immutable-copy choice; backing sharing is a pinned runtime detail, not an OS interner or cross-process address guarantee.

Replay with:
```text
python -X utf8 -I -S -B qualification/run.py
```
This reuses the existing offline Maven/RAM compiler and 47 pinned JARs, with no cache download/copy. Exact source and resources are readable, hash-bound files assembled into RAM. The runner requires the pinned Windows toolchain/cache inputs. Full logs, packets, source/projection identities and counters are in evidence/qualification.json (zlib/hex, expanded length/SHA-256 verified).

Canonical packet: Synexia synexia-openrewrite-recipes/crates/m3-instance-normalize-r247/.
Mirror: M3JDK21 m3/lexicon/recipe/instance-normalize-r247/.
The existing R246 packet and original recipe/evidence remain intact as historical proof in the same paired PRs. Current replay order is synexia-instance-reconcile-r247 (current default SDK -> SDK with both registrations), synexia-instance-utf8-r246 (current original owner -> R246 comparator), then synexia-instance-normalize-r247 (R246 owner -> final normalization). The old R246 registration recipe remains tied to its original base and is not a source-drift bypass.

C: was below the 8GiB reserve on the fresh check, so C writes remain held. K: is the verified output lane; F: inputs are read-only. The authorized 545MiB total task budget and 8GiB K reserve were preserved, one transient POM was removed after child reaping, and zero task scratch files remain. Fresh current-ABI/native/platform, full-reactor, complete corpus and complete LangDex admission-bridge proof remain outstanding; the full goal stays active.
