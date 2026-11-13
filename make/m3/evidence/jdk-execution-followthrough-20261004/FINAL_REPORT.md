# JDK execution follow-through — 2026-10-04

One existing assembler changes; no HotSpot/java.base product source, public API, JNI ABI or root POM changes. Seven recipe targets add the optional developer profile, independent product-source password proof, tests/docs and Maven orchestration. Existing Java/Text OpenRewrite adapters are reused.

## Executed

- Exact preimages/postimages checked; Python and XML syntax validated.
- 24 Python unit/refusal tests, including explicit synthetic fixtures; 41 existing standalone Java module-kernel checks.
- Linux developer image: 11 real JDK tools, a second linked modular application, and an actual native jpackage app-image launcher all executed.
- Existing diagnostics image/JFR/jcmd smoke rerun.
- Current unchanged Password.java compiled into a temporary java.base patch; 14 policy scenarios in JIT and interpreter VMs, 28 fresh VMs per run, repeated in clean outputs. It is not a rebuilt JDK or jtreg result.
- Four original JAR/JMOD outputs matched byte-for-byte across two clean developer runs; password class files also matched. This is not a whole-image reproducibility claim.

## Not passed / not executed

Actual Maven/OpenRewrite scheduler/JUnit/JaCoCo: blocked by missing mvn (127) and dependency DNS (6). No testing-framework stubs substituted. Existing coverage thresholds remain unchanged; 99% is not measured. Full modified M3JDK21 build, jtreg, JavaFX, full backport denominator, all module packs and cross-platform acceptance are not complete.

Nebula means hsoliwal/nebula. Its existing Synexia Maven/Tycho donor-assimilation pipeline remains authoritative; this JDK fixture is not Nebula UI proof. Application/promotion there stays conditional on the actual gates. The recipe and implementation are saved in separate additive commits; no master/develop rebase, force-push or merge is performed.

Selected raw logs, runtime receipt summaries, exact source hashes and outstanding exits accompany this report. Do not redistribute fixture private keys or upstream runtime binaries as original M3 artifacts.
