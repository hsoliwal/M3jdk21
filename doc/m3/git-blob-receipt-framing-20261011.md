# Git blob receipt framing defect — 2026-10-11

## CI evidence

The `M3 foundation contracts` and `M3JDK String owner-coordinate runtime`
jobs fail in `m3/runtime-integration/char-array-copy/recipe/apply.py`:

`superseded owner drift: src/java.base/share/classes/java/lang/M3String.java expected=70d1d03c109126fdda36c54da21b484bbe368fd9 actual=2e1239e157650dda20d4fd1d95e6fe35d1fe4c5a`.

The helper `git_blob(data)` is intended to calculate the Git blob SHA-1:
`SHA1(b"blob " + decimal_length + NUL + data)`. Its header currently uses a
Python source literal containing two backslashes before `0`, so it hashes the
two bytes backslash and ASCII zero rather than the required NUL byte. The
computed digest therefore differs from the repository's actual Git blob SHA.

## Minimal correction

- Replace the two-backslash source escape with the single `\0` Python NUL
  escape in the blob header.
- Add a regression assertion against the exact current M3String Git blob
  `70d1d03c109126fdda36c54da21b484bbe368fd9`.
- Preserve the manifest, historical transformation atoms, superseded-source
  policy, and all target Java files unchanged.
- Keep unknown drift and symlink refusal tests intact.

## Verification

Run Python syntax checks, the complete `test_recipe.py` suite, then rerun the
foundation and String owner-coordinate runtime jobs. The fix must prove both
that a known repository blob matches and that mutated bytes are still refused.
