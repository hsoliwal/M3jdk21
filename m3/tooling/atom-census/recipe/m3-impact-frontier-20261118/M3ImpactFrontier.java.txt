/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.tooling.atoms.contract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Read-only, incremental atom/file impact planning over the existing M3 coverage ledger.
 *
 * <p>The dependency direction is dependent -> prerequisite. A changed prerequisite invalidates
 * its transitive dependents. All atoms of an affected file share its single canonical mutation
 * boundary. Internally, dependent edges and file membership are stored as primitive offset arrays;
 * no graph-node objects are retained. Cycles are bounded by one visit per atom.</p>
 *
 * <p>This is tool-plane candidate evidence, not an execution engine, recipe, compiler, proof of
 * semantic equivalence, or authorization to edit files. Every affected atom's existing admission
 * findings are carried forward without inventing PASS evidence. Parent owners are source-ledger
 * owner identities, not inferred JPMS module names.</p>
 */
public final class M3ImpactFrontier {
    public static final String SCHEMA = "M3_IMPACT_FRONTIER_V1";
    public static final int MAX_EDGES = 1_000_000;

    private M3ImpactFrontier() {}

    /** One known dependency in the direction from the caller to what the caller uses. */
    public record Dependency(String dependent, String prerequisite) {
        public Dependency {
            dependent = id(dependent, "dependent");
            prerequisite = id(prerequisite, "prerequisite");
        }
    }

    /** Immutable, source-bound impact receipt; never canonical mutation or promotion authority. */
    public record Plan(
            String ledgerSha256,
            List<String> changedAtoms,
            List<String> affectedAtoms,
            List<String> affectedFiles,
            List<String> affectedOwners,
            List<String> blockers,
            String graphRootSha256,
            String rootSha256) {
        public Plan {
            ledgerSha256 = sha(ledgerSha256, "ledgerSha256");
            changedAtoms = sortedUnique(changedAtoms, "changedAtoms");
            affectedAtoms = sortedUnique(affectedAtoms, "affectedAtoms");
            affectedFiles = sortedUnique(affectedFiles, "affectedFiles");
            affectedOwners = sortedUnique(affectedOwners, "affectedOwners");
            blockers = sortedUnique(blockers, "blockers");
            graphRootSha256 = sha(graphRootSha256, "graphRootSha256");
            rootSha256 = sha(rootSha256, "rootSha256");
            if (changedAtoms.isEmpty() || affectedAtoms.isEmpty() || affectedFiles.isEmpty()) {
                throw new IllegalArgumentException("empty impact frontier");
            }
            if (!affectedAtoms.containsAll(changedAtoms)) {
                throw new IllegalArgumentException("missing changed atom");
            }
            if (!rootSha256.equals(root(ledgerSha256, changedAtoms, affectedAtoms,
                    affectedFiles, affectedOwners, blockers, graphRootSha256))) {
                throw new IllegalArgumentException("impact receipt root mismatch");
            }
        }

        /** Does not rewrite source or authorize promotion. */
        public boolean mutationAuthority() { return false; }
        /** Admission remains with compiler, tests, runtime and coverage verifier. */
        public boolean promotionAuthority() { return false; }
        /** A blocker-free frontier is still not semantic-equivalence proof. */
        public boolean semanticAuthority() { return false; }
    }

    /**
     * Compute the minimal conservative file-level impact closure.
     *
     * @param ledger previously validated M3 atom/evidence ledger
     * @param dependencies complete known dependency edges for the input scope
     * @param changedAtomIds source-delta seeds, never a request to edit files
     * @return deterministic source-bound revalidation plan
     */
    public static Plan plan(AtomCoverageLedger ledger,
                            Collection<Dependency> dependencies,
                            Collection<String> changedAtomIds) {
        Objects.requireNonNull(ledger, "ledger");
        Objects.requireNonNull(dependencies, "dependencies");
        Objects.requireNonNull(changedAtomIds, "changedAtomIds");
        Map<String, AtomRecord> atoms = ledger.atoms();
        String[] ids = atoms.keySet().toArray(String[]::new); // already sorted by ledger
        int n = ids.length;
        if (n == 0) throw new IllegalArgumentException("empty ledger");

        String[] fileNames = atoms.values().stream().map(AtomRecord::sourcePath)
                .distinct().sorted().toArray(String[]::new);
        int[] fileId = new int[n];
        int[] fileCounts = new int[fileNames.length];
        for (int i = 0; i < n; i++) {
            fileId[i] = Arrays.binarySearch(fileNames, atoms.get(ids[i]).sourcePath());
            fileCounts[fileId[i]]++;
        }
        int[] fileOffsets = offsets(fileCounts);
        int[] fileCursor = Arrays.copyOf(fileOffsets, fileOffsets.length);
        int[] atomByFile = new int[n];
        for (int i = 0; i < n; i++) atomByFile[fileCursor[fileId[i]]++] = i;

        if (dependencies.size() > MAX_EDGES) throw new IllegalArgumentException("edge budget exceeded");
        long[] edgeKeys = new long[dependencies.size()];
        int position = 0;
        for (Dependency edge : dependencies) {
            Objects.requireNonNull(edge, "dependency");
            int dependent = index(ids, edge.dependent());
            int prerequisite = index(ids, edge.prerequisite());
            edgeKeys[position++] = ((long) prerequisite << 32) | (dependent & 0xffff_ffffL);
        }
        Arrays.sort(edgeKeys);
        int edgeCount = 0;
        for (long key : edgeKeys) {
            if (edgeCount == 0 || edgeKeys[edgeCount - 1] != key) {
                edgeKeys[edgeCount++] = key;
            }
        }
        MessageDigest edgeDigest = digest();
        frame(edgeDigest, "M3_IMPACT_EDGES_V1");
        frame(edgeDigest, Integer.toString(edgeCount));
        int[] outCounts = new int[n];
        for (int i = 0; i < edgeCount; i++) {
            int prerequisite = (int) (edgeKeys[i] >>> 32);
            outCounts[prerequisite]++;
            frame(edgeDigest, ids[prerequisite]);
            frame(edgeDigest, ids[(int) edgeKeys[i]]);
        }
        String graphRoot = HexFormat.of().formatHex(edgeDigest.digest());
        int[] reverseOffsets = offsets(outCounts);
        int[] reverseCursor = Arrays.copyOf(reverseOffsets, reverseOffsets.length);
        int[] dependents = new int[edgeCount];
        for (int i = 0; i < edgeCount; i++) {
            int prerequisite = (int) (edgeKeys[i] >>> 32);
            dependents[reverseCursor[prerequisite]++] = (int) edgeKeys[i];
        }

        TreeSet<String> changed = new TreeSet<>();
        for (String seed : changedAtomIds) {
            String validated = id(seed, "changed atom");
            index(ids, validated); // missing atom is an error, not an empty impact
            changed.add(validated);
        }
        if (changed.isEmpty()) throw new IllegalArgumentException("empty change seed");

        boolean[] seen = new boolean[n];
        boolean[] expandedFile = new boolean[fileNames.length];
        int[] queue = new int[n];
        int tail = 0;
        for (String seed : changed) {
            int node = index(ids, seed);
            if (!seen[node]) {
                seen[node] = true;
                queue[tail++] = node;
            }
        }
        for (int head = 0; head < tail; head++) {
            int atom = queue[head];
            int file = fileId[atom];
            if (!expandedFile[file]) {
                expandedFile[file] = true;
                for (int k = fileOffsets[file]; k < fileOffsets[file + 1]; k++) {
                    int sibling = atomByFile[k];
                    if (!seen[sibling]) {
                        seen[sibling] = true;
                        queue[tail++] = sibling;
                    }
                }
            }
            for (int k = reverseOffsets[atom]; k < reverseOffsets[atom + 1]; k++) {
                int dependent = dependents[k];
                if (!seen[dependent]) {
                    seen[dependent] = true;
                    queue[tail++] = dependent;
                }
            }
        }

        ArrayList<String> impacted = new ArrayList<>();
        TreeSet<String> owners = new TreeSet<>();
        for (int i = 0; i < n; i++) {
            if (seen[i]) {
                impacted.add(ids[i]);
                owners.add(atoms.get(ids[i]).owner());
            }
        }
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < fileNames.length; i++) {
            if (expandedFile[i]) files.add(fileNames[i]);
        }
        ArrayList<String> blockers = new ArrayList<>();
        for (String finding : ledger.admissionFindings()) {
            int tab = finding.indexOf('\t');
            if (tab < 1) throw new IllegalStateException("invalid admission finding");
            int i = Arrays.binarySearch(ids, finding.substring(0, tab));
            if (i >= 0 && seen[i]) blockers.add(finding);
        }
        blockers.sort(String::compareTo);
        String ledgerRoot = ledger.semanticSha256();
        List<String> changedOut = List.copyOf(changed);
        List<String> impactedOut = List.copyOf(impacted);
        List<String> filesOut = List.copyOf(files);
        List<String> ownersOut = List.copyOf(owners);
        List<String> blockersOut = List.copyOf(blockers);
        return new Plan(ledgerRoot, changedOut, impactedOut, filesOut, ownersOut,
                blockersOut, graphRoot, root(ledgerRoot, changedOut, impactedOut,
                        filesOut, ownersOut, blockersOut, graphRoot));
    }

    private static int index(String[] ids, String id) {
        int result = Arrays.binarySearch(ids, id);
        if (result < 0) throw new IllegalArgumentException("unresolved dependency/seed atom: " + id);
        return result;
    }

    private static int[] offsets(int[] counts) {
        int[] result = new int[counts.length + 1];
        for (int i = 0; i < counts.length; i++) result[i + 1] = Math.addExact(result[i], counts[i]);
        return result;
    }

    private static List<String> sortedUnique(List<String> rows, String field) {
        List<String> copy = List.copyOf(Objects.requireNonNull(rows, field));
        for (int i = 0; i < copy.size(); i++) {
            Objects.requireNonNull(copy.get(i), field);
            if (i > 0 && copy.get(i - 1).compareTo(copy.get(i)) >= 0) {
                throw new IllegalArgumentException("non-canonical " + field);
            }
        }
        return copy;
    }

    private static String id(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[A-Za-z0-9][A-Za-z0-9._:/@+\\-]{0,255}")) {
            throw new IllegalArgumentException("invalid " + field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (!checked.matches("[0-9a-f]{64}")) throw new IllegalArgumentException(field);
        return checked;
    }

    private static String root(String ledger, List<String> changed, List<String> atoms,
                               List<String> files, List<String> owners, List<String> blockers,
                               String graph) {
        MessageDigest hash = digest();
        frame(hash, SCHEMA);
        frame(hash, ledger);
        frame(hash, graph);
        frameList(hash, changed);
        frameList(hash, atoms);
        frameList(hash, files);
        frameList(hash, owners);
        frameList(hash, blockers);
        return HexFormat.of().formatHex(hash.digest());
    }

    private static void frameList(MessageDigest hash, List<String> values) {
        frame(hash, Integer.toString(values.size()));
        for (String value : values) frame(hash, value);
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new ExceptionInInitializerError(impossible); }
    }

    private static void frame(MessageDigest hash, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        hash.update((byte) (bytes.length >>> 24));
        hash.update((byte) (bytes.length >>> 16));
        hash.update((byte) (bytes.length >>> 8));
        hash.update((byte) bytes.length);
        hash.update(bytes);
    }
}
