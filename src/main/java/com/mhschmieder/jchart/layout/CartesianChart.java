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
package com.mhschmieder.jchart.layout;

import com.mhschmieder.jgraphics.render.HighlightStroke;
import com.mhschmieder.jmath.MathConstants;
import com.mhschmieder.jmath.MathUtilities;
import org.apache.commons.math3.util.FastMath;

import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A labeled box for signal plots and other plots.
 * <p>
 * This class provides a labeled box within which to place a data plot. A title,
 * X and Y axis labels, tic marks, and a legend are all supported. Zooming in
 * and out is supported. To zoom in, drag the mouse downwards to draw a box. To
 * zoom out, drag the mouse upward.
 * <p>
 * The box can be configured either through a file with commands or through
 * direct invocation of the public methods of the class.
 * <p>
 * When calling the methods, in most cases the changes will not be visible until
 * paintComponent() has been called. To request that this be _done, call
 * repaint().
 * <p>
 * The tic marks for the axes are usually computed automatically from the
 * ranges. Every attempt is made to choose reasonable positions for the tic
 * marks regardless of the data ranges (powers of ten multiplied by 1, 2, or 5
 * are used). However, they can also be specified explicitly using commands
 * like:
 *
 * <pre>
 *  XTics: &lt;i&gt;label position, label position, ...&lt;/i&gt;
 *  YTics: &lt;i&gt;label position, label position, ...&lt;/i&gt;
 * </pre>
 *
 * A <i>label</i> is a string that must be surrounded by quotation _marksStyle
 * if it contains any spaces. A <i>position</i> is a number giving the location
 * of the tic mark along the axis. For example, a horizontal axis for a
 * frequency domain plot might have tic marks as follows:
 *
 * <pre>
 *  XTics: -PI -3.14159, -PI/2 -1.570795, 0 0, PI/2 1.570795, PI 3.14159
 * </pre>
 *
 * Tic marks could also denote years, months, days of the week, etc.
 * <p>
 * The X and Y axes can use a logarithmic scale with the following commands:
 *
 * <pre>
 *  XLog: on
 *  YLog: on
 * </pre>
 *
 * The grid labels represent powers of 10. Note that if a logarithmic scale is
 * used, then the values must be positive. Non-positive values will be silently
 * dropped.
 * <p>
 * By default, tic marks are _connected by a light grey background grid. This
 * grid can be turned off with the following command:
 *
 * <pre>
 *  Grid: off
 * </pre>
 *
 * It can be turned back on with
 *
 * <pre>
 *  Grid: on
 * </pre>
 *
 * Also, by default, the first ten data sets are shown each in a unique color.
 * The use of color can be turned off with the command:
 *
 * <pre>
 *  Color: off
 * </pre>
 *
 * It can be turned back on with
 *
 * <pre>
 *  Color: on
 * </pre>
 */
public abstract class CartesianChart extends Chart {
    /**
     *
     */
    private static final long  serialVersionUID          = 7209329758139893721L;

    // Define watermark opacity level as a constant, both in case we switch to a
    // computed ratio and in case we provide programmatic support for changing
    // its value (this then gives us our defined default value).
    private static final float WATERMARK_OPACITY_DEFAULT = 0.15f;

    // Utility method to round position up to the nearest value in the grid.
    public static double gridRoundUp( final List< Double > grid, final double position ) {
        final double x = position - Math.floor( position );
        int i;
        final int numberOfGridSteps = grid.size();
        for ( i = 0; ( i < numberOfGridSteps ) && ( x >= grid.get( i ) ); i++ ) {}

        return ( i >= numberOfGridSteps ) ? position : Math.floor( position ) + grid.get( i );
    }

    // The range of the data to be plotted.
    protected double         xMin                      = 0.0d;
    protected double         xMax                      = 0.0d;
    protected double         yMin                      = 0.0d;
    protected double         yMax                      = 0.0d;

    // The factor we pad by so that we don't plot points on the axes.
    private double           padding                   = 0.05d;

    // Cache whether the ranges have been given or not.
    private boolean          xRangeGiven               = false;
    private boolean          yRangeGiven               = false;

    /**
     * @serial The given X and Y ranges. If they have been given the top and
     *         bottom of the x and y ranges. This is different from xMin and
     *         xMax, which actually represent the range of data that is
     *         plotted. This represents the range specified (which may be
     *         different due to zooming).
     */
    private double           xLowGiven;
    private double           xHighGiven;
    private double           yLowGiven;
    private double           yHighGiven;

    /** @serial The minimum X value registered so far, for auto ranging. */
    public double            xBottom                   = Double.MAX_VALUE;

    /** @serial The maximum X value registered so far, for auto ranging. */
    public double            xTop                      = -Double.MAX_VALUE;

    /** @serial The minimum Y value registered so far, for auto ranging. */
    public double            yBottom                   = Double.MAX_VALUE;

    /** @serial The maximum Y value registered so far, for auto ranging. */
    public double            yTop                      = -Double.MAX_VALUE;

    /** @serial Whether to draw the axes using a logarithmic scale. */
    protected boolean        xLog                      = false;
    protected boolean        yLog                      = false;

    /** @serial Whether to draw a background grid. */
    private boolean          gridOn                    = true;

    /** @serial The scale applied to the auto-tic generated grid resolution. */
    private double           gridScale                 = 1.0d;

    /**
     * The starting positions of the x-axis and y-axis labels.
     */
    private int              ySPos                     = 0;
    private int              xSPos                     = 0;

    // NOTE: subjective tic length.
    private final int        ticLength                 = 5;

    /**
     * The starting positions of the x-axis and y-axis tic marks.
     */
    private int              ticTopX                   = 0;
    private int              ticTopY                   = 1;
    private int              ticBottomX                = 1;
    private int              ticBottomY                = 0;

    /**
     * Scaling used for the vertical axis in plotting points. The units are
     * pixels/unit, where unit is the units of the Y axis.
     */
    protected double         yScale                    = 1.0d;

    /**
     * Scaling used for the horizontal axis in plotting points. The units are
     * pixels/unit, where unit is the units of the X axis.
     */
    protected double         xScale                    = 1.0d;

    /** @serial Metrics for titles and labels. */
    private int              labelHeight               = 2;
    private int              halfLabelHeight           = 1;

    /** @serial x-axis and y-axis labels and their widths. */
    private String           xLabels[];
    private String           yLabels[];
    private int              yLabelWidths[];

    /** @serial Superscript for exponential axes labels. */
    private String           superscript               = "";               //$NON-NLS-1$

    /** @serial Width and height of component in pixels. */
    private int              preferredWidth            = 500;
    private int              preferredHeight           = 300;

    /** @serial Indicator that size has been set. */
    private boolean          sizeHasBeenSet            = false;

    /** @serial Indicator of whether user specified aspect ratio is applied. */
    private boolean          aspectRatioApplied        = false;

    /** @serial The user specified aspect ratio for the plot area. */
    private double           aspectRatio               = 0.0d;

    /** @serial The x-axis and y-axis increments. */
    private double           xStep                     = 1.0d;
    private double           yStep                     = 1.0d;

    /** @serial The x-axis and y-axis starting points. */
    private double           xStart                    = 1.0d;
    private double           yStart                    = 1.0d;

    /**
     * @serial The range of the plot as labeled (multiply by 10^exp for actual
     *         range.
     */
    private double           xTicMin                   = 0.0d;
    private double           xTicMax                   = 0.0d;
    private double           yTicMin                   = 0.0d;
    private double           yTicMax                   = 0.0d;

    /**
     * @serial Indicator of whether the exponent is aligned with the tic labels.
     */
    private boolean          xExpAligned               = false;
    private boolean          yExpAligned               = false;

    /** @serial Scaling used in making tic marks. */
    private double           yTicScale                 = 0.0d;
    private double           xTicScale                 = 0.0d;

    /** @serial Font information. */
    private Font             labelFont;
    private Font             superscriptFont;
    private Font             ticFont;
    private Font             unitsFont;

    /** @serial FontMetric information. */
    private FontMetrics      labelFontMetrics;
    private FontMetrics      superscriptFontMetrics;
    private FontMetrics      ticFontMetrics;
    private FontMetrics      unitsFontMetrics;

    // Cache the size of the widest label.
    private int              widestTicLabel            = 0;

    // Used for log axes. Index into vector of axis labels.
    private int              gridCurJuke               = 0;

    // Used for log axes. Base of the grid.
    private double           gridBase                  = 0.0d;

    /** @serial The title and label strings. */
    private String           xLabel;
    protected String         xUnits;
    private String           xUnitsSublabel;
    private String           yLabel;
    protected String         yUnits;
    private String           yUnitsSublabel;

    /** @serial If XTics or YTics are given/ */
    private List< Double > xTics;
    private List< String > xTicLabels;
    private List< Double > yTics;
    private List< String > yTicLabels;

    /** @serial The number of x-axis and y-axis tic marks. */
    private int              numberOfXTics             = 1;
    private int              numberOfYTics             = 1;
    private int              xTicIndex                 = 0;
    private int              yTicIndex                 = 0;

    // Indicator of whether X and Y range has been first specified.
    private boolean          originalXRangeGiven       = false;
    private boolean          originalYRangeGiven       = false;

    // First values specified to setXRange() and setYRange().
    private double           originalXLow              = 0.0d;
    private double           originalXHigh             = 0.0d;
    private double           originalYLow              = 0.0d;
    private double           originalYHigh             = 0.0d;

    // Indicator of whether to override the computed maximum fraction digits.
    private boolean          maxFractionDigitsOverride = false;

    // The maximum fraction digits for displaying labels (if overridden)
    private int              maxFractionDigits         = 1;

    // Indicator of whether to override the computed minimum fraction digits.
    private boolean          minFractionDigitsOverride = false;

    // The minimum fraction digits for displaying labels (if overridden)
    private int              minFractionDigits         = 0;

    // Stroke for non-highlighted objects.
    protected BasicStroke    defaultStroke;

    // Stroke for drag boxes and highlighted objects.
    protected BasicStroke    dashStroke;

    // Use an image icon to load the watermark.
    protected ImageIcon      watermarkIcon;

    // Declare a flag for whether to use the watermark or not.
    protected boolean        useWatermark;

    // Declare a variable to hold the watermark opacity value.
    protected final float    watermarkOpacity;

    // Number format cache used for locale-specific number formatting.
    public NumberFormat      numberFormat;

    /*
     * Constructs a Cartesian plot box with a default configuration.
     */
    protected CartesianChart( final boolean watermarkInUse,
                              final String jarRelativeWatermarkIconFilename ) {
        // Always call the superclass constructor first!
        super();

        useWatermark = watermarkInUse;
        watermarkOpacity = WATERMARK_OPACITY_DEFAULT;

        try {
            initPanel( jarRelativeWatermarkIconFilename );
        }
        catch ( final Exception ex ) {
            ex.printStackTrace();
        }
    }

    /**
     * Specify a tic mark for the X axis. The label given is placed on the axis
     * at the position given by <i>position</i>. If this is called once or more,
     * automatic generation of tic marks is disabled. The tic mark will appear
     * only if it is within the X range.
     *
     * @param label
     *            The label for the tic mark.
     * @param position
     *            The position on the X axis.
     */
    protected void addXTic( final String label, final double position ) {
        if ( xTics == null ) {
            xTics = new ArrayList<>();
            xTicLabels = new ArrayList<>();
        }
        xTics.add( position );
        xTicLabels.add( label );
    }

    /**
     * Specify a tic mark for the Y axis. The label given is placed on the axis
     * at the position given by <i>position</i>. If this is called once or more,
     * automatic generation of tic marks is disabled. The tic mark will appear
     * only if it is within the Y range.
     *
     * @param label
     *            The label for the tic mark.
     * @param position
     *            The position on the Y axis.
     */
    protected void addYTic( final String label, final double position ) {
        if ( yTics == null ) {
            yTics = new ArrayList<>();
            yTicLabels = new ArrayList<>();
        }
        yTics.add( position );
        yTicLabels.add( label );
    }

    private final void adjustAxesAndLabelCoordinates( final Rectangle drawRect ) {
        // Number of vertical tic marks depends on the height of the font for
        // labeling tics and the height of the window.
        labelHeight = ticFontMetrics.getHeight();
        halfLabelHeight = ( int ) FastMath.round( 0.5d * labelHeight );

        // Determine scaling annotation for x axis.
        if ( xLog ) {
            xExp = ( int ) Math.floor( xTicMin );
        }
        superscript = Integer.toString( xExp );

        int bottomInset = chartInsets.bottom;
        if ( ( xExp != 0 ) && ( xTics == null ) ) {
            // NOTE: 5 pixel padding on the bottom
            bottomInset = ( int ) FastMath.round( 0.5d * ( 3.0d * labelHeight ) ) + 5;
        }

        // NOTE: 5 pixel padding on the bottom.
        if ( ( xLabel != null ) && ( bottomInset < ( labelHeight + 5 ) ) ) {
            bottomInset = labelHeight + 5;
        }

        // Update the chart insets for the revised bottom inset.
        chartInsets.set( chartInsets.top, chartInsets.left, bottomInset, chartInsets.right );

        // Vertical space for title, if appropriate.
        //
        // Modified to at least include the top padding from original code.
        //
        // We assume a one-line title.
        final boolean chartTitleValid = ( chartTitle != null ) && !chartTitle.trim().isEmpty();
        final int stringHeight = titleFontMetrics.getHeight();
        uly = ( chartTitleValid && ( yExp == 0 ) )
            ? ( int ) FastMath.round( 0.5 * stringHeight ) + 2 + chartInsets.top
            : chartInsets.top;

        // Compute the space needed around the chart, starting with vertical.
        //
        // The adding of 10 pixels below the title compensates for descenders.
        if ( chartTitleValid ) {
            // Apply the title padding again, to add space below the title.
            uly += chartInsets.top;
        }

        // NOTE: 3 pixels above bottom labels.
        lry = drawRect.height - labelHeight - chartInsets.bottom - 3;
        chartSize.height = lry - uly;

        // Find the widest label, if tics were explicitly specified.
        if ( yTics != null ) {
            for ( final String label : yTicLabels ) {
                final int labelWidth = ticFontMetrics.stringWidth( label );
                widestTicLabel = FastMath.max( widestTicLabel, labelWidth );
            }
        }

        // Next we do the horizontal spacing.
        // NOTE: pad by an additional 22 pixels, to leave room for axes labels.
        if ( yLabel != null ) {
            ulx = widestTicLabel + labelHeight + 10 + chartInsets.left;
        }
        else {
            ulx = widestTicLabel + chartInsets.left;
        }
        lrx = drawRect.width - 22 - chartInsets.right;
        chartSize.width = lrx - ulx;

        // If a square chart is desired (aspect ratio = 1:1), reset the width
        // and height to both be the same as the minimum dimension; otherwise
        // find the minimum of the minor dimension and the value obtained by
        // applying the aspect ratio to the major dimension, and then apply
        // the aspect ratio to that value to get the adjusted major dimension.
        // This guarantees that the entire plot will be visible in the
        // application.
        // TODO: Re-implement the comparisons using recommended Java fuzzy
        // math.
        // NOTE: This likely will not work correctly in the case of auto-tics,
        // as extreme aspect ratios could dramatically affect the practical
        // number of displayable Y-axis tics, but it is too difficult to find
        // a way to hold off since we need the tic width metrics here.
        if ( aspectRatioApplied ) {
            if ( aspectRatio == 1.0d ) {
                final int dimension = FastMath.min( chartSize.width, chartSize.height );
                chartSize.width = dimension;
                chartSize.height = dimension;
            }
            else if ( aspectRatio < 1.0d ) {
                final double minWidth = FastMath.min( chartSize.height * aspectRatio,
                                                        chartSize.width );
                chartSize.width = ( int ) FastMath.round( minWidth );
                chartSize.height = ( int ) FastMath.round( chartSize.width / aspectRatio );
            }
            else {
                final double minHeight = FastMath.min( chartSize.width / aspectRatio,
                                                         chartSize.height );
                chartSize.height = ( int ) FastMath.round( minHeight );
                chartSize.width = ( int ) FastMath.round( chartSize.height * aspectRatio );
            }

            // Readjust the lower right corner.
            // NOTE: This will result in some inconsistencies with certain
            // unlikely combinations of plot parameters, but it is too difficult
            // to fully generalize as there are too many metrics that are not
            // known until some of the labels and/or tics are already drawn.
            // TODO: Find a way to readjust the size of this component without
            // triggering recursive repaint(), to allow adjacent components to
            // grow when aspect ratio adjustments cause this component to
            // shrink.
            lrx = ulx + chartSize.width;
            lry = uly + chartSize.height;
        }

        // NOTE: 3 pixel spacing between axes and labels (if aligned).
        xSPos = ( xExpAligned ) ? lrx - 3 : drawRect.width - chartInsets.right;
        ySPos = ( xExpAligned )
            ? lry + labelHeight + labelFontMetrics.getHeight()
            : drawRect.height - 5;

        // TODO: Verify and combine all of this logic with the above.
        if ( aspectRatioApplied ) {
            // Readjust the position variables.
            // NOTE: 3 pixels above bottom labels.
            // NOTE: This will result in some inconsistencies with certain
            // unlikely combinations of plot parameters, but it is too difficult
            // to fully generalize as there are too many metrics that are not
            // known until some of the labels and/or tics are already drawn.
            // TODO: Find a way to readjust the size of this component without
            // triggering recursive repaint(), to allow adjacent components to
            // grow when aspect ratio adjustments cause this component to
            // shrink.
            xSPos = lrx;
            // ySPos = lry + _labelheight + 3 + labelFontMetrics.getHeight();
            ySPos = lry + labelHeight + labelFontMetrics.getHeight();
        }

        // NOTE: The scaling was moved here so that it is based on the final
        // aspect ratio adjusted width and height (it is not used beforehand).
        // NOTE: This appears to be unnecessary; this code could go anywhere as
        // it seems to have no dependencies on anything that comes before it
        // (and maybe after as well).
        yScale = chartSize.height / ( yMax - yMin );
        yTicScale = chartSize.height / ( yTicMax - yTicMin );
        xScale = chartSize.width / ( xMax - xMin );
        xTicScale = chartSize.width / ( xTicMax - xTicMin );

        // Calculate the tic mark relative positions.
        ticTopX = ulx + ticLength;
        ticTopY = uly + ticLength;
        ticBottomX = lrx - ticLength;
        ticBottomY = lry - ticLength;
    }

    private void autoXTics() {
        // Figure out how many digits after the decimal point will be used.
        final int numberOfFractionalDigits = MathUtilities.getNumberOfFractionalDigits( xStep );

        // NOTE: The following disables first tic. Not a good idea?
        // if (xStart == xMin) { xStart += xStep };

        final List< Double > xGrid = xLog ? gridInit( xStart, xStep, true, null ) : null;
        final double xTmpStart = xLog ? gridRoundUp( xGrid, xStart ) : xStart;

        for ( double xPos = xTmpStart; xPos <= xTicMax; xPos = getNextGridStep( xGrid,
                                                                                xPos,
                                                                                xStep,
                                                                                xLog ) ) {
            // Prevent out of bounds exceptions
            if ( xTicIndex >= numberOfXTics ) {
                break;
            }

            final String xTicLabel = xLog
                ? getFormattedLoggedNumber( xPos, numberOfFractionalDigits )
                : getFormattedNumber( xPos, numberOfFractionalDigits );
            xLabels[ xTicIndex++ ] = xTicLabel;
        }
    }

    private void autoYTics() {
        // Figure out how many digits after the decimal point will be used.
        final int numberOfFractionalDigits = MathUtilities.getNumberOfFractionalDigits( yStep );

        // NOTE: The following disables first tic. Not a good idea?
        // if (yStart == yMin) { yStart += yStep };

        final List< Double > yGrid = yLog ? gridInit( yStart, yStep, true, null ) : null;
        final double yTmpStart = yLog ? getNextGridStep( yGrid, yStart, yStep, yLog ) : yStart;

        for ( double yPos = yTmpStart; yPos <= yTicMax; yPos = getNextGridStep( yGrid,
                                                                                yPos,
                                                                                yStep,
                                                                                yLog ) ) {
            // Prevent out of bounds exceptions
            if ( yTicIndex >= numberOfYTics ) {
                break;
            }

            final String yTicLabel = yLog
                ? getFormattedLoggedNumber( yPos, numberOfFractionalDigits )
                : getFormattedNumber( yPos, numberOfFractionalDigits );
            yLabels[ yTicIndex ] = yTicLabel;

            final int labelWidth = ticFontMetrics.stringWidth( yTicLabel );
            yLabelWidths[ yTicIndex++ ] = labelWidth;
            if ( labelWidth > widestTicLabel ) {
                widestTicLabel = labelWidth;
            }
        }
    }

    /**
     * Clear all x tics. This will show up on the next redraw.
     */
    protected void clearXTics() {
        xTics = null;
        xTicLabels = null;
    }

    /**
     * Clear all y tics. This will show up on the next redraw.
     */
    protected void clearYTics() {
        yTics = null;
        yTicLabels = null;
    }

    private final void drawAxesLabels( final Graphics2D g2 ) {
        drawXAxisLabel( g2 );
        drawYAxisLabel( g2 );
    }

    // TODO: Re-factor these methods to break out the grid drawing.
    private final void drawAxesTics( final Graphics2D g2 ) {
        drawXAxisTics( g2 );
        drawYAxisTics( g2 );
    }

    /**
     * Draw the axes using the current range, label, and title information, at
     * the size of the specified rectangle. This method is called by
     * paintComponent(). To cause it to be called you would normally call
     * repaint(), which eventually causes paintComponent() to be called.
     *
     * @param graphicsContext
     *            The graphics context.
     */
    @Override
    public void drawChart( final Graphics2D graphicsContext ) {
        // Ignore if there is no graphics context to draw on.
        if ( graphicsContext == null ) {
            return;
        }

        // Draw the plot box outline.
        drawPlotBoxOutline( graphicsContext );

        // Draw the axes tics.
        drawAxesTics( graphicsContext );

        // Draw the axes labels.
        drawAxesLabels( graphicsContext );

        // Draw scaling annotation for the x-axis.
        drawScalingAnnotation( graphicsContext );

        // Draw the title.
        drawChartTitle( graphicsContext );
    }

    private final void drawPlotBoxOutline( final Graphics2D graphicsContext ) {
        // Cache the current color to restore later.
        final Color color = graphicsContext.getColor();

        // Use the standard foreground color for the plot box outline.
        graphicsContext.setColor( getForeground() );

        // Draw the plot box outline.
        graphicsContext.drawRect( ulx, uly, chartSize.width, chartSize.height );

        // Restore the previous color.
        graphicsContext.setColor( color );
    }

    @SuppressWarnings("nls")
    private final void drawScalingAnnotation( final Graphics2D graphicsContext ) {
        if ( ( xExp != 0 ) && ( xTics == null ) && ( !xLog ) ) {
            // Cache the current color and font to restore later.
            final Color color = graphicsContext.getColor();
            final Font font = graphicsContext.getFont();

            // Use the standard foreground color with the superscript font.
            graphicsContext.setColor( getForeground() );
            graphicsContext.setFont( superscriptFont );

            int xSPosAdjusted = xSPos - superscriptFontMetrics.stringWidth( superscript );
            graphicsContext.drawString( superscript, xSPosAdjusted, ySPos - halfLabelHeight );

            // Use the tic font for the scaling factor.
            graphicsContext.setFont( ticFont );

            xSPosAdjusted -= ticFontMetrics.stringWidth( "x10" );
            graphicsContext.drawString( "x10", xSPosAdjusted, ySPos );

            // Restore the previous color and font.
            graphicsContext.setColor( color );
            graphicsContext.setFont( font );
        }
    }

    private final void drawXAxisLabel( final Graphics2D g2 ) {
        if ( xLabel != null ) {
            // Cache the current color and font to restore later.
            final Color color = g2.getColor();
            final Font font = g2.getFont();

            // Use the standard foreground color with the label font.
            g2.setColor( getForeground() );
            g2.setFont( labelFont );

            int stringWidth = labelFontMetrics.stringWidth( xLabel );
            if ( xUnitsSublabel != null ) {
                stringWidth += unitsFontMetrics.stringWidth( xUnitsSublabel );
            }

            // Center the X-axis label over the plotting region, not over the
            // window.
            final int labelx = ulx + ( int ) Math.round( 0.5d * ( chartSize.width - stringWidth ) );
            g2.drawString( xLabel, labelx, ySPos );
            if ( xUnitsSublabel != null ) {
                g2.setFont( unitsFont );
                final int unitsx = labelx + labelFontMetrics.stringWidth( xLabel );
                g2.drawString( xUnitsSublabel, unitsx, ySPos );
            }

            // Restore the previous color and font.
            g2.setColor( color );
            g2.setFont( font );
        }
    }

    @SuppressWarnings("nls")
    private final void drawXAxisTics( final Graphics2D g2 ) {
        // Cache the current color and font to restore later.
        final Color color = g2.getColor();
        final Font font = g2.getFont();

        // Use the standard foreground color with the label font.
        final Color foregroundColor = getForeground();
        g2.setColor( foregroundColor );
        g2.setFont( ticFont );

        if ( xTics == null ) {
            // auto-tics
            List< Double > xGrid = null;
            double xTmpStart = xStart;
            if ( xLog ) {
                xGrid = gridInit( xStart, xStep, true, null );
                xTmpStart = gridRoundUp( xGrid, xStart );
                numberOfXTics = xTicIndex;
            }
            xTicIndex = 0;

            // Set to false if we don't need the exponent
            boolean needExponent = xLog;

            // Label the x axis. The labels are quantized so that they don't
            // have excess resolution.
            for ( double xPos = xTmpStart; xPos <= xTicMax; xPos = getNextGridStep( xGrid,
                                                                                    xPos,
                                                                                    xStep,
                                                                                    xLog ) ) {
                // Prevent out of bounds exceptions
                if ( xTicIndex >= numberOfXTics ) {
                    break;
                }

                // Draw the tic pair and grid line at this tic position.
                // NOTE: We avoid painting tic marks at the plot box boundary.
                // NOTE: Paint order is decoupled, by starting the grid lines
                // at the end points of the tic marks.
                final int ticX = ulx + ( int ) ( ( xPos - xTicMin ) * xTicScale );
                g2.drawLine( ticX, uly, ticX, ticTopY );
                g2.drawLine( ticX, lry, ticX, ticBottomY );
                if ( gridOn && ( ticX != ulx ) && ( ticX != lrx ) ) {
                    g2.setColor( gridColor );
                    g2.drawLine( ticX, ticTopY, ticX, ticBottomY );
                    g2.setColor( foregroundColor );
                }

                // Check to see if any of the labels printed contain the
                // exponent. If we don't see an exponent, then print it.
                final String xTicLabel = xLabels[ xTicIndex++ ];
                if ( xLog && ( xTicLabel != null ) && ( xTicLabel.indexOf( 'e' ) != -1 ) ) {
                    needExponent = false;
                }

                // Draw the tic label.
                // NOTE: 3 pixel spacing between axis and labels.
                if ( xTicLabel != null ) {
                    final int labelXPos = ticX
                            - ( int ) Math.ceil( 0.5d * ticFontMetrics.stringWidth( xTicLabel ) );
                    final int labelYPos = lry + 3 + labelHeight;
                    g2.drawString( xTicLabel, labelXPos, labelYPos );
                }
            }

            if ( xLog ) {
                // Draw in grid lines that don't have labels.

                // If the step is greater than 1, clamp it to 1 so that we draw
                // the unlabeled grid lines for each integer interval.
                final double tmpStep = ( xStep > 1.0d ) ? 1.0d : xStep;

                // Recalculate the start using the new step.
                xTmpStart = tmpStep * Math.ceil( xTicMin / tmpStep );

                final List< Double > unlabeledGrid = gridInit( xTmpStart, tmpStep, false, xGrid );
                if ( unlabeledGrid.size() > 0 ) {
                    for ( double xPos =
                                      getNextGridStep( unlabeledGrid,
                                                       xTmpStart,
                                                       tmpStep,
                                                       xLog ); xPos <= xTicMax; xPos =
                                                                                     getNextGridStep( unlabeledGrid,
                                                                                                      xPos,
                                                                                                      tmpStep,
                                                                                                      xLog ) ) {
                        ticTopX = ulx + ( int ) ( ( xPos - xTicMin ) * xTicScale );
                        if ( gridOn && ( ticTopX != ulx ) && ( ticTopX != lrx ) ) {
                            g2.setColor( gridColor );
                            g2.drawLine( ticTopX, uly + 1, ticTopX, lry - 1 );
                            g2.setColor( foregroundColor );
                        }
                    }
                }

                if ( needExponent ) {
                    xExp = ( int ) Math.floor( xTmpStart );
                    g2.setFont( superscriptFont );
                    g2.drawString( Integer.toString( xExp ), xSPos, ySPos - halfLabelHeight );
                    xSPos -= ticFontMetrics.stringWidth( "x10" );
                    g2.setFont( ticFont );
                    g2.drawString( "x10", xSPos, ySPos );
                }
                else {
                    xExp = 0;
                }
            }
        }
        else {
            int preLength = 0;

            // Tics have been explicitly specified
            int ticIndex = 0;
            for ( final String label : xTicLabels ) {
                // If xPos is out of range, ignore.
                final double xPos = xTics.get( ticIndex++ );
                if ( ( xPos > xMax ) || ( xPos < xMin ) ) {
                    continue;
                }

                // Find the center position of the label.
                final int ticX = ulx + ( int ) ( ( xPos - xMin ) * xScale );

                // Find the start position of x label.
                // NOTE: 3 pixel spacing between axis and labels.
                final int labelXCenterToEdge = ( int ) Math
                        .ceil( 0.5d * ticFontMetrics.stringWidth( label ) );
                final int labelXPos = ticX - labelXCenterToEdge;
                final int labelYPos = lry + 3 + labelHeight;

                // If the labels are not overlapped or too close (i.e. no more
                // than two pixels as spacing), then proceed and add tick/line.
                if ( labelXPos > preLength ) {
                    // Calculate the length of the label.
                    preLength = ticX + labelXCenterToEdge + 2;

                    // Draw the label.
                    g2.drawString( label, labelXPos, labelYPos );

                    // Draw the label mark on the axis.
                    g2.drawLine( ticX, uly, ticX, ticTopY );
                    g2.drawLine( ticX, lry, ticX, ticBottomY );

                    // Draw the grid line.
                    if ( gridOn && ( ticX != ulx ) && ( ticX != lrx ) ) {
                        g2.setColor( gridColor );
                        g2.drawLine( ticX, ticTopY, ticX, ticBottomY );
                        g2.setColor( foregroundColor );
                    }
                }
            }
        }

        // Restore the previous color and font.
        g2.setColor( color );
        g2.setFont( font );
    }

    private final void drawYAxisLabel( final Graphics2D g2 ) {
        if ( yLabel != null ) {
            // Cache the current color and font to restore later.
            final Color color = g2.getColor();
            final Font font = g2.getFont();

            // Use the standard foreground color with the label font.
            g2.setColor( getForeground() );
            g2.setFont( labelFont );

            // Rotate by 90 degrees so the label reads along the y-axis.
            // NOTE: We must account for the full string not just the main
            // label sans units.
            final String yLabelWithUnits = ( yUnitsSublabel != null )
                ? yLabel + yUnitsSublabel
                : yLabel;
            final int stringWidth = labelFontMetrics.stringWidth( yLabelWithUnits );
            final int labelX = ( int ) Math
                    .round( 0.5d * ( labelFontMetrics.getHeight() + chartInsets.left ) );
            final int labelY = uly + ( int ) Math.round( 0.5d * ( ( lry - uly ) + stringWidth ) );
            g2.translate( labelX, labelY );
            g2.rotate( -MathConstants.HALF_PI );
            // int labelOffsetX = (int)Math.round(-0.5d * stringWidth);
            // int labelOffsetY =
            // (int)Math.round(-labelFontMetrics.getDescent());
            // g2.drawString(yLabel, labelOffsetX, labelOffsetY);
            g2.drawString( yLabel, 0, 0 );
            g2.rotate( MathConstants.HALF_PI );
            g2.translate( -labelX, -labelY );
            if ( yUnitsSublabel != null ) {
                final int unitsX = ( int ) Math
                        .round( 0.5d * ( unitsFontMetrics.getHeight() + chartInsets.left ) );
                final int unitsY = labelY - labelFontMetrics.stringWidth( yLabel );
                g2.translate( unitsX, unitsY );
                g2.rotate( -MathConstants.HALF_PI );
                // int unitsXOffset = labelFontMetrics.stringWidth(yLabel);
                // int unitsYOffset =
                // (int)Math.round(-unitsFontMetrics.getDescent());
                g2.setFont( unitsFont );
                // g2.drawString(_units, unitsxOffset, unitsyOffset);
                g2.drawString( yUnitsSublabel, 0, 0 );
                g2.rotate( MathConstants.HALF_PI );
                g2.translate( -unitsX, -unitsY );
            }

            // Restore the previous color and font.
            g2.setColor( color );
            g2.setFont( font );
        }
    }

    @SuppressWarnings("nls")
    private final void drawYAxisTics( final Graphics2D g2 ) {
        // Cache the current color and font to restore later.
        final Color color = g2.getColor();
        final Font font = g2.getFont();

        // Use the standard foreground color with the label font.
        final Color foregroundColor = getForeground();
        g2.setColor( foregroundColor );
        g2.setFont( ticFont );

        if ( yTics == null ) {
            // auto-tics
            List< Double > yGrid = null;
            double yTmpStart = yStart;
            if ( yLog ) {
                yGrid = gridInit( yStart, yStep, true, null );
                yTmpStart = getNextGridStep( yGrid, yStart, yStep, yLog );
                numberOfYTics = yTicIndex;
            }
            yTicIndex = 0;

            // Set to false if we don't need the exponent
            boolean needExponent = yLog;

            // Label the y axis. The labels are quantized so that they don't
            // have excess resolution.
            for ( double yPos = yTmpStart; yPos <= yTicMax; yPos = getNextGridStep( yGrid,
                                                                                    yPos,
                                                                                    yStep,
                                                                                    yLog ) ) {
                // Prevent out of bounds exceptions
                if ( yTicIndex >= numberOfYTics ) {
                    break;
                }

                // Draw the tic pair and grid line at this tic position.
                // NOTE: We avoid painting tic marks at the plot box boundary.
                // NOTE: Paint order is decoupled, by starting the grid lines
                // at the end points of the tic marks.
                final int ticY = lry - ( int ) ( ( yPos - yTicMin ) * yTicScale );
                g2.drawLine( ulx, ticY, ticTopX, ticY );
                g2.drawLine( lrx, ticY, ticBottomX, ticY );
                if ( gridOn && ( ticY != uly ) && ( ticY != lry ) ) {
                    g2.setColor( gridColor );
                    g2.drawLine( ticTopX, ticY, ticBottomX, ticY );
                    g2.setColor( foregroundColor );
                }

                // Check to see if any of the labels printed contain the
                // exponent. If we don't see an exponent, then print it.
                final String yTicLabel = yLabels[ yTicIndex ];
                if ( yLog && ( yTicLabel != null ) && ( yTicLabel.indexOf( 'e' ) != -1 ) ) {
                    needExponent = false;
                }

                // Draw the tic label.
                // NOTE: 4 pixel spacing between axis and labels.
                if ( yTicLabel != null ) {
                    final int labelXPos = ulx - yLabelWidths[ yTicIndex++ ] - 4;
                    final int labelYPos = ticY + ( int ) Math.ceil( 0.3333d * labelHeight );
                    g2.drawString( yTicLabel, labelXPos, labelYPos );
                }
            }

            if ( yLog ) {
                // Draw in grid lines that don't have labels.
                final List< Double > unlabeledGrid = gridInit( yStart, yStep, false, yGrid );
                if ( unlabeledGrid.size() > 0 ) {
                    // If the step is greater than 1, clamp it to 1 so that we
                    // draw the unlabeled grid lines for each integer interval.
                    final double tmpStep = ( yStep > 1.0d ) ? 1.0d : yStep;

                    for ( double yPos =
                                      getNextGridStep( unlabeledGrid,
                                                       yStart,
                                                       tmpStep,
                                                       yLog ); yPos <= yTicMax; yPos =
                                                                                     getNextGridStep( unlabeledGrid,
                                                                                                      yPos,
                                                                                                      tmpStep,
                                                                                                      yLog ) ) {
                        final int yCoord1 = lry - ( int ) ( ( yPos - yTicMin ) * yTicScale );
                        if ( gridOn && ( yCoord1 != uly ) && ( yCoord1 != lry ) ) {
                            g2.setColor( gridColor );
                            g2.drawLine( ulx + 1, yCoord1, lrx - 1, yCoord1 );
                            g2.setColor( foregroundColor );
                        }
                    }
                }

                // If we zoomed in, we need the exponent
                yExp = needExponent ? ( int ) Math.floor( yTmpStart ) : 0;
            }

            // Draw scaling annotation for y axis.
            // TODO: Check for whether the exponent should be aligned, and
            // determine and appropriate alternate place to draw it.
            if ( yExp != 0 ) {
                g2.drawString( "x10", chartInsets.left, chartInsets.top );
                g2.setFont( superscriptFont );
                g2.drawString( Integer.toString( yExp ),
                               ticFontMetrics.stringWidth( "x10" ) + chartInsets.left,
                               chartInsets.top - halfLabelHeight );
                g2.setFont( ticFont );
            }
        }
        else {
            // Tics have been explicitly specified
            int ticIndex = 0;
            for ( final String label : yTicLabels ) {
                // If yPos is out of range, ignore.
                final double yPos = yTics.get( ticIndex++ );
                if ( ( yPos > yMax ) || ( yPos < yMin ) ) {
                    continue;
                }

                // Find the center position of the label.
                final int ticY = lry - ( int ) ( ( yPos - yMin ) * yScale );

                // Find the start position of y label.
                // NOTE: 3 pixel spacing between axis and labels.
                final int offset = ( yPos < ( lry - labelHeight ) ) ? halfLabelHeight : 0;
                final int labelXPos = ulx - ticFontMetrics.stringWidth( label ) - 3;
                final int labelYPos = ticY + offset;

                // Draw the label.
                g2.drawString( label, labelXPos, labelYPos );

                // Draw the label mark on the axis.
                g2.drawLine( ulx, ticY, ticTopX, ticY );
                g2.drawLine( lrx, ticY, ticBottomX, ticY );

                // Draw the grid line.
                if ( gridOn && ( ticY != uly ) && ( ticY != lry ) ) {
                    g2.setColor( gridColor );
                    g2.drawLine( ticTopX, ticY, ticBottomX, ticY );
                    g2.setColor( foregroundColor );
                }
            }
        }

        // Restore the previous color and font.
        g2.setColor( color );
        g2.setFont( font );
    }

    private void generateTics() {
        generateXTics();
        generateYTics();
    }

    @SuppressWarnings("nls")
    private void generateXTics() {
        // Number of x tic marks.
        // Need to start with a guess and converge on a solution here.
        numberOfXTics = 10;
        xStep = 0.0d;
        int numberOfFractionalDigits = 0;
        final int charWidth = ticFontMetrics.stringWidth( "8" );
        if ( xLog ) {
            // X axis log labels will be at most 6 chars: -1E-02
            numberOfXTics = 2 + ( chartSize.width / ( ( charWidth * 6 ) + 10 ) );
        }
        else {
            // Limit to 10 iterations
            int count = 0;
            while ( count++ <= 10 ) {
                xStep = MathUtilities.roundUp( ( xTicMax - xTicMin ) / numberOfXTics );

                // Compute the width of a label for this xStep
                numberOfFractionalDigits = MathUtilities.getNumberOfFractionalDigits( xStep );

                // Number of integer digits is the maximum of two end points
                int intdigits = MathUtilities.getNumberOfIntegerDigits( xTicMax );
                final int inttemp = MathUtilities.getNumberOfIntegerDigits( xTicMin );
                if ( intdigits < inttemp ) {
                    intdigits = inttemp;
                }

                // Allow two extra digits (decimal point and sign).
                final int maxLabelWidth = charWidth * ( numberOfFractionalDigits + 2 + intdigits );

                // Compute new estimate of number of tics.
                // NOTE: 10 additional pixels between labels.
                // NOTE: Try to ensure at least two tic marks.
                final int savenx = numberOfXTics;
                numberOfXTics = 2 + ( chartSize.width / ( maxLabelWidth + 10 ) );
                if ( ( ( numberOfXTics - savenx ) <= 1 ) || ( ( savenx - numberOfXTics ) <= 1 ) ) {
                    break;
                }
            }
        }

        // NOTE: This is a temporary stop-gap to allow a grid resolution twice
        // as fine as the automatically generated grid. This code should be
        // removed (and the associated grid scale variable and methods) once
        // the invoker has been re-implemented to always generate appropriate
        // manual grids/tics.
        numberOfXTics = ( int ) Math.floor( numberOfXTics / gridScale );

        // Compute x increment.
        xStep = MathUtilities.roundUp( ( xTicMax - xTicMin ) / numberOfXTics );

        // Compute x starting point so it is a multiple of xStep.
        xStart = xStep * Math.ceil( xTicMin / xStep );

        // These do not get used unless tics are automatic, but the compiler is
        // not smart enough to allow us to reference them in two distinct
        // conditional clauses unless they are allocated outside the clauses.
        xLabels = new String[ numberOfXTics ];

        // Define the strings that will label the x axis.
        // NOTE: The labels are quantized so that they don't have excess
        // resolution.
        xTicIndex = 0;
        if ( xTics == null ) {
            autoXTics();
        }
    }

    private void generateYTics() {
        // Number of y tic marks.
        // NOTE: subjective spacing factor.
        numberOfYTics = chartSize.height / ( labelHeight * 3 );

        // NOTE: This is a temporary stop-gap to allow a grid resolution twice
        // as fine as the automatically generated grid. This code should be
        // removed (and the associated grid scale variable and methods) once
        // the invoker has been re-implemented to always generate appropriate
        // manual grids/tics.
        numberOfYTics = ( int ) Math.floor( numberOfYTics / gridScale );

        // Compute y increment.
        yStep = MathUtilities.roundUp( ( yTicMax - yTicMin ) / numberOfYTics );

        // Compute y starting point so it is a multiple of yStep.
        yStart = yStep * Math.ceil( yTicMin / yStep );

        // These do not get used unless tics are automatic, but the compiler is
        // not smart enough to allow us to reference them in two distinct
        // conditional clauses unless they are allocated outside the clauses.
        yLabels = new String[ numberOfYTics ];
        yLabelWidths = new int[ numberOfYTics ];

        // Define the strings that will label the y axis. Meanwhile, find the
        // width of the widest label.
        // NOTE: The labels are quantized so that they don't have excess
        // resolution.
        yTicIndex = 0;
        if ( yTics == null ) {
            autoYTics();
        }
    }

    /**
     * Get the user specified aspect ratio, or 0.0 if none has been set.
     *
     * @return The user specified aspect ratio.
     */
    protected final double getAspectRatio() {
        return aspectRatio;
    }

    /*
     * Return the number as a String for use as a label on a logarithmic axis.
     * If this is a log plot, number passed in will not have too many digits to
     * cause problems. If the number is an integer, then we print 1e<num>. If
     * the number is not an integer, then print only the fractional components.
     */
    @SuppressWarnings("nls")
    protected final String getFormattedLoggedNumber( final double number,
                                                     final int numberOfFractionalDigits ) {
        String results;
        final int exponent = ( int ) number;

        // Determine the exponent, prepending 0 or -0 if necessary.
        if ( ( exponent >= 0 ) && ( exponent < 10 ) ) {
            results = "0" + exponent;
        }
        else {
            if ( ( exponent < 0 ) && ( exponent > -10 ) ) {
                results = "-0" + ( -exponent );
            }
            else {
                results = Integer.toString( exponent );
            }
        }

        // Handle the mantissa.
        final double fractionalPart = number % 1;
        if ( number >= 0.0d ) {
            if ( fractionalPart < 0.001 ) {
                results = "1e" + results;
            }
            else {
                results = getFormattedNumber( FastMath.pow( 10.0d, fractionalPart ),
                                              numberOfFractionalDigits );
            }
        }
        else {
            if ( Math.abs( fractionalPart ) < 0.001 ) {
                results = "1e" + results;
            }
            else {
                results = getFormattedNumber( FastMath.pow( 10.0d, ( fractionalPart * 10 ) ),
                                              numberOfFractionalDigits );
            }
        }

        return results;
    }

    /*
     * Returns a string for displaying the specified number using the specified
     * number of digits after the decimal point.
     */
    protected final String getFormattedNumber( final double number,
                                               final int numberOfFractionalDigits ) {
        final int minFracDigits = ( !minFractionDigitsOverride )
            ? numberOfFractionalDigits
            : Math.min( minFractionDigits, numberOfFractionalDigits );
        final int maxFracDigits = ( !maxFractionDigitsOverride )
            ? numberOfFractionalDigits
            : Math.min( maxFractionDigits, numberOfFractionalDigits );
        numberFormat.setMinimumFractionDigits( minFracDigits );
        numberFormat.setMaximumFractionDigits( maxFracDigits );
        return numberFormat.format( number );
    }

    /**
     * Return whether the grid is drawn (if {@code true}) or not.
     *
     * @return {@code true} if a grid is drawn
     */
    public final boolean isGridOn() {
        return gridOn;
    }

    /**
     * Return the grid scale.
     *
     * @return The grid scale
     */
    public double getGridScale() {
        return gridScale;
    }

    /**
     * Return the maximum number of fraction digits for label strings.
     *
     * @return The maximum number of fraction digits.
     */
    public final int getMaximumFractionDigits() {
        return maxFractionDigits;
    }

    /**
     * If the size of the plot has been set by setSize(), then return that size.
     * Otherwise, return what the superclass returns (which is undocumented, but
     * apparently imposes no maximum size). Currently (JDK 1.3), only BoxLayout
     * pays any attention to this.
     *
     * @return The maximum desired size.
     */
    @Override
    public Dimension getMaximumSize() {
        return sizeHasBeenSet
            ? new Dimension( preferredWidth, preferredHeight )
            : super.getMaximumSize();
    }

    /**
     * Return the minimum number of fraction digits for label strings.
     *
     * @return The minimum number of fraction digits.
     */
    public final int getMinimumFractionDigits() {
        return minFractionDigits;
    }

    /**
     * Get the minimum size of this component. This is simply the dimensions
     * specified by setSize(), if this has been called. Otherwise, return
     * whatever the base class returns, which is undocumented.
     *
     * @return The minimum size.
     */
    @Override
    public Dimension getMinimumSize() {
        return sizeHasBeenSet
            ? new Dimension( preferredWidth, preferredHeight )
            : super.getMinimumSize();
    }

    /*
     * Used to find the next value for the axis label. For non-log axes, we just
     * return pos + step. For log axes, we read the appropriate value in the
     * grid Vector, add it to gridBase and return the sum. We also take care to
     * reset gridCurJuke if necessary. Note that for log axes, gridInit() must
     * be called before calling getNextGridStep().
     */
    private final double getNextGridStep( final List< Double > grid,
                                          final double pos,
                                          final double step,
                                          final boolean logflag ) {
        if ( logflag ) {
            final int numberOfGridSteps = grid.size();
            if ( ++gridCurJuke >= numberOfGridSteps ) {
                gridCurJuke = 0;
                gridBase += Math.ceil( step );
            }
            if ( gridCurJuke >= numberOfGridSteps ) {
                return pos + step;
            }
            return gridBase + grid.get( gridCurJuke );
        }

        return pos + step;
    }

    /**
     * Get the preferred size of this component. This is simply the dimensions
     * specified by setSize(), if this has been called, or the default width and
     * height otherwise (500 by 300).
     *
     * @return The preferred size.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension( preferredWidth, preferredHeight );
    }

    /**
     * Get the label for the X (horizontal) axis, or null if none has been set.
     *
     * @return The X label.
     */
    public final String getXLabel() {
        return xLabel;
    }

    /**
     * Return whether the X axis is drawn with a logarithmic scale.
     *
     * @return True if the X axis is logarithmic.
     */
    public final boolean getXLog() {
        return xLog;
    }

    /*
     * Get the X range. The returned value is an array where the first element
     * is the minimum and the second element is the maximum. Return The current
     * X range.
     */
    public final double[] getXRange() {
        final double[] result = new double[ 2 ];
        if ( xRangeGiven ) {
            result[ 0 ] = xLowGiven;
            result[ 1 ] = xHighGiven;
        }
        else {
            // Have to first correct for the padding.
            result[ 0 ] = xMin + ( ( xMax - xMin ) * padding );
            result[ 1 ] = xMax - ( ( xMax - xMin ) * padding );
        }
        return result;
    }

    /**
     * Get the X tics that have been specified, or null if none. The return
     * value is an array with two vectors, the first of which specifies the X
     * tic locations (as instances of Double), and the second of which specifies
     * the corresponding labels.
     *
     * @return The X tics.
     */
    public final List< ? >[] getXTics() {
        if ( xTics == null ) {
            return null;
        }
        final List< ? >[] result = new ArrayList[ 2 ];
        result[ 0 ] = xTics;
        result[ 1 ] = xTicLabels;
        return result;
    }

    /**
     * Get the label for the X (horizontal) units, or null if none has been set.
     *
     * @return The X units.
     */
    public final String getXUnits() {
        return xUnits;
    }

    /**
     * Get the label for the Y (vertical) axis, or null if none has been set.
     *
     * @return The Y label.
     */
    public final String getYLabel() {
        return yLabel;
    }

    /**
     * Return whether the Y axis is drawn with a logarithmic scale.
     *
     * @return True if the Y axis is logarithmic.
     */
    public final boolean getYLog() {
        return yLog;
    }

    /*
     * Gets the Y range. The returned value is an array where the first element
     * is the minimum and the second element is the maximum. return The current
     * Y range.
     */
    public final double[] getYRange() {
        final double[] result = new double[ 2 ];
        if ( yRangeGiven ) {
            result[ 0 ] = yLowGiven;
            result[ 1 ] = yHighGiven;
        }
        else {
            // Have to first correct for the padding.
            result[ 0 ] = yMin + ( ( yMax - yMin ) * padding );
            result[ 1 ] = yMax - ( ( yMax - yMin ) * padding );
        }
        return result;
    }

    /**
     * Get the Y tics that have been specified, or null if none. The return
     * value is an array with two vectors, the first of which specifies the Y
     * tic locations (as instances of Double), and the second of which specifies
     * the corresponding labels.
     *
     * @return The Y tics.
     */
    public final List< ? >[] getYTics() {
        if ( yTics == null ) {
            return null;
        }
        final List< ? >[] result = new ArrayList[ 2 ];
        result[ 0 ] = yTics;
        result[ 1 ] = yTicLabels;
        return result;
    }

    /**
     * Get the units for the Y (vertical) axis, or null if none has been set.
     *
     * @return The Y units.
     */
    public final String getYUnits() {
        return yUnits;
    }

    // Determine what values to use for log axes.
    private final List< Double > gridInit( final double low,
                                             final double step,
                                             final boolean labeled,
                                             final List< Double > oldgrid ) {
        // How log axes work:
        // gridInit() creates a vector with the values to use for the log axes.
        // For example, the vector might contain {0.0 0.301 0.698}, which could
        // correspond to axis labels {1 1.2 1.5 10 12 15 100 120 150}
        //
        // gridStep() gets the proper value. _gridInitialization is cycled
        // through for each integer log value.
        //
        // Bugs in log axes:
        // * Sometimes not enough grid lines are displayed because the region is
        // small. This bug is present in the original xgraph binary, which is
        // the basis of this code. The problem is that as ratio gets closer to
        // 1.0, we need to add more and more grid marks styles.

        final List< Double > grid = new ArrayList<>( 10 );
        // grid.addElement(Double.valueOf(0.0));
        final double ratio = FastMath.pow( 10.0d, step );
        int ngrid = 1;
        if ( labeled ) {
            // Set up the number of grid lines that will be labeled
            if ( ratio <= 3.5d ) {
                if ( ratio > 2.0d ) {
                    ngrid = 2;
                }
                else if ( ratio > 1.26d ) {
                    ngrid = 5;
                }
                else if ( ratio > 1.125d ) {
                    ngrid = 10;
                }
                else {
                    ngrid = ( int ) Math.rint( 1.0d / step );
                }
            }
        }
        else {
            // Set up the number of grid lines that will not be labeled
            if ( ratio > 10.0d ) {
                ngrid = 1;
            }
            else if ( ratio > 3.0d ) {
                ngrid = 2;
            }
            else if ( ratio > 2.0d ) {
                ngrid = 5;
            }
            else if ( ratio > 1.125d ) {
                ngrid = 10;
            }
            else {
                ngrid = 100;
                // NOTE: we should keep going here, but this increases the size
                // of the grid array and slows everything down.
            }
        }

        int oldgridi = 0;
        for ( int i = 0; i < ngrid; i++ ) {
            final double gridval = ( i / ngrid ) * 10.0d;
            double logval = FastMath.log10( gridval );
            if ( logval == Double.NEGATIVE_INFINITY ) {
                logval = 0.0d;
            }

            // If oldgrid is not null, then do not draw lines that were already
            // drawn in oldgrid. This is necessary so we avoid obliterating the
            // tic marks on the plot borders.
            if ( oldgrid != null ) {
                final int numberOfOldGridSteps = oldgrid.size();
                if ( oldgridi < numberOfOldGridSteps ) {
                    // Cycle through the oldgrid until we find an element that
                    // is equal to or greater than the element we are trying to
                    // add.
                    while ( ( oldgridi < numberOfOldGridSteps )
                            && ( oldgrid.get( oldgridi ) < logval ) ) {
                        oldgridi++;
                    }

                    if ( oldgridi < numberOfOldGridSteps ) {
                        // Using == on doubles is bad if the numbers are close,
                        // but not exactly equal.
                        if ( Math.abs( oldgrid.get( oldgridi ) - logval ) > 0.00001d ) {
                            grid.add( logval );
                        }
                    }
                    else {
                        grid.add( logval );
                    }
                }
                else {
                    grid.add( logval );
                }
            }
        }

        // gridCurJuke and gridBase are used in _gridStep();
        gridCurJuke = 0;
        final double lowAdjusted = ( low == -0d ) ? 0.0d : low;
        gridBase = Math.floor( lowAdjusted );
        final double x = lowAdjusted - gridBase;

        // Set gridCurJuke so that the value in grid is greater than
        // or equal to x. This sets us up to process the first point.
        final int numberOfGridSteps = grid.size();
        for ( gridCurJuke = -1; ( ( gridCurJuke + 1 ) < numberOfGridSteps )
                && ( x >= grid.get( gridCurJuke + 1 ) ); gridCurJuke++ ) {}
        return grid;
    }

    private final void initPanel( final String jarRelativeWatermarkIconFilename ) {
        // Load all JAR-resident resources associated with this class.
        loadResources( jarRelativeWatermarkIconFilename );

        // Initialize the Number Formatters, now that we know Locale.
        initNumberFormatters();

        // Create regular stroke patterns.
        defaultStroke = new BasicStroke();
        dashStroke = new HighlightStroke( defaultStroke );
    }

    protected final void initNumberFormatters() {
        // Cache the number formats so that we don't have to get information
        // about locale from the OS each time we format a number.
        numberFormat = NumberFormat.getNumberInstance( Locale.getDefault() );

        // Set the precision for floating-point text formatting.
        numberFormat.setMinimumFractionDigits( 0 );
        numberFormat.setMaximumFractionDigits( 2 );
    }

    /**
     * Return whether the plot applies user specified aspect ratio.
     *
     * @return True if the plot applies user specified aspect ratio.
     */
    public final boolean isAspectRatioApplied() {
        return aspectRatioApplied;
    }

    /**
     * Return whether the X (horizontal) axis exponent is aligned with the lower
     * right corner.
     *
     * @return True if the X (horizontal) axis exponent is aligned with the
     *         lower right corner.
     */
    public final boolean isXExpAligned() {
        return xExpAligned;
    }

    /**
     * Return whether the Y (vertical) axis exponent is aligned with the upper
     * left corner.
     *
     * @return True if the Y (vertical) axis exponent is aligned with the upper
     *         left corner.
     */
    public final boolean isYExpAligned() {
        return yExpAligned;
    }

    // Load all the common JAR-resident resources for the plot.
    protected void loadResources( final String jarRelativeWatermarkIconFilename ) {
        // Demand-load the watermark icon into a thread-safe image icon that is
        // immediately available.
        if ( ( jarRelativeWatermarkIconFilename == null )
                || jarRelativeWatermarkIconFilename.isEmpty() ) {
            return;
        }
        final URL watermarkUrl =
                               CartesianChart.class.getResource( jarRelativeWatermarkIconFilename );
        if ( watermarkUrl != null ) {
            watermarkIcon = new ImageIcon( watermarkUrl );
        }
    }

    /**
     * Measure the various fonts. You only want to call this once.
     */
    @SuppressWarnings("nls")
    @Override
    protected final void measureFonts() {
        // Always call the superclass first!
        super.measureFonts();

        // We only measure the fonts once, and we do it from addNotify().
        // Use SansSerif font as the basis, for consistent look-and-feel
        // across the application (and platforms) and the sharpest text.
        if ( labelFont == null ) {
            labelFont = new Font( "SansSerif", Font.BOLD, 12 );
        }
        if ( superscriptFont == null ) {
            superscriptFont = labelFont.deriveFont( Font.PLAIN, 8 );
        }
        if ( ticFont == null ) {
            ticFont = labelFont.deriveFont( Font.PLAIN, 9 );
        }
        if ( unitsFont == null ) {
            unitsFont = labelFont.deriveFont( Font.ITALIC, 11 );
        }

        labelFontMetrics = getFontMetrics( labelFont );
        superscriptFontMetrics = getFontMetrics( superscriptFont );
        ticFontMetrics = getFontMetrics( ticFont );
        unitsFontMetrics = getFontMetrics( unitsFont );
    }

    /**
     * This method is for dealing with the details of off-screen z-buffering.
     * <p>
     * This method is provided for subclasses to implement, as it is too
     * tricky to change the repaint order to accommodate the unique needs of
     * subclasses otherwise.
     * <p>
     * The details of background image caching and regeneration are
     * context-dependent and not applicable towards all panels, so this base
     * class implementation treats this as a no-op.
     *
     * @param graphics2D
     *            The Graphics Context to use for off-screen z-buffering
     *
     * @since 1.0
     */
    public void paintBackgroundImage( final Graphics2D graphics2D ) {}

    /**
     * This is the preferred repaint method to override and invoke in Swing.
     * <p>
     * The reason for overriding it here, is so can take into account whether
     * EPS Export is active or we are just refreshing the screen.
     * <p>
     * If this is done at initialization time, we have a null Graphics Context.
     *
     * @param graphicsContext
     *            The Graphics Context to use as the canvas for rendering this
     *            component
     *
     * @since 1.0
     */
    @Override
    public void paintComponent( final Graphics graphicsContext ) {
        // Validate ranges regardless of mode, as everything else is calculated
        // off of these limits.
        validateRanges();

        // Create an off-screen graphics buffer, or use an existing Graphics
        // Context instance as the paint target (if vectorization is active).
        final Graphics2D graphics2D = isVectorizationActive()
            ? ( Graphics2D ) graphicsContext
            : createGraphics( graphicsContext );
        if ( graphics2D != null ) {
            // Redraw the background image before displaying it if anything has
            // changed; otherwise, just re-display it.
            //
            // We have to call the "super" class first, to make sure we preserve
            // the look-and-feel of the owner component.
            //
            // Due to off-screen buffering of superclass graphics, it is not
            // always necessary to repaint everything.
            //
            // We should rarely ever need to paint the background image for EPS
            // Export, as it is the entire panel and not the overlay elements
            // such as foreground images and stacked layout content.
            if ( !isVectorizationActive() ) {
                super.paintComponent( graphics2D );
            }

            // First draw the panel decoration before the background image.
            //
            // This method is sub-classed in order to take advantage of
            // off-screen buffering for the entire hierarchy.
            drawDecorations( graphics2D );

            // Paint the background image, if relevant -- this is the only way
            // to get vector graphics to sit atop a raster image vs. using XOR
            // Mode and getting weird psychedelic effects.
            paintBackgroundImage( graphics2D );

            // Now draw the panel layout elements atop the background image.
            //
            // This method is sub-classed in order to take advantage of
            // off-screen buffering for the entire hierarchy.
            drawElements( graphics2D );

            // Dispose of Graphics Context now that we have finished with it.
            if ( !isVectorizationActive() ) {
                graphics2D.dispose();
            }
        }

        // Show new background image, or re-display the old background image.
        //
        // Note that EPS Export needs to maintain line vector graphics vs.
        // taking advantage of efficiencies in off-screen buffering.
        if ( !isVectorizationActive() ) {
            showBackgroundImage( graphicsContext );
        }
    }

    /**
     * This is a wrapper method for drawing decorations onto a supplied
     * Graphics Context. It is provided to help with decoupling of dependencies.
     *
     * @param graphics2D
     *            The Graphics Context to use for drawing the decorations
     *
     * @since 1.0
     */
    public void drawDecorations( final Graphics2D graphics2D ) {
        // Readjust axes and labels to account for aspect ratio, etc.
        final Rectangle bounds = getBounds();
        adjustAxesAndLabelCoordinates( bounds );

        // Generate the tics and their labels.
        generateTics();
    }

    /**
     * This is a wrapper method for drawing relevant elements onto a supplied
     * Graphics Context. It is provided to help with decoupling of dependencies.
     *
     * @param graphicsContext
     *            The Graphics Context to use for drawing the elements
     *
     * @since 1.0
     */
    public void drawElements( final Graphics2D graphicsContext ) {
        // Draw the Cartesian Plot elements atop the background image.
        // NOTE: This method is sub-classed in order to take advantage of
        // off-screen buffering for the entire hierarchy.
        drawChart( graphicsContext );

        // In this simple sample panel, this just invokes the super-class's
        // paintComponent() method, but it is still safer to encapsulate this in
        // a method that can be invoked from the override of paintComponent()
        // so that EPS Export can change the order of processing if necessary,
        // such as for dealing with the paint order of background images vs.
        // vector graphics that must sit atop such images.
        // NOTE: Commented out in this context; this code was moved from a
        // sample app and is retained in case we ever need to move this up the
        // GUI hierarchy.
        if ( isVectorizationActive() ) {
            // super.paintComponent( graphics2D );
        }
    }

    /**
     * Reset the X and Y axes to the ranges that were first specified using
     * setXRange() and setYRange(). If these methods have not been called, then
     * reset to the default ranges. This method calls repaint(), which
     * eventually causes the display to be updated.
     */
    public final void resetAxes() {
        setXRange( originalXLow, originalXHigh );
        setYRange( originalYLow, originalYHigh );
        repaint();
    }

    /**
     * Set the user specified aspect ratio.
     *
     * @param newAspectRatio
     *            The new user specified aspect ratio.
     */
    protected final void setAspectRatio( final double newAspectRatio ) {
        aspectRatio = newAspectRatio;
    }

    /**
     * If the argument is true, apply the user specified aspect ratio.
     * Otherwise, use the aspect ratio that maximizes this component (the
     * default).
     *
     * @param newAspectRatioApplied
     *            {@code true} to apply the user specified aspect ratio.
     */
    protected final void setAspectRatioApplied( final boolean newAspectRatioApplied ) {
        aspectRatioApplied = newAspectRatioApplied;
    }

    /**
     * Control whether the grid is drawn.
     *
     * @param gridIsOn
     *            If {@code true}, a grid is drawn.
     */
    protected final void setGrid( final boolean gridIsOn ) {
        gridOn = gridIsOn;

        // Any grid state change requires regenerating the off-screen buffer.
        regenerateOffScreenImage = true;
    }

    /**
     * Set the grid scale.
     *
     * @param newGridScale
     *            The new grid scale.
     */
    protected final void setGridScale( final double newGridScale ) {
        gridScale = newGridScale;

        // Any grid state change requires regenerating the off-screen buffer.
        regenerateOffScreenImage = true;
    }

    /**
     * Set the maximum number of fraction digits for label strings.
     *
     * @param newMaxFractionDigits
     *            The maximum number of fraction digits.
     */
    protected final void setMaximumFractionDigits( final int newMaxFractionDigits ) {
        maxFractionDigitsOverride = true;
        maxFractionDigits = newMaxFractionDigits;
    }

    /**
     * Set the minimum number of fraction digits for label strings.
     *
     * @param newMinFractionDigits
     *            The minimum number of fraction digits.
     */
    protected final void setMinimumFractionDigits( final int newMinFractionDigits ) {
        minFractionDigitsOverride = true;
        minFractionDigits = newMinFractionDigits;
    }

    /**
     * Set the padding multiple. The plot rectangle can be "padded" in each
     * direction -x, +x, -y, and +y. If the padding is set to 0.05 (and the
     * padding is used), then there is 10% more length on each axis than set by
     * the setXRange() and setYRange() methods, 5% in each direction.
     *
     * @param newPadding
     *            The new padding multiple.
     */
    protected final void setPadding( final double newPadding ) {
        padding = newPadding;
    }

    /**
     * Set the size of the chart. This overrides the base class to make it work.
     * In particular, it records the specified size so that getMinimumSize() and
     * getPreferredSize() return the specified value. However, it only works if
     * the plot is placed in its own JPanel. This is because the JPanel asks the
     * contained component for its preferred size before determining the size of
     * the panel. If the plot is placed directly in the content pane of a
     * JApplet, then, mysteriously, this method has no effect.
     *
     * @param width
     *            The width, in pixels.
     * @param height
     *            The height, in pixels.
     */
    @Override
    public void setSize( final int width, final int height ) {
        preferredWidth = width;
        preferredHeight = height;
        sizeHasBeenSet = true;
        super.setSize( width, height );
    }

    /**
     * Specify whether the X (horizontal) axis exponent is aligned with the
     * lower right corner.
     *
     * @param newXExpAligned
     *            If {@code true}, the X (horizontal) axis exponent is aligned
     *            with the lower right corner.
     */
    public final void setXExpAligned( final boolean newXExpAligned ) {
        xExpAligned = newXExpAligned;
    }

    /**
     * Set the label for the X (horizontal) axis.
     *
     * @param label
     *            The label.
     */
    public final void setXLabel( final String label ) {
        xLabel = label;
    }

    /**
     * Specify whether the X axis is drawn with a logarithmic scale.
     *
     * @param newXLog
     *            If {@code true}, logarithmic axis is used.
     */
    public final void setXLog( final boolean newXLog ) {
        xLog = newXLog;
    }

    /**
     * Set the X (horizontal) range of the plot. If this is not done explicitly,
     * then the range is computed automatically from data available when the
     * plot is drawn. If min and max are identical, then the range is
     * arbitrarily spread by 1.
     *
     * @param min
     *            The left extent of the range.
     * @param max
     *            The right extent of the range.
     */
    public final void setXRange( final double min, final double max ) {
        xRangeGiven = true;
        xLowGiven = min;
        xHighGiven = max;
        if ( !originalXRangeGiven ) {
            originalXLow = min;
            originalXHigh = max;
            originalXRangeGiven = true;
        }

        setXRangeWithAutorange( min, max );

        // Any change to the scaling requires regenerating the off-screen
        // buffer.
        regenerateOffScreenImage = true;
    }

    /*
     * Internal implementation of setXRange, so that it can be called when
     * auto-ranging.
     */
    public final void setXRangeWithAutorange( final double min, final double max ) {
        // If values are invalid, try for something reasonable.
        double minAdjusted = min;
        double maxAdjusted = max;
        if ( min > max ) {
            minAdjusted = -1d;
            maxAdjusted = 1.0d;
        }
        else if ( min == max ) {
            minAdjusted -= 1.0d;
            maxAdjusted += 1.0d;
        }

        if ( xRangeGiven ) {
            // The user specified the range, so don't pad.
            xMin = minAdjusted;
            xMax = maxAdjusted;
        }
        else {
            // Pad slightly so that we don't plot points on the axes.
            xMin = minAdjusted - ( ( maxAdjusted - minAdjusted ) * padding );
            xMax = maxAdjusted + ( ( maxAdjusted - minAdjusted ) * padding );
        }

        // Find the exponent for the largest magnitude x value.
        final double largest = Math.max( Math.abs( xMin ), Math.abs( xMax ) );
        xExp = ( int ) Math.floor( FastMath.log10( largest ) );

        // Use the exponent only if it's larger than 3.
        if ( xExp > 3 ) {
            final double xs = 1.0d / FastMath.pow( 10.0d, xExp );
            xTicMin = xMin * xs;
            xTicMax = xMax * xs;
        }
        else {
            // Otherwise we must use different criteria (based on range) to
            // determine the exponent when the interim values may need many
            // decimal places to generate unique labels.
            // NOTE: Even this can result in large tic labels, so we are
            // disabling negative exponents for now.
            // double xdiff = Math.abs(xMax - xMin);
            // xExp = (int) Math.floor(FastMath.log10(xdiff));

            // Use the exponent only if it's smaller than -1. This doesn't cover
            // the worst case scenario of a single tic separating two values
            // values that differ by less then 1.0 or so, but that proved
            // confusing as it results in large magnitude tic labels. This seems
            // a good compromise solution.
            // if (xExp < -1) {
            // double xs = 1.0/FastMath.pow(10.0, (double)xExp);
            // xTicMin = xMin*xs;
            // xTicMax = xMax*xs;
            // } else {
            xTicMin = xMin;
            xTicMax = xMax;
            xExp = 0;
            // }
        }
    }

    /**
     * Set the units for the X (horizontal) axis.
     *
     * @param units
     *            The units.
     */
    @SuppressWarnings("nls")
    public final void setXUnits( final String units ) {
        xUnits = units;
        xUnitsSublabel = ( xUnits == null ) ? "" : " (" + xUnits + ")";
    }

    /**
     * Specify whether the Y (vertical) axis exponent is aligned with the upper
     * left corner.
     *
     * @param newYExpAligned
     *            If {@code true}, the Y (vertical) axis exponent is aligned
     *            with the upper left corner.
     */
    public final void setYExpAligned( final boolean newYExpAligned ) {
        yExpAligned = newYExpAligned;
    }

    /**
     * Set the label for the Y (vertical) axis.
     *
     * @param label
     *            The label.
     */
    public final void setYLabel( final String label ) {
        yLabel = label;
    }

    /**
     * Specify whether the Y axis is drawn with a logarithmic scale.
     *
     * @param newYLog
     *            If {@code true}, logarithmic axis is used.
     */
    public final void setYLog( final boolean newYLog ) {
        yLog = newYLog;
    }

    /**
     * Set the Y (vertical) range of the plot. If this is not done explicitly,
     * then the range is computed automatically from data available when the
     * plot is drawn. If min and max are identical, then the range is
     * arbitrarily spread by 0.1.
     *
     * @param min
     *            The bottom extent of the range.
     * @param max
     *            The top extent of the range.
     */
    public final void setYRange( final double min, final double max ) {
        yRangeGiven = true;
        yLowGiven = min;
        yHighGiven = max;
        if ( !originalYRangeGiven ) {
            originalYLow = min;
            originalYHigh = max;
            originalYRangeGiven = true;
        }
        setYRangeWithAutorange( min, max );

        // Any change to the scaling requires regenerating the off-screen
        // buffer.
        regenerateOffScreenImage = true;
    }

    /*
     * Internal implementation of setYRange, so that it can be called when
     * auto-ranging.
     */
    public final void setYRangeWithAutorange( final double min, final double max ) {
        // If values are invalid, try for something reasonable.
        double minAdjusted = min;
        double maxAdjusted = max;
        if ( min > max ) {
            minAdjusted = -1d;
            maxAdjusted = 1.0d;
        }
        else if ( min == max ) {
            minAdjusted -= 1.0d;
            maxAdjusted += 1.0d;
        }

        if ( yRangeGiven ) {
            // The user specified the range, so don't pad.
            yMin = minAdjusted;
            yMax = maxAdjusted;
        }
        else {
            // Pad slightly so that we don't plot points on the axes.
            yMin = minAdjusted - ( ( maxAdjusted - minAdjusted ) * padding );
            yMax = maxAdjusted + ( ( maxAdjusted - minAdjusted ) * padding );
        }

        // Find the exponent for the largest magnitude y value.
        final double largest = FastMath.max( Math.abs( yMin ), FastMath.abs( yMax ) );
        yExp = ( int ) FastMath.floor( FastMath.log10( largest ) );

        // Use the exponent only if it's larger than 3.
        if ( yExp > 3 ) {
            final double ys = 1.0d / FastMath.pow( 10.0d, yExp );
            yTicMin = yMin * ys;
            yTicMax = yMax * ys;
        }
        else {
            // Otherwise we must use different criteria (based on range) to
            // determine the exponent when the interim values may need many
            // decimal places to generate unique labels.
            // NOTE: Even this can result in large tic labels, so we
            // are disabling negative exponents for now.
            // double yDiff = Math.abs(yMax - yMin);
            // yExp = (int) Math.floor(FastMath.log10(yDiff));

            // Use the exponent only if it's smaller than -1. This doesn't cover
            // the worst case scenario of a single tic separating two values
            // values that differ by less then 1.0 or so, but that proved
            // confusing as it results in large magnitude tic labels. This seems
            // a good compromise solution.
            // if (yExp < -1) {
            // double ys = 1.0/FastMath.pow(10.0, (double)yExp);
            // yTicMin = yMin*ys;
            // yTicMax = yMax*ys;
            // } else {
            yTicMin = yMin;
            yTicMax = yMax;
            yExp = 0;
            // }
        }
    }

    /**
     * Set the units for the Y (vertical) axis.
     *
     * @param units
     *            The units.
     */
    @SuppressWarnings("nls")
    public final void setYUnits( final String units ) {
        yUnits = units;
        yUnitsSublabel = ( yUnits == null ) ? "" : " (" + yUnits + ")";
    }

    private final void validateRanges() {
        // Make sure we have an x and y range
        if ( !xRangeGiven ) {
            if ( xBottom > xTop ) {
                // have nothing to go on.
                setXRangeWithAutorange( 0, 0 );
            }
            else {
                setXRangeWithAutorange( xBottom, xTop );
            }
        }
        if ( !yRangeGiven ) {
            if ( yBottom > yTop ) {
                // have nothing to go on.
                setYRangeWithAutorange( 0, 0 );
            }
            else {
                setYRangeWithAutorange( yBottom, yTop );
            }
        }
    }

    /**
     * Zoom in or out to the specified rectangle. This method calls repaint().
     *
     * @param lowx
     *            The low end of the new X range.
     * @param lowy
     *            The low end of the new Y range.
     * @param highx
     *            The high end of the new X range.
     * @param highy
     *            The high end of the new Y range.
     */
    protected void zoom( final double lowx,
                         final double lowy,
                         final double highx,
                         final double highy ) {
        setXRangeWithAutorange( lowx, highx );
        setYRangeWithAutorange( lowy, highy );

        // Any new zoom range requires regenerating the off-screen buffer.
        regenerateOffScreenImage = true;

        repaint();
    }

}
