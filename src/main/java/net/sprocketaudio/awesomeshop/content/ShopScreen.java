package net.sprocketaudio.awesomeshop.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private int lockedGuiScale = -1;
    private int originalGuiScale = -1;

    private final int[] selectedQuantities;
    private final Map<Integer, Button> purchaseButtons = new HashMap<>();
    private List<OfferRow> rows = List.of();

    public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.selectedQuantities = new int[menu.getOffers().size()];
        Arrays.fill(this.selectedQuantities, 1);
        this.imageWidth = 360;
        this.imageHeight = calculateImageHeight();
    }

    @Override
    protected void init() {
        super.init();
        lockGuiScale();
        this.topPos = PADDING;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.rows = buildRows();
        this.imageHeight = calculateImageHeight();
        clearWidgets();
        purchaseButtons.clear();

        for (OfferRow row : rows) {
            int index = row.offerIndex();
            int rowY = row.startY();
            int quantityButtonX = leftPos + imageWidth - PADDING - PURCHASE_BUTTON_WIDTH - BUTTON_WIDTH - 6;
            int plusY = rowY + 2;
            int minusY = plusY + BUTTON_HEIGHT + BUTTON_GAP;

            addRenderableWidget(Button.builder(Component.literal("+"), b -> adjustQuantity(index, 1))
                    .bounds(quantityButtonX, plusY, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());

            addRenderableWidget(Button.builder(Component.literal("-"), b -> adjustQuantity(index, -1))
                    .bounds(quantityButtonX, minusY, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());

            Button purchaseButton = Button.builder(Component.literal(""), b -> purchaseOffer(index))
                    .bounds(leftPos + imageWidth - PADDING - PURCHASE_BUTTON_WIDTH, rowY, PURCHASE_BUTTON_WIDTH,
                            BUTTON_HEIGHT)
                    .build();
            purchaseButtons.put(index, addRenderableWidget(purchaseButton));
            updatePurchaseButton(index);
        }
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
        renderCurrencyTotals(graphics);
        renderOfferDetails(graphics);
    }

    private void renderOfferDetails(GuiGraphics graphics) {
        List<ConfiguredOffer> offers = menu.getOffers();
        for (OfferRow row : rows) {
            int index = row.offerIndex();
            if (index < 0 || index >= offers.size()) {
                continue;
            }

            ConfiguredOffer offer = offers.get(index);
            int baseY = row.startY();
            Component itemName = offer.item().getHoverName();

            graphics.renderItem(offer.item(), leftPos + PADDING, baseY + 2);
            graphics.renderItemDecorations(font, offer.item(), leftPos + PADDING, baseY + 2);
            graphics.drawString(font, itemName, leftPos + PADDING + 24, baseY + 4, 0xFFFFFF);

            int currencyY = baseY + font.lineHeight + 6;
            for (PriceRequirement requirement : offer.prices()) {
                ConfiguredCurrency currency = requirement.currency();
                int currencyX = leftPos + PADDING + 120;
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

        int currencyX = leftPos + imageWidth - PADDING - 16;
        int currencyY = PADDING;

        for (ConfiguredCurrency currency : currencies) {
            ItemStack currencyStack = new ItemStack(currency.item());
            graphics.renderItem(currencyStack, currencyX, currencyY);
            graphics.renderItemDecorations(font, currencyStack, currencyX, currencyY);

            Component totalLine = Component.literal("x " + menu.getCurrencyCount(currency));
            int textY = currencyY + Math.max(0, (16 - font.lineHeight) / 2);
            graphics.drawString(font, totalLine, currencyX + 20, textY, 0xFFFFFF);

            currencyY += getCurrencyLineHeight();
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = imageWidth / 2;
        int titleY = PADDING;
        graphics.drawCenteredString(font, title, centerX, titleY, 0xFFFFFF);
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        lockGuiScale();
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
        int height = SECTION_TOP;
        for (int i = 0; i < rows.size(); i++) {
            height += rows.get(i).height();
            if (i < rows.size() - 1) {
                height += ROW_GAP;
            }
        }
        height += PADDING;
        return Math.max(height, 140);
    }

    private List<OfferRow> buildRows() {
        List<OfferRow> result = new ArrayList<>();
        int currentY = topPos + SECTION_TOP;
        for (int i = 0; i < menu.getOffers().size(); i++) {
            ConfiguredOffer offer = menu.getOffers().get(i);
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
