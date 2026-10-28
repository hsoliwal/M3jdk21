<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Long path-mask owner convergence

This one-file successor refines the newly merged `M3LongIndexPathMasks` owner
from `hsoliwal/M3JDK21@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. It retains
that owner's public Java API and validation/window contracts while replacing
its duplicated validated selection loop with the existing shared
`M3IndexPathMasks.selectLongJava` kernel.

The candidate is
`candidates/m3/collections/src/main/java/com/m3/collections/M3LongIndexPathMasks.java`.
Its exact source bindings are recorded in `SOURCE_BINDINGS.json`. The existing
`m3-collections-contract-refinement-20261017` crate owns dataset
`m3jdk21-long-bridge-20261018`; no recipe engine or harness is added or changed.

## Contract boundary and required shared owner

The local `validateShape` and `validateVisitedOffsets` calls run in their
original order before the shared kernel. Thus null arguments, array alignment,
nonempty stage directories, plane validation, signed stride geometry and all
visited physical offsets retain their original exception classes and messages.
Constant-mask shortcuts apply only after that full existing admission.

The entire source tail beginning at `nextOrdinal`, including `scanWindow`,
packing, all validators and the per-stage matcher, is byte-identical to the
newly merged owner. Window limit 4096, alias refusal, zero-limit behavior,
preflight of precisely the inspected segment, packed next ordinal, guard cells
and exceptional output effects remain with that owner. No output allocation is
introduced into `scanWindow`.

The shared primitive kernel's only `ConcurrentModificationException` sites are
its two mismatch guards. The bridge translates that closed owner's message back
to the existing `state planes changed during selection` diagnostic. It does not
catch range/validation exceptions, or exceptions from user callbacks; the kernel
has no callbacks. Stable input arrays remain the public precondition. Neither
owner promises a concurrent snapshot or comprehensive mutation detection, and
this pass does not claim to qualify behavior for racing input mutation.

The shared-kernel dependency is the corrected wrapper r2 source:

- Owner: `M3IndexPathMasks.java`
- SHA-256: `e91152a0bb34178995835266ff7c52ae22d87ec6324d171415aed28e293794d1`
- Dataset: `m3jdk21-wrappers-r2-20261018`

The isolated receiver must first contain that qualified dependency. The one-file
bridge dataset seals the new facade's 2f preimage; its SDK syntax/replay proof
alone does not establish the required runtime closure. Root's final manifest and
owning-module gate bind the shared kernel and this facade together.

## Catalogue decision

`CATALOGUE.tsv` records this pass under validated primitive selection,
owner-specific admission, and bounded output windows. Existing wrapper catalogue
entries `M3_LONG_ROWS`, `M3_CONSTANT_MASK`, `M3_WINDOW` and `M3_JNI_CORE` remain
the inherited donor/algorithm comparison. This task copies no external source.

The public `M3IndexPathMasks.selectLong(..., JAVA)` entry is not used because
its own validation diagnostics belong to a different public API. The existing
package-private validated kernel provides the needed reuse boundary. Replacing
`scanWindow` with a detached full selection would introduce allocation and alter
inspection and failure behavior, so the window owner is retained. A duplicate
native long implementation is unnecessary; `M3IndexPathMasks` already owns the
explicit Java/JNI backend contract. This facade stays Java-only.

General selection retains two scans and exact-sized output, O(count*stages)
kernel work and O(matches) output. Validated impossible/all-zero mask stages
reuse the existing constant-stage paths. Full geometry admission still scans
all required coordinates. These are algorithmic properties, not a throughput
or allocation benchmark claim.

## Qualification and reproduction

The exact existing `M3LongIndexPathMasksSelfTest` is retained unchanged under
`baseline/` and passes on both owners: 7,127 checks across 1,500 randomized cases.
The independent `M3LongPathBridgeBehaviorProbe` compares 1,259 observations of
validation precedence, exception class/message, detached outputs, window
resumption and output guards. Its baseline/successor tape is identical:
`2b6d2baa61986e2357e8621bdc1d9ed663c4bfe08ae9de00117bd34c2a53bb41`.
Public descriptors and constants from `javap -public -s -constants` are also
byte-identical. Both source closures compile with Java 21, `-Xlint:all -Werror`.
Commands and logs are in `proof/GATES.json`.

Use the unchanged canonical recipe machinery:

```sh
cd synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017
mvn -B -ntp -Dm3.dataset=m3jdk21-long-bridge-20261018 -Pcontract verify
python3 receive.py --dataset m3jdk21-long-bridge-20261018 --target /absolute/m3-checkout --check
python3 receive.py --dataset m3jdk21-long-bridge-20261018 --target /absolute/m3-checkout
```

The actual SDK qualification passed five SDK tests, eight input-sealing tests,
and nine isolated receiver cases for this one target.
`proof/SDK_EXECUTION.json` records the actual Maven command and exit status;
the generic crate retains the corresponding SDK and isolated receiver records.
Root owns integration into the current complete receiver and the full module/JNI
gate. This task does not write root product files, edit the corrected 28 source
packet, alter an existing dataset, or add a JNI symbol.

The existing [Synexia recipe-first invariant](../../../docs/M3-SCALE/invariants/SYNEXIA-LLM-RECIPE-FIRST-2.md)
governs the pass: reuse reviewed behavioral atoms and catalogued choices,
transform serially with exact source seals, retain independent oracles, and
qualify the generated result. Existing Apache-2.0 notices and source lineage
remain intact.
