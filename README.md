# Android Help

ADB 工具集，包含剪贴板管理等实用功能。

## 剪贴板工具 (adb-clipboard)

### 功能
- 获取 Android 剪贴板内容
- 设置 Android 剪贴板
- 将 Android 剪贴板复制到 PC

### 安装 Android App

```bash
cd android/android_clipper
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
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
python -m src.adb_tools.clipboard.clipboard copy-to-pc
```
