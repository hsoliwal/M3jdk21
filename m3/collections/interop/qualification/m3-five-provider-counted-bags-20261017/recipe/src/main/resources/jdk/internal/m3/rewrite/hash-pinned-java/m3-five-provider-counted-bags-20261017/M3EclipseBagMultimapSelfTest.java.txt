/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import java.util.*;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.multimap.bag.HashBagMultimap;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;

/** Differential oracle against authentic Eclipse Collections 11.1.0. */
public final class M3EclipseBagMultimapSelfTest {
  private static long checks;
  private M3EclipseBagMultimapSelfTest() { }
  private static void eq(Object expected,Object actual){checks++;if(!Objects.equals(expected,actual))throw new AssertionError("check="+checks+" expected="+expected+" got="+actual);}
  private static void testBag(){
    MutableBag<Integer> actual=new M3EclipseBag<>(), reference=new HashBag<>();
    Random rng=new Random(20261017);
    for(int i=0;i<4500;i++){
      Integer e=rng.nextInt(10)==0?null:rng.nextInt(40);int n=rng.nextInt(5);
      switch(rng.nextInt(7)){
        case 0 -> eq(reference.addOccurrences(e,n),actual.addOccurrences(e,n));
        case 1 -> eq(reference.removeOccurrences(e,n),actual.removeOccurrences(e,n));
        case 2 -> eq(reference.setOccurrences(e,n),actual.setOccurrences(e,n));
        case 3 -> eq(reference.add(e),actual.add(e));
        case 4 -> eq(reference.remove(e),actual.remove(e));
        case 5 -> eq(reference.occurrencesOf(e),actual.occurrencesOf(e));
        default -> eq(reference.contains(e),actual.contains(e));
      }
      eq(reference.size(),actual.size());eq(reference.sizeDistinct(),actual.sizeDistinct());
      eq(reference.toMapOfItemToCount(),actual.toMapOfItemToCount());
      eq(reference,actual);eq(actual,reference);
      if(i%137==0){
        var selected=actual.selectByOccurrences(c->c>1);
        for(var key:selected.toMapOfItemToCount().keySet())eq(true,actual.occurrencesOf(key)>1);
      }
    }
    actual.clear();reference.clear();eq(reference,actual);
    actual.addOccurrences(null,3); reference.addOccurrences(null,3);eq(reference,actual);
    var it=actual.iterator();eq(null,it.next());it.remove();eq(2,actual.occurrencesOf(null));
  }
  private static void compare(MutableBagMultimap<Integer,Integer> expected, MutableBagMultimap<Integer,Integer> actual){
    eq(expected.size(),actual.size());eq(expected.sizeDistinct(),actual.sizeDistinct());
    eq(expected.keySet(),actual.keySet());eq(expected.keysView().toBag(),actual.keysView().toBag());
    for(int j=-1;j<25;j++){Integer key=j<0?null:j;eq(expected.get(key),actual.get(key));eq(expected.get(key).toMapOfItemToCount(),actual.get(key).toMapOfItemToCount());}
    eq(expected,actual);eq(actual,expected);
  }
  private static void testMultimap(){
    MutableBagMultimap<Integer,Integer> actual=new M3EclipseBagMultimap<>(), expected=HashBagMultimap.newMultimap();
    Random rng=new Random(731077);
    for(int i=0;i<3800;i++){
      Integer key=rng.nextInt(11)==0?null:rng.nextInt(25);
      Integer val=rng.nextInt(13)==0?null:rng.nextInt(30);
      switch(rng.nextInt(7)){
        case 0,1 -> eq(expected.put(key,val),actual.put(key,val));
        case 2 -> eq(expected.remove(key,val),actual.remove(key,val));
        case 3 -> {expected.putOccurrences(key,val,2); actual.putOccurrences(key,val,2);}
        case 4 -> eq(expected.removeAll(key),actual.removeAll(key));
        case 5 -> eq(expected.replaceValues(key,Arrays.asList(val,val)), actual.replaceValues(key,Arrays.asList(val,val)));
        default -> {eq(expected.get(key),actual.get(key));}
      }
      compare(expected,actual);
    }
    var selected=actual.selectKeysValues((k,v)->v!=null && v%2==0);
    var selectedRef=expected.selectKeysValues((k,v)->v!=null && v%2==0);
    eq(selectedRef,selected);
    eq(expected.flip(),actual.flip());
  }
  public static void main(String[] args){testBag();testMultimap();System.out.println("PASS ECLIPSE_BAG_M3 checks="+checks+" operations=8300");}
}
