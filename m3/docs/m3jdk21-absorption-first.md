# M3JDK21 absorption-first control plane

## Purpose

M3JDK21 keeps the original OpenJDK `configure` / `make` build as the product oracle. Maven is an
additive control plane used to inventory, normalize, atomize, patternize, document, plan and verify
candidate absorption work.

The governing order is:

```text
original JDK21 source
  -> Maven/OpenRewrite Java-21 convergence
  -> FILE inventory
  -> FILE atomization
  -> FILE patternization / IOP
  -> FILE documentation
  -> second-pass fixed point
  -> detached normalized JDK21 baseline
  -> complete post-21 JEP/JBS/commit inventory
  -> compatibility/dependency proof
  -> file-atomic recipe crates
  -> apply only in admitted scope
  -> OpenJDK build / jtreg / runtime / JNI oracle
  -> serial promotion
```

Atomization and patternization therefore happen before donor absorption whenever the touched Java
file is compatible with the locked Java-21 parser/contract.

## Do not Maven-replace OpenJDK

The Maven reactor under `m3/` must never pretend to be the OpenJDK product build.

Maven owns:

- OpenRewrite recipe compilation and JUnit proof;
- semantic inventory and M3IndexDB;
- source-convergence candidate generation;
- deterministic upstream inventory/queue generation;
- absorption manifests and recipe-DAG evidence.

OpenJDK still owns:

- configure/autoconf;
- make;
- HotSpot/native build;
- generated JDK images;
- jtreg;
- runtime/JNI/JVMTI/CDS/JFR/GC/tool verification.

## One-command absorption planning

The `m3-jdk-absorption` Maven profile is the common orchestration profile.

In the migration-recipes module it runs
`M3Jdk21SourceConvergenceMain` against the original `src/**/*.java` and `test/**/*.java` trees and writes only
candidate postimages plus `SOURCE_CONVERGENCE.tsv` under `target/`.

In the backports module it:

1. inventories every commit in the pinned JDK22..27 donor intervals;
2. creates the deterministic Java-21 compatibility proof queue;
3. joins that complete commit denominator with the feature/JEP/JBS queue and the exact
   source-convergence manifest;
4. writes `ABSORPTION_PLAN.tsv` and its content-derived root.

Example:

```bash
mvn -B -ntp -f m3/pom.xml \
  -Pm3-jdk-absorption \
  -Dm3.upstream.jdk.root=/path/to/openjdk-jdk \
  -Dm3.convergence.threads=8 \
  verify
```

The donor checkout must contain the pinned GA refs. No donor source is copied by the planning profile.

## Count-independent denominator

The absorption join must not hard-code a JEP count.

The feature plane consumes the repository's authoritative `BACKPORT_WORK_QUEUE.tsv`. The complete
enhancement plane consumes the generated `COMPATIBILITY_QUEUE.tsv`, which derives from every
upstream commit in the pinned release intervals.

When JEP/JBS authority grows, the same join accepts the new rows without a new implementation.

## Baseline states

For complete commit rows, Java target paths are joined against `SOURCE_CONVERGENCE.tsv`:

- `JAVA_BASELINE_CONVERGED` — every touched `src/**/*.java` or `test/**/*.java` file has a fixed-point convergence
  receipt;
- `JAVA_BASELINE_HOLD` — at least one touched Java file cannot reach the admitted Java-21
  atomization/patternization fixed point;
- `JAVA_BASELINE_MISSING` — the convergence manifest does not cover a touched Java file;
- `NO_JAVA_SOURCE` — the upstream commit has no Java source target.

A baseline state is evidence only. It never proves semantic compatibility.

Feature/JEP/JBS rows that do not yet have exact touched-path dependency closure remain
`AWAIT_FILE_ATOMS`. Rejected Java-21 contract changes and superseded preview lineages remain
explicit exclusions/redirects.

## Authority

The absorption plan is read-only evidence.

It grants no:

- source mutation authority;
- donor-copy authority;
- compatibility admission authority;
- promotion authority.

Only the existing `apply` pass may mutate a candidate worktree, and its Java targets must have
matching fixed-point source-convergence receipts.

## Recipe-first rule

The source-convergence recipe DAG remains the reusable product. Adding a new atom/pattern family
means improving `M3FileConvergenceRecipeDag` with its own JUnit proof, not hand-editing thousands of
JDK files.

The absorption manifest consumes those reusable fixed-point results. It does not introduce another
atomization engine.
