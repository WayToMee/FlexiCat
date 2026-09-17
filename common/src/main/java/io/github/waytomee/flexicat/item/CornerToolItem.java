package io.github.waytomee.flexicat.item;

import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.edit.ShapeFeedback;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.platform.LoaderHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * The tool that edits corner points.
 *
 * <ul>
 *   <li>Right-click a FlexiCat block: toggle corner editing for that block on the
 *       client (handles appear; see {@code CornerEditClient}). The server does
 *       nothing for a plain click — moves arrive later as separate intents.</li>
 *   <li>Sneak + right-click: reset the block to a cube (server side, synced).</li>
 *   <li>Copy / paste keys (stage 7): the tool remembers one shape in its
 *       {@link FlexiCatComponents#SHAPE} component and can stamp it onto other blocks;
 *       the material is left alone.</li>
 * </ul>
 */
public class CornerToolItem extends Item {

    public CornerToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Optional<FlexiCatBlockEntity> target = FlexiCatBlock.entityAt(level, pos);
        if (target.isEmpty()) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        boolean reset = player != null && player.isSecondaryUseActive();
        if (level.isClientSide()) {
            if (!reset) {
                LoaderHooks.cornerToolUsedOnClient(pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (reset) {
            boolean changed = target.get().setShape(CornerShape.cube());
            if (changed && level instanceof ServerLevel serverLevel) {
                ShapeFeedback.shapeChanged(serverLevel, pos, target.get());
            }
            player.displayClientMessage(Component.translatable(changed
                    ? "message.flexicat.shape_reset" : "message.flexicat.shape_already_cube"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CornerShape shape = stack.get(FlexiCatComponents.SHAPE);
        if (shape == null) {
            tooltip.add(Component.translatable("item.flexicat.corner_tool.empty").withStyle(ChatFormatting.GRAY));
        } else if (shape.isCube()) {
            tooltip.add(Component.translatable("item.flexicat.corner_tool.holds_cube").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.flexicat.corner_tool.holds", shape.movedCorners())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** {@code true} if the player holds a corner tool in either hand. */
    public static boolean isHeldBy(Player player) {
        return !heldBy(player).isEmpty();
    }

    /** The corner tool in the player's main hand, else off hand, else {@link ItemStack#EMPTY}. */
    public static ItemStack heldBy(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof CornerToolItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof CornerToolItem ? off : ItemStack.EMPTY;
    }
}