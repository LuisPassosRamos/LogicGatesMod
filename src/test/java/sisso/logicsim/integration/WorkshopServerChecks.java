package sisso.logicsim.integration;

import com.hypixel.hytale.builtin.crafting.component.BenchBlock;
import com.hypixel.hytale.builtin.crafting.component.CraftingManager;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.bench.CraftingBench;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemCategory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import org.joml.Vector3i;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.util.BlockComponentAccess;
import sisso.logicsim.util.GateCatalog;
import java.util.ArrayList;
import java.util.List;

/** Actual 0.7.4 assets, native inventory transactions and multiblock operations.
 * Player UI/queue/network interaction remains a client acceptance test.
 */
final class WorkshopServerChecks {
    private static final String BENCH = "Logic_Workbench";
    private final List<String> passed;
    WorkshopServerChecks(List<String> passed) { this.passed = passed; }

    void create(World world) throws Exception {
        assetsAndRecipes();
        for (int x = 0; x < 30; x++) for (int z = -18; z < -11; z++)
            place(world, p(x, 63, z), "Rock_Stone", RotationTuple.NONE_INDEX);
        int index = 0;
        for (var yaw : Rotation.NORMAL) {
            var rotation = RotationTuple.of(yaw, Rotation.None, Rotation.None);
            var position = p(3 + index * 7, 64, -15);
            var direction = new Vector3i(1, 0, 0);
            rotation.applyRotationTo(direction);
            var filler = p(position.x() + direction.x, 64, position.z() + direction.z);
            place(world, filler, "Rock_Stone", RotationTuple.NONE_INDEX);
            check(!canPlace(world, position, rotation), "occupied filler must prevent placement");
            place(world, filler, "Empty", RotationTuple.NONE_INDEX);
            check(canPlace(world, position, rotation), "bench fits on its two-block footprint: " + position + "/" + yaw);
            place(world, position, BENCH, rotation.index());
            check(BlockComponentAccess.getIfLoaded(world, position, BenchBlock.getComponentType()) != null,
                    "native BenchBlock attached");
            check(blocks(world, filler).getFiller(filler.x(), filler.y(), filler.z()) != 0, "second footprint block reserved");
            check(BlockComponentAccess.rotationIfLoaded(world, position).index() == rotation.index(), "bench orientation kept");
            if (index < 3) {
                place(world, position, "Empty", RotationTuple.NONE_INDEX);
                check(blocks(world, filler).getFiller(filler.x(), filler.y(), filler.z()) == 0, "removing bench releases filler");
                check(canPlace(world, position, rotation), "bench can be replaced without invisible leftovers");
            }
            index++;
        }
        passed.add("native bench placement, blocked footprint, rotation and filler removal across four orientations");
    }

    void reload(World world) throws Exception {
        assetsAndRecipes();
        var position = p(24, 64, -15);
        var type = BlockComponentAccess.getBlockType(world, position);
        check(type != null && type.getItem() != null && BENCH.equals(type.getItem().getId()), "bench survived complete restart");
        check(type.getBench() instanceof CraftingBench, "native crafting restored");
        check(BlockComponentAccess.getIfLoaded(world, position, BenchBlock.getComponentType()) != null, "persisted native bench component restored");
        var rotation = BlockComponentAccess.rotationIfLoaded(world, position);
        var direction = new Vector3i(1, 0, 0);
        rotation.applyRotationTo(direction);
        var filler = p(position.x() + direction.x, 64, position.z() + direction.z);
        check(blocks(world, filler).getFiller(filler.x(), 64, filler.z()) != 0, "reserved footprint survived restart");
        place(world, position, "Empty", RotationTuple.NONE_INDEX);
        check(blocks(world, filler).getFiller(filler.x(), 64, filler.z()) == 0, "restored bench removes cleanly");
        passed.add("native crafting bench, component, orientation and reserved footprint survive restart and remove cleanly");
    }

    private void assetsAndRecipes() throws Exception {
        var category = ItemCategory.getAssetMap().getAsset("LogicGates");
        check(category != null && category.getChildren().length == 3, "creative category loaded with three sections");
        var type = BlockType.getAssetMap().getAsset(BENCH);
        check(type != null && type.getBench() instanceof CraftingBench, "native bench asset loaded");
        var nativeBench = (CraftingBench) type.getBench();
        check("LogicGates_Workbench".equals(nativeBench.getId()) && nativeBench.getCategories().length == 2,
                "native bench has both recipe tabs");
        var ids = new ArrayList<>(List.of("Logic_Cable", "Logic_Lever", "Logic_Lamp", BENCH));
        for (var gate : GateCatalog.gates()) ids.add(gate.itemId());
        // Exercise the exact native transaction called by craftItem, without inventing
        // a network player/session or invoking a second implementation of recipe logic.
        var consume = CraftingManager.class.getDeclaredMethod("removeInputFromInventory", ItemContainer.class, CraftingRecipe.class, int.class);
        consume.setAccessible(true);
        for (String id : ids) {
            var item = Item.getAssetMap().getAsset(id);
            check(item != null && item.getIcon() != null && item.getCategories().length == 1, "item icon/category registered: " + id);
            var recipes = new ArrayList<CraftingRecipe>();
            item.collectRecipesToGenerate(recipes);
            check(recipes.size() == 1, "one direct recipe: " + id);
            var candidate = recipes.getFirst();
            var recipe = CraftingRecipe.getAssetMap().getAsset(candidate.getId());
            check(recipe != null, "native recipe registered: " + id);
            var requirement = recipe.getBenchRequirement();
            check(requirement.length == 1 && (id.equals(BENCH) ? "Fieldcraft" : "LogicGates_Workbench").equals(requirement[0].id), "correct station: " + id);
            var inventory = new SimpleItemContainer((short) 20);
            var materials = CraftingManager.getInputMaterials(recipe);
            for (int i = 0; i < materials.size(); i++) {
                var material = materials.get(i);
                String ingredient = ingredient(material);
                check(Item.getAssetMap().getAsset(ingredient) != null, "real ingredient exists: " + ingredient);
                int count = material.getQuantity() - (i == materials.size() - 1 ? 1 : 0);
                if (count > 0) inventory.addItemStack(new ItemStack(ingredient, count));
            }
            var before = inventory.clone();
            check(!(Boolean) consume.invoke(null, inventory, recipe, 1), "insufficient materials rejected: " + id);
            check(before.equals(inventory), "failed transaction consumes nothing: " + id);
            var last = materials.getLast();
            inventory.addItemStack(new ItemStack(ingredient(last), 1));
            check((Boolean) consume.invoke(null, inventory, recipe, 1), "sufficient ingredients consumed: " + id);
            check(inventory.isEmpty(), "exact material quantities consumed: " + id);
            var output = CraftingManager.getOutputItemStacks(recipe);
            check(output.size() == 1 && output.getFirst().getItemId().equals(id), "native output matches item: " + id);
            check(output.getFirst().getQuantity() == (id.equals("Logic_Cable") ? 8 : 1), "native batch size: " + id);
            check(inventory.addItemStack(output.getFirst()).succeeded(), "native output fits inventory: " + id);
            var doubleBatch = CraftingManager.getOutputItemStacks(recipe, 2);
            check(doubleBatch.stream().mapToInt(ItemStack::getQuantity).sum() == (id.equals("Logic_Cable") ? 16 : 2), "multiple batch outputs: " + id);
        }
        passed.add("eleven native recipes register correct creative categories, station requirements and ingredient identifiers");
        passed.add("native CraftingManager input transactions reject insufficient materials without consumption and consume exact costs");
        passed.add("native CraftingManager outputs produce eight cables or one component and scale correctly for multiple batches");
    }

    private String ingredient(MaterialQuantity material) {
        if (material.getItemId() != null) return material.getItemId();
        String resource = material.getResourceTypeId();
        if ("Wood_Trunk".equals(resource)) return "Wood_Apple_Trunk";
        if ("Rock".equals(resource)) return "Rock_Stone";
        throw new AssertionError("Unexpected resource ingredient: " + resource);
    }
    private boolean canPlace(World world, BlockPosition p, RotationTuple rotation) {
        return BlockOperations.testPlaceBlock(world.getChunkStore().getStore(), blocks(world, p), p.x(), p.y(), p.z(),
                BlockType.getAssetMap().getAsset(BENCH), rotation.index());
    }
    private BlockSection blocks(World world, BlockPosition p) {
        return world.getChunkStore().getStore().getComponent(BlockComponentAccess.sectionIfLoaded(world, p), BlockSection.getComponentType());
    }
    private void place(World world, BlockPosition p, String id, int rotation) {
        var type = BlockType.getAssetMap().getAsset(id);
        check(type != null, "block asset exists: " + id);
        BlockOperations.setBlock(world.getChunkStore(), BlockComponentAccess.sectionIfLoaded(world, p), p.x(), p.y(), p.z(),
                BlockType.getAssetMap().getIndex(id), type, rotation, 0, 0);
    }
    private static BlockPosition p(int x, int y, int z) { return new BlockPosition(x, y, z); }
    private void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
