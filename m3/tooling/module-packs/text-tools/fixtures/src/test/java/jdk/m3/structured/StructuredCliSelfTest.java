package jdk.m3.structured;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
public final class StructuredCliSelfTest {
  private static void eq(Object expected, Object actual) {
    if(!Objects.equals(expected,actual))throw new AssertionError("expected "+expected+", got "+actual);
  }
  private static void run(int status,String output,String input,String... args) {
    var out=new ByteArrayOutputStream(); var err=new ByteArrayOutputStream();
    int actual=StructuredCli.run(args,new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),out,new PrintStream(err));
    eq(status,actual);eq(output,out.toString(StandardCharsets.UTF_8));
  }
  public static void main(String[] args){
    run(0,"a\n","a,b\n","csv-cut","0");
    run(0,"b\ta\n","a\tb\n","tsv-cut","1,0");
    run(0,"2\n","{\"a\":[1,2]}\n","jsonl-pointer","/a/1");
    run(0,"v\n","[sec]\nk=v\n","ini-get","sec","k");
    run(0,"abc\n","KEY=abc\n","env-get","KEY");
    run(2,"","a,b\n","csv-cut","2");
    run(2,"","a,b\n","csv-cut","-1");
    run(2,"","a,b\n","bogus","0");
    run(2,"","a,b\n","csv-cut","1","extra");
    var err=new ByteArrayOutputStream();
    int bad=StructuredCli.run(new String[]{"csv-cut","0"},new ByteArrayInputStream(new byte[]{(byte)0xc3}),new ByteArrayOutputStream(),new PrintStream(err));
    eq(2,bad);
    System.out.println("STRUCTURED_CLI_TEST_PASS");
  }
}

