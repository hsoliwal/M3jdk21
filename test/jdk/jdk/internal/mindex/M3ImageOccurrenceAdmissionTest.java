/* Copyright 2026 Synexia Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary A mapped image coordinate is not a semantic identity without verified UTF-16 and generation
 * @requires (os.family == "linux")
 * @modules java.base/jdk.internal.mindex java.base/jdk.m3.text
 * @run main/othervm M3ImageOccurrenceAdmissionTest
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import jdk.internal.mindex.M3LexiconKey;
import jdk.internal.mindex.M3QualifiedLexicon;
import jdk.internal.mindex.M3StringBacking;
import jdk.m3.text.LocalM3Arena;
import jdk.m3.text.SharedLexiconImage;

public final class M3ImageOccurrenceAdmissionTest {
    private static int checks;
    private static void check(boolean condition, String detail) {
        if (!condition) throw new AssertionError(detail);
        checks++;
    }
    private static final class ImmutableFixture implements M3StringBacking {
        private final Map<Long,String> words = Map.of(
                1L, "alpha", 2L, "beta", 3L, "betx", 4L, "wrong-length", 5L, "lone\ud800z");
        private String text(long id) { return java.util.Objects.requireNonNull(words.get(id)); }
        public void close() { }
        public int length(long id) { return text(id).length(); }
        public int utf8Length(long id) { return text(id).getBytes(StandardCharsets.UTF_8).length; }
        public int codePointCount(long id) {
            String s=text(id); return s.codePointCount(0,s.length());
        }
        public int unpairedSurrogateCount(long id) { return 0; }
        public int hashCode(long id) { return text(id).hashCode(); }
        public char charAt(long id,int index) { return text(id).charAt(index); }
        public CharBuffer utf16View(long id) { return CharBuffer.wrap(text(id)).asReadOnlyBuffer(); }
        public ByteBuffer utf8View(long id) {
            return ByteBuffer.wrap(text(id).getBytes(StandardCharsets.UTF_8)).asReadOnlyBuffer();
        }
        public String materialize(long id) { return text(id); }
    }
    @FunctionalInterface interface Checked { void run() throws Exception; }
    private static void rejected(Class<? extends Throwable> kind, Checked action) {
        try { action.run(); }
        catch (Throwable failure) {
            if (!kind.isInstance(failure)) throw new AssertionError("wrong rejection", failure);
            checks++; return;
        }
        throw new AssertionError("expected " + kind.getSimpleName());
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("m3lex-typed-admission-");
        try {
            List<String> sorted = List.of("alpha", "beta", "lone\ud800z", "omega");
            Path file = dir.resolve("sealed-format-v1.m3lex");
            SharedLexiconImage.create(file, sorted);
            SharedLexiconImage image = SharedLexiconImage.open(file);
            var value = image.atomCoordinate(1);
            long ns = M3LexiconKey.newNamespace();
            M3QualifiedLexicon lexicon = new M3QualifiedLexicon(new ImmutableFixture());
            var key = new M3LexiconKey(M3LexiconKey.Domain.STRING,ns,1L,1L,2L);
            var entry = lexicon.intern(key,2L);
            LocalM3Arena arena = new LocalM3Arena();
            var binding = lexicon.admitImageRecord(entry,image,value,10L,7L,arena);
            check(M3QualifiedLexicon.ImageBinding.class.getConstructors().length == 0,
                    "verified binding has no forgeable public constructor");
            check(binding.atomCoordinate().equals(value), "portable value retained");
            check("beta".contentEquals(binding.verifiedLocalContent()), "verified local copy");
            check(binding.occurrence().lexiconEntry().equals(key), "semantic key retained");
            check(binding.occurrence().ownerId()==10L, "owner qualification");
            check(lexicon.isCurrent(entry,binding.occurrence()), "initial generation current");
            check(lexicon.admitImageRecord(entry,image,value,11L,7L,arena)
                    .occurrence().ownerId()==11L, "distinct owner");
            check(lexicon.invalidateOwner(10L)==1L, "generation increment");
            check(!lexicon.isCurrent(entry,binding.occurrence()), "stale owner invalidated");
            check(lexicon.isCurrent(entry,lexicon.admitImageRecord(
                    entry,image,value,10L,7L,arena).occurrence()), "new owner generation");
            rejected(IllegalArgumentException.class,()->lexicon.admitImageRecord(
                    entry,image,image.atomCoordinate(0),10L,7L,arena));
            var sameLength = lexicon.intern(new M3LexiconKey(
                    M3LexiconKey.Domain.STRING,ns,2L,1L,3L),3L);
            rejected(IllegalArgumentException.class,()->lexicon.admitImageRecord(
                    sameLength,image,value,10L,7L,arena));
            var differentLength = lexicon.intern(new M3LexiconKey(
                    M3LexiconKey.Domain.STRING,ns,3L,1L,4L),4L);
            rejected(IllegalArgumentException.class,()->lexicon.admitImageRecord(
                    differentLength,image,value,10L,7L,arena));
            var foreign = new M3QualifiedLexicon(new ImmutableFixture()).intern(key,2L);
            rejected(IllegalArgumentException.class,()->lexicon.admitImageRecord(
                    foreign,image,value,10L,7L,arena));
            rejected(IllegalArgumentException.class,()->lexicon.admitImageRecord(
                    entry,image,value,-1L,7L,arena));
            rejected(NullPointerException.class,()->lexicon.admitImageRecord(
                    entry,image,value,10L,7L,null));
            var surrogate = lexicon.intern(new M3LexiconKey(
                    M3LexiconKey.Domain.STRING,ns,4L,1L,5L),5L);
            check("lone\ud800z".contentEquals(lexicon.admitImageRecord(
                    surrogate,image,image.atomCoordinate(2),10L,8L,arena)
                    .verifiedLocalContent()), "exact unpaired surrogate units");
            check(lexicon.size()==4,"only explicit canonical entries admitted");
            // A metadata match is insufficient after a second process modifies mapped bytes.
            try(FileChannel writer=FileChannel.open(file,StandardOpenOption.WRITE)) {
                writer.write(ByteBuffer.wrap(new byte[]{(byte)'c'}),64+8L*sorted.size()+2L*sorted.get(0).length());
                writer.force(true);
            }
            rejected(IOException.class,()->lexicon.admitImageRecord(
                    entry,image,value,10L,7L,arena));
            check("beta".contentEquals(binding.verifiedLocalContent()),"old local copy intact");
            rejected(IOException.class,()->SharedLexiconImage.open(file));
            System.out.println("M3_IMAGE_OCCURRENCE_ADMISSION_PASS|checks="+checks);
        } finally {
            try(var stream=Files.list(dir)) {
                for(Path path:stream.toList()) Files.deleteIfExists(path);
            }
            Files.deleteIfExists(dir);
        }
    }
}
