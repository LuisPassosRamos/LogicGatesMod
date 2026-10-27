package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

/** Send every system label through the same client-resolved path as dynamic messages. */
final class ClassroomText {
    private ClassroomText() { }

    static void classroom(UICommandBuilder c) {
        c.set("#PageTitle.TextSpans", Message.translation("server.logicgates.classroom.title"));
        c.set("#NavClass.Text", Message.translation("server.logicgates.classroom.nav.class"));
        c.set("#Activities.Text", Message.translation("server.logicgates.learning.activities"));
        c.set("#NavIntakes.Text", Message.translation("server.logicgates.classroom.nav.intakes"));
        c.set("#NavInvite.Text", Message.translation("server.logicgates.classroom.invite"));
        c.set("#NavSettings.Text", Message.translation("server.logicgates.classroom.nav.settings"));
        c.set("#HeaderTitle.TextSpans", Message.translation("server.logicgates.classroom.title"));
        c.set("#HeaderInfo.TextSpans", Message.translation("server.logicgates.classroom.oneClass"));
        c.set("#Find.Text", Message.translation("server.logicgates.classroom.find"));
        c.set("#Empty.TextSpans", Message.translation("server.logicgates.classroom.emptyList"));
        c.set("#Previous.Text", Message.translation("server.logicgates.classroom.previous"));
        c.set("#Next.Text", Message.translation("server.logicgates.classroom.next"));
        c.set("#OverviewTitle.TextSpans", Message.translation("server.logicgates.progress.overview"));
        c.set("#DescriptionLabel.TextSpans", Message.translation("server.logicgates.classroom.description"));
        c.set("#MessageLabel.TextSpans", Message.translation("server.logicgates.classroom.message"));
        c.set("#Technical.Text", Message.translation("server.logicgates.progress.details"));
        c.set("#StudentActivitiesLabel.TextSpans", Message.translation("server.logicgates.progress.studentActivities"));
        c.set("#StudentActivitiesEmpty.TextSpans", Message.translation("server.logicgates.progress.noActivities"));
        c.set("#NameLabel.TextSpans", Message.translation("server.logicgates.classroom.name"));
        c.set("#DescriptionLabel2.TextSpans", Message.translation("server.logicgates.classroom.description"));
        c.set("#MessageLabel2.TextSpans", Message.translation("server.logicgates.classroom.message"));
        c.set("#NoAccess.TextSpans", Message.translation("server.logicgates.classroom.noAccess"));
        c.set("#ActivityPrevious.Text", Message.translation("server.logicgates.classroom.previous"));
        c.set("#ActivityNext.Text", Message.translation("server.logicgates.classroom.next"));
        c.set("#ManageMember.Text", Message.translation("server.logicgates.progress.manageStudent"));
        c.set("#EnrollmentLabel.TextSpans", Message.translation("server.logicgates.classroom.enrollment"));
        c.set("#Primary.Text", Message.translation("server.logicgates.classroom.request"));
        c.set("#Secondary.Text", Message.translation("server.logicgates.classroom.cancel"));
        c.set("#Danger.Text", Message.translation("server.logicgates.classroom.remove"));
        c.set("#CancelConfirm.Text", Message.translation("server.logicgates.classroom.cancel"));
        c.set("#Save.Text", Message.translation("server.logicgates.classroom.save"));
        c.set("#Disband.Text", Message.translation("server.logicgates.classroom.disband"));
        c.set("#ConfirmDisband.Text", Message.translation("server.logicgates.classroom.confirmDisband"));
        c.set("#CancelDisband.Text", Message.translation("server.logicgates.classroom.cancel"));
        c.set("#Status.TextSpans", Message.translation("server.logicgates.classroom.loading"));
        c.set("#Reload.Text", Message.translation("server.logicgates.classroom.reload"));
        c.set("#Close.Text", Message.translation("server.logicgates.classroom.probe.close"));
    }

    static void learning(UICommandBuilder c) {
        c.set("#PageTitle.TextSpans", Message.translation("server.logicgates.learning.activities"));
        c.set("#Back.Text", Message.translation("server.logicgates.learning.classroom"));
        c.set("#Activities.Text", Message.translation("server.logicgates.learning.activities"));
        c.set("#New.Text", Message.translation("server.logicgates.learning.newActivity"));
        c.set("#Discard.Text", Message.translation("server.logicgates.learning.discard"));
        c.set("#DiscardHelpLabel.TextSpans", Message.translation("server.logicgates.review.discardHelp"));
        c.set("#ConfirmCopy.Text", Message.translation("server.logicgates.review.confirmDiscard"));
        c.set("#CancelCopy.Text", Message.translation("server.logicgates.classroom.cancel"));
        c.set("#Previous.Text", Message.translation("server.logicgates.classroom.previous"));
        c.set("#Next.Text", Message.translation("server.logicgates.classroom.next"));
        c.set("#SectionBasics.Text", Message.translation("server.logicgates.editor.basics"));
        c.set("#SectionBasicsActive.Text", Message.translation("server.logicgates.editor.basics"));
        c.set("#SectionComponents.Text", Message.translation("server.logicgates.editor.components"));
        c.set("#SectionComponentsActive.Text", Message.translation("server.logicgates.editor.components"));
        c.set("#SectionTable.Text", Message.translation("server.logicgates.editor.table"));
        c.set("#SectionTableActive.Text", Message.translation("server.logicgates.editor.table"));
        c.set("#UnsavedLabel.TextSpans", Message.translation("server.logicgates.editor.unsaved"));
        c.set("#SaveAndLeave.Text", Message.translation("server.logicgates.editor.saveAndLeave"));
        c.set("#DiscardChanges.Text", Message.translation("server.logicgates.editor.discardChanges"));
        c.set("#KeepEditing.Text", Message.translation("server.logicgates.editor.keepEditing"));
        c.set("#CatalogOverviewTitle.TextSpans", Message.translation("server.logicgates.progress.overview"));
        c.set("#TitleLabel.TextSpans", Message.translation("server.logicgates.learning.title"));
        c.set("#DeadlineLabel.TextSpans", Message.translation("server.logicgates.learning.deadline"));
        c.set("#DayLabel.TextSpans", Message.translation("server.logicgates.learning.day"));
        c.set("#MonthLabel.TextSpans", Message.translation("server.logicgates.learning.month"));
        c.set("#YearLabel.TextSpans", Message.translation("server.logicgates.learning.year"));
        c.set("#HourLabel.TextSpans", Message.translation("server.logicgates.learning.hour"));
        c.set("#MinuteLabel.TextSpans", Message.translation("server.logicgates.learning.minute"));
        c.set("#ValueLabel.TextSpans", Message.translation("server.logicgates.learning.value"));
        c.set("#DescriptionLabel.TextSpans", Message.translation("server.logicgates.learning.description"));
        c.set("#RequirementsLabel.TextSpans", Message.translation("server.logicgates.learning.requirements"));
        c.set("#RequirementsHelpLabel.TextSpans", Message.translation("server.logicgates.learning.requirementsHelp"));
        c.set("#ModuleLeverLabel.TextSpans", Message.translation("server.logicgates.learning.moduleLever"));
        c.set("#ModuleCableLabel.TextSpans", Message.translation("server.logicgates.learning.moduleCable"));
        c.set("#ModuleLampLabel.TextSpans", Message.translation("server.logicgates.learning.moduleLamp"));
        c.set("#ExpectedTableLabel.TextSpans", Message.translation("server.logicgates.learning.expectedTable"));
        c.set("#IntroLabel.TextSpans", Message.translation("server.logicgates.table.intro"));
        c.set("#TruthToggle.Text", Message.translation("server.logicgates.table.enable"));
        c.set("#TruthHelp.Text", Message.translation("server.logicgates.table.help"));
        c.set("#TruthHelpText.TextSpans", Message.translation("server.logicgates.table.helpText"));
        c.set("#InputsLabel.TextSpans", Message.translation("server.logicgates.table.inputs"));
        c.set("#OutputsLabel.TextSpans", Message.translation("server.logicgates.table.outputs"));
        c.set("#TruthAnd.Text", Message.translation("server.logicgates.table.andExample"));
        c.set("#TruthMode.Text", Message.translation("server.logicgates.table.advanced"));
        c.set("#LegendLabel.TextSpans", Message.translation("server.logicgates.table.legend"));
        c.set("#TruthPrevious.Text", Message.translation("server.logicgates.classroom.previous"));
        c.set("#TruthNext.Text", Message.translation("server.logicgates.classroom.next"));
        c.set("#TableHelpLabel.TextSpans", Message.translation("server.logicgates.learning.tableHelp"));
        c.set("#ReplaceHelpLabel.TextSpans", Message.translation("server.logicgates.table.replaceHelp"));
        c.set("#TruthConfirmChange.Text", Message.translation("server.logicgates.table.replace"));
        c.set("#TruthCancelChange.Text", Message.translation("server.logicgates.classroom.cancel"));
        c.set("#Edit.Text", Message.translation("server.logicgates.learning.editActivity"));
        c.set("#DeadlineState.TextSpans", Message.translation("server.logicgates.learning.deadlineClosed"));
        c.set("#NoSubmission.TextSpans", Message.translation("server.logicgates.learning.notSubmitted"));
        c.set("#ScoreLabel.TextSpans", Message.translation("server.logicgates.learning.score"));
        c.set("#FeedbackLabel.TextSpans", Message.translation("server.logicgates.learning.feedback"));
        c.set("#ShowStatement.Text", Message.translation("server.logicgates.review.statement"));
        c.set("#ShowSubmission.Text", Message.translation("server.logicgates.review.mySubmission"));
        c.set("#SaveDraft.Text", Message.translation("server.logicgates.learning.saveDraft"));
        c.set("#Publish.Text", Message.translation("server.logicgates.learning.publish"));
        c.set("#CancelEdit.Text", Message.translation("server.logicgates.learning.cancelEdit"));
        c.set("#Archive.Text", Message.translation("server.logicgates.learning.archive"));
        c.set("#Restore.Text", Message.translation("server.logicgates.learning.restore"));
        c.set("#Students.Text", Message.translation("server.logicgates.learning.students"));
        c.set("#Deliver.Text", Message.translation("server.logicgates.learning.deliver"));
        c.set("#SubmitArea.Text", Message.translation("server.logicgates.learning.submitArea"));
        c.set("#CancelSelection.Text", Message.translation("server.logicgates.learning.cancelSelection"));
        c.set("#Newer.Text", Message.translation("server.logicgates.learning.newer"));
        c.set("#Older.Text", Message.translation("server.logicgates.learning.older"));
        c.set("#Verify.Text", Message.translation("server.logicgates.learning.verify"));
        c.set("#SaveGrade.Text", Message.translation("server.logicgates.learning.saveGrade"));
        c.set("#Paste.Text", Message.translation("server.logicgates.learning.paste"));
        c.set("#Status.TextSpans", Message.translation("server.logicgates.learning.loading"));
        c.set("#Reload.Text", Message.translation("server.logicgates.classroom.reload"));
        c.set("#Close.Text", Message.translation("server.logicgates.classroom.probe.close"));
    }

    static void probe(UICommandBuilder c) {
        c.set("#PageTitle.TextSpans", Message.translation("server.logicgates.classroom.probe.title"));
        c.set("#Description.TextSpans", Message.translation("server.logicgates.classroom.probe.description"));
        c.set("#Increment.Text", Message.translation("server.logicgates.classroom.probe.increment"));
        c.set("#Close.Text", Message.translation("server.logicgates.classroom.probe.close"));
    }
}
