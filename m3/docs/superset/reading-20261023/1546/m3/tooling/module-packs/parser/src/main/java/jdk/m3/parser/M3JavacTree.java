/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.parser;

import com.sun.source.tree.Tree;
import java.util.Objects;
import java.util.Optional;

/**
 * Detached preorder projection of one parsed javac unit into primitive lanes.
 * No live javac tree, compiler context, element, or type is retained. Kind ordinals
 * are interpreted with this running compiler's Tree.Kind; this is not a portable
 * serialized attributed-AST format.
 *
 * <p>Consolidation: the four geometry lanes are a strict subset of the JAVAC-domain
 * {@link M3ASTIndex}. {@link #of(M3ASTIndex)} is the same typed surface as a view over
 * that one index, so a consumer holding the detailed index never requests a second
 * projection or copies geometry; only a kind-translation lane is retained.
 */
public final class M3JavacTree {
    private static final Tree.Kind[] KIND_BY_ORDINAL = Tree.Kind.values();
    private final M3File source;
    private final int[] kinds;
    private final int[] parents;
    private final int[] starts;
    private final int[] ends;
    private final M3ASTIndex index;
    /** Index kind-vocabulary ordinal to Tree.Kind ordinal; present only for a view. */
    private final int[] kindOfVocabulary;

    M3JavacTree(M3File source, int[] kinds, int[] parents, int[] starts, int[] ends) {
        this.source = Objects.requireNonNull(source, "source");
        this.kinds = kinds;
        this.parents = parents;
        this.starts = starts;
        this.ends = ends;
        this.index = null;
        this.kindOfVocabulary = null;
    }

    private M3JavacTree(M3ASTIndex index, int[] kindOfVocabulary) {
        this.source = index.source();
        this.kinds = null;
        this.parents = null;
        this.starts = null;
        this.ends = null;
        this.index = index;
        this.kindOfVocabulary = kindOfVocabulary;
    }

    /**
     * View an existing JAVAC-domain index through the javac geometry surface. The
     * index stays the owner of every lane; a foreign domain or a kind name that is
     * not a {@code Tree.Kind} of this running compiler is refused.
     */
    public static M3JavacTree of(M3ASTIndex index) {
        Objects.requireNonNull(index, "original javac M3ASTIndex");
        if (index.domain() != M3ASTIndex.Domain.JAVAC) {
            throw new IllegalArgumentException("M3JavacTree view requires a JAVAC domain index");
        }
        int[] translate = new int[index.kindVocabularySize()];
        for (int id = 0; id < translate.length; id++) {
            translate[id] = Tree.Kind.valueOf(index.kindVocabularyName(id)).ordinal();
        }
        return new M3JavacTree(index, translate);
    }

    public M3File source() { return source; }
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    /** The owning detailed index when this tree is a view; empty for a detached projection. */
    public Optional<M3ASTIndex> index() { return Optional.ofNullable(index); }
    public int nodeCount() { return index == null ? kinds.length : index.nodeCount(); }
    public Tree.Kind kind(int node) {
        return index == null ? KIND_BY_ORDINAL[kinds[check(node)]]
                : KIND_BY_ORDINAL[kindOfVocabulary[index.kindOrdinal(node)]];
    }
=======
    public int nodeCount() { return kinds.length; }
    public Tree.Kind kind(int node) { return KIND_BY_ORDINAL[kinds[check(node)]]; }
>>>>>>> 9a87e78465ea2ae4f5c8a74d429c5b68111608e1
    /** Parent row, or -1 for the compilation-unit root. */
    public int parent(int node) { return index == null ? parents[check(node)] : index.parent(node); }
    /** Exact javac UTF-16 start offset, or -1 if unavailable. */
    public int start(int node) { return index == null ? starts[check(node)] : index.start(node); }
    /** Exact javac UTF-16 end offset, or -1 if unavailable. */
    public int end(int node) { return index == null ? ends[check(node)] : index.end(node); }
    public boolean hasSourceRange(int node) { return start(node) >= 0 && end(node) >= 0; }

    public String sourceText(int node) {
        if (!hasSourceRange(node)) throw new IllegalStateException("javac source range unavailable");
        return source.subSequence(start(node), end(node));
    }

    /** Lanes retained by this object alone: four per row, or one kind-translation lane for a view. */
    public long primitivePayloadBytes() {
        return index == null ? 4L * Integer.BYTES * nodeCount() : (long) Integer.BYTES * kindOfVocabulary.length;
    }
    private int check(int node) { return Objects.checkIndex(node, kinds.length); }
}
