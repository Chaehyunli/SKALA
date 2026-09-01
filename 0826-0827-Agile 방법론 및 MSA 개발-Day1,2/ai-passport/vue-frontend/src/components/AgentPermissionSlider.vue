<template>
  <section
    class="agent-slider"
    tabindex="0"
    aria-roledescription="carousel"
    :aria-label="title"
    @keydown.left.prevent="previous"
    @keydown.right.prevent="next"
    @touchstart.passive="onTouchStart"
    @touchend.passive="onTouchEnd"
  >
    <header class="slider-heading">
      <div>
        <p class="slider-eyebrow">AGENT PERMISSIONS</p>
        <h2>{{ title }}</h2>
        <span>{{ description }}</span>
      </div>
      <div v-if="agents.length > 1" class="slider-controls">
        <span class="slide-count">{{ activeIndex + 1 }} / {{ agents.length }}</span>
        <button type="button" aria-label="이전 Agent" :disabled="activeIndex === 0" @click="previous">
          <LeftOutlined />
        </button>
        <button type="button" aria-label="다음 Agent" :disabled="activeIndex === agents.length - 1" @click="next">
          <RightOutlined />
        </button>
      </div>
    </header>

    <div v-if="agents.length" class="slider-viewport">
      <div class="slider-track" :style="{ transform: `translateX(-${activeIndex * 100}%)` }">
        <article
          v-for="(agent, index) in agents"
          :key="`${agent.agentCode}-${index}`"
          class="agent-slide"
          :aria-hidden="index !== activeIndex"
        >
          <div class="agent-header">
            <div class="agent-index">{{ String(index + 1).padStart(2, '0') }}</div>
            <div class="agent-title">
              <h3>{{ getAgentName(agent.agentCode) }}</h3>
              <code>{{ agent.agentCode }}</code>
            </div>
            <div class="agent-stats">
              <span><strong>{{ agent.permissions.length }}</strong> 포함</span>
              <span><strong>{{ agent.excludedPermissions.length }}</strong> 제외</span>
            </div>
          </div>

          <div class="permission-columns">
            <section class="permission-section">
              <div class="permission-heading">
                <div><i class="state-dot allowed" />포함 권한</div>
                <span>{{ agent.permissions.length }}개</span>
              </div>
              <div v-if="agent.permissions.length" class="permission-list">
                <div v-for="permission in agent.permissions" :key="permission.code" class="permission-row">
                  <div>
                    <strong>{{ permission.name }}</strong>
                    <code>{{ permission.code }}</code>
                    <p>{{ permission.reason || '권한 제안 근거가 없습니다.' }}</p>
                  </div>
                  <span class="state-label allowed">{{ allowedLabel }}</span>
                </div>
              </div>
              <p v-else class="empty-copy">포함된 권한이 없습니다.</p>
            </section>

            <section class="permission-section excluded-section">
              <div class="permission-heading">
                <div><i class="state-dot excluded" />제외 권한</div>
                <span>{{ agent.excludedPermissions.length }}개</span>
              </div>
              <div v-if="agent.excludedPermissions.length" class="permission-list">
                <div v-for="permission in agent.excludedPermissions" :key="permission.code" class="permission-row">
                  <div>
                    <strong>{{ permission.name }}</strong>
                    <code>{{ permission.code }}</code>
                    <p>{{ permission.reason || '권한 제외 사유가 없습니다.' }}</p>
                  </div>
                  <span class="state-label excluded">{{ excludedLabel }}</span>
                </div>
              </div>
              <p v-else class="empty-copy">제외된 권한이 없습니다.</p>
            </section>
          </div>
        </article>
      </div>
    </div>

    <a-empty v-else class="empty-agent" description="표시할 Agent 권한이 없습니다." />

    <div v-if="agents.length > 1" class="slide-dots" role="tablist" aria-label="Agent 선택">
      <button
        v-for="(agent, index) in agents"
        :key="`${agent.agentCode}-dot-${index}`"
        type="button"
        :class="{ active: index === activeIndex }"
        :aria-label="`${index + 1}번째 Agent 보기`"
        :aria-selected="index === activeIndex"
        role="tab"
        @click="goTo(index)"
      />
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import { getAgentName, normalizeAgentList } from '@/constants/workRequest.js'

const props = defineProps({
  agentList: { type: Array, default: () => [] },
  title: { type: String, default: 'Agent별 권한' },
  description: { type: String, default: 'Agent를 넘겨 각 권한 범위와 제외 사유를 확인합니다.' },
  allowedLabel: { type: String, default: '허용' },
  excludedLabel: { type: String, default: '제외' }
})

const activeIndex = ref(0)
const touchStartX = ref(null)
const agents = computed(() => normalizeAgentList({ agentList: props.agentList }).map((agent) => ({
  ...agent,
  permissions: normalizePermissions(agent.permissions),
  excludedPermissions: normalizePermissions(agent.excludedPermissions)
})))

watch(agents, (items) => {
  if (activeIndex.value >= items.length) activeIndex.value = Math.max(items.length - 1, 0)
})

function normalizePermissions(items = []) {
  return items.map((permission) => ({
    ...permission,
    name: permission.label || permission.name || permission.code
  }))
}

function goTo(index) {
  activeIndex.value = Math.min(Math.max(index, 0), Math.max(agents.value.length - 1, 0))
}

function previous() {
  goTo(activeIndex.value - 1)
}

function next() {
  goTo(activeIndex.value + 1)
}

function onTouchStart(event) {
  touchStartX.value = event.changedTouches[0]?.clientX ?? null
}

function onTouchEnd(event) {
  if (touchStartX.value === null) return
  const distance = (event.changedTouches[0]?.clientX ?? touchStartX.value) - touchStartX.value
  if (Math.abs(distance) > 45) distance > 0 ? previous() : next()
  touchStartX.value = null
}
</script>

<style scoped>
.agent-slider { overflow: hidden; border: 1px solid var(--forest-border); border-radius: 11px; background: var(--forest-surface); outline: none; }
.agent-slider:focus-visible { box-shadow: 0 0 0 3px rgba(51, 116, 86, .16); }
.slider-heading { padding: 22px 24px 18px; display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; border-bottom: 1px solid var(--forest-border); }
.slider-eyebrow { margin: 0 0 5px; color: var(--forest-primary); font-size: 10px; font-weight: 700; letter-spacing: .12em; }
.slider-heading h2 { margin: 0; color: var(--forest-text); font-size: 17px; }
.slider-heading span { display: block; margin-top: 5px; color: var(--forest-text-secondary); font-size: 11px; }
.slider-controls { display: flex; align-items: center; gap: 7px; }
.slider-controls .slide-count { margin: 0 4px 0 0; font-variant-numeric: tabular-nums; white-space: nowrap; }
.slider-controls button { width: 34px; height: 34px; display: grid; place-items: center; border: 1px solid var(--forest-border); border-radius: 9px; color: var(--forest-text); background: var(--forest-subtle); }
.slider-controls button:disabled { cursor: not-allowed; color: var(--forest-text-muted); opacity: .5; }
.slider-viewport { overflow: hidden; }
.slider-track { display: flex; align-items: stretch; transition: transform .32s ease; will-change: transform; }
.agent-slide { min-width: 100%; padding: 24px; }
.agent-header { padding-bottom: 18px; display: grid; grid-template-columns: 38px minmax(0, 1fr) auto; align-items: center; gap: 12px; border-bottom: 1px solid var(--forest-border); }
.agent-index { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 11px; color: var(--forest-primary-dark); background: var(--forest-subtle); font-size: 12px; font-weight: 700; }
.agent-title h3 { margin: 0 0 3px; color: var(--forest-text); font-size: 16px; }
.agent-title code { color: var(--forest-text-secondary); font-size: 10px; }
.agent-stats { display: flex; gap: 7px; }
.agent-stats span { padding: 6px 9px; border-radius: 999px; color: var(--forest-text-secondary); background: var(--forest-subtle); font-size: 10px; }
.agent-stats strong { color: var(--forest-text); }
.permission-columns { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin-top: 18px; }
.permission-section { min-width: 0; padding: 16px; border: 1px solid #cfe0d6; border-radius: 10px; background: #f7fbf8; }
.excluded-section { border-color: #edd5d5; background: #fffafa; }
.permission-heading { margin-bottom: 10px; display: flex; align-items: center; justify-content: space-between; gap: 10px; color: var(--forest-text); font-size: 12px; font-weight: 600; }
.permission-heading > div { display: flex; align-items: center; gap: 7px; }
.permission-heading > span { color: var(--forest-text-secondary); font-size: 10px; font-weight: 500; }
.state-dot { width: 7px; height: 7px; border-radius: 50%; }
.state-dot.allowed { background: #2d7d55; }
.state-dot.excluded { background: #b34b4b; }
.permission-list { display: flex; flex-direction: column; }
.permission-row { padding: 12px 0; display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; border-top: 1px solid rgba(73, 93, 82, .12); }
.permission-row:first-child { border-top: 0; }
.permission-row > div { min-width: 0; }
.permission-row strong { display: inline; color: var(--forest-text); font-size: 12px; }
.permission-row code { margin-left: 7px; color: var(--forest-text-secondary); font-size: 9px; overflow-wrap: anywhere; }
.permission-row p { margin: 4px 0 0; color: var(--forest-text-secondary); font-size: 10px; line-height: 1.5; }
.state-label { flex: 0 0 auto; padding: 4px 7px; border-radius: 6px; font-size: 9px; }
.state-label.allowed { color: #126b48; background: #e4f2ea; }
.state-label.excluded { color: #b33a3a; background: #fceded; }
.empty-copy { margin: 18px 0; color: var(--forest-text-muted); font-size: 11px; text-align: center; }
.empty-agent { padding: 36px 0; }
.slide-dots { padding: 0 24px 20px; display: flex; align-items: center; justify-content: center; gap: 7px; }
.slide-dots button { width: 7px; height: 7px; padding: 0; border: 0; border-radius: 999px; background: var(--forest-border-strong); transition: width .2s ease, background .2s ease; }
.slide-dots button.active { width: 24px; background: var(--forest-primary); }
@media (max-width: 680px) {
  .slider-heading { padding: 18px; }
  .slider-heading > div:first-child span { max-width: 230px; }
  .slide-count { display: none !important; }
  .agent-slide { padding: 18px; }
  .agent-header { grid-template-columns: 34px minmax(0, 1fr); }
  .agent-stats { grid-column: 1 / -1; }
  .permission-columns { grid-template-columns: 1fr; }
}
</style>
