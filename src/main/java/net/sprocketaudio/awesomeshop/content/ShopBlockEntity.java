package net.sprocketaudio.awesomeshop.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sprocketaudio.awesomeshop.AwesomeShop;
import net.sprocketaudio.awesomeshop.Config;

import net.minecraft.network.chat.Component;

public class ShopBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private static final String CURRENCY_COUNT_TAG = "CurrencyCount";
    private static final int[] SLOTS = new int[] { 0 };

    private int currencyCount = 0;

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(AwesomeShop.SHOP_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean tryPurchase(ItemStack offer, Player player) {
        if (level == null || level.isClientSide) {
            return false;
        }

        Item currency = Config.getCurrencyItem();
        if (currencyCount <= 0) {
            return false;
        }

        currencyCount -= 1;
        if (!player.addItem(offer)) {
            player.drop(offer, false);
        }
        setChanged();
        return true;
    }

    public void dropCurrency(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(1);
        Item currency = Config.getCurrencyItem();
        int remaining = currencyCount;
        while (remaining > 0) {
            int dropAmount = Math.min(remaining, currency.getDefaultMaxStackSize());
            container.setItem(0, new ItemStack(currency, dropAmount));
            Containers.dropContents(level, pos, container);
            remaining -= dropAmount;
        }
        currencyCount = 0;
    }

    public ResourceLocation getCurrencyId() {
        return Config.getCurrencyLocation();
    }

    public Item getCurrencyItem() {
        return Config.getCurrencyItem();
    }

    public int getCurrencyCount() {
        return currencyCount;
    }

    private void addCurrency(int count) {
        if (count > 0) {
            currencyCount += count;
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt(CURRENCY_COUNT_TAG, currencyCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        currencyCount = tag.getInt(CURRENCY_COUNT_TAG);
    }

    // WorldlyContainer implementation
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == 0 && stack.is(Config.getCurrencyItem());
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return currencyCount <= 0;
    }

    @Override
    public ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0 && stack.is(Config.getCurrencyItem())) {
            addCurrency(stack.getCount());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr((double) this.worldPosition.getX() + 0.5D, (double) this.worldPosition.getY() + 0.5D,
                (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        currencyCount = 0;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index == 0 && stack.is(Config.getCurrencyItem());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.awesomeshop.shop_block");
    }

    @Override
    public ShopMenu createMenu(int id, Inventory inventory, Player player) {
        return new ShopMenu(id, inventory, this, Config.getConfiguredOffers());
    }
}
