package com.example.slayerscape;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class SlayerScapeMiniMap extends JPanel
{
    private static final int TILE_SIZE = 18;
    private static final int GAP = 1;

    private final SlayerManager manager;
    private final Consumer<GridTile> onTileSelected;

    private Point selectedCoords = new Point(SlayerManager.GRID_SIZE / 2, SlayerManager.GRID_SIZE / 2); // Default center

    // Panning / Viewport
    private double offsetX = 0;
    private double offsetY = 0;
    private final Timer scrollTimer;
    private int scrollSpeedX = 0;
    private int scrollSpeedY = 0;
    private static final int EDGE_THRESHOLD = 30; // pixels from edge to trigger scroll
    private static final int SCROLL_SPEED = 5;

    public SlayerScapeMiniMap(SlayerManager manager, Consumer<GridTile> onTileSelected)
    {
        this.manager = manager;
        this.onTileSelected = onTileSelected;

        // Ensure we center the view initially on the start tile
        int gridSizePx = SlayerManager.GRID_SIZE * (TILE_SIZE + GAP);
        int viewSize = 225; // Approximate panel width

        // Start centered
        offsetX = (viewSize - gridSizePx) / 2.0;
        offsetY = (viewSize - gridSizePx) / 2.0;

        // Just set a fixed size for the panel, or fill parent?
        // If we want panning, we should probably take up all available space.
        // But for now, let's set a size that fits the sidebar width nicely, but maybe taller?
        setPreferredSize(new Dimension(225, 225));

        // Timer for smooth scrolling
        scrollTimer = new Timer(16, e -> {
            if (scrollSpeedX != 0 || scrollSpeedY != 0) {
                offsetX += scrollSpeedX;
                offsetY += scrollSpeedY;
                limitOffset();
                repaint();
            }
        });
        scrollTimer.start();

        MouseAdapter mouseHandler = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                // Calculate clicked tile based on offset
                int clickX = e.getX();
                int clickY = e.getY();

                // Inverse translation
                int gridX = (int)((clickX - offsetX) / (TILE_SIZE + GAP));
                int gridY = (int)((clickY - offsetY) / (TILE_SIZE + GAP));

                if (gridX >= 0 && gridX < SlayerManager.GRID_SIZE && gridY >= 0 && gridY < SlayerManager.GRID_SIZE)
                {
                    selectedCoords = new Point(gridX, gridY);
                    onTileSelected.accept(manager.grid[gridX][gridY]);
                    repaint();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                // Ensure focus or activation if needed
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                scrollSpeedX = 0;
                scrollSpeedY = 0;
            }

            @Override
            public void mouseMoved(MouseEvent e)
            {
                updateScrollSpeed(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                updateScrollSpeed(e.getX(), e.getY());
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // Initial selection
        SwingUtilities.invokeLater(() -> onTileSelected.accept(manager.grid[selectedCoords.x][selectedCoords.y]));
    }

    private void updateScrollSpeed(int mouseX, int mouseY)
    {
        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) return;

        scrollSpeedX = 0;
        scrollSpeedY = 0;

        if (mouseX < EDGE_THRESHOLD) {
            scrollSpeedX = SCROLL_SPEED;
        } else if (mouseX > w - EDGE_THRESHOLD) {
            scrollSpeedX = -SCROLL_SPEED;
        }

        if (mouseY < EDGE_THRESHOLD) {
            scrollSpeedY = SCROLL_SPEED;
        } else if (mouseY > h - EDGE_THRESHOLD) {
            scrollSpeedY = -SCROLL_SPEED;
        }
    }

    private void limitOffset()
    {
        int gridSizePx = SlayerManager.GRID_SIZE * (TILE_SIZE + GAP);
        int w = getWidth();
        int h = getHeight();

        // If grid is smaller than view, center it.
        // Otherwise, clamp.

        // Min offset (scrolled to far right/bottom): w - gridSizePx
        // Max offset (scrolled to top/left): 0 (or padding?)

        // Actually, we want to allow scrolling until the edge of the grid aligns with the edge of the view.
        // Max offset: 0 (or slight positive for padding)
        // Min offset: w - gridSizePx

        int minX = w - gridSizePx;
        int maxX = 0;
        int minY = h - gridSizePx;
        int maxY = 0;

        // Add some padding/margin
        int padding = 20;
        minX -= padding;
        maxX += padding;
        minY -= padding;
        maxY += padding;

        if (minX > maxX) {
             // Grid is smaller than view, center it
             offsetX = (w - gridSizePx) / 2.0;
        } else {
            if (offsetX < minX) offsetX = minX;
            if (offsetX > maxX) offsetX = maxX;
        }

        if (minY > maxY) {
            offsetY = (h - gridSizePx) / 2.0;
        } else {
            if (offsetY < minY) offsetY = minY;
            if (offsetY > maxY) offsetY = maxY;
        }
    }

    public GridTile getSelectedTile()
    {
        return manager.grid[selectedCoords.x][selectedCoords.y];
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Ensure offset is valid before painting (handles resize)
        limitOffset();

        Graphics2D g2d = (Graphics2D) g;

        // Apply translation
        g2d.translate(offsetX, offsetY);

        for (int x = 0; x < SlayerManager.GRID_SIZE; x++)
        {
            for (int y = 0; y < SlayerManager.GRID_SIZE; y++)
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
            }
        }

        // Reset translation for any overlay? No overlay needed here.
        g2d.translate(-offsetX, -offsetY);
    }
}
