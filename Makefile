.PHONY: build
build:
	./gradlew installDist

.PHONY: sim_3_nodes_default
sim_3_nodes_default: build
	app/build/install/app/bin/app \
		--runs 50000 --duration 5m --tick 100 \
		--nodes 3 \
		--reconnectBackoffMs 50 --reconnectBackoffMaxMs 1000 \
		--connectionSetupTimeoutMs 10000 --connectionSetupTimeoutMaxMs 30000

.PHONY: sim_3_nodes
sim_3_nodes: build
	app/build/install/app/bin/app \
		--runs 50000 --duration 5m --tick 100 \
		--nodes 3 \
		--reconnectBackoffMs 50 --reconnectBackoffMaxMs 1000 \
		--connectionSetupTimeoutMs 10000 --connectionSetupTimeoutMaxMs 0

.PHONY: sim_6_nodes
sim_6_nodes: build
	app/build/install/app/bin/app \
		--runs 50000 --duration 5m --tick 100 \
		--nodes 6 \
		--reconnectBackoffMs 50 --reconnectBackoffMaxMs 1000 \
		--connectionSetupTimeoutMs 10000 --connectionSetupTimeoutMaxMs 0

.PHONY: sim_9_nodes
sim_9_nodes: build
	app/build/install/app/bin/app \
		--runs 50000 --duration 5m --tick 100 \
		--nodes 9 \
		--reconnectBackoffMs 50 --reconnectBackoffMaxMs 1000 \
		--connectionSetupTimeoutMs 10000 --connectionSetupTimeoutMaxMs 0

.PHONY: sim_15_nodes_default
sim_15_nodes_default: build
	app/build/install/app/bin/app \
		--runs 50000 --duration 5m --tick 100 \
		--nodes 15 \
		--reconnectBackoffMs 50 --reconnectBackoffMaxMs 1000 \
		--connectionSetupTimeoutMs 10000 --connectionSetupTimeoutMaxMs 0

.PHONY: sim_15_nodes_tuning
sim_15_nodes_tuning: build
	app/build/install/app/bin/app \
		--runs 50000 --duration 5m --tick 100 \
		--nodes 15 \
		--reconnectBackoffMs 50 --reconnectBackoffMaxMs 2000 \
		--connectionSetupTimeoutMs 10000 --connectionSetupTimeoutMaxMs 0

.PHONY: sim_30_nodes_tuning
sim_30_nodes_tuning: build
	app/build/install/app/bin/app \
		--runs 50000 --duration 5m --tick 100 \
		--nodes 30 \
		--reconnectBackoffMs 50 --reconnectBackoffMaxMs 3000 \
		--connectionSetupTimeoutMs 10000 --connectionSetupTimeoutMaxMs 0

.PHONY: sim_manual
sim_manual: build
	app/build/install/app/bin/app \
		--runs 1 --duration 10m --tick 100 \
		--nodes 3 \
		--reconnectBackoffMs 30000 --reconnectBackoffMaxMs 30000 \
		--connectionSetupTimeoutMs 10000 --connectionSetupTimeoutMaxMs 10000
