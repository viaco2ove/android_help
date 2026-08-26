```bash
# 1. 进入目录
cd android/android_clipper

# 2. 构建 APK
./gradlew assembleDebug

# 3. 查看设备
adb devices

# 4. 安装（单设备）
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 5. 安装（多设备需指定序列号）
adb -s <设备序列号> install -r app/build/outputs/apk/debug/app-debug.apk
# adb -s 3B65CS012RH00000  install -r app/build/outputs/apk/debug/app-debug.apk
```