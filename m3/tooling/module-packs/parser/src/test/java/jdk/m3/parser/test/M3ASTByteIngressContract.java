// SPDX-License-Identifier: Apache-2.0
package jdk.m3.parser.test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import jdk.m3.parser.M3ASTByteIngress;
import jdk.m3.parser.M3ASTExternalIngress;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import jdk.m3.parser.M3Utf8SourceMap;

/** Native field roles and byte-to-UTF16 scalar safety without a second AST. */
public final class M3ASTByteIngressContract {
    private static int checks;
    private record Node(String kind,int start,int end,int tag,Node[] children) { }
    private static final class Adapter implements M3ASTByteIngress.ByteNodes<Node> {
        public String parserId(){return "tree-sitter:fixture";}
        public String kind(Node n){return n.kind();}
        public int childCount(Node n){return n.children().length;}
        public Node child(Node n,int i){return n.children()[i];}
        public String childRole(Node n,int i){return i==0?"left":i==2?"right":null;}
        public int startByte(Node n){return n.start();}
        public int endByte(Node n){return n.end();}
        public int portableFlags(Node n){return n.tag();}
    }
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("byte ingress");}
    private static void reject(Runnable action){
        checks++;try{action.run();}catch(IllegalArgumentException expected){return;}
        throw new AssertionError("expected refusal");
    }
    private static M3ASTIndex project(M3File src,M3Utf8SourceMap map,Node root){
        return M3ASTByteIngress.project(src,map,root,new Adapter(),new M3ASTIngress.Limits(128,16,5));
    }
    public static void main(String[]args)throws Exception{
        byte[] bytes="é+\ud83d\ude00".getBytes(StandardCharsets.UTF_8);
        M3File file=M3File.read(URI.create("memory:/tree"),new ByteArrayInputStream(bytes),
                StandardCharsets.UTF_8,bytes.length);
        M3Utf8SourceMap map=M3Utf8SourceMap.verified(file,bytes,bytes.length);
        Node root=new Node("expr",0,7,M3ASTIndex.NAMED,new Node[]{
                new Node("name",0,2,M3ASTIndex.TOKEN,new Node[0]),
                new Node("+",2,3,M3ASTIndex.TOKEN,new Node[0]),
                new Node("name",3,7,M3ASTIndex.TOKEN,new Node[0])});
        var index=project(file,map,root);
        check(index.nodeCount()==4 && index.start(3)==2 && index.end(3)==4);
        check(index.sourceText(1).equals("é") && index.sourceText(3).equals("\ud83d\ude00"));
        check(index.childRole(3).orElseThrow().equals("right"));
        check((index.flags(0)&M3ASTIndex.SOURCE_UNVERIFIED)!=0);
        reject(()->project(file,map,new Node("bad",1,2,M3ASTIndex.TOKEN,new Node[0])));
        reject(()->project(file,map,new Node("bad",4,5,M3ASTIndex.TOKEN,new Node[0])));
        reject(()->project(file,map,new Node("bad",-1,2,0,new Node[0])));
        check(!project(file,map,new Node("unknown",-1,-1,0,new Node[0])).hasExactRange(0));
        reject(()->project(M3File.of(file.uri(),file.text()),map,root));
        System.out.println("M3_AST_BYTE_INGRESS_PASS checks="+checks);
    }
}
