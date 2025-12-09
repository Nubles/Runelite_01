package com.example.slayerscape;

import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.ui.FontManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

public class SlayerScapePanel extends PluginPanel
{
    private final SlayerManager manager;
    private final JLabel keyLabel;
    private final JProgressBar xpProgressBar;
    private final SlayerScapeConfig config;
    private final ItemManager itemManager;
    private final SpriteManager spriteManager;

    private final SlayerScapeMiniMap miniMap;
    private final JPanel detailPanel;
    private final JLabel detailTitle;
    private final JLabel detailIcon;
    private final JLabel detailStatus;
    private final JButton unlockButton;
    private final JButton completeButton;

    public SlayerScapePanel(SlayerManager manager, SlayerScapeConfig config, ItemManager itemManager, SpriteManager spriteManager)
    {
        this.manager = manager;
        this.config = config;
        this.itemManager = itemManager;
        this.spriteManager = spriteManager;

        setLayout(new BorderLayout());

        // --- Top Bar ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        keyLabel = new JLabel("Keys: " + manager.slayerKeys);
        keyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(keyLabel, BorderLayout.NORTH);

        xpProgressBar = new JProgressBar(0, config.xpThreshold());
        xpProgressBar.setStringPainted(true);
        xpProgressBar.setToolTipText("XP until next pity Key");
        topPanel.add(xpProgressBar, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // --- Center: MiniMap + Details ---
        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));

        // Mini Map
        miniMap = new SlayerScapeMiniMap(manager, this::updateDetailView);
        // Center the minimap
        JPanel mapWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        mapWrapper.add(miniMap);
        centerContainer.add(mapWrapper);

        // Detail Panel
        detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        detailIcon = new JLabel();
        detailIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailPanel.add(detailIcon);

        detailPanel.add(Box.createVerticalStrut(10));

        detailTitle = new JLabel("Select a Tile");
        detailTitle.setFont(FontManager.getRunescapeBoldFont());
        detailTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailPanel.add(detailTitle);

        detailStatus = new JLabel("");
        detailStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailPanel.add(detailStatus);

        detailPanel.add(Box.createVerticalStrut(10));

        unlockButton = new JButton("Unlock (1 Key)");
        unlockButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        unlockButton.addActionListener(this::onUnlockClicked);
        unlockButton.setVisible(false);
        detailPanel.add(unlockButton);

        completeButton = new JButton("Mark Complete");
        completeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        completeButton.addActionListener(this::onCompleteClicked);
        completeButton.setVisible(false);
        detailPanel.add(completeButton);

        centerContainer.add(detailPanel);

        add(centerContainer, BorderLayout.CENTER);
    }

    public void refreshUI()
    {
        keyLabel.setText("Keys: " + manager.slayerKeys);

        if (config.enableXpPity())
        {
            xpProgressBar.setVisible(true);
            xpProgressBar.setMaximum(config.xpThreshold());
            xpProgressBar.setValue(manager.xpTowardNextKey);
            xpProgressBar.setString("XP Key: " + manager.xpTowardNextKey + " / " + config.xpThreshold());
        }
        else
        {
            xpProgressBar.setVisible(false);
        }

        miniMap.repaint();
        // Update details for currently selected tile
        updateDetailView(miniMap.getSelectedTile());
    }

    private void updateDetailView(GridTile tile)
    {
        if (tile == null) return;

        // Title
        detailTitle.setText("<html><center>" + tile.requirementText + "</center></html>");

        // Status & Buttons
        unlockButton.setVisible(false);
        completeButton.setVisible(false);

        if (tile.isCompleted) {
            detailStatus.setText("Completed");
            detailStatus.setForeground(Color.GREEN);
        } else if (tile.isUnlocked) {
            detailStatus.setText("Active Task");
            detailStatus.setForeground(Color.YELLOW);
            completeButton.setVisible(true);
        } else {
            detailStatus.setText("Locked");
            detailStatus.setForeground(Color.GRAY);

            // Fog of War Check
            if (manager.isNeighborUnlocked(tile.x, tile.y)) {
                unlockButton.setVisible(true);
                unlockButton.setEnabled(manager.slayerKeys > 0);
                unlockButton.setText("Unlock (1 Key)");
            } else {
                detailStatus.setText("Too far away");
                // detailStatus.setForeground(Color.DARK_GRAY);
            }
        }

        // Icon logic (Safe Async) - Show icon even if locked if reachable?
        // User asked "show what will be unlocked". So if neighbor unlocked, show icon.
        boolean showContent = tile.isUnlocked || manager.isNeighborUnlocked(tile.x, tile.y);

        if (showContent) {
            if (tile.isSprite) {
                // To fix crash: We cannot call getSprite on EDT.
                // We leave the icon blank or set a default.
                // NOTE: To properly fix this, we would need to pass the Client Thread and schedule a callback.
                // For now, text is sufficient to prevent crashing.
                detailIcon.setIcon(null);
                detailIcon.setText("[Skill Icon]");
            } else if (tile.iconId != -1) {
                AsyncBufferedImage img = itemManager.getImage(tile.iconId);
                detailIcon.setIcon(new ImageIcon(img));
                detailIcon.setText("");
            } else {
                detailIcon.setIcon(null);
                detailIcon.setText("");
            }
        } else {
            detailIcon.setIcon(null); // Locked icon?
            detailIcon.setText("?");
        }
    }

    private void onUnlockClicked(ActionEvent e)
    {
        GridTile tile = miniMap.getSelectedTile();
        if (tile != null && !tile.isUnlocked)
        {
            if (manager.spendKey(tile.x, tile.y))
            {
                refreshUI();
            }
        }
    }

    private void onCompleteClicked(ActionEvent e)
    {
        GridTile tile = miniMap.getSelectedTile();
        if (tile != null && tile.isUnlocked && !tile.isCompleted)
        {
            manager.completeTask(tile);
            refreshUI();
        }
    }
}
