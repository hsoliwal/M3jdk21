# Exact donor-license policy receiving candidate

These are exact application copies of [source PR11986](https://github.com/hsoliwal/com.synexia/pull/11986), source commit 662bb8dde4242930460b809e25bb1877c9c27c6a: policy core d9f99d9d99ff66ae4afd37f147af66f63da61e55 (21files) and workflow bootstrap core d39dc33d8459b0f38492efc3747ed73df54f9738 (11files).

The candidate qualification workflow is the exact recipe AFTER fixture4e363da0abf913e6a5c46dd7ea82bf3ea9a05f87. It preserves all existing commands and appends two steps that run both modules: four policy SDK tests, then two bootstrap SDK tests, strict verifiers and combined artifacts. External metadata avoids a self-reference cycle.

No policy output is applied in this qualification commit. The three policy outputs change only root attributes and two hygiene workflows; four license owners remain exact KEEP inputs. Those outputs require actual passing recipe tests and exact emitted-output custody before a separate application. The added SDK's two raw license copies are intentionally preserved; hygiene may report their bytes before policy application.

Static review passed. Maven/OpenRewrite execution remains NOT_RUN until hosted CI completes. Candidate/preparation metadata is frozen; later receipts provide executed status. Product Java/JNI owners are unchanged. Full-JDK output remains NOT_ADMITTED; runtime/phase advancement is false, and the full migration/continual optimization goal remains ACTIVE.
