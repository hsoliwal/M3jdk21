# M3JDK Synexia Donor Admission

**Target:** `hsoliwal/M3jdk21`  
**Canonical convergence workspace:** `hsoliwal/com.synexia`  
**Purpose:** make reuse of Synexia code, recipes, task crates, and implementation ideas explicit, traceable, and reviewable.

## Required direction

Synexia is the donor convergence workspace. M3JDK21 should discover and freely evaluate the reusable Synexia catalogue, and should receive polished outputs only after source-side recipe convergence and evidence sealing. This includes implementation ideas and reusable engineering assets—not only runtime Java classes. Recipes, Maven task crates, DAGs, templates, tests, fixtures, indexes, donor comparisons, benchmarks, docs, and verification tools all belong in the catalogue.

The default flow is:

```text
M3 target need
  -> inventory existing M3 and Synexia owners
  -> identify donor + license + exact source revision
  -> improve/replay the canonical Synexia recipe
  -> seal source hashes + test/build/runtime receipts
  -> evaluate target compatibility and license boundary
  -> receive the verified output with provenance
  -> run M3-specific verification
  -> report gaps back to Synexia
```

Do not hand-edit repeated target files when a canonical recipe can generate the same transformation. Do not invent a second canonical owner for a capability already converged in Synexia. Do not claim a recipe ran, a test passed, or a target is polished without an actual receipt.

## License boundary (mandatory)

Synexia-owned material is intended to be Apache-2.0 reusable. Preserve its license, notices, source commit, source path, and artifact hashes on receipt. Ideas and behavior can inform independent implementations, but source-code copying must follow the actual donor license.

This repository currently declares GPL-2.0 in its repository metadata. Apache-2.0 is **not automatically compatible with GPL-2.0-only** for combining and redistributing code. Therefore this policy authorizes discovery, evaluation, and provenance tracking immediately, but does not silently authorize importing Apache-2.0 source into a GPL-2.0-only combined work. Before each source import, record the actual target licensing terms and choose a documented compatible boundary: for example, a separately maintained component where legally appropriate, a valid applicable exception/dual-license grant, or an explicitly reviewed licensing decision. Never strip Apache notices or relabel Synexia code as GPL-only by assumption.

Recipes are first-class licensed artifacts. A recipe's transformation logic, fixtures, templates, and generated outputs each need provenance and license review; do not assume that calling a recipe removes the source-license obligations of copied/generated code.

## Admission checklist

- [ ] Existing target owner and API contract inventoried; duplicate implementation avoided.
- [ ] Synexia donor commit, path, artifact SHA-256, recipe identity, and license recorded.
- [ ] Donor's own license/version/provenance reviewed; public availability is not treated as permission.
- [ ] Preimage/postimage hashes recorded for every modified target file.
- [ ] Recipe refusal, replay, and fixed-point behavior verified where applicable.
- [ ] Format/static checks -> compile -> tests -> JNI/native runtime -> target runtime executed in order.
- [ ] Logs and receipts retained; unexecuted stages remain OPEN.
- [ ] Target-specific adaptations and unresolved gaps recorded and fed back to Synexia.

This document does not change M3JDK21's declared license, alter public APIs, or authorize an unreviewed source import. It establishes the required donor-convergence and admission workflow.
