/**
 * MIT License
 *
 * Copyright (c) 2020, 2023 Mark Schmieder
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

import java.awt.Dimension;

import org.apache.commons.math3.util.FastMath;

import com.mhschmieder.acousticstoolkit.FrequencySignalUtilities;
import com.mhschmieder.acousticstoolkit.RelativeBandwidth;
import com.mhschmieder.mathtoolkit.MathConstants;

public final class PolarAmplitudeChart extends SemiLogRPolarChart {
    /**
     * Unique Serial Version ID for this class, to avoid class loader conflicts.
     */
    private static final long  serialVersionUID            = 6560987527681952635L;

    // Declare the default amplitude values.
    // NOTE: We set a range of -6.0dB (knowing we also gain +6dB headroom).
    public static final double DEFAULT_AMPLITUDE           = -6d;

    // Cache the host window's dimensions so we can avoid coupling projects.
    protected final int        _polarResponseViewerWidth;
    protected final int        _polarResponseViewerHeight;

    // Declare a string to note the orientation of the polar response data
    // (horizontal vs. vertical)
    protected String           _polarOrientation;
    
    // The number of polar data points must be calculated from the angle increment.
    protected final int        _numberOfPolarDataPoints;

    // Declare the theta polar axis, which is invariant so could be static.
    protected final double[]   _theta;

    public PolarAmplitudeChart( final int polarResponseViewerWidth,
                                final int polarResponseViewerHeight,
                                final String polarOrientation,
                                final double angleIncrementDegrees ) {
        // Always call the superclass constructor first!
        super();

        _polarResponseViewerWidth = polarResponseViewerWidth;
        _polarResponseViewerHeight = polarResponseViewerHeight;
        _polarOrientation = polarOrientation;
        
        // The data points wrap back to 360 degrees for line drawing end point,
        // but this isn't necessarily redundant with the first point at 0 degrees
        // if the requested angle increment is not an integral divisor of 360.
        _numberOfPolarDataPoints = ( int ) FastMath.floor( 
                360.0d / angleIncrementDegrees ) + 1;

        // Initialize the invariant theta polar axis to a uniform default value.
        _theta = new double[ _numberOfPolarDataPoints ];
        double thetaDegrees = 0.0d;
        final int lastValidIndex = _numberOfPolarDataPoints - 1;
        for ( int i = 0; i < lastValidIndex; i++ ) {
            _theta[ i ] = FastMath.toRadians( thetaDegrees );
            thetaDegrees += angleIncrementDegrees;
        }
        
        // Set the final angle separately, so that it always corresponds to 360
        // degrees, as otherwise we don't close the circle and get plotting errors.
        _theta[ lastValidIndex ] = MathConstants.TWO_PI;

        try {
            initPanel();
        }
        catch ( final Exception ex ) {
            ex.printStackTrace();
        }
    }

    public void clearPlot() {
        clearResponse();
    }

    public double[] getPolarAmplitudeData() {
        return getDataX();
    }

    public double[] getPolarAngleData() {
        return getDataY();
    }

    // NOTE: Must override the preferred size query as SwingNode can't bind or
    //  set sizes of hosted components.
    @Override
    public Dimension getPreferredSize() {
        return new Dimension( ( int ) FastMath.round( ( 0.5d * _polarResponseViewerWidth ) - 60d ),
                              _polarResponseViewerHeight - 100 );
    }

    private void initPanel() {
        // Create a polar "xy-grid" overlay for polar responses.
        final String title = _polarOrientation + " " + "Polar Response"; //$NON-NLS-1$ //$NON-NLS-2$
        setChartTitle( title );
        setGridSpacing( DEFAULT_GRID_SPACING );

        // Initialize the variant amplitude axis to a uniform default value.
        // NOTE: We set a range of -6.0dB (knowing we also gain +6dB headroom)
        //  so that the initial empty plot is representative of a true data plot.
        final double amplitude[] = new double[ _numberOfPolarDataPoints ];
        for ( int i = 0; i < _numberOfPolarDataPoints; i++ ) {
            amplitude[ i ] = DEFAULT_AMPLITUDE;
        }

        // Set the magnitude units for polar plots.
        setXUnits( "dBr" ); //$NON-NLS-1$

        // Set the initial label and data set.
        setXLabel( "No Data" ); //$NON-NLS-1$
        setData( amplitude, _theta );
    }

    public void updatePolarAmplitudeTrace( final double[] amplitude,
                                           final String loudspeakerModel,
                                           RelativeBandwidth relativeBandwidth,
                                           final double centerFrequency ) {
        // Update the plot label and the data set.
        final String sCenterFrequency = FrequencySignalUtilities
                .getFormattedFrequency( centerFrequency, numberFormat );
        final String xLabel = loudspeakerModel + " " + relativeBandwidth.toPresentationString() //$NON-NLS-1$
                + " centered at " + sCenterFrequency; //$NON-NLS-1$
        setXLabel( xLabel );
        setData( amplitude, _theta );

        // Force a repaint event, to display/update the Polar Amplitude Trace.
        repaint();
    }
}
