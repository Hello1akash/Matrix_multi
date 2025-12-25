// src/main/java/worker/WorkerServer.java
package worker;

import api.MatrixMultiplyWorker;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;

public class WorkerServer {

    private static final int DEFAULT_RMI_PORT = 1099;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java worker.WorkerServer <workerName> [port]");
            System.exit(1);
        }

        String workerName = args[0];
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : DEFAULT_RMI_PORT;

        try {
            // Try to start RMI registry on this port (first worker will succeed).
            try {
                LocateRegistry.createRegistry(port);
                System.out.println("RMI registry created on port " + port);
            } catch (ExportException e) {
                // Registry already exists – that's fine if other worker or master started it.
                System.out.println("RMI registry already running on port " + port);
            }

            MatrixMultiplyWorker worker = new MatrixMultiplyWorkerImpl();

            String url = String.format("rmi://localhost:%d/%s", port, workerName);
            Naming.rebind(url, worker);

            System.out.println("Worker '" + workerName + "' bound at " + url);
            System.out.println("Worker is ready. Press Ctrl+C to exit.");

        } catch (RemoteException e) {
            System.err.println("RemoteException while starting worker: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Error while starting worker: " + e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }
}
