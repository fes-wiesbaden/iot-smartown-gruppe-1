<script setup lang="ts">
import { computed } from 'vue'

import BridgeModeControls from '@/components/bridge/BridgeModeControls.vue'
import BridgeStatusCard from '@/components/bridge/BridgeStatusCard.vue'
import LanternModeControls from '@/components/lanterns/LanternModeControls.vue'
import LanternStatusCard from '@/components/lanterns/LanternStatusCard.vue'
import { useBridge } from '@/composables/useBridge'
import { useLanterns } from '@/composables/useLanterns'

/**
 * Bindet Snapshot, Live-Status und Moduswechsel in die Dashboard-Ansicht ein.
 */
const { brokerConnected, error, lanternOnline, liveConnected: lanternLiveConnected, loading, setMode, snapshot, submittingMode } = useLanterns()
const { bridgeMode, submittingBridgeMode, setBridgeMode, snapshot: bridgeSnapshot, loading: bridgeLoading, error: bridgeError, brokerConnected: bridgeBroker, bridgeOnline, liveConnected: bridgeLiveConnected } = useBridge()

const lanternControlsEnabled = computed(() => brokerConnected.value && lanternOnline.value)
const bridgeControlsEnabled = computed(() => bridgeBroker.value && bridgeOnline.value)
const liveUpdatesActive = computed(() => lanternLiveConnected.value || bridgeLiveConnected.value)

/**
 * Zeigt den Stand der Stadtmodule, wobei Laternen und Bruecke bereits live angebunden sind.
 */
const modules = computed(() => [
  { name: 'Bruecke', status: 'Offen' },
  { name: 'Flughafen', status: 'Offen' },
  {
    name: 'Laternen',
    status: !snapshot.value ? 'Warte auf ESP32' : lanternOnline.value ? 'ESP32 online' : 'ESP32 offline',
  },
])
</script>

<template>
  <main class="dashboard">
    <header class="dashboard__header">
      <div>
        <img class="dashboard__logo" src="/smartown-logo.png" alt="SmarTown Logo" />
        <h1 class="dashboard__title">Kontrollzentrum</h1>
        <span class="dashboard__live" :class="{ 'dashboard__live--offline': !liveUpdatesActive }">
          {{ liveUpdatesActive ? 'Live' : 'Live aus' }}
        </span>
      </div>
      <span class="dashboard__status" :class="{ 'dashboard__status--offline': !brokerConnected }">
        {{ brokerConnected ? 'MQTT verbunden' : 'MQTT getrennt' }}
      </span>
    </header>

    <section class="dashboard__section" aria-label="Stadtmodule">
      <div class="module-grid">
        <article v-for="module in modules" :key="module.name" class="module-card">
          <h2 class="module-card__title">{{ module.name }}</h2>
          <p class="module-card__status">{{ module.status }}</p>
        </article>
      </div>
    </section>

    <section class="dashboard__section dashboard__section--feature" aria-label="Laternen MQTT">
      <LanternStatusCard
        :broker-connected="brokerConnected"
        :error="error"
        :loading="loading"
        :snapshot="snapshot"
      />
      <LanternModeControls
        :controls-enabled="lanternControlsEnabled"
        :current-mode="snapshot?.state.mode ?? null"
        :submitting-mode="submittingMode"
        @set-mode="setMode"
      />
    </section>

    <section class="dashboard__section dashboard__section--feature" aria-label="Brücken MQTT">
      <BridgeStatusCard
        :broker-connected="bridgeBroker"
        :bridge-online="bridgeOnline"
        :error="bridgeError"
        :loading="bridgeLoading"
        :snapshot="bridgeSnapshot"
      />
      <BridgeModeControls
        :controls-enabled="bridgeControlsEnabled"
        :current-mode="bridgeMode"
        :submitting-mode="submittingBridgeMode"
        @set-mode="setBridgeMode"
      />
    </section>
  </main>
</template>

<style scoped>
.dashboard {
  min-height: 100vh;
  padding: 32px;
  color: #172026;
  background:
    radial-gradient(circle at top left, rgba(96, 53, 250, 0.14), transparent 26%),
    radial-gradient(circle at top right, rgba(96, 53, 250, 0.1), transparent 24%),
    #f6f3ff;
}

.dashboard__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin: 0 auto 32px;
  max-width: 1120px;
}

.dashboard__logo {
  display: block;
  width: 72px;
  height: 72px;
  margin-bottom: 10px;
  object-fit: contain;
  filter: drop-shadow(0 12px 24px rgba(96, 53, 250, 0.16));
}

.dashboard__title {
  margin: 0;
  color: #172026;
  font-size: 2rem;
  font-weight: 800;
}

.dashboard__live {
  display: inline-flex;
  margin-top: 10px;
  border: 1px solid #dc2626;
  border-radius: 999px;
  padding: 6px 14px;
  color: #ffffff;
  background: #dc2626;
  font-size: 0.9375rem;
  font-weight: 800;
  text-transform: uppercase;
  box-shadow: 0 10px 24px rgba(220, 38, 38, 0.22);
}

.dashboard__live--offline {
  border-color: #d8dfe2;
  color: #5c6870;
  background: #f5f7f8;
  box-shadow: none;
}

.dashboard__status {
  border: 1px solid var(--theme-accent-border);
  border-radius: 999px;
  padding: 6px 12px;
  color: var(--theme-accent-strong);
  background: var(--theme-accent-soft);
  font-size: 0.875rem;
  font-weight: 700;
}

.dashboard__status--offline {
  border-color: var(--theme-offline-border);
  color: var(--theme-offline);
  background: var(--theme-offline-bg);
}

.dashboard__section {
  margin: 0 auto 24px;
  max-width: 1120px;
}

.dashboard__section--feature {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(300px, 1fr);
  gap: 16px;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.module-card {
  border: 1px solid var(--theme-card-border);
  border-radius: 14px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
  box-shadow: 0 16px 40px rgba(96, 53, 250, 0.08);
}

.module-card__title {
  margin: 0 0 8px;
  color: #172026;
  font-size: 1rem;
  font-weight: 800;
}

.module-card__status {
  margin: 0;
  color: var(--theme-muted);
  font-weight: 600;
}

@media (max-width: 920px) {
  .dashboard__section--feature {
    grid-template-columns: 1fr;
  }

  .module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .dashboard {
    padding: 20px;
  }

  .dashboard__header {
    align-items: stretch;
    flex-direction: column;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
