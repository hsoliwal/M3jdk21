# java.util stored-occurrence receiver

SPDX-License-Identifier: Apache-2.0

This target branch is stacked on M3JDK21 PR #1100 at
`9dd436215763652eb93aa265366f6bbc661ba4de`.
Canonical source-first Maven/OpenRewrite recipe is
[Synexia PR #12099](https://github.com/hsoliwal/com.synexia/pull/12099),
commit `9b6df6d7970a6e4e2912e911b681ec46eb2c15db`;
name `com.synexia.m3.JavaUtilStoredOccurrence20261017`.
The only production FILE target is
`src/java.base/share/classes/java/util/CollectionViews.java`.
The public interface, other source files, facade class, JNI source, primitive
lanes and M3String remain unchanged. Exactly one regression test is added.

The current owner incorrectly tests a filter predicate against caller-supplied
equal probes, then may delete a hidden equal List occurrence. This scoped
repair evaluates stored elements, uses List physical indexes for deletion,
protects IdentityHashMap-backed Sets and retains existing SortedSet overrides.
No cached live predicate mask or per-entry state is introduced; generic
membership becomes O(n). This is a contract fix, not a performance win.

Run on a clean Linux Java21 checkout:
```sh
bash m3/runtime-integration/java-util-stored-occurrence/verify.sh /absolute/new-evidence
```
Verifier preserves original source under a separate preimage directory, checks
all exact SHA-256 source seals, compiles old/new java.base candidates strictly,
compares public JVM signatures, proves original failure and runs new oracle in
default/interpreter/C1/C2. Its source code is independent proof tooling, not
a second runtime replacement implementation.

Local scoped model oracle passed 736018 checks in each -Xint/C1/C2;
patched Java21 model passed -Xint/C2. That local proof was against the earlier
first-party owner with identical replaced methods, not the exact current
26,096-byte java.util file; target Actions is the exact file qualification.
No production JNI ABI changed; prior native masks remain as-is. Full JDK image,
jtreg, JCK, full reactor, five-provider census, custom identity Set contracts,
external rewrite Maven/JUnit and source licensing remain promotion HOLD.
