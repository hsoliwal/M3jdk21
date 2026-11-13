# Translation receiver test syntax repair — 2026-10-11

## Evidence

The latest immutable text foundation CI run failed at Java compilation in
`m3/core/test/M3TranslationReceiverMapTest.java`. The final element in a
`List.of(...)` argument list lacked its closing parenthesis before the
enhanced-for loop body, causing four downstream parser errors.

## Minimal change

Close the list argument before the loop body. Do not change assertions,
field names, source contracts, or runtime code.

## Verification order

1. Inspect the one-file diff and `git diff --check`.
2. Compile/run the M3 build script on the pinned JDK 21.
3. Re-run the foundation and release-admission workflows.
4. Keep independent Maven dependency-resolution failures separately classified;
   this Java syntax repair does not solve unpublished `*-m3-source-1` artifacts.
