# Regras e arquitetura

## Comportamento atual

O mod implementa circuitos combinacionais horizontais. Não há condução vertical/diagonal, atenuação, atraso intencional, relógio ou memória persistente de circuitos. Cruzamentos de cabos unem os caminhos. Cada rede agrega seus produtores por OR.

Alavancas e lâmpadas são terminais e não unem redes distintas. A alavanca mantém a intenção ligada/desligada. A lâmpada apenas consome sinal. Formas dos fios representam conexão física; indicadores representam o resultado lógico.

As sete portas usam uma única saída frontal. No referencial do modelo, saída = **+X**, A = **−Z**, B = **+Z**, C = **−X**. A rotação real do bloco converte essas faces para o mundo. A colocação corrige a orientação do modelo para seguir o olhar; a rotação manual continua disponível. NOT usa somente C, atrás.

| Porta | Saída, quando há pelo menos uma entrada conectada |
| --- | --- |
| AND | Todas as entradas conectadas estão ligadas |
| OR | Pelo menos uma entrada está ligada |
| NOT | Entrada traseira desligada |
| NAND | Inverso de AND |
| NOR | Inverso de OR |
| XOR | Quantidade ímpar de entradas ligadas |
| XNOR | Quantidade par de entradas ligadas |

Entradas vazias são ignoradas; cabo conectado sem energia conta como 0. **Sem entradas conectadas, todas as portas ficam desligadas.** Remover uma entrada 0 pode ligar AND. Cada face é um operando, mesmo se duas faces lerem a mesma rede: duas entradas energizadas da mesma rede desligam XOR; três ligam XOR.

Entradas aceitam cabo, alavanca ou saída de outra porta. Saídas alimentam cabo, lâmpada ou entrada de porta. Entrada–entrada e saída–saída não conectam. A compatibilidade é compartilhada entre simulação e desenho. Energia externa no cabo da saída não altera o resultado/indicador da porta por si só.

## Camadas

| Local em `src/main/java/sisso/logicsim/` | Responsabilidade |
| --- | --- |
| `LogicSimPlugin` | Registro de componentes, sistemas, interação e comandos |
| `circuit/` | Posições, faces, compatibilidade, grafo, avaliação e resultado; sem acesso ao mundo Hytale |
| `components/` | Intenção persistente da alavanca e identificação de cabo, lâmpada e porta |
| `systems/` | Índices por mundo, ciclo de vida de entidades/seções e avaliação após invalidação |
| `util/BlockComponentAccess` | Leitura de dados carregados, rotação, disponibilidade e publicação visual na API 0.7.4 |
| `util/LogicBlockTypes` | Identificadores e estados dos blocos |
| `util/GateOrientation` | Conversão compartilhada da rotação horizontal para mundo e snapshot |
| `util/BuildInfo` | Identificação da compilação empacotada |
| `interactions/` | Acionamento da alavanca |
| `commands/` | Ajuda `/logichelp`, inspeção `/logicinspect`, diagnóstico `/logicenergy` e abertura de `/classroom` |
| `classroom/ui/` | Páginas Custom UI; estado por abertura e retorno assíncrono à sessão atual |
| `classroom/domain/` | Serviço, contratos e regras administrativas sem dependência de Hytale |
| `classroom/persistence/` | SQLite compartilhado pelo servidor, esquema versionado e transações |
| `classroom/learning/` | Atividades, entregas, verificação, arquivos privados, seleção e cópias temporárias |
| `classroom/ClassroomRuntime` | Fila de I/O, inicialização, notificações e encerramento dos serviços |

`CircuitWorldState` é transitório e pertence ao ChunkStore de um mundo, acessado na thread apropriada. O índice acompanha entidades ativas e holders estacionados em seções carregadas. Mudanças na orientação e na disponibilidade das faces invalidam a avaliação, inclusive quando uma seção vazia retorna.

## Avaliação e persistência

Cada snapshot começa sem reaproveitar energia da avaliação anterior. Redes de cabos e saídas de portas são sinais distintos. Os operandos preservam multiplicidade por face; as dependências do grafo não precisam de duplicação.

O avaliador identifica componentes fortemente conectados sem recursão. Trechos acíclicos são resolvidos na ordem das dependências. Ciclos monotônicos AND/OR partem de desligado e propagam até estabilizar. Ciclos com NOT/NAND/NOR/XOR/XNOR propagam valores determinados a partir de estados indefinidos; o que não puder ser determinado permanece desligado no resultado e aparece no diagnóstico. Não é um resolvedor geral de equações booleanas nem um latch.

O resultado final contém estados elétricos, máscaras visuais, portas aguardando seções e realimentações indefinidas. Somente esse resultado é publicado; o jogador não observa as iterações internas.

Somente a intenção da alavanca é fonte autoritativa persistente. Componentes/identificadores e rotação são preservados pelo mundo; redes, saídas e índices são reconstruídos. Alterações visuais preservam entidade e rotação.

## Disponibilidade e desempenho

Consultas não forçam carregamento de seções. Qualquer face de entrada relevante indisponível suspende a porta com saída/indicador desligados; indisponível não é uma entrada removida. As laterais fechadas do NOT não o suspendem. Redes deixam de incluir seus trechos descarregados.

A invalidação atual pode reconstruir o conjunto carregado do mundo, e há verificações de holders/topologia por tick. Medir tempo de avaliação, custo em repouso e múltiplos jogadores antes de trocar isso por invalidação localizada. `benchmarkServer` mede descoberta/publicação em seções reais, em cenários controlados sem clientes; não justifica uma promessa de capacidade máxima. O monitoramento compara identidade/posição/quantidade dos holders com o índice anterior e só reconstrói esse índice quando muda. Seções vazias e holders não elétricos não criam mapas temporários por tick.

## Ensino, inspeção e métricas (1.2.0)

`CircuitSnapshot` mantém uma cópia imutável dos nós e do último resultado concluído por mundo. `diagnostics/` lê esse snapshot sem consultar Hytale: a inspeção identifica A/B/C pela orientação, distingue vazio/0/1/face incompatível/indisponibilidade e informa a razão da saída. As tabelas de ajuda usam o próprio avaliador com entradas explícitas. Nenhum desses dados é persistido.

`/logicinspect` executa no contexto do mundo e não carrega seções. Se há avaliação suja, atualiza os sinais derivados antes de responder. `/logicenergy` aguarda a coleta assíncrona dos mundos sem bloqueá-los. `CircuitMetrics` armazena no máximo 256 amostras por categoria; o monitoramento mede detecção de mudanças e a avaliação completa inclui coleta, solução e publicação. Os percentis são calculados somente quando consultados.

`GateCatalog` usa os metadados compartilhados com os geradores. A semântica booleana continua explícita no avaliador Java, com validação contra as classificações do catálogo. A identidade de blocos/componentes e a persistência existente não mudaram.

## Oficina e fabricação (1.3.0)

A bancada usa `BenchBlock` e a interface/fabricação nativa de Hytale. Não participa da indexação elétrica nem acrescenta estado aos componentes de circuito. O catálogo da oficina em `assets/workshop/catalog.json` concentra aparência, ícones, categorias e receitas; o catálogo lógico permanece responsável pelas regras e faces das portas. Os geradores publicam os recursos e o build exige registro completo de todo item novo. Veja [fabricação](crafting.md) e [arte](assets.md).

## Classroom — aplicação, banco e ciclo de vida

`ClassroomCommand` usa `AbstractPlayerCommand` e abre `ClassroomPage` no contexto do mundo. A página lê e modifica dados pelo `ClassroomRuntime` compartilhado pelo plugin. `ClassroomService` consulta a permissão nativa atual em cada operação docente; o repositório revalida responsabilidade, vínculo, UUID e revisão na transação. Nomes e matrículas são apresentação/conteúdo, não autorização. O domínio administrativo não importa Hytale.

`SqliteClassroomRepository` usa um `classroom.db` por servidor, no diretório de dados do plugin. `SqliteAccess` compartilha configuração da conexão, transação de leitura/gravação, commit/rollback e helpers de consulta/paginação com `LearningStore`; SQL, autorização e tradução de erros permanecem nos respectivos consumidores. O esquema atual é **v6**, com identificação própria, chaves estrangeiras e índices de unicidade. As migrações conservam dados administrativos e pedagógicos; critérios de verificação foram acrescentados na v6. Banco conhecido recebe backup consistente antes de migrar; falha ou estrutura incompatível não substitui os arquivos. O journal permanece DELETE, sem pool, WAL ou cache de consultas entre requisições. [Uso e backup](classroom.md#dados-migração-e-backup).

Criação de turma/vínculo, aprovação e encerramento usam `BEGIN IMMEDIATE`, revisão e restrições de unicidade. Cada UUID tem um vínculo ativo; matrícula ativa é única por turma com comparação BINARY. Pendências não reservam matrícula. A primeira aprovação ou criação docente encerra as demais pendências da conta. Encerramento registra a data, termina vínculos e cancela processos pendentes sem apagar histórico; descoberta e vínculo ativo filtram turmas abertas.

A fila dedicada inicializa os serviços antes de atender requisições e admite até 256 operações pendentes. Seu encerramento tenta drenar o trabalho por até cinco segundos, resolvendo pendências como indisponíveis se interrompido. Cada callback retorna ao contexto do mundo e confere referência, Store, mundo, conexão, identidade da página e geração de autorização. Fechar a página não cancela uma gravação já aceita, mas sua resposta não atualiza outra página. Falha do Classroom preserva os sistemas elétricos.

O driver JDBC é empacotado em configuração Gradle exclusiva com recursos nativos/licenças; Hytale não é incorporado. Um classloader isolado executa JDBC a partir do JAR de produção, inclusive na matriz Windows/Linux. Essas verificações e a integração real de reinício não renderizam formulários no cliente.

## Classroom — leituras e apresentação

`ClassroomReadPolicy` centraliza autorização pura do contexto, normalização de abas e validação de busca/paginação. O contexto agregado une vínculo ativo, turma e nomes. `readHome` usa uma conexão/SELECT; `readScreen` usa uma conexão em `BEGIN DEFERRED`, reutilizando esse contexto nos helpers de lista e contagem. Cabeçalho, registros e total pertencem ao mesmo snapshot. A tela docente de membros mantém até três SELECTs, incluindo contagem de atividades sem duplicar uma publicação com rascunho privado. Commit/rollback e fechamento são obrigatórios tanto no sucesso quanto na falha.

`ClassroomScreenRenderer` monta comandos, bindings e mensagens a partir de DTOs; não executa domínio. A página mantém seleção, revisão apresentada, consulta e confirmação por abertura. Atualização comum preserva campos e revisão original; seleção/recarregamento explícitos adotam os dados consultados. IDs das linhas são validados contra o snapshot. Listas têm páginas de 20, conteúdo rolável e ações acessíveis fora da rolagem dos detalhes.

A orientação considera seção, papel, seleção e linhas. Página posterior vazia oferece Anterior sem concluir que não existem registros na turma. Confirmações, atualização e mensagens explícitas de erro/gravação/resultado mantêm prioridade. O resumo docente inclui atividades não arquivadas; o do aluno somente publicadas não arquivadas. Prazo vencido não elimina a atividade da contagem.

`LearningPage` separa consulta de edição: selecionar atividade abre informações, e Editar atividade inicia o formulário docente. Criação usa largura total; sair da edição não salva automaticamente. Rascunhos salvos permanecem privados e alunos leem a publicação vigente. Seletores de dia/mês/ano/hora/minuto são estado de apresentação; a conversão estrita em `America/Sao_Paulo` e a validação do prazo continuam no servidor, com UTC no banco.

## Classroom — autorização, notificações e transporte

`ClassroomPermissions` recebe os seis eventos concretos nativos de permissões individuais, vínculos de grupo/OP e permissões de grupo. O runtime reavalia autorização efetiva num canal separado dos commits; não há polling ou varredura de mundos. Assinaturas são agrupadas por UUID; eventos individuais atingem a conta, e alterações de grupo percorrem cada conta com página aberta uma vez. A última autorização evita callbacks redundantes, sem substituir a consulta nativa feita por cada operação.

`ClassroomPageAccess` registra gerações antes de agendar retorno ao mundo. Respostas anteriores à invalidação não repovoam a interface. Revogações são lembradas mesmo se houver nova concessão antes do processamento: rascunhos visuais docentes são descartados e a restauração usa os dados persistidos. Autorização efetiva inalterada conserva campos e revisão. Fechar, desconectar e encerrar remove assinaturas.

Notificações de dados são emitidas após commit, filtradas antes de agendar trabalho. Páginas conservam snapshots imutáveis de interesse e um lote pendente limitado durante I/O. Rajadas agendam no máximo uma tarefa por página/canal, mantendo os contadores de revogação. Eventos de catálogo são separados de notas/entregas e filtrados por turma; mensagem do dia não é reenviada por essas invalidações.

`ClassroomPageUpdates` conserva o último estado enviado pela abertura. Registros iguais mantêm elementos/bindings; registros alterados são reconstruídos e invalidam propriedades das linhas. Sets intermediários são compactados, mas `.Value` nunca é deduplicado: o cliente pode ter editado localmente. Respostas vazias ainda confirmam o evento nativo, liberando a entrada enquanto uma resposta estiver pendente. Esse estado de transporte não substitui autorização, identidade ou revisão.

`ClassroomPresence` acompanha `PlayerReadyEvent`/`PlayerDisconnectEvent`. O primeiro ready da conexão registra o nome autenticado e consulta a mensagem privada; trocar de mundo não repete. Revisões de sessão invalidam mensagens enfileiradas após alterações de vínculo/turma. Shutdown/desconexão removem listeners.

## Classroom — entregas e correção

`LearningStore` é a fronteira de aplicação/JDBC para rascunhos, publicações imutáveis, revisões, notas e reservas. Revalida contexto e autorização; gravações usam `BEGIN IMMEDIATE` e revisão otimista. Valor e nota usam centésimos inteiros para evitar ponto flutuante. Nota pertence à revisão e à publicação referenciada; avaliar uma antiga não atribui nota à mais recente.

`SubmissionFiles` publica o conteúdo privado antes de referenciá-lo. Inicialização recupera temporários/órfãos reconhecidos, preservando arquivos referenciados e nomes desconhecidos. Checksum, expansão limitada e UUIDs impedem caminhos controlados pelo cliente. Banco e arquivos exigem backup conjunto.

`AreaSessions` restringe seleção a conexão/mundo/atividade; o item não concede autoridade. `AreaWorkQueue` limita operações pesadas e separa arquivos/pacotes do acesso a blocos. `NativeAreaAdapter` lê somente seções carregadas e preserva asset, orientação e intenção. `AreaRuntime` coordena captura, commit e reservas, revalidando o contexto ao concluir.

Reservas são duráveis antes da colagem em volume originalmente vazio; guardas de edição acompanham interações nativas, sem varredura por tick. Limpeza após reinício aguarda disponibilidade natural das seções. Comandos administrativos/APIs que contornem esses eventos não integram a exclusividade da reserva; proteções externas precisam de validação conjunta.

`LearningPage` carrega listas paginadas de metadados. O conteúdo/preview só é carregado para a revisão selecionada, é limpo ao trocar/fechar e não é reenviado a cada atualização. O canal pedagógico não invalida o painel administrativo/MOTD a cada nota ou entrega. [Contratos e limites](classroom-learning.md).

## Classroom — verificação assistida

`VerificationRules` conserva quantidades exatas opcionais e tabela esperada com rascunho/publicação. `LearningStore.verification` lê entrega e publicação correspondente em contexto autorizado ao professor. `AreaRuntime.verify` usa a fila pesada, confere o arquivo/checksum e revalida autorização depois da análise. `SubmissionVerifier` executa o avaliador puro, sem consultar o mundo, mudar nota ou persistir resultado.

Entradas são alavancas e saídas são lâmpadas, ordenadas por posição local X/Y/Z. A tabela completa admite até seis entradas, quatro saídas e 64 casos; a simulação admite 2048 componentes lógicos. I/O desconectado ou realimentação não resolvida produz resultado inconclusivo, sem inventar erro lógico do aluno. `LearningVerification` apresenta o relatório da revisão selecionada sem repor nota/feedback. Não há avaliação contínua ou pontuação automática.

## Página de diagnóstico

`/classroom --probe` conserva a prova mínima com `ClassroomProbePage`, `CustomPageLifetime.CanDismiss` e estilos nativos de `Common.ui`. O contador é independente por jogador/abertura, atualizado por `TextSpans`, sem persistência; eventos desconhecidos recebem confirmação vazia. A página de diagnóstico não participa de turmas, entregas ou notas. O [registro de validação](validation.md) conserva a confirmação original do cliente.

## Progresso e formulários — rc.17

`LearningProgress` concentra as projeções de metadados usadas por `SqliteClassroomRepository` e `LearningStore`. A lista docente usa uma conexão e até três SELECTs; selecionar um aluno acrescenta um SELECT paginado (até quatro), validando vínculo e lendo atividades/últimas entregas/últimas notas no mesmo snapshot diferido. Subconsultas usam os índices existentes; não há cache global, nova migração ou consulta por aluno. A contagem considera atividades distintas publicadas não arquivadas, independentemente do número de revisões/publicações. Notas usam o valor da publicação efetivamente entregue.

`ClassroomRuntime.LearningChange` distingue catálogo, entrega, avaliação e outras mudanças. `listenProgress` recebe as três primeiras, com filtro por turma, após o commit; assinaturas continuam removidas ao fechar/desconectar. O canal administrativo/MOTD não recebe essas invalidações.

`LearningTruthTable` mantém as respostas locais e `LearningTableRenderer` cria no máximo oito linhas/32 dropdowns nativos. Paginação, seções e seletores de data não consultam SQLite. Formulários novos permanecem locais até salvar; a primeira gravação/publicação é atômica. `LearningEditorSnapshot` compara os campos exibidos antes de sair; `CantClose` evita descarte por Esc durante edição. Revalidação de autorização continua limpando campos e invalidando callbacks antigos.
