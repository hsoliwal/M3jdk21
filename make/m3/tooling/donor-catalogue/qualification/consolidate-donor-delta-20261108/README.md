# Consolidation: m3/tooling/donor-delta folded into m3/tooling/donor-catalogue

Both modules declared package `jdk.m3.tooling.donors`: one package split across two module roots. #2791 had received
the com.synexia donor-delta planner as its own module. Its six sources move into the module that owns the package,
`m3/tooling/donor-catalogue`; its README becomes `DONOR_DELTA.md` there; its verify step is appended to the owner's
`verify.sh` (the four delta mains run after the catalogue plan fixed point), so the existing
`.github/workflows/m3-donor-catalogue.yml` now runs them too. The old module root is removed. No class name collides.

| gate (WSL, OpenJDK 21) | result |
|---|---|
| donor-catalogue/verify.sh: javac -Xlint:all -Werror over all main+test sources, javadoc -Xdoclint:all -Werror over all main sources (now including M3DonorDeltaPlanner, M3DonorTreeLanes) | rc=0 |
| catalogue self-test, validate, plan fixed point | M3_DONOR_CATALOGUE_SELF_TEST checks=36 PASS; plan byte-identical twice |
| delta mains | M3_DONOR_DELTA_SELF_TEST checks=32; M3_DONOR_LANES_PASS checks=100005; M3_ATOMIC_GROWTH_BOUNDARY_PASS checks=256051; M3_DONOR_SORTED_FASTPATH_PASS checks=113203 (same as before the move) |
