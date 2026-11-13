# Receipt: m3jdk-donor-convergence (m3/tooling/donor-delta)

This receives the com.synexia receiver crate `m3jdk-donor-convergence-20261101` at develop `ded4a4acdffe`. The crate
carries four postimage generations (`postimages`, `-v2`, `-v3`, `-v4`). Each target path takes its newest generation,
as `PROVENANCE.tsv` records per file.

The result is a new tool, `m3/tooling/donor-delta` (package `jdk.m3.tooling.donors`):
- `M3DonorDeltaPlanner`, the deterministic, fail-closed, read-only donor delta inventory.
- `M3DonorTreeLanes`, a primitive-lane tree with atomic growth and a sorted fast path.
- Four self-test and proof mains.

There is one adaptation. The v1 `verify.sh` compiled only the planner and its self-test, but the planner has used
`M3DonorTreeLanes` since v2, so that script failed with 8 compile errors. It now compiles every source and runs all
four mains.

Gates are listed in `GATES.tsv` and replayed by `gate-wsl.sh`. The crate's donor registry and recipe qualification
stay in com.synexia.
