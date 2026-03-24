# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2026-03-24

### Added
- **Banking-Grade Pinyin Search (Advanced)**:
  - Support for Chinese, full Pinyin, Pinyin initials, and mixed-input searching.
  - Prefix matching via `edge_ngram` for instant feedback.
  - Fuzzy matching (Levenshtein distance) for Pinyin typo tolerance.
  - Autocomplete (Suggest) API using Elasticsearch Completion Suggester.
- **Observability & Debugging**:
  - New `SearchResponseDTO` providing metadata: latentcy (`took`), search `dsl`, and `matchedFields`.
  - Analyze API (`GET /contacts/analyze`) for real-time pinyin tokenization inspection.
- **Large-Scale Validation**:
  - High-performance data generation script (Python) supporting 10,000+ records.
  - Mass-scale verification results (100% success rate on 10k data).
- **Documentation**:
  - Comprehensive `test_guide.md` for functional verification.
  - Updated `README.md` with search principles and architecture.

### Fixed
- Mapping issue for `completion` field in Elasticsearch.
- Duplicate entry errors in MySQL caused by millisecond-level ID collisions in parallel imports.
- Missing imports and package declarations in `ContactSearchRepository`.
- Unused imports across service and repository layers.

### Modified
- Standardized search results format across `ContactService` and `VoiceService`.
- Enhanced `ContactSearchRepository` with weighted scoring (`function_score`) to prioritize exact matches and frequent contacts.

## [1.0.0] - Initial Version
- Basic contact management and simple search functionality.
