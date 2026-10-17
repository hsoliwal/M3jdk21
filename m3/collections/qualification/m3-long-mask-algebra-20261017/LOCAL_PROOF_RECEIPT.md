# M3JDK21 mask algebra receiver: evidence and gates

- Source master remains untouched; stacked on prior draft #1121.
- Canonical source owner: Synexia draft #12123, published and sealed first.
- New source change: two Java existing-file replacements + parallel postimage templates.
- Preimages: M3LongMaskRows Git blob d79c3303f9819de65e9435452803126474e2760e; original contract Git blob 1d067a594dcebd0b410f6da22b4479c15d215ec8.
- Postimages: M3LongMaskRows Git blob 5cded927c64fb273f423d20394dffb77dc7787ec; contract Git blob 30fbf5973022d1409b533e71649427d75ad06769.
- Local isolated strict Java 21 compile: PASS; local contract Java 2,602 assertions at two heaps PASS; 3,253 simulated JNI branch checks PASS. No actual native C execution in isolated gate.
- Source-byte seal model: 4 partial subsets, 2 refusals and no-change fixed-point PASS.
- Actual OpenRewrite SDK and Maven: NOT_RUN.
- Full real-owner Java 21 compilation, C17 JNI/UBSan, whole module, jtreg, alternate OS and speed benchmark: NOT_RUN.
- GitHub Actions: inspect independently. Queued or red jobs do not establish promotion.
- Promotion status: DRAFT / HOLD.
