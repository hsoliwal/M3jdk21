# M3JDK21 backport programme status — 2026-10-06

This is an execution snapshot, not a completion claim.

## Released JEP denominator

The current JDK 22–27 released-feature denominator is **85 JEPs**.

The previous 82-row catalogue omitted:

- JEP 404 — Generational Shenandoah (Experimental), JDK 24;
- JEP 483 — Ahead-of-Time Class Loading & Linking, JDK 24;
- JEP 521 — Generational Shenandoah, JDK 25.

`RELEASE_JEP_AUTHORITY.tsv` now owns the fail-closed release/JEP set. All JDK 22 through JDK 27
rows are released; JDK 27 is no longer treated as an in-development snapshot.

The per-release counts are:

- JDK 22: 12
- JDK 23: 12
- JDK 24: 24
- JDK 25: 18
- JDK 26: 10
- JDK 27: 9

Total: **85**.

The released commit-history denominator remains **14,948 upstream commits**. The JEP authority is
a feature denominator, not a substitute for the complete commit inventory.

## Current packet state

- JEP 493: generated FILE-crate materialization packet is at **verify**. The executor now re-proves
  baseline/donor refs, candidate status, manifest ownership, first/last-path metadata, payload
  hashes, target preimages and fixed point before ephemeral product materialization.
- JEP 485: retained only as an **explicit opt-in SE API extension**. Both OpenRewrite and the Python
  materializer are default-off; mutation requires the explicit JEP485 token. It remains **verify**.
- A3 V6 mastery gate: product-clean proof/admission lane remains **verify**.
- M3 String work remains a separate stacked runtime lane and does not change the released-backport
  denominator.

Current hosted GitHub Actions attempts for several packet branches have failed before GitHub creates
job objects. Those cards are infrastructure evidence only and are not treated as source/test
failures or green proof.

## Completion boundary

Do not report compatible-backport completion until:

1. all 14,948 released upstream commits are inventoried;
2. every commit and all 85 released JEPs have an explicit compatibility decision;
3. every proven-compatible leaf is implemented or proven equivalent;
4. every accepted packet has exact source-bound replay and verification evidence;
5. relevant OpenJDK configure/build/jtreg/runtime gates are green;
6. inventory/admission reruns reach no-compatible-residue fixed point.
