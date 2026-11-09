import java.util.*;
import com.m3.collections.M3FilteredLists;
public class ExhaustedSubList {
  public static void main(String[] args) {
    for (boolean linked : List.of(false, true)) for (int op=0;op<3;op++) {
      List<Integer> owner=linked?new LinkedList<>(List.of(0,2,4,6)):new ArrayList<>(List.of(0,2,4,6));
      List<Integer> span=owner.subList(1,3);
      List<Integer> view=M3FilteredLists.filtered(span,x->true);
      ListIterator<Integer> cur=view.listIterator();
      cur.next(); cur.next();
      span.add(0,8); span.remove(0);
      List<Integer> before=new ArrayList<>(owner);
      try {
        if(op==0) cur.remove(); else if(op==1) cur.set(10); else cur.add(10);
        System.out.println((linked?"LinkedList":"ArrayList")+" op="+op+" SUCCESS owner="+owner+" before="+before);
      } catch(Throwable t) {System.out.println((linked?"LinkedList":"ArrayList")+" op="+op+" threw="+t+" owner="+owner);}
    }
  }
}
