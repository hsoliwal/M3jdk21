# Native owner qualification: direct GCC lane

The exact current `m3/fs/native` sources passed all 12 existing C suites and the
45-function Java/JNI ABI comparison on Linux x86_64. Every compiler invocation
retains the owner warning flags, including `-Werror`; all compile logs are empty.
The two JNI bridges also passed syntax checks with their freshly generated Java
headers force-included. `source-sha256.json` seals all native owner inputs.

`direct-gcc-runner.py.txt` is an exact text copy of the executed task-root
`run_native_capture.py`. Individual JSON receipts preserve every actual command
and exit code. It selects the same library sources and 12 suite names as the
existing CMake owner, including the two white-box suites, and reproduces the
existing DB/WAL/SHM cleanup driver. CMake and CTest themselves were unavailable
and were not run. This lane does not claim the full Maven lifecycle.

The official Ubuntu SQLite 3.45.1 development package was acquired and verified
against the SHA-256 in existing Ubuntu package metadata. `acquisition/` preserves
the initial sandbox denial, the newer-package HTTP 404s, the successful base
header download, and extraction identities. Only task-local files were added.
The header package is 3.45.1-1ubuntu2; the installed runtime used by the tests is
3.45.1-1ubuntu2.5. Both expose SQLite API version 3.45.1; the distribution revisions
and exact independent hashes are recorded rather than treated as identical.

Pkg-config is required only for optional `M3FS_FUSE=ON` in the existing owner.
FUSE was disabled. Sanitizers and other platforms were not qualified by this run.

The completed runtime image and combined VFS/capture results are recorded in
`../qualification-native-capture-receipts/runtime-proof.json` and
`../qualification-native-capture-receipts/combined-image-proof.json`. The native
build receipt's pending-image field records its earlier, separate build stage.
The subsequent image probe passed with JNI required and `-Xcheck:jni`.

`PUBLICATION_FILES.tsv` is the mandatory text-only evidence inventory. Native
binaries, class files, test databases and generated runtime images are working
artifacts and are outside that publication inventory.
