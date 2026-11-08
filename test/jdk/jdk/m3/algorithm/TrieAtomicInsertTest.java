// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import jdk.m3.algorithm.ProgressMonitor;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Guard the exact no-ghost-prefix contract of a failed M3StringTrie.insert. */
//FLLDCIM: RELOCATE - donor blob 0d6166040baa at commit 3385d79a2968 (repository, path, URL and digests: m3/algorithms/recipe/graph-trie-20261106/PROVENANCE.tsv); recipe m3/algorithms/recipe/graph-trie-20261106/relocate.py v1
final class TrieAtomicInsertTest {
    private static final class Trace implements ProgressMonitor {
        int begins, ticks, ends, checks;
        int abortAfter = Integer.MAX_VALUE;
        boolean cancellation;
        boolean throwInWorked;
        @Override public boolean isCanceled() {
            checks++;
            return cancellation;
        }
        @Override public void beginTask(String name, int total) {
            begins++;
            assertEquals("trie-insert", name);
        }
        @Override public void worked(int value) {
            ticks += value;
            if (ticks >= abortAfter) {
                if (throwInWorked) throw new IllegalStateException("monitor failed");
                cancellation = true;
            }
        }
        @Override public void done() { ends++; }
    }

    @Test
    void cancellationDoesNotPublishANewPrefixIntoEmptyTrie() {
        StringTrie trie = new StringTrie();
        Trace trace = new Trace();
        trace.abortAfter = 1;
        assertThrows(CancellationException.class, () -> trie.insert("xyz", trace));
        assertFalse(trie.startsWith("x", ProgressMonitor.none()));
        assertFalse(trie.contains("xyz", ProgressMonitor.none()));
        assertEquals(0, trie.wordCount(ProgressMonitor.none()));
        assertEquals(1, trace.begins);
        assertEquals(1, trace.ends);
        assertEquals(1, trace.ticks);
    }

    @Test
    void cancellationAfterCreatingUncommittedSuffixPreservesExistingWord() {
        StringTrie trie = new StringTrie();
        assertTrue(trie.insert("pre", ProgressMonitor.none()));
        Trace trace = new Trace();
        trace.abortAfter = 4;
        assertThrows(CancellationException.class, () -> trie.insert("prefix", trace));
        assertTrue(trie.contains("pre", ProgressMonitor.none()));
        assertFalse(trie.contains("prefix", ProgressMonitor.none()));
        assertFalse(trie.startsWith("pref", ProgressMonitor.none()));
        assertEquals(1, trie.wordCount(ProgressMonitor.none()));
        assertEquals(4, trace.ticks);
        assertEquals(1, trace.ends);
    }

    @Test
    void progressFailureBeforeCommitDoesNotExposeUnfinishedSuffix() {
        StringTrie trie = new StringTrie();
        assertTrue(trie.insert("pre", ProgressMonitor.none()));
        Trace trace = new Trace();
        trace.abortAfter = 5;
        trace.throwInWorked = true;
        assertThrows(IllegalStateException.class, () -> trie.insert("prefix", trace));
        assertTrue(trie.contains("pre", ProgressMonitor.none()));
        assertFalse(trie.startsWith("pref", ProgressMonitor.none()));
        assertEquals(1, trie.wordCount(ProgressMonitor.none()));
        assertEquals(1, trace.ends);
    }

    @Test
    void successAndDuplicateUnicodeEmptyAndRemoveContractsRemain() {
        StringTrie trie = new StringTrie();
        Trace first = new Trace();
        assertTrue(trie.insert("café😀", first));
        assertEquals(5, first.ticks);
        assertEquals(6, first.checks);
        assertEquals(1, first.begins);
        assertEquals(1, first.ends);
        assertTrue(trie.startsWith("café", ProgressMonitor.none()));
        assertFalse(trie.contains("café", ProgressMonitor.none()));
        assertTrue(trie.insert("café💡", ProgressMonitor.none()));
        assertFalse(trie.insert("café😀", ProgressMonitor.none()));
        assertEquals(2, trie.wordCount(ProgressMonitor.none()));
        assertTrue(trie.remove("café😀", ProgressMonitor.none()));
        assertFalse(trie.contains("café😀", ProgressMonitor.none()));
        assertTrue(trie.contains("café💡", ProgressMonitor.none()));
        assertEquals(1, trie.wordCount(ProgressMonitor.none()));
        assertTrue(trie.insert("", ProgressMonitor.none()));
        assertTrue(trie.contains("", ProgressMonitor.none()));
        assertEquals(2, trie.wordCount(ProgressMonitor.none()));
    }

    @Test
    void cancellationSetOnLastWorkedTickStillPermitsSuccessfulInsert() {
        StringTrie trie = new StringTrie();
        Trace trace = new Trace();
        trace.abortAfter = 2;
        assertTrue(trie.insert("de", trace));
        assertTrue(trace.cancellation);
        assertTrue(trie.contains("de", ProgressMonitor.none()));
        assertEquals(2, trace.ticks);
        assertEquals(1, trace.ends);
    }
}
