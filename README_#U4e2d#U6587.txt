iGO 离线中文 RecognitionService 测试工程
====================================

目标：
让 iGO 的 Android SpeechRecognizer 能发现一个离线 RecognitionService，
从而把目前的 “VR engine unavailable” 变成可用。

设计：
- 包名：com.carsocx.igovosk
- 只编译 armeabi-v7a，适合 32-bit ARM 车机
- Vosk Android 0.3.75
- 中文模型固定路径：
  /sdcard/vosk-model-small-cn-0.22
- RecognitionService action：
  android.speech.RecognitionService

测试步骤（编译并安装 APK 后）：
1. 将 vosk-model-small-cn-0.22 整个文件夹复制到车机：
   /sdcard/vosk-model-small-cn-0.22
2. 安装 APK。
3. 打开 “iGO Offline Voice”，允许麦克风权限。
4. 回 iGO -> Main Menu -> Voice Test。
5. 如果不再出现 “VR engine unavailable”，说明系统已发现 RecognitionService。

注意：
此工程是测试版。不同 Android 车机可能还要求在
Settings -> Language & input -> Voice input / Speech recognition
中把该服务设为默认语音识别服务。
