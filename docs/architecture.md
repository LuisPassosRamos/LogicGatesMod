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
| `util/BuildInfo` | Identificação da compilação empacotada |
| `interactions/` | Acionamento da alavanca |
| `commands/` | Ajuda `/logichelp`, inspeção `/logicinspect`, diagnóstico `/logicenergy` e abertura de `/classroom` |
| `classroom/ui/` | Páginas Custom UI; estado por abertura e retorno assíncrono à sessão atual |
| `classroom/domain/` | Serviço, contratos e regras educacionais sem dependência de Hytale |
| `classroom/persistence/` | SQLite compartilhado pelo servidor, esquema versionado e transações |
| `classroom/ClassroomRuntime` | Fila única de I/O, inicialização e encerramento dos serviços |

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

## Classroom — incremento 0 (1.4.0-rc.1)

`ClassroomCommand` usa `AbstractPlayerCommand`, que despacha no contexto do mundo do jogador. A prova original está em `/classroom --probe`, com `ClassroomProbePage` e `CustomPageLifetime.CanDismiss`. O documento fica em `Common/UI/Custom/Pages/LogicGates/ClassroomProbe.ui` e reutiliza estilos nativos de `Common.ui`.

Bindings enviam somente `Action` pelo codec próprio da página. O contador é mantido pelo servidor e a atualização altera `#Counter.TextSpans` por mensagem traduzida, sem reconstruir o documento ou os bindings. Eventos desconhecidos recebem atualização vazia para liberar a interface; fechamento/dispensa encerra a instância. Cada jogador/abertura tem estado independente e não persistente.

Os testes capturam o transporte e exercitam builders/codecs reais. O usuário confirmou o fluxo central em pt-BR na rc.1; os demais casos continuam no [roteiro](classroom.md). O núcleo de circuitos permanece independente.

## Classroom — incremento 1 (1.4.0-rc.2)

`/classroom` abre `ClassroomPage`, que lê os dados pelo `ClassroomRuntime` compartilhado pelo plugin. Criação e edição passam por `ClassroomService`; o predicado de autorização consulta a permissão nativa atual, inclusive em gravações de uma página já aberta. O domínio não importa Hytale. UUID, turma e revisão são obtidos da sessão, nunca de campos enviados pelo cliente.

`SqliteClassroomRepository` usa `classroom.db` no diretório de dados do plugin, esquema v4 com `application_id`, tabelas de turmas/vínculos/ingressos, índices únicos e chaves estrangeiras. `BEGIN IMMEDIATE` protege criação/vínculo e atualização por revisão entre conexões independentes. O banco usa journal DELETE padrão; backup/restauração exigem encerramento limpo e estão descritos no guia Classroom. Matrícula e ingresso são descritos a seguir; as migrações mantêm os dados das versões anteriores.

A fila dedicada inicializa o repositório antes de atender requisições e preserva os sistemas elétricos se Classroom falhar. O encerramento drena a fila por até cinco segundos e resolve pendências como indisponíveis se for interrompido. Cada callback volta à thread do mundo e verifica referência, Store, mundo e identidade da página ativa antes de publicar; não há I/O no tick. Fechar a página não cancela uma gravação já aceita.

O driver JDBC é empacotado por uma configuração Gradle exclusiva, com recursos nativos e licenças; a dependência Hytale não é incorporada. JUnit carrega o JAR de produção em um classloader isolado e executa uma consulta real. A matriz CI roda essa verificação em Windows/Linux. A integração nativa usa o serviço real e confere a turma após reinício; o formulário continua dependente de validação no cliente.

## Classroom — núcleo de ingresso e administração

O domínio usa DTOs imutáveis distintos para resumo público, vínculo, processo de ingresso e entrada por papel. A permissão docente continua sendo consultada por operação; o repositório revalida responsabilidade e estado na transação. Solicitação e convite convergem para conferência antes da criação do vínculo. A primeira aprovação (ou criação de turma docente) encerra as outras pendências do UUID.

Esquema v2 acrescenta diretório, processos e matrícula/histórico; v3 acrescenta mensagem. A migração da v1 preserva IDs/revisões e cria um backup consistente por VACUUM INTO antes da transação. Matrícula ativa é única por turma com comparação BINARY; pendências não reservam matrícula. Falhas de migração não substituem o banco. Verificações auditam também a definição dos índices únicos.

A fila de I/O possui limite de 256 operações pendentes. Notificações internas carregam IDs, sem dados privados, e são emitidas após commit; cada página filtra os contextos relevantes e retorna ao mundo antes de atualizar. Os testes de núcleo cobrem concorrência, rollback, idempotência, privacidade, revogação, migração e restauração. A rc.3 liga esses fluxos à página, com consultas por papel e recursos nativos. A validação no cliente permanece pendente.

A página mantém apenas seleção, revisão apresentada, contexto de consulta e confirmação por abertura. Atualização automática não envia valores aos campos; seleção/recarregamento explícitos adotam a nova revisão. As linhas transportam UUIDs validados contra o snapshot e a autorização é conferida novamente no serviço. Listas possuem paginação de 20 registros, sem capacidade de turma imposta.

ClassroomPresence registra PlayerReadyEvent/PlayerDisconnectEvent 0.7.4. O primeiro ready de cada conexão registra o nome autenticado e consulta a mensagem privada. A identidade da conexão evita repetição entre mundos; revisões de sessão invalidam mensagens enfileiradas após mudanças de vínculo/turma. Shutdown/desconexão removem listeners. O adapter usa componentes atuais, sem a API Player.getPlayerRef depreciada.

## Classroom — polimento e autorização na rc.5

ClassroomScreenRenderer monta comandos/bindings e mensagens literais a partir dos DTOs existentes; não executa operações de domínio. A coluna de detalhes separa conteúdo rolável de ações e matrícula. Estimativas conservadoras de altura consideram a região e os caracteres; fontes e renderização final continuam dependentes do cliente.

ClassroomPermissions recebe os eventos concretos PermissionsAdded/PermissionsRemoved, PlayerGroupEvent.Added/Removed e GroupPermissionChangeEvent.Added/Removed emitidos pela API nativa depois de alterar o provedor. O runtime reavalia a permissão efetiva por UUID em canal separado dos commits. Alterações em permissões de grupo reavaliam os assinantes das páginas abertas, sem varrer mundos nem consultar periodicamente. Não há persistência ou reenvio de mensagem do dia nesse canal.

ClassroomPageAccess registra gerações atomicamente antes de agendar a thread do mundo. Consultas e respostas de operações anteriores a uma invalidação não podem repovoar a UI; uma reconsulta obtém o estado atual. Revogações são lembradas mesmo que haja concessão antes de processar a fila: rascunhos docentes são descartados e os dados persistidos são carregados novamente. Quando a autorização não muda, o estado de edição e sua revisão permanecem preservados. As verificações de conexão, Store, mundo e página ativa e as regras de autorização de cada operação continuam necessárias. Fechar/desconectar/encerrar remove as assinaturas.

## Classroom — orientação e contrato na rc.7

A orientação padrão considera a seção, o papel e a ausência de seleção/linhas. Páginas posteriores vazias oferecem Anterior e usam o texto de página vazia, sem inferir ausência total de alunos. Uma seleção mantida durante atualização conserva a orientação da ficha. Confirmações e mensagens de atualização permanecem prioritárias no renderer; o adapter conserva mensagens explícitas de erro, gravação e resultado. Nenhuma estrutura de layout, evento, API pública ou regra de domínio muda.

O [contrato de atividades](roadmap/classroom-learning-contracts.md) está aprovado para implementação posterior. Prevê publicação versionada, prazo, valor e arquivamento; não corresponde a tipos, tabelas ou serviços existentes na rc.7. A nota ausente é representada conceitualmente por —/valor, sem persistir nota zero fictícia. A rc.7 conservava o esquema v3; o esquema atual é v4 desde o encerramento administrativo na rc.9.

## Classroom — notificações e transporte na rc.8

Assinaturas de autorização são agrupadas por UUID; alterações individuais consultam somente o grupo correspondente e mudanças de grupo percorrem as contas abertas uma vez. O último resultado efetivo evita callbacks redundantes; é um controle de notificação, não um cache de autorização do domínio. Cada operação continua consultando a permissão atual no servidor.

O canal de dados aceita um predicado de contexto antes de agendar trabalho no mundo. Páginas publicam snapshots imutáveis de interesse; durante I/O retêm um lote limitado de mudanças e conferem sua relevância após aplicar a resposta. Rajadas agendam no máximo uma tarefa pendente por página/canal, sem perder os contadores de revogação. Isso evita reconsultas por eventos sem relação com o contexto resultante. Presença mantém seu assinante sem filtro. A conferência de contexto e sessão no mundo continua necessária.

ClassroomPageUpdates conserva somente o último estado enviado de uma página aberta. Registros iguais mantêm elementos e bindings nativos; registros alterados recebem reconstrução completa e invalidação das propriedades das linhas. Sets intermediários são compactados antes da comparação. Valores `.Value` nunca são deduplicados, pois o cliente pode ter editado localmente. Respostas vazias também são enviadas: PageManager 0.7.4 exige acknowledgements e bloqueia entrada enquanto eles estão pendentes. Estado de transporte não substitui validação de identidade, revisão, vínculo ou permissão. Não há temporizador de polling, novo mundo ou esquema de banco.

### Encerramento administrativo (rc.9)

O esquema v4 reconstrói somente `classrooms`, preservando IDs e referências; `closed_at` registra o encerramento e um índice parcial limita `teacher_uuid` às turmas abertas. A inicialização usa o procedimento transacional de reconstrução SQLite com foreign keys desativadas apenas nessa conexão de migração, verifica referências antes de commit e reativa a validação depois. Bancos anteriores identificados recebem backup consistente antes da migração.

`disband` exige permissão no serviço e valida dono, UUID e revisão em `BEGIN IMMEDIATE`. Atualiza turma, vínculos ativos de ambos os papéis e ingressos abertos na mesma transação; nenhum histórico é apagado. A descoberta e as consultas de turma ativa filtram `closed_at IS NULL`. Uma notificação por turma após commit revalida páginas associadas, incluindo históricos visíveis de candidatos sem vínculo. Não há varredura periódica nem evento por aluno. A confirmação da página usa a revisão original do formulário, não adota silenciosamente revisões de atualizações automáticas e é descartada na troca de contexto ou revogação.

## Classroom — leituras agregadas (rc.10)

`ClassroomService` consulta o predicado de permissão nativa e delega `readHome`/`readScreen` ao repositório. `ClassroomReadPolicy` centraliza a política pura do contexto, normalização de abas e validação de busca/paginação. O contexto agregado une vínculo ativo, turma e nomes do membro/professor; UUID continua sendo a identidade. Um professor sem permissão não recebe o contexto docente; a tela também exige que o vínculo corresponda ao responsável pela turma.

`readScreen` usa uma conexão em `BEGIN DEFERRED`, lê o contexto e reutiliza seu UUID de turma nos helpers de lista/contagem, sem repetir `owned`. Assim, cabeçalho, linhas e total pertencem ao mesmo snapshot, mesmo com outra conexão aprovando, encerrando vínculo ou encerrando turma. `readHome` precisa de um único SELECT e conexão. Commit/rollback e fechamento continuam por try-with-resources. Os métodos granulares anteriores permanecem para seus consumidores, e gravações conservam `BEGIN IMMEDIATE`, revisão, responsabilidade e restrições únicas. Não há pool, WAL, cache entre requisições ou novos executores. O journal DELETE pode fazer um escritor aguardar o término breve de uma leitura; o teste concorrente respeita esse comportamento. O runtime mantém a fila de I/O existente.

A instrumentação JDBC pertence somente aos testes; compara também o JAR rc.9 preservado. O [ensaio de desempenho](performance.md#classroom--leituras-agregadas-rc10) mede as consultas e a fila, sem certificar capacidade de jogadores ou renderização no cliente. Esquema v4 e DTOs consumidos pela UI permanecem iguais.

## Classroom pedagógico — rc.11

`classroom.learning.LearningStore` é a fronteira pura de aplicação/JDBC para rascunhos, versões, revisões, notas e reservas. Revalida contexto e permissão nativa por predicado; grava em `BEGIN IMMEDIATE` e usa revisão otimista para atividade/nota. SQLite v5 preserva a administração v4. `SubmissionFiles` publica conteúdo privado antes de referenciá-lo; startup recupera temporários/órfãos conhecidos. Checksum, expansão limitada e UUIDs impedem caminhos controlados pelo cliente.

`AreaSessions` restringe seleção a conexão/mundo/atividade; o item não concede autoridade. `AreaWorkQueue` limita operações pesadas e separa arquivos/pacotes do acesso a blocos. `NativeAreaAdapter` lê apenas seções carregadas e conserva asset/orientação/intenção. `AreaRuntime` coordena commit e reservas, revalidando o envio ao concluir. Reservas são duráveis antes da colagem; guardas de edição consultam somente interações nativas, sem varredura por tick. Recuperação acompanha disponibilidade natural de seções.

`LearningPage` usa metadados paginados e IDs presentes nas linhas retornadas pelo servidor. Reutiliza o transporte incremental e geração de autorização; respostas antigas não restauram acesso. Atualização normal preserva campos; revogação descarta rascunho docente. O canal pedagógico é separado das invalidações administrativas e da mensagem do dia, evitando recarregar esses painéis a cada nota/entrega. O preview só é carregado para a revisão selecionada e é limpo ao trocar/fechar a página. Nota pertence à revisão e à publicação referenciada; valor monetário em centésimos evita ponto flutuante.

[Contrato e limites](classroom-learning.md). Proteções externas precisam integrar eventos nativos; comandos administrativos/APIs que contornam esses eventos não fazem parte da exclusividade desta reserva.
