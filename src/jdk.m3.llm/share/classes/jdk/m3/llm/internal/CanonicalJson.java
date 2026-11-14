// Modified for M3JDK: attributed package relocation and product module integration.
// Derivation: EXECUTED_CANONICAL_JSON_CORRECTION_WITH_COPYRIGHT_METADATA; see SOURCE_PROVENANCE.tsv.
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.internal;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Deterministic JSON codec for M3 receipts; maps are emitted in UTF-8 lexical key order. */
public final class CanonicalJson {
    private CanonicalJson() {}
    public static byte[] bytes(Object value) { return (string(value) + "\n").getBytes(StandardCharsets.UTF_8); }
    public static String string(Object value) { StringBuilder out=new StringBuilder(); write(value,out); return out.toString(); }
    public static Object parse(String json) { Parser p=new Parser(json); Object v=p.value(); p.ws(); if(!p.end()) throw new M3ContractException("trailing JSON data"); return v; }
    @SuppressWarnings("unchecked")
    private static void write(Object v, StringBuilder out) {
        if (v==null) { out.append("null"); return; }
        if (v instanceof String s) { quote(s,out); return; }
        if (v instanceof Boolean || v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) { out.append(v); return; }
        if (v instanceof Number n) { double d=n.doubleValue(); if (!Double.isFinite(d)) throw new M3ContractException("non-finite JSON number"); out.append(n); return; }
        if (v instanceof Map<?,?> m) {
            out.append('{'); boolean first=true;
            List<String> keys=m.keySet().stream().map(Object::toString).sorted(Comparator.naturalOrder()).toList();
            for (String k:keys) { if(!first) out.append(','); first=false; quote(k,out); out.append(':'); write(((Map<String,Object>)m).get(k),out); }
            out.append('}'); return;
        }
        if (v instanceof Iterable<?> it) { out.append('['); boolean first=true; for(Object x:it){ if(!first)out.append(','); first=false; write(x,out);} out.append(']'); return; }
        throw new M3ContractException("unsupported canonical JSON type: "+v.getClass());
    }
    private static void quote(String s,StringBuilder out){ out.append('"'); for(char c:s.toCharArray()){ switch(c){case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '\b'->out.append("\\b");case '\f'->out.append("\\f");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");default->{if(c<0x20)out.append(String.format("\\u%04x",(int)c));else out.append(c);}}} out.append('"'); }
    private static final class Parser {
        private final String s; private int i;
        Parser(String s){this.s=Objects.requireNonNull(s);}
        boolean end(){return i>=s.length();} void ws(){while(!end()&&Character.isWhitespace(s.charAt(i)))i++;}
        Object value(){ws();if(end())throw new M3ContractException("unexpected JSON EOF");char c=s.charAt(i);if(c=='{')return object();if(c=='[')return array();if(c=='"')return str();if(c=='t'){literal("true");return true;}if(c=='f'){literal("false");return false;}if(c=='n'){literal("null");return null;}return number();}
        Map<String,Object> object(){i++;TreeMap<String,Object> m=new TreeMap<>();ws();if(!end()&&s.charAt(i)=='}'){i++;return m;}while(true){ws();String k=str();ws();expect(':');Object v=value();if(m.containsKey(k))throw new M3ContractException("duplicate JSON key");m.put(k,v);ws();if(!end()&&s.charAt(i)=='}'){i++;return m;}expect(',');}}
        List<Object> array(){i++;List<Object>a=new ArrayList<>();ws();if(!end()&&s.charAt(i)==']'){i++;return a;}while(true){a.add(value());ws();if(!end()&&s.charAt(i)==']'){i++;return a;}expect(',');}}
        String str(){expect('"');StringBuilder o=new StringBuilder();while(!end()){char c=s.charAt(i++);if(c=='"')return o.toString();if(c<0x20)throw new M3ContractException("unescaped JSON control character");if(c!='\\'){o.append(c);continue;}if(end())throw new M3ContractException("bad JSON escape");char e=s.charAt(i++);switch(e){case '"'->o.append('"');case '\\'->o.append('\\');case '/'->o.append('/');case 'b'->o.append('\b');case 'f'->o.append('\f');case 'n'->o.append('\n');case 'r'->o.append('\r');case 't'->o.append('\t');case 'u'->{if(i+4>s.length())throw new M3ContractException("bad unicode escape");o.append((char)Integer.parseInt(s.substring(i,i+4),16));i+=4;}default->throw new M3ContractException("bad JSON escape");}}throw new M3ContractException("unterminated JSON string");}
        Number number(){int start=i;if(s.charAt(i)=='-')i++;while(!end()&&Character.isDigit(s.charAt(i)))i++;boolean decimal=false;if(!end()&&s.charAt(i)=='.'){decimal=true;i++;while(!end()&&Character.isDigit(s.charAt(i)))i++;}if(!end()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){decimal=true;i++;if(!end()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;while(!end()&&Character.isDigit(s.charAt(i)))i++;}String n=s.substring(start,i);try{if(decimal)return Double.valueOf(n);return Long.valueOf(n);}catch(NumberFormatException e){throw new M3ContractException("bad JSON number",e);}}
        void literal(String x){if(!s.startsWith(x,i))throw new M3ContractException("bad JSON literal");i+=x.length();}void expect(char c){ws();if(end()||s.charAt(i++)!=c)throw new M3ContractException("expected JSON token "+c);}
    }
}
