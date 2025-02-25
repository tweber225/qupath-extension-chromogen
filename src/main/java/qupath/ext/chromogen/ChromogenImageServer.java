package qupath.ext.chromogen;

import qupath.lib.images.servers.TransformingImageServer;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;
import qupath.lib.regions.RegionRequest;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Example TransformingImageServer that applies a simple 'sublinear' transform
 * to each pixel, demonstrating how to override readRegion.
 */
public class ChromogenImageServer extends TransformingImageServer<BufferedImage> {

    // For demonstration, we'll do a trivial 'sublinear' approach
    // that just uses Math.sqrt() on each color channel (i.e. sublinear in the sense
    // that output grows slower than input). 
    // In real code, you'd adapt this to your own transform function.

    public ChromogenImageServer(ImageServer<BufferedImage> server) {
        super(server);
        // Typically you'd validate whether the server is 8-bit or 16-bit, 
        // check the number of channels, etc., in case you need special handling.
    }

    // @Override
    // public BufferedImage readRegion(final RegionRequest request) throws IOException {
    //     // Call the super method to get the tile from the wrapped server
    //     BufferedImage img = super.readRegion(request);
    //     if (img == null) {
    //         return null;
    //     }

    //     // Apply a simple transform to each pixel channel
    //     final int width = img.getWidth();
    //     final int height = img.getHeight();
    //     for (int y = 0; y < height; y++) {
    //         for (int x = 0; x < width; x++) {
    //             int argb = img.getRGB(x, y);

    //             // Extract channels
    //             int alpha = (argb >> 24) & 0xFF;
    //             int red   = (argb >> 16) & 0xFF;
    //             int green = (argb >>  8) & 0xFF;
    //             int blue  = (argb      ) & 0xFF;

    //             // For demonstration, do sqrt on each channel 
    //             // (Clamp result to max 255 for 8-bit)
    //             red   = (int)Math.min(255, Math.sqrt(red)   * 16); 
    //             green = (int)Math.min(255, Math.sqrt(green) * 16);
    //             blue  = (int)Math.min(255, Math.sqrt(blue)  * 16);

    //             // Reassemble ARGB
    //             int newArgb = (alpha << 24) | (red << 16) | (green << 8) | blue;
    //             img.setRGB(x, y, newArgb);
    //         }
    //     }

    //     return img;
    // }
    @Override
    public BufferedImage readRegion(RegionRequest request) throws IOException {
        BufferedImage img = super.readRegion(request);
        if (img == null)
            return null;

        var raster = img.getRaster();      // The pixel data
        var width = img.getWidth();
        var height = img.getHeight();
        int nBands = raster.getNumBands(); // e.g. 4 channels => nBands=4

        // Example: sublinear transform channel by channel
        // If 16-bit, be sure to handle values up to 65535
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int b = 0; b < nBands; b++) {
                    int val = raster.getSample(x, y, b); // raw intensity
                    // apply your transform, e.g. sqrt or log
                    int newVal = (int)Math.min(65535, Math.sqrt(val) * 256);
                    raster.setSample(x, y, b, newVal);
                }
            }
        }

        return img;
    }

    @Override
    public String getServerType() {
        // Descriptive name to show in QuPath
        return getWrappedServer().getServerType() + " [sublinear transform]";
    }

    @Override
    protected String createID() {
        // A unique ID for the server
        return getWrappedServer().getPath() + " [sublinear transform]";
    }

    /**
	 * Returns null (does not support ServerBuilders).
	 */
    @Override
    protected ServerBuilder<BufferedImage> createServerBuilder() {
        return null;
    }


}

