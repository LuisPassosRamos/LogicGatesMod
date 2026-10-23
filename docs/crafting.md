# Inventário e fabricação

No criativo, abra **LogicGates**, identificada pelo pequeno circuito. As abas **Componentes**, **Portas lógicas** e **Bancadas** reúnem os onze itens. Os estados internos ligados/desligados e de conexão não devem aparecer como itens separados.

Na sobrevivência, fabrique a **Bancada LogicGates** no inventário do personagem, na categoria Ferramentas. Coloque-a num espaço livre de 2 × 1 blocos e interaja para abrir a interface nativa. Os dez componentes são fabricados nessa bancada, sem combustível, energia elétrica ou melhorias de nível.

| Resultado | Ingredientes | Tempo |
| --- | --- | --- |
| 1 bancada | 6 troncos + 4 pedras + 2 barras de cobre + 2 de ferro | Instantâneo |
| 8 cabos | 1 barra de cobre + 1 fibra | 2 s |
| 1 alavanca | 1 barra de cobre + 1 de ferro + 1 graveto | 2 s |
| 1 lâmpada | 1 barra de cobre + 1 de ferro + 1 carvão vegetal | 2 s |
| 1 NOT | 1 barra de cobre + 1 de ferro + 1 fibra + 1 carvão vegetal | 3 s |
| 1 AND ou OR | 2 barras de cobre + 1 de ferro + 1 fibra + 1 carvão vegetal | 3 s |
| 1 NAND ou NOR | 3 barras de cobre + 1 de ferro + 2 fibras + 1 carvão vegetal | 4 s |
| 1 XOR ou XNOR | 3 barras de cobre + 2 de ferro + 2 fibras + 1 carvão vegetal | 4 s |

Troncos e pedras usam os tipos nativos `Wood_Trunk` e `Rock`, aceitando os materiais correspondentes. Cobre representa condutores/contatos, ferro a carcaça, fibras o isolamento e carvão vegetal os contatos de carbono/filamentos. Não há cristais nem consumo de outras portas nas receitas.

A fabricação manual `Fieldcraft` é instantânea no Hytale 0.7.4; atribuir os 3 segundos planejados gera aviso de validação e não cria uma fila no inventário. As receitas da estação conservam os tempos da tabela.

A bancada usa o item `Logic_Workbench`, a estação `LogicGates_Workbench` e o componente nativo `BenchBlock`. O AND sobre a mesa é decorativo e não entra nas contagens de `/logicenergy`. Os nomes e categorias estão traduzidos para pt-BR e en-US.
