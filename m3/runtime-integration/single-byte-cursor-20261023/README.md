# Single-byte scalar-run cursor qualification

Saved authoring source: hsoliwal/com.synexia@aa7661674605341352b97ad844177106eae2f053 in synexia-openrewrite-recipes/tasks/m3-single-byte-cursor-20261023/authoring.

Current M3String preimage is 8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e (master797d5646898509cfbc0bfd3e7737e08400d59dc2). The candidate reuses its existing CodeUnitCursor in encodeSingleByte after the unchanged ASCII/Latin-1 bulk return. Existing surrogate replacement, code-point sizing, scalar native-read lifetime fences and public/native contracts are retained.

The canonical module imports the current three-source Synexia Java snapshot provider unchanged. It seals one production replacement and one additive actual-M3JDK test. Six SDK methods cover preimages, exact scheduled outputs/fixed point, missing/duplicate/drift/occupied-additive refusal, Java parsing, unrelated owners and named YAML activation. The checker contains 28 input-closure refusal fixtures. These are authored, NOT_RUN at this preparation checkpoint.

The pre-change control retains the original production body and adds only the test. Its two-output assertion is deliberately expected to fail; it must never qualify the candidate. Control and candidate preserve their separate source/pins identities.

Neither production source nor the jtreg test is applied by this preparation commit. Actual target runtime/JNI, owner lifetime and comparative CPU/heap/native retention require a source-matched M3JDK image; none was available in the bounded local inventory. Storage protection/general bulk/C-output holds and the complete migration/continual-optimization goal remain ACTIVE.
