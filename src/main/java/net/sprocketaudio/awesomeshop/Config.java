package net.sprocketaudio.awesomeshop;

import java.util.ArrayList;
import java.util.Collections;
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

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CATEGORIES = BUILDER
            .comment("Categories that will be shown in the shop GUI.")
            .defineListAllowEmpty("categories", List.of("cat1", "cat2", "cat3"), () -> "",
                    Config::validateCategoryName);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> CURRENCIES = BUILDER
            .comment("Items that can be used as currency for shop transactions.",
                    "Format: namespace:item", "Example: minecraft:emerald")
            .defineListAllowEmpty("currencies", List.of("minecraft:emerald", "minecraft:gold_ingot"), () -> "",
                    Config::validateItemName);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SHOP_OFFERS = BUILDER
            .comment(
                    "Items that can be purchased from the shop block along with their prices, currencies, and categories.",
                    "Format: namespace:item|category|currency=price[,currency=price]",
                    "Example: minecraft:apple|produce|minecraft:emerald=2,minecraft:gold_ingot=1")
            .defineListAllowEmpty("shopOffers",
                    List.of("minecraft:apple|cat1|minecraft:emerald=1", "minecraft:bread|cat1|minecraft:gold_ingot=2"),
                    () -> "",
                    Config::validateOffer);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateCategoryName(final Object obj) {
        if (!(obj instanceof String category)) {
            return false;
        }
        return !category.trim().isEmpty();
    }

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

    public static List<String> getConfiguredCategories() {
        List<String> categories = CATEGORIES.get().stream()
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));

        if (categories.isEmpty()) {
            categories.add("default");
        }
        return categories;
    }

    public static List<ConfiguredOffer> getConfiguredOffers() {
        List<String> categories = getConfiguredCategories();
        List<ConfiguredCurrency> currencies = getConfiguredCurrencies();
        Map<ResourceLocation, ConfiguredCurrency> currencyLookup = buildCurrencyLookup(currencies);
        return SHOP_OFFERS.get().stream()
                .map(raw -> parseOffer(raw, categories, currencyLookup))
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

    public static Map<ConfiguredCurrency, Integer> aggregatePriceRequirements(List<PriceRequirement> prices) {
        Map<ConfiguredCurrency, Integer> totals = new LinkedHashMap<>();
        for (PriceRequirement price : prices) {
            if (price.price() <= 0) {
                continue;
            }
            totals.merge(price.currency(), price.price(), Integer::sum);
        }
        return Collections.unmodifiableMap(totals);
    }

    public static class ConfiguredOffer {
        private final ItemStack item;
        private final List<PriceRequirement> prices;
        private final String category;

        public ConfiguredOffer(ItemStack item, List<PriceRequirement> prices, String category) {
            this.item = item;
            this.prices = List.copyOf(prices);
            this.category = category;
        }

        public ItemStack item() {
            return item;
        }

        public List<PriceRequirement> prices() {
            return prices;
        }

        public String category() {
            return category;
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

        Optional<OfferTokens> tokens = parseOfferTokens(raw);
        if (tokens.isEmpty()) {
            return false;
        }

        OfferTokens parsed = tokens.get();
        if (parsed.itemId() == null || !BuiltInRegistries.ITEM.containsKey(parsed.itemId())) {
            return false;
        }

        return parsePriceRequirements(parsed.priceSection(), raw).stream()
                .allMatch(req -> BuiltInRegistries.ITEM.containsKey(req.id()));
    }

    private static Optional<ConfiguredCurrency> parseCurrency(String currencyId) {
        ResourceLocation id = ResourceLocation.tryParse(currencyId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return Optional.empty();
        }

        Item currencyItem = BuiltInRegistries.ITEM.get(id);
        return Optional.of(new ConfiguredCurrency(id, currencyItem));
    }

    private static Optional<ConfiguredOffer> parseOffer(final String raw, List<String> categories,
            Map<ResourceLocation, ConfiguredCurrency> currencyLookup) {
        Optional<OfferTokens> tokens = parseOfferTokens(raw);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }

        OfferTokens parsed = tokens.get();
        if (parsed.itemId() == null || !BuiltInRegistries.ITEM.containsKey(parsed.itemId())) {
            return Optional.empty();
        }

        List<PriceRequirement> requirements = parsePriceRequirements(parsed.priceSection(), raw).stream()
                .map(req -> buildRequirement(req, currencyLookup))
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(ArrayList::new));
        if (requirements.isEmpty()) {
            return Optional.empty();
        }

        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(parsed.itemId()));
        String category = resolveCategory(parsed.category(), categories);
        return Optional.of(new ConfiguredOffer(stack, requirements, category));
    }

    private static Optional<OfferTokens> parseOfferTokens(String raw) {
        String[] parts = raw.split("\\|", 3);
        if (parts.length < 2) {
            return Optional.empty();
        }

        ResourceLocation itemId = ResourceLocation.tryParse(parts[0]);
        String category = parts.length == 3 ? parts[1].trim() : "";
        String priceSection = parts.length == 3 ? parts[2] : parts[1];
        return Optional.of(new OfferTokens(itemId, category, priceSection));
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

    private static String resolveCategory(String category, List<String> categories) {
        String trimmed = category == null ? "" : category.trim();
        if (!trimmed.isEmpty() && categories.contains(trimmed)) {
            return trimmed;
        }
        String fallback = categories.isEmpty() ? "default" : categories.get(0);
        if (!trimmed.isEmpty()) {
            AwesomeShop.LOGGER.warn("Category '{}' is not configured; defaulting to '{}'.", trimmed, fallback);
        } else {
            AwesomeShop.LOGGER.warn(
                    "No category was provided for an offer entry; add a category between the item id and price section (e.g. 'minecraft:apple|cat1|minecraft:emerald=1'). Defaulting to '{}'.",
                    fallback);
        }
        return fallback;
    }

    private record RawRequirement(ResourceLocation id, int price) {
    }

    private record OfferTokens(ResourceLocation itemId, String category, String priceSection) {
    }

    private static List<RawRequirement> parsePriceRequirements(String priceSection, String rawOffer) {
        List<RawRequirement> requirements = new ArrayList<>();
        String[] entries = priceSection.split(",");
        for (String entry : entries) {
            parseRequirement(entry.trim(), rawOffer).ifPresent(requirements::add);
        }
        return requirements;
    }

    private static Optional<RawRequirement> parseRequirement(String entry, String rawOffer) {
        if (entry.isEmpty()) {
            return Optional.empty();
        }

        String[] parts = entry.split("=", 2);
        if (parts.length != 2) {
            AwesomeShop.LOGGER.warn("Ignoring unrecognized price entry '{}' in shop offer '{}'.", entry, rawOffer);
            return Optional.empty();
        }

        return parseKeyValueRequirement(parts[0].trim(), parts[1].trim(), rawOffer);
    }

    private static Optional<RawRequirement> parseKeyValueRequirement(String currencyToken, String priceToken, String rawOffer) {
        ResourceLocation currencyId = ResourceLocation.tryParse(currencyToken);
        if (currencyId == null || !BuiltInRegistries.ITEM.containsKey(currencyId)) {
            AwesomeShop.LOGGER.warn("Ignoring invalid currency '{}' in shop offer '{}'.", currencyToken, rawOffer);
            return Optional.empty();
        }

        try {
            int price = Integer.parseInt(priceToken);
            if (price > 0) {
                return Optional.of(new RawRequirement(currencyId, price));
            }
        } catch (NumberFormatException ex) {
            AwesomeShop.LOGGER.warn("Invalid price for shop offer '{}': {}", rawOffer, ex.getMessage());
        }
        return Optional.empty();
    }

    private static Component formatPriceList(ConfiguredOffer offer) {
        return Component.literal(aggregatePriceRequirements(offer.prices()).entrySet().stream()
                .map(entry -> Component.translatable("block.awesomeshop.shop_block.price_entry", entry.getValue(),
                        Component.translatable(entry.getKey().item().getDescriptionId())).getString())
                .collect(Collectors.joining(", ")));
    }
}
