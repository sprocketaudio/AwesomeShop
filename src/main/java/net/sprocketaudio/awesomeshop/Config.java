package net.sprocketaudio.awesomeshop;

import java.util.ArrayList;
import java.util.List;
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

    public static final ModConfigSpec.ConfigValue<String> CURRENCY_ITEM = BUILDER
            .comment("Item used as currency for all shop transactions.")
            .define("currencyItem", "minecraft:emerald", Config::validateItemName);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SHOP_OFFERS = BUILDER
            .comment("Items that can be purchased from the shop block along with their prices.",
                    "Format: namespace:item|price", "Example: minecraft:apple|2")
            .defineListAllowEmpty("shopOffers", List.of("minecraft:apple|1", "minecraft:bread|2"), () -> "",
                    Config::validateOffer);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof String itemName)) {
            return false;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(itemName);
        return parsed != null && BuiltInRegistries.ITEM.containsKey(parsed);
    }

    public static ResourceLocation getCurrencyLocation() {
        ResourceLocation parsed = ResourceLocation.tryParse(CURRENCY_ITEM.get());
        return parsed == null ? ResourceLocation.parse("minecraft:emerald") : parsed;
    }

    public static Item getCurrencyItem() {
        return BuiltInRegistries.ITEM.getOptional(getCurrencyLocation()).orElse(Items.EMERALD);
    }

    public static List<ConfiguredOffer> getConfiguredOffers() {
        return SHOP_OFFERS.get().stream()
                .map(Config::parseOffer)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static Component offerSummaryMessage(ItemStack offer, Item currency) {
        return Component.translatable("block.awesomeshop.shop_block.offer", offer.getHoverName(), Component.translatable(currency.getDescriptionId()));
    }

    public static Component purchaseSuccessMessage(ItemStack offer, int quantity) {
        return Component.translatable("block.awesomeshop.shop_block.purchase", offer.getHoverName(), quantity);
    }

    public static Component purchaseFailureMessage(ItemStack offer, Item currency, int totalCost) {
        return Component.translatable("block.awesomeshop.shop_block.failed", offer.getHoverName(), Component.translatable(currency.getDescriptionId()), totalCost);
    }

    public static class ConfiguredOffer {
        private final ItemStack item;
        private final int price;

        public ConfiguredOffer(ItemStack item, int price) {
            this.item = item;
            this.price = price;
        }

        public ItemStack item() {
            return item;
        }

        public int price() {
            return price;
        }
    }

    private static boolean validateOffer(final Object obj) {
        return obj instanceof String s && parseOffer(s).isPresent();
    }

    private static Optional<ConfiguredOffer> parseOffer(final String raw) {
        String[] parts = raw.split("\\|", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }

        ResourceLocation itemId = ResourceLocation.tryParse(parts[0]);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return Optional.empty();
        }

        try {
            int price = Integer.parseInt(parts[1]);
            if (price <= 0) {
                return Optional.empty();
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            return Optional.of(new ConfiguredOffer(stack, price));
        } catch (NumberFormatException ex) {
            AwesomeShop.LOGGER.warn("Invalid price for shop offer '{}': {}", raw, ex.getMessage());
            return Optional.empty();
        }
    }
}
