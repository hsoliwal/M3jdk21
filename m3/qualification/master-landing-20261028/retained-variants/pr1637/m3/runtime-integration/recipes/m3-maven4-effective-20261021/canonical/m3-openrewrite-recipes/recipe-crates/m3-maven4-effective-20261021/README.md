<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Maven 4 completed-model projection: canonical recipe packet

This packet adds `M3ASTAuthoritativeModels.maven4Effective` to the existing M3
parser owner. It projects the completed model from the real Maven 4 public
`ModelBuilderResult` into the existing detached `M3ASTIndex`. The caller retains
the builder, session, model and repositories. The existing Maven 3 entry and
JDT route retain their observable shapes. The one added public callable is an
explicit requested API addition; no existing callable is removed or replaced.

The implementation reads an already completed model. It does not construct a
second parser, model builder, resolver, session or classpath. Effective-model
occurrences remain `SOURCE_UNVERIFIED` with `(-1, -1)` coordinates because their
values can originate in inherited POMs, BOMs and activated profiles. This index
does not supply authoritative source-edit coordinates or dependency resolution.

## Exact owners and source custody

| Owner | Pin and role |
| --- | --- |
| Receiving M3 sources | `hsoliwal/M3jdk21` at `2779aba3f98f31c324be43f731be7914e1a05935`; exact preimages in this crate. Later stacked application is accepted only at each sealed preimage or postimage. |
| Existing canonical engines | `hsoliwal/com.synexia` at `e02e7cb0f2e8e46db4c5b476d59aa26658fa4d07`; all four shared engine/scope sources remain byte-identical. |
| Qualified harness donor | The separate integrated packet's SDK003 at receipt SHA-256 `72ea68db76198ff1f8d4e55f7df9bd6c5a40c1cc075abbfb851a7a343ff9e0ff`; exact source snapshots, modification diffs and lineage are retained. Its runtime results and memory events are not inherited. |
| Real Maven 4 provider | [hsoliwal/maven at `2993571eedbe96e06a737eaa8ae47cf112b255de`](https://github.com/hsoliwal/maven/tree/2993571eedbe96e06a737eaa8ae47cf112b255de), version `4.1.0-SNAPSHOT`; original API, implementation and test owners remain authoritative. |

`SOURCE_SEALS.json` selects seven active file phases. Phase IDs retain their
original identity: `001`, `003`, `004`, `005`, `006`, `007`, `008`. The two Java
phases contain the existing owner and an actual-provider contract fixture. The
five text phases contain the existing workflow amendment, the real-provider
README link, and the Maven 4 qualifier, POM and decision README. The named recipe
is `com.synexia.m3.Maven4Effective20261021`.

The target owner and tests are first-party Apache-2.0 sources with existing
notices retained. The task reuses the existing canonical Apache-2.0 engines and
qualified harness. No Maven implementation body or provider binary is copied
into the target runtime. `licenses/` retains the exact Synexia and Maven fork
license and notice texts. `provenance/SOURCE_AND_RIGHTS.json` records the source
pins, API/test roles and per-file target dispositions. Provider JAR hashes are
evidence of runtime inputs; those JARs are not distributed by this packet.

## Retained CSV refusal and selected successor

The first eight-file handoff included an update to the historical
`real-providers/CATALOGUE.csv`. Actual SDK001 refused its manifest with
`invalid hash-pinned text manifest row`: the unchanged text engine does not
admit CSV. That failed attempt, complete original inputs, reports, original
handoff, CSV preimage, proposed postimage and inactive phase `002` remain in the
packet. No result from SDK001 was applied.

The seven-file successor excludes only that CSV change. Every active candidate
retains exactly the original reviewed bytes. The complete searchable category
and selection rationale already exists in the supported Maven 4 README's
“Exact donor and ownership decisions” table, linked by the real-provider README.
It records the exact fork, completed-model authority, distinct Maven 3/4 APIs,
subprojects/modules rule, existing bounded index ownership, rejected alternatives
and rights. The historical CSV stays byte-identical.

`select_supported_successor.py` records this finite inventory choice without
editing an engine or target. `provenance/successor-002/` retains both handoffs,
the prior active metadata, exact selection receipt and subsequent source diffs.
The current harness runs the original inactive CSV fixture as a required refusal
with zero emitted candidates before executing the supported active phases.

## Qualification authorities

| Gate | Evidence and scope |
| --- | --- |
| First actual recipe attempt | `proof/sdk-001-offline/RESULT.json`: retained failure; one failing test, zero errors/skips; no receiver publication. |
| Fresh actual OpenRewrite successor | `proof/sdk-002-offline/RESULT.json`: PASS, one JUnit harness test, zero failures/errors/skips, seven active phases/targets. All source inputs and compiled parser dependencies read back unchanged. |
| Recipe contract coverage | Exact before/after bytes, Java LST and lossless parse, missing/drift/duplicate/wrong-kind refusals, inactive CSV refusal, fixed point, mixed state, source order, serial repeat, unrelated payload preservation, two independent replay workers and named composition. |
| Actual parser attribution | Calls resolve to the real M3 Maven 4/Maven 3/JDT entry points and actual Maven 4, Maven 3 and JDT provider interfaces. The frozen classpath has one compiled M3 directory plus 90 provider JARs; 16 JARs are source-built from the pinned Maven 4 fork. |
| Proof-checked copier | `proof/receiver-qualification-002/RESULT.json`: seven isolated first writes, zero replay writes, four refusals with no fixture mutation; original CSV retained. Product drift, symlink destinations, emitted-output drift and shared-owner drift are refused. |
| Provider authoring proof | `proof/runtime-authoring/qualified-candidate-002/PROOF.json`: fresh offline 19-module fork reactor, 41 original model-builder tests with zero failures/errors/skips; frozen baseline 13 checks and candidate 102 checks, identical incumbent Maven 3/JDT shapes and exactly one public addition. |
| Actual receiving-source application | `proof/receiving-application-001/RESULT.json`: seven generated writes on the integrated target, zero replay writes and exact source-seal verification. |
| Actual emitted-owner runtime | `proof/receiver-runtime-001/PROOF.json`: fresh offline 19-module fork reactor and 41 original builder tests with zero failures/errors/skips; baseline 13 and emitted-owner 102 checks, identical incumbent Maven 3/JDT shapes, one public addition and unchanged source/artifact readbacks. |
| Fresh correction-memory conformance | `memory/FINAL_RESULT-002.json`: 129 conformance checks, 32 control cases, two actual event integrity checks and 27 bound artifacts. Profile conformance passes; event admission remains `PENDING`. |

The single recipe JUnit test is an inventory-driven contract harness, not a
claim of seven independent JUnit cases. Its receipt records each phase and
named-composition gate. Recipe replay, copier behavior and actual provider
execution remain separate authorities. Provider-authoring receipts explicitly
identify their candidate source. The later receiving-application and runtime
receipts bind the generated owner actually installed in the target repository.

The retained provider setup failures are recorded in
`proof/runtime-authoring/evidence/CORRECTION_OBSERVATIONS.json`: incorrect
test-session security mode, a missing Maven 4.1 XML namespace, and a sandbox
refusal of the original Surefire local socket. Their original logs remain; the
successful successor changes neither the fork's validator nor its test transport.
The fresh task-specific profile under `memory/` records the CSV event and the
provider setup observations. Historical fixture/argument custody that was not
retained is explicitly unknown. Its original malformed authoring draft is also
retained as a refusal. Conformance and artifact-integrity receipts live under
`proof/memory-*`; event admission remains `PENDING`. No integrated-task memory
event or runtime promotion is inherited.

## Run the existing owners

With Java 21, the required Maven artifacts already in a task-local cache, and an
inventory containing one actual compiled directory or JAR path per line:

```sh
python3 -B qualify.py \
  --java-home /path/to/jdk21 \
  --maven /path/to/maven/bin/mvn \
  --maven-repo /path/to/task-cache \
  --parser-classpath /path/to/PARSER_CLASSPATH.txt \
  --attempt fresh-offline-attempt --timeout-seconds 300
```

The qualifier retains a new attempt and never replaces a previous one. The
actual Maven invocation is offline, compiles the unchanged four engines and the
task harness with `-Xlint:all -Werror`, and runs the real OpenRewrite scheduler.
Compiled parser and provider inputs must be supplied by their owning source or
runtime qualification; the packet contains no parser-provider stand-ins.

After a passing attempt, the existing copier reads the qualified receiver bytes
and checks every source/proof/resource guard before any target write:

```sh
python3 -B apply_receiver.py /path/to/M3jdk21
python3 -B apply_receiver.py /path/to/M3jdk21 --apply
python3 -B apply_receiver.py /path/to/M3jdk21 --apply
python3 -B verify_sources.py /path/to/M3jdk21
```

The second application must report zero writes. The emitted Maven 4
`real-providers/maven4/verify.py` supplies the separate actual-provider gate. Its
existing workflow job runs that same qualifier with the exact fork pin; the
prior real-provider workflow job is preserved.

The retained evidence covers host Java 21, the selected Maven API/implementation
closure and 41 original builder tests. External build dependencies and incumbent
Maven 3.9.11/JDT 3.40.0 libraries are cached artifacts. Full Maven distributions,
plugin realms, all fork tests, a current custom JDT source build, Maven Resolver,
m2e, Tycho, mvnd, live Eclipse workspaces, the full parser/JMOD gate, rebuilt
M3JDK21/HotSpot and other platforms require their separate owning acceptance.
No speed, allocation, complete-reactor or GitHub CI pass is implied by these
local receipts.
