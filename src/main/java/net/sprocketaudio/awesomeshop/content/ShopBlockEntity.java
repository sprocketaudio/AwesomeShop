package net.sprocketaudio.awesomeshop.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.sprocketaudio.awesomeshop.AwesomeShop;
import net.sprocketaudio.awesomeshop.Config;

public class ShopBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final String SLOT_TAG = "Currency";
    private static final String OFFER_TAG = "OfferIndex";
    private static final int[] SLOTS = new int[] { 0 };

    private final ItemStackHandler currencySlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Config.getCurrencyItem());
        }
    };

    private int offerIndex = 0;

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(AwesomeShop.SHOP_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean tryPurchase(ItemStack offer, Player player) {
        if (level == null || level.isClientSide) {
            return false;
        }

        Item currency = Config.getCurrencyItem();
        ItemStack stored = currencySlot.getStackInSlot(0);
        if (!stored.is(currency) || stored.isEmpty()) {
            return false;
        }

        currencySlot.extractItem(0, 1, false);
        if (!player.addItem(offer)) {
            player.drop(offer, false);
        }
        setChanged();
        return true;
    }

    public int cycleOffer(int offerCount) {
        if (offerCount <= 0) {
            offerIndex = 0;
            return offerIndex;
        }
        offerIndex = (offerIndex + 1) % offerCount;
        setChanged();
        return offerIndex;
    }

    public int getOfferIndex(int offerCount) {
        if (offerCount <= 0) {
            offerIndex = 0;
        } else if (offerIndex >= offerCount) {
            offerIndex = 0;
        }
        return offerIndex;
    }

    public void dropCurrency(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(currencySlot.getSlots());
        for (int i = 0; i < currencySlot.getSlots(); i++) {
            container.setItem(i, currencySlot.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, container);
    }

    public ResourceLocation getCurrencyId() {
        return Config.getCurrencyLocation();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(SLOT_TAG, currencySlot.serializeNBT(provider));
        tag.putInt(OFFER_TAG, offerIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains(SLOT_TAG)) {
            currencySlot.deserializeNBT(provider, tag.getCompound(SLOT_TAG));
        }
        offerIndex = tag.getInt(OFFER_TAG);
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
        return index == 0;
    }

    @Override
    public int getContainerSize() {
        return currencySlot.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < currencySlot.getSlots(); i++) {
            if (!currencySlot.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return currencySlot.getStackInSlot(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack result = currencySlot.extractItem(index, count, false);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = currencySlot.getStackInSlot(index);
        currencySlot.setStackInSlot(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0 && stack.is(Config.getCurrencyItem())) {
            currencySlot.setStackInSlot(index, stack);
            setChanged();
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
        for (int i = 0; i < currencySlot.getSlots(); i++) {
            currencySlot.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return index == 0 && stack.is(Config.getCurrencyItem());
    }
}
