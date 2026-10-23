# Arte, modelos e arquivos gerados

## Fontes e destinos

| Conteúdo | Fonte mantida | Saída |
| --- | --- | --- |
| Cabo | Geometria/UV em `tools/generate_cable_assets.py` e atlas Common existente | 16 máscaras, item e hitbox |
| Alavanca/lâmpada | Exportações em `assets/models/Lever` e `Lamp`; PNGs runtime em Common | Modelos por potência/máscara, colisões e itens |
| Sete portas | Exportação em `assets/models/<pasta>` e atlas runtime em Common | Estados conectados, indicadores, colisões e itens |
| XOR/XNOR metálicos | `assets/textures/Logic_Parity_Metal_Texture.png` e `tools/parity_material.py` | Atlas com nomes próprios e UVs funcionais |
| Blockbench XOR/XNOR | Exportações em `assets/models/XorGate` e `XnorGate`; PNG Common correspondente | Projetos portáteis em `assets/blockbench/XOR/` e `XNOR/` |
| Blockbench NAND original | `assets/blockbench/NAND/NAND_port.bbmodel` | Fonte editável preservada; não integra o JAR |

As onze exportações originais estão em `assets/models/` e são entradas dos geradores. Elas não são incluídas no JAR. Os modelos em Common são saídas geradas para o jogo. Preservar nomes/caminhos dos recursos já usados pelos mundos. Os projetos `.bbmodel` são de autoria e ficam fora do JAR.

## Fluxo de edição

1. Abrir o projeto no Blockbench com o [plugin Hytale](https://github.com/JannisX11/hytale-blockbench-plugin).
2. Trabalhar na escala original; o gerador aplica 2× ao corpo no runtime. Os fios têm espessura 3 e chegam às bordas do bloco.
3. Exportar para o `.blockymodel` fonte da pasta `assets/models/` correspondente, sem substituir uma variante `_Connections_`.
4. Para XOR/XNOR, manter caixas independentes, alinhadas aos eixos, orientação identidade e nomes funcionais das peças. Os geradores atuais dependem desse contrato.
5. Regenerar e executar o build:

```powershell
python tools/generate_cable_assets.py
python tools/generate_terminal_assets.py
python tools/generate_gate_assets.py
python tools/sync_gate_projects.py
.\gradlew.bat build
```

Para XOR/XNOR, a exportação `.blockymodel` é a referência de sincronização. `sync_gate_projects.py` recria os `.bbmodel` portáteis, incorporando a textura. Exporte suas alterações antes de rodá-lo para não perder uma edição existente somente no projeto Blockbench.

`--check` em cada ferramenta não altera arquivos. A conferência compara o conjunto gerado com as fontes e falha quando há divergência; ela é obrigatória em `build`/CI. Os geradores não recriam pixels do atlas metálico. Sua proveniência e prompts ficam em [assets/textures/README.md](../assets/textures/README.md).

## Apresentação e seleção

Há 16 máscaras laterais para cada componente de quatro faces. Portas têm ligado/desligado por máscara: 32 estados para AND/OR/NAND/NOR/XOR/XNOR; NOT usa somente frente/trás e tem 8 estados. No total são 200 variantes de modelos de portas.

Os indicadores ficam apagados em cinza e ligados em vermelho luminoso, independentemente de existir um consumidor na saída. XOR/XNOR têm base prateada, símbolos em aço e indicador apagado em ardósia. Colisões e seleção são compostas por peça, acompanhando corpo e fios presentes.

Os quatro geradores conferem 426 arquivos runtime e dois projetos Blockbench. Testes verificam alinhamento, UVs opacos, colisões, texturas incorporadas e propriedades de luz. Aparência real, inventário/mão, sensação da colisão e abertura interativa no editor continuam no [roteiro manual](validation.md).

## Catálogo e marcações das portas

`src/main/resources/logicgates/gate-catalog.json` centraliza identificadores, pastas, máscaras permitidas e configuração de arte. Java verifica sua correspondência com os tipos/regras do avaliador; os geradores e a auditoria do JAR consomem o mesmo catálogo. Não editar variantes para acrescentar uma porta nova.

`tools/port_markings.py` gera letras A/B/C e seta +X sobre os corpos, mesmo com máscara 0. NOT recebe somente C e seta. As marcações são caixas baixas com colisão/seleção compatível e UVs opacos; não alteram pixels dos PNGs. XOR/XNOR usam traço escuro sobre prata; as demais portas usam patches claros para contraste. As marcações são acrescentadas na geração e não precisam ser desenhadas à mão nos projetos Blockbench.
