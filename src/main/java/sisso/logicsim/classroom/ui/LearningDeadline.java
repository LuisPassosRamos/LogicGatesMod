package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import sisso.logicsim.classroom.learning.LearningViews;
import java.time.*;
import java.util.*;

/** Native dropdown presentation only; the domain still receives one nullable UTC instant. */
final class LearningDeadline {
    private static final String[] MONTHS={
        "server.logicgates.learning.month1","server.logicgates.learning.month2","server.logicgates.learning.month3",
        "server.logicgates.learning.month4","server.logicgates.learning.month5","server.logicgates.learning.month6",
        "server.logicgates.learning.month7","server.logicgates.learning.month8","server.logicgates.learning.month9",
        "server.logicgates.learning.month10","server.logicgates.learning.month11","server.logicgates.learning.month12"};
    record Parts(String day,String month,String year,String hour,String minute) {
        Parts { day=clean(day);month=clean(month);year=clean(year);hour=clean(hour);minute=clean(minute); }
        private static String clean(String s){return s==null?"":s.strip();}
    }
    static final class Invalid extends IllegalArgumentException {
        final String key;
        Invalid(String key){this.key=key;}
    }
    private LearningDeadline() { }
    static Long deadline(Parts p) {
        if(p.day.isEmpty() && p.month.isEmpty() && p.year.isEmpty())return null;
        if(p.day.isEmpty() || p.month.isEmpty() || p.year.isEmpty() || p.hour.isEmpty() || p.minute.isEmpty())throw new Invalid("learning.deadlineIncomplete");
        try {
            var date=LocalDate.of(number(p.year,1,9999),number(p.month,1,12),number(p.day,1,31));
            var time=LocalTime.of(number(p.hour,0,23),number(p.minute,0,59));
            return LearningViews.deadline(LearningViews.DATE.format(LocalDateTime.of(date,time)));
        }catch(DateTimeException failure){throw new Invalid("learning.deadlineInvalid");}
    }
    static Parts parts(Long at) {
        if(at==null)return new Parts("","","","23","59");
        var d=Instant.ofEpochMilli(at).atZone(LearningViews.BRASILIA);
        return new Parts(Integer.toString(d.getDayOfMonth()),Integer.toString(d.getMonthValue()),Integer.toString(d.getYear()),two(d.getHour()),two(d.getMinute()));
    }
    static void render(UICommandBuilder c,Long at,int currentYear) {
        var p=parts(at);
        var months=new ArrayList<DropdownEntryInfo>();months.add(placeholder("month"));
        for(int m=1;m<=12;m++)months.add(new DropdownEntryInfo(LocalizableString.fromMessageId(MONTHS[m-1]),Integer.toString(m)));
        c.set("#DeadlineMonth.Entries",months);
        var years=new TreeSet<Integer>();for(int y=currentYear;y<=Math.min(9999,currentYear+10);y++)years.add(y);
        if(!p.year.isEmpty())years.add(number(p.year,1,9999));
        var entries=new ArrayList<DropdownEntryInfo>();entries.add(placeholder("year"));
        years.forEach(y->entries.add(raw(Integer.toString(y),Integer.toString(y))));c.set("#DeadlineYear.Entries",entries);
        c.set("#DeadlineHour.Entries",range(0,23,true));c.set("#DeadlineMinute.Entries",range(0,59,true));
        c.set("#DeadlineMonth.Value",p.month);c.set("#DeadlineYear.Value",p.year);
        c.set("#DeadlineHour.Value",p.hour);c.set("#DeadlineMinute.Value",p.minute);
        days(c,p);
    }
    /** Updating month/year touches only the day control; no I/O and no reset of other drafts. */
    static boolean days(UICommandBuilder c,Parts p) {
        int maximum=31;
        if(!p.month.isEmpty() && !p.year.isEmpty())maximum=YearMonth.of(number(p.year,1,9999),number(p.month,1,12)).lengthOfMonth();
        var entries=new ArrayList<DropdownEntryInfo>();entries.add(placeholder("day"));entries.addAll(range(1,maximum,false));
        c.set("#DeadlineDay.Entries",entries);
        boolean reset=!p.day.isEmpty() && number(p.day,1,31)>maximum;
        c.set("#DeadlineDay.Value",reset?"":p.day);return reset;
    }
    private static int number(String value,int min,int max) {
        if(!value.matches("[0-9]{1,4}"))throw new Invalid("learning.deadlineInvalid");
        int result=Integer.parseInt(value);if(result<min || result>max)throw new Invalid("learning.deadlineInvalid");return result;
    }
    private static List<DropdownEntryInfo> range(int from,int to,boolean padded) {
        var entries=new ArrayList<DropdownEntryInfo>();for(int n=from;n<=to;n++)entries.add(raw(padded?two(n):Integer.toString(n),padded?two(n):Integer.toString(n)));return entries;
    }
    private static DropdownEntryInfo placeholder(String field){return new DropdownEntryInfo(LocalizableString.fromMessageId(switch(field){
        case "day"->"server.logicgates.learning.day";case "month"->"server.logicgates.learning.month";case "year"->"server.logicgates.learning.year";
        default->throw new IllegalArgumentException(field);}),"");}
    private static DropdownEntryInfo raw(String label,String value){return new DropdownEntryInfo(LocalizableString.fromString(label),value);}
    private static String two(int n){return String.format(Locale.ROOT,"%02d",n);}
}
