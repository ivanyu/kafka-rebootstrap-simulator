package org.apache.kafka.clients;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.common.Node;
import org.apache.kafka.common.utils.LogContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Simulation {
    private static final LogContext LOG_CONTEXT = new LogContext();

    private static final Logger LOG = LoggerFactory.getLogger(Simulation.class);

    private final SimulationParams params;
    private final ClusterConnectionStates connectionStates;
    private final List<Node> nodes;

    private final Random randOffset = new Random();

    Simulation(SimulationParams params) {
        this.params = params;
        this.connectionStates = new ClusterConnectionStates(
            params.reconnectBackoffMs(), params.reconnectBackoffMaxMs(),
            params.connectionSetupTimeoutMs(),
            params.connectionSetupTimeoutMaxMs(),
            LOG_CONTEXT, null);
        this.nodes = IntStream.range(0, params.nodes()).boxed()
            .map(id -> new Node(id, "host" + id, 9092))
            .collect(Collectors.toList());
    }

    Long run() {
        for (long now = 0; now <= params.duration().toMillis(); now += params.tick()) {
            handleTimedOutConnections(now);

            Node node = leastLoadedNode(now);

            if (node == null) {
                LOG.debug("[{}] Found rebootstrap opportunity", now);
                return now;
            }

            maybeUpdate(now, node);
        }
        return null;
    }

    /**
     * This simulates {@code NetworkClient.handleTimedOutConnections}.
     */
    private void handleTimedOutConnections(long now) {
        List<String> nodes = connectionStates.nodesWithConnectionSetupTimeout(now);
        for (String nodeId : nodes) {
            connectionStates.disconnected(nodeId, now);
            LOG.debug("[{}] Node {} disconnected", now, nodeId);
        }
    }
    /**
     * This simulates {@code NetworkClient.leastLoadedNode}.
     */
    private Node leastLoadedNode(long now) {
        Node foundConnecting = null;
        Node foundCanConnect = null;
        int offset = this.randOffset.nextInt(params.nodes());
        for (int i = 0; i < params.nodes(); i++) {
            int idx = (offset + i) % params.nodes();
            Node node = nodes.get(idx);

            // canSendRequest -- assume always false as the condition of this simulation.

            if (connectionStates.isPreparingConnection(node.idString())) {
                foundConnecting = node;
            } else if (connectionStates.canConnect(node.idString(), now)) {
                if (foundCanConnect == null ||
                    this.connectionStates.lastConnectAttemptMs(foundCanConnect.idString()) >
                        this.connectionStates.lastConnectAttemptMs(node.idString())) {
                    foundCanConnect = node;
                }
            }
        }

        if (foundConnecting != null) {
            return foundConnecting;
        } else if (foundCanConnect != null) {
            return foundCanConnect;
        } else {
            return null;
        }
    }

    /**
     * This simulates {@code NetworkClient.maybeUpdate}.
     */
    private void maybeUpdate(long now, Node node) {
        String nodeConnectionId = node.idString();

        // connectionStates.isReady -- assume always false as the condition of this simulation.

        // If there's any connection establishment underway, wait until it completes. This prevents
        // the client from unnecessarily connecting to additional nodes while a previous connection
        // attempt has not been completed.
        if (isAnyNodeConnecting()) {
            return;
        }

        if (connectionStates.canConnect(nodeConnectionId, now)) {
            LOG.debug("[{}] Node {} will connect", now, node.id());
            // We don't have a connection to this node right now, make one
            initiateConnect(node, now);
        }
    }

    private boolean isAnyNodeConnecting() {
        for (Node node : nodes) {
            if (connectionStates.isConnecting(node.idString())) {
                return true;
            }
        }
        return false;
    }

    private void initiateConnect(Node node, long now) {
        String nodeConnectionId = node.idString();
        connectionStates.connecting(nodeConnectionId, now, node.host());
    }
}
