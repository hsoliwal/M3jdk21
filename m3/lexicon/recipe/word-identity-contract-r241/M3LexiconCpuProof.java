// Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0
// CPU qualification adapter of the existing RAM compiler; no product runtime owner.
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import javax.tools.*;
class M3LexiconCpuProof {
 static String hash(byte[] b)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b));}
 public static void main(String[] a)throws Exception{
  byte[] p;try(var i=URI.create(a[0]).toURL().openStream()){p=i.readNBytes(2097153);}
  if(p.length>2097152||!hash(p).equals(a[1]))throw new AssertionError("packet seal");
  var r=new BufferedReader(new StringReader(new String(p,StandardCharsets.UTF_8)));var s=new LinkedHashMap<String,String>();var resources=new HashMap<String,byte[]>();
  int n=Integer.parseInt(r.readLine());if(n<1||n>128)throw new AssertionError("source count");
  for(int j=0;j<n;j++){var f=r.readLine().split("\t",-1);byte[] b=Base64.getDecoder().decode(f[2]);if(!hash(b).equals(f[1])||s.put(f[0],new String(b,StandardCharsets.UTF_8))!=null)throw new AssertionError("source seal");}
  String line;while((line=r.readLine())!=null){var f=line.split("\t",-1);byte[] b=Base64.getDecoder().decode(f[2]);if(!hash(b).equals(f[1])||resources.put(f[0],b)!=null)throw new AssertionError("resource seal");}
  var out=new HashMap<String,ByteArrayOutputStream>();var c=ToolProvider.getSystemJavaCompiler();
  try(var std=c.getStandardFileManager(null,null,StandardCharsets.UTF_8);var fm=new ForwardingJavaFileManager<StandardJavaFileManager>(std){
   public JavaFileObject getJavaFileForOutput(Location l,String name,JavaFileObject.Kind k,FileObject f){return new SimpleJavaFileObject(URI.create("mem:///"+name.replace('.','/')+k.extension),k){public OutputStream openOutputStream(){var b=new ByteArrayOutputStream();if(out.put(name,b)!=null)throw new AssertionError("duplicate output");return b;}};}
  }){
   var units=new ArrayList<JavaFileObject>();for(var e:s.entrySet())units.add(new SimpleJavaFileObject(URI.create("string:///"+e.getKey().replace('.','/')+".java"),JavaFileObject.Kind.SOURCE){public CharSequence getCharContent(boolean ignored){return e.getValue();}});
   if(!c.getTask(null,fm,null,List.of("--release","21","-proc:none","-g:none","-Xlint:all","-Werror","-classpath",System.getProperty("java.class.path")),null,units).call())throw new AssertionError("compile");
  }
  var loader=new ClassLoader(M3LexiconCpuProof.class.getClassLoader()){
   protected Class<?> findClass(String name)throws ClassNotFoundException{var b=out.get(name);if(b==null)throw new ClassNotFoundException(name);return defineClass(name,b.toByteArray(),0,b.size());}
   public InputStream getResourceAsStream(String name){var b=resources.get(name);return b==null?super.getResourceAsStream(name):new ByteArrayInputStream(b);}
  };
  for(String name:s.keySet())if(loader.loadClass(name).getClassLoader()!=loader)throw new AssertionError("class origin");
  var old=Thread.currentThread().getContextClassLoader();Thread.currentThread().setContextClassLoader(loader);
  try{loader.loadClass(a[2]).getMethod("main",String[].class).invoke(null,(Object)Arrays.copyOfRange(a,3,a.length));}finally{Thread.currentThread().setContextClassLoader(old);}
  System.out.println("M3_CPU_RAM_COMPILE_PASS sources="+s.size()+" classes="+out.size());
 }
}
