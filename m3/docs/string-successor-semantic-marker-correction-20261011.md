# M3String successor semantic-marker correction — 2026-10-11

Baseline: `182007381a27df753e939fa5f4be6d44626dd352`
Branch: `aix/repair-string-successor-semantic-marker-20261011`

## Failure

The owner-coordinate runtime workflow now reaches
`test_actual_owner_sources_match_exact_receipts`, where
`check_string_phase.py` rejects current `M3String.java` because its marker
list still requires the older expression:

`return M3StringPool.concat(canonicalize(first), canonicalize(second));`

That expression was intentionally replaced by the A6 bounded-pool refusal fix in
M3JDK PR #531. Current source at commit
`3c3d66694aaa1b345bbee1c3fe905f1a07ad3906` explicitly canonicalizes each
operand and returns `null` if either admission is refused, allowing
`String.m3Concat` to retain the flat compatibility path.

## Repair contract

Replace the obsolete single expression marker with exact markers for the current
control flow:

- `M3String left = canonicalize(first);`
- `if (left == null) return null;`
- `M3String right = canonicalize(second);`
- `if (right == null) return null;`
- `return M3StringPool.concat(left, right);`

Retain all existing owner/value, designated-join, literal-regex, and pool
markers. Do not weaken source checks, remove refusal tests, alter source code,
or promote the successor. The marker update asserts the stronger bounded
admission/fallback invariant instead of accepting an arbitrary concat call.

## Verification

Run `test_string_phase_successors.py` and `check_string_phase.py`, then the
owner-coordinate workflow and the stacked runtime/foundation/release gates.
