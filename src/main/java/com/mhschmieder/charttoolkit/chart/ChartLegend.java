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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.mhschmieder.graphicstoolkit.color.ColorUtilities;
import com.mhschmieder.guitoolkit.border.BorderUtilities;
import com.mhschmieder.guitoolkit.component.VectorizationXPanel;
import com.mhschmieder.guitoolkit.layout.SpringLayoutUtilities;

/**
 * {@code ChartLegend} is a simple legend for chart colors, for using with chart
 * toolkits that don't have their own, such as the rudimentary chart facilities
 * in this toolkit. It is deliberately simple and low maintenance, so it only
 * supports a typical vertical layout that would be placed to the right of any
 * chart or group of stacked charts. It also supports export to Vector Graphics.
 *
 * @version 0.1
 *
 * @author Mark Schmieder
 */
public class ChartLegend extends VectorizationXPanel {
    /**
     * Unique Serial Version ID for this class, to avoid class loader conflicts.
     */
    private static final long   serialVersionUID           = -7951399468212008536L;

    /**
     * The dimension (same for width as height) of each legend icon, in pixels.
     */
    private static final int    ICON_SIZE                  = 12;

    /**
     * The default number of data sets, for purposes of initial list sizing.
     */
    private static final int    DEFAULT_NUMBER_OF_DATASETS = 16;

    /**
     * The sub-panel that is used as a layout container for the legend elements.
     */
    private JPanel              legendSubpanel;

    /**
     * The number of data sets in use by the chart that owns this legend.
     */
    private int                 numberOfDataSetsInUse;

    /**
     * The indices to use to reference each of the data sets in use.
     */
    private Vector< Integer >   legendDataSets;

    /**
     * The labels for each of the data sets in use.
     */
    private Vector< JLabel >    legendLabels;

    /**
     * The currently active legend icons for the data sets in use.
     */
    private Vector< ImageIcon > legendIcons;

    /**
     * The icons to use when the legend is displayed against a dark background.
     */
    private Vector< ImageIcon > iconsForDarkBackground;

    /**
     * The icons to use when the legend is displayed against a light background.
     */
    private Vector< ImageIcon > iconsForLightBackground;

    //////////////////////////// Constructors ////////////////////////////////

    /**
     * Constructs a basic Chart Legend with no customization.
     *
     * @since 1.0
     */
    public ChartLegend() {
        // Always call the superclass constructor first!
        super();

        // Avoid constructor failure by wrapping the layout initialization in an
        // exception handler that logs the exception and then returns an object.
        try {
            initPanel();
        }
        catch ( final Exception ex ) {
            ex.printStackTrace();
        }
    }

    /////////////////////// Initialization methods ///////////////////////////

    /**
     * Initializes this panel in an encapsulated way that protects all
     * constructors from run-time exceptions that might prevent instantiation.
     * <p>
     * The method is declared final, as any derived classes should avoid
     * unwanted side effects and simply write their own GUI initialization
     * method that adds any extended behaviour or components to the layout.
     *
     * @since 1.0
     */
    private void initPanel() {
        // Chart legends must be explicitly added; there are no defaults.
        legendDataSets = new Vector<>( DEFAULT_NUMBER_OF_DATASETS );
        legendLabels = new Vector<>( DEFAULT_NUMBER_OF_DATASETS );
        iconsForDarkBackground = new Vector<>( DEFAULT_NUMBER_OF_DATASETS );
        iconsForLightBackground = new Vector<>( DEFAULT_NUMBER_OF_DATASETS );
        for ( int dataSetIndex = 0; dataSetIndex < DEFAULT_NUMBER_OF_DATASETS; dataSetIndex++ ) {
            legendDataSets.addElement( Integer.valueOf( dataSetIndex ) );
            legendLabels.addElement( new JLabel( "" ) ); //$NON-NLS-1$
            iconsForDarkBackground.addElement( null );
            iconsForLightBackground.addElement( null );
        }
        legendIcons = iconsForDarkBackground;

        // Layout the main sub-panel using the Spring Layout, with labels linked
        // to their associated components.
        legendSubpanel = new JPanel();
        legendSubpanel.setLayout( new SpringLayout() );
        final TitledBorder titledBorder = BorderUtilities.makeTitledBorder( "Chart Legend" ); //$NON-NLS-1$
        legendSubpanel.setBorder( titledBorder );

        for ( int dataSetIndex = 0; dataSetIndex < DEFAULT_NUMBER_OF_DATASETS; dataSetIndex++ ) {
            legendSubpanel.add( legendLabels.elementAt( dataSetIndex ) );
        }

        SpringLayoutUtilities.makeCompactGrid( legendSubpanel,
                                               DEFAULT_NUMBER_OF_DATASETS, // rows
                                               1, // columns
                                               6, // initX
                                               6, // initY
                                               12, // padX
                                               12 ); // padY

        // Layout the main panel with its component panels, using the box layout
        // to stack them vertically.
        //
        // Vertical glue makes this panel the same height as its neighbors to
        // the left and the right.
        setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ) );
        add( legendSubpanel );
        add( Box.createVerticalGlue() );
    }

    ////////////////// Accessor methods for private data /////////////////////

    /**
     * Returns the index for a data set, or throw an exception if there is none.
     * The legend would have been set by addLegend().
     *
     * @param dataSetNumber
     *            The data set number
     * @return The data set index, or throw an exception if there is none
     *
     * @since 1.0
     */
    protected int getDatasetIndex( final int dataSetNumber ) {
        final int dataSetIndex = legendDataSets.indexOf( Integer.valueOf( dataSetNumber ), 0 );
        if ( dataSetIndex == -1 ) {
            throw new IllegalArgumentException( "ChartLegend.getDatasetIndex: Cannot" //$NON-NLS-1$
                    + " give a negative number for the data set index." ); //$NON-NLS-1$
        }

        return dataSetIndex;
    }

    /**
     * Returns the legend for a data set, or an empty string if there is none.
     * The legend would have been set by addLegend().
     *
     * @param dataSetIndex
     *            The data set index
     * @return The legend label, or an empty string if there is none
     *
     * @since 1.0
     */
    @SuppressWarnings("nls")
    public String getLegend( final int dataSetIndex ) {
        final JLabel legendLabel = legendLabels.elementAt( getDatasetIndex( dataSetIndex ) );
        return ( legendLabel != null ) ? legendLabel.getText() : "";
    }

    ////////////////////// Legend manipulation methods ///////////////////////

    /**
     * Clears all legends. This will show up on the next repaint.
     *
     * @since 1.0
     */
    public void clearLegends() {
        for ( int dataSetIndex = 0; dataSetIndex < DEFAULT_NUMBER_OF_DATASETS; dataSetIndex++ ) {
            clearLegend( dataSetIndex );
        }

        // Reset the counter for how many data sets are in use.
        numberOfDataSetsInUse = 0;
    }

    /**
     * Clears the specified legend. This will show up on the next repaint.
     *
     * @param dataSetIndex
     *            The data set index
     *
     * @since 1.0
     */
    public void clearLegend( final int dataSetIndex ) {
        if ( dataSetIndex < 0 ) {
            throw new IllegalArgumentException( "ChartLegend.clearLegend: Cannot" //$NON-NLS-1$
                    + " give a negative number for the data set index." ); //$NON-NLS-1$
        }

        legendDataSets.setElementAt( Integer.valueOf( dataSetIndex ), dataSetIndex );
        legendLabels.setElementAt( new JLabel( "" ), dataSetIndex ); //$NON-NLS-1$
        iconsForDarkBackground.setElementAt( null, dataSetIndex );
        iconsForLightBackground.setElementAt( null, dataSetIndex );

        // Adjust the counter for how many data sets are in use.
        --numberOfDataSetsInUse;

        // Make sure we erase the cleared legend from the screen.
        repaint();
    }

    /**
     * Adds a legend for the specified data set with the specified string.
     * <p>
     * Short strings generally fit better than long strings. If the string is
     * empty, or the argument is null, then no legend is added.
     *
     * @param dataSetIndex
     *            The data set index
     * @param legends
     *            The labels for the data sets
     * @param colorsForDarkBackground
     *            The colors for the data sets against a dark background
     * @param colorsForLightBackground
     *            The colors for the data sets against a light background
     *
     * @since 1.0
     */
    public void addLegend( final int dataSetIndex,
                           final String[] legends,
                           final Color[] colorsForDarkBackground,
                           final Color[] colorsForLightBackground ) {
        if ( dataSetIndex < 0 ) {
            throw new IllegalArgumentException( "ChartLegend.addLegend: Cannot" //$NON-NLS-1$
                    + " give a negative number for the data set index." ); //$NON-NLS-1$
        }

        addLegend( dataSetIndex,
                   legends[ dataSetIndex ],
                   colorsForDarkBackground[ dataSetIndex ],
                   colorsForLightBackground[ dataSetIndex ] );
    }

    /**
     * Adds a legend for the specified data set with the specified string
     * <p>
     * Short strings generally fit better than long strings. If the string is
     * empty, or the argument is null, then no legend is added.
     *
     * @param dataSetIndex
     *            The data set index
     * @param legend
     *            The label for the data set
     * @param colorForDarkBackground
     *            The color for the data set against a dark background
     * @param colorForLightBackground
     *            The color for the data set against a light background
     *
     * @since 1.0
     */
    public void addLegend( final int dataSetIndex,
                           final String legend,
                           final Color colorForDarkBackground,
                           final Color colorForLightBackground ) {
        if ( legend.trim().isEmpty() || ( colorForDarkBackground == null )
                || ( colorForLightBackground == null ) ) {
            return;
        }
        if ( dataSetIndex < 0 ) {
            throw new IllegalArgumentException( "ChartLegend.addLegend: Cannot" //$NON-NLS-1$
                    + " give a negative number for the data set index." ); //$NON-NLS-1$
        }

        legendDataSets.setElementAt( Integer.valueOf( dataSetIndex ), numberOfDataSetsInUse );

        final JLabel legendLabel = legendLabels.elementAt( numberOfDataSetsInUse );
        legendLabel.setText( legend );

        final int iconSize = ICON_SIZE;
        final BufferedImage bimageForDarkBackground = new BufferedImage( iconSize,
                                                                         iconSize,
                                                                         Transparency.BITMASK );
        final BufferedImage bimageForLightBackground = new BufferedImage( iconSize,
                                                                          iconSize,
                                                                          Transparency.BITMASK );
        for ( int i = 0; i < iconSize; i++ ) {
            for ( int j = 0; j < iconSize; j++ ) {
                bimageForDarkBackground.setRGB( i, j, colorForDarkBackground.getRGB() );
                bimageForLightBackground.setRGB( i, j, colorForLightBackground.getRGB() );
            }
        }
        final ImageIcon imageIconForDarkBackground = new ImageIcon( bimageForDarkBackground );
        final ImageIcon imageIconForLightBackground = new ImageIcon( bimageForLightBackground );

        iconsForDarkBackground.setElementAt( imageIconForDarkBackground, numberOfDataSetsInUse );
        iconsForLightBackground.setElementAt( imageIconForLightBackground, numberOfDataSetsInUse );

        legendLabel.setIcon( legendIcons.elementAt( numberOfDataSetsInUse ) );

        numberOfDataSetsInUse++;
    }

    ///////////////////// XPanel method overrides //////////////////////////

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

        // Make sure the foreground color is never masked by the background.
        final Color foreColor = ColorUtilities.getForegroundFromBackground( backColor );

        // This takes care of the unused parts of the layout, to avoid gaps of
        // the wrong background color between layout regions for subcomponents.
        legendSubpanel.setBackground( backColor );
        legendSubpanel.setForeground( foreColor );

        // This clears up problems with background color not being honored by
        // the Mac OS X Look and Feel for Swing when a Spring Layout is used.
        final JRootPane rootPane = legendSubpanel.getRootPane();
        if ( rootPane != null ) {
            rootPane.setBackground( backColor );
            rootPane.setForeground( foreColor );
        }

        // Always check for the presence of a Titled Border, as it has specific
        // API for matching its title text color to the foreground in use.
        final Border border = legendSubpanel.getBorder();
        if ( border instanceof TitledBorder ) {
            final TitledBorder titledBorder = ( TitledBorder ) border;
            titledBorder.setTitleColor( foreColor );
        }

        // Set which legend icons to use based on dark vs. light background.
        legendIcons = ColorUtilities.isColorDark( backColor )
            ? iconsForDarkBackground
            : iconsForLightBackground;

        JLabel legendLabel;
        for ( int dataSetIndex = 0; dataSetIndex < DEFAULT_NUMBER_OF_DATASETS; dataSetIndex++ ) {
            legendLabel = legendLabels.elementAt( dataSetIndex );
            legendLabel.setBackground( backColor );
            legendLabel.setForeground( foreColor );
            legendLabel.setIcon( legendIcons.elementAt( dataSetIndex ) );
        }
    }

    ///////////////////// JComponent method overrides ////////////////////////

    /**
     * Paints this panel using the current Graphics Context canvas.
     * <p>
     * This is the preferred repaint method to override and invoke in Swing.
     * <p>
     * The only reason for overriding it here, is so that we can deal with the
     * specifics of rendering order, exclusions, and additions, when our
     * Graphics Context is part of a Vector Graphics export action.
     *
     * @param graphicsContext
     *            The Graphics Context to use as the canvas for rendering this
     *            panel
     *
     * @since 1.0
     */
    @Override
    public void paintComponent( final Graphics graphicsContext ) {
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
            // We should never paint the background image for vectorization, as
            // it is the entire panel and not the loaded image or the plot.
            if ( !isVectorizationActive() ) {
                super.paintComponent( graphics2D );
            }

            // Iterate over all of the data sets and paint their legends.
            final Enumeration< Integer > dataSets = legendDataSets.elements();
            while ( dataSets.hasMoreElements() ) {
                final int dataSetNumber = dataSets.nextElement().intValue();
                final int dataSetIndex = getDatasetIndex( dataSetNumber );
                final JLabel legendLabel = legendLabels.elementAt( dataSetIndex );
                if ( legendLabel != null ) {
                    final String legend = legendLabel.getText();
                    if ( !legend.trim().isEmpty() && ( dataSetIndex < numberOfDataSetsInUse ) ) {
                        if ( !isVectorizationActive() ) {
                            // Paint the legend label (icon plus title)
                            // directly, for this data set.
                            legendLabel.repaint();
                        }
                        else {
                            final ImageIcon legendIcon = legendIcons.elementAt( dataSetIndex );
                            if ( legendIcon != null ) {
                                // Paint the image icon for the legend key.
                                final int iconOffsetHz = ( int ) Math.floor( 0.5d * ICON_SIZE );
                                final int iconOffsetVt = ( int ) Math.floor( 0.25 * ICON_SIZE );
                                final int legendLabelX = legendLabel.getX();
                                final int legendLabelY = legendLabel.getY();
                                final int legendIconX = legendLabelX - legendIcon.getIconWidth()
                                        - iconOffsetHz;
                                final int legendIconY =
                                                      ( legendLabelY - legendIcon.getIconHeight() )
                                                              + iconOffsetVt;
                                legendIcon.paintIcon( this, graphics2D, legendIconX, legendIconY );

                                // Cache the current color to restore later.
                                final Color color = graphics2D.getColor();

                                // Use the standard foreground color for the
                                // legend labels. No need to set foreground or
                                // background.
                                graphics2D.setColor( getForeground() );

                                // Draw the label for the data set's legend key.
                                graphics2D.drawString( legend, legendLabelX, legendLabelY );

                                // Restore the previous color.
                                graphics2D.setColor( color );
                            }
                        }
                    }
                }
            }

            // Dispose of Graphics Context now that we have finished with it.
            if ( !isVectorizationActive() ) {
                graphics2D.dispose();
            }
        }

        // Show new background image, or re-display old background image.
        //
        // Vectorization needs to maintain line vector graphics vs. taking
        // advantage of efficiencies in off-screen buffering.
        if ( !isVectorizationActive() ) {
            showBackgroundImage( graphicsContext );
        }
    }

}
