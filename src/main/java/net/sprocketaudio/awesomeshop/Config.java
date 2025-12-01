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
            .comment(
                    "Items that can be purchased from the shop block along with their prices and currencies.",
                    "Format: namespace:item|currency=price[,currency=price]", 
                    "Example: minecraft:apple|minecraft:emerald=2,minecraft:gold_ingot=1")
            .defineListAllowEmpty("shopOffers",
                    List.of("minecraft:apple|minecraft:emerald=1", "minecraft:bread|minecraft:gold_ingot=2"), () -> "",
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
        return Component.translatable("block.awesomeshop.shop_block.offer", offer.item().getHoverName(),
                formatPriceList(offer));
    }

    public static Component purchaseSuccessMessage(ItemStack offer, int quantity) {
        return Component.translatable("block.awesomeshop.shop_block.purchase", offer.getHoverName(), quantity);
    }

    public static Component purchaseFailureMessage(ItemStack offer, ConfiguredOffer configuredOffer) {
        return Component.translatable("block.awesomeshop.shop_block.failed", offer.getHoverName(),
                formatPriceList(configuredOffer));
    }

    public static class ConfiguredOffer {
        private final ItemStack item;
        private final List<PriceRequirement> prices;

        public ConfiguredOffer(ItemStack item, List<PriceRequirement> prices) {
            this.item = item;
            this.prices = List.copyOf(prices);
        }

        public ItemStack item() {
            return item;
        }

        public List<PriceRequirement> prices() {
            return prices;
        }
    }

    public record PriceRequirement(ConfiguredCurrency currency, int price) {
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
        if (!(obj instanceof String raw)) {
            return false;
        }

        String[] parts = raw.split("\\|", 2);
        if (parts.length != 2) {
            return false;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(parts[0]);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return false;
        }

        return parsePriceRequirements(parts[1]).stream().allMatch(req -> BuiltInRegistries.ITEM.containsKey(req.id()));
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
        String[] parts = raw.split("\\|", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        ResourceLocation itemId = ResourceLocation.tryParse(parts[0]);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return Optional.empty();
        }

        List<PriceRequirement> requirements = parsePriceRequirements(parts[1]).stream()
                .map(req -> buildRequirement(req, currencyLookup))
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        if (requirements.isEmpty()) {
            return Optional.empty();
        }

        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
        return Optional.of(new ConfiguredOffer(stack, requirements));
    }

    private static Optional<PriceRequirement> buildRequirement(RawRequirement req,
            Map<ResourceLocation, ConfiguredCurrency> currencyLookup) {
        ConfiguredCurrency currency = currencyLookup.get(req.id());
        if (currency == null || req.price() <= 0) {
            return Optional.empty();
        }
        return Optional.of(new PriceRequirement(currency, req.price()));
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

    private record RawRequirement(ResourceLocation id, int price) {
    }

    private static List<RawRequirement> parsePriceRequirements(String raw) {
        List<RawRequirement> requirements = new ArrayList<>();
        String[] entries = raw.split(",");
        for (String entry : entries) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            ResourceLocation currencyId = ResourceLocation.tryParse(parts[0]);
            if (currencyId == null || !BuiltInRegistries.ITEM.containsKey(currencyId)) {
                continue;
            }
            try {
                int price = Integer.parseInt(parts[1]);
                if (price > 0) {
                    requirements.add(new RawRequirement(currencyId, price));
                }
            } catch (NumberFormatException ex) {
                AwesomeShop.LOGGER.warn("Invalid price for shop offer '{}': {}", raw, ex.getMessage());
            }
        }
        return requirements;
    }

    private static Component formatPriceList(ConfiguredOffer offer) {
        return Component.literal(offer.prices().stream()
                .map(price -> Component.translatable("block.awesomeshop.shop_block.price_entry", price.price(),
                        Component.translatable(price.currency().item().getDescriptionId())).getString())
                .collect(Collectors.joining(", ")));
    }
}
