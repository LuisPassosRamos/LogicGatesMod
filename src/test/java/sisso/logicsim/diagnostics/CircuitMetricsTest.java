package sisso.logicsim.diagnostics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircuitMetricsTest {
    @Test void emptyMetricsAndIdleMonitoringDoNotInventEvaluations() {
        var metrics = new CircuitMetrics();
        assertEquals(0, metrics.summary().samples());
        metrics.recordMonitoring(2_000_000);
        assertEquals(0, metrics.summary().samples());
        assertEquals(2, metrics.summary().monitoringMeanMs());
    }
    @Test void rollingWindowAndPercentilesStayBoundedAfterRollover() {
        var metrics = new CircuitMetrics();
        for (int i = 1; i <= 300; i++) {
            metrics.recordEvaluation(i * 1_000_000L, i * 500_000L);
            metrics.recordMonitoring(i * 1_000_000L);
        }
        var result = metrics.summary();
        assertEquals(256,result.samples());
        assertEquals(300,result.lastMs());
        assertEquals(172.5,result.meanMs());
        assertEquals(288,result.p95Ms());
        assertEquals(300,result.maxMs());
        assertEquals(150,result.solverMs());
        assertEquals(172.5,result.monitoringMeanMs());
    }
}
