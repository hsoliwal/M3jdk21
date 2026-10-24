/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
import com.m3.collections.M3CollectionViews;
import java.util.*;
public final class ReviewProbe {
  static int checks;
  static void ok(boolean condition, String name) {
    checks++;
    if (!condition) throw new AssertionError(name);
  }
  static final class Key {
    final String text; final boolean visible;
    Key(String text, boolean visible) { this.text=text; this.visible=visible; }
    public boolean equals(Object other) { return other instanceof Key k && text.equals(k.text); }
    public int hashCode() { return text.hashCode(); }
  }
  static final class Poison {
    public boolean equals(Object other) { throw new AssertionError("identity path used equals"); }
    public int hashCode() { throw new AssertionError("identity path used hashCode"); }
  }
  public static void main(String[] args) {
    Set<Poison> identity = Collections.newSetFromMap(new IdentityHashMap<>());
    Poison stored=new Poison(), absent=new Poison(); identity.add(stored);
    List<String> effects=new ArrayList<>();
    Collection<Poison> erased=identity;
    Collection<Poison> nested=M3CollectionViews.filtered(M3CollectionViews.filtered(erased,
      x->{effects.add("first"); return true;}),x->{effects.add("second"); return true;});
    ok(nested instanceof Set<?>,"runtime Set dispatch");
    ok(!nested.contains(absent) && effects.isEmpty(),"absent identity no predicate/equals");
    ok(nested.contains(stored) && effects.equals(List.of("first","second")),"nested predicate order");
    effects.clear();
    ok(nested.remove(stored) && identity.isEmpty() && effects.equals(List.of("first","second")),"nested exact removal effects");
    for (boolean navigable : new boolean[]{false,true}) {
      TreeSet<Key> tree=new TreeSet<>(Comparator.comparing(x->x.text));
      Key hidden=new Key("hidden",false), visible=new Key("visible",true);
      tree.add(hidden); tree.add(visible);
      SortedSet<Key> owner = navigable ? tree : Collections.unmodifiableSortedSet(tree);
      Collection<Key> view=M3CollectionViews.filtered((Collection<Key>)owner,x->x.visible);
      ok(view instanceof SortedSet<?>,"runtime sorted dispatch");
      ok((view instanceof NavigableSet<?>)==navigable,"runtime navigable dispatch");
      ok(view.contains(new Key("visible",false)),"sorted comparator alias stored visibility");
      ok(!view.contains(new Key("hidden",true)),"sorted comparator hidden representative");
      SortedSet<Key> nestedSorted=M3CollectionViews.filtered((SortedSet<Key>)view,x->true);
      ok(nestedSorted.contains(new Key("visible",false)),"nested sorted flatten visible alias");
      ok(!nestedSorted.headSet(new Key("visible",true)).contains(new Key("hidden",true)),"sorted range hidden representative");
      if (navigable) {
        ok(nestedSorted.remove(new Key("visible",false)) && !tree.contains(visible),"sorted alias removal");
      }
    }
    Key selected=new Key("same",true); final RuntimeException failure=new IllegalStateException("cursor marker");
    Set<Key> exceptional=new AbstractSet<>() {
      public int size(){return 1;}
      public boolean contains(Object x){return x==selected;}
      public Iterator<Key> iterator(){return new Iterator<>(){
        boolean pending=true;
        public boolean hasNext(){return pending;}
        public Key next(){if(!pending)throw new NoSuchElementException();pending=false;return selected;}
        public void remove(){throw failure;}
      };}
    };
    try { M3CollectionViews.filtered(exceptional,x->true).remove(selected); throw new AssertionError("cursor failure absent"); }
    catch(RuntimeException actual){ok(actual==failure,"cursor exception identity");}
    System.out.println("Independent review: "+checks+" checks passed");
  }
}
