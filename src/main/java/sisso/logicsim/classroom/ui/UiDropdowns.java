package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import java.util.ArrayList;
import java.util.List;

final class UiDropdowns {
    private UiDropdowns() { }
    static DropdownEntryInfo raw(String label,String value) { return new DropdownEntryInfo(LocalizableString.fromString(label),value); }
    static DropdownEntryInfo translated(String key,String value) { return new DropdownEntryInfo(LocalizableString.fromMessageId("server.logicgates."+key),value); }
    static List<DropdownEntryInfo> numbers(int min,int max) { var entries=new ArrayList<DropdownEntryInfo>();for(int n=min;n<=max;n++)entries.add(raw(Integer.toString(n),Integer.toString(n)));return entries; }
}
