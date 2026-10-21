// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-18: map the pinned Synexia CPU tool into its existing M3JDK21 module name.
// SPDX-License-Identifier: Apache-2.0
package com.m3.tools.codec.varint;
import java.util.Arrays;
/** Canonical nonnegative signed-long LEB128 with strict overflow and trailing-data rejection. */
public final class VarInts {
    private VarInts() { }
    public static byte[] encode(long value) {
        if(value<0)throw new IllegalArgumentException("negative unsigned varint");
        byte[] data=new byte[9];int used=0;
        do{
            int part=(int)(value&127);
            value>>>=7;
            data[used++]=(byte)(part|(value!=0?128:0));
        }while(value!=0);
        return Arrays.copyOf(data,used);
    }
    public static long decode(byte[] data) {
        if(data==null||data.length==0||data.length>9)throw new IllegalArgumentException("encoded length");
        long value=0;
        for(int i=0;i<data.length;i++){
            int part=data[i]&255;
            value|=(long)(part&127)<<(i*7);
            if((part&128)==0){
                if(i!=data.length-1)throw new IllegalArgumentException("trailing data");
                if(data.length!=encode(value).length)throw new IllegalArgumentException("overlong encoding");
                return value;
            }
        }
        throw new IllegalArgumentException("unterminated/overflow");
    }
    public static void main(String[] args){
        if(args.length==1&&args[0].equals("--self-test")){
            for(long n:new long[]{0,1,127,128,16384,Long.MAX_VALUE})
                if(decode(encode(n))!=n)throw new AssertionError("roundtrip "+n);
            try{decode(new byte[]{(byte)0x80,0});throw new AssertionError("overlong");}
            catch(IllegalArgumentException expected){}
            System.out.println("M3_JMOD_TEST_PASS codec.varint");
        }else if(args.length==1)System.out.println(Arrays.toString(encode(Long.parseLong(args[0]))));
        else throw new IllegalArgumentException("value required");
    }
}
