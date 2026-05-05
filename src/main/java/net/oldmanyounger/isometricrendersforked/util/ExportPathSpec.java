package net.oldmanyounger.isometricrendersforked.util;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import net.oldmanyounger.isometricrendersforked.property.GlobalProperties;

import java.io.File;
import java.nio.file.Path;

/**
 * Describes where an exported render should be written.
 *
 * <p>All exports resolve under the Minecraft game directory's {@code renders}
 * folder. The root offset can be ignored when the user chooses to dump exports
 * directly into the root render folder.</p>
 *
 * @param rootOffset path segment under the render root
 * @param filename exported file name without extension
 * @param ignoreSaveIntoRoot whether this path ignores the global root-dump setting
 */
public record ExportPathSpec(String rootOffset, String filename, boolean ignoreSaveIntoRoot) {
    // Creates a normal export path.
    public static ExportPathSpec of(String rootOffset, String filename) {
        return new ExportPathSpec(rootOffset, filename, false);
    }

    // Creates an export path that always keeps its root offset.
    public static ExportPathSpec forced(String rootOffset, String filename) {
        return new ExportPathSpec(rootOffset, filename, true);
    }

    // Creates an export path from a registry id and render type.
    public static ExportPathSpec ofIdentified(ResourceLocation id, String type) {
        return new ExportPathSpec(id.getNamespace() + "/" + type, id.getPath(), false);
    }

    // Resolves the effective output folder.
    public Path resolveOffset() {
        return exportRoot().resolve(this.effectiveOffset());
    }

    // Resolves the next available file for the requested extension.
    public File resolveFile(String extension) {
        return ImageIO.next(exportRoot().resolve(this.effectiveOffset()).resolve(this.filename + "." + extension)).toFile();
    }

    // Returns this spec with a different root offset.
    public ExportPathSpec relocate(String newOffset) {
        return new ExportPathSpec(newOffset, this.filename, this.ignoreSaveIntoRoot);
    }

    // Applies the root-dump setting to the stored offset.
    private String effectiveOffset() {
        return this.rootOffset.isEmpty() || (!this.ignoreSaveIntoRoot && GlobalProperties.saveIntoRoot.get())
                ? "./"
                : this.rootOffset + "/";
    }

    // Returns the root folder for all Isometric Renders exports.
    public static Path exportRoot() {
        return FMLPaths.GAMEDIR.get().resolve("renders");
    }
}
