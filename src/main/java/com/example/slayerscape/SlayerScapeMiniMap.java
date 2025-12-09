package com.example.slayerscape;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import java.awt.image.BufferedImage;

public class SlayerScapeMiniMap extends JPanel
{
    private static final int TILE_SIZE = 18;
    private static final int GAP = 1;

    private final SlayerManager manager;
    private final Consumer<GridTile> onTileSelected;
    private final ItemManager itemManager;

    private Point selectedCoords = new Point(SlayerManager.GRID_SIZE / 2, SlayerManager.GRID_SIZE / 2); // Default center

    public SlayerScapeMiniMap(SlayerManager manager, Consumer<GridTile> onTileSelected, ItemManager itemManager)
    {
        this.manager = manager;
        this.onTileSelected = onTileSelected;
        this.itemManager = itemManager;

        // Calculate size: 50 * 19 = 950px.
        int dim = SlayerManager.GRID_SIZE * (TILE_SIZE + GAP);
        setPreferredSize(new Dimension(dim, dim));

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                int x = e.getX() / (TILE_SIZE + GAP);
                int y = e.getY() / (TILE_SIZE + GAP);

                if (x >= 0 && x < SlayerManager.GRID_SIZE && y >= 0 && y < SlayerManager.GRID_SIZE)
                {
                    selectedCoords = new Point(x, y);
                    onTileSelected.accept(manager.grid[x][y]);
                    repaint();
                }
            }
        });

        // Initial selection
        SwingUtilities.invokeLater(() -> onTileSelected.accept(manager.grid[selectedCoords.x][selectedCoords.y]));
    }

    public GridTile getSelectedTile()
    {
        return manager.grid[selectedCoords.x][selectedCoords.y];
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Get visible rect to only draw what is needed
        Rectangle clip = g.getClipBounds();

        int startX = Math.max(0, clip.x / (TILE_SIZE + GAP));
        int endX = Math.min(SlayerManager.GRID_SIZE, (clip.x + clip.width) / (TILE_SIZE + GAP) + 1);
        int startY = Math.max(0, clip.y / (TILE_SIZE + GAP));
        int endY = Math.min(SlayerManager.GRID_SIZE, (clip.y + clip.height) / (TILE_SIZE + GAP) + 1);

        for (int x = startX; x < endX; x++)
        {
            for (int y = startY; y < endY; y++)
            {
                int drawX = x * (TILE_SIZE + GAP);
                int drawY = y * (TILE_SIZE + GAP);

                GridTile tile = manager.grid[x][y];

                // Color Logic
                if (tile.isCompleted) {
                    g2d.setColor(new Color(0, 180, 0)); // Green
                } else if (tile.isUnlocked) {
                    g2d.setColor(new Color(200, 200, 0)); // Yellow/Gold
                } else {
                    // Fog of War Logic
                    if (manager.isNeighborUnlocked(x, y)) {
                        g2d.setColor(Color.GRAY); // Visible/Reachable
                    } else {
                        g2d.setColor(Color.BLACK); // Hidden/Fog
                    }
                }

                g2d.fillRect(drawX, drawY, TILE_SIZE, TILE_SIZE);

                // Draw Icon if visible and has icon
                if ((tile.isUnlocked || tile.isCompleted || manager.isNeighborUnlocked(x, y)) && tile.iconId != -1)
                {
                    // For now, only using Item IDs as per GridTile change.
                    // Sprites not supported in this version unless we add SpriteManager back.
                    // Assuming all tasks have item IDs.
                    if (!tile.isSprite)
                    {
                        AsyncBufferedImage img = itemManager.getImage(tile.iconId);
                        if (img != null)
                        {
                            img.onLoaded(this::repaint);
                            g2d.drawImage(img, drawX, drawY, TILE_SIZE, TILE_SIZE, null);
                        }
                    }
                }

                // Selection Highlight
                if (x == selectedCoords.x && y == selectedCoords.y)
                {
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRect(drawX + 1, drawY + 1, TILE_SIZE - 2, TILE_SIZE - 2);
                }
                else
                {
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawRect(drawX, drawY, TILE_SIZE, TILE_SIZE);
                }

                // Optional: Draw '?' for locked
                if (!tile.isUnlocked && !tile.isCompleted && manager.isNeighborUnlocked(x, y))
                {
                    // g2d.setColor(new Color(60, 60, 60));
                    // g2d.drawString("?", drawX + 5, drawY + 14); // Too small
                }
            }
        }
    }
}
