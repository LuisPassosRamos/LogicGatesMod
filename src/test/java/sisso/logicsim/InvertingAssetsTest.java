package sisso.logicsim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.protocol.BlockPlacementRotationMode;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockPlacementSettings;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import org.bson.BsonDocument;
import org.joml.Vector3i;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class InvertingAssetsTest {
    private byte[] bytes(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream("/" + path)) {
            assertNotNull(stream, path);
            return stream.readAllBytes();
        }
    }
    private JsonObject json(String path) throws Exception {
        return JsonParser.parseString(new String(bytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private double n(JsonObject vector, String axis) { return vector.get(axis).getAsDouble(); }

    @Test void placementPacketAndYawOffsetPointTheLocalOutputAlongThePlayersLook() throws Exception {
        for (String kind : new String[]{"AND", "OR", "NOT", "NAND", "NOR", "XOR", "XNOR"}) {
            var block = json("Server/Item/Items/Deco/Logic_" + kind + "_Gate.json").getAsJsonObject("BlockType");
            var settings = BlockPlacementSettings.CODEC.decode(
                    BsonDocument.parse(block.getAsJsonObject("PlacementSettings").toString()), new ExtraInfo()).toPacket();
            assertEquals(BlockPlacementRotationMode.FacingPlayer, settings.rotationMode);
            assertTrue(settings.allowRotationKey);
            var offset = Rotation.valueOf(block.get("RotationYawPlacementOffset").getAsString());
            assertEquals(Rotation.Ninety, offset);
            for (var facingPlayer : Rotation.NORMAL) {
                var towardPlayer = new Vector3i(0, 0, 1);
                RotationTuple.of(facingPlayer, Rotation.None, Rotation.None).applyRotationTo(towardPlayer);
                var look = towardPlayer.negate();
                var actualOutput = new Vector3i(1, 0, 0);
                RotationTuple.of(facingPlayer.add(offset), Rotation.None, Rotation.None).applyRotationTo(actualOutput);
                assertEquals(look, actualOutput, kind + " output must point away from player along their look");
            }
        }
    }

    @Test void notUsesTheAndTextureWithoutEditingItsPixelsAndHasOnlyFrontRearVariants() throws Exception {
        assertArrayEquals(bytes("Common/Items/LogicGates/AndGate/Logic_AND_Gate_Texture.png"),
                bytes("Common/Items/LogicGates/NotGate/Logic_NOT_Gate_Texture.png"));
        var item = json("Server/Item/Items/Deco/Logic_NOT_Gate.json");
        var states = item.getAsJsonObject("BlockType").getAsJsonObject("State").getAsJsonObject("Definitions");
        assertEquals(8, states.size());
        for (var entry : states.entrySet()) {
            int mask = Integer.parseInt(entry.getKey().substring(entry.getKey().lastIndexOf('_') + 1));
            assertEquals(0, mask & ~10);
        }
    }

    @Test void inverterGeometryHitboxesTexturesAndIndicatorsFitAllStates() throws Exception {
        for (String kind : new String[]{"NOT", "NAND", "NOR", "XOR", "XNOR"}) {
            var item = json("Server/Item/Items/Deco/Logic_" + kind + "_Gate.json");
            var block = item.getAsJsonObject("BlockType");
            assertEquals("Logic_" + kind + "_Gate", block.getAsJsonObject("Gathering")
                    .getAsJsonObject("Soft").get("ItemId").getAsString());
            assertNotNull(json("Common/" + item.get("Model").getAsString()));
            var texture = ImageIO.read(getClass().getResource("/Common/" + item.get("Texture").getAsString()));
            var states = block.getAsJsonObject("State").getAsJsonObject("Definitions");
            assertEquals(kind.equals("NOT") ? 8 : 32, states.size());
            for (var entry : states.entrySet()) {
                boolean on = entry.getKey().startsWith("On_");
                int mask = Integer.parseInt(entry.getKey().substring(entry.getKey().lastIndexOf('_') + 1));
                var state = entry.getValue().getAsJsonObject();
                assertEquals(on, !state.get("Light").isJsonNull());
                assertEquals(state.get("HitboxType"), state.get("InteractionHitboxType"));
                var nodes = json("Common/" + state.get("CustomModel").getAsString()).getAsJsonArray("nodes");
                var boxes = json("Server/Item/Block/Hitboxes/" + state.get("HitboxType").getAsString()
                        + ".json").getAsJsonArray("Boxes");
                int bodyCount = kind.equals("NOT") ? 10 : kind.equals("NAND") ? 5 :
                        kind.equals("XOR") || kind.equals("XNOR") ? 21 : 14;
                assertEquals(bodyCount + Integer.bitCount(mask), nodes.size());
                assertEquals(nodes.size(), boxes.size());
                int indicators = 0;
                for (int i = 0; i < nodes.size(); i++) {
                    var node = nodes.get(i).getAsJsonObject();
                    var shape = node.getAsJsonObject("shape");
                    var size = shape.getAsJsonObject("settings").getAsJsonObject("size");
                    var bounds = boxes.get(i).getAsJsonObject();
                    for (String axis : new String[]{"x", "y", "z"}) {
                        double center = n(node.getAsJsonObject("position"), axis) + n(shape.getAsJsonObject("offset"), axis);
                        double half = n(size, axis) * n(shape.getAsJsonObject("stretch"), axis) / 2;
                        double low = n(bounds.getAsJsonObject("Min"), axis.toUpperCase());
                        double high = n(bounds.getAsJsonObject("Max"), axis.toUpperCase());
                        assertEquals((center - half) / 32 + (axis.equals("y") ? 0 : .5), low, 1e-8);
                        assertEquals((center + half) / 32 + (axis.equals("y") ? 0 : .5), high, 1e-8);
                        assertTrue(low >= 0 && high <= 1 && low < high);
                        if (axis.equals("y")) assertTrue(high <= .1875);
                    }
                    boolean indicator = node.get("name").getAsString().startsWith("indicator");
                    if (indicator) indicators++;
                    assertEquals(on && indicator, shape.get("shadingMode").getAsString().equals("fullbright"));
                    for (var face : shape.getAsJsonObject("textureLayout").entrySet()) {
                        var uv = face.getValue().getAsJsonObject();
                        assertEquals(0, uv.get("angle").getAsInt());
                        int w = size.get(face.getKey().equals("left") || face.getKey().equals("right") ? "z" : "x").getAsInt();
                        int h = size.get(face.getKey().equals("top") || face.getKey().equals("bottom") ? "z" : "y").getAsInt();
                        int u = uv.getAsJsonObject("offset").get("x").getAsInt();
                        int v = uv.getAsJsonObject("offset").get("y").getAsInt();
                        if (uv.getAsJsonObject("mirror").get("x").getAsBoolean()) u -= w;
                        if (uv.getAsJsonObject("mirror").get("y").getAsBoolean()) v -= h;
                        assertTrue(u >= 0 && v >= 0 && u + w <= texture.getWidth() && v + h <= texture.getHeight());
                        for (int x = u; x < u + w; x++) for (int y = v; y < v + h; y++) {
                            int color = texture.getRGB(x, y);
                            assertEquals(255, color >>> 24, kind + " transparent face " + node.get("name"));
                            if (indicator) assertEquals(on, (color >> 16 & 255) > (color >> 8 & 255) * 2
                                    && (color >> 16 & 255) > (color & 255) * 2);
                        }
                    }
                }
                assertEquals(kind.equals("NOT") ? 9 : 1, indicators);
            }
        }
    }
}
