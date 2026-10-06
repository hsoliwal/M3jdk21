# JEP 423 cumulative materialization plan

Status: configured candidate; no compatibility or promotion claim.

## Exact lineage

The candidate is not the initial JEP commit alone.

1. `38cfb220ddadbb401cc15f313aadb8234f626210` — JEP 423 implementation.
2. `8643cc21333c6b51242ed3b9295b25f372244755` — HeapRegion pin-count overflow fix.
3. `0d5f5e15d43f94a79c6133baecd5af217365d176` — G1 pin-cache performance repair.

The initial implementation touches 59 paths. The mandatory performance repair adds five more G1
owners. The cumulative denominator is 64 paths; two deleted Java21 JNI stress paths are preserved,
leaving 62 admitted FILE replay atoms.

## Mechanical convergence

    59-path initial denominator
      -> 64-path cumulative lineage
      -> selected-path Git lineage audit
      -> prove no later selected-path JDK22-GA drift
      -> exact JDK21/JDK22 file delta
      -> one Java/native/text recipe crate per admitted changed FILE
      -> exact current-tree preimage proof
      -> ephemeral materialization
      -> postimage proof
      -> second-apply fixed point
      -> MODULE/MULTI_MODULE DAG
      -> fastdebug OpenJDK image
      -> pinned-object/evacuation/GCLocker jtreg
      -> preserved TestJNIBlockFullGC JNI-critical regression
      -> JFR cause/phase proof
      -> serviceability-agent proof
      -> built-JDK G1 runtime smoke
      -> serial promotion review

## Authority

FILE replay does not make JEP423 a FILE-scoped feature. The feature join remains MULTI_MODULE across
HotSpot G1, runtime/WhiteBox, serviceability-agent and test/JFR owners.

The two upstream-deleted `TestJNIBlockFullGC` paths are explicitly outside mutation authority and
must still exist and pass after candidate materialization.

## No GA contamination

JDK22 GA is allowed as the physical postimage source only if the selected-path lineage audit proves:

- implementation-parent -> cumulative-final-fix contains only the three approved commits for the
  62 selected paths; and
- cumulative-final-fix -> `jdk-22+36` contains no later commit touching those selected paths.

If either condition fails, the workflow stops before source mutation and the lineage TSV becomes a
new dependency-review input.

## Proof truth

Every configured gate remains pending until exact-head executable CI produces receipts. Source
inspection, packet completeness, and workflow presence do not imply runtime correctness or
performance improvement.
