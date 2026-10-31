# M3JDK21 product ownership — additive convergence of PR #1820 and #1822

Source heads:
- [PR #1820](https://github.com/hsoliwal/M3jdk21/pull/1820): `ff87c765f5f4ecccb36e910fd6ed032c78b9c52f`
- [PR #1822](https://github.com/hsoliwal/M3jdk21/pull/1822): `067e409eadc31bb118b8b44e4a44bb8508d20e30`

Target source baseline: `master` commit `dcd02229f7fb6fbc65823354d43eb12b0502589d`.

The two original draft PRs independently changed `m3/compatibility/check_public_names.py` and `test_public_names.py`. Selecting one branch wholesale would lose the other's checks.

## Canonical source union

- Reuse the complete lexical donor-namespace check from #1820 for newly introduced references in real JDK product source, including UTF-8 Java source, comments/string/text-block masking, baseline comparison for modified files, and relevant tests.
- Retain #1822's recipe custody root and descriptor gates. New OpenRewrite descriptors and new source recipes are refused inside M3JDK21. Existing historical product recipe source is preserved as immutable provenance but no further modified recipe source is admitted.
- Preserve #1822's product-ownership explanation in `m3/docs/public-naming.md`.
- Preserve all existing public name guards and the tests from both PRs. The combined `test_public_names.py` includes 28 distinct test methods with no duplicate method names.

## Ownership and nonclaims

Synexia is the recipe/convergence workspace; M3JDK21 directly owns the resulting JDK Java/JNI/HotSpot implementation. This patch changes only the Python compatibility checker, its tests, product documentation and verification workflow. It does not rename, remove, or rewrite any JDK class or native implementation.

The required proof gate is `python -m py_compile` plus `python m3/compatibility/test_public_names.py -v` (GitHub workflow `m3-product-custody-python-check.yml`). Hosted workflow results must be examined before source admission. This branch is a reviewable candidate; until those tests and current-tree checks pass, neither source integration nor supersession of #1820/#1822 is claimed.

No force-push, squash, rebase, or ancestry-only completion claims.
