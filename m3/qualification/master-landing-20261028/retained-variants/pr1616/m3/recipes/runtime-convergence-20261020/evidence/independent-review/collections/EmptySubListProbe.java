import java.util.*;
import com.m3.collections.M3FilteredLists;
public class EmptySubListProbe {
  public static void main(String[] args) {
    for (boolean filtered : List.of(false,true)) {
      List<Integer> owner=new LinkedList<>(List.of(0,6));
      List<Integer> span=owner.subList(1,1);
      ListIterator<Integer> cur=(filtered?M3FilteredLists.filtered(span,x->true):span).listIterator();
      span.add(8); span.remove(0);
      try {cur.add(10); System.out.println("filtered="+filtered+" SUCCESS owner="+owner);}
      catch(Throwable t) {System.out.println("filtered="+filtered+" threw="+t+" owner="+owner);}
    }
  }
}
