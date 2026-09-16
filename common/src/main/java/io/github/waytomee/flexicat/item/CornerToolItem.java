package io.github.waytomee.flexicat.item;

import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.geometry.Corner;
import io.github.waytomee.flexicat.geometry.CornerShape;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * The tool that will edit corner points.
 *
 * <p>Stage 2 placeholder behaviour, so the block entity and its sync can be
 * exercised in a dev world before the real interaction exists:
 * <ul>
 *   <li>right-click a FlexiCat block: show how many corners are moved (action bar);</li>
 *   <li>sneak + right-click: reset the block to a cube (server-side, synced).</li>
 * </ul>
 * Stage 3 replaces this with handle selection and move intents.
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
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        FlexiCatBlockEntity be = target.get();
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()) {
            boolean changed = be.setShape(CornerShape.cube());
            player.displayClientMessage(Component.translatable(changed
                    ? "message.flexicat.shape_reset" : "message.flexicat.shape_already_cube"), true);
        } else if (player != null) {
            player.displayClientMessage(describe(be.shape()), true);
        }
        return InteractionResult.CONSUME;
    }

    static Component describe(CornerShape shape) {
        if (shape.isCube()) {
            return Component.translatable("message.flexicat.shape_cube");
        }
        int moved = 0;
        for (Corner corner : Corner.values()) {
            if (!shape.offset(corner).isZero()) {
                moved++;
            }
        }
        return Component.translatable("message.flexicat.shape_moved", moved);
    }
}