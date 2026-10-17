// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.ArrayList;
import java.util.Collections;
import java.nio.file.Files;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RecipeTest {
    @Test void qualifyNamedRecipeBeforeReceiving() throws Exception {
        String seal=Fixture.seal();int schedules=0,refusals=0;
        var expected=Fixture.texts(Fixture.inputs(15));
        for(int mask=0;mask<16;mask++)for(int order=0;order<2;order++) {
            var input=Fixture.inputs(mask);if(order==1)Collections.reverse(input);
            var run=Fixture.run(input,null);assertTrue(run.errors().isEmpty(),run.errors().toString());
            assertEquals(4-Integer.bitCount(mask),run.results().size());
            var after=Fixture.apply(input,run);assertEquals(expected,Fixture.texts(after));
            var fixed=Fixture.run(after,null);assertTrue(fixed.errors().isEmpty());assertTrue(fixed.results().isEmpty());schedules++;
        }
        var full=Fixture.inputs(15);
        for(int i=0;i<full.size();i++) {
            var original=full.get(i);String path=original.getSourcePath().toString();
            if(!Fixture.manifest().target(path).addition()) {
                var missing=new ArrayList<>(full);missing.remove(i);
                assertTrue(Fixture.failure(Fixture.run(missing,null),"required source missing"));refusals++;
            }
            var dup=new ArrayList<>(full);dup.add(Fixture.source(path,original.printAll()));
            assertTrue(Fixture.failure(Fixture.run(dup,null),"duplicate source target"));refusals++;
            var drift=new ArrayList<>(full);drift.set(i,Fixture.source(path,original.printAll()+"// drift\n"));
            var failed=Fixture.run(drift,null);assertTrue(Fixture.failure(failed,"source drift"));
            assertThrows(IllegalStateException.class,()->Fixture.apply(drift,failed));refusals++;
        }
        assertTrue(Fixture.failure(Fixture.run(Fixture.inputs(0),()->{throw new java.util.concurrent.CancellationException("cancel witness");}),"cancel witness"));refusals++;
        assertThrows(IllegalStateException.class,()->M3GpuToolJoinManifest.load(Fixture.RESOURCE,p->{
            var in=Fixture.class.getResourceAsStream(p);
            if(in==null||!p.endsWith("M3LongIndexProjection.java"))return in;
            try(in){return new ByteArrayInputStream((new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)+"// drift\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        },()->{}));refusals++;
        var input=Fixture.inputs(0);input.add(Fixture.source("payload/fake.java","not code: class X { native int foo(); }\n"));
        var run=Fixture.run(input,null);assertTrue(run.errors().isEmpty());assertEquals("not code: class X { native int foo(); }\n",Fixture.texts(Fixture.apply(input,run)).get("payload/fake.java"));
        assertEquals(seal,Fixture.seal());Files.writeString(Fixture.proof().resolve("RECIPE_OK"),seal+"\n");
        Files.writeString(Fixture.proof().resolve("RECIPE_COUNTS.txt"),"SCHEDULES="+schedules+"\nREFUSALS="+refusals+"\n");
    }
}
