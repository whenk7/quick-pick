# Release Workflow 实施报告

日期：2026-10-16
Commit：`0941c0a1169b2a2998f2aeb3bf4c44aa8427b6d7`（main，未 push，待总控统一推）

## 改动文件

1. **`.github/workflows/release.yml`**（新增）
   - 触发：push tag `v*` + workflow_dispatch
   - ubuntu-latest；checkout@v4 → setup-java@v4（temurin 17）→ setup-android@v3
   - `yes | sdkmanager --licenses` + 安装 `platforms;android-36`、`build-tools;36.0.0`（幂等）
   - secret `KEYSTORE_BASE64` base64 解码到 `$RUNNER_TEMP/keystore.jks`
   - `./gradlew assembleRelease lintVitalRelease`，env 注入 KEYSTORE_FILE（`runner.temp`，与 `$RUNNER_TEMP` 同值）/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD
   - 改名 `app-release.apk` → `quickpick-${GITHUB_REF_NAME}.apk`
   - softprops/action-gh-release@v2 挂包
   - 顶部注释：4 个 secret 名称 + "keystore 永不入库"
2. **`app/build.gradle.kts`**
   - signingConfigs 新增 `release`，四个字段全部 `System.getenv()` 读取
   - release buildType 仅当 `KEYSTORE_FILE` env 存在时才 `signingConfig = signingConfigs.getByName("release")`；本地无 env → 产未签名包不报错
   - 版本矩阵及其他配置未动

## 验证结论

| 项 | 命令/方式 | 结果 |
|---|---|---|
| 无 env 本地 release 构建 | `./gradlew assembleRelease` | BUILD SUCCESSFUL in 48s，产出未签名 apk（安全降级生效） |
| lint 阻断检查 | `./gradlew lintVitalRelease` | BUILD SUCCESSFUL（绿） |
| workflow 语法 | python3 `yaml.safe_load` | 解析通过；`on:` 被 YAML 1.1 解析为布尔 `True`——已知无害，未改 |
| 产物路径一致性 | 对照本地 outputs 目录 | CI 有签名时产物即 `app/build/outputs/apk/release/app-release.apk`，与 workflow 改名步骤路径一致（本地无签名时是 `app-release-unsigned.apk`，仅本地场景，不影响 CI） |

## 产物路径

- CI（签名）：`app/build/outputs/apk/release/app-release.apk` → 改名 `quickpick-v*.apk` 挂 Release
- 本地（无 env）：`app/build/outputs/apk/release/app-release-unsigned.apk`

## 遗留疑虑

1. **workflow_dispatch 且 repo 未配 secrets 时**：解码步骤产出空 keystore 文件，gradle 签名步骤会以 keystore 错误失败——失败模式可接受（不会静默产错误包），但报错信息不直观。正式 tag 流程不受影响。
2. R8 有 "parsing kotlin metadata" WARNING（Kotlin 版本新于 R8 内置 metadata 支持），非本次引入、不阻断构建，属版本矩阵既有现象。
3. 本机无法真实模拟 CI 签名路径（无正式 keystore），签名接线逻辑仅静态审查 + 无 env 降级路径验证；首次打 tag 时总控需盯一眼 Actions 运行结果。
