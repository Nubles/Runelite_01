package com.example.slayerscape;

import net.runelite.client.ui.PluginPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SlayerScapePanel extends PluginPanel
{
    private final SlayerManager manager;
    private final JPanel gridContainer;
    private final JLabel keyLabel;
    private final JProgressBar xpProgressBar;
    private final SlayerScapeConfig config;

    public SlayerScapePanel(SlayerManager manager, SlayerScapeConfig config)
    {
        this.manager = manager;
        this.config = config;
        setLayout(new BorderLayout());

        // Top bar: Key count and XP Progress
        JPanel topPanel = new JPanel(new BorderLayout());

        keyLabel = new JLabel("Keys: " + manager.slayerKeys);
        keyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(keyLabel, BorderLayout.NORTH);

        xpProgressBar = new JProgressBar(0, config.xpThreshold());
        xpProgressBar.setStringPainted(true);
        xpProgressBar.setToolTipText("XP until next pity Key");
        topPanel.add(xpProgressBar, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // Center: The Grid
        gridContainer = new JPanel();
        gridContainer.setLayout(new GridLayout(SlayerManager.GRID_SIZE, SlayerManager.GRID_SIZE));
        add(gridContainer, BorderLayout.CENTER);

        refreshUI();
    }

    public void refreshUI()
    {
        gridContainer.removeAll();
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

        for (int x = 0; x < SlayerManager.GRID_SIZE; x++)
        {
            for (int y = 0; y < SlayerManager.GRID_SIZE; y++)
            {
                JPanel tile = new JPanel();
                tile.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                GridTile data = manager.grid[x][y];

                // Visual Logic
                if (data.isCompleted) {
                    tile.setBackground(Color.GREEN); // Done!
                    tile.setToolTipText(data.requirementText + " (DONE)");
                } else if (data.isUnlocked) {
                    tile.setBackground(Color.YELLOW); // Active Challenge
                    tile.setToolTipText(data.requirementText);
                } else {
                    tile.setBackground(Color.GRAY); // Locked / Fog of War
                    tile.setToolTipText("Locked - Click to Unlock");
                }

                // Click listener to spend keys
                final int finalX = x;
                final int finalY = y;
                tile.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (!data.isUnlocked && manager.spendKey(finalX, finalY)) {
                            refreshUI();
                        }
                    }
                });

                gridContainer.add(tile);
            }
        }

        gridContainer.revalidate();
        gridContainer.repaint();
    }
}
