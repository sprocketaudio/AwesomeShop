package net.sprocketaudio.awesomeshop.client;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.sprocketaudio.awesomeshop.AwesomeShop;
import net.sprocketaudio.awesomeshop.content.ShopScreen;

public class AwesomeShopClient {
    private AwesomeShopClient() {
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AwesomeShop.SHOP_MENU.get(), ShopScreen::new);
    }
}
