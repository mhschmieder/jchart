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

import com.mhschmieder.jchart.chart.ChartType;
import com.mhschmieder.jchart.chart.ChartUtilities;
import com.mhschmieder.jgraphics.shape.ShapeUtilities;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

// The CartesianDataChart class primarily adds methods and data associated with
// plots that are a function of a variable along the x-axis producing a value
// along the y-axis, or simply points in 2D Cartesian Space without a function
// context.
//
// The number of data sets to be plotted does not need to be specified. Data
// sets are added as needed. Each data set can be optionally identified with
// color (see the base class).
//
// NOTE: There are quite a few subjective spacing parameters, all given,
// unfortunately, in pixels. This means that as resolutions get better, this
// program may need to be adjusted.
public abstract class CartesianDataChart extends CartesianChart {
    /**
     *
     */
    private static final long   serialVersionUID = 5689333438972608460L;

    // Need separate lists of plotting colors for dark vs. light background.
    private final List< Color > _legendColorsForDarkBackground;
    private final List< Color > _legendColorsForLightBackground;
    private List< Color >       _legendColors;

    // For performance reasons, use standard arrays for x and y coordinates vs.
    // a collection of point objects.
    protected double[][]        _xData;
    protected double[][]        _yData;

    // Keep track of which data set is active for this plot.
    protected int               _activeDataSet;

    // Keep track of whether to display data for all data sets or just the
    // active data set (default).
    private boolean             _displayDataForAllDataSets;

    // Cache the type of this plot.
    protected ChartType         _plotType;

    // Cache the total number of data sets in use for this plot.
    private final int           _numberOfDataSets;

    // NOTE: The main point of this constructor is to ensure that the color
    // arrays are non-null.
    protected CartesianDataChart( final int numberOfDataSets,
                                  final boolean useWatermark,
                                  final String jarRelativeWatermarkIconFilename ) {
        // Always call the superclass constructor first!
        super( useWatermark, jarRelativeWatermarkIconFilename );

        _numberOfDataSets = numberOfDataSets;

        _legendColorsForDarkBackground = new ArrayList<>( _numberOfDataSets );
        _legendColorsForLightBackground = new ArrayList<>( _numberOfDataSets );

        _xData = new double[ _numberOfDataSets ][];
        _yData = new double[ _numberOfDataSets ][];

        // Initialize the default plotting colors to reasonable contrast values
        // against allowed background colors, and avoid data set null pointers.
        for ( int i = 0; i < _numberOfDataSets; i++ ) {
            _legendColorsForDarkBackground.add( i, Color.WHITE );
            _legendColorsForLightBackground.add( i, Color.BLACK );

            _xData[ i ] = new double[ 0 ];
            _yData[ i ] = new double[ 0 ];
        }

        _legendColors = _legendColorsForDarkBackground;
        _activeDataSet = 0;
        _displayDataForAllDataSets = false;
        _plotType = ChartType.defaultValue();
    }

    /**
     * Check the argument to ensure that it is a valid data set index.
     *
     * @param dataSetIndex
     *            The data set index.
     * @return True if within range; False if below zero or beyond the maximum
     *         number of data sets for this plot
     */
    protected final boolean checkDataSetIndex( final int dataSetIndex ) {
        return ( dataSetIndex >= 0 ) && ( dataSetIndex < _numberOfDataSets );
    }

    // Clear the plot of data points in the specified data set index. This calls
    // repaint() to request an update of the display.
    protected void clear( final int dataSetIndex ) {
        if ( !checkDataSetIndex( dataSetIndex ) ) {
            return;
        }

        // Any change to the data or scaling requires regenerating the
        // off-screen buffer (if applicable).
        regenerateOffScreenImage = true;

        _xData[ dataSetIndex ] = new double[ 0 ];
        _yData[ dataSetIndex ] = new double[ 0 ];

        repaint();
    }

    /**
     * Draw bar lines from mid-way between the start and end points towards each
     * point at its respective y position. The current color is used. If the
     * <i>clip</i> argument is true, then draw only those portions of the bar
     * lines that lie within the plotting rectangle.
     *
     * @param g2
     *            The graphics context.
     * @param startX
     *            The starting x position.
     * @param startY
     *            The starting y position.
     * @param endX
     *            The ending x position.
     * @param endY
     *            The ending y position.
     * @param clip
     *            If true, then do not draw outside the range.
     */
    protected final long drawBarLine( final Graphics2D g2,
                                      final long startX,
                                      final long startY,
                                      final long endX,
                                      final long endY,
                                      final boolean clip ) {
        // Calculate the mid-point between the start point and end point.
        final long midX = Math.round( 0.5d * ( startX + endX ) );

        // First, draw a bar line from the start point mid-way towards the end
        // point, at the start point's height.
        drawLine( g2, startX, startY, midX, startY, clip );

        // Next, draw a bar line from mid-way towards the end point to the end
        // point, at the end point's height.
        drawLine( g2, midX, endY, endX, endY, clip );

        // Finally, draw a vertical line to connect these two bar lines.
        drawLine( g2, midX, startY, midX, endY, clip );

        // Return the delta, as this is used for first and last point.
        final long deltaX = startX - endX;
        return deltaX;
    }

    protected final void drawCenterBands( final Graphics2D g2,
                                          final double[] xCoordinates,
                                          final double[] yCoordinates ) {
        // Loop through all the points in the current data set, determining
        // whether to connect them, drop impulses, etc.
        long prevX = 0;
        long prevY = 0;
        long deltaX = 0;
        boolean firstPoint = true;
        final int numberOfCoordinates = xCoordinates.length;
        for ( int i = 0; i < numberOfCoordinates; i++ ) {
            // Use long here because these numbers can be quite large (when we
            // are zoomed out a lot).
            final long xPos = ulx + Math.round( ( xCoordinates[ i ] - xMin ) * xScale );
            final long yPos = lry - Math.round( ( yCoordinates[ i ] - yMin ) * yScale );

            // NOTE: The first point can't draw to a previous point.
            if ( !firstPoint ) {
                // Draw a bar line mid-way to the previous point.
                deltaX = drawBarLine( g2, xPos, yPos, prevX, prevY, true );
            }
            else {
                firstPoint = false;
            }

            // Save the current point as the "previous" point for the next line.
            prevX = xPos;
            prevY = yPos;
        }

        // If we are doing a center band plot, deal with the first and last
        // points.
        // NOTE: Must account for empty data sets to avoid throwing exceptions.
        if ( numberOfCoordinates > 0 ) {
            // Get the first point in the data set.
            long xPos = ulx + ( long ) ( ( xCoordinates[ 0 ] - xMin ) * xScale );
            long yPos = lry - ( long ) ( ( yCoordinates[ 0 ] - yMin ) * yScale );

            // Calculate the mid-point extrapolated from the first point.
            long midX = xPos - Math.round( 0.5d * deltaX );

            // Draw a bar line from the first point towards the extrapolated
            // mid-way point, at the first point's height.
            drawLine( g2, xPos, yPos, midX, yPos, true );

            // Get the last point in the data set.
            xPos = ulx + ( long ) ( ( xCoordinates[ numberOfCoordinates - 1 ] - xMin ) * xScale );
            yPos = lry - ( long ) ( ( yCoordinates[ numberOfCoordinates - 1 ] - yMin ) * yScale );

            // Calculate the mid-point extrapolated from the last point.
            midX = xPos + Math.round( 0.5d * deltaX );

            // Draw a bar line from the last point towards the extrapolated
            // mid-way point, at the last point's height.
            drawLine( g2, midX, yPos, xPos, yPos, true );
        }
    }

    /**
     * This method draws the supplied data vector on the screen, within the
     * established chart bounds, after mapping from model/domain space to screen
     * space and potentially applying data reduction techniques.
     *
     * @param graphicsContext
     *            The {@link Graphics2D} Graphics Context to use for drawing the
     *            resulting lines between the data points
     * @param xCoordinates
     *            The original x-coordinates in domain/model space
     * @param yCoordinates
     *            The original y-coordinates in domain/model space
     * @param numberOfCoordinates
     *            The number of original coordinates
     */
    protected final void drawDataVector( final Graphics2D graphicsContext,
                                         final double[] xCoordinates,
                                         final double[] yCoordinates,
                                         final int numberOfCoordinates ) {
        // Make copies of the coordinate arrays, for transformed values.
        final double[] xCoordinatesTransformed = new double[ numberOfCoordinates ];
        final double[] yCoordinatesTransformed = new double[ numberOfCoordinates ];

        // Transform the data points to screen space, using data reduction with
        // a data variance factor that accounts for AWT pixel values being
        // effectively integer-based.
        final int numberOfCoordinatesTransformed = ChartUtilities
                .transformDataVectorToScreenCoordinates( xCoordinates,
                                                         yCoordinates,
                                                         numberOfCoordinates,
                                                         xMin,
                                                         yMin,
                                                         xMax,
                                                         yMax,
                                                         xScale,
                                                         yScale,
                                                         ulx,
                                                         lry,
                                                         true,
                                                         0.001d,
                                                         xCoordinatesTransformed,
                                                         yCoordinatesTransformed );

        // Fortunately, we can be efficient by combining the entire trace into a
        // single open polyline, having stripped any redundant data points. This
        // improves AWT performance, and reduces file size for Graphics Exports.
        if ( isVectorizationActive() ) {
            // For vectorization to various vector graphics file formats, we can
            // invoke the double-precision polygon method. This greatly
            // reduces the number of PostScript moveto's and newpath's (and
            // their equivalents) in the format-specific overrides of the draw()
            // method from each format's specialized version of Graphics2D.
            final GeneralPath path =
                                   ShapeUtilities.makePolyline( xCoordinatesTransformed,
                                                                   yCoordinatesTransformed,
                                                                   numberOfCoordinatesTransformed );
            graphicsContext.draw( path );
        }
        else {
            // For AWT, we have to first round all coordinates to integers.
            final int[] xCoordinatesRounded = new int[ numberOfCoordinatesTransformed ];
            final int[] yCoordinatesRounded = new int[ numberOfCoordinatesTransformed ];
            for ( int i = 0; i < numberOfCoordinatesTransformed; i++ ) {
                xCoordinatesRounded[ i ] = ( int ) Math.round( xCoordinatesTransformed[ i ] );
                yCoordinatesRounded[ i ] = ( int ) Math.round( yCoordinatesTransformed[ i ] );
            }
            final GeneralPath path =
                                   ShapeUtilities.makePolyline( xCoordinatesRounded,
                                                                   yCoordinatesRounded,
                                                                   numberOfCoordinatesTransformed );
            graphicsContext.draw( path );
        }
    }

    /**
     * Draw a line from the specified starting point to the specified ending
     * point. The current color is used. If the <i>clip</i> argument is true,
     * then draw only that portion of the line that lies within the plotting
     * rectangle.
     *
     * @param g2
     *            The graphics context.
     * @param startX
     *            The starting x position.
     * @param startY
     *            The starting y position.
     * @param endX
     *            The ending x position.
     * @param endY
     *            The ending y position.
     * @param clip
     *            If true, then do not draw outside the range.
     */
    protected final void drawLine( final Graphics2D g2,
                                   final long startX,
                                   final long startY,
                                   final long endX,
                                   final long endY,
                                   final boolean clip ) {
        long startXAdjusted = startX;
        long startYAdjusted = startY;
        long endXAdjusted = endX;
        long endYAdjusted = endY;
        if ( clip ) {
            // Rule out impossible cases.
            if ( ( ( ( endXAdjusted > ulx ) || ( startXAdjusted > ulx ) )
                    && ( ( endXAdjusted < lrx ) || ( startXAdjusted < lrx ) )
                    && ( ( endYAdjusted > uly ) || ( startYAdjusted > uly ) )
                    && ( ( endYAdjusted < lry ) || ( startYAdjusted < lry ) ) ) ) {
                // If the end point is out of x range, adjust end point to
                // boundary. The integer arithmetic has to be done with longs
                // so as to not lose precision on extremely close zooms.
                if ( startXAdjusted != endXAdjusted ) {
                    if ( endXAdjusted < ulx ) {
                        endYAdjusted = ( int ) ( endYAdjusted
                                + ( ( ( startYAdjusted - endYAdjusted ) * ( ulx - endXAdjusted ) )
                                        / ( startXAdjusted - endXAdjusted ) ) );
                        endXAdjusted = ulx;
                    }
                    else if ( endXAdjusted > lrx ) {
                        endYAdjusted = ( int ) ( endYAdjusted
                                + ( ( ( startYAdjusted - endYAdjusted ) * ( lrx - endXAdjusted ) )
                                        / ( startXAdjusted - endXAdjusted ) ) );
                        endXAdjusted = lrx;
                    }
                }

                // If end point is out of y range, adjust to boundary. Note that
                // y increases downward.
                if ( startYAdjusted != endYAdjusted ) {
                    if ( endYAdjusted < uly ) {
                        endXAdjusted = ( int ) ( endXAdjusted
                                + ( ( ( startXAdjusted - endXAdjusted ) * ( uly - endYAdjusted ) )
                                        / ( startYAdjusted - endYAdjusted ) ) );
                        endYAdjusted = uly;
                    }
                    else if ( endYAdjusted > lry ) {
                        endXAdjusted = ( int ) ( endXAdjusted
                                + ( ( ( startXAdjusted - endXAdjusted ) * ( lry - endYAdjusted ) )
                                        / ( startYAdjusted - endYAdjusted ) ) );
                        endYAdjusted = lry;
                    }
                }

                // Adjust current point to lie on the boundary.
                if ( startXAdjusted != endXAdjusted ) {
                    if ( startXAdjusted < ulx ) {
                        startYAdjusted = ( int ) ( startYAdjusted
                                + ( ( ( endYAdjusted - startYAdjusted ) * ( ulx - startXAdjusted ) )
                                        / ( endXAdjusted - startXAdjusted ) ) );
                        startXAdjusted = ulx;
                    }
                    else if ( startXAdjusted > lrx ) {
                        startYAdjusted = ( int ) ( startYAdjusted
                                + ( ( ( endYAdjusted - startYAdjusted ) * ( lrx - startXAdjusted ) )
                                        / ( endXAdjusted - startXAdjusted ) ) );
                        startXAdjusted = lrx;
                    }
                }
                if ( startYAdjusted != endYAdjusted ) {
                    if ( startYAdjusted < uly ) {
                        startXAdjusted = ( int ) ( startXAdjusted
                                + ( ( ( endXAdjusted - startXAdjusted ) * ( uly - startYAdjusted ) )
                                        / ( endYAdjusted - startYAdjusted ) ) );
                        startYAdjusted = uly;
                    }
                    else if ( startYAdjusted > lry ) {
                        startXAdjusted = ( int ) ( startXAdjusted
                                + ( ( ( endXAdjusted - startXAdjusted ) * ( lry - startYAdjusted ) )
                                        / ( endYAdjusted - startYAdjusted ) ) );
                        startYAdjusted = lry;
                    }
                }
            }

            // Are the new points in range?
            if ( ( endXAdjusted >= ulx ) && ( endXAdjusted <= lrx ) && ( endYAdjusted >= uly )
                    && ( endYAdjusted <= lry ) && ( startXAdjusted >= ulx )
                    && ( startXAdjusted <= lrx ) && ( startYAdjusted >= uly )
                    && ( startYAdjusted <= lry ) ) {
                g2.drawLine( ( int ) startXAdjusted,
                             ( int ) startYAdjusted,
                             ( int ) endXAdjusted,
                             ( int ) endYAdjusted );
            }
        }
        else {
            // Draw unconditionally.
            g2.drawLine( ( int ) startXAdjusted,
                         ( int ) startYAdjusted,
                         ( int ) endXAdjusted,
                         ( int ) endYAdjusted );
        }
    }

    /**
     * Draw the axes and then plot all points. This method is called by
     * paintComponent(). To cause it to be called you would normally call
     * repaint(), which eventually causes paintComponent() to be called.
     *
     * @param graphicsContext
     *            The graphics context.
     */
    @Override
    public void drawChart( final Graphics2D graphicsContext ) {
        // We must call the superclass before calling drawPlotPoints so that
        // xScale and yScale are set.
        super.drawChart( graphicsContext );

        if ( _numberOfDataSets < 1 ) {
            return;
        }

        // Save the current clipping so that we can restore it after painting.
        final Shape clip = graphicsContext.getClip();

        // Set the clipping to prevent the plot lines from writing beyond the
        // chart boundaries.
        final int plotX = getUlx();
        final int plotY = getUly();
        final int plotWidth = getLrx() - plotX;
        final int plotHeight = getLry() - plotY;
        graphicsContext.setClip( plotX, plotY, plotWidth, plotHeight );

        // Plot the data sets in reverse order so that the first colors appear
        // on top.
        for ( int dataSetIndex = _numberOfDataSets - 1; dataSetIndex >= 0; dataSetIndex-- ) {
            drawPlotPoints( graphicsContext, dataSetIndex );
        }

        // Restore the clipping to what it was before we painted the traces.
        graphicsContext.setClip( clip );
    }

    /**
     * Draws the specified data set and associated lines, if any. Note that
     * paintComponent() should be called before calling this method so that it
     * calls drawPlot(), which sets xScale and yScale. Note that this does
     * not check the data set index. It is up to the caller to do that.
     */
    protected final void drawPlotPoints( final Graphics2D g2, final int dataSetIndex ) {
        // Pre-load the data set-specific objects and data, and declare all
        // reused variables outside the tight loop, for efficiency.
        final double[] xCoordinates = getXCoordinates( dataSetIndex );
        final double[] yCoordinates = getYCoordinates( dataSetIndex );

        final int numberOfCoordinates = xCoordinates.length;
        if ( numberOfCoordinates <= 0 ) {
            return;
        }

        // Cache the color to restore after custom graphics.
        final Color previousColor = g2.getColor();

        // Set the color.
        final Color currentColor = getColor( dataSetIndex );
        g2.setColor( currentColor );

        switch ( _plotType ) {
        case DATA_VECTOR:
            drawDataVector( g2, xCoordinates, yCoordinates, numberOfCoordinates );
            break;
        case CENTER_BAND:
            drawCenterBands( g2, xCoordinates, yCoordinates );
            break;
        default:
            break;
        }

        // Restore the color, in case the plot gets redrawn.
        g2.setColor( previousColor );
    }

    /**
     * Returns the color for a data set, or throw an exception if there is none.
     * The color would have been set by setColor().
     *
     * @param dataSetIndex
     *            The data set index.
     * @return The color, or throw an exception if there is none.
     */
    protected final Color getColor( final int dataSetIndex ) {
        if ( !checkDataSetIndex( dataSetIndex ) ) {
            return Color.GRAY;
        }

        return _legendColors.get( dataSetIndex );
    }

    /**
     * Return the legend colors default.
     *
     * @param backColor
     *            The graphics background color.
     */
    protected final List< Color > getLegendColorsDefault( final Color backColor ) {
        // Set the legend colors to use based on dark vs. light background.
        return ( Color.BLACK.equals( backColor ) || Color.DARK_GRAY.equals( backColor ) )
            ? _legendColorsForDarkBackground
            : _legendColorsForLightBackground;
    }

    /**
     * Get the x coordinates for a data set (set by setDataset()).
     *
     * @param dataSetIndex
     *            The data set index.
     * @return The x coordinates, or throw an exception if there are none.
     */
    protected final double[] getXCoordinates( final int dataSetIndex ) {
        if ( !isDataSetValid( dataSetIndex ) ) {
            return new double[ 0 ];
        }

        return _xData[ dataSetIndex ];
    }

    /**
     * Get the y coordinates for a data set (set by setDataset()).
     *
     * @param dataSetIndex
     *            The data set index.
     * @return The y coordinates, or throw an exception if there are none.
     */
    protected final double[] getYCoordinates( final int dataSetIndex ) {
        if ( !isDataSetValid( dataSetIndex ) ) {
            return new double[ 0 ];
        }

        return _yData[ dataSetIndex ];
    }

    // Query whether this component is valid or not; that is, whether it
    // contains a valid data vector or not.
    public final boolean isDataSetValid() {
        return isDataSetValid( _activeDataSet );
    }

    /**
     * Return whether the specified data set is valid or not. Check the argument
     * to ensure that it is a valid data set index. If so, verify non-empty
     * data at the specified index.
     *
     * @param dataSetIndex
     *            The data set index.
     * @return True if the specified data set is valid; false if not.
     */
    public boolean isDataSetValid( final int dataSetIndex ) {
        if ( !checkDataSetIndex( dataSetIndex ) ) {
            return false;
        }

        final double[] xCoordinates = _xData[ dataSetIndex ];
        final double[] yCoordinates = _yData[ dataSetIndex ];

        return ( ( ( xCoordinates != null ) && ( xCoordinates.length > 0 ) )
                && ( ( yCoordinates != null ) && ( yCoordinates.length > 0 ) ) );
    }

    public final boolean isDisplayDataForAllDataSets() {
        return _displayDataForAllDataSets;
    }

    @Override
    public void paintComponent( final Graphics g ) {
        // NOTE: We have to call the "super" class first, to make sure
        // we preserve the look-and-feel of the owner component.
        super.paintComponent( g );

        // Cast the graphics object so we can set rendering hints etc.
        final Graphics2D g2 = ( Graphics2D ) g;

        // If relevant, composite the watermark with the image.
        // NOTE: Commented out for now, until rewritten for performance.
        // if ( useWatermark ) {
        final boolean watermarkInUse = false;
        if ( watermarkInUse ) {
            // Use the watermark at actual size, and tile it across the plot,
            // semi-checkerboard fashion, with odd rows leading the watermark
            // alternating with a blank space 2/3 the watermark width, and even
            // rows leading with white space. The odd and even rows themselves
            // are spaced apart by the height of the watermark.
            final int plotWidth = Math.abs( getLrx() - getUlx() );
            final int plotHeight = Math.abs( getUly() - getLry() );
            final Image watermarkImage = watermarkIcon.getImage();
            final int watermarkWidth = watermarkImage.getWidth( null );
            final int watermarkHeight = watermarkImage.getHeight( null );
            final int blankingWidth = ( int ) Math.round( ( 2.0d * watermarkWidth ) / 3.0d );
            final int blankingHeight = watermarkHeight;

            // NOTE: Virtual rows consist of a pair of left-justified and
            // left-offset rows; each one followed by a blanking row.
            final int numRows = ( int ) Math.ceil( ( plotHeight + blankingHeight )
                    / ( 2.0d * ( watermarkHeight + blankingHeight ) ) );
            final int numCols = ( int ) Math
                    .ceil( ( plotWidth + blankingWidth ) / ( watermarkWidth + blankingWidth ) );

            // Composite the transparent watermark with the background
            // image. This maintains the integrity of the background image
            // while also being harder to reverse, at the client end,
            // than simply painting the transparent watermark atop the
            // background image.
            //
            // NOTE: We use a copy of the graphics context, to minimize
            // the chance of having a side effect on later use in the
            // repaint() methods due to setting the composite value.
            // TODO: Add bounds checking, or trim to the plot boundary,
            // as the blanking width and height make it difficult to get
            // an exact count for the tiling otherwise, and it's important
            // not to leave blank space.
            //
            // TODO: Refine this logic. as we are not getting enough
            // rows and sometimes not enough columns, since the blanking
            // width is different from the watermark width and throws off
            // the algorithm in terms of how many of each to paint.
            g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.5f ) );

            // TODO: Do bounds-checking on the edge of the plot.
            // TODO: Review proper direction of y-axis here and elsewhere,
            // considering that only the watermark inverts this order, and it
            // might have been incorrect as it never worked properly at the
            // Plot2D level anyway (just at the ImagePlot level).
            int x = getUlx();
            int y = getUly();
            for ( int i = 0; i < numRows; i++ ) {
                x = ( ( i % 2 ) == 0 ) ? getUlx() : getUlx() + blankingWidth;
                for ( int j = 0; j < numCols; j++ ) {
                    g2.drawImage( watermarkImage, x, y, null );
                    x += watermarkWidth + blankingWidth;
                }
                y += watermarkHeight + blankingHeight;
            }
        }
    }

    public abstract void reset();

    public final void setActiveDataSet( final int dataSetIndex ) {
        _activeDataSet = dataSetIndex;
    }

    /**
     * Set the colors for all relevant data set indices.
     * <p>
     * NOTE: This is an optional method that should be overridden as needed.
     */
    public void setChartColors() {}

    public final void setChartType( final ChartType plotType ) {
        _plotType = plotType;
    }

    /**
     * Set the color for the specified data set index.
     */
    public final void setColor( final Color colorForDarkBackground,
                                final Color colorForLightBackground,
                                final int dataSetIndex ) {
        if ( !checkDataSetIndex( dataSetIndex ) ) {
            return;
        }

        _legendColorsForDarkBackground.set( dataSetIndex, colorForDarkBackground );
        _legendColorsForLightBackground.set( dataSetIndex, colorForLightBackground );
    }

    /**
     * Set the color for the specified data set index.
     */
    public final void setColor( final int dataSetIndex,
                                final Color[] colorsForDarkBackground,
                                final Color[] colorsForLightBackground ) {
        if ( !checkDataSetIndex( dataSetIndex ) ) {
            return;
        }

        _legendColorsForDarkBackground.set( dataSetIndex, colorsForDarkBackground[ dataSetIndex ] );
        _legendColorsForLightBackground.set( dataSetIndex,
                                             colorsForLightBackground[ dataSetIndex ] );
    }

    /**
     * Replace the specified data set with the given points. This calls
     * repaint() to request an update of the display.
     *
     * @param dataSetIndex
     *            The data set index.
     * @param xCoordinates
     *            The X positions of the new points.
     * @param yCoordinates
     *            The Y positions of the new points.
     * @param firstIndex
     *            The first point in the data set to use
     * @param lastIndex
     *            The last point in the data set to use
     */
    protected void setDataSet( final int dataSetIndex,
                               final double[] xCoordinates,
                               final double[] yCoordinates,
                               final int firstIndex,
                               final int lastIndex ) {
        if ( !checkDataSetIndex( dataSetIndex ) ) {
            return;
        }

        // Any change to the data or scaling requires regenerating the
        // off-screen buffer (if applicable).
        regenerateOffScreenImage = true;

        // Replace the specified data set with the given points. This calls
        // repaint() to request an update of the display.
        clear( dataSetIndex );

        final int numberOfCoordinates = ( lastIndex - firstIndex ) + 1;
        _xData[ dataSetIndex ] = new double[ numberOfCoordinates ];
        _yData[ dataSetIndex ] = new double[ numberOfCoordinates ];

        for ( int i = firstIndex, j = 0; i <= lastIndex; i++, j++ ) {
            _xData[ dataSetIndex ][ j ] = xCoordinates[ i ];
            _yData[ dataSetIndex ][ j ] = yCoordinates[ i ];
        }

        // Make sure the new data set is shown in place of the old.
        repaint();
    }

    public final void setDisplayDataForAllDataSets( final boolean displayDataForAllDataSets ) {
        _displayDataForAllDataSets = displayDataForAllDataSets;
    }

    /**
     * Set the graphics background color, and from this contextually set the
     * foreground color.
     *
     * @param backColor
     *            The graphics background color.
     */
    @Override
    public void setForegroundFromBackground( final Color backColor ) {
        super.setForegroundFromBackground( backColor );

        final List< Color > legendColorsDefault = getLegendColorsDefault( backColor );
        setLegendColors( legendColorsDefault );
    }

    /**
     * Set the legend colors.
     */
    protected final void setLegendColors( final List< Color > legendColors ) {
        _legendColors = legendColors;
    }

}
