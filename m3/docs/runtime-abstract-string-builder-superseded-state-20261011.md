# Runtime AbstractStringBuilder superseded-state reconciliation — 2026-10-11

Baseline: `67560245263dc6a8dcccdeb505ee930f572b8806`
Branch: `aix/repair-runtime-builder-superseded-state-20261011`

## Exact failure and lineage

After the M3String owner and runtime String source-state repairs, the layered
runtime recipe reports drift at
`src/java.base/share/classes/java/lang/AbstractStringBuilder.java`.

The original runtime recipe postimage SHA-256 remains
`a5e7213ca951fab21d6db842804a8abd79e90f924ec672de6c4c455cdebb47d5`.
The exact reviewed current source at stack head
`592c7f28969b5f283d1cae418e4d0e281fe590df` has SHA-256
`d83328e987776e47de2875415d5f0fb62ff4b24e2d74ba60ea0357239cdef6dc`.

The forward compare from the runtime recipe target
`3c3d66694aaa1b345bbee1c3fe905f1a07ad3906` to the reviewed stack head shows
one hunk in this file: five additions and one deletion in
`appendChars(String, int, int)`. When the source String has canonical M3
storage, the Latin1-to-UTF16 builder path now uses the source's bounded
`getBytes` traversal; otherwise the original `StringUTF16.putCharsSB` path
remains. This is the follow-on optimization in commits
`6cb3230c19f26ea5581fc054b82471387fba2ccf` and
`592c7f28969b5f283d1cae418e4d0e281fe590df`. The fallback branch is preserved.

## Repair contract

- Keep the original `before` and `after` SHA-256 values unchanged.
- Add the exact current SHA-256 as a distinct `superseded` state for this path.
- Do not modify `runtime.patch` or `patch_sha256`.
- Keep the source-state schema strict: a superseded hash must differ from both
  original hashes, and unlisted drift remains rejected.
- Re-run the runtime recipe fixed-point/refusal tests and full foundation gate.

This is an exact source-state receipt for a reviewed later optimization, not a
claim of full runtime qualification.
