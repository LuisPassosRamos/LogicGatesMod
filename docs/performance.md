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
