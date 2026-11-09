import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import com.m3.collections.M3FilteredLists;

public class RepairEdgeProbe {
  static int checks;
  static void equal(Object expected,Object actual) {
    checks++;
    if(!Objects.equals(expected,actual)) throw new AssertionError("expected="+expected+" actual="+actual);
  }
  static void expect(Class<? extends Throwable> type,Runnable op) {
    checks++;
    try {op.run();} catch(Throwable t) {if(type.isInstance(t)) return; throw new AssertionError(t);}
    throw new AssertionError("missing "+type.getName());
  }
  static final class ReadonlyCursorList extends AbstractList<Integer> {
    final List<Integer> values=new ArrayList<>(); int adds;
    public Integer get(int at){return values.get(at);} public int size(){return values.size();}
    public void add(int at,Integer value){adds++;values.add(at,value);}
    public Integer remove(int at){return values.remove(at);}
    public Integer set(int at,Integer value){return values.set(at,value);}
    public ListIterator<Integer> listIterator(int at){return Collections.unmodifiableList(values).listIterator(at);}
  }
  static List<Integer> form(List<Integer> owner,int kind) {
    if(kind==0) return owner;
    List<Integer> sub=owner.subList(0,owner.size());
    return switch(kind) {case 1 -> sub;case 2 -> sub.subList(0,sub.size());default -> sub.reversed();};
  }
  static void emptyWrites(List<Integer> owner,int kind) {
    List<Integer> span=form(owner,kind); int[] calls={0};
    ListIterator<Integer> cur=M3FilteredLists.filtered(span,v->{calls[0]++;return v%2==0;}).listIterator();
    equal(0,calls[0]); equal(false,cur.hasNext());equal(false,cur.hasPrevious());
    cur.add(2);equal(1,calls[0]);equal(List.of(2),span);equal(1,cur.nextIndex());equal(0,cur.previousIndex());
    cur.add(4);equal(2,calls[0]);equal(List.of(2,4),span);equal(2,cur.nextIndex());
    expect(IllegalStateException.class,cur::remove);expect(IllegalStateException.class,()->cur.set(8));
    equal(4,cur.previous());cur.remove();equal(List.of(2),span);equal(1,cur.nextIndex());
    equal(2,cur.previous());cur.remove();equal(List.of(),span);equal(0,cur.nextIndex());
    cur.add(6);equal(List.of(6),span);equal(6,cur.previous());equal(0,cur.nextIndex());
  }
  public static void main(String[] args) {
    List<Supplier<List<Integer>>> factories=List.of(ArrayList::new,LinkedList::new,CopyOnWriteArrayList::new,
      ()->Collections.synchronizedList(new LinkedList<>()),ReadonlyCursorList::new);
    for(Supplier<List<Integer>> f:factories) for(int k=0;k<4;k++) emptyWrites(f.get(),k);
    ReadonlyCursorList custom=new ReadonlyCursorList();emptyWrites(custom,0);equal(3,custom.adds);
    for(List<Integer> immutable:List.of(List.<Integer>of(),Arrays.<Integer>asList(),
        Collections.<Integer>emptyList(),Collections.unmodifiableList(new ArrayList<Integer>()))) {
      for(int k=0;k<4;k++) {
        List<Integer> span=form(immutable,k);ListIterator<Integer> cur=M3FilteredLists.filtered(span,v->v%2==0).listIterator();
        expect(IllegalArgumentException.class,()->cur.add(1));expect(UnsupportedOperationException.class,()->cur.add(2));
        equal(0,cur.nextIndex());equal(false,cur.hasNext());equal(false,cur.hasPrevious());equal(List.of(),span);
      }
    }
    // Nonempty physical backing, no visible elements: retain hidden occurrences and append position.
    for(Supplier<List<Integer>> f:factories) {
      List<Integer> owner=f.get();owner.addAll(List.of(1,3));
      ListIterator<Integer> cur=M3FilteredLists.filtered(owner,v->v%2==0).listIterator();
      cur.hasNext();cur.hasPrevious();cur.add(2);equal(List.of(1,3,2),owner);equal(2,cur.previous());
      cur.remove();equal(List.of(1,3),owner);equal(0,cur.nextIndex());
    }
    System.out.println("REPAIR_EDGE_PASS checks="+checks+" providers="+factories.size()+" forms=4");
  }
}
