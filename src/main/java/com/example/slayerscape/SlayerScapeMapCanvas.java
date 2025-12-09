package com.example.slayerscape;

import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SlayerScapeMapCanvas extends JPanel
{
    private static final int TILE_SIZE = 48;
    private static final int GAP = 2;

    private final SlayerManager manager;
    private final ItemManager itemManager;
    private final Runnable onAction; // Callback to refresh UI

    public SlayerScapeMapCanvas(SlayerManager manager, ItemManager itemManager, Runnable onAction)
    {
        this.manager = manager;
        this.itemManager = itemManager;
        this.onAction = onAction;

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                int x = e.getX();
                int y = e.getY();

                int gridX = x / (TILE_SIZE + GAP);
                int gridY = y / (TILE_SIZE + GAP);

                if (gridX >= 0 && gridX < SlayerManager.GRID_SIZE && gridY >= 0 && gridY < SlayerManager.GRID_SIZE)
                {
                    if (manager.spendKey(gridX, gridY))
                    {
                        onAction.run();
                        repaint();
                    }
                }
            }
        });

        // Enable tooltips
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public Dimension getPreferredSize()
    {
        int size = SlayerManager.GRID_SIZE * (TILE_SIZE + GAP);
        return new Dimension(size, size);
    }

    @Override
    public String getToolTipText(MouseEvent event)
    {
        int x = event.getX();
        int y = event.getY();
        int gridX = x / (TILE_SIZE + GAP);
        int gridY = y / (TILE_SIZE + GAP);

        if (gridX >= 0 && gridX < SlayerManager.GRID_SIZE && gridY >= 0 && gridY < SlayerManager.GRID_SIZE)
        {
            GridTile tile = manager.grid[gridX][gridY];
            if (tile.isCompleted) return "<html><b>Completed!</b><br>" + tile.requirementText + "</html>";
            if (tile.isUnlocked) return "<html><b>Active Task</b><br>" + tile.requirementText + "</html>";
            return "<html><b>Locked</b><br>Click to spend 1 Key</html>";
        }
        return super.getToolTipText(event);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        for (int x = 0; x < SlayerManager.GRID_SIZE; x++)
        {
            for (int y = 0; y < SlayerManager.GRID_SIZE; y++)
            {
                int drawX = x * (TILE_SIZE + GAP);
                int drawY = y * (TILE_SIZE + GAP);

                GridTile tile = manager.grid[x][y];

                // Draw Background
                if (tile.isCompleted)
                {
                    g2d.setColor(new Color(0, 200, 0)); // Green
                }
                else if (tile.isUnlocked)
                {
                    g2d.setColor(new Color(255, 255, 0)); // Yellow
                }
                else
                {
                    g2d.setColor(Color.DARK_GRAY);
                }
                g2d.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(drawX, drawY, TILE_SIZE, TILE_SIZE);

                // Draw Content if Unlocked
                if (tile.isUnlocked)
                {
                    // Draw Icon
                    if (!tile.isSprite && tile.iconId != -1)
                    {
                        AsyncBufferedImage img = itemManager.getImage(tile.iconId);
                        if (img != null)
                        {
                            // Center the image (usually 32x32)
                            int imgX = drawX + (TILE_SIZE - 32) / 2;
                            int imgY = drawY + (TILE_SIZE - 32) / 2;
                            g2d.drawImage(img, imgX, imgY, null);
                        }
                    }
                    else
                    {
                        // Draw Text Fallback (Small)
                        g2d.setColor(Color.BLACK);
                        g2d.setFont(FontManager.getRunescapeSmallFont());
                        // Simple crude wrapping or truncation
                        String text = tile.requirementText;
                        if (text.length() > 8) text = text.substring(0, 6) + "..";

                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = drawX + (TILE_SIZE - fm.stringWidth(text)) / 2;
                        int textY = drawY + (TILE_SIZE + fm.getAscent()) / 2;
                        g2d.drawString(text, textX, textY);
                    }
                }
                else
                {
                    // Draw Locked Symbol? Or just leave gray
                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.setFont(FontManager.getRunescapeSmallFont());
                    g2d.drawString("?", drawX + TILE_SIZE/2 - 3, drawY + TILE_SIZE/2 + 4);
                }
            }
        }
    }
}
