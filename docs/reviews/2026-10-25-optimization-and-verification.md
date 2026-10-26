# Revisão de otimização e verificações — 25/10/2026

Base analisada: `codex/classroom-ui`, revisão `b60f71e8522c`, produção **1.4.0-rc.9**, build `1.4.0-rc.9+e16132240905`. Revisão de fontes, registros/entry points, fluxos de leitura e notificação, dependências do Gradle, verificadores, conteúdo do JAR e relatórios já existentes. Não foi repetida a bateria de testes nem iniciado servidor para esta análise; os tempos abaixo são da entrega anterior, na mesma árvore de produção. Não é um teste novo de carga ou uma prova de ausência de defeitos.

## Resultado

A base está adequada ao MVP e tem otimizações úteis, sem classes de produção inteiras aparentemente órfãs na inspeção de referências e registros nativos. Não há evidência que justifique uma reescrita, retirar a atualização automática de permissões ou eliminar testes importantes. Existem pequenas sobras de API interna e oportunidades concretas de reduzir trabalho por consulta e repetições no desenvolvimento.

Produção: 48 arquivos Java, 3.155 linhas. Testes/harnesses/fixtures: 43 arquivos Java, 5.705 linhas. Ferramentas e seus testes: 31 arquivos Python, 2.438 linhas. Esses totais não medem desempenho nem indicam, isoladamente, excesso de código.

## Código sem uso e tamanho da entrega

- Todas as 48 classes Java de produção têm referências em outros fontes de produção; também foram inspecionados os registros do plugin, codecs, callbacks e comandos. Busca textual não comprova sozinha execução, especialmente em APIs com callbacks/reflexão; a conclusão não equivale a um detector formal de código morto.
- `ClassroomRuntime.browse`, `players` e `members` não têm chamadas internas localizadas, incluindo testes e referências de método. São três wrappers de uma linha; a página utiliza `screen`. São candidatos de baixa prioridade para uma próxima limpeza de API, não um gargalo. Não foram removidos nem foi gerado novo JAR para economizar três linhas. Outros métodos do runtime usados pela integração nativa não são órfãos.
- `CircuitNode.cable/lamp` são fábricas usadas pelos cenários de teste; `LogicBlockTypes.isLogicGate` participa da integração nativa. Componentes marcadores e codecs mantêm descoberta/reconstrução e compatibilidade dos saves. Não apagar esses elementos por terem pouco código ou baixa cobertura em JUnit.
- `/classroom --probe` continua acessível e serve para separar transporte Custom UI de banco/regras. É diagnóstico intencional; sua retirada pode ser reavaliada depois da estabilização, não tratada como código morto.
- Interseção das 52 classes compiladas de teste com o JAR rc.9: **zero**. JUnit, benchmarks, plugin de integração e massa sintética não estão no mod instalado. Portanto esses testes não consomem tick ou memória de execução no jogo.
- Os 84 arquivos `.class` de produção, incluindo classes internas, somam aproximadamente 432 KB descompactados. A maior parte do pacote corresponde a assets e SQLite nativo, não a código Java próprio. O driver mantém banco e recursos Windows/Linux necessários; removê-lo ou podar plataformas exige decidir suporte e validar empacotamento.

## Otimização do runtime

### Classroom: pontos positivos

`ClassroomRuntime` usa uma fila dedicada, um worker e limite de 256 operações pendentes: o acesso SQLite não executa no tick do mundo. Operações de gravação preservam transações, autorização atual, responsabilidade, revisão e unicidade. As notificações são posteriores ao commit.

A permissão usa eventos nativos, índice por UUID e comparação da autorização efetiva; não há consulta periódica por jogador. As páginas agrupam tarefas pendentes e descartam respostas antigas. Assinaturas e caches pertencem à conexão/página e são removidos no encerramento apropriado. O transporte conserva linhas/bindings e evita reenviar propriedades idênticas; a confirmação vazia de 59 bytes é necessária ao protocolo, não um envio inútil.

### Classroom: oportunidade principal

Em `ClassroomService.home`, vínculo, turma e nome do professor são lidos separadamente. Para a tela docente de membros, `screen` acrescenta lista e contagem; cada chamada abre/fecha uma conexão em `SqliteClassroomRepository.read`. O caminho normal faz **cinco conexões sequenciais e sete SELECTs**, além dos PRAGMAs de conexão. Não são cinco conexões simultâneas retidas por jogador.

O [benchmark rc.9](../evidence/classroom/2026-10-24-rc9-performance.json) registrou mediana de consulta de membros de aproximadamente **5 ms** e p95 de **1.004 ms** numa rajada sintética de 250 consultas. Não são 250 jogadores reais; a espera está na fila, sem comprovar travamento do mundo. Uma primeira consulta após ready ficou em cerca de 12 ms nesse ensaio, que não reproduz a demora inicial do cliente.

Prioridade futura média: agregar a leitura de uma tela numa conexão/contexto de leitura consistente, preservando filtros de acesso e revisões. Comparar a mesma massa e rajada antes/depois. Não introduzir pool, paralelismo de gravações ou cache de autorização apenas por hipótese: SQLite tem características de serialização e há invariantes concorrentes a preservar.

`ClassroomScreenRenderer` ainda monta comandos de linhas antes de `ClassroomPageUpdates` filtrar o transporte. A redução de bytes não elimina todo o custo de construção no servidor. Evitar a montagem das partes inalteradas é uma segunda hipótese, somente após medir; não deve impedir descarte de texto local na recarga/revogação nem a confirmação de eventos.

Histórico cresce intencionalmente. Busca por substring e ordenações merecem massa maior se o uso real crescer. O limite da fila protege memória, mas picos além da capacidade podem receber indisponibilidade; não há certificação de quantidade máxima de jogadores.

### Circuitos

O solver usa percursos iterativos, redes de cabos compartilhadas e componentes de dependência para tratar ciclos. Não depende de recursão profunda nem de um número arbitrário de tentativas. A simulação não força carregamento de seções e só publica estado visual quando muda. Mudanças são agrupadas por mundo e a avaliação é disparada por invalidação.

Há monitoramento por tick dos holders e topologias de portas carregadas. Ele é necessário porque certos holders/seções vazias não produzem os callbacks esperados pelo índice ECS. Removê-lo como redundância quebraria cenários cobertos pelo teste nativo.

A avaliação completa continua abrangendo os circuitos do mundo afetado; uma alteração pequena em mundo com muitas redes independentes pode ter trabalho maior que o necessário. Monitoramento cresce com os elementos carregados, e a topologia de portas cria objetos temporários. São pontos para futuras medições em servidor representativo. Invalidação por rede/componente pode valer a pena em escala; não é mudança simples nem necessária para aprovar o MVP atual. O [guia de desempenho](../performance.md) distingue solver, monitoramento e publicação. Benchmarks antigos de circuitos não foram apresentados como medições novas da rc.9.

## Necessidade e custo dos testes

A rc.9 possui 834 casos JUnit em 31 suítes. **648** casos vêm de `6 portas × 4 orientações × 27 configurações ausente/0/1`; a suíte correspondente tem 658 casos incluindo regressões adicionais e levou 0,34 s. Cada combinação ainda cobre conexão direta e por cabo. Reduzir essa matriz pouparia muito pouco e eliminaria proteção sobre o comportamento central do mod.

| Grupo JUnit | Casos | Soma dos tempos XML da rc.9 |
| --- | ---: | ---: |
| Circuitos | 710 | 0,474 s |
| Classroom | 83 | 4,346 s |
| Assets, adaptadores e diagnóstico | 41 | 3,885 s |
| Total | 834 | 8,705 s |

São durações dos testes no XML, não o tempo completo do comando Gradle. Inicialização, compilação, empacotamento, Python, regeneração para comparação e servidor têm custos adicionais. A rodada final `fullCheck benchmarkClassroom` da entrega levou 3 min 8 s; os 37 testes Python levaram cerca de 37 s. Não foi feito novo cronômetro nesta revisão.

| Camada | Proteção distinta | Decisão |
| --- | --- | --- |
| Lógica de circuitos | Entradas removidas, orientações, redes, paridade, ciclos, estado imutável | Manter; usar durante mudanças no motor |
| Banco/domínio | Vínculo único, matrícula, concorrência, revisões, rollback, migração v1–v4 e backup | Manter; alto impacto de regressão |
| Permissões e callbacks assíncronos | Revogação, resposta antiga, rascunho, agrupamento e desmontagem de subscriptions | Manter; segurança e coerência da UI |
| Transporte/markup/traduções | Codec, seletores `.Anchor.*`, ordem de parâmetros, campos locais, chaves e parâmetros | Manter; regressões ligadas às falhas reais rc.3/rc.5 |
| Assets Java/Python | Física/rotação/receitas e geometria aprovada versus geração determinística/fontes atuais | Manter; verificações complementares, não cópias exatas |
| Ferramentas e distribuição | JAR real, SQLite nativo, checksum, fontes stale, links no ZIP e limpeza limitada de universo | Manter; evita artefato errado e perda de arquivos |
| Integração nativa | Registro real, API Hytale, holders/seções, publicação, SQLite e reinício | Manter na candidata final e nas mudanças de integração |
| Benchmarks | Comparação de consultas/CPU/alocações/pacotes; sem limiar arbitrário de aprovação | Manter disponíveis; não repetir em cada edição sem relação |
| Cliente | Renderização, cliques, escala/idioma e interação multiplayer | Manter por cenário afetado; automação atual não substitui |

Sobreposição entre camadas tem propósito: um gerador pode produzir bytes coerentes consigo mesmo e ainda uma física errada; o teste independente de geometria/rotação cobre isso. Um codec pode passar no JUnit e falhar ao registrar no servidor; integração cobre a ligação. E nenhuma dessas camadas prova a renderização no cliente.

O JaCoCo local registra cerca de 65,3% das linhas no JUnit; circuitos e persistência têm cobertura alta, enquanto plugin/adaptadores de mundo são exercidos em processo nativo separado, não incluído nessa medição. Não confundir zero cobertura no JUnit com ausência de uso, nem centenas de casos com cobertura total.

## Repetições evitáveis

O principal desperdício identificado é de **frequência e saída**, não de quantidade de cenários:

1. Executar `check`, depois `build`, depois `fullCheck` em invocações separadas repete verificações pesadas. `fullCheck` já depende de build/check/empacotamento. Dentro da mesma invocação, Gradle deduplica uma mesma task no grafo; não foram contadas como duplicadas as dependências compartilhadas.
2. `verifyProject`, `verifyGeneratedAssets` e `testTools` são Exec sem entradas/saídas declaradas para verificação incremental; voltam a rodar entre invocações. Os sete geradores em `--check` reconstroem saídas em memória, incluindo ícones. Não é necessário fazer essa reconstrução após cada edição Java do Classroom.
3. CI atual executa build e os dois benchmarks em todo push/PR, inclusive documentação. Uma futura separação entre validação de documentos e pipeline de produção pode economizar minutos de runner. Não foi alterada ou desabilitada nesta revisão; filtros devem cobrir todos os caminhos de produção/configuração/assets/ferramentas antes de serem adotados.
4. `generateBuildInfo` consulta identidade a cada build por projeto; `write_if_changed` evita reescrever conteúdo idêntico. Revisão/dirty podem alterar bytes mesmo com lógica igual. Não retirar rastreabilidade para ganhar tempo; preservar JAR já testado nas atualizações de evidência.

Recomendação operacional adotada no [guia de desenvolvimento](../development.md#verificação-proporcional-à-alteração): testes direcionados na edição, uma rodada completa no candidato final e nova rodada apenas quando mudança/falha/risco novo justificar. Nenhum teste existente foi eliminado.

## Contexto e tokens

Rodar um teste não implica enviar seu arquivo-fonte ou todo o relatório ao modelo. O que entra na conversa são instruções, arquivos/trechos carregados, chamadas, resultados de ferramentas e respostas. As [orientações oficiais sobre uso](https://developers.openai.com/api/docs/guides/agents-api/observability) incluem resultados de ferramentas no contexto e argumentos/raciocínio na saída. Elas não determinam o custo ou a quota exatos desta conversa no aplicativo.

A [orientação oficial de chamadas programáticas](https://developers.openai.com/api/docs/guides/tools-programmatic-tool-calling) recomenda reduzir resultados intermediários e medir economia junto com correção/completude. Aplicação ao projeto: guardar log completo em arquivo, devolver resumo de aprovação/contagem e abrir trechos de falha quando necessário; procurar símbolos antes de ler arquivos inteiros; reutilizar evidência válida da mesma revisão. Isso pode reduzir contexto e tokens sem perder testes. Não foi medido um percentual de economia.

Eliminar casos rápidos com saída resumida economizaria principalmente milissegundos de CPU e pouquíssimo texto. Evitar rodadas completas desnecessárias, logs extensos e releituras tem benefício muito mais plausível. Economia de CPU/runner/tempo e economia de tokens são métricas diferentes.

## Encaminhamento

1. Agora: conservar testes, JAR rc.9, esquema v4 e regras atuais; adotar execução proporcional e saída resumida.
2. Próxima otimização de runtime, se necessária: leitura agregada de tela, comparada com a linha de base rc.9. Depois avaliar evitar montagem de UI inalterada.
3. Limpeza opcional, de baixo retorno: remover os três wrappers sem consumidores numa futura alteração de código, após confirmar que não há integração externa dependendo deles.
4. CI/incrementalidade: mudança separada com grafo de entradas completo; manter a checagem completa de entrega. Não usar um filtro incompleto para marcar verificações como aprovadas sem executar.
5. Escala de circuitos: medir redes independentes e muitas seções/portas antes de decidir invalidação mais localizada.

O encerramento da turma foi [confirmado por relato nesta data](../evidence/classroom/2026-10-25-rc9-client-report.md). Isso não aprova automaticamente todas as pendências anteriores de idiomas, resoluções, desempenho inicial e edição concorrente.
