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

O aceite final exige o [reteste rc.8](classroom-acceptance.md#roteiro-administrativo), incluindo primeira troca de aba e seleção após atualizar listas. Para estimar capacidade de produção, repetir com a base, hardware, proporção de interfaces abertas, rede e carga de circuitos reais; os números isolados servem para comparar os caminhos medidos.

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

O ganho corresponde à redução de abertura/configuração de conexões e de SELECTs repetidos. A fila continua única e limitada; tempo de rajada inclui sua espera, não o tick do mundo. O journal DELETE pode fazer um escritor aguardar o término de um snapshot de leitura; os testes exercitam aprovação, saída e encerramento concorrentes, sem WAL, pool, cache ou paralelismo novo. Histórico volumoso e duração prolongada não foram simulados. A variação de máquina/JIT/filesystem continua relevante, e os resultados não certificam capacidade de jogadores nem resolvem por evidência a demora visual da primeira troca de aba. Posteriormente, a [rodada breve de cliente rc.10](evidence/classroom/2026-10-26-rc10-client.md) confirmou resposta instantânea nas abas e os outros quatro cenários propostos. As [pendências complementares](classroom-acceptance.md#roteiro-administrativo) permanecem; o relato não mede tempos nem capacidade.

## Classroom — áreas e cópias rc.11

Build `1.4.0-rc.11+31e3c78c7881`, JAR `140d80eae81a91206567b120df4315a87de6ecd1991f656817f5b7e09b7a0c14`. Duas execuções nativas isoladas no mesmo host Windows/Java 25: [dados completos e escopo](evidence/classroom/2026-10-26-rc11-performance.json). A segunda acrescentou medição de arquivo/alocação ao harness, mantendo o JAR idêntico. Não são percentis, médias de produção ou comparação de capacidade.

| Operação/caso | Amostras observadas |
| --- | --- |
| Captura pequena, 9 × 2 × 12 posições | 1,67–2,12 ms |
| Colagem pequena | 2,11–2,30 ms |
| Arquivo pequeno | 687 bytes comprimidos; gravar/reabrir 34,25–40,49 ms fora do mundo |
| Captura máxima, 32 × 16 × 32 preenchida com construção | 34,62–55,72 ms no contexto do mundo |
| Colagem máxima de construção | 42,12–72,31 ms |
| Descarte máximo de construção | 31,71–88,88 ms |
| Arquivo máximo de construção | 1.093.689 bytes JSON, 36.521 bytes comprimidos; gravar/reabrir 312,64 ms fora do mundo |
| Pacote máximo de preview | 278.590 bytes antes da compressão do protocolo; enviado somente à revisão selecionada |
| Alocação no thread do mundo, captura + pacote + colagem máximos | 12.283.672 bytes alocados na segunda amostra; não é memória retida nem pico de heap |
| Captura/colagem de volume máximo com 8.192 componentes e 8.192 apoios | 19,91–28,50 / 38,41–58,51 ms |
| Última avaliação elétrica com a cópia ativa | 16,34–22,03 ms; 8.239 nós no mundo, incluindo 47 do fixture anterior |

A cópia densa respondeu ao acionamento nos ticks reais e foi descartada. O delta líquido de heap variou de zero a 12.247.608 bytes entre amostras e pode ser afetado por GC; **zero não significa ausência de alocação**. A medição por thread acrescenta uma observação mais útil, mas não representa todo o processo, compressão em outro thread, recursos nativos ou pressão de todos os circuitos de um servidor.

Áreas máximas podem causar picos que ultrapassam um tick; tamanho, estado do JIT, disco e simulação alteram o custo. O ensaio de primitivas não inclui latência do jogador nem o custo de todos os eventos de plugins externos de proteção. A fila mantém uma operação pesada ativa + oito aguardando; regressões de rajada verificam serialização, recusa de duplicata/saturação e encerramento de pedidos, sem estimar jogadores suportados. Metadados pedagógicos usam canal separado para não recarregar a administração/MOTD a cada nota/entrega.

Começar o aceite por uma área pequena. Os limites de dimensão, bytes e cota são configuráveis; reduzi-los é uma opção para servidores com uso intenso. Se os testes reais indicarem picos frequentes, o próximo refinamento é fracionar colagem/descarte com revalidação, preservando reserva e recuperação. O arquivo de entrega permanece imutável. A prova não autoriza afirmar “sem lag” ou aumentar os limites com base apenas no tamanho comprimido.

## Verificação assistida — rc.15

`SubmissionVerifierCostTest` mede trabalho puro sobre a topologia entregue, em três rodadas no mesmo processo/JVM. Relatório regenerável `build/reports/submission-verification.json`; cópia histórica na [evidência da candidata](evidence/classroom/2026-10-26-rc15-server.json). Build `1.4.0-rc.15+d57974299eb7`, SHA-256 `14f00c9b0b9776b4283123d87ff2fdbb825d019cd60c5b9b5fd25d8784da3213`.

- Pequeno: 7 componentes lógicos, 4 combinações, 0,489–0,755 ms.
- Limite comportamental: 2048 componentes lógicos, 6 alavancas/4 lâmpadas, 64 combinações, 149,844–160,516 ms; aproximadamente 67,4 MB alocados cumulativamente por operação, sem medir memória retida/pico.
- Contagem sem tabela não roda o avaliador. Simulação acima do limite retorna erro explícito, mantendo entrega/correção manual.

São amostras locais, sem limiar artificial, não medição de rede, cliente, espera na fila ou capacidade de jogadores. A execução real inclui leitura limitada do arquivo/checksum, resolução de assets e revalidação de autorização antes/depois. Usa o único trabalhador pesado existente com fila de oito; não roda a cada atualização da UI, em ticks ou ao salvar nota. Não acrescenta SELECTs à montagem do resumo administrativo. A CI portável também executa a regressão de custo, mas suas máquinas/arquivos não substituem a identidade desta medição.

## Candidata rc.17 — comparação administrativa

A [comparação local rc.16/rc.17](evidence/classroom/2026-10-27-rc17-performance.json) usou o mesmo `ClassroomBenchmark`, três JVMs novas por JAR, ordem alternada, Windows/Java 25. Classpath substituiu as classes/recursos de produção pelo JAR escolhido e conservou o mesmo harness de teste/dependências. Build IDs confirmam os bytes selecionados. Cada caso aquecido usa dez aquecimentos e 25 amostras; a tabela mostra a mediana dos três resultados de cada versão.

| Consulta | rc.16 mediana / p95 (ms) | rc.17 mediana / p95 (ms) |
| --- | --- | --- |
| Membros | 1,4968 / 1,7158 | 1,6285 / 1,8986 |
| Ingressos | 1,4881 / 1,7500 | 1,6289 / 1,8774 |
| Jogadores | 1,3909 / 1,5144 | 1,4465 / 1,6488 |
| Administração | 1,1278 / 1,2979 | 1,2668 / 1,5399 |

Houve aumento pequeno de custo absoluto nesse ensaio; não é uma alegação de otimização de tempo. Todas as rajadas de 25/100/250 consultas terminaram sem falhas nas seis execuções. A fixture administrativa congelada tem 25 membros e **nenhuma atividade publicada**: não mede custo com muitas entregas. Progresso com publicações/reenvios/notas é verificado nos testes `ClassroomReadTest`, incluindo uma conexão/até três SELECTs e quatro na ficha, privacidade e snapshot consistente. A tabela visual cria no máximo oito linhas/32 dropdowns; interações locais não consultam o banco. Nenhuma capacidade de jogadores, FPS ou latência de cliente é inferida.

A atualização idêntica de vinte linhas manteve **zero comandos, zero bindings e 59 bytes de confirmação** nas seis execuções. A seleção agora envia também os novos estados de acompanhamento; esse caso congelado não inclui consulta de ficha com entregas.
