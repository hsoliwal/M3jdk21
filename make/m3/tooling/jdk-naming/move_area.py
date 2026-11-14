"""move_area.py <area-root> <out-root> <area> : MOVE_TO_JDK_MODULE v1 (approved layout 2026-11-08), java.base step.

Moves one m3/<area> Maven module whose single package is jdk.m3.<area> into java.base as jdk.internal.m3.<area>:
  src/main/java/jdk/m3/<area>/X.java  -> src/java.base/share/classes/jdk/internal/m3/<area>/X.java
  src/test/java/jdk/m3/<area>/X.java  -> test/jdk/jdk/internal/m3/<area>/X.java
  src/main/native/X.c                 -> src/java.base/share/native/libm3<area>/X.c   (opt-in library, unchanged)
  src/main/native/tests/X.c           -> test/jdk/jdk/internal/m3/<area>/native/X.c
  src/main/native/CMakeLists.txt      -> make/m3/<area>/CMakeLists.txt (sources re-pointed)
  recipe/<name>/...                   -> make/m3/evidence/<area>/recipe/...   (one store, no dated directory)
  NOTICE                              -> src/java.base/share/legal/m3<area>.md
  pom.xml, src/main/java/module-info.java -> removed (java.base owns the package; not exported)
Text rules on moved Java and C: jdk.m3.<area> -> jdk.internal.m3.<area>; JNI jdk_m3_<area>_ -> jdk_internal_m3_<area>_;
v1.1: slash form jdk/m3/<area>/ -> jdk/internal/m3/<area>/ (class resources, JNI class descriptors).
Test-only rule: MethodHandles.publicLookup() -> MethodHandles.lookup() (an internal java.base package is not
unconditionally exported). Prints: old path, new path (or REMOVED), rules applied.
v1.1 (M3_TO_JDK_LAYOUT P1): src/test/native/X.c -> test/jdk/jdk/internal/m3/<area>/native/X.c; two sources with one
target refuse (three recipe directories with one file name used to overwrite each other); --keep-pom keeps the Maven
bridge and removes nothing: pom.xml -> make/m3/<area>/pom.xml (same artifact; one build-helper plugin adds the moved
main and test package directories, so every Maven consumer keeps building on a stock JDK 21),
src/main/java/module-info.java -> make/m3/<area>/src/main/java/module-info.java (exports the renamed package), and
recipe/<name>/... and every other record -> make/m3/<area>/... under their own names.
usage: python3 -I move_area.py <area-root> <out-root> <area> [--keep-pom]"""
import re
import sys
from pathlib import Path

SRC, OUT, AREA = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3]
KEEP_POM = "--keep-pom" in sys.argv[4:]
BRIDGE = ("pom.xml", "src/main/java/module-info.java")
PKG, NEWPKG = f"jdk.m3.{AREA}", f"jdk.internal.m3.{AREA}"
RULES = [(rf"(?<![\w.]){re.escape(PKG)}(?![\w])", NEWPKG), (rf"Java_jdk_m3_{AREA}_", f"Java_jdk_internal_m3_{AREA}_"),
         (rf"(?<![\w])jdk_m3_{AREA}_", f"jdk_internal_m3_{AREA}_"),
         # v1.1: the slash form (class resources "/jdk/m3/<area>/X.class", JNI FindClass descriptors)
         (rf"(?<![\w/.])/?jdk/m3/{AREA}(?=/)", None)]


def target(rel):
    if rel.startswith(f"src/main/java/jdk/m3/{AREA}/"):
        return f"src/java.base/share/classes/jdk/internal/m3/{AREA}/" + rel.rsplit("/", 1)[1]
    if rel.startswith(f"src/test/java/jdk/m3/{AREA}/"):
        return f"test/jdk/jdk/internal/m3/{AREA}/" + rel.rsplit("/", 1)[1]
    if rel == "src/main/native/CMakeLists.txt":
        return f"make/m3/{AREA}/CMakeLists.txt"
    if rel.startswith("src/main/native/tests/"):
        return f"test/jdk/jdk/internal/m3/{AREA}/native/" + rel.rsplit("/", 1)[1]
    if rel.startswith("src/main/native/"):
        return f"src/java.base/share/native/libm3{AREA}/" + rel.rsplit("/", 1)[1]
    if rel.startswith("src/test/native/"):
        return f"test/jdk/jdk/internal/m3/{AREA}/native/" + rel.rsplit("/", 1)[1]
    if KEEP_POM and not rel.startswith("src/") or KEEP_POM and rel in BRIDGE:
        return f"make/m3/{AREA}/" + rel
    if rel.startswith("recipe/"):
        parts = rel.split("/")
        return f"make/m3/evidence/{AREA}/recipe/" + "/".join(parts[2:])
    if rel == "NOTICE":
        return f"src/java.base/share/legal/m3{AREA}.md"
    if rel in ("pom.xml", "src/main/java/module-info.java"):
        return None
    sys.exit(f"REFUSE unmapped path {rel}")


def bridge_pom(pom: str) -> str:
    """--keep-pom: the area's own pom stays; one build-helper plugin adds the moved main and test package directories."""
    rel = "${project.basedir}/../../.."

    def goal(eid, gname, path):
        return (f"          <execution>\n            <id>{eid}</id>\n            <phase>generate-sources</phase>\n"
                f"            <goals>\n              <goal>{gname}</goal>\n            </goals>\n"
                f"            <configuration>\n              <sources>\n                <source>{rel}/{path}</source>\n"
                f"              </sources>\n            </configuration>\n          </execution>\n")
    plugin = ("      <!-- MOVE_TO_JDK_MODULE v1.1 (keep-pom mode, make/m3/tooling/jdk-naming/move_area.py): the sources live in\n"
              f"           java.base (jdk.internal.m3.{AREA}) and the tests in test/jdk; this pom keeps its own configuration. -->\n"
              "      <plugin>\n        <groupId>org.codehaus.mojo</groupId>\n        <artifactId>build-helper-maven-plugin</artifactId>\n"
              "        <version>3.6.1</version>\n        <executions>\n"
              + goal("jdk-module-sources", "add-source", f"src/java.base/share/classes/jdk/internal/m3/{AREA}")
              + goal("jdk-module-test-sources", "add-test-source", f"test/jdk/jdk/internal/m3/{AREA}")
              + "        </executions>\n      </plugin>\n")
    at = pom.index("<plugins>") + len("<plugins>\n")
    return pom[:at] + plugin + pom[at:]


seen = {}
for f in sorted(p for p in SRC.rglob("*") if p.is_file()):
    rel = f.relative_to(SRC).as_posix()
    dst = target(rel)
    if dst is not None and seen.setdefault(dst, rel) != rel:
        sys.exit(f"REFUSE target collision {dst}: {seen[dst]} and {rel}")
    if dst is None:
        print(f"m3/{AREA}/{rel}\tREMOVED\t")
        continue
    data = f.read_bytes()
    applied = []
    if rel.endswith((".java", ".c", ".h")) and not rel.startswith("recipe/") or KEEP_POM and rel == "pom.xml":
        text = data.decode("utf-8")
        for rx, rep in RULES:
            if rep is None:  # slash form: keep a leading slash
                rep = f"jdk/internal/m3/{AREA}"
                text, n = re.subn(rx, lambda m: ("/" if m.group(0).startswith("/") else "") + f"jdk/internal/m3/{AREA}", text)
            else:
                text, n = re.subn(rx, rep, text)
            if n:
                applied.append(f"{rep}x{n}")
        if rel.startswith("src/test/"):
            # An internal java.base package is not unconditionally exported, so publicLookup() cannot
            # resolve its types; the same-package test helpers take a full-privilege lookup() instead.
            text, n = re.subn(r"MethodHandles\.publicLookup\(\)", "MethodHandles.lookup()", text)
            if n:
                applied.append(f"publicLookup->lookup x{n}")
        if rel == "pom.xml":
            text = bridge_pom(text)
            applied.append("bridge-pom")
        data = text.encode("utf-8")
    if rel == "src/main/native/CMakeLists.txt":
        text = data.decode("utf-8")
        src = f"${{CMAKE_CURRENT_SOURCE_DIR}}/../../../src/java.base/share/native/libm3{AREA}"
        tst = f"${{CMAKE_CURRENT_SOURCE_DIR}}/../../../test/jdk/jdk/internal/m3/{AREA}/native"
        text = re.sub(r"(?<![/\w])(m3_\w+\.c)(?=[\s)])", lambda m: f"{src}/{m.group(1)}", text)
        text = text.replace(f"{src}/tests/", f"{tst}/").replace("tests/m3_", f"{tst}/m3_")
        data = text.encode("utf-8")
        applied.append("cmake-paths")
    out = OUT / dst
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(data)
    print(f"m3/{AREA}/{rel}\t{dst}\t{';'.join(applied)}")
