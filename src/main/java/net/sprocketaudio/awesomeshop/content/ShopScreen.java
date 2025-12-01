package net.sprocketaudio.awesomeshop.content;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final int PADDING = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 140;

    public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 220;
        this.imageHeight = 20 + (menu.getOffers().size() * (BUTTON_HEIGHT + 2)) + 40;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        int x = leftPos + PADDING;
        int y = topPos + 40;

        List<ItemStack> offers = menu.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            ItemStack offer = offers.get(i);
            Component label = Component.translatable("screen.awesomeshop.shop_block.buy", offer.getHoverName());
            int buttonIndex = i;
            addRenderableWidget(Button.builder(label, b -> purchaseOffer(buttonIndex))
                    .bounds(x, y + i * (BUTTON_HEIGHT + 2), BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void purchaseOffer(int index) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderTransparentBackground(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component currencyName = Component.translatable(menu.getCurrencyItem().getDescriptionId());
        Component currencyLine = Component.translatable("screen.awesomeshop.shop_block.currency", menu.getCurrencyCount(), currencyName);
        graphics.drawString(font, title, PADDING, PADDING, 0xFFFFFF);
        graphics.drawString(font, currencyLine, PADDING, PADDING + font.lineHeight + 2, 0xFFFFFF);
    }
}
