# Changelog

As versões seguem `MAJOR.MINOR.PATCH`: quebra de compatibilidade, incremento compatível e correção compatível. Pré-versões usam, por exemplo, `1.2.0-rc.1`. O manifest de produção é a fonte única da versão do mod e do alvo Hytale. Cada entrega deve atualizar a seção correspondente antes de compilar.

## [Unreleased]

Nenhuma alteração registrada após 1.4.0-rc.2.

## [1.4.0-rc.2] - 2026-10-24

### Classroom — incremento 1
- `/classroom` abre formulário de criação/edição da única turma do professor; nome e descrição validados no servidor, erros e telas en-US/pt-BR. A prova de contador permanece em `/classroom --probe`.
- Permissão nativa `sisso.logicgates.classroom.teach`, conferida em cada operação, junto da responsabilidade pelo vínculo. Identidade, ID de turma e revisão vêm da sessão do servidor.
- SQLite JDBC 3.53.4.0 empacotado com recursos nativos e licenças; banco por servidor em `getDataDirectory()`, esquema v1, vínculo atômico, índices de unicidade, edição por revisão e executor de I/O dedicado.
- Falhas de armazenamento preservam os dados e isolam Classroom dos circuitos. Documentados backup/restauração, autorização docente e roteiro das novas telas.
- Testes de persistência, autorização, revogação, concorrência, rollback e esquemas inválidos; teste do driver isolado no JAR de produção e integração da mesma turma antes/depois de reiniciar o servidor.

### Validação
- Em 24/10/2026, usuário confirmou abertura de `/classroom`, dez cliques com atualização, fechamento e reabertura com reset no build `1.4.0-rc.1+43804c498938`, pt-BR. Captura preservada; demais cenários de cliente mantidos como pendentes.
- O formulário de rc.2 ainda requer teste no cliente. Convites, ingresso de alunos, matrícula e mensagem do dia permanecem nos incrementos seguintes.

## [1.4.0-rc.1] - 2026-10-24

### Classroom — incremento 0
- `/classroom` abre uma página Custom UI dispensável, com contador mantido pelo servidor, atualização parcial e botão de fechar. Cada abertura começa em zero; en-US e pt-BR desde a primeira tela.
- Testes com builders/codecs reais da API 0.7.4 cobrem bindings, eventos, estado independente, fechamento e dispensa; o verificador de idiomas passa a conferir referências em arquivos `.ui`.
- O plano aprovado registra os contratos futuros de turmas, ingresso, matrícula, SQLite, atividades, entregas e avaliação, separados da prova de interface.

### Validação
- Na entrega inicial, os testes no cliente estavam pendentes. A confirmação posterior do fluxo central em pt-BR está registrada acima; testes sem cliente e inicialização do servidor não aprovam a interface.
- Turmas, vínculos, persistência e painéis completos serão implementados após a prova mínima no cliente. Circuitos, IDs, codecs e arte existente preservados.

## [1.3.3] - 2026-10-23

### Polimento
- Compactação PNG sem perdas, determinística entre versões de Python/zlib: mesmos pixels, dimensões, transparência, UVs e geometria. Atlas compilados, ícones e projeto editável da bancada ocupam menos espaço.
- Monitoramento em repouso reutiliza índices de holders e usa chaves primitivas, alocando novos mapas somente após mudança real de vínculos; sem alterar o escopo global da avaliação.
- Benchmark nativo opcional `benchmarkServer`: seções reais, redes independentes, alteração de uma fonte, grade de 1.024 cabos e bancada não elétrica; mede monitoramento, avaliação/publicação e alocações.
- Resolvedor nativo de drops confere um item base por componente, bancada e variantes ligadas/desligadas/conectadas, antes e depois de reiniciar.
- Documentação atual separada do histórico; multiplayer, circuitos e crafts registrados conforme confirmação do usuário. Drops no cliente e cancelamento/reembolso não são considerados aprovados por esse relato.
- Roteiro de manutenção e critérios de entrega consolidados. Classroom e conexões verticais permanecem no planejamento.

## [1.3.2] - 2026-10-23

### Alterado
- Carcaça, tampa removida e lente do AND decorativo da bancada reutilizam exatamente os pixels dos materiais do AND colocado no chão. Madeira, ferramentas e contatos internos são preservados.
- Marcações ON/OFF impressas na base da alavanca, nas faces frontal, traseira e superior, alinhadas aos extremos da haste. Sem novas peças, colisões ou alterações elétricas.
- Atlas de bancada e alavanca compilados a partir das artes existentes e das letras em pixels, sem modificar os PNGs fonte.
- Ícones regenerados; projeção dos UVs da face superior corrigida para corresponder ao modelo no Blockbench/Hytale.

## [1.3.1] - 2026-10-23

### Corrigido
- Texturas dos dez componentes simplificadas: uma cor suave por porta, poucos tons de luz/sombra e bases neutras para alavanca e lâmpada.
- Cabos, junções e conectores inteiramente pretos, com variação discreta de borracha; retirados condutores de cobre visíveis e enfeites pintados.
- Ícones regenerados a partir dos modelos com as novas texturas; formas, orientação, colisões dos componentes e regras elétricas preservadas.
- Eliminadas faces sobrepostas entre a base e as paredes do AND aberto na bancada, origem de cintilação por profundidade. Textura e desenho da bancada preservados.
- Verificações automáticas de borracha preta e de ausência de faces externas coplanares no AND decorativo.
- Conferência exaustiva de pixels compartilhados reaproveitada entre variantes, mantendo a validação de UVs, cores e materiais de cada face.
- Plugin de integração recompilado quando a versão do mod ou do jogo muda; versões incompatíveis são rejeitadas antes de iniciar o servidor.

## [1.3.0] - 2026-10-23

### Adicionado
- Categoria LogicGates no criativo, com três divisões e ícones próprios dos onze itens.
- Bancada artesanal de 2 × 1 blocos, com AND aberto, fonte Blockbench e fabricação nativa.
- Receitas diretas de cobre, ferro e materiais comuns; bancada fabricável instantaneamente no inventário, conforme o Fieldcraft nativo.
- Renderizador reproduzível dos modelos reais e validação obrigatória de categoria, ícone e receita.

### Alterado
- Texturas individuais no estilo de oficina Hytale, com cores por porta, ferro claro e cobre.
- Indicadores cinza escuro desligados e vermelhos luminosos ligados; geometria dos 264 modelos anteriores preservada.
- ZIP inclui fontes editáveis, atlas, prompts, documentação e JAR verificado; en-US e pt-BR completos.

### Validação
- Build, geradores, testes de geometria/UV/ícones e integração de receitas/fillers no servidor 0.7.4.
- Conferência visual, interface/filas de fabricação, drops por quebra e dois jogadores continuam no roteiro do cliente.

## [1.2.0] - 2026-10-23

### Ensino e localização

- Tradução completa pt-BR dos itens, ajuda e diagnósticos, com en-US como base e verificação de chaves/parâmetros entre línguas.
- Letras A/B/C e seta de saída nos sete módulos, inclusive sem cabos conectados; NOT identifica somente C e saída. Texturas anteriores preservadas.
- `/logichelp` explica entradas opcionais, paridade, limitações e gera tabelas verdade com 1-3 entradas usando o avaliador real.
- `/logicinspect x y z` informa orientação, cada entrada física, saída e motivo do resultado: lógica, ausência de entradas, seção indisponível ou realimentação indefinida. Não carrega seções.

### Manutenção e medições

- Catálogo de metadados compartilhado entre Java, geradores e auditoria do JAR; identificadores existentes preservados.
- Onze exportações originais movidas para `assets/models/`, separadas dos recursos gerados de runtime; fluxo Blockbench atualizado.
- `/logicenergy` acrescenta tempos da avaliação completa, avaliador e monitoramento; janela limitada a 256 amostras.
- `benchmarkCircuit` mede mediana, p95 e alocação em linhas, grades, cadeias das sete portas e redes independentes.
- Novos testes de inspeção, tabelas, idiomas, marcações, métricas e comandos/permissões no servidor real.
- Repositório próprio e histórico organizado por incrementos; CI portável preparada para execução remota.

Classroom e conexões verticais continuam fora deste incremento. Teste com dois jogadores fica a cargo do usuário.

## [1.1.0] - 2026-10-23

### Organização

- Documentação consolidada em um índice: arquitetura, desenvolvimento, arte, validação, referências, histórico e funcionalidades futuras.
- Removidos o exemplo legado, cópias extensas de documentação externa e placeholders sem uso `EnergyNodeComponent`, `ToggleSourceComponent` e `BlockContextAware`.
- Projeto Blockbench original de NAND transferido para a área de autoria, fora dos recursos de produção. Fontes de arte preservadas.
- `build/` passa a conter somente saídas reproduzíveis, relatórios e universos temporários; mundos de desenvolvimento permanecem fora dele.

### Build e diagnóstico

- JAR e pacote de entrega identificados pela versão, com checksum SHA-256 e `release.json`.
- Identificação por versão e impressão digital das fontes no log de inicialização e em `/logicenergy`; revisão Git e estado de alterações registrados no pacote.
- Build não regrava o manifest de origem. Plugin de integração recebe a mesma versão e dependência exata do mod.
- `build` inclui testes, conferência dos quatro geradores, traduções, links locais e auditoria do JAR. `fullCheck` acrescenta integração em servidor real e reinício.
- CI para Windows/Linux e workflow manual de integração nativa em runner próprio. Assets do jogo não são distribuídos.

### Compatibilidade

- Mantidos Hytale 0.7.4, identificadores de blocos/componentes, persistência e regras de circuitos.
- Classroom, novas línguas e transmissão vertical continuam planejados.

## [1.0.0] - Estado anterior, sem release formal

O manifest anterior declarava 1.0.0, mas as entregas locais usavam um nome fixo de JAR, sem uma release ou tag formal correspondente. Esta entrada registra o conjunto anterior, sem atribuir retroativamente uma versão às compilações históricas.

- Alavanca, cabo e lâmpada; conexões horizontais, máscaras visuais, colisão por peça e estados reconstruídos.
- AND, OR, NOT, NAND, NOR, XOR e XNOR; entradas opcionais, isolamento de faces e tratamento de ciclos sem memória.
- Modelos editáveis de XOR/XNOR e atlas metálico com contraste entre base e indicador.
- Última referência antes da organização: 733 testes JUnit e 44 grupos no servidor. Consulte o [histórico de validação](docs/history/validation.md).
