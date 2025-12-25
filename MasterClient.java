// src/main/java/master/MasterClient.java
package master;

import api.MatrixMultiplyWorker;
import common.MatrixUtils;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MasterClient {

    public static void main(String[] args) throws Exception {
        int n = 2000;     // rows in A, rows in B
        int m = 2000;     // columns in A
        int p = 2000;     // columns in B

        if (args.length == 3) {
            n = Integer.parseInt(args[0]);
            m = Integer.parseInt(args[1]);
            p = Integer.parseInt(args[2]);
        }

        System.out.println("Matrix multiplication C = A(" + n + "x" + m + ") * B(" + m + "x" + p + ")");
        System.out.println("Using " + WorkerConfig.WORKER_URLS.length + " worker(s).");

        // Generate random matrices
        double[][] A = MatrixUtils.randomMatrix(n, m, -1.0, 1.0, 42L);
        double[][] B = MatrixUtils.randomMatrix(m, p, -1.0, 1.0, 1337L);

        // Look up workers
        List<MatrixMultiplyWorker> workers = lookupWorkers();

        // Distributed multiplication
        long startDistributed = System.currentTimeMillis();
        double[][] CDistributed = multiplyDistributedRowWise(A, B, workers);
        long endDistributed = System.currentTimeMillis();
        long timeDistributed = endDistributed - startDistributed;

        System.out.println("Distributed multiplication completed in " + timeDistributed + " ms");

        // Local baseline (optional, for smaller sizes; beware of too-big n)
        long startLocal = System.currentTimeMillis();
        double[][] CLocal = MatrixUtils.multiplyLocal(A, B);
        long endLocal = System.currentTimeMillis();
        long timeLocal = endLocal - startLocal;

        System.out.println("Local single-node multiplication completed in " + timeLocal + " ms");

        // Compare
        boolean same = MatrixUtils.matricesAlmostEqual(CDistributed, CLocal, 1e-6);
        System.out.println("Results match: " + same);

        // Print small portion if small matrices
        if (n <= 10 && p <= 10) {
            System.out.println("\nMatrix C (distributed):");
            MatrixUtils.printMatrix(CDistributed, n, p);
        }
    }

    private static List<MatrixMultiplyWorker> lookupWorkers() throws Exception {
        List<MatrixMultiplyWorker> workers = new ArrayList<>();

        for (String url : WorkerConfig.WORKER_URLS) {
            try {
                Object obj = Naming.lookup(url);
                MatrixMultiplyWorker worker = (MatrixMultiplyWorker) obj;
                System.out.println("Connected to worker at " + url);
                workers.add(worker);
            } catch (NotBoundException e) {
                System.err.println("No worker bound at " + url + " (NotBoundException).");
            } catch (RemoteException e) {
                System.err.println("Failed to connect to worker at " + url + ": " + e.getMessage());
            }
        }

        if (workers.isEmpty()) {
            throw new IllegalStateException("No workers available. Start at least one WorkerServer.");
        }

        return workers;
    }

    /**
     * Perform distributed matrix multiplication using 1D row-wise partition over the available workers.
     *
     * @param A       matrix A (n x m)
     * @param B       matrix B (m x p)
     * @param workers list of worker stubs
     * @return result C (n x p)
     */
    private static double[][] multiplyDistributedRowWise(double[][] A, double[][] B,
                                                         List<MatrixMultiplyWorker> workers)
            throws InterruptedException, ExecutionException {

        int n = A.length;
        int m = n == 0 ? 0 : A[0].length;
        int bRows = B.length;
        int p = bRows == 0 ? 0 : B[0].length;

        if (m != bRows) {
            throw new IllegalArgumentException("Incompatible dimensions: A is " + n + "x" + m +
                    ", B is " + bRows + "x" + p);
        }

        double[][] C = new double[n][p];

        int numWorkers = workers.size();
        int rowsPerWorker = (n + numWorkers - 1) / numWorkers; // ceil(n / numWorkers)

        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
        List<Future<ChunkResult>> futures = new ArrayList<>();

        for (int workerIndex = 0; workerIndex < numWorkers; workerIndex++) {
            final int wIdx = workerIndex;
            final MatrixMultiplyWorker worker = workers.get(workerIndex);

            final int startRow = wIdx * rowsPerWorker;
            final int endRowExclusive = Math.min(startRow + rowsPerWorker, n);

            if (startRow >= endRowExclusive) {
                continue; // more workers than needed rows
            }

            double[][] aChunk = sliceRows(A, startRow, endRowExclusive);

            Callable<ChunkResult> task = () -> {
                try {
                    double[][] cChunk = worker.multiply(aChunk, B);
                    return new ChunkResult(startRow, endRowExclusive, cChunk);
                } catch (RemoteException e) {
                    throw new RuntimeException("RemoteException from worker " + wIdx, e);
                }
            };

            futures.add(executor.submit(task));
        }

        for (Future<ChunkResult> future : futures) {
            ChunkResult result = future.get();
            copyChunkToC(C, result);
        }

        executor.shutdown();

        return C;
    }

    private static double[][] sliceRows(double[][] matrix, int startRow, int endRowExclusive) {
        int totalRows = matrix.length;
        if (startRow < 0 || endRowExclusive > totalRows || startRow > endRowExclusive) {
            throw new IllegalArgumentException("Invalid row range: " + startRow + " to " + endRowExclusive);
        }

        int numRows = endRowExclusive - startRow;
        if (numRows == 0) {
            return new double[0][];
        }

        int cols = matrix[0].length;
        double[][] chunk = new double[numRows][cols];

        for (int i = 0; i < numRows; i++) {
            System.arraycopy(matrix[startRow + i], 0, chunk[i], 0, cols);
        }

        return chunk;
    }

    private static void copyChunkToC(double[][] C, ChunkResult result) {
        int startRow = result.startRow;
        int endRowExclusive = result.endRowExclusive;
        double[][] cChunk = result.cChunk;

        int numRows = endRowExclusive - startRow;
        int cols = cChunk.length == 0 ? 0 : cChunk[0].length;

        if (numRows != cChunk.length) {
            throw new IllegalStateException("Chunk row count mismatch: expected " + numRows +
                    ", got " + cChunk.length);
        }

        for (int i = 0; i < numRows; i++) {
            System.arraycopy(cChunk[i], 0, C[startRow + i], 0, cols);
        }
    }

    private static class ChunkResult {
        final int startRow;
        final int endRowExclusive;
        final double[][] cChunk;

        ChunkResult(int startRow, int endRowExclusive, double[][] cChunk) {
            this.startRow = startRow;
            this.endRowExclusive = endRowExclusive;
            this.cChunk = cChunk;
        }
    }
}
