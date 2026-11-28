# PLAN_QA_POLISH.md — Plano Unificado de Alinhamento à Especificação Executiva & Polimento Pós-ADB (`CacauLens`)

> **Status:** Planejamento Detalhado Congelado para Orquestração (Execução via **Neon / Orchestrator**)
> **Base Auditada:** Commit `c9f2526` (`53/53` testes unitários verdes • Auditoria ao vivo via ADB no aparelho `23122PCD1G` `1220x2712` + Análise integral do Fluxograma Oficial enviado pela chefia)
> **Objetivo Central:**
> 1. Atender **100% das demandas do Fluxograma Executivo da Chefia** (nomenclatura oficial `Relatório Parcial` e `Relatório Cumulativo`, coluna `"Npla"` preservada, PDF + WhatsApp também para o gráfico cumulativo, filtro de período no cumulativo, explicitação visual da fórmula `NFT` vs `NFS`, instrução clara de `Produção Real` e modo de demonstração com a fazenda oficial **`Sempre Viva`**);
> 2. Corrigir **todos os bugs visuais, táteis e de navegação** detectados na auditoria real via ADB, **sem quebrar nenhum teste unitário legado nem alterar schemas do Room ou binários TFLite**.

---

## 1. Cruzamento Completo: Demandas do Chefe + Achados ADB

### 1.1. Novas Demandas Extraídas do Fluxograma Oficial da Chefia (`BOSS-01` a `BOSS-06`)

| ID | Exigência no Documento do Chefe | Lacuna Atual no App | Solução Projetada neste Plano |
| :--- | :--- | :--- | :--- |
| **BOSS-01** | Dois relatórios nomeados **`Relatório Parcial`** *(1 faz, 1 ou + quadras)* e **`Relatório Cumulativo`** *(1 faz e 1 quadra)*, coluna **`Npla`** e título **`Estimação de Safras e Perdas`** / **`Produção`**. | Abas atuais chamam-se `"Consolidado (Tabela & PDF)"` (quebrando em 2 linhas) e `"Evolução (Gráfico)"`; `PDF_TITLE` diz `"Relatório Consolidado de Safra"`. | Renomear abas para **`Relatório Parcial`** e **`Relatório Cumulativo`**, manter **`Npla`** na tabela e ajustar `BrandIdentity` para `CacauLens – Estimação de Safras e Perdas` e `CacauLens – Produção`. |
| **BOSS-02** | Emissão de documento gerencial também para o **Relatório Cumulativo** (`Faz: Sempre Viva Quadra: Topo do Morro`, gráfico `ProdEstimada (@)` vs `ProdReal (@)`). | Apenas a aba 1 (Tabela) gera PDF e envia no WhatsApp; a aba 2 (Gráfico Cumulativo) só aparece na tela do celular. | Criar `CumulativePdfReportGenerator.kt` (PDF A4 nativo com gráfico vetorial de 2 linhas, KPIs, tabela da série histórica e `DISCLAIMER_TEXT`) + botões **WhatsApp PDF** e **Salvar PDF** na aba 2. |
| **BOSS-03** | Caixa `Filtro: Fazenda, Quadra, Colheita (Mês/Ano)` antecedendo os relatórios + gráfico com eixo Y `"Produção (@)"` e datas `dd/MM/yyyy` inclinadas. | Aba Cumulativa não possui filtro de período/safra (exibe apenas todo o histórico eterno) e usa datas curtas `dd/MM` sem título no eixo Y. | Adicionar filtro de período no Cumulativo (`Todo o Histórico`, `Ano <YYYY>` ou `Mês/Ano`) + aprimorar `EstimatedVsRealChart` com rótulo `"Produção (@)"`, legenda `"ProdEstimada (@)"` / `"ProdReal (@)"` e datas legíveis. |
| **BOSS-04** | Quadro **Estimar** destaca `Produção = ((NFT/NPF)*NPT)/FA` + `Disclaimer` explícito nos relatórios. | O seletor `NFS` vs `NFT` não mostra a fórmula matemática ativa na tela e o `Disclaimer` aparece apenas no PDF exportado, não no rodapé da tela. | Exibir legenda dinâmica de 1 linha abaixo do seletor mostrando a fórmula ativa (`((NFT/NPF)×NPT)/FA` vs `((NFS/NPF)×NPT)/FA`) e exibir o `DISCLAIMER_TEXT` discreto no rodapé das duas abas na UI. |
| **BOSS-05** | Pergunta em vermelho no fluxograma: **`"Produção Real??? Como/quando Adicionar?"`** | O recurso já existe em `PlotHistoryScreen`, mas quem abre o app pela primeira vez não vê explicação imediata de *onde* e *quando* lançar. | Adicionar callout didático em `CumulativePlaceholderContent` e `PlotHistoryScreen` explicando o lançamento pós-colheita e destacar o botão da quadra como `Histórico / Real (@)`. |
| **BOSS-06** | Exemplo concreto no documento: **Fazenda `Sempre Viva`** (`Topo do Morro` 3ha/2500/FA 300 -> `300@`, `100@/ha`, `5%`; `Jatium` 5ha/4000 -> `400@`, `80@/ha`, `0%`; `Peroto do Rio` 1ha/600 -> `80@`, `80@/ha`, `8%` + curva de 6 meses). | Demonstrar o app em reunião exige cadastrar tudo na mão ou tirar 20 fotos por quadra na hora. | Criar `DemoSeedManager.kt` acionável em 1 toque na tela de Backup (`"Carregar Demonstração: Fazenda Sempre Viva"`), populando os exatos números da tabela e do gráfico de 6 meses do chefe! |

---

### 1.2. Defeitos de UI, UX e Navegação Auditados via ADB no Hardware (`QA-01` a `QA-13`)

1. **`QA-01` (P0 — Navegação):** Tocar na aba `Fazendas` após `Amostrar -> Resumo -> Abrir Relatórios` restaura `SamplingSummaryScreen` / `SamplingSetupScreen` por causa de `saveState/restoreState` sobre `FARMS_LIST` (`CacaoNavGraph.kt:76-84`).
2. **`QA-02` (P0 — Seletor de Período):** Em `PeriodPickerDialog.kt:72-89`, o padding horizontal padrão (`24.dp` × 2) do `OutlinedButton` espreme os meses para `Ja`, `Fe`, `M`, `A`, `M`, `Ju`, `Ju`, `A`, `S`, `O`, `N`, `D`, e `"Ano Todo"` fica sempre verde.
3. **`QA-03` (P0 — Fazenda Errada nos Relatórios):** `"Abrir Relatórios da Fazenda"` (`SamplingSummaryScreen.kt:181`) não passa `summary.farmId` nem `summary.dataMillis`, abrindo a 1ª fazenda em ordem alfabética.
4. **`QA-04` (P1 — Risco de Perda de Rascunho):** `DefaultCacaoRepository.startNewSamplingSession` cancela e apaga fotos de sessões `IN_PROGRESS`, enquanto `SamplingSetupScreen.kt:331` mantém o botão verde `"Iniciar Amostragem"` habilitado sem confirmação quando já existe rascunho naquela roça.
5. **`QA-05` (P1 — Alvos de Toque `16dp/30dp`):** Botões `-` e `+` em `DiseaseMiniCounter` (`PlantPhotoReviewScreen.kt:382-392`) medem `30.dp` (`16.dp` de ícone), abaixo do mínimo de `48.dp`.
6. **`QA-06` (P1 — Larguras da Tabela e Chips):** Coluna `Roça` com `156.dp` empurra `Prod. (@)` (`320,0`) para fora da borda direita sem indicador visual de scroll horizontal, e chip `"Nome A–Z"` corta na direita (`TabularReportContent.kt:373-489`).
7. **`QA-07` (P1 — Truncamento de KPIs):** `"Perda Ponderada"` corta a 2ª palavra sem `Ellipsis` (`KpiStatCard` em `CacauLensComponents.kt:160`), e `"Sem real"` fica desproporcional em `CumulativePlaceholderContent.kt:238`.
8. **`QA-08` (P1 — Simetria no `PlotFormDialog`):** `"NPT (Plantas) *"` quebra em 2 linhas quando vazio deixando o campo mais alto que `"Área (ha) *"`; `"Meta Fotos (NPF) *"` fica em 2 linhas mesmo focado; falta padding inferior no `verticalScroll` (`PlotFormDialog.kt:71,104,169,219`).
9. **`QA-09` (P1 — Acessibilidade da `NavigationBar`):** `Modifier.height(74.dp)` em `CacaoNavGraph.kt:89` zera `bounds=[0,0][0,0]` dos 3 itens na árvore de acessibilidade.
10. **`QA-10` (P2 — Retângulo Fantasma nos Cards Coloridos):** `SolarCard` com `defaultElevation = 1.dp` + `containerColor.copy(alpha < 1f)` projeta sombra retangular visível por trás do fundo translúcido (`SamplingSetupScreen`, `SamplingSummaryScreen`, `TabularReportContent`).
11. **`QA-11` (P2 — Desalinhamento `Período × Quadras`):** `OutlinedButton(54.dp)` desalinhado em relação ao `OutlinedTextField` com label flutuante (`TabularReportContent.kt:178-204`).
12. **`QA-12` (P2 — Tipografia, Milhar, Jargão e Insets):** Viúvas `"Gerenciar\n→"` e `"400,0\n@"`, `"Área Total: cadastrados"`, `10000 pés` sem ponto de milhar, jargão `WAL / PRAGMA` em `BackupRestoreScreen.kt`, `"NPF/NPF"` no estado vazio e falta de `navigationBarsPadding()`/`imePadding()` nos BottomSheets.
13. **`QA-13` (P2 — Câmera):** Obturador deslocado à direita por dividir um `Row` com `Galeria`, e modal `"Sair da Amostragem?"` sem botão explícito `"Continuar fotografando"` (`SamplingCameraScreen.kt:329-416`).

---

## 2. Pontos Críticos de Atenção (Armadilhas para o Neon / Orchestrator)

> [!CAUTION]
> **REGRAS INEGOCIÁVEIS DE EXECUÇÃO.** Qualquer desvio nestes 7 pontos quebrará suítes existentes ou a fidelidade ao documento da chefia.

1. **ARMADILHA #1 — MANTER `"Npla"` TANTO NO PDF QUANTO NA TABELA DA UI:**
   - `"Npla"` faz parte literal da tabela especificada pelo chefe E é validado em `PdfPaginationMathTest.kt` (linha 234: `listOf("Roça", "Área (ha)", "Npla", "Prod. (@)", "Produt. (@/ha)", "Perdas (%)")`). **Não renomeie `"Npla"`!** Apenas reduza a largura exagerada da 1ª coluna (`Roça` de `156.dp` para `118.dp`) na UI para caber `"Roça"`, `"Área (ha)"`, `"Npla"` e `"Prod. (@)"` inteiros na tela inicial.
2. **ARMADILHA #2 — NÃO MUDAR O DEFAULT `ProductionBasis.NET_HEALTHY_NFS` EM `ReportFilterState`:**
   - Em `ViewModelsConformityTest.kt` (linha 313), o teste instancia `FarmReportViewModel` sem chamar `selectBasis` e valida `assertEquals(192.0, ...)` (que corresponde a `NET_HEALTHY_NFS`, enquanto `240.0` de `GROSS_TOTAL_NFT` é testado na linha 343).
   - **REGRA:** Mantenha `basis: ProductionBasis = ProductionBasis.NET_HEALTHY_NFS` como default em `ReportFilterState`! Na UI, deixe o `SegmentedButton` claro (`Líquida Sadia (NFS)` | `Potencial Bruto (NFT)`) exibindo abaixo dele a fórmula matemática ativa.
3. **ARMADILHA #3 — PARÂMETROS NOVOS EM VIEWMODELS DEVEM TER VALOR DEFAULT `= null`:**
   - `ViewModelsConformityTest.kt` (linha 305) instancia diretamente `FarmReportViewModel(repository, PdfReportGenerator(context), CsvReportExporter(context))`.
   - Portanto, ao injetar `CumulativePdfReportGenerator` em `FarmReportViewModel`, declare no construtor primário:
     `private val cumulativePdfReportGenerator: CumulativePdfReportGenerator? = null`
     (passando a instância real dentro de `FarmReportViewModel.Factory` e `CacaoNavGraph`). O mesmo vale para `DemoSeedManager? = null` em `BackupViewModel`! Assim **nenhum teste existente quebra compilação**.
4. **ARMADILHA #4 — INTOCABILIDADE DO SCHEMA ROOM (`VERSION = 1`), CÁLCULOS E TFLITE:**
   - Proibido alterar `CacaoDatabase.kt`, entidades (`FarmEntity`, `PlotEntity`, `EvaluationEntity`, `PlantSampleEntity`), DAOs, `DefaultCacaoRepository.kt`, `ForecastCalculator.kt`, `FarmReportConsolidator.kt`, `CumulativeStatsCalculator.kt` ou `ml/*`.
   - `PdfReportGenerator.DISCLAIMER_TEXT` deve permanecer **caractere por caractere**: `"Disclaimer: Aplicativo em desenvolvimento. Não nos responsabilizamos pelos resultados."`
5. **ARMADILHA #5 — PROIBIDO `Color.copy(alpha < 1f)` EM `Card` / `SolarCard`:**
   - Use apenas cores hexadecimais 100% opacas (`0xFF......`) em `Color.kt` e `defaultElevation = 0.dp` quando `containerColor != CacaoSurfaceCard` para eliminar o retângulo fantasma de sombra.
6. **ARMADILHA #6 — MILHAR (`10.000`) APENAS EM `Text` DE LEITURA:**
   - Nunca aplique `%,d` ao estado `nptText` de `OutlinedTextField` editável em `PlotFormDialog.kt`.
7. **ARMADILHA #7 — MATEMÁTICA EXATA DO `DemoSeedManager` (`Sempre Viva`):**
   - Use exatamente os inteiros de frutos homologados na `QA-TASK-07` (`Topo do Morro`: `NFS=684, NFD=36`; `Jatium`: `NFS=500, NFD=0`; `Peroto do Rio`: `NFS=736, NFD=64`) passando por `ForecastCalculator.calculate(...)`, o que reproduz com precisão matemática absoluta `300,0 @ (100,0 @/ha, 5,0%)`, `400,0 @ (80,0 @/ha, 0,0%)` e `80,0 @ (80,0 @/ha, 8,0%)` na visão `Potencial Bruto (NFT)`!

---

## 3. Tarefas Atômicas para Orquestração (`QA-TASK-01` → `QA-TASK-08`)

> **Instrução ao Neon / Orchestrator:** Executar sequencialmente `QA-TASK-01` até `QA-TASK-08`, compilando a cada etapa e rodando a bateria completa de testes + `assembleRelease` ao final.

---

### `QA-TASK-01` — Navegação Limpa na `BottomBar`, Vínculo Automático `farmId`/Mês nos Relatórios & Acessibilidade
- **Resolve:** `QA-01`, `QA-03`, `QA-09`
- **Arquivos alvo:**
  - `app/src/main/java/com/cacaoforecast/ai/ui/reports/FarmReportViewModel.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/sampling/SamplingSummaryScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/navigation/CacaoNavGraph.kt`
- **Mudanças exatas:**
  1. Em `FarmReportViewModel.kt`, adicionar método aditivo `openFarmReportForEvaluation(farmId: Long, evaluationMillis: Long)`:
     - Atualiza `_filters`: define `selectedFarmId = farmId`, `selectedPlotId = null`, e se `evaluationMillis !in startMillis..endMillis`, recalcula `monthRange(y, m)` e `formatMonthLabel(y, m)` a partir de `evaluationMillis` para garantir que a avaliação recém-concluída apareça imediatamente.
  2. Em `SamplingSummaryScreen.kt`, alterar `onGoToReports: () -> Unit` para `onGoToReports: (farmId: Long, evaluationDateMillis: Long) -> Unit` e chamar `onGoToReports(summary.farmId, summary.dataMillis)`.
  3. Em `CacaoNavGraph.kt`:
     - Remover `modifier = Modifier.height(74.dp)` de `NavigationBar` (linha 89), restaurando os `windowInsets` e `bounds` reais de acessibilidade dos 3 itens.
     - Reescrever `navigateTopTab(route: String)`: quando `route == Routes.FARMS_LIST`, executar `if (!navController.popBackStack(Routes.FARMS_LIST, inclusive = false)) { navController.navigate(Routes.FARMS_LIST) { popUpTo(navController.graph.findStartDestination().id) { inclusive = false }; launchSingleTop = true } }`. Para as demais abas, usar `popUpTo(Routes.FARMS_LIST) { inclusive = false }; launchSingleTop = true` (sem `saveState/restoreState` que prendia `SAMPLING_SUMMARY` na aba `Fazendas`).
     - Em `FarmPlotsScreen.onStartSampling`, navegar com `popUpTo(Routes.FARMS_LIST) { inclusive = false }; launchSingleTop = true`.
     - Em `SamplingSummaryScreen.onGoToReports`, chamar `reportViewModel.openFarmReportForEvaluation(farmId, evalMillis)` antes de `navigateTopTab(Routes.REPORTS_HUB)`.
- **Critério de Aceite:** Abrir relatórios pelo resumo já abre na fazenda avaliada; clicar na aba `Fazendas` de qualquer tela volta direto para `FarmsListScreen`.

---

### `QA-TASK-02` — `PeriodPickerDialog` Legível (`Jan`..`Dez`), Nomenclatura Oficial do Chefe nas Abas & `BrandIdentity`
- **Resolve:** `BOSS-01`, `QA-02`, `QA-11`
- **Arquivos alvo:**
  - `app/src/main/java/com/cacaoforecast/ai/ui/theme/BrandIdentity.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/reports/PeriodPickerDialog.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/reports/ReportsHubScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/reports/TabularReportContent.kt`
- **Mudanças exatas:**
  1. **`BrandIdentity.kt`:**
     - Atualizar `PDF_TITLE = "CacauLens – Estimação de Safras e Perdas"` e adicionar `const val PDF_CUMULATIVE_TITLE = "CacauLens – Produção (Relatório Cumulativo)"` *(100% compatível com `PdfPaginationMathTest` e idêntico aos títulos do documento do chefe)*.
  2. **`ReportsHubScreen.kt`:**
     - Renomear as 2 abas exatamente para **`Relatório Parcial`** e **`Relatório Cumulativo`** (`maxLines = 1`, `overflow = TextOverflow.Ellipsis`), adotando a terminologia oficial da chefia e eliminando a quebra de 2 linhas.
  3. **`PeriodPickerDialog.kt`:**
     - Adicionar parâmetro `currentPeriodLabel: String = ""` + `allowAllHistory: Boolean = false` + callback opcional `onSelectAllHistory: (() -> Unit)? = null` (para reutilizar o mesmo diálogo também na aba Cumulativa!).
     - Inicializar `year` extraindo os 4 dígitos finais de `currentPeriodLabel` se presentes.
     - Nos 12 botões de mês: usar `contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)`, `height(44.dp)`, `maxLines = 1`, `softWrap = false`. Renderizar `Button` preenchido **somente** quando `currentPeriodLabel == formatMonthLabel(year, mIdx)` e `OutlinedButton` nos demais.
     - Nos botões `"Ano Todo"` e `"Mês Atual"` (e `"Todo o Histórico"` quando `allowAllHistory == true`): destacar com `Button` preenchido **apenas** a opção que coincidir com `currentPeriodLabel`.
  4. **`TabularReportContent.kt`:**
     - Substituir o `OutlinedButton(54.dp)` de período por um `OutlinedTextField` `readOnly = true` com `label = { Text("Colheita (Mês/Ano)", maxLines = 1) }`, `leadingIcon = CalendarMonth` e overlay clicável abrindo `showPeriodDialog = true`, ficando 100% alinhado em altura e borda ao campo `Quadras` ao lado.
- **Critério de Aceite:** `Jan`..`Dez` aparecem inteiros sem cortar letras; apenas o período real ativo fica verde; abas exibem `Relatório Parcial` e `Relatório Cumulativo` em 1 linha.

---

### `QA-TASK-03` — Superfícies Opacas em `SolarCard` & Trava de Segurança contra Perda de Rascunho
- **Resolve:** `QA-04`, `QA-10`
- **Arquivos alvo:**
  - `app/src/main/java/com/cacaoforecast/ai/ui/theme/Color.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/components/CacauLensComponents.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/sampling/SamplingSetupScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/sampling/SamplingSummaryScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/reports/TabularReportContent.kt`
- **Mudanças exatas:**
  1. Em `Color.kt`, criar tokens **100% opacos**: `CacaoSelectedPlotBg = Color(0xFFE1F1E6)`, `MoniliaseQuarantineBg = Color(0xFFF9E8FC)`, `MoniliaseQuarantineBorder = Color(0xFFC56CE0)`, `MoniliaseQuarantineText = Color(0xFF7B0099)`, `PodridaoMiniBg = Color(0xFFFFF0E3)`, `PodridaoMiniText = Color(0xFFB54700)`, `VassouraMiniBg = Color(0xFFFDE8E8)`, `VassouraMiniText = Color(0xFFB71C1C)`.
  2. Em `SolarCard` (`CacauLensComponents.kt`), usar `defaultElevation = if (containerColor == CacaoSurfaceCard) 1.dp else 0.dp` e substituir todos os `.copy(alpha = ...)` de `containerColor` nos cards de `SamplingSetupScreen`, `SamplingSummaryScreen` e `TabularReportContent` pelos novos tokens opacos.
  3. Em `SamplingSetupScreen.kt`, quando `uiState.pendingResumeDraft?.plotId == plot.id`, transformar o botão inferior em `OutlinedButton("Recomeçar do Zero (${plot.npf} plantas)")` que exige confirmação num `AlertDialog("Descartar rascunho em andamento?")` antes de chamar `viewModel.startNewSession(plot)`, mantendo `"Retomar"` como único CTA verde primário.
- **Critério de Aceite:** Cards coloridos lisos sem retângulo interno; impossível perder um rascunho salvo em campo por toque acidental.

---

### `QA-TASK-04` — Alvos Táteis `>= 48dp` nas 3 Doenças & Centralização Ergônomica na Câmera
- **Resolve:** `QA-05`, `QA-13`
- **Arquivos alvo:**
  - `app/src/main/java/com/cacaoforecast/ai/ui/sampling/PlantPhotoReviewScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/sampling/SamplingCameraScreen.kt`
- **Mudanças exatas:**
  1. Em `PlantPhotoReviewScreen.kt` (`DiseaseMiniCounter`): remover `Modifier.size(30.dp)` e `16.dp`; usar cards opacos (`PodridaoMiniBg`, `MoniliaseQuarantineBg`, `VassouraMiniBg`) com `elevation = 0.dp`, borda `1.dp` e botões `-`/`+` com `Modifier.size(44.dp).minimumInteractiveComponentSize()` + ícone `20.dp` distribuídos com `Arrangement.SpaceBetween`.
  2. Em `SamplingCameraScreen.kt`:
     - No rodapé, usar `Box(Modifier.fillMaxWidth())` com `Galeria` em `Alignment.CenterStart` e o botão circular de captura (`76.dp`) exatamente em **`Alignment.Center`**.
     - No `AlertDialog("Sair da Amostragem?")`, exibir 3 ações claras empilhadas: `"Continuar fotografando"` (primário seguro), `"Salvar rascunho e sair"` (secundário) e `"Cancelar amostragem"` (erro/destrutivo).
- **Critério de Aceite:** Steppers de doenças fáceis de tocar com o polegar (`>= 44-48dp`); obturador centralizado; retorno seguro à câmera em 1 toque.

---

### `QA-TASK-05` — Tabela do Relatório Parcial Fiel ao Chefe (`"Npla"`), Fórmula Explícita (`NFT` vs `NFS`), `Disclaimer` na UI & `PlotFormDialog`
- **Resolve:** `BOSS-04`, `QA-06`, `QA-07`, `QA-08`
- **Arquivos alvo:**
  - `app/src/main/java/com/cacaoforecast/ai/ui/components/CacauLensComponents.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/reports/TabularReportContent.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/farms/PlotFormDialog.kt`
- **Mudanças exatas:**
  1. Em `KpiStatCard` (`CacauLensComponents.kt`), adicionar `overflow = TextOverflow.Ellipsis` e `softWrap = false` em `label`, `value` e `subtitle`.
  2. Em `TabularReportContent.kt`:
     - Abaixo do `SingleChoiceSegmentedButtonRow` (`Líquida Sadia (NFS)` | `Potencial Bruto (NFT)`), exibir texto explicativo de 1 linha:
       - Se `NFT`: `"Fórmula ativa (Potencial Total): Produção = ((NFT / NPF) × NPT) / FA"`
       - Se `NFS`: `"Fórmula ativa (Colheita Sadia): Produção = ((NFS / NPF) × NPT) / FA"`
     - Renomear o KPI `"Perda Ponderada"` para **`"Perda Média"`** (evita truncamento).
     - Nos `FilterChip`s de ordenação, usar `"Maior Perda (%)"`, `"Maior Safra (@)"` e `"Nome A–Z"` para caberem sem cortar `"Nome A–Z"`.
     - **Manter `"Npla"` como cabeçalho da 3ª coluna** (fiel ao slide do chefe!) e rebalancear larguras das colunas para caber as **4 primeiras colunas inteiras na primeira dobra**: `Roça: 118.dp`, `Área (ha): 66.dp`, `Npla: 64.dp`, `Prod. (@): 88.dp`, `Produt. (@/ha): 94.dp`, `Perdas (%): 132.dp`. Adicionar faixa de gradiente horizontal (`18.dp`) na borda direita da tabela enquanto `hScroll.canScrollForward == true`.
     - Exibir `PdfReportGenerator.DISCLAIMER_TEXT` em estilo `labelSmall` itálico discreto no rodapé da tela após os botões de exportação.
  3. Em `PlotFormDialog.kt`:
     - Ajustar rótulos de meia largura para 1 linha (`maxLines = 1, softWrap = false`): `"Área (ha) *"` | `"NPT (Pés) *"` e `"FA (Frutos/@) *"` | `"Meta (NPF) *"`.
     - Adicionar `.padding(bottom = 10.dp)` no `Column` rolável e texto auxiliar conciso sem corte na borda inferior.
- **Critério de Aceite:** Tabela mostra `"Npla"` e `Prod. (@)` sem cortar dígitos ao meio; fórmula do chefe fica explícita na tela; `PlotFormDialog` simétrico.

---

### `QA-TASK-06` — `Relatório Cumulativo` Completo: Filtro de Período/Safra, Gráfico no Padrão do Chefe & Exportação PDF / WhatsApp
- **Resolve:** `BOSS-02`, `BOSS-03`, `QA-07`
- **Arquivos alvo:**
  - `app/src/main/java/com/cacaoforecast/ai/pdf/CumulativePdfReportGenerator.kt` *(novo)*
  - `app/src/main/java/com/cacaoforecast/ai/di/AppContainer.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/reports/FarmReportViewModel.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/navigation/CacaoNavGraph.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/reports/ReportsHubScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/reports/CumulativePlaceholderContent.kt`
- **Mudanças exatas:**
  1. **Filtro de Período no Relatório Cumulativo (`FarmReportViewModel.kt`):**
     - Adicionar em `ReportFilterState`:
       - `val cumulativeStartMillis: Long? = null` (`null` = Todo o Histórico)
       - `val cumulativeEndMillis: Long? = null`
       - `val cumulativePeriodLabel: String = "Todo o Histórico"`
     - Adicionar método `selectCumulativePeriod(startMillis: Long?, endMillis: Long?, label: String)`.
     - Na montagem de `cumulativeReport`, se `cumulativeStartMillis != null && cumulativeEndMillis != null`, filtrar `evals.filter { it.dataMillis in cumulativeStartMillis..cumulativeEndMillis }` antes de chamar `CumulativeStatsCalculator.build(...)`.
  2. **Novo Gerador PDF Cumulativo (`CumulativePdfReportGenerator.kt`):**
     - Criar classe em `com.cacaoforecast.ai.pdf` usando `android.graphics.pdf.PdfDocument` nativo (`595 x 842`, A4, salvando em `File(context.cacheDir, "reports")` com prefixo `${BrandIdentity.FILE_PREFIX}_cumulativo_...pdf`).
     - Renderizar na página:
       - Título `BrandIdentity.PDF_CUMULATIVE_TITLE` (`"CacauLens – Produção (Relatório Cumulativo)"`);
       - Subtítulo `Fazenda: <farm.nome>   |   Quadra: <plot.nome>   |   Período: <periodLabel>`;
       - Critério (`Safra Líquida Sadia — NFS` ou `Potencial Bruto — NFT`) + resumo de KPIs (`Total Estimado`, `Total Real`, `Erro Médio Relativo`);
       - **Gráfico vetorial de linhas idêntico ao slide do chefe:** caixa com grade horizontal, título lateral/superior `"Produção (@)"`, legenda `"— ProdEstimada (@)"` (verde/azul) e `"— ProdReal (@)"` (âmbar/vermelho), plotagem dos pontos e segmentos de reta das duas séries e datas `dd/MM/yyyy` rotacionadas `-35°` no eixo X;
       - Tabela compacta abaixo do gráfico listando as avaliações da série (`Data`, `ProdEstimada (@)`, `ProdReal (@)`, `Perdas (%)`);
       - Rodapé obrigatório com `PdfReportGenerator.DISCLAIMER_TEXT`.
     - Expor em `AppContainer.kt` (`val cumulativePdfReportGenerator by lazy { CumulativePdfReportGenerator(appContext) }`) e injetar em `FarmReportViewModel` com **valor default `= null`** no construtor primário (`private val cumulativePdfReportGenerator: CumulativePdfReportGenerator? = null`) + método `generateCumulativePdf(onGenerated: (File, String) -> Unit)`.
  3. **Melhorias na Tela `CumulativePlaceholderContent.kt`:**
     - Adicionar seletor de `Colheita / Período` (abrindo `PeriodPickerDialog` com `allowAllHistory = true` para escolher `"Todo o Histórico"`, `"Ano 2026"` ou mês específico).
     - Substituir `"Sem real"` no KPI `"Erro Médio"` por **`"—"`**.
     - Atualizar `EstimatedVsRealChart` e legendas para `"ProdEstimada (@)"` e `"ProdReal (@)"`, adicionar indicação `"Produção (@)"` no eixo Y e usar `dd/MM/yy` (ou `dd/MM/yyyy` rotacionado `-30°` no `nativeCanvas`) no eixo X.
     - Adicionar barra de exportação ao final da tela: **`Enviar Gráfico PDF no WhatsApp`** (primário `54.dp`) + **`Salvar PDF Cumulativo`** (`OutlinedButton`), seguidos pelo `PdfReportGenerator.DISCLAIMER_TEXT` discreto.
- **Critério de Aceite:** Aba `Relatório Cumulativo` filtra por Ano/Histórico, plota o gráfico no padrão do chefe e envia o PDF do gráfico direto para o WhatsApp!

---

### `QA-TASK-07` — Modo Demonstração Executiva (`Fazenda Sempre Viva` do Chefe em 1 Toque) & Guia de `"Produção Real"`
- **Resolve:** `BOSS-05`, `BOSS-06`
- **Arquivos alvo:**
  - `app/src/main/java/com/cacaoforecast/ai/data/demo/DemoSeedManager.kt` *(novo)*
  - `app/src/main/java/com/cacaoforecast/ai/di/AppContainer.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/backup/BackupViewModel.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/backup/BackupRestoreScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/navigation/CacaoNavGraph.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/farms/FarmPlotsScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/history/PlotHistoryScreen.kt`
- **Mudanças exatas:**
  1. **Criar `DemoSeedManager.kt` (`class DemoSeedManager(private val databaseProvider: () -> CacaoDatabase)`):**
     - Método `suspend fun seedSempreVivaDemo(referenceNowMillis: Long = System.currentTimeMillis()): Result<Unit>` executando em `db.withTransaction`:
       - Se já existir fazenda ativa chamada `"Sempre Viva"`, arquiva ou remove apenas a `"Sempre Viva"` anterior para evitar duplicatas (preservando 100% de quaisquer outras fazendas do usuário!).
       - Cria a fazenda **`Sempre Viva`** (`municipioUf = "Ilhéus/BA"`, `proprietario = "Demonstração Executiva"`).
       - Cria as **3 quadras exatas do documento do chefe** e insere avaliações `COMPLETED` usando `ForecastCalculator.calculate(...)`:
         1. **`Topo do Morro`** (`areaHa = 3.0`, `npt = 2500`, `fa = 300.0`, `npf = 20`):
            - **5 avaliações mensais anteriores** (nos 5 meses anteriores a `referenceNowMillis`, dia 01 às 10:00) formando a curva histórica de manejo com `producaoReal` preenchida (ex.: Mês-5: `NFS=240, NFD=12` [`~105@` est / `95@` real]; Mês-4: `NFS=432, NFD=24` [`190@` est / `180@` real]; Mês-3: `NFS=114, NFD=6` [`50@` est / `48@` real]; Mês-2: `NFS=126, NFD=6` [`55@` est / `52@` real]; Mês-1: `NFS=132, NFD=8` [`58,3@` est / `56@` real]).
            - **1 avaliação no mês atual (`referenceNowMillis`)** com `NFS = 684`, `NFD = 36` (`Podridão = 28`, `Vassoura = 8`, `Monilíase = 0`), gerando matematicamente **`Produção Bruta (NFT) = 300,0 @`**, **`Produtividade Bruta = 100,0 @/ha`**, **`Perdas = 5,0%`** (e `Líquida = 285,0 @`, `producaoReal = 290,0 @`)!
         2. **`Jatium`** (`areaHa = 5.0`, `npt = 4000`, `fa = 250.0`, `npf = 20`):
            - Avaliação no mês atual com `NFS = 500`, `NFD = 0` -> **`Produção = 400,0 @`**, **`Produtividade = 80,0 @/ha`**, **`Perdas = 0,0%`**!
         3. **`Peroto do Rio`** (`areaHa = 1.0`, `npt = 600`, `fa = 300.0`, `npf = 20`):
            - Avaliação no mês atual com `NFS = 736`, `NFD = 64` (`Podridão = 50`, `Vassoura = 14`, `Monilíase = 0`) -> **`Produção Bruta (NFT) = 80,0 @`**, **`Produtividade Bruta = 80,0 @/ha`**, **`Perdas = 8,0%`**!
  2. **Expor na UI (`BackupViewModel` + `BackupRestoreScreen`):**
     - Adicionar `private val demoSeedManager: DemoSeedManager? = null` (com default `null`) em `BackupViewModel` + método `loadOfficialDemo()`.
     - Em `BackupRestoreScreen.kt`, adicionar seção `SolarCard` no final: **"Modo Apresentação / Demonstração"** com botão `OutlinedButton("Carregar Fazenda 'Sempre Viva' (Exemplo Oficial)")` e confirmação rápida.
  3. **Callout Didático de `"Produção Real"` (`BOSS-05`):**
     - Em `FarmPlotsScreen.kt` (linha 248), alterar o texto do botão secundário de `"Histórico"` para **`"Histórico / Real"`**.
     - Em `PlotHistoryScreen.kt`, exibir um card informativo compacto no topo da lista: *"Pós-colheita: quando fechar o peso das amêndoas secas colhidas nesta quadra, toque em '+ Informar Produção Real (@)' na avaliação correspondente para alimentar o Relatório Cumulativo."*
- **Critério de Aceite:** Em 1 toque, `Sempre Viva` carrega no celular exibindo exatamente `300 @ / 100 @/ha / 5%`, `400 @ / 80 @/ha / 0%` e `80 @ / 80 @/ha / 8%` no Relatório Parcial (modo `NFT`) e a curva de 6 meses no Relatório Cumulativo!

---

### `QA-TASK-08` — Polimento Tipográfico (`\u00A0`), Milhar/Concordância `pt-BR`, Linguagem Simples no Backup, Insets & Suíte de Testes
- **Resolve:** `QA-12` + Cobertura automatizada de todas as novas features.
- **Arquivos alvo:**
  - `app/src/main/java/com/cacaoforecast/ai/ui/farms/FarmsListScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/farms/FarmPlotsScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/sampling/SamplingSetupScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/history/PlotHistoryScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/history/RealProductionBottomSheet.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/backup/BackupRestoreScreen.kt`
  - `app/src/main/java/com/cacaoforecast/ai/ui/components/CacauLensComponents.kt`
  - `app/src/test/java/com/cacaoforecast/ai/ui/ViewModelsConformityTest.kt`
  - `app/src/test/java/com/cacaoforecast/ai/pdf/CumulativePdfReportGeneratorTest.kt` *(novo)*
  - `app/src/test/java/com/cacaoforecast/ai/data/demo/DemoSeedManagerTest.kt` *(novo)*
- **Mudanças exatas:**
  1. **Espaços não-separáveis (`\u00A0`) contra viúvas:** `"Gerenciar\u00A0→"`, `"dividir áreas\u00A0→"` (`FarmsListScreen.kt`), `"%.1f\u00A0@"` e `"%.1f\u00A0@/ha"` (`PlotHistoryScreen.kt`, `RealProductionBottomSheet.kt`).
  2. **Concordância e milhar:** `"Área Total"` -> subtítulo `"cadastrada"`, e `String.format(ptBr, "%,d", npt)` em `FarmPlotsScreen.kt`, `SamplingSetupScreen.kt` e `TabularReportContent.kt`.
  3. **Linguagem acessível no Backup (`BackupRestoreScreen.kt`):** usar `SolarCard`, título `"Backup & Restauração (MicroSD / Nuvem)"` (espelhando os termos `MicroSD` e `Nuvens` do fluxograma do chefe!) e remover jargões `WAL` / `PRAGMA`. No estado vazio de `TabularReportContent.kt`, substituir `"NPF/NPF plantas"` por `"todas as árvores planejadas da quadra"`.
  4. **Insets em `ModalBottomSheet`s:** adicionar `.navigationBarsPadding().imePadding()` em `GlossaryHelpSheet` e `RealProductionBottomSheet`.
  5. **Novos Testes Unitários Obrigatórios:**
     - Em `ViewModelsConformityTest.kt`: testar `openFarmReportForEvaluation` e `selectCumulativePeriod`.
     - Em `CumulativePdfReportGeneratorTest.kt`: testar geração do PDF Cumulativo com Robolectric validando arquivo em `cacheDir/reports`, cabeçalho `"Produção"`, `"Sempre Viva"`, `"Topo do Morro"` e `PdfReportGenerator.DISCLAIMER_TEXT`.
     - Em `DemoSeedManagerTest.kt`: testar `seedSempreVivaDemo` em Room in-memory validando que as 3 quadras geram exatamente `300.0 @`, `400.0 @` e `80.0 @` (base `NFT`), 6 pontos cumulativos em `Topo do Morro` e idempotência ao rodar 2 vezes seguidas.
  6. **Validação Global Release:**
     ```powershell
     $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
     .\gradlew.bat testDebugUnitTest assembleDebug assembleRelease --console=plain
     ```
     - **Critério Global de Encerramento:** `BUILD SUCCESSFUL`, 100% dos testes antigos + novos verdes (`0` falhas) e `app-release.apk` assinado gerado sem alertas críticos de R8/Lint.
