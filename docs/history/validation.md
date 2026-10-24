# Histórico de validação

Os relatórios antigos descreviam incrementos locais do manifest 1.0.0, sempre com nome fixo `sisso.logicsim.jar`. Não são releases Git formais. Os roteiros repetidos foram reunidos no [roteiro atual](../validation.md); este arquivo preserva evidências úteis sem tratá-las como resultados da compilação atual.

| Data | Incremento | JUnit | Integração nativa |
| --- | --- | --- | --- |
| 22/10/2026 | Primeiro circuito funcional | 25 | 12 cenários |
| 22/10/2026 | Correção de UV do cabo | 26 | Avaliador sem mudança; resultado nativo anterior |
| 22/10/2026 | Terminais, orientação e hitboxes | 33 | 15 grupos: 13 criação + 2 reinício |
| 22/10/2026 | AND/OR | 263 | Criação, seções e reinício aprovados conforme registro anterior |
| 22/10/2026 | NOT/NAND/NOR | 503 | 36 grupos: 30 criação + 6 reinício |
| 22/10/2026 | XOR/XNOR | 732 | 44 grupos: 36 criação + 8 reinício |
| 22/10/2026 | Material metálico XOR/XNOR | 733 | 44 grupos: 36 criação + 8 reinício |

## Identificação das últimas referências

| Incremento | Execução antiga em `build/server-integration/` | SHA-256 do JAR antigo |
| --- | --- | --- |
| Terminais | `20261022-200736-0ab7f3` | `24bda6b993d3c87bbc8d05dfba5459c28f93e629b493b4aacf150ddb89d0e5c5` |
| AND/OR | `20261022-210349-1c46c3` | `7b7fcdcc034b43896aaf5afb182e34f8e9a893f63a2a1d9921a4270cb6ba117b` |
| Inversoras | `20261022-220118-f18428` | `0db3bf877595906145f7ab98de77da2005a0b09a3f83771fc4b10551dba0dfc0` |
| Paridade | `20261022-231653-bcf8cd` | `5d3b10d94b8bc0a17a8d426b9a9efd3c9201f3190f909f3b9323bc31148ce46a` |
| Material metálico | `20261022-234854-cf0ef3` | `f016b54a24fe8e6560855dbc97b5fdca71c18188fd2ef16c0889be6155bd34d4` |

Essas pastas eram saídas locais e foram preservadas no backup externo antes de limpar `build/`. Não são caminhos exigidos pelo projeto. A referência metálica tinha 1.689.355 bytes; o pacote novo exclui arquivos de autoria e tem outra identidade.

O usuário confirmou os circuitos básicos, fontes múltiplas, remoção/reconexão, isolamento, reinício e retorno após afastamento. Também aprovou as portas e a revisão visual. Um relato de afastamento não prova sozinho descarga de seção; a integração automatizada faz descarga/recarga explícita. Dois clientes e abertura interativa no Blockbench permanecem sem evidência manual registrada.

A partir de 1.1.0, usar o changelog, ID da compilação, checksum e relatórios da própria execução. Não reutilizar os números ou hashes desta página para anunciar uma compilação nova como testada.

## Entregas anteriores à consolidação 1.3.3

Os estados de pendência abaixo pertencem à data de cada incremento. O [registro atual](../validation.md) incorpora relatos posteriores.

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
- Benchmark reproduzível em 12 cenários, descrito em [desempenho](../performance.md). A prévia técnica das marcações verifica geometria, sem substituir o cliente.
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

## Correção 1.3.1 — texturas simples e AND decorativo

O escopo automatizado inclui os **751 testes JUnit**, agora com contraste em uma mesma família de cor, e **20 testes das ferramentas**. Três testes novos verificam a borracha preta de todos os cabos/conectores, a ausência de faces externas coplanares sobrepostas no AND decorativo e a rejeição de plugins de integração com versão incompatível antes de iniciar o servidor. As 264 variantes aprovadas e as lâmpadas ligadas mantêm a mesma geometria física dos componentes.

`fullCheck` repete os **61 grupos de servidor 0.7.4**, incluindo criação e reinício. O relatório de cada execução identifica os bytes exatos do JAR; consultar o resumo mais recente e `release.json`. A textura da bancada permanece idêntica à de 1.3.0. Os ícones, as fontes XOR/XNOR e os projetos portáteis acompanham as novas texturas.

A prévia dos modelos reais permite revisar a simplificação da arte. A confirmação da cintilação corrigida depende de observar a bancada no cliente, movendo a câmera e variando a distância; permanece no roteiro acima junto da avaliação de aparência sob a iluminação do jogo.

## Correção 1.3.2 — AND da bancada e identificação da alavanca

O build verifica a igualdade dos pixels do AND nas peças decorativas, a preservação integral das fontes nas duas texturas compiladas, a legibilidade ON/OFF em todas as máscaras/estados e os eixos UV dos ícones. As 264 variantes dos componentes mantêm a geometria física aprovada; a bancada mantém as mesmas 51 peças e colisões.

A conferência no cliente deve observar as duas faces da alavanca, a borda superior da base e o alinhamento da haste em ON/OFF. Comparar a bancada com um AND colocado ao lado, incluindo iluminação e ausência de cintilação. O relatório `fullCheck` e o `release.json` identificam o JAR exato validado.
