package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import java.util.*;

/** Per-open-page transport state. Local field values are never deduplicated. */
final class ClassroomPageUpdates {
    private final Map<String,String> properties=new HashMap<>();
    private List<?> rows;
    private boolean rowsKnown;

    CustomPage packet(String key,CustomPageLifetime lifetime,UICommandBuilder commands,UIEventBuilder events,List<?> currentRows) {
        boolean touchesRows=Arrays.stream(commands.getCommands()).anyMatch(c->c.type==CustomUICommandType.Clear && "#Rows".equals(c.selector));
        boolean sameRows=touchesRows && rowsKnown && Objects.equals(rows,currentRows);
        if(touchesRows && !sameRows) {
            rows=currentRows==null ? null : List.copyOf(currentRows); rowsKnown=true;
            properties.keySet().removeIf(selector->selector.startsWith("#Rows["));
        }
        var changed=new ArrayList<CustomUICommand>();
        var normalized=new ArrayList<CustomUICommand>(); var seen=new HashSet<String>();
        var source=commands.getCommands();
        for(int i=source.length-1;i>=0;i--) {
            var command=source[i];
            boolean rowStructure="#Rows".equals(command.selector)
                    && (command.type==CustomUICommandType.Clear || command.type==CustomUICommandType.Append);
            if(sameRows && rowStructure) continue;
            if(command.type==CustomUICommandType.Set) {
                if(!seen.add(command.selector)) continue;
            } else seen.clear();
            normalized.add(command);
        }
        Collections.reverse(normalized);
        for(var command:normalized) {
            boolean rowStructure="#Rows".equals(command.selector)
                    && (command.type==CustomUICommandType.Clear || command.type==CustomUICommandType.Append);
            if(command.type==CustomUICommandType.Set) {
                String previous=properties.put(command.selector,command.data);
                // Reload/revocation must overwrite locally edited text even when the last server value was identical.
                if(!command.selector.endsWith(".Value") && Objects.equals(previous,command.data)) continue;
            } else if(!rowStructure) {
                if(command.selector!=null && (command.type==CustomUICommandType.Clear || command.type==CustomUICommandType.Append)) {
                    if(command.type==CustomUICommandType.Clear)properties.keySet().removeIf(selector->selector.startsWith(command.selector+"["));
                } else { properties.clear(); rowsKnown=false; }
            }
            changed.add(command);
        }
        var bindings=Arrays.stream(events.getEvents())
                .filter(event->!sameRows || !event.selector.startsWith("#Rows["))
                .toArray(CustomUIEventBinding[]::new);
        // Even an empty event response must be sent: the native client expects an acknowledgement.
        return new CustomPage(key,false,false,lifetime,changed.toArray(CustomUICommand[]::new),bindings);
    }
}
