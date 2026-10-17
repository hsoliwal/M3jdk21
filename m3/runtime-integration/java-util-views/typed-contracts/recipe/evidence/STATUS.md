# Execution receipts

Run1: SDK tests passed, but runtime orchestration failed at a stale negative-control source anchor. No complete Maven success is claimed. The verifier anchor was corrected, not the runtime templates. The review archive retains the failed Maven log.

Run2: complete offline Maven verify passed. Run3: complete offline Maven verify passed from a separately packaged task copy. Both run the actual named OpenRewrite SDK and all five JUnit methods, followed by 58 direct commands with expected outcomes. Both generated the same three source hashes and runtime observation tapes.

SUMMARY.json records the selected results. RAW_RECEIPTS.json binds the original compressed Maven logs, full command/stdout/stderr journal and final Surefire XML; their bytes are in the conversation review archive, not in this Git tree. The archive additionally retains the complete failed/earlier/final raw proof and generated wrong candidate sources. Those candidates are reproduced permanently by verify.py and never applied to product source.

No binaries, dependencies, fonts, credentials or toolchain archives are shipped. Assertions overlap; repetitions add no new unique coverage. The target remains draft pending the documented product/license gates.
