package sisso.logicsim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class ParityAssetsTest {
    private byte[] bytes(String path) throws Exception {
        try (var stream = getClass().getResourceAsStream("/" + path)) {
            assertNotNull(stream, path);
            return stream.readAllBytes();
        }
    }
    private JsonObject json(String path) throws Exception {
        return JsonParser.parseString(new String(bytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private double n(JsonObject object, String field) { return object.get(field).getAsDouble(); }

    @Test void portableBlockbenchProjectsMatchSourceGeometryUvsAndEmbeddedTexture() throws Exception {
        var faces = Map.of("north", "back", "south", "front", "west", "left", "east", "right", "up", "top", "down", "bottom");
        for (String kind : new String[]{"XOR", "XNOR"}) {
            String folder = kind.equals("XOR") ? "XorGate" : "XnorGate";
            var source = JsonParser.parseString(Files.readString(Path.of("assets/models/" + folder + "/Logic_" + kind + "_Gate.blockymodel"))).getAsJsonObject();
            var project = JsonParser.parseString(Files.readString(Path.of("assets/blockbench/" + kind + "/Logic_" + kind + "_Gate.bbmodel"))).getAsJsonObject();
            assertEquals("hytale_prop", project.getAsJsonObject("meta").get("model_format").getAsString());
            var nodes = source.getAsJsonArray("nodes");
            var elements = project.getAsJsonArray("elements");
            assertEquals(21, nodes.size());
            assertEquals(nodes.size(), elements.size());
            assertEquals(elements.size(), project.getAsJsonArray("outliner").size());
            var texture = project.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();
            assertTrue(texture.startsWith("data:image/png;base64,"));
            assertArrayEquals(bytes("Common/Items/LogicGates/" + folder + "/Logic_" + kind + "_Gate_Texture.png"),
                    Base64.getDecoder().decode(texture.substring(texture.indexOf(',') + 1)));
            for (int i = 0; i < nodes.size(); i++) {
                var node = nodes.get(i).getAsJsonObject();
                var element = elements.get(i).getAsJsonObject();
                assertEquals(node.get("name"), element.get("name"));
                assertEquals(element.get("uuid"), project.getAsJsonArray("outliner").get(i));
                var shape = node.getAsJsonObject("shape");
                for (int a = 0; a < 3; a++) {
                    String axis = "xyz".substring(a, a + 1);
                    double low = element.getAsJsonArray("from").get(a).getAsDouble();
                    double high = element.getAsJsonArray("to").get(a).getAsDouble();
                    assertEquals(n(shape.getAsJsonObject("settings").getAsJsonObject("size"), axis), high - low, 1e-8);
                    assertEquals(n(node.getAsJsonObject("position"), axis) + n(shape.getAsJsonObject("offset"), axis), (low + high) / 2, 1e-8);
                    assertEquals(n(shape.getAsJsonObject("stretch"), axis), element.getAsJsonArray("stretch").get(a).getAsDouble(), 1e-8);
                }
                for (var face : faces.entrySet()) {
                    var layout = shape.getAsJsonObject("textureLayout").getAsJsonObject(face.getValue());
                    var uv = element.getAsJsonObject("faces").getAsJsonObject(face.getKey()).getAsJsonArray("uv");
                    assertEquals(n(layout.getAsJsonObject("offset"), "x"), uv.get(0).getAsDouble());
                    assertEquals(n(layout.getAsJsonObject("offset"), "y"), uv.get(1).getAsDouble());
                    var size = shape.getAsJsonObject("settings").getAsJsonObject("size");
                    String faceName = face.getValue();
                    assertEquals(n(size, faceName.equals("left") || faceName.equals("right") ? "z" : "x"),
                            uv.get(2).getAsDouble() - uv.get(0).getAsDouble(), 1e-8);
                    assertEquals(n(size, faceName.equals("top") || faceName.equals("bottom") ? "z" : "y"),
                            uv.get(3).getAsDouble() - uv.get(1).getAsDouble(), 1e-8);
                }
            }
        }
    }

    @Test void runtimeModelsScaleSourceExactlyOnceAndKeepDistinctLogicalSymbols() throws Exception {
        for (String kind : new String[]{"XOR", "XNOR"}) {
            String directory = "Common/Items/LogicGates/" + (kind.equals("XOR") ? "XorGate" : "XnorGate") + "/Logic_" + kind + "_Gate";
            var source = JsonParser.parseString(Files.readString(Path.of("assets/models/" + (kind.equals("XOR") ? "XorGate" : "XnorGate") + "/Logic_" + kind + "_Gate.blockymodel"))).getAsJsonObject().getAsJsonArray("nodes");
            var runtime = json(directory + "_Off_Connections_0.blockymodel").getAsJsonArray("nodes");
            assertEquals(source.size() + 23, runtime.size());
            int curve = 0, marks = 0;
            for (int i = 0; i < source.size(); i++) {
                var before = source.get(i).getAsJsonObject();
                var after = runtime.get(i).getAsJsonObject();
                assertEquals(before.get("name"), after.get("name"));
                String name = after.get("name").getAsString();
                if (name.startsWith("extra_curve_")) curve++;
                if (name.equals("inversion_mark")) marks++;
                for (String axis : new String[]{"x", "y", "z"}) {
                    assertEquals(n(before.getAsJsonObject("position"), axis) * 2, n(after.getAsJsonObject("position"), axis), 1e-8);
                    for (String field : new String[]{"offset", "stretch"}) {
                        assertEquals(n(before.getAsJsonObject("shape").getAsJsonObject(field), axis) * 2,
                                n(after.getAsJsonObject("shape").getAsJsonObject(field), axis), 1e-8);
                    }
                }
            }
            assertEquals(7, curve);
            assertEquals(kind.equals("XNOR") ? 1 : 0, marks);
            assertArrayEquals(Files.readAllBytes(Path.of("assets/textures/Logic_Parity_Metal_Texture.png")), bytes(directory + "_Texture.png"));
        }
    }

    @Test void allWireVariantsMeetTheModelAndBlockEdgesWithThreeUnitThickness() throws Exception {
        for (String kind : new String[]{"XOR", "XNOR"}) for (boolean on : new boolean[]{false, true}) for (int mask = 0; mask < 16; mask++) {
            String directory = "Common/Items/LogicGates/" + (kind.equals("XOR") ? "XorGate" : "XnorGate") + "/Logic_" + kind + "_Gate";
            int actualMask = 0;
            for (var element : json(directory + "_" + (on ? "On" : "Off") + "_Connections_" + mask + ".blockymodel").getAsJsonArray("nodes")) {
                var node = element.getAsJsonObject();
                String name = node.get("name").getAsString();
                if (!name.startsWith("input_") && !name.equals("output")) continue;
                var shape = node.getAsJsonObject("shape");
                var size = shape.getAsJsonObject("settings").getAsJsonObject("size");
                String axis = name.equals("input_a") || name.equals("input_b") ? "z" : "x";
                double center = n(node.getAsJsonObject("position"), axis) + n(shape.getAsJsonObject("offset"), axis);
                double half = n(size, axis) * n(shape.getAsJsonObject("stretch"), axis) / 2;
                assertEquals(16, Math.abs(center) + half, 1e-8);
                assertEquals(axis.equals("z") ? 10 : name.equals("output") ? kind.equals("XOR") ? 12 : 14 : 4,
                        Math.abs(center) - half, 1e-8);
                assertEquals(3, n(size, "y") * n(shape.getAsJsonObject("stretch"), "y"), 1e-8);
                actualMask |= switch (name) { case "input_a" -> 1; case "output" -> 2; case "input_b" -> 4; case "input_c" -> 8; default -> 0; };
            }
            assertEquals(mask, actualMask);
        }
    }

    private double[] averagePatch(java.awt.image.BufferedImage image, JsonObject shape, String faceName) {
        var size = shape.getAsJsonObject("settings").getAsJsonObject("size");
        var uv = shape.getAsJsonObject("textureLayout").getAsJsonObject(faceName).getAsJsonObject("offset");
        int w = size.get(faceName.equals("left") || faceName.equals("right") ? "z" : "x").getAsInt();
        int h = size.get(faceName.equals("top") || faceName.equals("bottom") ? "z" : "y").getAsInt();
        var result = new double[3];
        for (int x = uv.get("x").getAsInt(); x < uv.get("x").getAsInt() + w; x++) {
            for (int y = uv.get("y").getAsInt(); y < uv.get("y").getAsInt() + h; y++) {
                int color = image.getRGB(x, y);
                for (int channel = 0; channel < 3; channel++) result[channel] += (color >> (16 - channel * 8) & 255) / (double) (w * h);
            }
        }
        return result;
    }

    @Test void silverBodyHasReadableShadowsAndTheUnlitIndicatorIsClearlyDarker() throws Exception {
        for (String kind : new String[]{"XOR", "XNOR"}) {
            String directory = "Common/Items/LogicGates/" + (kind.equals("XOR") ? "XorGate" : "XnorGate") + "/Logic_" + kind + "_Gate";
            var image = ImageIO.read(getClass().getResource("/" + directory + "_Texture.png"));
            JsonObject body = null, indicator = null, curve = null;
            for (var element : json(directory + "_Off_Connections_0.blockymodel").getAsJsonArray("nodes")) {
                var node = element.getAsJsonObject();
                String name = node.get("name").getAsString();
                if (name.equals("body_1")) body = node.getAsJsonObject("shape");
                if (name.equals("indicator")) indicator = node.getAsJsonObject("shape");
                if (name.equals("extra_curve_1")) curve = node.getAsJsonObject("shape");
            }
            assertNotNull(body); assertNotNull(indicator); assertNotNull(curve);
            var top = averagePatch(image, body, "top");
            var side = averagePatch(image, body, "left");
            var off = averagePatch(image, indicator, "top");
            var symbol = averagePatch(image, curve, "top");
            double light = (top[0] + top[1] + top[2]) / 3;
            assertTrue(light > 195, "base must read as light silver");
            assertTrue(light - (side[0] + side[1] + side[2]) / 3 > 50, "side shading separates the silhouette");
            assertTrue(light - (off[0] + off[1] + off[2]) / 3 > 75, "unlit indicator must contrast with the body");
            assertTrue(light - (symbol[0] + symbol[1] + symbol[2]) / 3 > 30, "XOR curve must remain legible");
            assertTrue(off[2] - off[0] < 65 && Math.abs(off[0] - off[1]) < 45, "unlit lens remains a muted gray");
            assertEquals("flat", indicator.get("shadingMode").getAsString());
            var uv = body.getAsJsonObject("textureLayout").getAsJsonObject("top").getAsJsonObject("offset");
            int min = 255, max = 0;
            for (int x = 0; x < 64; x++) for (int y = 0; y < 64; y++) {
                int red = image.getRGB(uv.get("x").getAsInt() + x, uv.get("y").getAsInt() + y) >> 16 & 255;
                min = Math.min(min, red); max = Math.max(max, red);
            }
            assertTrue(max - min > 10, "metal must contain texture detail rather than a flat fill");
        }
    }
}
