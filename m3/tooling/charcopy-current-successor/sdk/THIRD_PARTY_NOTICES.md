# Third-party notices: current-owner regex relation continuation

The recipe adapter, manifest kernel, qualifier and Synexia lineage are Apache-2.0. The M3StringRegexDp.java baseline/candidate and M3StringHierarchicalRegexTest.java fixtures retain their GPLv2 with Classpath exception headers. Packaging them as sealed recipe resources does not relicense those files under Apache-2.0.

The already-public M3DotStarRelations.java, M3StateRelation.java and M3StateRelation.c are Apache-2.0 target adaptations of Synexia MIndexDotStarRelations / MIndexStateRelation; original notices are retained. Exact donors, revisions, paths and the pre-code reuse decision are in FOSS_REUSE_DECISION.tsv and the outer source-binding.json.

The unchanged OpenRewrite adapter1c1b1b568d369f688f6953161636bf36b484bf5a and manifest kernel38df5f97bbeec8a8a8cfc77091d56bda925c07f2 provide transformation behavior. The unchanged Maven POMd79867bcc09ad6329e12418e15907e54b8ab025f retains OpenRewrite8.17.1 and JUnit5.10.2 dependencies. No challenge-site implementation or private dataset is copied. No new product runtime dependency, native ABI or alternative regex parser is introduced.

## Charcopy successor reuse

This task reuses only the unchanged Apache-2.0 engine, manifest kernel and Maven POM identified above. It does not copy the regex payloads described by that historical donor notice. The local charcopy payloads retain their own original notices; see ../THIRD_PARTY_NOTICES.md. Existing headerless receiver helper/test executables remain NOASSERTION per-file and execute unchanged only in ephemeral receiver-shaped fixtures, without Apache relabeling. Source qualification does not establish target runtime acceptance.
