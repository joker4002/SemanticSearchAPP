# Semantic Search - Android App

English | [Chinese](README.zh.md) | [French](README.fr.md)

An Android semantic search app based on vector embeddings, providing fully offline semantic search over a local knowledge base.

## Features

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
│  │  - search/ui state  - CRUD documents             │    │
│  │  - folder sync (SAF) - visualization refresh     │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                  Repository Layer                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │           DocumentRepository                     │    │
│  │  - data access  - embedding generation  - search │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                Indexing Layer (v2)                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │           FileIndexingService                   │    │
│  │  - scan/diff/incremental updates                │    │
│  │  - PDF/DOCX extraction                          │    │
│  └─────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────┐    │
│  │              ManifestStore (JSON)               │    │
│  │  - index_manifest_v2.json fingerprint ↔ ID map  │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                    Core Engines                         │
│  ┌──────────────────┐    ┌─────────────────────────┐    │
│  │ EmbeddingEngine  │    │   VectorSearchEngine    │    │
│  │ - N-gram tokens  │    │   - cosine similarity    │   │
│  │ - hashing vector │    │   - K-NN search          │   │
│  │ - L2 normalize   │    │   - in-memory index      │   │

│  └──────────────────┘    └─────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                    Data Layer                           │
│  ┌──────────────────┐    ┌─────────────────────────┐    │
│  │   Room Database  │    │      Document DAO       │    │
│  │   (SQLite)       │    │                         │    │
│  └──────────────────┘    └─────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## Core Algorithms

### 1. Embedding Generation (EmbeddingEngine)

```kotlin
// vector dimension: 256
// pipeline:
1. preprocessing (lowercasing, punctuation removal)
2. tokenization (supports mixed Chinese/English)
3. N-grams (1-gram to 3-gram)
4. hashing into a fixed-dimension vector
5. L2 normalization
```

### 2. K-NN Search (VectorSearchEngine)

```kotlin
// cosine similarity
cos(A, B) = (A · B) / (||A|| * ||B||)

// search pipeline:
1. compute similarity between query vector and all document vectors
2. filter by threshold
3. sort by similarity desc
4. return Top-K
```

## Project Structure

```
app/src/main/java/com/semanticsearch/app/
├── SemanticSearchApp.kt          # Application
├── MainActivity.kt               # Main activity
├── data/
│   ├── Document.kt              # Entity
│   ├── DocumentDao.kt           # DAO
│   └── AppDatabase.kt           # Room database
├── embedding/
│   └── EmbeddingEngine.kt       # Embedding engine
├── search/
│   └── VectorSearchEngine.kt    # Vector search engine
├── indexing/
│   ├── FileIndexingService.kt    # Incremental indexing + SAF folder sync + PDF/DOCX extraction
│   └── IndexManifest.kt          # Manifest model + JSON persistence
├── repository/
│   └── DocumentRepository.kt    # Repository
├── viewmodel/
│   └── MainViewModel.kt         # ViewModel
├── visualization/
│   └── Pca2DReducer.kt           # PCA reducer (EJML)
└── ui/
    ├── MainScreen.kt            # Root screen
    ├── theme/                   # Material theme
    ├── screens/
    │   ├── SearchScreen.kt      # Search screen
    │   └── KnowledgeBaseScreen.kt # Knowledge base screen
    └── components/
        └── Dialogs.kt           # Dialog components
```

## Dependencies

- **Jetpack Compose**: modern declarative UI
- **Room**: SQLite abstraction
- **Kotlin Coroutines**: async programming
- **Material 3**: Material Design components
- **AndroidX DocumentFile**: SAF folder traversal
- **pdfbox-android**: PDF text extraction (requires `PDFBoxResourceLoader` init in `Application`)
- **EJML**: PCA computation
- **AppCompat**: in-app language switching (AppCompatDelegate per-app locales)
- **ONNX Runtime** (optional): on-device model inference

## Build & Run

### Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Gradle 8.2

### Build

```bash
# Run from the project root
./gradlew assembleDebug

# APK output:
# app/build/outputs/apk/debug/app-debug.apk
```

## Usage

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

## Extensions

### Improve quality with pretrained models

If you need stronger semantic understanding, you can integrate a lightweight pretrained model:

1. **MiniLM**: a small BERT variant suitable for mobile
2. **DistilBERT**: distilled BERT
3. **Sentence-BERT**: sentence embedding models

```kotlin
// Example: integrate an ONNX model
// 1. Download a model file (e.g., all-MiniLM-L6-v2.onnx)
// 2. Put it under assets/
// 3. Load and run with ONNX Runtime
```

## License

MIT License
