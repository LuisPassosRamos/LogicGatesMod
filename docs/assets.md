# Arte, modelos e arquivos gerados

## Fontes e contratos

| Conteúdo | Fonte mantida | Saída |
| --- | --- | --- |
| Identidade visual e receitas | [Catálogo da oficina](../assets/workshop/catalog.json) | Registro consistente dos onze itens |
| Texturas | [Atlas e proveniência](../assets/textures/README.md) | Cópias PNG com nomes próprios no runtime |
| Cabo | `tools/generate_cable_assets.py` | 16 máscaras, item, textura e seleção baixa |
| Alavanca e lâmpada | Exportações em `assets/models/Lever` e `Lamp` | Modelos por potência/máscara e colisões |
| Sete portas | `assets/models/<pasta>` e catálogo lógico | 200 estados, indicadores e colisões |
| Bancada | [Fonte Blockymodel](../assets/models/Workbench/Logic_Workbench.blockymodel) | Modelo, atlas, colisões e bancada nativa |
| Projeto editável da bancada | [Blockbench](../assets/blockbench/Workbench/Logic_Workbench.bbmodel) | Caixas nomeadas e textura incorporada |
| Ícones | Modelos e texturas finais, câmera fixa em `tools/model_icons.py` | 11 ícones de 64 × 64 e 4 de 48 × 48 |

As fontes `.blockymodel`, os projetos `.bbmodel`, os atlas e os prompts são incluídos no ZIP, fora do JAR. Somente os recursos runtime integram o JAR. Os identificadores dos dez componentes, seus estados salvos e os caminhos anteriores de modelos continuam resolvíveis.

## Fluxo de edição

1. Abrir um projeto com o [plugin Hytale do Blockbench](https://github.com/JannisX11/hytale-blockbench-plugin), editar e exportar a fonte em `assets/models/`.
2. Componentes existentes usam a escala original; os geradores continuam aplicando 2×. A bancada já usa sua escala final: 32 unidades por bloco.
3. Preservar as caixas independentes e sem rotação nas fontes XOR/XNOR e bancada. Os projetos portáteis são sincronizados a partir dessas exportações; exporte as alterações antes de regenerar.
4. Alterar atlas fonte e metadados do catálogo, sem editar variantes `_Connections_` ou ícones manualmente.
5. Regenerar nesta ordem:

```powershell
python tools/generate_cable_assets.py
python tools/generate_terminal_assets.py
python tools/generate_gate_assets.py
python tools/generate_workbench_assets.py
python tools/generate_inventory_assets.py
python tools/sync_gate_projects.py
python tools/model_icons.py
.\gradlew.bat build
```

Os sete comandos aceitam `--check`, que confere sem regravar fontes. Arquivos já corretos ficam intocados; arquivos alterados são publicados atomicamente. O renderizador usa somente a biblioteca padrão Python: interpreta caixas, quaternions, UVs e atlas reais, com câmera ortográfica, luz consistente e transparência. A codificação PNG é determinística entre versões de zlib.

## Apresentação e bancada

Todos os componentes usam metal pintado, ferro, cobre e sombras em pixels. Cada porta possui atlas próprio; letras A/B/C, setas, formas e marcas de inversão continuam presentes. Indicadores apagados são cinza escuro; ligados, vermelhos e luminosos.

Os 264 modelos da versão 1.2.0 têm sua geometria física protegida por uma referência em `tools/tests/fixtures/approved_geometry.json`. Dezesseis modelos ligados de lâmpada acrescentam somente UVs e material emissivo diferentes. Colisões dos componentes permanecem iguais.

A bancada tem 51 peças, ocupa 2 × 1 blocos e cabe em um bloco de altura. O AND aberto, a placa interna, contatos, tampa removida e ferramentas são decoração. O jogo reserva e remove a segunda célula pelo sistema nativo de fillers. Colisão e seleção acompanham cada peça.

O catálogo lógico `src/main/resources/logicgates/gate-catalog.json` conserva tipos, máscaras e regras de simulação. O catálogo da oficina cuida da apresentação e fabricação. A conferência exige categoria, ícone e receita para todos os itens, inclusive os futuros. A categoria criativa tem Componentes, Portas lógicas e Bancadas; a estação oferece Componentes e Portas lógicas.

Veja [fabricação](crafting.md) e [validação no cliente](validation.md).
