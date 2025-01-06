package lab3;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class lab3 {

    public static <T extends Comparable<T>> void parallelQuickSort(ArrayList<T> list) {
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(new QuickSortTask<>(list, 0, list.size() - 1));
    }

    static class QuickSortTask<T extends Comparable<T>> extends RecursiveAction {

        private final ArrayList<T> list;
        private final int start;
        private final int end;

        public QuickSortTask(ArrayList<T> list, int start, int end) {
            this.list = list;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (start >= end) {
                return;
            }

            T pivot = list.get(end);
            int partitionIndex = partition(list, start, end, pivot);

            QuickSortTask<T> leftTask = new QuickSortTask<>(list, start, partitionIndex - 1);
            QuickSortTask<T> rightTask = new QuickSortTask<>(list, partitionIndex + 1, end);

            leftTask.fork();
            rightTask.compute();
            leftTask.join();
        }

        private int partition(ArrayList<T> list, int start, int end, T pivot) {
            int i = start - 1;
            for (int j = start; j < end; j++) {
                if (list.get(j).compareTo(pivot) <= 0) {
                    i++;
                    swap(list, i, j);
                }
            }
            swap(list, i + 1, end);
            return i + 1;
        }

        private void swap(ArrayList<T> list, int i, int j) {
            T temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    public static <T extends Comparable<T>> void sequentialQuickSort(ArrayList<T> list, int start, int end) {
        if (start >= end) {
            return;
        }
        T pivot = list.get(end);
        int partitionIndex = partition(list, start, end, pivot);
        sequentialQuickSort(list, start, partitionIndex - 1);
        sequentialQuickSort(list, partitionIndex + 1, end);
    }

    private static <T extends Comparable<T>> int partition(ArrayList<T> list, int start, int end, T pivot) {
        int i = start - 1;
        for (int j = start; j < end; j++) {
            if (list.get(j).compareTo(pivot) <= 0) {
                i++;
                swap(list, i, j);
            }
        }
        swap(list, i + 1, end);
        return i + 1;
    }

    private static <T> void swap(ArrayList<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    public static void main(String[] args) throws IOException {
        Random random = new Random();
        ArrayList<Integer> list = new ArrayList<>();
        int[] sizes = {100, 1000, 10000, 100000, 1000000, 5000000, 10000000, 20000000};

        try (FileWriter writer = new FileWriter("performance.txt")) {
            for (int size : sizes) {
                writer.write(size + "  ");

                list.clear();
                for (int i = 0; i < size; i++) {
                    list.add(random.nextInt());
                }

                long startTime = System.currentTimeMillis();
                sequentialQuickSort(list, 0, list.size() - 1);
                long endTime = System.currentTimeMillis();
                writer.write((endTime - startTime) + "  ");
                System.out.println("Sequential - " + size + "; time: " + (endTime - startTime) + " ms");

                list.clear();
                for (int i = 0; i < size; i++) {
                    list.add(random.nextInt());
                }

                startTime = System.currentTimeMillis();
                parallelQuickSort(list);
                endTime = System.currentTimeMillis();
                writer.write((endTime - startTime) + "\n");
                System.out.println("Parallel - " + size + "; time: " + (endTime - startTime) + " ms");
            }
        }
    }
}
