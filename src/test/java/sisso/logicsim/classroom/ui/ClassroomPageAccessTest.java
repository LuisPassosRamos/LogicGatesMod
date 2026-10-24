package sisso.logicsim.classroom.ui;

import org.junit.jupiter.api.Test;
import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import org.bson.BsonDocument;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

class ClassroomPageAccessTest {
    @Test void aDelayedSuccessAndDelayedFailureFromBeforeRevocationCannotApplyAfterRestoration() {
        var access=new ClassroomPageAccess(); var stamp=access.current();
        var query=new CompletableFuture<String>(); var applied=new ArrayList<String>();
        query.whenComplete((value,failure)->{ if(access.current(stamp)) applied.add(failure==null?value:"old error"); });
        access.changed(false); access.changed(true);
        query.complete("old authorized screen"); assertTrue(applied.isEmpty());
        assertTrue(access.consumeRevocation()); assertFalse(access.consumeRevocation());
        var beforeSecondChange=access.current(); var failed=new CompletableFuture<String>();
        failed.whenComplete((value,failure)->{ if(access.current(beforeSecondChange)) applied.add("old error"); });
        access.changed(false); access.changed(true); failed.completeExceptionally(new IllegalStateException());
        assertTrue(applied.isEmpty()); assertTrue(access.current(access.current()));
    }
    @Test void unchangedEffectiveAuthorizationKeepsDraftsButRevocationClearsFieldsAndLoadsOnlyPersistedData() {
        var page=new ClassroomPage(null,null,null); UUID owner=UUID.randomUUID(), id=UUID.randomUUID();
        var c=new Classroom(id,owner,"Saved name","Saved description",3,"Saved message");
        var link=new Member(UUID.randomUUID(),id,owner,"Teacher",Role.TEACHER,null,true,1,null,0);
        var screen=new Screen(new Home(c,link,"Teacher",true),Kind.SETTINGS,new Page<>(List.of(),0,false),0);
        page.model.apply(screen,true); var stamp=page.access.current(); page.access.changed(true);
        assertFalse(page.access.current(stamp)); assertFalse(page.access.consumeRevocation());
        assertFalse(page.model.apply(screen,false));
        var refresh=new UICommandBuilder(); page.render(refresh,new UIEventBuilder(),false);
        assertTrue(Arrays.stream(refresh.getCommands()).noneMatch(command->command.selector!=null && command.selector.endsWith(".Value")));
        page.access.changed(false); page.access.changed(true); assertTrue(page.access.consumeRevocation());
        page.model.confirming=true; page.model.revoked();
        var denied=new UICommandBuilder(); ClassroomScreenRenderer.denied(denied);
        var values=Arrays.stream(denied.getCommands()).filter(command->command.selector!=null && command.selector.endsWith(".Value")).toList();
        assertEquals(5,values.size()); for(var value:values) assertEquals("",BsonDocument.parse(value.data).getString("0").getValue());
        assertNull(page.model.screen); assertNull(page.model.selected); assertFalse(page.model.confirming);
        assertTrue(page.model.apply(screen,false));
        var restored=new UICommandBuilder(); page.render(restored,new UIEventBuilder(),true);
        var saved=Arrays.stream(restored.getCommands()).filter(command->"#Message.Value".equals(command.selector)).findFirst().orElseThrow();
        assertEquals("Saved message",BsonDocument.parse(saved.data).getString("0").getValue());
    }
}
