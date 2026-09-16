package io.github.waytomee.flexicat.item;

import io.github.waytomee.flexicat.block.FlexiCatBlock;
import io.github.waytomee.flexicat.block.FlexiCatBlockEntity;
import io.github.waytomee.flexicat.geometry.CornerShape;
import io.github.waytomee.flexicat.platform.LoaderHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * The tool that edits corner points.
 *
 * <ul>
 *   <li>Right-click a FlexiCat block: toggle corner editing for that block on the
 *       client (handles appear; see {@code CornerEditClient}). The server does
 *       nothing for a plain click — moves arrive later as separate intents.</li>
 *   <li>Sneak + right-click: reset the block to a cube (server side, synced).</li>
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
            player.displayClientMessage(Component.translatable(changed
                    ? "message.flexicat.shape_reset" : "message.flexicat.shape_already_cube"), true);
        }
        return InteractionResult.CONSUME;
    }

    /** {@code true} if the player holds a corner tool in either hand. */
    public static boolean isHeldBy(Player player) {
        return player.getMainHandItem().getItem() instanceof CornerToolItem
                || player.getOffhandItem().getItem() instanceof CornerToolItem;
    }
}