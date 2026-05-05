package net.oldmanyounger.isometricrendersforked.util;

import net.oldmanyounger.isometricrendersforked.IsometricRendersForked;
import net.oldmanyounger.isometricrendersforked.property.GlobalProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * Detects and dispatches FFmpeg for animation assembly.
 *
 * <p>Image sequence rendering will be ported later. This utility only restores
 * the external process handling used after a sequence has already been written.</p>
 */
public final class FFmpegDispatcher {
    private static Boolean ffmpegDetected = null;

    private FFmpegDispatcher() {}

    // Returns whether FFmpeg detection has completed at least once.
    public static boolean wasFFmpegDetected() {
        return ffmpegDetected != null;
    }

    // Returns whether FFmpeg was detected successfully.
    public static boolean ffmpegAvailable() {
        return ffmpegDetected != null && ffmpegDetected;
    }

    // Detects whether the ffmpeg executable is available on the system path.
    public static CompletableFuture<Boolean> detectFFmpeg() {
        if (ffmpegDetected != null) {
            return CompletableFuture.completedFuture(ffmpegDetected);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                var process = new ProcessBuilder("ffmpeg", "-version")
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();

                process.onExit().join();
                var output = new String(process.getInputStream().readAllBytes());

                IsometricRendersForked.LOGGER.info("FFmpeg detected, version: {}", output.split(" ")[2]);
                return true;
            } catch (IOException exception) {
                IsometricRendersForked.LOGGER.info("Did not detect FFmpeg for reason: {}", exception.getMessage());
                return false;
            }
        }, ForkJoinPool.commonPool()).whenComplete((result, throwable) -> {
            if (throwable != null) {
                ffmpegDetected = false;
                IsometricRendersForked.LOGGER.warn("Could not complete FFmpeg detection", throwable);
            } else {
                ffmpegDetected = result;
            }
        });
    }

    // Assembles an exported image sequence into the requested animation format.
    public static CompletableFuture<File> assemble(ExportPathSpec target, Path sourcePath, AnimationFormat format) {
        target.resolveOffset().toFile().mkdirs();

        var defaultArgs = new ArrayList<>(List.of(
                "ffmpeg",
                "-y",
                "-f", "image2",
                "-framerate", String.valueOf(GlobalProperties.exportFramerate),
                "-i", "seq_%d.png"
        ));

        if (format.arguments.length != 0) {
            defaultArgs.addAll(Arrays.asList(format.arguments));
        }

        var animationFile = target.resolveFile(format.extension);
        defaultArgs.add(animationFile.getAbsolutePath());

        var process = new ProcessBuilder(defaultArgs)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .directory(sourcePath.toFile());

        try {
            return process.start().onExit().thenApply(exited -> {
                cleanSequenceDirectory(sourcePath);
                return animationFile;
            });
        } catch (IOException exception) {
            IsometricRendersForked.LOGGER.error("Could not launch FFmpeg", exception);
            return CompletableFuture.failedFuture(exception);
        }
    }

    // Deletes temporary sequence frames after FFmpeg exits.
    private static void cleanSequenceDirectory(Path sourcePath) {
        try (var files = Files.list(sourcePath)) {
            files.filter(path -> path.getFileName().toString().matches("seq_\\d+\\.png"))
                    .forEach(deletePath -> {
                        try {
                            Files.delete(deletePath);
                        } catch (IOException exception) {
                            IsometricRendersForked.LOGGER.warn("Could not delete sequence frame {}", deletePath, exception);
                        }
                    });
        } catch (IOException exception) {
            IsometricRendersForked.LOGGER.warn("Could not clean up sequence directory", exception);
        }
    }
}
