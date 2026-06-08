#requires -Version 7
<#
.SYNOPSIS
  产出 native 加密的 NBTExporter mod jar。

.DESCRIPTION
  仅 native 化 com.lirxowo.nbtexporter.exporter 包（客户端渲染逻辑），
  主类 / 命令 / Mixin 保持普通字节码（服务端与非渲染路径安全）。
  流程: gradlew jar(v69) -> 字节级降版本到 v65 -> native-obfuscator 转译
        -> MSVC 编译 dll -> 内嵌 native0/x64-windows.dll -> 输出 *-native.jar

  限制: 生成的 dll 仅 Windows x64。非 Windows 客户端加载 exporter 渲染类会
        UnsatisfiedLinkError，须在入口按 OS 守卫（见 README 说明）。
#>
param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$Vcvars  = "C:\Program Files\Microsoft Visual Studio\18\Enterprise\VC\Auxiliary\Build\vcvars64.bat"
)
$ErrorActionPreference = 'Stop'
if (-not $JdkHome) { $JdkHome = "C:\Program Files\Java\jdk-21.0.10" }

$jar  = Join-Path $JdkHome 'bin\jar.exe'
$java = Join-Path $JdkHome 'bin\java.exe'
foreach ($p in @($jar, $java, $Vcvars)) {
    if (-not (Test-Path $p)) { throw "未找到: $p" }
}

$toolDir   = $PSScriptRoot
$root      = (Resolve-Path (Join-Path $toolDir '..\..')).Path
$obfJar    = Join-Path $toolDir 'obfuscator.jar'
$whitelist = Join-Path $toolDir 'whitelist.txt'
$work      = Join-Path $toolDir 'pipeline'

if (-not (Test-Path $obfJar))    { throw "缺少 obfuscator.jar: $obfJar" }
if (-not (Test-Path $whitelist)) { throw "缺少 whitelist.txt: $whitelist" }

Write-Host '[1/6] gradlew jar (v69) ...' -ForegroundColor Cyan
$env:JAVA_HOME = $JdkHome
& (Join-Path $root 'gradlew.bat') jar --console=plain | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'gradlew jar 失败' }
$srcJar = Get-ChildItem (Join-Path $root 'build\libs\*.jar') |
    Where-Object { $_.Name -notmatch 'sources|javadoc|native' } | Select-Object -First 1
if (-not $srcJar) { throw 'build/libs 未找到源 jar' }

if (Test-Path $work) { Remove-Item $work -Recurse -Force }
$null = New-Item -ItemType Directory $work

Write-Host '[2/6] 字节级降 class 版本 69 -> 65 ...' -ForegroundColor Cyan
$exp = Join-Path $work 'exp'
Copy-Item $srcJar.FullName (Join-Path $work 'in.zip')
Expand-Archive (Join-Path $work 'in.zip') $exp
$patchedCount = 0
Get-ChildItem $exp -Recurse -Filter *.class | ForEach-Object {
    $b = [IO.File]::ReadAllBytes($_.FullName)
    if ($b[6] -eq 0 -and $b[7] -eq 69) { $b[7] = 65; [IO.File]::WriteAllBytes($_.FullName, $b); $patchedCount++ }
}
Write-Host "      降版本 $patchedCount 个 class"
$patched = Join-Path $work 'patched.jar'
& $jar cf $patched -C $exp .

Write-Host '[3/6] native-obfuscator 转译 (白名单: exporter 包) ...' -ForegroundColor Cyan
$obf = Join-Path $work 'obf'
& $java -jar $obfJar $patched $obf -w $whitelist -p hotspot | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'native-obfuscator 失败' }

Write-Host '[4/6] MSVC 编译 dll ...' -ForegroundColor Cyan
$cpp   = Join-Path $obf 'cpp'
$build = Join-Path $cpp 'b'
$cmd = "call `"$Vcvars`" >nul 2>&1 && set `"JAVA_HOME=$JdkHome`" && " +
       "cmake -S `"$cpp`" -B `"$build`" -G `"NMake Makefiles`" -DCMAKE_BUILD_TYPE=Release >nul 2>&1 && " +
       "cmake --build `"$build`" >nul 2>&1"
cmd /c $cmd
$dll = Join-Path $build 'build\lib\native_library.dll'
if (-not (Test-Path $dll)) { throw 'dll 编译失败' }

Write-Host '[5/6] 内嵌 dll -> native0/x64-windows.dll ...' -ForegroundColor Cyan
$obfJarOut = (Get-ChildItem (Join-Path $obf '*.jar') | Select-Object -First 1).FullName
$stage = Join-Path $work 'dllstage'
$null = New-Item -ItemType Directory (Join-Path $stage 'native0') -Force
Copy-Item $dll (Join-Path $stage 'native0\x64-windows.dll')
& $jar uf $obfJarOut -C $stage 'native0/x64-windows.dll'

Write-Host '[6/6] 输出最终 jar ...' -ForegroundColor Cyan
$distDir = Join-Path $root 'dist'
$null = New-Item -ItemType Directory $distDir -Force
$finalPath = Join-Path $distDir ($srcJar.BaseName + '-native.jar')
for ($i = 1; $i -le 5; $i++) {
    try {
        if (Test-Path $finalPath) { Remove-Item $finalPath -Force }
        Copy-Item $obfJarOut $finalPath -Force
        break
    } catch {
        if ($i -eq 5) { throw }
        Start-Sleep -Milliseconds 800
    }
}

Write-Host ""
Write-Host "完成 -> $finalPath" -ForegroundColor Green
Write-Host ("大小: {0:N0} bytes" -f (Get-Item $finalPath).Length)
