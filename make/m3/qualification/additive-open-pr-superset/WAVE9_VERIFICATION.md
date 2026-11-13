# M3JDK21 Wave 9 — 115-file Commons decorator additive source census (2026-11-03)

**DRAFT / HOLD — source verified, product build and canonical recipe ownership not qualified.**

Base: existing non-default product [PR #2359](https://github.com/hsoliwal/M3jdk21/pull/2359) head `33d38dec7fe97ab8e46d7cc998c630120ff17953`, already containing original wave8 source.
Original Commons decorator donor: [PR #2169](https://github.com/hsoliwal/M3jdk21/pull/2169) at `091e5eacf135073d3d100a08f0f41161733783d4`, original preimage `bb66a8c16642a4e898dbc5fc7c3af0d5ed572136`.
Source-transport [PR #2389](https://github.com/hsoliwal/M3jdk21/pull/2389) two-parent commit `0eb5624751813fc657e25cef9d2353cbdeb96286`.
Current review source head prior to passive evidence `bb16c179eb026e80a2e9f751b35b0d612d1bd0df`, a second two-parent history-only join with current #2359 after it absorbed wave8. Review [PR #2390](https://github.com/hsoliwal/M3jdk21/pull/2390).

## Actual exact source custody

- Donor original Git change set: **115 paths**, 75 modified, 40 newly added, zero removed.
- Before merge: Git tree source snapshots compared `m3/collections/implementations` and `make/m3/collections/qualification`, both complete and untruncated. All **75 modified original preimage blob SHA-1s match** the current review source, and **40 new targets absent**.
- After merge: all **115 original donor Git blob SHA-1s and file modes match** the review tree; zero absent, changed, or dropped paths. True regular two-parent merge, no rebase, squash, force push, or default-branch edit.
- The 115-row `WAVE9_DECORATOR_SOURCE_CENSUS.tsv` records original path, PR head, status, blob SHA-1, Git mode, and reviewed source commit. This is a Git source-and-mode proof, NOT an executed Java21/JNI/HotSpot/ABI or source-built product proof.
- The earlier wave8 [#2387](https://github.com/hsoliwal/M3jdk21/pull/2387) is already an ancestor of #2359 at the exact source bytes; wave9 preserved [transport #2388](https://github.com/hsoliwal/M3jdk21/pull/2388) history without duplicating it in the final source diff.

## Mandatory source ownership and release HOLD

The original PR #2169 adds `make/m3/collections/qualification/decorator-fill-20261030` generated decorator tests and pattern/template/generator helpers. Reusable source-changing Maven/OpenRewrite recipe ownership must remain in canonical `hsoliwal/com.synexia`, not become another product-internal recipe engine. Audit and relocate reusable transformation logic before any M3JDK21 product reception while preserving result-only Java source, tests, licences and API contracts. Original donor says Commons Collections 4.5.0 is a reflective oracle and first-party templates generated the implementations; no external method body copy is assumed licensed.

Full Java21/JDK image, Maven/OpenRewrite fixed point, JUnit/jtreg, native JNI/C17, HotSpot, GC, API/ABI, differential benchmarks, provenance/licensing and hosted CI must run **on this exact merged head**. Prior donor-local testing does not admit the union. Older overlapping PRs (including Object.toAdpater vtable and other collections) are not silently combined. The account-wide `hsoliwal/*` stable full PR DAG remains incomplete.

**This is review-only; no default `master` or Synexia `develop` merge is authorized.**
