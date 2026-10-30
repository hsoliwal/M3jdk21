#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom (pattern CHROME_PROMPT_API_PER_FILE): one Chrome built-in AI (Prompt API, on-device Gemini Nano) call per
input file through the DevTools protocol of an already running Chrome (the harness lane, --remote-debugging-port).
Opens ONE dedicated tab (never touches other tabs), creates a LanguageModel session per file with the given system
prompt, sends the file's prompt text, writes the reply next to the input as <name>.reply.txt and a receipt line
(file sha256, prompt sha256, reply sha256, ms, status) to RECEIPTS.jsonl. Model output is a CANDIDATE only.
Usage: chrome_prompt_cdp.py <port> <system prompt file> <out dir> <prompt files...>
       chrome_prompt_cdp.py <port> --probe     (prints LanguageModel availability)"""
import hashlib, json, os, sys, time, urllib.request
import websocket

port = int(sys.argv[1])
sha = lambda b: hashlib.sha256(b).hexdigest()


def http(path, method="GET"):
    req = urllib.request.Request(f"http://127.0.0.1:{port}{path}", method=method)
    return json.loads(urllib.request.urlopen(req, timeout=20).read().decode())


def open_tab():
    for m in ("PUT", "GET"):
        try:
            return http("/json/new?https://www.google.com/robots.txt", m)
        except Exception:
            continue
    raise SystemExit("cannot open a tab")


class Tab:
    def __init__(self, t):
        self.t = t; self.ws = websocket.create_connection(t["webSocketDebuggerUrl"], timeout=600, suppress_origin=True); self.n = 0

    def eval(self, expr, timeout=600):
        self.n += 1; mid = self.n
        self.ws.send(json.dumps({"id": mid, "method": "Runtime.evaluate",
                                 "params": {"expression": expr, "awaitPromise": True, "returnByValue": True, "timeout": timeout * 1000}}))
        while True:
            m = json.loads(self.ws.recv())
            if m.get("id") == mid:
                r = m.get("result", {})
                if "exceptionDetails" in r:
                    raise RuntimeError(json.dumps(r["exceptionDetails"])[:400])
                return r.get("result", {}).get("value")

    def close(self):
        try:
            self.ws.close(); http(f"/json/close/{self.t['id']}")
        except Exception:
            pass


tab = Tab(open_tab()); time.sleep(3)
try:
    if sys.argv[2] == "--probe":
        print(tab.eval("(async () => { if (typeof LanguageModel === 'undefined') return 'NO_API'; "
                       "const a = await LanguageModel.availability(); const p = (typeof LanguageModel.params === 'function') ? await LanguageModel.params() : null; "
                       "return JSON.stringify({availability: a, params: p}); })()"))
        sys.exit(0)
    system = open(sys.argv[2], encoding="utf-8").read(); out = sys.argv[3]; os.makedirs(out, exist_ok=True)
    rec = open(os.path.join(out, "RECEIPTS.jsonl"), "a", encoding="utf-8")
    for f in sys.argv[4:]:
        text = open(f, encoding="utf-8").read(); t0 = time.time(); status = "OK"
        try:
            reply = tab.eval("(async () => { const s = await LanguageModel.create({initialPrompts: [{role: 'system', content: %s}]}); "
                             "try { return await s.prompt(%s); } finally { s.destroy(); } })()" % (json.dumps(system), json.dumps(text)))
        except Exception as e:
            reply, status = "", "ERROR " + str(e)[:200]
        name = os.path.join(out, os.path.basename(f) + ".reply.txt"); open(name, "w", encoding="utf-8", newline="\n").write(reply or "")
        rec.write(json.dumps({"file": f, "file_sha256": sha(text.encode()), "system_sha256": sha(system.encode()),
                              "reply_sha256": sha((reply or "").encode()), "ms": int((time.time() - t0) * 1000), "status": status}) + "\n")
        rec.flush(); print(f"{os.path.basename(f)}\t{status}\t{int((time.time() - t0) * 1000)} ms", flush=True)
finally:
    tab.close()
