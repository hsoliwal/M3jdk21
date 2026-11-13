"""ord_relocate.py <donor.java> <out.java> <donor-blob> : PROGRESS_MONITOR_TEST_MAP v1.
Relocates a m3-unix4j-m3 nativecore test into jdk.m3.text and maps its jdk.internal.m3.common.progress monitor
probe onto java.util.concurrent.ProgressMonitor (the M3jdk21 java.base owner NativeLineBatch takes):
beginTask->begin, isCanceled->isCancelled, setCanceled(boolean)->cancel(); setProgress/setTaskName/subTask/state()
have no java.base counterpart and are dropped (the kernel never calls them). Every rule must hit exactly once
(fail closed); a provenance line records the donor blob."""
import sys

RULES = [
    ("package jdk.internal.m3.unix4jm3.nativecore;", "package jdk.m3.text;"),
    ("import jdk.internal.m3.common.progress.ProgressMonitor;\n", "import java.util.concurrent.ProgressMonitor;\n"),
    ("import jdk.internal.m3.common.progress.ProgressState;\n", ""),
    ("@Override public void beginTask(String name, long total) { }", "@Override public void begin(String name, long total) { }"),
    ("        @Override public void setProgress(long work, long total) { worked = work; }\n", ""),
    ("        @Override public void setTaskName(String name) { }\n", ""),
    ("        @Override public void subTask(String name) { }\n", ""),
    ("@Override public boolean isCanceled()", "@Override public boolean isCancelled()"),
    ("@Override public void setCanceled(boolean value) { canceled = value; }", "@Override public void cancel() { canceled = true; }"),
    ("        @Override public ProgressState state() {\n"
     "            return new ProgressState(\"native order\", \"\", worked, UNKNOWN, canceled, done, 0, 0);\n"
     "        }\n", ""),
]

src, out, blob = sys.argv[1:4]
text = open(src, encoding="utf-8", newline="").read()
for old, new in RULES:
    n = text.count(old)
    if n != 1:
        sys.exit(f"RULE_MISS x{n}: {old.strip()[:70]}")
    text = text.replace(old, new)
text = text.replace("package jdk.m3.text;",
                    "// Relocated from m3-unix4j-m3 NativeLineBatchOrderSelfTest blob " + blob
                    + " by PROGRESS_MONITOR_TEST_MAP v1.\npackage jdk.m3.text;", 1)
open(out, "w", encoding="utf-8", newline="").write(text)
print(f"RELOCATED rules={len(RULES)} -> {out}")
