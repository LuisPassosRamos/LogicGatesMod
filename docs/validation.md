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
