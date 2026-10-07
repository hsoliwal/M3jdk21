# JEP484 current-tree custody planning

This pass advances the merged evidence-only path map into target-side custody classification without copying or mutating Class-File API source.

Every direct descendant, unmapped JDK21 source and donor-only JDK24 addition is checked against the current M3JDK21 tree using sealed SHA-256 identities. The planner distinguishes same-path replacement candidates, moved public/internal additions, already-present donor bytes, source drift, occupied targets, retained unmapped residue and donor-only additions.

The plan never equates a mapped file with semantic compatibility. Version/release/class-file-major signals remain review-only. No internal source is deleted when a public descendant exists.

The next pass, only after exact-head evidence, is source-sealed opt-in crate generation for mechanically clean rows plus explicit adaptation packets for drift/version-signal residue.
