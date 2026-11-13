# M3 Gram reviewed-search preflight — source and executed proof

Parent `hsoliwal/com.synexia` #16558, source commit
`c314ec13e6af1fd10cbe7242bc931c2ef645305a`.

The exact change preserves existing public Java/JNI ABI and original native search:
- `PmemGramCampaignAdmission.java` original SHA
  `37ac0361c9dd49b4e29ed48464307aa32bee608b35b264987e896bf74e5321a9`
  -> revised `b6f7ad4fc485e9c6b8bf0f54d5a5eb85e0f9edc959144db407e3639872f12905`.
- `PmemGramCampaignAdmissionJUnitTest.java` original SHA
  `dc6b0274cf0bc0fce2b7f92da02a466c0a8df0ed1c835fd89813c2949ef9ed04`
  -> revised `88e8b29fd1639c66f01d8ae97ee597fca09d796091385a6be340e5e120d32c63`.
- Typed Maven/OpenRewrite exact source preimages and postimages,
  actual SDK `Recipe.run`, fixed-point and missing/drift refusal authored.

Executed locally with OpenJDK 21.0.12.1:
`javac --release 21 -Xlint:all -Werror` of candidate owner and test
against first-party API-compatible test stubs, with original GramIndex
and SelectedSearch sources.
Both JIT and `-Xint` emitted identical `846 assertions, 20,000 selected
sources, 274 exact candidate tasks`.
Four quick JUnit-shaped methods passed, including a physically different
source-image fixture to reject a foreign index before reviewer callback.
The stub's source root was corrected to a content digest, not a constant,
for this refusal proof.

NOT RUN: real Maven/OpenRewrite, JUnit engine, native C17 JNI/POSIX/JMOD,
full-repository CI and provenance, 1GB physical source residency, 20k live
Chrome calls or downstream M3JDK product receiver. Keep DRAFT/HOLD.
