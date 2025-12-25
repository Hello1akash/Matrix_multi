// src/main/java/master/WorkerConfig.java
package master;

public final class WorkerConfig {

    private WorkerConfig() {}

    /**
     * Static list of worker RMI URLs.
     *
     * For multiple physical machines, change "localhost" to actual hostnames/IPs.
     */
    public static final String[] WORKER_URLS = {
            "rmi://localhost:1099/worker1",
            "rmi://localhost:1099/worker2",
            "rmi://localhost:1099/worker3",
            "rmi://localhost:1099/worker4"
            // Add more if needed: worker3, worker4, ...
    };
}
