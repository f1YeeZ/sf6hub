<template>
  <div v-if="canAccessAdmin" class="admin-console">
    <aside class="admin-sidebar">
      <router-link to="/" class="admin-brand">
        <span>Combos Hub</span>
        <strong>Ops Console</strong>
      </router-link>

      <nav class="admin-nav">
        <button
          v-for="item in sections"
          :key="item.key"
          type="button"
          :class="['admin-nav-item', { active: activeSection === item.key }]"
          @click="activeSection = item.key"
        >
          <span class="material-symbols-outlined">{{ item.icon }}</span>
          {{ item.label }}
        </button>
      </nav>

      <div class="admin-sidebar-foot">
        <small>当前管理员</small>
        <strong>{{ adminAuth.user?.username }}</strong>
        <button type="button" @click="adminAuth.logout()">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-topbar">
        <div>
          <p>ADMIN SYSTEM</p>
          <h1>{{ currentSection.label }}</h1>
        </div>
        <div class="admin-top-actions">
          <router-link to="/" class="admin-link-btn">
            <span class="material-symbols-outlined">open_in_new</span>
            返回前台
          </router-link>
          <button type="button" class="admin-primary" :disabled="loading" @click="loadAll">
            <span class="material-symbols-outlined">refresh</span>
            {{ loading ? '刷新中...' : '刷新' }}
          </button>
        </div>
      </header>

      <section v-if="activeSection === 'dashboard'" class="admin-stack">
        <div class="ops-workbench">
          <article v-for="task in dashboardTasks" :key="task.key" :class="['ops-task-card', task.tone]">
            <span class="material-symbols-outlined">{{ task.icon }}</span>
            <div>
              <strong>{{ formatNumber(task.count) }}</strong>
              <p>{{ task.label }}</p>
              <small>{{ task.hint }}</small>
            </div>
            <button type="button" @click="openDashboardTask(task)">处理</button>
          </article>
        </div>

        <div class="ops-focus-grid">
          <section class="ops-focus-panel">
            <div class="dashboard-card-head">
              <h2>高风险连招</h2>
              <span>按风险优先级</span>
            </div>
            <article v-for="combo in dashboardHighRiskCombos" :key="combo.id" class="ops-risk-row" @click="focusComboFromDashboard(combo)">
              <div>
                <strong>{{ combo.character }} · {{ combo.starter }}</strong>
                <p>{{ combo.route }}</p>
                <small>{{ combo.manualReviewReason || '疑点、举报、视频状态综合排序' }}</small>
              </div>
              <mark>{{ combo.riskScore }}</mark>
            </article>
            <p v-if="!dashboardHighRiskCombos.length" class="empty-row">暂无高风险连招</p>
          </section>

          <section class="ops-focus-panel">
            <div class="dashboard-card-head">
              <h2>待处理投稿</h2>
              <span>按风险优先处理</span>
            </div>
            <article v-for="combo in dashboardIssueReviewCombos" :key="combo.id" class="ops-risk-row" @click="focusComboFromDashboard(combo)">
              <div>
                <strong>{{ combo.character }} · {{ combo.starter }}</strong>
                <p>{{ combo.route }}</p>
                <small>{{ combo.manualReviewReason || '投稿、举报或视频异常' }}</small>
              </div>
              <mark class="danger">{{ combo.riskScore || 0 }}</mark>
            </article>
            <p v-if="!dashboardIssueReviewCombos.length" class="empty-row">暂无待处理投稿</p>
          </section>
        </div>

        <section class="ops-focus-panel recent-audit-panel">
          <div class="dashboard-card-head">
            <h2>最近操作记录</h2>
            <span>追踪后台动作</span>
          </div>
          <div class="recent-audit-list">
            <article v-for="log in dashboardRecentAuditLogs" :key="log.id">
              <strong>{{ log.adminName || 'system' }} · {{ log.action }}</strong>
              <p>{{ log.targetType }} #{{ log.targetId || '-' }} · {{ formatDateTime(log.createdAt) }}</p>
              <small class="recent-audit-detail">{{ log.detail || '无详情' }}</small>
            </article>
          </div>
        </section>

        <div class="dashboard-metrics">
          <el-card v-for="metric in overviewMetrics" :key="metric.label" :class="['overview-card', { muted: metric.unavailable }]" shadow="never">
            <div class="overview-card-head">
              <el-icon><component :is="metric.icon" /></el-icon>
              <span v-if="metric.growth !== null && metric.growth !== undefined" :class="['metric-growth', { down: metric.growth < 0 }]">
                {{ metric.growth >= 0 ? '+' : '' }}{{ metric.growth }}%
              </span>
              <span v-else-if="metric.status" class="metric-growth live">{{ metric.status }}</span>
              <span v-else-if="metric.unavailable" class="metric-growth muted">未接入</span>
            </div>
            <strong>{{ metric.unavailable ? '-' : formatNumber(metric.value) }}</strong>
            <small>{{ metric.label }}</small>
          </el-card>
        </div>

        <div class="dashboard-chart-row">
          <el-card class="dashboard-card" shadow="never">
            <template #header>
              <div class="dashboard-card-head">
                <h2>用户增长趋势</h2>
                <span>最近 30 天</span>
              </div>
            </template>
            <div ref="userGrowthChartRef" class="chart-surface"></div>
          </el-card>

          <el-card class="dashboard-card" shadow="never">
            <template #header>
              <div class="dashboard-card-head">
                <h2>网站访问趋势</h2>
                <span>最近 30 天</span>
              </div>
            </template>
            <div ref="visitTrendChartRef" class="chart-surface"></div>
          </el-card>
        </div>

        <div class="dashboard-chart-row">
          <el-card class="dashboard-card" shadow="never">
            <template #header>
              <div class="dashboard-card-head">
                <h2>角色连招数量排行</h2>
                <span>TOP 10</span>
              </div>
            </template>
            <div v-if="characterComboRanking.length" ref="characterHeatChartRef" class="chart-surface"></div>
            <div v-else class="chart-empty">
              <strong>暂无角色连招数据</strong>
              <p>当前没有可聚合的连招数据，暂不展示排行。</p>
            </div>
          </el-card>

          <el-card class="dashboard-card" shadow="never">
            <template #header>
              <div class="dashboard-card-head">
                <h2>投稿审核状态</h2>
                <span>连招投稿</span>
              </div>
            </template>
            <div ref="reviewDonutChartRef" class="chart-surface"></div>
          </el-card>
        </div>

        <el-card class="dashboard-card" shadow="never">
          <template #header>
            <div class="dashboard-card-head">
              <h2>热门连招排行榜</h2>
              <span>按收藏量排序</span>
            </div>
          </template>
          <el-table :data="popularComboRows" class="dashboard-table" stripe>
            <el-table-column prop="rank" label="排名" width="76" />
            <el-table-column prop="name" label="连招名称" min-width="220" />
            <el-table-column prop="character" label="角色" width="140" />
            <el-table-column prop="favorites" label="收藏量" width="120" />
            <el-table-column prop="likes" label="点赞量" width="120" />
          </el-table>
        </el-card>
      </section>

      <section v-if="activeSection === 'users'" class="admin-stack">
        <div class="admin-panel">
          <div class="panel-head">
            <h2>用户管理</h2>
          </div>
          <div class="admin-filter user-filter-row">
            <input v-model.trim="userQuery" placeholder="用户名 / 邮箱" @keyup.enter="refreshUsers" />
            <select v-model="userRole" @change="refreshUsers">
              <option value="">全部角色</option>
              <option value="user">普通用户</option>
              <option value="admin">管理员</option>
            </select>
            <button type="button" @click="refreshUsers">搜索</button>
          </div>

          <div class="bulk-bar">
            <label class="select-all">
              <input type="checkbox" :checked="isAllSelected('users', selectableUsers)" @change="toggleAll('users', selectableUsers, $event.target.checked)" />
              全选可操作用户
            </label>
            <span>已选 {{ selectedCount('users') }} 个</span>
            <button type="button" :disabled="!selectedCount('users')" class="danger" @click="openBatchUserBanModal(true)">批量封禁</button>
            <button type="button" :disabled="!selectedCount('users')" @click="openBatchUserBanModal(false)">批量解封</button>
            <button type="button" class="ghost" :disabled="!selectedCount('users')" @click="clearSelection('users')">清空</button>
          </div>

          <div class="user-row-list">
            <article
              v-for="user in users"
              :id="adminTargetDomId('user', user.id)"
              :key="user.id"
              :class="['user-row-card', { 'target-highlight': isHighlighted('user', user.id) }]"
            >
              <input v-model="selected.users" class="select-box user-row-select" type="checkbox" :value="user.id" :disabled="isCurrentAdmin(user)" />
              <div class="user-identity">
                <span class="user-avatar">{{ userAvatarText(user) }}</span>
                <div>
                  <strong>{{ user.username }}</strong>
                  <small>#{{ user.id }} · {{ user.email || '未绑定邮箱' }}</small>
                </div>
              </div>
              <div class="user-meta-grid">
                <div>
                  <small>角色</small>
                  <span>{{ user.role === 'admin' ? '管理员' : '普通用户' }}</span>
                </div>
                <div>
                  <small>权限</small>
                  <span>{{ hasUserPermission(user, COMBO_REVIEW_PERMISSION) ? '连招审核管理' : '基础权限' }}</span>
                </div>
                <div>
                  <small>最近登录</small>
                  <span>{{ user.lastLoginAt ? formatDateTime(user.lastLoginAt) : '暂无记录' }}</span>
                </div>
                <div>
                  <small>投稿</small>
                  <span>{{ user.comboCount || 0 }} 条</span>
                </div>
                <div>
                  <small>投稿被举报</small>
                  <span>{{ user.reportCount || 0 }} 次</span>
                </div>
                <div>
                  <small>审核权限</small>
                  <span>{{ hasUserPermission(user, COMBO_REVIEW_PERMISSION) ? '已授权' : '未授权' }}</span>
                </div>
              </div>
              <div class="user-status">
                <mark :class="{ danger: user.banned }">{{ user.banned ? '已封禁' : '正常' }}</mark>
                <small v-if="user.banReason">{{ user.banReason }}</small>
                <small v-if="isCurrentAdmin(user)" class="locked-note">当前账号</small>
              </div>
              <div class="row-actions">
                <button type="button" :disabled="isCurrentAdmin(user)" @click="openUserManageModal(user)">管理</button>
                <button type="button" class="ghost" @click="openUserCombos(user)">投稿</button>
                <button type="button" class="ghost" @click="openUserReports(user)">投稿举报</button>
                <button v-if="!user.banned" type="button" class="danger" :disabled="isCurrentAdmin(user)" @click="setUserBan(user, true)">封禁</button>
                <button v-else type="button" @click="setUserBan(user, false)">解封</button>
              </div>
            </article>
            <p v-if="!users.length" class="empty-row">暂无用户</p>
          </div>
          <div class="report-summary-row">
            <span>当前 {{ users.length }} 条</span>
            <span>总计 {{ userTotal }} 条</span>
            <span>第 {{ userPage }} / {{ userPageCount }} 页</span>
          </div>
          <div v-if="userTotal > userPageSize" class="pager-row admin-pager">
            <button type="button" :disabled="userPage <= 1" @click="changeUserPage(userPage - 1)">
              <span class="material-symbols-outlined">chevron_left</span>
              上一页
            </button>
            <span>{{ userPage }} / {{ userPageCount }}</span>
            <button type="button" :disabled="userPage >= userPageCount" @click="changeUserPage(userPage + 1)">
              下一页
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </section>

      <section v-if="activeSection === 'characters'" class="admin-stack">
        <div class="admin-panel source-panel">
          <div class="panel-head">
            <h2>角色管理</h2>
            <button type="button" class="admin-primary" @click="openCharacterModal()">
              <span class="material-symbols-outlined">add</span>
              新增角色
            </button>
          </div>
          <p>角色资料、头像与定位通过弹窗维护，列表保持轻量浏览。</p>
        </div>

        <div class="admin-panel">
          <div class="panel-head">
            <h2>角色列表</h2>
            <BulkActions
              :count="selectedCount('characters')"
              :all-selected="isAllSelected('characters', pagedCharacters)"
              @toggle-all="toggleAll('characters', pagedCharacters, $event)"
              @clear="clearSelection('characters')"
            >
              <button type="button" class="danger" :disabled="!selectedCount('characters')" @click="batchDelete('characters', pagedCharacters, api.deleteAdminCharacter, reloadCharactersArea, '角色')">批量删除</button>
            </BulkActions>
          </div>
          <div class="admin-list-grid">
            <article v-for="character in pagedCharacters" :key="character.id" class="admin-item character-admin-item">
              <input v-model="selected.characters" class="select-box" type="checkbox" :value="character.id" />
              <div class="character-order-tools" aria-label="角色排序">
                <button type="button" title="置顶" :disabled="!canMoveCharacter(character, 'top')" @click="moveCharacter(character, 'top')">
                  <span class="material-symbols-outlined">keyboard_double_arrow_up</span>
                </button>
                <button type="button" title="上移" :disabled="!canMoveCharacter(character, 'up')" @click="moveCharacter(character, 'up')">
                  <span class="material-symbols-outlined">keyboard_arrow_up</span>
                </button>
                <button type="button" title="下移" :disabled="!canMoveCharacter(character, 'down')" @click="moveCharacter(character, 'down')">
                  <span class="material-symbols-outlined">keyboard_arrow_down</span>
                </button>
                <button type="button" title="置底" :disabled="!canMoveCharacter(character, 'bottom')" @click="moveCharacter(character, 'bottom')">
                  <span class="material-symbols-outlined">keyboard_double_arrow_down</span>
                </button>
              </div>
              <div>
                <strong>{{ character.name }}</strong>
                <p>{{ character.archetype || '暂无定位' }} · 排序 {{ character.displayOrder || characterIndex(character) + 1 }}</p>
                <div class="combo-content-strip">
                  <span>连招 {{ character.comboCount || 0 }}</span>
                  <span class="issue">待审 {{ character.pendingComboCount || 0 }}</span>
                  <span>帧数 {{ character.frameCount || 0 }}</span>
                  <span v-if="!character.avatar || !character.description" class="issue">资料缺失</span>
                  <span v-if="!hasCharacterData(character)" class="issue">基础数据未配置</span>
                </div>
              </div>
              <div class="row-actions">
                <button type="button" class="ghost" @click="openCharacterCombos(character)">连招</button>
                <button type="button" class="ghost" @click="openCharacterFrames(character)">帧数</button>
                <button type="button" @click="openCharacterModal(character)">编辑</button>
                <button type="button" class="danger" @click="deleteCharacter(character.id)">删除</button>
              </div>
            </article>
          </div>
          <p v-if="!characters.length" class="empty-row">暂无角色</p>
          <div v-if="characters.length > characterPageSize" class="pager-row admin-pager">
            <button type="button" :disabled="characterPage <= 1" @click="changeCharacterPage(characterPage - 1)">
              <span class="material-symbols-outlined">chevron_left</span>
              上一页
            </button>
            <span>{{ characterPage }} / {{ characterPageCount }}</span>
            <button type="button" :disabled="characterPage >= characterPageCount" @click="changeCharacterPage(characterPage + 1)">
              下一页
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </section>

      <section v-if="activeSection === 'frames'" class="admin-stack">
        <div class="admin-panel source-panel">
          <div>
            <h2>帧数数据来源</h2>
            <p>来自 Capcom 官网街霸6帧数表。当前维护版本：{{ frameDataVersion }}</p>
          </div>
          <div class="row-actions">
            <button type="button" class="admin-primary" @click="openFrameModal()">
              <span class="material-symbols-outlined">add</span>
              新增帧数
            </button>
            <button type="button" class="ghost" @click="loadFrameOps">刷新日志</button>
          </div>
        </div>

        <div class="admin-panel">
          <div class="panel-head">
            <h2>帧数数据</h2>
            <div class="admin-filter">
              <button type="button" class="ghost" :disabled="syncingOfficialFrames" @click="syncOfficialFrames">
                {{ syncingOfficialFrames ? '同步中...' : '同步 Capcom 帧数' }}
              </button>
              <select v-model="frameControlFilter" aria-label="帧数分类" @change="refreshFrames">
                <option value="">全部分类</option>
                <option value="modern">现代</option>
                <option value="classic">经典</option>
              </select>
              <select v-model="frameFilterCharacterId" @change="refreshFrames">
                <option value="">全部角色</option>
                <option v-for="c in characters" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
              <input v-model.trim="frameSearch" type="search" placeholder="招式 / 来源" aria-label="搜索帧数" />
            </div>
          </div>
          <div class="bulk-bar">
            <label class="select-all">
              <input type="checkbox" :checked="isAllSelected('frames', frames)" @change="toggleAll('frames', frames, $event.target.checked)" />
              全选
            </label><span>已选 {{ selectedCount('frames') }} 个</span>
            <button type="button" class="danger" :disabled="!selectedCount('frames')" @click="batchDelete('frames', frames, api.deleteAdminFrame, reloadFramesArea, '帧数')">批量删除</button>
            <button type="button" class="ghost" :disabled="!selectedCount('frames')" @click="clearSelection('frames')">清空</button>
          </div>
          <div class="admin-table frames-table">
            <div class="table-row table-head">
              <span></span><span>角色</span><span>招式</span><span>启动</span><span>持续</span><span>恢复</span><span>防御</span><span>命中</span><span>取消</span><span>伤害</span><span>属性</span><span>来源</span><span>操作</span>
            </div>
            <div v-for="frame in frames" :key="frame.id" :class="['table-row', { highlighted: frame.recentlyChanged }]">
              <span><input v-model="selected.frames" class="select-box" type="checkbox" :value="frame.id" /></span>
              <span>{{ characterName(frame.characterId) }}</span>
              <span><strong>{{ frame.moveName }}</strong></span>
              <span>{{ frame.startup ?? '-' }}</span>
              <span>{{ frame.active ?? '-' }}</span>
              <span>{{ frame.recovery ?? '-' }}</span>
              <span>{{ frame.onBlock || '-' }}</span>
              <span>{{ frame.onHit || '-' }}</span>
              <span>{{ frame.cancel || '-' }}</span>
              <span>{{ frame.damage || '-' }}</span>
              <span>{{ frame.properties || '-' }}</span>
              <span>
                {{ frame.sourceCharacterSlug || '-' }}
                <mark v-if="frame.recentlyChanged">最近变更</mark>
                <mark v-if="frame.manuallyCorrected">人工修正</mark>
              </span>
              <span class="row-actions">
                <button type="button" @click="openFrameModal(frame)">编辑</button>
                <button type="button" class="danger" @click="deleteFrame(frame.id)">删除</button>
              </span>
            </div>
          </div>
          <div class="report-summary-row">
            <span>当前 {{ frames.length }} 条</span>
            <span>总计 {{ frameTotal }} 条</span>
            <span>第 {{ framePage }} / {{ framePageCount }} 页</span>
          </div>
          <div v-if="frameTotal > framePageSize" class="pager-row admin-pager">
            <button type="button" :disabled="framePage <= 1" @click="changeFramePage(framePage - 1)">
              <span class="material-symbols-outlined">chevron_left</span>
              上一页
            </button>
            <span>{{ framePage }} / {{ framePageCount }}</span>
            <button type="button" :disabled="framePage >= framePageCount" @click="changeFramePage(framePage + 1)">
              下一页
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>

        <div class="admin-two-col">
          <div class="admin-panel">
            <div class="panel-head">
              <h2>同步日志</h2>
            </div>
            <div class="admin-list-grid">
              <article v-for="log in frameSyncLogs" :key="log.id" class="admin-item single-column">
                <strong>{{ log.status }} · {{ formatDateTime(log.createdAt) }}</strong>
                <p>{{ log.detail }}</p>
              </article>
              <p v-if="!frameSyncLogs.length" class="empty-row">暂无同步日志</p>
            </div>
          </div>
          <div class="admin-panel">
            <div class="panel-head">
              <h2>变更历史</h2>
            </div>
            <div class="admin-list-grid">
              <article v-for="item in frameChangeHistory" :key="item.id" class="admin-item single-column">
                <strong>{{ item.action }} · {{ item.moveName }}</strong>
                <p>{{ item.adminName }} · {{ formatDateTime(item.createdAt) }}</p>
              </article>
              <p v-if="!frameChangeHistory.length" class="empty-row">暂无变更历史</p>
            </div>
          </div>
        </div>
      </section>

      <section v-if="activeSection === 'combos'" class="admin-stack">
        <div class="admin-panel source-panel">
          <div class="panel-head">
            <h2>连招审核工作台</h2>
            <button type="button" class="admin-primary" @click="openComboModal()">
              <span class="material-symbols-outlined">add</span>
              新增连招
            </button>
          </div>
          <p>按风险队列处理投稿，审核时同时查看视频、父连招、重复候选和训练备注。</p>
        </div>

        <div class="admin-panel">
          <div class="panel-head">
            <h2>连招列表</h2>
            <div class="admin-filter">
              <div class="combo-status-tabs" aria-label="连招审核状态">
                <button
                  v-for="option in comboStatusFilterOptions"
                  :key="option.value || 'all'"
                  type="button"
                  :aria-pressed="comboStatusFilter === option.value"
                  :class="['combo-status-tab', option.value, { active: comboStatusFilter === option.value }]"
                  @click="comboStatusFilter = option.value"
                >
                  <span>{{ option.label }}</span>
                  <strong>{{ option.count }}</strong>
                </button>
              </div>
              <div class="combo-control-tabs" aria-label="连招类型分类">
                <button
                  v-for="option in comboControlFilterOptions"
                  :key="option.value || 'all-control-types'"
                  type="button"
                  :aria-pressed="comboControlFilter === option.value"
                  :class="['combo-control-tab', option.value ? `is-${option.value}` : 'all', { active: comboControlFilter === option.value }]"
                  @click="comboControlFilter = option.value"
                >
                  <span>{{ option.label }}</span>
                  <strong>{{ option.count }}</strong>
                </button>
              </div>
              <select v-model="comboFilterCharacterId">
                <option value="">全部角色</option>
                <option v-for="c in characters" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <BulkActions
              :count="selectedCount('combos')"
              :all-selected="isAllSelected('combos', comboBulkRows)"
              @toggle-all="toggleAll('combos', comboBulkRows, $event)"
              @clear="clearSelection('combos')"
            >
              <button type="button" :disabled="!selectedCount('combos')" @click="batchApproveCombos">批量通过</button>
              <button type="button" class="danger" :disabled="!selectedCount('combos')" @click="batchDelete('combos', comboBulkRows, api.deleteAdminCombo, reloadCombosArea, '连招')">批量删除</button>
            </BulkActions>
          </div>
          <div class="combo-card-toolbar">
            <label class="combo-queue-search">
              <span class="material-symbols-outlined">search</span>
              <input v-model.trim="comboQueueQuery" type="search" placeholder="搜索角色 / 起手 / 路线" />
            </label>
            <span>共 {{ comboTotal }} 条连招</span>
          </div>
          <div class="admin-combo-card-grid">
            <article
              v-for="combo in queuedComboRows"
              :id="adminTargetDomId('combo', combo.id)"
              :key="combo.id"
              :class="['admin-combo-card', { 'target-highlight': isHighlighted('combo', combo.id) }]"
              @click="openComboReview(combo)"
            >
              <div class="combo-card-head">
                <input v-model="selected.combos" class="select-box" type="checkbox" :value="combo.id" @click.stop />
                <span :class="['combo-review-status', combo.status || 'pending']">{{ comboReviewStatusLabel(combo.status) }}</span>
                <mark>风险 {{ combo.reviewPriority || 0 }}</mark>
              </div>
              <strong>{{ characterName(combo.characterId) }} · {{ combo.starter || '未填写起手' }}</strong>
              <p>{{ combo.route || combo.comboText || '-' }}</p>
              <div class="combo-card-metrics">
                <span><strong>{{ combo.damage || 0 }}</strong>伤害</span>
                <span><strong>{{ combo.driveCost ?? 0 }}</strong>Drive</span>
                <span><strong>{{ combo.saCost ?? 0 }}</strong>SA</span>
                <span><strong>{{ combo.reportCount || 0 }}</strong>举报</span>
              </div>
              <div class="combo-card-actions">
                <button type="button" @click.stop="openComboReview(combo)">审核</button>
                <button type="button" class="ghost" @click.stop="openComboModal(combo)">编辑</button>
                <button type="button" class="danger" @click.stop="deleteCombo(combo.id)">删除</button>
              </div>
            </article>
            <p v-if="!queuedComboRows.length" class="empty-row">暂无连招</p>
          </div>
          <div v-if="comboTotal > comboPageSize" class="admin-pager">
            <button type="button" :disabled="comboPage <= 1" @click="changeComboPage(comboPage - 1)">上一页</button>
            <span>{{ comboPage }} / {{ comboPageCount }}</span>
            <button type="button" :disabled="comboPage >= comboPageCount" @click="changeComboPage(comboPage + 1)">下一页</button>
          </div>
        </div>
      </section>

      <section v-if="activeSection === 'announcements'" class="admin-stack">
        <div class="admin-panel source-panel">
          <div class="panel-head">
            <h2>使用指南管理</h2>
            <button type="button" class="admin-primary" @click="openAnnouncementModal()">
              <span class="material-symbols-outlined">add</span>
              新增指南
            </button>
          </div>
          <p>后台维护的指南会显示在前台使用指南页，列表外只露出标题和内容开头。</p>
        </div>

        <div class="admin-panel">
          <div class="panel-head">
            <h2>指南记录</h2>
            <button type="button" class="ghost" @click="loadAnnouncements">刷新</button>
          </div>
          <div class="admin-list-grid">
            <article v-for="item in announcements" :key="item.id" class="admin-item single-column">
              <strong>{{ item.title }}</strong>
              <p>{{ announcementLevelLabel(item.level) }} · {{ item.published ? '前台展示' : '已隐藏' }} · {{ item.createdBy }} · {{ formatDateTime(item.createdAt) }}</p>
              <p>{{ item.content }}</p>
              <div class="inline-actions">
                <button type="button" @click="openAnnouncementModal(item)">编辑</button>
                <button type="button" class="ghost" @click="toggleAnnouncementPublished(item)">
                  {{ item.published ? '隐藏' : '展示' }}
                </button>
                <button type="button" class="danger" @click="deleteAnnouncement(item.id)">删除</button>
              </div>
            </article>
            <p v-if="!announcements.length" class="empty-row">暂无指南</p>
          </div>
        </div>
      </section>

      <section v-if="activeSection === 'reports'" class="admin-stack">
        <div class="admin-panel">
          <div class="panel-head">
            <h2>举报处理</h2>
          </div>
          <div class="admin-filter report-filter-row">
            <select v-model="reportStatus" @change="refreshReports">
              <option value="">全部</option>
              <option value="pending">待处理</option>
              <option value="processing">处理中</option>
              <option value="resolved">已处理</option>
              <option value="rejected">已驳回</option>
            </select>
          </div>
          <div class="bulk-bar">
            <label class="select-all">
              <input type="checkbox" :checked="isAllSelected('reports', reports)" @change="toggleAll('reports', reports, $event.target.checked)" />
              全选
            </label>
            <span>已选 {{ selectedCount('reports') }} 个</span>
            <button type="button" :disabled="!selectedCount('reports')" @click="openBatchReportModal('resolved')">批量处理</button>
            <button type="button" :disabled="!selectedCount('reports')" class="ghost" @click="openBatchReportModal('rejected')">批量驳回</button>
            <button type="button" class="ghost" :disabled="!selectedCount('reports')" @click="clearSelection('reports')">清空</button>
          </div>
          <div class="report-summary-row">
            <span>当前 {{ reports.length }} 条</span>
            <span>总计 {{ reportTotal }} 条</span>
            <span>第 {{ reportPage }} / {{ reportPageCount }} 页</span>
          </div>
          <article
            v-for="report in reports"
            :key="report.id"
            class="admin-item report-item"
            role="button"
            tabindex="0"
            @click="openReportTarget(report)"
            @keydown.enter.prevent="openReportTarget(report)"
            @keydown.space.prevent="openReportTarget(report)"
          >
            <input v-model="selected.reports" class="select-box" type="checkbox" :value="report.id" @click.stop @keydown.stop />
            <div class="report-content">
              <div class="report-title-row">
                <span class="report-section-chip">{{ reportTargetLabel(report) }}</span>
                <strong>{{ report.targetTitle || `${reportTargetLabel(report)} #${report.targetId}` }}</strong>
                <mark :class="{ danger: report.status === 'pending' }">{{ reportStatusLabel(report.status) }}</mark>
              </div>
              <p>{{ report.reporter || '匿名用户' }} · {{ reportReasonLabel(report.reason) }} · {{ formatDateTime(report.createdAt) }}</p>
              <dl class="report-detail-grid">
                <div>
                  <dt>举报板块</dt>
                  <dd>{{ reportTargetLabel(report) }}</dd>
                </div>
                <div>
                  <dt>对象编号</dt>
                  <dd>#{{ report.targetId || '-' }}</dd>
                </div>
                <div>
                  <dt>对象归属</dt>
                  <dd>{{ report.targetOwner || '无归属' }}</dd>
                </div>
                <div>
                  <dt>对象状态</dt>
                  <dd>{{ reportTargetStatusLabel(report.targetStatus) }}</dd>
                </div>
              </dl>
              <small v-if="report.targetSubtitle">对象摘要：{{ report.targetSubtitle }}</small>
              <small class="report-detail-text">举报说明：{{ report.detail || '暂无补充说明' }}</small>
              <small v-if="report.resolution">处理说明：{{ report.resolution }}</small>
              <small v-if="report.handler">处理人：{{ report.handler }} · {{ formatDateTime(report.handledAt) }}</small>
            </div>
            <div class="row-actions">
              <button v-if="reportTargetLink(report)" type="button" class="admin-link-btn" @click.stop="openReportTarget(report)">
                <span class="material-symbols-outlined">open_in_new</span>
                定位对象
              </button>
              <button type="button" class="ghost" @click.stop="openReportModal(report, 'processing')">处理中</button>
              <button type="button" class="danger" @click.stop="rejectReportedCombo(report)">驳回连招</button>
              <button type="button" class="danger" @click.stop="deleteReportedCombo(report)">删除连招</button>
              <button type="button" @click.stop="openReportModal(report, 'resolved')">标记处理</button>
              <button type="button" class="ghost" @click.stop="openReportModal(report, 'rejected')">驳回</button>
            </div>
          </article>
          <p v-if="!reports.length" class="empty-row">暂无举报</p>
          <div v-if="reportTotal > reportPageSize" class="pager-row admin-pager">
            <button type="button" :disabled="reportPage <= 1" @click="changeReportPage(reportPage - 1)">
              <span class="material-symbols-outlined">chevron_left</span>
              上一页
            </button>
            <span>{{ reportPage }} / {{ reportPageCount }}</span>
            <button type="button" :disabled="reportPage >= reportPageCount" @click="changeReportPage(reportPage + 1)">
              下一页
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </section>

      <section v-if="activeSection === 'feedbacks'" class="admin-stack">
        <div class="admin-panel">
          <div class="panel-head">
            <h2>反馈处理</h2>
          </div>
          <div class="admin-filter report-filter-row">
            <select v-model="feedbackStatus" @change="refreshFeedbacks">
              <option value="">全部</option>
              <option value="pending">待处理</option>
              <option value="processing">处理中</option>
              <option value="resolved">已处理</option>
              <option value="rejected">已驳回</option>
            </select>
          </div>
          <div class="bulk-bar">
            <label class="select-all">
              <input type="checkbox" :checked="isAllSelected('feedbacks', feedbacks)" @change="toggleAll('feedbacks', feedbacks, $event.target.checked)" />
              全选
            </label>
            <span>已选 {{ selectedCount('feedbacks') }} 个</span>
            <button type="button" :disabled="!selectedCount('feedbacks')" @click="openBatchFeedbackModal('resolved')">批量处理</button>
            <button type="button" :disabled="!selectedCount('feedbacks')" class="ghost" @click="openBatchFeedbackModal('rejected')">批量驳回</button>
            <button type="button" class="ghost" :disabled="!selectedCount('feedbacks')" @click="clearSelection('feedbacks')">清空</button>
          </div>
          <div class="report-summary-row">
            <span>当前 {{ feedbacks.length }} 条</span>
            <span>总计 {{ feedbackTotal }} 条</span>
            <span>第 {{ feedbackPage }} / {{ feedbackPageCount }} 页</span>
          </div>
          <article
            v-for="feedback in feedbacks"
            :key="feedback.id"
            class="admin-item report-item"
          >
            <input v-model="selected.feedbacks" class="select-box" type="checkbox" :value="feedback.id" @click.stop @keydown.stop />
            <div class="report-content">
              <div class="report-title-row">
                <span class="report-section-chip">反馈</span>
                <strong>{{ feedback.username || '匿名用户' }}</strong>
                <mark :class="{ danger: feedback.status === 'pending' }">{{ feedbackStatusLabel(feedback.status) }}</mark>
              </div>
              <p>{{ feedback.username || '匿名用户' }} · {{ feedbackReasonLabel(feedback.reason) }} · {{ formatDateTime(feedback.createdAt) }}</p>
              <small class="report-detail-text">反馈内容：{{ feedback.detail || '暂无反馈内容' }}</small>
              <small v-if="feedback.resolution">处理说明：{{ feedback.resolution }}</small>
              <small v-if="feedback.handler">处理人：{{ feedback.handler }} · {{ formatDateTime(feedback.handledAt) }}</small>
            </div>
            <div class="row-actions">
              <button type="button" class="ghost" @click.stop="openFeedbackDetailModal(feedback)">查看详情</button>
              <button type="button" class="ghost" @click.stop="openFeedbackModal(feedback, 'processing')">处理中</button>
              <button type="button" @click.stop="openFeedbackModal(feedback, 'resolved')">标记处理</button>
              <button type="button" class="ghost" @click.stop="openFeedbackModal(feedback, 'rejected')">驳回</button>
            </div>
          </article>
          <p v-if="!feedbacks.length" class="empty-row">暂无反馈</p>
          <div v-if="feedbackTotal > feedbackPageSize" class="pager-row admin-pager">
            <button type="button" :disabled="feedbackPage <= 1" @click="changeFeedbackPage(feedbackPage - 1)">
              <span class="material-symbols-outlined">chevron_left</span>
              上一页
            </button>
            <span>{{ feedbackPage }} / {{ feedbackPageCount }}</span>
            <button type="button" :disabled="feedbackPage >= feedbackPageCount" @click="changeFeedbackPage(feedbackPage + 1)">
              下一页
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </section>

      <section v-if="activeSection === 'audit'" class="admin-stack">
        <div class="admin-panel">
          <div class="panel-head">
            <h2>审计日志</h2>
            <button type="button" class="ghost" @click="refreshAuditLogs">刷新</button>
          </div>
          <div class="admin-filter report-filter-row audit-filter-row">
            <select v-model="auditTargetType" @change="refreshAuditLogs">
              <option v-for="option in auditTargetTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
            <select v-model="auditAction" @change="refreshAuditLogs">
              <option v-for="option in auditActionOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
            <input v-model="auditStartDate" class="audit-date-input" type="date" aria-label="开始日期" @change="refreshAuditLogs" />
            <input v-model="auditEndDate" class="audit-date-input" type="date" aria-label="结束日期" @change="refreshAuditLogs" />
            <button type="button" class="ghost" :disabled="!hasAuditTimeFilter" @click="clearAuditTimeFilter">清空时间</button>
          </div>
          <div class="report-summary-row">
            <span>当前 {{ auditLogs.length }} 条</span>
            <span>总计 {{ auditTotal }} 条</span>
            <span>第 {{ auditPage }} / {{ auditPageCount }} 页</span>
          </div>
          <div class="admin-list-grid">
            <article v-for="log in auditLogs" :key="log.id" :class="['admin-item', 'single-column', { 'audit-risk': isHighRiskAudit(log) }]">
              <strong>{{ log.adminName || 'system' }} · {{ auditActionLabel(log.action) }}</strong>
              <p>{{ log.targetType }} #{{ log.targetId || '-' }} · {{ formatDateTime(log.createdAt) }}</p>
              <small>{{ log.detail || '无详情' }}</small>
              <div class="inline-actions">
                <button type="button" class="ghost" :disabled="!canOpenAuditTarget(log)" @click="openAuditTarget(log)">定位对象</button>
                <mark v-if="isHighRiskAudit(log)">高风险</mark>
              </div>
            </article>
            <p v-if="!auditLogs.length" class="empty-row">暂无审计日志</p>
          </div>
          <div v-if="auditTotal > auditPageSize" class="pager-row admin-pager">
            <button type="button" :disabled="auditPage <= 1" @click="changeAuditPage(auditPage - 1)">
              <span class="material-symbols-outlined">chevron_left</span>
              上一页
            </button>
            <span>{{ auditPage }} / {{ auditPageCount }}</span>
            <button type="button" :disabled="auditPage >= auditPageCount" @click="changeAuditPage(auditPage + 1)">
              下一页
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </section>
    </main>

    <AdminModal v-if="adminModal === 'user-ban'" :title="banForm.banned ? (banForm.batch ? '批量封禁用户' : '封禁用户') : (banForm.batch ? '批量解除封禁' : '解除封禁')" :subtitle="banModalSubtitle" :tone="banForm.banned ? 'danger' : 'default'" @close="closeAdminModal">
      <form class="modal-form" @submit.prevent="submitUserBan">
        <div class="modal-field-grid ban-form-grid">
          <label v-if="banForm.banned" class="modal-field-row">
            <span>封禁理由，用户模块会展示这条记录</span>
            <textarea v-model.trim="banForm.reason" :required="banForm.banned" rows="4"></textarea>
          </label>
          <label v-if="banForm.banned" class="modal-field-row">
            <span>封禁天数，0 为永久</span>
            <input v-model.number="banForm.days" type="number" min="0" />
          </label>
          <p v-if="banForm.banned" class="form-hint">封禁天数填 0 表示永久封禁。</p>
          <p v-if="!banForm.banned" class="form-hint">确认后会清空该用户的封禁理由和到期时间。</p>
        </div>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeAdminModal">取消</button>
          <button class="admin-primary" :class="{ danger: banForm.banned }" type="submit">{{ banForm.banned ? '确认封禁' : '确认解封' }}</button>
        </div>
      </form>
    </AdminModal>

    <AdminModal v-if="adminModal === 'user-manage'" title="管理用户" :subtitle="activeUser ? `${activeUser.username} · ${activeUser.email || '未绑定邮箱'}` : ''" @close="closeAdminModal">
      <form class="modal-form" @submit.prevent="submitUserManage">
        <div class="modal-field-grid">
          <label class="modal-field-row">
            <span>用户角色</span>
            <select v-model="userManageForm.role" :disabled="activeUser && isCurrentAdmin(activeUser)">
              <option value="user">普通用户</option>
              <option value="admin">管理员</option>
            </select>
          </label>
          <div class="modal-field-row is-check">
            <span>后台权限</span>
            <label class="check-line modal-check">
              <input v-model="userManageForm.canReviewCombos" type="checkbox" :disabled="activeUser && isCurrentAdmin(activeUser)" />
              连招审核管理
            </label>
          </div>
        </div>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeAdminModal">取消</button>
          <button class="admin-primary" type="submit">保存用户</button>
        </div>
      </form>
    </AdminModal>

    <AdminModal v-if="adminModal === 'character'" :title="editingCharacterId ? '编辑角色' : '新增角色'" @close="closeAdminModal">
      <form class="modal-form" @submit.prevent="saveCharacter">
        <div class="modal-field-grid">
          <label class="modal-field-row">
            <span>角色名，例如 CHUN-LI</span>
            <input v-model.trim="characterForm.name" required />
          </label>
          <label class="modal-field-row">
            <span>角色定位，例如 charge / grappler</span>
            <input v-model.trim="characterForm.archetype" />
          </label>
          <div class="modal-field-row">
            <span>角色头像</span>
            <div class="modal-field-stack">
              <div class="character-avatar-uploader">
                <div class="character-avatar-preview">
                  <img v-if="characterForm.avatar" :src="characterForm.avatar" :alt="`${characterForm.name || '角色'}头像预览`" />
                  <span v-else class="material-symbols-outlined">image</span>
                </div>
                <div class="character-avatar-controls">
                  <label :class="['admin-video-upload', 'character-avatar-upload', { uploading: uploadingCharacterAvatar }]">
                    <input type="file" accept="image/jpeg,image/png,image/webp" class="hidden" :disabled="uploadingCharacterAvatar" @change="handleCharacterAvatarUpload" />
                    <span class="material-symbols-outlined">upload</span>
                    <span>{{ characterAvatarUploadText }}</span>
                  </label>
                  <button v-if="characterForm.avatar" type="button" class="ghost" :disabled="uploadingCharacterAvatar" @click="clearCharacterAvatar">
                    <span class="material-symbols-outlined">delete</span>
                    清空头像
                  </button>
                </div>
              </div>
              <div v-if="uploadingCharacterAvatar" class="admin-upload-progress" role="progressbar" :aria-valuenow="characterAvatarUploadProgress" aria-valuemin="0" aria-valuemax="100">
                <span :style="{ width: `${characterAvatarUploadProgress}%` }"></span>
              </div>
              <div v-if="avatarCrop.active" class="avatar-crop-panel">
                <div
                  ref="avatarCropStageRef"
                  class="avatar-crop-stage"
                  @pointerdown="startAvatarCropDrag"
                  @pointermove="moveAvatarCropDrag"
                  @pointerup="stopAvatarCropDrag"
                  @pointercancel="stopAvatarCropDrag"
                  @lostpointercapture="stopAvatarCropDrag"
                >
                  <img :src="avatarCrop.url" alt="角色头像裁剪预览" :style="avatarCropImageStyle" draggable="false" />
                  <div class="avatar-crop-box" :style="avatarCropFrameStyle">
                    <button
                      type="button"
                      class="avatar-crop-move-handle"
                      aria-label="移动裁剪框"
                      title="拖动裁剪框"
                      @pointerdown.stop.prevent="startAvatarCropFrameMove"
                    >
                      <span class="material-symbols-outlined">open_with</span>
                    </button>
                    <button
                      v-for="corner in avatarCropResizeCorners"
                      :key="corner"
                      type="button"
                      :class="['avatar-crop-resize-handle', `corner-${corner}`]"
                      :aria-label="`调整裁剪框${corner.toUpperCase()}角`"
                      @pointerdown.stop.prevent="startAvatarCropFrameResize($event, corner)"
                    ></button>
                  </div>
                </div>
                <div class="avatar-crop-controls">
                  <div class="avatar-crop-control-group">
                    <div class="avatar-crop-control-heading">
                      <span>缩放</span>
                      <output>{{ avatarCropScalePercent }}%</output>
                    </div>
                    <div class="avatar-crop-scale-row">
                      <input v-model.number="avatarCropScalePercent" type="range" min="100" max="500" step="1" aria-label="图片缩放百分比" />
                      <label class="avatar-crop-percent-input">
                        <input v-model.number="avatarCropScalePercent" type="number" min="100" max="500" step="1" aria-label="输入图片缩放百分比" />
                        <span>%</span>
                      </label>
                    </div>
                  </div>
                  <div class="avatar-crop-control-group">
                    <div class="avatar-crop-control-heading">
                      <span>裁剪框</span>
                      <output>{{ Math.round(avatarCrop.frameWidth) }} × {{ Math.round(avatarCrop.frameHeight) }} px</output>
                    </div>
                    <div class="avatar-crop-presets" aria-label="裁剪框比例">
                      <button
                        v-for="preset in avatarCropAspectPresets"
                        :key="preset.value"
                        type="button"
                        :class="['crop-preset', { active: avatarCrop.aspect === preset.value }]"
                        @click="setAvatarCropAspect(preset.value)"
                      >{{ preset.label }}</button>
                    </div>
                    <div class="avatar-crop-size-row">
                      <label>
                        <span>宽</span>
                        <input type="number" min="80" :max="avatarCropStageSize" :value="Math.round(avatarCrop.frameWidth)" @change="setAvatarCropFrameDimension('width', $event.target.value)" />
                      </label>
                      <label>
                        <span>高</span>
                        <input type="number" min="80" :max="avatarCropStageSize" :value="Math.round(avatarCrop.frameHeight)" @change="setAvatarCropFrameDimension('height', $event.target.value)" />
                      </label>
                    </div>
                    <p class="avatar-crop-help">拖动图片调整构图；拖动裁剪框顶部可移动，拖动四角可自由缩放。</p>
                  </div>
                  <div class="avatar-crop-actions">
                    <button type="button" class="ghost" :disabled="uploadingCharacterAvatar" @click="resetAvatarCrop">重置构图</button>
                    <button type="button" class="ghost" :disabled="uploadingCharacterAvatar" @click="cancelAvatarCrop">取消裁剪</button>
                    <button type="button" class="admin-primary" :disabled="uploadingCharacterAvatar" @click="uploadCroppedCharacterAvatar">确认并上传</button>
                  </div>
                </div>
              </div>
              <p v-if="characterForm.avatar" class="form-hint">{{ characterForm.avatar }}</p>
            </div>
          </div>
          <label class="modal-field-row">
            <span>角色描述</span>
            <textarea v-model.trim="characterForm.description"></textarea>
          </label>
          <section class="character-data-editor" aria-labelledby="character-data-editor-title">
            <div class="character-data-editor-head">
              <div>
                <span class="material-symbols-outlined" aria-hidden="true">database</span>
                <div>
                  <h3 id="character-data-editor-title">角色基础数据</h3>
                  <p>创建角色时同步生成，可保留复合数值，例如 4+38+3 或 1.2 (1.86)。</p>
                </div>
              </div>
              <span>{{ CHARACTER_DATA_FIELDS.length }} 项</span>
            </div>
            <div class="character-data-form-grid">
              <label v-for="field in CHARACTER_DATA_FIELDS" :key="field.key">
                <span>{{ field.label }} <small>{{ field.sourceLabel }}</small></span>
                <input v-model.trim="characterForm.characterData[field.key]" :placeholder="field.placeholder" maxlength="40" />
              </label>
            </div>
            <div class="character-data-source-fields">
              <label>
                <span>数据来源名称</span>
                <input v-model.trim="characterForm.characterData.sourceName" placeholder="例如 SuperCombo / FAT" maxlength="80" />
              </label>
              <label>
                <span>数据来源链接</span>
                <input v-model.trim="characterForm.characterData.sourceUrl" type="url" placeholder="https://..." maxlength="500" />
              </label>
            </div>
          </section>
        </div>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeAdminModal">取消</button>
          <button class="admin-primary" type="submit">{{ editingCharacterId ? '保存角色' : '添加角色' }}</button>
        </div>
      </form>
    </AdminModal>

    <AdminModal v-if="adminModal === 'frame'" :title="editingFrameId ? '编辑帧数' : '新增帧数'" @close="closeAdminModal">
      <form class="modal-form" @submit.prevent="saveFrame">
        <div class="modal-field-grid">
          <label class="modal-field-row">
            <span>角色</span>
            <select v-model="frameForm.characterId" required>
              <option disabled value="">选择角色</option>
              <option v-for="c in characters" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </label>
          <label class="modal-field-row">
            <span>招式名，例如 5MP；TC 用 - 连接，例如 LP-MP-HP</span>
            <input v-model.trim="frameForm.moveName" required />
          </label>
          <label class="modal-field-row">
            <span>启动，例如 4</span>
            <input v-model.trim="frameForm.startup" />
          </label>
          <label class="modal-field-row">
            <span>持续，例如 4-6</span>
            <input v-model.trim="frameForm.active" />
          </label>
          <label class="modal-field-row">
            <span>恢复，例如 46 total frames</span>
            <input v-model.trim="frameForm.recovery" />
          </label>
          <label class="modal-field-row">
            <span>防御硬直差，例如 -2</span>
            <input v-model.trim="frameForm.onBlock" />
          </label>
          <label class="modal-field-row">
            <span>命中硬直差，例如 +4</span>
            <input v-model.trim="frameForm.onHit" />
          </label>
          <label class="modal-field-row">
            <span>取消，例如 C / SA / *</span>
            <input v-model.trim="frameForm.cancel" />
          </label>
          <label class="modal-field-row">
            <span>伤害，例如 300</span>
            <input v-model.trim="frameForm.damage" />
          </label>
          <label class="modal-field-row">
            <span>属性，例如 High / Low</span>
            <input v-model.trim="frameForm.properties" />
          </label>
          <label class="modal-field-row">
            <span>显示顺序</span>
            <input v-model.number="frameForm.displayOrder" type="number" />
          </label>
          <label class="modal-field-row">
            <span>命中 Drive 增加</span>
            <input v-model.trim="frameForm.driveGainOnHit" />
          </label>
          <label class="modal-field-row">
            <span>防御 Drive 减少</span>
            <input v-model.trim="frameForm.driveLossOnBlock" />
          </label>
          <label class="modal-field-row">
            <span>Punish Counter Drive 减少</span>
            <input v-model.trim="frameForm.driveLossOnPunishCounter" />
          </label>
          <label class="modal-field-row">
            <span>SA 增加</span>
            <input v-model.trim="frameForm.superArtGain" />
          </label>
          <label class="modal-field-row">
            <span>来源角色 slug，例如 mai</span>
            <input v-model.trim="frameForm.sourceCharacterSlug" />
          </label>
          <label class="modal-field-row">
            <span>来源语言，例如 zh-hant</span>
            <input v-model.trim="frameForm.sourceLang" />
          </label>
          <label class="modal-field-row">
            <span>来源 URL</span>
            <input v-model.trim="frameForm.sourceUrl" />
          </label>
          <label class="modal-field-row">
            <span>连段补正说明</span>
            <textarea v-model.trim="frameForm.comboScaling" rows="2"></textarea>
          </label>
          <label class="modal-field-row">
            <span>备注</span>
            <textarea v-model.trim="frameForm.miscellaneous" rows="2"></textarea>
          </label>
        </div>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeAdminModal">取消</button>
          <button class="admin-primary" type="submit">{{ editingFrameId ? '保存帧数' : '添加帧数' }}</button>
        </div>
      </form>
    </AdminModal>

    <AdminModal v-if="adminModal === 'combo'" :title="editingComboId ? '编辑连招' : '新增连招'" @close="closeAdminModal">
      <form class="modal-form" @submit.prevent="saveCombo">
        <div class="modal-field-grid">
          <label v-if="comboForm.controlType !== 'world-tour'" class="modal-field-row">
            <span>角色</span>
            <select v-model="comboForm.characterId" required>
              <option disabled value="">选择角色</option>
              <option v-for="c in characters" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </label>
          <div class="modal-field-row">
            <span>连招类型</span>
            <div class="admin-segmented-control" role="radiogroup" aria-label="连招类型">
              <button
                v-for="option in comboControlOptions"
                :key="option.value"
                type="button"
                :class="['admin-segmented-option', `is-${option.value}`, { active: comboForm.controlType === option.value }]"
                :aria-checked="comboForm.controlType === option.value"
                role="radio"
                @click="setComboControlType(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
          <label class="modal-field-row">
            <span>后续关系，可选一个同角色原连招</span>
            <select v-model="comboForm.followupParentId" :disabled="adminParentCombosLoading">
              <option value="">普通连招，不关联原连招</option>
              <option v-if="adminCurrentParentUnavailable" :value="comboForm.followupParentId" disabled>
                当前关联 #{{ comboForm.followupParentId }}（已不可用，请重新选择）
              </option>
              <option v-for="option in adminParentComboOptions" :key="option.id" :value="option.id">
                {{ comboControlLabel(option.controlType) }} / {{ option.route || option.comboText || option.starter }}
              </option>
            </select>
            <small v-if="adminParentCombosLoading">正在加载该角色全部原连招...</small>
            <small v-else-if="adminParentCombosError" class="field-error">{{ adminParentCombosError }}</small>
          </label>
          <div class="modal-field-row">
            <span>连招路线</span>
            <div class="combo-route-builder">
              <div class="combo-route-head">
                <span>按顺序填写每一招</span>
                <button type="button" class="route-add" aria-label="增加一个招式" title="增加一个招式" @click="addRouteMove">
                  <span class="material-symbols-outlined">add</span>
                </button>
              </div>
              <div class="route-move-list">
                <template v-for="(move, index) in routeMoves" :key="index">
                  <div class="route-move-field">
                    <span>第 {{ index + 1 }} 招，例如 {{ routeMoveExamples[index] || '236HP' }}</span>
                    <div class="route-move-controls">
                      <select
                        v-if="comboForm.controlType === 'world-tour'"
                        v-model="routeCharacterIds[index]"
                        class="route-character-select"
                        :aria-label="`第 ${index + 1} 招角色`"
                        required
                      >
                        <option disabled value="">选择角色</option>
                        <option v-for="character in characters" :key="character.id" :value="character.id">{{ character.name }}</option>
                      </select>
                      <input
                        v-model.trim="routeMoves[index]"
                        :ref="el => setRouteMoveInput(el, index)"
                        class="route-move-input"
                        :aria-label="`第 ${index + 1} 招`"
                        @keydown.enter.prevent="handleRouteMoveEnter(index)"
                      />
                      <button
                        type="button"
                        class="route-remove"
                        :disabled="routeMoves.length <= 1"
                        :aria-label="`删除第 ${index + 1} 招`"
                        :title="routeMoves.length <= 1 ? '至少保留一招' : `删除第 ${index + 1} 招`"
                        @click="removeRouteMove(index)"
                      >
                        <span class="material-symbols-outlined" aria-hidden="true">delete</span>
                      </button>
                    </div>
                  </div>
                  <span v-if="index < routeMoves.length - 1" class="route-separator">></span>
                </template>
              </div>
              <p class="route-preview">保存为：{{ mergedRoute || '请先填写第 1 招' }}</p>
            </div>
          </div>
          <label class="modal-field-row">
            <span>伤害（必填）</span>
            <input v-model.number="comboForm.damage" type="number" min="0" required :aria-invalid="comboForm.damage === ''" />
          </label>
          <label class="modal-field-row">
            <span>Drive 消耗</span>
            <input v-model.number="comboForm.driveCost" type="number" min="0" max="6" step="0.1" />
          </label>
          <label class="modal-field-row">
            <span>SA 消耗</span>
            <input v-model.number="comboForm.saCost" type="number" min="0" />
          </label>
          <label class="modal-field-row">
            <span>有利帧数（必填），例如 +2</span>
            <input v-model.trim="comboForm.advantageFrames" required :aria-invalid="!comboForm.advantageFrames" />
          </label>
          <label class="modal-field-row">
            <span>难度（必填）</span>
            <select v-model="comboForm.difficulty" required :aria-invalid="!comboForm.difficulty">
              <option disabled value="">选择难度</option>
              <option value="简单">简单</option>
              <option value="中等">中等</option>
              <option value="困难">困难</option>
            </select>
          </label>
          <div v-if="!adminPressureMode" class="modal-field-row">
            <span>连招标签</span>
            <div class="admin-tag-picker">
              <label v-for="option in comboTagOptions" :key="option.value" class="admin-tag-option">
                <input
                  v-model="comboForm.tags"
                  type="checkbox"
                  :value="option.value"
                  :disabled="!comboForm.tags.includes(option.value) && comboForm.tags.length >= MAX_COMBO_TAGS"
                />
                <span>{{ option.label }}</span>
              </label>
              <p class="tag-limit-hint">已选择 {{ comboForm.tags.length }} / {{ MAX_COMBO_TAGS }} 个标签</p>
            </div>
          </div>
          <div v-else class="modal-field-row">
            <span>压制类型（必选）</span>
            <div class="admin-tag-picker" role="radiogroup" aria-label="压制类型">
              <label v-for="option in pressureTypeOptions" :key="option.value" class="admin-tag-option">
                <input v-model="comboForm.pressureType" type="radio" name="admin-pressure-type" :value="option.value" />
                <span>{{ option.label }}</span>
              </label>
              <p class="tag-limit-hint">选择这段路线主要承担的压制目的。</p>
            </div>
          </div>
          <div class="modal-field-row">
            <span>演示视频（必填）</span>
            <div class="modal-field-stack">
              <label :class="['admin-video-upload', { uploading: uploadingComboVideo }]">
                <input type="file" accept="video/*" class="hidden" :disabled="uploadingComboVideo" @change="handleComboVideoUpload" />
                <span class="material-symbols-outlined">movie</span>
                <span>{{ comboVideoUploadText }}</span>
              </label>
              <div v-if="uploadingComboVideo" class="admin-upload-progress" role="progressbar" :aria-valuenow="comboVideoUploadProgress" aria-valuemin="0" aria-valuemax="100">
                <span :style="{ width: `${comboVideoUploadProgress}%` }"></span>
              </div>
              <video v-if="comboForm.videoUrl" class="admin-video-preview" :src="comboForm.videoUrl" controls preload="metadata"></video>
            </div>
          </div>
          <label class="modal-field-row">
            <span>训练要点、关键延迟、取消时机或容易失误的位置</span>
            <textarea v-model.trim="comboForm.trainingNotes" class="combo-training-notes"></textarea>
          </label>
        </div>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeAdminModal">取消</button>
          <button class="admin-primary" type="submit">{{ editingComboId ? '保存连招' : '添加连招' }}</button>
        </div>
      </form>
    </AdminModal>

    <AdminModal v-if="adminModal === 'combo-review'" title="审核连招" :subtitle="activeReviewCombo ? `${characterName(activeReviewCombo.characterId)} · ${activeReviewCombo.starter}` : ''" class="combo-review-modal-shell" @close="closeAdminModal">
      <div v-if="activeReviewCombo" class="review-panel modal-review-panel">
        <div class="combo-review-modal-grid">
          <div class="combo-review-modal-content">
            <section class="combo-review-hero">
              <div class="combo-review-title">
                <span :class="['combo-review-status', activeReviewCombo.status || 'pending']">{{ comboReviewStatusLabel(activeReviewCombo.status) }}</span>
                <strong>{{ activeReviewCombo.route || activeReviewCombo.comboText || activeReviewCombo.starter }}</strong>
                <p>{{ characterName(activeReviewCombo.characterId) }} · {{ activeReviewCombo.starter || '未填写起手' }} · #{{ activeReviewCombo.id }}</p>
              </div>
              <div class="combo-review-scoreboard" aria-label="连招关键数据">
                <div><strong>{{ activeReviewCombo.damage || 0 }}</strong><span>伤害</span></div>
                <div><strong>{{ activeReviewCombo.driveCost ?? 0 }}</strong><span>Drive</span></div>
                <div><strong>{{ activeReviewCombo.saCost ?? 0 }}</strong><span>SA</span></div>
                <div><strong>{{ activeReviewCombo.difficulty || '未标' }}</strong><span>难度</span></div>
              </div>
            </section>

            <section class="combo-review-notation-card" aria-label="图标化连招">
              <div class="review-section-head">
                <span class="material-symbols-outlined">sports_martial_arts</span>
                <strong>图标连招</strong>
                <small>{{ reviewRouteMoveCount }} 段</small>
              </div>
              <WorldTourComboRoute
                v-if="comboMatchesControlType(activeReviewCombo, 'world-tour')"
                :route="activeReviewCombo.route || activeReviewCombo.comboText || ''"
                :character-ids="activeReviewCombo.routeCharacterIds"
                :characters="characters"
                size="lg"
              />
              <ComboNotation v-else :route="activeReviewCombo.route || activeReviewCombo.comboText || ''" size="lg" />
              <p class="combo-review-raw-route">{{ activeReviewCombo.route || activeReviewCombo.comboText || '-' }}</p>
            </section>

            <section v-if="activeReviewCombo.followupParent" class="combo-parent-panel">
              <div class="review-section-head">
                <span class="material-symbols-outlined">account_tree</span>
                <strong>父连招</strong>
                <router-link :to="`/combos/${activeReviewCombo.followupParent.id}`">查看前台</router-link>
              </div>
              <strong>{{ activeReviewCombo.followupParent.starter }}</strong>
              <ComboNotation :route="activeReviewCombo.followupParent.route || activeReviewCombo.followupParent.comboText || ''" size="sm" />
              <p>{{ activeReviewCombo.followupParent.route }}</p>
            </section>

            <div class="combo-review-workbench">
              <div v-if="activeReviewCombo.videoUrl" class="review-video-card">
                <div class="review-section-head">
                  <span class="material-symbols-outlined">play_circle</span>
                  <strong>投稿视频</strong>
                  <a :href="activeReviewCombo.videoUrl" target="_blank" rel="noopener">新窗口打开</a>
                </div>
                <video class="review-video" :src="activeReviewCombo.videoUrl" controls preload="metadata"></video>
              </div>
              <div v-else class="review-video-empty">
                <span class="material-symbols-outlined">videocam_off</span>
                <strong>该连招没有上传视频</strong>
                <p>可以先审核资料，也可以要求投稿者补充视频。</p>
              </div>

              <section class="combo-review-notes inline-notes">
                <div v-if="activeReviewCombo.trainingNotes"><span>练习备注</span><p>{{ activeReviewCombo.trainingNotes }}</p></div>
                <div v-if="activeReviewCombo.difficultyNote"><span>当前难度备注</span><p>{{ activeReviewCombo.difficultyNote }}</p></div>
                <div v-if="activeReviewCombo.rejectionReason"><span>历史驳回原因</span><p>{{ activeReviewCombo.rejectionReason }}</p></div>
                <div><span>视频状态</span><p>{{ videoReviewStatusLabel(activeReviewCombo.videoReviewStatus) }} {{ activeReviewCombo.videoReviewReason || '' }}</p></div>
              </section>
            </div>

            <div v-if="duplicateCheckLoading || duplicateCombos.length" class="duplicate-list" aria-live="polite">
              <p v-if="duplicateCheckLoading" class="duplicate-loading">正在自动检查重复路线...</p>
              <article v-for="candidate in duplicateCombos" :key="candidate.combo.id">
                <div class="duplicate-item-head">
                  <strong>#{{ candidate.combo.id }} {{ characterName(candidate.combo.characterId) }} · {{ candidate.combo.starter }}</strong>
                  <span :class="['duplicate-match-pill', candidate.matchType]">{{ duplicateMatchLabel(candidate.matchType) }} · {{ candidate.similarity }}%</span>
                </div>
                <WorldTourComboRoute
                  v-if="comboMatchesControlType(candidate.combo, 'world-tour')"
                  :route="candidate.combo.route || candidate.combo.comboText || ''"
                  :character-ids="candidate.combo.routeCharacterIds"
                  :characters="characters"
                  size="sm"
                />
                <ComboNotation v-else :route="candidate.combo.route || candidate.combo.comboText || ''" size="sm" />
                <p>{{ candidate.combo.damage || 0 }} 伤害 · {{ comboReviewStatusLabel(candidate.combo.status) }} · {{ candidate.combo.difficulty || '未标难度' }}</p>
                <div class="duplicate-item-actions">
                  <router-link :to="`/combos/${candidate.combo.id}`">查看已有连招</router-link>
                  <button v-if="candidate.matchType === 'exact'" type="button" class="danger" @click="rejectAsDuplicate(candidate)">按重复驳回</button>
                </div>
              </article>
            </div>
          </div>

          <aside class="combo-review-actions modal-actions-panel">
            <h3>审核动作</h3>
            <div class="action-button-grid">
              <button type="button" class="admin-primary" :disabled="duplicateCheckExact && !duplicateCheckOverridden" @click="submitComboReview('approved')">通过</button>
              <button type="button" class="danger" @click="submitComboReview('rejected')">驳回</button>
              <button type="button" @click="markComboManualReview">标记待处理</button>
              <button type="button" @click="rejectComboVideo">视频违规</button>
              <button type="button" :disabled="duplicateCheckLoading" @click="loadDuplicateCombos(activeReviewCombo)">重新检测重复</button>
              <button v-if="duplicateCheckExact && !duplicateCheckOverridden" type="button" @click="confirmNotDuplicate">确认不是重复</button>
              <button type="button" @click="openComboModal(activeReviewCombo)">编辑数据</button>
              <router-link class="admin-link-btn" :to="`/combos/${activeReviewCombo.id}`">
                <span class="material-symbols-outlined">open_in_new</span>
                前台详情
              </router-link>
              <button type="button" class="danger" @click="deleteCombo(activeReviewCombo.id)">删除</button>
            </div>
            <label class="review-action-note">
              <span>驳回/处理说明</span>
              <textarea v-model.trim="reviewForm.rejectionReason" rows="5"></textarea>
            </label>
            <div class="review-action-calibration">
              <label>
                <span>难度校准</span>
                <select v-model="reviewForm.difficulty">
                  <option value="">保持原难度</option>
                  <option value="简单">简单</option>
                  <option value="中等">中等</option>
                  <option value="困难">困难</option>
                </select>
              </label>
              <label>
                <span>难度备注</span>
                <input v-model.trim="reviewForm.difficultyNote" placeholder="例如 需要 1F 目押" />
              </label>
            </div>
            <div class="review-checks">
              <span :class="['check-pill', { danger: duplicateCheckExact && !duplicateCheckOverridden }]">
                {{ duplicateCheckLoading ? '重复检测中' : `重复候选 ${duplicateCombos.length} 条` }}
              </span>
            </div>
          </aside>
        </div>
      </div>
    </AdminModal>

    <AdminModal v-if="adminModal === 'announcement'" :title="editingAnnouncementId ? '编辑指南' : '新增指南'" @close="closeAdminModal">
      <form class="modal-form" @submit.prevent="sendAnnouncement">
        <div class="modal-field-grid">
          <label class="modal-field-row">
            <span>指南标题</span>
            <input v-model.trim="announcementForm.title" required />
          </label>
          <label class="modal-field-row">
            <span>指南类型</span>
            <select v-model="announcementForm.level">
              <option value="info">基础指南</option>
              <option value="warning">重点指南</option>
              <option value="maintenance">规则说明</option>
            </select>
          </label>
          <label class="modal-field-row">
            <span>展示状态</span>
            <select v-model="announcementForm.published">
              <option :value="true">前台展示</option>
              <option :value="false">暂存/隐藏</option>
            </select>
          </label>
          <label class="modal-field-row">
            <span>指南正文，前台列表只展示开头，点击卡片查看全文</span>
            <textarea v-model.trim="announcementForm.content" required rows="8"></textarea>
          </label>
        </div>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeAdminModal">取消</button>
          <button class="admin-primary" type="submit">{{ editingAnnouncementId ? '保存指南' : '保存指南' }}</button>
        </div>
      </form>
    </AdminModal>

    <AdminModal v-if="adminModal === 'report'" :title="reportActionForm.status === 'resolved' ? (reportActionForm.batch ? '批量处理举报' : '处理举报') : (reportActionForm.batch ? '批量驳回举报' : '驳回举报')" :subtitle="reportModalSubtitle" @close="closeAdminModal">
      <form class="modal-form" @submit.prevent="submitReportAction">
        <div class="modal-field-grid">
          <label class="modal-field-row">
            <span>处理说明，留空则使用默认文案</span>
            <textarea v-model.trim="reportActionForm.resolution"></textarea>
          </label>
        </div>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeAdminModal">取消</button>
          <button class="admin-primary" type="submit">{{ reportActionForm.status === 'resolved' ? '确认处理' : '确认驳回' }}</button>
        </div>
      </form>
    </AdminModal>

    <AdminModal v-if="adminModal === 'feedback'" :title="feedbackActionForm.status === 'resolved' ? (feedbackActionForm.batch ? '批量处理反馈' : '处理反馈') : (feedbackActionForm.batch ? '批量驳回反馈' : '驳回反馈')" :subtitle="feedbackModalSubtitle" @close="closeAdminModal">
      <form class="modal-form" @submit.prevent="submitFeedbackAction">
        <div class="modal-field-grid">
          <label class="modal-field-row">
            <span>处理说明，留空则使用默认文案</span>
            <textarea v-model.trim="feedbackActionForm.resolution"></textarea>
          </label>
        </div>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeAdminModal">取消</button>
          <button class="admin-primary" type="submit">{{ feedbackActionForm.status === 'resolved' ? '确认处理' : '确认驳回' }}</button>
        </div>
      </form>
    </AdminModal>

    <AdminModal v-if="adminModal === 'feedback-detail'" title="反馈详情" :subtitle="feedbackModalSubtitle" @close="closeAdminModal">
      <div v-if="activeFeedback" class="feedback-detail-modal">
        <dl class="report-detail-grid">
          <div>
            <dt>提交用户</dt>
            <dd>{{ activeFeedback.username || '匿名用户' }}</dd>
          </div>
          <div>
            <dt>反馈类型</dt>
            <dd>{{ feedbackReasonLabel(activeFeedback.reason) }}</dd>
          </div>
          <div>
            <dt>处理状态</dt>
            <dd>{{ feedbackStatusLabel(activeFeedback.status) }}</dd>
          </div>
          <div>
            <dt>提交时间</dt>
            <dd>{{ formatDateTime(activeFeedback.createdAt) }}</dd>
          </div>
        </dl>
        <section class="feedback-detail-block">
          <span>反馈内容</span>
          <p>{{ activeFeedback.detail || '暂无反馈内容' }}</p>
        </section>
        <section class="feedback-detail-block">
          <span>处理记录</span>
          <p>{{ activeFeedback.resolution || '暂无处理说明' }}</p>
          <small v-if="activeFeedback.handler">处理人：{{ activeFeedback.handler }} · {{ formatDateTime(activeFeedback.handledAt) }}</small>
        </section>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="closeAdminModal">关闭</button>
        </div>
      </div>
    </AdminModal>

    <Teleport to="body">
      <Transition name="admin-toast">
        <div
          v-if="message"
          class="admin-toast-layer"
          :role="messageType === 'error' ? 'alert' : 'status'"
          :aria-live="messageType === 'error' ? 'assertive' : 'polite'"
          aria-atomic="true"
        >
          <div :class="['admin-message', messageType]">
            <span class="material-symbols-outlined" aria-hidden="true">{{ messageType === 'error' ? 'error' : 'check_circle' }}</span>
            <span class="admin-message-copy">{{ message }}</span>
            <button type="button" class="admin-message-close" aria-label="关闭提示" title="关闭提示" @click="dismissMessage">
              <span class="material-symbols-outlined" aria-hidden="true">close</span>
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>

  <div v-else class="admin-login-screen">
    <section class="admin-login-card" aria-labelledby="admin-login-title">
      <router-link to="/" class="admin-login-back">
        <span class="material-symbols-outlined">arrow_back</span>
        返回前台
      </router-link>
      <div class="admin-login-head">
        <span class="material-symbols-outlined">admin_panel_settings</span>
        <p>ADMIN SYSTEM</p>
        <h1 id="admin-login-title">后台管理登录</h1>
        <small>请使用管理员账号或拥有后台权限的账号登录。</small>
      </div>
      <form class="admin-login-form" @submit.prevent="handleAdminLogin">
        <label>
          <span>邮箱</span>
          <input v-model.trim="adminLoginForm.email" type="email" autocomplete="username" placeholder="请输入后台邮箱号" required />
        </label>
        <label>
          <span>密码</span>
          <input v-model="adminLoginForm.password" type="password" autocomplete="current-password" placeholder="请输入后台密码" required />
        </label>
        <p v-if="adminLoginError" class="admin-login-error">{{ adminLoginError }}</p>
        <button type="submit" class="admin-primary" :disabled="adminLoginLoading">
          <span class="material-symbols-outlined">login</span>
          {{ adminLoginLoading ? '登录中...' : '登录后台' }}
        </button>
      </form>
    </section>

  </div>
</template>

<script setup>
import { Teleport, computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElCard, ElIcon, ElTable, ElTableColumn } from 'element-plus'
import { ChatDotRound, DataLine, Flag, Pointer, TrendCharts, User, View } from '@element-plus/icons-vue'
import { api } from '@/js/api'
import { useAdminAuthStore } from '@/js/stores/adminAuth'
import { useUiStore } from '@/js/stores/ui'
import { buildPressureTags, comboControlLabel, comboControlOptions, comboMatchesControlType, normalizeComboControlType, comboTagLabel, comboTagOptions, normalizeComboTags, normalizeRouteCharacterIds, pressureTypeOptions, splitPressureTags, MAX_COMBO_TAGS } from '@/js/utils/helpers'
import { CHARACTER_DATA_FIELDS, createEmptyCharacterData } from '@/js/data/characterProfileData'
import ComboNotation from '@/components/common/ComboNotation.vue'
import WorldTourComboRoute from '@/components/common/WorldTourComboRoute.vue'
import 'element-plus/es/components/card/style/css'
import 'element-plus/es/components/icon/style/css'
import 'element-plus/es/components/table/style/css'
import 'element-plus/theme-chalk/dark/css-vars.css'

const BulkActions = defineComponent({
  props: {
    count: { type: Number, default: 0 },
    allSelected: { type: Boolean, default: false },
  },
  emits: ['toggle-all', 'clear'],
  setup(props, { emit, slots }) {
    return () => h('div', { class: 'bulk-inline' }, [
      h('label', { class: 'select-all' }, [
        h('input', {
          type: 'checkbox',
          checked: props.allSelected,
          onChange: event => emit('toggle-all', event.target.checked),
        }),
        '全选',
      ]),
      h('span', null, `已选 ${props.count} 个`),
      ...(slots.default ? slots.default() : []),
      h('button', {
        type: 'button',
        class: 'ghost',
        disabled: !props.count,
        onClick: () => emit('clear'),
      }, '清空'),
    ])
  },
})

const AdminModal = defineComponent({
  inheritAttrs: false,
  props: {
    title: { type: String, required: true },
    subtitle: { type: String, default: '' },
    tone: { type: String, default: 'default' },
  },
  emits: ['close'],
  setup(props, { emit, slots, attrs }) {
    return () => h(Teleport, { to: 'body' }, h('div', {
      class: 'admin-modal-backdrop',
      role: 'presentation',
      onClick: event => {
        if (event.target === event.currentTarget) emit('close')
      },
    }, [
      h('section', {
        class: ['admin-modal-panel', attrs.class, { danger: props.tone === 'danger' }],
        role: 'dialog',
        'aria-modal': 'true',
        'aria-label': props.title,
        onKeydown: event => {
          if (event.key === 'Escape') {
            event.preventDefault()
            emit('close')
          }
        },
      }, [
        h('header', { class: 'admin-modal-head' }, [
          h('div', null, [
            h('h2', null, props.title),
            props.subtitle ? h('p', null, props.subtitle) : null,
          ]),
          h('button', {
            type: 'button',
            class: 'modal-close',
            title: '关闭',
            'aria-label': '关闭',
            onClick: () => emit('close'),
          }, [h('span', { class: 'material-symbols-outlined' }, 'close')]),
        ]),
        h('div', { class: 'admin-modal-body' }, slots.default ? slots.default() : []),
      ]),
    ]))
  },
})

const adminAuth = useAdminAuthStore()
const ui = useUiStore()
const route = useRoute()
const router = useRouter()
const isAdmin = computed(() => adminAuth.user?.role === 'admin')
const COMBO_REVIEW_PERMISSION = 'combo_review'
const userPermissions = computed(() => String(adminAuth.user?.adminPermissions || '').split(',').map(item => item.trim()).filter(Boolean))
const canManageCombos = computed(() => isAdmin.value || userPermissions.value.includes(COMBO_REVIEW_PERMISSION))
const canAccessAdmin = computed(() => isAdmin.value || canManageCombos.value)
const loading = ref(false)
const hasLoadedAdminData = ref(false)
const message = ref('')
const messageType = ref('success')
const activeSection = ref(canManageCombos.value && !isAdmin.value ? 'combos' : 'dashboard')
const adminLoginForm = reactive({ email: '', password: '' })
const adminLoginLoading = ref(false)
const adminLoginError = ref('')
const uploadingComboVideo = ref(false)
const uploadingCharacterAvatar = ref(false)
const syncingOfficialFrames = ref(false)
const avatarCropStageRef = ref(null)
const avatarCropOutputSize = 512
const avatarCropStageSize = 292
const avatarCropFrameSize = 260
const avatarCropMinFrameSize = 80
const avatarCropResizeCorners = ['nw', 'ne', 'sw', 'se']
const avatarCropAspectPresets = [
  { label: '1:1', value: '1:1' },
  { label: '4:5', value: '4:5' },
  { label: '3:4', value: '3:4' },
  { label: '自由', value: 'free' },
]
const avatarCrop = reactive({
  active: false,
  url: '',
  fileName: '',
  image: null,
  imageWidth: 0,
  imageHeight: 0,
  scale: 1,
  minScale: 1,
  maxScale: 4,
  offsetX: 0,
  offsetY: 0,
  dragging: false,
  dragMode: '',
  dragStartX: 0,
  dragStartY: 0,
  startOffsetX: 0,
  startOffsetY: 0,
  frameX: 16,
  frameY: 16,
  frameWidth: avatarCropFrameSize,
  frameHeight: avatarCropFrameSize,
  startFrameX: 16,
  startFrameY: 16,
  startFrameWidth: avatarCropFrameSize,
  startFrameHeight: avatarCropFrameSize,
  resizeCorner: '',
  aspect: '1:1',
})

const allSections = [
  { key: 'dashboard', label: '概览', icon: 'dashboard' },
  { key: 'users', label: '用户', icon: 'group' },
  { key: 'characters', label: '角色', icon: 'sports_martial_arts' },
  { key: 'frames', label: '帧数', icon: 'data_table' },
  { key: 'combos', label: '连招', icon: 'bolt' },
  { key: 'announcements', label: '指南', icon: 'menu_book' },
  { key: 'reports', label: '举报', icon: 'flag' },
  { key: 'feedbacks', label: '反馈', icon: 'feedback' },
  { key: 'audit', label: '审计', icon: 'manage_search' },
]
const sections = computed(() => {
  if (isAdmin.value) return allSections
  if (canManageCombos.value) return allSections.filter(item => item.key === 'combos')
  return []
})
const currentSection = computed(() => sections.value.find(item => item.key === activeSection.value) || sections.value[0] || allSections[0])
const defaultAdminSection = computed(() => (isAdmin.value ? 'dashboard' : 'combos'))

const dashboard = ref({})
const users = ref([])
const characters = ref([])
const frames = ref([])
const combos = ref([])
const adminParentCombos = ref([])
const adminParentCombosLoading = ref(false)
const adminParentCombosError = ref('')
const reports = ref([])
const feedbacks = ref([])
const frameSyncLogs = ref([])
const frameChangeHistory = ref([])
const announcements = ref([])
const auditLogs = ref([])
const userQuery = ref('')
const userRole = ref('')
const userPage = ref(1)
const userPageSize = 20
const userTotal = ref(0)
const reportStatus = ref('pending')
const reportAuthorId = ref('')
const reportPage = ref(1)
const reportPageSize = 12
const reportTotal = ref(0)
const feedbackStatus = ref('pending')
const feedbackPage = ref(1)
const feedbackPageSize = 12
const feedbackTotal = ref(0)
const auditTargetType = ref('')
const auditAction = ref('')
const auditStartDate = ref('')
const auditEndDate = ref('')
const auditPage = ref(1)
const auditPageSize = 12
const auditTotal = ref(0)
const highlightedTarget = ref({ type: '', id: null })
let highlightTimer = null
let frameSearchTimer = null
const frameFilterCharacterId = ref('')
const frameControlFilter = ref('')
const frameSearch = ref('')
const comboFilterCharacterId = ref('')
const comboStatusFilter = ref('')
const comboControlFilter = ref('')
const comboQueueQuery = ref('')
const comboPage = ref(1)
const comboPageSize = 24
const comboTotal = ref(0)
const expandedComboFollowupQueues = ref([])
const activeReviewCombo = ref(null)
const activeInlineReviewCombo = ref(null)
const adminModal = ref('')
const activeUser = ref(null)
const activeReport = ref(null)
const activeFeedback = ref(null)
const duplicateCombos = ref([])
const duplicateCheckLoading = ref(false)
const duplicateCheckExact = ref(false)
const duplicateCheckOverridden = ref(false)
const comboVideoUploadProgress = ref(0)
const characterAvatarUploadProgress = ref(0)
const frameDataVersion = 'Capcom 官网同步版'
const characterPage = ref(1)
const characterPageSize = 12
const framePage = ref(1)
const framePageSize = 50
const frameTotal = ref(0)
const userGrowthChartRef = ref(null)
const visitTrendChartRef = ref(null)
const characterHeatChartRef = ref(null)
const reviewDonutChartRef = ref(null)
let dashboardCharts = []
let dashboardChartModulePromise = null
let dashboardChartRenderId = 0
let adminParentComboRequestId = 0
let hydratingComboForm = false
let adminEventSource = null
let adminRealtimeTimer = null
let adminEventReconnectTimer = null
const pendingAdminRealtimeAreas = new Set()

const editingCharacterId = ref(null)
const editingFrameId = ref(null)
const editingComboId = ref(null)
const editingAnnouncementId = ref(null)
const routeMoves = ref([''])
const routeCharacterIds = ref([''])
const routeMoveInputs = ref([])
const routeMoveExamples = ['2MK', 'DR', '5HP', '214HK', 'SA3']
function uppercaseMoveText(value) {
  return String(value || '').replace(/[A-Za-z]+/g, match => match.toUpperCase())
}

const normalizedAdminRouteEntries = computed(() => routeMoves.value
  .map((move, index) => ({ move: uppercaseMoveText(move.trim()), characterId: Number(routeCharacterIds.value[index]) || null }))
  .filter(entry => entry.move))
const mergedRoute = computed(() => normalizedAdminRouteEntries.value.map(entry => entry.move).join(' > '))
const firstComboRouteMove = computed(() => normalizedAdminRouteEntries.value[0]?.move || '')
const adminWorldTourRouteCharacterIds = computed(() => normalizedAdminRouteEntries.value.map(entry => entry.characterId))
watch(routeCharacterIds, characterIds => {
  if (comboForm.controlType !== 'world-tour') return
  comboForm.characterId = Number(characterIds[0]) || ''
}, { deep: true })
const comboVideoUploadText = computed(() => {
  if (uploadingComboVideo.value) return `上传中 ${comboVideoUploadProgress.value}%`
  return comboForm.videoUrl ? '已上传，点击可更换' : '上传连招视频'
})
const characterAvatarUploadText = computed(() => {
  if (uploadingCharacterAvatar.value) return `上传中 ${characterAvatarUploadProgress.value}%`
  if (avatarCrop.active) return '重新选择图片'
  return characterForm.avatar ? '更换本地图片' : '上传本地图片'
})
const avatarCropImageStyle = computed(() => ({
  width: `${avatarCrop.imageWidth * avatarCrop.scale}px`,
  height: `${avatarCrop.imageHeight * avatarCrop.scale}px`,
  transform: `translate(calc(-50% + ${avatarCrop.offsetX}px), calc(-50% + ${avatarCrop.offsetY}px))`,
}))
const avatarCropFrameStyle = computed(() => ({
  left: `${avatarCrop.frameX}px`,
  top: `${avatarCrop.frameY}px`,
  width: `${avatarCrop.frameWidth}px`,
  height: `${avatarCrop.frameHeight}px`,
}))
const avatarCropScalePercent = computed({
  get: () => Math.round((avatarCrop.scale / Math.max(avatarCrop.minScale, 0.0001)) * 100),
  set: value => {
    const percent = Math.min(500, Math.max(100, Number(value) || 100))
    avatarCrop.scale = avatarCrop.minScale * percent / 100
    clampAvatarCropOffset()
  },
})
const selected = reactive({
  users: [],
  characters: [],
  frames: [],
  combos: [],
  reports: [],
  feedbacks: [],
})

const emptyCharacterForm = () => ({
  name: '',
  avatar: '',
  archetype: '',
  description: '',
  characterData: createEmptyCharacterData(),
})
const characterForm = reactive(emptyCharacterForm())
const emptyFrameForm = () => ({
  characterId: '',
  controlType: 'classic',
  moveName: '',
  startup: '',
  active: '',
  recovery: '',
  onBlock: '',
  onHit: '',
  cancel: '',
  damage: '',
  comboScaling: '',
  driveGainOnHit: '',
  driveLossOnBlock: '',
  driveLossOnPunishCounter: '',
  superArtGain: '',
  properties: '',
  miscellaneous: '',
  sourceUrl: '',
  sourceCharacterSlug: '',
  sourceLang: '',
  displayOrder: null,
})
const frameForm = reactive(emptyFrameForm())
const comboForm = reactive({
  characterId: '',
  controlType: 'classic',
  route: '',
  comboText: '',
  damage: '',
  driveCost: '',
  saCost: '',
  advantageFrames: '',
  difficulty: '',
  tags: ['counter-hit'],
  pressureType: '',
  videoUrl: '',
  trainingNotes: '',
  difficultyNote: '',
  cornerOnly: false,
  followupParentId: '',
})
const reviewForm = reactive({
  rejectionReason: '',
  difficulty: '',
  difficultyNote: '',
})
const banForm = reactive({
  banned: true,
  reason: '',
  days: 0,
  batch: false,
})
const userManageForm = reactive({
  role: 'user',
  canReviewCombos: false,
})
const reportActionForm = reactive({
  status: 'resolved',
  resolution: '',
  batch: false,
})
const feedbackActionForm = reactive({
  status: 'resolved',
  resolution: '',
  batch: false,
})
const announcementForm = reactive({
  title: '',
  content: '',
  level: 'info',
  published: true,
})

const dashboardReviewQueueCount = computed(() => Number(dashboard.value.reviewQueueCombos ?? ((dashboard.value.manualReviewCombos || 0) + (dashboard.value.pendingCombos || 0))))
const overviewMetrics = computed(() => [
  { label: '总用户数', value: dashboard.value.users || 0, icon: User, growth: todayGrowthPercent(dashboard.value.todayUsers, yesterdayValue(dashboard.value.userTrend)) },
  { label: '总连招数', value: dashboard.value.combos || 0, icon: DataLine, growth: todayGrowthPercent(dashboard.value.todayCombos, yesterdayValue(dashboard.value.comboTrend)) },
  { label: '待处理投稿', value: dashboard.value.reviewQueueCombos ?? dashboardReviewQueueCount.value, icon: Pointer, growth: null, status: '待办' },
  { label: '待处理举报', value: dashboard.value.pendingReports ?? 0, icon: Flag, growth: null, status: '实时' },
  { label: '待处理反馈', value: dashboard.value.pendingFeedbacks ?? 0, icon: ChatDotRound, growth: null, status: '实时' },
  { label: '今日 UV', value: dashboard.value.todayUv ?? 0, icon: View, growth: todayGrowthPercent(dashboard.value.todayUv, yesterdayVisitValue('uv')) },
  { label: '今日 PV', value: dashboard.value.todayPv ?? 0, icon: TrendCharts, growth: todayGrowthPercent(dashboard.value.todayPv, yesterdayVisitValue('pv')) },
])
const dashboardTasks = computed(() => [
  {
    key: 'manual-review',
    label: '连招审核队列',
    count: dashboardReviewQueueCount.value,
    hint: '用户投稿等待处理',
    icon: 'rule_settings',
    tone: 'danger',
    section: 'combos',
    status: 'manual_review',
  },
  {
    key: 'reports',
    label: '待处理举报',
    count: dashboard.value.pendingReports || 0,
    hint: '优先看违规内容和错误数据',
    icon: 'flag',
    tone: 'warning',
    section: 'reports',
  },
  {
    key: 'feedbacks',
    label: '待处理反馈',
    count: dashboard.value.pendingFeedbacks || 0,
    hint: '产品问题、建议和站点反馈',
    icon: 'feedback',
    tone: 'neutral',
    section: 'feedbacks',
  },
])
const dashboardHighRiskCombos = computed(() => dashboard.value.highRiskCombos || [])
const dashboardIssueReviewCombos = computed(() => dashboard.value.issueReviewCombos || [])
const dashboardRecentAuditLogs = computed(() => dashboard.value.recentAuditLogs || [])
const trendDays = computed(() => (dashboard.value.userTrend || []).map(item => formatTrendDate(item.date)))
const userGrowthSeries = computed(() => (dashboard.value.userTrend || []).map(item => Number(item.value || 0)))
const visitTrendDays = computed(() => (dashboard.value.visitTrend || []).map(item => formatTrendDate(item.date)))
const visitUvSeries = computed(() => (dashboard.value.visitTrend || []).map(item => Number(item.uv || 0)))
const visitPvSeries = computed(() => (dashboard.value.visitTrend || []).map(item => Number(item.pv || 0)))
const reviewStatusData = computed(() => [
  { name: '待处理', value: dashboardReviewQueueCount.value },
  { name: '已通过', value: dashboard.value.approvedCombos || 0 },
  { name: '已拒绝', value: dashboard.value.rejectedCombos || 0 },
])
const popularComboRows = computed(() => (dashboard.value.popularCombos || []).map((combo, index) => ({
  rank: index + 1,
  name: combo.name || '-',
  character: combo.character || '-',
  favorites: Number(combo.favorites || 0),
  likes: Number(combo.likes || 0),
})))
const characterComboRanking = computed(() => {
  const counts = new Map()
  combos.value.forEach(combo => {
    const name = characterName(combo.characterId)
    counts.set(name, (counts.get(name) || 0) + 1)
  })
  return Array.from(counts.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 10)
})
const userPageCount = computed(() => Math.max(1, Math.ceil(userTotal.value / userPageSize)))
const characterPageCount = computed(() => Math.max(1, Math.ceil(characters.value.length / characterPageSize)))
const pagedCharacters = computed(() => {
  const start = (characterPage.value - 1) * characterPageSize
  return characters.value.slice(start, start + characterPageSize)
})
const framePageCount = computed(() => Math.max(1, Math.ceil(frameTotal.value / framePageSize)))
const reportPageCount = computed(() => Math.max(1, Math.ceil(reportTotal.value / reportPageSize)))
const feedbackPageCount = computed(() => Math.max(1, Math.ceil(feedbackTotal.value / feedbackPageSize)))
const auditPageCount = computed(() => Math.max(1, Math.ceil(auditTotal.value / auditPageSize)))
const comboPageCount = computed(() => Math.max(1, Math.ceil(comboTotal.value / comboPageSize)))
const selectableUsers = computed(() => users.value.filter(user => !isCurrentAdmin(user)))
const hasAuditTimeFilter = computed(() => Boolean(auditStartDate.value || auditEndDate.value))
const auditTargetTypeOptions = computed(() => [
  { value: '', label: '全部分类' },
  { value: 'user', label: '用户' },
  { value: 'combo', label: '连招' },
  { value: 'frame', label: '帧数' },
  { value: 'frame_sync', label: '帧数同步' },
  { value: 'report', label: '举报' },
  { value: 'feedback', label: '反馈' },
  { value: 'announcement', label: '指南' },
  { value: 'notification', label: '通知' },
])
const auditActionOptions = computed(() => [
  { value: '', label: '全部操作' },
  { value: 'update_user', label: '更新用户' },
  { value: 'ban_user', label: '封禁用户' },
  { value: 'unban_user', label: '解封用户' },
  { value: 'create_frame', label: '新增帧数' },
  { value: 'update_frame', label: '更新帧数' },
  { value: 'delete_frame', label: '删除帧数' },
  { value: 'sync_official_frames', label: '同步官方帧数' },
  { value: 'create_combo', label: '新增连招' },
  { value: 'update_combo', label: '更新连招' },
  { value: 'approve_combo', label: '通过连招' },
  { value: 'review_combo', label: '审核连招' },
  { value: 'delete_combo', label: '删除连招' },
  { value: 'handle_report', label: '处理举报' },
  { value: 'batch_handle_reports', label: '批量处理举报' },
  { value: 'handle_feedback', label: '处理反馈' },
  { value: 'batch_handle_feedbacks', label: '批量处理反馈' },
  { value: 'create_announcement', label: '新增指南' },
  { value: 'update_announcement', label: '更新指南' },
  { value: 'delete_announcement', label: '删除指南' },
  { value: 'broadcast_notification', label: '广播通知' },
])
const comboStatusFilterOptions = computed(() => [
  { value: '', label: '全部', count: comboTotal.value },
  { value: 'reviewed', label: '已审核', count: combos.value.filter(combo => combo.status === 'approved').length },
  { value: 'rejected', label: '已驳回', count: combos.value.filter(combo => combo.status === 'rejected').length },
  { value: 'manual_review', label: '待处理', count: combos.value.filter(combo => ['manual_review', 'pending'].includes(combo.status)).length },
])
const comboControlFilterBase = computed(() => combos.value.filter(combo => {
  const matchesCharacter = !comboFilterCharacterId.value || Number(combo.characterId) === Number(comboFilterCharacterId.value)
  const matchesStatus = comboMatchesStatusFilter(combo, comboStatusFilter.value)
  return matchesCharacter && matchesStatus
}))
const comboControlFilterOptions = computed(() => [
  { value: '', label: '全部类型', count: comboControlFilterBase.value.length },
  ...comboControlOptions.map(option => ({
    ...option,
    count: comboControlFilterBase.value.filter(combo => comboMatchesControlType(combo, option.value)).length,
  })),
])
const reviewComboTags = computed(() => activeReviewCombo.value ? normalizeComboTags(activeReviewCombo.value, activeReviewCombo.value.type) : [])
const reviewRouteMoveCount = computed(() => String(activeReviewCombo.value?.route || activeReviewCombo.value?.comboText || '').split(/\s*>\s*/).filter(Boolean).length)
const reviewDetailItems = computed(() => {
  const combo = activeReviewCombo.value
  if (!combo) return []
  return [
    { label: '角色', value: characterName(combo.characterId) },
    { label: '起手', value: combo.starter || '未填写' },
    { label: '帧数优势', value: combo.advantageFrames || '未填写' },
    { label: '资源消耗', value: `绿条 ${combo.driveCost ?? 0} / SA ${combo.saCost ?? 0}` },
    { label: '视频', value: combo.videoUrl ? '已上传' : '未上传' },
    { label: '审核人', value: combo.reviewedBy || '尚未审核' },
    { label: '审核时间', value: combo.reviewedAt ? formatDateTime(combo.reviewedAt) : '尚未审核' },
    { label: '关系', value: combo.followupParentId ? `后续：${comboLabelById(combo.followupParentId)}` : '独立连招' },
  ]
})
const filteredCombos = computed(() => {
  return combos.value
})
const visibleComboRows = computed(() => filteredCombos.value.filter(combo => !isApprovedFollowupCombo(combo)))
const queuedComboRows = computed(() => {
  return visibleComboRows.value
})
const comboBulkRows = computed(() => {
  const rows = [...queuedComboRows.value]
  const seenIds = new Set(rows.map(combo => Number(combo.id)))
  queuedComboRows.value.forEach(combo => {
    if (!isComboFollowupQueueOpen(combo.id)) return
    approvedFollowupsForCombo(combo).forEach(followup => {
      const id = Number(followup.id)
      if (seenIds.has(id)) return
      seenIds.add(id)
      rows.push(followup)
    })
  })
  return rows
})
const approvedFollowupCombosByParentId = computed(() => {
  const groups = new Map()
  combos.value
    .filter(combo => isApprovedFollowupCombo(combo))
    .filter(combo => !comboFilterCharacterId.value || Number(combo.characterId) === Number(comboFilterCharacterId.value))
    .filter(combo => !comboControlFilter.value || comboMatchesControlType(combo, comboControlFilter.value))
    .forEach(combo => {
      const parentId = Number(combo.followupParentId)
      if (!groups.has(parentId)) groups.set(parentId, [])
      groups.get(parentId).push(combo)
    })
  groups.forEach(items => {
    items.sort((left, right) => Number(right.id || 0) - Number(left.id || 0))
  })
  return groups
})
const adminParentComboOptions = computed(() => adminParentCombos.value.filter(combo => {
  if (!comboForm.characterId) return false
  if (Number(combo.characterId) !== Number(comboForm.characterId)) return false
  if (editingComboId.value && Number(combo.id) === Number(editingComboId.value)) return false
  if (!comboMatchesControlType(combo, comboForm.controlType)) return false
  return true
}))
const adminCurrentParentUnavailable = computed(() => (
  !adminParentCombosLoading.value
  && !!comboForm.followupParentId
  && !adminParentComboOptions.value.some(combo => Number(combo.id) === Number(comboForm.followupParentId))
))
const adminPressureMode = computed(() => !!comboForm.followupParentId)
const banModalSubtitle = computed(() => {
  if (banForm.batch) return `已选择 ${selectedCount('users')} 个可操作用户`
  return activeUser.value ? `${activeUser.value.username} · #${activeUser.value.id}` : ''
})
const reportModalSubtitle = computed(() => {
  if (reportActionForm.batch) return `已选择 ${selectedCount('reports')} 条举报`
  return activeReport.value ? `${reportTargetLabel(activeReport.value)} · #${activeReport.value.id}` : ''
})
const feedbackModalSubtitle = computed(() => {
  if (feedbackActionForm.batch) return `已选择 ${selectedCount('feedbacks')} 条反馈`
  return activeFeedback.value ? `${activeFeedback.value.username || '匿名用户'} · #${activeFeedback.value.id}` : ''
})
function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
  window.clearTimeout(showMessage.timer)
  showMessage.timer = window.setTimeout(dismissMessage, 3200)
}

function dismissMessage() {
  window.clearTimeout(showMessage.timer)
  message.value = ''
}

async function runAction(successText, action) {
  try {
    await action()
    showMessage(successText)
  } catch (error) {
    showMessage(error.message || '操作失败', 'error')
  }
}

async function loadAll() {
  if (!canAccessAdmin.value) return
  if (loading.value) return
  loading.value = true
  try {
    if (isAdmin.value) {
      await Promise.all([loadDashboard(), loadCharacters(), loadCombos()])
      await Promise.all([loadUsers(), loadFrames(), loadFrameOps(), loadReports(), loadFeedbacks(), loadAnnouncements(), loadAuditLogs()])
    } else {
      await Promise.all([loadCharacters(), loadCombos()])
    }
    hasLoadedAdminData.value = true
    return true
  } catch (error) {
    showMessage(error.message || '后台数据刷新失败', 'error')
    return false
  } finally {
    loading.value = false
  }
}

async function handleAdminLogin() {
  if (adminLoginLoading.value) return
  adminLoginError.value = ''
  adminLoginLoading.value = true
  try {
    const user = await api.adminLogin(adminLoginForm.email, adminLoginForm.password)
    adminAuth.login(user)
    adminLoginForm.password = ''
    activeSection.value = user.role === 'admin' ? 'dashboard' : 'combos'
    hasLoadedAdminData.value = false
    await nextTick()
    await loadAll()
  } catch (error) {
    adminLoginError.value = error.message || '后台登录失败'
  } finally {
    adminLoginLoading.value = false
  }
}

async function loadDashboard() {
  dashboard.value = await api.getAdminDashboard()
  await nextTick()
  renderDashboardCharts()
}

async function refreshDashboardIfAdmin() {
  if (isAdmin.value) {
    await loadDashboard()
  }
}

async function loadUsers() {
  const result = await api.getAdminUsers({ page: userPage.value, pageSize: userPageSize, q: userQuery.value, role: userRole.value })
  users.value = result?.list || []
  userPage.value = Number(result?.page || userPage.value || 1)
  userTotal.value = Number(result?.total || 0)
  if (userPage.value > userPageCount.value) {
    userPage.value = userPageCount.value
    await loadUsers()
    return
  }
  syncSelection('users', users.value)
}

async function loadCharacters() {
  characters.value = isAdmin.value ? await api.getAdminCharacters() : await api.getCharacters()
  if (characterPage.value > characterPageCount.value) characterPage.value = characterPageCount.value
  syncSelection('characters', characters.value)
}

async function loadFrames() {
  const result = await api.getAdminFrames({
    page: framePage.value,
    pageSize: framePageSize,
    characterId: frameFilterCharacterId.value,
    controlType: frameControlFilter.value,
    q: frameSearch.value,
  })
  frames.value = result?.list || []
  framePage.value = Number(result?.page || framePage.value || 1)
  frameTotal.value = Number(result?.total || 0)
  if (framePage.value > framePageCount.value) {
    framePage.value = framePageCount.value
    await loadFrames()
    return
  }
  syncSelection('frames', frames.value)
}

async function refreshUsers() {
  userPage.value = 1
  await loadUsers()
}

function changeCharacterPage(page) {
  const nextPage = Math.min(Math.max(1, page), characterPageCount.value)
  if (nextPage === characterPage.value) return
  characterPage.value = nextPage
  clearSelection('characters')
}

async function refreshFrames() {
  framePage.value = 1
  await loadFrames()
}

async function changeUserPage(page) {
  const nextPage = Math.min(Math.max(1, page), userPageCount.value)
  if (nextPage === userPage.value) return
  userPage.value = nextPage
  await loadUsers()
}

async function changeFramePage(page) {
  const nextPage = Math.min(Math.max(1, page), framePageCount.value)
  if (nextPage === framePage.value) return
  framePage.value = nextPage
  await loadFrames()
}

async function loadFrameOps() {
  const [syncLogs, changes] = await Promise.all([api.getAdminFrameSyncLogs(), api.getAdminFrameChangeHistory()])
  frameSyncLogs.value = syncLogs || []
  frameChangeHistory.value = changes || []
}

async function loadCombos() {
  const result = await api.getAdminCombos({
    page: comboPage.value,
    pageSize: comboPageSize,
    characterId: comboFilterCharacterId.value,
    status: comboStatusFilter.value,
    controlType: comboControlFilter.value,
    q: comboQueueQuery.value,
  })
  const comboList = Array.isArray(result) ? result : result?.list || []
  combos.value = comboList
  comboPage.value = Number(result?.page || comboPage.value || 1)
  comboTotal.value = Number(result?.total ?? comboList.length)
  if (activeReviewCombo.value) {
    activeReviewCombo.value = combos.value.find(combo => Number(combo.id) === Number(activeReviewCombo.value.id)) || activeReviewCombo.value
  }
  syncSelection('combos', combos.value)
  await nextTick()
  renderDashboardCharts()
}

async function changeComboPage(page) {
  const nextPage = Math.min(Math.max(1, page), comboPageCount.value)
  if (nextPage === comboPage.value) return
  comboPage.value = nextPage
  await loadCombos()
}

async function loadReports() {
  const result = await api.getAdminReports({
    page: reportPage.value,
    pageSize: reportPageSize,
    status: reportStatus.value,
    authorId: reportAuthorId.value,
  })
  reports.value = result?.list || []
  reportPage.value = Number(result?.page || reportPage.value || 1)
  reportTotal.value = Number(result?.total || 0)
  syncSelection('reports', reports.value)
}

async function loadFeedbacks() {
  const result = await api.getAdminFeedbacks({ page: feedbackPage.value, pageSize: feedbackPageSize, status: feedbackStatus.value })
  feedbacks.value = result?.list || []
  feedbackPage.value = Number(result?.page || feedbackPage.value || 1)
  feedbackTotal.value = Number(result?.total || 0)
  if (feedbackPage.value > feedbackPageCount.value) {
    feedbackPage.value = feedbackPageCount.value
    await loadFeedbacks()
    return
  }
  syncSelection('feedbacks', feedbacks.value)
}

async function refreshReports() {
  reportPage.value = 1
  await loadReports()
}

async function refreshFeedbacks() {
  feedbackPage.value = 1
  await loadFeedbacks()
}

async function refreshAuditLogs() {
  auditPage.value = 1
  await loadAuditLogs()
}

async function clearAuditTimeFilter() {
  auditStartDate.value = ''
  auditEndDate.value = ''
  await refreshAuditLogs()
}

async function changeReportPage(page) {
  const nextPage = Math.min(Math.max(1, page), reportPageCount.value)
  if (nextPage === reportPage.value) return
  reportPage.value = nextPage
  await loadReports()
}

async function changeFeedbackPage(page) {
  const nextPage = Math.min(Math.max(1, page), feedbackPageCount.value)
  if (nextPage === feedbackPage.value) return
  feedbackPage.value = nextPage
  await loadFeedbacks()
}

async function loadAnnouncements() {
  announcements.value = await api.getAdminAnnouncements()
}

async function loadAuditLogs() {
  const result = await api.getAdminAuditLogs({
    page: auditPage.value,
    pageSize: auditPageSize,
    targetType: auditTargetType.value,
    action: auditAction.value,
    startDate: auditStartDate.value,
    endDate: auditEndDate.value,
  })
  if (Array.isArray(result)) {
    auditLogs.value = result
    auditPage.value = 1
    auditTotal.value = result.length
    return
  }
  auditLogs.value = result?.list || []
  auditPage.value = Number(result?.page || auditPage.value || 1)
  auditTotal.value = Number(result?.total || 0)
  if (auditPage.value > auditPageCount.value) {
    auditPage.value = auditPageCount.value
    await loadAuditLogs()
  }
}

async function changeAuditPage(page) {
  const nextPage = Math.min(Math.max(1, page), auditPageCount.value)
  if (nextPage === auditPage.value) return
  auditPage.value = nextPage
  await loadAuditLogs()
}

function connectAdminEvents() {
  if (!canAccessAdmin.value || adminEventSource) return
  adminEventSource = api.openAdminEvents()
  adminEventSource.addEventListener('open', refreshDashboardIfAdmin)
  adminEventSource.addEventListener('admin-update', handleAdminUpdate)
  adminEventSource.addEventListener('error', handleAdminEventError)
}

function disconnectAdminEvents() {
  if (!adminEventSource) return
  adminEventSource.close()
  adminEventSource = null
  pendingAdminRealtimeAreas.clear()
  window.clearTimeout(adminRealtimeTimer)
  window.clearTimeout(adminEventReconnectTimer)
  adminEventReconnectTimer = null
}

function handleAdminEventError() {
  window.clearTimeout(adminEventReconnectTimer)
  adminEventReconnectTimer = window.setTimeout(() => {
    if (!canAccessAdmin.value) return
    disconnectAdminEvents()
    connectAdminEvents()
    refreshDashboardIfAdmin()
  }, 1500)
}

function handleAdminUpdate(event) {
  let payload = {}
  try {
    payload = JSON.parse(event.data || '{}')
  } catch (_) {
    payload = {}
  }
  scheduleAdminRealtimeRefresh(Array.isArray(payload.areas) ? payload.areas : ['combos'])
}

function scheduleAdminRealtimeRefresh(areas) {
  if (!canAccessAdmin.value || !hasLoadedAdminData.value) return
  areas.forEach(area => pendingAdminRealtimeAreas.add(area))
  window.clearTimeout(adminRealtimeTimer)
  adminRealtimeTimer = window.setTimeout(flushAdminRealtimeRefresh, 250)
}

async function flushAdminRealtimeRefresh() {
  if (!canAccessAdmin.value || !hasLoadedAdminData.value) return
  const areas = new Set(pendingAdminRealtimeAreas)
  pendingAdminRealtimeAreas.clear()
  const tasks = []
  if (areas.has('dashboard') && isAdmin.value) tasks.push(loadDashboard())
  if (areas.has('users') && isAdmin.value) tasks.push(loadUsers())
  if (areas.has('characters') && isAdmin.value) tasks.push(loadCharacters())
  if (areas.has('frames') && isAdmin.value) tasks.push(loadFrames())
  if (areas.has('frameOps') && isAdmin.value) tasks.push(loadFrameOps())
  if (areas.has('combos')) tasks.push(loadCombos())
  if (areas.has('reports') && isAdmin.value) tasks.push(loadReports())
  if (areas.has('feedbacks') && isAdmin.value) tasks.push(loadFeedbacks())
  if (areas.has('announcements') && isAdmin.value) tasks.push(loadAnnouncements())
  if (areas.has('audit') && isAdmin.value) tasks.push(loadAuditLogs())
  await Promise.allSettled(tasks)
}

function characterName(id) {
  return characters.value.find(item => Number(item.id) === Number(id))?.name || `#${id}`
}

function comboLabelById(id) {
  const combo = combos.value.find(item => Number(item.id) === Number(id))
  if (!combo) return `#${id}`
  return `${characterName(combo.characterId)} · ${combo.route || combo.comboText || combo.starter}`
}

function isApprovedFollowupCombo(combo) {
  return combo?.status === 'approved' && !!combo.followupParentId
}

function approvedFollowupsForCombo(combo) {
  return approvedFollowupCombosByParentId.value.get(Number(combo?.id)) || []
}

function isComboFollowupQueueOpen(comboId) {
  return expandedComboFollowupQueues.value.includes(Number(comboId))
}

function toggleComboFollowupQueue(comboId) {
  const id = Number(comboId)
  if (!id) return
  if (isComboFollowupQueueOpen(id)) {
    expandedComboFollowupQueues.value = expandedComboFollowupQueues.value.filter(item => item !== id)
    return
  }
  expandedComboFollowupQueues.value = [...expandedComboFollowupQueues.value, id]
}

function openComboFollowupQueue(comboId) {
  const id = Number(comboId)
  if (!id || isComboFollowupQueueOpen(id)) return
  expandedComboFollowupQueues.value = [...expandedComboFollowupQueues.value, id]
}

function adminTargetDomId(type, id) {
  return `admin-${type}-${id}`
}

function isHighlighted(type, id) {
  return highlightedTarget.value.type === type && Number(highlightedTarget.value.id) === Number(id)
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString()
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString()
}

function formatTrendDate(value) {
  return String(value || '').slice(5)
}

function yesterdayValue(rows = []) {
  if (!Array.isArray(rows) || rows.length < 2) return null
  return Number(rows[rows.length - 2]?.value || 0)
}

function yesterdayVisitValue(key) {
  const rows = dashboard.value.visitTrend || []
  if (!Array.isArray(rows) || rows.length < 2) return null
  return Number(rows[rows.length - 2]?.[key] || 0)
}

function todayGrowthPercent(today, yesterday) {
  if (yesterday === null || yesterday === undefined) return null
  const current = Number(today || 0)
  const previous = Number(yesterday || 0)
  if (previous === 0) return current > 0 ? 100 : 0
  return Number((((current - previous) / previous) * 100).toFixed(1))
}

async function renderDashboardCharts() {
  const renderId = ++dashboardChartRenderId
  if (activeSection.value !== 'dashboard') return
  dashboardChartModulePromise ||= import('@/js/admin/dashboardCharts.js')
  const { createDashboardCharts } = await dashboardChartModulePromise
  if (renderId !== dashboardChartRenderId || activeSection.value !== 'dashboard') return
  disposeDashboardCharts(false)
  dashboardCharts = createDashboardCharts({
    userGrowth: userGrowthChartRef.value,
    visitTrend: visitTrendChartRef.value,
    characterHeat: characterHeatChartRef.value,
    reviewDonut: reviewDonutChartRef.value,
  }, {
    trendDays: trendDays.value,
    userGrowthSeries: userGrowthSeries.value,
    visitTrendDays: visitTrendDays.value,
    visitUvSeries: visitUvSeries.value,
    visitPvSeries: visitPvSeries.value,
    characterComboRanking: characterComboRanking.value,
    reviewStatusData: reviewStatusData.value,
  })
}

function disposeDashboardCharts(invalidatePendingRender = true) {
  if (invalidatePendingRender) dashboardChartRenderId += 1
  dashboardCharts.forEach(chart => chart.dispose())
  dashboardCharts = []
}

function resizeDashboardCharts() {
  dashboardCharts.forEach(chart => chart.resize())
}

function userAvatarText(user) {
  return String(user?.username || '?').trim().slice(0, 2).toUpperCase()
}

function isCurrentAdmin(user) {
  return Number(user?.id) === Number(adminAuth.user?.id)
}

function userPermissionList(user) {
  return String(user?.adminPermissions || '').split(',').map(item => item.trim()).filter(Boolean)
}

function hasUserPermission(user, permission) {
  return userPermissionList(user).includes(permission)
}

function nextPermissionString(user, permission, checked) {
  const permissions = new Set(userPermissionList(user))
  if (checked) permissions.add(permission)
  else permissions.delete(permission)
  return Array.from(permissions).join(',')
}

function selectedCount(key) {
  return selected[key]?.length || 0
}

function clearSelection(key) {
  selected[key] = []
}

function targetKindFromReport(report) {
  if (report?.targetType === 'combo') return 'combo'
  if (report?.targetType === 'user') return 'user'
  return ''
}

function sectionForTargetKind(kind) {
  return {
    combo: 'combos',
    user: 'users',
  }[kind] || ''
}

function targetKindFromSection(section) {
  return {
    combos: 'combo',
    users: 'user',
  }[section] || ''
}

function reportTargetIdForAdmin(report) {
  return report?.targetId || null
}

async function ensureSectionData(section) {
  if (section === 'combos' && !combos.value.length) await loadCombos()
  if (section === 'users' && !users.value.length) await loadUsers()
}

function clearAdminTargetQuery() {
  if (!route.query.section && !route.query.targetId) return
  const { section, targetId, ...restQuery } = route.query
  router.replace({ name: 'Admin', query: restQuery }).catch(() => {})
}

async function focusAdminTarget(section, targetId, { updateUrl = false, notifyOnMissing = true } = {}) {
  const kind = targetKindFromSection(section)
  if (!kind || !targetId) return false
  if (!sections.value.some(item => item.key === section)) return false
  activeSection.value = section
  await ensureSectionData(section)
  if (section === 'combos') {
    const targetCombo = combos.value.find(combo => Number(combo.id) === Number(targetId))
    const parentCombo = targetCombo?.followupParentId
      ? combos.value.find(combo => Number(combo.id) === Number(targetCombo.followupParentId))
      : null
    const listAnchorCombo = isApprovedFollowupCombo(targetCombo) ? parentCombo : targetCombo
    if (targetCombo?.characterId) comboFilterCharacterId.value = String(targetCombo.characterId)
    if (targetCombo) comboControlFilter.value = normalizeComboControlType(targetCombo.controlType)
    if (targetCombo) {
      comboStatusFilter.value = targetCombo.status === 'pending'
        ? 'pending'
        : targetCombo.status === 'manual_review'
          ? 'manual_review'
          : targetCombo.status === 'rejected'
            ? 'rejected'
            : 'reviewed'
    }
    if (listAnchorCombo?.id && isApprovedFollowupCombo(targetCombo)) {
      openComboFollowupQueue(listAnchorCombo.id)
    }
  }
  await nextTick()
  const element = document.getElementById(adminTargetDomId(kind, targetId))
  if (!element) {
    if (notifyOnMissing) showMessage('对象不在当前后台列表中，可能已删除或没有权限查看', 'error')
    return false
  }
  highlightedTarget.value = { type: kind, id: Number(targetId) }
  window.clearTimeout(highlightTimer)
  highlightTimer = window.setTimeout(() => {
    highlightedTarget.value = { type: '', id: null }
  }, 3600)
  element.scrollIntoView({ behavior: 'smooth', block: 'center' })
  if (updateUrl) {
    router.replace({ name: 'Admin', query: { ...route.query, section, targetId } })
  }
  return true
}

async function applyAdminTargetFromQuery(query = route.query) {
  const section = String(query.section || '')
  const targetId = Number(query.targetId || 0)
  if (!canAccessAdmin.value) return
  if (!section || !targetId) {
    activeSection.value = defaultAdminSection.value
    return
  }
  const focused = await focusAdminTarget(section, targetId, { notifyOnMissing: false })
  if (!focused) {
    activeSection.value = defaultAdminSection.value
    clearAdminTargetQuery()
  }
}

function rowIds(rows = []) {
  return rows.map(item => item.id).filter(id => id !== undefined && id !== null)
}

function selectedRows(key, rows = []) {
  const ids = new Set(selected[key] || [])
  return rows.filter(item => ids.has(item.id))
}

function isAllSelected(key, rows = []) {
  const ids = rowIds(rows)
  return ids.length > 0 && ids.every(id => selected[key].includes(id))
}

function toggleAll(key, rows = [], checked) {
  selected[key] = checked ? rowIds(rows) : []
}

function syncSelection(key, rows = []) {
  const validIds = new Set(rowIds(rows))
  selected[key] = selected[key].filter(id => validIds.has(id))
}

function closeAdminModal() {
  adminModal.value = ''
  activeUser.value = null
  activeReport.value = null
  activeFeedback.value = null
  banForm.batch = false
  reportActionForm.batch = false
  if (avatarCrop.active) cancelAvatarCrop()
  if (activeReviewCombo.value) closeComboReview()
}

function openUserManageModal(user) {
  activeUser.value = user
  Object.assign(userManageForm, {
    role: user.role || 'user',
    canReviewCombos: hasUserPermission(user, COMBO_REVIEW_PERMISSION),
  })
  adminModal.value = 'user-manage'
}

function openUserBanModal(user, banned) {
  if (isCurrentAdmin(user) && banned) {
    showMessage('不能封禁当前登录管理员', 'error')
    return
  }
  activeUser.value = user
  Object.assign(banForm, {
    banned,
    reason: banned ? (user.banReason || '管理员封禁') : '',
    days: 0,
    batch: false,
  })
  adminModal.value = 'user-ban'
}

async function openUserCombos(user) {
  activeSection.value = 'combos'
  comboStatusFilter.value = ''
  comboControlFilter.value = ''
  comboFilterCharacterId.value = ''
  if (!combos.value.length) await loadCombos()
  showMessage(`${user.username} 的投稿数：${user.comboCount || 0}`)
}

async function openUserReports(user) {
  activeSection.value = 'reports'
  reportAuthorId.value = user.id
  reportStatus.value = ''
  reportPage.value = 1
  await loadReports()
  showMessage(`${user.username} 的投稿被举报 ${user.reportCount || 0} 次`)
}

async function openCharacterCombos(character) {
  activeSection.value = 'combos'
  comboFilterCharacterId.value = String(character.id)
  comboStatusFilter.value = ''
  await loadCombos()
}

async function openCharacterFrames(character) {
  activeSection.value = 'frames'
  frameFilterCharacterId.value = String(character.id)
  await loadFrames()
}

function openBatchUserBanModal(banned) {
  const targets = selectedRows('users', users.value).filter(user => !isCurrentAdmin(user))
  if (!targets.length) return
  activeUser.value = null
  Object.assign(banForm, {
    banned,
    reason: banned ? '管理员封禁' : '',
    days: 0,
    batch: true,
  })
  adminModal.value = 'user-ban'
}

async function submitUserManage() {
  if (!activeUser.value) return
  if (isCurrentAdmin(activeUser.value) && userManageForm.role !== 'admin') {
    showMessage('不能降级当前登录管理员', 'error')
    return
  }
  const permissions = new Set(userPermissionList(activeUser.value))
  if (userManageForm.canReviewCombos) permissions.add(COMBO_REVIEW_PERMISSION)
  else permissions.delete(COMBO_REVIEW_PERMISSION)
  await runAction('用户已更新', async () => {
    await api.updateAdminUser(activeUser.value.id, {
      role: userManageForm.role,
      banned: activeUser.value.banned,
      adminPermissions: Array.from(permissions).join(','),
    })
    closeAdminModal()
    await loadUsers()
  })
}

function banPayload(user, banned) {
  const days = Number(banForm.days || 0)
  return {
    role: user.role,
    banned,
    banReason: banned ? (banForm.reason || '管理员封禁') : '',
    bannedUntil: banned && days > 0 ? new Date(Date.now() + days * 86400000).toISOString() : null,
  }
}

async function setUserBan(user, banned) {
  openUserBanModal(user, banned)
}

async function submitUserBan() {
  const targets = banForm.batch
    ? selectedRows('users', users.value).filter(user => !isCurrentAdmin(user))
    : (activeUser.value ? [activeUser.value] : [])
  if (!targets.length) return
  await runAction(banForm.banned ? (banForm.batch ? '批量封禁完成' : '用户已封禁') : (banForm.batch ? '批量解封完成' : '用户已解封'), async () => {
    await Promise.all(targets.map(user => api.updateAdminUser(user.id, banPayload(user, banForm.banned))))
    if (banForm.batch) clearSelection('users')
    closeAdminModal()
    await Promise.all([loadUsers(), loadDashboard()])
  })
}

async function batchDelete(key, rows, deleteAction, reloadAction, label) {
  const targets = selectedRows(key, rows)
  if (!targets.length) return
  const confirmed = await ui.confirmDialog({
    title: '批量删除',
    message: `确定删除 ${targets.length} 个${label}？`,
    tone: 'danger',
  })
  if (!confirmed) return
  await runAction(`已删除 ${targets.length} 个${label}`, async () => {
    await Promise.all(targets.map(item => deleteAction(item.id)))
    clearSelection(key)
    await reloadAction()
    await refreshDashboardIfAdmin()
  })
}

async function batchApproveCombos() {
  const targets = selectedRows('combos', combos.value).filter(combo => combo.status !== 'approved')
  if (!targets.length) return
  await runAction('批量审核完成', async () => {
    await Promise.all(targets.map(combo => api.approveCombo(combo.id)))
    clearSelection('combos')
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function reloadCharactersArea() {
  await Promise.all([loadCharacters(), loadFrames(), loadCombos()])
}

async function reloadFramesArea() {
  await loadFrames()
}

async function syncOfficialFrames() {
  syncingOfficialFrames.value = true
  try {
    const result = await api.syncOfficialFrames()
    const failures = (result?.results || []).filter(item => !item.success)
    if (failures.length) {
      const failedNames = failures.slice(0, 4).map(item => item.name || item.slug).join('、')
      showMessage(`Capcom 帧数部分同步：${result?.successCount || 0}/${result?.totalCharacters || 0} 个角色，失败：${failedNames}`, 'error')
    } else {
      showMessage(`Capcom 帧数同步完成：${result?.successCount || 0}/${result?.totalCharacters || 0} 个角色，${result?.importedCount || 0} 条数据`)
    }
    await Promise.all([loadCharacters(), loadFrames(), loadFrameOps(), loadDashboard()])
  } catch (error) {
    showMessage(error.message || 'Capcom 帧数同步失败', 'error')
  } finally {
    syncingOfficialFrames.value = false
  }
}

async function reloadCombosArea() {
  await loadCombos()
}

function characterIndex(character) {
  return characters.value.findIndex(item => Number(item.id) === Number(character?.id))
}

function canMoveCharacter(character, direction) {
  const index = characterIndex(character)
  if (index < 0 || characters.value.length <= 1) return false
  if (direction === 'top' || direction === 'up') return index > 0
  if (direction === 'bottom' || direction === 'down') return index < characters.value.length - 1
  return false
}

async function moveCharacter(character, direction) {
  const fromIndex = characterIndex(character)
  if (fromIndex < 0 || !canMoveCharacter(character, direction)) return
  const nextCharacters = [...characters.value]
  const [target] = nextCharacters.splice(fromIndex, 1)
  let toIndex = fromIndex
  if (direction === 'top') toIndex = 0
  if (direction === 'up') toIndex = fromIndex - 1
  if (direction === 'down') toIndex = fromIndex + 1
  if (direction === 'bottom') toIndex = nextCharacters.length
  nextCharacters.splice(toIndex, 0, target)
  const previousCharacters = [...characters.value]
  characters.value = nextCharacters.map((item, index) => ({ ...item, displayOrder: index + 1 }))
  try {
    characters.value = await api.reorderAdminCharacters(characters.value.map(item => item.id))
    if (characterPage.value > characterPageCount.value) characterPage.value = characterPageCount.value
    syncSelection('characters', characters.value)
    showMessage('角色排序已更新')
  } catch (error) {
    characters.value = previousCharacters
    showMessage(error.message || '角色排序保存失败', 'error')
  }
}

function resetCharacterForm() {
  editingCharacterId.value = null
  Object.assign(characterForm, emptyCharacterForm())
}

function openCharacterModal(character = null) {
  if (character) editCharacter(character)
  else resetCharacterForm()
  adminModal.value = 'character'
}

function editCharacter(character) {
  editingCharacterId.value = character.id
  Object.assign(characterForm, {
    name: character.name || '',
    avatar: character.avatar || '',
    archetype: character.archetype || '',
    description: character.description || '',
    characterData: {
      ...createEmptyCharacterData(),
      ...(character.characterData || {}),
    },
  })
}

function hasCharacterData(character) {
  const data = character?.characterData
  return Boolean(data && CHARACTER_DATA_FIELDS.some(field => String(data[field.key] || '').trim()))
}

async function saveCharacter() {
  await runAction('角色已保存', async () => {
    if (uploadingCharacterAvatar.value) throw new Error('头像上传中，请稍后再保存')
    if (editingCharacterId.value) await api.updateAdminCharacter(editingCharacterId.value, { ...characterForm })
    else await api.createAdminCharacter({ ...characterForm })
    resetCharacterForm()
    closeAdminModal()
    await Promise.all([loadCharacters(), loadDashboard()])
  })
}

async function handleCharacterAvatarUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  try {
    await prepareAvatarCrop(file)
  } catch (error) {
    showMessage(error.message || '图片读取失败', 'error')
  }
}

function clearCharacterAvatar() {
  characterForm.avatar = ''
  cancelAvatarCrop()
}

function prepareAvatarCrop(file) {
  if (!file.type.startsWith('image/')) throw new Error('请选择图片文件')
  if (file.type === 'image/gif') throw new Error('裁剪头像暂不支持 GIF，请使用 JPG、PNG 或 WebP')
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      revokeAvatarCropUrl()
      avatarCrop.active = true
      avatarCrop.url = url
      avatarCrop.fileName = file.name || 'character-avatar.png'
      avatarCrop.image = image
      avatarCrop.imageWidth = image.naturalWidth
      avatarCrop.imageHeight = image.naturalHeight
      resetAvatarCrop()
      resolve()
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片读取失败'))
    }
    image.src = url
  })
}

function resetAvatarCrop() {
  avatarCrop.aspect = '1:1'
  avatarCrop.frameWidth = avatarCropFrameSize
  avatarCrop.frameHeight = avatarCropFrameSize
  avatarCrop.frameX = (avatarCropStageSize - avatarCrop.frameWidth) / 2
  avatarCrop.frameY = (avatarCropStageSize - avatarCrop.frameHeight) / 2
  avatarCrop.offsetX = 0
  avatarCrop.offsetY = 0
  refreshAvatarCropScaleBounds(true)
}

function cancelAvatarCrop() {
  revokeAvatarCropUrl()
  avatarCrop.active = false
  avatarCrop.url = ''
  avatarCrop.fileName = ''
  avatarCrop.image = null
  avatarCrop.imageWidth = 0
  avatarCrop.imageHeight = 0
  avatarCrop.scale = 1
  avatarCrop.minScale = 1
  avatarCrop.maxScale = 4
  avatarCrop.offsetX = 0
  avatarCrop.offsetY = 0
  avatarCrop.dragging = false
  avatarCrop.dragMode = ''
  avatarCrop.frameX = 16
  avatarCrop.frameY = 16
  avatarCrop.frameWidth = avatarCropFrameSize
  avatarCrop.frameHeight = avatarCropFrameSize
  avatarCrop.resizeCorner = ''
  avatarCrop.aspect = '1:1'
}

function revokeAvatarCropUrl() {
  if (avatarCrop.url?.startsWith('blob:')) URL.revokeObjectURL(avatarCrop.url)
}

function clampAvatarCropOffset() {
  const renderedWidth = avatarCrop.imageWidth * avatarCrop.scale
  const renderedHeight = avatarCrop.imageHeight * avatarCrop.scale
  const imageCenter = avatarCropStageSize / 2
  const minX = avatarCrop.frameX + avatarCrop.frameWidth - imageCenter - renderedWidth / 2
  const maxX = avatarCrop.frameX - imageCenter + renderedWidth / 2
  const minY = avatarCrop.frameY + avatarCrop.frameHeight - imageCenter - renderedHeight / 2
  const maxY = avatarCrop.frameY - imageCenter + renderedHeight / 2
  avatarCrop.offsetX = Math.min(maxX, Math.max(minX, avatarCrop.offsetX))
  avatarCrop.offsetY = Math.min(maxY, Math.max(minY, avatarCrop.offsetY))
}

function refreshAvatarCropScaleBounds(resetScale = false) {
  if (!avatarCrop.imageWidth || !avatarCrop.imageHeight) return
  const minScale = Math.max(
    avatarCrop.frameWidth / avatarCrop.imageWidth,
    avatarCrop.frameHeight / avatarCrop.imageHeight,
  )
  avatarCrop.minScale = Math.ceil(minScale * 10000) / 10000
  avatarCrop.maxScale = avatarCrop.minScale * 5
  if (resetScale) avatarCrop.scale = avatarCrop.minScale
  else avatarCrop.scale = Math.min(avatarCrop.maxScale, Math.max(avatarCrop.minScale, avatarCrop.scale))
  clampAvatarCropOffset()
}

function setAvatarCropAspect(aspect) {
  avatarCrop.aspect = aspect
  if (aspect === 'free') return
  const ratio = aspect === '4:5' ? 4 / 5 : aspect === '3:4' ? 3 / 4 : 1
  avatarCrop.frameHeight = avatarCropFrameSize
  avatarCrop.frameWidth = avatarCropFrameSize * ratio
  avatarCrop.frameX = (avatarCropStageSize - avatarCrop.frameWidth) / 2
  avatarCrop.frameY = (avatarCropStageSize - avatarCrop.frameHeight) / 2
  refreshAvatarCropScaleBounds()
}

function setAvatarCropFrameDimension(axis, value) {
  const nextValue = Math.min(avatarCropStageSize, Math.max(avatarCropMinFrameSize, Number(value) || avatarCropMinFrameSize))
  const centerX = avatarCrop.frameX + avatarCrop.frameWidth / 2
  const centerY = avatarCrop.frameY + avatarCrop.frameHeight / 2
  avatarCrop.aspect = 'free'
  if (axis === 'width') {
    avatarCrop.frameWidth = nextValue
    avatarCrop.frameX = Math.min(avatarCropStageSize - nextValue, Math.max(0, centerX - nextValue / 2))
  } else {
    avatarCrop.frameHeight = nextValue
    avatarCrop.frameY = Math.min(avatarCropStageSize - nextValue, Math.max(0, centerY - nextValue / 2))
  }
  refreshAvatarCropScaleBounds()
}

function startAvatarCropDrag(event) {
  if (!avatarCrop.active || uploadingCharacterAvatar.value) return
  avatarCrop.dragging = true
  avatarCrop.dragMode = 'image'
  avatarCrop.dragStartX = event.clientX
  avatarCrop.dragStartY = event.clientY
  avatarCrop.startOffsetX = avatarCrop.offsetX
  avatarCrop.startOffsetY = avatarCrop.offsetY
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

function rememberAvatarCropFrameDrag(event, mode) {
  if (!avatarCrop.active || uploadingCharacterAvatar.value) return false
  avatarCrop.dragging = true
  avatarCrop.dragMode = mode
  avatarCrop.dragStartX = event.clientX
  avatarCrop.dragStartY = event.clientY
  avatarCrop.startFrameX = avatarCrop.frameX
  avatarCrop.startFrameY = avatarCrop.frameY
  avatarCrop.startFrameWidth = avatarCrop.frameWidth
  avatarCrop.startFrameHeight = avatarCrop.frameHeight
  event.currentTarget.setPointerCapture?.(event.pointerId)
  return true
}

function startAvatarCropFrameMove(event) {
  rememberAvatarCropFrameDrag(event, 'frame-move')
}

function startAvatarCropFrameResize(event, corner) {
  if (!rememberAvatarCropFrameDrag(event, 'frame-resize')) return
  avatarCrop.resizeCorner = corner
  avatarCrop.aspect = 'free'
}

function moveAvatarCropDrag(event) {
  if (!avatarCrop.dragging) return
  const deltaX = event.clientX - avatarCrop.dragStartX
  const deltaY = event.clientY - avatarCrop.dragStartY
  if (avatarCrop.dragMode === 'image') {
    avatarCrop.offsetX = avatarCrop.startOffsetX + deltaX
    avatarCrop.offsetY = avatarCrop.startOffsetY + deltaY
    clampAvatarCropOffset()
    return
  }
  if (avatarCrop.dragMode === 'frame-move') {
    avatarCrop.frameX = Math.min(avatarCropStageSize - avatarCrop.frameWidth, Math.max(0, avatarCrop.startFrameX + deltaX))
    avatarCrop.frameY = Math.min(avatarCropStageSize - avatarCrop.frameHeight, Math.max(0, avatarCrop.startFrameY + deltaY))
    clampAvatarCropOffset()
    return
  }
  if (avatarCrop.dragMode !== 'frame-resize') return
  const startRight = avatarCrop.startFrameX + avatarCrop.startFrameWidth
  const startBottom = avatarCrop.startFrameY + avatarCrop.startFrameHeight
  let left = avatarCrop.resizeCorner.includes('w') ? avatarCrop.startFrameX + deltaX : avatarCrop.startFrameX
  let right = avatarCrop.resizeCorner.includes('e') ? startRight + deltaX : startRight
  let top = avatarCrop.resizeCorner.includes('n') ? avatarCrop.startFrameY + deltaY : avatarCrop.startFrameY
  let bottom = avatarCrop.resizeCorner.includes('s') ? startBottom + deltaY : startBottom
  if (avatarCrop.resizeCorner.includes('w')) left = Math.min(startRight - avatarCropMinFrameSize, Math.max(0, left))
  if (avatarCrop.resizeCorner.includes('e')) right = Math.max(avatarCrop.startFrameX + avatarCropMinFrameSize, Math.min(avatarCropStageSize, right))
  if (avatarCrop.resizeCorner.includes('n')) top = Math.min(startBottom - avatarCropMinFrameSize, Math.max(0, top))
  if (avatarCrop.resizeCorner.includes('s')) bottom = Math.max(avatarCrop.startFrameY + avatarCropMinFrameSize, Math.min(avatarCropStageSize, bottom))
  avatarCrop.frameX = left
  avatarCrop.frameY = top
  avatarCrop.frameWidth = right - left
  avatarCrop.frameHeight = bottom - top
  refreshAvatarCropScaleBounds()
  clampAvatarCropOffset()
}

function stopAvatarCropDrag() {
  avatarCrop.dragging = false
  avatarCrop.dragMode = ''
  avatarCrop.resizeCorner = ''
}

async function uploadCroppedCharacterAvatar() {
  if (!avatarCrop.active || !avatarCrop.image) return
  uploadingCharacterAvatar.value = true
  characterAvatarUploadProgress.value = 0
  try {
    const file = await createCroppedAvatarFile()
    const result = await api.uploadFile(file, 'avatar', {
      onProgress: value => { characterAvatarUploadProgress.value = value },
    })
    characterForm.avatar = result.url
    characterAvatarUploadProgress.value = 100
    cancelAvatarCrop()
    showMessage('头像裁剪并上传成功')
  } catch (error) {
    showMessage(error.message || '头像上传失败', 'error')
  } finally {
    uploadingCharacterAvatar.value = false
  }
}

function createCroppedAvatarFile() {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement('canvas')
    const outputScale = avatarCropOutputSize / Math.max(avatarCrop.frameWidth, avatarCrop.frameHeight)
    canvas.width = Math.max(1, Math.round(avatarCrop.frameWidth * outputScale))
    canvas.height = Math.max(1, Math.round(avatarCrop.frameHeight * outputScale))
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      reject(new Error('浏览器不支持图片裁剪'))
      return
    }
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'
    ctx.fillStyle = '#0b0e13'
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    const bleed = 1
    const renderedWidth = avatarCrop.imageWidth * avatarCrop.scale
    const renderedHeight = avatarCrop.imageHeight * avatarCrop.scale
    const imageLeft = avatarCropStageSize / 2 - renderedWidth / 2 + avatarCrop.offsetX
    const imageTop = avatarCropStageSize / 2 - renderedHeight / 2 + avatarCrop.offsetY
    const drawWidth = renderedWidth * outputScale
    const drawHeight = renderedHeight * outputScale
    const drawX = (imageLeft - avatarCrop.frameX) * outputScale
    const drawY = (imageTop - avatarCrop.frameY) * outputScale
    ctx.drawImage(avatarCrop.image, drawX - bleed, drawY - bleed, drawWidth + bleed * 2, drawHeight + bleed * 2)
    canvas.toBlob(blob => {
      if (!blob) {
        reject(new Error('图片裁剪失败'))
        return
      }
      const baseName = avatarCrop.fileName.replace(/\.[^.]+$/, '') || 'character-avatar'
      resolve(new File([blob], `${baseName}-crop.png`, { type: 'image/png' }))
    }, 'image/png', 0.95)
  })
}

async function deleteCharacter(id) {
  const confirmed = await ui.confirmDialog({
    title: '删除角色',
    message: '确定删除该角色？相关帧数和连招也会受影响。',
    tone: 'danger',
  })
  if (!confirmed) return
  await runAction('角色已删除', async () => {
    await api.deleteAdminCharacter(id)
    clearSelection('characters')
    await Promise.all([loadCharacters(), loadFrames(), loadCombos(), loadDashboard()])
  })
}

function resetFrameForm() {
  editingFrameId.value = null
  Object.assign(frameForm, emptyFrameForm())
}

function openFrameModal(frame = null) {
  if (frame) editFrame(frame)
  else resetFrameForm()
  adminModal.value = 'frame'
}

function editFrame(frame) {
  editingFrameId.value = frame.id
  Object.assign(frameForm, {
    characterId: frame.characterId,
    controlType: frame.controlType || 'classic',
    moveName: frame.moveName || '',
    startup: frame.startup,
    active: frame.active,
    recovery: frame.recovery,
    onBlock: frame.onBlock || '',
    onHit: frame.onHit || '',
    cancel: frame.cancel || '',
    damage: frame.damage || '',
    comboScaling: frame.comboScaling || '',
    driveGainOnHit: frame.driveGainOnHit || '',
    driveLossOnBlock: frame.driveLossOnBlock || '',
    driveLossOnPunishCounter: frame.driveLossOnPunishCounter || '',
    superArtGain: frame.superArtGain || '',
    properties: frame.properties || '',
    miscellaneous: frame.miscellaneous || '',
    sourceUrl: frame.sourceUrl || '',
    sourceCharacterSlug: frame.sourceCharacterSlug || '',
    sourceLang: frame.sourceLang || '',
    displayOrder: frame.displayOrder ?? null,
  })
}

async function saveFrame() {
  await runAction('帧数已保存', async () => {
    const payload = { ...frameForm, characterId: Number(frameForm.characterId) }
    if (editingFrameId.value) await api.updateAdminFrame(editingFrameId.value, payload)
    else await api.createAdminFrame(payload)
    resetFrameForm()
    closeAdminModal()
    await Promise.all([loadFrames(), loadDashboard()])
    await loadFrameOps()
  })
}

async function deleteFrame(id) {
  const confirmed = await ui.confirmDialog({
    title: '删除帧数',
    message: '确定删除该帧数数据？',
    tone: 'danger',
  })
  if (!confirmed) return
  await runAction('帧数已删除', async () => {
    await api.deleteAdminFrame(id)
    await Promise.all([loadFrames(), loadDashboard()])
    await loadFrameOps()
  })
}

function resetComboForm() {
  adminParentComboRequestId += 1
  adminParentCombosLoading.value = false
  editingComboId.value = null
  Object.assign(comboForm, {
    characterId: '',
    controlType: 'classic',
    route: '',
    comboText: '',
    damage: '',
    driveCost: '',
    saCost: '',
    advantageFrames: '',
    difficulty: '',
    tags: ['counter-hit'],
    pressureType: '',
    videoUrl: '',
    trainingNotes: '',
    difficultyNote: '',
    cornerOnly: false,
    followupParentId: '',
  })
  routeMoves.value = ['']
  routeCharacterIds.value = ['']
  adminParentCombos.value = []
  adminParentCombosError.value = ''
}

function openComboModal(combo = null) {
  if (combo) editCombo(combo)
  else resetComboForm()
  adminModal.value = 'combo'
}

function setComboControlType(type) {
  const nextType = normalizeComboControlType(type)
  if (comboForm.controlType === nextType) return
  comboForm.controlType = nextType
  if (nextType === 'world-tour') {
    routeCharacterIds.value = routeMoves.value.map(() => '')
    comboForm.characterId = ''
  }
  comboForm.followupParentId = ''
  comboForm.pressureType = ''
  loadAdminParentCombos()
}

async function loadAdminParentCombos() {
  const requestId = ++adminParentComboRequestId
  adminParentCombosError.value = ''
  if (!comboForm.characterId || comboForm.controlType === 'world-tour') {
    adminParentCombos.value = []
    adminParentCombosLoading.value = false
    return
  }
  adminParentCombosLoading.value = true
  try {
    const result = await api.getComboParentOptions(comboForm.characterId, { controlType: comboForm.controlType })
    if (requestId !== adminParentComboRequestId) return
    adminParentCombos.value = Array.isArray(result) ? result : []
  } catch (error) {
    if (requestId !== adminParentComboRequestId) return
    adminParentCombos.value = []
    adminParentCombosError.value = error.message || '原连招选项加载失败'
  } finally {
    if (requestId === adminParentComboRequestId) adminParentCombosLoading.value = false
  }
}

function splitRoute(routeText) {
  const moves = String(routeText || '')
    .split(/\s*>\s*/)
    .map(move => move.trim())
    .filter(Boolean)
  return moves.length ? moves : ['']
}

function setRouteMoveInput(el, index) {
  if (el) routeMoveInputs.value[index] = el
}

function focusRouteMove(index) {
  nextTick(() => {
    routeMoveInputs.value[index]?.focus()
  })
}

function addRouteMove() {
  routeMoves.value.push('')
  routeCharacterIds.value.push('')
  focusRouteMove(routeMoves.value.length - 1)
}

function removeRouteMove(index) {
  if (routeMoves.value.length <= 1) return
  routeMoves.value.splice(index, 1)
  routeCharacterIds.value.splice(index, 1)
  routeMoveInputs.value.splice(index, 1)
  focusRouteMove(Math.min(index, routeMoves.value.length - 1))
}

function handleRouteMoveEnter(index) {
  if (!routeMoves.value[index]?.trim()) return
  const nextIndex = index + 1
  if (routeMoves.value[nextIndex] === '') {
    focusRouteMove(nextIndex)
    return
  }
  routeMoves.value.splice(nextIndex, 0, '')
  routeCharacterIds.value.splice(nextIndex, 0, '')
  focusRouteMove(nextIndex)
}

function editCombo(combo) {
  hydratingComboForm = true
  editingComboId.value = combo.id
  const savedTags = normalizeComboTags(combo, combo.type)
  const savedPressureTags = splitPressureTags(savedTags)
  Object.assign(comboForm, {
    characterId: combo.characterId,
    controlType: normalizeComboControlType(combo.controlType),
    route: combo.route || '',
    comboText: combo.comboText || combo.route || '',
    damage: combo.damage || 0,
    driveCost: combo.driveCost || 0,
    saCost: combo.saCost || 0,
    advantageFrames: combo.advantageFrames || '',
    difficulty: combo.difficulty || '中等',
    tags: combo.followupParentId ? ['counter-hit'] : savedTags,
    pressureType: combo.followupParentId ? savedPressureTags.type : '',
    videoUrl: combo.videoUrl || '',
    trainingNotes: combo.trainingNotes || '',
    difficultyNote: combo.difficultyNote || '',
    cornerOnly: Boolean(combo.cornerOnly),
    followupParentId: combo.followupParentId || '',
  })
  routeMoves.value = splitRoute(combo.route)
  const savedRouteCharacterIds = normalizeRouteCharacterIds(combo.routeCharacterIds)
  routeCharacterIds.value = routeMoves.value.map((_, index) => savedRouteCharacterIds[index] || combo.characterId || characters.value[0]?.id || '')
  nextTick(() => {
    hydratingComboForm = false
    loadAdminParentCombos()
  })
}

async function handleComboVideoUpload(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  uploadingComboVideo.value = true
  comboVideoUploadProgress.value = 0
  try {
    const result = await api.uploadFile(file, 'combo-video', {
      onProgress: value => { comboVideoUploadProgress.value = value },
    })
    comboForm.videoUrl = result.url
    comboVideoUploadProgress.value = 100
    showMessage('视频上传成功')
  } catch (error) {
    showMessage(error.message || '视频上传失败', 'error')
  } finally {
    uploadingComboVideo.value = false
  }
}

async function saveCombo() {
  await runAction('连招已保存', async () => {
    if (uploadingComboVideo.value) throw new Error('视频上传中，请稍后再保存')
    if (!mergedRoute.value) throw new Error('请至少填写一个连招招式')
    if (comboForm.controlType === 'world-tour' && adminWorldTourRouteCharacterIds.value.some(characterId => !characterId)) {
      throw new Error('请为每一招选择角色')
    }
    if (comboForm.damage === '' || comboForm.damage === null || !Number.isFinite(Number(comboForm.damage))) {
      throw new Error('请填写连招伤害')
    }
    if (!String(comboForm.advantageFrames || '').trim()) throw new Error('请填写有利帧')
    if (!comboForm.difficulty) throw new Error('请选择连招难度')
    if (adminParentCombosLoading.value) throw new Error('原连招选项仍在加载，请稍后保存')
    if (adminCurrentParentUnavailable.value) throw new Error('当前关联的原连招已不可用，请重新选择')
    if (adminPressureMode.value && !comboForm.pressureType) throw new Error('请选择这条路线的压制类型')
    if (!comboForm.videoUrl) throw new Error('请先上传连招演示视频')
    const submissionTags = adminPressureMode.value
      ? buildPressureTags(comboForm.pressureType)
      : [...comboForm.tags]
    const { pressureType: _pressureType, ...requestForm } = comboForm
    const payload = {
      ...requestForm,
      characterId: comboForm.controlType === 'world-tour' ? null : Number(comboForm.characterId),
      controlType: normalizeComboControlType(comboForm.controlType),
      routeCharacterIds: comboForm.controlType === 'world-tour' ? adminWorldTourRouteCharacterIds.value : [],
      type: submissionTags[0] || 'counter-hit',
      tags: submissionTags,
      starter: firstComboRouteMove.value,
      route: mergedRoute.value,
      comboText: mergedRoute.value,
      damage: Number(comboForm.damage),
      driveCost: Number(comboForm.driveCost),
      saCost: Number(comboForm.saCost),
      cornerOnly: false,
      followupParentId: comboForm.followupParentId ? Number(comboForm.followupParentId) : null,
    }
    if (editingComboId.value) await api.updateAdminCombo(editingComboId.value, payload)
    else await api.createAdminCombo(payload)
    resetComboForm()
    closeAdminModal()
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function approveCombo(id) {
  await runAction('连招已通过', async () => {
    await api.approveCombo(id)
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

function openComboReview(combo) {
  activeReviewCombo.value = combo
  duplicateCombos.value = []
  duplicateCheckExact.value = false
  duplicateCheckOverridden.value = false
  Object.assign(reviewForm, {
    rejectionReason: combo.rejectionReason || '',
    difficulty: combo.difficulty || '',
    difficultyNote: combo.difficultyNote || '',
  })
  adminModal.value = 'combo-review'
  loadDuplicateCombos(combo, { silent: true })
}

async function selectInlineReviewCombo(combo) {
  activeInlineReviewCombo.value = combo
  duplicateCombos.value = []
  duplicateCheckExact.value = false
  duplicateCheckOverridden.value = false
  Object.assign(reviewForm, {
    rejectionReason: combo.rejectionReason || combo.manualReviewReason || '',
    difficulty: combo.difficulty || '',
    difficultyNote: combo.difficultyNote || '',
  })
}

function closeComboReview() {
  activeReviewCombo.value = null
  duplicateCombos.value = []
  duplicateCheckExact.value = false
  duplicateCheckOverridden.value = false
  duplicateCheckLoading.value = false
}

async function loadDuplicateCombos(combo, { silent = false } = {}) {
  if (!combo?.id || duplicateCheckLoading.value) return
  const requestedComboId = Number(combo.id)
  duplicateCheckLoading.value = true
  try {
    const result = await api.getAdminComboDuplicates(combo.id)
    if (Number(activeReviewCombo.value?.id) !== requestedComboId) return
    duplicateCombos.value = Array.isArray(result?.candidates) ? result.candidates : []
    duplicateCheckExact.value = Boolean(result?.exactDuplicate)
    duplicateCheckOverridden.value = false
    if (!silent) {
      showMessage(duplicateCombos.value.length
        ? `发现 ${duplicateCombos.value.length} 条重复候选`
        : '未发现重复连招')
    }
  } catch (error) {
    if (Number(activeReviewCombo.value?.id) !== requestedComboId) return
    showMessage(error.message || '重复检测失败', 'error')
  } finally {
    if (Number(activeReviewCombo.value?.id) === requestedComboId) duplicateCheckLoading.value = false
  }
}

function duplicateMatchLabel(matchType) {
  return {
    exact: '完全重复',
    'same-route': '同路线变体',
    historical: '历史驳回路线',
    similar: '高度相似',
  }[matchType] || '相似路线'
}

function confirmNotDuplicate() {
  duplicateCheckOverridden.value = true
  showMessage('已允许人工判定为非重复')
}

async function rejectAsDuplicate(candidate) {
  if (!activeReviewCombo.value || !candidate?.combo?.id) return
  reviewForm.rejectionReason = `连招重复，已有连招：#${candidate.combo.id}`
  await submitComboReview('rejected')
}

async function submitComboReview(status) {
  if (!activeReviewCombo.value) return
  await runAction(status === 'approved' ? '连招已通过' : '连招已驳回', async () => {
    await api.reviewAdminCombo(activeReviewCombo.value.id, {
      status,
      rejectionReason: reviewForm.rejectionReason,
      difficulty: reviewForm.difficulty,
      difficultyNote: reviewForm.difficultyNote,
    })
    closeAdminModal()
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function markComboManualReview() {
  if (!activeReviewCombo.value) return
  await runAction('已标记待处理', async () => {
    await api.reviewAdminCombo(activeReviewCombo.value.id, {
      status: 'manual_review',
      rejectionReason: reviewForm.rejectionReason || '需要管理员处理',
      difficulty: reviewForm.difficulty,
      difficultyNote: reviewForm.difficultyNote,
    })
    closeAdminModal()
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function rejectComboVideo() {
  if (!activeReviewCombo.value) return
  await runAction('连招已因视频违规驳回', async () => {
    await api.updateAdminComboVideoReview(activeReviewCombo.value.id, {
      status: 'video_rejected',
      reason: '视频违规',
    })
    closeAdminModal()
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function submitInlineComboReview(status) {
  if (!activeInlineReviewCombo.value) return
  await runAction(status === 'approved' ? '连招已通过' : '连招已驳回', async () => {
    await api.reviewAdminCombo(activeInlineReviewCombo.value.id, {
      status,
      rejectionReason: reviewForm.rejectionReason,
      difficulty: reviewForm.difficulty,
      difficultyNote: reviewForm.difficultyNote,
    })
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function markInlineManualReview() {
  if (!activeInlineReviewCombo.value) return
  await runAction('已标记待处理', async () => {
    await api.reviewAdminCombo(activeInlineReviewCombo.value.id, {
      status: 'manual_review',
      rejectionReason: reviewForm.rejectionReason || '需要管理员处理',
      difficulty: reviewForm.difficulty,
      difficultyNote: reviewForm.difficultyNote,
    })
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function rejectInlineVideo() {
  if (!activeInlineReviewCombo.value) return
  await runAction('连招已因视频违规驳回', async () => {
    await api.updateAdminComboVideoReview(activeInlineReviewCombo.value.id, {
      status: 'video_rejected',
      reason: '视频违规',
    })
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function deleteCombo(id) {
  const confirmed = await ui.confirmDialog({
    title: '删除连招',
    message: '确定删除该连招？',
    tone: 'danger',
  })
  if (!confirmed) return
  await runAction('连招已删除', async () => {
    await api.deleteAdminCombo(id)
    await Promise.all([loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function sendAnnouncement() {
  await runAction('指南已保存', async () => {
    const payload = { ...announcementForm, published: Boolean(announcementForm.published) }
    if (editingAnnouncementId.value) await api.updateAdminAnnouncement(editingAnnouncementId.value, payload)
    else await api.createAdminAnnouncement(payload)
    resetAnnouncementForm()
    closeAdminModal()
    await Promise.all([loadAnnouncements(), loadDashboard()])
  })
}

function openAnnouncementModal(item = null) {
  if (item) editAnnouncement(item)
  else resetAnnouncementForm()
  adminModal.value = 'announcement'
}

function editAnnouncement(item) {
  editingAnnouncementId.value = item.id
  Object.assign(announcementForm, {
    title: item.title || '',
    content: item.content || '',
    level: item.level || 'info',
    published: item.published !== false,
  })
}

function resetAnnouncementForm() {
  editingAnnouncementId.value = null
  Object.assign(announcementForm, { title: '', content: '', level: 'info', published: true })
}

async function toggleAnnouncementPublished(item) {
  await runAction(item.published ? '指南已隐藏' : '指南已展示', async () => {
    await api.updateAdminAnnouncement(item.id, { ...item, published: !item.published })
    await Promise.all([loadAnnouncements(), loadDashboard()])
  })
}

async function deleteAnnouncement(id) {
  const confirmed = await ui.confirmDialog({
    title: '删除指南',
    message: '确定删除这条使用指南吗？',
    tone: 'danger',
  })
  if (!confirmed) return
  await runAction('指南已删除', async () => {
    await api.deleteAdminAnnouncement(id)
    if (editingAnnouncementId.value === id) resetAnnouncementForm()
    await Promise.all([loadAnnouncements(), loadDashboard()])
  })
}

function announcementLevelLabel(level) {
  return {
    info: '基础指南',
    warning: '重点指南',
    maintenance: '规则说明',
  }[level] || level || '基础指南'
}

function openReportModal(report, status) {
  activeReport.value = report
  Object.assign(reportActionForm, {
    status,
    resolution: report.resolution || '',
    batch: false,
  })
  adminModal.value = 'report'
}

function openBatchReportModal(status) {
  const targets = selectedRows('reports', reports.value)
  if (!targets.length) return
  activeReport.value = null
  Object.assign(reportActionForm, {
    status,
    resolution: '',
    batch: true,
  })
  adminModal.value = 'report'
}

async function submitReportAction() {
  const status = reportActionForm.status
  const resolution = reportResolutionText(status)
  if (reportActionForm.batch) {
    const targets = selectedRows('reports', reports.value)
    if (!targets.length) return
    await runAction(status === 'resolved' ? '批量处理完成' : '批量驳回完成', async () => {
      await api.updateAdminReportsBatch({
        ids: targets.map(report => report.id),
        status,
        resolution,
      })
      clearSelection('reports')
      closeAdminModal()
      await Promise.all([refreshReports(), loadDashboard()])
    })
    return
  }
  if (!activeReport.value) return
  await runAction(status === 'resolved' ? '举报已处理' : '举报已驳回', async () => {
    await api.updateAdminReport(activeReport.value.id, { status, resolution })
    closeAdminModal()
    await Promise.all([refreshReports(), loadDashboard()])
  })
}

async function rejectReportedCombo(report) {
  if (!report?.targetId) return
  await runAction('已驳回被举报连招', async () => {
    await api.reviewAdminCombo(report.targetId, {
      status: 'rejected',
      rejectionReason: reportActionForm.resolution || `举报处理：${reportReasonLabel(report.reason)}`,
    })
    await api.updateAdminReport(report.id, {
      status: 'resolved',
      resolution: '已根据举报驳回目标连招',
    })
    await Promise.all([refreshReports(), loadCombos(), refreshDashboardIfAdmin()])
  })
}

async function deleteReportedCombo(report) {
  if (!report?.targetId) return
  const confirmed = await ui.confirmDialog({
    title: '删除被举报连招',
    message: '确定删除该举报对象吗？',
    tone: 'danger',
  })
  if (!confirmed) return
  await runAction('已删除被举报连招', async () => {
    await api.deleteAdminCombo(report.targetId)
    await api.updateAdminReport(report.id, {
      status: 'resolved',
      resolution: '已根据举报删除目标连招',
    })
    await Promise.all([refreshReports(), loadCombos(), refreshDashboardIfAdmin()])
  })
}

function reportResolutionText(status) {
  return reportActionForm.resolution || (status === 'resolved' ? '管理员已处理' : '管理员已驳回')
}

function openFeedbackModal(feedback, status) {
  activeFeedback.value = feedback
  Object.assign(feedbackActionForm, {
    status,
    resolution: feedback.resolution || '',
    batch: false,
  })
  adminModal.value = 'feedback'
}

function openFeedbackDetailModal(feedback) {
  activeFeedback.value = feedback
  adminModal.value = 'feedback-detail'
}

function openBatchFeedbackModal(status) {
  const targets = selectedRows('feedbacks', feedbacks.value)
  if (!targets.length) return
  activeFeedback.value = null
  Object.assign(feedbackActionForm, {
    status,
    resolution: '',
    batch: true,
  })
  adminModal.value = 'feedback'
}

async function submitFeedbackAction() {
  const status = feedbackActionForm.status
  const resolution = feedbackResolutionText(status)
  if (feedbackActionForm.batch) {
    const targets = selectedRows('feedbacks', feedbacks.value)
    if (!targets.length) return
    await runAction(status === 'resolved' ? '批量处理完成' : '批量驳回完成', async () => {
      await api.updateAdminFeedbacksBatch({
        ids: targets.map(feedback => feedback.id),
        status,
        resolution,
      })
      clearSelection('feedbacks')
      closeAdminModal()
      await Promise.all([refreshFeedbacks(), loadDashboard()])
    })
    return
  }
  if (!activeFeedback.value) return
  await runAction(status === 'resolved' ? '反馈已处理' : '反馈已驳回', async () => {
    await api.updateAdminFeedback(activeFeedback.value.id, { status, resolution })
    closeAdminModal()
    await Promise.all([refreshFeedbacks(), loadDashboard()])
  })
}

function feedbackResolutionText(status) {
  return feedbackActionForm.resolution || (status === 'resolved' ? '管理员已处理' : '管理员已驳回')
}

function isComboReviewed(combo) {
  return ['approved', 'rejected'].includes(combo?.status)
}

function comboReviewStatusLabel(status) {
  return {
    pending: '待处理',
    manual_review: '待处理',
    approved: '已通过',
    rejected: '已驳回',
  }[status] || status || '状态未知'
}

function comboMatchesStatusFilter(combo, filter) {
  return !filter
    || (filter === 'reviewed' && combo.status === 'approved')
    || (filter === 'rejected' && combo.status === 'rejected')
    || (filter === 'manual_review' && ['manual_review', 'pending'].includes(combo.status))
}

function videoReviewStatusLabel(status) {
  return {
    unchecked: '未检测',
    video_checking: '检测中',
    video_approved: '视频通过',
    video_rejected: '视频违规',
  }[status] || '未检测'
}

async function openReportsByStatus(status) {
  reportStatus.value = status
  reportPage.value = 1
  activeSection.value = 'reports'
  await loadReports()
}

async function openDashboardTask(task) {
  activeSection.value = task.section
  if (task.section === 'combos') {
    comboStatusFilter.value = task.status || ''
    comboControlFilter.value = ''
    comboFilterCharacterId.value = ''
    if (!combos.value.length) await loadCombos()
    return
  }
  if (task.section === 'reports') {
    reportStatus.value = 'pending'
    reportPage.value = 1
    await loadReports()
    return
  }
  if (task.section === 'feedbacks') {
    feedbackStatus.value = 'pending'
    feedbackPage.value = 1
    await loadFeedbacks()
  }
}

async function focusComboFromDashboard(combo) {
  if (!combo?.id) return
  activeSection.value = 'combos'
  comboStatusFilter.value = combo.status === 'manual_review' ? 'manual_review' : ''
  if (!combos.value.length) await loadCombos()
  await nextTick()
  const target = combos.value.find(item => Number(item.id) === Number(combo.id))
  if (target) {
    await selectInlineReviewCombo(target)
  }
}

async function openReportTarget(report) {
  const kind = targetKindFromReport(report)
  const section = sectionForTargetKind(kind)
  const targetId = reportTargetIdForAdmin(report)
  if (!section || !targetId) {
    showMessage('该举报对象暂时无法定位', 'error')
    return
  }
  await focusAdminTarget(section, targetId, { updateUrl: true })
}

function reportStatusLabel(status) {
  return {
    pending: '待处理',
    processing: '处理中',
    resolved: '已处理',
    rejected: '已驳回',
  }[status] || status || '未知'
}

function reportReasonLabel(reason) {
  return {
    spam: '垃圾信息',
    abuse: '攻击谩骂',
    duplicate: '重复内容',
    wrong_data: '数据错误',
    illegal: '违规内容',
    other: '其他原因',
  }[reason] || reason || '未填写原因'
}

function reportTargetLabel(report) {
  return {
    combo: '连招',
    user: '用户',
  }[report?.targetType] || report?.targetType || '对象'
}

function feedbackStatusLabel(status) {
  return {
    pending: '待处理',
    processing: '处理中',
    resolved: '已处理',
    rejected: '已驳回',
  }[status] || status || '未知'
}

function auditActionLabel(action) {
  return auditActionOptions.value.find(item => item.value === action)?.label || action || '未知动作'
}

function isHighRiskAudit(log) {
  return ['delete_combo', 'combo:delete', 'ban_user', 'review_combo_video'].some(action => String(log?.action || '').includes(action))
}

function canOpenAuditTarget(log) {
  return Boolean(log?.targetType && log?.targetId && ['combo', 'user', 'report', 'feedback'].includes(log.targetType))
}

async function openAuditTarget(log) {
  if (!canOpenAuditTarget(log)) return
  if (log.targetType === 'combo') {
    await focusAdminTarget('combos', log.targetId, { updateUrl: true })
    return
  }
  if (log.targetType === 'user') {
    await focusAdminTarget('users', log.targetId, { updateUrl: true })
    return
  }
  if (log.targetType === 'report') {
    activeSection.value = 'reports'
    reportStatus.value = ''
    await loadReports()
    return
  }
  if (log.targetType === 'feedback') {
    activeSection.value = 'feedbacks'
    feedbackStatus.value = ''
    await loadFeedbacks()
  }
}

function feedbackReasonLabel(reason) {
  return {
    abuse: '体验问题',
    duplicate: '重复内容',
    wrong_data: '数据错误',
    other: '其他建议',
  }[reason] || reason || '未填写类型'
}

function reportTargetLink(report) {
  if (report?.targetUrl) return report.targetUrl
  if (!report?.targetId) return ''
  if (report.targetType === 'combo') return `/combos/${report.targetId}`
  if (report.targetType === 'user') return `/admin?section=users&targetId=${report.targetId}`
  return ''
}

function reportTargetStatusLabel(status) {
  return {
    pending: '待处理',
    approved: '已通过',
    rejected: '已驳回',
    active: '正常',
    banned: '已封禁',
    deleted: '已删除',
  }[status] || status || '状态未知'
}

watch(canAccessAdmin, value => {
  if (!value) {
    hasLoadedAdminData.value = false
    disconnectAdminEvents()
    return
  }
  connectAdminEvents()
  if (!route.query.section && !route.query.targetId) activeSection.value = defaultAdminSection.value
  if (!hasLoadedAdminData.value) loadAll().then(loaded => {
    if (loaded) applyAdminTargetFromQuery()
  })
}, { immediate: true })

watch(activeSection, section => {
  if (!canAccessAdmin.value) return
  if (!sections.value.some(item => item.key === section)) {
    activeSection.value = sections.value[0]?.key || 'combos'
    return
  }
  if (!isAdmin.value && section === 'combos') {
    loadCombos()
    return
  }
  if (section === 'dashboard') {
    refreshDashboardIfAdmin()
  }
  if (section === 'frames') {
    loadFrames()
    loadFrameOps()
  }
  if (section === 'reports') loadReports()
  if (section === 'feedbacks') loadFeedbacks()
  if (section === 'announcements') loadAnnouncements()
  if (section === 'audit') loadAuditLogs()
})

watch([comboFilterCharacterId, comboStatusFilter, comboControlFilter, comboQueueQuery], () => {
  if (activeSection.value !== 'combos') return
  comboPage.value = 1
  loadCombos()
})

watch(frameSearch, () => {
  if (activeSection.value !== 'frames') return
  window.clearTimeout(frameSearchTimer)
  frameSearchTimer = window.setTimeout(refreshFrames, 300)
})

watch(() => comboForm.characterId, () => {
  if (hydratingComboForm || adminModal.value !== 'combo') return
  comboForm.followupParentId = ''
  comboForm.pressureType = ''
  loadAdminParentCombos()
})

watch(approvedFollowupCombosByParentId, groups => {
  expandedComboFollowupQueues.value = expandedComboFollowupQueues.value.filter(id => groups.has(Number(id)))
})

watch(
  () => [route.query.section, route.query.targetId, canAccessAdmin.value, hasLoadedAdminData.value],
  () => {
    if (hasLoadedAdminData.value) applyAdminTargetFromQuery()
  },
)

onMounted(() => {
  window.addEventListener('resize', resizeDashboardCharts)
  window.addEventListener('focus', refreshDashboardIfAdmin)
  document.addEventListener('visibilitychange', handleAdminVisibilityChange)
  connectAdminEvents()
  if (canAccessAdmin.value && !hasLoadedAdminData.value) loadAll().then(loaded => {
    if (loaded) applyAdminTargetFromQuery()
  })
})

onBeforeUnmount(() => {
  window.clearTimeout(frameSearchTimer)
  window.clearTimeout(showMessage.timer)
  adminParentComboRequestId += 1
  window.removeEventListener('resize', resizeDashboardCharts)
  window.removeEventListener('focus', refreshDashboardIfAdmin)
  document.removeEventListener('visibilitychange', handleAdminVisibilityChange)
  cancelAvatarCrop()
  disconnectAdminEvents()
  disposeDashboardCharts()
})

function handleAdminVisibilityChange() {
  if (document.visibilityState === 'visible') {
    refreshDashboardIfAdmin()
  }
}
</script>

<style scoped>
.admin-console {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 248px 1fr;
  background: var(--ink-bg);
  --el-bg-color: #111827;
  --el-bg-color-overlay: #111827;
  --el-border-color: #243044;
  --el-border-color-light: #243044;
  --el-text-color-primary: var(--ink-text);
  --el-text-color-regular: var(--ink-text-soft);
  --el-fill-color-light: #0f1622;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 24px 18px;
  background: #0f1622;
  border-right: 1px solid #243044;
}

.admin-brand {
  display: grid;
  gap: 4px;
  color: var(--ink-text);
  text-decoration: none;
}
.admin-brand span {
  width: fit-content;
  padding: 3px 8px;
  background: var(--accent-red);
  color: #190607;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 900;
}
.admin-brand strong {
  font-family: inherit;
  font-size: 22px;
  font-weight: 900;
  font-style: normal;
  text-transform: uppercase;
}

.admin-nav {
  display: grid;
  gap: 6px;
}

.admin-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  color: var(--ink-text-soft);
  border: 1px solid transparent;
  text-align: left;
  font-family: 'JetBrains Mono';
  font-size: 13px;
  transition: background 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}
.admin-nav-item:hover,
.admin-nav-item.active {
  color: var(--ink-text);
  background: rgba(91, 214, 199, 0.14);
  border-color: rgba(91, 214, 199, 0.34);
}
.admin-nav-item .material-symbols-outlined {
  font-size: 19px;
}

.admin-sidebar-foot {
  margin-top: auto;
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
}
.admin-sidebar-foot small,
.admin-item p,
.admin-item small,
.metric-card small,
.admin-topbar p {
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.admin-sidebar-foot strong {
  color: #fff;
}

.admin-main {
  min-width: 0;
  padding: 28px;
}

.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
}
.admin-topbar h1 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 34px;
  font-weight: 900;
  font-style: normal;
}
.admin-top-actions,
.row-actions,
.denied-actions,
.admin-filter,
.bulk-bar,
.bulk-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.admin-stack {
  display: grid;
  gap: 18px;
}

.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(168px, 1fr));
  gap: 14px;
}
.ops-workbench {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}
.ops-task-card {
  min-height: 118px;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(22, 25, 27, 0.9);
}
.ops-task-card .material-symbols-outlined {
  color: var(--accent-cyan-soft);
  font-size: 30px;
}
.ops-task-card strong {
  display: block;
  color: #fff;
  font-family: 'JetBrains Mono';
  font-size: 28px;
  line-height: 1;
}
.ops-task-card p,
.ops-task-card small {
  margin: 5px 0 0;
  color: var(--ink-text-soft);
}
.ops-task-card small {
  display: block;
  color: var(--ink-muted);
  font-size: 12px;
}
.ops-task-card button {
  min-height: 32px;
  padding: 0 12px;
  border-color: rgba(91, 214, 199, 0.34);
  color: var(--accent-cyan-soft);
  background: rgba(91, 214, 199, 0.08);
}
.ops-task-card.danger {
  border-color: rgba(255, 118, 135, 0.34);
  background: rgba(255, 118, 135, 0.075);
}
.ops-task-card.danger .material-symbols-outlined {
  color: var(--state-error);
}
.ops-task-card.warning {
  border-color: rgba(255, 193, 93, 0.34);
}
.ops-task-card.warning .material-symbols-outlined {
  color: var(--accent-amber);
}
.ops-focus-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.ops-focus-panel {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(22, 25, 27, 0.88);
}
.ops-risk-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 11px 12px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(0,0,0,0.18);
  cursor: pointer;
}
.ops-risk-row:hover {
  border-color: rgba(91, 214, 199, 0.38);
  background: rgba(91, 214, 199, 0.07);
}
.ops-risk-row strong,
.recent-audit-list strong {
  color: #fff;
}
.ops-risk-row p,
.ops-risk-row small,
.recent-audit-list p,
.recent-audit-list small {
  margin: 4px 0 0;
  color: var(--ink-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}
.ops-risk-row mark {
  min-width: 38px;
  padding: 6px 8px;
  color: var(--accent-amber);
  border: 1px solid rgba(255, 193, 93, 0.36);
  background: rgba(255, 193, 93, 0.1);
  text-align: center;
}
.ops-risk-row mark.danger {
  color: var(--state-error);
  border-color: rgba(255, 118, 135, 0.34);
  background: rgba(255, 118, 135, 0.08);
}
.recent-audit-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 8px;
  max-height: 360px;
  overflow: auto;
  padding-right: 4px;
}
.recent-audit-list article {
  height: 138px;
  min-height: 138px;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 6px;
  padding: 10px;
  overflow: hidden;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(0,0,0,0.18);
}
.recent-audit-list article strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent-audit-list article p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent-audit-detail {
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
  line-height: 1.55;
  word-break: break-all;
}
.overview-card,
.dashboard-card {
  border: 1px solid #243044;
  background: #111827;
  border-radius: 8px;
}
.overview-card :deep(.el-card__body),
.dashboard-card :deep(.el-card__body) {
  padding: 18px;
}
.dashboard-card :deep(.el-card__header) {
  padding: 16px 18px;
  border-bottom: 1px solid #243044;
}
.overview-card.muted {
  opacity: .78;
}
.overview-card-head,
.dashboard-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.overview-card-head .el-icon {
  color: var(--accent-red);
  font-size: 20px;
}
.metric-growth {
  color: #78e1d3;
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.metric-growth.down {
  color: var(--state-error);
}
.metric-growth.live {
  padding: 2px 7px;
  color: var(--accent-cyan-soft);
  border: 1px solid rgba(91, 214, 199, 0.34);
  border-radius: 999px;
  background: rgba(91, 214, 199, 0.08);
}
.metric-growth.muted {
  color: #7c8798;
}
.overview-card strong {
  display: block;
  margin-top: 18px;
  color: #fff;
  font-size: 30px;
  font-weight: 900;
}
.overview-card small {
  display: block;
  margin-top: 6px;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.dashboard-chart-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.dashboard-card-head h2 {
  margin: 0;
  color: var(--ink-text);
  font-size: 18px;
  font-weight: 800;
}
.dashboard-card-head span {
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.chart-surface,
.chart-empty {
  height: 320px;
}
.chart-empty {
  display: grid;
  place-content: center;
  gap: 8px;
  text-align: center;
  color: var(--ink-muted);
}
.chart-empty strong {
  color: var(--ink-text);
}
.chart-empty p {
  max-width: 42ch;
  margin: 0;
  line-height: 1.6;
}
.dashboard-table {
  --el-table-bg-color: #111827;
  --el-table-tr-bg-color: #111827;
  --el-table-header-bg-color: #0f1622;
  --el-table-row-hover-bg-color: #132233;
  --el-table-border-color: #243044;
  --el-fill-color-lighter: #0f1622;
  width: 100%;
}
.metric-card,
.admin-panel,
.admin-item {
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(22, 25, 27, 0.88);
}
.metric-card {
  display: grid;
  gap: 9px;
  padding: 18px;
}
.metric-card .material-symbols-outlined {
  color: var(--accent-red);
}
.metric-card strong {
  color: #fff;
  font-family: inherit;
  font-size: 30px;
}

.admin-two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}
.admin-panel {
  padding: 18px;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}
.panel-head h2 {
  margin: 0;
  color: var(--ink-text);
  font-family: inherit;
  font-size: 20px;
  font-weight: 900;
  font-style: normal;
}
.user-filter-row.user-filter-row {
  align-items: stretch;
  flex-wrap: nowrap;
  width: fit-content;
  max-width: 100%;
  margin: -2px 0 14px;
}
.user-filter-row.user-filter-row input {
  flex: 0 1 360px;
  width: 360px;
  min-width: 0;
}
.user-filter-row.user-filter-row select {
  flex: 0 0 auto;
}
.user-filter-row.user-filter-row button {
  flex: 0 0 auto;
}
.report-filter-row.report-filter-row {
  align-items: stretch;
  flex-wrap: wrap;
  margin: -2px 0 14px;
}
.report-filter-row.report-filter-row select,
.report-filter-row.report-filter-row input {
  flex: 0 0 auto;
}

.audit-filter-row.audit-filter-row .audit-date-input {
  flex: 0 0 176px;
  width: 176px;
  min-width: 176px;
  max-width: 176px;
}

.bulk-bar,
.bulk-inline {
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.bulk-bar {
  justify-content: flex-start;
  padding: 10px 12px;
  margin-bottom: 10px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.025);
}
.bulk-inline {
  justify-content: flex-end;
}
.select-all {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--ink-text-soft);
}
.select-box,
.select-all input {
  width: 16px;
  height: 16px;
  min-height: 16px;
  padding: 0;
  accent-color: var(--accent-red);
}

.task-list,
.compact-stats,
.admin-list-grid,
.status-stack {
  display: grid;
  gap: 10px;
}
.task-list button,
.compact-stats span,
.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.03);
  color: #e6ebe8;
}
.status-row {
  min-height: 45px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.status-row strong {
  color: #fff;
  font-family: inherit;
  font-size: 22px;
}
.action-row {
  width: 100%;
  cursor: pointer;
}

.source-panel,
.review-panel {
  display: grid;
  gap: 12px;
}
.source-panel {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}
.source-panel h2,
.source-panel p {
  margin: 0;
}
.source-panel h2 {
  color: var(--ink-text);
  font-family: inherit;
  font-size: 20px;
  font-weight: 900;
  font-style: normal;
}
.source-panel p {
  margin-top: 5px;
  color: var(--ink-text-soft);
}
.source-panel .panel-head {
  grid-column: 1 / -1;
  margin-bottom: 0;
}
.source-panel > p {
  grid-column: 1 / -1;
  margin: 0;
}
.review-panel {
  margin-bottom: 12px;
  padding: 14px;
  border: 1px solid rgba(91, 214, 199, 0.28);
  background: rgba(91, 214, 199, 0.065);
}
.combo-review-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: stretch;
  padding: 14px;
  border: 1px solid rgba(91, 214, 199, 0.32);
  background:
    linear-gradient(90deg, rgba(255, 64, 84, 0.11), transparent 46%),
    rgba(0, 0, 0, 0.16);
}
.combo-review-title {
  min-width: 0;
  display: grid;
  align-content: center;
  gap: 8px;
}
.combo-review-title strong {
  color: #fff;
  font-size: 24px;
  line-height: 1.18;
  overflow-wrap: anywhere;
}
.combo-review-title p {
  margin: 0;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  overflow-wrap: anywhere;
}
.combo-review-status {
  width: max-content;
  padding: 4px 8px;
  color: var(--accent-cyan-soft);
  border: 1px solid rgba(91, 214, 199, 0.42);
  background: rgba(91, 214, 199, 0.12);
  font-family: 'JetBrains Mono';
  font-size: 11px;
  font-weight: 900;
}
.combo-review-status.approved {
  color: #bfe8cc;
  border-color: rgba(106, 181, 137, 0.42);
  background: rgba(106, 181, 137, 0.12);
}
.combo-review-status.rejected {
  color: var(--state-error);
  border-color: rgba(255, 118, 135, 0.42);
  background: rgba(255, 118, 135, 0.1);
}
.combo-review-status.manual_review {
  color: #ffe4ad;
  border-color: rgba(255, 193, 93, 0.46);
  background: rgba(255, 193, 93, 0.12);
}
.combo-review-scoreboard {
  display: grid;
  grid-template-columns: repeat(4, minmax(76px, 1fr));
  gap: 8px;
}
.combo-review-scoreboard div,
.combo-review-detail-item {
  min-width: 0;
  padding: 10px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(0,0,0,0.18);
}
.combo-review-scoreboard strong {
  display: block;
  color: #fff;
  font-family: 'JetBrains Mono';
  font-size: 22px;
  line-height: 1;
}
.combo-review-scoreboard span,
.combo-review-detail-item span,
.combo-review-notes span {
  display: block;
  margin-top: 6px;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}
.combo-review-notation-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(255, 64, 84, 0.28);
  background:
    linear-gradient(135deg, rgba(255, 64, 84, 0.08), transparent 38%),
    rgba(0, 0, 0, 0.16);
}
.review-section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.review-section-head .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 20px;
}
.review-section-head strong {
  color: #fff;
}
.review-section-head small,
.review-section-head a {
  margin-left: auto;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.review-section-head a {
  color: var(--accent-cyan-soft);
  text-decoration: none;
}
.combo-review-raw-route {
  margin: 0;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  overflow-wrap: anywhere;
}
.combo-review-detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}
.combo-review-detail-item strong {
  display: block;
  margin-top: 4px;
  color: var(--ink-text);
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}
.combo-review-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.combo-review-tag,
.combo-review-byline {
  min-height: 30px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid rgba(91, 214, 199, 0.32);
  color: var(--accent-cyan-soft);
  background: rgba(91, 214, 199, 0.1);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.combo-review-tag.warning {
  color: #ffe4ad;
  border-color: rgba(255, 193, 93, 0.42);
  background: rgba(255, 193, 93, 0.11);
}
.combo-review-byline {
  color: var(--ink-text-soft);
  border-color: rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.03);
}
.combo-review-notes {
  display: grid;
  gap: 8px;
}
.combo-review-notes div {
  padding: 11px 12px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.025);
}
.combo-review-notes p {
  margin: 5px 0 0;
  color: var(--ink-text-soft);
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.combo-review-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 12px;
  align-items: start;
}
.combo-review-decision {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(0,0,0,0.16);
}
.review-panel strong,
.duplicate-list strong {
  color: #fff;
}
.review-panel p,
.duplicate-list p {
  margin: 4px 0 0;
  color: var(--ink-text-soft);
}
.review-checks,
.duplicate-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.review-video-card {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(0,0,0,0.18);
}
.review-video {
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  background: #08090b;
}
.review-video-card a {
  width: max-content;
  color: #7dd8c4;
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.review-video-empty {
  min-height: 180px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  padding: 18px;
  color: var(--ink-text-soft);
  border: 1px dashed rgba(255,255,255,0.14);
  background: rgba(255,255,255,0.035);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  text-align: center;
}
.review-video-empty .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 30px;
}
.review-video-empty strong,
.review-video-empty p {
  margin: 0;
}
.check-pill {
  padding: 6px 9px;
  border: 1px solid rgba(255,255,255,0.1);
  color: var(--ink-text-soft);
  background: rgba(255,255,255,0.035);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.check-pill.ok {
  color: #bfe8cc;
  border-color: rgba(106, 181, 137, 0.36);
  background: rgba(106, 181, 137, 0.1);
}
.check-pill.danger {
  color: var(--state-error);
  border-color: rgba(255, 118, 135, 0.34);
}
.combo-status-tabs,
.combo-control-tabs {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.025);
}
.combo-status-tab,
.combo-control-tab {
  min-height: 32px;
  gap: 8px;
  padding: 0 10px;
  border-color: transparent;
  background: transparent;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.combo-status-tab strong,
.combo-control-tab strong {
  color: var(--ink-text-soft);
  font-family: inherit;
  font-size: 12px;
}
.combo-status-tab:hover,
.combo-control-tab:hover {
  border-color: rgba(91, 214, 199, 0.34);
}
.combo-status-tab.active {
  color: var(--accent-cyan-soft);
  border-color: rgba(91, 214, 199, 0.46);
  background: rgba(91, 214, 199, 0.14);
}
.combo-status-tab.active strong {
  color: #fff;
}
.combo-status-tab.rejected.active {
  color: var(--state-error);
  border-color: rgba(255, 118, 135, 0.46);
  background: rgba(255, 86, 105, 0.14);
}
.combo-control-tab.active {
  color: var(--ink-text);
  border-color: rgba(91, 214, 199, 0.46);
  background: rgba(91, 214, 199, 0.12);
}
.combo-control-tab.is-classic.active {
  border-color: rgba(167, 139, 250, 0.82);
  background: rgba(139, 92, 246, 0.34);
}
.combo-control-tab.is-modern.active {
  border-color: rgba(251, 146, 60, 0.82);
  background: rgba(249, 115, 22, 0.34);
}
.combo-control-tab.is-world-tour.active {
  border-color: rgba(49, 213, 230, 0.72);
  background: rgba(49, 213, 230, 0.2);
}
.combo-control-tab.active strong {
  color: #fff;
}
.combo-review-board {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(520px, 1fr) minmax(280px, 320px);
  gap: 12px;
  align-items: stretch;
  min-height: 0;
}
.combo-review-queue,
.combo-review-preview,
.combo-review-actions {
  min-width: 0;
  display: grid;
  gap: 10px;
}
.combo-review-queue {
  height: min(760px, calc(100vh - 270px));
  align-content: start;
  overflow: auto;
  padding-right: 4px;
}
.combo-queue-toolbar {
  position: sticky;
  top: 0;
  z-index: 2;
  display: grid;
  gap: 8px;
  padding: 0 0 8px;
  background: var(--admin-panel, #111820);
}
.combo-queue-toolbar > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.045);
}
.combo-queue-toolbar strong {
  color: #fff;
  font-family: 'JetBrains Mono';
  font-size: 18px;
}
.combo-queue-toolbar span {
  color: var(--ink-muted);
  font-size: 12px;
}
.combo-queue-search {
  position: relative;
  display: block;
}
.combo-queue-search .material-symbols-outlined {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ink-muted);
  font-size: 18px;
  pointer-events: none;
}
.combo-queue-search input {
  min-height: 36px;
  padding-left: 36px;
}
.queue-combo-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 5px 8px;
  min-height: 84px;
  padding: 10px;
  text-align: left;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(0,0,0,0.18);
  color: var(--ink-text-soft);
}
.queue-combo-row.active,
.queue-combo-row:hover {
  border-color: rgba(91, 214, 199, 0.42);
  background: rgba(91, 214, 199, 0.08);
}
.queue-combo-row span,
.queue-combo-row mark {
  width: max-content;
  padding: 3px 7px;
  font-family: 'JetBrains Mono';
  font-size: 11px;
}
.queue-combo-row span {
  color: var(--accent-cyan-soft);
  border: 1px solid rgba(91, 214, 199, 0.32);
  background: rgba(91, 214, 199, 0.08);
}
.queue-combo-row strong {
  grid-column: 1 / -1;
  color: #fff;
  overflow-wrap: anywhere;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.queue-combo-row small {
  grid-column: 1 / -1;
  color: var(--ink-muted);
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}
.queue-combo-row mark {
  justify-self: end;
  color: var(--accent-amber);
  border: 1px solid rgba(255, 193, 93, 0.34);
  background: rgba(255, 193, 93, 0.08);
}
.combo-review-preview {
  max-height: min(760px, calc(100vh - 270px));
  align-content: start;
  overflow: auto;
  padding-right: 4px;
}
.combo-review-hero.compact {
  grid-template-columns: minmax(0, 1fr);
}
.combo-review-workbench.inline {
  grid-template-columns: minmax(0, 1.05fr) minmax(240px, 0.95fr);
}
.combo-parent-panel {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(91, 214, 199, 0.24);
  background: rgba(91, 214, 199, 0.055);
}
.combo-parent-panel > strong {
  color: #fff;
}
.combo-parent-panel p {
  margin: 0;
  color: var(--ink-muted);
  overflow-wrap: anywhere;
}
.combo-review-actions {
  position: sticky;
  top: 14px;
  padding: 12px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(0,0,0,0.2);
  align-content: start;
  max-height: min(760px, calc(100vh - 270px));
  overflow: auto;
}
.combo-review-actions h3 {
  margin: 0;
  color: #fff;
  font-size: 16px;
}
.action-button-grid {
  display: grid;
  gap: 8px;
}
.action-button-grid > button,
.action-button-grid > a {
  width: 100%;
  justify-content: center;
}
.review-action-note {
  display: grid;
  gap: 8px;
  min-width: 0;
  margin-top: 2px;
}
.review-action-note span {
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.review-action-note textarea {
  width: 100%;
  min-height: 128px;
  resize: vertical;
}
.duplicate-list {
  display: grid;
  gap: 8px;
}
.duplicate-list article {
  display: grid;
  gap: 8px;
  padding: 9px 10px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(0,0,0,0.16);
}
.duplicate-loading {
  margin: 0;
  color: var(--accent-cyan-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.duplicate-item-head,
.duplicate-item-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.duplicate-item-head strong {
  min-width: 0;
}
.duplicate-match-pill {
  flex: 0 0 auto;
  padding: 4px 7px;
  color: #ffe7b3;
  border: 1px solid rgba(245, 158, 11, 0.34);
  background: rgba(245, 158, 11, 0.09);
  font-family: 'JetBrains Mono';
  font-size: 10px;
}
.duplicate-match-pill.exact {
  color: var(--state-error);
  border-color: rgba(255, 118, 135, 0.42);
  background: rgba(255, 64, 84, 0.1);
}
.duplicate-item-actions a {
  color: var(--accent-cyan-soft);
  font-size: 12px;
  font-weight: 900;
}
.duplicate-item-actions button {
  width: auto;
  min-height: 34px;
  padding: 6px 10px;
}

input,
textarea,
select {
  min-height: 38px;
  width: 100%;
  border: 1px solid rgba(255,255,255,0.12);
  background: var(--ink-surface-low);
  color: var(--ink-text);
  padding: 9px 10px;
  outline: none;
  color-scheme: dark;
}

.admin-filter select {
  width: fit-content;
  min-width: 116px;
  max-width: 220px;
}
select option {
  background: var(--ink-surface-low);
  color: var(--ink-text);
}
select option:disabled {
  color: #7d8781;
}
textarea {
  min-height: 96px;
  resize: vertical;
}
input:focus,
textarea:focus,
select:focus {
  border-color: rgba(91, 214, 199, 0.72);
}

button,
.admin-link-btn {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 12px;
  border: 1px solid rgba(255,255,255,0.12);
  color: var(--ink-text);
  background: rgba(255,255,255,0.055);
  text-decoration: none;
  cursor: pointer;
}
button:hover,
.admin-link-btn:hover {
  border-color: rgba(91, 214, 199, 0.58);
}
.admin-primary {
  border-color: rgba(91, 214, 199, 0.54);
  background: var(--accent-red);
  color: #190607;
  font-weight: 900;
}
button:disabled,
select:disabled {
  opacity: .55;
  cursor: not-allowed;
}
.danger {
  color: var(--state-error);
  border-color: rgba(255, 118, 135, 0.34);
  background: rgba(255, 118, 135, 0.08);
}
.ghost {
  background: transparent;
}

.admin-toast-layer {
  position: fixed;
  top: max(16px, env(safe-area-inset-top));
  left: 50%;
  z-index: 240;
  width: min(calc(100vw - 32px), 560px);
  pointer-events: none;
  transform: translateX(-50%);
}

.admin-message {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  margin: 0;
  padding: 11px 10px 11px 14px;
  border: 1px solid rgba(106, 181, 137, 0.42);
  border-radius: 6px;
  color: #bfe8cc;
  background: rgba(21, 35, 29, 0.96);
  box-shadow: 0 6px 8px rgba(0, 0, 0, 0.28);
  font-size: 13px;
  line-height: 1.4;
  pointer-events: auto;
}
.admin-message > .material-symbols-outlined {
  flex: 0 0 auto;
  color: #7de0a0;
  font-size: 20px;
}
.admin-message-copy {
  min-width: 0;
  flex: 1 1 auto;
  overflow-wrap: anywhere;
}
.admin-message-close {
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  padding: 0;
  color: rgba(255, 255, 255, 0.68);
  border: 1px solid transparent;
  background: transparent;
}
.admin-message-close:hover,
.admin-message-close:focus-visible {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.08);
  outline: none;
}
.admin-message-close .material-symbols-outlined {
  font-size: 17px;
}
.admin-message.error {
  border-color: rgba(255, 118, 135, 0.36);
  color: var(--state-error);
  background: rgba(54, 22, 28, 0.96);
}
.admin-message.error > .material-symbols-outlined {
  color: #ff8b9b;
}

.admin-toast-enter-active,
.admin-toast-leave-active {
  transition: opacity 220ms ease-out, transform 220ms ease-out, filter 220ms ease-out;
}
.admin-toast-enter-from,
.admin-toast-leave-to {
  opacity: 0;
  filter: blur(2px);
  transform: translate(-50%, -10px);
}

@media (max-width: 640px) {
  .admin-toast-layer {
    top: max(12px, env(safe-area-inset-top));
    width: calc(100vw - 24px);
  }

  .admin-message {
    padding-left: 12px;
    font-size: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .admin-toast-enter-active,
  .admin-toast-leave-active {
    transition: opacity 120ms linear;
  }

  .admin-toast-enter-from,
  .admin-toast-leave-to {
    filter: none;
    transform: translateX(-50%);
  }
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.form-grid textarea {
  grid-column: 1 / -1;
}
.frame-form,
.combo-form {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.frame-form .wide-field {
  grid-column: span 2;
}
.ban-form-grid .wide-field,
.field-line.wide-field {
  grid-column: 1 / -1;
}
.field-line {
  display: grid;
  gap: 7px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.field-line span {
  color: var(--ink-muted);
}
.modal-field-grid {
  display: grid;
  gap: 12px;
}
.modal-field-row {
  display: grid;
  grid-template-columns: minmax(150px, 220px) minmax(0, 1fr);
  align-items: start;
  gap: 14px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.modal-field-row > span {
  min-height: 38px;
  display: flex;
  align-items: center;
  color: var(--ink-muted);
  line-height: 1.45;
}
.modal-field-row > input,
.modal-field-row > select,
.modal-field-row > textarea,
.modal-field-row > .combo-route-builder,
.modal-field-row > .modal-field-stack,
.modal-field-row > .admin-segmented-control,
.modal-field-row > .admin-tag-picker,
.modal-field-row > .modal-check {
  min-width: 0;
}
.modal-field-row textarea {
  min-height: 110px;
}
.modal-field-row.is-check {
  align-items: center;
}
.modal-field-stack {
  display: grid;
  gap: 10px;
}

.admin-segmented-control {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 4px;
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
}

.admin-segmented-option {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-text-soft);
  border: 1px solid transparent;
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 900;
}

.admin-segmented-option.active {
  color: var(--ink-text);
}

.admin-segmented-option.is-classic.active {
  background: rgba(139, 92, 246, 0.34);
  border-color: rgba(167, 139, 250, 0.82);
  box-shadow: inset 0 0 0 1px rgba(221, 214, 254, 0.14);
}

.admin-segmented-option.is-modern.active {
  background: rgba(249, 115, 22, 0.34);
  border-color: rgba(251, 146, 60, 0.82);
  box-shadow: inset 0 0 0 1px rgba(255, 237, 213, 0.14);
}

.admin-segmented-option.is-world-tour.active {
  background: rgba(49, 213, 230, 0.2);
  border-color: rgba(49, 213, 230, 0.72);
  box-shadow: inset 0 0 0 1px rgba(207, 250, 254, 0.1);
}

.admin-tag-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-tag-option {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 0 11px;
  color: var(--ink-text-soft);
  background: var(--ink-surface-low);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  font-family: 'JetBrains Mono';
  font-size: 12px;
  cursor: pointer;
}

.admin-tag-option:has(input:checked) {
  color: #190607;
  border-color: var(--accent-red);
  background: var(--accent-red);
}

.admin-tag-option:has(input:disabled) {
  cursor: not-allowed;
  opacity: 0.46;
}

.admin-tag-option input {
  width: 14px;
  height: 14px;
  accent-color: var(--accent-red);
}
.tag-limit-hint {
  flex: 1 0 100%;
  margin: 2px 0 0;
  color: var(--ink-muted);
  font-size: 11px;
}
.form-hint {
  margin: 0;
  color: var(--ink-muted);
  font-size: 12px;
}
.combo-route-builder {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid rgba(255,255,255,0.1);
  background: rgba(255,255,255,0.035);
}
.combo-route-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 13px;
}
.route-add {
  width: 34px;
  min-height: 34px;
  padding: 0;
  border-color: rgba(91, 214, 199, 0.46);
  background: rgba(91, 214, 199, 0.14);
  color: var(--accent-cyan-soft);
}
.route-add .material-symbols-outlined {
  font-size: 20px;
}
.route-move-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
}
.route-move-field {
  display: grid;
  grid-template-columns: minmax(120px, 180px) minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}
.route-move-field > span {
  color: var(--ink-muted);
  line-height: 1.45;
}
.route-move-input {
  flex: 1 1 0;
  min-width: 0;
  width: auto;
}
.route-move-controls {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.route-character-select {
  flex: 0 0 138px;
  width: 138px;
}
.route-remove {
  width: 38px;
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 38px;
  padding: 0;
  color: #ff8c96;
  border-color: rgba(255, 77, 90, 0.42);
  background: rgba(255, 77, 90, 0.08);
}
.route-remove:hover:not(:disabled) {
  color: #fff;
  border-color: var(--accent-red);
  background: rgba(255, 77, 90, 0.24);
}
.route-remove:disabled {
  cursor: not-allowed;
  opacity: 0.3;
}
.route-remove .material-symbols-outlined {
  font-size: 19px;
}
.route-separator {
  color: var(--accent-red);
  font-family: 'JetBrains Mono';
  font-size: 14px;
}
.route-preview {
  margin: 0;
  color: var(--ink-muted);
  font-size: 12px;
  overflow-wrap: anywhere;
}
.check-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-text-soft);
}
.check-line input {
  width: auto;
  min-height: auto;
}
.admin-video-upload {
  min-height: 42px;
  padding: 0 12px;
  border: 1px dashed rgba(91, 214, 199, 0.46);
  color: var(--ink-text-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  font-family: 'JetBrains Mono';
  font-size: 13px;
  transition: all .18s ease;
}
.admin-video-upload:hover {
  border-color: var(--accent-red);
  color: var(--accent-cyan-soft);
  background: rgba(91, 214, 199, 0.12);
}
.admin-video-upload.uploading {
  cursor: wait;
  pointer-events: none;
  border-color: rgba(91, 214, 199, 0.72);
  background: rgba(91, 214, 199, 0.12);
}
.admin-video-upload .material-symbols-outlined {
  font-size: 20px;
}
.character-data-editor {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid rgba(49, 213, 230, 0.28);
  border-left: 3px solid var(--accent-red);
  border-radius: 6px;
  background:
    linear-gradient(135deg, rgba(255, 64, 84, 0.07), transparent 34%),
    rgba(5, 9, 14, 0.42);
}
.character-data-editor-head,
.character-data-editor-head > div {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.character-data-editor-head {
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
}
.character-data-editor-head .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 22px;
}
.character-data-editor-head h3 {
  margin: 0;
  color: var(--ink-text);
  font-size: 16px;
}
.character-data-editor-head p {
  margin: 5px 0 0;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 10px;
  line-height: 1.5;
}
.character-data-editor-head > span {
  flex: 0 0 auto;
  padding: 5px 8px;
  color: var(--accent-cyan-soft);
  border: 1px solid rgba(49, 213, 230, 0.28);
  border-radius: 999px;
  font-family: 'JetBrains Mono';
  font-size: 10px;
}
.character-data-form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.character-data-form-grid label,
.character-data-source-fields label {
  min-width: 0;
  display: grid;
  gap: 7px;
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}
.character-data-form-grid label > span,
.character-data-source-fields label > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.character-data-form-grid small {
  display: block;
  margin-top: 3px;
  color: var(--ink-muted);
  font-size: 9px;
  font-weight: 400;
}
.character-data-form-grid input,
.character-data-source-fields input {
  width: 100%;
  min-width: 0;
  height: 40px;
  padding: 0 10px;
  color: var(--ink-text);
  border: 1px solid var(--ink-line);
  border-radius: 4px;
  background: var(--ink-surface-low);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  outline: none;
}
.character-data-form-grid input:focus,
.character-data-source-fields input:focus {
  border-color: var(--accent-red);
  box-shadow: 0 0 0 3px rgba(255, 64, 84, 0.12);
}
.character-data-source-fields {
  display: grid;
  grid-template-columns: minmax(180px, 0.7fr) minmax(0, 1.3fr);
  gap: 10px;
  padding-top: 2px;
}
.character-avatar-uploader {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}
.character-avatar-preview {
  width: 112px;
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid rgba(91, 214, 199, 0.46);
  background:
    linear-gradient(135deg, rgba(91, 214, 199, 0.1), rgba(255,255,255,0.02)),
    var(--ink-bg);
}
.character-avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.character-avatar-preview .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 34px;
}
.character-avatar-controls {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.character-avatar-upload {
  min-width: 180px;
}
.avatar-crop-panel {
  display: grid;
  grid-template-columns: 292px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 14px;
  border: 1px solid rgba(49, 213, 230, 0.24);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(49, 213, 230, 0.07), transparent 46%),
    rgba(0, 0, 0, 0.18);
}
.avatar-crop-stage {
  position: relative;
  width: 292px;
  aspect-ratio: 1;
  overflow: hidden;
  touch-action: none;
  cursor: grab;
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 8px;
  background:
    linear-gradient(45deg, rgba(255,255,255,0.045) 25%, transparent 25%),
    linear-gradient(-45deg, rgba(255,255,255,0.045) 25%, transparent 25%),
    #080b10;
  background-size: 18px 18px;
}
.avatar-crop-stage:active {
  cursor: grabbing;
}
.avatar-crop-stage img {
  position: absolute;
  left: 50%;
  top: 50%;
  max-width: none;
  user-select: none;
  pointer-events: none;
}
.avatar-crop-box {
  position: absolute;
  pointer-events: none;
  border: 2px solid rgba(255, 248, 239, 0.92);
  border-radius: 6px;
  box-shadow:
    0 0 0 999px rgba(0, 0, 0, 0.58),
    0 0 0 1px rgba(0, 0, 0, 0.7),
    inset 0 0 0 1px rgba(0, 0, 0, 0.54);
}
.avatar-crop-box::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(to right, transparent calc(100% - 1px), rgba(255, 248, 239, 0.4) 0),
    linear-gradient(to bottom, transparent calc(100% - 1px), rgba(255, 248, 239, 0.4) 0);
  background-size: 33.333% 100%, 100% 33.333%;
}
.avatar-crop-move-handle,
.avatar-crop-resize-handle {
  position: absolute;
  z-index: 2;
  display: grid;
  place-items: center;
  padding: 0;
  color: #071014;
  background: var(--admin-cyan, var(--accent-cyan));
  border: 2px solid #f8ffff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.72), 0 0 12px rgba(49, 213, 230, 0.42);
  pointer-events: auto;
}
.avatar-crop-move-handle {
  top: -14px;
  left: 50%;
  width: 30px;
  height: 24px;
  border-radius: 5px;
  cursor: move;
  transform: translateX(-50%);
}
.avatar-crop-move-handle .material-symbols-outlined {
  font-size: 16px;
}
.avatar-crop-resize-handle {
  width: 16px;
  height: 16px;
  border-radius: 3px;
}
.avatar-crop-resize-handle.corner-nw { top: -8px; left: -8px; cursor: nwse-resize; }
.avatar-crop-resize-handle.corner-ne { top: -8px; right: -8px; cursor: nesw-resize; }
.avatar-crop-resize-handle.corner-sw { bottom: -8px; left: -8px; cursor: nesw-resize; }
.avatar-crop-resize-handle.corner-se { right: -8px; bottom: -8px; cursor: nwse-resize; }
.avatar-crop-move-handle:focus-visible,
.avatar-crop-resize-handle:focus-visible {
  outline: 2px solid #fff8ef;
  outline-offset: 3px;
}
.avatar-crop-controls {
  min-width: 0;
  display: grid;
  gap: 16px;
}
.avatar-crop-control-group {
  display: grid;
  gap: 10px;
}
.avatar-crop-control-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.avatar-crop-control-heading > span {
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.avatar-crop-control-heading output {
  color: var(--admin-cyan, var(--accent-cyan));
  font-family: 'JetBrains Mono';
  font-size: 12px;
  font-weight: 800;
}
.avatar-crop-scale-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 82px;
  gap: 10px;
  align-items: center;
}
.avatar-crop-controls input[type="range"] {
  width: 100%;
  min-height: auto;
  padding: 0;
  accent-color: var(--admin-cyan, var(--accent-cyan));
  box-shadow: none;
}
.avatar-crop-percent-input {
  position: relative;
  display: block;
}
.avatar-crop-percent-input input,
.avatar-crop-size-row input {
  width: 100%;
  min-height: 38px;
  padding: 7px 24px 7px 9px;
  color: var(--ink-text);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  background: rgba(7, 10, 14, 0.78);
  border: 1px solid rgba(255, 248, 239, 0.26);
  border-radius: 5px;
}
.avatar-crop-percent-input > span {
  position: absolute;
  top: 50%;
  right: 9px;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
  pointer-events: none;
  transform: translateY(-50%);
}
.avatar-crop-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}
.crop-preset {
  min-width: 48px;
  padding: 7px 10px;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
  background: rgba(7, 10, 14, 0.66);
  border: 1px solid rgba(255, 248, 239, 0.18);
  border-radius: 4px;
}
.crop-preset:hover,
.crop-preset.active {
  color: #031214;
  background: var(--admin-cyan, var(--accent-cyan));
  border-color: var(--admin-cyan, var(--accent-cyan));
}
.avatar-crop-size-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.avatar-crop-size-row label {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 7px;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}
.avatar-crop-size-row input {
  padding-right: 8px;
}
.avatar-crop-help {
  margin: 0;
  color: var(--ink-muted);
  font-size: 11px;
  line-height: 1.55;
}
.avatar-crop-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.admin-upload-progress {
  grid-column: span 2;
  height: 6px;
  overflow: hidden;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(91, 214, 199, 0.24);
}
.admin-upload-progress span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--accent-red), var(--accent-cyan-soft));
  transition: width 0.16s ease;
}
.admin-video-preview {
  grid-column: span 2;
  width: 100%;
  max-height: 220px;
  object-fit: cover;
  border: 1px solid rgba(91, 214, 199, 0.46);
  background: var(--ink-bg);
}
.combo-training-notes {
  grid-column: span 2;
  min-height: 96px;
  resize: vertical;
}

:global(.admin-modal-backdrop) {
  position: fixed;
  inset: 0;
  z-index: 160;
  display: grid;
  place-items: center;
  padding: 24px;
  overflow-y: auto;
  background: rgba(4, 7, 12, 0.76);
  backdrop-filter: blur(14px);
}

:global(.admin-modal-panel) {
  width: min(920px, 100%);
  max-height: calc(100dvh - 48px);
  margin: auto 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid rgba(91, 214, 199, 0.3);
  background:
    linear-gradient(135deg, rgba(91, 214, 199, 0.08), transparent 34%),
    rgba(16, 19, 24, 0.98);
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.54), 0 0 0 1px rgba(255,255,255,0.05);
}

:global(.admin-modal-panel.danger) {
  border-color: rgba(255, 118, 135, 0.34);
  background:
    linear-gradient(135deg, rgba(255, 118, 135, 0.1), transparent 32%),
    rgba(16, 19, 24, 0.98);
}

:global(.admin-modal-head) {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

:global(.admin-modal-head h2) {
  margin: 0;
  color: #fff;
  font-size: 24px;
  font-weight: 900;
}

:global(.admin-modal-head p) {
  margin: 5px 0 0;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  overflow-wrap: anywhere;
}

:global(.modal-close) {
  width: 38px;
  min-height: 38px;
  flex: 0 0 auto;
  padding: 0;
}

:global(.admin-modal-body) {
  min-height: 0;
  overflow: auto;
  padding: 20px;
}

:global(.modal-form) {
  display: grid;
  gap: 14px;
}

:global(.modal-actions) {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  padding-top: 2px;
}

:global(.split-actions) {
  justify-content: flex-start;
}

:global(.modal-check) {
  min-height: 38px;
  padding: 9px 10px;
  border: 1px solid rgba(255,255,255,0.12);
  background: var(--ink-surface-low);
}

:global(.modal-review-panel) {
  margin: 0;
  background: transparent;
}

:global(.admin-modal-panel.combo-review-modal-shell) {
  width: min(1500px, calc(100vw - 36px));
}
.combo-review-modal-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 340px);
  gap: 14px;
  align-items: start;
}
.combo-review-modal-content {
  min-width: 0;
  display: grid;
  gap: 12px;
}
.modal-actions-panel {
  position: sticky;
  top: 0;
  max-height: calc(100dvh - 138px);
}
.review-action-calibration {
  display: grid;
  gap: 8px;
}
.review-action-calibration label {
  display: grid;
  gap: 6px;
}
.review-action-calibration span {
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.admin-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: flex-start;
  gap: 14px;
  padding: 14px;
}
.combo-admin-row {
  grid-template-columns: auto minmax(0, 1fr) auto;
}
.combo-content-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.combo-content-strip span {
  min-height: 24px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border: 1px solid rgba(91, 214, 199, 0.28);
  color: var(--accent-cyan-soft);
  background: rgba(91, 214, 199, 0.08);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}
.combo-content-strip span.issue {
  color: var(--state-error);
  border-color: rgba(255, 118, 135, 0.32);
  background: rgba(255, 118, 135, 0.08);
}
.combo-manual-reason {
  color: #ffe4ad !important;
}
.combo-card-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.combo-card-toolbar .combo-queue-search {
  flex: 1 1 360px;
  max-width: 520px;
}
.combo-card-toolbar > span {
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.admin-combo-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.admin-combo-card {
  min-width: 0;
  min-height: 218px;
  display: grid;
  grid-template-rows: auto auto minmax(44px, 1fr) auto auto;
  gap: 10px;
  padding: 12px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(0,0,0,0.18);
  cursor: pointer;
}
.admin-combo-card:hover {
  border-color: rgba(91, 214, 199, 0.42);
  background: rgba(91, 214, 199, 0.07);
}
.combo-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.combo-card-head .select-box {
  width: 18px;
  min-height: 18px;
}
.combo-card-head mark {
  margin-left: auto;
  padding: 3px 7px;
  color: var(--accent-amber);
  border: 1px solid rgba(255, 193, 93, 0.34);
  background: rgba(255, 193, 93, 0.08);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}
.admin-combo-card > strong {
  min-width: 0;
  color: #fff;
  font-size: 17px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.admin-combo-card > p {
  margin: 0;
  color: var(--ink-muted);
  line-height: 1.5;
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.combo-card-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
}
.combo-card-metrics span {
  min-width: 0;
  padding: 7px 8px;
  color: var(--ink-muted);
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.035);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}
.combo-card-metrics strong {
  display: block;
  margin-bottom: 3px;
  color: #fff;
  font-size: 15px;
}
.combo-card-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.combo-card-actions button {
  min-width: 0;
  padding-inline: 8px;
}
.admin-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 14px;
  font-family: 'JetBrains Mono';
}
.admin-pager button {
  min-height: 36px;
  padding: 0 14px;
}
.admin-pager span {
  color: var(--admin-muted, var(--ink-muted));
  font-size: 12px;
}
.combo-followup-queue {
  grid-column: 1 / -1;
  display: grid;
  gap: 8px;
  margin: 4px 0 0 34px;
  padding: 12px;
  border: 1px solid rgba(91, 214, 199, 0.24);
  background: rgba(91, 214, 199, 0.055);
}
.combo-followup-queue-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--accent-cyan-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.combo-followup-queue-head .material-symbols-outlined {
  font-size: 18px;
}
.combo-followup-queue-head strong {
  color: var(--accent-cyan-soft);
}
.combo-followup-queue-head small {
  color: var(--ink-muted);
}
.combo-followup-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: flex-start;
  gap: 12px;
  padding: 10px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(0,0,0,0.12);
}
.combo-followup-item strong {
  color: #fff;
  overflow-wrap: anywhere;
}
.combo-followup-item p,
.combo-followup-item small {
  display: block;
  margin: 4px 0 0;
  color: var(--ink-muted);
  overflow-wrap: anywhere;
}
.character-admin-item {
  grid-template-columns: auto auto minmax(0, 1fr) auto;
  align-items: center;
}
.character-order-tools {
  display: grid;
  grid-template-columns: repeat(2, 30px);
  gap: 5px;
}
.character-order-tools button {
  width: 30px;
  min-height: 30px;
  padding: 0;
}
.character-order-tools .material-symbols-outlined {
  font-size: 18px;
}
.admin-item.single-column {
  grid-template-columns: minmax(0, 1fr);
}
.admin-item strong {
  color: #fff;
  overflow-wrap: anywhere;
}
.admin-item p {
  margin: 4px 0 0;
  overflow-wrap: anywhere;
}
.target-highlight {
  border-color: rgba(255, 198, 95, 0.88) !important;
  background: rgba(255, 198, 95, 0.12) !important;
  box-shadow: 0 0 0 1px rgba(255, 198, 95, 0.28), 0 0 24px rgba(255, 198, 95, 0.2);
  animation: targetPulse 1.2s ease-in-out 3;
}
.empty-row {
  color: var(--ink-muted);
  font-size: 12px;
}
.user-row-list {
  display: grid;
  gap: 10px;
}

.user-row-card {
  display: grid;
  grid-template-columns: auto minmax(220px, 1.1fr) minmax(300px, 1.7fr) minmax(150px, .8fr) auto;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid rgba(255,255,255,0.08);
  background:
    linear-gradient(90deg, rgba(91, 214, 199, 0.055), transparent 44%),
    rgba(255,255,255,0.028);
  transition: border-color .18s ease, background .18s ease, transform .18s ease;
}

.user-row-card:hover {
  border-color: rgba(91, 214, 199, 0.34);
  background:
    linear-gradient(90deg, rgba(91, 214, 199, 0.09), transparent 48%),
    rgba(255,255,255,0.04);
}

.user-row-select {
  justify-self: center;
}

.user-identity {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  color: #190607;
  background: var(--accent-red);
  border: 1px solid rgba(191, 250, 242, 0.42);
  font-family: 'JetBrains Mono';
  font-weight: 900;
  font-size: 13px;
}

.user-identity strong {
  display: block;
  color: #fff;
  font-size: 16px;
  overflow-wrap: anywhere;
}

.user-identity small,
.user-meta-grid small,
.user-status small {
  display: block;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
  line-height: 1.45;
}

.user-meta-grid {
  min-width: 0;
  display: grid;
  grid-template-columns: .7fr 1fr 1.2fr;
  gap: 8px;
}

.user-meta-grid div {
  min-width: 0;
  padding: 8px 9px;
  border: 1px solid rgba(255,255,255,0.07);
  background: rgba(0,0,0,0.12);
}

.user-meta-grid span {
  display: block;
  margin-top: 3px;
  color: var(--ink-text);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.user-status {
  min-width: 0;
  display: grid;
  gap: 5px;
  justify-items: start;
}
.report-item {
  cursor: pointer;
  transition: border-color .18s ease, background .18s ease, transform .18s ease;
}
.report-item:hover,
.report-item:focus-visible {
  border-color: rgba(91, 214, 199, 0.48);
  background: rgba(255,255,255,0.045);
}
.report-item:focus-visible {
  outline: 2px solid rgba(91, 214, 199, 0.66);
  outline-offset: 2px;
}
.report-content {
  min-width: 0;
}
.report-title-row {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
}
.report-section-chip {
  flex: 0 0 auto;
  padding: 4px 8px;
  border: 1px solid rgba(91, 214, 199, 0.4);
  color: var(--accent-cyan-soft);
  background: rgba(91, 214, 199, 0.12);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.report-detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin: 10px 0 0;
}
.report-detail-grid div {
  min-width: 0;
  padding: 8px 9px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.025);
}
.report-detail-grid dt {
  margin: 0 0 4px;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 11px;
}
.report-detail-grid dd {
  margin: 0;
  color: var(--ink-text);
  overflow-wrap: anywhere;
  font-size: 13px;
}
.report-item small {
  display: block;
  margin-top: 7px;
  color: #bdc6c0;
}
.report-detail-text {
  color: #f0d2c5 !important;
}
.feedback-detail-modal {
  display: grid;
  gap: 14px;
}
.feedback-detail-block {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.025);
}
.feedback-detail-block span {
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.feedback-detail-block p {
  margin: 0;
  color: var(--ink-text);
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.feedback-detail-block small {
  color: var(--ink-muted);
}
.report-summary-row,
.admin-pager {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.report-summary-row {
  margin: 0 0 10px;
}
.report-summary-row span,
.admin-pager span {
  padding: 6px 9px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(255,255,255,0.025);
}
.admin-pager {
  justify-content: flex-end;
  margin-top: 12px;
}
.report-resolution-panel {
  display: grid;
  grid-template-columns: auto minmax(240px, 560px);
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding: 10px 12px;
  border: 1px solid rgba(91, 214, 199, 0.24);
  background: rgba(91, 214, 199, 0.055);
}
.report-resolution-panel label {
  color: var(--accent-cyan-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.report-item .admin-link-btn {
  min-height: 34px;
}
.admin-table {
  display: grid;
  gap: 6px;
  overflow-x: auto;
}
.table-row {
  display: grid;
  gap: 10px;
  align-items: center;
  min-width: 820px;
  padding: 10px 12px;
  border: 1px solid rgba(255,255,255,0.07);
  background: rgba(255,255,255,0.025);
}
.users-table .table-row {
  grid-template-columns: 32px 1.1fr 1.4fr .8fr .7fr 1fr;
}
.frames-table .table-row {
  grid-template-columns: 32px .7fr 1.35fr .55fr .6fr .7fr .55fr .55fr .55fr .55fr .8fr .65fr 1fr;
  min-width: 1320px;
}
.table-head {
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
  background: rgba(255,255,255,0.045);
}
.table-row small {
  display: block;
  margin-top: 3px;
  color: var(--ink-muted);
}
.locked-note {
  color: var(--accent-red);
}
mark {
  padding: 3px 8px;
  color: #bfe8cc;
  background: rgba(106, 181, 137, 0.13);
}
mark.danger {
  color: var(--state-error);
}

@keyframes targetPulse {
  0%, 100% {
    box-shadow: 0 0 0 1px rgba(255, 198, 95, 0.18), 0 0 12px rgba(255, 198, 95, 0.12);
  }
  50% {
    box-shadow: 0 0 0 2px rgba(255, 198, 95, 0.42), 0 0 30px rgba(255, 198, 95, 0.28);
  }
}

.admin-login-screen {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(91, 214, 199, 0.14), transparent 34%),
    linear-gradient(315deg, rgba(125, 216, 196, 0.1), transparent 32%),
    var(--ink-bg);
}
.admin-login-card {
  width: min(440px, 100%);
  display: grid;
  gap: 22px;
  padding: 28px;
  border: 1px solid rgba(255,255,255,0.08);
  background: rgba(22, 25, 27, 0.94);
  box-shadow: 0 28px 72px rgba(0,0,0,0.34), 0 0 0 1px rgba(91, 214, 199, 0.08);
}
.admin-login-back {
  width: max-content;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--ink-text-soft);
  text-decoration: none;
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.admin-login-back:hover {
  color: var(--accent-red);
}
.admin-login-back .material-symbols-outlined {
  font-size: 18px;
}
.admin-login-head {
  display: grid;
  gap: 8px;
}
.admin-login-head .material-symbols-outlined {
  color: var(--accent-red);
  font-size: 42px;
}
.admin-login-head p {
  margin: 0;
  color: var(--ink-muted);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.admin-login-head h1 {
  margin: 10px 0 8px;
  color: #fff;
  font-family: inherit;
  font-size: 30px;
  font-weight: 900;
  font-style: normal;
}
.admin-login-head small {
  color: var(--ink-text-soft);
}
.admin-login-form {
  display: grid;
  gap: 14px;
}
.admin-login-form label {
  display: grid;
  gap: 7px;
}
.admin-login-form label span {
  color: var(--ink-text-soft);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}
.admin-login-form .admin-primary {
  width: 100%;
  min-height: 42px;
}
.admin-login-error {
  margin: 0;
  padding: 10px 12px;
  color: var(--state-error);
  border: 1px solid rgba(255, 118, 135, 0.36);
  background: rgba(255, 118, 135, 0.08);
  font-family: 'JetBrains Mono';
  font-size: 12px;
}

@media (max-width: 980px) {
  .admin-console {
    grid-template-columns: 1fr;
  }
  .admin-sidebar {
    position: static;
    height: auto;
  }
  .admin-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .dashboard-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .dashboard-chart-row {
    grid-template-columns: 1fr;
  }
  .admin-two-col,
  .dashboard-grid,
  .form-grid,
  .frame-form,
  .combo-form,
  .combo-review-hero,
  .combo-review-workbench,
  .modal-field-row,
  .route-move-field {
    grid-template-columns: 1fr;
  }
  .route-move-controls {
    align-items: stretch;
    flex-direction: column;
  }
  .route-remove {
    align-self: flex-end;
  }
  .route-character-select {
    flex-basis: auto;
    width: 100%;
  }
  .modal-field-row > span {
    min-height: auto;
  }
  .user-row-card {
    grid-template-columns: auto minmax(0, 1fr);
    align-items: flex-start;
  }
  .user-meta-grid,
  .user-status,
  .user-row-card .row-actions {
    grid-column: 2;
  }
  .user-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .frame-form .wide-field {
    grid-column: auto;
  }
  .combo-route-builder {
    grid-column: auto;
  }
  .admin-topbar,
  .panel-head {
    align-items: flex-start;
    flex-direction: column;
  }
  .admin-item {
    grid-template-columns: auto minmax(0, 1fr);
  }
  .admin-item .row-actions,
  .admin-item > button {
    grid-column: 2;
  }
  .report-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .combo-review-detail-grid,
  .combo-review-scoreboard {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .report-resolution-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .admin-sidebar {
    gap: 14px;
    padding: 16px;
  }

  .admin-brand strong {
    font-size: 19px;
  }

  .admin-nav {
    display: flex;
    overflow-x: auto;
    gap: 8px;
    padding-bottom: 4px;
    scrollbar-width: none;
    -webkit-overflow-scrolling: touch;
  }

  .admin-nav::-webkit-scrollbar {
    display: none;
  }

  .admin-nav-item {
    flex: 0 0 auto;
    min-height: 38px;
    padding: 0 10px;
    font-size: 12px;
  }

  .admin-sidebar-foot {
    display: none;
  }

  .admin-main {
    padding: 16px;
  }
  .admin-topbar h1 {
    font-size: 27px;
    line-height: 1.05;
  }

  .admin-top-actions,
  .admin-filter,
  .bulk-bar,
  .bulk-inline {
    width: 100%;
  }

  .user-filter-row.user-filter-row {
    flex-wrap: wrap;
    width: 100%;
  }

  .user-filter-row.user-filter-row input {
    flex: 1 1 100%;
    width: 100%;
  }

  .admin-top-actions button,
  .admin-top-actions a,
  .admin-filter input,
  .admin-filter select,
  .admin-filter button,
  .bulk-bar button,
  .bulk-inline button {
    flex: 1 1 auto;
  }

  .audit-filter-row.audit-filter-row .audit-date-input {
    flex: 1 1 100%;
    width: 100%;
    min-width: 0;
    max-width: none;
  }

  .metric-card,
  .admin-panel,
  .admin-item {
    padding: 12px;
  }
  :global(.admin-modal-backdrop) {
    padding: 12px;
  }
  :global(.admin-modal-panel) {
    max-height: calc(100dvh - 24px);
  }
  :global(.admin-modal-head),
  :global(.admin-modal-body) {
    padding: 16px;
  }
  :global(.admin-modal-head h2) {
    font-size: 20px;
  }

  .metric-card strong {
    font-size: 24px;
  }

  .panel-head h2 {
    font-size: 18px;
  }

  .admin-item {
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
  }
  .user-row-card {
    grid-template-columns: minmax(0, 1fr);
    padding: 12px;
  }
  .user-row-select,
  .user-meta-grid,
  .user-status,
  .user-row-card .row-actions {
    grid-column: auto;
  }
  .user-row-select {
    justify-self: start;
  }
  .user-meta-grid {
    grid-template-columns: 1fr;
  }

  .admin-item .row-actions,
  .admin-item > button {
    grid-column: auto;
  }

  .row-actions {
    width: 100%;
  }

  .row-actions button,
  .row-actions a {
    flex: 1 1 auto;
  }

  .admin-table {
    margin-inline: -16px;
    padding-inline: 16px;
  }

  .table-row {
    min-width: 720px;
    padding: 9px 10px;
  }

  .report-detail-grid {
    grid-template-columns: 1fr;
  }
  .combo-review-detail-grid,
  .combo-review-scoreboard {
    grid-template-columns: 1fr;
  }

  .combo-route-builder,
  .admin-video-preview,
  .combo-training-notes {
    grid-column: auto;
  }
}

/* Admin theme refresh */
.admin-console {
  --admin-bg: #0b0e13;
  --admin-sidebar: #101722;
  --admin-panel: #151b24;
  --admin-panel-soft: #111720;
  --admin-panel-high: #1a222d;
  --admin-line: rgba(159, 178, 203, 0.16);
  --admin-line-strong: rgba(49, 213, 230, 0.34);
  --admin-text: #f5f7fb;
  --admin-soft: #c9d2df;
  --admin-muted: #8f9bad;
  --admin-red: #ff4b5f;
  --admin-red-soft: rgba(255, 75, 95, 0.12);
  --admin-cyan: #31d5e6;
  --admin-cyan-soft: rgba(49, 213, 230, 0.11);
  --admin-amber: #ffc15d;
  --admin-success: #69f3a8;
  background:
    radial-gradient(circle at 18% -10%, rgba(49, 213, 230, 0.1), transparent 28%),
    radial-gradient(circle at 82% 0%, rgba(255, 75, 95, 0.09), transparent 24%),
    var(--admin-bg);
  --el-bg-color: var(--admin-panel);
  --el-bg-color-overlay: var(--admin-panel-high);
  --el-border-color: var(--admin-line);
  --el-border-color-light: var(--admin-line);
  --el-text-color-primary: var(--admin-text);
  --el-text-color-regular: var(--admin-soft);
  --el-fill-color-light: var(--admin-panel-soft);
}

.admin-sidebar {
  background:
    linear-gradient(180deg, rgba(49, 213, 230, 0.06), transparent 38%),
    var(--admin-sidebar);
  border-right: 1px solid var(--admin-line);
}

.admin-brand span {
  background: var(--admin-red);
  color: #18090d;
}

.admin-nav-item {
  color: var(--admin-soft);
  border-radius: 6px;
}

.admin-nav-item:hover,
.admin-nav-item.active {
  color: var(--admin-text);
  border-color: var(--admin-line-strong);
  background: var(--admin-cyan-soft);
}

.admin-sidebar-foot,
.admin-panel,
.admin-item,
.overview-card,
.dashboard-card,
.ops-task-card,
.ops-focus-panel,
.combo-review-actions,
.combo-review-decision,
.review-video-card,
.combo-review-scoreboard div,
.combo-review-detail-item,
.user-row-card,
.report-item,
.bulk-bar,
.task-list button,
.compact-stats span,
.status-row {
  border-color: var(--admin-line);
  background: var(--admin-panel);
  box-shadow: none;
}

.admin-panel,
.admin-item,
.overview-card,
.dashboard-card,
.ops-task-card,
.ops-focus-panel {
  border-radius: 8px;
}

.admin-item,
.ops-risk-row,
.recent-audit-list article,
.queue-combo-row,
.combo-parent-panel,
.combo-review-notation-card,
.combo-review-hero,
.combo-review-decision,
.review-video-card,
.review-video-empty,
.combo-followup-queue,
.combo-followup-item,
.duplicate-list article,
.user-meta-grid div,
.report-detail-grid div,
.feedback-detail-block {
  border-color: var(--admin-line);
  background: var(--admin-panel-soft);
  border-radius: 6px;
}

.ops-task-card.danger,
.danger,
.combo-review-status.rejected,
.combo-content-strip span.issue {
  border-color: rgba(255, 75, 95, 0.32);
  background: var(--admin-red-soft);
}

.ops-task-card.warning,
.ops-risk-row mark,
.combo-review-status.manual_review {
  border-color: rgba(255, 193, 93, 0.34);
  background: rgba(255, 193, 93, 0.1);
}

.combo-review-hero,
.combo-review-notation-card {
  background:
    linear-gradient(135deg, rgba(49, 213, 230, 0.055), transparent 42%),
    var(--admin-panel);
}

.dashboard-table {
  --el-table-bg-color: var(--admin-panel);
  --el-table-tr-bg-color: var(--admin-panel);
  --el-table-header-bg-color: var(--admin-panel-soft);
  --el-table-row-hover-bg-color: #1a2734;
  --el-table-border-color: var(--admin-line);
  --el-fill-color-lighter: var(--admin-panel-soft);
}

input,
textarea,
select,
:global(.modal-check),
.admin-segmented-control,
.admin-tag-option {
  border-color: var(--admin-line);
  background: #0f141c;
  color: var(--admin-text);
  border-radius: 6px;
}

input:focus,
textarea:focus,
select:focus {
  border-color: var(--admin-cyan);
  box-shadow: 0 0 0 3px rgba(49, 213, 230, 0.12);
}

button,
.admin-link-btn {
  border-color: var(--admin-line);
  background: #18212c;
  color: var(--admin-soft);
  border-radius: 6px;
  transition: border-color .16s ease, background-color .16s ease, color .16s ease, transform .16s ease;
}

button:hover,
.admin-link-btn:hover {
  color: var(--admin-text);
  border-color: var(--admin-line-strong);
  background: #1c2a36;
}

.admin-primary {
  border-color: rgba(255, 75, 95, 0.62);
  background: var(--admin-red);
  color: #16080c;
}

.admin-primary:hover {
  border-color: rgba(255, 122, 135, 0.72);
  background: #ff6575;
  color: #16080c;
}

.modal-actions-panel .action-button-grid > button,
.modal-actions-panel .action-button-grid > a {
  min-height: 46px;
  border-color: rgba(201, 210, 223, 0.44);
  background: #192330;
  color: #f5f7fb;
  font-weight: 800;
}

.modal-actions-panel .action-button-grid > button:hover,
.modal-actions-panel .action-button-grid > a:hover {
  border-color: rgba(49, 213, 230, 0.62);
  background: #223140;
  color: #ffffff;
}

.modal-actions-panel .action-button-grid > .admin-primary {
  border-color: rgba(105, 243, 168, 0.72);
  background: #69f3a8;
  color: #04110b;
}

.modal-actions-panel .action-button-grid > .admin-primary:hover {
  border-color: rgba(151, 255, 196, 0.9);
  background: #8cffbf;
  color: #04110b;
}

.modal-actions-panel .action-button-grid > .danger {
  border-color: rgba(255, 75, 95, 0.62);
  background: rgba(255, 75, 95, 0.14);
  color: #ffb8c0;
}

.modal-actions-panel .action-button-grid > .danger:hover {
  border-color: rgba(255, 122, 135, 0.78);
  background: #7f1d2d;
  color: #ffffff;
}

.ghost {
  background: transparent;
}

.danger {
  color: #ff9aa5;
}

.metric-growth.live,
.combo-review-status,
.combo-content-strip span,
.queue-combo-row span,
.review-section-head .material-symbols-outlined,
.overview-card-head .el-icon,
.ops-task-card .material-symbols-outlined {
  color: var(--admin-cyan);
}

.overview-card strong,
.metric-card strong,
.admin-item strong,
.dashboard-card-head h2,
.panel-head h2,
.combo-review-title strong,
.queue-combo-row strong,
.recent-audit-list article strong,
.ops-risk-row strong {
  color: var(--admin-text);
}

.admin-sidebar-foot small,
.admin-item p,
.admin-item small,
.metric-card small,
.admin-topbar p,
.dashboard-card-head span,
.ops-risk-row p,
.ops-risk-row small,
.recent-audit-list p,
.recent-audit-list small,
.queue-combo-row small {
  color: var(--admin-muted);
}

:global(.admin-modal-panel) {
  border-color: var(--admin-line-strong);
  background:
    linear-gradient(135deg, rgba(49, 213, 230, 0.06), transparent 34%),
    var(--admin-panel);
  border-radius: 8px;
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.42);
}

@media (max-width: 1180px) {
  .combo-review-board {
    grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  }

  .combo-review-actions {
    position: static;
    grid-column: 1 / -1;
    max-height: none;
  }

  .combo-review-actions h3,
  .combo-review-actions .review-action-note,
  .combo-review-actions .review-checks {
    grid-column: 1 / -1;
  }

  .action-button-grid {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }
}

@media (max-width: 760px) {
  .avatar-crop-panel {
    grid-template-columns: 1fr;
  }
  .avatar-crop-stage {
    width: min(292px, 100%);
  }
}

@media (max-width: 980px) {
  .character-data-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .character-data-form-grid,
  .character-data-source-fields {
    grid-template-columns: 1fr;
  }

  .character-data-editor-head {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 760px) {
  .ops-focus-grid,
  .combo-review-board,
  .combo-review-workbench.inline {
    grid-template-columns: 1fr;
  }

  .combo-review-queue {
    height: auto;
    max-height: 360px;
  }

  .combo-review-preview,
  .combo-review-actions {
    max-height: none;
  }
}
</style>

