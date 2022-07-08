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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

import com.mhschmieder.graphicstoolkit.color.ColorUtilities;
import com.mhschmieder.graphicstoolkit.font.FontUtilities;
import com.mhschmieder.mathtoolkit.MathConstants;

/**
 * Class <code>SemiLogRPolarChart</code> contains methods to plot <i>(x,y)</i>
 * polar data values.
 * <p>
 * This is a lightweight component suitable for use with Swing GUI components.
 * <p>
 * This class supports the input of a single <code>double</code> array, or two
 * <code>double</code> arrays. If a single <code>double</code> array is
 * supplied, the elements of that array will be plotted as a function of their
 * array indices. If two single <code>double</code> arrays are supplied, the
 * first array will provide the <i>x</i> coordinate and the second array will
 * provide the <i>y</i> coordinate.
 * <p>
 * Features of this class include
 * <p>
 * 1. Grid lines.
 * <p>
 * 2. User-settable tic mark locations.
 * <p>
 * 3. User-settable chartTitle and axis labels.
 * <p>
 * Since this class extends <tt>JComponent</tt>, it can be used with either
 * applications or applets.
 * <p>
 * <b>Note 1:</b> The <i>(x,y)</i> values are interpreted as <i>(r,theta)</i>,
 * where <i>r</i> is the radial distance from the origin of the plot, and
 * <i>theta</i> is the angle in radians, counterclockwise from the positive
 * x-axis. Also, the input angles must be specified in <i>radians</i>, but the
 * angle labels on the plot are in <i>degrees</i>.
 * <p>
 * <b>Note 2:</b> If an <i>x</i> or <i>y</i> axis is logarithmic, then zero or
 * negative values on that axis will produce a <tt>RuntimeException</tt>.
 */
public class SemiLogRPolarChart extends Chart {
    /**
     *
     */
    private static final long serialVersionUID      = 3817582450900853248L;

    // Constants containing Line Style descriptors.
    public static final float LINESTYLE_SOLID[]     = { 6f, 0f };
    public static final float LINESTYLE_DOT[]       = { 3f, 6f };
    public static final float LINESTYLE_LONGDASH[]  = { 16f, 6f };
    public static final float LINESTYLE_SHORTDASH[] = { 8f, 6f };

    // Default grid spacing is 6 dB / div.
    public static final int   DEFAULT_GRID_SPACING  = 6;

    /**
     * Return the chart line color default.
     *
     * @param backColor
     *            The graphics background color.
     *
     * @version 0.1
     */
    public static final Color getDefaultChartLineColor( final Color backColor ) {
        // Set the chart line label color to cyan (if dark background) or blue
        // (if light background), so that the lines show up better against the
        // grid and curves. We are careful to pick colors unused elsewhere.
        return ColorUtilities.isColorDark( backColor ) ? Color.CYAN : Color.BLUE;
    }

    /**
     * Return the tic label color default.
     *
     * @param backColor
     *            The graphics background color.
     *
     * @version 0.1
     */
    public static final Color getDefaultTicLabelColor( final Color backColor ) {
        // Set the tic label color to yellow (if dark background) or magenta
        // (if light background), so that the labels show up better against the
        // grid and curves. We are careful to pick colors unused elsewhere.
        return ColorUtilities.isColorDark( backColor ) ? Color.YELLOW : Color.MAGENTA;
    }

    /**
     * Flag to indicate that the chart is empty (no data, grid only)
     */
    private boolean     emptyChart;

    // Colors for plot lines and other feature subsets.
    private Color       chartBoundaryColor = Color.BLACK;
    private Color       axisLabelColor     = Color.BLACK;
    private Color       ticLabelcolor      = Color.MAGENTA;
    private Color       chartLineColor     = Color.BLUE;

    // Strings for labels, units, etc.
    private String      xLabel             = "X label";           //$NON-NLS-1$
    private String      xUnits             = "X units";           //$NON-NLS-1$

    /**
     * Flag to indicate that a valid x-label is present.
     */
    private boolean     validXLabel        = false;

    /**
     * Flag to indicate that a valid x-units is present.
     */
    private boolean     validXUnits        = false;

    // Stroke styles for different elements of the chart.
    private BasicStroke chartBoundaryStroke;
    private BasicStroke gridStroke;
    private BasicStroke chartLineStroke;

    // Grid range, spacing, count, etc.
    private float       gridRangeX         = 48f;
    private float       gridRangeY         = 48f;
    private int         gridSpacing        = DEFAULT_GRID_SPACING;
    private int         gridWidth          = 21;
    private int         gridDepth          = 21;

    /**
     * This variable contains the minimum user-defined x value to map into
     * pixels within the "plot" area. The user-defined coordinates to plot are
     * specified by the bounds <tt>xMax</tt>, <tt>xMin</tt>, <tt>yMax</tt>,
     * and <tt>yMin</tt>, plus the scale factors <tt>xScale</tt> and
     * <tt>yScale</tt>.
     */
    public double       xMin               = 0.0d;

    /**
     * This variable contains the logarithm of the minimum user-defined x value
     * to map into pixels within the "plot" area.
     */
    public double       xMinLog            = 0.0d;

    /**
     * This variable contains the minimum user-defined y value to map into
     * pixels within the "plot" area. The user-defined coordinates to plot are
     * specified by the bounds <tt>xMax</tt>, <tt>xMin</tt>, <tt>yMax</tt>,
     * and <tt>yMin</tt>, plus the scale factors <tt>xScale</tt> and
     * <tt>yScale</tt>.
     */
    public double       yMin               = 0.0d;

    /**
     * This variable contains the logarithm of the minimum user-defined y value
     * to map into pixels within the "plot" area.
     */
    public double       yMinLog            = 0.0d;

    /**
     * This variable contains the maximum user-defined x value to map into
     * pixels within the "plot" area. The user-defined coordinates to plot are
     * specified by the bounds <tt>xMax</tt>, <tt>xMin</tt>, <tt>yMax</tt>,
     * and <tt>yMin</tt>, plus the scale factors <tt>xScale</tt> and
     * <tt>yScale</tt>.
     */
    public double       xMax               = 0.0d;

    /**
     * This variable contains the logarithm of the maximum user-defined x value
     * to map into pixels within the "plot" area.
     */
    public double       xMaxLog            = 0.0d;

    /**
     * This variable contains the maximum user-defined y value to map into
     * pixels within the "plot" area. The user-defined coordinates to plot are
     * specified by the bounds <tt>xMax</tt>, <tt>xMin</tt>, <tt>yMax</tt>,
     * and <tt>yMin</tt>, plus the scale factors <tt>xScale</tt> and
     * <tt>yScale</tt>.
     */
    public double       yMax               = 0.0d;

    /**
     * This variable contains the logarithm of the maximum user-defined y value
     * to map into pixels within the "plot" area.
     */
    public double       yMaxLog            = 0.0d;

    /**
     * This variable contains the scale factor for mapping the user-defined
     * x-coordinates of the supplied curve(s) into pixels within the "plot"
     * area. The user-defined coordinates to plot are specified by the bounds
     * <tt>xMax</tt>, <tt>xMin</tt>, <tt>yMax</tt>, and <tt>yMin</tt>, plus
     * the scale factors <tt>xScale</tt> and <tt>yScale</tt>.
     */
    public double       xScale;

    /**
     * This variable contains the scale factor for mapping the user-defined
     * y-coordinates of the supplied curve(s) into pixels within the "plot"
     * area. The user-defined coordinates to plot are specified by the bounds
     * <tt>xMax</tt>, <tt>xMin</tt>, <tt>yMax</tt>, and <tt>yMin</tt>, plus
     * the scale factors <tt>xScale</tt> and <tt>yScale</tt>.
     */
    public double       yScale;

    /**
     * This array specifies the locations of y-axis tic marks in user-defined
     * coordinates.
     */
    private double      yTics[];

    /**
     * This array character strings corresponding to the values of y-axis tic
     * marks in user-defined coordinates.
     */
    private String      yTicLabels[];

    /**
     * This array contains the x-values of the curves to plot, in user-defined
     * coordinates.
     */
    public double       xValues[];

    /**
     * This array contains the y-values of the curves to plot, in user-defined
     * coordinates.
     */
    public double       yValues[];

    /**
     * This array contains the normalized x-values of the curves to plot, in
     * user-defined coordinates.
     */
    private double      xValuesNormalized[];

    /**
     * Number format cache used for locale-specific number formatting.
     */
    public NumberFormat numberFormat;

    /**
     * Constructs an uninitialized {@code SemiLogRPolarChart} with no data.
     *
     * @version 0.1
     */
    public SemiLogRPolarChart() {
        // Always call the superclass constructor first!
        super();

        emptyChart = true;

        // Avoid constructor failure by wrapping the layout initialization in an
        // exception handler that logs the exception and then returns an object.
        try {
            initPanel();
        }
        catch ( final Exception ex ) {
            ex.printStackTrace();
        }
    }

    /**
     * Calculates and returns the default location of the concentric circles on
     * a logged polar plot. It selects nice round numbers for the tic marks, and
     * restricts the total number of tic marks to 11 or less. Because polar
     * plots get "busy" with too many rings, this method further reduces the
     * number of tics if the number is evenly divisible by 2 or 3. The resulting
     * tic mark locations are stored directly into instance variable yTics.
     *
     * @param ymin
     *            The minimum value to display
     * @param ymax
     *            The maximum value to display
     * @param ticrange
     *            The desired tic range for the plot
     * @param ticinc
     *            The desired tic increment for the plot
     * @return The amount by which the data was shifted
     *
     * @version 0.1
     */
    private final double calculatePolarCirclesLogged( final double ymin,
                                                      final double ymax,
                                                      final double ticrange,
                                                      final double ticinc ) {
        // double delta; // Step between tics
        double high; // Highest value to plot
        double low; // Lowest value to plot
        int tics; // Number of tic marks

        // Compute fit based on desired tick increment passed in.
        // NOTE: In case of uniform data sets or other anomalies, we must
        // ensure that the plot range goes to the first positive tick increment.
        // This, however, needs to be more modular so that it does not assume
        // logged data (once we merge the two calcPolarCircles methods again).
        // NOTE: If a non-zero dynamic range was passed it, we use it to adjust
        // the display range, as this makes the plots consistent regardless of
        // the actual dynamic range of the data, for quick comparisons (that is,
        // uniform scale!).
        low = ticinc * Math.floor( ymin / ticinc );
        high = ticinc * Math.ceil( ymax / ticinc );
        while ( high <= 0.0d ) {
            high += ticinc;
        }
        if ( ticrange != 0.0d ) {
            low = high - ticrange;
        }
        tics = ( int ) Math.round( ( high - low ) / ticinc ) + 1;

        // Adjust the data bounds such that the rounded lower bound is at zero,
        // since polar plotting assumes positive coordinates.
        if ( low < 0.0d ) {
            yMin = 0.0d;
            yMax = high - low;
        }
        else {
            yMin = low;
            yMax = high;
        }

        // Calculate locations of tick marks, and adjust labels for the true
        // magnitudes vs. the shifted/normalized magnitudes.
        yTics = new double[ tics ];
        yTicLabels = new String[ tics ];
        for ( int j = 0; j < yTics.length; j++ ) {
            yTics[ j ] = yMin + ( ticinc * j );
            yTicLabels[ j ] = numberFormat.format( ( low + ( ticinc * j ) ) );
        }

        // Return the amount by which the data was shifted.
        return -low;
    }

    public final void clearResponse() {
        // TODO: Implement the data clear method.
        // clear();
        regenerateOffScreenImage = true;
        repaint();
    }

    /**
     * Tests for containment of a point within the boundaries of this chart.
     *
     * @param point2D
     *            A point in screen coordinates.
     * @return {@code true} if the point is contained within the boundaries of
     *         this chart
     *
     * @version 0.1
     */
    protected final boolean contains( final Point2D point2D ) {
        final Point point = new Point( ( int ) Math.round( point2D.getX() ),
                                       ( int ) Math.round( point2D.getY() ) );
        return super.contains( point );
    }

    // Draw the bounding circle.
    private final void drawBoundingCircle( final Graphics2D g2 ) {
        // Cache the current color and stroke to restore later.
        final Color color = g2.getColor();
        final Stroke stroke = g2.getStroke();

        // Set the stroke for the bounding circle -- a thin solid line.
        g2.setColor( chartBoundaryColor );
        g2.setStroke( chartBoundaryStroke );

        // Add the bounding circle.
        // NOTE: We _changed to yMax and y-scaling on the x-values, to ensure
        // circles vs. ellipses. This should be the right thing to do, because
        // only the y-values project into polar coordinates.
        final double x1 = yMax * yScale;
        final double y1 = yMax * yScale;
        final double w1 = 2.0d * yMax * yScale;
        final double h1 = 2.0d * yMax * yScale;
        final Ellipse2D ellipse = new Ellipse2D.Double( ( ulx
                + Math.round( 0.5d * chartSize.width ) )
                - x1, ( uly + Math.round( 0.5d * chartSize.height ) ) - y1, w1, h1 );
        g2.draw( ellipse );

        // Restore the previous color and stroke.
        g2.setColor( color );
        g2.setStroke( stroke );
    }

    // Draw the grid lines.
    private final void drawGridLines( final Graphics2D g2 ) {
        // Cache the current color and stroke to restore later.
        final Color color = g2.getColor();
        final Stroke stroke = g2.getStroke();

        // Set the stroke for the grid line -- a thin dotted line.
        g2.setColor( gridColor );
        g2.setStroke( gridStroke );

        // Draw the radial lines.
        // NOTE: We _changed to yMax and y-scaling on the x-values, to ensure
        // circles vs. ellipses.
        // NOTE: This is a bit of a hack, to select 1/4 to 5/8 radial distance
        // for the switch-over from 5 to 10 degree increments, vs. 1/3 to 2/3
        // radial distance.
        final double outerRatio = ( ( int ) Math.IEEEremainder( ( yTics.length - 1 ), 4.0d ) == 0 )
            ? MathConstants.FIVE_EIGHTHS
            : MathConstants.TWO_THIRDS;
        final double innerRatio = ( ( int ) Math.IEEEremainder( ( yTics.length - 1 ), 4.0d ) == 0 )
            ? MathConstants.ONE_FOURTH
            : MathConstants.ONE_THIRD;

        // Draw every five degrees for the outer rings.
        for ( int i = 0; i < 360; i = i + 5 ) {
            final double temp1 = yMax * Math.sin( Math.toRadians( i ) );
            final double temp2 = yMax * Math.cos( Math.toRadians( i ) );
            final Line2D line = new Line2D.Double(
                                                   ulx + ( 0.5d * chartSize.width )
                                                           + ( outerRatio * temp1 * yScale ),
                                                   uly + ( 0.5d * chartSize.height )
                                                           + ( outerRatio * temp2 * yScale ),
                                                   ulx + ( 0.5d * chartSize.width )
                                                           + ( temp1 * yScale ),
                                                   uly + ( 0.5d * chartSize.height )
                                                           + ( temp2 * yScale ) );
            g2.draw( line );
        }

        // Draw every ten degrees for the center rings.
        for ( int i = 0; i < 360; i = i + 10 ) {
            final double temp1 = yMax * Math.sin( Math.toRadians( i ) );
            final double temp2 = yMax * Math.cos( Math.toRadians( i ) );
            final Line2D line = new Line2D.Double(
                                                   ulx + ( 0.5d * chartSize.width )
                                                           + ( innerRatio * temp1 * yScale ),
                                                   uly + ( 0.5d * chartSize.height )
                                                           + ( innerRatio * temp2 * yScale ),
                                                   ulx + ( 0.5d * chartSize.width )
                                                           + ( outerRatio * temp1 * yScale ),
                                                   uly + ( 0.5d * chartSize.height )
                                                           + ( outerRatio * temp2 * yScale ) );
            g2.draw( line );
        }

        // Draw every forty-five degrees to link the labels to the center.
        for ( int i = 0; i < 360; i = i + 45 ) {
            final double temp1 = yMax * Math.sin( Math.toRadians( i ) );
            final double temp2 = yMax * Math.cos( Math.toRadians( i ) );
            final Line2D line = new Line2D.Double( ulx + ( 0.5d * chartSize.width ),
                                                   uly + ( 0.5d * chartSize.height ),
                                                   ulx + ( 0.5d * chartSize.width )
                                                           + ( temp1 * yScale ),
                                                   uly + ( 0.5d * chartSize.height )
                                                           + ( temp2 * yScale ) );
            g2.draw( line );
        }

        // Draw the concentric circles.
        // NOTE: We changed to y-scaling on the x-values, to ensure circles
        // vs. ellipses.
        for ( int i = 1; i < ( yTics.length - 1 ); i++ ) {
            final Ellipse2D.Double ellipse =
                                           new Ellipse2D.Double( ( ulx
                                                   + ( 0.5d * chartSize.width ) )
                                                   - ( yTics[ i ] * yScale ),
                                                                 ( uly + ( 0.5d
                                                                         * chartSize.height ) )
                                                                         - ( yTics[ i ] * yScale ),
                                                                 2.0d * yTics[ i ] * yScale,
                                                                 2.0d * yTics[ i ] * yScale );
            g2.draw( ellipse );
        }

        // Restore the previous color and stroke.
        g2.setColor( color );
        g2.setStroke( stroke );
    }

    // Draw a polar chart.
    @Override
    public final void drawChart( final Graphics2D graphicsContext ) {
        // Ignore if there is no graphics context to draw on.
        if ( graphicsContext == null ) {
            return;
        }

        // Draw the bounding circle, grid lines, and tic marks/labels.
        drawPolarGridAndTics( graphicsContext );

        // Draw the title and labels.
        drawTitleAndLabels( graphicsContext );

        // Plot the data points and connect them with lines.
        drawChartLines( graphicsContext );
    }

    // Plot the data points and connect them with lines.
    private final void drawChartLines( final Graphics2D graphicsContext ) {
        if ( emptyChart ) {
            return;
        }

        // Cache the current color and stroke to restore later.
        final Color color = graphicsContext.getColor();
        final Stroke stroke = graphicsContext.getStroke();

        // Set up stroke, color, etc.
        graphicsContext.setColor( chartLineColor );
        graphicsContext.setStroke( chartLineStroke );

        double x1, y1, x2, y2;
        for ( int j = 0; j < ( xValuesNormalized.length - 1 ); j++ ) {
            // The convention for polar charts is for 0 degrees to point north
            // and 90 degrees to point east, whereas the convention for linear
            // charts is for 0 degrees to point east and 90 degrees to point
            // north.
            x1 = xValuesNormalized[ j ] * ( Math.sin( yValues[ j ] ) * yScale );
            y1 = xValuesNormalized[ j ] * ( Math.cos( yValues[ j ] ) * yScale );
            x2 = xValuesNormalized[ j + 1 ] * ( Math.sin( yValues[ j + 1 ] ) * yScale );
            y2 = xValuesNormalized[ j + 1 ] * ( Math.cos( yValues[ j + 1 ] ) * yScale );
            final Line2D.Double line =
                                     new Line2D.Double( ulx + ( 0.5d * chartSize.width ) + x1,
                                                        ( uly + ( 0.5d * chartSize.height ) ) - y1,
                                                        ulx + ( 0.5d * chartSize.width ) + x2,
                                                        ( uly + ( 0.5d * chartSize.height ) )
                                                                - y2 );
            graphicsContext.draw( line );
        }

        // Restore the previous color and stroke.
        graphicsContext.setColor( color );
        graphicsContext.setStroke( stroke );
    }

    // Draw the bounding circle, grid lines, and tic mark labels.
    private final void drawPolarGridAndTics( final Graphics2D graphicsContext ) {
        // Draw the bounding circle.
        drawBoundingCircle( graphicsContext );

        // Draw the grid lines.
        drawGridLines( graphicsContext );

        // Conditionally draw the tic mark labels.
        drawTicMarkLabels( graphicsContext );
    }

    // Draw the tic mark labels.
    @SuppressWarnings("nls")
    private final void drawTicMarkLabels( final Graphics2D graphicsContext ) {
        // Cache the current color and font to restore later.
        final Color color = graphicsContext.getColor();
        final Font font = graphicsContext.getFont();

        // Set the axis label color.
        graphicsContext.setColor( axisLabelColor );

        // Get the tic mark label font.
        final float maxFontSize = 8f;
        final int maxCharacterHeight = ( getHeight() - chartSize.height - uly ) / 2;
        final int maxStringWidth = ( int ) Math.round( 0.5d * ulx );
        final Font labelFontCandidate = getFont().deriveFont( Font.PLAIN, 12f );
        final FontMetrics fontMetrics = FontUtilities.pickFont( graphicsContext,
                                                                labelFontCandidate,
                                                                maxFontSize,
                                                                maxCharacterHeight,
                                                                maxStringWidth,
                                                                yTicLabels[ 0 ] );

        // Label the radial tics in degrees, at every fifteen degree increment.
        // TODO: Show a minor tick at every degree?
        for ( int i = 0; i < 360; i = i + 15 ) {
            // Draw tic label.
            // NOTE: Due to polar plot conventions, we label from 0 to 180
            // degrees in both the positive and the negative directions, vs. a
            // full 360 positive.
            // TODO: Add a flag for labeling 0 to 360 degrees vs. -180 to 180
            // degrees?
            final int fudge = 4;

            // NOTE: We add the symbol for "degree". Use Unicode quoted text?
            final String s = "" + ( ( i > 180 ) ? ( i - 360 ) : i ) + '\u00B0';

            final int stringWidth = fontMetrics.stringWidth( s );
            final int stringHeight = fontMetrics.getHeight();

            // NOTE: The convention for polar plots is for 0 degrees to point
            // north and 90 degrees to point east, whereas the convention for
            // linear plots is for 0 degrees to point east and 90 degrees to
            // point north.
            final double temp1 = ( yMax + ( ( 4 + ( 0.5d * stringWidth ) ) / yScale ) )
                    * Math.sin( Math.toRadians( i ) );
            final double temp2 = ( yMax + ( ( 4 + ( 0.5d * stringHeight ) ) / yScale ) )
                    * Math.cos( Math.toRadians( i ) );
            graphicsContext.drawString( s,
                                        ( int ) ( ( ulx + Math.round( 0.5d * chartSize.width )
                                                + ( temp1 * yScale ) )
                                                - Math.round( 0.5d * stringWidth ) ),
                                        ( int ) ( ( ( uly + Math.round( 0.5d * chartSize.height ) )
                                                - ( temp2 * yScale ) - fudge )
                                                + Math.round( 0.5d * stringHeight ) ) );
        }

        // Label circles (except for the first, which is the origin).
        graphicsContext.setColor( ticLabelcolor );

        for ( int i = 1; i < yTicLabels.length; i++ ) {
            // Draw tic label.
            final int fudge = 4;
            // final int stringWidth = fontMetrics.stringWidth( yTicLabels[ i ]
            // );
            // final int stringHeight = fontMetrics.getHeight();
            final double temp1 = yTics[ i ] * Math.sin( MathConstants.ONE_EIGHTH * Math.PI );
            final double temp2 = yTics[ i ] * Math.cos( MathConstants.ONE_EIGHTH * Math.PI );
            graphicsContext
                    .drawString( yTicLabels[ i ],
                                 ( int ) ( ( ulx + ( chartSize.width / 2 ) + ( temp1 * yScale ) )
                                         - 6 ),
                                 ( int ) ( ( uly + ( chartSize.height / 2 ) ) - ( temp2 * yScale )
                                         - fudge ) );
        }

        // Restore the previous color and font.
        graphicsContext.setColor( color );
        graphicsContext.setFont( font );
    }

    // Draw the title and labels.
    private final void drawTitleAndLabels( final Graphics2D g2 ) {
        // Draw the title.
        drawChartTitle( g2 );

        // Draw the x label (and units, if applicable).
        drawXLabelAndUnits( g2 );
    }

    // Draw the x-axis label (and units, if applicable).
    // TODO: Rework this logic so that units are optional.
    @SuppressWarnings("nls")
    private final void drawXLabelAndUnits( final Graphics2D graphicsContext ) {
        if ( validXLabel && validXUnits ) {
            // Cache the current color and font to restore later.
            final Color color = graphicsContext.getColor();
            final Font font = graphicsContext.getFont();

            // Use the axis label color for the x label and units.
            graphicsContext.setColor( axisLabelColor );

            final float maxFontSize = 12f;
            final int maxCharacterHeight = uly - 15;

            // Get x-axis label font info.
            // NOTE: We try to use SansSerif 18 point PLAIN if it fits.
            // TODO: Compare to other font work in the toolkit libraries.
            final int maxStringWidth = chartSize.width;
            final String axisLabel = xLabel + xUnits;
            final Font labelFontCandidate = new Font( "SansSerif", Font.PLAIN, 18 );
            FontMetrics fontMetrics = FontUtilities.pickFont( graphicsContext,
                                                              labelFontCandidate,
                                                              maxFontSize,
                                                              maxCharacterHeight,
                                                              maxStringWidth,
                                                              axisLabel );

            // Draw the x-axis label, horizontally centered.
            // NOTE: We factor the string height to account for the tic labels
            // between the bottom of the plot and the x-axis label.
            // NOTE: This is a bit of a hack, since the tic labels typically
            // use a much smaller font.
            // NOTE: We special case for frequency response, since the tic
            // labels are written vertically and thus require more room.
            final int stringWidth = fontMetrics.stringWidth( ( xLabel + xUnits ) );
            final int stringHeight = fontMetrics.getHeight();
            final int xPos = ulx + ( int ) Math.round( 0.5d * ( chartSize.width - stringWidth ) );
            final int yPos = uly + chartSize.height
                    + ( int ) Math.round( 0.5d * ( 3.0d * stringHeight ) );
            graphicsContext.drawString( xLabel, xPos, yPos );

            // Get x-axis units font info.
            // NOTE: We try to use SansSerif 16 point ITALIC if it fits.
            // TODO: Compare to other font work in the toolkit libraries.
            final String sUnits = " (" + xUnits + ")";
            final int oldStringWidth = fontMetrics.stringWidth( xLabel );
            final Font unitsFontCandidate = new Font( "SansSerif", Font.ITALIC, 16 );
            fontMetrics = FontUtilities.pickFont( graphicsContext,
                                                  unitsFontCandidate,
                                                  maxFontSize,
                                                  maxCharacterHeight,
                                                  maxStringWidth - stringWidth,
                                                  sUnits );

            // Draw the x-axis units next to the x-axis label.
            // stringWidth = fontMetrics.stringWidth( sUnits );
            // stringHeight = fontMetrics.getHeight();
            graphicsContext.drawString( sUnits, xPos + oldStringWidth, yPos );

            // Restore the previous color and font.
            graphicsContext.setColor( color );
            graphicsContext.setFont( font );
        }
    }

    public final double[] getDataX() {
        return xValues;
    }

    public final double[] getDataY() {
        return yValues;
    }

    public final float getGridDepth() {
        return gridDepth;
    }

    public final float getGridRange() {
        // NOTE: Grid Range x and y are currently linked.
        return gridRangeX;
    }

    public final float getGridRangeX() {
        return gridRangeX;
    }

    public final float getGridRangeY() {
        return gridRangeY;
    }

    public final int getGridSpacing() {
        return gridSpacing;
    }

    public final float getGridWidth() {
        return gridWidth;
    }

    public final String getXLabel() {
        return xLabel;
    }

    public final String getXUnits() {
        return xUnits;
    }

    private final void initPanel() {
        // Initialize the Number Formatters, now that we know Locale.
        initNumberFormatters();

        // Clear data to plot.
        xValues = null;
        yValues = null;
        xValuesNormalized = null;
        emptyChart = true;

        // Create plot boundary stroke style.
        chartBoundaryStroke = new BasicStroke( 1f,
                                               BasicStroke.CAP_SQUARE,
                                               BasicStroke.JOIN_MITER,
                                               6f,
                                               LINESTYLE_SOLID,
                                               0f );

        // Create grid stroke style.
        gridStroke = new BasicStroke( 1f,
                                      BasicStroke.CAP_SQUARE,
                                      BasicStroke.JOIN_MITER,
                                      1f,
                                      LINESTYLE_DOT,
                                      0f );

        // Create plot line stroke style.
        chartLineStroke = new BasicStroke( 1f,
                                           BasicStroke.CAP_SQUARE,
                                           BasicStroke.JOIN_MITER,
                                           6f,
                                           LINESTYLE_SOLID,
                                           0f );
    }

    protected final void initNumberFormatters() {
        // Cache the number formats so that we don't have to get information
        // about locale from the OS each time we format a number.
        numberFormat = NumberFormat.getNumberInstance( Locale.getDefault() );

        // Set the precision for floating-point text formatting.
        numberFormat.setMinimumFractionDigits( 0 );
        numberFormat.setMaximumFractionDigits( 2 );
    }

    // NOTE: The recalculations of scale factor/etc. might be better placed in
    // a window resize callback, so that recalculations are only performed when
    // needed as opposed to on every repaint event.
    @Override
    public final void paintComponent( final Graphics graphicsContext ) {
        // Create an off-screen graphics buffer, or use an existing Graphics
        // Context instance as the paint target (if vectorization is active).
        final Graphics2D graphics2D = isVectorizationActive()
            ? ( Graphics2D ) graphicsContext
            : createGraphics( graphicsContext );
        if ( graphics2D != null ) {
            // Redraw the background image before displaying it if anything has
            // changed; otherwise, just re-display it.
            // NOTE: We have to call the "super" class first, to make sure we
            // preserve the look-and-feel of the owner component.
            // NOTE: Due to off-screen buffering of superclass graphics, it is
            // not always necessary to repaint everything.
            // NOTE: We should never paint the background image for EPS Export,
            // as it is the entire panel and not the loaded image or the plot.
            if ( !isVectorizationActive() ) {
                super.paintComponent( graphics2D );
            }

            // Update the plot size to account for any resizing of the parent
            // component, compensating for the plot margins.
            updateChartSize();

            // Update the displayable data limits.
            updateDisplayableDataLimits();

            // Update the locations of the tic marks.
            updateTicLocations();

            // Update the scale factors every time, since the plot component
            // could have been resized or the plot data could have changed.
            updateScaleFactors();

            // Now draw the chart elements atop the background image.
            // NOTE: This method is sub-classed in order to take
            // advantage of off-screen buffering for the entire hierarchy.
            drawChart( graphics2D );

            // Dispose of graphics context now that we have finished with it.
            if ( !isVectorizationActive() ) {
                graphics2D.dispose();
            }
        }

        // Show new background image, or re-display old background image.
        // NOTE: Vectorization needs to maintain line vector graphics vs.
        // taking advantage of efficiencies in off-screen buffering.
        if ( !isVectorizationActive() ) {
            showBackgroundImage( graphicsContext );
        }
    }

    public final void setData( final double[] x, final double[] y ) {
        xValues = Arrays.copyOf( x, x.length );
        yValues = Arrays.copyOf( y, y.length );
        xValuesNormalized = new double[ x.length ];
        emptyChart = ( x.length < 2 );
        regenerateOffScreenImage = true;
    }

    // This method sets the background color, and where appropriate, the
    // foreground color is set to complement it for text-based components.
    @Override
    public void setForegroundFromBackground( final Color backColor ) {
        super.setForegroundFromBackground( backColor );

        // Cache background-sensitive colors used for individual features.
        final Color foreColor = getForeground();
        chartBoundaryColor = foreColor;
        axisLabelColor = foreColor;

        final Color ticLabelColorDefault = getDefaultTicLabelColor( backColor );
        setTicLabelColor( ticLabelColorDefault );

        final Color plotLineColorDefault = getDefaultChartLineColor( backColor );
        setPlotLineColor( plotLineColorDefault );
    }

    public final void setGridDepth( final int depth ) {
        gridDepth = depth;
        regenerateOffScreenImage = true;
    }

    public final void setGridRange( final float range ) {
        setGridRangeX( range );
        setGridRangeY( range );
    }

    public final void setGridRangeX( final float rangeX ) {
        gridRangeX = rangeX;
        regenerateOffScreenImage = true;
    }

    public final void setGridRangeY( final float rangeY ) {
        gridRangeY = rangeY;
        regenerateOffScreenImage = true;
    }

    public final void setGridSpacing( final int spacing ) {
        gridSpacing = spacing;
        regenerateOffScreenImage = true;
    }

    public final void setGridWidth( final int width ) {
        gridWidth = width;
        regenerateOffScreenImage = true;
    }

    /**
     * Sets the chart line color.
     *
     * @param newChartLineColor
     *            The new chart line color.
     *
     * @version 0.1
     */
    public final void setPlotLineColor( final Color newChartLineColor ) {
        chartLineColor = newChartLineColor;

        // Plot line color change requires regenerating the off-screen buffer.
        regenerateOffScreenImage = true;
    }

    /**
     * Sets the tic label color.
     *
     * @param newTicLabelColor
     *            The new tic label color.
     *
     * @version 0.1
     */
    public final void setTicLabelColor( final Color newTicLabelColor ) {
        ticLabelcolor = newTicLabelColor;

        // Tic label color change requires regenerating the off-screen buffer.
        regenerateOffScreenImage = true;
    }

    public final void setXLabel( final String s ) {
        xLabel = s;
        validXLabel = true;
        regenerateOffScreenImage = true;
    }

    public final void setXUnits( final String s ) {
        xUnits = s;
        validXUnits = true;
        regenerateOffScreenImage = true;
    }

    // Update the displayable data limits.
    public final void updateDisplayableDataLimits() {
        if ( !emptyChart ) {
            // First, find the data limits in (x,y) space.
            xMin = xValues[ 0 ];
            xMax = xValues[ 0 ];
            for ( int j = 1; j < xValues.length; j++ ) {
                xMin = Math.min( xMin, xValues[ j ] );
                xMax = Math.max( xMax, xValues[ j ] );
            }

            // Calculate limits for plot, and set values.
            yMax = xMax;
            yMin = xMin;
        }
    }

    // Update the chart size.
    public final void updateChartSize() {
        // NOTE: Due to differences in label positioning, etc., we use
        // different margins for Cartesian vs. Polar plots.
        // TODO: Find a way to accommodate maximum tic label length!
        // NOTE: There are side effects related to using grid bag layout with
        // this component, so we choose margins that do not result in clipping
        // in any of our current use cases.
        //
        // These are the margins of the "chart" area as a fraction of the total
        // width or height of the "user" area.
        final double xLeftMargin = 0.075d;
        final double xRightMargin = 0.075d;
        final double yTopMargin = 0.075d;
        final double yBottomMargin = 0.075d;

        // Calculate the _size and location of the "chart" area in pixels.
        ulx = ( int ) Math.round( getWidth() * xLeftMargin );
        uly = ( int ) Math.round( getHeight() * yTopMargin );
        chartSize.width = ( int ) Math.round( getWidth() * ( 1f - xLeftMargin - xRightMargin ) );
        chartSize.height = ( int ) Math.round( getHeight() * ( 1f - yTopMargin - yBottomMargin ) );
    }

    // Update the scale factors.
    private final void updateScaleFactors() {
        // Recalculate the scale factors every time, since the window
        // could have been resized or the data could have changed.
        // TODO: Debug the case of asymmetric scaling giving an
        // elliptical vs. circular grid.
        xScale = ( Math.abs( xMax - xMin ) > 0 )
            ? Math.min( chartSize.width, chartSize.height ) / ( xMax - xMin )
            : 1;
        yScale = ( Math.abs( yMax - yMin ) > 0 )
            ? ( 0.5d * Math.min( chartSize.width, chartSize.height ) ) / ( yMax - yMin )
            : 1;
    }

    // Conditionally update the locations of the tic marks.
    private final void updateTicLocations() {
        // Calculate the concentric circles for the polar plot.
        final double shift = calculatePolarCirclesLogged( yMin, yMax, gridRangeX, gridSpacing );

        // If data dips into negative zone, normalize to be all positive.
        // NOTE: To account for uniform data sets, we must also shift when the
        // minimum is zero, but this could cause side effects in other cases so
        // should be special cased for logged data.
        // NOTE: We must clip to the displayed dynamic range, if necessary.
        // TODO: Determine if we also need to clip beyond +6dB.
        // NOTE: We copy to a local array, so that repeated calls to this
        // method by repaint() / etc. do not cause cumulative error.
        if ( xMin <= 0.0d ) {
            xValuesNormalized = new double[ xValues.length ];
            for ( int j = 0; j < xValues.length; j++ ) {
                final double curval = xValues[ j ];
                xValuesNormalized[ j ] = ( curval < ( yMin - shift ) ) ? yMin : ( curval + shift );
            }
        }
    }

}