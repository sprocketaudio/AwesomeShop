package net.sprocketaudio.awesomeshop;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ShopStyleConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> GUI_BORDER_COLOR = colorValue("gui.borderColor",
            "0x777777", "Color used for the main GUI border and divider.");
    public static final ModConfigSpec.IntValue GUI_BORDER_THICKNESS = BUILDER
            .comment("Thickness of the main GUI border in pixels.")
            .defineInRange("gui.borderThickness", 2, 1, 12);

    public static final ModConfigSpec.ConfigValue<String> TITLE_BACKGROUND_COLOR = colorValue(
            "gui.title.backgroundColor", "0x161616", "Base RGB color for the shop title background box.");
    public static final ModConfigSpec.DoubleValue TITLE_BACKGROUND_OPACITY = opacityValue(
            "gui.title.backgroundOpacity", 0.8d, "Opacity (0-1) applied to the shop title background box.");
    public static final ModConfigSpec.ConfigValue<String> TITLE_BORDER_COLOR = colorValue("gui.title.borderColor",
            "0x777777", "Border color for the shop title background box.");
    public static final ModConfigSpec.IntValue TITLE_BORDER_THICKNESS = BUILDER
            .comment("Border thickness for the shop title box in pixels.")
            .defineInRange("gui.title.borderThickness", 2, 0, 12);
    public static final ModConfigSpec.ConfigValue<String> TITLE_TEXT_COLOR = colorValue("gui.title.textColor",
            "0xFFFFFF", "Text color for the shop title.");
    public static final ModConfigSpec.DoubleValue TITLE_FONT_SCALE = BUILDER
            .comment("Scale multiplier applied to the shop title font size.")
            .defineInRange("gui.title.fontScale", 1.7d, 0.5d, 3.0d);

    public static final ModConfigSpec.ConfigValue<String> CATEGORY_PANEL_BACKGROUND_COLOR = colorValue(
            "gui.categoryPanel.backgroundColor", "0x161616",
            "Base RGB color for the category column background.");
    public static final ModConfigSpec.DoubleValue CATEGORY_PANEL_BACKGROUND_OPACITY = opacityValue(
            "gui.categoryPanel.backgroundOpacity", 0.8d,
            "Opacity (0-1) applied to the category column background.");

    public static final ModConfigSpec.ConfigValue<String> ITEM_PANEL_BACKGROUND_COLOR = colorValue(
            "gui.itemPanel.backgroundColor", "0x161616", "Base RGB color for the item panel background.");
    public static final ModConfigSpec.DoubleValue ITEM_PANEL_BACKGROUND_OPACITY = opacityValue(
            "gui.itemPanel.backgroundOpacity", 0.8d,
            "Opacity (0-1) applied to the item panel background.");

    public static final ModConfigSpec.ConfigValue<String> CATEGORY_BUTTON_BACKGROUND_SELECTED = colorValue(
            "category.button.backgroundSelected", "0x333333",
            "Background RGB color for the selected category button.");
    public static final ModConfigSpec.DoubleValue CATEGORY_BUTTON_BACKGROUND_SELECTED_OPACITY = opacityValue(
            "category.button.backgroundSelectedOpacity", 1.0d,
            "Opacity (0-1) applied to the selected category button background.");
    public static final ModConfigSpec.ConfigValue<String> CATEGORY_BUTTON_BACKGROUND_HOVER = colorValue(
            "category.button.backgroundHover", "0x222222",
            "Background RGB color for category buttons when hovered.");
    public static final ModConfigSpec.DoubleValue CATEGORY_BUTTON_BACKGROUND_HOVER_OPACITY = opacityValue(
            "category.button.backgroundHoverOpacity", 1.0d,
            "Opacity (0-1) applied to hovered category buttons.");
    public static final ModConfigSpec.ConfigValue<String> CATEGORY_BUTTON_BACKGROUND = colorValue(
            "category.button.background", "0x111111", "Background RGB color for unselected category buttons.");
    public static final ModConfigSpec.DoubleValue CATEGORY_BUTTON_BACKGROUND_OPACITY = opacityValue(
            "category.button.backgroundOpacity", 0.7d,
            "Opacity (0-1) applied to unselected category buttons.");

    public static final ModConfigSpec.ConfigValue<String> CATEGORY_BUTTON_BORDER_COLOR = colorValue(
            "category.button.borderColor", "0x282828",
            "Border color for unselected category buttons in their normal state.");
    public static final ModConfigSpec.ConfigValue<String> CATEGORY_BUTTON_BORDER_COLOR_HOVER = colorValue(
            "category.button.borderColorHover", "0xFFFFFF",
            "Border color for category buttons when hovered.");
    public static final ModConfigSpec.ConfigValue<String> CATEGORY_BUTTON_BORDER_COLOR_SELECTED = colorValue(
            "category.button.borderColorSelected", "0xD3D3D3",
            "Border color for selected category buttons.");
    public static final ModConfigSpec.IntValue CATEGORY_BUTTON_BORDER_THICKNESS = BUILDER
            .comment("Border thickness for category buttons in pixels.")
            .defineInRange("category.button.borderThickness", 1, 0, 6);

    public static final ModConfigSpec.ConfigValue<String> CATEGORY_TITLE_TEXT_COLOR = colorValue(
            "category.titleTextColor", "0xFFFFFF",
            "Text color for the \"Categories\" heading.");
    public static final ModConfigSpec.DoubleValue CATEGORY_TITLE_FONT_SCALE = BUILDER
            .comment("Scale multiplier applied to the Categories heading font size.")
            .defineInRange("category.titleFontScale", 1.5d, 0.5d, 3.0d);
    public static final ModConfigSpec.ConfigValue<String> CATEGORY_BUTTON_TEXT_COLOR = colorValue(
            "category.button.textColor", "0xFFFFFF",
            "Text color for unselected category buttons.");
    public static final ModConfigSpec.ConfigValue<String> CATEGORY_BUTTON_TEXT_COLOR_HOVER = colorValue(
            "category.button.textColorHover", "0xFFFFFF",
            "Text color for hovered category buttons.");
    public static final ModConfigSpec.ConfigValue<String> CATEGORY_BUTTON_TEXT_COLOR_SELECTED = colorValue(
            "category.button.textColorSelected", "0xFFFFFF",
            "Text color for selected category buttons.");

    public static final ModConfigSpec.ConfigValue<String> CARD_PANEL_BACKGROUND_COLOR = colorValue(
            "card.panel.backgroundColor", "0x161616", "Base RGB color for offer cards.");
    public static final ModConfigSpec.DoubleValue CARD_PANEL_BACKGROUND_OPACITY = opacityValue(
            "card.panel.backgroundOpacity", 0.8d, "Opacity (0-1) applied to offer cards.");
    public static final ModConfigSpec.ConfigValue<String> CARD_PANEL_BORDER_COLOR = colorValue(
            "card.panel.borderColor", "0x777777", "Border color for offer cards.");
    public static final ModConfigSpec.IntValue CARD_PANEL_BORDER_THICKNESS = BUILDER
            .comment("Border thickness for offer cards in pixels.")
            .defineInRange("card.panel.borderThickness", 1, 0, 8);

    public static final ModConfigSpec.ConfigValue<String> CARD_BUTTON_BACKGROUND_COLOR = colorValue(
            "card.button.backgroundColor", "0x222222", "Base RGB color for offer buttons.");
    public static final ModConfigSpec.DoubleValue CARD_BUTTON_BACKGROUND_OPACITY = opacityValue(
            "card.button.backgroundOpacity", 0.2d, "Opacity (0-1) applied to offer buttons.");
    public static final ModConfigSpec.ConfigValue<String> CARD_BUTTON_BACKGROUND_COLOR_HOVER = colorValue(
            "card.button.backgroundColorHover", "0x555555",
            "Background RGB color for hovered offer buttons.");
    public static final ModConfigSpec.DoubleValue CARD_BUTTON_BACKGROUND_OPACITY_HOVER = opacityValue(
            "card.button.backgroundOpacityHover", 1.0d,
            "Opacity (0-1) applied to hovered offer buttons.");
    public static final ModConfigSpec.ConfigValue<String> CARD_BUTTON_BACKGROUND_COLOR_DISABLED = colorValue(
            "card.button.backgroundColorDisabled", "0x3C3C3C",
            "Background RGB color for disabled offer buttons.");
    public static final ModConfigSpec.DoubleValue CARD_BUTTON_BACKGROUND_OPACITY_DISABLED = opacityValue(
            "card.button.backgroundOpacityDisabled", 0.67d,
            "Opacity (0-1) applied to disabled offer buttons.");

    public static final ModConfigSpec.ConfigValue<String> CARD_BUTTON_BORDER_COLOR = colorValue(
            "card.button.borderColor", "0xAAAAAA", "Border color for offer buttons.");
    public static final ModConfigSpec.ConfigValue<String> CARD_BUTTON_BORDER_COLOR_HOVER = colorValue(
            "card.button.borderColorHover", "0xFFFFFF", "Border color for hovered offer buttons.");
    public static final ModConfigSpec.ConfigValue<String> CARD_BUTTON_BORDER_COLOR_DISABLED = colorValue(
            "card.button.borderColorDisabled", "0x282828", "Border color for disabled offer buttons.");
    public static final ModConfigSpec.IntValue CARD_BUTTON_BORDER_THICKNESS = BUILDER
            .comment("Border thickness for offer buttons in pixels.")
            .defineInRange("card.button.borderThickness", 1, 0, 6);

    public static final ModConfigSpec.ConfigValue<String> CARD_ITEM_TEXT_COLOR = colorValue(
            "card.itemTextColor", "0xFFFFFF", "Text color for the item title on cards.");
    public static final ModConfigSpec.ConfigValue<String> CARD_BUTTON_TEXT_COLOR = colorValue(
            "card.button.textColor", "0xFFFFFF", "Text color for active offer buttons.");
    public static final ModConfigSpec.ConfigValue<String> CARD_BUTTON_TEXT_COLOR_HOVER = colorValue(
            "card.button.textColorHover", "0xFFFFFF", "Text color for hovered offer buttons.");
    public static final ModConfigSpec.ConfigValue<String> CARD_BUTTON_TEXT_COLOR_DISABLED = colorValue(
            "card.button.textColorDisabled", "0x666666", "Text color for disabled offer buttons.");

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static ShopStyle getStyle() {
        return new ShopStyle(
                composeColor(GUI_BORDER_COLOR.get(), 1.0d, "gui.borderColor"),
                GUI_BORDER_THICKNESS.get(),
                composeColor(CATEGORY_PANEL_BACKGROUND_COLOR.get(), CATEGORY_PANEL_BACKGROUND_OPACITY.get(),
                        "gui.categoryPanel.background"),
                composeColor(ITEM_PANEL_BACKGROUND_COLOR.get(), ITEM_PANEL_BACKGROUND_OPACITY.get(),
                        "gui.itemPanel.background"),
                composeColor(TITLE_BACKGROUND_COLOR.get(), TITLE_BACKGROUND_OPACITY.get(), "gui.title.background"),
                composeColor(TITLE_BORDER_COLOR.get(), 1.0d, "gui.title.border"),
                TITLE_BORDER_THICKNESS.get(),
                composeColor(TITLE_TEXT_COLOR.get(), 1.0d, "gui.title.text"),
                TITLE_FONT_SCALE.get().floatValue(),
                composeColor(CATEGORY_BUTTON_BACKGROUND_SELECTED.get(),
                        CATEGORY_BUTTON_BACKGROUND_SELECTED_OPACITY.get(), "category.button.selected"),
                composeColor(CATEGORY_BUTTON_BACKGROUND_HOVER.get(), CATEGORY_BUTTON_BACKGROUND_HOVER_OPACITY.get(),
                        "category.button.hover"),
                composeColor(CATEGORY_BUTTON_BACKGROUND.get(), CATEGORY_BUTTON_BACKGROUND_OPACITY.get(),
                        "category.button.background"),
                composeColor(CATEGORY_BUTTON_BORDER_COLOR.get(), 1.0d, "category.button.borderColor"),
                composeColor(CATEGORY_BUTTON_BORDER_COLOR_HOVER.get(), 1.0d, "category.button.borderColorHover"),
                composeColor(CATEGORY_BUTTON_BORDER_COLOR_SELECTED.get(), 1.0d,
                        "category.button.borderColorSelected"),
                CATEGORY_BUTTON_BORDER_THICKNESS.get(),
                composeColor(CATEGORY_TITLE_TEXT_COLOR.get(), 1.0d, "category.titleTextColor"),
                CATEGORY_TITLE_FONT_SCALE.get().floatValue(),
                composeColor(CATEGORY_BUTTON_TEXT_COLOR_SELECTED.get(), 1.0d, "category.button.text.selected"),
                composeColor(CATEGORY_BUTTON_TEXT_COLOR_HOVER.get(), 1.0d, "category.button.text.hover"),
                composeColor(CATEGORY_BUTTON_TEXT_COLOR.get(), 1.0d, "category.button.text"),
                composeColor(CARD_PANEL_BACKGROUND_COLOR.get(), CARD_PANEL_BACKGROUND_OPACITY.get(),
                        "card.panel.background"),
                composeColor(CARD_PANEL_BORDER_COLOR.get(), 1.0d, "card.panel.borderColor"),
                CARD_PANEL_BORDER_THICKNESS.get(),
                composeColor(CARD_BUTTON_BACKGROUND_COLOR.get(), CARD_BUTTON_BACKGROUND_OPACITY.get(),
                        "card.button.background"),
                composeColor(CARD_BUTTON_BACKGROUND_COLOR_HOVER.get(), CARD_BUTTON_BACKGROUND_OPACITY_HOVER.get(),
                        "card.button.hover"),
                composeColor(CARD_BUTTON_BACKGROUND_COLOR_DISABLED.get(), CARD_BUTTON_BACKGROUND_OPACITY_DISABLED.get(),
                        "card.button.disabled"),
                composeColor(CARD_BUTTON_BORDER_COLOR.get(), 1.0d, "card.button.border"),
                composeColor(CARD_BUTTON_BORDER_COLOR_HOVER.get(), 1.0d, "card.button.borderHover"),
                composeColor(CARD_BUTTON_BORDER_COLOR_DISABLED.get(), 1.0d, "card.button.borderDisabled"),
                CARD_BUTTON_BORDER_THICKNESS.get(),
                composeColor(CARD_ITEM_TEXT_COLOR.get(), 1.0d, "card.itemTextColor"),
                composeColor(CARD_BUTTON_TEXT_COLOR.get(), 1.0d, "card.button.text"),
                composeColor(CARD_BUTTON_TEXT_COLOR_HOVER.get(), 1.0d, "card.button.text.hover"),
                composeColor(CARD_BUTTON_TEXT_COLOR_DISABLED.get(), 1.0d, "card.button.text.disabled"));
    }

    private static ModConfigSpec.ConfigValue<String> colorValue(String key, String defaultValue, String... comment) {
        return BUILDER.comment(comment).define(key, defaultValue);
    }

    private static ModConfigSpec.DoubleValue opacityValue(String key, double defaultValue, String... comment) {
        return BUILDER.comment(comment).defineInRange(key, defaultValue, 0.0d, 1.0d);
    }

    private static int composeColor(String colorValue, double opacity, String configKey) {
        ParsedColor parsed = parseColor(colorValue, configKey);
        int alphaFromOpacity = (int) Math.round(Mth.clamp(opacity, 0.0d, 1.0d) * 255.0d);
        double normalizedAlpha = (parsed.alpha() / 255.0d) * (alphaFromOpacity / 255.0d);
        int finalAlpha = Mth.clamp((int) Math.round(normalizedAlpha * 255.0d), 0, 255);
        return (finalAlpha << 24) | parsed.rgb();
    }

    private static ParsedColor parseColor(String raw, String key) {
        if (raw == null) {
            return new ParsedColor(0xFFFFFF, 0xFF);
        }
        String normalized = raw.trim();
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        normalized = normalized.replace("_", "");

        try {
            long parsed = Long.parseLong(normalized, 16);
            if (normalized.length() > 6) {
                int alpha = (int) ((parsed >> 24) & 0xFF);
                int rgb = (int) (parsed & 0xFFFFFF);
                return new ParsedColor(rgb, alpha);
            }
            return new ParsedColor((int) parsed & 0xFFFFFF, 0xFF);
        } catch (NumberFormatException ex) {
            AwesomeShop.LOGGER.warn("Invalid color value '{}' for {}; defaulting to white.", raw, key);
            return new ParsedColor(0xFFFFFF, 0xFF);
        }
    }

    public record ShopStyle(int guiBorderColor, int guiBorderThickness, int categoryPanelBackground,
            int itemPanelBackground, int titleBackgroundColor, int titleBorderColor, int titleBorderThickness,
            int titleTextColor, float titleFontScale, int categoryButtonSelectedBackground,
            int categoryButtonHoverBackground, int categoryButtonBackground, int categoryButtonBorderColor,
            int categoryButtonBorderHoverColor, int categoryButtonBorderSelectedColor,
            int categoryButtonBorderThickness, int categoryTitleTextColor, float categoryTitleFontScale,
            int categoryButtonTextSelectedColor, int categoryButtonTextHoverColor, int categoryButtonTextColor,
            int cardPanelBackground, int cardPanelBorderColor, int cardPanelBorderThickness, int cardButtonBackground,
            int cardButtonHoverBackground, int cardButtonDisabledBackground, int cardButtonBorderColor,
            int cardButtonBorderHoverColor, int cardButtonBorderDisabledColor, int cardButtonBorderThickness,
            int cardItemTextColor, int cardButtonTextColor, int cardButtonTextHoverColor,
            int cardButtonTextDisabledColor) {
    }

    private record ParsedColor(int rgb, int alpha) {
    }
}
