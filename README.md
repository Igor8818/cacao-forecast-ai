# CacaoForecastAI

Aplicativo Android nativo (offline-first) para previsão de safra e monitoramento fitossanitário de cacau com visão computacional embarcada (modelos TFLite CEPEC).

## Principais Capacidades
- **Gestão Hierárquica Offline:** Cadastro e arquivamento seguro (soft delete) de Fazendas e Roças / Quadras (`Área ha`, `NPT`, `FA`, `NPF`) persistidos no Room (`SQLite`).
- **Amostragem Resiliente em Campo (`1..NPF`):** Captura via CameraX (`1600x1200`) em armazenamento privado, retomada automática de sessões `IN_PROGRESS` após queda de bateria/processo, trava de finalização antes de `NPF/NPF` e revisão rápida de 1 toque (`Confirmar e Próxima`).
- **Inferência Dupla TFLite CEPEC:** Thread dedicada (`cacao-ml-worker`) e buffer `640x640` compartilhado executando `detector.tflite` (contagem geral) + `detector_doenca.tflite` (`PodridaoParda`, `Moniliase`, `Sadio`, `VassouraBruxa`), refinamento morfocolorimétrico e fusão IoU (`0.20f`).
- **Motor Matemático & Relatórios Consolidados:** Produção Bruta (`NFT`), Produção Líquida Sadia (`NFS`), semáforo fitossanitário, deduplicação por quadra no período e ponderação consolidada por área e arrobas.
- **PDF Multi-Página & WhatsApp:** Geração nativa com `PdfDocument`, paginação automática, `Disclaimer` obrigatório em todas as páginas e compartilhamento direto via `FileProvider`.
- **Backup / Restauração SQLite:** Exportação após `PRAGMA wal_checkpoint(FULL)` e importação validada (`PRAGMA integrity_check`, `user_version`, tabelas obrigatórias e rollback automático).

## Stack Técnica
- **Linguagem / UI:** Kotlin `2.0.21`, Jetpack Compose (Material 3), Navigation Compose `2.8.5`
- **Build / SDK:** AGP `8.9.1`, Gradle `8.11.1`, KSP `2.0.21-1.0.28`, `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`
- **Persistência & ML:** Room `2.6.1`, CameraX `1.5.3`, TensorFlow Lite `2.16.1`, ExifInterface `1.3.7`

## Verificação e Build
```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease
```

Consulte [PLAN.md](PLAN.md) para o blueprint arquitetural completo e o detalhamento das tarefas `TASK-01` a `TASK-10`.
