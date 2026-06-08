# 渲染逻辑 Native 加密

把客户端渲染逻辑（`com.lirxowo.nbtexporter.exporter` 包）的 Java 字节码转译为本地
`dll`，方法体以 `native` 形式存在 —— 反编译最终 jar 只能看到空的 native 声明，无法
还原渲染实现。主类 / 命令 / Mixin 保持普通字节码，服务端与非渲染路径不受影响。

## 前置环境（开发机即可）

- JDK 21（脚本默认 `C:\Program Files\Java\jdk-21.0.10`，工具与 gradlew 启动用）
- Visual Studio「使用 C++ 的桌面开发」工作负载（MSVC + Windows SDK）
- CMake（脚本自动调用）

## 用法

```powershell
pwsh -NoProfile -File tools\native-obfuscator\build-native.ps1
# 可选: -JdkHome <path>  -Vcvars <vcvars64.bat path>
```

产物：`dist\nbtexporter-<version>-native.jar`

## 原理（脚本 6 步）

1. `gradlew jar` 产出正常 v69 jar
2. 字节级把 class 主版本 `69 → 65`（native-obfuscator 的 ASM 不支持 v69；运行时
   Java 25 向后兼容，渲染代码为 Java 21 语义故合法）
3. native-obfuscator 按 `whitelist.txt` 只转译 exporter 包 → C++ 源码 + native stub jar
4. MSVC 编译 C++ → `native_library.dll`
5. dll 内嵌为 jar 资源 `native0/x64-windows.dll`（`native0.Loader` 运行时解压后
   `System.load`）
6. 输出 `dist` 最终 jar

## 限制与注意

- **仅 Windows x64**：dll 只为 win-x64 编译。非 Windows / 非 x64 客户端执行导出命令时，
  `ExporterCommand` 的 `isNativeSupported()` 守卫会拦下并提示
  `nbtexporter.command.unsupported_platform`，不会崩溃，但无法使用导出功能。
- **渲染类不得使用 Java 22+ 语言特性**：降版本到 65 的前提是字节码在 v65 下合法。
  新增 / 修改渲染代码请保持 Java 21 语义。
- 改动 exporter 包代码后，必须重新运行脚本重建 dll 与 jar。
- `whitelist.txt` 控制 native 化范围，当前为 `com/lirxowo/nbtexporter/exporter/**`。

## 运行时验证（须真机 MC 实测）

转译 / 编译 / 打包链已验证通过；native 方法在 NeoForge ClassLoader + MC 渲染环境下
的运行时正确性需自测：

1. 将 `dist` 的 native jar 放入 MC 1.21.11 + NeoForge 26.1.2 客户端实例的 `mods\`
2. 进入存档，执行 `/nbtexporter export`
3. 确认导出界面正常渲染、PNG 导出正确、无 `UnsatisfiedLinkError` / JNI 崩溃
4. 关注帧率：native 方法经 JNI 反射式调用 MC API，留意渲染性能开销
