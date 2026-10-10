# M3JDK Synexia Donor Admission

**Target:** `hsoliwal/M3jdk21`  
**Canonical convergence workspace:** `hsoliwal/com.synexia`  
**Purpose:** make reuse of Synexia code, recipes, task crates, and implementation ideas explicit, traceable, and reviewable.

## Required direction

Synexia is the donor convergence workspace. M3JDK21 should freely discover, evaluate, reuse, and receive qualified Synexia material under the applicable license. Synexia is where reusable ideas are compared, recipes are corrected, implementations are converged, and evidence is sealed. M3 is a receiving project/target, not a competing canonical owner for the same reusable capability.

This covers implementation ideas and engineering assets—not only runtime Java classes. Recipes, Maven task crates, DAGs, templates, tests, fixtures, indexes, donor comparisons, benchmarks, docs, and verification tools all belong in the catalogue. Recipes are first-class deliverables and must not be omitted from donor reuse merely because they are build-time assets.

The default flow is:

```text
M3 target need
  -> inventory existing M3 and Synexia owners
  -> identify donor + license + exact source revision
  -> improve/replay the canonical Synexia recipe
  -> seal source hashes + test/build/runtime receipts
  -> import/reuse under preserved license and provenance
  -> run M3-specific verification
  -> report gaps back to Synexia
```

Do not hand-edit repeated target files when a canonical recipe can generate the same transformation. Do not invent a second canonical owner for a capability already converged in Synexia. Do not claim a recipe ran, a test passed, or a target is polished without an actual receipt.

## License and repository metadata

Synexia-owned material is intended to be reusable under Apache License 2.0. M3 should borrow that material—including recipes—while preserving applicable copyright/license notices, NOTICE obligations, source commit, source path, artifact hashes, and other required provenance. Reuse remains subject to the actual license of each file and donor; public availability alone is not a license grant.

**Inventory finding requiring reconciliation:** the M3JDK21 GitHub repository metadata endpoint reports `GPL-2.0`, while the repository's root `LICENSE` file and README describe Apache-2.0. Those signals conflict. This PR does not resolve or silently override that conflict. Until the canonical license declaration is reconciled, each import must preserve its upstream license and notices, and maintainers must verify the applicable repository/file terms before combining or redistributing the result. Do not infer GPL-only from API metadata or infer that all historical files are Apache-2.0 from the README alone.

Recipes are first-class licensed artifacts. Review recipe source, fixtures, templates, and generated output provenance separately where needed; executing a recipe does not automatically erase source-license obligations. Never strip notices or silently relicense Synexia material.

## Admission checklist

- [ ] Existing target owner and API contract inventoried; duplicate implementation avoided.
- [ ] Synexia donor commit, path, artifact SHA-256, recipe identity, and license recorded.
- [ ] Donor's own license/version/provenance reviewed; public availability is not treated as permission.
- [ ] Target license declaration reconciled; applicable terms checked before combining/redistributing.
- [ ] Preimage/postimage hashes recorded for every modified target file.
- [ ] Recipe refusal, replay, and fixed-point behavior verified where applicable.
- [ ] Format/static checks -> compile -> tests -> JNI/native runtime -> target runtime executed in order.
- [ ] Logs and receipts retained; unexecuted stages remain OPEN.
- [ ] Target-specific adaptations and unresolved gaps recorded and fed back to Synexia.

This document does not change the target's license. It establishes the required donor-convergence, provenance, and admission workflow.
