# PACK-VITALS: SapMachine Vitals and HiMemReport

Vitals is SapMachine's built-in VM and OS health sampler:
- `jcmd <pid> VM.vitals` shows a short-term and long-term history of heap, metaspace, threads, RSS, swap,
  container limits and load average;
- `-XX:+DumpVitalsAtExit` writes the history at exit;
- a Vitals section is added to hs_err files;
- HiMemReport (Linux) writes a report, and optionally runs jcmds, when rss+swap reaches 66%, 75% or 90% of the
  container or physical memory.

Flags: `EnableVitals`, `VitalsSampleInterval`, `HiMemReport` and related, all in `runtime/globals.hpp` and
`os/linux/globals_linux.hpp`.

## Extraction

- **Donor:** SAP/SapMachine sapmachine21 f831cc777ecb. The lineage is 24 commits from 2021-07 to 2026-10, of
  which 19 are non-merge commits (LINEAGE_COMMITS.tsv).
- **New files (31):** taken unchanged:
  - share/vitals (7 files);
  - os backends: linux, with oswrapper and himemreport, plus windows, bsd and aix;
  - a gtest;
  - 16 jtreg tests in test/hotspot/jtreg/runtime/Vitals.
- **Hooks (14 shared files).** The diff is from openjdk/jdk21u jdk-21.0.13.0.3+1 (SapMachine's last 21u merge) to
  the donor. HUNK_SELECT v1.1 then keeps 30 hunks, drops 26 and strips 10 mixed malloc-trace blocks; every
  decision is in HUNK_AUDIT.tsv. The result was 3-way applied onto M3 master 16e35bee with 0 conflicts.
- **Static checks:**
  - nothing in src refers to a dropped feature (MallocStatistic, mallocTrace, GlibcMallocArenas,
    VMError::print_stack, the cloud-info helpers);
  - `os::Linux::get_total_procs_and_threads` is declared once, defined once and used by 3 files;
  - HeapDumpPath and PrintExtendedThreadInfo are existing upstream flags.
- **Test group:** TEST.groups gains a `tier1_sapmachine` group (runtime/Vitals). The donor's name is kept.

## Proof status

| Check | State |
|-------|-------|
| `make images` on linux-x64, plus the windows and macOS builds that compile the os backends | PENDING: needs WSL, which the RC freeze occupies |
| gtest `vitals`; jtreg `runtime/Vitals` (16 tests) | PENDING: local execution of donor-derived code is blocked by the session classifier |
| `jcmd VM.vitals` smoke test, hs_err Vitals section, `-XX:-EnableVitals` (VitalsOff) | PENDING |

## Manual adjustments after HUNK_SELECT

- `test/hotspot/jtreg/TEST.groups`: stripping the malloc-trace block removed the group's last entry
  (`runtime/malloctrace`) and the blank line after it. That left `runtime/Vitals \` continuing into `tier1 = \`, and
  jtreg failed with "One or more groups are invalid". The trailing `\` is dropped and the blank line is restored.
- `vmError.cpp`: the `#endif` closing `#ifdef LINUX` was lost. It is restored by HUNK_SELECT v1.2.

## Linux x64 results (2026-11-10; WSL Ubuntu 24.04, i9-14900HX)

- HotSpot built with Vitals (`make hotspot exploded-image-base` plus the jcmd modules, no native debug symbols).
- `jcmd <pid> VM.vitals`: works, printing the system/process/jvm tables.
- jtreg `runtime/Vitals`: NOT COMPLETED. The run was stopped when the build disk was handed back to the release lane.
