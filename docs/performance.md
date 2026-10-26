# Medições de desempenho

## Reproduzir e interpretar

```powershell
.\gradlew.bat benchmarkCircuit
.\gradlew.bat benchmarkServer
```

Ambos usam dez aquecimentos e 25 amostras por cenário, mediana/p95 e bytes alocados na thread medida quando a JVM oferece esse recurso. Não há limiar de aprovação de desempenho no CI: máquinas, JIT e carga variam. Asserções funcionais impedem medir cenários vazios ou uma lâmpada que não acompanha a fonte.

`benchmarkCircuit` mede somente o avaliador Java: linhas, grades, cadeias das sete portas e redes independentes em 100, 1.000 e 10.000 componentes principais. Reconstrói o grafo em cada amostra; exclui Hytale, jogadores e renderização. Grava `build/reports/benchmarks/circuit.json`.

`benchmarkServer` exige `Assets.zip`, aceita `HYTALE_ASSETS` ou `-PhytaleAssets` como `fullCheck` e usa um universo descartável próprio. Mede blocos, seções e holders reais do servidor 0.7.4; cronometra separadamente o monitoramento em repouso e alternância de fonte/coleta/solução/publicação visual. A matriz varia redes independentes, alteração de uma única fonte, grade de 1.024 cabos e 128 bancadas não elétricas. Os nós, tipos e holders são conferidos e registrados; não basta cronometrar uma chamada sem circuitos.

As séries nativas executam dentro de uma tarefa na thread do mundo. Incluem APIs reais de blocos, mas **não** a espera até o próximo tick, trânsito de rede, jogadores, física entre amostras, renderização ou concorrência de outras tarefas. São uma referência controlada de custo, não um ensaio completo de servidor com alunos. Relatórios ficam em `build/reports/server-integration/<execução>/selftest-benchmark.json` e `summary.json`, com build e SHA-256 do JAR. O workflow nativo manual executa também esse benchmark; a CI Windows/Linux mantém o benchmark puro sem exigir assets licenciados.

## Referência local de 23/10/2026

Windows 11, Java 25.0.3+9-LTS, 12 processadores disponíveis, heap de 2 GiB, sem clientes. Horários UTC nos JSONs podem cair em 24/10; esta data usa America/Sao_Paulo.

Avaliador: ID `1.3.3+321dbcd773a9`. Valores em milissegundos; fontes/consumidores podem acrescentar um ou dois nós.

| Cenário | Componentes principais | Mediana (ms) | p95 (ms) |
| --- | --- | --- | --- |
| `cable-line` | 100 | 0.543 | 0.883 |
| `cable-grid` | 100 | 0.352 | 0.503 |
| `mixed-gate-chain` | 100 | 0.521 | 1.040 |
| `independent-networks` | 100 | 0.294 | 0.460 |
| `cable-line` | 1000 | 1.035 | 1.479 |
| `cable-grid` | 1000 | 0.743 | 1.635 |
| `mixed-gate-chain` | 1000 | 1.882 | 2.626 |
| `independent-networks` | 1000 | 0.952 | 1.392 |
| `cable-line` | 10000 | 5.293 | 6.481 |
| `cable-grid` | 10000 | 3.263 | 3.828 |
| `mixed-gate-chain` | 10000 | 12.074 | 13.510 |
| `independent-networks` | 10000 | 5.426 | 7.397 |

Servidor: ID `1.3.3+321dbcd773a9`, execução `20261023-231935-16e847`. O cenário manteve 220 seções carregadas. As bancadas acrescentam 128 holders reais não elétricos, mantendo 1.025 nós lógicos.

| Cenário | Nós | Monitor mediana / p95 (ms) | Alternar e avaliar mediana / p95 (ms) |
| --- | --- | --- | --- |
| `loaded-empty-sections` | 0 | 0.086 / 0.142 | 0.104 / 0.135 |
| `independent-networks-16` | 64 | 0.208 / 0.278 | 1.715 / 2.417 |
| `independent-networks-128` | 512 | 0.389 / 0.653 | 3.637 / 4.633 |
| `one-source-in-128-networks` | 512 | 0.107 / 0.117 | 0.993 / 1.991 |
| `connected-grid-1024` | 1025 | 0.122 / 0.156 | 1.822 / 2.750 |
| `grid-with-128-unrelated-benches` | 1025 | 0.156 / 0.234 | 1.890 / 2.784 |

## Mudança medida e limites

O monitoramento antigo reconstruía mapas de holders em todo tick, inclusive sem alterações. A versão 1.3.3 compara os vínculos do índice anterior, reaproveita o snapshot em repouso e usa chaves primitivas. Somente mudanças reais alocam outro índice; troca de holder na mesma posição, retirada/retorno e seções vazias continuam detectadas.

A tabela compara alocações por chamada de monitoramento na mesma máquina e matriz. Antes: `1.3.3+eee3c401535b`, execução `20261023-231332-39a89c`; depois: ID acima. São duas execuções sequenciais, não uma garantia de tempo sob toda carga. Não houve mudança no avaliador booleano.

| Cenário | Antes (bytes) | Depois (bytes) |
| --- | --- | --- |
| `loaded-empty-sections` | 24879 | 14233 |
| `independent-networks-16` | 35276 | 21583 |
| `independent-networks-128` | 114264 | 73544 |
| `one-source-in-128-networks` | 88664 | 47944 |
| `connected-grid-1024` | 113840 | 38752 |
| `grid-with-128-unrelated-benches` | 116912 | 41824 |

Na grade de 1.024 cabos, a alocação observada no monitoramento caiu aproximadamente 66%. Continua havendo trabalho por seção/porta e a invalidação ainda pode reconstruir todos os circuitos carregados do mundo. O caso de uma fonte entre 128 redes explicita esse custo global; não introduz atualização por região.

`/logicenergy` apresenta monitoramento e avaliação completa/solver numa janela limitada a 256 amostras por mundo. Consultar métricas não grava dados. O teste multiplayer do usuário aprovou funcionamento, sem instrumentação que permita inferir quantidade máxima de alunos.

Antes de mudar o escopo de invalidação, medir vários jogadores, mundos e construções maiores, incluindo custo em repouso e alternância contínua. Preservar regressões de orientação, ciclos, holders, seções vazias e reconstrução. Os relatórios são regeneráveis; comparar versões nas mesmas condições.

## Classroom — consultas, notificações e transporte

Reproduzir com `.\gradlew.bat benchmarkClassroom` (Linux: `bash ./gradlew benchmarkClassroom`). O programa cria um diretório novo e banco sintético em `build/reports/benchmarks/classroom-benchmark-data/<UUID>/`, pelos serviços existentes; não abre o banco real nem cria jogadores conectados. O JSON é `build/reports/benchmarks/classroom.json`. Mede consultas a uma turma com 25 membros, rajadas de 25/100/250 consultas, 10/100/500/1.000 assinaturas sintéticas e comandos/tamanho dos pacotes nativos. Usa 25 amostras aquecidas nos casos repetidos, com estado de permissões estabelecido antes das contagens; rajadas e primeiras consultas têm medição própria. Não há limiar de aprovação por tempo no CI.

A [linha de base rc.7](evidence/classroom/2026-10-24-rc7-performance.json), build `1.4.0-rc.7+5294934c944b`, foi medida localmente em Windows/Java 25. As primeiras consultas após a inicialização ficaram entre 3,3 e 13,3 ms; medianas aquecidas entre 3,4 e 5,5 ms. Uma rajada de 250 consultas chegou a p95 de aproximadamente 1,07 s. Isso demonstra espera na fila de I/O sob rajada, não duração do tick do mundo.

A rc.7 reenviava 182 comandos e 20 bindings (17.613 bytes) numa atualização idêntica de lista com 20 linhas. Um evento de grupo com 1.000 assinaturas percorria os assinantes repetidamente, com mediana de aproximadamente 8,6 ms e 1.000 callbacks mesmo sem mudança efetiva. Uma alteração sem relação com as outras páginas também alcançava todos os callbacks antes de filtrarem no mundo. São cenários sintéticos com callbacks simples; não representam o custo completo de mil clientes.

A rc.8 corrige os três desperdícios: índice por UUID e comparação de autorização efetiva, filtro imutável antes do agendamento no mundo, agrupamento limitado de rajadas e transporte incremental com linhas/bindings conservados. A [medição rc.8](evidence/classroom/2026-10-24-rc8-performance.json), build `1.4.0-rc.8+e3a834923f93`, preserva os resultados finais locais:

| Caso sintético | rc.7 | rc.8 |
| --- | --- | --- |
| Atualização idêntica, 20 linhas | 182 comandos, 20 bindings, 17.613 bytes | 0 comandos, 0 bindings, confirmação de 59 bytes |
| Grupo sem mudança efetiva, 1.000 contas abertas | 1.000 callbacks; mediana 8,615 ms | 0 callbacks; mediana 0,032 ms |
| Grupo com mudança efetiva, 1.000 contas abertas | 1.000 callbacks; mediana 7,996 ms | 1.000 callbacks; mediana 0,211 ms |
| Mudança não relacionada às outras 999 páginas | 1.000 callbacks | 1 callback |
| Rajada de 250 consultas de membros, p95 | 1.067,448 ms | 980,587 ms |

Os callbacks de permissões são contadores simples no ensaio: tempos de despacho não incluem consultar/reconstruir mil páginas reais. A redução do pacote vale para atualização sem mudança; uma primeira consulta, troca de contexto ou lista alterada continua enviando os dados necessários. O caso de seleção isola a alteração dos detalhes preservando campos (`resetFields=false`); o clique real também repõe os valores dos campos, custo adicional intencional que não foi incluído nesse caso. O transporte reduz bytes e reconstruções no cliente; sua comparação de estado acrescenta trabalho Java e nem todo p95 de montagem diminuiu. Nenhum ganho uniforme de CPU/FPS é inferido dessa medição.

A fila de I/O continua demonstrando espera sob rajada. Seu algoritmo e as consultas SQL não mudaram; a diferença de tempo entre essas duas execuções não é uma otimização de banco comprovada. Inicialização e primeiras consultas possuem amostra separada e podem variar com JIT, filesystem e carga da máquina. As duas versões tiveram zero falhas nas rajadas de 25/100/250 pedidos medidas. A CI executa o mesmo harness em Windows/Linux, com relatórios separados que não devem ser comparados como se fossem a mesma máquina. O cache de transporte pertence à página, guarda somente suas propriedades/linhas atuais e é liberado com a instância; assinaturas são removidas ao fechar/desconectar/encerrar. Não cresce com cada atualização nem executa trabalho periódico por jogador em repouso.

### Limites e impacto prolongado

- A espera visual relatada de alguns segundos no host e 1–2 s no notebook ocorreu na primeira interação de cada personagem. Os ensaios não a reproduzem e não medem carregamento inicial de recursos do cliente, JIT do cliente, rede, FPS ou competição entre cliente e servidor no host. Não atribuir esse atraso automaticamente ao canal de permissão.
- O banco usa fila dedicada única e limitada a 256 operações pendentes. Mais pedidos simultâneos podem aumentar a espera ou receber indisponibilidade; assinaturas abertas não equivalem a esse volume de consultas simultâneas. A consulta não roda na thread do mundo, mas construir/publicar UI e despachar callbacks tem custo no contexto apropriado.
- Mudanças reais de autorização ou dados continuam podendo atingir várias páginas. Descoberta e busca de jogadores podem precisar de atualização ampla quando o diretório muda. O benchmark não certifica capacidade de jogadores, TPS ou desempenho com dezenas de milhares de registros.
- Histórico cresce por decisão do MVP. Índices existentes restringem vínculos por turma/estado e processos por aluno/instante; buscas textuais públicas e ordenações ainda precisam de medição com base representativa caso cresçam muito. Não foi introduzida remoção de histórico nem migração para otimizar uma carga hipotética.
- A atualização automática de permissões é por evento e só interessa às páginas abertas. Retirá-la não elimina a consulta e a renderização de uma troca de aba. As otimizações mantêm o fluxo automático já confirmado, com autorização real revalidada em cada operação.

O aceite final exige o [reteste rc.8](classroom-rc8-acceptance.md), incluindo primeira troca de aba e seleção após atualizar listas. Para estimar capacidade de produção, repetir com a base, hardware, proporção de interfaces abertas, rede e carga de circuitos reais; os números isolados servem para comparar os caminhos medidos.

## Classroom — leituras agregadas rc.10

Comparação local em Windows 11/Temurin Java 25.0.3, em 26/10/2026, com o mesmo harness e fixture existentes, sem alterações desde a rc.9. Cada versão foi executada em três JVMs novas, com 25 membros, dez aquecimentos e 25 amostras por consulta repetida; cada rodada também mede rajadas de 25/100/250 pedidos. O JAR selecionado vem primeiro no classpath, sem diretórios de produção ou outro driver SQLite. As três rodadas da linha de base precedem as três da candidata, sem build em paralelo durante as medições.

O [registro completo das seis rodadas e contagens JDBC](evidence/classroom/2026-10-26-rc10-database-performance.json) identifica arquivos, checksums, instantes UTC, protocolo e limites. A linha de base é o JAR rc.9 já entregue, build `1.4.0-rc.9+e16132240905`, SHA-256 `395c4b5bec09337c1428c20e9b14d9053f13970aba58c750d86c81dce6c4d8b9`, preservado sem recompilação. A candidata é `1.4.0-rc.10+86bb2076e27f`, SHA-256 `471c5b66fcde9146b4b18006a9ec2f7f304902f9b9033c3c3b8b2987ca0ad657`, o mesmo arquivo do `fullCheck` e da integração nativa.

| Leitura | Conexões rc.9 → rc.10 | SELECTs rc.9 → rc.10 |
| --- | --- | --- |
| Home de professor/aluno | 3 → 1 | 3 → 1 |
| Membros, ingressos e jogadores do professor | 5 → 1 | 7 → 3 |
| Administração do professor | 4 → 1 | 5 → 2 |
| Ficha do aluno | 3 → 1 | 3 → 1 |
| Descoberta/histórico sem vínculo | 2 → 1 | 2 → 2 |

Contadores ficam apenas nos testes e medem conexões JDBC e comandos SELECT externos, sem contar PRAGMAs ou subconsultas como comandos separados. Todas as conexões observadas fecharam, sem conexões ativas ao terminar. Cada `Screen` da candidata também registra um `BEGIN DEFERRED` e um commit; as falhas têm regressão de rollback/fechamento. `Home` usa um único SELECT sem transação explícita adicional. A meta de uma conexão/três SELECTs para membros foi atingida.

| Cenário (ms) | rc.9 | rc.10 | Redução observada |
| --- | --- | --- | --- |
| Membros, mediana | 5,086 | 1,336 | 73,7% |
| Ingressos, mediana | 4,776 | 1,386 | 71,0% |
| Jogadores, mediana | 4,506 | 1,243 | 72,4% |
| Administração, mediana | 3,381 | 1,006 | 70,2% |
| Rajada 25, p95 | 112,138 | 29,187 | 74,0% |
| Rajada 100, p95 | 435,762 | 110,498 | 74,6% |
| Rajada 250, p95 | 993,748 | 265,538 | 73,3% |

A tabela usa a mediana das três medianas por consulta ou dos três p95 por rajada; não mistura amostras nem representa um intervalo de confiança. As medianas de membros variaram entre 5,018–5,619 ms na rc.9 e 1,333–1,647 ms na rc.10. O p95 da rajada 250 variou entre 983,149–1.008,082 ms e 261,420–293,123 ms. Houve zero falhas nas três rajadas de cada uma das seis rodadas. JSONs e logs completos ficam em `build/reports/benchmarks/rc10-comparison/`; o log do `fullCheck` fica em `build/tmp/rc10-fullcheck.log`.

O ganho corresponde à redução de abertura/configuração de conexões e de SELECTs repetidos. A fila continua única e limitada; tempo de rajada inclui sua espera, não o tick do mundo. O journal DELETE pode fazer um escritor aguardar o término de um snapshot de leitura; os testes exercitam aprovação, saída e encerramento concorrentes, sem WAL, pool, cache ou paralelismo novo. Histórico volumoso e duração prolongada não foram simulados. A variação de máquina/JIT/filesystem continua relevante, e os resultados não certificam capacidade de jogadores nem resolvem por evidência a demora visual da primeira troca de aba. A [regressão de cliente rc.10](classroom-rc10-acceptance.md) permanece pendente; as pendências anteriores também são preservadas.
