package api;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MatrixMultiplyWorker extends Remote {

    /**
     * Multiply a subset of rows from A with full matrix B.
     *
     * @param aRows subset of rows from A; size = [rowsChunk x n]
     * @param b     full matrix B; size = [n x p]
     * @return resulting subset of rows for C; size = [rowsChunk x p]
     */
    double[][] multiply(double[][] aRows, double[][] b) throws RemoteException;
}
