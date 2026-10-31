<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Release JEP authority manifest input order SDK

This is a source-only proposal under admission 24bdaa2c54d7ff2da53f3edfcf068ddd4ffa70f7. It uses the exact original Maven POM and source-sealed providers. The one target is the authority recipe input manifest, not its 11 generated product files. The original 1966-byte manifest is preserved; the new manifest permutes complete rows by Java-compatible case-sensitive path order 1,2,3,4,6,8,5,7,9,10,11. All hashes, target paths and payload resource names remain identical. The inverse restores the original bytes.

Three JUnit methods are authored: 16 named forward/inverse successful runs including fixed points and source-order permutations; 6 drift/missing/duplicate refusals across both directions; and execution of the unchanged authentic strict provider against all 11 original target templates, followed by its fixed point and direct refusal of the original row 6 and latent row 8 manifests. The original authority and strict-provider tests are retained unchanged as frozen resources. They require their original receiving Maven model and are not silently replaced by this SDK.

The strict provider source 4a38e39d0755462d40c2b22548b87abde938ea0b is compiled in pinned-engine under the unchanged original POM. No coordinate aliases or dependencies are added. If that exact model cannot compile it, the gate stays open; do not patch the POM or substitute a local provider. The generic providers remain 1c1b1b568d369f688f6953161636bf36b484bf5a and 38df5f97bbeec8a8a8cfc77091d56bda925c07f2; POM d79867bcc09ad6329e12418e15907e54b8ab025f.

verify.py reuses the frozen owned SDK Surefire report validator and adds only this closure/counter adapter. Its synthetic controls are not JUnit execution. Qualify with fresh admitted scratch, M3_QUALIFICATION_ID and EXPECTED_SOURCE_SHA:

```sh
python3 -I -S -B verify.py --inputs --self-test
mvn -B -ntp -Dstyle.color=never -Dmaven.test.skip=false -DskipTests=false test
python3 -I -S -B verify.py --reports
```

No Maven/Python execution, authentic module verification, source application, scope-registry repair or product promotion has occurred during RAM authoring. The existing autoreview helper is NOT_RUN under local bulk holds. Source snapshots and templates preserve donor notices verbatim; generated bare protocol manifests cannot contain comment rows under the unchanged manifest kernel, so their notices are supplied in .license sidecars. Keep source admission, original tests and historical failures intact.
