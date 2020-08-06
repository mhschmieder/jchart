package com.mhschmieder.charttoolkit;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

import com.mhschmieder.guitoolkit.component.VectorizationXPanel;

/**
 * {@code Chart} is an abstract base class for all chart types, implementing
 * just the core functionality that is shared even between polar and Cartesian
 * charts Due to how Swing works with the canvas, it has to be implemented as a
 * member of the {@code Panel} class hierarchy, and as our will need to be
 * capable of exporting to various vector graphics formats, we derived from our
 * custom {@link VectorizationXPanel} class.
 * <p>
 * We may consolidate title handling later on and use a higher-level class
 * derivation, but this first step is needed in order to evaluate the
 * differences between title handling in charts for on-screen rendering vs.
 * header titles that are specifically just for vector graphics output files.
 *
 * @version 1.0
 *
 * @author Mark Schmieder
 */
public abstract class Chart extends VectorizationXPanel {
    /**
     * Unique Serial Version ID for this class, to avoid class loader conflicts.
     */
    private static final long serialVersionUID        = 4888983936757340982L;

    /**
     * This is the default Font Size for Titles; smaller is OK as the window
     * shrinks, but if the window grows then the font gets no larger than this.
     */
    public final static int   DEFAULT_TITLE_FONT_SIZE = 18;

    /**
     * Returns a font that is appropriate for chart titles; bold and large.
     *
     * Use SansSerif font as the basis, for consistent look-and-feel across the
     * application (and platforms) and the sharpest text.
     *
     * @return A font that is appropriate for chart titles; bold and large
     */
    @SuppressWarnings("nls")
    public final static Font makeTitleFontCandidate() {
        final Font titleFontCandidate = new Font( "SansSerif", Font.BOLD, DEFAULT_TITLE_FONT_SIZE );

        return titleFontCandidate;
    }

    /**
     * The title for this chart; usually placed above a chart or chart group.
     * <p>
     * For stacked charts, it is often left blank for the lower charts.
     */
    protected String      chartTitle;

    /**
     * The {@link Color} to use for the Grid Lines.
     */
    protected Color       gridColor;

    /**
     * The {@link FontMetrics} for the currently active title {@link Font}.
     */
    protected FontMetrics titleFontMetrics;

    //////////////////////////// Constructors ////////////////////////////////

    /**
     * Constructs a basic {@code Chart}.
     * <p>
     * As this is an abstract base class, it will not be constructed directly in
     * client code; rather, it will get invoked as the super-constructor by
     * derived concrete chart classes.
     *
     * @version 1.0
     */
    @SuppressWarnings("nls")
    protected Chart() {
        // Always call the superclass constructor first!
        super();

        // Start with an empty title.
        //
        // Pass in the title as a constructor argument?
        chartTitle = "";

        // Default to a subtle monochrome gray that shows against most
        // backgrounds without getting masked.
        gridColor = Color.LIGHT_GRAY;

        // make sure the Title Font Metrics aren't null at startup time.
        final Font titleFontCandidate = Chart.makeTitleFontCandidate();
        titleFontMetrics = getFontMetrics( titleFontCandidate );
    }

    ////////////////// Accessor methods for private data /////////////////////

    /**
     * Returns the chart title, or an empty string if there is none.
     *
     * @return The chart title, or an empty string if there is none
     *
     * @version 1.0
     */
    @SuppressWarnings("nls")
    public final String getChartTitle() {
        if ( chartTitle == null ) {
            return "";
        }
        return chartTitle;
    }

    /**
     * Sets the new chart title, or an empty string if there is none.
     *
     * @param newChartTitle
     *            The new chart title, or an empty string if there is none
     *
     * @version 1.0
     */
    public final void setChartTitle( final String newChartTitle ) {
        chartTitle = newChartTitle.trim();

        // Chart title changes require regeneration of the off-screen buffer.
        regenerateOffScreenImage = true;
    }

    /**
     * Returns the grid color.
     *
     * @return The grid color.
     *
     * @version 1.0
     */
    public final Color getGridColor() {
        return gridColor;
    }

    /**
     * Sets the grid color.
     *
     * @param newGridColor
     *            The new grid color.
     *
     * @version 1.0
     */
    public final void setGridColor( final Color newGridColor ) {
        gridColor = newGridColor;

        // Grid color changes require regeneration of the off-screen buffer.
        regenerateOffScreenImage = true;
    }

    /////////////////////// Chart rendering methods //////////////////////////

    /**
     * Draws the chart and all of its parts.
     * <p>
     * This method is abstract as the base class has no data or chart components
     * and is just present for enforcing basic shared chart behavior.
     *
     * @param graphicsContext
     *            The graphics context.
     *
     * @version 1.0
     */
    public abstract void drawChart( final Graphics2D graphicsContext );

    /**
     * Draws the chart title, generally above the chart or chart group.
     * <p>
     * This method is abstract as the base class has no data or chart components
     * and is just present for enforcing basic shared chart behavior.
     *
     * @param graphicsContext
     *            The graphics context.
     *
     * @version 1.0
     */
    public abstract void drawChartTitle( final Graphics2D graphicsContext );

    //////////////////////// XPanel method overrides /////////////////////////

    /**
     * Sets the appropriate foreground color for this panel based on the
     * specified background color.
     * <p>
     * Both the background and the foreground are applied to the entire layout
     * hierarchy, with the foreground color chosen to provide adequate contrast
     * against the background for text rendering as well as for line graphics.
     * <p>
     * This method should be overridden and called as the first line in the
     * method override, before adding support for GUI elements unique to the
     * derived class hierarchy.
     *
     * @param backColor
     *            The current background color to apply to this panel
     *
     * @since 1.0
     */
    @Override
    public void setForegroundFromBackground( final Color backColor ) {
        // Always call the superclass first!
        super.setForegroundFromBackground( backColor );

        // Determine the new Grid Color that provides the best contrast.
        final Color defaultGridColor = ChartUtilities.getDefaultGridColor( backColor );
        setGridColor( defaultGridColor );
    }

}
