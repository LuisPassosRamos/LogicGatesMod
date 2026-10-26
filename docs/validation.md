# Validação e relato de testes

## Classroom — 1.4.0-rc.2

O incremento 1 acrescenta autorização docente, criação/edição de uma turma por professor e SQLite compartilhado entre mundos. A suíte inclui autorização/revogação, responsabilidade, vínculo entre papéis, limites de texto, concorrência, revisão, rollback e esquema incompatível. Um teste isolado carrega o driver a partir do JAR de produção; a integração usa o serviço e as permissões nativas e confere a mesma turma após reinício.

**Formulário novo no cliente: pendente.** Executar o [roteiro do incremento 1](classroom.md), especialmente criação, descrição multilinha, edição, revogação com página aberta e retorno após reinício. A confirmação abaixo se refere ao contador de rc.1, não a estas novas telas. A matriz CI inclui teste real do driver em Windows/Linux; não considerar uma execução local Windows como resultado remoto Linux.

## Classroom — 1.4.0-rc.1

A pré-versão acrescenta somente `/classroom`, página de prova com contador no servidor, atualização parcial e fechamento. Builders/codecs reais são exercitados por JUnit; Python confere traduções `.ui` e documentos no pacote. A integração nativa confere traduções e registro do comando antes/depois de reiniciar, sem cliente.

**Fluxo central confirmado pelo usuário em 24/10/2026**, build `1.4.0-rc.1+43804c498938`, Hytale 0.7.4, pt-BR: abertura de `/classroom`, dez cliques com atualização em todos, fechamento, reabertura pelo comando e reset do contador. A [captura e o roteiro](classroom.md) preservam a evidência. A imagem mostra contador 10, textos legíveis e a identidade da compilação.

Esc, reconexão, en-US, acesso sem OP, substituição de página e isolamento com dois jogadores continuam sem confirmação específica. A resolução/escala completa não foi informada. A prova central permite iniciar o incremento 1; não equivale à aprovação dos casos restantes nem de telas futuras. As evidências históricas de circuitos abaixo preservam sua versão e seu escopo original.

## Automação

`gradlew build` executa JUnit, testes das ferramentas e conferências de versão/changelog, links locais, traduções, sete geradores e JAR. `gradlew fullCheck` acrescenta o servidor real 0.7.4 em duas fases: criar/atualizar circuitos e reiniciar para reconstruí-los.

Os testes lógicos incluem as 27 combinações de entrada ausente/0/1 nas quatro orientações, conexões diretas/por cabo, multiplicidade por face, terminais, produtores múltiplos, cadeias, ciclos, retirada de fontes e independência da ordem. A integração usa os codecs/assets reais, verifica atualizações automáticas, seções indisponíveis, retorno de seção vazia e reinício completo.

Relatórios JUnit: `build/reports/tests/test/index.html` e `build/test-results/test/`. Integração: `build/reports/server-integration/<execução>/summary.json`, JSONs por fase e logs. O resumo inclui ID da compilação e SHA-256 do JAR efetivamente copiado para o universo isolado. Os mundos antigos de desenvolvimento não são abertos. Porta local usada: `127.0.0.1:5534`.

Os números de cada execução vêm dos relatórios, não de uma contagem fixa usada para aprovar o build. Evidências de versões anteriores estão no [histórico](history/validation.md).

## Roteiro único no cliente

Use um mundo criativo de testes no Hytale 0.7.4. Instale somente o JAR de produção e reinicie. Anote o ID mostrado em `/logicenergy` antes de começar.

| Caso | Procedimento | Resultado esperado |
| --- | --- | --- |
| Inventário | Abrir LogicGates no criativo e suas três abas | Dez componentes e uma bancada, ícones fiéis, sem variantes internas duplicadas |
| Bancada manual | Em sobrevivência, fabricar a bancada pelo inventário | Receita na categoria Ferramentas; consome 6 troncos, 4 pedras, 2 cobres e 2 ferros |
| Bancada colocada | Colocar nas quatro orientações, bloquear a segunda célula, quebrar e recolocar | Ocupa 2 × 1; impede sobreposição; colisão por peça; solta uma bancada e libera ambas as células |
| Fabricação | Abrir a bancada e testar as dez receitas, falta de materiais, lotes e cancelamento | Abas Componentes/Portas lógicas; custos e tempos corretos; 8 cabos por lote; sem perda/duplicação ao cancelar |
| AND decorativo | Observar a bancada de perto, mover a câmera e variar a distância | Sem faces piscando no módulo aberto; aparência da bancada preservada |
| Bancada após reinício | Salvar, encerrar, retornar e abrir a bancada | Modelo, orientação, ocupação e receitas continuam funcionais |
| Identificação da alavanca | Observar ON/OFF na base por cima e pelos lados; alternar a haste e girar a câmera | Letras legíveis e alinhadas às duas posições; colisão e funcionamento preservados |
| AND da bancada | Comparar o AND colocado no chão com carcaça e tampa aberta da bancada | Mesmo verde, sombras e lente cinza; contatos internos e restante da bancada preservados |
| Circuito básico | Alavanca → dois cabos → lâmpada; alternar uma vez por clique | Sinal, indicador e luz acompanham cada acionamento |
| Formas | Pontas, retas, curvas, T e cruzamento | Fios contínuos, sem transparência indevida; vizinhos se ajustam |
| Desconexão | Remover cabo no meio; recolocar com fonte ligada | Trecho isolado apaga e reconecta automaticamente |
| Fontes | Usar duas alavancas na mesma rede | Retirar uma fonte não apaga enquanto a outra alimenta |
| Terminais | Colocar uma lâmpada entre trechos distintos | A lâmpada não une as redes |
| Orientação | Colocar portas olhando nas quatro direções; aplicar rotação manual | Saída segue olhar/rotação e faces elétricas correspondem ao símbolo |
| Entradas opcionais | AND com A=1; conectar B=0; remover B | Liga, apaga, volta a ligar |
| Sem entrada | Deixar cada porta sem entradas e energizar externamente sua saída | Resultado e indicador continuam desligados |
| Duas entradas | Testar 00, 01, 10 e 11 com redes independentes | AND 0/0/0/1; OR 0/1/1/1; NAND 1/1/1/0; NOR 1/0/0/0; XOR 0/1/1/0; XNOR 1/0/0/1 |
| NOT | Cabo 0/1 atrás; testar cabos nas laterais | Saída 1/0; laterais fechadas |
| Três entradas | Testar zero, uma, duas e três entradas energizadas | XOR liga com uma/três; XNOR com zero/duas, havendo conexão |
| Mesma rede | Levar a mesma rede ativa a duas/três entradas XOR | Duas desligam e três ligam; cada face conta |
| Portas diretas | Saída → entrada; depois entrada → entrada e saída → saída | Somente a primeira ligação conecta |
| Isolamento | Alimentar cabo da saída com alavanca externa | Indicador representa a lógica das entradas, sem retorno interno de sinal |
| Indicador livre | Ligar uma porta sem nada na saída | Indicador e luz representam o resultado mesmo sem consumidor |
| Ciclos | Remover fonte externa de anel AND/OR; experimentar laço inversor | Sem energia retida; indefinição aparece no diagnóstico e fica desligada |
| Altura/cantos | Aproximar componentes acima/abaixo ou em diagonal | Sem transmissão; vertical ainda não implementado |
| Seleção e remoção | Mirar peças/fios, caminhar sobre módulos e quebrar | Colisão compatível, sem cubo invisível; drop correto e vizinho intacto |
| Aparência | Ver os dez componentes de cima/lado, no sol/sombra | Uma cor suave por porta, poucas variações de luz/sombra; cabos inteiramente pretos; lentes cinza escuro ou vermelhas luminosas |
| Sons | Acionar alavanca e observar mudança de lâmpada | Confirmar comportamento sonoro no cliente e registrar divergências |
| Seções | Afastar-se até sair da área carregada, voltar e cruzar fronteiras com circuito ligado/desligado | Reconstrução sem sinal antigo; afastamento sozinho não prova descarga real |
| Reinício | Salvar, encerrar completamente e abrir de novo | Alavanca preserva intenção; portas/lâmpadas correspondem ao circuito |
| Dois jogadores | Um alterna/remove/recoloca enquanto outro observa e depois reconecta | Estados visuais e luz consistentes nos dois clientes |
| Blockbench | Abrir XOR/XNOR e bancada, conferir textura, exportar e regenerar | Fontes editáveis correspondem às variantes, sem caminhos pessoais |

A descarga real é comprovada pelo teste de servidor, que remove e recarrega a seção/coluna. No cliente, andar para longe oferece uma verificação prática de retorno, mas a distância por si só não confirma o descarregamento.

## Evidência e pendências

Em **23/10/2026**, após a entrega 1.3.2, o usuário informou que o teste multiplayer ocorreu sem problemas, os sistemas funcionaram corretamente e os crafts também. O ID/checksum não foi reinformado no relato: a confirmação é da instalação usada no teste, anterior a esta correção, e não identifica automaticamente cada JAR futuro.

| Verificação | Estado atual |
| --- | --- |
| Circuitos e sincronização com dois jogadores | Aprovados pelo usuário; não é teste de carga de uma turma |
| Fabricação em jogo | Aprovada pelo usuário; o relato não detalha cancelamento, reembolso ou todos os lotes |
| Modelos, texturas e posicionamento atuais | Aprovados nos relatos visuais anteriores; esta correção preserva exatamente os pixels |
| Drops resolvidos pelo servidor | Automatizados para os onze itens e todas as variantes, antes/depois de reiniciar |
| Quebra e coleta de drops no cliente atual | **Não retestadas**; aprovação de etapa antiga não é nova execução |
| Cancelamento/reembolso e fila de fabricação | Sem confirmação específica; manter no roteiro de cliente |
| Abertura interativa das fontes no Blockbench | Sem confirmação manual; sincronização das exportações verificada pelo build |

O teste de drops usa `BlockHarvestUtils.getDrops` com os assets e configurações reais de coleta/quebra. Confere identidade e quantidade, inclusive nos estados derivados. Não simula o clique de um jogador, a entidade no chão, coleta ou quebra da segunda célula da bancada; estes continuam no roteiro do cliente.

Para cada rodada, registrar: ID, SHA do JAR quando necessário, versão do jogo, data, ambiente, cenário, esperado/observado e captura/log da falha. Não considerar logs/relatórios antigos de `build/` como evidência de uma compilação nova.

## Consolidação 1.3.3

A validação preserva as 264 geometrias aprovadas, os materiais/UVs e as regras elétricas. A compactação tem regressões de leitura pelo decoder zlib, transparência, limites da janela DEFLATE e bytes canônicos; um comparativo local confirmou igualdade RGBA dos 28 PNGs de runtime antes/depois.

`fullCheck` inclui criação e reinício completo, receitas, drops, rotação, orientação, entidades estacionadas, indisponibilidade e retorno de seções. `benchmarkServer` é um experimento opcional de carga nativa, separado da aprovação funcional e sem limiar artificial de desempenho; resultados e limites estão em [desempenho](performance.md).

Consultar `build/release-metadata/release.json` e o `summary.json` da execução correspondente para ID, revisão e SHA-256 do JAR exato. O ZIP contém esse mesmo JAR, documentos e fontes de arte; não é uma cópia integral do repositório de desenvolvimento.

Na rodada local de 23/10/2026: **751 testes JUnit**, **28 testes Python** e **64 grupos nativos** (47 criação/atualização e 17 após reinício). A integração também verifica que o índice estacionado é reaproveitado em repouso, detecta troca de holder na mesma posição, retirada do último componente e retorno, e ignora holders não elétricos.

## Classroom administrativo — rc.3

Em 24/10/2026, `fullCheck` local Windows/Java 25 foi aprovado: **801 testes JUnit**, **32 testes Python** e **76 verificações nativas** (53 na criação, 23 após reinício real). Build `1.4.0-rc.3+db6586d63971`, revisão de produção `d560cd697b95`, árvore limpa na compilação. SHA-256 do JAR: `fd5e9cce042f3dbd10ea5a45461fe62d720c16bbab64254f7783a37f0c0c1614`.

A [evidência nativa preservada](evidence/classroom/2026-10-24-rc3-server.json) identifica o pacote exato e seus cenários. Confirma persistência da matrícula `00123`, convite offline e mensagem literal entre processos, além de aprovação, correção, saída, reingresso e remoção. As regressões de circuitos/receitas/assets continuam aprovadas pela automação.

**Cliente rc.3: falha confirmada.** Em 24/10/2026, o usuário relatou desconexão ao abrir `/classroom`; captura e log do cliente identificam `#HeaderTitle.Anchor.Height` como propriedade inválida. O JAR instalado corresponde ao build acima. A [captura preservada](evidence/classroom/2026-10-24-rc3-anchor-client-error.png) registra a falha. As aprovações automatizadas anteriores permanecem evidências de servidor/transporte, sem aprovação da interface. Os relatos de rc.1/rc.2 permanecem históricos.

## Correção de Custom UI — rc.4

Em 24/10/2026, `fullCheck` local Windows/Java 25 foi aprovado: **802 testes JUnit**, **32 testes Python** e **76 verificações nativas** (53 na criação, 23 após reinício real). Build `1.4.0-rc.4+39c111e7c82a`, revisão `dfb7546907d0`, árvore limpa na compilação. SHA-256 do JAR: `daf906232735836ffdf37cb993e365024928ad2778b9a0de96d25d5a2a823d03`. A [evidência nativa preservada](evidence/classroom/2026-10-24-rc4-server.json) corresponde a esse arquivo exato.

A regressão nova exercita as seis telas, seleções e atualizações com/sem reset de campos. Rejeita `.Anchor.*` e confere os objetos nativos serializados, alturas e margens dos quatro textos afetados. O JAR e o ZIP foram auditados, incluindo traduções e documentação. O esquema do banco permanece v3.

**Cliente rc.4: fluxos centrais confirmados em pt-BR.** O usuário confirmou convite e solicitação com aprovação, matrícula vazia recusada, `00123`, saída/remoção com confirmação, rejeição, mensagem, persistência relatada e atualização automática das duas contas. Confirmou também que revogar a autorização bloqueia navegação e alterações, sem salvar os dados modificados. [Capturas e limites da evidência](evidence/classroom/2026-10-24-rc4-client.md). En-US e cenários complementares permanecem pendentes; isso não é aprovação completa da interface.

## Polimento e autorização automática — rc.5

Em 24/10/2026, `fullCheck` final local Windows/Java 25 foi aprovado: **810 testes JUnit**, **32 testes Python** e **78 verificações nativas** (54 na criação, 24 após reinício real). Build `1.4.0-rc.5+af23d18990f0`, revisão `489b3063f8f2`, árvore limpa na compilação. SHA-256 do JAR: `44bbab5e15e9393322292749169a3b093ce41c25190c5dee1af83390b5dc8d72`. A [evidência nativa preservada](evidence/classroom/2026-10-24-rc5-server.json) identifica o arquivo exato.

A integração exercitou os seis eventos reais: permissões individuais, entrada/saída de OP e concessão/remoção de permissão por grupo. Assinantes de autorização receberam a permissão efetiva atual; o canal de dados/MOTD não foi acionado. Regressões JUnit cobriram callbacks anteriores à revogação, restauração rápida antes do processamento visual, descarte docente e preservação de rascunho de aluno/capacidade inalterada, fechamento dos listeners, seleção e paginação. Os testes conferem também IDs/propriedades emitidos e ações fora da região rolável, sem interpretar o cliente.

O gerador isolado foi executado e recusou sobrescrita em teste. Sua massa usa os serviços de produção, 25 membros, solicitações e perfis sintéticos, além de histórico; banco e gerador não integram o mod distribuído. As 140 traduções anteriores de cada idioma permaneceram intactas, com 19 novas chaves correspondentes. Esquema v3 e fontes/assets dos circuitos preservados.

**Cliente rc.5: falhou ao entrar no mundo**, antes de permitir testar `/classroom`. O [registro da captura](evidence/classroom/2026-10-24-rc5-client.md) preserva `Expected {, found =` em `Classroom.ui (31:58)`. A causa foi a declaração de parâmetros após `FlexWeight` em Buscar e Próxima; a automação acima não detectava essa ordem. A rc.6 corrige os dois casos e acrescenta verificação nas fontes/JAR e regressões. Seguir o [roteiro rc.6](classroom-rc6-acceptance.md), iniciando pela entrada e abertura. A aprovação manual da rc.4 não aprova automaticamente as novas dimensões, rolagem, ações fixas ou reação visual a permissões. CI Windows/Linux verifica separadamente a revisão enviada; a rodada nativa acima é local Windows. Nenhuma tag estável ou merge em main foi realizado.

## Correção de markup — rc.6

Em 24/10/2026, `fullCheck` local Windows/Java 25 foi aprovado: **810 testes JUnit**, **37 testes Python** e **78 verificações nativas** (54 na criação, 24 após reinício real). Build `1.4.0-rc.6+54d82d732a5d`, revisão `e542070de052`, árvore limpa na compilação. SHA-256 do JAR: `0bd65ff97941c6b391829707ffcfc46c63998a4ff43c088e29c37e8f8185480f`. A [evidência nativa preservada](evidence/classroom/2026-10-24-rc6-server.json) identifica o mesmo arquivo distribuído.

O verificador novo rejeitou o documento rc.5 antes da correção, no `@Anchor` de Buscar; a regressão também cobre o `@Text` de Próxima. Agora verifica a ordem das declarações nas três fontes `.ui` e no conteúdo do JAR, com testes de comentários, literais, blocos aninhados e empacotamento inválido. A regra direcionada foi exercitada nos 166 documentos nativos da instalação local 0.7.4, sem rejeições. Não é um parser completo nem uma validação de renderização.

**Cliente rc.6: abertura e apresentação inicial confirmadas em pt-BR**, build ID visível `1.4.0-rc.6+54d82d732a5d`. A [captura representativa e seus limites](evidence/classroom/2026-10-24-rc6-client.md) mostram a turma carregada, metadados compactos, contador separado e Anterior/Próxima completos. Os quatro arquivos recebidos eram idênticos. O usuário não pôde fazer testes detalhados; interações e demais cenários do [roteiro](classroom-rc6-acceptance.md) continuam pendentes, incluindo fechar/reabrir. A instrução genérica no rodapé com lista vazia foi registrada como ajuste de polimento. A captura anterior continua registrada como falha da rc.5. Banco v3, regras e fontes dos circuitos não foram alterados. A CI Windows/Linux é conferida separadamente na revisão enviada; não equivale a cliente validado. Sem merge em main ou tags.

## Orientação das listas e contrato — rc.7

Em 24/10/2026, `fullCheck` local Windows/Java 25 foi aprovado: **813 testes JUnit**, **37 testes Python** e **78 verificações nativas** (54 na criação, 24 após reinício real). Build `1.4.0-rc.7+5294934c944b`, revisão `e20ad241e49b`, árvore limpa na compilação. SHA-256 do JAR: `71df3bb37b0898ee8aeb3b2732f068751d8aa9dee2da84648ccc38707f2d6c2c`. A [evidência nativa preservada](evidence/classroom/2026-10-24-rc7-server.json) identifica o mesmo arquivo distribuído. A CI Windows/Linux é conferida separadamente na revisão enviada.

A regressão direcionada `ClassroomPolishTest` passou com seis testes, também incluídos na suíte final. Cobre orientações por seção/papel, página posterior vazia, seleção preservada e prioridade de confirmação, atualização e mensagens explícitas de erro/gravação/resultado. O verificador conferiu 165 chaves por idioma, parâmetros correspondentes, recursos do JAR e três documentos UI. As 159 traduções anteriores de cada idioma foram preservadas, com seis novas orientações. O teste de massa isolada reutilizou o gerador/serviços existentes, com 25 membros e solicitações/histórico paginados, sem tocar no banco real ou incluir dados sintéticos no pacote.

As mudanças de produção se limitam ao renderer, às traduções e à versão. Layout, comandos, APIs públicas, domínio, esquema SQLite v3 e fontes/assets dos circuitos permanecem preservados. Os testes de transporte/markup não substituem renderização no cliente.

Atividades possuem [contrato aprovado](roadmap/classroom-learning-contracts.md), sem implementação ou casos de atividade executados. A [captura rc.6](evidence/classroom/2026-10-24-rc6-client.md) continua evidência histórica de abertura docente; **cliente rc.7 pendente** conforme o [roteiro](classroom-rc7-acceptance.md). Os cenários de autorização automática, campos em edição, conteúdo longo, listas, idiomas/resoluções e regressão funcional aguardam disponibilidade do usuário. Não há aceite estável ou tag.

## Desempenho e atualização incremental — rc.8

O usuário [relatou multiplayer e revogação automática após a rc.7](evidence/classroom/2026-10-24-rc7-multiplayer-report.md), com demora somente na primeira interação de cada personagem. O build/checksum instalado não foi confirmado separadamente. A [medição isolada da rc.7](evidence/classroom/2026-10-24-rc7-performance.json) e o [guia de desempenho](performance.md#classroom--consultas-notificações-e-transporte) distinguem espera de I/O, notificações e transporte de renderização no cliente.

Em 24/10/2026, `fullCheck benchmarkClassroom` local Windows/Java 25 foi aprovado: **824 testes JUnit**, **37 testes Python** e **78 verificações nativas** (54 na criação, 24 após reinício real). Build `1.4.0-rc.8+e3a834923f93`, revisão `4b77da615b78`, árvore limpa na compilação. SHA-256 do JAR: `3b8cc32e738d31c8b7d6c342682bd6bab232b641afee613e4001d01a276ec156`. A [evidência nativa](evidence/classroom/2026-10-24-rc8-server.json) identifica o arquivo exato; o [benchmark final](evidence/classroom/2026-10-24-rc8-performance.json) usa o mesmo build ID. A CI Windows/Linux é conferida separadamente na revisão enviada.

As regressões cobrem 1.000 contas com mais de uma assinatura, ausência de callbacks quando a autorização efetiva permanece igual, descarte/restauração e respostas antigas, filtragem anterior ao agendamento, agrupamento de rajadas, lote pendente limitado, linhas/bindings conservados e envio obrigatório de valores locais. O transporte nativo mantém respostas vazias de confirmação; não houve mudança nos documentos `.ui`. A [comparação e seus limites](performance.md#classroom--consultas-notificações-e-transporte) registra a redução de 17.613 para 59 bytes numa atualização idêntica e de 1.000 para zero callbacks em evento sem mudança efetiva; isso não certifica mil jogadores reais. Cliente rc.8 pendente conforme o [roteiro](classroom-rc8-acceptance.md); não declarar o carregamento inicial resolvido sem reteste. Domínio, SQLite v3, layout e circuitos preservados. Atividades continuam somente documentadas.

## Encerramento de turma — rc.9

Em 24/10/2026, `fullCheck benchmarkClassroom` local Windows/Java 25 foi aprovado: **834 testes JUnit**, **37 testes Python** e **80 verificações nativas** (55 na criação, 25 após reinício real). Build `1.4.0-rc.9+e16132240905`, revisão `b4d683628020`, árvore limpa na compilação. SHA-256 do JAR: `395c4b5bec09337c1428c20e9b14d9053f13970aba58c750d86c81dce6c4d8b9`. A [evidência nativa](evidence/classroom/2026-10-24-rc9-server.json) identifica o arquivo exato; o [benchmark final](evidence/classroom/2026-10-24-rc9-performance.json) usa o mesmo build ID. A CI Windows/Linux é conferida separadamente na revisão enviada.

O teste nativo usa o runtime, as permissões e o SQLite empacotado: um professor encerra a própria turma, os alunos são liberados, ingressos pendentes são cancelados e uma nova turma pode ser criada. O reinício real confirma a turma substituta, os vínculos encerrados e o histórico cancelado. JUnit acrescenta concorrência com aprovação, rollback integral sob falha, escopo entre turmas, revisão antiga, revogação, cancelamento de convites, nova matrícula `00123`, migração do DDL v3 congelado com referências, restauração do backup e FK válida. O teste de transporte confirma ações de encerramento restritas ao contexto docente, confirmação/cancelamento e retirada de ações antigas na ficha de um ingresso cancelado, sem apagar campos locais.

Os verificadores conferiram **168 chaves por idioma**, parâmetros correspondentes e três documentos UI. O markup e os recursos dos circuitos não foram alterados. A migração **v4** preserva IDs e histórico, gera backup antes de atualizar bancos antigos e permite mais de uma turma histórica por professor, mantendo um único vínculo ativo. Encerramento não oferece restauração pela interface nem exclui registros históricos.

O benchmark continua um ensaio isolado; não mede jogadores reais ou a demora de renderização no cliente. As otimizações da rc.8 permanecem, incluindo pacote idêntico de 59 bytes, zero callbacks de grupo sem mudança efetiva após inicialização e agrupamento de eventos. **Cliente rc.9 pendente**, inclusive encerramento e reteste das mudanças da rc.8, que o usuário informou ainda não ter testado. O [roteiro atual](classroom-rc9-acceptance.md) permite começar com cancelamento, encerramento e recriação numa turma descartável. Evidências da rc.4/rc.6/rc.7 continuam históricas; não há aceite estável, merge em main ou tag. Atividades continuam somente documentadas.

### Confirmação do cliente em 25/10/2026

Após a entrega rc.9, o usuário informou que o recurso funcionou perfeitamente. O [registro](evidence/classroom/2026-10-25-rc9-client-report.md) confirma o encerramento por relato, sem novas capturas ou discriminação de todos os cenários. O aceite passou a parcial; demais pendências e estabilização continuam em aberto. A [revisão de otimização e verificações](reviews/2026-10-25-optimization-and-verification.md) reutiliza os relatórios existentes, sem nova bateria ou modificação do JAR entregue.

## Leituras agregadas — rc.10

Em 26/10/2026, uma rodada final de `fullCheck` local Windows/Java 25 foi aprovada: **845 testes JUnit**, sem falhas/erros/ignorados, **37 testes Python** e **82 verificações nativas** (56 na criação, 26 após reinício real). Build `1.4.0-rc.10+86bb2076e27f`, revisão `e0acb9ba79e9`, árvore limpa na compilação. SHA-256 do JAR: `471c5b66fcde9146b4b18006a9ec2f7f304902f9b9033c3c3b8b2987ca0ad657`. A [evidência nativa](evidence/classroom/2026-10-26-rc10-server.json) e o [comparativo das seis rodadas](evidence/classroom/2026-10-26-rc10-database-performance.json) identificam esse mesmo arquivo. Alterações posteriores somente de documentação/evidências são incorporadas ao ZIP preservando os bytes do JAR testado. A CI Windows/Linux é conferida no workflow da revisão enviada, separadamente da integração com assets locais.

Durante a edição foram executados testes direcionados de Classroom e, após as últimas regressões, de `ClassroomReadTest`. Os onze novos casos executados cobrem equivalência da composição rc.9 por papel/aba, nomes e fallback UUID, paginação na massa existente de 25 membros, consulta da permissão pelo serviço, recusa docente, responsabilidade, estado inconsistente, rollback/fechamento sob falha e snapshots durante aprovação, saída ou encerramento por outra conexão. A integração real acrescenta leitura agregada da lista/contagem docente, ficha privada do aluno com `00123` e recusa de leitura após revogação nativa, na criação e após reinício.

O probe JDBC somente de teste mediu a tela de membros passando de **cinco conexões/sete SELECTs para uma conexão/três SELECTs**, sem conexões abertas ao concluir. Três rodadas por versão no mesmo ambiente, com o JAR rc.9 preservado e a candidata exata, reduziram a mediana de membros de 5,086 para 1,336 ms e o p95 mediano de rajada 250 de 993,748 para 265,538 ms, sem falhas nas rajadas 25/100/250. O [protocolo e os limites](performance.md#classroom--leituras-agregadas-rc10) não estimam capacidade de jogadores nem aprovam renderização.

Verificadores conferiram **168 chaves por idioma**, parâmetros correspondentes e três documentos UI, incluindo a regressão contra seletores `.Anchor.*`. Esquema SQLite **v4**, DTOs da interface, operações de gravação, layout, comandos, circuitos e mundos foram preservados. Não há pool, cache entre requisições, WAL, paralelismo novo ou limpeza dos wrappers fora do escopo. Atividades seguem somente especificadas.

**Cliente rc.10 pendente:** executar a [regressão curta](classroom-rc10-acceptance.md) de abas, busca/paginação, papéis e atualização após alteração. As evidências históricas e o relato parcial rc.9 permanecem válidos dentro de seu escopo; conteúdo longo, idiomas/resoluções, edição durante atualização e demais casos anteriores continuam abertos. Nenhuma tag, versão estável, publicação ou merge em `main` é criado por esta entrega.

### Confirmação breve do cliente rc.10 em 26/10/2026

O usuário confirmou os cinco testes propostos: troca de abas com resposta instantânea, atualização automática nos painéis de professor/aluno ao ingressar, diminuição imediata da contagem ao sair, busca positiva/negativa e preservação/salvamento da descrição durante atualização. As [duas capturas](evidence/classroom/2026-10-26-rc10-client.md) mostram o build `1.4.0-rc.10+86bb2076e27f`, Hytale 0.7.4, pt-BR, professor com um aluno ativo e ficha do aluno com `00123`. A sequência temporal é confirmada pelo relato; os PNGs não comprovam a resolução/escala do jogo ou novo cálculo do checksum instalado.

A regressão breve está confirmada dentro desse escopo. Paginação extensa, en-US/resoluções, textos longos, revogação/restauração nesta candidata e demais cenários não discriminados continuam pendentes. O relato não aprova a versão estável. O JAR/ZIP entregues permanecem byte a byte preservados; nesta rodada foram atualizadas somente documentação e evidências, com verificação de repositório e sem repetir testes/benchmarks ou recompilar. A [análise de viabilidade de logging](reviews/2026-10-26-logging-feasibility.md) examina o código e o SDK local; não implementa nem mede um novo logger.

## Atividades, áreas e avaliação — rc.11

Candidata **1.4.0-rc.11+31e3c78c7881**, Hytale 0.7.4, revisão de produção `d222e32f32ad`, compilação limpa. JAR SHA-256 **140d80eae81a91206567b120df4315a87de6ecd1991f656817f5b7e09b7a0c14**. Foram aprovados **871 testes JUnit**, sem falhas/erros, **38 testes Python**, verificadores de recursos/pacote, **241 chaves por idioma**, quatro documentos UI e **89 verificações nativas** (62 criação, 27 após reinício real).

A rodada de `fullCheck` encontrou primeiro a regra antiga de fabricação aplicada à ferramenta interna, corrigida com regressão própria. A rodada seguinte passou testes/geradores e encontrou links de fontes fora do ZIP em uma revisão histórica. O ajuste foi exclusivamente documental. A conclusão executou `fullCheck -x generateBuildInfo -x processResources -x test -x testTools -x verifyGeneratedAssets`, reutilizando os testes/geradores já aprovados e o mesmo JAR, e completou pacote e integração. Logs integrais: `build/tmp/rc11-fullCheck-first.log`, `rc11-fullCheck-package-attempt.log` e `rc11-fullCheck.log`. Uma rodada nativa adicional mediu arquivos/alocação por thread, com o **mesmo SHA-256**, sem recompilar o JAR; `build/tmp/rc11-native-final.log` e relatórios em `build/reports/server-integration/20261026-125150-762e4c/`.

A [evidência nativa](evidence/classroom/2026-10-26-rc11-server.json) confirma captura em fronteiras de seção, codec nativo, pacote de preview, arquivo reaberto, sete portas lógicas nas orientações horizontais, intenção das alavancas, simulação independente, espaço ocupado recusado e descarte incluindo edições. A cópia com reserva persistida foi deixada para o **outro processo JVM**: o reinício limpou o volume quando as seções carregaram, removeu a reserva e manteve a entrega válida por checksum.

As regressões cobrem rascunho privado/publicação imutável, arquivo/checksum/cota, versões e notas independentes, atraso inicial/reenvio, concorrência, vínculo/encerramento/republicação, sessão cancelada, fila limitada e desligamento. O DDL v4 congelado confirma migração para v5 com matrícula `00123`, aprovação, turma encerrada e backup v4 restaurável. Falha de armazenamento da área não bloqueia edição normal fora de reservas conhecidas. O canal pedagógico não notifica telas administrativas nem o canal de autorização/MOTD. A massa isolada oferece mais de 20 atividades, alunos e revisões; não é empacotada no mod.

Os [tempos e limites](performance.md#classroom--áreas-e-cópias-rc11) não comprovam capacidade de jogadores ou ausência de picos de tick. **Cliente rc.11 bloqueado**: a [captura posterior](evidence/classroom/2026-10-26-rc11-client-blocker.md) mostra desconexão ao abrir Atividades por `#ScoreField.Disabled`. Seleção sem OP, renderização/gestos do preview, edição/proteção/descarte em multiplayer, layout e casos complementares do [roteiro](classroom-rc11-acceptance.md) continuam pendentes. Nenhuma evidência administrativa anterior é usada para aprovar essas novas interações. Não houve tag, publicação estável ou merge em `main`. Logging ampliado e avaliação automática permanecem posteriores.

## Compatibilidade de campos da interface — rc.12

Candidata **1.4.0-rc.12+3da7d0578c7b**, Hytale 0.7.4, revisão de produção `ca102d6f992e`, compilação limpa. JAR SHA-256 **7bcaa2d4b7dd02b5c35dcd48a712a1a9270ec5e54378734a7ee0ac444fe45cd2**. A correção remove `.Disabled` dos seis campos TextField/MultilineTextField na renderização, início de gravação e erro. Botões, validação do servidor e preservação de campos nas atualizações comuns permanecem. Banco v5 e conteúdo salvo não mudam.

Foram aprovados **41 testes direcionados de UI**, seguidos de uma rodada completa de `fullCheck`: **873 testes JUnit**, **38 testes Python**, **241 chaves por idioma**, quatro documentos UI, recursos/distribuição e **89 verificações nativas** (62 criação, 27 após reinício em outro processo). As novas regressões conferem comandos efetivamente renderizados contra o tipo do widget empacotado em oito contextos, com e sem reposição de campos; também verificam gravação e todas as mensagens de erro do domínio. Isso cobre a lacuna do teste anterior, limitado à abertura inicial e bindings. Não constitui um parser completo da API do cliente.

[Evidência automatizada](evidence/classroom/2026-10-26-rc12-server.json). Logs integrais locais: `build/tmp/rc12-ui-tests.log`, `build/tmp/rc12-fullCheck.log`; relatório nativo `build/reports/server-integration/20261026-143531-db8eb5/`. O pacote final conserva exatamente o JAR dessa rodada, e os artefatos rc.11 permanecem intactos.

**Cliente rc.12: reteste pendente.** Começar por [abrir Atividades, criar/salvar/publicar rascunho e provocar erro de validação](classroom-rc12-acceptance.md); depois retomar os casos de entrega, preview, cópia e avaliação. Nenhuma confirmação de renderização ou de estabilidade é deduzida da execução do servidor isolado. Não houve merge, tag ou publicação estável.


## Evidências do cliente rc.12 e polimento rc.13

O [registro de cliente rc.12](evidence/classroom/2026-10-26-rc12-client.md) incorpora as capturas representativas e separa o que foi demonstrado visualmente do que foi confirmado pelo relato. O fluxo principal pedagógico em multiplayer está confirmado: atividade, entrega sem OP, preview/cópia fiéis inclusive ligada, edição restrita, revisões, avaliação e limpeza ao retornar ao mundo. Não é aceite da matriz de resoluções/escalas, de todas as situações concorrentes ou da versão estável.

A rc.13 muda apresentação e transporte interno: painéis com margem 16, criação ampla, fichas compactas, cinco seletores nativos de prazo, ações próprias da administração e contagem por papel. Não altera o esquema v5 nem o conteúdo das entregas. A contagem usa a transação de leitura existente e mantém uma conexão/até três SELECTs na tela docente de membros. Eventos de catálogo não passam pelo canal administrativo/MOTD; notas e entregas não invalidam a contagem.

Durante a edição, a suíte direcionada de Classroom passou com regressões de datas, transporte, criação, privacidade, snapshot e orçamento SQL. A rodada de `fullCheck` foi aprovada. Após ampliar a compatibilidade dos anos persistidos, a conclusão `fullCheck -x testTools -x verifyGeneratedAssets` repetiu os testes Java, pacote e integração nativa, reaproveitando somente as ferramentas Python e assets inalterados já aprovados na primeira rodada. Os logs integrais ficam em `build/tmp/rc13-classroom.log` e `build/tmp/rc13-fullCheck.log`. **Renderização, escala, quebra de linha e seleção nativa no cliente rc.13 permanecem pendentes**, conforme o [roteiro](classroom-rc13-acceptance.md).


Candidata final **1.4.0-rc.13+33d7e296575a**, revisão de produção `1f9982578bfb`, compilação limpa. JAR SHA-256 `78a3c8ef3224e8200f752f31791da1122582abc111dde4a35e91d9e6c79e34c7`. A [evidência final](evidence/classroom/2026-10-26-rc13-server.json) registra **884 testes JUnit**, zero falhas/erros/ignorados, **38 testes Python**, **262 chaves por idioma**, quatro documentos UI e **89 verificações nativas** (62 criação, 27 após reinício real). Log final: `build/tmp/rc13-fullCheck-final.log`. A CI Windows/Linux é conferida separadamente; sua compilação não substitui o checksum deste JAR testado. Documentação posterior é empacotada preservando os bytes do JAR. Os arquivos rc.12 conservam seus checksums originais. Nenhum resultado de renderização rc.13 é inferido desses testes.
