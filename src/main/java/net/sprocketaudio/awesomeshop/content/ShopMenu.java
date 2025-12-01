package net.sprocketaudio.awesomeshop.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.sprocketaudio.awesomeshop.AwesomeShop;
import net.sprocketaudio.awesomeshop.Config;

public class ShopMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ShopBlockEntity shop;
    private final List<ItemStack> offers;
    private final Item currencyItem;

    private int currencyCount;

    public ShopMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, readShopFromClient(inventory.player.level(), data), readCurrency(data), readOffers(data));
    }

    public ShopMenu(int id, Inventory inventory, ShopBlockEntity shop, List<ItemStack> offers) {
        this(id, inventory, shop, getCurrencyItem(shop), offers);
    }

    private ShopMenu(int id, Inventory inventory, ShopBlockEntity shop, Item currencyItem, List<ItemStack> offers) {
        super(AwesomeShop.SHOP_MENU.get(), id);
        this.shop = shop;
        this.offers = List.copyOf(offers);
        this.currencyItem = currencyItem;
        this.access = shop == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(shop.getLevel(), shop.getBlockPos());

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return shop != null ? shop.getCurrencyCount() : 0;
            }

            @Override
            public void set(int value) {
                currencyCount = value;
            }
        });
    }

    private static ShopBlockEntity readShopFromClient(Level level, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        return level.getBlockEntity(pos) instanceof ShopBlockEntity be ? be : null;
    }

    private static Item readCurrency(RegistryFriendlyByteBuf data) {
        return BuiltInRegistries.ITEM.getOptional(data.readResourceLocation()).orElse(Config.getCurrencyItem());
    }

    private static List<ItemStack> readOffers(RegistryFriendlyByteBuf data) {
        return data.readList(buf -> ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf));
    }

    private static Item getCurrencyItem(ShopBlockEntity shop) {
        if (shop != null) {
            return BuiltInRegistries.ITEM.getOptional(shop.getCurrencyId()).orElse(Config.getCurrencyItem());
        }
        return Config.getCurrencyItem();
    }

    public List<ItemStack> getOffers() {
        return offers;
    }

    public Item getCurrencyItem() {
        return currencyItem;
    }

    public int getCurrencyCount() {
        if (shop != null && shop.getLevel() != null && !shop.getLevel().isClientSide) {
            return shop.getCurrencyCount();
        }
        return currencyCount;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (shop == null || id < 0 || id >= offers.size()) {
            return false;
        }

        ItemStack offer = offers.get(id);
        if (offer.isEmpty()) {
            return false;
        }

        ItemStack delivery = offer.copy();
        boolean success = shop.tryPurchase(delivery, player);
        if (success) {
            player.displayClientMessage(Config.purchaseSuccessMessage(offer), true);
        } else {
            player.displayClientMessage(Config.purchaseFailureMessage(offer, shop.getCurrencyItem()), true);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, AwesomeShop.SHOP_BLOCK.get());
    }

    public static void writeScreenData(ShopBlockEntity shop, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(Objects.requireNonNull(shop).getBlockPos());
        buffer.writeResourceLocation(shop.getCurrencyId());
        buffer.writeCollection(new ArrayList<>(Config.getConfiguredOffers()),
                (buf, stack) -> ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack));
    }
}
