package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.server.core.Message;
import sisso.logicsim.classroom.domain.ClassroomViews.ActivityProgress;
import sisso.logicsim.classroom.learning.LearningViews;
import static sisso.logicsim.util.LogicMessages.text;

final class ProgressMessages {
    private ProgressMessages() { }
    static Message state(ActivityProgress activity) {
        return text(activity.submission()==null?"progress.pending":activity.score()!=null?"progress.graded":activity.late()?"progress.late":"progress.delivered")
                .param("revision",activity.revision());
    }
    static Message score(ActivityProgress activity) {
        return Message.raw((activity.score()==null?"—":LearningViews.points(activity.score()))+"/"+
                LearningViews.points(activity.submission()==null?activity.value():activity.submittedValue()));
    }
}
