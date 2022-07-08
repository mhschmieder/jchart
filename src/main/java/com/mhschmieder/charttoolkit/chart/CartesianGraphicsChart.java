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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RasterFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.mhschmieder.graphicstoolkit.color.ColorConstants;
import com.mhschmieder.graphicstoolkit.geometry.AttributedShape;
import com.mhschmieder.graphicstoolkit.geometry.AttributedShapeContainer;
import com.mhschmieder.mathtoolkit.geometry.GridResolution;
import com.mhschmieder.physicstoolkit.DistanceUnit;
import com.mhschmieder.physicstoolkit.UnitConversion;

/**
 * The CartesianGraphicsChart class is derived directly from the CartesianChart
 * container class, since it doesn't plot data points but rather maps raster
 * images and vector graphics to spatial coordinates.
 * 
 * TODO: Make another layer to the class hierarchy to simplify what is needed
 * for stuff like SPL Palette.
 * 
 * NOTE: All coordinate based variables are cached in meters; only the plot
 * grid overlay and tic marks are converted to display units.
 * 
 * NOTE: There is now an exception as the Plot Boundary is stored in display
 * units due to data binding between the cached model and the GUI in the new
 * JavaFX Drawing Limits and Region properties windows.
 */
public class CartesianGraphicsChart extends CartesianChart {
    /**
     *
     */
    private static final long  serialVersionUID      = 2635236776918249395L;

    public static final double X_METERS_DEFAULT      = 0.0d;
    public static final double Y_METERS_DEFAULT      = 0.0d;
    public static final double WIDTH_METERS_DEFAULT  = 40d;
    public static final double HEIGHT_METERS_DEFAULT = 20.0d;

    // Regardless of whether an image is loaded, plots that can host background
    // images must take different tactics towards grid color than using the
    // background color.
    // NOTE: Original color was ( 217, 223, 214 ) -- maybe different order?
    public static final Color  GRID_COLOR_DEFAULT    = ColorConstants.GRAY60;

    // NOTE: This is a unitless method, but does assume the units are at
    // least consistent. Preferably everything is metric (meters).
    protected static final boolean contains( final Rectangle2D rect,
                                             final Point2D point,
                                             final boolean useFuzzyEq ) {
        if ( useFuzzyEq ) {
            // Add a fudge factor to account for floating point imprecision.
            final double fudgeFactor = 0.02 * Math.max( rect.getWidth(), rect.getHeight() );
            final Rectangle2D rect2 = new Rectangle2D.Double( rect.getX() - fudgeFactor,
                                                              rect.getY() - fudgeFactor,
                                                              rect.getWidth() + fudgeFactor,
                                                              rect.getHeight() + fudgeFactor );
            return rect2.contains( point );
        }

        return rect.contains( point );
    }

    // Declare an image object to hold image data.
    private BufferedImage            _bufferedImage;

    // Keep track of what units we're using to display, for later conversion.
    public DistanceUnit              _distanceUnit;

    // Maintain a reference to the overall boundary of the plot, in Meters.
    public Rectangle2D               _plotBoundary;

    // Declare a current zoom which corresponds to a typical desktop window
    // aspect ratio and initial chart size.
    public final Rectangle2D         _zoomCurrent            =
                                                  new Rectangle2D.Double( X_METERS_DEFAULT,
                                                                          Y_METERS_DEFAULT,
                                                                          WIDTH_METERS_DEFAULT,
                                                                          HEIGHT_METERS_DEFAULT );

    // Declare an Image Plane (updated separately from other bounds, on image
    // load).
    protected Rectangle2D            _imagePlane;

    // Declare an image bounding rectangle (updated when zoomed, if supported).
    private Rectangle2D              _imageBounds;

    // Declare variable to keep track of the current grid resolution (default to
    // grid ff).
    private GridResolution           _gridResolution         = GridResolution.OFF;

    // Declare conversion factors for display of (possibly zoomed) image.
    public double                    _modelToViewRatioX;
    public double                    _modelToViewRatioY;

    // Declare a flag to indicate whether imported geometry is active or not.
    private boolean                  _importedGeometryActive = false;

    // Declare a module-scoped geometry container for the imported geometry
    // entities, reset per import action.
    private AttributedShapeContainer _geometryContainer;

    // Imported Geometry may need a different stroke style than other stuff.
    private final Stroke             _importedGeometryStroke;

    // Declare variables to keep track of the mouse location, for zooming/etc.
    public int                       _soundFieldMouseX       = 0;
    public int                       _soundFieldMouseY       = 0;

    // Declare status flag for whether the context pop-up menu is active or not.
    private boolean                  _contextMenuActive      = false;

    // NOTE: There is nothing extra for the default constructor to do, as the
    // image and associated stream cannot be set until the image is added.
    public CartesianGraphicsChart( final boolean useWatermark,
                                   final String jarRelativeWatermarkIconFilename ) {
        // Always call the superclass constructor first!
        super( useWatermark, jarRelativeWatermarkIconFilename );

        _distanceUnit = DistanceUnit.defaultValue();

        // Use slightly narrower strokes for Imported Geometry.
        _importedGeometryStroke = new BasicStroke( 0.75f );

        try {
            initPanel();
        }
        catch ( final Exception ex ) {
            ex.printStackTrace();
        }
    }

    public final void addYTicAutoLabel( final double positionInUserUnits,
                                        final int minFractionalDigits,
                                        final int maxFractionalDigits ) {
        final NumberFormat ticNumberFormat = ( NumberFormat ) numberFormat.clone();
        ticNumberFormat.setMinimumFractionDigits( minFractionalDigits );
        ticNumberFormat.setMaximumFractionDigits( maxFractionalDigits );
        addYTic( ticNumberFormat.format( positionInUserUnits ), positionInUserUnits );
    }

    protected final void addYTicAutoLabel( final int positionInUserUnits ) {
        final NumberFormat ticNumberFormat = ( NumberFormat ) numberFormat.clone();
        addYTic( ticNumberFormat.format( positionInUserUnits ), positionInUserUnits );
    }

    // Adjust the aspect ratio manually due to PtPlot auto-scale issues.
    private final void adjustAspectRatio() {
        // NOTE: We must guarantee that the Sound Field aspect ratio has the
        // same meters to pixel ratio in both dimensions, since it can't be
        // auto-scaled properly (there are anomalies based on the application
        // scale).
        // TODO: Discover whether this is necessary, or if plot can automate,
        // if we set the labels explicitly (including their positions).
        final double aspectRatio = _zoomCurrent.getWidth() / _zoomCurrent.getHeight();
        setAspectRatio( aspectRatio );
        setAspectRatioApplied( true );
    }

    // The point of this method is to take advantage of off-screen buffering.
    // NOTE: This gets invoked indirectly by the superclass paintComponent().
    @Override
    public void drawChart( final Graphics2D graphicsContext ) {
        // NOTE: We have to call the "super" class first, to make sure we
        // preserve the look-and-feel of the owner component.
        super.drawChart( graphicsContext );

        // Update the drawing dimensions and conversion factors. Recalculate the
        // zoom to plot mapping ratios, based on the current plot size.
        _modelToViewRatioX = ( getLrx() - getUlx() ) / _zoomCurrent.getWidth();
        _modelToViewRatioY = ( getLry() - getUly() ) / _zoomCurrent.getHeight();

        // Repaint the architectural entity graphics for the entire
        // collection.
        if ( _importedGeometryActive && ( _geometryContainer != null ) ) {
            paintEntities( graphicsContext, _geometryContainer );
        }
    }

    // Get the default position (in meters) as either the current mouse
    // position, or as a ratio applied to the current zoom.
    public final Point2D getDefaultPosition( final boolean useMousePosition,
                                             final double scaleX,
                                             final double scaleY ) {
        Point2D defaultPosition = null;
        if ( useMousePosition || isContextMenuActive() ) {
            defaultPosition =
                            getViewPointInModelCoordinates( _soundFieldMouseX, _soundFieldMouseY );
            // TODO: Review whether this is the exclusion criteria we wish to
            // use.
            if ( contains( _zoomCurrent, defaultPosition, true ) ) {
                return defaultPosition;
            }
        }

        final double x = _zoomCurrent.getX() + ( _zoomCurrent.getWidth() * scaleX );
        final double y = _zoomCurrent.getY() + ( _zoomCurrent.getHeight() * scaleY );
        defaultPosition = new Point2D.Double( x, y );

        return defaultPosition;
    }

    public Color getGridColorDefault( final Color backColor ) {
        // Regardless of whether an image is loaded, plots that can host
        // background images must take different tactics towards grid color than
        // using the background color.
        return GRID_COLOR_DEFAULT;
    }

    public final GridResolution getGridResolution() {
        return _gridResolution;
    }

    protected final BufferedImage getImage() {
        return _bufferedImage;
    }

    protected final Rectangle getImageBoundsInScreenCoordinates( final BufferedImage image,
                                                                 final int plotX,
                                                                 final int plotY,
                                                                 final int plotWidth,
                                                                 final int plotHeight ) {
        // If zooming is not supported, scale the image to the plot boundary.
        if ( _imageBounds == null ) {
            final Rectangle imageBoundsInScreenCoordinates = new Rectangle( plotX,
                                                                            plotY,
                                                                            plotWidth,
                                                                            plotHeight );
            return imageBoundsInScreenCoordinates;
        }

        // Calculate the scaling ratios of the cached sub-image plane (in
        // meters) to the cached raw sub-image (in bits).
        final int rawSubImageWidthInBits = image.getWidth();
        final int rawSubImageHeightInBits = image.getHeight();
        final double xScale = ( rawSubImageWidthInBits != 0 )
            ? ( _imageBounds.getWidth() / rawSubImageWidthInBits )
            : 1.0;
        final double yScale = ( rawSubImageHeightInBits != 0 )
            ? ( _imageBounds.getHeight() / rawSubImageHeightInBits )
            : 1.0;

        // Now get the ratios of meters to pixels.
        final double xPixelScale = plotWidth / _zoomCurrent.getWidth();
        final double yPixelScale = plotHeight / _zoomCurrent.getHeight();

        // From this, get the bits to pixels scaling.
        final double xBitsToPixelsScale = xScale * xPixelScale;
        final double yBitsToPixelsScale = yScale * yPixelScale;

        // Find the sub-image bounds in pixels, converted from bits.
        final int imageWidthPixels = ( int ) Math
                .round( rawSubImageWidthInBits * xBitsToPixelsScale );
        final int imageHeightPixels = ( int ) Math
                .round( rawSubImageHeightInBits * yBitsToPixelsScale );

        // Adjust the plot origin for the differential, when the current zoom
        // origin is less than that of the image bounds.
        final double xDifferential = _zoomCurrent.getX() - _imageBounds.getX();
        final double yDifferential = _zoomCurrent.getY() - _imageBounds.getY();
        final int plotXAdjusted = ( xDifferential < 0.0 )
            ? plotX - ( int ) Math.round( xDifferential * xPixelScale )
            : plotX;
        final int plotYAdjusted = ( yDifferential < 0.0 )
            ? ( ( plotY + plotHeight ) - imageHeightPixels )
                    + ( int ) Math.round( yDifferential * yPixelScale )
            : ( ( plotY + plotHeight ) - imageHeightPixels );

        // Scale the potentially clipped sub-image to the zoom box, with upper
        // vs. lower left origin.
        final Rectangle imageBoundsInScreenCoordinates = new Rectangle( plotXAdjusted,
                                                                        plotYAdjusted,
                                                                        imageWidthPixels,
                                                                        imageHeightPixels );
        return imageBoundsInScreenCoordinates;
    }

    // Transform the specified point from model coordinates (meters) to view
    // coordinates (pixels).
    // TODO: Cache the transform, only updating as necessary.
    public final Point2D getModelPointInViewCoordinates( final double x, final double y ) {
        final AffineTransform modelToViewTransform = getModelToViewTransform();
        final Point2D pointInViewCoordinates = modelToViewTransform
                .transform( new Point2D.Double( x, y ), null );
        return pointInViewCoordinates;
    }

    // TODO: Cache this and only recompute when necessary (including at
    // repaint() time), in place of the modelToViewRatio variables (which are
    // updated at repaint() time).
    public final AffineTransform getModelToViewTransform() {
        // Move the origin from the lower left of the current zoom to the upper
        // left of the Sound Field's PlotBox parent component.
        final AffineTransform modelToViewTransform = AffineTransform
                .getTranslateInstance( getUlx(), getLry() );

        // Scale from model units (meters) to view units (pixels), negating the
        // Y-axis scale factor to ensure that the room is simply painted from
        // top to bottom vs. from bottom to top (otherwise, a simple translation
        // by itself would move the room display out of the plot box!).
        modelToViewTransform.scale( ( float ) _modelToViewRatioX, ( float ) -_modelToViewRatioY );

        // When zooming (which may display a smaller area of the room which
        // does not start at the room's origin), the lower left corner of the
        // zoom area has to be moved down to the lower left corner of the plot.
        modelToViewTransform.translate( -_zoomCurrent.getX(), -_zoomCurrent.getY() );

        return modelToViewTransform;
    }

    // NOTE: This version of the method accounts for an additional transform.
    // Most often that is needed in the context of imported geometry.
    // TODO: Cache this and only recompute when necessary (including at
    // repaint() time), in place of the modelToViewRatio variables (which are
    // updated at repaint() time).
    public final AffineTransform getModelToViewTransform( final AffineTransform shapeTransform,
                                                          final AffineTransform containerTransform ) {
        // Move the origin from the lower left of the current zoom to the upper
        // left of the Sound Field's PlotBox parent component.
        final double translateX = getUlx(); // + shapeTransform.getTranslateX();
        final double translateY = getLry(); // + shapeTransform.getTranslateY();
        final AffineTransform modelToViewTransform = AffineTransform
                .getTranslateInstance( translateX, translateY );

        // Scale from model units (meters) to view units (pixels), negating the
        // Y-axis scale factor to ensure that the room is simply painted from
        // top to bottom vs. from bottom to top (otherwise, a simple translation
        // by itself would move the room display out of the plot box!).
        final double scaleX = _modelToViewRatioX;
        final double scaleY = _modelToViewRatioY;
        modelToViewTransform.scale( ( float ) scaleX, ( float ) -scaleY );

        // When zooming (which may display a smaller area of the room which
        // does not start at the room's origin), the lower left corner of the
        // zoom area has to be moved down to the lower left corner of the plot.
        modelToViewTransform.translate( -_zoomCurrent.getX(), -_zoomCurrent.getY() );

        // End with the container transform, as it should be applied last, but
        // first combine it with the local shape transform as both are in the
        // same coordinate space.
        final AffineTransform combinedTransform = new AffineTransform( containerTransform );
        combinedTransform.concatenate( shapeTransform );
        modelToViewTransform.concatenate( combinedTransform );

        return modelToViewTransform;
    }

    // This method returns a sub-image from the original image, cropped from
    // the full raw image such that its origin corresponds to the current zoom
    // origin and its aspect ratio to the current zoom window's aspect ratio.
    protected BufferedImage getSubimage() {
        // Get a local copy of the full original cached raw image.
        final BufferedImage image = getImage();

        // Avoid throwing null pointer exceptions by exiting early with the
        // original raw image (even if it too is null).
        if ( ( _imagePlane == null ) || ( _imageBounds == null ) || ( image == null ) ) {
            return image;
        }

        // Calculate the scaling ratios of the cached full Image Plane (in
        // meters) to the full original cached raw image (in bits).
        final int rawImageWidthInBits = image.getWidth();
        final int rawImageHeightInBits = image.getHeight();
        final double xScale = ( _imagePlane.getWidth() != 0 )
            ? ( rawImageWidthInBits / _imagePlane.getWidth() )
            : 1.0d;
        final double yScale = ( _imagePlane.getHeight() != 0 )
            ? ( rawImageHeightInBits / _imagePlane.getHeight() )
            : 1.0d;

        // Get the sub-image corresponding to the current image bounds.
        // NOTE: Images are displayed from top to bottom, so we use the upper
        // left corner, converting from the image plane's coordinate system.
        // NOTE: We adjust the image by the inverse of its bounds, so that
        // (0, 0) of the original image still maps to (0, 0) of the plot
        // coordinate system; accounting for origin-shift also.
        // NOTE: We have to round up for the origin, and round down for the
        // dimensions, as otherwise we run the risk of being one bit outside the
        // original image boundary. This should not affect accuracy, however.
        final int imageX =
                         ( int ) Math.ceil( ( _imageBounds.getX() - _imagePlane.getX() ) * xScale );
        final int imageY = ( int ) Math.ceil( ( ( _imagePlane.getY() + _imagePlane.getHeight() )
                - ( _imageBounds.getY() + _imageBounds.getHeight() ) ) * yScale );
        final int imageWidth =
                             ( int ) Math.max( 1, Math.floor( _imageBounds.getWidth() * xScale ) );
        final int imageHeight = ( int ) Math.max( 1,
                                                  Math.floor( _imageBounds.getHeight() * yScale ) );

        try {
            final BufferedImage subimage = image
                    .getSubimage( imageX, imageY, imageWidth, imageHeight );
            return subimage;
        }
        catch ( final RasterFormatException rfe ) {
            rfe.printStackTrace();
        }

        return null;
    }

    // NOTE: This method assumes the incoming shape is in meters and degrees.
    // All invokers of this method are responsible for converting any units
    // not already in these defaults. The reason this is done here is that the
    // graphics have many points that are computed in metric in the loudspeaker
    // library, as does DXF. So the overall shape is transformed, vs. key
    // attributes of the graphical object such as view location.
    protected final Shape getTransformedShape( final Shape shape ) {
        // Convert to a transformed generic shape, for display.
        final AffineTransform modelToViewTransform = getModelToViewTransform();
        return modelToViewTransform.createTransformedShape( shape );
    }

    // NOTE: This method takes care of the provided additional transform.
    protected final Shape getTransformedShape( final Shape shape,
                                               final AffineTransform shapeTransform,
                                               final AffineTransform containerTransform ) {
        // Convert to a transformed generic shape, for display.
        final AffineTransform modelToViewTransform = getModelToViewTransform( shapeTransform,
                                                                              containerTransform );
        return modelToViewTransform.createTransformedShape( shape );
    }

    // Transform the specified point from view coordinates (pixels) to model
    // coordinates (meters).
    // TODO: Cache the transform, only updating as necessary.
    public final Point2D getViewPointInModelCoordinates( final int x, final int y ) {
        final AffineTransform viewToModelTransform = getViewToModelTransform();
        if ( viewToModelTransform == null ) {
            return new Point2D.Double( 0.0, 0.0 );
        }
        final Point2D pointInModelCoordinates = viewToModelTransform
                .transform( new Point2D.Double( x, y ), null );
        return pointInModelCoordinates;
    }

    // TODO: Cache this and only recompute when necessary (including at
    // repaint() time), in place of the modelToViewRatio variables (which are
    // updated at repaint() time).
    public final AffineTransform getViewToModelTransform() {
        AffineTransform viewToModelTransform = getModelToViewTransform();

        try {
            viewToModelTransform = viewToModelTransform.createInverse();
            return viewToModelTransform;
        }
        catch ( final NoninvertibleTransformException nte ) {
            nte.printStackTrace();
            return null;
        }
    }

    private final void initPanel() {
        // Ensure that the axis labels never exceed one decimal place of
        // precision, and only use decimals when needed (not for integers).
        setMinimumFractionDigits( 0 );
        setMaximumFractionDigits( 1 );
    }

    public final boolean isContextMenuActive() {
        return _contextMenuActive;
    }

    public final boolean isImageValid() {
        return _bufferedImage != null;
    }

    // Query the imported geometry active state
    public final boolean isImportedGeometryActive() {
        return _importedGeometryActive;
    }

    protected final boolean loadImage( final InputStream inputStream ) {
        // Avoid throwing unnecessary exceptions by not attempting to open bad
        // input streams.
        if ( inputStream == null ) {
            return false;
        }

        try {
            // Load the image if there isn't one already loaded, using immediate
            // mode imaging (i.e. BufferedImage), so that the image is
            // immediately available for compositing with the watermark.
            if ( !isImageValid() ) {
                // The format type is inferred from the filename extension.
                // There are problems with PNG under Mac OS X, however, so it is
                // recommended that uncompressed GIF's be used instead. The
                // problem occurs when certain color table sizes are used
                // (multiples of 64 always work fine).
                final BufferedImage bufferedImage = ImageIO.read( inputStream );
                paintImage( bufferedImage );
            }
        }
        catch ( final IOException ioe ) {
            ioe.printStackTrace();
            return false;
        }

        // Any change to the image requires regenerating the off-screen buffer.
        regenerateOffScreenImage = true;

        return true;
    }

    // Nullify the image (useful for sub-woofers).
    // TODO: Modify to use the current palette vs. Jet Palette assumptions.
    protected final void nullifyImage() {
        // Flush the overlay image resources to ensure that the new overlay
        // image is loaded vs. the cached overlay image.
        removeImage();

        // Update the cached image with one that is at the bottom end of the
        // dynamic range of the current assumption of Jet Palette. Note that
        // this is a very dark blue, indicating no energy. Jet Palette
        // documentation says it starts at mid-intensity.
        final BufferedImage bimage = new BufferedImage( 1, 1, Transparency.BITMASK );
        bimage.setRGB( 0, 0, 0xFF00007F );
        setImage( bimage );

        // Force a repaint event, to display/update the new image.
        repaint();
    }

    // Nullify the Image Plane.
    // NOTE: This is no longer called, as showing all-blue plots to indicate
    // null data was deemed confusing.
    protected final void nullifyImagePlane( final Rectangle2D imagePlane ) {
        // Nullify the image.
        nullifyImage();

        // Reset the Image Plane to the current Region, as it may change later
        // and we don't want that to have a side effect on image display size
        // and origin.
        updateImagePlane( imagePlane );
    }

    // The point of this method is to take advantage of off-screen buffering.
    @Override
    public void paintBackgroundImage( final Graphics2D g2 ) {
        // Repaint the overlay image (or sub-image, if usage conditions support
        // an image bounding plane) if there is a raw image loaded and cached.
        final BufferedImage image = getSubimage();
        if ( image != null ) {
            // Save the current clipping so that we can restore it after
            // painting the background image.
            final Shape clip = g2.getClip();

            // Fetch the current plot boundaries, in pixels.
            final int plotX = getUlx();
            final int plotY = getUly();
            final int plotWidth = getLrx() - plotX;
            final int plotHeight = getLry() - plotY;

            // Set the clipping so that the background image does not extend
            // past the plot boundaries.
            g2.setClip( plotX, plotY, plotWidth, plotHeight );

            // Get the conditionally adjusted image bounds in screen
            // coordinates, using an AWT Rectangle as we need integers.
            final Rectangle imageBoundsInScreenCoordinates =
                                                           getImageBoundsInScreenCoordinates( image,
                                                                                              plotX,
                                                                                              plotY,
                                                                                              plotWidth,
                                                                                              plotHeight );

            // Scale the image to the screen, with upper vs. lower left origin.
            final Image scaledImage =
                                    image.getScaledInstance( imageBoundsInScreenCoordinates.width,
                                                             imageBoundsInScreenCoordinates.height,
                                                             Image.SCALE_AREA_AVERAGING );
            g2.drawImage( scaledImage,
                          imageBoundsInScreenCoordinates.x,
                          imageBoundsInScreenCoordinates.y,
                          this );

            // Restore the clipping to what it was before we painted the
            // background image.
            g2.setClip( clip );
        }
    }

    // This method loops through a collection of graphical entities and paints
    // them on the view.
    protected final void paintEntities( final Graphics2D g2,
                                        final AttributedShapeContainer geometryContainer ) {
        // For effiency's sake, exit early if nothing to paint.
        if ( ( geometryContainer == null ) || ( geometryContainer.getNumberOfShapes() < 1 ) ) {
            return;
        }

        // Save the current clipping so that we can restore it after painting.
        final Shape clip = g2.getClip();

        // Save the current stroke so that we can restore it after painting.
        final Stroke stroke = g2.getStroke();

        // Save the current color so that we can restore it after painting.
        final Color color = g2.getColor();

        // Set the clipping to prevent the graphical entities from writing
        // beyond the plot boundaries, as this keeps presentations clean.
        final int plotX = getUlx();
        final int plotY = getUly();
        final int plotWidth = getLrx() - plotX;
        final int plotHeight = getLry() - plotY;
        g2.setClip( plotX, plotY, plotWidth, plotHeight );

        // Use a solid line unconditionally.
        // TODO: Apply DXF line type info, and move this back inside the
        // per-entity loop?
        g2.setStroke( _importedGeometryStroke );

        // Repaint the geometry shapes for the entire collection.
        final AffineTransform scaleTransform = geometryContainer.getScaleTransform();
        for ( final AttributedShape shapeContainer : geometryContainer.getAttributedShapes() ) {
            // Get the transformed graphics for this geometric entity.
            final AffineTransform transform = new AffineTransform( scaleTransform );
            final Shape shape = getTransformedShape( shapeContainer.shape,
                                                     shapeContainer.transform,
                                                     transform );

            // Use the entity-specific color; being sure to reverse black and
            // white depending on the current background color.
            final Color backgroundColor = getBackground();
            if ( Color.BLACK.equals( shapeContainer.penColor )
                    && ( Color.BLACK.equals( backgroundColor )
                            || Color.DARK_GRAY.equals( backgroundColor ) ) ) {
                g2.setColor( Color.WHITE );
            }
            else if ( Color.DARK_GRAY.equals( shapeContainer.penColor )
                    && ( Color.BLACK.equals( backgroundColor )
                            || Color.DARK_GRAY.equals( backgroundColor ) ) ) {
                g2.setColor( Color.LIGHT_GRAY );
            }
            else if ( Color.LIGHT_GRAY.equals( shapeContainer.penColor )
                    && ( !Color.BLACK.equals( backgroundColor )
                            && !Color.DARK_GRAY.equals( backgroundColor ) ) ) {
                g2.setColor( Color.DARK_GRAY );
            }
            else if ( Color.WHITE.equals( shapeContainer.penColor )
                    && ( !Color.BLACK.equals( backgroundColor )
                            && !Color.DARK_GRAY.equals( backgroundColor ) ) ) {
                g2.setColor( Color.BLACK );
            }
            else {
                g2.setColor( shapeContainer.penColor );
            }

            // Draw or Fill the transformed architectural entity.
            switch ( shapeContainer.drawMode ) {
            case STROKE:
                g2.draw( shape );
                break;
            case FILL:
                g2.fill( shape );
                break;
            case CLIP:
                break;
            default:
                break;
            }
        }

        // Restore the clipping to what it was before we painted the graphical
        // entities.
        g2.setColor( color );

        // Restore the stroke to what it was before we painted the graphical
        // entities.
        g2.setStroke( stroke );

        // Restore the clipping to what it was before we painted the graphical
        // entities.
        g2.setClip( clip );
    }

    private final void paintImage( final BufferedImage bufferedImage ) {
        final int imageWidth = bufferedImage.getWidth();
        final int imageHeight = bufferedImage.getHeight();
        _bufferedImage = new BufferedImage( imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB );
        final Graphics2D g2 = _bufferedImage.createGraphics();
        g2.drawImage( bufferedImage, 0, 0, this );

        // If relevant, composite the watermark with the image.
        if ( useWatermark ) {
            // Scale the watermark first, and also place it centrally, such that
            // the watermark width is roughly 75% the width of the original
            // image.
            // NOTE: Bi-cubic interpolation is best, but I can't find a way to
            // access that algorithm from the JAI API.
            Image watermarkImage = watermarkIcon.getImage();
            int watermarkWidth = watermarkImage.getWidth( null );
            int watermarkHeight = watermarkImage.getHeight( null );
            final float aspectRatio = watermarkHeight / ( float ) watermarkWidth;
            final float scaleFactor = 0.75f;
            watermarkWidth = Math.round( scaleFactor * imageWidth );
            watermarkHeight = Math.round( watermarkWidth * aspectRatio );

            // Verify that the watermark is not clipped vertically. If it is,
            // rescale to 75% of image height.
            if ( watermarkHeight >= ( 0.95f * imageHeight ) ) {
                watermarkHeight = Math.round( 0.75f * imageHeight );
                watermarkWidth = Math.round( watermarkHeight / aspectRatio );
            }

            // Guarantee at least one pixel in each direction.
            watermarkWidth = Math.max( 1, watermarkWidth );
            watermarkHeight = Math.max( 1, watermarkHeight );

            final AreaAveragingScaleFilter scaleFilter =
                                                       new AreaAveragingScaleFilter( watermarkWidth,
                                                                                     watermarkHeight );
            final ImageProducer imageProducer = new FilteredImageSource( watermarkImage.getSource(),
                                                                         scaleFilter );
            watermarkImage = Toolkit.getDefaultToolkit().createImage( imageProducer );

            // Load the scaled watermark into an ImageIcon, to guarantee it is
            // immediately available vs. still processing in the background.
            // This has been proven to be a necessary step.
            final ImageIcon watermark = new ImageIcon();
            watermark.setImage( watermarkImage );

            // Composite the transparent watermark with the original image. This
            // maintains the integrity of the original image while also being
            // harder to reverse, at the client end, than simply painting the
            // transparent watermark atop the original image.
            //
            // Due to the perception that the registered trademark symbol is not
            // part of most company logos due to the effective width of the core
            // watermark image, we calculate that ratio to the full image width
            // and shift the watermark to the right by that ratio so that the
            // perception is closer to a centered image. That ratio turns out to
            // be roughly 5% of the total width.
            g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER,
                                                         watermarkOpacity ) );
            g2.drawImage( watermark.getImage(),
                          ( int ) ( ( 0.5f * imageWidth ) - ( 0.45f * watermarkWidth ) ),
                          ( int ) ( 0.5f * ( imageHeight - watermarkHeight ) ),
                          this );
        }

        // Dispose of the graphics context for the original image.
        g2.dispose();
    }

    // Recalculate the Image Bounds.
    private final void recalculateImageBounds() {
        if ( _imagePlane != null ) {
            // Compute and cache the Image Bounds as the intersection of the
            // Image Plane with the current zoom box.
            _imageBounds = new Rectangle2D.Double( 0.0, 0.0, 1.0, 1.0 );
            Rectangle2D.intersect( _imagePlane, _zoomCurrent, _imageBounds );

            // Force a repaint event, to display/update the new image
            // size/location.
            repaint();
        }
    }

    // Remove the entities from the container, to recover large memory chunks.
    public final void removeEntities() {
        setImportedGeometryActive( false );
        if ( _geometryContainer != null ) {
            _geometryContainer.clearShapes();
            _geometryContainer = null;
        }
    }

    public final void removeImage() {
        // Flush the overlay image resources to remove the overlay image from
        // the plot.
        if ( _bufferedImage != null ) {
            _bufferedImage.flush();
            _bufferedImage = null;
        }

        // Any change to the image requires regenerating the off-screen buffer.
        regenerateOffScreenImage = true;

        // Force a repaint event, to display/update the plot without the image.
        repaint();
    }

    // NOTE: This method is used when we don't need a stream because the
    // image is a resource loaded into the host app's jar file.
    protected final void replaceImage( final BufferedImage bufferedImage ) {
        // Flush the overlay image resources to ensure that the new overlay
        // image is loaded vs. the cached overlay image.
        removeImage();

        // Update the cached image with the new one.
        setImage( bufferedImage );

        // Force a repaint event, to display/update the new image.
        repaint();
    }

    // Conditionally set a manual grid.
    protected final void rescaleGrid() {
        // Set Grid Resolution.
        switch ( _gridResolution ) {
        case OFF:
            // Default the grid and then disable it.
            setGridScale( 1.0d );
            setGrid( false );
            break;
        case COARSE:
            // Re-enable the grid, and let the plot compute the new tics.
            setGridScale( 2.0d );
            setGrid( true );
            break;
        case MEDIUM:
            // Re-enable the grid, and let the plot compute the new tics.
            setGridScale( 1.0d );
            setGrid( true );
            break;
        case FINE:
            // Re-enable the grid, and let the plot compute the new tics.
            setGridScale( 0.5d );
            setGrid( true );
            break;
        default:
            setGridScale( 1.0d );
            setGrid( false );
            break;
        }

        // Force a repaint event, to display/update the grid at its new
        // resolution.
        repaint();
    }

    // Reset the plot range based on current zoom, in current display units.
    private final void resetPlotRange() {
        // NOTE: Working in metric units and only setting the axis labels to
        // display units works ONLY because we do not plot data in SoundField.
        final double x1 = UnitConversion
                .convertDistance( _zoomCurrent.getX(), DistanceUnit.METERS, _distanceUnit );
        final double x2 = UnitConversion.convertDistance( _zoomCurrent.getX()
                + _zoomCurrent.getWidth(), DistanceUnit.METERS, _distanceUnit );
        setXRange( x1, x2 );
        final double y1 = UnitConversion
                .convertDistance( _zoomCurrent.getY(), DistanceUnit.METERS, _distanceUnit );
        final double y2 = UnitConversion.convertDistance( _zoomCurrent.getY()
                + _zoomCurrent.getHeight(), DistanceUnit.METERS, _distanceUnit );
        setYRange( y1, y2 );
    }

    public final void setContextMenuActive( final boolean contextMenuActive ) {
        _contextMenuActive = contextMenuActive;
    }

    // Set the grid resolution, and conditionally rescale the grid.
    public final void setGridResolution( final GridResolution gridResolution ) {
        _gridResolution = gridResolution;

        rescaleGrid();
    }

    // NOTE: This method is used when we don't need a stream because the
    // image is a resource loaded from the host app's jar file.
    protected final void setImage( final BufferedImage bimage ) {
        // Add the image if there isn't one already loaded.
        if ( _bufferedImage == null ) {
            _bufferedImage = bimage;
        }

        // Any change to the image requires regenerating the off-screen buffer.
        regenerateOffScreenImage = true;
    }

    // Set the imported geometry active state
    protected final void setImportedGeometryActive( final boolean importedGeometryActive ) {
        _importedGeometryActive = importedGeometryActive;
    }

    // This method resets the overall boundary of the plot.
    // NOTE: This method is only called at startup, when blanking a project
    // or when opening one, as well as when importing DXF and setting Auto-Sync
    // to match the DXF Limits. Otherwise its values update in real-time from
    // external editing due to being a reference to a data-bound JavaFX object.
    // This means we should not apply Measurement Units directly to it; only to
    // derivations and propagations of its values. This should cover all cases.
    public final void setPlotBoundary( final Rectangle2D plotBoundary ) {
        // Cache the new Plot Boundary.
        _plotBoundary = plotBoundary;

        // Any change to view extents requires regenerating the off-view buffer.
        regenerateOffScreenImage = true;

        // Zoom all the way out anytime we change the Plot Boundary.
        zoomToExtents();
    }

    public final void setZoomCurrent( final Rectangle2D zoomCurrent ) {
        // Update the current zoom.
        _zoomCurrent.setRect( zoomCurrent );

        // Adjust the aspect ratio manually due to PtPlot auto-scale issues.
        adjustAspectRatio();

        // Reset the plot range based on current zoom, in current display units.
        resetPlotRange();

        // Re-scale the grid overlay, if necessary.
        rescaleGrid();

        // Recalculate the Image Bounds, as they are based on two factors.
        recalculateImageBounds();

        // Force a repaint event, to display/update the image at its new size.
        repaint();
    }

    // Update the Distance Unit in all of the relevant GUI components.
    public final void updateDistanceUnit( final DistanceUnit distanceUnit ) {
        // Store the new Distance Unit to provide context for Loudspeaker
        // Graphics.
        // NOTE: We must do this before calling any methods that cause a
        // repaint, as otherwise we'll have an interim flash update with
        // unscaled Loudspeakers!
        _distanceUnit = distanceUnit;

        // Reset the Distance Unit labels directly, since these are left
        // unchanged by all the other plot update methods.
        // TODO: Use the angle units for displaying in mouse rotate mode?
        final String distanceUnitName = distanceUnit.toCanonicalString();
        setXUnits( distanceUnitName );
        setYUnits( distanceUnitName );

        // Reset the plot range to account for the new Measurement Units.
        resetPlotRange();
    }

    public final void updateEntities( final AttributedShapeContainer importedGraphics ) {
        removeEntities();
        _geometryContainer = importedGraphics;
        setImportedGeometryActive( true );

        // Any change to the DXF entities requires regenerating the off-view
        // buffer.
        regenerateOffScreenImage = true;
    }

    protected final void updateImage( final BufferedImage bufferedImage ) {
        // Flush the overlay image resources to ensure that the new overlay
        // image is loaded vs. the cached overlay image.
        removeImage();

        // Update the cached image with the new one.
        paintImage( bufferedImage );

        // Any change to the image requires regenerating the off-screen buffer.
        regenerateOffScreenImage = true;

        // Force a repaint event, to display/update the new image.
        repaint();
    }

    protected final boolean updateImage( final InputStream inputStream ) {
        // Flush the overlay image resources to ensure that the new overlay
        // image is loaded vs. the cached overlay image.
        removeImage();

        // Update the cached image with the new one.
        final boolean succeeded = loadImage( inputStream );

        // Force a repaint event, to display/update the new image.
        repaint();

        return succeeded;
    }

    // Update the Image Plane.
    protected final void updateImagePlane( final BufferedImage bufferedImage,
                                           final Rectangle2D imagePlane ) {
        // Delegate the image load to the underlying ImagePlot.
        updateImage( bufferedImage );

        // Reset the Image Plane to the current Region, as it may change later
        // and we don't want that to have a side effect on image display size
        // and origin.
        updateImagePlane( imagePlane );
    }

    // Update the Image Plane.
    protected final boolean updateImagePlane( final InputStream inputStream,
                                              final Rectangle2D imagePlane ) {
        // Delegate the image load to the underlying ImagePlot.
        final boolean succeeded = updateImage( inputStream );

        // Reset the Image Plane to the current Region, as it may change later
        // and we don't want that to have a side effect on image display size
        // and origin.
        updateImagePlane( imagePlane );

        return succeeded;
    }

    // Update the Image Plane.
    protected final void updateImagePlane( final Rectangle2D imagePlane ) {
        // Reset the Image Plane to the current Region, as it may change later
        // and we don't want that to have a side effect on image display size
        // and origin.
        _imagePlane = imagePlane;

        // Recalculate the Image Bounds, as they are based on two factors.
        recalculateImageBounds();
    }

    // Reset the Sound Field to a scaled subset, preserving aspect ratio, but
    // blocking zoom past the maximum Region or zoom extents.
    public final void zoom( final double zoomFactor, final boolean useMousePosition ) {
        final double oldX = _plotBoundary.getX();
        final double oldY = _plotBoundary.getY();
        final double oldWidth = _plotBoundary.getWidth();
        final double oldHeight = _plotBoundary.getHeight();

        // First, determine whether any further zooming is possible. If not,
        // then zoom to the current zoom extents (if at the far end of the
        // scale) or do nothing (if at the near end of the scale in either
        // dimension, which is set to 0.5 meters x 0.5 meters), and then exit.
        // NOTE: Make sure to allow for zooming out once at maximum zoom.
        final double newWidth = _zoomCurrent.getWidth() * zoomFactor;
        final double newHeight = _zoomCurrent.getHeight() * zoomFactor;
        if ( ( newWidth > oldWidth ) || ( newHeight > oldHeight ) ) {
            zoomToExtents();
            return;
        }
        if ( ( zoomFactor < 1.0d ) && ( ( newWidth < 0.5d ) || ( newHeight < 0.5d ) ) ) {
            return;
        }

        // NOTE: When the mouse zoom mode is active, the mouse location is
        // relative to the plot origin, and is converted to current zoom units.
        // Otherwise we stay centered on the current zoom extents, as
        // menu-triggered zooming is useless without a consistent context.
        final Point2D defaultPosition = getDefaultPosition( useMousePosition, 0.5d, 0.5d );

        // Clip the zoom range to the Drawing Limits (if no imported geometry).
        // NOTE: We destroy the aspect ratio if we modify the width and/or
        // height at this point in time. Besides, we already checked earlier
        // against the zoom extents, so the worst that can happen here is
        // that we use the correct zoom factor but shift the center of the
        // zoom to keep the entire zoom window inside the zoom extents.
        double newX = defaultPosition.getX() - ( newWidth * 0.5d );
        double newY = defaultPosition.getY() - ( newHeight * 0.5d );
        if ( !_importedGeometryActive ) {
            if ( newX < oldX ) {
                newX = oldX;
            }
            if ( newX > ( ( oldX + oldWidth ) - newWidth ) ) {
                newX = ( oldX + oldWidth ) - newWidth;
            }
            if ( newY < oldY ) {
                newY = oldY;
            }
            if ( newY > ( ( oldY + oldHeight ) - newHeight ) ) {
                newY = ( oldY + oldHeight ) - newHeight;
            }
        }

        // Any incremental zooming requires regenerating the off-view buffer.
        regenerateOffScreenImage = true;

        // Zoom the view to the scaled and clipped new extents.
        setZoomCurrent( new Rectangle2D.Double( newX, newY, newWidth, newHeight ) );
    }

    // Zoom in by a factor of two.
    public final void zoomIn( final boolean useMousePosition ) {
        zoom( 0.5d, useMousePosition || isContextMenuActive() );
    }

    // Zoom out by a factor of two.
    public final void zoomOut( final boolean useMousePosition ) {
        zoom( 2.0d, useMousePosition || isContextMenuActive() );
    }

    // Restore the full zoom extents.
    public final void zoomToExtents() {
        setZoomCurrent( _plotBoundary );
    }

}
