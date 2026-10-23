package sisso.logicsim;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import sisso.logicsim.circuit.CircuitNode;
import sisso.logicsim.util.GateCatalog;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class PortMarkingsTest {
    @Test void permanentLabelsAndOutputArrowAreReadableWithoutAnyConnectedCable() throws Exception {
        for (var gate:GateCatalog.gates()) {
            String path="/Common/Items/LogicGates/"+gate.folder()+"/"+gate.itemId()+"_Off_Connections_0.blockymodel";
            try(var input=getClass().getResourceAsStream(path)) {
                assertNotNull(input);
                var nodes=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("nodes");
                var labels=new HashSet<String>();
                int arrows=0;
                for(var entry:nodes) {
                    var node=entry.getAsJsonObject();
                    String name=node.get("name").getAsString();
                    if(!name.startsWith("port_")) continue;
                    var shape=node.getAsJsonObject("shape");
                    assertEquals("flat",shape.get("shadingMode").getAsString());
                    double x=node.getAsJsonObject("position").get("x").getAsDouble();
                    double z=node.getAsJsonObject("position").get("z").getAsDouble();
                    if(name.startsWith("port_output_arrow")) { assertTrue(x>0);arrows++; }
                    if(name.startsWith("port_label_A")) { assertTrue(z<0);labels.add("A"); }
                    if(name.startsWith("port_label_B")) { assertTrue(z>0);labels.add("B"); }
                    if(name.startsWith("port_label_C")) { assertTrue(x<0);labels.add("C"); }
                    assertTrue(shape.getAsJsonObject("offset").get("y").getAsDouble()>4);
                }
                assertEquals(gate.kind()==CircuitNode.Kind.NOT?java.util.Set.of("C"):java.util.Set.of("A","B","C"),labels);
                assertEquals(3,arrows);
            }
        }
    }
}
