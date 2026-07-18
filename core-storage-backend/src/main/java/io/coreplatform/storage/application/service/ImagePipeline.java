package io.coreplatform.storage.application.service;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Image Pipeline — 图片处理管线。
 * 每次调用生成一个新实例，配置步骤后执行。不是 Spring Bean。
 *
 * <pre>
 * ImagePipeline.from(sourceFile, "png")
 *     .analyze()
 *     .resize(200, 200)
 *     .convert("webp")
 *     .compress(0.8f)
 *     .execute();
 * </pre>
 *
 * 步骤可自由组合。每个步骤都是独立节点，不修改原图。
 */
public class ImagePipeline {

    private static final Logger log = LoggerFactory.getLogger(ImagePipeline.class);

    private final Path sourceFile;
    private final String sourceFormat;
    private final List<PipelineStep> steps = new ArrayList<>();
    private final int maxDimension;

    // Analysis results
    private int sourceWidth;
    private int sourceHeight;
    private String colorSpace;
    private boolean hasAlpha;
    private int orientation;
    private int dpi;

    private ImagePipeline(Path sourceFile, String sourceFormat, int maxDimension) {
        this.sourceFile = sourceFile;
        this.sourceFormat = sourceFormat != null ? sourceFormat.toLowerCase() : "jpg";
        this.maxDimension = maxDimension;
    }

    /** Factory method with default max dimension. */
    public static ImagePipeline from(Path sourceFile, String format) {
        return new ImagePipeline(sourceFile, format, 10000);
    }

    /** Factory method with explicit max dimension. */
    public static ImagePipeline from(Path sourceFile, String format, int maxDimension) {
        return new ImagePipeline(sourceFile, format, maxDimension);
    }

    /**
     * 分析图片元数据（必须最先调用）。
     */
    public ImagePipeline analyze() throws IOException {
        BufferedImage img = ImageIO.read(sourceFile.toFile());
        if (img == null) {
            throw new IOException("Unable to read image: " + sourceFile);
        }

        this.sourceWidth = img.getWidth();
        this.sourceHeight = img.getHeight();

        // 检查尺寸限制
        if (sourceWidth > maxDimension || sourceHeight > maxDimension) {
            throw new ImageTooLargeException(
                    String.format("Image dimension %dx%d exceeds max %d", sourceWidth, sourceHeight, maxDimension),
                    sourceWidth, sourceHeight, maxDimension);
        }

        this.hasAlpha = img.getTransparency() != Transparency.OPAQUE;
        this.colorSpace = img.getColorModel().getColorSpace().toString();
        this.dpi = 72;

        log.debug("Image analyzed: {}x{}, format={}, alpha={}, colorSpace={}",
                sourceWidth, sourceHeight, sourceFormat, hasAlpha, colorSpace);
        return this;
    }

    /** Add resize step. */
    public ImagePipeline resize(int targetWidth, int targetHeight) {
        steps.add(new ResizeStep(targetWidth, targetHeight));
        return this;
    }

    /** Add compress step (quality 0.0-1.0). */
    public ImagePipeline compress(float quality) {
        steps.add(new CompressStep(quality));
        return this;
    }

    /** Add format conversion step. */
    public ImagePipeline convert(String targetFormat) {
        steps.add(new ConvertStep(targetFormat));
        return this;
    }

    /**
     * Add watermark step. MVP: throws UnsupportedOperationException.
     * Full implementation deferred.
     */
    public ImagePipeline watermark(Path watermarkFile, float opacity) {
        steps.add(new WatermarkStep(watermarkFile, opacity));
        return this;
    }

    /**
     * Execute the pipeline and return results.
     * Each step produces one VariantFile.
     */
    public PipelineResult execute() throws IOException {
        if (steps.isEmpty()) {
            return new PipelineResult(Collections.emptyList());
        }

        BufferedImage current = ImageIO.read(sourceFile.toFile());
        if (current == null) {
            throw new IOException("Unable to read image: " + sourceFile);
        }

        List<VariantFile> results = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("img-pipeline-");

        try {
            for (PipelineStep step : steps) {
                Path output = tempDir.resolve(UUID.randomUUID().toString() + "." + step.outputFormat());
                BufferedImage processed = step.process(current);

                // 处理后的宽高
                int outWidth = processed.getWidth();
                int outHeight = processed.getHeight();

                writeImage(processed, step.outputFormat(), output, step.compressionQuality());
                results.add(new VariantFile(output, step.variantName(), step.outputFormat(),
                        outWidth, outHeight));
                current = processed;
            }
        } catch (Exception e) {
            // 清理已生成的临时文件
            for (VariantFile vf : results) {
                try { Files.deleteIfExists(vf.path()); } catch (IOException ignored) { }
            }
            throw e;
        }

        return new PipelineResult(results);
    }

    // ---- analysis getters ----

    public int getSourceWidth() { return sourceWidth; }
    public int getSourceHeight() { return sourceHeight; }
    public String getColorSpace() { return colorSpace; }
    public boolean isHasAlpha() { return hasAlpha; }
    public int getOrientation() { return orientation; }
    public int getDpi() { return dpi; }

    // ---- private helpers ----

    private void writeImage(BufferedImage image, String format, Path output, Float quality) throws IOException {
        // 使用 Thumbnailator 进行格式转换和质量压缩
        Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(image).scale(1.0);

        if (quality != null) {
            builder.outputQuality(quality);
        }

        builder.outputFormat(format).toFile(output.toFile());
    }

    // ---- inner types ----

    /** A single processing step in the pipeline. */
    interface PipelineStep {
        BufferedImage process(BufferedImage input) throws IOException;
        String outputFormat();
        String variantName();
        /** 压缩质量 0.0-1.0，null 表示不压缩 */
        default Float compressionQuality() { return null; }
    }

    record ResizeStep(int width, int height) implements PipelineStep {
        @Override
        public BufferedImage process(BufferedImage input) throws IOException {
            return Thumbnails.of(input).size(width, height).asBufferedImage();
        }
        @Override
        public String outputFormat() { return "jpg"; }
        @Override
        public String variantName() { return "THUMBNAIL"; }
    }

    record CompressStep(float quality) implements PipelineStep {
        @Override
        public BufferedImage process(BufferedImage input) {
            return input;
        }
        @Override
        public String outputFormat() { return "jpg"; }
        @Override
        public String variantName() { return "COMPRESSED"; }
        @Override
        public Float compressionQuality() { return Math.max(0.0f, Math.min(1.0f, quality)); }
    }

    record ConvertStep(String format) implements PipelineStep {
        @Override
        public BufferedImage process(BufferedImage input) {
            return input;
        }
        @Override
        public String outputFormat() { return format.toLowerCase(); }
        @Override
        public String variantName() { return format.toUpperCase(); }
    }

    record WatermarkStep(Path watermarkFile, float opacity) implements PipelineStep {
        @Override
        public BufferedImage process(BufferedImage input) {
            throw new UnsupportedOperationException(
                    "Watermark is not yet implemented. This is a stub for future implementation.");
        }
        @Override
        public String outputFormat() { return "png"; }
        @Override
        public String variantName() { return "WATERMARKED"; }
    }

    /** Pipeline execution result. */
    public record PipelineResult(List<VariantFile> files) {
        public boolean isEmpty() { return files.isEmpty(); }
    }

    /** A single variant output file. */
    public record VariantFile(Path path, String variantName, String format,
                              int width, int height) {
    }

    /** Image too large exception. */
    public static class ImageTooLargeException extends IOException {
        private final int width;
        private final int height;
        private final int maxDimension;

        public ImageTooLargeException(String message, int width, int height, int maxDimension) {
            super(message);
            this.width = width;
            this.height = height;
            this.maxDimension = maxDimension;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getMaxDimension() { return maxDimension; }
    }
}