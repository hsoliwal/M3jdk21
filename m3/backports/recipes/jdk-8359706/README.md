# JDK-8359706 — Java 21 compatible open-file-descriptor diagnostics

Primary upstream issue: `JDK-8359706`, **Add file descriptor count to VM.info**.

Primary donor commit: `openjdk/jdk@b0831572e2cd9dbff9ee2abcdf81a493ddcecc7e`.

Required macOS follow-up: `JDK-8380236` /
`openjdk/jdk@3a109f49feb19f313632be6a2aa24ba7d9b7269b`.

## Compatibility decision

This is additive HotSpot/serviceability diagnostics. It adds an internal
`os::print_open_file_descriptors` contract and reports the current process descriptor count in
`VM.info` and fatal-error reports on supported platforms.

It does not change Java source grammar, class-file versions, public Java SE API, JNI/JVMTI ABI,
serialization/persistent formats, GC policy or JIT semantics.

M3JDK21 adaptations:

- Linux uses bounded `/proc/self/fd` enumeration, the donor 50 ms timeout idea, an ASCII digit
  check instead of later-header assumptions, and `unknown` when procfs cannot be opened.
- macOS keeps libproc/Mach includes Apple-only and folds the JDK-8380236 `precond` build repair
  into the initial postimage.
- the fatal-error path reuses the caller-owned scratch buffer on macOS.
- AIX and Windows keep explicit unsupported/no-op implementations.
- the count is emitted at the two required serviceability outputs rather than also duplicating it
  inside Linux `print_os_info`.
- the existing `TestJcmdSanity` contract is extended only for Linux/macOS and accepts the bounded
  lower-threshold form.

## Recipe atoms and DAG

`M3Jdk8359706OpenFdCountBackportRecipe` joins eight independent FILE atoms:

1. AIX stub;
2. BSD/macOS implementation;
3. BSD/macOS helper declaration;
4. Linux counter;
5. Windows stub;
6. shared internal OS contract;
7. VMError/VM.info output join;
8. jtreg proof.

`packet.tsv` keeps those leaves independent and adds one explicitly approved MODULE join.
`atom-evidence.tsv` binds every atom to contract, documentation, pattern/IOP role, JUnit proof and
required fixed point. Camel, Airflow and Drools are projections of this same evidence-bound DAG and
gain no mutation or promotion authority.

## Promotion gates

Status remains `candidate-adapted` until recipe/evidence validation, Linux and macOS image builds,
focused `TestJcmdSanity.java` jtreg, diff/lint, fixed-point replay and later unsupported-platform
build lanes pass. No whole-JDK or whole-programme completion is implied.
