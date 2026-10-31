# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# pom_model.awk: lexical pom.xml model extractor (no XML library, no Maven, no network).
# RS="<" tokenizes each file into tags; an element stack gives each value its path.
# Usage: awk -f pom_model.awk <pom.xml>...   (many files in one run; each record starts with the pom path)
# Output (tab separated; field 1 = pom path as given, field 3 = profile id, empty = default build):
#   COORD   prof  groupId artifactId version packaging
#   PARENT  prof  groupId artifactId version relativePath
#   PROP    prof  name value
#   MODULE  prof  module
#   DEP     prof  section groupId artifactId version scope type systemPath optional
#           section: dependencies | dependencyManagement | plugin:<pluginArtifactId> | other:<path>
#   PLUGIN  prof  section groupId artifactId version          section: plugins | pluginManagement | reporting
#   EXT     prof  groupId artifactId version
#   REPO    prof  kind id url                                 kind: repository | pluginRepository
#   SRC     prof  kind path                                   kind: sourceDirectory | testSourceDirectory |
#                                                             outputDirectory | add-source | add-test-source | resource | testResource
BEGIN { RS = "<"; T = "\t"; depth = 0; incomment = 0; prof = "" }
FNR == 1 { if (NR > 1) flush(); F = FILENAME; depth = 0; incomment = 0; prof = ""; split("", coord); split("", par) }
{
  gsub(/\r/, "")
  rec = $0
  if (incomment) { if (index(rec, "-->")) incomment = 0; next }
  if (rec ~ /^!--/) { if (!index(rec, "-->")) incomment = 1; next }
  if (rec ~ /^[?!]/ || rec == "") next
  if (substr(rec, 1, 1) == "/") {               # closing tag
    name = substr(rec, 2); sub(/>.*/, "", name); gsub(/[ \t\n]/, "", name)
    close_tag(name)
    if (depth > 0) depth--
    next
  }
  gt = index(rec, ">"); if (!gt) next
  head = substr(rec, 1, gt - 1); text = substr(rec, gt + 1)
  selfclose = (head ~ /\/$/)
  name = head; sub(/[ \t\n\/].*/, "", name)
  gsub(/^[ \t\n]+|[ \t\n]+$/, "", text)
  gsub(/[\t\n]+/, " ", text)
  depth++; stack[depth] = name; val[depth] = text
  open_tag(name)
  if (selfclose) { close_tag(name); depth-- }
}
function emit(s) { print F T s }
function path(   i, p) { p = stack[1]; for (i = 2; i <= depth; i++) p = p "/" stack[i]; return p }
function parent_is(n) { return depth >= 2 && stack[depth - 1] == n }
function has(n,   i) { for (i = 1; i < depth; i++) if (stack[i] == n) return 1; return 0 }
function base(p) {        # strip the profile prefix so the same rules apply inside a profile
  sub(/^project\/profiles\/profile\//, "project/", p); return p
}
function open_tag(n) {
  if (n == "dependency") { dg = da = dv = ds = dt = dsp = dopt = "" }
  else if (n == "plugin") { pg = pa = pv = "" }
  else if (n == "extension") { xg = xa = xv = "" }
  else if (n == "repository" || n == "pluginRepository") { rid = rurl = "" }
  else if (n == "execution") { ngoal = 0; nsrc = 0 }
  else if (n == "profile") { prof = "" }
}
function close_tag(n,   p, v, sec, k, i, bp) {
  p = path(); v = val[depth]; bp = base(p)
  if (p == "project/profiles/profile/id") { prof = v; return }
  if (n == "profile" && p == "project/profiles/profile") { prof = ""; return }
  if (parent_is("dependency")) {
    if (n == "groupId") dg = v; else if (n == "artifactId") da = v; else if (n == "version") dv = v
    else if (n == "scope") ds = v; else if (n == "type") dt = v; else if (n == "systemPath") dsp = v
    else if (n == "optional") dopt = v
    return
  }
  if (n == "dependency") {
    if (bp ~ /^project\/dependencies\/dependency$/) sec = "dependencies"
    else if (bp ~ /^project\/dependencyManagement\/dependencies\/dependency$/) sec = "dependencyManagement"
    else if (bp ~ /\/plugin\/dependencies\/dependency$/) sec = "plugin:" pa
    else sec = "other:" bp
    emit("DEP" T prof T sec T dg T da T dv T ds T dt T dsp T dopt)
    return
  }
  if (parent_is("plugin") && !has("dependencies") && !has("executions") && !has("configuration")) {
    if (n == "groupId") pg = v; else if (n == "artifactId") pa = v; else if (n == "version") pv = v
    return
  }
  if (n == "plugin") {
    if (bp ~ /^project\/build\/plugins\/plugin$/) sec = "plugins"
    else if (bp ~ /^project\/build\/pluginManagement\/plugins\/plugin$/) sec = "pluginManagement"
    else if (bp ~ /^project\/reporting\/plugins\/plugin$/) sec = "reporting"
    else sec = "other:" bp
    emit("PLUGIN" T prof T sec T (pg == "" ? "org.apache.maven.plugins" : pg) T pa T pv)
    return
  }
  if (parent_is("extension")) {
    if (n == "groupId") xg = v; else if (n == "artifactId") xa = v; else if (n == "version") xv = v
    return
  }
  if (n == "extension" && bp ~ /^project\/build\/extensions\/extension$/) { emit("EXT" T prof T xg T xa T xv); return }
  if (parent_is("repository") || parent_is("pluginRepository")) {
    if (n == "id") rid = v; else if (n == "url") rurl = v
    return
  }
  if (n == "repository" || n == "pluginRepository") { emit("REPO" T prof T n T rid T rurl); return }
  if (bp ~ /^project\/(groupId|artifactId|version|packaging)$/) { coord[n] = v; return }
  if (bp ~ /^project\/parent\/(groupId|artifactId|version|relativePath)$/) { par[n] = v; return }
  if (bp ~ /^project\/properties\/[^\/]+$/) { emit("PROP" T prof T n T v); return }
  if (bp ~ /^project\/modules\/module$/) { emit("MODULE" T prof T v); return }
  if (bp ~ /^project\/build\/(sourceDirectory|testSourceDirectory|outputDirectory)$/) { emit("SRC" T prof T n T v); return }
  if (bp ~ /^project\/build\/resources\/resource\/directory$/) { emit("SRC" T prof T "resource" T v); return }
  if (bp ~ /^project\/build\/testResources\/testResource\/directory$/) { emit("SRC" T prof T "testResource" T v); return }
  if (n == "goal" && parent_is("goals") && has("execution")) { goal[++ngoal] = v; return }
  if (n == "source" && parent_is("sources") && has("execution") && pa == "build-helper-maven-plugin") { src[++nsrc] = v; return }
  if (n == "execution" && nsrc > 0) {
    k = "add-source"; for (i = 1; i <= ngoal; i++) if (goal[i] == "add-test-source") k = "add-test-source"
    for (i = 1; i <= nsrc; i++) emit("SRC" T prof T k T src[i])
    nsrc = 0; return
  }
}
END { if (NR > 0) flush() }
function flush() {
  emit("COORD" T "" T coord["groupId"] T coord["artifactId"] T coord["version"] T (coord["packaging"] == "" ? "jar" : coord["packaging"]))
  if (par["artifactId"] != "") emit("PARENT" T "" T par["groupId"] T par["artifactId"] T par["version"] T (("relativePath" in par) ? par["relativePath"] : "../pom.xml"))
}
