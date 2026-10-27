package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import java.util.List;
import static sisso.logicsim.util.LogicMessages.text;

final class LearningTableRenderer {
    private LearningTableRenderer() { }
    static void render(LearningTruthTable table,UICommandBuilder c,UIEventBuilder e,boolean active) {
        c.set("#TruthConfig.Visible",table.enabled);c.set("#TruthAdvanced.Visible",table.enabled && table.advanced);
        c.set("#TruthGrid.Visible",table.enabled && !table.advanced);
        c.set("#TruthPages.Visible",table.enabled && !table.advanced && table.cases()>LearningTruthTable.PAGE_SIZE);
        c.set("#TruthToggle.Text",text(table.enabled?"table.disable":"table.enable"));
        c.set("#TruthMode.Text",text(table.advanced?"table.visual":"table.advanced"));
        completion(table,c);
        if(!active || !table.structure)return;
        table.structure=false;
        c.set("#TruthInputs.Entries",UiDropdowns.numbers(1,6));c.set("#TruthOutputs.Entries",UiDropdowns.numbers(1,4));
        c.set("#TruthInputs.Value",Integer.toString(table.inputs));c.set("#TruthOutputs.Value",Integer.toString(table.outputs));
        c.set("#TruthTableField.Value",table.text());c.clear("#TruthRows");
        if(!table.enabled || table.advanced)return;
        int index=0;
        for(int row=table.page*LearningTruthTable.PAGE_SIZE;row<Math.min(table.cases(),(table.page+1)*LearningTruthTable.PAGE_SIZE);row++){
            String selector="#TruthRows["+index+++"]";c.append("#TruthRows","Pages/LogicGates/TruthRow.ui");
            c.set(selector+" #InputBits.TextSpans",text("table.inputBits").param("bits",table.bits(row)));
            for(int column=0;column<table.outputs;column++){
                String cell=selector+" #Outputs["+column+"]";c.append(selector+" #Outputs","Pages/LogicGates/TruthOutput.ui");
                c.set(cell+" #OutputName.TextSpans",text("table.outputName").param("number",column+1));
                c.set(cell+" #Value.Entries",List.of(UiDropdowns.translated("table.undefined","?"),UiDropdowns.raw("0","0"),UiDropdowns.raw("1","1")));
                c.set(cell+" #Value.Value",Character.toString(table.cell(row,column)));
                e.addEventBinding(CustomUIEventBindingType.ValueChanged,cell+" #Value",EventData.of("Action","TruthCell")
                        .append("Target",table.bits(row)+":"+column).append("@Cell",cell+" #Value.Value"));
            }
        }
        c.set("#TruthPages.Visible",table.cases()>LearningTruthTable.PAGE_SIZE);
        c.set("#TruthPrevious.Disabled",table.page==0);c.set("#TruthNext.Disabled",(table.page+1)*LearningTruthTable.PAGE_SIZE>=table.cases());
        c.set("#TruthPage.TextSpans",text("table.page").param("page",table.page+1).param("total",(table.cases()+LearningTruthTable.PAGE_SIZE-1)/LearningTruthTable.PAGE_SIZE));
    }
    static void completion(LearningTruthTable table,UICommandBuilder c) {
        c.set("#TruthCompletion.TextSpans",text("table.completion").param("done",table.completed()).param("total",table.cases()));
    }
}
