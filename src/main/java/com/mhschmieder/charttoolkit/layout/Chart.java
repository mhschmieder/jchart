/**
 * MIT License
 *
 * Copyright (c) 2020, 2022 Mark Schmieder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * This file is part of the ChartToolkit Library
 *
 * You should have received a copy of the MIT License along with the
 * ChartToolkit Library. If not, see <https://opensource.org/licenses/MIT>.
 *
 * Project: https://github.com/mhschmieder/charttoolkit
 */
package com.mhschmieder.charttoolkit.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;

import com.mhschmieder.charttoolkit.chart.ChartUtilities;
import com.mhschmieder.graphicstoolkit.font.FontUtilities;
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
    public static final int   DEFAULT_TITLE_FONT_SIZE = 18;

    /**
     * Returns a font that is appropriate for chart titles; bold and large.
     *
     * Use SansSerif font as the basis, for consistent look-and-feel across the
     * application (and platforms) and the sharpest text.
     *
     * @return A font that is appropriate for chart titles; bold and large
     *
     * @version 1.0
     */
    @SuppressWarnings("nls")
    public static final Font makeTitleFontCandidate() {
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
     * The insets of the chart from the host panel on all four sides.
     */
    protected Insets      chartInsets;

    /**
     * This variable contains the size of the chart area in <i>pixels</i>. The
     * chart area is usually offset from upper left-hand corner of the host
     * panel, and also accounts for the chart title (if present), so variables
     * {@code ulx} and {@code uly} specify the origin of the chart area.
     */
    protected Dimension   chartSize;

    /**
     * The x value for the upper left corner of the chart area in pixels.
     */
    protected int         ulx;

    /**
     * The y value for the upper left corner of the chart area in pixels.
     */
    protected int         uly;

    /**
     * The x value for the lower right corner of the chart area in pixels.
     */
    protected int         lrx;

    /**
     * The y value for the lower right corner of the chart area in pixels.
     */
    protected int         lry;

    /**
     * The {@link Color} to use for the Grid Lines.
     */
    protected Color       gridColor;

    /**
     * The {@link FontMetrics} for the currently active title {@link Font}.
     */
    protected FontMetrics titleFontMetrics;

    /**
     * The power of ten by which the x-axis tic labels should be multiplied.
     */
    protected int         xExp;

    /**
     * The power of ten by which the x-axis tic labels should be multiplied.
     */
    protected int         yExp;

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

        // Assume that axis labels are on the left and bottom, meaning we want
        // slightly asymmetrical insets for the chart from its host panel.
        chartInsets = new Insets( 10, 16, 16, 10 );

        // Start with an initial chart size that is guaranteed to show something
        // even if the window has been significantly shrunk.
        chartSize = new Dimension( 100, 100 );

        // Set the initial chart area to a reasonable size that fits in minimal
        // windows, just in case the derived classes don't cleanly initialize
        // things in an ideal order.
        ulx = 1;
        uly = 1;
        lrx = 100;
        lry = 100;

        // Default to a subtle monochrome gray that shows against most
        // backgrounds without getting masked.
        gridColor = Color.LIGHT_GRAY;

        // Make sure the Title Font Metrics aren't null at startup time.
        final Font titleFontCandidate = Chart.makeTitleFontCandidate();
        titleFontMetrics = getFontMetrics( titleFontCandidate );

        // Start with the assumption that exponents are not needed for either
        // axis; this is only required for huge magnitude numbers that would
        // otherwise cause tic labels to overlap and become unreadable.
        xExp = 0;
        yExp = 0;
    }

    ////////////////// Accessor methods for private data /////////////////////

    /**
     * Returns the x value for the upper left corner of the chart area in
     * pixels.
     *
     * @return The x value for the upper left corner of the chart area in pixels
     *
     * @version 1.0
     */
    public final int getUlx() {
        return ulx;
    }

    /**
     * Returns the y value for the upper left corner of the chart area in
     * pixels.
     *
     * @return The y value for the upper left corner of the chart area in pixels
     *
     * @version 1.0
     */
    public final int getUly() {
        return uly;
    }

    /**
     * Returns the x value for the lower right corner of the chart area in
     * pixels.
     *
     * @return The x value for the lower right corner of the chart area in
     *         pixels
     *
     * @version 1.0
     */
    protected final int getLrx() {
        return lrx;
    }

    /**
     * Returns the y value for the lower right corner of the chart area in
     * pixels.
     *
     * @return The y value for the lower right corner of the chart area in
     *         pixels
     *
     * @version 1.0
     */
    protected final int getLry() {
        return lry;
    }

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
     * Draws the chart title, generally above a single chart or group of charts.
     *
     * @param graphicsContext
     *            The graphics context.
     *
     * @version 1.0
     */
    public final void drawChartTitle( final Graphics2D graphicsContext ) {
        // There is nothing to do if the chart title isn't present.
        if ( ( chartTitle == null ) || chartTitle.trim().isEmpty() ) {
            return;
        }

        // Cache the current Color and Font to restore later.
        final Color color = graphicsContext.getColor();
        final Font font = graphicsContext.getFont();

        final int maxCharacterHeight = uly - 15;

        // Get the maximum string width for determining Font Metrics.
        final int maxStringWidth = ( int ) Math.round( 0.25d * ( 3.0d * chartSize.width ) );

        // Use the standard foreground color with the Title Font.
        graphicsContext.setColor( getForeground() );
        final Font titleFontCandidate = Chart.makeTitleFontCandidate();
        titleFontMetrics = FontUtilities.pickFont( graphicsContext,
                                                   titleFontCandidate,
                                                   Chart.DEFAULT_TITLE_FONT_SIZE,
                                                   maxCharacterHeight,
                                                   maxStringWidth,
                                                   chartTitle );

        // Center the Title over the charting region, not over the window.
        final int stringWidth = titleFontMetrics.stringWidth( chartTitle );
        final int titleX = ulx + ( int ) Math.round( 0.5d * ( chartSize.width - stringWidth ) );

        // Vertical space for title, including the top padding above the title.
        //
        // We assume a one-line title, and the title above the main chart.
        final int stringHeight = titleFontMetrics.getHeight();
        final int titleY = ( yExp == 0 )
            ? ( int ) Math.round( 0.5d * stringHeight ) + 2 + chartInsets.top
            : chartInsets.top;

        graphicsContext.drawString( chartTitle, titleX, titleY );

        // Restore the previous Color and Font.
        graphicsContext.setColor( color );
        graphicsContext.setFont( font );
    }

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
