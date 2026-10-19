import java.util.List;
import java.util.regex.TokenPattern;
public class EmptyFailureProbe {
 public static void main(String[] args) {
  var m=TokenPattern.<Integer>empty().matcher(List.of(1));
  System.out.println("first="+m.find()+" span="+m.span());
  System.out.println("end="+m.find()+" span="+m.span());
  System.out.println("exhausted="+m.find());
  System.out.println("hasMatch="+m.hasMatch());
  try { System.out.println("start="+m.start()); } catch (RuntimeException e) {System.out.println("start_exception="+e.getClass().getSimpleName());}
  try { System.out.println("group="+m.group()); } catch (RuntimeException e) {System.out.println("group_exception="+e.getClass().getSimpleName());}
  try { System.out.println("span="+m.span()); } catch (RuntimeException e) {System.out.println("span_exception="+e.getClass().getSimpleName());}
 }
}
