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

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final int PADDING = 8;
    private static final int SECTION_TOP = 64;
    private static final int GRID_GAP = 12;
    private static final int BUTTON_WIDTH = 18;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;
    private static final int PURCHASE_BUTTON_WIDTH = 100;
    private static final int CATEGORY_BUTTON_HEIGHT = 26;
    private static final int CATEGORY_TITLE_GAP = 6;
    private static final int CATEGORY_BUTTON_Y_OFFSET = 18;
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
    private static final int BUTTON_BASE_COLOR = 0xFF6E6E6E;
    private static final int BUTTON_HOVER_COLOR = 0xFF9C9C9C;
    private static final int BUTTON_DISABLED_COLOR = 0xFF3A3A3A;
    private static final int BUTTON_DISABLED_TEXT_COLOR = 0xFF7A7A7A;
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
        minusButtons.clear();
        plusButtons.clear();
        purchaseButtons.clear();
        offerButtons.clear();
        categoryButtons.clear();

        if (!categories.contains(selectedCategory) && !categories.isEmpty()) {
            selectedCategory = categories.get(0);
        }

        int contentHeight = calculateContentHeight();
        int maxHeight = this.height - (PADDING * 2);
        int targetHeight = (int) (this.height * GUI_HEIGHT_RATIO);
        this.imageHeight = Mth.clamp(Math.min(calculateImageHeight(contentHeight), targetHeight), 140, maxHeight);
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
        int startX = leftPos + PADDING;
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

        selectedQuantities[index] = Math.max(1, selectedQuantities[index] + delta);
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
            int clampedQuantity = Mth.clamp(selectedQuantities[i], 1, Math.max(1, maxAffordable));
            selectedQuantities[i] = clampedQuantity;
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
        int barBottom = topPos + imageHeight - PADDING;
        int trackHeight = barBottom - barTop;

        graphics.fill(barX, barTop, barX + SCROLLBAR_WIDTH, barBottom, 0x66000000);

        int thumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT, (int) ((visibleHeight / (double) contentHeight) * trackHeight));
        thumbHeight = Math.min(thumbHeight, trackHeight);
        int thumbAvailable = trackHeight - thumbHeight;
        int thumbOffset = maxScroll == 0 ? 0 : (int) ((scrollOffset / maxScroll) * thumbAvailable);
        int thumbTop = barTop + thumbOffset;
        graphics.fill(barX, thumbTop, barX + SCROLLBAR_WIDTH, thumbTop + thumbHeight, BUTTON_HOVER_COLOR);
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
        int barBottom = topPos + imageHeight - PADDING;
        int trackHeight = barBottom - barTop;

        graphics.fill(barX, barTop, barX + SCROLLBAR_WIDTH, barBottom, 0x66000000);

        int thumbHeight = Math.max(MIN_SCROLLBAR_HEIGHT, (int) ((visibleHeight / (double) contentHeight) * trackHeight));
        thumbHeight = Math.min(thumbHeight, trackHeight);
        int thumbAvailable = trackHeight - thumbHeight;
        int thumbOffset = categoryMaxScroll == 0 ? 0 : (int) ((categoryScrollOffset / categoryMaxScroll) * thumbAvailable);
        int thumbTop = barTop + thumbOffset;
        graphics.fill(barX, thumbTop, barX + SCROLLBAR_WIDTH, thumbTop + thumbHeight, BUTTON_HOVER_COLOR);
    }

    private void renderOfferDetails(GuiGraphics graphics) {
        if (maxScroll > 0) {
            graphics.enableScissor(getShopColumnLeft(), getOffersStartY(), leftPos + imageWidth - PADDING, topPos + imageHeight - PADDING);
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
        int offersBottom = topPos + imageHeight - PADDING;
        return Math.max(0, offersBottom - offersTop);
    }

    private int getVisibleCategoryHeight() {
        int categoryTop = getCategoryButtonsStartY();
        int categoryBottom = topPos + imageHeight - PADDING;
        return Math.max(0, categoryBottom - categoryTop);
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
        return calculateImageHeight(calculateContentHeight());
    }

    private int calculateImageHeight(int contentHeight) {
        int headerHeight = PADDING + font.lineHeight + CATEGORY_TITLE_GAP + CATEGORY_BUTTON_Y_OFFSET;
        int height = headerHeight + contentHeight + PADDING;
        return Math.max(height, 140);
    }

    private int getCategoryButtonsStartY() {
        return topPos + PADDING + font.lineHeight + CATEGORY_TITLE_GAP + CATEGORY_BUTTON_Y_OFFSET;
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
        int offersBottom = topPos + imageHeight - PADDING;
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
        int viewportBottom = topPos + imageHeight - PADDING;

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
        int viewportBottom = topPos + imageHeight - PADDING;

        int right = x + width;
        int bottom = y + height;
        return right > viewportLeft && x < viewportRight && bottom > viewportTop && y < viewportBottom;
    }

    private boolean isWithinCategoryColumn(double mouseX, double mouseY) {
        int columnLeft = leftPos;
        int columnRight = leftPos + getCategoryColumnWidth();
        int columnTop = topPos;
        int columnBottom = topPos + imageHeight;
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

        Map<ConfiguredCurrency, Integer> reserved = calculateReservedCurrencyExcluding(offerIndex);
        return calculateMaxAffordable(offers.get(offerIndex), reserved);
    }

    private Map<ConfiguredCurrency, Integer> calculateReservedCurrencyExcluding(int excludedIndex) {
        Map<ConfiguredCurrency, Integer> reserved = new HashMap<>();
        List<ConfiguredOffer> offers = menu.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            if (i == excludedIndex) {
                continue;
            }
            if (i >= selectedQuantities.length) {
                break;
            }
            int quantity = selectedQuantities[i];
            if (quantity <= 0) {
                continue;
            }
            int maxAffordable = calculateMaxAffordable(offers.get(i), reserved);
            if (maxAffordable <= 0) {
                continue;
            }
            int effectiveQuantity = Math.min(quantity, maxAffordable);
            for (Map.Entry<ConfiguredCurrency, Integer> entry : Config.aggregatePriceRequirements(offers.get(i).prices())
                    .entrySet()) {
                int total = entry.getValue() * effectiveQuantity;
                if (total > 0) {
                    reserved.merge(entry.getKey(), total, Integer::sum);
                }
            }
        }
        return reserved;
    }

    private int calculateMaxAffordable(ConfiguredOffer offer, Map<ConfiguredCurrency, Integer> reserved) {
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
            int available = menu.getCurrencyCount(entry.getKey()) - reserved.getOrDefault(entry.getKey(), 0);
            maxAffordable = Math.min(maxAffordable, Math.max(0, available) / priceEach);
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
            if (!isWithinOffersViewport(this)) {
                return;
            }

            int viewportLeft = getShopColumnLeft();
            int viewportRight = leftPos + imageWidth - PADDING;
            int viewportTop = getOffersStartY();
            int viewportBottom = topPos + imageHeight - PADDING;

            graphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
            boolean hovered = active && isHoveredOrFocused();
            int background = !active ? BUTTON_DISABLED_COLOR : hovered ? BUTTON_HOVER_COLOR : BUTTON_BASE_COLOR;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
            int textY = getY() + (height - font.lineHeight) / 2;
            int textColor = active ? 0xFFFFFFFF : BUTTON_DISABLED_TEXT_COLOR;
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
            int viewportBottom = topPos + imageHeight - PADDING;

            graphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
            int background = isHoveredOrFocused() ? BUTTON_HOVER_COLOR : BUTTON_BASE_COLOR;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, background);

            int textY = getY() + (height - font.lineHeight) / 2;
            int textX = getX() + TEXT_PADDING;
            int textColor = isSelected() ? 0xFFFFFFFF : 0xFFB5B5B5;
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
