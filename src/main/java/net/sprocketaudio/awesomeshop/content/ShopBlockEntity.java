package net.sprocketaudio.awesomeshop.content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sprocketaudio.awesomeshop.AwesomeShop;
import net.sprocketaudio.awesomeshop.Config;
import net.sprocketaudio.awesomeshop.Config.ConfiguredCurrency;
import net.sprocketaudio.awesomeshop.Config.ConfiguredOffer;

import net.minecraft.network.chat.Component;

public class ShopBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private static final String CURRENCIES_TAG = "Currencies";
    private static final String CURRENCY_ID_TAG = "Id";
    private static final String CURRENCY_COUNT_TAG = "Count";

    private final Map<ResourceLocation, Integer> currencyCounts = new HashMap<>();

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(AwesomeShop.SHOP_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean tryPurchase(ConfiguredOffer offer, int quantity, Player player) {
        if (level == null || level.isClientSide || quantity <= 0) {
            return false;
        }

        Map<ResourceLocation, Integer> requiredTotals = new HashMap<>();
        for (Config.PriceRequirement requirement : offer.prices()) {
            int requiredTotal = requirement.price() * quantity;
            if (requiredTotal <= 0) {
                return false;
            }
            requiredTotals.merge(requirement.currency().id(), requiredTotal, Integer::sum);
        }

        Map<ResourceLocation, Integer> updatedCounts = new HashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : requiredTotals.entrySet()) {
            int available = getCurrencyCount(entry.getKey());
            int requiredTotal = entry.getValue();
            if (available < requiredTotal) {
                return false;
            }
            updatedCounts.put(entry.getKey(), available - requiredTotal);
        }

        updatedCounts.forEach(this::setCurrencyCount);
        ItemStack delivery = offer.item().copy();
        delivery.setCount(offer.item().getCount() * quantity);
        boolean fullyAdded = player.addItem(delivery);
        if (!fullyAdded && !delivery.isEmpty()) {
            player.drop(delivery, false);
        }
        setChanged();
        return true;
    }

    public void dropCurrency(Level level, BlockPos pos) {
        SimpleContainer container = new SimpleContainer(1);
        for (Map.Entry<ResourceLocation, Integer> entry : currencyCounts.entrySet()) {
            Item currency = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(Items.EMERALD);
            int remaining = entry.getValue();
            while (remaining > 0) {
                int dropAmount = Math.min(remaining, currency.getDefaultMaxStackSize());
                container.setItem(0, new ItemStack(currency, dropAmount));
                Containers.dropContents(level, pos, container);
                remaining -= dropAmount;
            }
        }
        currencyCounts.clear();
    }

    public int getCurrencyCount(ConfiguredCurrency currency) {
        return getCurrencyCount(currency.id());
    }

    public int getCurrencyCount(ResourceLocation id) {
        return currencyCounts.getOrDefault(id, 0);
    }

    private void addCurrency(ConfiguredCurrency currency, int count) {
        if (count > 0 && currency != null) {
            setCurrencyCount(currency.id(), getCurrencyCount(currency) + count);
        }
    }

    private void setCurrencyCount(ResourceLocation id, int count) {
        currencyCounts.put(id, Math.max(0, count));
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ListTag currencyList = new ListTag();
        for (Map.Entry<ResourceLocation, Integer> entry : currencyCounts.entrySet()) {
            CompoundTag currencyTag = new CompoundTag();
            currencyTag.putString(CURRENCY_ID_TAG, entry.getKey().toString());
            currencyTag.putInt(CURRENCY_COUNT_TAG, entry.getValue());
            currencyList.add(currencyTag);
        }
        tag.put(CURRENCIES_TAG, currencyList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        currencyCounts.clear();
        if (tag.contains(CURRENCIES_TAG, Tag.TAG_LIST)) {
            ListTag list = tag.getList(CURRENCIES_TAG, Tag.TAG_COMPOUND);
            list.forEach(entry -> {
                if (entry instanceof CompoundTag currencyTag) {
                    ResourceLocation id = ResourceLocation.tryParse(currencyTag.getString(CURRENCY_ID_TAG));
                    int count = currencyTag.getInt(CURRENCY_COUNT_TAG);
                    if (id != null) {
                        currencyCounts.put(id, count);
                    }
                }
            });
        } else if (tag.contains(CURRENCY_COUNT_TAG)) {
            int count = tag.getInt(CURRENCY_COUNT_TAG);
            Config.getConfiguredCurrencies().stream().findFirst()
                    .ifPresent(currency -> currencyCounts.put(currency.id(), count));
        }
    }

    // WorldlyContainer implementation
    @Override
    public int[] getSlotsForFace(Direction side) {
        int size = Math.max(1, Config.getConfiguredCurrencies().size());
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) {
        List<ConfiguredCurrency> currencies = Config.getConfiguredCurrencies();
        if (index < 0 || index >= currencies.size()) {
            return false;
        }
        return stack.is(currencies.get(index).item());
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return Math.max(1, Config.getConfiguredCurrencies().size());
    }

    @Override
    public boolean isEmpty() {
        return currencyCounts.values().stream().allMatch(count -> count <= 0);
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
        List<ConfiguredCurrency> currencies = Config.getConfiguredCurrencies();
        if (index >= 0 && index < currencies.size()) {
            ConfiguredCurrency currency = currencies.get(index);
            if (stack.is(currency.item())) {
                addCurrency(currency, stack.getCount());
            }
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
        currencyCounts.clear();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        List<ConfiguredCurrency> currencies = Config.getConfiguredCurrencies();
        if (index < 0 || index >= currencies.size()) {
            return false;
        }
        return stack.is(currencies.get(index).item());
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
