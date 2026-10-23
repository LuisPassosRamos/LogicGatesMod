package sisso.logicsim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class WorkshopAssetsTest {
    private JsonObject json(String name) throws Exception {
        try (var stream = getClass().getResourceAsStream("/" + name)) {
            assertNotNull(stream, name);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private JsonObject catalog() throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("assets/workshop/catalog.json"))).getAsJsonObject().getAsJsonObject("items");
    }
    private JsonObject item(String id) throws Exception {
        return json("Server/Item/Items/" + (id.equals("Logic_Workbench") ? "Bench/" : "Deco/") + id + ".json");
    }

    @Test void creativeCategoryHasAllThreeSectionsAndUsesNativeIconRoots() throws Exception {
        var data = json("Server/Item/Category/CreativeLibrary/LogicGates.json");
        // Full codec validation needs the game's common-asset registry and runs in
        // WorkshopServerChecks. This portable test verifies the packaged contract.
        assertEquals("server.logicgates.categories.title", data.get("Name").getAsString());
        var definitions = data.getAsJsonArray("Children");
        assertEquals(3, definitions.size());
        var children = new HashSet<String>();
        for (var definition : definitions) {
            var child = definition.getAsJsonObject();
            children.add(child.get("Id").getAsString());
            assertTrue(child.get("Icon").getAsString().startsWith("Icons/ItemCategories/"));
            var image = ImageIO.read(getClass().getResource("/Common/" + child.get("Icon").getAsString()));
            assertEquals(48, image.getWidth()); assertEquals(48, image.getHeight());
        }
        assertEquals(java.util.Set.of("Components", "Gates", "Benches"), children);
        var emblem = ImageIO.read(getClass().getResource("/Common/" + data.get("Icon").getAsString()));
        assertEquals(48, emblem.getWidth()); assertEquals(48, emblem.getHeight());
    }

    @Test void everyItemHasItsOwnTransparentModelIconAndCompletePresentation() throws Exception {
        var icons = new HashSet<Integer>();
        var atlases = new HashSet<Integer>();
        assertEquals(11, catalog().size());
        for (var entry : catalog().entrySet()) {
            String id = entry.getKey();
            var item = item(id);
            assertEquals(entry.getValue().getAsJsonObject().get("color"), item.getAsJsonObject("BlockType").get("ParticleColor"));
            assertEquals(entry.getValue().getAsJsonObject().get("color"), item.getAsJsonObject("BlockType").get("TextureComputedColor"));
            assertEquals("LogicGates." + entry.getValue().getAsJsonObject().get("category").getAsString(), item.getAsJsonArray("Categories").get(0).getAsString());
            assertTrue(item.getAsJsonObject("TranslationProperties").has("Description"));
            var image = ImageIO.read(getClass().getResource("/Common/" + item.get("Icon").getAsString()));
            assertEquals(64, image.getWidth()); assertEquals(64, image.getHeight());
            int visible = 0;
            int[] pixels = image.getRGB(0, 0, 64, 64, null, 0, 64);
            for (int pixel : pixels) if ((pixel >>> 24) != 0) visible++;
            assertTrue(visible > 100 && visible < 3500, id + " icon must show a framed model");
            assertEquals(0, image.getRGB(0, 0) >>> 24);
            assertEquals(0, image.getRGB(63, 63) >>> 24);
            assertTrue(icons.add(java.util.Arrays.hashCode(pixels)), id + " duplicates another icon");
            byte[] bytes;
            try (var stream = getClass().getResourceAsStream("/Common/" + item.get("Texture").getAsString())) {
                assertNotNull(stream); bytes = stream.readAllBytes();
            }
            assertTrue(atlases.add(java.util.Arrays.hashCode(bytes)), id + " must have its own material atlas");
        }
    }

    @Test void directRecipesUseTheCorrectStationAndApprovedBatchSizes() throws Exception {
        for (var entry : catalog().entrySet()) {
            String id = entry.getKey();
            var recipe = item(id).getAsJsonObject("Recipe");
            assertEquals(entry.getValue().getAsJsonObject().get("recipe"), recipe);
            assertEquals(id.equals("Logic_Cable") ? 8 : 1, recipe.get("OutputQuantity").getAsInt());
            assertFalse(recipe.has("KnowledgeRequired"));
            if (id.equals("Logic_Workbench")) assertEquals(0, recipe.get("TimeSeconds").getAsInt(), "Fieldcraft is instantaneous");
            else assertTrue(recipe.get("TimeSeconds").getAsInt() >= 2);
            var station = recipe.getAsJsonArray("BenchRequirement");
            assertEquals(1, station.size());
            assertEquals(id.equals("Logic_Workbench") ? "Fieldcraft" : "LogicGates_Workbench", station.get(0).getAsJsonObject().get("Id").getAsString());
            boolean copper = false;
            for (var element : recipe.getAsJsonArray("Input")) {
                var input = element.getAsJsonObject();
                assertTrue(input.get("Quantity").getAsInt() > 0);
                assertNotEquals(input.has("ItemId"), input.has("ResourceTypeId"));
                if (input.has("ItemId")) {
                    String material = input.get("ItemId").getAsString();
                    assertTrue(material.startsWith("Ingredient_"), "Direct recipe must use common game materials");
                    copper |= material.equals("Ingredient_Bar_Copper");
                }
            }
            assertTrue(copper, id + " uses copper contacts/conductors");
        }
    }

    @Test void workbenchIsNativeCraftingAndHasNoElectricalComponentOrUpgrade() throws Exception {
        var block = item("Logic_Workbench").getAsJsonObject("BlockType");
        assertEquals("Crafting", block.getAsJsonObject("Bench").get("Type").getAsString());
        assertEquals("LogicGates_Workbench", block.getAsJsonObject("Bench").get("Id").getAsString());
        assertEquals(2, block.getAsJsonObject("Bench").getAsJsonArray("Categories").size());
        assertFalse(block.getAsJsonObject("Bench").has("TierLevels"));
        var components = block.getAsJsonObject("BlockEntity").getAsJsonObject("Components");
        assertEquals(java.util.Set.of("BenchBlock"), components.keySet());
        assertEquals(block.get("HitboxType"), block.get("InteractionHitboxType"));
        assertEquals("NESW", block.get("VariantRotation").getAsString());
    }

    @Test void benchHitboxesMatchEveryPieceAndOccupyTwoByOneBlocks() throws Exception {
        var block = item("Logic_Workbench").getAsJsonObject("BlockType");
        var model = json("Common/" + block.get("CustomModel").getAsString());
        var source = JsonParser.parseString(Files.readString(Path.of("assets/models/Workbench/Logic_Workbench.blockymodel"))).getAsJsonObject();
        assertEquals(source, model);
        var nodes = model.getAsJsonArray("nodes");
        var boxes = json("Server/Item/Block/Hitboxes/Logic_Workbench.json").getAsJsonArray("Boxes");
        assertEquals(nodes.size(), boxes.size());
        boolean open = false, lid = false, contacts = false;
        double minimumX = 2, maximumX = 0;
        for (int i = 0; i < nodes.size(); i++) {
            var node = nodes.get(i).getAsJsonObject();
            String name = node.get("name").getAsString();
            open |= name.equals("open_AND_inner_board"); lid |= name.startsWith("removed_AND_lid"); contacts |= name.startsWith("AND_terminal");
            var shape = node.getAsJsonObject("shape");
            var box = boxes.get(i).getAsJsonObject();
            for (String axis : new String[]{"x", "y", "z"}) {
                double center = node.getAsJsonObject("position").get(axis).getAsDouble() + shape.getAsJsonObject("offset").get(axis).getAsDouble();
                double half = shape.getAsJsonObject("settings").getAsJsonObject("size").get(axis).getAsDouble() * shape.getAsJsonObject("stretch").get(axis).getAsDouble() / 2;
                double low = box.getAsJsonObject("Min").get(axis.toUpperCase()).getAsDouble();
                double high = box.getAsJsonObject("Max").get(axis.toUpperCase()).getAsDouble();
                double offset = axis.equals("y") ? 0 : .5;
                assertEquals((center - half) / 32 + offset, low, 1e-8);
                assertEquals((center + half) / 32 + offset, high, 1e-8);
                assertTrue(low >= 0 && high <= (axis.equals("x") ? 2 : 1));
                if (axis.equals("x")) {minimumX = Math.min(minimumX, low); maximumX = Math.max(maximumX, high);}
            }
        }
        assertTrue(open && lid && contacts, "Visible open AND construction must be present");
        assertTrue(minimumX < .05 && maximumX > 1.95);
    }

    @Test void editableBenchProjectEmbedsTheExactTextureAndSourcePieces() throws Exception {
        var project = JsonParser.parseString(Files.readString(Path.of("assets/blockbench/Workbench/Logic_Workbench.bbmodel"))).getAsJsonObject();
        var source = JsonParser.parseString(Files.readString(Path.of("assets/models/Workbench/Logic_Workbench.blockymodel"))).getAsJsonObject();
        assertEquals(source.getAsJsonArray("nodes").size(), project.getAsJsonArray("elements").size());
        String embedded = project.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString().split(",", 2)[1];
        assertArrayEquals(Files.readAllBytes(Path.of("assets/textures/workshop/Logic_Workbench_Atlas.png")), java.util.Base64.getDecoder().decode(embedded));
    }
}
