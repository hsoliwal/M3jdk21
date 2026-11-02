<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->
# Object.toAdapter: Java/JNI receiving evidence

The required API is `public <T> T toAdapter(Class<T> clazz)`. The existing
implementation already provides assignable identity, exact M3Object capability,
null for unsupported requests, a null-class NPE, and normal subclass dispatch.
This change adds the missing foreign-function regression coverage while keeping
Object, M3Object, libjava and HotSpot runtime sources unchanged.

The two test files are exact executed results of the Synexia recipe
`com.synexia.m3.ObjectAdapterContract20261102`. `RECEIPT.json` records their hashes,
the protected-source baseline, the actual execution image and bounded outcomes.
Recipe implementation and correction records remain in the authoring repository.

## Coverage

`ObjectAdapterNativeTest` resolves the erased method descriptor through JNI and
checks virtual `CallObjectMethod`, `CallObjectMethodA`, and `CallObjectMethodV`.
It covers identity, primitive/reference arrays, unsupported and null requests,
exact M3Object capability, override results and exception identity, reflection,
method handles, classes with equal names from different defining loaders, and
bounded local-reference reuse. A deliberately nonvirtual mutant is rejected by
the subclass-override assertion.

## Executed qualification

Both received files compiled with warnings as errors, using the existing source-built
M3JDK image's `javac -h`, generated JNI header, platform JNI headers and C17 compiler.
Six direct main invocations passed with `-Xcheck:jni`: default, interpreted and
forced C2, each with observation disabled and enabled. Each run completed
666 assertions and 61,440 repeated JNI calls; totals were 3,996 and 368,640.

The selected existing Object/M3Object/progress/native-event runtime tests also
passed. The unchanged progress test has a pre-existing unused-resource lint
warning and was compiled with lint enabled without treating that warning as an
error. The other selected baseline Java/C compilation used warnings as errors.
The recipe SDK suite passed against both absent targets and received postimages.

## Qualification limits

These are direct Linux image runs. The jtreg harness, a new full JDK build,
Windows/macOS, full-repository/release qualification and performance benchmarking
were not run. This fixture calls the installed Object method through JNI; the
old event fixture includes product C source and does not independently qualify
installed libjava notification exports. The image release marker contains `+`;
all thirteen pertinent working source files were matched to committed blobs and
the binaries were hashed, without claiming a reproducible whole-image build.

The historical `object-doop-events-20261018` seals remain unchanged and correctly
reject current `classLoader.cpp` drift. This successor records current-source
evidence without rewriting that history. No performance improvement is claimed.

The repository-wide naming gate was run on the exact baseline checkout plus this
change. It refused an existing companion-map hash for `java/util/Arrays.java`.
That file matches committed HEAD and is unchanged by this patch. The receipt
retains the refusal separately from passing adapter qualification; the older
source-build working tree's naming-gate pass is not substituted for this result.

For normal target CI, the new Java file supplies six `@run main/othervm/native`
declarations and the companion native library follows the existing jtreg naming
convention. Run it using the repository's normal `make run-test` configuration.
