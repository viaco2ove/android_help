# Android Help

ADB 工具集，包含剪贴板管理等实用功能。

## 剪贴板工具 (adb-clipboard)

### 功能
- 获取 Android 剪贴板内容
- 设置 Android 剪贴板
- 将 Android 剪贴板复制到 PC

### 构建和安装 Android App

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
```

### 使用方法

```python
from src.adb_tools.clipboard import get_clipboard, set_clipboard, copy_to_pc

# 获取剪贴板
result = get_clipboard()
print(result.text)

# 设置剪贴板
set_clipboard("Hello")

# 复制到 PC 剪贴板
copy_to_pc()
```

### 命令行用法

```bash
# 复制到 PC
python -m src.adb_tools.clipboard.clipboard copy-to-pc

# 多设备时指定序列号
python -m src.adb_tools.clipboard.clipboard -s <设备序列号> copy-to-pc

# 或设置环境变量
set ANDROID_SERIAL=<设备序列号>
python -m src.adb_tools.clipboard.clipboard copy-to-pc
```

### Android App 使用说明

1. 打开 App 后服务会自动启动
2. 支持两种模式：
   - **粘贴板模式**：从系统剪贴板读取
   - **输入内容模式**：直接使用输入框内容
3. 可拖动调整输入框高度
4. 支持文本选择和全选
5. 悬浮窗按钮可显示浮动图标

### ADB 直接访问

```bash
# 读取剪贴板
adb shell content query --uri content://com.clipper.android_clipper.provider/clipboard

# 写入剪贴板
adb shell content insert --uri content://com.clipper.android_clipper.provider/clipboard --bind text:s:Hello
```
