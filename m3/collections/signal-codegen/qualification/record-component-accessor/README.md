# M3JDK21 numeric record accessor — passive successor receipt
Canonical Synexia recipe branch: https://github.com/hsoliwal/com.synexia/pull/15376
Product receiver: https://github.com/hsoliwal/M3jdk21/pull/2308
Recipe-first owner: com.synexia.m3jdk.RecordComponentAccessor20261102; source resources are retained in Synexia, not duplicated here.
The processor now uses RecordComponentElement.getAccessor for named numeric record components. Ordinary class member resolution and public/generated API remain unchanged. Original processor and test Git blobs remain historically reachable and checked by the parent verifier.
Before/after Java SHA-256 and new blob IDs are sealed in SOURCE_MANIFEST.tsv. The target verify.sh compiles actual Java21 annotation processing and tests, but until the exact-head run executes the overall state is HOLD. Synexia's Maven/OpenRewrite SDK, native/HotSpot, whole M3JDK21 image and cross-platform gates remain NOT_RUN. No new JNI symbols or copied challenge solution source.
