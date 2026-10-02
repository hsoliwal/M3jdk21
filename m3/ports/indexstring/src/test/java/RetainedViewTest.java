// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring;

import com.m3.text.compat.M3Text;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
      if (data != null) result.add(data);
      else {
        Object bytes = field(atom, "utf16");
        Object heap = field(bytes, "heap");
        result.add(heap != null ? heap : field(bytes, "mapped"));
      }
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
  public static void main(String[] args) throws Exception {
    FrozenByteInterner arena = new FrozenByteInterner(8, 1024);
    FrozenBytes heapAdmitted = arena.internUtf16("shared-owner-\uD800-\u0000");
    FrozenChars projected = FrozenChars.fromUtf16Bytes(heapAdmitted);
    check(projected == FrozenChars.fromUtf16Bytes(heapAdmitted));
    check(!projected.isDirect());
    MIndexJoinedChars byteBacked = MIndexJoinedChars.of(projected);
    M3Text borrowed = M3Text.fromJoined(byteBacked);
    check(borrowed.storage() == byteBacked);
    check(payloads(byteBacked).contains(field(heapAdmitted, "heap")));
    check(borrowed.toString().equals("shared-owner-\uD800-\u0000"));
    expect(IllegalStateException.class, byteBacked::directUtf16Buffers);
    arena.clear();
    check(borrowed.toString().equals("shared-owner-\uD800-\u0000"));
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
    System.out.println("RETAINED_VIEW_PASS checks="+checks+" concurrency_iterations=30000");
  }
}
