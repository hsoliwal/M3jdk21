# ClassVer candidate execution report

One existing production file changes: M3ModuleInspector.validateClass now implements the declared Java21 non-preview header policy and documents its semantic atom. The exact previously saved inspector/checks/JUnit postimages are reused, not regenerated.

The earlier unlanded dedicated class-version snapshot engine is not installed. ClassVer is a small named configuration of the existing Java snapshot recipe, with a new three-case actual parser/scheduler test. Its manifest owns three kernel-relative Java targets and retains Java syntax trees. Documentation, seals/tests and materialization are separate additive commits on PR95 ancestry.

Executed locally: exact source/manifest checks; Java21 warnings-as-errors compilation of the real kernel and shared corpus; 639 archive/VM assertions in each of JIT and interpreter modes; 41 existing kernel checks; 11 Python tests; actual JMOD/jlink diagnostics image with JFR and native jcmd. The current builder blob 7e90032177993b367a07fa68572db72f6915b097 was reconstructed from its full read and hash-verified before the diagnostics run. No supplied JAVA_HOME mutation occurred.

These are independent local subset experiments, not promotion after missing framework gates. Maven/OpenRewrite/JUnit/JaCoCo are not reported executed. Local mvn returns127, dependency DNS6. PR95's actual workflow run37190317862 reports failure and zero jobs; its cause remains unknown from the available endpoints. No coverage threshold, exclusion, dependency, root POM, public signature or native ABI is changed.

All results use installed Debian OpenJDK21.0.11 on Linux, not a modified M3JDK21 build. Whole-JDK build/jtreg, the compatible-backport denominator, joined String/JNI/GC work, naming, additional module/UI packs, Nebula and cross-platform admission remain open. Preserve draft state until actual required framework/product gates pass.
