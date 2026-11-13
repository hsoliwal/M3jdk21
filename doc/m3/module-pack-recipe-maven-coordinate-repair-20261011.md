# Module-pack OpenRewrite dependency coordinates — 2026-10-11

## Observed failure

The release-admission/control-reactor workflow failed while resolving
`org.openrewrite:rewrite-xml` and `org.openrewrite:rewrite-test` at
`8.17.1-m3-source-1` from Maven Central. That source-build suffix is a
repository-local coordinate, not a published Central release. The module-pack
recipe module is also built standalone by CI, outside the vendored-source
reactor, so those coordinates cannot be satisfied by reactor ordering alone.

## Minimal correction

Use the already-declared `rewrite.version=8.17.1` for these two build-time
recipe/test dependencies. This aligns the module with the adjacent
`migration-recipes` module, which already uses published OpenRewrite 8.17.1
coordinates. These are tooling/test dependencies, not JDK runtime dependencies.

Do not change M3 source-owner manifests, target Java/JNI code, or licensing
dispositions.

## Verification

Run the standalone module-pack recipe Maven tests, then the M3 control reactor
and release-admission workflows. A successful dependency resolution is not
itself proof of runtime behavior or release readiness.
