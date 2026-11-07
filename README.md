# 取件宝 (QuickPick)

从手机短信里自动提取快递取件码的 Android 应用。数据只存在本机，没有网络权限。

## 它做什么

快递到驿站/快递柜时会发短信，里面有取件码、柜号、驿站地址。这软件读这些短信，把取件信息整理成卡片列表，按天分组，取完点一下圆圈标记。

- 不追踪物流（那是菜鸟裹裹的活）
- 不上传任何数据，Manifest 里没有网络权限
- 只处理普通文本短信

## 怎么用

1. 装好打开，给短信读取权限
2. **首次打开首页是空的** —— 这软件不内置规则。各家短信格式差异太大，得先告诉它怎么认
3. 点底栏「规则」，配一条规则：
   - 点「新建」：粘贴一条快递短信，点词块自动生成锚点
   - 点「AI 生成」：页面自动挑样本，复制提示词去问任意 AI，把返回的 JSON 贴回来
4. 配好规则回首页，快递信息会扫出来
5. 到驿站看卡片上的取件码取件，取完点圆圈或左滑标记已取
6. 长按卡片能看到短信原文

首页下拉刷新会重新扫一遍所有短信。

## 添加规则（核心操作）

**这软件的重点是规则。** 它不认识「取件码」这个词，得你告诉它长什么样。

### 最快的办法：AI 生成

规则页 → 「AI 生成」→ 页面会自动挑几条你最近的快递短信当样本 → 点「一键复制提示词」→ 粘到任意 AI（DeepSeek、ChatGPT 都行）→ 把 AI 返回的 JSON 贴回页面 → 导入。

### 手动配：编辑器点词块

规则页 → 「新建」→ 粘贴一条完整的快递短信 → 短信会被切成可点的词块 → 点你想提取的内容（比如取件码数字）→ 上下方的「前缀/后缀」输入框会自动填好 → 下方实时预览能看到提取效果 → 保存。

工作原理是锚点定位：**前缀 + 后缀之间的内容**就是提取结果。比如短信里有「取件码为11-2-0272，地址:...」，前缀填「取件码为」、后缀填「，」，提取结果就是 `11-2-0272`。

- 后缀留空 = 一直取到短信末尾
- 后缀填了但在短信里找不到 = 这条字段不提取
- 定位的是**首次出现**，所以锚点要够长够独特

### 规则 JSON 格式

可以导入导出，方便备份和分享：

```json
[
  {
    "name": "兔喜生活",
    "senderPattern": "",
    "fields": {
      "pickupCode": { "prefix": "取件码为", "suffix": "，" },
      "address":    { "prefix": "地址:",   "suffix": "" },
      "brand":      { "prefix": "【",      "suffix": "】" }
    }
  }
]
```

`name` 是规则名（纯记忆用，不显示在卡片上）。卡片标题取的是 `brand` 提取到的品牌名，提不到就显示「快递」。

## 不内置规则

应用不带任何预置规则（v1.1.11 起）。早期版本内置过顺丰、京东两条，实测命中率接近 0——各家驿站模板差异太大，通用规则没有意义，反而让用户以为「装了就该有数据」。

规则完全由用户自己配：手动点词块，或用 AI 生成。

## 架构

```
app/src/main/java/com/quickpick/
├── domain/          纯 Kotlin，不依赖 Android
│   ├── engine/      MatchEngine（锚点匹配）、AnchorSuggestion（编辑器锚点推导）
│   └── model/       MatchRule、PackageRecord、AddressAlias 等
├── data/
│   ├── db/          Room v4，3 张表（规则/记录/已扫短信）
│   ├── repository/  入库去重与归并逻辑都在 PackageRepository
│   ├── local/       SettingsStore（DataStore 偏好）
├── ui/
│   ├── screen/      页面
│   ├── component/   卡片、弹窗、玻璃底栏等
│   ├── viewmodel/   状态管理
│   └── theme/       主题（miuix，无 material3）
└── util/            SmsReader、JsonExporter、UiNotifier
```

技术栈：Kotlin + Jetpack Compose + miuix 0.9.4（不是 material3）+ Room + DataStore。
minSdk 33 / targetSdk 37。

## 构建与测试

```bash
./gradlew assembleDebug          # 打调试包
./gradlew testDebugUnitTest      # 跑单测（需 JDK 21）
```

`app/src/test/java/com/quickpick/corpus/` 下有 50 条真实短信语料，覆盖提取精度、误报、归并等场景。改匹配引擎或规则逻辑前先跑这个。

`review/` 目录下的测试用 Robolectric，需要额外下载 `android-all` jar，网络不好会卡很久。

## 发版

推 `v*` tag 触发 GitHub Actions，自动签名打包并挂到 Release。版本号在 `app/build.gradle.kts` 的 `versionCode` / `versionName`，每次发版 versionCode +1。

## 已知限制

- **多码短信只提取成一串**：短信里有两个包裹两个码（`取件码为2-3-0644, 1-2-0602`）时，整串被当做一个取件码
- **规则不通用**：锚点跟具体短信措辞强绑定，换城市、换驿站名可能要改规则
- 只提取取件短信，不支持退货上门取件那类

## 给 AI 开发者的注意事项

### 不能破的约定

- **domain 层不依赖 Android**：`domain/` 下全是纯 Kotlin，可以跑 JVM 单测
- **UI 层不写死颜色**：所有颜色从 `MiuixTheme.colorScheme` 取（见 `ui/theme/Color.kt`），这样主题切换才能全局生效
- **匹配引擎不用正则**：纯 `indexOf` 字符串定位，这是刻意的设计选择
- **中文文案不用「错误」「失败」**：用「没能…」「暂时…」这类温和措辞

### 容易踩的坑

- **`PackageRepository.insertFromSms` 很复杂**：同一条短信去重、同单号换柜覆盖、无单号同码归并、归并历史防复活，四套逻辑叠在一起。改动前先看 `PackageRepositoryTest` 和 `PackageRepositoryMergeTest` 里的用例
- **`mergedSmsIds` 不能简化**：归并时被顶掉的旧短信 id 存在这里，删除记录时要把它们全加进忽略名单，否则重扫会复活
- **规则变更必须打 `rules_dirty` 标记**：任何写规则的地方（编辑器保存、规则页导入、AI 导入、删除）都要调 `settingsStore.markRulesDirty()`。首页 `ON_RESUME` 看到标记就全量重扫并复位。漏掉这一步，用户配完规则回首页还是空的
- **编辑器锚点推导**：`AnchorSuggestion.suggestAnchor` 有强制自检——算出的锚点必须能还原出值才采用。别把自检去掉
- **miuix `OverlayDialog` 不消费返回键**：每个弹窗都要自己加 `BackHandler`

### 测试环境

本地 `./gradlew testDebugUnitTest` 能跑纯 JVM 测试（domain/corpus/repository）。
`review/` 下的 Robolectric 测试在部分网络环境会卡在下载 `android-all`，CI 上正常。

### 分支与提交

单人项目，最简模型：`main` 永远可发布，改动开 `feat/` `fix/` `refactor/` 分支，绿了合回 main 删掉。版本用 tag 打，不用分支。commit 前缀 `feat:` `fix:` `docs:` `chore:`。
