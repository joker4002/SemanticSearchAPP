# 语义搜索 (Semantic Search) - Android应用

一个基于嵌入向量的智能推荐/搜索Android应用，实现本地知识库的语义搜索功能。

## 功能特性

- **语义搜索**: 通过文本输入，快速搜索并返回语义最相似的内容
- **本地知识库**: 完全离线运行，数据存储在本地SQLite数据库
- **向量嵌入**: 基于N-gram和哈希技巧的轻量级文本向量化
- **K-NN搜索**: 使用余弦相似度进行K-最近邻搜索
- **现代UI**: 采用Material Design 3和Jetpack Compose

<<<<<<< Updated upstream
## 技术架构
=======
- **Semantic Search**: type a query and get the most semantically similar content
- **Local Knowledge Base**: fully offline, stored in local SQLite (Room)
- **Embeddings**: lightweight N-gram + hashing vectorization
- **K-NN Search**: cosine similarity based nearest-neighbor search
- **Modern UI**: Material Design 3 + Jetpack Compose
- **Dynamic Incremental Indexing (v2)**: a manifest tracks file fingerprints ↔ vector IDs, enabling accurate add/update/delete sync
- **Folder Sync (SAF)**: pick a folder via Storage Access Framework and sync into the knowledge base
- **PDF / DOCX Support**: extract text from PDF and Word (docx) and index it
- **Embedding Space Visualization**: PCA (EJML) reduction to 2D scatter plot, highlighting newly upserted vectors
- **In-app Language Switch**: Chinese / English / French (AppCompat per-app locales)

## Architecture
>>>>>>> Stashed changes

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ SearchScreen│  │KnowledgeBase│  │    Dialogs      │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   ViewModel Layer                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │              MainViewModel                       │    │
│  │  - 搜索状态管理  - UI状态  - 文档CRUD操作       │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                  Repository Layer                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │           DocumentRepository                     │    │
│  │  - data access  - embedding generation  - search │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
<<<<<<< Updated upstream
=======
│                Indexing Layer (v2)                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │           FileIndexingService                   │    │
│  │  - scan/diff/incremental updates  - PDF/DOCX extraction│    │
│  └─────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────┐    │
│  │              ManifestStore (JSON)               │    │
│  │  - index_manifest_v2.json maintains fingerprint ↔ ID map│    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
>>>>>>> Stashed changes
│                    Core Engines                          │
│  ┌──────────────────┐    ┌─────────────────────────┐    │
│  │ EmbeddingEngine  │    │   VectorSearchEngine    │    │
│  │ - N-gram tokens  │    │   - cosine similarity    │    │
│  │ - hashing vector │    │   - K-NN search          │    │
│  │ - L2 normalize   │    │   - in-memory index      │    │
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

### 2. K-NN搜索 (VectorSearchEngine)

```kotlin
// cosine similarity
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
├── repository/
│   └── DocumentRepository.kt    # 数据仓库
├── viewmodel/
│   └── MainViewModel.kt         # 视图模型
└── ui/
    ├── MainScreen.kt            # 主屏幕
    ├── theme/                   # Material主题
    ├── screens/
    │   ├── SearchScreen.kt      # 搜索界面
    │   └── KnowledgeBaseScreen.kt # 知识库界面
    └── components/
        └── Dialogs.kt           # 对话框组件
```

## 依赖项

- **Jetpack Compose**: 现代声明式UI框架
- **Room**: SQLite抽象层
- **Kotlin Coroutines**: 异步编程
- **Material 3**: Material Design组件
- **ONNX Runtime** (可选): 用于更高级的ML模型推理

## 构建与运行

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

### 构建步骤

```bash
# 克隆项目后，使用Android Studio打开
# 或使用命令行构建:

cd SemanticSearch
./gradlew assembleDebug

# APK输出位置:
# app/build/outputs/apk/debug/app-debug.apk
```

## 使用说明

<<<<<<< Updated upstream
1. **添加文档**: 在"知识库"页面点击"添加文档"按钮
2. **输入内容**: 填写文档标题和内容，点击保存
3. **语义搜索**: 切换到"搜索"页面，输入查询文本
4. **查看结果**: 系统会返回语义最相似的文档，并显示相似度百分比
=======
1. **Add a document**: in “Knowledge Base”, tap “Add Document”
2. **Enter content**: provide title and content, then save
3. **Folder sync (SAF)**: in “Knowledge Base”, tap “Pick folder & sync”, then select a folder
   - supported extensions: `txt` / `md` / `pdf` / `docx`
   - sync is incremental: add / update / delete are detected and applied
4. **Search**: switch to “Search” and type your query
5. **Embedding visualization**: switch to “Visualization” to view PCA-reduced 2D scatter plot
   - gray points: existing vectors
   - highlighted points: newly upserted vectors (useful to demonstrate dynamic insertion/drift)
6. **Language**: tap the top “Language” button and switch between Chinese / English / French
>>>>>>> Stashed changes

## 扩展建议

### 使用预训练模型提升效果

如需更好的语义理解能力，可以集成轻量级预训练模型:

1. **MiniLM**: 小型BERT变体，适合移动端
2. **DistilBERT**: 蒸馏版BERT
3. **Sentence-BERT**: 专为句子嵌入设计

```kotlin
// 示例: 集成ONNX模型
// 1. 下载模型文件 (如 all-MiniLM-L6-v2.onnx)
// 2. 放置到 assets/ 目录
// 3. 使用 ONNX Runtime 加载并推理
```

## 许可证

MIT License
