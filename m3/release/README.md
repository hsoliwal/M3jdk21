# M3JDK21 evidence-first release pack

The first public M3JDK21 release is admitted by evidence, not by repository size, PR count, donor
catalogue size, or marketing language.

## Release theorem

A claim is publishable only when the release proof pack contains the exact source revision,
upstream baseline, commands, environment, raw result or test receipt, and SHA-256 needed to
reproduce that claim.

The release therefore has two layers:

1. **What is implemented and actually proved.**
2. **What remains an explicitly named qualification target.**

A planned feature is not a failed feature. It is simply not a release claim yet.

## P0 order

The canonical release priority is:

```text
M3 String
  -> regex precompute
  -> broader reusable precompute
  -> arrays
  -> collections
  -> SWT / Eclipse IDE integration
```

Later layers may be inventoried in parallel, but they must not be advertised as qualified before
their predecessor gates and their own target-specific evidence are sealed.

## Current first-release claim boundary

The current public P0 foundation is intentionally narrower than the full M3 programme.

**Currently safe to claim, subject to the release-admission workflow passing:**

- an OpenJDK 21-derived M3JDK21 product/research runtime target exists;
- an independently authored immutable-text/precompute foundation exists under the explicit
  first-party licensing boundary;
- source-bound transformation recipes, verification machinery, Java precompute differential tests,
  and JNI/native parity tests exist in the repository;
- the project has a deterministic release contract, rights/provenance record, and proof-pack
  checksum process.

**Not yet a first-release claim unless later evidence upgrades the machine contract:**

- that `java.lang.String` is already fully backed by M3 String;
- complete Java String compatibility;
- complete static/dynamic regex precomputation;
- a full jtreg pass;
- a universal performance win over OpenJDK 21;
- completed array/collection/SWT/Eclipse migration;
- production readiness.

This boundary is a strength. It makes every future upgrade measurable.

## Mandatory proof pack

A release candidate must preserve:

- exact release commit and tree;
- exact OpenJDK upstream baseline;
- M3/Synexia rights and licensing notices;
- foundation build/test logs;
- Java precompute differential results;
- JNI/native parity results where JNI/native is used;
- Maven control-reactor verification;
- release-contract validation;
- generated SHA-256 manifest;
- raw benchmark data before any performance claim.

The release workflow may produce artifacts and receipts. Those artifacts are evidence only; they do
not independently create a release or upgrade a claim.

## Performance claims

A performance claim must compare the exact M3JDK21 candidate against the exact OpenJDK baseline on
the same machine and configuration. Preserve raw output and environment metadata.

At minimum record:

- CPU model, cores/threads, RAM, OS/kernel;
- JDK build identity and flags;
- warmup and measurement policy;
- cold and warm runs separately;
- wall time and CPU time;
- allocations and heap;
- RSS/native memory where applicable;
- GC counts/time;
- startup impact;
- score/error or median/percentiles and run-to-run variance.

Cherry-picked best runs are not release evidence.

## String / regex claim promotion

Before the release text can say that M3 String backs `java.lang.String`, the product must prove
the relevant JDK-visible contracts under product builds and applicable jtreg, including UTF-16 code
units and unpaired surrogates, equals/hash/compare, substring/views, interning, exceptions,
interpreter/C1/C2, GC/lifetime behavior, and CDS where affected.

Before the release text can say regex is fully precomputed, the release must preserve a corpus of at
least 10,000 regex cases against the Java semantic oracle, separate cold preparation from warm reuse,
seal cache keys by owner/snapshot/options/version, and test invalidation/corruption refusal.

## Release immutability

Before the first public tag, unreleased history may be consolidated while preserving the current
tree and upstream ancestry. Once a release/tag is published, that released history is immutable.
Subsequent work starts after the release.

See:

- `release-contract.json`
- `BENCHMARK_AND_COMPATIBILITY_SPEC.md`
- `verify_release_admission.py`
- `../../M3-SYNEXIA-NOTICE.md`
- `../../M3-SYNEXIA-RIGHTS.md`
