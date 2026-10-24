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

Os testes conferem UVs e material em todas as faces de todas as variantes. Como muitas faces compartilham a mesma região do atlas, a inspeção exaustiva dos pixels é reaproveitada por textura, retângulo e regra de cor; cada pixel utilizado continua sendo validado, sem repetir a leitura para cada cópia do modelo.

## Apresentação e bancada

Os componentes usam uma linguagem minimalista de pixels, com uma cor suave por porta e variações discretas de luz e sombra. Faixas de cobre, rebites e detalhes pintados foram retirados dos componentes. Cabos, junções e fios conectores usam borracha preta; alavanca e lâmpada têm bases cinza neutro. Cada porta possui atlas próprio; letras A/B/C, setas, formas e marcas de inversão continuam presentes. Indicadores apagados são cinza escuro; ligados, vermelhos e luminosos.

Os 264 modelos da versão 1.2.0 têm sua geometria física protegida por uma referência em `tools/tests/fixtures/approved_geometry.json`. Dezesseis modelos ligados de lâmpada acrescentam somente UVs e material emissivo diferentes. Colisões dos componentes permanecem iguais.

A bancada tem 51 peças, ocupa 2 × 1 blocos e cabe em um bloco de altura. O AND aberto, a placa interna, contatos, tampa removida e ferramentas são decoração. O jogo reserva e remove a segunda célula pelo sistema nativo de fillers. Colisão e seleção acompanham cada peça. O atlas original da bancada é preservado como fonte. Desde 1.3.2, o atlas de runtime acrescenta uma faixa lateral com cópias exatas dos materiais do AND atual; a carcaça, a tampa removida e a lente da decoração usam essa faixa, enquanto madeira, ferramentas e contatos internos conservam os materiais anteriores. Na correção 1.3.1, as paredes do AND aberto passam a começar acima da base, eliminando faces externas coplanares sobrepostas que causavam disputa no buffer de profundidade. Um teste de geometria protege esse encaixe.

A base da alavanca tem ON no lado local +X e OFF no lado −X, acompanhando a inclinação real da haste. As palavras são impressas na textura, sem acrescentar geometria ou colisão, e aparecem em todas as máscaras/estados. `tools/presentation_atlas.py` compila os dois atlas, reutilizando os pixels fonte e os glifos; `tools/png_codec.py` mantém a codificação PNG reproduzível. A face superior segue os eixos UV do [exportador Hytale](https://github.com/JannisX11/hytale-blockbench-plugin/blob/main/src/blockymodel.ts) e do [Blockbench](https://github.com/JannisX11/blockbench/blob/master/js/outliner/types/cube.js).

O catálogo lógico `src/main/resources/logicgates/gate-catalog.json` conserva tipos, máscaras e regras de simulação. O catálogo da oficina cuida da apresentação e fabricação. A conferência exige categoria, ícone e receita para todos os itens, inclusive os futuros. A categoria criativa tem Componentes, Portas lógicas e Bancadas; a estação oferece Componentes e Portas lógicas.

Veja [fabricação](crafting.md) e [validação no cliente](validation.md).

## Compactação sem perdas

`tools/portable_deflate.py` publica um único bloco Huffman fixo RFC 1951, com busca limitada a oito candidatos por bucket e janela de 32 KiB. `png_codec.py` aplica o filtro Up, preserva exatamente os pixels RGBA8 (atlas opacos usam RGB8 com alpha 255 implícito) e usa zlib somente para leitura/checksums. A escolha evita depender de decisões de compressão que variam entre versões da biblioteca do sistema. Não há nova dependência Python.

Na consolidação 1.3.3, o atlas da alavanca passou de 7.656.318 para 1.555.424 bytes e o da bancada para 2.303.842 bytes. O projeto editável da bancada passou de 10.285.830 para 3.149.198 bytes. Dimensões e pixels dos 28 PNGs de runtime foram comparados e permaneceram idênticos; fontes de arte e geometria não foram redesenhadas. O tamanho do JAR tem redução diferente, pois ele já aplica compressão ZIP.
