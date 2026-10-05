# Fork superset intake

M3JDK21 treats downstream JDKs and related transformation projects as evidence sources, not as
automatic merge authorities.

The intake order is:

```text
pin donor revision
→ identify the matching upstream baseline
→ isolate donor-only delta
→ classify license and contract scope
→ record prerequisites and later fixes
→ feed the existing Java21 compatibility queue
→ generate/reuse the existing recipe crate
→ verify diff/lint/build/jtreg/runtime
→ serial promotion
```

A donor can improve M3JDK21 only after Java21 compatibility proof. Popularity, benchmark claims,
or the fact that another distribution ships a feature do not constitute admission.

Current first-pass candidates:

- Dragonwell 21 SIMD sort: native/JIT candidate. Requires the complete JBS dependency set,
  floating-point ordering proof, CPU-dispatch/fallback proof, OpenJDK build, jtreg and benchmark.
- SapMachine Vitals: diagnostics candidate. GPLv2/OpenJDK licensing must remain intact; lifecycle,
  crash/report ownership, platform support and overhead require proof.
- JetBrains Runtime Wayland repairs: useful only if the receiving runtime has the corresponding
  backend. Until then the individual patch is a typed exclusion, not a transplant candidate.
- OpenRewrite parser hardening: tooling-plane candidate. It informs M3 recipe/parser safety but is
  not JDK product code.
- Rewrite Prethink: source-available licensing prevents treating it as Apache-2.0/FOSS substrate;
  retain as research evidence only.

No row in the fork ledger is auto-selected for distribution. The existing M3 compatibility and
recipe machinery remains authoritative.
