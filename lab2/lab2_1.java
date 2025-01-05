package labki2;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class lab2_1 {

    public static class MandelbrotTask implements Runnable {
        private final BufferedImage image;
        private final int startX, endX, width, height;
        private final double minX, minY, dx, dy;
        private final int maxIterations;

        public MandelbrotTask(BufferedImage image, int startX, int endX, int width, int height,
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
        public void run() {
            for (int x = startX; x < endX; x++) {
                for (int y = 0; y < height; y++) {
                    double cx = minX + x * dx;
                    double cy = minY + y * dy;

                    int iterations = computeMandelbrot(cx, cy, maxIterations);
                    int color = mapColor(iterations, maxIterations);
                    image.setRGB(x, y, color);
                }
            }
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

    public static BufferedImage generateMandelbrotImage(int width, int height, double minX, double minY, double maxX, double maxY, int maxIterations) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int cores = Runtime.getRuntime().availableProcessors();
        List<Thread> threads = new ArrayList<>();

        double dx = (maxX - minX) / width;
        double dy = (maxY - minY) / height;
        int blockSize = width / cores;

        for (int i = 0; i < cores; i++) {
            int startX = i * blockSize;
            int endX = (i == cores - 1) ? width : startX + blockSize;

            Thread thread = new Thread(new MandelbrotTask(image, startX, endX, width, height, minX, minY, dx, dy, maxIterations));
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return image;
    }

    public static double benchmarkGeneration(int width, int height, double minX, double minY, double maxX, double maxY, int maxIterations, int repetitions) {
        long totalTime = 0;

        for (int i = 0; i < repetitions; i++) {
            long startTime = System.nanoTime();
            generateMandelbrotImage(width, height, minX, minY, maxX, maxY, maxIterations);
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        return totalTime / (repetitions * 1e6);
    }

    public static void main(String[] args) throws IOException {
        int[] sizes = {8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
        double minX = -2.1, minY = -1.2, maxX = 0.6, maxY = 1.2;
        int maxIterations = 200;
        int repetitions = 5;

        try (PrintWriter writer = new PrintWriter(new File("time_results_multithreaded.txt"))) {
            for (int size : sizes) {
                System.out.printf("Benchmarking %dx%d...%n", size, size);
                double avgTime = benchmarkGeneration(size, size, minX, minY, maxX, maxY, maxIterations, repetitions);
                System.out.printf("Average time for %dx%d: %.2f ms%n", size, size, avgTime);

                writer.printf("%d %.2f%n", size, avgTime);

                BufferedImage image = generateMandelbrotImage(size, size, minX, minY, maxX, maxY, maxIterations);
                ImageIO.write(image, "png", new File("mandelbrot_multithreaded_" + size + "x" + size + ".png"));
            }
        }
        
        System.out.printf("%nDone!%n");
    }
}
