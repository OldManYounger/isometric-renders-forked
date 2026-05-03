package net.oldmanyounger.isometricrenders.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.network.chat.Component;
import net.oldmanyounger.isometricrenders.IsometricRenders;
import net.oldmanyounger.isometricrenders.property.GlobalProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Asynchronous image export helpers.
 *
 * <p>Render code should hand completed {@link NativeImage} instances to this
 * class so PNG writing can happen off the render thread.</p>
 */
public final class ImageIO {
    private static final AtomicInteger TASK_COUNT = new AtomicInteger(0);

    private ImageIO() {}

    // Saves a native image as a PNG using the supplied export path.
    public static CompletableFuture<File> save(NativeImage image, ExportPathSpec path) {
        var future = new CompletableFuture<File>();

        TASK_COUNT.incrementAndGet();
        ForkJoinPool.commonPool().submit(() -> {
            var imageFile = path.resolveFile("png");

            imageFile.getParentFile().mkdirs();

            try {
                image.writeToFile(imageFile.toPath());
                IsometricRenders.LOGGER.info("Image {} saved", imageFile.getAbsolutePath());
                future.complete(imageFile);
            } catch (IOException exception) {
                IsometricRenders.LOGGER.warn("Could not save image {}", imageFile.getAbsolutePath(), exception);
                future.completeExceptionally(exception);
            } finally {
                image.close();
                TASK_COUNT.decrementAndGet();
            }
        });

        return future;
    }

    // Returns the number of active image-write tasks.
    public static int taskCount() {
        return TASK_COUNT.get();
    }

    // Returns translated exporter status text.
    public static Component progressText() {
        int jobs = taskCount();
        if (jobs == 0) return Translate.gui("exporter.idle");
        return Translate.gui("exporter.jobs", jobs);
    }

    // Chooses a non-conflicting path unless overwriting the latest file is enabled.
    public static Path next(Path input) {
        var filename = input.getFileName().toString();

        var separatorIndex = filename.lastIndexOf('.');
        if (separatorIndex == -1) separatorIndex = filename.length();

        var name = filename.substring(0, separatorIndex);
        var extension = filename.substring(separatorIndex);

        var path = input.getParent();

        var currentPath = path.resolve(join(name, extension, 0));
        var lastPath = currentPath;

        for (int index = 1; Files.exists(currentPath); index++) {
            lastPath = currentPath;
            currentPath = path.resolve(join(name, extension, index));
        }

        return GlobalProperties.overwriteLatest.get() ? lastPath : currentPath;
    }

    // Builds a filename with an optional numeric suffix.
    private static String join(String filename, String extension, int index) {
        return index == 0
                ? filename + extension
                : filename + "_" + index + extension;
    }
}
