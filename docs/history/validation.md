# Histórico de validação anterior ao versionamento

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
