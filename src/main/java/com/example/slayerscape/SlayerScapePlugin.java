package com.example.slayerscape;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.StatChanged;
import net.runelite.api.ChatMessageType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;
import java.util.Random;

@PluginDescriptor(
    name = "SlayerScape",
    description = "Unlock OSRS one tile at a time via Slayer",
    tags = {"slayer", "chunk", "gamemode"}
)
public class SlayerScapePlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    private SlayerManager manager;
    private SlayerScapePanel panel;
    private NavigationButton navButton;

    @Override
    protected void startUp() throws Exception
    {
        manager = new SlayerManager();
        panel = new SlayerScapePanel(manager);

        // Add the icon to the sidebar
        // Note: ensure you have an image named "icon.png" in your resources folder
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");

        navButton = NavigationButton.builder()
            .tooltip("SlayerScape")
            .icon(icon)
            .priority(5)
            .panel(panel)
            .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception
    {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
        {
            return;
        }

        String msg = event.getMessage();

        // 1. Detect Slayer Task Completion
        // OSRS Message: "You have completed your task! You killed 15 Goblins."
        if (msg.contains("You have completed your task!"))
        {
            // RNG Check: 50% chance to get a key
            if (new Random().nextBoolean()) {
                manager.addKey();
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "SlayerScape: Key Found!", null);
                panel.refreshUI();
            }
        }

        // 2. (Optional) Detect Level Ups logic would go here
        // Chat messages vary:
        // "Congratulations, you just advanced an Agility level."
        // "Congratulations, you just advanced a Thieving level."
        // "Congratulations, you've just advanced your Attack level."
        if (msg.startsWith("Congratulations, you"))
        {
             if (msg.contains("advanced a") || msg.contains("advanced an") || msg.contains("advanced your"))
             {
                 manager.addKey();
                 client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "SlayerScape: Key Found (Level Up)!", null);
                 panel.refreshUI();
             }
        }
    }
}
