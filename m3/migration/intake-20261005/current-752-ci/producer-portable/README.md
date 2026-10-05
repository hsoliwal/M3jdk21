# Cij/Cit current producer evidence

The actual producer-v2 proof passed all four ordered gates and all nine JUnit methods, with zero failures, errors or skips. Its immutable RESULT SHA-256 is `d6d265a959e378fce47d68b5aa4a3a3bbee14bebf21355a54bd8077cb51e8630`. Source authority is Synexia `9963cc08ff13922b92a0e3db7c30f56fddacba7d`; the adaptation target is M3 `752191c9291f6467110fb8a7badfbdc4c2d41af2`.

The five actual scheduler outputs are preserved inside `evidence-bundle/` with their OUTPUT receipt and patch. `OUTPUT.json` is copied byte-for-byte at this level. Its source-output seal is `3895ac2617d07cefd481a299aa18092d49c5bfb9733177740038b35e1c161442`. The runtime repeated all nine controls: Cij four states/twelve refusals, Cit eight states/fourteen refusals, thirty-two union states in each of two recipe orders, and ten post-scan refusals. Cij and Cit remain independently admitted families; no cross-family atomicity is claimed.

## Preserved unsuccessful preparation

Producer-v1 was prepared but its runner refused before creating a chain or executing any gate because Path component ordering differed from serialized path string ordering. `PREFLIGHT-REFUSAL-v1.json` is the original external disposition. No terminal marker or result was inserted into that old proof. Both complete 23-file project snapshots are retained. The sole project-file correction was `sorted(sealed)` to `sorted(sealed, key=str)` in `proof.py`; recipes, fixtures, resources, POM and README were unchanged. The exact diff and both framework snapshots are in the bundle and are identified by `SOURCE-PROVENANCE.json`.

## Retention and integrity

The unchanged existing packager has SHA-256 `89d43693f508c723e431296cc06f611c8de4f94df8eaddea95a2c7742feb0574`. It produced 1 bounded archive part(s), retaining 209 logical paths, 83 unique content chunks and 2,194,560 logical bytes. `VERIFY.json` reports archive integrity only; packaging ran no compiler, test or runtime proof.

The bundle retains both source snapshots; original command plans, inputs and tool manifests; all producer-v2 gate command receipts and raw logs; exact JUnit reports; generated sources, OUTPUT and candidate.patch; source-authoring/provenance; both specs; the original task packet and six canonical metadata documents; all three additive admission/route records; and applicable source instructions. `RETENTION-AUDIT.json` verifies exact aliases for 140 admitted noncache source/metadata references and verifies that old project bytes stayed unchanged.

Tool installations, dependency-cache bodies, compiled classes and settings are omitted. Their recorded SHA-256 and byte identities remain in the original manifests. Absolute paths in original receipts are preserved, not rewritten into portable execution promises. The evidence is review/reconstruction material, not a hermetic toolchain image or a complete third-party source-custody claim.

Verify or extract with the packaged unchanged helper:

```sh
python evidence-tools/package_evidence.py verify --bundle evidence-bundle
python evidence-tools/package_evidence.py extract --bundle evidence-bundle --output /absolute/fresh-directory
```

`RESULT.json`, `PATCH.json`, `V1-PATCH.json`, `OUTPUT.json`, `candidate.patch`, owner/source provenance, the original v1 disposition, specs, and task metadata are also retained loose for review. The bundle's internal LANES inventory lists the executed terminal producer-v2 lane. Producer-v1 is an explicitly included historical preparation with its external disposition; it is not mislabeled as an executed failed gate.

## Remaining authority boundaries

This focused producer PASS does not qualify M3's receiving Maven/backports modules, workflow/JDK selection, current JNI integration, full reactor, hosted CI or protected-branch publication. `consumerQualified`, `canonicalProductionApplied`, `strictCanonicalAdmission`, `crossFamilyAtomicity` and canonical offline source-closure authority remain false where declared in the original proof. No historical proof was retargeted or promoted by this packaging operation.
