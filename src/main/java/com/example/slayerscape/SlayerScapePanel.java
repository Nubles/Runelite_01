package com.example.slayerscape;

import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class SlayerScapePanel extends PluginPanel
{
    private final SlayerManager manager;
    private final JPanel gridContainer;
    private final JLabel keyLabel;
    private final JProgressBar xpProgressBar;
    private final SlayerScapeConfig config;
    private final ItemManager itemManager;
    private final SpriteManager spriteManager;

    public SlayerScapePanel(SlayerManager manager, SlayerScapeConfig config, ItemManager itemManager, SpriteManager spriteManager)
    {
        this.manager = manager;
        this.config = config;
        this.itemManager = itemManager;
        this.spriteManager = spriteManager;

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
        // Use GridBagLayout for flexible grid that allows scrolling in both directions
        gridContainer = new JPanel();
        gridContainer.setLayout(new GridBagLayout());

        // Wrap gridContainer in another panel to prevent stretching/expanding
        JPanel centeringPanel = new JPanel(new GridBagLayout());
        centeringPanel.add(gridContainer);

        JScrollPane scrollPane = new JScrollPane(centeringPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Faster scrolling

        add(scrollPane, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::refreshUI);
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

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;

        for (int x = 0; x < SlayerManager.GRID_SIZE; x++)
        {
            for (int y = 0; y < SlayerManager.GRID_SIZE; y++)
            {
                JPanel tile = new JPanel(new BorderLayout());
                tile.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                Dimension dim = new Dimension(50, 50);
                tile.setPreferredSize(dim);
                tile.setMinimumSize(dim);
                tile.setMaximumSize(dim); // Prevent expanding

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

                // Add Icon if unlocked
                if (data.isUnlocked && data.iconId != -1)
                {
                    JLabel iconLabel = new JLabel();
                    iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

                    if (data.isSprite)
                    {
                        // SKIPPED TO PREVENT CRASH
                        // getSprite on EDT causes AssertionError.
                        // TODO: Implement async sprite loading or map to ItemIDs.
                    }
                    else
                    {
                        AsyncBufferedImage image = itemManager.getImage(data.iconId);
                        iconLabel.setIcon(new ImageIcon(image));
                        tile.add(iconLabel, BorderLayout.CENTER);
                    }
                }

                // Add text label for all tiles (fallback for missing icons or just clarity)
                // Use HTML for wrapping
                JLabel textLabel = new JLabel("<html><center>" + data.requirementText + "</center></html>");
                textLabel.setHorizontalAlignment(SwingConstants.CENTER);
                textLabel.setFont(new Font("Arial", Font.PLAIN, 10)); // Small font

                // If unlocked and has icon, show icon in CENTER, text in SOUTH
                if (data.isUnlocked && data.iconId != -1)
                {
                    // Icon logic handled above (added to CENTER)
                    // We need to change layout or add text elsewhere.
                    // Let's put text in SOUTH
                    tile.add(textLabel, BorderLayout.SOUTH);
                }
                else
                {
                    // If locked or no icon, show text in CENTER
                    tile.add(textLabel, BorderLayout.CENTER);
                }

                // Click listener to spend keys
                final int finalX = x;
                final int finalY = y;
                MouseAdapter clickListener = new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (!data.isUnlocked) {
                             if (manager.spendKey(finalX, finalY)) {
                                refreshUI();
                             } else {
                                 // Maybe flash red or show message?
                                 System.out.println("Failed to spend key at " + finalX + "," + finalY);
                             }
                        }
                    }
                };

                tile.addMouseListener(clickListener);
                // Propagate clicks from children
                for (Component comp : tile.getComponents()) {
                    comp.addMouseListener(clickListener);
                }

                c.gridx = x;
                c.gridy = y;
                gridContainer.add(tile, c);
            }
        }

        gridContainer.revalidate();
        gridContainer.repaint();
    }
}
