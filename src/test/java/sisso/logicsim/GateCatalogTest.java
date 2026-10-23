package sisso.logicsim;

import org.junit.jupiter.api.Test;
import sisso.logicsim.circuit.CircuitNode;
import sisso.logicsim.util.GateCatalog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class GateCatalogTest {
    @Test void sharedCatalogCoversAllKindsAndAuthoringExportsStayOutsideRuntime() {
        assertEquals(Arrays.stream(CircuitNode.Kind.values()).filter(CircuitNode.Kind::isGate).count(),GateCatalog.gates().size());
        var ids = new HashSet<String>();
        for(var gate:GateCatalog.gates()) {
            assertTrue(ids.add(gate.itemId()));
            assertNotNull(getClass().getResource("/Server/Item/Items/Deco/"+gate.itemId()+".json"));
            assertTrue(Files.isRegularFile(Path.of("assets/models",gate.folder(),gate.itemId()+".blockymodel")));
            assertNull(getClass().getResource("/Common/Items/LogicGates/"+gate.folder()+"/"+gate.itemId()+".blockymodel"));
        }
    }
}
