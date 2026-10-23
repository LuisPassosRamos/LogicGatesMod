# Texturas da oficina LogicGates

As onze fontes PNG estão em `workshop/`. Foram criadas com a skill **imagegen**, usando a ferramenta integrada em 23/10/2026. A revisão 1.3.1 substitui os dez atlas dos componentes por versões minimalistas; mantém o atlas da bancada aprovado pelo usuário. Os prompts exatos e a associação de cada arquivo ao componente estão em [prompts.json](../workshop/prompts.json). Os PNGs são preservados sem repintura ou redimensionamento por scripts; os geradores leem suas dimensões reais.

Cada atlas tem quatro colunas e quatro linhas: cor principal e sombras; cinzas neutros; lente apagada/acesa; borracha e material das marcações. Os componentes amostram patches de 256 × 256, incluindo os poucos blocos de luz/sombra, e de 64 × 64 nas lentes. Os corpos e curvas usam a mesma família de cor; fios e junções usam somente a última linha preta. Consulte `tools/workshop_material.py` para o mapeamento.

O padrão é simples, com linguagem visual de texturas voxel 32 × 32: áreas quase sólidas e poucas sombras amplas, sem circuitos desenhados, rebites, faixas de cobre ou ruído fino. Isso descreve o estilo; os arquivos fonte não têm resolução 32 × 32. AND é oliva, OR azul, NOT ocre, NAND vermelho suave, NOR roxo, XOR turquesa e XNOR índigo. Lentes desligadas são cinza escuro; ligadas, vermelhas. O histórico Git conserva os atlas anteriores.

A bancada conserva madeira, ferro, cobre e seus UVs originais de 128 × 128. O AND aberto em cima dela é decorativo e usa o atlas próprio da bancada, sem acompanhar mudanças nos materiais dos módulos elétricos.

O catálogo [catalog.json](../workshop/catalog.json) reúne as paletas, categorias e receitas dos onze itens. O gerador copia cada atlas para seu caminho runtime, sem alterar seus pixels. Os ícones são renders dos modelos reais, produzidos por `tools/model_icons.py`.
