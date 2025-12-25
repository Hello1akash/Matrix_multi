package worker;

import api.MatrixMultiplyWorker;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MatrixMultiplyWorkerImpl extends UnicastRemoteObject implements MatrixMultiplyWorker {

    protected MatrixMultiplyWorkerImpl() throws RemoteException {
        super();
    }

    @Override
    public double[][] multiply(double[][] aRows, double[][] b) throws RemoteException {
        if (aRows == null || b == null) {
            throw new IllegalArgumentException("Input matrices must not be null");
        }

        int aRowsCount = aRows.length;
        int aCols = aRowsCount == 0 ? 0 : aRows[0].length;
        int bRows = b.length;
        int bCols = bRows == 0 ? 0 : b[0].length;

        if (aCols != bRows) {
            throw new IllegalArgumentException("Incompatible dimensions: A_rows has " +
                    aCols + " columns, B has " + bRows + " rows");
        }

        double[][] cChunk = new double[aRowsCount][bCols];

        // Standard O(n^3) triple-loop, but restricted to a subset of rows.
        for (int i = 0; i < aRowsCount; i++) {
            for (int k = 0; k < aCols; k++) {
                double a_ik = aRows[i][k];
                for (int j = 0; j < bCols; j++) {
                    cChunk[i][j] += a_ik * b[k][j];
                }
            }
        }

        return cChunk;
    }
}
