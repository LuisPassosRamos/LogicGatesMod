package sisso.logicsim.classroom.ui;

import sisso.logicsim.classroom.learning.LearningViews;
import sisso.logicsim.classroom.learning.LearningViews.Draft;
import sisso.logicsim.circuit.CircuitNode.Kind;
import java.util.Arrays;
import java.util.List;

/** Exact displayed inputs for leave confirmation; normalize only when saving. */
record LearningEditorSnapshot(String title,String description,String value,LearningDeadline.Parts date,List<String> quantities,String table) {
    LearningEditorSnapshot { quantities=List.copyOf(quantities); }
    static LearningEditorSnapshot from(Draft draft,String table) {
        return new LearningEditorSnapshot(draft.title(),draft.description(),LearningViews.points(draft.value()),LearningDeadline.parts(draft.deadline()),
                Arrays.stream(Kind.values()).map(kind->{var count=draft.rules().quantities().get(kind);return count==null?"":count.toString();}).toList(),table);
    }
    static LearningEditorSnapshot from(LearningPage.Event event) {
        return new LearningEditorSnapshot(clean(event.title),clean(event.description),clean(event.value),event.dateParts(),
                Arrays.stream(event.quantities).map(LearningEditorSnapshot::clean).toList(),clean(event.truthTable));
    }
    LearningPage.Event event(String action) {
        var e=new LearningPage.Event();e.action=action;e.title=title;e.description=description;e.value=value;e.truthTable=table;
        e.day=date.day();e.month=date.month();e.year=date.year();e.hour=date.hour();e.minute=date.minute();
        for(int n=0;n<quantities.size();n++)e.quantities[n]=quantities.get(n);return e;
    }
    private static String clean(String value){return value==null?"":value;}
}
