package net.oldmanyounger.isometricrenders.util;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * Clipboard wrapper for exported images.
 *
 * <p>This allows render exports to be copied directly into the system clipboard
 * once clipboard export is restored.</p>
 */
public class ImageTransferable implements Transferable, ClipboardOwner {
    private final Image image;

    /**
     * Creates a transferable clipboard image.
     *
     * @param image the image to place on the clipboard
     */
    public ImageTransferable(Image image) {
        this.image = image;
    }

    // Returns the supported transfer flavors.
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    // Checks whether the requested flavor is supported.
    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.imageFlavor);
    }

    // Returns the wrapped image for supported image transfer requests.
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!this.isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }

        return this.image;
    }

    // No cleanup is required when clipboard ownership is lost.
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {}
}
