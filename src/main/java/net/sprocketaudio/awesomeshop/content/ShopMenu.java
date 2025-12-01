package net.sprocketaudio.awesomeshop.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.sprocketaudio.awesomeshop.AwesomeShop;
import net.sprocketaudio.awesomeshop.Config;
import net.sprocketaudio.awesomeshop.Config.ConfiguredCurrency;
import net.sprocketaudio.awesomeshop.Config.ConfiguredOffer;
import net.sprocketaudio.awesomeshop.Config.PriceRequirement;

public class ShopMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ShopBlockEntity shop;
    private final List<ConfiguredOffer> offers;
    private final List<ConfiguredCurrency> currencies;
    private final Map<ResourceLocation, Integer> currencyIndex;
    private final int[] currencyCounts;

    private final IntSupplier offerCountLookup;

    public ShopMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, decodeData(inventory.player.level(), data));
    }

    public ShopMenu(int id, Inventory inventory, ShopBlockEntity shop, List<ConfiguredOffer> offers) {
        this(id, inventory, shop, Config.getConfiguredCurrencies(), offers);
    }

    private ShopMenu(int id, Inventory inventory, MenuData data) {
        this(id, inventory, data.shop(), data.currencies(), data.offers());
    }

    private ShopMenu(int id, Inventory inventory, ShopBlockEntity shop, List<ConfiguredCurrency> currencies,
            List<ConfiguredOffer> offers) {
        super(AwesomeShop.SHOP_MENU.get(), id);
        this.shop = shop;
        this.offers = List.copyOf(offers);
        this.currencies = List.copyOf(currencies);
        this.currencyIndex = createCurrencyIndex(this.currencies);
        this.currencyCounts = new int[this.currencies.size()];
        this.access = shop == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(shop.getLevel(), shop.getBlockPos());
        this.offerCountLookup = () -> this.offers.size();

        for (int i = 0; i < this.currencies.size(); i++) {
            final int slotIndex = i;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    if (ShopMenu.this.shop != null) {
                        return ShopMenu.this.shop.getCurrencyCount(ShopMenu.this.currencies.get(slotIndex));
                    }
                    return 0;
                }

                @Override
                public void set(int value) {
                    if (slotIndex < currencyCounts.length) {
                        currencyCounts[slotIndex] = value;
                    }
                }
            });
        }
    }

    private static MenuData decodeData(Level level, RegistryFriendlyByteBuf data) {
        ShopBlockEntity shop = readShopFromClient(level, data);
        List<ConfiguredCurrency> currencies = readCurrencies(data);
        Map<ResourceLocation, ConfiguredCurrency> currencyLookup = buildCurrencyLookup(currencies);
        List<ConfiguredOffer> offers = readOffers(data, currencyLookup);
        return new MenuData(shop, currencies, offers);
    }

    private static ShopBlockEntity readShopFromClient(Level level, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        return level.getBlockEntity(pos) instanceof ShopBlockEntity be ? be : null;
    }

    private static List<ConfiguredCurrency> readCurrencies(RegistryFriendlyByteBuf data) {
        return data.readList(buf -> {
            ResourceLocation id = buf.readResourceLocation();
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.EMERALD);
            return new ConfiguredCurrency(id, item);
        });
    }

    private static Map<ResourceLocation, ConfiguredCurrency> buildCurrencyLookup(List<ConfiguredCurrency> currencies) {
        Map<ResourceLocation, ConfiguredCurrency> lookup = new HashMap<>();
        for (ConfiguredCurrency currency : currencies) {
            lookup.put(currency.id(), currency);
        }
        if (lookup.isEmpty()) {
            ConfiguredCurrency fallback = new ConfiguredCurrency(ResourceLocation.parse("minecraft:emerald"), Items.EMERALD);
            lookup.put(fallback.id(), fallback);
        }
        return lookup;
    }

    private static List<ConfiguredOffer> readOffers(RegistryFriendlyByteBuf data,
            Map<ResourceLocation, ConfiguredCurrency> currencyLookup) {
        return data.readList(buf -> {
            ItemStack stack = ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
            int requirementCount = ((RegistryFriendlyByteBuf) buf).readVarInt();
            List<PriceRequirement> prices = new ArrayList<>();
            for (int i = 0; i < requirementCount; i++) {
                ResourceLocation currencyId = ((RegistryFriendlyByteBuf) buf).readResourceLocation();
                int price = ((RegistryFriendlyByteBuf) buf).readVarInt();
                ConfiguredCurrency currency = currencyLookup.get(currencyId);
                if (currency != null && price > 0) {
                    prices.add(new PriceRequirement(currency, price));
                }
            }

            if (prices.isEmpty()) {
                ConfiguredCurrency fallback = currencyLookup.values().stream().findFirst()
                        .orElse(new ConfiguredCurrency(ResourceLocation.parse("minecraft:emerald"), Items.EMERALD));
                prices.add(new PriceRequirement(fallback, 1));
            }

            return new ConfiguredOffer(stack, prices);
        });
    }

    private static Map<ResourceLocation, Integer> createCurrencyIndex(List<ConfiguredCurrency> currencies) {
        Map<ResourceLocation, Integer> lookup = new HashMap<>();
        for (int i = 0; i < currencies.size(); i++) {
            lookup.put(currencies.get(i).id(), i);
        }
        return lookup;
    }

    private record MenuData(ShopBlockEntity shop, List<ConfiguredCurrency> currencies, List<ConfiguredOffer> offers) {
    }

    public List<ConfiguredOffer> getOffers() {
        return offers;
    }

    public List<ConfiguredCurrency> getCurrencies() {
        return currencies;
    }

    public int getCurrencyCount(ConfiguredCurrency currency) {
        Integer index = currencyIndex.get(currency.id());
        if (index == null) {
            return 0;
        }
        if (shop != null && shop.getLevel() != null && !shop.getLevel().isClientSide) {
            return shop.getCurrencyCount(currency);
        }
        if (index >= 0 && index < currencyCounts.length) {
            return currencyCounts[index];
        }
        return 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int offerSize = offerCountLookup.getAsInt();
        if (shop == null || offerSize <= 0 || id < 0) {
            return false;
        }

        int quantity = id / offerSize;
        int offerIndex = id % offerSize;
        if (offerIndex >= offers.size() || quantity <= 0) {
            return false;
        }

        ConfiguredOffer offer = offers.get(offerIndex);
        if (offer.item().isEmpty()) {
            return false;
        }

        boolean success = shop.tryPurchase(offer, quantity, player);
        if (success) {
            player.displayClientMessage(Config.purchaseSuccessMessage(offer.item(), quantity), true);
        } else {
            player.displayClientMessage(Config.purchaseFailureMessage(offer.item(), offer), true);
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
        List<ConfiguredCurrency> currencies = Config.getConfiguredCurrencies();
        buffer.writeCollection(currencies, (buf, currency) -> buf.writeResourceLocation(currency.id()));
        buffer.writeCollection(new ArrayList<>(Config.getConfiguredOffers()), (buf, offer) -> {
            ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, offer.item());
            ((RegistryFriendlyByteBuf) buf).writeVarInt(offer.prices().size());
            offer.prices().forEach(price -> {
                ((RegistryFriendlyByteBuf) buf).writeResourceLocation(price.currency().id());
                ((RegistryFriendlyByteBuf) buf).writeVarInt(price.price());
            });
        });
    }
}
