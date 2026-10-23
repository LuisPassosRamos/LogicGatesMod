package sisso.logicsim;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.google.gson.JsonParser;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import sisso.logicsim.circuit.HorizontalDirection;
import sisso.logicsim.components.LampComponent;
import sisso.logicsim.components.LeverComponent;
import sisso.logicsim.util.BlockComponentAccess;
import org.joml.Vector3i;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class HytaleCompatibilityTest {
    private String resource(String name) throws IOException {
        try (var stream = getClass().getResourceAsStream("/" + name)) {
            assertNotNull(stream, "Missing packaged resource: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void manifestDecodesWithTheRealServerCodec() throws Exception {
        var manifest = PluginManifest.CODEC.decode(BsonDocument.parse(resource("manifest.json")), new ExtraInfo());
        assertEquals("sisso.logicsim.LogicSimPlugin", manifest.getMain());
        assertTrue(manifest.getServerVersion().satisfies(Semver.fromString("0.7.4")));
        assertFalse(manifest.getServerVersion().satisfies(Semver.fromString("0.7.3")));
        assertFalse(manifest.getServerVersion().satisfies(Semver.fromString("0.7.5")));
    }

    @Test void leverStateSurvivesCodecRoundTripsAndClonesAreIndependent() {
        var lever = new LeverComponent();
        for (boolean expected : new boolean[]{false, true, false}) {
            var encoded = LeverComponent.CODEC.encode(lever, new ExtraInfo());
            assertEquals(expected, encoded.getBoolean("On").getValue());
            var decoded = LeverComponent.CODEC.decode(encoded, new ExtraInfo());
            assertEquals(expected, decoded.isOn());
            var clone = decoded.clone();
            clone.toggle();
            assertEquals(expected, decoded.isOn());
            assertEquals(!expected, clone.isOn());
            lever.toggle();
        }
    }

    @Test void lampsDoNotPersistDerivedPower() {
        assertFalse(LampComponent.CODEC.encode(new LampComponent(), new ExtraInfo()).containsKey("On"));
    }

    @Test void cableModelsAndStatesArePackagedWithOneDropItem() throws Exception {
        var item = JsonParser.parseString(resource("Server/Item/Items/Deco/Logic_Cable.json")).getAsJsonObject();
        var block = item.getAsJsonObject("BlockType");
        assertTrue(block.getAsJsonObject("BlockEntity").getAsJsonObject("Components").has("LogicGates_Cable"));
        assertEquals("Logic_Cable", block.getAsJsonObject("Gathering").getAsJsonObject("Soft").get("ItemId").getAsString());
        var definitions = block.getAsJsonObject("State").getAsJsonObject("Definitions");
        assertEquals(16, definitions.size());
        for (int mask = 0; mask < 16; mask++) {
            var state = definitions.getAsJsonObject("Connections_" + mask);
            var model = JsonParser.parseString(resource("Common/" + state.get("CustomModel").getAsString())).getAsJsonObject();
            assertEquals(1 + Integer.bitCount(mask), model.getAsJsonArray("nodes").size());
        }
        resource("Common/" + item.get("Icon").getAsString());
        resource("Common/Items/LogicGates/Cable/Logic_Cable_Texture.png");
        resource("Server/Item/Block/Hitboxes/Logic_Cable.json");
    }

    @Test void cableHitboxStaysInsideItsBlockAndCannotOverwriteNeighbors() throws Exception {
        var hitbox = JsonParser.parseString(resource("Server/Item/Block/Hitboxes/Logic_Cable.json")).getAsJsonObject();
        for (var boxElement : hitbox.getAsJsonArray("Boxes")) {
            var box = boxElement.getAsJsonObject();
            for (String axis : new String[]{"X", "Y", "Z"}) {
                double min = box.getAsJsonObject("Min").get(axis).getAsDouble();
                double max = box.getAsJsonObject("Max").get(axis).getAsDouble();
                assertTrue(min >= 0 && max <= 1 && min < max, "Hitbox extends into a neighbor: " + axis);
            }
        }
    }

    @Test void cableFacesOnlySampleOpaqueTexturePixels() throws Exception {
        var textureUrl = getClass().getResource("/Common/Items/LogicGates/Cable/Logic_Cable_Texture.png");
        assertNotNull(textureUrl);
        var texture = ImageIO.read(textureUrl);
        for (int mask = 0; mask < 16; mask++) {
            var model = JsonParser.parseString(resource("Common/Items/LogicGates/Cable/Logic_Cable_"
                    + mask + ".blockymodel")).getAsJsonObject();
            for (var element : model.getAsJsonArray("nodes")) {
                var node = element.getAsJsonObject();
                var shape = node.getAsJsonObject("shape");
                var size = shape.getAsJsonObject("settings").getAsJsonObject("size");
                for (var face : shape.getAsJsonObject("textureLayout").entrySet()) {
                    String name = face.getKey();
                    var mapping = face.getValue().getAsJsonObject();
                    var offset = mapping.getAsJsonObject("offset");
                    int width = size.get(name.equals("left") || name.equals("right") ? "z" : "x").getAsInt();
                    int height = size.get(name.equals("top") || name.equals("bottom") ? "z" : "y").getAsInt();
                    if (mapping.get("angle").getAsInt() % 180 != 0) {
                        int swap = width; width = height; height = swap;
                    }
                    int u = offset.get("x").getAsInt(), v = offset.get("y").getAsInt();
                    String message = "Cable " + mask + ", node " + node.get("name") + ", face " + name;
                    assertTrue(u >= 0 && v >= 0 && u + width <= texture.getWidth()
                            && v + height <= texture.getHeight(), "UV outside texture: " + message);
                    for (int x = u; x < u + width; x++) {
                        for (int y = v; y < v + height; y++) {
                            assertEquals(255, texture.getRGB(x, y) >>> 24,
                                    "Transparent cable pixel at " + x + "," + y + ": " + message);
                        }
                    }
                }
            }
        }
    }

    @Test void thickerCableArmsMeetTheHubWithoutOverlappingTopFaces() throws Exception {
        var nodes = JsonParser.parseString(resource("Common/Items/LogicGates/Cable/Logic_Cable_15.blockymodel"))
                .getAsJsonObject().getAsJsonArray("nodes");
        var hub = nodes.get(0).getAsJsonObject().getAsJsonObject("shape");
        for (int i = 1; i < nodes.size(); i++) {
            var node = nodes.get(i).getAsJsonObject();
            var position = node.getAsJsonObject("position");
            String axis = position.get("x").getAsDouble() == 0 ? "z" : "x";
            var shape = node.getAsJsonObject("shape");
            double center = Math.abs(position.get(axis).getAsDouble());
            double half = shape.getAsJsonObject("settings").getAsJsonObject("size").get(axis).getAsDouble()
                    * shape.getAsJsonObject("stretch").get(axis).getAsDouble() / 2;
            double hubHalf = hub.getAsJsonObject("settings").getAsJsonObject("size").get(axis).getAsDouble()
                    * hub.getAsJsonObject("stretch").get(axis).getAsDouble() / 2;
            assertEquals(3, hubHalf, 1e-8);
            assertEquals(hubHalf, center - half, 1e-8, "overlapping top faces can flicker");
            assertEquals(16, center + half, 1e-8, "endpoint must meet the adjacent block");
        }
    }

    @Test void rotatedCableModelsRepresentTheSameWorldConnections() {
        int testedRotations = 0;
        for (var rotation : RotationTuple.VALUES) {
            var up = new Vector3i(0, 1, 0);
            rotation.applyRotationTo(up);
            if (!up.equals(new Vector3i(0, 1, 0))) continue;
            testedRotations++;
            for (int worldMask = 0; worldMask < 16; worldMask++) {
                int localMask = BlockComponentAccess.localConnectionMask(rotation, worldMask);
                int reconstructed = 0;
                for (var local : HorizontalDirection.values()) {
                    if ((localMask & local.bit()) == 0) continue;
                    var direction = new Vector3i(local.dx(), 0, local.dz());
                    rotation.applyRotationTo(direction);
                    for (var worldDirection : HorizontalDirection.values()) {
                        if (direction.x == worldDirection.dx() && direction.z == worldDirection.dz()) {
                            reconstructed |= worldDirection.bit();
                        }
                    }
                }
                assertEquals(worldMask, reconstructed, "Rotation " + rotation);
            }
        }
        assertTrue(testedRotations >= 4);
    }
}
