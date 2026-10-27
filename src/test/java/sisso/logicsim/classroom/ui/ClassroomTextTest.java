package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Check the messages actually sent for packaged widgets, independently of the server locale. */
class ClassroomTextTest {
    @Test void allMarkupLabelsUseClientResolvedMessagesIncludingHiddenFormsAndConfirmations() throws Exception {
        record Page(String layout, Consumer<UICommandBuilder> build) { }
        var pages=List.of(
                new Page(ClassroomPage.LAYOUT,c->new ClassroomPage(null,null,null).build(null,c,new UIEventBuilder(),null)),
                new Page(LearningPage.LAYOUT,c->new LearningPage(null,null,null,null,null).build(null,c,new UIEventBuilder(),null)),
                new Page(ClassroomProbePage.LAYOUT,c->new ClassroomProbePage(null).build(null,c,new UIEventBuilder(),null)));
        var english=catalog("en-US");var portuguese=catalog("pt-BR");
        var widget=Pattern.compile("(?:\\$C\\.@)?(Label|Title|\\w*TextButton)\\s+#(\\w+)\\s*\\{([^{}]*)}",Pattern.DOTALL);
        var blank=Pattern.compile("(?:@Text\\s*=|\\bText\\s*:)\\s*\"\";");
        var messageId=Pattern.compile("server\\.logicgates\\.[A-Za-z0-9_.]+");
        for(var page:pages){
            String layout=resource("/Common/UI/Custom/"+page.layout());
            assertFalse(layout.contains("%server.logicgates."),page.layout());
            var c=new UICommandBuilder();page.build().accept(c);
            var matcher=widget.matcher(layout);int translated=0;
            while(matcher.find()){
                if(!blank.matcher(matcher.group(3)).find())continue;
                String selector="#"+matcher.group(2)+(matcher.group(1).endsWith("TextButton")?".Text":".TextSpans");
                var command=Arrays.stream(c.getCommands()).filter(x->selector.equals(x.selector)).findFirst().orElseThrow(()->new AssertionError("Untranslated widget: "+selector));
                var id=messageId.matcher(command.data);assertTrue(id.find(),selector+": "+command.data);
                assertTrue(english.containsKey(id.group()),selector);assertTrue(portuguese.containsKey(id.group()),selector);
                translated++;
            }
            assertTrue(translated>0,page.layout());
        }
    }

    private static Map<String,String> catalog(String locale) throws Exception {
        var result=new HashMap<String,String>();
        for(String line:resource("/Server/Languages/"+locale+"/server.lang").split("\\R")){
            if(line.isBlank() || line.stripLeading().startsWith("#"))continue;
            String[] entry=line.split("=",2);result.put("server."+entry[0].strip(),entry[1].strip());
        }
        return result;
    }
    private static String resource(String path) throws Exception {
        try(var input=ClassroomTextTest.class.getResourceAsStream(path)){
            assertNotNull(input,path);return new String(input.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
}
