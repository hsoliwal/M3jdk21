// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Bounded primitive regex x String trial image used to rank expensive compiler/JUnit follow-up.
 *
 * <p>JDK {@link Pattern} is the exact finite-corpus regex oracle. M3 code-text, SimHash and MinHash
 * facts are ranking evidence only. The caller may select Java or the existing JNI signal provider;
 * regex semantics are never delegated to JNI.</p>
 */
public final class M3RegexStringTrialImage {
    public static final int MATCHES = 1;
    public static final int LOOKING_AT = 1 << 1;
    public static final int FIND = 1 << 2;

    private static final int ALLOWED_PATTERN_FLAGS =
            Pattern.UNIX_LINES
                    | Pattern.CASE_INSENSITIVE
                    | Pattern.COMMENTS
                    | Pattern.MULTILINE
                    | Pattern.LITERAL
                    | Pattern.DOTALL
                    | Pattern.UNICODE_CASE
                    | Pattern.CANON_EQ
                    | Pattern.UNICODE_CHARACTER_CLASS;

    public record RegexSpec(String expression, int flags) {
        public RegexSpec {
            expression = Objects.requireNonNull(expression, "expression");
            if ((flags & ~ALLOWED_PATTERN_FLAGS) != 0) {
                throw new IllegalArgumentException("unsupported Pattern flags");
            }
        }

        public RegexSpec(String expression) {
            this(expression, 0);
        }
    }

    public record Limits(
            int maxRegexes,
            int maxStrings,
            long maxPairs,
            long maxUtf16Units,
            long maxPrimitiveBytes) {
        public static final Limits DEFAULT =
                new Limits(4_096, 65_536, 4_000_000L, 16_000_000L, 96L << 20);

        public Limits {
            if (maxRegexes < 1
                    || maxStrings < 1
                    || maxPairs < 1
                    || maxUtf16Units < 0
                    || maxPrimitiveBytes < 1) {
                throw new IllegalArgumentException("invalid regex/string trial limits");
            }
        }
    }

    private final int regexCount;
    private final int stringCount;
    private final int[] regexFlags;
    private final int[] syntaxErrorIndex;
    private final byte[] regexValid;
    private final byte[] regexShape;
    private final int[] regexCodeScore;
    private final int[] regexRegexScore;
    private final int[] stringCodeScore;
    private final int[] stringRegexScore;
    private final int[] matchesCount;
    private final int[] lookingAtCount;
    private final int[] findCount;
    private final byte[] outcomeClassMask;
    private final byte[] outcomes;
    private final byte[] simHashDistance;
    private final byte[] estimatedJaccard255;
    private final int[] difficulty;
    private final String rootHash;

    private M3RegexStringTrialImage(
            int regexCount,
            int stringCount,
            int[] regexFlags,
            int[] syntaxErrorIndex,
            byte[] regexValid,
            byte[] regexShape,
            int[] regexCodeScore,
            int[] regexRegexScore,
            int[] stringCodeScore,
            int[] stringRegexScore,
            int[] matchesCount,
            int[] lookingAtCount,
            int[] findCount,
            byte[] outcomeClassMask,
            byte[] outcomes,
            byte[] simHashDistance,
            byte[] estimatedJaccard255,
            int[] difficulty,
            String rootHash) {
        this.regexCount = regexCount;
        this.stringCount = stringCount;
        this.regexFlags = regexFlags;
        this.syntaxErrorIndex = syntaxErrorIndex;
        this.regexValid = regexValid;
        this.regexShape = regexShape;
        this.regexCodeScore = regexCodeScore;
        this.regexRegexScore = regexRegexScore;
        this.stringCodeScore = stringCodeScore;
        this.stringRegexScore = stringRegexScore;
        this.matchesCount = matchesCount;
        this.lookingAtCount = lookingAtCount;
        this.findCount = findCount;
        this.outcomeClassMask = outcomeClassMask;
        this.outcomes = outcomes;
        this.simHashDistance = simHashDistance;
        this.estimatedJaccard255 = estimatedJaccard255;
        this.difficulty = difficulty;
        this.rootHash = rootHash;
    }

    public static M3RegexStringTrialImage compile(
            List<RegexSpec> regexes,
            List<? extends CharSequence> strings,
            M3Progress monitor) {
        return compile(
                regexes,
                strings,
                M3CodeTextSignalBatchJava.INSTANCE,
                Limits.DEFAULT,
                monitor);
    }

    public static M3RegexStringTrialImage compile(
            List<RegexSpec> regexes,
            List<? extends CharSequence> strings,
            M3CodeTextSignalBatch signalBatch,
            Limits limits,
            M3Progress monitor) {
        List<RegexSpec> checkedRegexes = List.copyOf(Objects.requireNonNull(regexes, "regexes"));
        List<? extends CharSequence> checkedStrings =
                List.copyOf(Objects.requireNonNull(strings, "strings"));
        Objects.requireNonNull(signalBatch, "signalBatch");
        Objects.requireNonNull(limits, "limits");

        if (checkedRegexes.isEmpty() || checkedRegexes.size() > limits.maxRegexes()) {
            throw new IllegalArgumentException("regex row budget exceeded");
        }
        if (checkedStrings.isEmpty() || checkedStrings.size() > limits.maxStrings()) {
            throw new IllegalArgumentException("string row budget exceeded");
        }

        long pairCountLong = Math.multiplyExact((long) checkedRegexes.size(), checkedStrings.size());
        if (pairCountLong > limits.maxPairs() || pairCountLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("regex/string pair budget exceeded");
        }

        long utf16 = 0;
        for (RegexSpec regex : checkedRegexes) {
            utf16 = Math.addExact(utf16, regex.expression().length());
        }
        for (CharSequence string : checkedStrings) {
            utf16 = Math.addExact(utf16, Objects.requireNonNull(string, "string").length());
        }
        if (utf16 > limits.maxUtf16Units()) {
            throw new IllegalArgumentException("regex/string UTF-16 budget exceeded");
        }

        int regexCount = checkedRegexes.size();
        int stringCount = checkedStrings.size();
        int pairCount = (int) pairCountLong;
        long primitiveBytes =
                Math.addExact(
                        Math.addExact(31L * regexCount, 8L * stringCount),
                        7L * pairCount);
        if (primitiveBytes > limits.maxPrimitiveBytes()) {
            throw new IllegalArgumentException("regex/string primitive image budget exceeded");
        }

        M3CodeTextSignalBatch.Limits batchLimits =
                new M3CodeTextSignalBatch.Limits(
                        limits.maxPrimitiveBytes(),
                        limits.maxUtf16Units(),
                        Math.max(regexCount, stringCount));

        M3Progress progress = monitor == null ? M3Progress.none() : monitor;
        long totalWork = Math.addExact(pairCountLong, (long) regexCount + stringCount);
        progress.begin("M3 regex/string trial image", totalWork);
        try {
            List<String> regexText = checkedRegexes.stream().map(RegexSpec::expression).toList();
            List<M3CodeTextSignals.Snapshot> regexSignals =
                    M3CodeTextSignals.compileBatch(
                            regexText, signalBatch, batchLimits, M3Progress.none());
            progress.worked(regexCount);
            List<M3CodeTextSignals.Snapshot> stringSignals =
                    M3CodeTextSignals.compileBatch(
                            checkedStrings, signalBatch, batchLimits, M3Progress.none());
            progress.worked(stringCount);

            int[] flags = new int[regexCount];
            int[] syntax = new int[regexCount];
            Arrays.fill(syntax, -1);
            byte[] valid = new byte[regexCount];
            byte[] shape = new byte[regexCount];
            int[] regexCode = new int[regexCount];
            int[] regexRegex = new int[regexCount];
            int[] stringCode = new int[stringCount];
            int[] stringRegex = new int[stringCount];
            Pattern[] patterns = new Pattern[regexCount];
            M3RegexShape[] shapes = new M3RegexShape[regexCount];

            for (int row = 0; row < regexCount; row++) {
                RegexSpec spec = checkedRegexes.get(row);
                flags[row] = spec.flags();
                shapes[row] = M3RegexShape.analyze(spec.expression());
                shape[row] = (byte) shapes[row].kind().ordinal();
                regexCode[row] = regexSignals.get(row).codeScore();
                regexRegex[row] = regexSignals.get(row).regexScore();
                try {
                    patterns[row] = Pattern.compile(spec.expression(), spec.flags());
                    valid[row] = 1;
                } catch (PatternSyntaxException invalid) {
                    syntax[row] = invalid.getIndex();
                }
            }
            for (int row = 0; row < stringCount; row++) {
                stringCode[row] = stringSignals.get(row).codeScore();
                stringRegex[row] = stringSignals.get(row).regexScore();
            }

            int[] matches = new int[regexCount];
            int[] lookingAt = new int[regexCount];
            int[] find = new int[regexCount];
            byte[] classes = new byte[regexCount];
            byte[] outcomes = new byte[pairCount];
            byte[] hamming = new byte[pairCount];
            byte[] jaccard = new byte[pairCount];
            int[] difficulty = new int[pairCount];

            for (int regexRow = 0; regexRow < regexCount; regexRow++) {
                RegexSpec spec = checkedRegexes.get(regexRow);
                for (int stringRow = 0; stringRow < stringCount; stringRow++) {
                    int pair = regexRow * stringCount + stringRow;
                    if ((pair & 255) == 0) progress.checkCanceled();

                    M3CodeTextSignals.Snapshot regexSignal = regexSignals.get(regexRow);
                    M3CodeTextSignals.Snapshot stringSignal = stringSignals.get(stringRow);
                    int distance = regexSignal.simHashDistance(stringSignal);
                    int jaccard255 =
                            (int) Math.round(regexSignal.estimatedJaccard(stringSignal) * 255.0);
                    hamming[pair] = (byte) distance;
                    jaccard[pair] = (byte) jaccard255;

                    int bits = 0;
                    if (valid[regexRow] != 0) {
                        String text = checkedStrings.get(stringRow).toString();
                        Matcher matcher = patterns[regexRow].matcher(text);
                        if (matcher.matches()) bits |= MATCHES;
                        matcher.reset();
                        if (matcher.lookingAt()) bits |= LOOKING_AT;
                        matcher.reset();
                        if (matcher.find()) bits |= FIND;

                        if (spec.flags() == 0 && shapes[regexRow].specialized()) {
                            int predicted = specializedOutcomes(shapes[regexRow], text);
                            if (bits != predicted) {
                                throw new IllegalStateException(
                                        "M3RegexShape/JDK parity drift regex="
                                                + regexRow
                                                + " string="
                                                + stringRow);
                            }
                        }
                    }

                    outcomes[pair] = (byte) bits;
                    if (valid[regexRow] != 0) {
                        if ((bits & MATCHES) != 0) matches[regexRow]++;
                        if ((bits & LOOKING_AT) != 0) lookingAt[regexRow]++;
                        if ((bits & FIND) != 0) find[regexRow]++;
                        classes[regexRow] |= (byte) (1 << bits);
                    }

                    difficulty[pair] =
                            difficulty(
                                    regexSignal,
                                    stringSignal,
                                    shapes[regexRow],
                                    valid[regexRow] != 0,
                                    bits,
                                    distance,
                                    jaccard255);
                    progress.worked(1);
                }
            }
            progress.checkCanceled();

            String root =
                    root(
                            checkedRegexes,
                            checkedStrings,
                            valid,
                            shape,
                            syntax,
                            regexCode,
                            regexRegex,
                            stringCode,
                            stringRegex,
                            matches,
                            lookingAt,
                            find,
                            classes,
                            outcomes,
                            hamming,
                            jaccard,
                            difficulty);

            return new M3RegexStringTrialImage(
                    regexCount,
                    stringCount,
                    flags,
                    syntax,
                    valid,
                    shape,
                    regexCode,
                    regexRegex,
                    stringCode,
                    stringRegex,
                    matches,
                    lookingAt,
                    find,
                    classes,
                    outcomes,
                    hamming,
                    jaccard,
                    difficulty,
                    root);
        } finally {
            progress.done();
        }
    }

    public int regexCount() { return regexCount; }
    public int stringCount() { return stringCount; }
    public int pairCount() { return outcomes.length; }
    public String rootHash() { return rootHash; }
    public long primitivePayloadBytes() {
        return 31L * regexCount + 8L * stringCount + 7L * outcomes.length;
    }

    public boolean regexValid(int regexRow) {
        checkRegex(regexRow);
        return regexValid[regexRow] != 0;
    }

    public int regexFlags(int regexRow) {
        checkRegex(regexRow);
        return regexFlags[regexRow];
    }

    public int regexSyntaxErrorIndex(int regexRow) {
        checkRegex(regexRow);
        return syntaxErrorIndex[regexRow];
    }

    public M3RegexShape.Kind regexKind(int regexRow) {
        checkRegex(regexRow);
        return M3RegexShape.Kind.values()[Byte.toUnsignedInt(regexShape[regexRow])];
    }

    public int regexCodeScore(int regexRow) {
        checkRegex(regexRow);
        return regexCodeScore[regexRow];
    }

    public int regexRegexScore(int regexRow) {
        checkRegex(regexRow);
        return regexRegexScore[regexRow];
    }

    public int stringCodeScore(int stringRow) {
        checkString(stringRow);
        return stringCodeScore[stringRow];
    }

    public int stringRegexScore(int stringRow) {
        checkString(stringRow);
        return stringRegexScore[stringRow];
    }

    public int regexMatchesCount(int regexRow) {
        checkRegex(regexRow);
        return matchesCount[regexRow];
    }

    public int regexLookingAtCount(int regexRow) {
        checkRegex(regexRow);
        return lookingAtCount[regexRow];
    }

    public int regexFindCount(int regexRow) {
        checkRegex(regexRow);
        return findCount[regexRow];
    }

    public int regexOutcomeClassMask(int regexRow) {
        checkRegex(regexRow);
        return Byte.toUnsignedInt(outcomeClassMask[regexRow]);
    }

    public int regexOutcomeDiversity(int regexRow) {
        return Integer.bitCount(regexOutcomeClassMask(regexRow));
    }

    public int regexDifficultyScore(int regexRow) {
        checkRegex(regexRow);
        long score =
                regexRegexScore[regexRow]
                        + regexCodeScore[regexRow] / 4L
                        + regexOutcomeDiversity(regexRow) * 64L
                        + (regexValid[regexRow] == 0 ? 32L : 0L);
        return (int) Math.min(Integer.MAX_VALUE, score);
    }

    public boolean matches(int regexRow, int stringRow) {
        return (validOutcome(regexRow, stringRow) & MATCHES) != 0;
    }

    public boolean lookingAt(int regexRow, int stringRow) {
        return (validOutcome(regexRow, stringRow) & LOOKING_AT) != 0;
    }

    public boolean find(int regexRow, int stringRow) {
        return (validOutcome(regexRow, stringRow) & FIND) != 0;
    }

    public int simHashDistance(int regexRow, int stringRow) {
        return Byte.toUnsignedInt(simHashDistance[pair(regexRow, stringRow)]);
    }

    public double estimatedJaccard(int regexRow, int stringRow) {
        return Byte.toUnsignedInt(estimatedJaccard255[pair(regexRow, stringRow)]) / 255.0;
    }

    public int difficultyScore(int regexRow, int stringRow) {
        return difficulty[pair(regexRow, stringRow)];
    }

    public int[] topPairs(int limit) {
        if (limit < 0) throw new IllegalArgumentException("negative top-pair limit");
        int count = Math.min(limit, outcomes.length);
        long[] keys = new long[outcomes.length];
        for (int pair = 0; pair < keys.length; pair++) {
            keys[pair] =
                    ((long) difficulty[pair] << 32)
                            | (0xffff_ffffL - Integer.toUnsignedLong(pair));
        }
        Arrays.sort(keys);
        int[] result = new int[count];
        for (int index = 0; index < count; index++) {
            long key = keys[keys.length - 1 - index];
            result[index] = (int) (0xffff_ffffL - (key & 0xffff_ffffL));
        }
        return result;
    }

    public int[] topRegexRows(int limit) {
        if (limit < 0) throw new IllegalArgumentException("negative top-regex limit");
        int count = Math.min(limit, regexCount);
        long[] keys = new long[regexCount];
        for (int regexRow = 0; regexRow < regexCount; regexRow++) {
            keys[regexRow] =
                    ((long) regexDifficultyScore(regexRow) << 32)
                            | (0xffff_ffffL - Integer.toUnsignedLong(regexRow));
        }
        Arrays.sort(keys);
        int[] result = new int[count];
        for (int index = 0; index < count; index++) {
            long key = keys[keys.length - 1 - index];
            result[index] = (int) (0xffff_ffffL - (key & 0xffff_ffffL));
        }
        return result;
    }

    public int[] topPairsForRegex(int regexRow, int limit) {
        checkRegex(regexRow);
        if (limit < 0) throw new IllegalArgumentException("negative per-regex limit");
        int count = Math.min(limit, stringCount);
        long[] keys = new long[stringCount];
        int base = regexRow * stringCount;
        for (int stringRow = 0; stringRow < stringCount; stringRow++) {
            int pair = base + stringRow;
            keys[stringRow] =
                    ((long) difficulty[pair] << 32)
                            | (0xffff_ffffL - Integer.toUnsignedLong(pair));
        }
        Arrays.sort(keys);
        int[] result = new int[count];
        for (int index = 0; index < count; index++) {
            long key = keys[keys.length - 1 - index];
            result[index] = (int) (0xffff_ffffL - (key & 0xffff_ffffL));
        }
        return result;
    }

    public int regexRowOfPair(int pair) {
        checkPair(pair);
        return pair / stringCount;
    }

    public int stringRowOfPair(int pair) {
        checkPair(pair);
        return pair % stringCount;
    }

    private int validOutcome(int regexRow, int stringRow) {
        checkRegex(regexRow);
        if (regexValid[regexRow] == 0) {
            throw new IllegalStateException("regex row is syntactically invalid");
        }
        return Byte.toUnsignedInt(outcomes[pair(regexRow, stringRow)]);
    }

    private int pair(int regexRow, int stringRow) {
        checkRegex(regexRow);
        checkString(stringRow);
        return regexRow * stringCount + stringRow;
    }

    private void checkRegex(int row) {
        if (row < 0 || row >= regexCount) throw new IndexOutOfBoundsException("regex row");
    }

    private void checkString(int row) {
        if (row < 0 || row >= stringCount) throw new IndexOutOfBoundsException("string row");
    }

    private void checkPair(int row) {
        if (row < 0 || row >= outcomes.length) throw new IndexOutOfBoundsException("pair row");
    }

    private static int specializedOutcomes(M3RegexShape shape, String text) {
        String literal = shape.literal();
        boolean equal = text.equals(literal);
        return switch (shape.kind()) {
            case LITERAL ->
                    (equal ? MATCHES : 0)
                            | (text.startsWith(literal) ? LOOKING_AT : 0)
                            | (text.contains(literal) ? FIND : 0);
            case STRICT_PREFIX ->
                    (equal ? MATCHES : 0)
                            | (text.startsWith(literal) ? LOOKING_AT | FIND : 0);
            case STRICT_SUFFIX ->
                    (equal ? MATCHES | LOOKING_AT : 0)
                            | (text.endsWith(literal) ? FIND : 0);
            case STRICT_EXACT -> equal ? MATCHES | LOOKING_AT | FIND : 0;
            case GENERAL -> throw new IllegalArgumentException("general regex cannot specialize");
        };
    }

    private static int difficulty(
            M3CodeTextSignals.Snapshot regex,
            M3CodeTextSignals.Snapshot string,
            M3RegexShape shape,
            boolean valid,
            int outcomes,
            int simDistance,
            int jaccard255) {
        long score =
                regex.regexScore()
                        + string.codeScore()
                        + (shape.kind() == M3RegexShape.Kind.GENERAL ? 24L : 4L)
                        + (valid ? 0L : 32L)
                        + (64L - simDistance)
                        + jaccard255 / 4L;
        int acceptedModes = Integer.bitCount(outcomes & (MATCHES | LOOKING_AT | FIND));
        if (acceptedModes > 0 && acceptedModes < 3) score += 16L;
        if (string.likelyCode()) score += 12L;
        if (regex.likelyRegex()) score += 12L;
        return (int) Math.min(Integer.MAX_VALUE, score);
    }

    private static String root(
            List<RegexSpec> regexes,
            List<? extends CharSequence> strings,
            byte[] valid,
            byte[] shape,
            int[] syntax,
            int[] regexCode,
            int[] regexRegex,
            int[] stringCode,
            int[] stringRegex,
            int[] matchesCount,
            int[] lookingAtCount,
            int[] findCount,
            byte[] outcomeClasses,
            byte[] outcomes,
            byte[] hamming,
            byte[] jaccard,
            int[] difficulty) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            frame(digest, "M3_REGEX_STRING_TRIAL_IMAGE_V2");
            frame(digest, Integer.toString(regexes.size()));
            frame(digest, Integer.toString(strings.size()));
            for (int row = 0; row < regexes.size(); row++) {
                RegexSpec regex = regexes.get(row);
                frame(digest, regex.expression());
                frame(digest, Integer.toString(regex.flags()));
                frame(digest, Integer.toString(Byte.toUnsignedInt(valid[row])));
                frame(digest, Integer.toString(Byte.toUnsignedInt(shape[row])));
                frame(digest, Integer.toString(syntax[row]));
                frame(digest, Integer.toString(regexCode[row]));
                frame(digest, Integer.toString(regexRegex[row]));
                frame(digest, Integer.toString(matchesCount[row]));
                frame(digest, Integer.toString(lookingAtCount[row]));
                frame(digest, Integer.toString(findCount[row]));
                frame(digest, Integer.toString(Byte.toUnsignedInt(outcomeClasses[row])));
            }
            for (int row = 0; row < strings.size(); row++) {
                frame(digest, strings.get(row).toString());
                frame(digest, Integer.toString(stringCode[row]));
                frame(digest, Integer.toString(stringRegex[row]));
            }
            digest.update(outcomes);
            digest.update(hamming);
            digest.update(jaccard);
            for (int value : difficulty) updateInt(digest, value);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static void frame(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateInt(digest, bytes.length);
        digest.update(bytes);
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }
}
