# Existing four-library census refusal at PR 1515

The [failed Actions job](https://github.com/hsoliwal/M3jdk21/actions/runs/39262096545/job/118092118868) is reproducible on the exact PR base, `9a164c31b38c7013c179a3ff38b0bb60c199c7fd`. Its source-census refusal precedes compilation and execution of the changed collection owners.

At head `4b703804cadd55f2fa1b6e3fe22a416faeb8dffc`, the fourth step fails with `ValueError: installed interop files missing or not admitted` at `current-census-20261017/verify.py:113`. The verifier admits exactly 36 paths, while the base already tracks 76 interop source/test files. There are 40 additional files and no missing admitted files. The complete additional-path inventory, source identities and failed/skipped steps are recorded in `FOUR_LIBRARY_CENSUS_FAILURE.json`.

The [workflow invocation](https://github.com/hsoliwal/M3jdk21/blob/4b703804cadd55f2fa1b6e3fe22a416faeb8dffc/.github/workflows/m3-collections-interop-receiving.yml#L33-L42) and [exact tracked-set refusal](https://github.com/hsoliwal/M3jdk21/blob/4b703804cadd55f2fa1b6e3fe22a416faeb8dffc/m3/collections/interop/qualification/current-census-20261017/verify.py#L108-L114) have the same Git blobs at base and head: `8f57b8a77361aa1ba9dfa2313a04dcb5bc1cb17f` and `efe6d0f959cdc3172d6f6659f06bb486ef825291`, respectively. The seven PR source/test changes are confined to `m3/collections/src/` and do not change the interop tree, workflow or verifier.

In a checkout of that exact base, this original command returns exit code 1 with the same 40-path error message as the remote run:

```sh
PYTHONDONTWRITEBYTECODE=1 python3 \
  m3/collections/interop/qualification/current-census-20261017/verify.py
```

The remote recipe-engine hash check and four census unit tests passed. The later recipe proofs, collection compilation and runtime contract steps were skipped. The job retained its [failure artifact](https://github.com/hsoliwal/M3jdk21/actions/runs/39262096545/artifacts/12191487354).

This establishes an existing source-admission mismatch. It is not evidence that skipped collection contracts passed, and it does not certify other queued workflows or make CI green. The census, immutable historical receipt, admissions and workflow remain unchanged.
