package common;

import java.util.Random;

public final class MatrixUtils {

    private MatrixUtils() {
        // utility class
    }

    public static double[][] randomMatrix(int rows, int cols, double min, double max, long seed) {
        double[][] m = new double[rows][cols];
        Random random = new Random(seed);
        double range = max - min;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m[i][j] = min + random.nextDouble() * range;
            }
        }
        return m;
    }

    public static double[][] zeroMatrix(int rows, int cols) {
        return new double[rows][cols];
    }

    public static void printMatrix(double[][] m, int maxRows, int maxCols) {
        int rows = Math.min(maxRows, m.length);
        int cols = m.length > 0 ? Math.min(maxCols, m[0].length) : 0;
        for (int i = 0; i < rows; i++) {
            System.out.print("[ ");
            for (int j = 0; j < cols; j++) {
                System.out.printf("%8.3f ", m[i][j]);
            }
            if (cols < m[0].length) {
                System.out.print("... ");
            }
            System.out.println("]");
        }
        if (rows < m.length) {
            System.out.println("... (" + (m.length - rows) + " more rows)");
        }
    }

    /**
     * Naive single-node matrix multiplication for baseline comparison.
     */
    public static double[][] multiplyLocal(double[][] a, double[][] b) {
        int aRows = a.length;
        int aCols = aRows == 0 ? 0 : a[0].length;
        int bRows = b.length;
        int bCols = bRows == 0 ? 0 : b[0].length;

        if (aCols != bRows) {
            throw new IllegalArgumentException("Incompatible dimensions: A is " +
                    aRows + "x" + aCols + ", B is " + bRows + "x" + bCols);
        }

        double[][] c = new double[aRows][bCols];

        for (int i = 0; i < aRows; i++) {
            for (int k = 0; k < aCols; k++) {
                double a_ik = a[i][k];
                for (int j = 0; j < bCols; j++) {
                    c[i][j] += a_ik * b[k][j];
                }
            }
        }

        return c;
    }

    public static boolean matricesAlmostEqual(double[][] x, double[][] y, double eps) {
        if (x.length != y.length) return false;
        if (x.length == 0) return true;
        if (x[0].length != y[0].length) return false;

        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[0].length; j++) {
                if (Math.abs(x[i][j] - y[i][j]) > eps) {
                    return false;
                }
            }
        }
        return true;
    }
}
