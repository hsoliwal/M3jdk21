# Temurin / Adoptium donor intake

The Adoptium JDK21u source mirror is pinned at the same exact commit as the current upstream21u
baseline, so this intake deliberately records no unique Temurin VM/JDK source optimization at that
pin.

Two separate Apache-2.0 tooling donors are admitted as evidence-only catalogs:

- temurin-build for build orchestration, SBOM generation, source/tag selection, DevKit/toolchain
  selection and build metadata;
- aqa-tests/AQAvit for OpenJDK regression, system/load, external application and performance test
  lanes.

Eight mechanism candidates are recorded with copy/promotion authority false. The next step for each
is a bounded comparison against existing M3 owners followed by ADAPT/WRAP only when it produces a
smaller verified superset. OpenJDK configure/make remains the native JDK build authority.
