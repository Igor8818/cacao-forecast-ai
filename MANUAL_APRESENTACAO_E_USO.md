# Manual de Uso & Guia de Apresentação Executiva (`CacauLens` / `CacaoForecastAI`)

Este documento reúne duas partes práticas:
1. **Roteiro de Apresentação e Demonstração para a Chefia** *(como demonstrar o app funcionando ao vivo em uma sala de reunião e como responder às perguntas técnicas e agronômicas)*;
2. **Manual de Operação do Aplicativo** *(passo a passo completo para produtores, técnicos de campo e pesquisadores)*.

---

## Parte 1 — Como Demonstrar o App Funcionando para o Chefe

### 1.1. Preparação Rápida Antes da Reunião (3 minutos)

Você **não precisa estar no meio da roça** para fazer uma demonstração impressionante e real:

1. **Espelhe a tela do celular no seu notebook / TV da sala (opcional, altamente recomendado):**
   - Com o cabo USB conectado, rode a ferramenta gratuita `scrcpy` no PC (ou espelhamento sem fio do Windows/Xiaomi). Assim seu chefe acompanha cada toque em tela grande enquanto você segura o celular.
2. **Salve 2 ou 3 fotos reais de cacaueiros na Galeria do celular:**
   - Escolha fotos de tronco/copa com cabaças sadias e fotos com sintomas visíveis (Podridão Parda, Vassoura de Bruxa ou Monilíase). O botão **`Galeria`** na câmera do app passa a imagem real exatamente pelos **mesmos 2 modelos de IA embutidos** (`detector.tflite` + `detector_doenca.tflite`).
3. **Carregue a Fazenda `"Sempre Viva"` (após executar o `PLAN_QA_POLISH.md`) ou deixe uma quadra de teste com `Meta (NPF) = 2 fotos`:**
   - Para mostrar os relatórios prontos na hora, use a fazenda `Sempre Viva` (com os mesmos nomes e números do slide dele: `Topo do Morro`, `Jatium`, `Peroto do Rio`).
   - Para demonstrar a câmera e a IA ao vivo sem precisar tirar 20 fotos na frente dele, crie uma quadra rápida chamada `Quadra Demo Ao Vivo` com **`Meta (NPF) = 2`**.
4. **Truque de Ouro — Ligue o Modo Avião na frente dele!**
   - Antes de abrir a amostragem, ative o **Modo Avião** do celular e diga: *"Vou desligar o Wi-Fi e o 4G agora para simular que estamos no fundo da roça sem sinal nenhum."*
   - Só religue o Wi-Fi nos últimos 30 segundos para disparar o PDF direto para o **WhatsApp dele**!

---

### 1.2. Roteiro da Demonstração em 5 Atos (Duração: ~6 a 8 minutos)

#### Ato 1 — Cadastro Estruturado da Fazenda e das Quadras (1 min)
- Abra a aba **Fazendas** -> toque na fazenda **`Sempre Viva`**.
- Mostre como a fazenda é dividida em **Roças / Quadras** independentes (`Topo do Morro`, `Jatium`, `Peroto do Rio`), cada uma com seus 4 parâmetros oficiais do fluxograma:
  - **Área (`ha`)** `[= A]`
  - **Total de Plantas** `[NPT]` *(mostre o botão "Não sabe o total de plantas? Estimar pelo espaçamento" `3×3m`, `3×2,5m`, `Cabruca`)*
  - **Frutos por Arroba** `[FA]` *(mostre os atalhos de 1 toque por clone: `CCN-51: 220`, `Clonal CEPEC: 250`, `Comum: 290`)*
  - **Meta de Fotos** `[NPF]` *(ex.: `20` plantas)*

#### Ato 2 — Amostragem Offline + Os 2 Modelos de IA Rodando Ao Vivo (2 min)
- Com o **Modo Avião ligado**, toque em **`Amostrar`** na `Quadra Demo Ao Vivo (2 plantas)`.
- Mostre o visor da câmera com guia de enquadramento, controle de zoom (`1x / 2x / 3x`) e lanterna.
- Importe a 1ª foto de cacaueiro pelo botão **`Galeria`** (ou tire foto de uma imagem impressa/tela):
  - Em **~1,1 segundo**, mostre a tela de **Revisão** com as caixas detectadas pelos **2 modelos de IA embutidos** (`S` = Sadio, `PP` = Podridão Parda, `MO` = Monilíase, `VB` = Vassoura de Bruxa).
  - Toque no **ícone de olho** no topo para ocultar/mostrar as caixas (útil para o técnico conferir o fruto real por baixo da marcação) e mostre que um toque na caixa corrige a classificação ou os botões `- / +` ajustam a contagem na hora.
- **Mostre a proteção contra imprevistos:** na Planta 2, toque em Voltar -> mostre a opção **"Salvar rascunho e sair"** (*"Se a bateria do técnico acabar na árvore 14 de 20, ele não perde nada do que já caminhou"*). Retome e conclua a Planta 2.

#### Ato 3 — Respondendo à Dúvida em Vermelho do Slide (*"Produção Real??? Como/quando Adicionar?"*) (1,5 min)
- Aponte para o fluxograma dele onde está escrito em vermelho *"Produção Real??? Como/quando Adicionar?"* e diga:
  > *"Nós já desenhamos e implementamos essa etapa pensando na rotina real da fazenda: no dia da foto o fruto ainda está no pé; a Produção Real só existe semanas depois, quando o cacau daquela quadra é colhido, fermentado, seco na barcaça e pesado em arrobas."*
- Entre no **Histórico da Roça** -> toque em **`+ Informar Produção Real (@)`** na avaliação concluída -> digite o peso real colhido e salve.
- Mostre que o app preserva a estimativa original intacta e já calcula na hora a **Diferença (`@`)** e o **Erro Relativo (`%`)** para calibrar o manejo e o fator `FA` daquela quadra!

#### Ato 4 — Os 2 Relatórios Gerenciais + Disparo Real no WhatsApp Dele (2 min)
- Religar o Wi-Fi/4G e abrir a aba **Relatórios**:
  1. **Relatório Parcial** *(1 fazenda, 1 ou mais quadras)*:
     - Mostre os filtros exatamente como no diagrama dele (`Fazenda`, `Quadra: Todas ou Específica`, `Colheita: Mês/Ano`).
     - Mostre o seletor **`Potencial Bruto (NFT)`** vs. **`Líquida Sadia (NFS)`**: em `NFT` aparecem os números da fórmula literal dele; em `NFS` aparece quanto realmente sobra limpo para venda e contratação de mão de obra descontando as doenças!
     - Toque em **`Enviar Relatório PDF no WhatsApp`** -> selecione o contato do seu chefe -> ele recebe o PDF paginado no celular dele na mesma hora, com as 6 colunas oficiais (`Roça`, `Área (ha)`, `Npla`, `Prod. (@)`, `Produt. (@/ha)`, `Perdas (%)`), destaque da quadra crítica e o `Disclaimer` obrigatório no rodapé!
  2. **Relatório Cumulativo** *(1 fazenda e 1 quadra)*:
     - Selecione `Sempre Viva` -> `Topo do Morro`.
     - Mostre o gráfico comparativo **`ProdEstimada (@)` vs. `ProdReal (@)`** ao longo dos meses, comprovando a evolução do manejo fitossanitário (antes e depois da aplicação de fungicida/adubo), e exporte o PDF também!

#### Ato 5 — Backup Offline em MicroSD e Nuvens (30 seg)
- Na tela inicial, toque no ícone de **Backup** no topo direito.
- Mostre as duas saídas pedidas no diagrama:
  - **Salvar Backup no Aparelho / MicroSD (`.db`)**
  - **Compartilhar Arquivo `.db` (Google Drive / Nuvens / E-mail)**, explicando que na restauração o app verifica se o arquivo está íntegro antes de substituir os dados para nunca corromper o banco do produtor.

---

## Parte 2 — "Cola Executiva": Como Explicar as Decisões Técnicas se Ele Perguntar

| Possível Pergunta do Chefe | Resposta Segura, Curta e Técnica |
| :--- | :--- |
| **"Por que vocês colocaram `Safra Líquida (NFS)` além da fórmula `NFT` que eu mandei?"** | *"A sua fórmula com `NFT` (sadios + doentes) está 100% implementada e mede o **potencial biológico total** da quadra. Adicionamos a visão `NFS` (só sadios) como apoio complementar porque, se uma roça estiver com 25% de perda por Podridão Parda, usar `NFT` para fechar contrato antecipado de venda ou contratar diaristas faria o produtor vender cacau que não vai para o cocho. Assim ele tem os dois números na mão."* |
| **"E se o técnico avaliar a mesma roça duas vezes no mesmo mês (ex.: dia 02 e dia 25)?"** | *"Se somássemos as duas avaliações no Relatório Parcial do mês, duplicaríamos os hectares e o número de pés da fazenda. Por isso, no Relatório Parcial o sistema pega automaticamente a **avaliação concluída mais recente** de cada quadra no mês filtrado, e no Relatório Cumulativo ele plota **todas** na linha do tempo para medir o efeito do manejo."* |
| **"E se daqui a um ano o produtor replantar pés (`NPT`) ou ajustar o `FA` da quadra? Vai bagunçar os relatórios antigos?"** | *"Não. Cada avaliação salva uma 'fotografia congelada' (`snapshot`) da Área, `NPT`, `FA` e `NPF` vigentes naquele dia. O passado nunca é recalculado errado."* |
| **"As fotos das 20 árvores de cada quadra vão lotar a memória do celular do técnico?"** | *"Não. As fotos ficam numa pasta temporária privada apenas enquanto a sessão de 20 plantas está em andamento (para permitir revisão e retomada de rascunho). Assim que a avaliação é concluída, as imagens pesadas são limpas e ficam gravados apenas os números auditados no banco SQLite, ocupando poucos kilobytes por safra."* |
| **"Como funcionam os 2 modelos de IA juntos?"** | *"O 1º modelo (`detector.tflite`, YOLOv8) é especialista em encontrar todos os frutos no tronco/copa sem deixar passar nada. O 2º modelo (`detector_doenca.tflite`, YOLO11n) diagnostica cada fruto entre Sadio, Podridão Parda, Monilíase e Vassoura de Bruxa. Cruzamos os dois por sobreposição geométrica (IoU) em ~1,1 segundo sem precisar de internet, e se detectar sequer 1 fruto suspeito de **Monilíase**, o app dispara um Alerta Quarentenário imediato no painel e no PDF."* |
| **"Por que o app está com o nome `CacauLens`?"** | *"Testamos um visual de produto pronto para o produtor rural — `CacauLens` tem 9 letras, não corta na tela inicial do Android e ganhou um ícone próprio da cacauicultura. Mas deixamos o nome centralizado em uma única constante (`BrandIdentity.kt`): se o senhor preferir manter `CacaoForecastAI` oficialmente, trocamos em 10 segundos."* |

---

## Parte 3 — Manual Rápido do Usuário (Operação no Dia a Dia)

### 3.1. Glossário Rápido dos Parâmetros
- **Fazenda:** Propriedade rural onde ficam as áreas produtivas.
- **Roça / Quadra:** Divisão homogênea da fazenda (por clone, idade, relevo ou manejo).
- **Área (`ha`) `[A]`:** Tamanho da roça em hectares (ex.: `3,0`).
- **Total de Plantas `[NPT]`:** Quantidade total estimada de cacaueiros produtivos na roça (ex.: `2.500`).
- **Frutos por Arroba `[FA]`:** Quantos frutos sadios são necessários para produzir `1 @` (`15 kg`) de amêndoa seca:
  - *Referência prática:* `CCN-51 ≈ 220` • `Clones CEPEC ≈ 250` • `Cacau Comum / Pará ≈ 280 a 300`.
- **Meta de Fotos `[NPF]`:** Quantidade de árvores que serão fotografadas na amostragem daquela quadra (padrão recomendado: `20` plantas).
- ** Contadores Fitossanitários:**
  - `NFS` = Número de Frutos Sadios contados na amostra.
  - `NFD` = Número de Frutos Doentes contados na amostra (*Podridão Parda + Monilíase + Vassoura de Bruxa*).
  - `NFT` = Total de frutos na amostra (`NFS + NFD`).

---

### 3.2. Passo a Passo de Uso em Campo

#### Passo 1 — Cadastrar Fazenda e Roças (Aba `Fazendas`)
1. Na aba **Fazendas**, toque no botão **`+`** e informe o nome da fazenda (município e responsável são opcionais).
2. Toque na fazenda criada e depois em **`Adicionar Roça / Quadra`**.
3. Preencha o nome da quadra, **Área (`ha`)**, **Total de Plantas (`NPT`)** *(ou use a calculadora automática por espaçamento)*, **Frutos/@ (`FA`)** *(ou toque num chip de clone)* e a **Meta de Fotos (`NPF`)** -> toque em **`Salvar`**.

#### Passo 2 — Fotografar as Árvores da Quadra (Aba `Amostrar`)
1. Na aba **Amostrar**, escolha a Fazenda e toque no card da **Roça / Quadra** desejada -> **`Iniciar Amostragem`**.
2. Caminhe em **zigue-zague** pela quadra para representar bem toda a área.
3. Em cada árvore sorteada (`Planta 01 de 20` ... `Planta 20 de 20`), posicione-se a cerca de **1,5 metro** do tronco/copa baixa e toque no botão central da câmera.
4. Na tela de **Revisão**:
   - Confira as marcações da IA sobre os frutos;
   - Se precisar corrigir algum fruto, **toque diretamente sobre a caixa** na foto para alternar (`Sadio → Podridão Parda → Monilíase → Vassoura → Remover`) ou ajuste os botões **`-` / `+`** abaixo;
   - Toque em **`Próxima`** até completar `NPF/NPF` e clique em **`Concluir`**.
5. *Pausa em campo:* Se precisar interromper no meio da caminhada, toque em Voltar -> **`Salvar rascunho e sair`**. Ao retornar mais tarde, basta tocar em **`Retomar`**.

#### Passo 3 — Emitir Relatórios e Enviar no WhatsApp (Aba `Relatórios`)
1. **Relatório Parcial** *(visão consolidada da fazenda no mês/ano)*:
   - Selecione a **Fazenda**, o **Período (`Mês/Ano`)** e **Quadras** (`Todas as Quadras` ou uma específica).
   - Escolha entre **`Líquida Sadia (NFS)`** ou **`Potencial Bruto (NFT)`**.
   - Confira a tabela de semáforo fitossanitário:
     - **Controlado (`< 10%` de perda):** manejo preventivo normal;
     - **Atenção (`10% a 19,9%`):** reforçar remoção de frutos doentes e monitorar;
     - **Crítico (`>= 20%`):** foco prioritário de intervenção fitossanitária;
     - **Alerta Quarentenário (Monilíase):** qualquer fruto suspeito exige isolamento imediato e comunicação à CEPLAC/defesa agropecuária.
   - Toque em **`Enviar Relatório PDF no WhatsApp`** para compartilhar o documento oficial ou em **`Planilha (.csv)`** para abrir no Excel.
2. **Relatório Cumulativo** *(evolução de 1 fazenda e 1 quadra)*:
   - Selecione a **Fazenda** e a **Quadra específica** para visualizar o gráfico de linhas comparando **Produção Estimada (`@`)** vs. **Produção Real (`@`)** ao longo das colheitas e exporte o PDF.

#### Passo 4 — Lançar a Produção Real Pós-Colheita
1. Após colher e beneficiar o cacau da quadra, acesse **Fazendas -> sua fazenda -> botão `Histórico`** da quadra (ou toque direto na linha da quadra dentro da tabela de Relatórios).
2. Localize a avaliação correspondente àquela safra e toque em **`+ Informar Produção Real (@)`**.
3. Digite a quantidade de arrobas efetivamente obtidas e toque em **`Salvar Produção Real`**. O gráfico do **Relatório Cumulativo** e o cálculo de precisão serão atualizados automaticamente.

#### Passo 5 — Fazer Backup de Segurança (`MicroSD` / `Nuvens`)
1. Na tela inicial (**Fazendas**), toque no ícone de **nuvem/backup** no canto superior direito.
2. Escolha **`Salvar Backup no Aparelho (.db)`** (para gravar no armazenamento interno ou cartão **MicroSD**) ou **`Compartilhar Arquivo .db`** (para enviar uma cópia segura para **Google Drive, OneDrive, e-mail ou WhatsApp**).
3. Para recuperar dados em outro celular, basta abrir a mesma tela e tocar em **`Selecionar Arquivo .db para Restaurar`**.
