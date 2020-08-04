/**
 * MIT License
 *
 * Copyright (c) 2020 Mark Schmieder
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
package com.mhschmieder.charttoolkit;

/**
 * {@code ChartUtilities} is a utility class for AWT based chart methods, usable
 * in either the AWT or Swing GUI toolkits.
 *
 * @version 1.0
 *
 * @author Mark Schmieder
 */
public final class ChartUtilities {

    /**
     * This method serves merely as a sanity check that the Maven integration
     * and builds work properly and also behave correctly inside Eclipse IDE. It
     * will likely get removed once I gain more confidence that I have solved
     * the well-known issues with Maven inside Eclipse as I move on to more
     * complex projects with dependencies (this project is quite simple and has
     * no dependencies at this time, until more functionality is added).
     *
     * @param args
     *            The command-line arguments for executing this class as the
     *            main entry point for an application
     *
     * @since 1.0
     */
    public static void main( final String[] args ) {
        System.out.println( "Hello Maven from ChartToolkit!" ); //$NON-NLS-1$
    }

    /**
     * The default constructor is disabled, as this is a static utilities class.
     */
    private ChartUtilities() {}

    /**
     * Transforms a set of data points to screen coordinates for purposes of
     * rendering or export.
     * <p>
     * The goal here is coding efficiencies that avoid auto-boxing and
     * unboxing, class instancing, etc. Thus we bypass some nice programming
     * paradigms, as the data vectors could have millions of points. This also
     * means that the target arrays have to be pre-constructed by the invoker,
     * and should match the original coordinate arrays in size. This is somewhat
     * wasteful, but less so than other approaches (until a better way is
     * found). The actual number of transformed data points is returned so that
     * client code can then make more efficient storage structures downstream.
     *
     * @param xCoordinates
     *            The original x-coordinates in domain/model space
     * @param yCoordinates
     *            The original y-coordinates in domain/model space
     * @param numberOfCoordinates
     *            The number of original coordinates
     * @param xMin
     *            The minimum x-axis value for the data window
     * @param yMin
     *            The minimum y-axis value for data normalization
     * @param xMax
     *            The maximum x-axis value for the data window
     * @param yMax
     *            The maximum y-axis value for data normalization
     * @param xScale
     *            The x-axis scale factor to apply from domain/model space to
     *            screen space
     * @param yScale
     *            The y-axis scale factor to apply from domain/model space to
     *            screen space
     * @param ulx
     *            The x-coordinate of the upper-left corner of the chart, in
     *            screen space
     * @param lry
     *            The y-coordinate of the lower-right corner of the chart, in
     *            screen space
     * @param applyDataReduction
     *            Flag for whether to apply data reduction techniques
     * @param dataVarianceFactor
     *            The amount of variance between neighboring data values
     *            (y-axis) to use for determining redundancy during data
     *            reduction
     * @param xCoordinatesTransformed
     *            The (potentially data-reduced) transformed x-coordinates in
     *            screen space
     * @param yCoordinatesTransformed
     *            The (potentially data-reduced) transformed y-coordinates in
     *            screen space
     * @return The number of data points transformed to screen coordinates
     *
     * @version 1.0
     */
    public static int transformDataVectorToScreenCoordinates( final double[] xCoordinates,
                                                              final double[] yCoordinates,
                                                              final int numberOfCoordinates,
                                                              final double xMin,
                                                              final double yMin,
                                                              final double xMax,
                                                              final double yMax,
                                                              final double xScale,
                                                              final double yScale,
                                                              final double ulx,
                                                              final double lry,
                                                              final boolean applyDataReduction,
                                                              final double dataVarianceFactor,
                                                              final double[] xCoordinatesTransformed,
                                                              final double[] yCoordinatesTransformed ) {
        // Cache the first in-range data point, so we can do a move followed by
        // any number of contiguous line segments.
        double prevX = 0d;
        double prevY = 0d;
        int transformedCoordinateIndex = -1;
        int firstDataPointIndex = 0;
        final int finalCoordinateIndex = numberOfCoordinates - 1;
        for ( int i = firstDataPointIndex; i <= finalCoordinateIndex; i++ ) {
            final double xValue = xCoordinates[ i ];
            if ( xValue >= xMin ) {
                prevX = ulx + ( ( xValue - xMin ) * xScale );
                prevY = lry - ( ( yCoordinates[ i ] - yMin ) * yScale );

                transformedCoordinateIndex++;
                xCoordinatesTransformed[ transformedCoordinateIndex ] = prevX;
                yCoordinatesTransformed[ transformedCoordinateIndex ] = prevY;

                firstDataPointIndex = i;

                break;
            }
        }

        // Loop through all of the in-range points in the current data set.
        boolean dataValueInvariant = false;
        for ( int i = firstDataPointIndex + 1; i <= finalCoordinateIndex; i++ ) {
            final double xValue = xCoordinates[ i ];
            if ( xValue > xMax ) {
                // If we are past the valid data range, ensure that there are at
                // least two retained data points so that we don't end up with a
                // blank trace. In this case, it means grabbing the previous.
                if ( transformedCoordinateIndex < 1 ) {
                    final double xPosPrev = ulx + ( ( xCoordinates[ i - 1 ] - xMin ) * xScale );
                    final double yPosPrev = lry - ( ( yCoordinates[ i - 1 ] - yMin ) * yScale );

                    transformedCoordinateIndex++;
                    xCoordinatesTransformed[ transformedCoordinateIndex ] = xPosPrev;
                    yCoordinatesTransformed[ transformedCoordinateIndex ] = yPosPrev;
                }

                break;
            }

            final double xPos = ulx + ( ( xValue - xMin ) * xScale );
            final double yPos = lry - ( ( yCoordinates[ i ] - yMin ) * yScale );

            if ( i == finalCoordinateIndex ) {
                // Once we get to the final valid data point and it is in-range,
                // automatically include it so that we don't have a truncated
                // trace or a strange trajectory to the top or bottom of chart.
                transformedCoordinateIndex++;
                xCoordinatesTransformed[ transformedCoordinateIndex ] = xPos;
                yCoordinatesTransformed[ transformedCoordinateIndex ] = yPos;

                break;
            }

            // If the magnitude is the same as before, even zooming won't show
            // more resolution, so it is wasteful to plot this point.
            //
            // Due to the pixel values being coarse integer values, we can get
            // noise in double precision floating-point math, so we apply a data
            // variance factor to the comparison as otherwise an all-zero data
            // vector (or flat data range subset) can have misleading variance.
            if ( applyDataReduction && ( yPos >= ( prevY - dataVarianceFactor ) )
                    && ( yPos <= ( prevY + dataVarianceFactor ) ) ) {
                dataValueInvariant = true;
                continue;
            }
            else if ( dataValueInvariant ) {
                // Once we get to variant data, ensure that there is at least
                // one flat line for the invariant data or else we slope towards
                // the first changed data value.
                final double xPosPrev = ulx + ( ( xCoordinates[ i - 1 ] - xMin ) * xScale );
                final double yPosPrev = lry - ( ( yCoordinates[ i - 1 ] - yMin ) * yScale );

                transformedCoordinateIndex++;
                xCoordinatesTransformed[ transformedCoordinateIndex ] = xPosPrev;
                yCoordinatesTransformed[ transformedCoordinateIndex ] = yPosPrev;

                dataValueInvariant = false;
            }

            // Cache the scaled current point as a unique data point.
            transformedCoordinateIndex++;
            xCoordinatesTransformed[ transformedCoordinateIndex ] = xPos;
            yCoordinatesTransformed[ transformedCoordinateIndex ] = yPos;

            // Save the current point as the previous point for the next line.
            prevX = xPos;
            prevY = yPos;
        }

        // As data reduction may have been applied, we cannot use the allocated
        // array size to determine the number of transformed coordinates in
        // screen space, and must instead convert the last array index to a
        // size/count/length.
        final int numberOfCoordinatesTransformed = transformedCoordinateIndex + 1;

        return numberOfCoordinatesTransformed;
    }

}
