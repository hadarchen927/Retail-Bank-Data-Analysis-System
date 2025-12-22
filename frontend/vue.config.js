const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false, // 之前我们关了代码检查
  devServer: {
    client: {
      overlay: false,
    },
  },
})