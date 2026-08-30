iGO Offline Voice - Vosk 中文独立测试版

GitHub Actions 会在编译时自动下载：
vosk-model-small-cn-0.22

然后把模型打包进 APK assets。

安装测试顺序：
1. 打开 APK
2. 允许麦克风权限
3. 点击“准备中文模型”
4. 等待模型从 APK 解压到应用内部目录
5. 点击“开始监听”
6. 说：你好 / 导航 / 取消导航 / YES
7. 查看屏幕识别结果

本测试用于验证：车机麦克风 + Vosk + 中文模型。
暂时不依赖 iGO 原生 VR engine。
