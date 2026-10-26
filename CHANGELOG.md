# Changelog

As versões seguem `MAJOR.MINOR.PATCH`: quebra de compatibilidade, incremento compatível e correção compatível. Pré-versões usam, por exemplo, `1.2.0-rc.1`. O manifest de produção é a fonte única da versão do mod e do alvo Hytale. Cada entrega deve atualizar a seção correspondente antes de compilar.

## [Unreleased]

Nenhuma alteração registrada após 1.4.0-rc.14.

## [1.4.0-rc.14] - 2026-10-26

### Proporções e consulta de atividades
- Listas e detalhes compartilham a largura disponível, com limites na lateral. Tipografia, campos, botões e preview foram ampliados; paginação e ações mantêm quebra de linha e conteúdo rolável.
- Selecionar uma atividade abre sua ficha de consulta. Somente Editar atividade abre o formulário docente; Voltar à visualização encerra a edição sem salvar campos automaticamente. Criação continua ampla; publicar retorna à consulta.
- Professor consulta rascunhos salvos com indicação de privacidade; aluno continua vendo somente a publicação vigente. Eventos de gravação são recusados fora do editor e arquivadas exigem restauração para editar.
- Mantém autorização por operação, revisões, contagens, SQLite v5 e conteúdo das entregas. A apresentação final continua sujeita a teste no cliente.

## [1.4.0-rc.13] - 2026-10-26

### Polimento de turmas e atividades
- Painéis com margem externa de 16 unidades, consultas com lateral de 280, navegação e ações com quebra de linha. Fichas compactas usam altura do conteúdo; feedback vazio fica oculto. Ocupação e escalas continuam sujeitas ao aceite no cliente.
- Criação de atividade em largura total, sem lista lateral ou paginação. Salvar rascunho mantém o formulário; publicar retorna à apresentação normal e Voltar às atividades preserva o rascunho já criado, sem salvar campos automaticamente.
- Prazo com seletores nativos de dia, mês, ano, hora e minuto, em Brasília. Data vazia permite rascunho; parcial ou inexistente é recusada. Dias acompanham mês/ano, todos os minutos estão disponíveis e a persistência permanece em UTC.
- Resumo da turma informa atividades não arquivadas para o professor e somente publicadas para o aluno, sem duplicar edição privada. Contagem no mesmo snapshot e orçamento de uma conexão/até três SELECTs na tela docente; invalidações de catálogo separadas de entregas/notas e mensagem do dia.
- Administração coloca Salvar alterações e Encerrar turma na mesma linha quando possível, com controles próprios de confirmação. Regras de autorização, revisões, SQLite v5, circuitos e conteúdo das entregas preservados.

## [1.4.0-rc.12] - 2026-10-26

### Compatibilidade da tela de atividades
- Remove comandos `.Disabled` de campos de texto da tela de atividades, incompatíveis com o cliente Hytale 0.7.4. A rc.11 desconectava o jogador ao abrir Atividades com o seletor `#ScoreField.Disabled`; o mesmo problema foi corrigido na edição, gravação e recuperação de erros dos seis campos.
- Mantém bloqueio dos botões durante operações e validação de eventos, permissão, vínculo e revisão no servidor. Banco v5, entregas, circuitos e mundos preservados.
- Regressões verificam os comandos renderizados contra os tipos dos widgets empacotados, incluindo professor/aluno, rascunho, publicação, arquivamento, nota, gravação, erros e preservação dos campos durante atualização. Renderização e interação ainda exigem reteste no cliente.

## [1.4.0-rc.11] - 2026-10-26

### Atividades, entregas de áreas e avaliação manual
- Atividades com rascunho privado, publicação explícita em versões imutáveis, prazo futuro obrigatório em Brasília, valor positivo padrão 10, arquivamento e restauração. Alunos ativos consultam apenas publicações da própria turma.
- Seleção restrita ao fluxo de entrega, sem conceder OP ou comandos de edição. Confirmação captura conteúdo fixo, preservando orientação e intenção das alavancas; reenvios preservam revisões. Envios iniciais e reenvios atrasados são aceitos e identificados.
- Preview 3D nativo da revisão selecionada e cópia temporária para correção, com espaço vazio, validação de proteção, reserva exclusiva, edição pelo responsável e descarte do volume completo. Reservas persistentes permitem recuperação após interrupção/reinício sem carregar terreno distante.
- Notas/feedback associados à revisão exata e ao valor daquela publicação; zero atribuído é distinto de ausência de nota. Alterações na nota preservam histórico; reenvio começa sem avaliação.
- SQLite v5 acrescenta metadados pedagógicos com backup pré-migração; arquivos privados comprimidos, versionados e verificados por checksum. Cotas e fila limitada de operações pesadas evitam trabalho ilimitado; listas carregam apenas metadados paginados.
- Prova nativa e regressões de papéis, versões, concorrência, limites, gravação e transporte. Seleção, preview/renderização e edição no cliente permanecem sujeitos ao roteiro de aceite. Circuitos, mundos e aparência dos componentes preservados; logging ampliado e avaliação automática posteriores.

## [1.4.0-rc.10] - 2026-10-26

### Leituras agregadas do Classroom
- Home e telas usam uma conexão por consulta. Vínculo, turma e nomes são lidos juntos; listas e contagens reutilizam o contexto validado, eliminando verificações SQL repetidas da mesma responsabilidade.
- Uma transação de leitura diferida conserva o mesmo snapshot entre cabeçalho, lista e contagem. Conexões são fechadas após sucesso ou falha. Política pura de leitura centraliza autorização de contexto, normalização de abas e validação de busca/paginação; a permissão nativa permanece consultada no serviço.
- Regressões e contadores somente no classpath de teste verificam equivalência por papel, paginação, uma conexão por tela, limite de consultas, rollback e concorrência com aprovação/saída/encerramento.
- Esquema SQLite v4, operações de gravação, layout, comandos e circuitos preservados. Não há pool, cache entre requisições, WAL ou paralelismo novo. Atividades seguem para um incremento posterior.

## [1.4.0-rc.9] - 2026-10-24

### Encerramento de turma pelo professor
- Administração oferece Encerrar turma, com confirmação e cancelamento. Somente o responsável com permissão docente pode confirmar; a revisão exibida protege contra alterações antigas.
- Uma transação encerra os vínculos ativos do professor e dos alunos e cancela convites/solicitações pendentes. Turmas encerradas deixam a descoberta; participantes podem ingressar em outra turma, e o professor pode criar uma nova. Matrículas, vínculos anteriores, aprovações e conteúdo da turma são preservados no banco. Não há restauração de turma ou exclusão definitiva nesta entrega.
- SQLite v4 substitui a unicidade permanente do professor por unicidade de turma aberta e registra o instante de encerramento. Migração com backup consistente preserva IDs e referências de v1–v3; o rollback para um mod antigo exige o banco do backup correspondente.
- Notificação de encerramento atualiza páginas relacionadas após o commit, incluindo alunos e histórico de ingressos visível, sem consulta periódica. As otimizações da rc.8 permanecem; atividades, circuitos e mundos não são alterados.
- Regressões cobrem migração/restauração, concorrência com aprovação, rollback integral, autorização, confirmação, histórico e recriação. Encerramento e demais interações da candidata ainda exigem validação no cliente.

## [1.4.0-rc.8] - 2026-10-24

### Redução de atualizações e análise de escala
- Assinaturas de autorização indexadas por UUID; eventos de grupo percorrem cada conta com página aberta uma vez. Eventos que mantêm a autorização efetiva não reconsultam nem redesenham as páginas.
- Filtro de contexto imutável evita agendar trabalho no mundo para páginas sem relação com uma mudança de dados. Durante I/O, eventos são agrupados em lote limitado e conferidos contra o contexto resultante. Rajadas agendam no máximo uma tarefa pendente por página/canal.
- Transporte incremental conserva linhas/bindings quando os registros não mudam e envia somente propriedades alteradas. Valores dos campos continuam sempre enviados quando explicitamente solicitados, preservando recarga e descarte na revogação. Respostas vazias mantêm a confirmação exigida pelo cliente nativo.
- Benchmark isolado compara consultas, rajadas concorrentes, assinaturas e tamanho dos pacotes com a rc.7; não mede jogadores reais, rede, renderização ou tick do mundo. Registra relato do usuário sobre multiplayer, atualização de permissão confirmada e demora na primeira interação de cada personagem.
- Esquema SQLite v3, autorização por operação, regras de vínculo, layout e circuitos preservados. O tempo de carregamento inicial do cliente ainda exige reteste; não há promessa de capacidade de jogadores baseada em assinaturas sintéticas.

## [1.4.0-rc.7] - 2026-10-24

### Orientação das listas e contrato de atividades
- Listas vazias apresentam orientação específica para membros, ingressos do professor, histórico do aluno, busca de jogadores e descoberta de turmas. Páginas posteriores vazias orientam voltar por Anterior e não afirmam que a turma inteira está vazia.
- Confirmação, atualização e mensagens explícitas de gravação, erro e resultado conservam prioridade sobre as orientações. Seleções mantidas durante atualização preservam a orientação da ficha em edição.
- Seis textos novos correspondentes em pt-BR/en-US, sem alterar o markup ou a estrutura do layout. Regressões cobrem contextos vazios, paginação, seleção preservada e prioridade das mensagens.
- Documentado o contrato aprovado de atividades: rascunho/publicação versionada, título, enunciado, prazo futuro obrigatório em Brasília, valor positivo padrão 10, nota ainda não atribuída como —/valor, arquivamento/restauração e acesso por vínculo. Atividades, entregas e avaliação ainda não têm código, tabelas ou telas.
- Abertura docente da rc.6 em pt-BR permanece evidência parcial histórica; interação, autorização automática, textos longos e demais cenários de cliente continuam pendentes na candidata. Esquema SQLite v3, regras de vínculo, comandos e circuitos preservados.

## [1.4.0-rc.6] - 2026-10-24

### Correção da leitura de Custom UI
- Corrigida a ordem de declarações de parâmetros em Buscar e Próxima: `@Anchor`/`@Text` agora precedem `FlexWeight`, conforme a sintaxe nativa 0.7.4. Na rc.5, o cliente reportou `Expected {, found =` ao entrar no mundo, antes de permitir testar `/classroom`.
- Verificação direcionada rejeita parâmetros declarados após propriedades ou filhos, tanto nas fontes quanto nos documentos do JAR. Regressões cobrem os dois casos originais, comentários, literais, blocos aninhados e arquivo empacotado inválido. Não substitui o parser/renderizador do cliente.
- Registro da falha rc.5 e roteiro rc.6 começam pela entrada no mundo, abertura, fechamento e reabertura. Os demais testes de polimento continuam pendentes no cliente.
- Sem alterações de domínio, permissões, esquema SQLite v3, dados de turma ou circuitos.

## [1.4.0-rc.5] - 2026-10-24

### Classroom — polimento e autorização automática
- Mantidas as abas e a organização lista/ficha. Busca ampliada, paginação com contador em linha própria, metadados separados, conteúdo vazio oculto e seleção identificada. Matrícula, confirmações e Salvar ficam fora da área rolável.
- Fichas conservam o texto completo; listas apresentam resumos. Histórico identifica tipo, situação e instante de criação UTC. Mensagens de estado e listas vazias são específicas do contexto, em en-US/pt-BR.
- Eventos nativos de permissões individuais, grupos/OP e permissões de grupo revalidam páginas abertas sem consultas periódicas. Canal próprio não altera dados nem reenvia a mensagem do dia.
- Gerações de autorização invalidam respostas antigas. Revogação descarta rascunhos docentes; restauração carrega os dados persistidos. Atualizações com autorização inalterada preservam campos e revisões apresentados.
- Regressões de transporte, ciclo de autorização, isolamento de notificações, paginação e preservação de dados; integração real exercita os seis eventos nativos.
- `prepareClassroomUiFixture` cria um banco sintético novo, por serviços de produção, para testar listas extensas em servidor/save isolado. Gerador e dados não entram no JAR de produção.
- Evidências da rc.4 consolidadas: usuário confirmou fluxos centrais em pt-BR e bloqueio de alterações após revogação. O novo layout e atualização visual automática da rc.5 ainda exigem validação no cliente.
- Sem migração de banco, mudanças de regras de vínculo ou alterações de circuitos/assets existentes. Versão estável e tags dependem de aceite posterior.

## [1.4.0-rc.4] - 2026-10-24

### Correção de Custom UI
- Corrigida a desconexão ao abrir `/classroom`: alturas dos títulos, descrição e mensagem agora usam `setObject(selector + ".Anchor", Anchor)` com o codec nativo 0.7.4, preservando as margens do layout. Seletores como `#HeaderTitle.Anchor.Height` eram recusados pelo cliente.
- Regressão de transporte cobre as seis telas, seleções e atualizações, incluindo textos longos e multilinha. Confere objetos de âncora serializados e rejeita seletores de subpropriedades; não substitui renderização no cliente.
- Documentação distingue o JAR instalável do ZIP de entrega e registra a falha real no cliente rc.3. A rc.4 requer repetir abertura, navegação e o roteiro com professor/aluno.
- Esquema SQLite, dados existentes, circuitos e assets preservados.

## [1.4.0-rc.3] - 2026-10-24

### Classroom — MVP administrativo para validação
- Entrada por vínculo: turma do professor, consulta básica do aluno e descoberta/ingresso para jogadores sem turma.
- Lista pública pesquisável, solicitações com matrícula textual, convites persistidos para jogadores conhecidos e conferência docente antes de qualquer ingresso. Pendências convergem e a primeira aprovação encerra as demais.
- Alunos e processos em listas paginadas, seleção por UUID, ficha, correção de matrícula, remoção e saída com confirmação; histórico preservado. O aluno acompanha os estados dos próprios ingressos.
- Administração de nome, descrição e mensagem em gravação atômica por revisão. Mensagem no painel e no chat após a primeira prontidão da conexão, sem repetição por troca de mundo.
- Migrações v1 → v2 → v3 preservam a turma existente e guardam backup consistente antes da atualização. Índices garantem matrícula ativa única e um único vínculo ativo entre papéis.
- Notificações após commit, revisões preservadas durante edição, limpeza de sessões/listeners e I/O com fila limitada. Traduções nativas en-US/pt-BR.
- Testes de domínio, persistência, migração/restauração, concorrência, transporte e sessão; integração nativa exercita solicitações, convites, matrícula e mensagem antes/depois de reiniciar.

### Validação
- Usuário confirmou o fluxo central da rc.2 em pt-BR: turma preservada após sair/entrar e recarregar, além da regressão do contador. Captura e escopo preservados na documentação.
- As novas telas de rc.3 aguardam validação no cliente 0.7.4 com professor/aluno nos dois idiomas. Testes automatizados não comprovam renderização/interação.
- Atividades, entregas, avaliação e painel pedagógico completo permanecem posteriores; circuitos, IDs, codecs e aparência preservados.

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
