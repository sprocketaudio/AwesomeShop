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
import net.sprocketaudio.awesomeshop.Config;
import net.sprocketaudio.awesomeshop.Config.ConfiguredCurrency;
import net.sprocketaudio.awesomeshop.Config.ConfiguredOffer;
import net.sprocketaudio.awesomeshop.Config.PriceRequirement;
import net.sprocketaudio.awesomeshop.ShopStyleConfig;
import net.sprocketaudio.awesomeshop.ShopStyleConfig.ShopStyle;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final int PADDING = 8;
    private static final int GRID_GAP = 12;
    private static final int BUTTON_WIDTH = 18;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;
    private static final int CATEGORY_BUTTON_HEIGHT = 26;
    private static final int CATEGORY_TITLE_GAP = 6;
    private static final int CATEGORY_BUTTON_Y_OFFSET = 18;
    private static final int COLUMN_GAP = 10;
    private static final int MIN_IMAGE_WIDTH = 320;
    private static final int TITLE_BOX_PADDING = 6;
    private static final int TITLE_BOX_GAP = 8;
    private static final float GUI_WIDTH_RATIO = 0.8f;
    private static final float GUI_HEIGHT_RATIO = 0.8f;
    private static final float CATEGORY_COLUMN_RATIO = 0.25f;
    private static final int CARD_WIDTH = 170;
    private static final int CARD_HEIGHT = 100;
    private static final int CARD_PADDING = 8;
    private static final int CURRENCY_GAP = 8;
    private static final int ITEM_ICON_SIZE = 24;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int SCROLLBAR_MARGIN = 6;
    private static final int MIN_SCROLLBAR_HEIGHT = 12;
    private static final double SCROLL_SPEED = 12.0d;

    private int lockedGuiScale = -1;
    private int originalGuiScale = -1;

    private final int[] selectedQuantities;
    private final Map<Integer, Button> minusButtons = new HashMap<>();
    private final Map<Integer, Button> plusButtons = new HashMap<>();
    private final Map<Integer, Button> purchaseButtons = new HashMap<>();
    private final List<Button> offerButtons = new ArrayList<>();
    private final Map<String, Button> categoryButtons = new HashMap<>();
    private final List<String> categories;
    private String selectedCategory;
    private List<OfferCard> cards = List.of();
    private double scrollOffset;
    private double maxScroll;
    private double categoryScrollOffset;
    private double categoryMaxScroll;
    private ShopStyle style;

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
        this.style = ShopStyleConfig.getStyle();
    }

    @Override
    protected void init() {
        super.init();
        this.style = ShopStyleConfig.getStyle();
        lockGuiScale();
        recalculateDimensions();
        rebuildLayout();
    }

    private void recalculateDimensions() {
        this.imageWidth = Math.max(MIN_IMAGE_WIDTH, (int) (this.width * GUI_WIDTH_RATIO));
        this.leftPos = (this.width - this.imageWidth) / 2;
    }

    private int getTopRowY() {
        return getMainAreaTop() + PADDING + getGuiBorderThickness();
    }

    private int getGuiBorderThickness() {
        return Math.max(0, style.guiBorderThickness());
    }

    private int getCardBorderThickness() {
        return Math.max(0, style.cardPanelBorderThickness());
    }

    private int getCategoryButtonBorderThickness() {
        return Math.max(0, style.categoryButtonBorderThickness());
    }

    private int getButtonBorderThickness() {
        return Math.max(0, style.cardButtonBorderThickness());
    }

    private int getTitleBorderThickness() {
        return Math.max(0, style.titleBorderThickness());
    }

    private float getTitleFontScale() {
        return Mth.clamp(style.titleFontScale(), 0.5f, 3.0f);
    }

    private float getCategoryTitleScale() {
        return Mth.clamp(style.categoryTitleFontScale(), 0.5f, 3.0f);
    }

    private int getTitleFontHeight() {
        return getScaledFontHeight(getTitleFontScale());
    }

    private int getCategoryTitleHeight() {
        return getScaledFontHeight(getCategoryTitleScale());
    }

    private int getScaledFontHeight(float scale) {
        return (int) Math.ceil(font.lineHeight * scale);
    }

    private int getTitleBoxHeight() {
        int border = getTitleBorderThickness();
        return (TITLE_BOX_PADDING * 2) + (border * 2) + getTitleFontHeight();
    }

    private int getMainAreaTop() {
        return topPos + getTitleBoxHeight() + TITLE_BOX_GAP;
    }

    private int getMainAreaHeight() {
        return Math.max(0, imageHeight - getTitleBoxHeight() - TITLE_BOX_GAP);
    }

    private int getMainAreaBottom() {
        return getMainAreaTop() + getMainAreaHeight();
    }

    private void rebuildLayout() {
        clearWidgets();
        minusButtons.clear();
        plusButtons.clear();
        purchaseButtons.clear();
        offerButtons.clear();
        categoryButtons.clear();

        if (!categories.contains(selectedCategory) && !categories.isEmpty()) {
            selectedCategory = categories.get(0);
        }

        int contentHeight = calculateContentHeight();
        int desiredHeight = calculateImageHeight(contentHeight);
        int maxHeight = this.height - (PADDING * 2);
        int ratioHeight = (int) (this.height * GUI_HEIGHT_RATIO);
        this.imageHeight = Math.max(140, Math.min(desiredHeight, Math.min(maxHeight, ratioHeight)));
        this.topPos = Math.max(PADDING, (this.height - this.imageHeight) / 2);
        updateScrollBounds(contentHeight);
        updateCategoryScrollBounds();
        this.cards = buildCards();

        placeCategoryButtons();
        placeOfferButtons();
        reconcileQuantities();
    }

    private void placeOfferButtons() {
        for (OfferCard card : cards) {
            int index = card.offerIndex();
            int priceRowY = getPriceRowY(card.startY());
            int quantityButtonY = priceRowY - Math.max(0, (BUTTON_HEIGHT - font.lineHeight) / 2) - 1;
            int minusX = card.startX() + CARD_PADDING;
            int plusX = card.startX() + CARD_WIDTH - CARD_PADDING - BUTTON_WIDTH;
            boolean visible = isCardVisible(card);

            Button minusButton = createTintedButton(minusX, quantityButtonY, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("-"),
                    b -> adjustQuantity(index, -1));
            minusButton.visible = visible;
            minusButtons.put(index, minusButton);
            offerButtons.add(addRenderableWidget(minusButton));

            Button plusButton = createTintedButton(plusX, quantityButtonY, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("+"),
                    b -> adjustQuantity(index, 1));
            plusButton.visible = visible;
            plusButtons.put(index, plusButton);
            offerButtons.add(addRenderableWidget(plusButton));

            int purchaseY = quantityButtonY + BUTTON_HEIGHT + BUTTON_GAP;
            Button purchaseButton = createTintedButton(card.startX() + CARD_PADDING, purchaseY, CARD_WIDTH - (CARD_PADDING * 2),
                    BUTTON_HEIGHT, Component.literal(""), b -> purchaseOffer(index));
            purchaseButton.visible = visible;
            Button widget = addRenderableWidget(purchaseButton);
            purchaseButtons.put(index, widget);
            offerButtons.add(widget);
            int maxAffordable = calculateMaxAffordable(index);
            updatePurchaseButton(index, maxAffordable);
        }
    }

    private void placeCategoryButtons() {
        int buttonWidth = getCategoryButtonWidth();
        int startX = leftPos + ((getCategoryColumnWidth() - buttonWidth) / 2);
        int startY = getCategoryButtonsStartY() - (int) categoryScrollOffset;

        for (String category : categories) {
            Button button = createCategoryButton(startX, startY, buttonWidth, CATEGORY_BUTTON_HEIGHT, category,
                    b -> selectCategory(category));
            categoryButtons.put(category, addRenderableWidget(button));
            startY += CATEGORY_BUTTON_HEIGHT + BUTTON_GAP;
        }
        updateCategoryButtonStates();
    }

    private void purchaseOffer(int index) {
        if (minecraft != null && minecraft.gameMode != null) {
            int quantity = selectedQuantities[index];
            int maxAffordable = calculateMaxAffordable(index);
            if (quantity < 1 || maxAffordable < quantity) {
                selectedQuantities[index] = Math.max(1, Math.min(quantity, Math.max(1, maxAffordable)));
                reconcileQuantities();
                return;
            }
            int buttonId = (quantity * menu.getOffers().size()) + index;
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
            ConfiguredOffer offer = menu.getOffers().get(index);
            selectedQuantities[index] = Math.max(1, Math.min(quantity, Math.max(1, maxAffordable)));
            reconcileQuantities();
        }
    }

    private void adjustQuantity(int index, int delta) {
        List<ConfiguredOffer> offers = menu.getOffers();
        if (index < 0 || index >= offers.size()) {
            return;
        }

        int maxAffordable = calculateMaxAffordable(index);
        if (delta > 0 && maxAffordable < 1) {
            return;
        }

        int updated = Math.max(1, selectedQuantities[index] + delta);
        if (delta > 0) {
            updated = Math.min(updated, Math.max(1, maxAffordable));
        }

        selectedQuantities[index] = updated;
        reconcileQuantities();
    }

    private void selectCategory(String category) {
        if (!Objects.equals(selectedCategory, category) && categories.contains(category)) {
            selectedCategory = category;
            scrollOffset = 0;
            categoryScrollOffset = 0;
            rebuildLayout();
        }
    }

    private void updateCategoryButtonStates() {
        categoryButtons.forEach((category, button) -> button.active = !Objects.equals(category, selectedCategory));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        reconcileQuantities();
    }

    private void updatePurchaseButton(int index, int maxAffordable) {
        if (index < 0 || !purchaseButtons.containsKey(index)) {
            return;
        }
        Button button = purchaseButtons.get(index);
        int quantity = selectedQuantities[index];
        ConfiguredOffer offer = menu.getOffers().get(index);
        button.setMessage(Component.translatable("screen.awesomeshop.shop_block.buy", quantity, offer.item().getHoverName()));
        button.active = maxAffordable >= quantity && quantity >= 1;
    }

    private void reconcileQuantities() {
        for (int i = 0; i < selectedQuantities.length; i++) {
            int maxAffordable = calculateMaxAffordable(i);
            selectedQuantities[i] = Math.max(1, selectedQuantities[i]);
            updatePurchaseButton(i, maxAffordable);
            updateQuantityButtons(i, maxAffordable);
        }
    }

    private void updateQuantityButtons(int index, int maxAffordable) {
        Button minusButton = minusButtons.get(index);
        Button plusButton = plusButtons.get(index);
        int quantity = index < selectedQuantities.length ? selectedQuantities[index] : 0;
        if (minusButton != null) {
            minusButton.active = quantity > 1;
        }
        if (plusButton != null) {
            plusButton.active = quantity < maxAffordable;
        }
    }

    private Button createTintedButton(int x, int y, int width, int height, Component label, Button.OnPress onPress) {
        return new SolidButton(x, y, width, height, label, onPress);
    }

    private Button createCategoryButton(int x, int y, int width, int height, String category, Button.OnPress onPress) {
        return new CategoryButton(x, y, width, height, Component.literal(category), onPress, category);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTitleBox(graphics);
        renderPanels(graphics);
        renderOfferDetails(graphics);
        renderScrollbar(graphics);
        renderCategoryScrollbar(graphics);
        refreshOfferButtonVisibility();
        refreshCategoryButtonVisibility();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCategoryPanel(graphics);
        renderCurrencyTotals(graphics);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void renderPanels(GuiGraphics graphics) {
        int mainTop = getMainAreaTop();
        int mainBottom = getMainAreaBottom();
        int right = leftPos + imageWidth;
        int categoryRight = leftPos + getCategoryColumnWidth();
        int borderThickness = getGuiBorderThickness();
        int innerLeft = leftPos + borderThickness;
        int innerTop = mainTop + borderThickness;
        int innerRight = right - borderThickness;
        int innerBottom = mainBottom - borderThickness;

        // Panel frame
        graphics.fill(leftPos, mainTop, right, mainTop + borderThickness, style.guiBorderColor());
        graphics.fill(leftPos, mainBottom - borderThickness, right, mainBottom, style.guiBorderColor());
        graphics.fill(leftPos, mainTop, leftPos + borderThickness, mainBottom, style.guiBorderColor());
        graphics.fill(right - borderThickness, mainTop, right, mainBottom, style.guiBorderColor());

        int dividerX = Math.min(right - borderThickness, categoryRight + (COLUMN_GAP / 2));

        int categoryBackgroundRight = Math.min(innerRight, dividerX);
        graphics.fill(innerLeft, innerTop, categoryBackgroundRight, innerBottom, style.categoryPanelBackground());

        int itemBackgroundLeft = Math.max(innerLeft, dividerX);
        if (itemBackgroundLeft < innerRight) {
            graphics.fill(itemBackgroundLeft, innerTop, innerRight, innerBottom, style.itemPanelBackground());
        }

        graphics.fill(dividerX, innerTop, dividerX + borderThickness, innerBottom, style.guiBorderColor());
    }

    private void renderCategoryPanel(GuiGraphics graphics) {
        int titleX = leftPos + PADDING;
        int titleY = getTopRowY();
        drawScaledString(graphics, Component.literal("Categories"), titleX, titleY, getCategoryTitleScale(),
                style.categoryTitleTextColor());
    }

    private void renderScrollbar(GuiGraphics graphics) {
        if (maxScroll <= 0) {
            return;
        }

        int contentHeight = calculateContentHeight();
        int visibleHeight = getVisibleOffersHeight();
        if (visibleHeight <= 0 || contentHeight <= 0) {
            return;
        }

        int barX = leftPos + imageWidth - PADDING - SCROLLBAR_WIDTH;
        int barTop = getOffersStartY();
        int barBottom = getMainAreaBottom() - PADDING;
        int trackHeight = barBottom - barTop;

        graphics.fill(barX, barTop, barX + SCROLLBAR_WIDTH, barBottom, 0x66000000);

        int thumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT, (int) ((visibleHeight / (double) contentHeight) * trackHeight));
        thumbHeight = Math.min(thumbHeight, trackHeight);
        int thumbAvailable = trackHeight - thumbHeight;
        int thumbOffset = maxScroll == 0 ? 0 : (int) ((scrollOffset / maxScroll) * thumbAvailable);
        int thumbTop = barTop + thumbOffset;
        graphics.fill(barX, thumbTop, barX + SCROLLBAR_WIDTH, thumbTop + thumbHeight, style.cardButtonHoverBackground());
    }

    private void renderCategoryScrollbar(GuiGraphics graphics) {
        if (categoryMaxScroll <= 0) {
            return;
        }

        int contentHeight = calculateCategoryContentHeight();
        int visibleHeight = getVisibleCategoryHeight();
        if (visibleHeight <= 0 || contentHeight <= 0) {
            return;
        }

        int barX = leftPos + getCategoryColumnWidth() - PADDING - SCROLLBAR_WIDTH;
        int barTop = getCategoryButtonsStartY();
        int barBottom = getMainAreaBottom() - PADDING;
        int trackHeight = barBottom - barTop;

        graphics.fill(barX, barTop, barX + SCROLLBAR_WIDTH, barBottom, 0x66000000);

        int thumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT, (int) ((visibleHeight / (double) contentHeight) * trackHeight));
        thumbHeight = Math.min(thumbHeight, trackHeight);
        int thumbAvailable = trackHeight - thumbHeight;
        int thumbOffset = categoryMaxScroll == 0 ? 0 : (int) ((categoryScrollOffset / categoryMaxScroll) * thumbAvailable);
        int thumbTop = barTop + thumbOffset;
        graphics.fill(barX, thumbTop, barX + SCROLLBAR_WIDTH, thumbTop + thumbHeight, style.cardButtonHoverBackground());
    }

    private void renderOfferDetails(GuiGraphics graphics) {
        if (maxScroll > 0) {
            graphics.enableScissor(getShopColumnLeft(), getOffersStartY(), leftPos + imageWidth - PADDING,
                    getMainAreaBottom() - PADDING);
        }

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

            int cardBorder = getCardBorderThickness();
            graphics.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT, style.cardPanelBackground());
            graphics.fill(cardX, cardY, cardX + CARD_WIDTH, cardY + cardBorder, style.cardPanelBorderColor());
            graphics.fill(cardX, cardY + CARD_HEIGHT - cardBorder, cardX + CARD_WIDTH, cardY + CARD_HEIGHT,
                    style.cardPanelBorderColor());
            graphics.fill(cardX, cardY, cardX + cardBorder, cardY + CARD_HEIGHT, style.cardPanelBorderColor());
            graphics.fill(cardX + CARD_WIDTH - cardBorder, cardY, cardX + CARD_WIDTH, cardY + CARD_HEIGHT,
                    style.cardPanelBorderColor());

            Component itemName = offer.item().getHoverName();
            int nameY = cardY + CARD_PADDING;
            graphics.drawCenteredString(font, itemName, cardCenterX, nameY, style.cardItemTextColor());

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

            List<Integer> currencyCenters = new ArrayList<>(requirements.size());
            for (PriceRequirement requirement : requirements) {
                ItemStack currencyStack = new ItemStack(requirement.currency().item());
                graphics.renderItem(currencyStack, currentX, currenciesY);
                graphics.renderItemDecorations(font, currencyStack, currentX, currenciesY);
                currencyCenters.add(currentX + 8);
                currentX += 16 + CURRENCY_GAP;
            }

            int quantity = selectedQuantities[index];
            int priceY = getPriceRowY(cardY);
            for (int i = 0; i < requirements.size(); i++) {
                PriceRequirement requirement = requirements.get(i);
                String costText = Integer.toString(quantity * requirement.price());
                graphics.drawCenteredString(font, Component.literal(costText), currencyCenters.get(i), priceY, 0xDDDDDD);
            }
        }

        if (maxScroll > 0) {
            graphics.disableScissor();
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
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isWithinCategoryColumn(mouseX, mouseY)) {
            setCategoryScrollOffset(categoryScrollOffset - (deltaY * SCROLL_SPEED));
            return true;
        }

        setScrollOffset(scrollOffset - (deltaY * SCROLL_SPEED));
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void setScrollOffset(double offset) {
        double clamped = Mth.clamp(offset, 0, maxScroll);
        if (clamped != scrollOffset) {
            scrollOffset = clamped;
            rebuildLayout();
        }
    }

    private void setCategoryScrollOffset(double offset) {
        double clamped = Mth.clamp(offset, 0, categoryMaxScroll);
        if (clamped != categoryScrollOffset) {
            categoryScrollOffset = clamped;
            rebuildLayout();
        }
    }

    private int getVisibleOffersHeight() {
        int offersTop = getOffersStartY();
        int offersBottom = getMainAreaBottom() - PADDING;
        return Math.max(0, offersBottom - offersTop);
    }

    private int getVisibleCategoryHeight() {
        int categoryTop = getCategoryButtonsStartY();
        int categoryBottom = getMainAreaBottom() - PADDING;
        return Math.max(0, categoryBottom - categoryTop);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private void renderTitleBox(GuiGraphics graphics) {
        int boxTop = topPos;
        int boxBottom = boxTop + getTitleBoxHeight();
        int boxLeft = leftPos;
        int boxRight = leftPos + imageWidth;

        graphics.fill(boxLeft, boxTop, boxRight, boxBottom, style.titleBackgroundColor());

        int borderThickness = getTitleBorderThickness();
        if (borderThickness > 0) {
            graphics.fill(boxLeft, boxTop, boxRight, boxTop + borderThickness, style.titleBorderColor());
            graphics.fill(boxLeft, boxBottom - borderThickness, boxRight, boxBottom, style.titleBorderColor());
            graphics.fill(boxLeft, boxTop, boxLeft + borderThickness, boxBottom, style.titleBorderColor());
            graphics.fill(boxRight - borderThickness, boxTop, boxRight, boxBottom, style.titleBorderColor());
        }

        int textY = boxTop + ((getTitleBoxHeight() - getTitleFontHeight()) / 2);
        int centerX = boxLeft + (imageWidth / 2);
        drawScaledCenteredString(graphics, title, centerX, textY, getTitleFontScale(), style.titleTextColor());
    }

    private void drawScaledCenteredString(GuiGraphics graphics, Component text, int centerX, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawCenteredString(font, text, 0, 0, color);
        graphics.pose().popPose();
    }

    private void drawScaledString(GuiGraphics graphics, Component text, int x, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
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
        return calculateImageHeight(calculateContentHeight());
    }

    private int calculateImageHeight(int contentHeight) {
        int headerHeight = PADDING + getGuiBorderThickness() + getCategoryTitleHeight() + CATEGORY_TITLE_GAP
                + CATEGORY_BUTTON_Y_OFFSET;
        int mainPanelHeight = headerHeight + contentHeight + PADDING;
        return getTitleBoxHeight() + TITLE_BOX_GAP + Math.max(mainPanelHeight, 140);
    }

    private int getCategoryButtonsStartY() {
        return getTopRowY() + getCategoryTitleHeight() + CATEGORY_TITLE_GAP + CATEGORY_BUTTON_Y_OFFSET;
    }

    private int getCategoryColumnWidth() {
        return Math.max((int) (imageWidth * CATEGORY_COLUMN_RATIO), 140);
    }

    private int getCategoryButtonWidth() {
        int buttonWidth = getCategoryColumnWidth() - (PADDING * 2);
        if (categoryMaxScroll > 0) {
            buttonWidth -= SCROLLBAR_WIDTH + SCROLLBAR_MARGIN;
        }
        return buttonWidth;
    }

    private int getShopColumnLeft() {
        return leftPos + getCategoryColumnWidth() + COLUMN_GAP;
    }

    private int getShopColumnWidth() {
        return Math.max(imageWidth - getCategoryColumnWidth() - COLUMN_GAP, 160);
    }

    private int getOffersStartY() {
        return getCategoryButtonsStartY();
    }

    private List<OfferCard> buildCards() {
        List<OfferCard> result = new ArrayList<>();
        int contentLeft = getShopColumnLeft() + PADDING;
        int currentX = contentLeft;
        int currentY = getOffersStartY() - (int) scrollOffset;
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

    private int calculateContentHeight() {
        int offerCount = 0;
        for (ConfiguredOffer offer : menu.getOffers()) {
            if (Objects.equals(offer.category(), selectedCategory)) {
                offerCount++;
            }
        }
        int columns = Math.max(1, getColumns());
        int rows = (int) Math.ceil((double) offerCount / columns);
        if (rows == 0) {
            return 0;
        }
        return (rows * CARD_HEIGHT) + ((rows - 1) * GRID_GAP);
    }

    private void updateScrollBounds(int contentHeight) {
        int visibleHeight = getVisibleOffersHeight();
        maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
    }

    private void updateCategoryScrollBounds() {
        int contentHeight = calculateCategoryContentHeight();
        int visibleHeight = getVisibleCategoryHeight();
        categoryMaxScroll = Math.max(0, contentHeight - visibleHeight);
        categoryScrollOffset = Mth.clamp(categoryScrollOffset, 0, categoryMaxScroll);
    }

    private int calculateCategoryContentHeight() {
        if (categories.isEmpty()) {
            return 0;
        }
        return (categories.size() * CATEGORY_BUTTON_HEIGHT) + ((categories.size() - 1) * BUTTON_GAP);
    }

    private int calculateCurrencyRowWidth(List<PriceRequirement> requirements) {
        if (requirements.isEmpty()) {
            return 0;
        }
        return (requirements.size() * 16) + ((requirements.size() - 1) * CURRENCY_GAP);
    }

    private boolean isCardVisible(OfferCard card) {
        int cardTop = card.startY();
        int cardBottom = cardTop + CARD_HEIGHT;
        int offersTop = getOffersStartY();
        int offersBottom = getMainAreaBottom() - PADDING;
        return cardBottom > offersTop && cardTop < offersBottom;
    }

    private void refreshOfferButtonVisibility() {
        for (Button button : offerButtons) {
            button.visible = isWithinOffersViewport(button);
        }
    }

    private void refreshCategoryButtonVisibility() {
        for (Button button : categoryButtons.values()) {
            button.visible = isWithinCategoryViewport(button);
        }
    }

    private boolean isWithinOffersViewport(Button button) {
        return isWithinOffersViewport(button.getX(), button.getY(), button.getWidth(), button.getHeight());
    }

    private boolean isWithinOffersViewport(int x, int y, int width, int height) {
        int viewportLeft = getShopColumnLeft();
        int viewportRight = leftPos + imageWidth - PADDING;
        int viewportTop = getOffersStartY();
        int viewportBottom = getMainAreaBottom() - PADDING;

        int right = x + width;
        int bottom = y + height;
        return right > viewportLeft && x < viewportRight && bottom > viewportTop && y < viewportBottom;
    }

    private boolean isWithinCategoryViewport(Button button) {
        return isWithinCategoryViewport(button.getX(), button.getY(), button.getWidth(), button.getHeight());
    }

    private boolean isWithinCategoryViewport(int x, int y, int width, int height) {
        int viewportLeft = leftPos + PADDING;
        int viewportRight = leftPos + getCategoryColumnWidth() - PADDING;
        int viewportTop = getCategoryButtonsStartY();
        int viewportBottom = getMainAreaBottom() - PADDING;

        int right = x + width;
        int bottom = y + height;
        return right > viewportLeft && x < viewportRight && bottom > viewportTop && y < viewportBottom;
    }

    private boolean isWithinCategoryColumn(double mouseX, double mouseY) {
        int columnLeft = leftPos;
        int columnRight = leftPos + getCategoryColumnWidth();
        int columnTop = getMainAreaTop();
        int columnBottom = getMainAreaBottom();
        return mouseX >= columnLeft && mouseX <= columnRight && mouseY >= columnTop && mouseY <= columnBottom;
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

    private int getColumns() {
        int contentWidth = getShopColumnWidth() - (PADDING * 2) - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
        return Math.max(1, (contentWidth + GRID_GAP) / (CARD_WIDTH + GRID_GAP));
    }

    private int getPriceRowY(int cardStartY) {
        return cardStartY + CARD_PADDING + font.lineHeight + 2 + ITEM_ICON_SIZE + 6 + 18;
    }

    private int calculateMaxAffordable(int offerIndex) {
        List<ConfiguredOffer> offers = menu.getOffers();
        if (offerIndex < 0 || offerIndex >= offers.size()) {
            return 0;
        }

        return calculateMaxAffordable(offers.get(offerIndex));
    }

    private int calculateMaxAffordable(ConfiguredOffer offer) {
        int maxAffordable = Integer.MAX_VALUE;
        Map<ConfiguredCurrency, Integer> prices = Config.aggregatePriceRequirements(offer.prices());
        if (prices.isEmpty()) {
            return 0;
        }
        for (Map.Entry<ConfiguredCurrency, Integer> entry : prices.entrySet()) {
            int priceEach = entry.getValue();
            if (priceEach <= 0) {
                continue;
            }
            int available = menu.getCurrencyCount(entry.getKey());
            maxAffordable = Math.min(maxAffordable, Math.max(0, available) / priceEach);
        }
        if (maxAffordable == Integer.MAX_VALUE) {
            return 0;
        }
        return Math.max(0, maxAffordable);
    }

    private void renderButtonOutline(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int color) {
        if (width <= 0 || height <= 0 || thickness <= 0) {
            return;
        }
        int clampedThickness = Math.max(1, Math.min(thickness, Math.min(width, height) / 2));
        graphics.fill(x, y, x + width, y + clampedThickness, color);
        graphics.fill(x, y + height - clampedThickness, x + width, y + height, color);
        graphics.fill(x, y, x + clampedThickness, y + height, color);
        graphics.fill(x + width - clampedThickness, y, x + width, y + height, color);
    }

    private class SolidButton extends Button {
        SolidButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!isWithinOffersViewport(this)) {
                return;
            }

            int viewportLeft = getShopColumnLeft();
            int viewportRight = leftPos + imageWidth - PADDING;
            int viewportTop = getOffersStartY();
            int viewportBottom = getMainAreaBottom() - PADDING;

            graphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
            boolean hovered = active && isMouseOver(mouseX, mouseY);
            int background = !active ? style.cardButtonDisabledBackground()
                    : hovered ? style.cardButtonHoverBackground() : style.cardButtonBackground();
            graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
            int outlineColor = !active ? style.cardButtonBorderDisabledColor()
                    : hovered ? style.cardButtonBorderHoverColor() : style.cardButtonBorderColor();
            renderButtonOutline(graphics, getX(), getY(), width, height, getButtonBorderThickness(), outlineColor);
            int textY = getY() + (height - font.lineHeight) / 2;
            int textColor = !active ? style.cardButtonTextDisabledColor()
                    : hovered ? style.cardButtonTextHoverColor() : style.cardButtonTextColor();
            graphics.drawCenteredString(font, getMessage(), getX() + (width / 2), textY, textColor);
            graphics.disableScissor();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return super.isMouseOver(mouseX, mouseY)
                    && isWithinOffersViewport((int) mouseX, (int) mouseY, 1, 1);
        }
    }

    private class CategoryButton extends Button {
        private static final int TEXT_PADDING = 6;
        private final String categoryId;

        CategoryButton(int x, int y, int width, int height, Component label, OnPress onPress, String categoryId) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
            this.categoryId = categoryId;
        }

        private boolean isSelected() {
            return Objects.equals(selectedCategory, categoryId);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!isWithinCategoryViewport(this)) {
                return;
            }

            int viewportLeft = leftPos + PADDING;
            int viewportRight = leftPos + getCategoryColumnWidth() - PADDING;
            int viewportTop = getCategoryButtonsStartY();
            int viewportBottom = getMainAreaBottom() - PADDING;

            graphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
            boolean hovered = isHoveredOrFocused();
            int background;
            if (isSelected()) {
                background = style.categoryButtonSelectedBackground();
            } else if (hovered) {
                background = style.categoryButtonHoverBackground();
            } else {
                background = style.categoryButtonBackground();
            }
            graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
            int outlineColor = isSelected() ? style.categoryButtonBorderSelectedColor()
                    : hovered ? style.categoryButtonBorderHoverColor() : style.categoryButtonBorderColor();
            renderButtonOutline(graphics, getX(), getY(), width, height, getCategoryButtonBorderThickness(), outlineColor);

            int textY = getY() + (height - font.lineHeight) / 2;
            int textX = getX() + TEXT_PADDING;
            int textColor = isSelected() ? style.categoryButtonTextSelectedColor()
                    : hovered ? style.categoryButtonTextHoverColor() : style.categoryButtonTextColor();
            graphics.drawString(font, getMessage(), textX, textY, textColor);
            graphics.disableScissor();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return super.isMouseOver(mouseX, mouseY)
                    && isWithinCategoryViewport((int) mouseX, (int) mouseY, 1, 1);
        }
    }

    private record OfferCard(int offerIndex, int startX, int startY) {
    }
}
