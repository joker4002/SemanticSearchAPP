# Recherche Sémantique (Semantic Search) - Application Android

[English](README.md) | [中文](README.zh.md) | Français

Une application Android de recherche/recommandation basée sur des embeddings vectoriels, permettant la recherche sémantique sur une base de connaissances locale (hors-ligne).

## Fonctionnalités

- **Recherche sémantique** : saisissez du texte et obtenez les contenus les plus similaires
- **Base de connaissances locale** : fonctionnement hors-ligne, données stockées en SQLite (Room)
- **Embeddings** : vectorisation légère basée sur N-grams et hashing
- **Recherche K-NN** : similarité cosinus pour le K plus proches voisins
- **UI moderne** : Material Design 3 et Jetpack Compose
- **Indexation incrémentale dynamique (v2)** : un manifest relie empreintes des fichiers et IDs de vecteurs, avec ajout/mise à jour/suppression incrémentale
- **Synchronisation de dossier (SAF)** : sélection d’un dossier via Storage Access Framework et synchronisation vers la base
- **Support PDF / DOCX** : extraction de texte depuis PDF et Word (docx) puis indexation
- **Visualisation de l’espace d’embeddings** : réduction PCA en 2D (EJML) et nuage de points, avec surlignage des ajouts/mises à jour récents
- **Changement de langue in-app** : 中文 / English / Français (AppCompat per-app locales)

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
│  │  - états UI/recherche  - CRUD documents          │    │
│  │  - sync dossier (SAF)  - refresh visualisation   │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                  Repository Layer                        │
│  ┌─────────────────────────────────────────────────┐    │
│  │           DocumentRepository                     │    │
│  │  - accès données  - embeddings  - recherche      │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                Indexing Layer (v2)                       │
│  ┌─────────────────────────────────────────────────┐    │
│  │           FileIndexingService                   │    │
│  │  - scan/diff/incrémental  - PDF/DOCX extraction │    │
│  └─────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────┐    │
│  │              ManifestStore (JSON)               │    │
│  │  - index_manifest_v2.json: empreinte ↔ IDs      │    │
│  └─────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                    Core Engines                          │
│  ┌──────────────────┐    ┌─────────────────────────┐    │
│  │ EmbeddingEngine  │    │   VectorSearchEngine    │    │
│  │ - N-grams/hash   │    │   - cosinus / KNN        │    │
│  └──────────────────┘    └─────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│                    Data Layer                            │
│  ┌──────────────────┐    ┌─────────────────────────┐    │
│  │   Room Database  │    │      Document DAO       │    │
│  └──────────────────┘    └─────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

## Algorithmes clés

### 1. Génération d’embeddings (EmbeddingEngine)

```kotlin
// dimension: 256
// pipeline:
1. pré-traitement (minuscule, suppression ponctuation)
2. tokenisation (mix chinois/anglais)
3. N-grams (1 à 3)
4. hashing vers un vecteur de dimension fixe
5. normalisation L2
```

### 2. Recherche K-NN (VectorSearchEngine)

```kotlin
// similarité cosinus
cos(A, B) = (A · B) / (||A|| * ||B||)

// pipeline:
1. calcul similitudes requête ↔ documents
2. filtre par seuil
3. tri décroissant
4. retourne Top-K
```

## Structure du projet

```
app/src/main/java/com/semanticsearch/app/
├── SemanticSearchApp.kt
├── MainActivity.kt
├── data/
├── embedding/
├── search/
├── indexing/
├── repository/
├── viewmodel/
├── visualization/
└── ui/
```

## Dépendances

- **Jetpack Compose**
- **Room**
- **Kotlin Coroutines**
- **Material 3**
- **AndroidX DocumentFile** (SAF)
- **pdfbox-android** (extraction PDF)
- **EJML** (PCA)
- **AppCompat** (changement de langue)
- **ONNX Runtime** (optionnel)

## Build & Run

### Prérequis

- Android Studio Hedgehog (2023.1.1) ou plus récent
- JDK 17
- Android SDK 34
- Gradle 8.2

### Build

```bash
# exécuter à la racine du projet
./gradlew assembleDebug

# APK:
# app/build/outputs/apk/debug/app-debug.apk
```

## Utilisation

1. **Ajouter un document** : dans “Knowledge Base”, cliquer “Add Document”
2. **Saisir le contenu** : titre + contenu puis sauvegarder
3. **Sync dossier (SAF)** : cliquer “Pick folder & sync” et choisir un dossier
   - extensions: `txt` / `md` / `pdf` / `docx`
   - mise à jour incrémentale: ajout / modification / suppression
4. **Recherche** : onglet “Search”, saisir une requête
5. **Visualisation** : onglet “Visualization” pour le nuage 2D (PCA)
6. **Langue** : bouton “Language” en haut, choisir 中文 / English / Français

## Licence

MIT License
