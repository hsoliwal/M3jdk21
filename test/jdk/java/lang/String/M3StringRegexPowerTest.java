/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary Repeated canonical fragments preserve regex matching, range and fallback contracts
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage M3StringRegexPowerTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage -XX:-CompactStrings M3StringRegexPowerTest
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseM3StringStorage M3StringRegexPowerTest
 */
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
public final class M3StringRegexPowerTest {
    private static int checks;
    private static void parity(String s,String p){
        boolean expected=Pattern.compile(p).matcher(new StringBuilder(s)).matches();
        if(s.matches(p)!=expected)throw new AssertionError("regex power contract: "+p);checks++;
    }
    public static void main(String[] ignored){
        String a=new String(new char[]{'a','a'}), pair=new String(new char[]{'a','b'});
        for(int level=0;level<=11;level++){
            for(String p:new String[]{"a*","a*b*",".*","a*".repeat(16)})parity(a,p);
            parity(pair,"a*b*");parity(pair,".*");parity(pair.substring(1),".*");
            a=a.concat(a);pair=pair.concat(pair);
        }
        parity("","a*b*");parity("abc","a+b*c");parity("a\nb",".*");
        parity(String.join("","a","\ud83d","\ude00","b"),"a.*b");
        parity("a".repeat(8193),"a*");
        for(String p:new String[]{"*a","a**"}){
            try{"aaa".matches(p);throw new AssertionError("expected PatternSyntaxException");}
            catch(PatternSyntaxException expected){checks++;}
        }
        System.out.println("POWER_PUBLIC cases="+checks);
    }
}
