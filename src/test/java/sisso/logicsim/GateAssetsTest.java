package sisso.logicsim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.codec.ExtraInfo;
import org.junit.jupiter.api.Test;
import sisso.logicsim.components.GateComponent;
import sisso.logicsim.util.LogicBlockTypes;

import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class GateAssetsTest {
    private JsonObject json(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream("/" + path)) {
            assertNotNull(stream, path);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private double number(JsonObject object, String key) { return object.get(key).getAsDouble(); }

    @Test void gateMarkersNeverPersistDerivedEnergy() {
        var marker = new GateComponent();
        assertTrue(GateComponent.CODEC.encode(marker, new ExtraInfo()).isEmpty());
        assertNotSame(marker, marker.clone());
    }

    @Test void bothItemsProvideAllPowerConnectionStatesAndInventoryModels() throws Exception {
        for (String kind : new String[]{"AND", "OR"}) {
            var item = json("Server/Item/Items/Deco/Logic_" + kind + "_Gate.json");
            var block = item.getAsJsonObject("BlockType");
            assertEquals("NESW", block.get("VariantRotation").getAsString());
            assertTrue(block.getAsJsonObject("BlockEntity").getAsJsonObject("Components").has("LogicGates_Gate"));
            assertEquals("Logic_" + kind + "_Gate", block.getAsJsonObject("Gathering").getAsJsonObject("Soft")
                    .get("ItemId").getAsString());
            assertNotNull(json("Common/" + item.get("Model").getAsString()));
            assertNotNull(getClass().getResource("/Common/" + item.get("Texture").getAsString()));
            var states = block.getAsJsonObject("State").getAsJsonObject("Definitions");
            assertEquals(32, states.size());
            for (boolean on : new boolean[]{false, true}) for (int mask = 0; mask < 16; mask++) {
                var state = states.getAsJsonObject(LogicBlockTypes.terminalState(on, mask));
                assertEquals(on, !state.get("Light").isJsonNull());
                if (on) assertEquals("#f00", state.getAsJsonObject("Light").get("Color").getAsString());
                assertEquals(state.get("HitboxType"), state.get("InteractionHitboxType"));
            }
        }
    }

    @Test void wiresMeetTheirPortsAndBlockEdgesAndHitboxesFitEveryPiece() throws Exception {
        for (String kind : new String[]{"AND", "OR"}) {
            var states = json("Server/Item/Items/Deco/Logic_" + kind + "_Gate.json").getAsJsonObject("BlockType")
                    .getAsJsonObject("State").getAsJsonObject("Definitions");
            for (boolean on : new boolean[]{false, true}) for (int mask = 0; mask < 16; mask++) {
                var state = states.getAsJsonObject(LogicBlockTypes.terminalState(on, mask));
                var nodes = json("Common/" + state.get("CustomModel").getAsString()).getAsJsonArray("nodes");
                var boxes = json("Server/Item/Block/Hitboxes/" + state.get("HitboxType").getAsString()
                        + ".json").getAsJsonArray("Boxes");
                assertEquals((kind.equals("AND") ? 4 : 14) + Integer.bitCount(mask), nodes.size());
                assertEquals(nodes.size(), boxes.size());
                int actualMask = 0;
                for (int i = 0; i < nodes.size(); i++) {
                    var node = nodes.get(i).getAsJsonObject();
                    var shape = node.getAsJsonObject("shape");
                    var bounds = boxes.get(i).getAsJsonObject();
                    double[] center = new double[3], halves = new double[3];
                    for (int a = 0; a < 3; a++) {
                        String axis = new String[]{"x", "y", "z"}[a];
                        center[a] = number(node.getAsJsonObject("position"), axis) + number(shape.getAsJsonObject("offset"), axis);
                        halves[a] = number(shape.getAsJsonObject("settings").getAsJsonObject("size"), axis)
                                * number(shape.getAsJsonObject("stretch"), axis) / 2;
                        double low = number(bounds.getAsJsonObject("Min"), axis.toUpperCase());
                        double high = number(bounds.getAsJsonObject("Max"), axis.toUpperCase());
                        assertEquals((center[a] - halves[a]) / 32 + (a == 1 ? 0 : .5), low, 1e-8);
                        assertEquals((center[a] + halves[a]) / 32 + (a == 1 ? 0 : .5), high, 1e-8);
                        assertTrue(low >= 0 && high <= 1 && low < high);
                        if (a == 1) assertTrue(high <= .1875);
                    }
                    String name = node.get("name").getAsString();
                    if (!name.startsWith("input_") && !name.equals("output")) continue;
                    int axis = center[0] == 0 ? 2 : 0;
                    assertEquals(16, Math.abs(center[axis]) + halves[axis], 1e-8);
                    double attachment = Math.abs(center[axis]) - halves[axis];
                    assertEquals(axis == 2 ? 10 : name.equals("output")
                            ? kind.equals("AND") ? 10 : 12 : kind.equals("AND") ? 10 : 4, attachment, 1e-8);
                    assertEquals(3, halves[1] * 2, 1e-8);
                    actualMask |= switch (name) {
                        case "input_a" -> 1;
                        case "output" -> 2;
                        case "input_b" -> 4;
                        case "input_c" -> 8;
                        default -> throw new AssertionError(name);
                    };
                }
                assertEquals(mask, actualMask);
            }
        }
    }

    @Test void everyFaceIsOpaqueAndOnlyTheOnIndicatorIsFullbrightRed() throws Exception {
        for (String kind : new String[]{"AND", "OR"}) {
            var item = json("Server/Item/Items/Deco/Logic_" + kind + "_Gate.json");
            var url = getClass().getResource("/Common/" + item.get("Texture").getAsString());
            assertNotNull(url);
            var image = ImageIO.read(url);
            var states = item.getAsJsonObject("BlockType").getAsJsonObject("State").getAsJsonObject("Definitions");
            for (boolean on : new boolean[]{false, true}) for (int mask = 0; mask < 16; mask++) {
                var state = states.getAsJsonObject(LogicBlockTypes.terminalState(on, mask));
                for (var element : json("Common/" + state.get("CustomModel").getAsString()).getAsJsonArray("nodes")) {
                    var node = element.getAsJsonObject();
                    var shape = node.getAsJsonObject("shape");
                    boolean indicator = node.get("name").getAsString().equals("indicator");
                    assertEquals(on && indicator, shape.get("shadingMode").getAsString().equals("fullbright"));
                    var size = shape.getAsJsonObject("settings").getAsJsonObject("size");
                    for (var face : shape.getAsJsonObject("textureLayout").entrySet()) {
                        var uv = face.getValue().getAsJsonObject();
                        assertEquals(0, uv.get("angle").getAsInt());
                        int w = size.get(face.getKey().equals("left") || face.getKey().equals("right") ? "z" : "x").getAsInt();
                        int h = size.get(face.getKey().equals("top") || face.getKey().equals("bottom") ? "z" : "y").getAsInt();
                        int u = uv.getAsJsonObject("offset").get("x").getAsInt();
                        int v = uv.getAsJsonObject("offset").get("y").getAsInt();
                        if (uv.getAsJsonObject("mirror").get("x").getAsBoolean()) u -= w;
                        if (uv.getAsJsonObject("mirror").get("y").getAsBoolean()) v -= h;
                        assertTrue(u >= 0 && v >= 0 && u + w <= image.getWidth() && v + h <= image.getHeight());
                        for (int x = u; x < u + w; x++) for (int y = v; y < v + h; y++) {
                            int color = image.getRGB(x, y);
                            assertEquals(255, color >>> 24);
                            if (indicator) {
                                int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
                                assertEquals(on, red > green * 2 && red > blue * 2);
                            }
                        }
                    }
                }
            }
        }
    }
}
