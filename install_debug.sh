#!/bin/bash
echo "安装调试版本APK..."
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    adb install -r "$APK_PATH"
    echo "安装完成！"
else
    echo "请先运行: ./gradlew assembleDebug"
fi