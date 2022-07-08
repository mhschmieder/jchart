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
 * This file is part of the FxGuiToolkit Library
 *
 * You should have received a copy of the MIT License along with the
 * GuiToolkit Library. If not, see <https://opensource.org/licenses/MIT>.
 *
 * Project: https://github.com/mhschmieder/fxguitoolkit
 */
package com.mhschmieder.charttoolkit.layout;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Locale;

public final class SplPaletteLegend extends CartesianGraphicsChart {
    /**
     *
     */
    private static final long   serialVersionUID       = 1816761465708022035L;

    // Declare default constants.
    private static final double DIV_DEFAULT            = 6.0d;
    private static final int    NUMBER_OF_DIVS_DEFAULT = 7;
    private static final double DYNAMIC_RANGE_DEFAULT  = DIV_DEFAULT * NUMBER_OF_DIVS_DEFAULT;

    private static final double MAG_MAX_DEFAULT        = 0.0d;
    private static final double MAG_MIN_DEFAULT        = MAG_MAX_DEFAULT - DYNAMIC_RANGE_DEFAULT;

    // Declare dynamic range and div.
    private double              _div;
    private int                 _numberOfDivs;
    private double              _dynamicRange;

    // Declare minimum and maximum magnitudes (must be valid 6.0dB divs)
    private double              _magMax;
    private double              _magMin;

    public SplPaletteLegend( final Locale locale ) {
        // Always call the superclass constructor first!
        super( false, null ); // Don't use watermark

        _div = DIV_DEFAULT;
        _numberOfDivs = NUMBER_OF_DIVS_DEFAULT;
        _dynamicRange = DYNAMIC_RANGE_DEFAULT;
        _magMax = MAG_MAX_DEFAULT;
        _magMin = MAG_MIN_DEFAULT;

        try {
            initLegend();
        }
        catch ( final Exception ex ) {
            ex.printStackTrace();
        }
    }

    // Get the maximum size of this component as the largest aesthetically
    // usable palette width (and height based on aspect ratio) -- generally
    // about 110 pixels. In JDK 1.3, only BoxLayout pays any attention to this.
    @Override
    public Dimension getMaximumSize() {
        return new Dimension( 110, ( int ) Math.round( 110d / getAspectRatio() ) );
    }

    // Get the minimum size of this component as the smallest aesthetically
    // usable palette width (and height based on aspect ratio) -- generally
    // about 60 pixels.
    @Override
    public Dimension getMinimumSize() {
        return new Dimension( 60, ( int ) Math.round( 60d / getAspectRatio() ) );
    }

    // Get the preferred size of this component; attempting to make the width
    // 1/8 of the available width, to achieve a 1-to-7 size ratio with sound
    // field, when a box layout of two horizontal elements is used (this is
    // based on the knowledge that the natural width is 1/2 the application
    // width).
    @Override
    public Dimension getPreferredSize() {
        final Dimension size = super.getPreferredSize();
        final Dimension minimumSize = super.getMinimumSize();
        final int newWidth = ( int ) Math
                .round( Math.max( minimumSize.getWidth(), size.getWidth() * 0.25d ) );
        final Dimension newSize = new Dimension( newWidth, ( int ) size.getHeight() );
        return new Dimension( newSize );
    }

    @SuppressWarnings("nls")
    private void initLegend() {
        // Create a Cartesian y-only axis for the palette.
        setChartTitle( "SPL" );
        setYLabel( "Amplitude" );
        setYUnits( "dB" );
        setGrid( false );
        setPadding( 0.0d );

        // Set a default range, just so it has labels if displayed without a
        // prediction.
        setXRange( 0.0d, 1.0d );
        addXTic( "", 0.0d );
        addXTic( "", 1.0d );

        // NOTE: We must guarantee that the palette has a roughly 1 to 10
        // aspect ratio, since it can't be auto-scaled properly (no X-axis).
        setAspectRatio( 0.1d );
        setAspectRatioApplied( true );

        // Set the plot labels based on the default dynamic range.
        updateAxes();
    }

    // This method resets the plot from the current dynamic range and scale.
    // NOTE: We "zero-normalize" the returned SPL range.
    // TODO: Force the dynamic range itself to an increment of the chosen scale
    // factor?
    protected void setYRangeByDivAndRange() {
        _div = ( _dynamicRange <= 66d ) ? ( _dynamicRange <= 27d ) ? 3 : 6 : 12;
        _numberOfDivs = ( int ) Math.floor( _dynamicRange / _div );

        clearYTics();
        setYRange( -_dynamicRange, 0.0d );

        // Use an inclusive range to cover the entire image height (the
        // x-axis is unlabeled, so there's no danger of label collision).
        for ( int i = 0; i < ( _numberOfDivs + 1 ); i++ ) {
            addYTicAutoLabel( ( int ) ( -_div * i ) );
        }
    }

    protected void updateAxes() {
        // Update the Y-axis tics and labels.
        setYRangeByDivAndRange();

        // Force a repaint event, to display/update the new palette image.
        repaint();
    }

    public void updateScale( final float fScale ) {
        // Update the scale factor and adjust the axis labels accordingly.
        _div = fScale;

        // Update the axes from the current Dynamic Range and Scale Factor.
        updateAxes();
    }

    /**
     * Update the SPL Palette Image.
     */
    public void updateSplPaletteImage( final BufferedImage splPaletteImageAwt ) {
        // Delegate the image load to the underlying geometry plot.
        updateImage( splPaletteImageAwt );
    }

    /**
     * Update the SPL Palette Image.
     */
    public boolean updateSplPaletteImage( final InputStream inputStream ) {
        // Delegate the image load to the underlying geometry plot.
        return updateImage( inputStream );
    }

    public void updateSplRange( final double splMinimum, final double splMaximum ) {
        // Store the new dynamic range, and use it to update the labels.
        _magMin = Math.abs( Math.max( splMinimum, splMaximum ) );
        _magMax = Math.abs( Math.min( splMinimum, splMaximum ) );
        _dynamicRange = Math.abs( _magMax - _magMin );

        // Update the axes from the current Dynamic Range and Scale Factor.
        updateAxes();
    }

}
