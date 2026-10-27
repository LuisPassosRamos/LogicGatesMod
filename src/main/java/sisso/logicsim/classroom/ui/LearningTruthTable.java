package sisso.logicsim.classroom.ui;

import sisso.logicsim.classroom.learning.VerificationRules;
import java.util.Arrays;
import java.util.HashSet;

/** Per-editor draft. Unknown bits are legal draft text, never a publishable truth table. */
final class LearningTruthTable {
    static final int PAGE_SIZE=8;
    int inputs=2,outputs=1,page;
    boolean enabled,advanced,structure=true;
    private String raw="";
    private char[][] cells;

    LearningTruthTable() { reset(2,1);enabled=false; }
    void load(String value) {
        raw=value==null?"":value;page=0;enabled=!raw.isBlank();advanced=false;structure=true;
        if(!enabled){reset(2,1);enabled=false;return;}
        try {
            String[] lines=raw.lines().filter(line->!line.isBlank()).toArray(String[]::new);
            String[] first=parts(lines[0]);reset(first[0].length(),first[1].length());
            var seen=new HashSet<String>();
            for(String line:lines){var pair=parts(line);
                if(pair[0].length()!=inputs || pair[1].length()!=outputs || !seen.add(pair[0]))throw new IllegalArgumentException();
                cells[Integer.parseInt(pair[0],2)]=pair[1].toCharArray();}
        }catch(RuntimeException invalid){advanced=true;}
    }
    private static String[] parts(String line) {
        var pair=line.replaceAll("\\s+","").split("->",-1);
        if(pair.length!=2 || !pair[0].matches("[01]{1,6}") || !pair[1].matches("[01?]{1,4}"))throw new IllegalArgumentException();
        return pair;
    }
    void reset(int in,int out) {
        if(in<1 || in>VerificationRules.MAX_INPUTS || out<1 || out>VerificationRules.MAX_OUTPUTS)throw new IllegalArgumentException();
        inputs=in;outputs=out;page=0;enabled=true;advanced=false;structure=true;
        cells=new char[1<<inputs][outputs];for(var row:cells)Arrays.fill(row,'?');
    }
    void disable() { enabled=false;structure=true;raw=""; }
    String bits(int row) { return String.format("%"+inputs+"s",Integer.toBinaryString(row)).replace(' ','0'); }
    char cell(int row,int column) { return cells[row][column]; }
    void cell(String target,String value) {
        var parts=target==null?new String[0]:target.split(":",-1);
        if(!enabled || advanced || parts.length!=2 || !parts[0].matches("[01]{1,6}") || parts[0].length()!=inputs
                || !parts[1].matches("[0-3]") || value==null || !value.matches("[01?]"))throw new IllegalArgumentException();
        int row=Integer.parseInt(parts[0],2),column=Integer.parseInt(parts[1]);
        if(row<page*PAGE_SIZE || row>=Math.min(cells.length,(page+1)*PAGE_SIZE) || column>=outputs)throw new IllegalArgumentException();
        cells[row][column]=value.charAt(0);
    }
    int cases() { return 1<<inputs; }
    int completed() { int count=0;for(var row:cells){boolean complete=true;for(char bit:row)complete&=bit!='?';if(complete)count++;}return count; }
    boolean hasAnswers() { for(var row:cells)for(char bit:row)if(bit!='?')return true;return false; }
    void advanced(String value) { raw=value==null?"":value;advanced=true;enabled=true;structure=true; }
    String text() {
        if(!enabled)return "";
        if(advanced)return raw;
        var text=new StringBuilder();for(int row=0;row<cells.length;row++){if(row>0)text.append('\n');text.append(bits(row)).append(" -> ").append(cells[row]);}return text.toString();
    }
    void andExample() { reset(2,1);for(int row=0;row<4;row++)cells[row][0]=row==3?'1':'0'; }
}
