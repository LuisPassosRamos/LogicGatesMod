package sisso.logicsim.diagnostics;

import java.util.Arrays;

/** Bounded, transient samples; accessed on the owning world thread only. */
public final class CircuitMetrics {
    private static final int CAPACITY = 256;
    private final long[] evaluations = new long[CAPACITY];
    private final long[] monitoring = new long[CAPACITY];
    private long evaluationCount, monitoringCount;
    private long lastSolver;

    public record Summary(int samples, double lastMs, double meanMs, double p95Ms, double maxMs,
                          double solverMs, double monitoringMeanMs) { }

    public void recordEvaluation(long totalNanos, long solverNanos) {
        evaluations[(int) (evaluationCount++ % CAPACITY)] = Math.max(0, totalNanos);
        lastSolver = Math.max(0, solverNanos);
    }

    public void recordMonitoring(long nanos) { monitoring[(int) (monitoringCount++ % CAPACITY)] = Math.max(0, nanos); }

    public Summary summary() {
        int count = (int) Math.min(evaluationCount, CAPACITY);
        int monitored = (int) Math.min(monitoringCount, CAPACITY);
        double monitorMean = monitored == 0 ? 0 : Arrays.stream(monitoring, 0, monitored).average().orElse(0) / 1_000_000;
        if (count == 0) return new Summary(0, 0, 0, 0, 0, 0, monitorMean);
        var sorted = Arrays.copyOf(evaluations, count);
        Arrays.sort(sorted);
        return new Summary(count, evaluations[(int) ((evaluationCount - 1) % CAPACITY)] / 1_000_000d,
                Arrays.stream(sorted).average().orElse(0) / 1_000_000,
                sorted[(int) Math.ceil(count * .95) - 1] / 1_000_000d, sorted[count - 1] / 1_000_000d,
                lastSolver / 1_000_000d, monitorMean);
    }
}
