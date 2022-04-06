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
package com.mhschmieder.charttoolkit.chart;

import java.text.NumberFormat;

// TODO: remove the need for this semi-log-x subclass and find a way to better
// flag that behavior higher up.
public class SemiLogXSignalChart extends CartesianDataChart {
    /**
     *
     */
    private static final long serialVersionUID = 1202805649801181396L;

    // This is the default constructor, which primarily just indicates the
    // x-axis is logged.
    protected SemiLogXSignalChart( final int numberOfDataSets,
                                   final boolean useWatermark,
                                   final String jarRelativeWatermarkIconFilename ) {
        // Always call the superclass constructor first!
        super( numberOfDataSets, useWatermark, jarRelativeWatermarkIconFilename );

        // Ensure that the axis labels never exceed one decimal place of
        // precision, and only use decimals when needed (not for integers).
        setMinimumFractionDigits( 0 );
        setMaximumFractionDigits( 1 );

        // NOTE: Data will not display correctly unless setXLog is called
        // BEFORE the data is set.
        setXLog( true );
    }

    protected final void addXTicInUserUnits( final String label,
                                             final double positionInUserUnits ) {
        // Since the X Axis is logged, the position must also be logged.
        final double positionInLogUserUnits = StrictMath.log10( positionInUserUnits );
        addXTic( label, positionInLogUserUnits );
    }

    public final void addXTicInUserUnitsAutoLabel( final double positionInUserUnits,
                                                   final int minFractionalDigits,
                                                   final int maxFractionalDigits ) {
        final NumberFormat ticNumberFormat = ( NumberFormat ) numberFormat.clone();
        ticNumberFormat.setMinimumFractionDigits( minFractionalDigits );
        ticNumberFormat.setMaximumFractionDigits( maxFractionalDigits );
        addXTicInUserUnits( ticNumberFormat.format( positionInUserUnits ), positionInUserUnits );
    }

    public final void addXTicInUserUnitsAutoLabel( final int positionInUserUnits ) {
        final NumberFormat ticNumberFormat = ( NumberFormat ) numberFormat.clone();
        addXTicInUserUnits( ticNumberFormat.format( positionInUserUnits ), positionInUserUnits );
    }

    protected final void addYTicInUserUnits( final String label,
                                             final double positionInUserUnits ) {
        // Since the Y axis is NOT logged, the position should also NOT be
        // logged.
        addYTic( label, positionInUserUnits );
    }

    public final void addYTicInUserUnitsAutoLabel( final double positionInUserUnits,
                                                   final int minFractionalDigits,
                                                   final int maxFractionalDigits ) {
        final NumberFormat ticNumberFormat = ( NumberFormat ) numberFormat.clone();
        ticNumberFormat.setMinimumFractionDigits( minFractionalDigits );
        ticNumberFormat.setMaximumFractionDigits( maxFractionalDigits );
        addYTicInUserUnits( ticNumberFormat.format( positionInUserUnits ), positionInUserUnits );
    }

    protected final void addYTicInUserUnitsAutoLabel( final int positionInUserUnits ) {
        final NumberFormat ticNumberFormat = ( NumberFormat ) numberFormat.clone();
        addYTicInUserUnits( ticNumberFormat.format( positionInUserUnits ), positionInUserUnits );
    }

    @Override
    public void reset() {
        // TODO: Implement this, if it is ever called.
    }

    // NOTE: This is a more efficient way of dealing with logx data, than to
    // depend on point-by-point additions to the data set in the plot, or to add
    // special-casing in a tight loop in the base class' same-named method.
    // NOTE: For reasons of tight-loop performance, it is the responsibility of
    // the caller to pre-filter the data sets for any invalid values (<= 0). In
    // the absence of this, the first and last index can be used to limit the
    // usable part of the data set to valid values, and in any case we prevent
    // invalid values from generating NaN errors/exceptions.
    // NOTE: Similarly, we apply the log10 scaling directly, rather than
    // calling the log10 method, to avoid tight-loop method call overhead.
    @Override
    public void setDataSet( final int dataSetIndex,
                            final double x[],
                            final double y[],
                            final int firstIndex,
                            final int lastIndex ) {
        final double xLog[] = new double[ x.length ];
        for ( int i = 0; i < x.length; i++ ) {
            xLog[ i ] = ( x[ i ] > 0.0d ) ? StrictMath.log10( x[ i ] ) : Double.NEGATIVE_INFINITY;
        }

        super.setDataSet( dataSetIndex, xLog, y, firstIndex, lastIndex );
    }

    protected final void setXRangeInUserUnits( final double xMin, final double xMax ) {
        // Since the X Axis is logged, the values must also be logged.
        final double xMinInLogUserUnits = StrictMath.log10( xMin );
        final double xMaxInLogUserUnits = StrictMath.log10( xMax );

        setXRange( xMinInLogUserUnits, xMaxInLogUserUnits );
    }

    protected final void setYRangeInUserUnits( final double yMin, final double yMax ) {
        // Since the Y Axis is NOT logged, the values must also NOT be logged.
        setYRange( yMin, yMax );
    }

    protected void setZoomInUserUnits( final double xMin,
                                       final double ymin,
                                       final double xMax,
                                       final double yMax ) {
        // Since the X Axis is logged the values must also be logged.
        final double xMinInLogUserUnits = StrictMath.log10( xMin );
        final double xMaxInLogUserUnits = StrictMath.log10( xMax );

        zoom( xMinInLogUserUnits, ymin, xMaxInLogUserUnits, yMax );
    }

}
