# 语义搜索 (Semantic Search) - Android 应用

[English](README.md) | 中文 | [Français](README.fr.md)

一个基于嵌入向量的智能推荐/搜索 Android 应用，实现本地知识库的语义搜索功能。

## 功能特性

- **语义搜索**: 通过文本输入，快速搜索并返回语义最相似的内容
- **本地知识库**: 完全离线运行，数据存储在本地 SQLite 数据库（Room）
- **向量嵌入**: 基于 N-gram 和哈希技巧的轻量级文本向量化
- **K-NN 搜索**: 使用余弦相似度进行 K-最近邻搜索
- **现代 UI**: 采用 Material Design 3 和 Jetpack Compose
- **动态增量索引 (v2)**: 通过 Manifest 记录文件指纹与向量 ID 映射，支持新增/修改/删除的增量更新
- **文件夹同步 (SAF)**: 支持通过 Storage Access Framework 选择文件夹并同步到知识库
- **PDF / DOCX 支持**: 支持从 PDF、Word(docx) 提取文本并索引
- **Embedding 空间可视化**: 对当前向量做 PCA 降维并以 2D 散点图展示，支持高亮本次新增/更新向量
- **应用内语言切换**: 支持中文 / English / Français 三语切换（基于 AppCompat per-app locales）

## 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ SearchScreen│  │KnowledgeBase│  │ Visualization   │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   ViewModel Layer                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │              MainViewModel                       │    │
│  │  - 搜索状态管理  - UI状态  - 文档CRUD操作       │    │
│  │  - 文件夹同步(SAF) - 可视化数据刷新             │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                  Repository Layer                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │           DocumentRepository                     │    │
│  │  - 协调数据访问  - 嵌入生成  - 搜索执行         │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                Indexing Layer (v2)                       │
│  ┌─────────────────────────────────────────────────┐    │
│  │           FileIndexingService                   │    │
│  │  - 扫描/比对/增量更新  - PDF/DOCX提取           │    │
│  └─────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────┐    │
│  │              ManifestStore (JSON)               │    │
│  │  - index_manifest_v2.json 维护文件指纹与ID映射  │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                    Core Engines                          │
│  ┌──────────────────┐    ┌─────────────────────────┐    │
│  │ EmbeddingEngine  │    │   VectorSearchEngine    │    │
│  │ - N-gram分词     │    │   - 余弦相似度计算      │    │
│  │ - 哈希向量化     │    │   - K-NN搜索            │    │
│  │ - L2归一化       │    │   - 内存向量索引        │    │
│  └──────────────────┘    └─────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                    Data Layer                            │
│  ┌──────────────────┐    ┌─────────────────────────┐    │
│  │   Room Database  │    │      Document DAO       │    │
│  │   (SQLite)       │    │                         │    │
│  └──────────────────┘    └─────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## 核心算法

### 1. 文本嵌入向量生成 (EmbeddingEngine)

```kotlin
// 向量维度: 256
// 算法流程:
1. 文本预处理 (小写化、去标点)
2. 分词 (支持中英文混合)
3. 生成N-gram (1-gram到3-gram)
4. 使用哈希技巧映射到固定维度向量
5. L2归一化
```

### 2. K-NN 搜索 (VectorSearchEngine)

```kotlin
// 余弦相似度计算
cos(A, B) = (A · B) / (||A|| * ||B||)

// 搜索流程:
1. 计算查询向量与所有文档向量的相似度
2. 过滤低于阈值的结果
3. 按相似度降序排序
4. 返回Top-K结果
```

## 项目结构

```
app/src/main/java/com/semanticsearch/app/
├── SemanticSearchApp.kt          # Application类
├── MainActivity.kt               # 主Activity
├── data/
│   ├── Document.kt              # 文档实体
│   ├── DocumentDao.kt           # 数据访问接口
│   └── AppDatabase.kt           # Room数据库
├── embedding/
│   └── EmbeddingEngine.kt       # 嵌入向量生成引擎
├── search/
│   └── VectorSearchEngine.kt    # 向量搜索引擎
├── indexing/
│   ├── FileIndexingService.kt   # 动态增量索引 + SAF 文件夹同步 + PDF/DOCX提取
│   └── IndexManifest.kt         # Manifest 数据结构与 JSON 持久化
├── repository/
│   └── DocumentRepository.kt    # 数据仓库
├── viewmodel/
│   └── MainViewModel.kt         # 视图模型
├── visualization/
│   └── Pca2DReducer.kt          # PCA 降维（EJML）
└── ui/
    ├── MainScreen.kt            # 主屏幕
    ├── theme/                   # Material主题
    ├── screens/
    │   ├── SearchScreen.kt      # 搜索界面
    │   ├── KnowledgeBaseScreen.kt # 知识库界面
    │   └── EmbeddingVisualizationScreen.kt # 可视化界面
    └── components/
        └── Dialogs.kt           # 对话框组件
```

## 依赖项

- **Jetpack Compose**: 现代声明式 UI 框架
- **Room**: SQLite 抽象层
- **Kotlin Coroutines**: 异步编程
- **Material 3**: Material Design 组件
- **AndroidX DocumentFile**: SAF 文件夹遍历与文档访问
- **pdfbox-android**: PDF 文本提取（需在 Application 初始化 `PDFBoxResourceLoader`）
- **EJML**: PCA 降维计算
- **AppCompat**: 应用内语言切换（AppCompatDelegate per-app locales）
- **ONNX Runtime**（可选）: 用于更高级的 ML 模型推理

## 构建与运行

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

### 构建步骤

```bash
# 在项目根目录执行
./gradlew assembleDebug

# APK 输出位置:
# app/build/outputs/apk/debug/app-debug.apk
```

## 使用说明

1. **添加文档**: 在“知识库”页面点击“添加文档”按钮
2. **输入内容**: 填写文档标题和内容，点击保存
3. **文件夹同步 (SAF)**: 在“知识库”页面点击“选择文件夹并同步”，选择包含文档的文件夹
   - 支持扩展名: `txt` / `md` / `pdf` / `docx`
   - 同步会执行增量更新：新增/修改/删除会被精确识别并更新到本地数据库与向量索引
4. **语义搜索**: 切换到“搜索”页面，输入查询文本
5. **Embedding 可视化**: 切换到“可视化”页面查看降维后的向量散点分布
   - 灰色点表示历史向量
   - 高亮点表示本次新增/更新的向量
6. **语言切换**: 点击顶部“语言”按钮，在中文 / English / Français 之间切换

## 扩展建议

### 使用预训练模型提升效果

如需更好的语义理解能力，可以集成轻量级预训练模型:

1. **MiniLM**: 小型 BERT 变体，适合移动端
2. **DistilBERT**: 蒸馏版 BERT
3. **Sentence-BERT**: 专为句子嵌入设计

```kotlin
// 示例: 集成ONNX模型
// 1. 下载模型文件 (如 all-MiniLM-L6-v2.onnx)
// 2. 放置到 assets/ 目录
// 3. 使用 ONNX Runtime 加载并推理
```

## 许可证

MIT License
