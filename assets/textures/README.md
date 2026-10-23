# Texturas da oficina LogicGates

As onze fontes PNG estão em `workshop/`. Foram criadas com a skill **imagegen**, usando a ferramenta integrada em 23/10/2026. Os prompts exatos e a associação de cada arquivo ao componente estão em [prompts.json](../workshop/prompts.json). As imagens retornadas têm 1254 × 1254 pixels, totalmente opacos; são preservadas sem repintura ou redimensionamento por scripts.

Cada atlas tem quatro colunas e quatro linhas: pintura e sombras; ferro/cobre/madeira; lente apagada/acesa; fio e materiais das marcações. O atlas da bancada usa madeira na primeira linha e pintura do AND na quarta célula da segunda linha. Os materiais amostram patches centrais de 128 × 128 (64 × 64 nas lentes), afastados das divisórias pintadas. Consulte `tools/workshop_material.py` para o mapeamento.

O padrão é de oficina artesanal: cores limitadas, pinceladas em pixels, ferro claro, cobre, grafite e madeira. AND é oliva, OR azul, NOT ocre, NAND vermelho terroso, NOR roxo, XOR turquesa e XNOR índigo. Lentes desligadas são cinza escuro; ligadas, vermelhas. O atlas metálico antigo de XOR/XNOR foi substituído; o histórico Git conserva sua origem.

O catálogo [catalog.json](../workshop/catalog.json) reúne as paletas, categorias e receitas dos onze itens. O gerador copia cada atlas para seu caminho runtime, sem alterar seus pixels. Os ícones são renders dos modelos reais, produzidos por `tools/model_icons.py`.
