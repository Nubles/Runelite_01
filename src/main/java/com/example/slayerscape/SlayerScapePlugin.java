package com.example.slayerscape;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.GameState;
import net.runelite.api.ChatMessageType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    @Inject
    private SlayerScapeConfig config;

    @Inject
    private ItemManager itemManager;

    @Inject
    private SpriteManager spriteManager;

    private SlayerManager manager;
    private SlayerScapePanel panel;
    private NavigationButton navButton;
    private long lastOverallXp = -1;

    @Provides
    SlayerScapeConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(SlayerScapeConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        try
        {
            System.out.println("SlayerScape: Starting up...");
            manager = new SlayerManager(config);
            System.out.println("SlayerScape: Manager created.");
            panel = new SlayerScapePanel(manager, config, itemManager, spriteManager);
            System.out.println("SlayerScape: Panel created.");

            // Add the icon to the sidebar
            // Note: ensure you have an image named "icon.png" in your resources folder
            BufferedImage icon;
            try {
                icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
            } catch (Exception ex) {
                System.out.println("SlayerScape: Failed to load icon.png, using blank.");
                icon = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            }

            navButton = NavigationButton.builder()
                .tooltip("SlayerScape")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

            clientToolbar.addNavigation(navButton);
            System.out.println("SlayerScape: Navigation button added.");
        }
        catch (Throwable e)
        {
            System.out.println("SlayerScape: CRITICAL STARTUP ERROR");
            e.printStackTrace();
            log.error("Failed to start SlayerScape plugin", e);
            // Don't rethrow? If we rethrow, RuneLite disables it.
            // But if we don't rethrow, it thinks it started, but maybe button is missing.
            // Let's rethrow to be standard, but we have the logs now.
            if (e instanceof Exception) throw (Exception) e;
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        clientToolbar.removeNavigation(navButton);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            lastOverallXp = client.getOverallExperience();
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        // Prevent XP tracking during login/loading to avoid massive key drops
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (lastOverallXp == -1)
        {
            lastOverallXp = client.getOverallExperience();
            return;
        }

        long currentXp = client.getOverallExperience();
        long diff = currentXp - lastOverallXp;

        if (diff > 0)
        {
            lastOverallXp = currentXp;
            int keys = manager.addXp((int) diff);

            if (keys > 0)
            {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "SlayerScape: Key Found (XP Pity)!", null);
            }
            SwingUtilities.invokeLater(() -> panel.refreshUI());
        }
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
            // Attempt to get a key with bad luck mitigation
            if (manager.attemptSlayerTaskKey()) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "SlayerScape: Key Found!", null);
                SwingUtilities.invokeLater(() -> panel.refreshUI());
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
                 SwingUtilities.invokeLater(() -> panel.refreshUI());
             }
        }
    }
}
