/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import com.m3.text.M3NumberSpace;
import com.m3.text.LocalM3StringPiece;

/** Contract proof for the source-manifest to M3NumberSpace owner mapping. */
public final class M3NumberSpaceContractTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("check " + checks);
        }
    }

    public static void main(String[] args) throws Exception {
        M3NumberSpace space = M3NumberSpace.INSTANCE;
        M3NumberSpace english = M3NumberSpace.forLanguage("en");
        M3NumberSpace hindi = M3NumberSpace.forLanguage("hi");
        check(M3NumberSpace.INSTANCE.languageTag().equals("und"));
        check(english.languageTag().equals("en"));
        check(hindi.languageTag().equals("hi"));
        check(english.storageIdentity().equals(hindi.storageIdentity()));
        check(english.number(42) == hindi.number(42));
        expect(NullPointerException.class, () -> M3NumberSpace.forLanguage(null));
        expect(IllegalArgumentException.class, () -> M3NumberSpace.forLanguage(" "));
        expect(IllegalArgumentException.class, () -> M3NumberSpace.forLanguage("en us"));
        check(M3NumberSpace.SOURCE_ID.equals("dictlang.numbers.0-10000"));
        check(M3NumberSpace.SOURCE_REVISION.equals("64a2ea61c73b548413fed6686a9daeeb0b9b0564"));
        check(M3NumberSpace.RECORD_ID.equals("number"));
        check(M3NumberSpace.MIN_VALUE == 0);
        check(M3NumberSpace.MAX_VALUE == 10_000);
        check(space.precomputedValueCount() == 10_001);
        check(space.number(0).storageIdentity().equals(space.number(10_000).storageIdentity()));
        check(space.number(0).encoding() == LocalM3StringPiece.Encoding.UTF16_LE);
        check(space.spelling(10_000).equals("10000"));
        check(space.parse(new View("10000")) == space.number(10_000));
        for (int value = M3NumberSpace.MIN_VALUE; value <= M3NumberSpace.MAX_VALUE; value++) {
            String canonical = Integer.toString(value);
            check(space.parse(new View(canonical)) == space.number(value));
        }
        expect(IndexOutOfBoundsException.class, () -> space.number(-1));
        expect(IndexOutOfBoundsException.class, () -> space.number(10_001));
        expect(NumberFormatException.class, () -> space.parse(new View("")));
        expect(NumberFormatException.class, () -> space.parse(new View("01")));
        expect(NumberFormatException.class, () -> space.parse(new View("10001")));

        String manifest = Files.readString(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        String row = Arrays.stream(manifest.split("\\n", -1))
                .filter(line -> line.startsWith(M3NumberSpace.SOURCE_ID + "\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("number manifest row missing"));
        String[] columns = row.split("\\t", -1);
        check(columns.length == 9);
        check(columns[8].equals(
                "source_id,record_id,min_value,max_value,precomputed_value_count,"
                        + "shared_utf16_storage,canonical_decimal_spelling,source_revision,language_tag"));
        String targetMap = Files.readString(
                Path.of("lexicon/synexia-number-target-map.tsv"), StandardCharsets.UTF_8);
        String[] targetLines = targetMap.split("\\n", -1);
        String[] targetHeader = targetLines[0].split("\\t", -1);
        check(targetHeader.length == 11 && targetHeader[0].equals("schema")
                && targetHeader[10].equals("source_revision"));
        String targetRow = Arrays.stream(targetLines)
                .filter(line -> line.startsWith("M3JDK_NUMBER_TARGET_MAP_V1\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("number target map row missing"));
        String[] targetColumns = targetRow.split("\\t", -1);
        check(targetColumns.length == 11);
        check(targetColumns[1].equals(M3NumberSpace.SOURCE_ID));
        check(targetColumns[4].equals("value,spelling,language_tag"));
        check(targetColumns[5].equals("value,language_tag"));
        check(Arrays.stream(columns[8].split(","))
                .anyMatch(field -> field.equals("language_tag")));
        check(Arrays.stream(targetColumns[4].split(","))
                .anyMatch(field -> field.equals("language_tag")));
        check(targetColumns[6].contains("com.m3.text.M3NumberSpace"));
        check(targetColumns[9].equals("ADMITTED_TYPED_RECEIVER"));
        check(targetColumns[10].equals(M3NumberSpace.SOURCE_REVISION));
        System.out.println("M3JDK_NUMBER_CONTRACT_PASS checks=" + checks + " fields=8");
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }

    private static final class View implements CharSequence {
        private final String value;

        View(String value) {
            this.value = value;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public char charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return value.subSequence(start, end);
        }

        @Override
        public String toString() {
            throw new AssertionError("number parser materialized view");
        }
    }
}
