# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# pom_resolve.awk: effective model + in-repo provider classification over MODEL.tsv
# (MODEL.tsv = "<pom path relative to repo>\t<pom_model.awk record>", one line per record).
# Usage: awk -f pom_resolve.awk -v OUT=<dir> -v ROOT=m3/pom.xml MODEL.tsv
# Writes into OUT:
#   GAV.tsv        pom  groupId  artifactId  version  packaging  local_parent_pom
#   REACTOR.tsv    pom  profile  packaging  groupId  artifactId  version
#   REFS.tsv       module_pom  profile  via  kind  scope  groupId  artifactId  version  class  provider_pom  note
#                  class: REACTOR | REPO | REPO_DRIFT | EXTERNAL | UNRESOLVED | JDK | REPO_PATH | OUTSIDE_PATH
#   EDGES.tsv      unit_pom  phase(main|test)  provider_pom  class      (default build only, transitive)
#   UNITS.tsv      unit_pom  role(reactor|provider)  packaging  postorder
# Classification is by exact groupId:artifactId:version against every pom held on disk; REPO_DRIFT means the
# groupId:artifactId exists in the repository only under another version (Maven would resolve it outside).
BEGIN {
  FS = OFS = "\t"
  if (EVIDENCE == "") EVIDENCE = "/(META-INF|qualification|verification|retained-variants|upstream-excluded|evidence|snapshots|receipts|history|template-history|archive-maven-projects|tasks|\\.superset|\\.merge-preserved|\\.m3)/"
}
{
  pom = $1; t = $2; pr = $3
  if (!(pom in seen)) { seen[pom] = 1; poms[++npoms] = pom }
  if (t == "COORD") { cg[pom] = $4; ca[pom] = $5; cv[pom] = $6; cp[pom] = $7 }
  else if (t == "PARENT") { pg[pom] = $4; pa[pom] = $5; pv[pom] = $6; prel[pom] = $7 }
  else if (t == "PROP") { if (!((pom SUBSEP pr SUBSEP $4) in prop)) prop[pom, pr, $4] = $5 }
  else if (t == "MODULE") { nm[pom]++; mod[pom, nm[pom]] = $4; modp[pom, nm[pom]] = pr }
  else if (t == "DEP" || t == "PLUGIN" || t == "EXT" || t == "REPO" || t == "SRC") { nr[pom]++; r[pom, nr[pom]] = $0 }
}
function dirof(p) { if (p !~ /\//) return "."; sub(/\/[^\/]*$/, "", p); return p }
function norm(p,   n, a, i, o, k, s) {
  gsub(/\\/, "/", p); n = split(p, a, "/"); k = 0
  for (i = 1; i <= n; i++) {
    if (a[i] == "" || a[i] == ".") continue
    if (a[i] == "..") { if (k > 0 && o[k] != "..") k--; else o[++k] = ".."; continue }
    o[++k] = a[i]
  }
  s = ""; for (i = 1; i <= k; i++) s = s (i > 1 ? "/" : "") o[i]
  return s
}
function asPom(p) { p = norm(p); if (p !~ /\.xml$/) p = p (p == "" ? "" : "/") "pom.xml"; return p }
function parentPom(pom,   c) {
  if (!(pom in pa) || prel[pom] == "") return ""
  c = asPom(dirof(pom) "/" prel[pom])
  if ((c in seen) && ca[c] == pa[pom]) return c
  return ""
}
function effG(pom) { return cg[pom] != "" ? cg[pom] : pg[pom] }
function effV(pom) { return cv[pom] != "" ? cv[pom] : pv[pom] }
function lookupProp(pom, pr, k,   q, d) {
  for (q = pom; q != ""; q = lp[q]) {
    if (pr != "" && ((q SUBSEP pr SUBSEP k) in prop)) return prop[q, pr, k]
    if ((q SUBSEP "" SUBSEP k) in prop) return prop[q, "", k]
  }
  return "\001"
}
function builtin(pom, k) {
  if (k == "project.version" || k == "pom.version" || k == "version") return effV(pom)
  if (k == "project.groupId" || k == "pom.groupId") return effG(pom)
  if (k == "project.artifactId") return ca[pom]
  if (k == "project.parent.version") return pv[pom]
  if (k == "project.parent.groupId") return pg[pom]
  if (k == "project.basedir" || k == "basedir") return dirof(pom)
  if (k == "project.build.directory") return dirof(pom) "/target"
  if (k == "maven.multiModuleProjectDirectory") return dirof(ROOT)
  return "\001"
}
function res(s, pom, pr,   i, m, k, v, pre, post) {
  for (i = 0; i < 12 && match(s, /\$\{[^}]+\}/); i++) {
    k = substr(s, RSTART + 2, RLENGTH - 3); pre = substr(s, 1, RSTART - 1); post = substr(s, RSTART + RLENGTH)
    v = builtin(pom, k); if (v == "\001") v = lookupProp(pom, pr, k)
    if (v == "\001") { s = pre "\002{" k "}" post; continue }
    s = pre v post
  }
  gsub(/\002/, "$", s); return s
}
function walk(pom, pr, depth,   i, c, p2) {
  if ((pom SUBSEP pr) in inreactor) return
  inreactor[pom, pr] = 1; if (!(pom in reactorProf)) reactorProf[pom] = pr
  if (cp[pom] != "pom") { if (!(pom in isReactor)) { isReactor[pom] = 1; reactorList[++nreactor] = pom } }
  print pom, pr, cp[pom], effG(pom), ca[pom], res(effV(pom), pom, "") > (OUT "/REACTOR.tsv")
  for (i = 1; i <= nm[pom]; i++) {
    c = asPom(dirof(pom) "/" mod[pom, i])
    if (!(c in seen)) { print pom, modp[pom, i], "module", "module", "", "", mod[pom, i], "", "MISSING_MODULE", c, "module pom not on disk" > (OUT "/REFS.tsv"); continue }
    p2 = modp[pom, i] != "" ? modp[pom, i] : pr
    walk(c, p2, depth + 1)
  }
}
function managed(pom, g, a, kind,   q, i, f) {    # version from dependencyManagement / pluginManagement chain
  for (q = pom; q != ""; q = lp[q])
    for (i = 1; i <= nr[q]; i++) {
      split(r[q, i], f, "\t")
      if (f[3] != "") continue
      if (kind == "dep" && f[2] == "DEP" && f[4] == "dependencyManagement" && f[5] == g && f[6] == a) return res(f[7], q, "")
      if (kind == "plugin" && f[2] == "PLUGIN" && f[4] == "pluginManagement" && f[5] == g && f[6] == a) return res(f[7], q, "")
    }
  return ""
}
function classify(g, a, v,   k) {    # sets CLS, PROV, NOTE
  NOTE = ""; PROV = ""
  if (g ~ /\$\{/ || a ~ /\$\{/ || v ~ /\$\{/) { CLS = "UNRESOLVED"; NOTE = "unresolved property"; return }
  k = g ":" a ":" v
  if (k in gav) { PROV = gav[k]; CLS = (PROV in isReactor) ? "REACTOR" : "REPO"; return }
  if ((g ":" a) in gaVers) {
    CLS = "REPO_DRIFT"; NOTE = "repo holds " gaVers[g ":" a]
    if ((k "-m3-source-1") in gav) { PROV = gav[k "-m3-source-1"]; NOTE = NOTE "; nearest " v "-m3-source-1" }
    else PROV = gaBest[g ":" a]
    return
  }
  CLS = "EXTERNAL"
}
function emitRef(mpom, pr, via, kind, scope, g, a, v, note2) {
  classify(g, a, v)
  print mpom, pr, via, kind, scope, g, a, v, CLS, PROV, (NOTE (note2 != "" ? (NOTE != "" ? "; " : "") note2 : "")) > (OUT "/REFS.tsv")
}
function pathClass(p, pom, pr,   v) {
  v = res(p, pom, pr)
  if (v ~ /\$\{java\.home\}/ || v ~ /^\$\{java\.home/) return "JDK"
  if (v ~ /\$\{(user\.home|env\.)/ || v ~ /^([A-Za-z]:|\/|~)/) return "OUTSIDE_PATH"
  if (v ~ /\$\{/) return "UNRESOLVED"
  if (norm(v) ~ /^\.\.(\/|$)/) return "OUTSIDE_PATH"
  return "REPO_PATH"
}
# direct + inherited references of one reactor module
function refsOf(m,   q, i, f, via, g, a, v, sc, pc, kind, kk) {
  for (q = m; q != ""; q = lp[q]) {
    via = (q == m) ? "direct" : ("inherited:" q)
    for (i = 1; i <= nr[q]; i++) {
      split(r[q, i], f, "\t")
      if (f[2] == "DEP") {
        if (f[4] == "dependencyManagement" && f[8] != "import") continue
        g = res(f[5], q, f[3]); a = res(f[6], q, f[3]); v = res(f[7], q, f[3])
        if (v == "" && f[4] == "dependencies") v = managed(m, g, a, "dep")
        sc = f[8]; if (sc == "" && f[4] == "dependencies") sc = "compile"
        kind = (f[4] == "dependencies") ? "dependency" : (f[4] == "dependencyManagement" ? "import" : (f[4] ~ /^plugin:/ ? "plugin-dependency" : f[4]))
        if (f[4] ~ /^plugin:/) sc = f[4]
        if (sc == "system") { pc = pathClass(f[10], q, f[3]); print m, f[3], via, "systemPath", sc, g, a, v, pc, "", res(f[10], q, f[3]) > (OUT "/REFS.tsv"); continue }
        emitRef(m, f[3], via, kind, sc, g, a, v, "")
        if (f[3] == "" && f[4] == "dependencies") { nd[m]++; dd[m, nd[m]] = g SUBSEP a SUBSEP v SUBSEP sc SUBSEP f[11] }
      } else if (f[2] == "PLUGIN") {
        if (f[4] == "pluginManagement") continue
        g = res(f[5], q, f[3]); a = res(f[6], q, f[3]); v = res(f[7], q, f[3])
        if (v == "") v = managed(m, g, a, "plugin")
        emitRef(m, f[3], via, "plugin", f[4], g, a, v, (v == "" ? "no version" : ""))
        if (f[3] == "") usedPlugin[m, g ":" a] = 1
      } else if (f[2] == "EXT") {
        emitRef(m, f[3], via, "extension", "build", res(f[4], q, f[3]), res(f[5], q, f[3]), res(f[6], q, f[3]), "")
      } else if (f[2] == "REPO") {
        print m, f[3], via, f[4], "", f[5], "", "", "EXTERNAL", "", f[6] > (OUT "/REFS.tsv")
      } else if (f[2] == "SRC" && q == m) {
        pc = pathClass(f[5], q, f[3])
        if (pc != "REPO_PATH") print m, f[3], via, "path:" f[4], "", "", "", "", pc, "", res(f[5], q, f[3]) > (OUT "/REFS.tsv")
      }
    }
  }
  for (q = m; q != ""; q = lp[q]) if (pa[q] != "" && lp[q] == "")
    emitRef(m, "", (q == m ? "direct" : "inherited:" q), "parent", "", pg[q], pa[q], pv[q], "parent not reachable by relativePath")
  if (cp[m] == "jar") {                 # lifecycle plugins Maven binds to jar packaging without a declaration
    split("maven-resources-plugin maven-compiler-plugin maven-surefire-plugin maven-jar-plugin maven-install-plugin maven-deploy-plugin", lc, " ")
    for (kk = 1; kk <= 6; kk++) if (!((m SUBSEP "org.apache.maven.plugins:" lc[kk]) in usedPlugin))
      print m, "", "implicit", "implicit-plugin", "jar", "org.apache.maven.plugins", lc[kk], "", "EXTERNAL", "", "bound by packaging jar" > (OUT "/REFS.tsv")
  }
}
# effective default-build dependency list of any pom (own + inherited), for the transitive closure
function depsOf(m,   q, i, f, g, a, v, sc, n) {
  if (m in depsDone) return; depsDone[m] = 1; n = 0
  for (q = m; q != ""; q = lp[q])
    for (i = 1; i <= nr[q]; i++) {
      split(r[q, i], f, "\t")
      if (f[2] != "DEP" || f[3] != "" || f[4] != "dependencies") continue
      g = res(f[5], q, ""); a = res(f[6], q, ""); v = res(f[7], q, "")
      if (v == "") v = managed(m, g, a, "dep")
      sc = f[8] == "" ? "compile" : f[8]
      n++; tdg[m, n] = g; tda[m, n] = a; tdv[m, n] = v; tds[m, n] = sc; tdo[m, n] = f[11]
    }
  tdn[m] = n
}
# closure: phase main = compile+provided(+system) of m and compile(+runtime for test) transitively
function closure(root, m, phase, chain, depth,   i, g, a, v, sc, k, c, quiet) {
  quiet = (unitRole[root] != "reactor")
  if (depth > 20) return
  depsOf(m)
  for (i = 1; i <= tdn[m]; i++) {
    g = tdg[m, i]; a = tda[m, i]; v = tdv[m, i]; sc = tds[m, i]
    if (depth > 0 && (sc == "test" || sc == "provided" || tdo[m, i] == "true")) continue
    if (phase == "main" && (sc == "test" || sc == "runtime")) continue
    if (sc == "system" || sc == "import") continue
    classify(g, a, v)
    if (depth > 0 && !quiet) print root, "", "transitive:" chain, "dependency", (phase == "main" ? sc : "test/" sc), g, a, v, CLS, PROV, NOTE > (OUT "/REFS.tsv")
    if (CLS != "REACTOR" && CLS != "REPO" && CLS != "REPO_DRIFT") continue
    c = PROV; k = root SUBSEP phase SUBSEP c
    if (k in edge) continue
    edge[k] = 1; print root, phase, c, CLS > (OUT "/EDGES.tsv")
    if (!(c in isUnit)) { isUnit[c] = 1; unitList[++nunits] = c; unitRole[c] = "provider" }
    closure(root, c, phase, (chain == "" ? "" : chain ">") ca[c], depth + 1)
  }
}
function post(u,   i, k) {      # postorder over EDGES (main phase) for compile order
  if (u in visited) return; visited[u] = 1
  for (i = 1; i <= nunits; i++) { k = u SUBSEP "main" SUBSEP unitList[i]; if (k in edge) post(unitList[i]) }
  for (i = 1; i <= nunits; i++) { k = u SUBSEP "test" SUBSEP unitList[i]; if (k in edge) post(unitList[i]) }
  order[++norder] = u
}
END {
  for (i = 1; i <= npoms; i++) { p = poms[i]; lp[p] = parentPom(p); parentPomOf[p] = lp[p] }
  for (i = 1; i <= npoms; i++) {
    p = poms[i]; g = res(effG(p), p, ""); a = ca[p]; v = res(effV(p), p, "")
    print p, g, a, v, cp[p], lp[p] > (OUT "/GAV.tsv")
    k = g ":" a ":" v
    if (p ~ EVIDENCE) continue        # evidence copies (ledgers, snapshots, retained variants) never provide
    if (!(k in gav)) gav[k] = p
    if (!((g ":" a SUBSEP v) in gaHas)) { gaHas[g ":" a, v] = 1; gaVers[g ":" a] = (gaVers[g ":" a] == "" ? "" : gaVers[g ":" a] ",") v
      if (v > gaBestV[g ":" a]) { gaBestV[g ":" a] = v; gaBest[g ":" a] = p } }
  }
  walk(ROOT, "", 0)
  for (i = 1; i <= nreactor; i++) { m = reactorList[i]; isUnit[m] = 1; unitList[++nunits] = m; unitRole[m] = "reactor" }
  for (i = 1; i <= nreactor; i++) refsOf(reactorList[i])
  for (i = 1; i <= nreactor; i++) { m = reactorList[i]; closure(m, m, "main", ca[m], 0); closure(m, m, "test", ca[m], 0) }
  # extra roots (EXTRA="pom ..."): any in-repo module compiled with its own in-repo closure, e.g. a repoint consumer
  nx = split(EXTRA, xs, " ")
  for (i = 1; i <= nx; i++) { x = xs[i]; if (!(x in seen)) continue; if (!(x in isUnit)) { isUnit[x] = 1; unitList[++nunits] = x; unitRole[x] = "extra" }; closure(x, x, "main", ca[x], 0) }
  # providers need their own main closure for compilation
  for (j = 1; j <= nunits; j++) if (unitRole[unitList[j]] == "provider") closure(unitList[j], unitList[j], "main", ca[unitList[j]], 0)
  # test runtime tooling: the JUnit Platform launcher matching the platform engine each reactor test closure holds
  for (k in edge) {
    split(k, kk, SUBSEP); c = kk[3]
    if (kk[2] != "test" || ca[c] != "junit-platform-engine") continue
    lk = "org.junit.platform:junit-platform-launcher:" res(effV(c), c, "")
    if (!(lk in gav)) { print kk[1], "", "test-runtime", "launcher", "test", "org.junit.platform", "junit-platform-launcher", res(effV(c), c, ""), "EXTERNAL", "", "no in-repo launcher for the platform engine" > (OUT "/REFS.tsv"); continue }
    L = gav[lk]; launcher[kk[1]] = L
    if (!(L in isUnit)) { isUnit[L] = 1; unitList[++nunits] = L; unitRole[L] = "provider" }
  }
  for (u in launcher) { print u, "launcher", launcher[u], "TOOL" > (OUT "/EDGES.tsv"); closure(launcher[u], launcher[u], "main", ca[launcher[u]], 0) }
  for (i = 1; i <= nunits; i++) post(unitList[i])
  for (i = 1; i <= norder; i++) {
    u = order[i]; enc = lookupProp(u, "", "project.build.sourceEncoding"); enc = (enc == "\001") ? "UTF-8" : res(enc, u, "")
    rel = lookupProp(u, "", "maven.compiler.release"); if (rel == "\001") rel = lookupProp(u, "", "maven.compiler.source")
    rel = (rel == "\001") ? "" : res(rel, u, "")
    print u, unitRole[u], cp[u], i, reactorProf[u], enc, rel > (OUT "/UNITS.tsv")
  }
  # resolved source roots (default build) of every unit
  for (i = 1; i <= norder; i++) {
    u = order[i]
    for (j = 1; j <= nr[u]; j++) {
      split(r[u, j], f, "\t")
      if (f[2] != "SRC" || f[3] != "") continue
      v = res(f[5], u, "")
      if (v !~ /^(\$|[A-Za-z]:|\/)/ && index(v, dirof(u) "/") != 1 && v != dirof(u)) v = dirof(u) "/" v
      print u, f[4], norm(v) > (OUT "/SOURCES.tsv")
    }
  }
}
