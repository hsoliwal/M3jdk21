/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
import com.synexia.indexstring.shared.SharedArrayPool;
import com.synexia.indexstring.shared.SharedChars;
import java.nio.file.Path;
public class SharedOwnerFixture {
 public static void main(String[] args)throws Exception{
  SharedChars retained;
  try(var pool=SharedArrayPool.open(Path.of(args[0]),1<<20,100,20)){
   pool.internBytes(new byte[]{42}); // exercise an odd mapped address for the next UTF16 atom
   retained=pool.internChars(0,"alpha");
   for(String word:new String[]{" ","beta","join","string","word","\u0100alpha","\ud800","\ud83d\ude00","\udfff"})pool.internChars(0,word);
   pool.internChars(17,"language-only");
   pool.concatChars(0,retained,pool.internChars(0,"beta"));
   System.out.println("OWNER_FIXTURE namespace="+pool.namespace()+" alphaRecord="+retained.handle().recordOffset()+" bytes="+pool.stats().fileBytes());
  }
  if(retained.charAt(0)!='a')throw new AssertionError("closed owner lifetime");
 }
}
