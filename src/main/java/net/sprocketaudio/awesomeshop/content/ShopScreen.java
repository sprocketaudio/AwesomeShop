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
    private static final int ROW_GAP = 6;
    private static final int BUTTON_WIDTH = 18;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;
    private static final int PURCHASE_BUTTON_WIDTH = 100;
    private static final int CATEGORY_BUTTON_HEIGHT = 20;
    private static final int CATEGORY_TITLE_GAP = 6;
    private static final int COLUMN_GAP = 10;
    private static final int MIN_IMAGE_WIDTH = 320;
    private static final float GUI_WIDTH_RATIO = 0.9f;
    private static final float CATEGORY_COLUMN_RATIO = 0.25f;

    private int lockedGuiScale = -1;
    private int originalGuiScale = -1;

    private final int[] selectedQuantities;
    private final Map<Integer, Button> purchaseButtons = new HashMap<>();
    private final Map<String, Button> categoryButtons = new HashMap<>();
    private final List<String> categories;
    private String selectedCategory;
    private List<OfferRow> rows = List.of();

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

    private void rebuildLayout() {
        clearWidgets();
        purchaseButtons.clear();
        categoryButtons.clear();

        if (!categories.contains(selectedCategory) && !categories.isEmpty()) {
            selectedCategory = categories.get(0);
        }

        this.rows = buildRows();
        this.imageHeight = calculateImageHeight();
        int targetHeight = (int) (this.height * GUI_WIDTH_RATIO);
        this.imageHeight = Math.max(this.imageHeight, targetHeight);
        this.topPos = Math.max(PADDING, (this.height - this.imageHeight) / 2);
        this.rows = buildRows();

        placeCategoryButtons();
        placeOfferButtons();
    }

    private void placeOfferButtons() {
        int shopLeft = getShopColumnLeft();
        int shopWidth = getShopColumnWidth();

        for (OfferRow row : rows) {
            int index = row.offerIndex();
            int rowY = row.startY();
            int quantityButtonX = shopLeft + shopWidth - PADDING - PURCHASE_BUTTON_WIDTH - BUTTON_WIDTH - 6;
            int plusY = rowY + 2;
            int minusY = plusY + BUTTON_HEIGHT + BUTTON_GAP;

            addRenderableWidget(Button.builder(Component.literal("+"), b -> adjustQuantity(index, 1))
                    .bounds(quantityButtonX, plusY, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());

            addRenderableWidget(Button.builder(Component.literal("-"), b -> adjustQuantity(index, -1))
                    .bounds(quantityButtonX, minusY, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());

            Button purchaseButton = Button.builder(Component.literal(""), b -> purchaseOffer(index))
                    .bounds(shopLeft + shopWidth - PADDING - PURCHASE_BUTTON_WIDTH, rowY, PURCHASE_BUTTON_WIDTH,
                            BUTTON_HEIGHT)
                    .build();
            purchaseButtons.put(index, addRenderableWidget(purchaseButton));
            updatePurchaseButton(index);
        }
    }

    private void placeCategoryButtons() {
        int buttonWidth = getCategoryColumnWidth() - (PADDING * 2);
        int startX = leftPos + PADDING;
        int startY = getCategoryButtonsStartY();

        for (String category : categories) {
            Button button = Button.builder(Component.literal(category), b -> selectCategory(category))
                    .bounds(startX, startY, buttonWidth, CATEGORY_BUTTON_HEIGHT)
                    .build();
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

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderTransparentBackground(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCategoryPanel(graphics);
        renderCurrencyTotals(graphics);
        renderOfferDetails(graphics);
    }

    private void renderCategoryPanel(GuiGraphics graphics) {
        int titleX = leftPos + PADDING;
        int titleY = topPos + PADDING;
        graphics.drawString(font, Component.literal("Categories"), titleX, titleY, 0xFFFFFF);
    }

    private void renderOfferDetails(GuiGraphics graphics) {
        List<ConfiguredOffer> offers = menu.getOffers();
        int shopLeft = getShopColumnLeft();
        int contentLeft = shopLeft + PADDING;

        for (OfferRow row : rows) {
            int index = row.offerIndex();
            if (index < 0 || index >= offers.size()) {
                continue;
            }

            ConfiguredOffer offer = offers.get(index);
            int baseY = row.startY();
            Component itemName = offer.item().getHoverName();

            graphics.renderItem(offer.item(), contentLeft, baseY + 2);
            graphics.renderItemDecorations(font, offer.item(), contentLeft, baseY + 2);
            graphics.drawString(font, itemName, contentLeft + 24, baseY + 4, 0xFFFFFF);

            int currencyY = baseY + font.lineHeight + 6;
            for (PriceRequirement requirement : offer.prices()) {
                ConfiguredCurrency currency = requirement.currency();
                int currencyX = contentLeft + 120;
                int quantity = selectedQuantities[index];
                int totalPrice = quantity * requirement.price();

                ItemStack currencyStack = new ItemStack(currency.item());
                graphics.renderItem(currencyStack, currencyX, currencyY);
                graphics.renderItemDecorations(font, currencyStack, currencyX, currencyY);

                Component totalLine = Component.literal("x " + totalPrice);
                graphics.drawString(font, totalLine, currencyX + 20, currencyY, 0xAAAAAA);

                currencyY += getCurrencyLineHeight();
            }
        }
    }

    private void renderCurrencyTotals(GuiGraphics graphics) {
        List<ConfiguredCurrency> currencies = menu.getCurrencies();
        if (currencies.isEmpty()) {
            return;
        }

        int currencyY = topPos + PADDING;

        for (ConfiguredCurrency currency : currencies) {
            ItemStack currencyStack = new ItemStack(currency.item());
            Component totalLine = Component.literal("x " + menu.getCurrencyCount(currency));
            int lineWidth = 16 + 4 + font.width(totalLine);
            int currencyX = leftPos + imageWidth - PADDING - 4 - lineWidth;
            int textX = currencyX + 16 + 4;

            graphics.renderItem(currencyStack, currencyX, currencyY);
            graphics.renderItemDecorations(font, currencyStack, currencyX, currencyY);

            int textY = currencyY + Math.max(0, (16 - font.lineHeight) / 2);
            graphics.drawString(font, totalLine, textX, textY, 0xFFFFFF);

            currencyY += getCurrencyLineHeight();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = getShopColumnLeft() + (getShopColumnWidth() / 2);
        int titleY = topPos + PADDING;
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
        int height = getOffersStartY() - topPos;
        for (int i = 0; i < rows.size(); i++) {
            height += rows.get(i).height();
            if (i < rows.size() - 1) {
                height += ROW_GAP;
            }
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

    private List<OfferRow> buildRows() {
        List<OfferRow> result = new ArrayList<>();
        int currentY = getOffersStartY();
        for (int i = 0; i < menu.getOffers().size(); i++) {
            ConfiguredOffer offer = menu.getOffers().get(i);
            if (!Objects.equals(offer.category(), selectedCategory)) {
                continue;
            }
            int height = calculateOfferHeight(offer);
            result.add(new OfferRow(i, currentY, height));
            currentY += height + ROW_GAP;
        }
        return result;
    }

    private int calculateOfferHeight(ConfiguredOffer offer) {
        int lines = Math.max(1, offer.prices().size());
        int currencyHeight = lines * getCurrencyLineHeight();
        return Math.max(40, currencyHeight + font.lineHeight + 10);
    }

    private int getCurrencyLineHeight() {
        return Math.max(font.lineHeight, 16) + 6;
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

    private record OfferRow(int offerIndex, int startY, int height) {
    }
}
