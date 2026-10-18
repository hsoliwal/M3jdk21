# Forward reconciliation before publication — 2026-10-18

The original recipe and proof-01/02/03 remain unchanged historical artifacts. New work is isolated under recipe-current and proof-current-*.

Latest inspected product base: f29b0afe7ad253cc0562bc8dfdd48fc1acec710e. Current Java blob 134fb98f8a39969c43fd75338ecb233c5fe49446; native blob aaa74686949de59b44b363741ef1c414fe3e95d0. These add selectPositions and two JNI exports; they must be preserved byte-for-byte outside the already selected counting atom. No wholesale replacement with earlier outputs is authorized.

Adapt only the existing source-bound recipe preimages and the native replacement anchor so it cannot touch the newly added positions body. Extend the frozen probe to cover selectPositions, including missing-native and legacy-binary fallback. New public descriptors and all seven current exports must match before/after. The unchanged M3LongIndexProjection consumer is blob 317136e66200db1d8fc72d47ff9f8cf52b4566b5.

Qualify each source stage in order: patch, lint, compile, tests, runtime. Keep failures and old receipts; source drift must fail before mutation. Synexia owns the reusable recipe, M3JDK21 owns runtime output. No default-branch writes, rebase, deletion or phase advancement.
