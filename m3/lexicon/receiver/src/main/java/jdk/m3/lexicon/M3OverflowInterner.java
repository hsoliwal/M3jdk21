// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.lexicon;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Process-local escape hatch for strings not present in immutable dictionaries.
 * Known dictionary words never enter this table.
 */
//FLLDCIM: RELOCATE - donor blob 6497b08b21d5 at commit 3385d79a2968 (repository, path, URL and digests: m3/lexicon/recipe/interned-language-20261106/PROVENANCE.tsv); recipe m3/lexicon/recipe/interned-language-20261106/relocate.py v1
public final class M3OverflowInterner {
    public record Entry(int sourceLanguageId, String text) { }
    private record Key(int sourceLanguageId, String text) { }

    private final Map<Key, Integer> ids = new HashMap<>();
    private final ArrayList<Entry> entries = new ArrayList<>();

    public synchronized long intern(int sourceLanguageId, String raw) {
        String text = Normalizer.normalize(Objects.requireNonNull(raw, "raw"), Normalizer.Form.NFC);
        Key key = new Key(sourceLanguageId, text);
        Integer existing = ids.get(key);
        if (existing != null) return M3TokenId.pack(M3TokenId.OVERFLOW_LANGUAGE_ID, existing);
        int id = entries.size();
        if (id == Integer.MAX_VALUE) throw new IllegalStateException("overflow interner exhausted");
        entries.add(new Entry(sourceLanguageId, text));
        ids.put(key, id);
        return M3TokenId.pack(M3TokenId.OVERFLOW_LANGUAGE_ID, id);
    }

    public synchronized Entry entry(int overflowId) {
        return entries.get(overflowId);
    }

    public synchronized int size() {
        return entries.size();
    }
}
