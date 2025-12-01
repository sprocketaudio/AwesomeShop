package net.sprocketaudio.awesomeshop;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> CURRENCY_ITEM = BUILDER
            .comment("Item used as currency for all shop transactions.")
            .define("currencyItem", "minecraft:emerald", Config::validateItemName);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SHOP_OFFERS = BUILDER
            .comment("Items that can be purchased from the shop block.")
            .defineListAllowEmpty("shopOffers", List.of("minecraft:apple", "minecraft:bread"), () -> "",
                    Config::validateItemName);

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

    public static List<ItemStack> getConfiguredOffers() {
        return SHOP_OFFERS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .map(id -> BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY))
                .filter(stack -> !stack.isEmpty())
                .collect(Collectors.toList());
    }

    public static int getOfferPrice(ItemStack offer) {
        return 1;
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
}
