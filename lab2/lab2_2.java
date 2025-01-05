package labki2;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

public class lab2_2 {

    public static class MandelbrotJob implements Callable<Void> {
        private final BufferedImage image;
        private final int startX, endX, width, height;
        private final double minX, minY, dx, dy;
        private final int maxIterations;

        public MandelbrotJob(BufferedImage image, int startX, int endX, int width, int height,
                             double minX, double minY, double dx, double dy, int maxIterations) {
            this.image = image;
            this.startX = startX;
            this.endX = endX;
            this.width = width;
            this.height = height;
            this.minX = minX;
            this.minY = minY;
            this.dx = dx;
            this.dy = dy;
            this.maxIterations = maxIterations;
        }

        @Override
        public Void call() {
            for (int x = startX; x < endX; x++) {
                for (int y = 0; y < height; y++) {
                    double cx = minX + x * dx;
                    double cy = minY + y * dy;

                    int iterations = computeMandelbrot(cx, cy, maxIterations);
                    int color = mapColor(iterations, maxIterations);
                    image.setRGB(x, y, color);
                }
            }
            return null;
        }

        private int computeMandelbrot(double cx, double cy, int maxIterations) {
            double zx = 0, zy = 0;
            int iteration = 0;

            while (zx * zx + zy * zy < 4 && iteration < maxIterations) {
                double temp = zx * zx - zy * zy + cx;
                zy = 2 * zx * zy + cy;
                zx = temp;
                iteration++;
            }

            return iteration;
        }

        private int mapColor(int iterations, int maxIterations) {
            if (iterations == maxIterations) {
                return Color.BLACK.getRGB();
            }
            int shade = (int)(255.0 * iterations / maxIterations);
            return new Color(255 - shade, 255 - shade, 255 - shade).getRGB();
        }
    }

    public static BufferedImage generateMandelbrotImage(int width, int height, double minX, double minY, double maxX, double maxY, int maxIterations, ExecutorService threadPool, int blockSize) throws InterruptedException, ExecutionException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        List<Future<Void>> futures = new ArrayList<>();

        double dx = (maxX - minX) / width;
        double dy = (maxY - minY) / height;

        for (int x = 0; x < width; x += blockSize) {
            int startX = x;
            int endX = Math.min(x + blockSize, width);

            MandelbrotJob job = new MandelbrotJob(image, startX, endX, width, height, minX, minY, dx, dy, maxIterations);
            futures.add(threadPool.submit(job));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        return image;
    }

    public static double benchmarkGeneration(int width, int height, double minX, double minY, double maxX, double maxY, int maxIterations, int repetitions, int blockSize, boolean reuseThreadPool) throws InterruptedException, ExecutionException {
        long totalTime = 0;
        ExecutorService threadPool = null;

        if (reuseThreadPool) {
            threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }

        for (int i = 0; i < repetitions; i++) {
            if (!reuseThreadPool) {
                threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            }

            long startTime = System.nanoTime();
            generateMandelbrotImage(width, height, minX, minY, maxX, maxY, maxIterations, threadPool, blockSize);
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);

            if (!reuseThreadPool) {
                threadPool.shutdown();
            }
        }

        if (reuseThreadPool) {
            threadPool.shutdown();
        }

        return totalTime / (repetitions * 1e6);
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        int[] sizes = {16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
        int[] blockSizes = {4, 8, 16, 32, 64, 128};
        double minX = -2.1, minY = -1.2, maxX = 0.6, maxY = 1.2;
        int maxIterations = 200;
        int repetitions = 5;

        try (PrintWriter writer = new PrintWriter(new File("time_results_threadpool.txt"))) {
            for (int blockSize : blockSizes) {
                //writer.printf("Block size: %d\n", blockSize);
                System.out.printf("Block size: %d\n", blockSize);

                for (int size : sizes) {
                    System.out.printf("Benchmarking %dx%d...%n", size, size);
                    double avgTime = benchmarkGeneration(size, size, minX, minY, maxX, maxY, maxIterations, repetitions, blockSize, false);
                    System.out.printf("Average time for %dx%d: %.2f ms%n", size, size, avgTime);

                    writer.printf("%d   %d  %.2f%n", blockSize, size, avgTime);

                    BufferedImage image = generateMandelbrotImage(size, size, minX, minY, maxX, maxY, maxIterations, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), blockSize);
                    ImageIO.write(image, "png", new File("mandelbrot_threadpool_" + size + "x" + size + "_block" + blockSize + ".png"));
                }
            }
        }
        System.out.printf("%nDone!%n");
    }
}
