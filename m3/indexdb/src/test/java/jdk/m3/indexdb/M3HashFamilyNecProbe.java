// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.indexdb;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import jdk.internal.m3.mindex.db.M3IndexDbFingerprintV1;

/**
 * NEC probes of the hash-family leaf (not a test): each probe measures how a relocated member relates to an owner it
 * overlaps and prints counts; it never decides equivalence (RELOCATION_MAP.tsv NEC rows). Owners outside this module
 * are loaded reflectively from the class path differential.sh assembles: {@code --simhash=} the donor crate's SimHash
 * ({@code ofFeatures(long[], int[], int)}, {@code fingerprint(CharSequence)}), {@code --search-simhash=} and
 * {@code --search-fnv=} the SimHash64 and FNV-1a string-hash owners 45 landed in jdk.m3.search (#2550:
 * {@code of(String)}, {@code hash(String, int, int)}), {@code --textsignatures=} the cpu-tools signatures
 * ({@code compute(List, int)}, {@code estimatedJaccard}); {@code --corpus-root=} and {@code --corpus-list=} name the
 * crate's oracle corpus (the 403 files of its LHASH_ORACLE.tsv). Probes: logic_hash_vs_fingerprint, hamming_member,
 * simhash_ties, crate_simhash_vs_search, crate_fnv_vs_search, minhash_vs_textsignatures.
 */
//FLLDCIM: NEC - recipe m3/indexdb/recipe/hash-family-20261106 differential.sh probe lane
final class M3HashFamilyNecProbe {
    static final long PROBE_SEED = 0x4d334e454350524fL;
    static final int LOGIC_INPUTS = 20_000;
    static final int HAMMING_PAIRS = 1_000_000;
    static final int SIMHASH_WINDOWS = 20_000;
    static final int FNV_TEXTS = 20_000;
    static final int SIGNATURE_PAIRS = 1_000;
    static final int SIGNATURE_LANES = 128;

    private M3HashFamilyNecProbe() { }

    public static void main(String[] args) throws ReflectiveOperationException, java.io.IOException {
        List<byte[]> inputs = M3HashFamilyDump.lhashInputs(Math.max(LOGIC_INPUTS, FNV_TEXTS));
        Class<?> crateSimHash = Class.forName(M3HashFamilyContractSupport.option(args, "--simhash=").orElseThrow());
        Class<?> searchSimHash = Class.forName(M3HashFamilyContractSupport.option(args, "--search-simhash=").orElseThrow());
        Class<?> searchFnv = Class.forName(M3HashFamilyContractSupport.option(args, "--search-fnv=").orElseThrow());
        logicHash(inputs);
        hamming();
        simhash(crateSimHash);
        List<String> corpus = corpus(args);
        crateVsSearch("corpus", corpus, crateSimHash, searchSimHash, searchFnv);
        crateVsSearch("lane", inputs.subList(0, FNV_TEXTS).stream().map(M3LHashTokens::decode).toList(), crateSimHash,
                searchSimHash, searchFnv);
        signatures(Class.forName(M3HashFamilyContractSupport.option(args, "--textsignatures=").orElseThrow()));
    }

    /** The crate's oracle corpus decoded as its command line decodes it (UTF-8, malformed input dropped). */
    static List<String> corpus(String[] args) throws java.io.IOException {
        java.nio.file.Path root = java.nio.file.Path.of(M3HashFamilyContractSupport.option(args, "--corpus-root=").orElseThrow());
        List<String> texts = new ArrayList<>();
        for (String file : java.nio.file.Files.readAllLines(java.nio.file.Path.of(
                M3HashFamilyContractSupport.option(args, "--corpus-list=").orElseThrow()))) {
            if (!file.isBlank()) texts.add(M3LHashTokens.decode(java.nio.file.Files.readAllBytes(root.resolve(file))));
        }
        return texts;
    }

    /**
     * The crate's SimHash64.fingerprint (words incl. Unicode classes, unigram 1 + adjacent pair 2, UTF-16 FNV, ties 0)
     * against jdk.m3.search.M3SimHash64.of (unigrams of [\p{L}\p{N}_']+, UTF-8 FNV, ties 1), and the lane FNV over
     * UTF-16 units against jdk.m3.search.M3Utf8Fnv1a64 over the same texts: agree/disagree counts only.
     */
    static void crateVsSearch(String name, List<String> texts, Class<?> crate, Class<?> search, Class<?> fnv)
            throws ReflectiveOperationException {
        Method fingerprint = crate.getMethod("fingerprint", CharSequence.class);
        Method of = search.getMethod("of", String.class);
        Method hash = fnv.getMethod("hash", String.class, int.class, int.class);
        int simEqual = 0;
        long hammingSum = 0;
        int fnvEqual = 0;
        int ascii = 0;
        int asciiFnvEqual = 0;
        for (String text : texts) {
            long left = (long) fingerprint.invoke(null, text);
            long right = (long) of.invoke(null, text);
            simEqual += left == right ? 1 : 0;
            hammingSum += Long.bitCount(left ^ right);
            boolean same = M3LaneFnv1a.ofChars(text) == (long) hash.invoke(null, text, 0, text.length());
            fnvEqual += same ? 1 : 0;
            if (text.chars().allMatch(c -> c < 0x80)) {
                ascii++;
                asciiFnvEqual += same ? 1 : 0;
            }
        }
        print("crate_simhash_vs_search %s texts=%d equal=%d different=%d mean_hamming=%.2f", name, texts.size(), simEqual,
                texts.size() - simEqual, texts.isEmpty() ? 0.0 : (double) hammingSum / texts.size());
        print("crate_fnv_vs_search %s texts=%d equal=%d different=%d ascii_texts=%d ascii_equal=%d", name, texts.size(),
                fnvEqual, texts.size() - fnvEqual, ascii, asciiFnvEqual);
    }

    /** H1 of the token lane vs M3IndexDbFingerprintV1.leaf(domain, [], token ids as text, source).logicHash64(). */
    static void logicHash(List<byte[]> inputs) {
        int equal = 0;
        for (int i = 0; i < LOGIC_INPUTS; i++) {
            String text = M3LHashTokens.decode(inputs.get(i));
            M3LHashTokens.Tokens tokens = M3LHashTokens.tokenize(text);
            List<String> features = new ArrayList<>(tokens.size());
            for (int t = 0; t < tokens.size(); t++) features.add(Integer.toString(tokens.ids()[t]));
            long logic64 = M3IndexDbFingerprintV1.leaf("LHASH", List.of(), features, text).logicHash64();
            equal += logic64 == M3LogicHash.of(tokens)[0] ? 1 : 0;
        }
        print("logic_hash_vs_fingerprint inputs=%d equal=%d different=%d", LOGIC_INPUTS, equal, LOGIC_INPUTS - equal);
    }

    static void hamming() {
        SplittableRandom random = new SplittableRandom(PROBE_SEED);
        int equal = 0;
        long[] candidates = new long[HAMMING_PAIRS];
        long query = random.nextLong();
        for (int i = 0; i < HAMMING_PAIRS; i++) {
            long left = i < 4 ? new long[] {0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE}[i] : random.nextLong();
            long right = i % 3 == 0 ? left ^ (1L << (i & 63)) : random.nextLong();
            int relocated = M3HashDistances.hamming(left, right);
            boolean same = relocated == M3IndexDbChunkSimHash.hammingDistance(left, right)
                    && relocated == M3IndexDbFingerprintV1.hammingDistance(left, right);
            equal += same ? 1 : 0;
            candidates[i] = right;
        }
        int[] all = M3HashDistances.hammingAll(query, candidates, candidates.length);
        int allEqual = 0;
        for (int i = 0; i < all.length; i++) allEqual += all[i] == M3IndexDbChunkSimHash.hammingDistance(query, candidates[i]) ? 1 : 0;
        print("hamming_member pairs=%d equal=%d different=%d hammingAll=%d equal=%d", HAMMING_PAIRS, equal,
                HAMMING_PAIRS - equal, all.length, allEqual);
    }

    /** Donor SimHash ofFeatures (unit weights) vs M3IndexDbChunkSimHash.fromFeatures(...).simHash64(), tie mask. */
    static void simhash(Class<?> donor) throws ReflectiveOperationException {
        Method ofFeatures = donor.getMethod("ofFeatures", long[].class, int[].class, int.class);
        SplittableRandom random = new SplittableRandom(PROBE_SEED ^ 1L);
        int equalUntied = 0;
        int tieMask = 0;
        int unexplained = 0;
        for (int w = 0; w < SIMHASH_WINDOWS; w++) {
            int n = random.nextInt(65);
            long[] features = new long[n];
            int[] ones = new int[n];
            int[] votes = new int[Long.SIZE];
            for (int f = 0; f < n; f++) {
                features[f] = random.nextLong();
                ones[f] = 1;
                for (int bit = 0; bit < Long.SIZE; bit++) votes[bit] += ((features[f] >>> bit) & 1L) != 0 ? 1 : -1;
            }
            long mask = 0;
            for (int bit = 0; bit < Long.SIZE; bit++) mask |= votes[bit] == 0 ? 1L << bit : 0L;
            long crate = (long) ofFeatures.invoke(null, features, ones, n);
            long owner = M3IndexDbChunkSimHash.fromFeatures("NEC", features, 0, ones, 0, n).simHash64();
            if (crate == owner && (mask == 0 || n == 0)) equalUntied++;
            else if (n > 0 && (crate ^ owner) == mask) tieMask++;
            else unexplained++;
        }
        print("simhash_ties windows=%d equal_untied_or_empty=%d differ_exactly_on_tiemask=%d unexplained=%d",
                SIMHASH_WINDOWS, equalUntied, tieMask, unexplained);
    }

    /** Both MinHash estimates against the exact Jaccard on the same random sets (tokens = hex of the 64-bit values). */
    static void signatures(Class<?> textSignatures) throws ReflectiveOperationException {
        Method compute = textSignatures.getMethod("compute", List.class, int.class);
        Method estimated = null;
        for (Method method : textSignatures.getMethods()) if (method.getName().equals("estimatedJaccard")) estimated = method;
        M3MinHash.Permutations permutations = M3MinHash.Permutations.of(SIGNATURE_LANES, M3MinHash.DEFAULT_SEED);
        SplittableRandom random = new SplittableRandom(PROBE_SEED ^ 2L);
        double relocatedError = 0;
        double ownerError = 0;
        int equalEstimates = 0;
        for (int p = 0; p < SIGNATURE_PAIRS; p++) {
            long[] left = M3HashFamilyDump.set(random, 100 + random.nextInt(900));
            long[] right = M3HashFamilyDump.overlap(random, left, random.nextDouble());
            double exact = M3HashDistances.jaccard(left, left.length, right, right.length);
            double relocated = M3MinHash.jaccardEstimate(M3MinHash.signature(left, left.length, permutations),
                    M3MinHash.signature(right, right.length, permutations));
            Object leftSignature = compute.invoke(null, tokens(left), SIGNATURE_LANES);
            Object rightSignature = compute.invoke(null, tokens(right), SIGNATURE_LANES);
            double owner = (double) estimated.invoke(null, leftSignature, rightSignature);
            relocatedError += Math.abs(exact - relocated);
            ownerError += Math.abs(exact - owner);
            equalEstimates += relocated == owner ? 1 : 0;
        }
        print("minhash_vs_textsignatures pairs=%d lanes=%d relocated_mean_abs_error=%.4f textsignatures_mean_abs_error=%.4f"
                + " equal_estimates=%d", SIGNATURE_PAIRS, SIGNATURE_LANES, relocatedError / SIGNATURE_PAIRS,
                ownerError / SIGNATURE_PAIRS, equalEstimates);
    }

    static List<String> tokens(long[] lane) {
        List<String> tokens = new ArrayList<>(lane.length);
        for (long value : lane) tokens.add(Long.toHexString(value));
        return tokens;
    }

    static void print(String format, Object... values) {
        System.out.println(String.format(Locale.ROOT, format, values));
    }
}
