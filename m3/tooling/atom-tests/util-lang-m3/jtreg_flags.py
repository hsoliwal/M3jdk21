# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""jtreg_flags.py <test.java> compile|run : the javac or java flags jtreg derives from one test's header (CPU).

Emulates jtreg for the stock-JDK runs (run_atom_tests.sh, run_atom_perf.sh, mutation_gate.py), so a test passes here
only with the access its own header declares, as under jtreg on an M3JDK build:
  @modules m/p         --add-exports m/p=ALL-UNNAMED            compile and run
  @modules m/p:open    --add-opens m/p=ALL-UNNAMED              run only (javac has no opens: not exported to javac)
  @modules m/p:+open   both                                     compile and run (opens at run time only)
  @run junit/othervm <options> <class>   the options            run only
One flag per line on stdout (bash: mapfile). Importable: flags(path, phase) -> [str]."""
import re
import sys

MODULES = re.compile(r"^\s*\*\s*@modules\s+(.*)$", re.M)
RUN = re.compile(r"^\s*\*\s*@run\s+junit/othervm((?:\s+-\S+)*)\s+\S+\s*$", re.M)


def flags(path, phase):
    text = open(path, encoding="utf-8").read()
    out = []
    for line in MODULES.findall(text):
        for item in line.split():
            target, _, flag = item.partition(":")
            if flag not in ("", "open", "+open") or "/" not in target:
                raise SystemExit(f"{path}: unsupported @modules item {item}")
            if flag in ("", "+open"):
                out += ["--add-exports", f"{target}=ALL-UNNAMED"]
            if flag in ("open", "+open") and phase == "run":
                out += ["--add-opens", f"{target}=ALL-UNNAMED"]
    if phase == "run":
        for options in RUN.findall(text):
            out += options.split()
    return out


if __name__ == "__main__":
    sys.stdout.reconfigure(newline="\n")
    sys.stdout.write("".join(flag + "\n" for flag in flags(sys.argv[1], sys.argv[2])))
