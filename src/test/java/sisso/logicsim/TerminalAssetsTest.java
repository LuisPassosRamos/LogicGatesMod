package sisso.logicsim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;
import sisso.logicsim.circuit.HorizontalDirection;
import sisso.logicsim.util.BlockComponentAccess;
import sisso.logicsim.util.LogicBlockTypes;

import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class TerminalAssetsTest {
    private JsonObject json(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream("/" + path)) {
            assertNotNull(stream, path);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private JsonObject definitions(String kind) throws Exception {
        return json("Server/Item/Items/Deco/Logic_" + kind + ".json").getAsJsonObject("BlockType")
                .getAsJsonObject("State").getAsJsonObject("Definitions");
    }

    private double value(JsonObject vector, String axis) { return vector.get(axis).getAsDouble(); }

    @Test void allPowerAndConnectionCombinationsKeepLegacyStatesAndCorrectLightAndDrops() throws Exception {
        for (String kind : new String[]{"Lever", "Lamp"}) {
            var block = json("Server/Item/Items/Deco/Logic_" + kind + ".json").getAsJsonObject("BlockType");
            var states = definitions(kind);
            assertEquals(34, states.size());
            assertTrue(states.has("On") && states.has("Off"));
            assertEquals("Logic_" + kind, block.getAsJsonObject("Gathering").getAsJsonObject("Soft")
                    .get("ItemId").getAsString());
            for (boolean on : new boolean[]{false, true}) for (int mask = 0; mask < 16; mask++) {
                var name = LogicBlockTypes.terminalState(on, mask);
                var state = states.getAsJsonObject(name);
                assertNotNull(state, name);
                assertEquals(on, LogicBlockTypes.isOnState(name));
                assertEquals(mask, LogicBlockTypes.connectionsInState(name));
                assertEquals(state.get("HitboxType"), state.get("InteractionHitboxType"));
                assertEquals(kind.equals("Lamp") && on, !state.get("Light").isJsonNull());
            }
        }
        assertTrue(LogicBlockTypes.isOnState("On"));
        assertFalse(LogicBlockTypes.isOnState("Off"));
        assertFalse(LogicBlockTypes.isOnState(null));
        assertEquals(0, LogicBlockTypes.connectionsInState("On"));
    }

    @Test void physicalAndSelectionBoxesFitEveryVisiblePieceWithoutAFullHeightBlock() throws Exception {
        for (String kind : new String[]{"Lever", "Lamp"}) {
            var states = definitions(kind);
            for (boolean on : new boolean[]{false, true}) for (int mask = 0; mask < 16; mask++) {
                var state = states.getAsJsonObject(LogicBlockTypes.terminalState(on, mask));
                var nodes = json("Common/" + state.get("CustomModel").getAsString()).getAsJsonArray("nodes");
                var boxes = json("Server/Item/Block/Hitboxes/" + state.get("HitboxType").getAsString()
                        + ".json").getAsJsonArray("Boxes");
                assertEquals((kind.equals("Lever") ? 3 : 4) + Integer.bitCount(mask), nodes.size());
                assertEquals(nodes.size(), boxes.size());
                for (int i = 0; i < nodes.size(); i++) {
                    var node = nodes.get(i).getAsJsonObject();
                    var shape = node.getAsJsonObject("shape");
                    var q = node.getAsJsonObject("orientation");
                    var rotation = new Quaterniond(value(q, "x"), value(q, "y"), value(q, "z"), value(q, "w")).normalize();
                    var minimum = new Vector3d(Double.POSITIVE_INFINITY);
                    var maximum = new Vector3d(Double.NEGATIVE_INFINITY);
                    for (int signs = 0; signs < 8; signs++) {
                        var corner = new Vector3d();
                        for (int a = 0; a < 3; a++) {
                            String axis = new String[]{"x", "y", "z"}[a];
                            double half = value(shape.getAsJsonObject("settings").getAsJsonObject("size"), axis)
                                    * value(shape.getAsJsonObject("stretch"), axis) / 2;
                            corner.setComponent(a, value(shape.getAsJsonObject("offset"), axis)
                                    + ((signs & (1 << a)) == 0 ? -half : half));
                        }
                        rotation.transform(corner);
                        var origin = node.getAsJsonObject("position");
                        corner.add(value(origin, "x"), value(origin, "y"), value(origin, "z"))
                                .div(32).add(.5, 0, .5);
                        minimum.min(corner);
                        maximum.max(corner);
                    }
                    var bounds = boxes.get(i).getAsJsonObject();
                    for (int a = 0; a < 3; a++) {
                        String axis = new String[]{"X", "Y", "Z"}[a];
                        double low = value(bounds.getAsJsonObject("Min"), axis);
                        double high = value(bounds.getAsJsonObject("Max"), axis);
                        assertEquals(minimum.get(a), low, 1e-8);
                        assertEquals(maximum.get(a), high, 1e-8);
                        assertTrue(low >= 0 && high <= 1 && low < high, kind + " extends into a neighbor");
                        if (a == 1) assertTrue(high < .65, kind + " has an invisible full-height collision");
                    }
                }
            }
        }
    }

    @Test void armsReachTheCorrectNeighborAfterEveryHorizontalBlockRotation() throws Exception {
        for (String kind : new String[]{"Lever", "Lamp"}) {
            var states = definitions(kind);
            for (int worldMask = 0; worldMask < 16; worldMask++) for (var rotation : RotationTuple.VALUES) {
                var up = new Vector3i(0, 1, 0);
                rotation.applyRotationTo(up);
                if (!up.equals(new Vector3i(0, 1, 0))) continue;
                int localMask = BlockComponentAccess.localConnectionMask(rotation, worldMask);
                var state = states.getAsJsonObject(LogicBlockTypes.terminalState(false, localMask));
                var nodes = json("Common/" + state.get("CustomModel").getAsString()).getAsJsonArray("nodes");
                int actualMask = 0;
                for (var element : nodes) {
                    var node = element.getAsJsonObject();
                    if (node.get("name").getAsString().startsWith("body_")) continue;
                    var position = node.getAsJsonObject("position");
                    int x = (int) Math.signum(value(position, "x"));
                    int z = (int) Math.signum(value(position, "z"));
                    var shape = node.getAsJsonObject("shape");
                    String axis = x != 0 ? "x" : "z";
                    double reach = Math.abs(value(position, axis)) + value(shape.getAsJsonObject("settings")
                            .getAsJsonObject("size"), axis) * value(shape.getAsJsonObject("stretch"), axis) / 2;
                    assertEquals(16, reach, 1e-8, "wire must meet the cable at the block edge");
                    var direction = new Vector3i(x, 0, z);
                    rotation.applyRotationTo(direction);
                    for (var cardinal : HorizontalDirection.values()) {
                        if (cardinal.dx() == direction.x && cardinal.dz() == direction.z) actualMask |= cardinal.bit();
                    }
                }
                assertEquals(worldMask, actualMask, kind + " points away from its neighbors");
            }
        }
    }

    @Test void enlargedBodiesAndNewArmsOnlyReadOpaquePixelsOfTheOriginalAtlases() throws Exception {
        for (String kind : new String[]{"Lever", "Lamp"}) {
            var states = definitions(kind);
            for (boolean on : new boolean[]{false, true}) {
                String textureName = "Logic_" + kind + "_" + (kind.equals("Lamp") && on ? "ON_" : "") + "Texture.png";
                var url = getClass().getResource("/Common/Items/LogicGates/" + kind + "/" + textureName);
                assertNotNull(url);
                var image = ImageIO.read(url);
                for (int mask = 0; mask < 16; mask++) {
                    var state = states.getAsJsonObject(LogicBlockTypes.terminalState(on, mask));
                    for (var node : json("Common/" + state.get("CustomModel").getAsString()).getAsJsonArray("nodes")) {
                        var shape = node.getAsJsonObject().getAsJsonObject("shape");
                        var size = shape.getAsJsonObject("settings").getAsJsonObject("size");
                        for (var face : shape.getAsJsonObject("textureLayout").entrySet()) {
                            var mapping = face.getValue().getAsJsonObject();
                            assertEquals(0, mapping.get("angle").getAsInt());
                            int w = size.get(face.getKey().equals("left") || face.getKey().equals("right") ? "z" : "x").getAsInt();
                            int h = size.get(face.getKey().equals("top") || face.getKey().equals("bottom") ? "z" : "y").getAsInt();
                            int u = mapping.getAsJsonObject("offset").get("x").getAsInt();
                            int v = mapping.getAsJsonObject("offset").get("y").getAsInt();
                            if (mapping.getAsJsonObject("mirror").get("x").getAsBoolean()) u -= w;
                            if (mapping.getAsJsonObject("mirror").get("y").getAsBoolean()) v -= h;
                            assertTrue(u >= 0 && v >= 0 && u + w <= image.getWidth() && v + h <= image.getHeight());
                            for (int x = u; x < u + w; x++) for (int y = v; y < v + h; y++) {
                                assertEquals(255, image.getRGB(x, y) >>> 24, kind + " transparent face " + face.getKey());
                            }
                        }
                    }
                }
            }
        }
    }
}
