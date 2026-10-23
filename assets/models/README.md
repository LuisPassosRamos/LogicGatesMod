# Exportações de autoria

Estas onze fontes `.blockymodel` foram separadas dos recursos de runtime em 1.2.0. Preservam a escala e geometria originais. Os geradores leem esta pasta; Common contém as variantes geradas usadas pelo jogo.

- `AndGate`, `OrGate`, `NotGate`, `NandGate`, `NorGate`, `XorGate`, `XnorGate`: uma exportação fonte por porta.
- `Lever`: modelos originais apagado/acionado.
- `Lamp`: fonte do corpo da lâmpada.
- `Cable`: original preservado como arte de referência; o gerador do cabo define a geometria proceduralmente.

Os projetos Blockbench ficam em `assets/blockbench/`. Exportar o modelo editado para a subpasta correspondente aqui e executar os geradores. As texturas runtime permanecem em `src/main/resources/Common/Items/LogicGates/`; o atlas fonte metálico permanece em `assets/textures/`.

As letras das portas e a seta de saída são adicionadas pelo gerador. Não editar `_Connections_` manualmente. O [fluxo completo](../../docs/assets.md) explica escala, atlas e sincronização dos projetos XOR/XNOR.
