# Synexia intake license-field semantics — 2026-10-11

## Failure

The intake verifier rejected the `indexed-literal-regex-replacement` row with
`license drift`. The row currently records two different authorities in one
field: `Apache-2.0 donor; GPLv2+Classpath receiver`. The verifier compared the
entire field to `Apache-2.0`, conflating the Synexia donor's license with the
M3JDK21 destination's license.

That exact comparison is incompatible with the repository's porting invariant:
the donor's license and the target file/module license must remain distinct, and
the record must not imply that Apache-2.0 re-licenses GPL/OpenJDK code.

## Correction

Interpret the existing field's first semicolon-delimited component as the donor
license declaration for Synexia rows. Require that component to identify
Apache-2.0, and continue to reject `DIRECT_IMPORT`. Preserve the receiver-license
annotation and all target licensing boundaries.

The fix is recipe-first: add a new hash-pinned OpenRewrite text-recipe packet
whose preimage is the exact current verifier and whose postimage changes only
the predicate. Do not mutate the old immutable recipe packet or weaken the
direct-copy review rules.

## Verification

Run the intake verifier, its focused OpenRewrite recipe tests, then the complete
M3 control reactor and release admission. Passing intake alone is not target
runtime or release proof.
