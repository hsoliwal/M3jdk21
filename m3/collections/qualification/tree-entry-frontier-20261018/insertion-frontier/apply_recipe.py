import hashlib
PREIMAGE="1b90f024f54a5d41a3b83b73879a9ea60d57cc43"
POSTIMAGE='fc8a9e75011f5dc8b86f1ee553caba979cb2e30b'
EDITS=[['        void balance(int s) {\n            while (s >= 0) {\n                update(s); int delta = h(left[s]) - h(right[s]);', '        void balance(int s) { balance(s, false); }\n        void balance(int s, boolean stopWhenStable) {\n            while (s >= 0) {\n                int previousHeight = stopWhenStable ? h(s) : 0;\n                update(s); int delta = h(left[s]) - h(right[s]);'], ['                s = parent[s];\n            }\n        }\n        void grow() {', '                // Insertion cannot affect higher ancestors after this height is stable.\n                // Deletion keeps the full walk: transplanted successors are pre-updated.\n                if (stopWhenStable && h(s) == previousHeight) { return; }\n                s = parent[s];\n            }\n        }\n        void grow() {'], ['            size++; modifications++; balance(p);', '            size++; modifications++; balance(p, true);']]
def blob(raw):
    return hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
def transform(raw, reverse=False):
    expected,target=(POSTIMAGE,PREIMAGE) if reverse else (PREIMAGE,POSTIMAGE)
    pin=blob(raw)
    if pin==target:return raw
    if pin!=expected:raise ValueError("unrecognized source")
    text=raw.decode("utf-8")
    for old,new in ([(b,a) for a,b in reversed(EDITS)] if reverse else EDITS):
        if text.count(old)!=1:raise ValueError("nonunique atom")
        text=text.replace(old,new)
    result=text.encode("utf-8")
    if blob(result)!=target:raise ValueError("wrong postimage")
    return result
