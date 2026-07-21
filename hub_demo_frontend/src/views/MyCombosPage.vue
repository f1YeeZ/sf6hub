<template>
  <AccountComboListPage
    kind="uploads"
    kicker="UPLOADED COMBOS"
    title="已上传连招"
    description="查看你提交过的连招，包括审核中的路线。通过审核后，它们会展示到对应角色详情页。"
    panel-title="我的上传"
    loading-text="已上传连招加载中..."
    empty-icon="upload_file"
    empty-title="还没有上传连招"
    empty-text="把训练模式里的路线整理成数据，提交后会进入审核队列。"
    empty-action-text="上传连招"
    empty-action-to="/upload"
  />
</template>

<script setup>
import AccountComboListPage from '@/components/common/AccountComboListPage.vue'
</script>
