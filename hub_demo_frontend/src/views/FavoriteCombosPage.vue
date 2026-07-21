<template>
  <AccountComboListPage
    kind="favorites"
    kicker="SAVED COMBOS"
    title="连招收藏"
    description="这里集中展示你收藏过的连招。取消收藏后，卡片会从当前列表移除。"
    panel-title="我的收藏"
    loading-text="收藏连招加载中..."
    empty-icon="bookmark"
    empty-title="还没有收藏连招"
    empty-text="在角色详情或连招详情里点击收藏按钮，常用路线会出现在这里。"
    empty-action-text="去看角色"
    empty-action-to="/"
  />
</template>

<script setup>
import AccountComboListPage from '@/components/common/AccountComboListPage.vue'
</script>
