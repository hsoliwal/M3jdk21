# STRING invariant CI failure — literal replacement atom boundary — 2026-10-11

## Observed failure

The `M3JDK String owner-coordinate runtime` workflow ran
`m3/runtime-integration/check-m3string-invariants.py` and failed with
`M3_STRING_INVARIANT_FAIL: M3String canonical literal replacement path missing`.

The checker currently requires the declaration
`M3String replace(M3String target, M3String replacement)`. The current
`M3String.java` does not expose that method. Its implementation is split into
three explicit atoms:
- `replaceAt(M3String target, M3String replacement, int found)` for an already
  located canonical M3 needle;
- `replaceFlatTarget(String receiver, String target, M3String replacement,
  int found)` for a flat JDK String target that is not admitted to the pool;
- `replaceEmptyTarget(M3String replacement)` for the empty-target boundary
  contract.

The JDK `String.replace` caller chooses the path and preserves empty-target
compatibility. The private `replaceMatches` implementation shares result
composition and output-length overflow checking. This is atomized ownership,
not evidence that replacement behavior should be flattened back into one
overload.

## Repair contract

- Preserve the current Java implementation and public JDK API.
- Update the invariant checker to verify the actual atom boundaries and their
  semantic obligations instead of requiring the removed monolithic signature.
- Keep the empty-target path, flat-target no-admission path, replacement
  overflow guard, and JDK compatibility caller checks.
- Add negative checker tests for removal of each required atom/guard where the
  existing test harness supports them.
- Run Python syntax/lint and focused invariant tests before the full runtime,
  foundation, and release gates.

## Other failures are separate

The same workflow batch also reports a missing
`m3/compatibility/synexia-string-representation-consumers-receipt.json`,
a full-borrow self-promotion refusal, and broad migration-recipe failures.
Do not weaken those gates or bundle them into this atom-boundary repair.
