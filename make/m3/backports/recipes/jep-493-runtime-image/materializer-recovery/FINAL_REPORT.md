# JEP 493 materializer recovery

Merged PR #144 introduced the generic generated-crate executor required to turn the existing
file-delta generator into exact, fail-closed FILE candidate materialization. Current master retained
the JEP493 inventory packet but lost the executor, its tests, materialization policy, updated receipt,
README section and execution workflow.

This branch restores those reviewed artifacts from PR #144. No OpenJDK product source is checked in
by this recovery. The executor remains generic and candidate-only: it validates path ownership,
symlinks, manifest hashes, payload hashes, preimages, one-file crate granularity and exact postimages.

The restored workflow regenerates the 47 selected JEP493 FILE atoms from pinned JDK21/JDK24 trees,
validates the feature DAG, materializes only in the ephemeral checkout, proves changed-path scope and
fixed point, then configures with --enable-linkable-runtime, builds images, runs runtimeImage and
legacy jlink/JMOD tests, and performs a real no-JMOD linking smoke.

No hosted success is claimed yet. The only workflow reconciliation beyond the branch trigger is
adding libxrandr-dev, based on an actual prior OpenJDK configure failure on the same runner family.
