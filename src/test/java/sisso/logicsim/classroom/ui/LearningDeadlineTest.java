package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import org.bson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LearningDeadlineTest {
    @TempDir Path directory;
    LearningDeadline.Parts parts(String day,String month,String year,String hour,String minute) {
        return new LearningDeadline.Parts(day,month,year,hour,minute);
    }
    @Test void blankDateIsAbsentAndPartialDateHasALocalizedError() {
        assertNull(LearningDeadline.deadline(LearningDeadline.parts(null)));
        assertEquals(parts("","","","23","59"),LearningDeadline.parts(null));
        for(var date:List.of(parts("1","","2027","23","59"),parts("","2","2028","00","00"))) {
            var failure=assertThrows(LearningDeadline.Invalid.class,()->LearningDeadline.deadline(date));
            assertEquals("server.logicgates.learning.deadlineIncomplete",LearningPage.error(failure).getMessageId());
        }
    }
    @Test void strictDatesAndEveryMinuteRoundTripThroughBrasiliaToUtc() {
        for(int minute=0;minute<60;minute++) {
            var date=parts("29","2","2028","00",String.format(Locale.ROOT,"%02d",minute));
            long instant=LearningDeadline.deadline(date);
            assertEquals(Instant.parse("2028-02-29T03:00:00Z").toEpochMilli()+minute*60_000,instant);
            assertEquals(date,LearningDeadline.parts(instant));
        }
        var midnight=parts("31","12","2038","23","59");
        assertEquals(midnight,LearningDeadline.parts(LearningDeadline.deadline(midnight)));
        for(var date:List.of(parts("29","2","2027","00","00"),parts("31","4","2028","23","59"),
                parts("1","1","2028","24","00"),parts("1","1","2028","23","60"),parts("1","1","forged","00","00")))
            assertEquals("learning.deadlineInvalid",assertThrows(LearningDeadline.Invalid.class,()->LearningDeadline.deadline(date)).key);
    }
    @Test void monthChangeClearsOnlyAnInvalidDayAndUpdatesOnlyThatControl() {
        var c=new UICommandBuilder();assertTrue(LearningDeadline.days(c,parts("31","2","2028","15","17")));
        assertEquals(Set.of("#DeadlineDay.Entries","#DeadlineDay.Value"),Arrays.stream(c.getCommands()).map(x->x.selector).collect(java.util.stream.Collectors.toSet()));
        assertEquals("",value(c,"#DeadlineDay.Value").asString().getValue());
        assertEquals(30,value(c,"#DeadlineDay.Entries").asArray().size()); // placeholder + 29 leap days
        c=new UICommandBuilder();assertFalse(LearningDeadline.days(c,parts("29","2","2028","15","17")));
        assertEquals("29",value(c,"#DeadlineDay.Value").asString().getValue());
        c=new UICommandBuilder();assertTrue(LearningDeadline.days(c,parts("29","2","2027","15","17")));
        assertEquals(29,value(c,"#DeadlineDay.Entries").asArray().size());
    }
    @Test void nativeEntriesIncludeTheSavedYearOutsideTheWindowAndAllTimeValues() {
        var c=new UICommandBuilder();long instant=LearningDeadline.deadline(parts("31","12","2048","23","59"));
        LearningDeadline.render(c,instant,2026);
        assertEquals(13,value(c,"#DeadlineYear.Entries").asArray().size()); // blank + 11 years + saved year
        assertEquals("2048",value(c,"#DeadlineYear.Value").asString().getValue());
        assertEquals(24,value(c,"#DeadlineHour.Entries").asArray().size());
        assertEquals(60,value(c,"#DeadlineMinute.Entries").asArray().size());
        assertEquals(13,value(c,"#DeadlineMonth.Entries").asArray().size());
        assertTrue(value(c,"#DeadlineMonth.Entries").toString().contains("server.logicgates.learning.month12"));
        assertTrue(Arrays.stream(c.getCommands()).noneMatch(x->x.selector.contains(".Anchor.") || x.selector.endsWith(".Disabled")));
    }
    @Test void aStoredYearOutsideTheConventionalFourDigitsAddsNoNewDomainRestriction() {
        for(String year:List.of("0","-4","10000")) {
            var date=parts("1","1",year,"23","59");long instant=LearningDeadline.deadline(date);
            assertEquals(date,LearningDeadline.parts(instant));var c=new UICommandBuilder();LearningDeadline.render(c,instant,2026);
            assertEquals(year,value(c,"#DeadlineYear.Value").asString().getValue());
            assertEquals(13,value(c,"#DeadlineYear.Entries").asArray().size());
        }
    }
    @Test void thePageCodecCarriesAllFivePartsWithoutTrustingAnEpochFromTheClient() throws Exception {
        var event=LearningPage.Event.CODEC.decodeJson(new RawJsonReader("{\"@Day\":\"29\",\"@Month\":\"2\",\"@Year\":\"2028\",\"@Hour\":\"23\",\"@Minute\":\"59\",\"@Deadline\":\"forged\"}".toCharArray()),new ExtraInfo());
        assertEquals(parts("29","2","2028","23","59"),event.dateParts());
        assertEquals(Instant.parse("2028-03-01T02:59:00Z").toEpochMilli(),LearningDeadline.deadline(event.dateParts()));
    }
    @Test void selectedDeadlineSurvivesSavingAndReopeningTheExistingDatabase() {
        UUID teacher=UUID.randomUUID();var database=directory.resolve("classroom.db");
        var service=new sisso.logicsim.classroom.domain.ClassroomService(new sisso.logicsim.classroom.persistence.SqliteClassroomRepository(database),id->true);
        service.create(teacher,"Class","");
        var store=new sisso.logicsim.classroom.learning.LearningStore(database,id->true,sisso.logicsim.classroom.learning.LearningLimits.DEFAULT);
        var activity=store.create(teacher);var selected=parts("29","2","2048","23","59");
        activity=store.save(teacher,activity.id(),activity.revision(),new sisso.logicsim.classroom.learning.LearningViews.Draft("Title","Statement",LearningDeadline.deadline(selected),1000L));
        var reopened=new sisso.logicsim.classroom.learning.LearningStore(database,id->true,sisso.logicsim.classroom.learning.LearningLimits.DEFAULT);
        var persisted=reopened.activity(teacher,activity.id());
        assertEquals(selected,LearningDeadline.parts(persisted.editable().deadline()));
        var published=reopened.publish(teacher,persisted.id(),persisted.revision());
        assertEquals(selected,LearningDeadline.parts(published.published().deadline()));
        reopened.save(teacher,published.id(),published.revision(),new sisso.logicsim.classroom.learning.LearningViews.Draft("Private edit","",LearningDeadline.deadline(LearningDeadline.parts(null)),1000L));
        assertNull(store.activity(teacher,activity.id()).draft().deadline());
        assertEquals(selected,LearningDeadline.parts(store.activity(teacher,activity.id()).published().deadline()));
    }
    private static BsonValue value(UICommandBuilder c,String selector) {
        return BsonDocument.parse(Arrays.stream(c.getCommands()).filter(x->selector.equals(x.selector)).findFirst().orElseThrow().data).get("0");
    }
}
