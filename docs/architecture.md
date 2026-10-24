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

`SqliteClassroomRepository` usa `classroom.db` no diretório de dados do plugin, esquema v1 com `application_id`, tabelas de turmas/vínculos, índices únicos e chaves estrangeiras. `BEGIN IMMEDIATE` protege criação/vínculo e atualização por revisão entre conexões independentes. O banco usa journal DELETE padrão; backup/restauração exigem encerramento limpo e estão descritos no guia Classroom. Ainda não existe esquema de matrícula ou operação de ingresso de alunos.

A fila dedicada inicializa o repositório antes de atender requisições e preserva os sistemas elétricos se Classroom falhar. O encerramento drena a fila por até cinco segundos e resolve pendências como indisponíveis se for interrompido. Cada callback volta à thread do mundo e verifica referência, Store, mundo e identidade da página ativa antes de publicar; não há I/O no tick. Fechar a página não cancela uma gravação já aceita.

O driver JDBC é empacotado por uma configuração Gradle exclusiva, com recursos nativos e licenças; a dependência Hytale não é incorporada. JUnit carrega o JAR de produção em um classloader isolado e executa uma consulta real. A matriz CI roda essa verificação em Windows/Linux. A integração nativa usa o serviço real e confere a turma após reinício; o formulário continua dependente de validação no cliente.
