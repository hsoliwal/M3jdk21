<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0
-->
# Bounded runtime repair and compatibility hold

The current receiving baseline is `4b24824c3aa09d802121447f8970d442a855f064`.
This maintenance pass uses the existing Synexia Maven/OpenRewrite snapshot host
and four unmodified pinned engines. Native, documentation and control changes
are distinct datasets of `com.synexia.m3jdk.CollectionsContractRefinement20261017`.
There is no Synexia or OpenRewrite runtime dependency.

## Product changes and owner mapping

`ClassLoader::compute_Object_vtable` changes its matching count from five to six
for the already-published non-final `Object.toAdapter` method. The existing
`klassVtable` mismatch guard is retained. There is no new JNI symbol, object
layout or native ownership. `Object` itself is unchanged by this repair.

Eight published Runnable/progress API owners and three functional overload owners receive Javadoc only. Strict Java
21 compilation uses the repository's existing `-Xdoclint:all/protected -Werror`
visibility scope. The eleven emitted sources produce fourteen byte-identical class files
against the original sources when debug metadata is omitted. All-visibility
private/anonymous-constructor documentation failures are retained separately;
this pass does not claim that wider documentation scope is qualified.

The owner map appends Object adaptation, the progress SPI, monitored templates
the functional progress overloads, and the matched native vtable owner. The existing Eclipse forwarder uses the
separate `com.m3.collections.M3ProgressMonitor` SPI; this pass does not conflate
that SPI with `java.util.concurrent.ProgressMonitor`.

## Immutable source succession

The original #1082 eleven-row manifest and #1091 five-row manifest remain
byte-exact. The documentation successors are new eight-row and three-row manifests.
The receiver checker composes only those four pinned manifests: two original
owners are replaced and three template owners are added by #1091, followed by
eight concurrent and three functional documentation replacements. It requires the resulting fourteen
current sources, rejects arbitrary drift or old/new mixtures, validates path
containment and symlinks, and performs no receiving writes. The default-template
checker shares that current-closure verification while retaining its five owners.

Run both receiving checks and the isolated negative regressions:

```sh
python3 m3/compatibility/check_object_adapter_progress_receiver_20261017.py
python3 m3/compatibility/verify_m3_default_template_20261017.py
python3 m3/compatibility/test_object_adapter_progress_receiver_20261018.py
```

## Object compatibility remains on hold

Adding this public Object method cannot be described as universally compatible.
The `M3RuntimeCompatibilityHold` jtreg driver deliberately distinguishes a
passing witness from a compatibility pass. It checks current self-adaptation,
then compiles legacy fixtures using an unmodified Java 21 compiler, records the
class hashes and `invokeinterface` bytecode, and runs those frozen classes on
both stock and candidate images. It separately records changed interface-default
dispatch, source collision diagnostics, Optional-return binary behavior, and
ordinary dual Runnable/Callable results and checked-exception identity.

Supply a genuine stock Java 21 toolchain using `-Dm3.stock.jdk`. Compile and run
the driver with the actual rebuilt candidate image. When invoking jtreg, pass
that system property through the test VM options. The driver uses a new evidence
directory, bounds each subprocess and keeps individual compiler/VM logs.
A successful witness writes `universal_no_break=FAIL_COUNTEREXAMPLES_OBSERVED`.
The candidate-source probes intentionally omit `--release`, so they see the
candidate Object API instead of historical `ct.sym` symbols.

The relevant language boundaries are documented in [JLS 9.4.1.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.4.1.2)
and [JLS 13.4.6](https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.4.6).
Neither source identity checks nor recipe passes replace VM execution, JNI,
JCK or full SDK qualification. Lost unpublished runtime proof is not reused.

## Catalogue and reusable verification

`CATALOGUE.tsv` records problem categories, evaluated candidates, complexity,
license and selection rationale. `FILE_REVIEW.tsv` binds the serial native and
Java source pass to exact before/after identities. The central algorithm
catalogue indexes this packet. Challenge-platform algorithms are marked
inapplicable for these runtime, documentation and source-custody problems;
no unrelated code or performance claims are imported.

Qualify each dataset with the canonical capped Maven host, its five actual SDK
tests and its input-sealing and receiver integrity profile. Apply only its
qualified retained SDK results using the unchanged `receive.py`, first with
`--check`, then apply, then repeat to prove a fixed point. Documentation requires
the additional exact-bytecode/doclint comparison; runtime witnesses and the
full fastdebug image build remain separate receiving gates. The receiving PR
must retain the fresh results and earlier failed attempts with their sources.
STRING phase and all wider compatibility/admission holds remain unchanged.
