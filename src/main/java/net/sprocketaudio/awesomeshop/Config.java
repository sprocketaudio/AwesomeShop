package net.sprocketaudio.awesomeshop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.sprocketaudio.awesomeshop.AwesomeShop;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CURRENCIES = BUILDER
            .comment("Items that can be used as currency for shop transactions.",
                    "Format: namespace:item", "Example: minecraft:emerald")
            .defineListAllowEmpty("currencies", List.of("minecraft:emerald", "minecraft:gold_ingot"), () -> "",
                    Config::validateItemName);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SHOP_OFFERS = BUILDER
            .comment("Items that can be purchased from the shop block along with their prices and currencies.",
                    "Format: namespace:item|price|currency", "Example: minecraft:apple|2|minecraft:emerald")
            .defineListAllowEmpty("shopOffers",
                    List.of("minecraft:apple|1|minecraft:emerald", "minecraft:bread|2|minecraft:gold_ingot"), () -> "",
                    Config::validateOffer);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof String itemName)) {
            return false;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(itemName);
        return parsed != null && BuiltInRegistries.ITEM.containsKey(parsed);
    }

    public static List<ConfiguredCurrency> getConfiguredCurrencies() {
        ArrayList<ConfiguredCurrency> currencies = CURRENCIES.get().stream()
                .map(Config::parseCurrency)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        if (currencies.isEmpty()) {
            currencies.add(new ConfiguredCurrency(ResourceLocation.parse("minecraft:emerald"), Items.EMERALD));
        }
        return currencies;
    }

    public static List<ConfiguredOffer> getConfiguredOffers() {
        List<ConfiguredCurrency> currencies = getConfiguredCurrencies();
        Map<ResourceLocation, ConfiguredCurrency> currencyLookup = buildCurrencyLookup(currencies);
        return SHOP_OFFERS.get().stream()
                .map(raw -> parseOffer(raw, currencyLookup))
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static Component offerSummaryMessage(ConfiguredOffer offer) {
        return Component.translatable("block.awesomeshop.shop_block.offer", offer.item().getHoverName(), offer.price(),
                Component.translatable(offer.currency().item().getDescriptionId()));
    }

    public static Component purchaseSuccessMessage(ItemStack offer, int quantity) {
        return Component.translatable("block.awesomeshop.shop_block.purchase", offer.getHoverName(), quantity);
    }

    public static Component purchaseFailureMessage(ItemStack offer, Item currency, int totalCost) {
        return Component.translatable("block.awesomeshop.shop_block.failed", offer.getHoverName(),
                Component.translatable(currency.getDescriptionId()), totalCost);
    }

    public static class ConfiguredOffer {
        private final ItemStack item;
        private final int price;
        private final ConfiguredCurrency currency;

        public ConfiguredOffer(ItemStack item, int price, ConfiguredCurrency currency) {
            this.item = item;
            this.price = price;
            this.currency = currency;
        }

        public ItemStack item() {
            return item;
        }

        public int price() {
            return price;
        }

        public ConfiguredCurrency currency() {
            return currency;
        }
    }

    public static class ConfiguredCurrency {
        private final ResourceLocation id;
        private final Item item;

        public ConfiguredCurrency(ResourceLocation id, Item item) {
            this.id = id;
            this.item = item;
        }

        public ResourceLocation id() {
            return id;
        }

        public Item item() {
            return item;
        }
    }

    private static boolean validateOffer(final Object obj) {
        return obj instanceof String s && parseOffer(s, buildCurrencyLookup(getConfiguredCurrencies())).isPresent();
    }

    private static Optional<ConfiguredCurrency> parseCurrency(String currencyId) {
        ResourceLocation id = ResourceLocation.tryParse(currencyId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Optional.empty();
        }

        Item currencyItem = BuiltInRegistries.ITEM.get(id);
        return Optional.of(new ConfiguredCurrency(id, currencyItem));
    }

    private static Optional<ConfiguredOffer> parseOffer(final String raw, Map<ResourceLocation, ConfiguredCurrency> currencyLookup) {
        String[] parts = raw.split("\\|", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }

        ResourceLocation itemId = ResourceLocation.tryParse(parts[0]);
        ResourceLocation currencyId = ResourceLocation.tryParse(parts[2]);
        if (itemId == null || currencyId == null || !BuiltInRegistries.ITEM.containsKey(itemId)
                || !BuiltInRegistries.ITEM.containsKey(currencyId)) {
            return Optional.empty();
        }

        try {
            int price = Integer.parseInt(parts[1]);
            if (price <= 0) {
                return Optional.empty();
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            ConfiguredCurrency currency = currencyLookup.get(currencyId);
            if (currency == null) {
                return Optional.empty();
            }
            return Optional.of(new ConfiguredOffer(stack, price, currency));
        } catch (NumberFormatException ex) {
            AwesomeShop.LOGGER.warn("Invalid price for shop offer '{}': {}", raw, ex.getMessage());
            return Optional.empty();
        }
    }

    private static Map<ResourceLocation, ConfiguredCurrency> buildCurrencyLookup(List<ConfiguredCurrency> currencies) {
        Map<ResourceLocation, ConfiguredCurrency> lookup = new LinkedHashMap<>();
        for (ConfiguredCurrency currency : currencies) {
            lookup.put(currency.id(), currency);
        }
        if (lookup.isEmpty()) {
            ConfiguredCurrency fallback = new ConfiguredCurrency(ResourceLocation.parse("minecraft:emerald"), Items.EMERALD);
            lookup.put(fallback.id(), fallback);
        }
        return lookup;
    }
}
