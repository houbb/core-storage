package io.coreplatform.storage;

import io.coreplatform.storage.application.service.ImagePipeline;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class ImagePipelineTest {

    @TempDir
    Path tempDir;

    private Path createTestImage(int width, int height) throws IOException {
        Path file = tempDir.resolve("test-" + width + "x" + height + ".png");
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(img, "png", file.toFile());
        return file;
    }

    @Test
    void analyzeReadsCorrectDimensions() throws IOException {
        Path source = createTestImage(640, 480);
        ImagePipeline pipeline = ImagePipeline.from(source, "png");
        pipeline.analyze();

        assertEquals(640, pipeline.getSourceWidth());
        assertEquals(480, pipeline.getSourceHeight());
    }

    @Test
    void analyzeDetectsAlpha() throws IOException {
        Path source = createTestImage(100, 100);
        // TYPE_INT_ARGB has alpha
        ImagePipeline pipeline = ImagePipeline.from(source, "png");
        pipeline.analyze();

        assertTrue(pipeline.isHasAlpha());
    }

    @Test
    void analyzeRejectsOversizedImage() throws IOException {
        // Create image larger than max (10000 default)
        // Use a small max via factory method
        Path source = createTestImage(200, 200);
        ImagePipeline pipeline = ImagePipeline.from(source, "png", 100);
        assertThrows(ImagePipeline.ImageTooLargeException.class, pipeline::analyze);
    }

    @Test
    void resizeProducesCorrectSize() throws IOException {
        Path source = createTestImage(640, 480);
        ImagePipeline pipeline = ImagePipeline.from(source, "png");
        pipeline.analyze();
        pipeline.resize(200, 200);

        ImagePipeline.PipelineResult result = pipeline.execute();
        assertFalse(result.isEmpty());
        assertEquals(1, result.files().size());

        ImagePipeline.VariantFile vf = result.files().get(0);
        // Thumbnailator size() preserves aspect ratio by default, fitting within bounds
        // 640x480 → 200x150 (maintaining 4:3 ratio within 200x200)
        assertEquals(200, vf.width());
        assertEquals(150, vf.height());
        assertEquals("jpg", vf.format());
        assertTrue(Files.exists(vf.path()));

        // Cleanup
        Files.deleteIfExists(vf.path());
    }

    @Test
    void convertToWebpProducesWebpFile() throws IOException {
        Path source = createTestImage(100, 100);
        ImagePipeline pipeline = ImagePipeline.from(source, "png");
        pipeline.analyze();
        pipeline.convert("webp");

        ImagePipeline.PipelineResult result = pipeline.execute();
        assertFalse(result.isEmpty());

        ImagePipeline.VariantFile vf = result.files().get(0);
        assertEquals("webp", vf.format());
        assertTrue(Files.exists(vf.path()));

        Files.deleteIfExists(vf.path());
    }

    @Test
    void pipelineChainResizeThenConvert() throws IOException {
        Path source = createTestImage(640, 480);
        ImagePipeline pipeline = ImagePipeline.from(source, "png");
        pipeline.analyze();
        pipeline.resize(300, 200);
        pipeline.convert("webp");

        ImagePipeline.PipelineResult result = pipeline.execute();
        // Each step produces one variant
        assertEquals(2, result.files().size());
        assertEquals("jpg", result.files().get(0).format());
        assertEquals("webp", result.files().get(1).format());
        // 640x480 resized to fit 300x200 → 267x200 (maintaining 4:3 ratio)
        assertEquals(267, result.files().get(0).width());

        // Cleanup
        for (var vf : result.files()) {
            Files.deleteIfExists(vf.path());
        }
    }

    @Test
    void pipelineWithNoStepsReturnsEmpty() throws IOException {
        Path source = createTestImage(100, 100);
        ImagePipeline pipeline = ImagePipeline.from(source, "png");
        // no analyze, no steps
        ImagePipeline.PipelineResult result = pipeline.execute();
        assertTrue(result.isEmpty());
    }

    @Test
    void watermarkThrowsUnsupportedOperation() throws IOException {
        Path source = createTestImage(100, 100);
        Path watermark = createTestImage(50, 50);
        ImagePipeline pipeline = ImagePipeline.from(source, "png");
        pipeline.watermark(watermark, 0.5f);

        assertThrows(UnsupportedOperationException.class, pipeline::execute);
    }

    @Test
    void aspectRatios() throws IOException {
        Path source = createTestImage(640, 480);
        ImagePipeline pipeline = ImagePipeline.from(source, "png");
        pipeline.analyze();

        // 640/480 = 1.333...
        assertTrue(pipeline.getSourceWidth() > pipeline.getSourceHeight());
    }
}
