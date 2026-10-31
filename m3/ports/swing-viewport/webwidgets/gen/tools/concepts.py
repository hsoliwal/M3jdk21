# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
"""Build CONCEPTS.tsv (104 web widget concepts x Nebula/SWT/Swing/SwingX/M3 port) and COMPONENT_CONCEPTS.tsv
(every harvested kit component name -> concept) from the pinned 69-kit harvest. CPU only, deterministic.

usage: concepts.py INPUTS_DIR MEMBERS.tsv OUT_DIR
INPUTS_DIR holds (from com.synexia d278d4c1c02 docs/M3-SCALE/runs/20261013-all-widget-viewport):
  universe_concepts.py, UNIVERSE_CONCEPTS.tsv, SUPERSET_WIDGET_MAP.tsv, UNIVERSE_COMPONENTS.tsv
plus CPU name lists: NEBULA_TREE.txt (git ls-tree of hsoliwal/nebula master widgets/), SWT_JAR.txt (unzip -Z1 of the
SWT jar), JDK_SWING.txt (javax.swing J* + AWT names from jdk-21 src.zip), SWINGX.txt (swingx-all 94f86aab class names).
The alias table and matcher are reused verbatim from universe_concepts.py (exec of the text before main()).
"""
import collections, os, sys

# Tier 2 (2026-10-30): exact-word aliases for names the verbatim table leaves unmatched; widget words only.
EXTRA = {"label": "text truncate display", "toolbar": "strip actions bar tool", "layout-primitive": "form forms layout view page content footer area canvas element control shell wrap hidden slot primitive main bleed",
         "text-input": "field validation mask", "list": "item items collection transfer iterator listy", "scroll": "scroll", 
         "web-view": "html", "color-picker": "color", "config-provider": "theme style css baseline global", "focus-trap": "focus away",
         "toggle-button": "toggle", "progress": "indicator", "skeleton": "placeholder", "code-block": "code", "stepper": "step",
         "time-picker": "time", "date-picker": "date", "virtual-list": "virtual sliver", "affix": "top", "rating": "rate",
         "alert-banner": "info", "popover": "floating popconfirm arrow", "table": "cell descriptions", "divider": "line", "empty-state": "result",
         "segmented-bar": "segment", "search": "filters filter", "splitter": "grip split", "message-list": "bubble chat", "spinner-loading": "loader spin",
         "number-input": "number", "select": "options option", "navigation": "route router", "panel": "section", "gallery": "masonry",
         "autocomplete": "mentions mention", "markdown-render": "blockquote", "file-upload": "download", "card": "paper surface",
         "transition-anim": "grow slide zoom fade ripple ink effect parallax presence show beam", "kbd": "hotkey shortcut",
         "carousel": "marquee", "accordion": "spoiler", "badge": "counter status", "toast": "announce sonner flash", "hover-card": "hover",
         "infinite-scroll": "lazy", "fab": "float", "map": "marker", "icon": "octicon", "dialog": "window interstitial confirm", "reorder-drag": "drag", "image": "icons thumbnail", "file-upload": "download attachment", "empty-state": "result blankslate", "splitter": "grip split resizer", "scroll": "scroll scroller", "popover": "floating popconfirm arrow dismissable layer", "heading": "pagehead"}
# Tier 3: infrastructure words; a name made of these is a non-widget support type (events, bindings, converters, ...).
NON_WIDGET = set("""event events args handler converter controller binding bindable manager utils util helper helpers service observer listener model
presenter builder definition attribute property extensions extension platform native type data state changed changing base to use mode value
direction orientation alignment configuration config context scope source template component widget renderer render format name ui jx i v
accessibility device system responsive relative position size font check edit gesture touchable recognizer click brush shadow border gradient
visual app locale version environment client only no ssr init script provider portal key keys of for with on is in at by""".split())
FAMILY_ORDER = ["TOKEN", "CONTAINER", "OVERLAY", "ANIMVALUE", "SEGMENTS", "COMMANDS", "INPUT", "PICKER", "COLLECTION", "PLOT", "EMBED"]
STATUS_RANK = {"gap": 0, "primitive": 1, "rare": 2, "style-bit": 3}


def load_matcher(path):
    src = open(path, encoding="utf-8").read()
    ns = {}
    exec(compile(src[:src.index("def main():")], path, "exec"), ns)
    return ns["CONCEPTS"], ns["tokens"]


def tsv(path):
    rows = [l.rstrip("\n").split("\t") for l in open(path, encoding="utf-8")]
    return [dict(zip(rows[0], r + [""] * (len(rows[0]) - len(r)))) for r in rows[1:]]


def best_concept(name, aliases, tokens):
    words, joined = tokens(name)
    hits = []
    for c, al in aliases.items():
        for a in al.split():
            if a in words or (len(a) >= 5 and a in joined):
                hits.append((len(a), c))
    hits.sort(key=lambda h: (-h[0], h[1]))
    if hits:
        return [c for _, c in hits], "T1_ALIAS"
    tier2 = sorted({c for c, al in EXTRA.items() for a in al.split() if a in words})
    if tier2:
        return tier2, "T2_WORD"
    return ([], "T3_NON_WIDGET") if words & NON_WIDGET else ([], "UNMAPPED")


def verify(existing, nebula, swt, jdk, m3port=frozenset()):
    if not existing or existing == "-":
        return ""
    parts = existing.split()
    kit, fq = parts[0], parts[1].split("#")[0]
    simple = fq.split(".")[-1]
    if kit == "SWT":
        return "OK" if fq.replace(".", "/") + ".class" in swt else "MISSING"
    if kit == "Nebula":
        return "OK" if fq.replace(".", "/") + ".java" in nebula else "MISSING"
    if kit == "M3port":
        return "OK" if fq in m3port else "MISSING"
    if kit in ("Swing", "AWT"):
        return "OK" if simple in jdk else "MISSING"
    return "UNCHECKED"


def main():
    inp, members_path, out = sys.argv[1:4]
    aliases, tokens = load_matcher(os.path.join(inp, "universe_concepts.py"))
    uni = {r["concept"]: r for r in tsv(os.path.join(inp, "UNIVERSE_CONCEPTS.tsv"))}
    smap = {r["concept"]: r for r in tsv(os.path.join(inp, "SUPERSET_WIDGET_MAP.tsv"))}
    members = {r["concept"]: r for r in tsv(members_path)}
    nebula = {l.strip().split("/src/", 1)[-1] for l in open(os.path.join(inp, "NEBULA_TREE.txt")) if "/src/" in l}
    swt = {l.strip() for l in open(os.path.join(inp, "SWT_JAR.txt"))}
    jdk = set(open(os.path.join(inp, "JDK_SWING.txt")).read().split())
    m3port = {l.strip() for l in open(os.path.join(inp, "M3PORT.txt"))}
    swingx = sorted(set(open(os.path.join(inp, "SWINGX.txt")).read().split()))
    comps = tsv(os.path.join(inp, "UNIVERSE_COMPONENTS.tsv"))
    per = collections.Counter()
    with open(os.path.join(out, "COMPONENT_CONCEPTS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("repo\tcomponent\tpath\tconcept\tfamily\tall_concepts\n")
        for r in comps:
            cs, tier = best_concept(r["component"], aliases, tokens)
            c = cs[0] if cs else ("NON_WIDGET" if tier == "T3_NON_WIDGET" else "UNMAPPED")
            per[c] += 1
            fam = members[c]["family"] if c in members else ""
            w.write("\t".join([r["repo"], r["component"], r["path"], c, fam, tier, ";".join(dict.fromkeys(cs))]) + "\n")
    order = sorted(members, key=lambda c: (STATUS_RANK.get(smap[c]["home"], 4), -int(smap[c]["kits"] or 0), c))
    cols = ["rank", "concept", "home", "kits", "component_names", "family", "pattern", "mode", "role", "nebula_existing", "nebula_verified",
            "nebula_decision", "swing_existing", "swing_verified", "swing_decision", "swingx_pattern", "m3_class", "jdk_swing_names",
            "swt_names", "nebula_names", "needs_input_events", "viewport_form"]
    with open(os.path.join(out, "CONCEPTS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("\t".join(cols) + "\n")
        for i, c in enumerate(order, 1):
            m, s, u = members[c], smap[c], uni.get(c, {})
            sx = [x for x in swingx if any(a in tokens(x)[0] or (len(a) >= 5 and a in tokens(x)[1]) for a in aliases[c].split())]
            ne, se = m["nebula_existing"], m["swing_existing"]
            row = [str(i), c, s["home"], s["kits"], str(per[c]), m["family"], "M3P-%s-1" % m["family"], m["mode"], m["role"],
                   ne, verify(ne, nebula, swt, jdk), "SKIP_EXISTING" if ne else "GENERATE", se, verify(se, nebula, swt, jdk, m3port),
                   "SKIP_EXISTING" if se else "GENERATE", ";".join(sx[:4]),
                   "M3" + "".join(p.capitalize() for p in c.split("-")), u.get("swing", ""), s["swt_class"][:80], s["nebula_class"][:80],
                   s["needs_input_events"], s["superset_form"]]
            w.write("\t".join(row) + "\n")
    mapped = sum(v for k, v in per.items() if k != "UNMAPPED")
    print("concepts", len(order), "components", len(comps), "mapped", mapped - per["NON_WIDGET"], "non_widget", per["NON_WIDGET"], "unmapped", per["UNMAPPED"])
    print("families", dict(collections.Counter(members[c]["family"] for c in order)))
    print("nebula GENERATE", sum(1 for c in order if not members[c]["nebula_existing"]), "swing GENERATE",
          sum(1 for c in order if not members[c]["swing_existing"]))


main()
