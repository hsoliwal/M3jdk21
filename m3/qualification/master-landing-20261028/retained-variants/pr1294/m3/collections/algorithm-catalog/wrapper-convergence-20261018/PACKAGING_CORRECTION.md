<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Catalogue delivery correction

The first metadata dataset, `m3jdk21-wrap-catalog-20261018`, is **not qualified**.
Its actual SDK ran five tests with four errors (zero assertion failures and zero
skips), reporting `invalid hash-pinned text manifest row`. The unchanged text
owner rejects metadata `.log`, `.args` and `.h` paths. This refusal concerns
catalogue packaging; the qualified runtime sources and 337-row verification
manifest remain unchanged.

The correction appends `.txt` to each unsupported delivery filename and preserves
every original payload byte. [DELIVERY_PATHS.json](DELIVERY_PATHS.json) / [TSV](DELIVERY_PATHS.tsv)
map original catalogue paths to their delivered paths and bind the unchanged
payload hashes. Executed receipts and historical inventories retain original
execution paths; resolve those delivery names through the mapping instead of
rewriting their contents. Current authored links and indices use delivered names.

Source inspection also identified the unchanged **256-target limit per text
recipe**. The first run failed at suffix admission before reaching that count
check, so this is an inspected additional constraint, not a second observed
execution failure. The correction uses two disjoint datasets without changing
the engine or packing evidence into an opaque archive:

| Dataset | Delivery membership |
| --- | --- |
| `m3jdk21-wrap-evidence-20261018` | All `runtime-proof/` paths |
| `m3jdk21-wrap-catalog2-20261018` | Every other catalogue path |

[DELIVERY_PARTITIONS.json](DELIVERY_PARTITIONS.json) binds exact membership and
counts. Both sets are within 256 targets, their intersection is empty, and their
union is the corrected catalogue. Every receiving path remains under
`m3/collections/algorithm-catalog/wrapper-convergence-20261018/`, absent at the
exact 2f baseline. The corrected datasets still require actual SDK and receiving
qualification by the parent; static packaging checks are not an execution pass.

The [original failure record](history/catalogue-delivery-v1/) retains the v1
dataset metadata, text manifest, modes, engine seal, acceptance freeze, actual
failed SDK log and XML. Only v1 summary payloads changed by this correction are
duplicated there. `V1_PAYLOAD_MAPPING.json` reconstructs all 315 original freeze
payloads from retained files and their exact hashes. The original failed local
dataset and freeze remain untouched. The historical runtime rejection and
independent unexecuted donation guard also retain their prior status.

The pending status describes this correction-authoring epoch. Actual catalogue SDK/receiving success is bound by the publishing parent's external `SOURCE_PIN.json` and receiving receipts; the core packet does not predict its own future qualification.
