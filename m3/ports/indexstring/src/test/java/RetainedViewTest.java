// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package com.synexia.indexstring;

import com.m3.text.compat.M3Text;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RetainedViewTest {
  private static int checks;
  private static void check(boolean condition) {
    checks++;
    if (!condition) throw new AssertionError("check " + checks);
  }
  private static void expect(Class<? extends Throwable> type, Throwing action) throws Exception {
    checks++;
    try { action.run(); } catch (Throwable error) {
      if (type.isInstance(error)) return;
      throw new AssertionError("expected " + type + ", got " + error, error);
    }
    throw new AssertionError("expected " + type);
  }
  @FunctionalInterface private interface Throwing { void run() throws Exception; }
  private static FrozenChars atom(String value) { return FrozenChars.copyOf(value.toCharArray()); }
  private static M3Text text(String value, int cut) {
    return M3Text.fromFrozen(atom(value.substring(0, cut)), atom(value.substring(cut)));
  }
  private static Object field(Object value, String name) throws Exception {
    Field field = value.getClass().getDeclaredField(name);
    field.setAccessible(true); return field.get(value);
  }
  private static Set<Object> payloads(MIndexJoinedChars value) throws Exception {
    Set<Object> result = Collections.newSetFromMap(new IdentityHashMap<>());
    Object body = field(value, "body");
    for (FrozenChars atom : (FrozenChars[]) field(body, "segments")) {
      Object data = field(atom, "data");
      result.add(data != null ? data : field(field(atom, "utf16"), "mapped"));
    }
    return result;
  }
  private static byte[] bytes(ByteBuffer value) {
    byte[] bytes = new byte[value.remaining()]; value.get(bytes); return bytes;
  }
  private static void regex(String value, M3Text text, String regex, int flags) {
    Pattern pattern = Pattern.compile(regex, flags);
    Matcher expected = pattern.matcher(value), actual = text.matcher(pattern);
    while (true) {
      boolean found = expected.find(); check(found == actual.find());
      if (!found) break;
      check(expected.start() == actual.start()); check(expected.end() == actual.end());
      check(expected.groupCount() == actual.groupCount());
      for (int group = 0; group <= expected.groupCount(); group++) {
        check(java.util.Objects.equals(expected.group(group), actual.group(group)));
        check(expected.start(group) == actual.start(group)); check(expected.end(group) == actual.end(group));
      }
    }
    for (boolean transparent : new boolean[] {false, true}) {
      for (boolean anchoring : new boolean[] {false, true}) {
        int from = Math.min(1, value.length()), to = Math.max(from, value.length() - 1);
        expected.reset().region(from, to).useTransparentBounds(transparent).useAnchoringBounds(anchoring);
        actual.reset().region(from, to).useTransparentBounds(transparent).useAnchoringBounds(anchoring);
        while (true) {
          boolean found = expected.find(); check(found == actual.find());
          if (!found) break;
          check(expected.start() == actual.start()); check(expected.end() == actual.end());
        }
      }
    }
  }
  // Reader cursors are local; the canonical backing payload remains shared.
  private static void joinedStreamCursors() throws Exception {
    positionedCharSegmentsResetToTheirVisibleStarts();
    positionedByteSegmentsResetToTheirVisibleStarts();
    characterMarksReplayAcrossNonzeroAndEmptyDescriptors();
    byteMarksReplayAcrossNonzeroAndEmptyDescriptors();
    characterReadersOwnIndependentDescriptorCursors();
    byteReadersOwnIndependentDescriptorCursors();
    scalarCharacterReadsHonorTheDeclaredLogicalPrefix();
    scalarByteReadsHonorTheDeclaredLogicalPrefix();
    invalidDescriptorCoverageIsRefusedAtAdmission();
    characterResetSkipAndClosePreserveCallerMarkAndPeer();
    byteResetSkipAndClosePreserveCallerMarkAndPeer();
    existingZeroOriginAndClosedStreamContractsRemain();
    randomizedCharacterDescriptorsMatchJdkReaderStateTransitions();
    randomizedByteDescriptorsMatchJdkInputStreamStateTransitions();
  }

  private static void positionedCharSegmentsResetToTheirVisibleStarts() throws Exception {
    CharBuffer[] spans = {chars("AB", false), chars("", false), chars("\ud800C\udfff", false)};
    try (Reader input = MIndexJoinedStreams.chars(spans, 5)) {
      check(("AB\ud800C\udfff").equals(allChars(input)));
      input.reset();
      check(("AB\ud800C\udfff").equals(allChars(input)));
      input.reset();
      check(("AB\ud800C\udfff").equals(allChars(input)));
    }
  }

  private static void positionedByteSegmentsResetToTheirVisibleStarts() throws Exception {
    ByteBuffer[] spans = {bytes(new byte[] {0, -1}, false), bytes(new byte[0], false),
        bytes(new byte[] {9, -128, 7}, false)};
    byte[] expected = {0, -1, 9, -128, 7};
    try (InputStream input = MIndexJoinedStreams.bytes(spans, expected.length)) {
      check(Arrays.equals(expected, input.readAllBytes()));
      input.reset();
      check(Arrays.equals(expected, input.readAllBytes()));
    }
  }

  private static void characterMarksReplayAcrossNonzeroAndEmptyDescriptors() throws Exception {
    CharBuffer[] spans = {chars("A\ud83d", true), chars("", true), chars("\ude00Z", true)};
    try (Reader input = MIndexJoinedStreams.chars(spans, 4)) {
      check('A' == input.read());
      input.mark(0);
      check(("\ud83d\ude00Z").equals(allChars(input)));
      input.reset();
      check(("\ud83d\ude00Z").equals(allChars(input)));
      input.reset();
      check(2 == input.skip(2));
      input.mark(0);
      check('Z' == input.read());
      input.reset();
      check('Z' == input.read());
      check(-1 == input.read());
      input.mark(0);
      input.reset();
      check(-1 == input.read());
    }
  }

  private static void byteMarksReplayAcrossNonzeroAndEmptyDescriptors() throws Exception {
    ByteBuffer[] spans = {bytes(new byte[] {1, 2}, true), bytes(new byte[0], true),
        bytes(new byte[] {-1, 4}, true)};
    try (InputStream input = MIndexJoinedStreams.bytes(spans, 4)) {
      check(1 == input.read());
      input.mark(0);
      check(Arrays.equals(new byte[] {2, -1, 4}, input.readAllBytes()));
      input.reset();
      check(Arrays.equals(new byte[] {2, -1, 4}, input.readAllBytes()));
    }
  }

  private static void characterReadersOwnIndependentDescriptorCursors() throws Exception {
    CharBuffer first = chars("ab", false);
    CharBuffer second = chars("cd", false);
    CharBuffer[] spans = {first, second};
    try (Reader one = MIndexJoinedStreams.chars(spans, 4);
         Reader two = MIndexJoinedStreams.chars(spans, 4)) {
      check('a' == one.read());
      check('a' == two.read());
      check(1 == first.position());
      check(1 == second.position());
      first.position(first.limit());
      second.limit(1);
      spans[0] = chars("XX", false);
      check(("bcd").equals(allChars(one)));
      check(("bcd").equals(allChars(two)));
    }
  }

  private static void byteReadersOwnIndependentDescriptorCursors() throws Exception {
    ByteBuffer first = bytes(new byte[] {1, 2}, false);
    ByteBuffer second = bytes(new byte[] {3, 4}, false);
    ByteBuffer[] spans = {first, second};
    try (InputStream one = MIndexJoinedStreams.bytes(spans, 4);
         InputStream two = MIndexJoinedStreams.bytes(spans, 4)) {
      check(1 == one.read());
      check(1 == two.read());
      check(1 == first.position());
      check(1 == second.position());
      first.position(first.limit());
      second.limit(1);
      spans[0] = bytes(new byte[] {9, 9}, false);
      check(Arrays.equals(new byte[] {2, 3, 4}, one.readAllBytes()));
      check(Arrays.equals(new byte[] {2, 3, 4}, two.readAllBytes()));
    }
  }

  private static void scalarCharacterReadsHonorTheDeclaredLogicalPrefix() throws Exception {
    for (int length = 0; length <= 3; length++) {
      try (Reader input = MIndexJoinedStreams.chars(new CharBuffer[] {chars("ABC", false)}, length)) {
        check(("ABC".substring(0, length)).equals(allChars(input)));
        check(-1 == input.read());
        check(0 == input.read(new char[0], 0, 0));
        check(0 == input.skip(Long.MAX_VALUE));
        input.reset();
        check(("ABC".substring(0, length)).equals(allChars(input)));
      }
    }
  }

  private static void scalarByteReadsHonorTheDeclaredLogicalPrefix() throws Exception {
    for (int length = 0; length <= 3; length++) {
      try (InputStream input = MIndexJoinedStreams.bytes(new ByteBuffer[] {bytes(new byte[] {1, 2, 3}, false)}, length)) {
        for (int i = 0; i < length; i++) check(i + 1 == input.read());
        check(-1 == input.read());
        check(0 == input.available());
        check(0 == input.read(new byte[0], 0, 0));
        check(0 == input.skip(Long.MAX_VALUE));
        input.reset();
        check(Arrays.equals(Arrays.copyOf(new byte[] {1, 2, 3}, length), input.readAllBytes()));
      }
    }
  }

  private static void invalidDescriptorCoverageIsRefusedAtAdmission() throws Exception {
    expect(IllegalArgumentException.class, () -> MIndexJoinedStreams.chars(new CharBuffer[0], -1));
    expect(IllegalArgumentException.class, () -> MIndexJoinedStreams.bytes(new ByteBuffer[0], -1));
    expect(IllegalStateException.class, () -> MIndexJoinedStreams.chars(new CharBuffer[] {chars("a", false)}, 2));
    expect(IllegalStateException.class, () -> MIndexJoinedStreams.bytes(new ByteBuffer[] {bytes(new byte[] {1}, false)}, 2));
    expect(NullPointerException.class, () -> MIndexJoinedStreams.chars(null, 0));
    expect(NullPointerException.class, () -> MIndexJoinedStreams.bytes(null, 0));
    expect(NullPointerException.class, () -> MIndexJoinedStreams.chars(new CharBuffer[] {null}, 0));
    expect(NullPointerException.class, () -> MIndexJoinedStreams.bytes(new ByteBuffer[] {null}, 0));
  }

  private static void characterResetSkipAndClosePreserveCallerMarkAndPeer() throws Exception {
    CharBuffer span = chars("abcd", false);
    span.mark();
    int limit = span.limit();
    Reader one = MIndexJoinedStreams.chars(new CharBuffer[] {span}, 4);
    try (Reader peer = MIndexJoinedStreams.chars(new CharBuffer[] {span}, 4)) {
      check(2 == one.skip(2));
      one.mark(0);
      check('c' == one.read());
      one.reset();
      check('c' == one.read());
      one.close();
      check(("abcd").equals(allChars(peer)));
    }
    check(limit == span.limit());
    span.reset();
    check(1 == span.position());
  }

  private static void byteResetSkipAndClosePreserveCallerMarkAndPeer() throws Exception {
    ByteBuffer span = bytes(new byte[] {1, 2, 3, 4}, true);
    span.mark();
    int limit = span.limit();
    InputStream one = MIndexJoinedStreams.bytes(new ByteBuffer[] {span}, 4);
    try (InputStream peer = MIndexJoinedStreams.bytes(new ByteBuffer[] {span}, 4)) {
      check(2 == one.skip(2));
      one.mark(0);
      check(3 == one.read());
      one.reset();
      check(3 == one.read());
      one.close();
      check(Arrays.equals(new byte[] {1, 2, 3, 4}, peer.readAllBytes()));
    }
    check(limit == span.limit());
    span.reset();
    check(1 == span.position());
  }

  private static void existingZeroOriginAndClosedStreamContractsRemain() throws Exception {
    InputStream bytes = MIndexJoinedStreams.bytes(new ByteBuffer[] {ByteBuffer.wrap(new byte[] {0, -1})}, 2);
    check(bytes.markSupported());
    check(0 == bytes.skip(-1));
    check(0 == bytes.read());
    bytes.mark(0);
    check(255 == bytes.read());
    bytes.reset();
    check(255 == bytes.read());
    check(-1 == bytes.read());
    bytes.close(); bytes.close();
    expect(IOException.class, bytes::read);
    expect(IOException.class, bytes::reset);
    expect(IOException.class, bytes::available);
    Reader chars = MIndexJoinedStreams.chars(new CharBuffer[] {CharBuffer.wrap("ab")}, 2);
    check(chars.markSupported());
    check(chars.ready());
    expect(IllegalArgumentException.class, () -> chars.skip(-1));
    expect(IllegalArgumentException.class, () -> chars.mark(-1));
    expect(IndexOutOfBoundsException.class, () -> chars.read(new char[1], 1, 1));
    check('a' == chars.read());
    chars.mark(0);
    check('b' == chars.read());
    chars.reset(); check('b' == chars.read());
    chars.close(); chars.close();
    expect(IOException.class, chars::read);
    expect(IOException.class, chars::reset);
    expect(IOException.class, chars::ready);
  }

  private static void randomizedCharacterDescriptorsMatchJdkReaderStateTransitions() throws Exception {
    Random random = new Random(0x553136L);
    for (int trial = 0; trial < 256; trial++) {
      int size = random.nextInt(65);
      char[] text = new char[size];
      for (int i = 0; i < size; i++) text[i] = (char) random.nextInt(65536);
      int split = random.nextInt(size + 1);
      int length = random.nextInt(size + 1);
      CharBuffer[] spans = {chars(new String(text, 0, split), (trial & 1) == 0), chars("", false),
          chars(new String(text, split, size - split), (trial & 1) != 0)};
      try (Reader actual = MIndexJoinedStreams.chars(spans, length);
           Reader expected = new CharArrayReader(Arrays.copyOf(text, length))) {
        for (int operation = 0; operation < 96; operation++) {
          switch (random.nextInt(5)) {
            case 0 -> check(expected.read() == actual.read());
            case 1 -> {
              int width = 1 + random.nextInt(7);
              char[] left = new char[width + 2], right = new char[width + 2];
              check(expected.read(left, 1, width) == actual.read(right, 1, width));
              check(Arrays.equals(left, right));
            }
            case 2 -> { long skip = random.nextInt(9); check(expected.skip(skip) == actual.skip(skip)); }
            case 3 -> { expected.mark(0); actual.mark(0); }
            default -> { expected.reset(); actual.reset(); }
          }
        }
      }
      check(1 == spans[0].position());
      check(1 == spans[2].position());
    }
  }

  private static void randomizedByteDescriptorsMatchJdkInputStreamStateTransitions() throws Exception {
    Random random = new Random(0x42595445L);
    for (int trial = 0; trial < 256; trial++) {
      int size = random.nextInt(65);
      byte[] text = new byte[size]; random.nextBytes(text);
      int split = random.nextInt(size + 1);
      int length = random.nextInt(size + 1);
      ByteBuffer[] spans = {bytes(Arrays.copyOfRange(text, 0, split), (trial & 1) == 0), bytes(new byte[0], false),
          bytes(Arrays.copyOfRange(text, split, size), (trial & 1) != 0)};
      try (InputStream actual = MIndexJoinedStreams.bytes(spans, length);
           InputStream expected = new ByteArrayInputStream(Arrays.copyOf(text, length))) {
        for (int operation = 0; operation < 96; operation++) {
          switch (random.nextInt(5)) {
            case 0 -> check(expected.read() == actual.read());
            case 1 -> {
              int width = 1 + random.nextInt(7);
              byte[] left = new byte[width + 2], right = new byte[width + 2];
              check(expected.read(left, 1, width) == actual.read(right, 1, width));
              check(Arrays.equals(left, right));
            }
            case 2 -> { long skip = random.nextInt(9) - 2; check(expected.skip(skip) == actual.skip(skip)); }
            case 3 -> { expected.mark(0); actual.mark(0); }
            default -> { expected.reset(); actual.reset(); }
          }
          check(expected.available() == actual.available());
        }
      }
      check(1 == spans[0].position());
      check(1 == spans[2].position());
    }
  }

  private static CharBuffer chars(String text, boolean direct) {
    CharBuffer buffer = direct
        ? ByteBuffer.allocateDirect((text.length() + 2) * Character.BYTES).order(ByteOrder.LITTLE_ENDIAN).asCharBuffer()
        : CharBuffer.allocate(text.length() + 2);
    buffer.put('#').put(text).put('!');
    buffer.limit(1 + text.length()).position(1);
    return buffer.asReadOnlyBuffer();
  }

  private static ByteBuffer bytes(byte[] text, boolean direct) {
    ByteBuffer buffer = direct ? ByteBuffer.allocateDirect(text.length + 2) : ByteBuffer.allocate(text.length + 2);
    buffer.put((byte) '#').put(text).put((byte) '!');
    buffer.limit(1 + text.length).position(1);
    return buffer.asReadOnlyBuffer();
  }

  private static String allChars(Reader reader) throws IOException {
    StringBuilder value = new StringBuilder();
    for (int c; (c = reader.read()) != -1;) value.append((char) c);
    return value.toString();
  }

  public static void main(String[] args) throws Exception {
    MIndexJoinedChars first = MIndexJoinedChars.of(atom("unique-A-\u1234\u4321"));
    MIndexJoinedChars second = MIndexJoinedChars.of(atom("unique-B-\u5763\u9876"));
    Set<Object> before = payloads(first); before.addAll(payloads(second));
    MIndexJoinedChars joined = first.concat(second);
    check(payloads(joined).equals(before));
    check(joined == first.concat(second));
    check(joined.length() == first.length() + second.length());
    MIndexJoinedChars head = joined.subSequence(0, 4), tail = joined.subSequence(4, joined.length());
    check(head.concat(tail) == joined);
    check(joined.concat(MIndexJoinedChars.of()) == joined);
    check(MIndexJoinedChars.of().concat(joined) == joined);
    MIndexJoinedChars cut = first.subSequence(2, 7).concat(second.subSequence(1, 8));
    check(before.containsAll(payloads(cut)));
    check(cut.toString().equals(first.toString().substring(2,7)+second.toString().substring(1,8)));
    char[] destination = new char[cut.length()+4]; Arrays.fill(destination, '#');
    cut.copyTo(0,destination,2,cut.length());
    check(new String(destination,2,cut.length()).equals(cut.toString()));
    check(destination[0]=='#' && destination[1]=='#' && destination[destination.length-1]=='#');
    char[] intact=destination.clone();
    expect(IndexOutOfBoundsException.class,()->cut.copyTo(0,destination,3,cut.length()+1));
    check(Arrays.equals(destination,intact));
    expect(IndexOutOfBoundsException.class,()->cut.copyTo(-1,destination,0,0));
    expect(NullPointerException.class,()->cut.concat(null));
    cut.copyTo(cut.length(),destination,destination.length,0);

    // Every UTF-16 code unit is admitted, sliced, hashed and streamed exactly.
    for (int unit=0;unit<=65535;unit++) {
      String value=new String(new char[]{'x',(char)unit,'y'});
      M3Text sample=text(value,2);
      check(sample.contentEquals(value)); check(sample.hashCode()==value.hashCode());
      check(Arrays.equals(sample.chars().toArray(),value.chars().toArray()));
      check(Arrays.equals(sample.codePoints().toArray(),value.codePoints().toArray()));
      check(sample.substring(1,2).charAt(0)==(char)unit);
    }
    String[] seams={"\uD83D\uDE03","a\uD83D\uDE03b","\uD83Dx\uDE03","\0\uD800\uDC00\uDFFF","\uD800\uD800\uDC00"};
    for(String value:seams) for(int seam=0;seam<=value.length();seam++) {
      M3Text sample=text(value,seam);
      check(Arrays.equals(sample.codePoints().toArray(),value.codePoints().toArray()));
      for(int start=0;start<=value.length();start++) for(int end=start;end<=value.length();end++) {
        check(sample.codePointCount(start,end)==value.codePointCount(start,end));
        check(sample.substring(start,end).asString().equals(value.substring(start,end)));
      }
    }
    Random random=new Random(0x5345414dL);
    Charset[] charsets={StandardCharsets.UTF_8,StandardCharsets.UTF_16BE,StandardCharsets.UTF_16LE,
        StandardCharsets.ISO_8859_1,Charset.forName("ISO-2022-JP")};
    for(int iteration=0;iteration<6000;iteration++) {
      char[] raw=new char[random.nextInt(64)];
      for(int i=0;i<raw.length;i++) raw[i]=(char)(random.nextBoolean()?random.nextInt(65536):'a'+random.nextInt(4));
      String value=new String(raw); int seam=random.nextInt(value.length()+1);
      M3Text sample=text(value,seam), alternate=text(value,random.nextInt(value.length()+1));
      check(sample.equals(alternate)); check(sample.hashCode()==value.hashCode());
      check(!sample.equals(value) && !value.equals(sample));
      check(sample.compareTo(alternate)==0);
      String other=value+"\0";
      check(sample.compareTo(text(other,0))==value.compareTo(other));
      for(int i=0;i<value.length();i++) check(sample.codePointAt(i)==value.codePointAt(i));
      for(int i=1;i<=value.length();i++) check(sample.codePointBefore(i)==value.codePointBefore(i));
      int left=random.nextInt(value.length()+1),right=left+random.nextInt(value.length()-left+1);
      String needle=iteration%3==0?"zz":value.substring(left,right);
      for(int from:new int[]{Integer.MIN_VALUE,-1,0,left,right,value.length(),Integer.MAX_VALUE}) {
        check(sample.indexOf(needle,from)==value.indexOf(needle,from));
        check(sample.lastIndexOf(needle,from)==value.lastIndexOf(needle,from));
      }
      check(sample.startsWith(needle)==value.startsWith(needle));
      check(sample.endsWith(needle)==value.endsWith(needle));
      M3Text rejoined=sample.substring(0,seam).concat(sample.substring(seam));
      check(rejoined.equals(sample));
      char[] output=sample.toCharArray(); check(Arrays.equals(output,raw));
      if(output.length>0) {output[0]^=1;check(sample.charAt(0)==raw[0]);}
      StringBuilder builder=new StringBuilder();sample.writeTo(builder);check(builder.toString().equals(value));
      for(Charset charset:charsets) {
        ByteBuffer encoded=sample.encode(charset,CodingErrorAction.REPLACE,CodingErrorAction.REPLACE);
        check(encoded.isReadOnly());check(Arrays.equals(bytes(encoded),value.getBytes(charset)));
      }
    }
    String oversized = "a".repeat(65537);
    M3Text bounded = text(oversized + "z", 32000);
    check(bounded.indexOf(oversized) == 0);
    check(bounded.lastIndexOf(oversized) == 0);
    check(bounded.indexOf("b" + oversized) == -1);
    M3Text malformed=text("\uD800",0);
    expect(CharacterCodingException.class,()->malformed.encode(StandardCharsets.UTF_8,CodingErrorAction.REPORT,CodingErrorAction.REPORT));
    check(malformed.encode(StandardCharsets.UTF_8,CodingErrorAction.IGNORE,CodingErrorAction.IGNORE).remaining()==0);
    AtomicInteger admissions=new AtomicInteger();
    M3Text admitted=M3Text.fromString("ab",value->{admissions.incrementAndGet();return MIndexJoinedChars.of(atom(value));});
    check(admissions.get()==1 && admitted.contentEquals("ab"));
    expect(IllegalArgumentException.class,()->M3Text.fromString("ab",ignored->MIndexJoinedChars.of(atom("ba"))));
    expect(NullPointerException.class,()->M3Text.fromString("ab",ignored->null));
    expect(IndexOutOfBoundsException.class,()->admitted.codePointBefore(0));
    expect(NullPointerException.class,()->admitted.indexOf(null));
    for(String value:new String[]{"aabb aaa\nword_word 12\uD83D\uDE03", "\uD83D\uDE03\uD83D\uDE03", "", "\0\uD800x\uDC00"}) {
      for(int seam=0;seam<=value.length();seam++) {
        M3Text sample=text(value,seam);
        for(String regex:new String[]{"(.)\\1?","(?<=a)b","(?=.)","^.*$","\\b\\w+\\b","(a+)?(b*)","(a)\\1","\\p{L}+","\\X"}) {
          for(int flags:new int[]{0,Pattern.UNICODE_CHARACTER_CLASS|Pattern.MULTILINE}) regex(value,sample,regex,flags);
        }
        check(sample.replaceAll("(a)","<$1>").equals(value.replaceAll("(a)","<$1>")));
        for(int limit:new int[]{-1,0,1,3}) check(Arrays.equals(sample.split("a|\\s",limit),value.split("a|\\s",limit)));
      }
    }

    // Actual file-backed UTF-16BE owner, with deliberately odd byte alignment.
    Path image=Files.createTempFile("m3-view-", ".bin");
    String mappedText="map-\0-\uD83D\uDE03-\uD800-end";
    byte[] physical=new byte[1+mappedText.length()*2];physical[0]=42;
    for(int i=0;i<mappedText.length();i++){physical[1+2*i]=(byte)(mappedText.charAt(i)>>>8);physical[2+2*i]=(byte)mappedText.charAt(i);}
    Files.write(image,physical);
    MIndexJoinedChars mappedJoined;
    try(FileChannel channel=FileChannel.open(image,StandardOpenOption.READ)) {
      ByteBuffer mapped=channel.map(FileChannel.MapMode.READ_ONLY,0,physical.length).asReadOnlyBuffer();
      ByteBuffer payload=mapped.slice(1,mappedText.length()*2).asReadOnlyBuffer();
      FrozenBytes frozen=FrozenBytes.mapped(payload,physical.length,FrozenBytes.hash(payload));
      FrozenChars chars=FrozenChars.mapped(frozen,mappedText.hashCode());
      check(chars.isDirect());check(chars.asReadOnlyBuffer().isDirect());
      mappedJoined=MIndexJoinedChars.of(chars.subSequence(0,7),chars.subSequence(7,chars.length()));
      check(mappedJoined.toString().equals(mappedText));
      ByteBuffer[] nativeLanes=mappedJoined.directUtf16Buffers();
      int units=0;
      for(ByteBuffer lane:nativeLanes) {
        check(lane.isReadOnly() && lane.isDirect());check(lane.remaining()%2==0);
        expect(ReadOnlyBufferException.class,()->lane.put(0,(byte)0));
        var utf16=lane.order(ByteOrder.BIG_ENDIAN).asCharBuffer();
        while(utf16.hasRemaining()) check(utf16.get()==mappedText.charAt(units++));
      }
      check(units==mappedText.length());
    }
    check(mappedJoined.toString().equals(mappedText)); // live view outlives channel closure
    MIndexJoinedChars mixed=mappedJoined.concat(MIndexJoinedChars.of(atom("-local")));
    check(mixed.toString().equals(mappedText+"-local"));
    expect(IllegalStateException.class, mixed::directUtf16Buffers);
    check(mappedJoined.subSequence(0,1).sharesBackingWith(mappedJoined));
    Files.delete(image); // Linux unlink behavior only; this is not a Windows acceptance claim.
    System.gc(); check(mappedJoined.toString().equals(mappedText));
    FrozenChars[] anchors={atom("concurrent-unique-x"),atom("concurrent-unique-y")};
    MIndexJoinedChars canonical=MIndexJoinedChars.of(anchors);
    try(var workers=Executors.newFixedThreadPool(6)) {
      Callable<Boolean> task=()->{
        for(int i=0;i<5000;i++) {
          if(MIndexJoinedChars.of(anchors)!=canonical)return false;
          M3Text view=M3Text.fromJoined(canonical);
          if(view.hashCode()!=canonical.toString().hashCode())return false;
        }
        return true;
      };
      for(var future:workers.invokeAll(List.of(task,task,task,task,task,task)))check(future.get());
    }
    check(canonical.length()==anchors[0].length()+anchors[1].length());
    joinedStreamCursors();
    System.out.println("RETAINED_VIEW_PASS checks="+checks+" concurrency_iterations=30000");
  }
}
