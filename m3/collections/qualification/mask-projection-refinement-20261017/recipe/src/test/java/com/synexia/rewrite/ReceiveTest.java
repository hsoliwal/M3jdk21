// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Files;
import java.util.ArrayList;
import org.openrewrite.SourceFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ReceiveTest {
    @Test void receiveActualResultsWithFixedPointReadback() throws Exception {
        String seal=Fixture.seal();assertEquals(seal+"\n",Files.readString(Fixture.proof().resolve("RECIPE_OK")));
        var observed=new ArrayList<SourceFile>();
        for(var t:Fixture.manifest().targets())if(Files.exists(Fixture.repo().resolve(t.path())))observed.add(Fixture.source(t.path(),Files.readString(Fixture.repo().resolve(t.path()))));
        var current=Fixture.run(observed,null);assertTrue(current.errors().isEmpty(),current.errors().toString());
        assertEquals(Fixture.texts(Fixture.inputs(15)),Fixture.texts(Fixture.apply(observed,current)));
        var before=Fixture.inputs(0);var run=Fixture.run(before,null);assertTrue(run.errors().isEmpty());assertEquals(4,run.results().size());
        var after=Fixture.apply(before,run);var dest=Fixture.proof().resolve("receiver");assertFalse(Files.exists(dest));Files.createDirectories(dest);
        for(var f:before)Fixture.emit(dest.resolve("before"),f);
        for(var f:after)Fixture.emit(dest.resolve("after"),f);
        for(var change:run.results())Fixture.emit(dest.resolve("results"),change.getAfter());
        var reread=new ArrayList<SourceFile>();
        for(var f:after)reread.add(Fixture.source(f.getSourcePath().toString(),Files.readString(dest.resolve("after").resolve(f.getSourcePath()))));
        assertEquals(Fixture.texts(after),Fixture.texts(reread));var fixed=Fixture.run(reread,null);assertTrue(fixed.errors().isEmpty());assertTrue(fixed.results().isEmpty());
        Files.writeString(dest.resolve("SDK_RECEIPT.txt"),"OBSERVED_RESULTS="+current.results().size()+"\nFROZEN_REPLAY_RESULTS=4\nREADBACK_RESULTS=0\n");
        Files.writeString(Fixture.proof().resolve("SDK_OK"),seal+"\n");
    }
}
