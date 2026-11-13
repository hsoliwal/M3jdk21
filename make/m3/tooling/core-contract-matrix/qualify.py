# Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
"""Qualify the explicit current cohort through unchanged make/m3/build.sh.
Adapted from the existing current-baseline qualifier; no product source edits.
"""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, os, re, shutil, signal, subprocess, time, traceback

CAP = 512 * 1024**2
RESERVE = 8 * 1024**3
def identity(raw):
    return dict(bytes=len(raw), sha256=hashlib.sha256(raw).hexdigest(),
                gitBlob=hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest())
def relative(name):
    p = PurePosixPath(name)
    if not name or p.is_absolute() or "\\" in name or ":" in name or any(x in ("", ".", "..") for x in name.split("/")):
        raise ValueError("unsafe source path")
    return Path(*p.parts)
def regular(path):
    if not path.is_file() or any(p.is_symlink() for p in [path, *path.parents]):
        raise ValueError("missing or redirected source: "+str(path))
    return path.read_bytes()
def admit(rows, observed):
    expected = {r["path"]: r for r in rows}
    if len(expected) != len(rows) or expected.keys() != observed.keys():
        raise ValueError("exact complete source ownership required")
    for name, row in expected.items():
        relative(name)
        if identity(observed[name]) != {k: row[k] for k in ("bytes", "sha256", "gitBlob")}:
            raise ValueError("source identity drift: "+name)
    return True
def pass_markers(text, marker):
    pattern = re.compile(re.escape(marker)+r" checks=[1-9][0-9]*(?:[ \t].*)?")
    return [line for line in text.splitlines() if pattern.fullmatch(line)]
def main():
    ap = argparse.ArgumentParser()
    for key in ("source", "out", "jdk"): ap.add_argument("--"+key, type=Path, required=True)
    a = ap.parse_args()
    source, out, jdk = a.source.resolve(), a.out.resolve(), a.jdk.resolve()
    if source == out or source in out.parents or out in source.parents:
        raise ValueError("disjoint source/output roots required")
    runner = Path(os.environ["RUNNER_TEMP"]).resolve()
    if runner not in out.parents or not out.is_dir():
        raise ValueError("owned runner output required")
    manifest = json.loads(regular(source/"make/m3/tooling/core-contract-matrix/CURRENT_INPUTS.json"))
    assert manifest["schema"] == "M3_CURRENT_CORE_CONTRACT_INPUTS_V1"
    assert manifest["repository"] == "hsoliwal/M3jdk21" and len(manifest["files"]) == 81
    rows = manifest["files"]
    before = {r["path"]: regular(source/relative(r["path"])) for r in rows}
    admit(rows, before)
    java_paths = {p.relative_to(source).as_posix() for domain in ("m3/core/src", "m3/core/test") for p in (source/domain).rglob("*.java")}
    assert java_paths == {p for p in before if p.endswith(".java")}, "unlisted Java source"
    build = before["make/m3/build.sh"].decode("utf-8")
    selected = re.findall(r'-cp build/tests ([A-Za-z0-9_]+) \| tee "build/logs/([^"]+)\-\$mode\.log"', build)
    assert [r[0] for r in selected] == manifest["programs"] and len(selected) == 18
    assert manifest["modes"] == ["jit", "int", "nocompact", "c2"]
    assert not (source/"m3/build").exists(), "never overwrite a prior build"
    work = out/"contract"
    work.mkdir()
    for directory in ("tmp", "home"): (work/directory).mkdir()
    def budget():
        total = 0
        for root in (source, out):
            for p in root.rglob("*"):
                if p.is_symlink(): raise ValueError("redirected output")
                if p.is_file(): total += p.stat().st_blocks * 512
            if shutil.disk_usage(root).free < RESERVE: raise ValueError("8GiB reserve exhausted")
        if total > CAP: raise ValueError("512MiB output cap exceeded")
        return total
    env = dict(os.environ)
    for key in ("BASH_ENV", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLASSPATH"):
        env.pop(key, None)
    env.update(M3JDK=str(jdk), M3_BOUNDED_COHORT=str(source), JAVA_HOME=str(jdk),
               TMPDIR=str(work/"tmp"), TEMP=str(work/"tmp"), TMP=str(work/"tmp"),
               HOME=str(work/"home"),
               JAVA_TOOL_OPTIONS="-Xmx128m -XX:-UsePerfData -Djava.io.tmpdir="+str(work/"tmp")+" -Duser.home="+str(work/"home")+" -XX:ErrorFile="+str(work/"hs_err_pid%p.log"))
    log = work/"original-build.log"
    wrapper = r'''set -euo pipefail
cd -- "$M3_BOUNDED_COHORT/m3"
M3_GUARDED_BUILD_ROOT="$(pwd -P)/build"
m3_checked_paths() {
  local target resolved
  for target in "$@"; do
    case "$target" in -*) continue;; esac
    resolved=$(realpath -m -- "$target")
    case "$resolved" in "$M3_GUARDED_BUILD_ROOT"/?*) ;; *) return 73;; esac
    printf 'M3_VERIFIED_FS_TARGET %s\n' "$resolved"
  done
}
rm() { m3_checked_paths "$@" && command rm "$@"; }
mv() { m3_checked_paths "$@" && command mv "$@"; }
rmdir() { m3_checked_paths "$@" && command rmdir "$@"; }
source "$M3_BOUNDED_COHORT/make/m3/build.sh"
'''
    argv = ["/bin/bash", "--noprofile", "--norc", "-c", wrapper, str(source/"make/m3/build.sh")]
    start = time.monotonic()
    child = None
    error = None
    peak = budget()
    result = dict(status="FAIL", sourceCommit=manifest["sourceCommit"],
                  sourceInputs=rows, programs=manifest["programs"], modes=manifest["modes"],
                  productChanges=0, generationCalls=0, nativeExecution=False)
    try:
        version = subprocess.run([str(jdk/"bin/java"), "-XX:-UsePerfData", "-version"], env=env, capture_output=True, timeout=15, check=True)
        result["jdkVersion"] = (version.stdout+version.stderr).decode("utf-8", "replace")
        with log.open("xb") as stream:
            child = subprocess.Popen(argv, cwd=source, env=env, stdin=subprocess.DEVNULL,
                                     stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
            print("M3_CORE_BUILD_STARTED pid="+str(child.pid), flush=True)
            while child.poll() is None:
                peak = max(peak, budget())
                if time.monotonic()-start > 600 or log.stat().st_size > 128*1024:
                    raise ValueError("bounded core replay deadline/output gate")
                time.sleep(.2)
        if child.returncode != 0:
            raise AssertionError("unchanged original build failed; retain original-build.log")
        expected = {prefix+"-"+mode+".log" for _, prefix in selected for mode in manifest["modes"]}
        actual = {p.name for p in (source/"m3/build/logs").glob("*.log")}
        assert actual == expected | {"compile.log"}, "all72 original test logs required"
        counts = {}
        for name in sorted(expected):
            text = regular(source/"m3/build/logs"/name).decode("utf-8")
            program = next(n for n, prefix in selected if any(name == prefix+"-"+mode+".log" for mode in manifest["modes"]))
            hits = pass_markers(text, manifest["markers"][program])
            assert len(hits) == 1, "one original pass marker required: "+name
            counts[name] = hits[0]
        classes = {p.relative_to(source/"m3/build/classes").as_posix(): identity(regular(p))
                   for p in sorted((source/"m3/build/classes").rglob("*.class"))}
        assert len(classes) >= 27
        for p in (source/"m3/build/classes").rglob("*.class"):
            raw = regular(p)
            assert raw[:4] == bytes.fromhex("cafebabe") and int.from_bytes(raw[6:8], "big") == 65
        result.update(status="PASS_CURRENT_ORIGINAL_CORE_BUILD", originalExecutions=72,
                      originalPassMarkers=counts, compiledClasses=classes,
                      coreJar=identity(regular(source/"m3/build/jdk.m3.text.jar")))
    except Exception:
        error = traceback.format_exc()
    finally:
        if child is not None and child.poll() is None:
            os.killpg(child.pid, signal.SIGKILL)
            child.wait(timeout=15)
        logs = {}
        for p in sorted((source/"m3/build/logs").glob("*")):
            if p.is_file() and p.stat().st_size <= 128*1024:
                raw = regular(p)
                logs[p.name] = dict(identity=identity(raw), content=raw.decode("utf-8", "replace"))
        result.update(exit=child.returncode if child else None, error=error,
                      reaped=child is None or child.poll() is not None,
                      elapsedNs=int((time.monotonic()-start)*1e9), sampledPeakAllocated=peak,
                      capBytes=CAP, reserveBytes=RESERVE, buildLogs=logs,
                      argv=argv, originalBuildLog=identity(regular(log)) if log.exists() else None)
        try:
            admit(rows, {r["path"]: regular(source/relative(r["path"])) for r in rows})
            result["sourcePosthash"] = "PASS"
            result["allocatedAfter"] = budget()
        except Exception:
            result["status"] = "FAIL"
            result["posthashError"] = traceback.format_exc()
        (work/"RESULT.json").write_text(json.dumps(result, indent=2)+"\n", encoding="utf-8")
        print(json.dumps(dict(status=result["status"], exit=result["exit"], error=error,
                              sourcePosthash=result.get("sourcePosthash"))), flush=True)
    raise SystemExit(0 if result["status"] == "PASS_CURRENT_ORIGINAL_CORE_BUILD" else 1)
if __name__ == "__main__": main()
