# Android 10+ OnePlus 剪贴板使用步骤

## 1. 安装 App

```bash
cd android/android_clipper
.\gradlew assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 2. Python 获取剪贴板

```python
from src.adb_tools.clipboard import get_clipboard

result = get_clipboard()
print(result.text)
```

## 3. Python 设置剪贴板

```python
from src.adb_tools.clipboard import set_clipboard

set_clipboard("要写入的文本")
```

## 4. 复制到 PC 剪贴板

```python
from src.adb_tools.clipboard import copy_to_pc

copy_to_pc()
```

或命令行：
```bash
python -m src.adb_tools.clipboard.clipboard copy-to-pc
```

> 注意：终端可能因缺少中文字体显示乱码，但内容已正确复制到 PC 剪贴板。

## 5. 命令行直接用 ADB

使用前需要手动打开 App。

```bash
# 读
adb shell content query --uri content://com.clipper.android_clipper.provider/clipboard
adb shell content query --uri content://com.clipper.android_clipper.provider/edit_text
```
```bash
adb devices
List of devices attached
3B65CS012RH00000        device
emulator-5554   device
adb -s 3B65CS012RH00000 shell content query --uri content://com.clipper.android_clipper.provider/clipboard
adb -s 3B65CS012RH00000 shell "run-as com.clipper.android_clipper cat files/clipboard_data.txt"
```

# 写
```bash
adb shell content insert --uri content://com.clipper.android_clipper.provider/clipboard --bind text:s:Hello
```
# 直接读进 PC 粘贴板（会乱码）
```bash
chcp 65001 | Out-Null
adb shell content query --uri content://com.clipper.android_clipper.provider/clipboard `
  | ForEach-Object { (($_ -replace '.*text=','') -replace ', timestamp.*','') } `
  | Set-Clipboard
```

# 直接读进 PC 粘贴板 python
```bash
python -m src.adb_tools.clipboard.clipboard copy-to-pc
```

```bash
python -m src.adb_tools.clipboard.clipboard -s 3B65CS012RH00000 copy-to-pc
set ANDROID_SERIAL=3B65CS012RH00000
python -m src.adb_tools.clipboard.clipboard copy-to-pc
```