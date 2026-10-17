/* SPDX-License-Identifier: Apache-2.0 */
import org.apache.commons.collections4.bidimap.DualLinkedHashBidiMap;

/** Executed contract inventory against the actual 4.5.0 provider before porting. */
public final class BidiProviderDiscriminator {
    private BidiProviderDiscriminator() { }
    public static void main(String[] args) {
        if(args.length!=0) throw new IllegalArgumentException("no args");
        var entries=new DualLinkedHashBidiMap<String,Integer>();
        entries.put("k",1);
        var entry=entries.entrySet().iterator().next();
        Integer entryReturn=entry.setValue(2);
        System.out.println("ENTRY_SET_VALUE return="+entryReturn+" mapping="+entries.get("k"));
        var iterated=new DualLinkedHashBidiMap<String,Integer>();
        iterated.put("k",1);
        var iterator=iterated.mapIterator();
        iterator.next();
        Integer iteratorReturn=iterator.setValue(2);
        System.out.println("MAP_ITERATOR_SET_VALUE return="+iteratorReturn+" mapping="+iterated.get("k"));
        var identity=new DualLinkedHashBidiMap<String,Integer>();
        String first=new String("k"),equal=new String("k");
        identity.put(first,1);identity.put(equal,1);
        var cursor=identity.mapIterator();
        cursor.next();
        String failure="NONE";
        try {cursor.setValue(1);} catch(RuntimeException ex) {failure=ex.getClass().getSimpleName();}
        System.out.println("EQUAL_KEY_SET_VALUE exception="+failure+" current_is_first="+(cursor.getKey()==first)+" inverse_is_equal="+(identity.getKey(1)==equal));
        if(!Integer.valueOf(2).equals(entryReturn)||!Integer.valueOf(1).equals(iteratorReturn)
                ||!failure.equals("IllegalArgumentException")) throw new AssertionError("provider contract inventory changed");
        System.out.println("PASS APACHE_BIDI_4_5_PROVIDER_DISCRIMINATORS cases=3");
    }
}
