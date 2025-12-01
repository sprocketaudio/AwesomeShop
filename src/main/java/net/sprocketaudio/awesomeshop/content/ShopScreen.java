package net.sprocketaudio.awesomeshop.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.sprocketaudio.awesomeshop.Config.ConfiguredOffer;
import net.sprocketaudio.awesomeshop.Config.ConfiguredCurrency;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final int PADDING = 8;
    private static final int SECTION_TOP = 64;
    private static final int ROW_HEIGHT = 36;
    private static final int HEADER_HEIGHT = 18;
    private static final int GROUP_GAP = 6;
    private static final int BUTTON_WIDTH = 18;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;
    private static final int PURCHASE_BUTTON_WIDTH = 100;

    private int lockedGuiScale = -1;
    private int originalGuiScale = -1;

    private final int[] selectedQuantities;
    private final Map<Integer, Button> purchaseButtons = new HashMap<>();
    private final List<OfferGroup> groups;

    public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.selectedQuantities = new int[menu.getOffers().size()];
        this.groups = buildGroups(menu.getOffers(), menu.getCurrencies());
        this.imageWidth = 360;
        this.imageHeight = calculateImageHeight();
    }

    @Override
    protected void init() {
        super.init();
        lockGuiScale();
        this.imageHeight = calculateImageHeight();
        this.topPos = PADDING;
        this.leftPos = (this.width - this.imageWidth) / 2;
        clearWidgets();
        purchaseButtons.clear();

        int y = topPos + SECTION_TOP;

        for (OfferGroup group : groups) {
            if (group.offerIndices().isEmpty()) {
                continue;
            }
            y += HEADER_HEIGHT;
            for (int index : group.offerIndices()) {
                int rowY = y;
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

                y += ROW_HEIGHT;
            }
            y += GROUP_GAP;
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
        int availableCurrency = menu.getCurrencyCount(offer.currency());
        int maxAffordable = price > 0 ? availableCurrency / price : Integer.MAX_VALUE;
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
        renderOfferDetails(graphics);
    }

    private void renderOfferDetails(GuiGraphics graphics) {
        List<ConfiguredOffer> offers = menu.getOffers();
        int y = topPos + SECTION_TOP;

        for (OfferGroup group : groups) {
            if (group.offerIndices().isEmpty()) {
                continue;
            }
            ConfiguredCurrency currency = group.currency();
            Component currencyName = Component.translatable(currency.item().getDescriptionId());
            Component header = Component.translatable("screen.awesomeshop.shop_block.currency_section",
                    menu.getCurrencyCount(currency), currencyName);
            graphics.drawString(font, header, leftPos + PADDING, y, 0xFFFFFF);
            y += HEADER_HEIGHT;

            for (int index : group.offerIndices()) {
                ConfiguredOffer offer = offers.get(index);
                int baseY = y;

                Component priceLine = Component.translatable("screen.awesomeshop.shop_block.price_each",
                        menu.getPriceForOffer(offer), currencyName);
                int totalCost = menu.getPriceForOffer(offer) * selectedQuantities[index];
                Component totalLine = Component.translatable("screen.awesomeshop.shop_block.total_price", totalCost,
                        currencyName);
                Component itemName = offer.item().getHoverName();

                graphics.renderItem(offer.item(), leftPos + PADDING, baseY + 2);
                graphics.renderItemDecorations(font, offer.item(), leftPos + PADDING, baseY + 2);
                graphics.drawString(font, itemName, leftPos + PADDING + 24, baseY + 4, 0xFFFFFF);
                int priceX = leftPos + PADDING + 120;
                graphics.drawString(font, priceLine, priceX, baseY + 2, 0xAAAAAA);
                graphics.drawString(font, totalLine, priceX, baseY + 2 + font.lineHeight + 2, 0xAAAAAA);

                y += ROW_HEIGHT;
            }
            y += GROUP_GAP;
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
        for (OfferGroup group : groups) {
            if (group.offerIndices().isEmpty()) {
                continue;
            }
            height += HEADER_HEIGHT;
            height += group.offerIndices().size() * ROW_HEIGHT;
            height += GROUP_GAP;
        }
        height += PADDING;
        return Math.max(height, 140);
    }

    private List<OfferGroup> buildGroups(List<ConfiguredOffer> offers, List<ConfiguredCurrency> currencies) {
        List<OfferGroup> result = new ArrayList<>();
        for (ConfiguredCurrency currency : currencies) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < offers.size(); i++) {
                if (offers.get(i).currency().id().equals(currency.id())) {
                    indices.add(i);
                }
            }
            if (!indices.isEmpty()) {
                result.add(new OfferGroup(currency, indices));
            }
        }
        return result;
    }

    private record OfferGroup(ConfiguredCurrency currency, List<Integer> offerIndices) {
    }
}
