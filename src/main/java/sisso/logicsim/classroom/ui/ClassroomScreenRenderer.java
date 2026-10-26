package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import static sisso.logicsim.util.LogicMessages.text;

/** Presentation only: literal user text, native markup properties and role-specific immutable results. */
final class ClassroomScreenRenderer {
    private static final String[] META = {"Teacher","State","Enrollment","Date","Type","Uuid"};
    private static final String[] FIELDS = {"Name","Description","Message","Enrollment","Search"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);
    private static final Pattern WORD = Pattern.compile("\\S+|\\s+");
    private ClassroomScreenRenderer() { }

    static void render(ClassroomPageModel model, UICommandBuilder c, UIEventBuilder e, boolean resetFields) {
        var home=model.screen.home(); var classroom=home.classroom(); boolean teacher=model.teacher();
        boolean linked=home.member()!=null, settings=model.kind==Kind.SETTINGS;
        boolean list=!settings && model.kind!=Kind.DETAIL;
        c.set("#Activities.Visible",linked);
        int width=list ? 600 : 900;
        c.set("#Navigation.Visible",true); c.set("#Details.Visible",true); c.set("#NoAccess.Visible",false);
        c.set("#NavClass.Text",teacher ? text("classroom.nav.class") : linked ? text("classroom.nav.myClass") : text("classroom.nav.browse"));
        c.set("#NavIntakes.Visible",!linked || teacher); c.set("#NavInvite.Visible",teacher);
        c.set("#NavSettings.Visible",teacher || !linked && home.canTeach());
        c.set("#NavSettings.Text",linked ? text("classroom.nav.settings") : text("classroom.create"));
        c.set("#HeaderTitle.TextSpans",classroom==null ? text("classroom.title") : Message.raw(classroom.name()));
        height(c,"#HeaderTitle",classroom==null ? 34 : textHeight(classroom.name(),900,24,30,34),4);
        c.set("#HeaderInfo.TextSpans",classroom==null ? text("classroom.oneClass") : teacher
                ? text("classroom.classInfo").param("teacher",home.teacherName()).param("count",model.screen.studentCount())
                : text("classroom.studentInfo").param("teacher",home.teacherName()).param("enrollment",home.member().enrollment()));
        height(c,"#HeaderInfo",textHeight("W".repeat(20)+home.teacherName()+(linked && !teacher?home.member().enrollment():""),900,15,20,24),8);
        c.set("#ListPanel.Visible",list); c.set("#SearchBar.Visible",model.kind==Kind.BROWSE || model.kind==Kind.PLAYERS);
        c.set("#Empty.Visible",model.screen.rows().items().isEmpty());
        c.set("#Empty.TextSpans",model.page>0 ? text("classroom.emptyList") : switch(model.kind) {
            case MEMBERS -> text("classroom.emptyMembers"); case INTAKES -> teacher ? text("classroom.emptyPending") : text("classroom.emptyHistory");
            case PLAYERS -> text("classroom.emptyPlayers"); default -> text("classroom.emptyBrowse");
        });
        c.set("#Page.TextSpans",text("classroom.page").param("page",model.page+1));
        c.clear("#Rows"); int n=0;
        for (Object row:model.screen.rows().items()) {
            c.append("#Rows",ClassroomPage.ROW_LAYOUT); String selector="#Rows["+n+++"]";
            String name=switch(row) { case Summary s->s.name(); case Profile p->p.name(); case Member m->m.name(); case Intake i->teacher?i.studentName():i.classroomName(); default->""; };
            Message info=switch(row) {
                case Summary s -> Message.raw(shortText(s.teacherName(),28));
                case Profile p -> text("classroom.shortUuid").param("uuid",p.id().toString().substring(0,8));
                case Member m -> Message.raw(shortText(m.enrollment(),28));
                case Intake i -> stateText(i.state()); default -> Message.raw("");
            };
            c.set(selector+" #RowName.TextSpans",Message.raw(shortText(name,28))); c.set(selector+" #RowInfo.TextSpans",info);
            c.set(selector+" #SelectionMark.Visible",model.selected!=null && ClassroomPageModel.id(row).equals(ClassroomPageModel.id(model.selected)));
            c.set(selector+" #RowStamp.Visible",row instanceof Intake);
            if (row instanceof Intake i) c.set(selector+" #RowStamp.TextSpans",text("classroom.rowStamp").param("type",originText(i.origin())).param("date",DATE.format(Instant.ofEpochMilli(i.createdAt()))));
            height(c,selector,row instanceof Intake ? 116 : 96,6);
            e.addEventBinding(CustomUIEventBindingType.Activating,selector,EventData.of("Action","Select").append("Target",ClassroomPageModel.id(row).toString()));
        }
        c.set("#Form.Visible",settings); c.set("#MessageEditor.Visible",settings && classroom!=null);
        c.set("#View.Visible",!settings); c.set("#ActionDock.Visible",true);
        c.set("#Actions.Visible",!settings || teacher); c.set("#Save.Visible",settings && !model.confirming);
        c.set("#Save.Text",classroom==null ? text("classroom.create") : text("classroom.save"));
        c.set("#EnrollmentForm.Visible",false);
        for (String field:META) c.set("#Meta"+field+".Visible",false);
        for (String action:new String[]{"Primary","Secondary","Danger","CancelConfirm"}) c.set("#"+action+".Visible",false);
        String enrollment="";
        if (!settings) {
            Message title=classroom==null ? text("classroom.selectHint") : Message.raw(classroom.name());
            String description=classroom==null ? "" : classroom.description();
            if (classroom!=null) meta(c,"Teacher",text("classroom.meta.teacher").param("teacher",home.teacherName()),home.teacherName(),width);
            else meta(c,"Teacher",text("classroom.ingressHint"),"W".repeat(90),width);
            switch (model.selected) {
                case Summary s -> {
                    title=Message.raw(s.name()); description=s.description();
                    meta(c,"Teacher",text("classroom.meta.teacher").param("teacher",s.teacherName()),s.teacherName(),width);
                    action(c,"Primary",text("classroom.request")); c.set("#EnrollmentForm.Visible",true);
                }
                case Profile p -> {
                    title=Message.raw(p.name()); description=""; c.set("#MetaTeacher.Visible",false);
                    uuid(c,p.id().toString(),width); action(c,"Primary",text("classroom.invite"));
                }
                case Member m -> {
                    title=Message.raw(m.name()); description=""; enrollment=m.enrollment();
                    member(c,m,width); c.set("#EnrollmentForm.Visible",true);
                    action(c,"Primary",text("classroom.correct")); action(c,"Danger",model.confirming?text("classroom.confirmRemove"):text("classroom.remove"));
                }
                case Intake i -> {
                    title=Message.raw(teacher?i.studentName():i.classroomName()); description=""; enrollment=i.enrollment()==null?"":i.enrollment();
                    meta(c,"State",text("classroom.meta.state").param("state",stateText(i.state())),"W".repeat(55),width);
                    meta(c,"Type",text("classroom.meta.type").param("type",originText(i.origin())),"W".repeat(16),width);
                    meta(c,"Date",text("classroom.meta.created").param("date",DATE.format(Instant.ofEpochMilli(i.createdAt()))),"W".repeat(25),width);
                    if (!enrollment.isEmpty()) meta(c,"Enrollment",text("classroom.meta.enrollment").param("enrollment",enrollment),enrollment,width);
                    uuid(c,i.studentId().toString(),width);
                    if (teacher && i.state()==State.REQUESTED) { action(c,"Primary",text("classroom.approve")); c.set("#EnrollmentForm.Visible",true); }
                    else if (!teacher && i.state()==State.INVITED) { action(c,"Primary",text("classroom.accept")); c.set("#EnrollmentForm.Visible",true); }
                    if (i.state()==State.INVITED || i.state()==State.REQUESTED) action(c,"Secondary",teacher && i.state()==State.REQUESTED ? text("classroom.reject") : !teacher && i.state()==State.INVITED ? text("classroom.decline") : text("classroom.cancel"));
                }
                case null, default -> { }
            }
            if (model.kind==Kind.DETAIL) { member(c,home.member(),width); action(c,"Danger",model.confirming?text("classroom.confirmLeave"):text("classroom.leave")); }
            c.set("#DetailTitle.TextSpans",title);
            height(c,"#DetailTitle",title.getRawText()==null ? 32 : textHeight(title.getRawText(),width,22,28,32),8);
            c.set("#DescriptionDisplay.Visible",!description.isEmpty()); literal(c,"#DescriptionText",description,width);
            c.set("#MessageDisplay.Visible",classroom!=null && !classroom.message().isEmpty());
            literal(c,"#MessageText",classroom==null?"":classroom.message(),width);
            c.set("#CancelConfirm.Visible",model.confirming);
        }
        if (settings && teacher && classroom!=null) {
            action(c,"Danger",text(model.confirming ? "classroom.confirmDisband" : "classroom.disband"));
            c.set("#CancelConfirm.Visible",model.confirming);
        }
        if (resetFields) {
            c.set("#Name.Value",classroom==null?"":classroom.name()); c.set("#Description.Value",classroom==null?"":classroom.description());
            c.set("#Message.Value",classroom==null?"":classroom.message()); c.set("#Enrollment.Value",enrollment); c.set("#Search.Value",model.query);
        }
        c.set("#Previous.Disabled",model.page==0); c.set("#Next.Disabled",!model.screen.rows().hasNext());
    }

    static Message status(ClassroomPageModel model,boolean refreshed) {
        if (model.confirming) return text(model.kind==Kind.SETTINGS ? "classroom.confirmDisbandHint" : "classroom.confirmHint");
        if (refreshed) return model.kind==Kind.SETTINGS || model.selected!=null && model.kind!=Kind.DETAIL
                ? text("classroom.refreshed") : text("classroom.updated");
        if (model.kind==Kind.SETTINGS) return text(model.screen.home().classroom()==null ? "classroom.createHint" : "classroom.settingsHint");
        if (model.kind==Kind.DETAIL) return text("classroom.myClassHint");
        if (model.selected==null && model.screen.rows().items().isEmpty()) {
            if (model.page>0) return text("classroom.emptyPageHint");
            return switch(model.kind) {
                case MEMBERS -> text("classroom.emptyMembersHint");
                case INTAKES -> text(model.teacher() ? "classroom.emptyPendingHint" : "classroom.emptyHistoryHint");
                case PLAYERS -> text("classroom.emptyPlayersHint");
                default -> text("classroom.emptyBrowseHint");
            };
        }
        return model.selected==null ? text("classroom.ready") : text("classroom.recordHint");
    }
    static void denied(UICommandBuilder c) {
        c.set("#Activities.Visible",false);
        c.clear("#Rows"); c.set("#Navigation.Visible",false); c.set("#ListPanel.Visible",false); c.set("#Details.Visible",true);
        for(String panel:new String[]{"View","Form","ActionDock"}) c.set("#"+panel+".Visible",false);
        c.set("#NoAccess.Visible",true); c.set("#HeaderTitle.TextSpans",text("classroom.title")); height(c,"#HeaderTitle",34,4);
        c.set("#HeaderInfo.TextSpans",text("classroom.oneClass")); c.set("#Status.TextSpans",text("classroom.error.forbidden"));
        for (String field:FIELDS) c.set("#"+field+".Value","");
    }
    private static void member(UICommandBuilder c,Member m,int width) {
        meta(c,"Enrollment",text("classroom.meta.enrollment").param("enrollment",m.enrollment()),m.enrollment(),width);
        meta(c,"Date",text("classroom.meta.joined").param("date",DATE.format(Instant.ofEpochMilli(m.joinedAt()))),"W".repeat(25),width);
        uuid(c,m.playerId().toString(),width);
    }
    private static void uuid(UICommandBuilder c,String uuid,int width) {
        meta(c,"Uuid",text("classroom.meta.uuid").param("uuid",uuid),uuid,width);
    }
    private static void meta(UICommandBuilder c,String id,Message message,String value,int width) {
        c.set("#Meta"+id+".Visible",true); c.set("#Meta"+id+".TextSpans",message);
        height(c,"#Meta"+id,textHeight("W".repeat(15)+" "+value,width,15,20,24),4);
    }
    private static void literal(UICommandBuilder c,String selector,String value,int width) {
        c.set(selector+".TextSpans",Message.raw(value)); height(c,selector,textHeight(value,width,17,24,24),12);
    }
    private static void height(UICommandBuilder c,String selector,int height,int bottom) {
        var anchor=new Anchor(); anchor.setHeight(Value.of(height)); anchor.setBottom(Value.of(bottom));
        c.setObject(selector+".Anchor",anchor); // 0.7.4 rejects selectors such as .Anchor.Height.
    }
    /** Conservative wrapping estimate by region; actual font/layout remains a client acceptance check. */
    static int textHeight(String value,int width,int font,int lineHeight,int minimum) {
        int lines=0;
        for (String paragraph:value.split("\\n",-1)) {
            lines++; double used=0; var words=WORD.matcher(paragraph);
            while(words.find()) {
                String word=words.group(); double size=word.codePoints().mapToDouble(cp->glyph(cp,font)).sum();
                if (!word.isBlank() && used>0 && used+size>width) { lines++; used=0; }
                for(int cp:word.codePoints().toArray()) {
                    double next=glyph(cp,font);
                    if (used+next>width) { lines++; used=0; }
                    if (used>0 || !Character.isWhitespace(cp)) used+=next;
                }
            }
        }
        return Math.max(minimum,lines*lineHeight);
    }
    private static double glyph(int cp,int font) {
        if (Character.getType(cp)==Character.NON_SPACING_MARK) return 0;
        if (" ilI.,:;'!|".indexOf(cp)>=0) return font*.4;
        if (cp=='\t') return font*2.6;
        if ("WMwm@%".indexOf(cp)>=0 || cp>0x024f) return font;
        return font*(Character.isUpperCase(cp)?.8:.65);
    }
    private static String shortText(String value,int limit) {
        return value.codePointCount(0,value.length())<=limit ? value : value.substring(0,value.offsetByCodePoints(0,limit-3))+"...";
    }
    private static void action(UICommandBuilder c,String id,Message label) { c.set("#"+id+".Visible",true); c.set("#"+id+".Text",label); }
    private static Message originText(Origin origin) { return origin==Origin.INVITE?text("classroom.origin.invite"):text("classroom.origin.request"); }
    private static Message stateText(State state) {
        return switch(state) {
            case INVITED->text("classroom.state.invited"); case REQUESTED->text("classroom.state.requested");
            case APPROVED->text("classroom.state.approved"); case REJECTED->text("classroom.state.rejected");
            case DECLINED->text("classroom.state.declined"); case CANCELLED->text("classroom.state.cancelled"); case SUPERSEDED->text("classroom.state.superseded");
        };
    }
}
