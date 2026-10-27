package sisso.logicsim.classroom.learning;

import com.google.gson.*;
import sisso.logicsim.circuit.CircuitNode.Kind;
import java.util.*;
import static sisso.logicsim.classroom.learning.LearningException.Code.INVALID_RULES;

/** Publication-owned teaching criteria. Missing quantities are unrestricted; zero forbids a kind. */
public record VerificationRules(Map<Kind,Integer> quantities,String table) {
    public static final int MAX_INPUTS=6,MAX_OUTPUTS=4,MAX_COMPONENTS=2048,MAX_QUANTITY=16384;
    public static final VerificationRules EMPTY=new VerificationRules(Map.of(),"");
    public record TruthCase(String inputs,String outputs) { }
    public VerificationRules {
        var copy=new EnumMap<Kind,Integer>(Kind.class);
        if(quantities!=null)quantities.forEach((kind,count)->{
            if(kind==null || count==null || count<0 || count>MAX_QUANTITY)throw new LearningException(INVALID_RULES);
            copy.put(kind,count);
        });
        quantities=Collections.unmodifiableMap(copy);
        table=table==null?"":table.strip();
        if(table.length()>4096 || table.codePoints().anyMatch(c->Character.isISOControl(c) && c!='\n' && c!='\r' && c!='\t'))throw new LearningException(INVALID_RULES);
    }
    public boolean configured(){return !quantities.isEmpty() || !table.isEmpty();}
    public static Integer quantity(String value){
        if(value==null || value.isBlank())return null;
        if(!value.strip().matches("[0-9]{1,5}"))throw new LearningException(INVALID_RULES);
        int result=Integer.parseInt(value.strip());
        if(result>MAX_QUANTITY)throw new LearningException(INVALID_RULES);
        return result;
    }
    /** Incomplete truth tables are allowed in a private draft, never in a publication. */
    public List<TruthCase> cases(){
        if(table.isBlank())return List.of();
        var rows=new TreeMap<String,String>();int inputs=0,outputs=0;
        for(String line:table.lines().toList()){
            if(line.isBlank())continue;
            String[] pair=line.replaceAll("\\s+","").split("->",-1);
            if(pair.length!=2 || !pair[0].matches("[01]{1,6}") || !pair[1].matches("[01]{1,4}"))throw new LearningException(INVALID_RULES);
            if(inputs==0){inputs=pair[0].length();outputs=pair[1].length();}
            if(pair[0].length()!=inputs || pair[1].length()!=outputs || rows.putIfAbsent(pair[0],pair[1])!=null)throw new LearningException(INVALID_RULES);
        }
        if(inputs==0 || rows.size()!=(1<<inputs)
                || quantities.containsKey(Kind.LEVER) && quantities.get(Kind.LEVER)!=inputs
                || quantities.containsKey(Kind.LAMP) && quantities.get(Kind.LAMP)!=outputs)throw new LearningException(INVALID_RULES);
        return rows.entrySet().stream().map(e->new TruthCase(e.getKey(),e.getValue())).toList();
    }
    public String encode(){
        if(!configured())return "";
        var object=new JsonObject();object.addProperty("format",1);var counts=new JsonObject();
        quantities.forEach((kind,count)->counts.addProperty(kind.name(),count));object.add("quantities",counts);object.addProperty("table",table);
        return object.toString();
    }
    public static VerificationRules decode(String encoded){
        if(encoded==null || encoded.isEmpty())return EMPTY;
        try{
            if(encoded.length()>16384)throw new IllegalArgumentException();
            var object=JsonParser.parseString(encoded).getAsJsonObject();
            if(object.get("format").getAsInt()!=1)throw new IllegalArgumentException();
            var counts=new EnumMap<Kind,Integer>(Kind.class);
            for(var e:object.getAsJsonObject("quantities").entrySet()){
                var value=quantity(e.getValue().getAsString());if(value==null)throw new IllegalArgumentException();counts.put(Kind.valueOf(e.getKey()),value);
            }
            return new VerificationRules(counts,object.get("table").getAsString());
        }catch(RuntimeException failure){throw new LearningException(INVALID_RULES,failure);}
    }
}
