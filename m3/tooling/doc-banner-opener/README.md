<!-- SPDX-License-Identifier: Apache-2.0 -->
# DOC_BANNER_OPENER: the Synexia license banner is a block comment, not a doc comment

The first line of the Synexia license banner, `/*****...`, starts with `/**`. javac's doclint therefore
reads the whole license block as a documentation comment placed before `package`. It reports
`dc.unexpected.comment` ("documentation comment not expected here", REFERENCE group), and under the JDK
build's `-Werror` that fails `jdk.m3.collections`.

[`tools/doc_banner_opener.py`](tools/doc_banner_opener.py) changes exactly one character. It applies where
line 1 matches `^/\*{3,}$` and line 2 starts with ` * Copyright (c) 20..` and names Synexia: the opener
`/**` becomes `/* `. The line keeps its length, and every other byte, line ending and file mode is
unchanged. `--check` is the fixed-point gate: it exits 1 while any file still matches.

```sh
python3 m3/tooling/doc-banner-opener/tools/doc_banner_opener.py --check src/jdk.m3*   # gate
python3 m3/tooling/doc-banner-opener/tools/doc_banner_opener.py src/jdk.m3*           # apply
```

The two generators that emitted the old opener now emit the new one:
- `m3/collections/qualification/array-map-fill-20261031/templates/ARRAY_MAP.java.tmpl`
- the `HEADER` literal in `m3/collections/qualification/stale-contracts-20261106/apply.py`

Doclint and `-Werror` stay fully on. Run the recipe again after any intake that brings in new banner files.
