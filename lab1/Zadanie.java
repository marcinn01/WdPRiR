package lab1;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

public class Zadanie {

    public static BufferedImage generateMandelbrotImage(int width, int height, double minX, double minY, double maxX, double maxY, int maxIterations) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        double dx = (maxX - minX) / width;
        double dy = (maxY - minY) / height;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double cx = minX + x * dx;
                double cy = minY + y * dy;

                int iterations = computeMandelbrot(cx, cy, maxIterations);
                int color = mapColor(iterations, maxIterations);
                image.setRGB(x, y, color);
            }
        }
        return image;
    }

    private static int computeMandelbrot(double cx, double cy, int maxIterations) {
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

    private static int mapColor(int iterations, int maxIterations) {
        if (iterations == maxIterations) {
            return Color.BLACK.getRGB();
        }
        int shade = (int)(255.0 * iterations / maxIterations);
        return new Color(255 - shade, 255 - shade, 255 - shade).getRGB();
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
        int[] sizes = {16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};
        double minX = -2.1, minY = -1.2, maxX = 0.6, maxY = 1.2;
        int maxIterations = 200;
        int repetitions = 5;

        try (PrintWriter writer = new PrintWriter(new File("time_results.txt"))) {
            for (int size : sizes) {
                System.out.printf("Benchmarking %dx%d...%n", size, size);
                double avgTime = benchmarkGeneration(size, size, minX, minY, maxX, maxY, maxIterations, repetitions);
                System.out.printf("Average time for %dx%d: %.2f ms%n", size, size, avgTime);

                writer.printf("%dx%d: %.2f ms%n", size, size, avgTime);

                BufferedImage image = generateMandelbrotImage(size, size, minX, minY, maxX, maxY, maxIterations);
                ImageIO.write(image, "png", new File("mandelbrot_" + size + "x" + size + ".png"));
            }
        } 

    }

}
