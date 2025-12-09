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

/**
 * The main UI panel for the SlayerScape plugin.
 * Displays current status (Keys, XP), the MiniMap grid, and details for the selected tile.
 */
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
    private final JLabel detailDifficulty;
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
        // Center the minimap wrapper
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

        detailPanel.add(Box.createVerticalStrut(5));

        detailTitle = new JLabel("Select a Tile");
        detailTitle.setFont(FontManager.getRunescapeBoldFont());
        detailTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailPanel.add(detailTitle);

        detailDifficulty = new JLabel("");
        detailDifficulty.setFont(FontManager.getRunescapeSmallFont());
        detailDifficulty.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailPanel.add(detailDifficulty);

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

        Task task = tile.getTask();

        // Title & Difficulty
        detailTitle.setText("<html><center>" + task.getDescription() + "</center></html>");
        detailDifficulty.setText(task.getDifficulty().toString());

        switch (task.getDifficulty()) {
            case EASY: detailDifficulty.setForeground(Color.GREEN); break;
            case MEDIUM: detailDifficulty.setForeground(Color.YELLOW); break;
            case HARD: detailDifficulty.setForeground(Color.ORANGE); break;
            case ELITE: detailDifficulty.setForeground(Color.RED); break;
        }

        // Status & Buttons
        unlockButton.setVisible(false);
        completeButton.setVisible(false);

        if (tile.isCompleted()) {
            detailStatus.setText("Completed");
            detailStatus.setForeground(Color.GREEN);
        } else if (tile.isUnlocked()) {
            detailStatus.setText("Active Task");
            detailStatus.setForeground(Color.YELLOW);
            completeButton.setVisible(true);
        } else {
            detailStatus.setText("Locked");
            detailStatus.setForeground(Color.GRAY);

            // Fog of War Check
            if (manager.isNeighborUnlocked(tile.getX(), tile.getY())) {
                unlockButton.setVisible(true);
                unlockButton.setEnabled(manager.slayerKeys > 0);
                unlockButton.setText("Unlock (1 Key)");
            } else {
                detailStatus.setText("Too far away");
            }
        }

        // Icon logic
        // We show the icon if the tile is visible (unlocked or reachable neighbor)
        boolean showContent = tile.isUnlocked() || manager.isNeighborUnlocked(tile.getX(), tile.getY());

        if (showContent) {
            if (task.isSprite()) {
                // IMPORTANT: SpriteManager must NOT be called on EDT.
                // Since this update runs on EDT, we cannot fetch sprites safely here without a callback mechanism.
                // For safety and stability, we use a placeholder text.
                // To fix this properly, we'd need to fetch on Client thread and post back to EDT.
                detailIcon.setIcon(null);
                detailIcon.setText("[Icon: " + task.getIconId() + "]");
            } else if (task.getIconId() != -1) {
                // ItemManager.getImage is mostly safe or async-friendly (returns AsyncBufferedImage)
                AsyncBufferedImage img = itemManager.getImage(task.getIconId());
                detailIcon.setIcon(new ImageIcon(img));
                detailIcon.setText("");
            } else {
                detailIcon.setIcon(null);
                detailIcon.setText("");
            }
        } else {
            detailIcon.setIcon(null);
            detailIcon.setText("?");
        }
    }

    private void onUnlockClicked(ActionEvent e)
    {
        GridTile tile = miniMap.getSelectedTile();
        if (tile != null && !tile.isUnlocked())
        {
            if (manager.spendKey(tile.getX(), tile.getY()))
            {
                refreshUI();
            }
        }
    }

    private void onCompleteClicked(ActionEvent e)
    {
        GridTile tile = miniMap.getSelectedTile();
        if (tile != null && tile.isUnlocked() && !tile.isCompleted())
        {
            manager.completeTask(tile);
            refreshUI();
        }
    }
}
