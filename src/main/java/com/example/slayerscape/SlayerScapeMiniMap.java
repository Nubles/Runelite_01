package com.example.slayerscape;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * The Minimap component that renders the grid and handles user interaction (panning, selection).
 */
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

    // Drag-to-pan state
    private Point lastDragPoint = null;

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

        // Set a fixed size for the panel to fit in the sidebar
        setPreferredSize(new Dimension(225, 225));

        // Timer for smooth edge scrolling
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
                // Capture start point for dragging
                lastDragPoint = e.getPoint();

                // Handle Selection
                // Note: We also select on press. If the user meant to drag, selection still happens.
                // This is generally acceptable behavior for maps (select + drag).
                int clickX = e.getX();
                int clickY = e.getY();

                // Translate screen click to grid coordinates
                // gridX * (SIZE) + offsetX = clickX
                // gridX = (clickX - offsetX) / SIZE
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
            public void mouseReleased(MouseEvent e)
            {
                lastDragPoint = null;
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                // Stop edge scrolling if mouse leaves
                scrollSpeedX = 0;
                scrollSpeedY = 0;
                lastDragPoint = null;
            }

            @Override
            public void mouseMoved(MouseEvent e)
            {
                updateScrollSpeed(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                // Handle Panning logic
                if (lastDragPoint != null) {
                    int dx = e.getX() - lastDragPoint.x;
                    int dy = e.getY() - lastDragPoint.y;

                    offsetX += dx;
                    offsetY += dy;
                    limitOffset();
                    repaint();

                    lastDragPoint = e.getPoint();
                }

                // Also check for edge scrolling during drag
                updateScrollSpeed(e.getX(), e.getY());
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // Trigger initial selection
        SwingUtilities.invokeLater(() -> onTileSelected.accept(manager.grid[selectedCoords.x][selectedCoords.y]));
    }

    /**
     * Updates the automatic scroll speed based on mouse position relative to panel edges.
     */
    private void updateScrollSpeed(int mouseX, int mouseY)
    {
        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) return;

        scrollSpeedX = 0;
        scrollSpeedY = 0;

        // Horizontal Edge Check
        if (mouseX < EDGE_THRESHOLD) {
            scrollSpeedX = SCROLL_SPEED;
        } else if (mouseX > w - EDGE_THRESHOLD) {
            scrollSpeedX = -SCROLL_SPEED;
        }

        // Vertical Edge Check
        if (mouseY < EDGE_THRESHOLD) {
            scrollSpeedY = SCROLL_SPEED;
        } else if (mouseY > h - EDGE_THRESHOLD) {
            scrollSpeedY = -SCROLL_SPEED;
        }
    }

    /**
     * Clamps the scroll offset to keep the grid within viewable bounds.
     */
    private void limitOffset()
    {
        int gridSizePx = SlayerManager.GRID_SIZE * (TILE_SIZE + GAP);
        int w = getWidth();
        int h = getHeight();

        // Calculate limits
        // Min Offset: When grid is scrolled fully to the left/top (showing bottom-right)
        // Offset + GridSize must be >= Width - Padding
        // Offset >= Width - GridSize - Padding

        int padding = 50; // Allow some over-scroll
        int minX = w - gridSizePx - padding;
        int maxX = padding;

        int minY = h - gridSizePx - padding;
        int maxY = padding;

        if (gridSizePx < w) {
             // Center if grid is smaller than view
             offsetX = (w - gridSizePx) / 2.0;
        } else {
            if (offsetX < minX) offsetX = minX;
            if (offsetX > maxX) offsetX = maxX;
        }

        if (gridSizePx < h) {
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

        limitOffset();

        Graphics2D g2d = (Graphics2D) g;

        // Apply Viewport Translation
        g2d.translate(offsetX, offsetY);

        // Get clip bounds to optimize rendering (Frustum Culling)
        // Clip bounds are in "screen" coordinates.
        // We need to map them to "grid" coordinates.
        // Rectangle clip = g.getClipBounds();
        // Note: Translation affects the coordinate system, so drawing at (x,y) lands on (x+off, y+off).
        // To cull, we check if (drawX + offsetX, drawY + offsetY) is within (0,0,width,height).

        int panelW = getWidth();
        int panelH = getHeight();

        for (int x = 0; x < SlayerManager.GRID_SIZE; x++)
        {
            for (int y = 0; y < SlayerManager.GRID_SIZE; y++)
            {
                int drawX = x * (TILE_SIZE + GAP);
                int drawY = y * (TILE_SIZE + GAP);

                // Optimization: Skip tiles outside viewport
                double screenX = drawX + offsetX;
                double screenY = drawY + offsetY;

                if (screenX + TILE_SIZE < 0 || screenX > panelW ||
                    screenY + TILE_SIZE < 0 || screenY > panelH)
                {
                    continue;
                }

                GridTile tile = manager.grid[x][y];
                Task task = tile.getTask();

                // Color Logic
                if (tile.isCompleted()) {
                    g2d.setColor(new Color(0, 180, 0)); // Green
                } else if (tile.isUnlocked()) {
                    // Tint based on difficulty?
                    switch (task.getDifficulty()) {
                        case EASY: g2d.setColor(new Color(200, 255, 200)); break; // Pale Green
                        case MEDIUM: g2d.setColor(new Color(255, 255, 200)); break; // Pale Yellow
                        case HARD: g2d.setColor(new Color(255, 200, 200)); break; // Pale Red
                        case ELITE: g2d.setColor(new Color(200, 200, 255)); break; // Pale Blue
                        default: g2d.setColor(new Color(200, 200, 0)); break;
                    }
                } else {
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

        // Reset translation
        g2d.translate(-offsetX, -offsetY);
    }
}
