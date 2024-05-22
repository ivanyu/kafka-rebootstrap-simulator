package org.apache.kafka.clients;

import java.time.Duration;

record SimulationParams(Duration duration, long tick, int nodes,
                        long reconnectBackoffMs, long reconnectBackoffMaxMs,
                        long connectionSetupTimeoutMs, long connectionSetupTimeoutMaxMs) { }
