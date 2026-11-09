package com.synexia.indexstring;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/** Donor-side stub (differential only): spelling-equal interning with dense handles from 1. */
public final class MatIndexStringPool {
    private final Map<String, Integer> ids = new HashMap<>(); private final List<String> values = new ArrayList<>();
    public MatIndexStringPool(IndexResolver resolver) { values.add(null); }
    public MatIndexString internText(int languageId, String text) {
        Integer id = ids.get(text);
        if (id == null) { id = values.size(); values.add(text); ids.put(text, id); }
        return new MatIndexString(id, text);
    }
    public MatIndexString value(int handle) { return new MatIndexString(handle, values.get(handle)); }
    public long retainedTokenCount() { return values.size() - 1; }
}
