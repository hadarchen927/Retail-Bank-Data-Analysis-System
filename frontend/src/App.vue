<template>
  <!-- router-view 是必须的，它负责根据 URL 显示对应的 Login 或 Dashboard 页面 -->
  <router-view />
</template>

<script setup>
</script>

<style>
/* 清理浏览器默认的边距，确保背景色能铺满全屏 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
}

#app {
  height: 100%;
  /* 移除之前的 text-align: center 和 margin-top */
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}
</style>