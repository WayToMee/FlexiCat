package io.github.waytomee.flexicat.neoforge;

import io.github.waytomee.flexicat.FlexiCat;
import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.item.CornerToolItem;
import io.github.waytomee.flexicat.item.FlexiCatComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * NeoForge registration glue. Registry names are part of the save format:
 * {@code flexicat:flexicat_block} (block, block item, block entity type) and
 * {@code flexicat:corner_tool}. The objects themselves live in {@code common}.
 */
public final class FlexiCatRegistration {

    public static final String BLOCK_NAME = "flexicat_block";
    public static final String TOOL_NAME = "corner_tool";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FlexiCat.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FlexiCat.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FlexiCat.MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, FlexiCat.MOD_ID);

    /** The corner tool's copied shape (stage 7); the type object itself lives in common. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CornerShape>> SHAPE_COMPONENT =
            DATA_COMPONENTS.register(FlexiCatComponents.SHAPE_NAME, () -> FlexiCatComponents.SHAPE);

    public static final DeferredBlock<FlexiCatBlock> FLEXICAT_BLOCK = BLOCKS.registerBlock(BLOCK_NAME,
            properties -> new FlexiCatBlock(properties, () -> FlexiCatRegistration.FLEXICAT_BLOCK_ENTITY.get()),
            FlexiCatBlock.defaultProperties());

    public static final DeferredItem<BlockItem> FLEXICAT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(FLEXICAT_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlexiCatBlockEntity>> FLEXICAT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(BLOCK_NAME, () -> BlockEntityType.Builder
                    .<FlexiCatBlockEntity>of((pos, state) -> new NeoForgeFlexiCatBlockEntity(FlexiCatRegistration.FLEXICAT_BLOCK_ENTITY.get(), pos, state),
                            FLEXICAT_BLOCK.get())
                    .build(null));

    public static final DeferredItem<CornerToolItem> CORNER_TOOL = ITEMS.registerItem(TOOL_NAME,
            CornerToolItem::new, new Item.Properties().stacksTo(1));

    private FlexiCatRegistration() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        DATA_COMPONENTS.register(modBus);
        modBus.addListener(FlexiCatRegistration::addCreativeTabEntries);
    }

    private static void addCreativeTabEntries(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(FLEXICAT_BLOCK_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CORNER_TOOL);
        }
    }
}