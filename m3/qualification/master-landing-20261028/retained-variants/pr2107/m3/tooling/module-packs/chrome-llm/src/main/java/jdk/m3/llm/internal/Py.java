/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK 21 from cpullm-java/src/main/java/jdk/internal/m3/cpullm/Py.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
package jdk.m3.llm.internal;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Explicit, bounded Python value semantics used by generated Java methods. */
public final class Py {
    private Py() { }
    private static final ThreadLocal<Integer> STEPS = ThreadLocal.withInitial(() -> 0);
    public static final int LIMIT = 100_000;
    public static final class Error extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public final String type;
        public Error(String type, String message) { super(message); this.type = type; }
    }
    public record Range(BigInteger start, BigInteger stop, BigInteger step) {
        public BigInteger size() {
            BigInteger distance=step.signum()>0?stop.subtract(start):start.subtract(stop);
            return distance.signum()<=0?BigInteger.ZERO:distance.subtract(BigInteger.ONE).divide(step.abs()).add(BigInteger.ONE);
        }
    }
    public record Tuple(List<Object> values) {
        public Tuple { values = java.util.Collections.unmodifiableList(new ArrayList<>(values)); }
    }
    public static final class Return extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public final transient Object value;
        public Return(Object value) { super(null, null, false, false); this.value=value; }
    }
    public static final class Break extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public Break() { super(null, null, false, false); }
    }
    public static final class Continue extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public Continue() { super(null, null, false, false); }
    }
    public static void ret(Object value) { throw new Return(value); }
    public static void brk() { throw new Break(); }
    public static void cont() { throw new Continue(); }
    public static void raise(String type, Object message) { throw error(type,str(message)); }
    public static Error error(String type, String message) { return new Error(type, message); }
    public static void reset() { STEPS.set(0); }
    public static void tick() {
        int next = STEPS.get() + 1;
        if (next > LIMIT) throw error("ResourceLimit", "execution step budget exceeded");
        STEPS.set(next);
    }
    public static Object get(Map<String,Object> scope, String key) {
        if (!scope.containsKey(key)) throw error("UnboundLocalError", key);
        return scope.get(key);
    }
    public static BigInteger integer(String value) { return new BigInteger(value); }
    public static BigInteger integer(Object value) {
        if (value instanceof BigInteger n) return n;
        if (value instanceof Boolean b) return b ? BigInteger.ONE : BigInteger.ZERO;
        throw error("TypeError", "integer required");
    }
    public static boolean truth(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof BigInteger n) return n.signum() != 0;
        if (value instanceof String s) return !s.isEmpty();
        if (value instanceof Collection<?> c) return !c.isEmpty();
        if (value instanceof Map<?,?> m) return !m.isEmpty();
        if (value instanceof Tuple t) return !t.values().isEmpty();
        if (value instanceof Range r) return r.size().signum()!=0;
        throw error("TypeError", "unsupported truth operand");
    }
    public static Object and(Object left, Supplier<Object> right) { return truth(left) ? right.get() : left; }
    public static Object or(Object left, Supplier<Object> right) { return truth(left) ? left : right.get(); }
    public static Object unary(String op, Object value) {
        return switch (op) {
            case "Not" -> !truth(value);
            case "USub" -> integer(value).negate();
            case "UAdd" -> integer(value);
            case "Invert" -> integer(value).not();
            default -> throw error("TypeError", "unsupported unary operation: " + op);
        };
    }
    public static Object binary(String op, Object left, Object right) {
        tick();
        if (op.equals("Add")) {
            if (left instanceof String a && right instanceof String b) return bounded(a + b);
            if (left instanceof List<?> a && right instanceof List<?> b) {
                var result = new ArrayList<Object>(a); result.addAll(b); checkSize(result.size()); return result;
            }
            if (left instanceof Tuple a && right instanceof Tuple b) {
                var result = new ArrayList<>(a.values()); result.addAll(b.values()); checkSize(result.size()); return new Tuple(result);
            }
        }
        if (op.equals("Mod") && left instanceof String text) return percent(text,right);
        if (op.equals("Mult")) {
            if (left instanceof String || left instanceof List<?> || left instanceof Tuple) return repeat(left, right);
            if (right instanceof String || right instanceof List<?> || right instanceof Tuple) return repeat(right, left);
        }
        if (left instanceof Boolean a && right instanceof Boolean b) {
            if (op.equals("BitAnd")) return a & b;
            if (op.equals("BitOr")) return a | b;
            if (op.equals("BitXor")) return a ^ b;
        }
        BigInteger a = integer(left), b = integer(right);
        BigInteger result = switch (op) {
            case "Add" -> a.add(b); case "Sub" -> a.subtract(b); case "Mult" -> a.multiply(b);
            case "FloorDiv" -> floor(a,b); case "Mod" -> a.subtract(floor(a,b).multiply(b));
            case "BitAnd" -> a.and(b); case "BitOr" -> a.or(b); case "BitXor" -> a.xor(b);
            case "LShift" -> a.shiftLeft(shift(b)); case "RShift" -> a.shiftRight(shift(b));
            default -> throw error("TypeError", "unsupported binary operation: " + op);
        };
        if (result.bitLength() > LIMIT) throw error("ResourceLimit", "integer budget exceeded");
        return result;
    }
    private static BigInteger floor(BigInteger a, BigInteger b) {
        if (b.signum() == 0) throw error("ZeroDivisionError", "integer division or modulo by zero");
        BigInteger[] qr = a.divideAndRemainder(b);
        return qr[1].signum() != 0 && a.signum() != b.signum() ? qr[0].subtract(BigInteger.ONE) : qr[0];
    }
    private static int shift(BigInteger count) {
        if (count.signum() < 0) throw error("ValueError", "negative shift count");
        if (count.compareTo(BigInteger.valueOf(LIMIT)) > 0) throw error("ResourceLimit", "shift budget exceeded");
        return count.intValueExact();
    }
    private static Object repeat(Object sequence, Object amount) {
        BigInteger n = integer(amount);
        if (n.signum() <= 0) return sequence instanceof String ? "" : sequence instanceof Tuple ? tuple() : list();
        int size = iter(sequence).size();
        if (size == 0) return sequence;
        if (n.multiply(BigInteger.valueOf(size)).compareTo(BigInteger.valueOf(LIMIT)) > 0)
            throw error("ResourceLimit", "sequence budget exceeded");
        if (sequence instanceof String s) return s.repeat(n.intValueExact());
        var out = new ArrayList<Object>();
        for (int i=0; i<n.intValueExact(); i++) out.addAll(iter(sequence));
        return sequence instanceof Tuple ? new Tuple(out) : out;
    }
    public static Object augment(String op,Object left,Object right) {
        if(left instanceof List<?> raw && op.equals("Add")) {
            var values=mutableList(raw);var additions=iter(right);checkSize(values.size()+additions.size());values.addAll(additions);return values;
        }
        if(left instanceof List<?> raw && op.equals("Mult")) {
            var values=mutableList(raw);var result=iter(repeat(left,right));values.clear();values.addAll(result);return values;
        }
        return binary(op,left,right);
    }
    public static boolean equal(Object a, Object b) {
        if ((a instanceof BigInteger || a instanceof Boolean) && (b instanceof BigInteger || b instanceof Boolean))
            return integer(a).equals(integer(b));
        if (a instanceof List<?> x && b instanceof List<?> y) return sequenceEqual(x,y);
        if (a instanceof Tuple x && b instanceof Tuple y) return sequenceEqual(x.values(), y.values());
        if(a instanceof Range x && b instanceof Range y){BigInteger size=x.size();return size.equals(y.size())&&(size.signum()==0||(x.start().equals(y.start())&&(size.equals(BigInteger.ONE)||x.step().equals(y.step()))));}
        if(a instanceof Map<?,?> x && b instanceof Map<?,?> y){if(x.size()!=y.size())return false;for(var e:x.entrySet()){Object k=findKey(y,e.getKey());if(k==ABSENT||!equal(e.getValue(),y.get(k)))return false;}return true;}
        if(a instanceof Set<?> x && b instanceof Set<?> y)return x.size()==y.size()&&x.stream().allMatch(v->y.stream().anyMatch(w->equal(v,w)));
        return Objects.equals(a,b);
    }
    private static boolean sequenceEqual(List<?> a,List<?> b) {
        if(a.size()!=b.size()) return false;
        for(int i=0;i<a.size();i++) if(!equal(a.get(i),b.get(i))) return false;
        return true;
    }
    public static int compare(Object a,Object b) {
        if ((a instanceof BigInteger || a instanceof Boolean) && (b instanceof BigInteger || b instanceof Boolean))
            return integer(a).compareTo(integer(b));
        if (a instanceof String x && b instanceof String y) {
            int[] ac=x.codePoints().toArray(),bc=y.codePoints().toArray();
            for(int i=0;i<Math.min(ac.length,bc.length);i++) if(ac[i]!=bc[i]) return Integer.compare(ac[i],bc[i]);
            return Integer.compare(ac.length,bc.length);
        }
        if((a instanceof List<?> && b instanceof List<?>) || (a instanceof Tuple && b instanceof Tuple)) {
            List<Object> x=iter(a),y=iter(b);
            for(int i=0;i<Math.min(x.size(),y.size());i++) if(!equal(x.get(i),y.get(i))) return compare(x.get(i),y.get(i));
            return Integer.compare(x.size(),y.size());
        }
        throw error("TypeError", "incomparable values");
    }
    public static boolean test(String op,Object a,Object b) {
        return switch(op) {
            case "Eq" -> equal(a,b); case "NotEq" -> !equal(a,b);
            case "Lt" -> compare(a,b)<0; case "LtE" -> compare(a,b)<=0;
            case "Gt" -> compare(a,b)>0; case "GtE" -> compare(a,b)>=0;
            case "In" -> contains(b,a); case "NotIn" -> !contains(b,a);
            case "Is" -> a==b; case "IsNot" -> a!=b;
            default -> throw error("TypeError", "unsupported comparison");
        };
    }
    public static boolean chain(String[] operations,Supplier<?>[] operands) {
        Object previous=operands[0].get();
        for(int i=0;i<operations.length;i++) {
            Object current=operands[i+1].get();
            if(!test(operations[i],previous,current)) return false;
            previous=current;
        }
        return true;
    }
    public static boolean contains(Object container,Object value) {
        if(container instanceof String s) {
            if(!(value instanceof String)) throw error("TypeError","string membership operand");
            return s.contains((String)value);
        }
        if(container instanceof Map<?,?> m) return findKey(m,key(value))!=ABSENT;
        return iter(container).stream().anyMatch(x->equal(x,value));
    }
    public static List<Object> list(Object... values) { return new ArrayList<>(Arrays.asList(values)); }
    public static Tuple tuple(Object... values) { return new Tuple(list(values)); }
    public static Set<Object> set(Object... values) {
        var result=new LinkedHashSet<Object>(); for(Object v:values) result.add(key(v)); return result;
    }
    public static Map<Object,Object> dict(Object... pairs) {
        var result=new LinkedHashMap<Object,Object>();
        for(int i=0;i<pairs.length;i+=2) dictPut(result,pairs[i],pairs[i+1]);
        return result;
    }
    private static Object key(Object value) {
        if(value instanceof List<?> || value instanceof Map<?,?> || value instanceof Set<?>) throw error("TypeError","unhashable key");
        if(value instanceof Tuple tuple)for(Object item:tuple.values())key(item);
        return value;
    }
    private static final Object ABSENT=new Object();
    private static Object findKey(Map<?,?> map,Object requested){key(requested);for(Object stored:map.keySet())if(equal(stored,requested))return stored;return ABSENT;}
    private static void dictPut(Map<Object,Object> map,Object requested,Object value){Object k=findKey(map,requested);map.put(k==ABSENT?key(requested):k,value);}
    public static List<Object> iter(Object value) {
        if(value instanceof Range r){if(r.size().compareTo(BigInteger.valueOf(LIMIT))>0)throw error("ResourceLimit","range budget");var out=new ArrayList<Object>();
            for(BigInteger n=r.start();r.step().signum()>0?n.compareTo(r.stop())<0:n.compareTo(r.stop())>0;n=n.add(r.step()))out.add(n);return out;}
        if(value instanceof String s) return s.codePoints().mapToObj(c->(Object)new String(Character.toChars(c))).toList();
        if(value instanceof Tuple t) return t.values();
        if(value instanceof Collection<?> c) return new ArrayList<>(c);
        if(value instanceof Map<?,?> m) return new ArrayList<>(m.keySet());
        throw error("TypeError","not iterable");
    }
    public static Object item(Object container,Object index) {
        if(container instanceof Map<?,?> m) {
            Object k=findKey(m,index); if(k==ABSENT) throw error("KeyError",repr(index)); return m.get(k);
        }
        if(!(container instanceof String || container instanceof List<?> || container instanceof Tuple || container instanceof Range))throw error("TypeError","not subscriptable");
        List<Object> values=iter(container); BigInteger n=integer(index);
        if(n.signum()<0) n=n.add(BigInteger.valueOf(values.size()));
        if(n.signum()<0 || n.compareTo(BigInteger.valueOf(values.size()))>=0) throw error("IndexError","index out of range");
        return values.get(n.intValueExact());
    }
    public static Object slice(Object container,Object start,Object stop,Object stride) {
        if(container instanceof Map<?,?>){key(start);key(stop);key(stride);throw error("KeyError","slice");}
        if(!(container instanceof String || container instanceof List<?> || container instanceof Tuple))throw error("TypeError","not sliceable");
        List<Object> values=iter(container); int size=values.size();
        BigInteger step=stride==null?BigInteger.ONE:integer(stride);
        if(step.signum()==0) throw error("ValueError","slice step cannot be zero");
        boolean forward=step.signum()>0;
        BigInteger lower=BigInteger.valueOf(forward?0:-1),upper=BigInteger.valueOf(forward?size:size-1);
        BigInteger from=start==null?BigInteger.valueOf(forward?0:size-1):clipIndex(integer(start),size,lower,upper);
        BigInteger to=stop==null?BigInteger.valueOf(forward?size:-1):clipIndex(integer(stop),size,lower,upper);
        var result=new ArrayList<Object>();
        for(BigInteger i=from;forward?i.compareTo(to)<0:i.compareTo(to)>0;i=i.add(step)) {tick();result.add(values.get(i.intValueExact()));}
        if(container instanceof String) {var text=new StringBuilder();for(Object v:result) text.append(v);return text.toString();}
        return container instanceof Tuple?new Tuple(result):result;
    }
    private static BigInteger clipIndex(BigInteger n,int size,BigInteger low,BigInteger high) {
        if(n.signum()<0)n=n.add(BigInteger.valueOf(size));return n.max(low).min(high);
    }
    public static void put(Object container,Object index,Object value) {
        if(container instanceof Map<?,?> raw) {dictPut(map(raw),index,value);return;}
        if(container instanceof List<?> raw) {
            var values=mutableList(raw); BigInteger n=integer(index);
            if(n.signum()<0)n=n.add(BigInteger.valueOf(values.size()));
            if(n.signum()<0 || n.compareTo(BigInteger.valueOf(values.size()))>=0)throw error("IndexError","index out of range");
            values.set(n.intValueExact(),value);return;
        }
        throw error("TypeError","item assignment unsupported");
    }
    @SuppressWarnings("unchecked") private static List<Object> mutableList(List<?> value){return (List<Object>)value;}
    @SuppressWarnings("unchecked") private static Map<Object,Object> map(Map<?,?> value){return (Map<Object,Object>)value;}
    public static Object builtin(String name,Object... args) {
        tick();
        return switch(name) {
            case "len" -> {arity(args,1,1);if(args[0] instanceof Range r){BigInteger size=r.size();if(size.compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0)throw error("OverflowError","range size");yield size;}yield BigInteger.valueOf(args[0] instanceof Map<?,?> m?m.size():iter(args[0]).size());}
            case "bool" -> {arity(args,0,1);yield args.length!=0&&truth(args[0]);}
            case "str" -> {arity(args,0,1);yield args.length==0?"":str(args[0]);}
            case "repr" -> {arity(args,1,1);yield repr(args[0]);}
            case "int" -> {arity(args,0,2);if(args.length==0)yield BigInteger.ZERO;
                if(!(args[0] instanceof String s)){arity(args,1,1);yield integer(args[0]);}
                BigInteger radix=args.length==1?BigInteger.TEN:integer(args[1]);
                if(radix.signum()!=0 && (radix.compareTo(BigInteger.TWO)<0||radix.compareTo(BigInteger.valueOf(36))>0))throw error("ValueError","invalid integer base");
                yield parseInteger(s,radix.intValue());}
            case "abs" -> {arity(args,1,1);yield integer(args[0]).abs();}
            case "min", "max" -> {if(args.length==0)throw error("TypeError","missing arguments");
                List<Object> values=args.length==1?iter(args[0]):list(args);
                if(values.isEmpty())throw error("ValueError","empty sequence");Object best=values.get(0);
                for(Object value:values)if(name.equals("min")?compare(value,best)<0:compare(value,best)>0)best=value;yield best;}
            case "sum" -> {arity(args,1,2);Object total=args.length==2?args[1]:BigInteger.ZERO;for(Object v:iter(args[0]))total=binary("Add",total,v);yield total;}
            case "all", "any" -> {arity(args,1,1);boolean all=name.equals("all"), result=all;
                for(Object v:iter(args[0])){if(truth(v)!=all){result=!all;break;}}yield result;}
            case "list" -> {arity(args,0,1);yield args.length==0?list():new ArrayList<>(iter(args[0]));}
            case "tuple" -> {arity(args,0,1);yield new Tuple(args.length==0?list():iter(args[0]));}
            case "set", "frozenset" -> {arity(args,0,1);var out=new LinkedHashSet<Object>();if(args.length==1)for(Object v:iter(args[0]))out.add(key(v));yield out;}
            case "dict" -> {arity(args,0,1);if(args.length==0)yield dict();if(args[0] instanceof Map<?,?> m)yield new LinkedHashMap<>(m);
                var out=new LinkedHashMap<Object,Object>();for(Object v:iter(args[0])){var pair=iter(v);if(pair.size()!=2)throw error("ValueError","dictionary pair size");dictPut(out,pair.get(0),pair.get(1));}yield out;}
            case "sorted" -> {arity(args,1,1);var out=new ArrayList<>(iter(args[0]));out.sort(Py::compare);yield out;}
            case "reversed" -> {arity(args,1,1);var out=new ArrayList<>(iter(args[0]));java.util.Collections.reverse(out);yield out;}
            case "range" -> range(args);
            case "enumerate" -> {arity(args,1,2);BigInteger index=args.length==1?BigInteger.ZERO:integer(args[1]);var out=new ArrayList<Object>();
                for(Object v:iter(args[0])){out.add(tuple(index,v));index=index.add(BigInteger.ONE);}yield out;}
            case "zip" -> {var sequences=Arrays.stream(args).map(Py::iter).toList();int size=sequences.stream().mapToInt(List::size).min().orElse(0);var out=new ArrayList<Object>();
                for(int i=0;i<size;i++){var row=new ArrayList<Object>();for(var values:sequences)row.add(values.get(i));out.add(new Tuple(row));}yield out;}
            case "ord" -> {arity(args,1,1);String s=string(args[0]);if(s.codePointCount(0,s.length())!=1)throw error("TypeError","ord expects one character");yield BigInteger.valueOf(s.codePointAt(0));}
            case "chr" -> {arity(args,1,1);BigInteger n=integer(args[0]);if(n.signum()<0||n.compareTo(BigInteger.valueOf(0x10ffff))>0)throw error("ValueError","chr out of range");yield new String(Character.toChars(n.intValueExact()));}
            case "hex", "bin", "oct" -> {arity(args,1,1);BigInteger n=integer(args[0]);int radix=name.equals("hex")?16:name.equals("bin")?2:8;
                yield (n.signum()<0?"-":"")+(radix==16?"0x":radix==2?"0b":"0o")+n.abs().toString(radix);}
            default -> throw error("NameError","unsupported builtin: "+name);
        };
    }
    private static Range range(Object[] args) {
        arity(args,1,3);BigInteger start=args.length==1?BigInteger.ZERO:integer(args[0]);
        BigInteger end=integer(args.length==1?args[0]:args[1]),step=args.length==3?integer(args[2]):BigInteger.ONE;
        if(step.signum()==0)throw error("ValueError","range step cannot be zero");return new Range(start,end,step);
    }
    private static BigInteger parseInteger(String input,int base){
        String value=input.strip();boolean negative=value.startsWith("-");if(value.startsWith("-")||value.startsWith("+"))value=value.substring(1);
        int prefix=value.startsWith("0x")||value.startsWith("0X")?16:value.startsWith("0o")||value.startsWith("0O")?8:value.startsWith("0b")||value.startsWith("0B")?2:0;
        if(base==0){base=prefix==0?10:prefix;if(prefix==0&&value.startsWith("0")&&value.codePoints().anyMatch(c->c!='0'&&c!='_'))throw error("ValueError","leading zero");}
        if(prefix==base){value=value.substring(2);if(value.startsWith("_"))value=value.substring(1);}
        if(value.isEmpty()||value.startsWith("_")||value.endsWith("_")||value.contains("__"))throw error("ValueError","invalid integer literal");
        value=value.replace("_","");
        for(int i=0;i<value.length();i++)if(Character.digit(value.charAt(i),base)<0)throw error("ValueError","invalid integer digit");
        try{BigInteger result=new BigInteger(value,base);return negative?result.negate():result;}catch(NumberFormatException e){throw error("ValueError","invalid integer literal");}
    }
    private static Object percent(String format,Object input){
        List<Object> args=input instanceof Tuple t?t.values():list(input);int index=0;var out=new StringBuilder();
        for(int i=0;i<format.length();i++){
            char c=format.charAt(i);if(c!='%'){out.append(c);continue;}
            if(++i>=format.length())throw error("ValueError","incomplete format");char spec=format.charAt(i);
            if(spec=='%'){out.append('%');continue;}
            if(index>=args.size())throw error("TypeError","not enough format arguments");Object v=args.get(index++);
            switch(spec){case 's' -> out.append(str(v));case 'r' -> out.append(repr(v));case 'd','i','u' -> out.append(integer(v));
                case 'x' -> out.append(integer(v).toString(16));case 'X' -> out.append(integer(v).toString(16).toUpperCase(java.util.Locale.ROOT));case 'o' -> out.append(integer(v).toString(8));
                default -> throw error("ValueError","unsupported format specifier");}
        }
        if(index!=args.size())throw error("TypeError","not all arguments converted");return bounded(out.toString());
    }
    public static Object callMethod(Object receiver,String name,Supplier<Object[]> arguments){
        Set<String> allowed=receiver instanceof List<?>?Set.of("append","extend","reverse","sort","copy","pop","count"):
            receiver instanceof Map<?,?>?Set.of("get","setdefault","keys","values","items","copy"):
            receiver instanceof String?Set.of("join","startswith","endswith","replace","removeprefix","removesuffix","count"):
            receiver instanceof Tuple?Set.of("count"):Set.of();
        if(!allowed.contains(name)&&!(receiver instanceof String&&StringOps.METHODS.contains(name)))throw error("AttributeError","no method: "+name);
        return method(receiver,name,arguments.get());
    }
    public static Object method(Object receiver,String name,Object... args) {
        tick();
        if(receiver instanceof List<?> raw){var values=mutableList(raw);
            switch(name){
                case "append":arity(args,1,1);checkSize(values.size()+1);values.add(args[0]);return null;
                case "extend":arity(args,1,1);var additions=iter(args[0]);checkSize(values.size()+additions.size());values.addAll(additions);return null;
                case "reverse":arity(args,0,0);java.util.Collections.reverse(values);return null;
                case "sort":arity(args,0,0);values.sort(Py::compare);return null;
                case "copy":arity(args,0,0);return new ArrayList<>(values);
                case "pop":arity(args,0,1);Object index=args.length==0?BigInteger.valueOf(-1):args[0];Object result=item(values,index);
                    int i=integer(index).intValueExact();values.remove(i<0?i+values.size():i);return result;
                case "count":arity(args,1,1);return BigInteger.valueOf(values.stream().filter(v->equal(v,args[0])).count());
                default:break;
            }
        }
        if(receiver instanceof Map<?,?> raw){var values=map(raw);
            switch(name){
                case "get":arity(args,1,2);Object found=findKey(values,args[0]);return found==ABSENT?(args.length==2?args[1]:null):values.get(found);
                case "setdefault":arity(args,1,2);Object k=findKey(values,args[0]);if(k==ABSENT){k=key(args[0]);values.put(k,args.length==2?args[1]:null);}return values.get(k);
                case "keys":arity(args,0,0);return new ArrayList<>(values.keySet());
                case "values":arity(args,0,0);return new ArrayList<>(values.values());
                case "items":arity(args,0,0);var out=new ArrayList<Object>();values.forEach((key,value)->out.add(tuple(key,value)));return out;
                case "copy":arity(args,0,0);return new LinkedHashMap<>(values);
                default:break;
            }
        }
        if(receiver instanceof Tuple tuple && name.equals("count")){arity(args,1,1);return BigInteger.valueOf(tuple.values().stream().filter(v->equal(v,args[0])).count());}
        if(receiver instanceof String text){
            if(StringOps.METHODS.contains(name))return StringOps.call(text,name,args);
            switch(name){
                case "count":arity(args,1,1);String needle=string(args[0]);if(needle.isEmpty())return BigInteger.valueOf(text.codePointCount(0,text.length())+1);
                    int found=0,position=0;while((position=text.indexOf(needle,position))>=0){found++;position+=needle.length();}return BigInteger.valueOf(found);
                case "join":arity(args,1,1);var joined=new StringBuilder();boolean first=true;
                    for(Object v:iter(args[0])){if(!first)joined.append(text);joined.append(string(v));first=false;}return bounded(joined.toString());
                case "startswith", "endswith":arity(args,1,1);if(args[0] instanceof Tuple t){for(Object v:t.values())if(name.equals("startswith")?text.startsWith(string(v)):text.endsWith(string(v)))return true;return false;}
                    return name.equals("startswith")?text.startsWith(string(args[0])):text.endsWith(string(args[0]));
                case "replace":arity(args,2,2);return bounded(text.replace(string(args[0]),string(args[1])));
                case "removeprefix":arity(args,1,1);String prefix=string(args[0]);return text.startsWith(prefix)?text.substring(prefix.length()):text;
                case "removesuffix":arity(args,1,1);String suffix=string(args[0]);return !suffix.isEmpty()&&text.endsWith(suffix)?text.substring(0,text.length()-suffix.length()):text;
                default:break;
            }
        }
        throw error("AttributeError","unsupported method: "+name);
    }
    public static String string(Object value){if(value instanceof String s)return s;throw error("TypeError","string required");}
    public static String str(Object value){return value instanceof String s?s:repr(value);}
    public static String repr(Object value){
        if(value==null)return "None";if(value instanceof Boolean b)return b?"True":"False";if(value instanceof BigInteger)return value.toString();
        if(value instanceof String s){String quote=s.contains("'")&&!s.contains("\"")?"\"":"'";
            return quote+s.replace("\\","\\\\").replace(quote,"\\"+quote).replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+quote;}
        if(value instanceof Range r)return "range("+r.start()+", "+r.stop()+(r.step().equals(BigInteger.ONE)?"":", "+r.step())+")";
        if(value instanceof Tuple t){String joined=String.join(", ",t.values().stream().map(Py::repr).toList());return "("+joined+(t.values().size()==1?",":"")+")";}
        if(value instanceof List<?> l)return "["+String.join(", ",l.stream().map(Py::repr).toList())+"]";
        if(value instanceof Map<?,?> m)return "{"+String.join(", ",m.entrySet().stream().map(e->repr(e.getKey())+": "+repr(e.getValue())).toList())+"}";
        if(value instanceof Set<?> s)return s.isEmpty()?"set()":"{"+String.join(", ",s.stream().map(Py::repr).toList())+"}";
        throw error("TypeError","unsupported representation");
    }
    public static String sha256(byte[] bytes){
        try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static String sha256(String text){return sha256(text.getBytes(StandardCharsets.UTF_8));}
    private static String bounded(String s){checkSize(s.length());return s;}
    private static void checkSize(int size){if(size>LIMIT)throw error("ResourceLimit","collection budget exceeded");}
    private static void arity(Object[] args,int min,int max){if(args.length<min||args.length>max)throw error("TypeError","wrong argument count");}
}
