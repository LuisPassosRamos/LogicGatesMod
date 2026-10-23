# Validação e relato de testes

## Automação

`gradlew build` executa JUnit, testes das ferramentas e conferências de versão/changelog, links locais, traduções, sete geradores e JAR. `gradlew fullCheck` acrescenta o servidor real 0.7.4 em duas fases: criar/atualizar circuitos e reiniciar para reconstruí-los.

Os testes lógicos incluem as 27 combinações de entrada ausente/0/1 nas quatro orientações, conexões diretas/por cabo, multiplicidade por face, terminais, produtores múltiplos, cadeias, ciclos, retirada de fontes e independência da ordem. A integração usa os codecs/assets reais, verifica atualizações automáticas, seções indisponíveis, retorno de seção vazia e reinício completo.

Relatórios JUnit: `build/reports/tests/test/index.html` e `build/test-results/test/`. Integração: `build/reports/server-integration/<execução>/summary.json`, JSONs por fase e logs. O resumo inclui ID da compilação e SHA-256 do JAR efetivamente copiado para o universo isolado. Os mundos antigos de desenvolvimento não são abertos. Porta local usada: `127.0.0.1:5534`.

Os números de cada execução vêm dos relatórios, não de uma contagem fixa usada para aprovar o build. A referência anterior à organização era 733 testes JUnit e 44 grupos de servidor; novas verificações de identidade/ferramentas ampliam essa cobertura. Evidências anteriores estão no [histórico](history/validation.md).

## Roteiro único no cliente

Use um mundo criativo de testes no Hytale 0.7.4. Instale somente o JAR de produção e reinicie. Anote o ID mostrado em `/logicenergy` antes de começar.

| Caso | Procedimento | Resultado esperado |
| --- | --- | --- |
| Inventário | Abrir LogicGates no criativo e suas três abas | Dez componentes e uma bancada, ícones fiéis, sem variantes internas duplicadas |
| Bancada manual | Em sobrevivência, fabricar a bancada pelo inventário | Receita na categoria Ferramentas; consome 6 troncos, 4 pedras, 2 cobres e 2 ferros |
| Bancada colocada | Colocar nas quatro orientações, bloquear a segunda célula, quebrar e recolocar | Ocupa 2 × 1; impede sobreposição; colisão por peça; solta uma bancada e libera ambas as células |
| Fabricação | Abrir a bancada e testar as dez receitas, falta de materiais, lotes e cancelamento | Abas Componentes/Portas lógicas; custos e tempos corretos; 8 cabos por lote; sem perda/duplicação ao cancelar |
| Bancada após reinício | Salvar, encerrar, retornar e abrir a bancada | Modelo, orientação, ocupação e receitas continuam funcionais |
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
| Aparência | Ver os dez componentes de cima/lado, no sol/sombra | Oficina artesanal; cores e detalhes próprios; ferro/cobre legíveis; lentes cinza escuro ou vermelhas luminosas |
| Sons | Acionar alavanca e observar mudança de lâmpada | Confirmar comportamento sonoro no cliente e registrar divergências |
| Seções | Afastar-se até sair da área carregada, voltar e cruzar fronteiras com circuito ligado/desligado | Reconstrução sem sinal antigo; afastamento sozinho não prova descarga real |
| Reinício | Salvar, encerrar completamente e abrir de novo | Alavanca preserva intenção; portas/lâmpadas correspondem ao circuito |
| Dois jogadores | Um alterna/remove/recoloca enquanto outro observa e depois reconecta | Estados visuais e luz consistentes nos dois clientes |
| Blockbench | Abrir XOR/XNOR e bancada, conferir textura, exportar e regenerar | Fontes editáveis correspondem às variantes, sem caminhos pessoais |

A descarga real é comprovada pelo teste de servidor, que remove e recarrega a seção/coluna. No cliente, andar para longe oferece uma verificação prática de retorno, mas a distância por si só não confirma o descarregamento.

## Evidência e pendências

O usuário aprovou manualmente os circuitos básicos, portas e a atualização visual em entregas anteriores. Isso não substitui uma nova execução com o ID da versão atual. A inspeção com dois jogadores e a abertura interativa dos projetos Blockbench não têm aprovação documentada; permanecem verificações manuais.

Para cada rodada, registrar: ID, SHA do JAR quando necessário, versão do jogo, data, ambiente, cenário, esperado/observado e captura/log da falha. Não considerar logs/relatórios antigos de `build/` como evidência de uma compilação nova.

## Entrega 1.1.0 verificada em 23/10/2026

- `clean build serverTestJar` aprovado; `fullCheck` aprovado novamente com o pacote final.
- **734 testes JUnit**, sem falhas, erros ou casos ignorados; **8 testes das ferramentas** aprovados.
- **426 arquivos runtime e 2 projetos Blockbench** conferidos sem reescrever fontes.
- **44 grupos no servidor real 0.7.4**: 36 na criação/atualização e 8 após reiniciar. Universo temporário removido após aprovação; relatórios preservados.
- ID: `1.1.0+40574acbac80`. SHA-256 do JAR: `cee3c00ac04c0d97c71cf40cde52469c583e353246553a27f2ec1bb2c97d28ae`.
- Relatório nativo: `build/reports/server-integration/20261023-090919-6d550f/summary.json`. Esse caminho é uma saída local regenerável, não uma dependência dos docs.
- JAR carregado na integração corresponde ao arquivo de produção e ao contido no ZIP. O pacote inclui os documentos mantidos e passa na conferência de links/checksum.
- Comparação com o backup: simulação e texturas preservadas; somente inicialização/comando receberam identificação. Três classes sem uso foram removidas e o original Blockbench do NAND foi movido sem alterar seus bytes.
- Workflows preparados para GitHub; não houve execução remota, commit, push ou criação de release nesta tarefa. A validação desta entrega foi local no Windows.

A inspeção visual com o cliente e a sincronização em duas máquinas continuam manuais. Esta organização não altera a lógica ou a geometria dos componentes.

## Incremento 1.2.0 — ensino e diagnóstico

- **745 testes JUnit**, sem falhas, erros ou casos ignorados; **11 testes das ferramentas** aprovados.
- **53 grupos nativos** aprovados, incluindo os 44 anteriores, leitura das duas línguas, inspeção por snapshot, métricas, parser dos comandos, permissões e reinício.
- Catálogo compartilhado, 200 estados de modelos de portas e sete geradores conferidos; onze exportações de autoria fora do JAR.
- Benchmark reproduzível em 12 cenários, descrito em [desempenho](performance.md). A prévia técnica das marcações verifica geometria, sem substituir o cliente.
- Build e ZIP verificados com documentos incluídos. Consultar `release.json`/SHA-256 do pacote para identificar os bytes exatos após os commits.

Roteiro adicional no cliente: selecionar pt-BR e en-US, conferir nomes/acentos e `/logichelp --gate=XOR --inputs=3`; verificar A/B/C e seta nas quatro rotações, com e sem cabos; usar `/logicinspect x y z` em entrada vazia, cabo 0/1 e circuito com realimentação. Conferir aparência/colisão dos marcadores e toda a sincronização com dois jogadores. Essa validação multiplayer será feita pelo usuário e continua pendente até seu relato.

## Incremento 1.3.0 — oficina, inventário e fabricação

- **751 testes JUnit** e **17 testes das ferramentas** aprovados, sem falhas.
- **61 grupos no servidor 0.7.4**: 45 na criação e 16 após reinício completo.
- **471 recursos runtime**, três projetos Blockbench e os sete geradores conferidos. Dezesseis variantes luminosas de lâmpada mantêm a mesma forma das apagadas; as 264 variantes anteriores preservam a geometria aprovada.
- Onze receitas registradas com ingredientes reais; transação nativa de consumo rejeita falta de material sem retirar itens, consome custos exatos e produz lotes de 8 cabos ou 1 componente.
- Bancada validada nas quatro orientações: espaço ocupado impede colocação, a segunda célula é reservada, remoção libera os fillers e a bancada persiste após reinício.
- Categoria e abas carregadas pelo registro real de assets; 64 chaves completas por língua em en-US e pt-BR.
- ID das fontes verificadas: `1.3.0+173495ebfec5`. Execução nativa: `build/reports/server-integration/20261023-161717-6f44f3/summary.json`. Revisão Git/checksum podem mudar ao compilar os mesmos fontes após os commits; o resumo de cada execução registra o JAR exato testado. Consulte a identificação final em `release.json`.

A verificação de consumo chama a transação real usada por `CraftingManager.craftItem`, em contêineres nativos e com receitas carregadas do jogo. Não cria uma sessão de jogador nem comprova a interface ou a fila temporizada. **Continuam pendentes no cliente:** abertura/abas e filas, cancelamento/reembolso, inventário/mão, inspeção visual sob iluminação real, drops ao quebrar e sincronização com dois jogadores. A prévia renderizada ajuda a revisar a arte, mas não substitui esse roteiro.

A bancada é fabricada instantaneamente pelo inventário: o `Fieldcraft` nativo de 0.7.4 não admite o atraso de três segundos proposto. As receitas da estação mantêm os tempos planejados. Nenhuma lógica elétrica ou formato persistido dos componentes existentes foi alterado.
