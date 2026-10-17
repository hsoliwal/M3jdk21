package com.synexia.mindex.experiment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/** Immutable ID table. Supplements preserve IDs; lexical ranks belong to each snapshot. */
public final class TokenDictionary implements TokenSpace {
    private final List<String> tokens;
    private final Map<String, Integer> ids;
    private final int[] ranks;
    private final String fingerprint;

    private TokenDictionary(List<String> ordered) {
        tokens = List.copyOf(ordered);
        if (tokens.isEmpty() || !" ".equals(tokens.get(0))) {
            throw new IllegalArgumentException("token zero must be U+0020");
        }
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).isEmpty() || index.put(tokens.get(i), i) != null) {
                throw new IllegalArgumentException("empty or duplicate token");
            }
        }
        ids = Map.copyOf(index);
        ranks = new int[tokens.size()];
        int rank = 0;
        for (String word : new TreeSet<>(tokens)) { ranks[ids.get(word)] = rank++; }
        fingerprint = Digests.sha256(canonicalBytes());
    }

    public static TokenDictionary of(Collection<String> words) {
        TreeSet<String> sorted = new TreeSet<>(Objects.requireNonNull(words, "words"));
        sorted.remove(" ");
        List<String> ordered = new ArrayList<>();
        ordered.add(" ");
        ordered.addAll(sorted);
        return new TokenDictionary(ordered);
    }

    /** Creates a new snapshot; existing token IDs retain their exact spelling. */
    public TokenDictionary supplement(Collection<String> words) {
        TreeSet<String> extra = new TreeSet<>(words);
        extra.removeAll(ids.keySet());
        List<String> ordered = new ArrayList<>(tokens);
        ordered.addAll(extra);
        return new TokenDictionary(ordered);
    }

    static TokenDictionary ordered(List<String> words) { return new TokenDictionary(words); }
    public Object namespace() { return this; }
    public int size() { return tokens.size(); }
    public String token(int id) { return tokens.get(id); }
    public int id(String token) { return ids.getOrDefault(token, -1); }
    public int rank(int id) { return ranks[id]; }
    public String fingerprint() { return fingerprint; }
    public List<String> tokens() { return tokens; }

    byte[] canonicalBytes() {
        try {
            var bytes = new ByteArrayOutputStream();
            var out = new DataOutputStream(bytes);
            out.writeInt(0x4d494431);
            out.writeInt(tokens.size());
            for (String token : tokens) {
                out.writeInt(token.length());
                for (int i = 0; i < token.length(); i++) { out.writeChar(token.charAt(i)); }
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
