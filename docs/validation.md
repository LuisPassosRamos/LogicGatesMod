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

Em 24/10/2026, `fullCheck` local Windows/Java 25 foi aprovado: **800 testes JUnit**, **32 testes Python** e **76 verificações nativas** (53 na criação, 23 após reinício real). Build `1.4.0-rc.3+d4a6c2c77d19`, revisão de produção `68aa3db9aa1d`, árvore limpa na compilação. SHA-256 do JAR: `40d1d1c10d5fc10e4c20392eb7c50bce149f8a625336eb8df26ccb137a65ea3f`.

A [evidência nativa preservada](evidence/classroom/2026-10-24-rc3-server.json) identifica o pacote exato e seus cenários. Confirma persistência da matrícula `00123`, convite offline e mensagem literal entre processos, além de aprovação, correção, saída, reingresso e remoção. As regressões de circuitos/receitas/assets continuam aprovadas pela automação.

**Cliente rc.3: pendente.** Nenhuma destas verificações renderiza Custom UI. O [roteiro](classroom.md) exige professor/aluno, listas, eventos, campos, idiomas e conexão. Os relatos de rc.1/rc.2 permanecem históricos. A CI remota Windows/Linux precisa aprovar a revisão enviada; o resultado local não comprova Linux.
