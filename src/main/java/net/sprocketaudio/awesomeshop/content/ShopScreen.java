package net.sprocketaudio.awesomeshop.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.sprocketaudio.awesomeshop.Config.ConfiguredCurrency;
import net.sprocketaudio.awesomeshop.Config.ConfiguredOffer;
import net.sprocketaudio.awesomeshop.Config.PriceRequirement;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final int PADDING = 8;
    private static final int SECTION_TOP = 64;
    private static final int GRID_GAP = 12;
    private static final int BUTTON_WIDTH = 18;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;
    private static final int PURCHASE_BUTTON_WIDTH = 100;
    private static final int CATEGORY_BUTTON_HEIGHT = 20;
    private static final int CATEGORY_TITLE_GAP = 6;
    private static final int COLUMN_GAP = 10;
    private static final int MIN_IMAGE_WIDTH = 320;
    private static final float GUI_WIDTH_RATIO = 0.8f;
    private static final float GUI_HEIGHT_RATIO = 0.8f;
    private static final float CATEGORY_COLUMN_RATIO = 0.25f;
    private static final int CARD_WIDTH = 170;
    private static final int CARD_HEIGHT = 120;
    private static final int CARD_PADDING = 8;
    private static final int BORDER_THICKNESS = 2;
    private static final int CARD_BORDER_COLOR = 0xCCFFFFFF;
    private static final int PANEL_BORDER_COLOR = 0xCCFFFFFF;
    private static final int PANEL_FILL_COLOR = 0xEE161616;
    private static final int CARD_FILL_COLOR = 0xEE161616;
    private static final int BUTTON_BASE_COLOR = 0xFFD4D4D4;
    private static final int BUTTON_HOVER_COLOR = 0xFFE4E4E4;
    private static final int CURRENCY_GAP = 10;
    private static final int ITEM_ICON_SIZE = 24;

    private int lockedGuiScale = -1;
    private int originalGuiScale = -1;

    private final int[] selectedQuantities;
    private final Map<Integer, Button> purchaseButtons = new HashMap<>();
    private final Map<String, Button> categoryButtons = new HashMap<>();
    private final List<String> categories;
    private String selectedCategory;
    private List<OfferCard> cards = List.of();

    public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.selectedQuantities = new int[menu.getOffers().size()];
        Arrays.fill(this.selectedQuantities, 1);
        this.categories = new ArrayList<>(menu.getCategories());
        if (this.categories.isEmpty()) {
            this.categories.add("default");
        }
        this.selectedCategory = this.categories.get(0);
        this.imageWidth = 360;
        this.imageHeight = 140;
    }

    @Override
    protected void init() {
        super.init();
        lockGuiScale();
        recalculateDimensions();
        rebuildLayout();
    }

    private void recalculateDimensions() {
        this.imageWidth = Math.max(MIN_IMAGE_WIDTH, (int) (this.width * GUI_WIDTH_RATIO));
        this.leftPos = (this.width - this.imageWidth) / 2;
    }

    private int getTopRowY() {
        return topPos + PADDING + BORDER_THICKNESS;
    }

    private void rebuildLayout() {
        clearWidgets();
        purchaseButtons.clear();
        categoryButtons.clear();

        if (!categories.contains(selectedCategory) && !categories.isEmpty()) {
            selectedCategory = categories.get(0);
        }

        this.cards = buildCards();
        int maxHeight = this.height - (PADDING * 2);
        int targetHeight = (int) (this.height * GUI_HEIGHT_RATIO);
        this.imageHeight = Mth.clamp(Math.max(calculateImageHeight(), targetHeight), 140, maxHeight);
        this.topPos = Math.max(PADDING, (this.height - this.imageHeight) / 2);
        this.cards = buildCards();

        placeCategoryButtons();
        placeOfferButtons();
    }

    private void placeOfferButtons() {
        for (OfferCard card : cards) {
            int index = card.offerIndex();
            int priceRowY = getPriceRowY(card.startY());
            int quantityButtonY = priceRowY - Math.max(0, (BUTTON_HEIGHT - font.lineHeight) / 2) - 1;
            int minusX = card.startX() + CARD_PADDING;
            int plusX = card.startX() + CARD_WIDTH - CARD_PADDING - BUTTON_WIDTH;

            addRenderableWidget(createTintedButton(minusX, quantityButtonY, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("-"),
                    b -> adjustQuantity(index, -1)));

            addRenderableWidget(createTintedButton(plusX, quantityButtonY, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("+"),
                    b -> adjustQuantity(index, 1)));

            int purchaseY = quantityButtonY + BUTTON_HEIGHT + BUTTON_GAP;
            Button purchaseButton = createTintedButton(card.startX() + CARD_PADDING, purchaseY, CARD_WIDTH - (CARD_PADDING * 2),
                    BUTTON_HEIGHT, Component.literal(""), b -> purchaseOffer(index));
            purchaseButtons.put(index, addRenderableWidget(purchaseButton));
            updatePurchaseButton(index);
        }
    }

    private void placeCategoryButtons() {
        int buttonWidth = getCategoryColumnWidth() - (PADDING * 2);
        int startX = leftPos + PADDING;
        int startY = getCategoryButtonsStartY();

        for (String category : categories) {
            Button button = createTintedButton(startX, startY, buttonWidth, CATEGORY_BUTTON_HEIGHT, Component.literal(category),
                    b -> selectCategory(category));
            categoryButtons.put(category, addRenderableWidget(button));
            startY += CATEGORY_BUTTON_HEIGHT + BUTTON_GAP;
        }
        updateCategoryButtonStates();
    }

    private void purchaseOffer(int index) {
        if (minecraft != null && minecraft.gameMode != null) {
            int quantity = selectedQuantities[index];
            if (quantity <= 0) {
                return;
            }
            int buttonId = (quantity * menu.getOffers().size()) + index;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
            ConfiguredOffer offer = menu.getOffers().get(index);
            int maxAffordable = calculateMaxAffordable(offer);
            selectedQuantities[index] = Math.min(Math.max(1, quantity), maxAffordable);
            adjustQuantity(index, 0);
        }
    }

    private void adjustQuantity(int index, int delta) {
        List<ConfiguredOffer> offers = menu.getOffers();
        if (index < 0 || index >= offers.size()) {
            return;
        }

        ConfiguredOffer offer = offers.get(index);
        int maxAffordable = calculateMaxAffordable(offer);
        int minQuantity = maxAffordable > 0 ? 1 : 0;
        int newQuantity = Mth.clamp(selectedQuantities[index] + delta, minQuantity, maxAffordable);
        selectedQuantities[index] = newQuantity;
        updatePurchaseButton(index);
    }

    private void selectCategory(String category) {
        if (!Objects.equals(selectedCategory, category) && categories.contains(category)) {
            selectedCategory = category;
            rebuildLayout();
        }
    }

    private void updateCategoryButtonStates() {
        categoryButtons.forEach((category, button) -> button.active = !Objects.equals(category, selectedCategory));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        for (int i = 0; i < selectedQuantities.length; i++) {
            adjustQuantity(i, 0);
        }
    }

    private void updatePurchaseButton(int index) {
        if (index < 0 || !purchaseButtons.containsKey(index)) {
            return;
        }
        Button button = purchaseButtons.get(index);
        int quantity = selectedQuantities[index];
        ConfiguredOffer offer = menu.getOffers().get(index);
        button.setMessage(Component.translatable("screen.awesomeshop.shop_block.buy", quantity, offer.item().getHoverName()));
        button.active = quantity > 0;
    }

    private Button createTintedButton(int x, int y, int width, int height, Component label, Button.OnPress onPress) {
        return new SolidButton(x, y, width, height, label, onPress);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanels(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCategoryPanel(graphics);
        renderCurrencyTotals(graphics);
        renderOfferDetails(graphics);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void renderPanels(GuiGraphics graphics) {
        int right = leftPos + imageWidth;
        int bottom = topPos + imageHeight;
        int categoryRight = leftPos + getCategoryColumnWidth();
        int innerLeft = leftPos + BORDER_THICKNESS;
        int innerTop = topPos + BORDER_THICKNESS;
        int innerRight = right - BORDER_THICKNESS;
        int innerBottom = bottom - BORDER_THICKNESS;

        // Panel frame
        graphics.fill(leftPos, topPos, right, topPos + BORDER_THICKNESS, PANEL_BORDER_COLOR);
        graphics.fill(leftPos, bottom - BORDER_THICKNESS, right, bottom, PANEL_BORDER_COLOR);
        graphics.fill(leftPos, topPos, leftPos + BORDER_THICKNESS, bottom, PANEL_BORDER_COLOR);
        graphics.fill(right - BORDER_THICKNESS, topPos, right, bottom, PANEL_BORDER_COLOR);

        // Unified background inside the frame
        graphics.fill(innerLeft, innerTop, innerRight, innerBottom, PANEL_FILL_COLOR);

        int dividerX = categoryRight + (COLUMN_GAP / 2);
        graphics.fill(dividerX, innerTop, dividerX + BORDER_THICKNESS, innerBottom, PANEL_BORDER_COLOR);
    }

    private void renderCategoryPanel(GuiGraphics graphics) {
        int titleX = leftPos + PADDING;
        int titleY = getTopRowY();
        graphics.drawString(font, Component.literal("Categories"), titleX, titleY, 0xFFFFFF);
    }

    private void renderOfferDetails(GuiGraphics graphics) {
        List<ConfiguredOffer> offers = menu.getOffers();
        for (OfferCard card : cards) {
            int index = card.offerIndex();
            if (index < 0 || index >= offers.size()) {
                continue;
            }

            ConfiguredOffer offer = offers.get(index);
            int cardX = card.startX();
            int cardY = card.startY();
            int cardCenterX = cardX + (CARD_WIDTH / 2);

            graphics.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, CARD_FILL_COLOR);
            graphics.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + BORDER_THICKNESS, CARD_BORDER_COLOR);
            graphics.fill(cardX, cardY + CARD_HEIGHT - BORDER_THICKNESS, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, CARD_BORDER_COLOR);
            graphics.fill(cardX, cardY, cardX + BORDER_THICKNESS, cardY + CARD_HEIGHT, CARD_BORDER_COLOR);
            graphics.fill(cardX + CARD_WIDTH - BORDER_THICKNESS, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, CARD_BORDER_COLOR);

            Component itemName = offer.item().getHoverName();
            int nameY = cardY + CARD_PADDING;
            graphics.drawCenteredString(font, itemName, cardCenterX, nameY, 0xFFFFFF);

            float iconScale = ITEM_ICON_SIZE / 16f;
            int iconX = cardCenterX - (int) (ITEM_ICON_SIZE / 2f);
            int iconY = nameY + font.lineHeight + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(iconX, iconY, 0);
            graphics.pose().scale(iconScale, iconScale, 1.0f);
            graphics.renderItem(offer.item(), 0, 0);
            graphics.renderItemDecorations(font, offer.item(), 0, 0);
            graphics.pose().popPose();

            int currenciesY = iconY + ITEM_ICON_SIZE + 6;
            List<PriceRequirement> requirements = offer.prices();
            int totalCurrencyWidth = calculateCurrencyRowWidth(requirements);
            int currencyStartX = cardCenterX - (totalCurrencyWidth / 2);
            int currentX = currencyStartX;

            for (PriceRequirement requirement : requirements) {
                ItemStack currencyStack = new ItemStack(requirement.currency().item());
                graphics.renderItem(currencyStack, currentX, currenciesY);
                graphics.renderItemDecorations(font, currencyStack, currentX, currenciesY);
                currentX += 16 + CURRENCY_GAP;
            }

            int quantity = selectedQuantities[index];
            Component priceLine = buildPriceLine(requirements, quantity);
            int priceY = getPriceRowY(cardY);
            graphics.drawCenteredString(font, priceLine, cardCenterX, priceY, 0xDDDDDD);
        }
    }

    private void renderCurrencyTotals(GuiGraphics graphics) {
        List<ConfiguredCurrency> currencies = menu.getCurrencies();
        if (currencies.isEmpty()) {
            return;
        }

        int currencyRowBaseline = getTopRowY();
        int totalWidth = calculateCurrencyTotalsWidth(currencies);
        int startX = leftPos + imageWidth - PADDING - totalWidth;
        int currentX = startX;

        for (ConfiguredCurrency currency : currencies) {
            ItemStack currencyStack = new ItemStack(currency.item());
            Component totalLine = Component.literal("x " + menu.getCurrencyCount(currency));
            int lineWidth = 16 + 4 + font.width(totalLine);

            int iconY = currencyRowBaseline - ((16 - font.lineHeight) / 2);
            graphics.renderItem(currencyStack, currentX, iconY);
            graphics.renderItemDecorations(font, currencyStack, currentX, iconY);

            int textY = currencyRowBaseline;
            graphics.drawString(font, totalLine, currentX + 16 + 4, textY, 0xFFFFFF);

            currentX += lineWidth + CURRENCY_GAP;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = imageWidth / 2;
        int titleY = getTopRowY() - topPos;
        graphics.drawCenteredString(font, title, centerX, titleY, 0xFFFFFF);
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        lockGuiScale();
        recalculateDimensions();
        rebuildLayout();
    }

    @Override
    public void removed() {
        restoreGuiScale();
        super.removed();
    }

    private void lockGuiScale() {
        if (minecraft == null) {
            return;
        }
        if (originalGuiScale == -1) {
            originalGuiScale = minecraft.options.guiScale().get();
            lockedGuiScale = (int) minecraft.getWindow().getGuiScale();
        }
        minecraft.options.guiScale().set(lockedGuiScale);
    }

    private void restoreGuiScale() {
        if (minecraft == null || originalGuiScale == -1) {
            return;
        }
        minecraft.options.guiScale().set(originalGuiScale);
        originalGuiScale = -1;
        lockedGuiScale = -1;
    }

    private int calculateImageHeight() {
        int offersTop = getOffersStartY();
        int columns = Math.max(1, getColumns());
        int rowCount = (int) Math.ceil((double) cards.size() / columns);
        int height = offersTop - topPos;
        if (rowCount > 0) {
            height += rowCount * CARD_HEIGHT;
            height += (rowCount - 1) * GRID_GAP;
        }
        height += PADDING;
        return Math.max(height, 140);
    }

    private int getCategoryButtonsStartY() {
        return topPos + PADDING + font.lineHeight + CATEGORY_TITLE_GAP;
    }

    private int getCategoryColumnWidth() {
        return Math.max((int) (imageWidth * CATEGORY_COLUMN_RATIO), 140);
    }

    private int getShopColumnLeft() {
        return leftPos + getCategoryColumnWidth() + COLUMN_GAP;
    }

    private int getShopColumnWidth() {
        return Math.max(imageWidth - getCategoryColumnWidth() - COLUMN_GAP, 160);
    }

    private int getOffersStartY() {
        int categoryButtonSpace = Math.max(0, categories.size() * (CATEGORY_BUTTON_HEIGHT + BUTTON_GAP) - BUTTON_GAP);
        int categoryBottom = getCategoryButtonsStartY() + categoryButtonSpace;
        return Math.max(topPos + SECTION_TOP, categoryBottom + PADDING);
    }

    private List<OfferCard> buildCards() {
        List<OfferCard> result = new ArrayList<>();
        int contentLeft = getShopColumnLeft() + PADDING;
        int currentX = contentLeft;
        int currentY = getOffersStartY();
        int columns = Math.max(1, getColumns());
        int columnIndex = 0;

        for (int i = 0; i < menu.getOffers().size(); i++) {
            ConfiguredOffer offer = menu.getOffers().get(i);
            if (!Objects.equals(offer.category(), selectedCategory)) {
                continue;
            }
            result.add(new OfferCard(i, currentX, currentY));

            columnIndex++;
            if (columnIndex >= columns) {
                columnIndex = 0;
                currentX = contentLeft;
                currentY += CARD_HEIGHT + GRID_GAP;
            } else {
                currentX += CARD_WIDTH + GRID_GAP;
            }
        }
        return result;
    }

    private int calculateCurrencyRowWidth(List<PriceRequirement> requirements) {
        if (requirements.isEmpty()) {
            return 0;
        }
        return (requirements.size() * 16) + ((requirements.size() - 1) * CURRENCY_GAP);
    }

    private int calculateCurrencyTotalsWidth(List<ConfiguredCurrency> currencies) {
        int total = 0;
        for (int i = 0; i < currencies.size(); i++) {
            ConfiguredCurrency currency = currencies.get(i);
            Component totalLine = Component.literal("x " + menu.getCurrencyCount(currency));
            total += 16 + 4 + font.width(totalLine);
            if (i < currencies.size() - 1) {
                total += CURRENCY_GAP;
            }
        }
        return total;
    }

    private Component buildPriceLine(List<PriceRequirement> requirements, int quantity) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < requirements.size(); i++) {
            PriceRequirement requirement = requirements.get(i);
            builder.append("x ").append(quantity * requirement.price());
            if (i < requirements.size() - 1) {
                builder.append("  |  ");
            }
        }
        return Component.literal(builder.toString());
    }

    private int getColumns() {
        int contentWidth = getShopColumnWidth() - (PADDING * 2);
        return Math.max(1, (contentWidth + GRID_GAP) / (CARD_WIDTH + GRID_GAP));
    }

    private int getPriceRowY(int cardStartY) {
        return cardStartY + CARD_PADDING + font.lineHeight + 2 + ITEM_ICON_SIZE + 6 + 18;
    }

    private int calculateMaxAffordable(ConfiguredOffer offer) {
        int maxAffordable = Integer.MAX_VALUE;
        for (PriceRequirement requirement : offer.prices()) {
            int priceEach = requirement.price();
            if (priceEach <= 0) {
                continue;
            }
            int available = menu.getCurrencyCount(requirement.currency());
            maxAffordable = Math.min(maxAffordable, available / priceEach);
        }
        if (maxAffordable == Integer.MAX_VALUE) {
            return 0;
        }
        return Math.max(0, maxAffordable);
    }

    private class SolidButton extends Button {
        SolidButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int background = isHoveredOrFocused() ? BUTTON_HOVER_COLOR : BUTTON_BASE_COLOR;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
            int textY = getY() + (height - font.lineHeight) / 2;
            int textColor = active ? 0xFF111111 : 0xFF555555;
            graphics.drawCenteredString(font, getMessage(), getX() + (width / 2), textY, textColor);
        }
    }

    private record OfferCard(int offerIndex, int startX, int startY) {
    }
}
