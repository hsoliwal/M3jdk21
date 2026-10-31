# Empty natural-order lookup contract

The target is the public `com.m3.collections.M3TreeMap` Java API. This is a
finite contract for one lookup refinement, not whole-library admission.

1. Execute the frozen operator CLI `EmptyRootLookupOracle` using JDK 21.0.9,
   the exact supplied fixture classes, and the qualified target classpath.
   Verify the reported provider class bytes, provider origin, Java home,
   temporary directory, and all 640 unique ordered case IDs.
2. For empty natural-order roots and their unbounded descending views,
   `get`, `getOrDefault`, `containsKey`, `remove`, and the supplied key/entry
   view queries must not invoke a lookup key's `compareTo` method. A valid
   counting key, a key throwing only on self-comparison, and a key throwing
   on any comparison must return the absent result without a callback.
   Exercise fresh maps and maps emptied through `clear` and removal.
3. Retain the JDK original's null rejection and non-Comparable rejection.
   Compare complete return/throw records, raw exception messages, marker
   identity flags, callback sequences, size, emptiness, and ordered entries.
   Do not normalize or omit differences.
4. Comparator-ordered empty roots and populated roots are control cases.
   Compare their complete observable records to both the frozen JDK original
   and the captured prior M3 baseline. Report inherited differences separately
   from any change introduced by the candidate; do not silently waive them.
5. Check all 640 complete case records against the JDK original, retain the
   exact difference paths, and classify every preceding clause. A complete
   exact match supports only this finite corpus. Any difference remains
   visible as a failure or explicit unresolved inherited observation.
6. Do not recompile, edit, replace, or derive alternate fixture expectations.
   Verify the supplied fixture and original-output hashes before and after
   execution. Capture the complete raw CLI output losslessly with a raw
   SHA-256 and a verified compression round trip; retain terminal command,
   exit status, reader-join and process-containment evidence.

The fixture was written by a source-aware orchestrator and frozen against
JDK-only execution before product modification. The behavioral validator
must not inspect product source, fixture source, diffs, build internals,
review packets, or implementation notes. It may execute the supplied compiled
operator CLI and inspect only its public observations, this contract, pinned
runtime access instructions, and opaque input identities.
