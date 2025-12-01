package net.sprocketaudio.awesomeshop.content;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.sprocketaudio.awesomeshop.Config;

public class ShopBlock extends Block implements EntityBlock {
    public ShopBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ShopBlockEntity shop)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        var offers = Config.getConfiguredOffers();
        if (offers.isEmpty()) {
            player.displayClientMessage(Component.translatable("block.awesomeshop.shop_block.no_offers"), true);
            return InteractionResult.CONSUME;
        }

        int offerIndex;
        if (player.isShiftKeyDown()) {
            offerIndex = shop.cycleOffer(offers.size());
            ItemStack offer = offers.get(offerIndex);
            player.displayClientMessage(Config.offerSummaryMessage(offer, Config.getCurrencyItem()), true);
            return InteractionResult.CONSUME;
        }

        offerIndex = shop.getOfferIndex(offers.size());
        ItemStack offer = offers.get(offerIndex).copy();
        if (shop.tryPurchase(offer, player)) {
            player.displayClientMessage(Config.purchaseSuccessMessage(offer), true);
        } else {
            player.displayClientMessage(Config.purchaseFailureMessage(offer, Config.getCurrencyItem()), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShopBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            if (level.getBlockEntity(pos) instanceof ShopBlockEntity shop) {
                shop.dropCurrency(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
