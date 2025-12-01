package net.sprocketaudio.awesomeshop.content;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;
import net.sprocketaudio.awesomeshop.Config.ConfiguredOffer;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final int PADDING = 8;
    private static final int ROW_HEIGHT = 36;
    private static final int BUTTON_WIDTH = 18;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;
    private static final int PURCHASE_BUTTON_WIDTH = 100;

    private int lockedGuiScale = -1;
    private int originalGuiScale = -1;

    private final int[] selectedQuantities;
    private final List<Button> purchaseButtons = new ArrayList<>();

    public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.selectedQuantities = new int[menu.getOffers().size()];
        this.imageWidth = 360;
        this.imageHeight = 80 + (menu.getOffers().size() * ROW_HEIGHT) + 20;
    }

    @Override
    protected void init() {
        super.init();
        lockGuiScale();
        this.topPos = PADDING;
        this.leftPos = (this.width - this.imageWidth) / 2;
        clearWidgets();

        int x = leftPos + PADDING;
        int y = topPos + 80;

        List<ConfiguredOffer> offers = menu.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            ConfiguredOffer offer = offers.get(i);
            int buttonIndex = i;
            int rowY = y + i * ROW_HEIGHT;
            int quantityButtonX = leftPos + imageWidth - PADDING - PURCHASE_BUTTON_WIDTH - BUTTON_WIDTH - 6;
            int plusY = rowY + 2;
            int minusY = plusY + BUTTON_HEIGHT + BUTTON_GAP;

            addRenderableWidget(Button.builder(Component.literal("+"), b -> adjustQuantity(buttonIndex, 1))
                    .bounds(quantityButtonX, plusY, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());

            addRenderableWidget(Button.builder(Component.literal("-"), b -> adjustQuantity(buttonIndex, -1))
                    .bounds(quantityButtonX, minusY, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());

            Button purchaseButton = Button.builder(Component.literal(""), b -> purchaseOffer(buttonIndex))
                    .bounds(leftPos + imageWidth - PADDING - PURCHASE_BUTTON_WIDTH, rowY, PURCHASE_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            purchaseButtons.add(addRenderableWidget(purchaseButton));
            updatePurchaseButton(buttonIndex);
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
            selectedQuantities[index] = 0;
            updatePurchaseButton(index);
        }
    }

    private void adjustQuantity(int index, int delta) {
        List<ConfiguredOffer> offers = menu.getOffers();
        if (index < 0 || index >= offers.size()) {
            return;
        }

        ConfiguredOffer offer = offers.get(index);
        int price = menu.getPriceForOffer(offer);
        int maxAffordable = price > 0 ? menu.getCurrencyCount() / price : Integer.MAX_VALUE;
        int newQuantity = Mth.clamp(selectedQuantities[index] + delta, 0, maxAffordable);
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
        if (index < 0 || index >= purchaseButtons.size()) {
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
        renderOfferDetails(graphics);
    }

    private void renderOfferDetails(GuiGraphics graphics) {
        List<ConfiguredOffer> offers = menu.getOffers();
        Component currencyName = Component.translatable(menu.getCurrencyItem().getDescriptionId());
        int y = topPos + 80;

        for (int i = 0; i < offers.size(); i++) {
            ConfiguredOffer offer = offers.get(i);
            int baseY = y + i * ROW_HEIGHT;

            Component priceLine = Component.translatable("screen.awesomeshop.shop_block.price_each", menu.getPriceForOffer(offer), currencyName);
            int totalCost = menu.getPriceForOffer(offer) * selectedQuantities[i];
            Component totalLine = Component.translatable("screen.awesomeshop.shop_block.total_price", totalCost, currencyName);
            Component itemName = offer.item().getHoverName();

            graphics.renderItem(offer.item(), leftPos + PADDING, baseY + 2);
            graphics.renderItemDecorations(font, offer.item(), leftPos + PADDING, baseY + 2);
            graphics.drawString(font, itemName, leftPos + PADDING + 24, baseY + 4, 0xFFFFFF);
            int priceX = leftPos + PADDING + 120;
            graphics.drawString(font, priceLine, priceX, baseY + 2, 0xAAAAAA);
            graphics.drawString(font, totalLine, priceX, baseY + 2 + font.lineHeight + 2, 0xAAAAAA);

            //Component quantityLine = Component.translatable("screen.awesomeshop.shop_block.quantity", selectedQuantities[i]);
            //int quantityTextX = leftPos + imageWidth - PADDING - PURCHASE_BUTTON_WIDTH - BUTTON_WIDTH - 6 + BUTTON_WIDTH + 4;
            //graphics.drawString(font, quantityLine, quantityTextX,
            //        baseY + 4 + font.lineHeight + 2, 0xFFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component currencyName = Component.translatable(menu.getCurrencyItem().getDescriptionId());
        Component currencyLine = Component.translatable("screen.awesomeshop.shop_block.currency", menu.getCurrencyCount(), currencyName);
        int centerX = leftPos + (imageWidth / 2);
        int titleY = topPos + PADDING;
        int currencyY = titleY + font.lineHeight + 4;
        graphics.drawCenteredString(font, title, centerX, titleY, 0xFFFFFF);
        graphics.drawCenteredString(font, currencyLine, centerX, currencyY, 0xFFFFFF);
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
}
