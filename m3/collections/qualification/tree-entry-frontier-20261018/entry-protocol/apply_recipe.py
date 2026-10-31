import hashlib
APPLICATION_ORDER=("M3SlotMap","M3TreeMap","M3OrderedSlotMap")
PINS={'M3SlotMap': {'before': '9e4d7588d5bd527f3988ca4d46d79f31132630c7', 'after': 'ff12610ddbd992dc493f5e06a89dc6484c3ad9ee', 'after_sha256': '9dd1dd222ab0882c117559ca088b92c73a89b48b5cfa2cc96a347e02f0b9b4dc', 'bytes': 11466}, 'M3TreeMap': {'before': '8d0690dd904df04cf087de7a7dc64107cf475210', 'after': '1b90f024f54a5d41a3b83b73879a9ea60d57cc43', 'after_sha256': 'a03d8250b6f2b117e396f5e3c45bb5de80ff4d2e7d2ea760c09c3dc14b1f2fcd', 'bytes': 27157}, 'M3OrderedSlotMap': {'before': 'ce5fa2ec4306af03239ab7539ad91acaeaaa8245', 'after': '641fdce56e354926336c63b6118fc519b0cea931', 'after_sha256': 'cf92cac1f51ca8fdb4b0cdfa7c6745fb7a1961aa7636b3e18a52bd99b066258f', 'bytes': 4498}}
EDITS={'M3SlotMap': [['    void accessed(int slot) { }\n', '    void accessed(int slot) { }\n    int entrySlot(K key, int hint, int expected) { return findSlot(key); }\n    final Entry<K, V> entryAt(int slot) { return new LiveEntry(keyAt(slot), slot, revision()); }\n'], ['        private final K key;\n        LiveEntry(K key) { this.key = key; }\n        @Override public K getKey() { return key; }\n        @Override public V getValue() { int s = findSlot(key); return s < 0 ? null : valueAt(s); }\n        @Override public V setValue(V value) {\n            int s = findSlot(key);\n', '        private final K key;\n        private final int hint;\n        private final int expected;\n        LiveEntry(K key) { this(key, -1, -1); }\n        LiveEntry(K key, int hint, int expected) {\n            this.key = key; this.hint = hint; this.expected = expected;\n        }\n        @Override public K getKey() { return key; }\n        @Override public V getValue() { int s = entrySlot(key, hint, expected); return s < 0 ? null : valueAt(s); }\n        @Override public V setValue(V value) {\n            int s = entrySlot(key, hint, expected);\n'], ['kind == 1 ? valueAt(last) : new LiveEntry(heldKey);', 'kind == 1 ? valueAt(last) : entryAt(last);']], 'M3TreeMap': [['    @Override int findSlot(Object key) {\n        K k = checked(key); return within(k) ? tree.find(k) : -1;\n    }\n', '    @Override int findSlot(Object key) {\n        if (tree.size == 0 && tree.comparator != null && !hasLow && !hasHigh) { return -1; }\n        K k = checked(key); return within(k) ? tree.find(k) : -1;\n    }\n    @Override int entrySlot(K key, int hint, int expected) {\n        // Attached API entries can address their slot without invoking the comparator.\n        // After structural change retain the existing key-based resolution semantics.\n        return expected == revision() && hint >= 0 && hint < tree.used\n                && tree.height[hint] != 0 && tree.keys[hint] == key ? hint : findSlot(key);\n    }\n']], 'M3OrderedSlotMap': [['remove ? new SimpleEntry<>(keyAt(s), valueAt(s)) : new LiveEntry(keyAt(s));', 'remove ? new SimpleEntry<>(keyAt(s), valueAt(s)) : entryAt(s);']]}
def git_blob(raw):
    return hashlib.sha1(b"blob "+str(len(raw)).encode()+b"\0"+raw).hexdigest()
def transform(raw, name, reverse=False):
    expected,target=(PINS[name]["after"],PINS[name]["before"]) if reverse else (PINS[name]["before"],PINS[name]["after"])
    pin=git_blob(raw)
    if pin==target:return raw
    if pin!=expected:raise ValueError("source preimage refused")
    text=raw.decode("utf-8")
    changes=[(b,a) for a,b in reversed(EDITS[name])] if reverse else EDITS[name]
    for old,new in changes:
        if text.count(old)!=1:raise ValueError("nonunique transformation atom")
        text=text.replace(old,new)
    result=text.encode("utf-8")
    if git_blob(result)!=target:raise ValueError("postimage refused")
    return result
