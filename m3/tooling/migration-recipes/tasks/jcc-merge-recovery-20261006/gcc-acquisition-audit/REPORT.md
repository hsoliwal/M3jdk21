# M3 exact GCC pin acquisition audit — 2026-10-06

## Result

The declared GCC/G++ version **10.4.0-4ubuntu1~22.04** is a recorded Ubuntu 22.04 backport, so the version suffix and Jammy runner are historically consistent. **Current acquisition remains unproven.** The official current and old-release pool listings checked here contain no exact-pin packages. The official snapshot service documents a possible recovery route, but every attempted snapshot metadata download failed before receiving a body. No workflow change is justified by an unverified snapshot timestamp.

## Concrete CI evidence and current applicability

[Linux x64 release job 111917562555](https://github.com/hsoliwal/M3jdk21/actions/runs/37355594879/job/111917562555) executed commit `0483f79ae51dd578c2e4fec1b8431bd51b59b469` on Ubuntu 22.04.5 LTS, image `20260927.309.1`. Its APT version was `2.4.14`. The command requested `gcc-10=10.4.0-4ubuntu1~22.04` and `g++-10=10.4.0-4ubuntu1~22.04` after refreshing ordinary Jammy archive pockets. APT reported both exact versions missing and exited 100; Configure and Build did not execute.

The two workflow bodies were retrieved at `b71ee5bf88398fb80961c13db03e8c675fb445ce`. Their Git blob identities still match the actual `.github` tree at `87590cb96fb0e2dc0f88fae8e01957f7179cd255`:

| Path | Git blob | SHA-256 |
|---|---|---|
| `.github/workflows/build-linux.yml` | `1f6823c2be45535d268217ee4c65af95ebcdce3b` | `db21647bdf5228850f5b3b5608f46b0d7de12fc53f4a775021ca21fa4a11c07a` |
| `.github/workflows/main.yml` | `04cea1293ee2ce11cf1b0f9e6f98d57ef8447118` | `6caf401e349de95b37041524a598aa0174c8511d216bbc7932a7fd9d0d997039` |

`main.yml` supplies the same native pin to the x64, x86/multilib, HotSpot variants, docs and cross-compile jobs. Cross-compilation additionally declares `10.4.0-4ubuntu1~22.04cross1`; that distinct package family was not qualified here. `build-linux.yml` uses `ubuntu-22.04` and installs from the runner's mutable APT indexes. The observed problem is this exact package acquisition path, not evidence that the JNI/JDK sources failed to compile.

## Official archive observations

- [Current Ubuntu GCC 10 pool](https://archive.ubuntu.com/ubuntu/pool/universe/g/gcc-10/) was fetched successfully: `742` `.deb` entries, no exact requested version.
- [Old-release Ubuntu GCC 10 pool](https://old-releases.ubuntu.com/ubuntu/pool/universe/g/gcc-10/) was fetched successfully: `2259` `.deb` entries, no exact requested version. This is a bounded pool observation, not proof that all historical copies are absent.
- The current [Ubuntu GCC 10 package catalog](https://packages.ubuntu.com/jammy/gcc-10) lists `10.5.0-1ubuntu1~22.04.3` for amd64. No substitution was made.
- The official [source descriptor](https://archive.ubuntu.com/ubuntu/pool/universe/g/gcc-10/gcc-10_10.5.0-1ubuntu1~22.04.3.dsc) and its [Debian source archive](https://archive.ubuntu.com/ubuntu/pool/universe/g/gcc-10/gcc-10_10.5.0-1ubuntu1~22.04.3.debian.tar.xz) were downloaded. The archive's SHA-256 `1d78f0939f984920a952a56421be768351774229e8940d458377223a4451972f` and size match the descriptor. Its `debian/changelog` entry at line 188 identifies the exact requested version as a Jammy backport dated 19 July 2022. The descriptor's PGP signature was not verified; this observation establishes the source-history record, not binary admission.

## Snapshot route and precise remaining gate

[Canonical's snapshot documentation](https://snapshot.ubuntu.com/) says snapshots are available from March 2023 and that updated Jammy APT supports the feature. The failing runner's APT version exceeds the documented `2.4.11` minimum. That makes snapshot-based acquisition a plausible faithful repair; the exact timestamp and package closure still need proof.

For candidate timestamp `20230302T030400Z`, this audit attempted the `jammy-updates` and `jammy-security` InRelease files and universe/amd64 compressed Packages indexes. All four returned curl exit 28, HTTP `000`, zero bytes and `Proxy CONNECT aborted due to timeout`. The exact InRelease and uncompressed Packages URLs were also inaccessible through the web lookup tool. This is an execution-environment transport failure; it does not establish that the service or exact packages are globally unavailable.

The remaining gate is to obtain an official, signature-verified snapshot index that names the exact packages, then acquire and hash-check their complete dependency closure. Native amd64, multilib/i386 and the separate cross-compiler pin need independent coverage. Once those inputs are proven, the smallest faithful change is a separate recipe for the existing acquisition owner that selects the verified snapshot while preserving all exact version pins and every existing build/test step. No unverified snapshot date, compiler upgrade, blanket dependency downgrade or fallback compiler should be introduced.

After recipe materialization, a declared Jammy runner must successfully perform package installation, Configure, Build and required native/JDK tests. This audit has not executed any of those stages. Existing GCC pins remain unchanged.

## Evidence packet

`METADATA_CALLS.json` and `ARCHIVE_CALLS.json` preserve each actual curl command, timing, exit status, response metadata, body size and SHA-256. Downloaded response headers and successful bodies are retained. `WEB_PRIMARY_SOURCES.json` preserves the successful snapshot-documentation response plus the failed G++ catalog lookup. `SUMMARY.json` records scope and machine-readable gates. `INPUT_SEALS.json` seals this packet and the reused CI/workflow evidence. No remote state or project target was modified.
