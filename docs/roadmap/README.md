# Objetivo educacional e próximos incrementos

O TCC propõe ensinar lógica de programação de forma gamificada por meio de circuitos no Hytale. A primeira meta era alavanca, cabo e lâmpada; o MVP incluía AND, OR e NOT, com símbolos reconhecíveis. O conjunto atual já contém também NAND, NOR, XOR e XNOR. A descrição inicial foi consolidada aqui para não manter um segundo briefing desatualizado.

O núcleo de circuitos está implementado. O produto educacional completo ainda depende da experiência de aprendizagem, criação de atividades, entrega e acompanhamento pelo professor. Não há escopo fechado que permita um percentual confiável de conclusão do TCC.

## Prioridades propostas

1. Iniciar o incremento 1 do [plano Classroom](classroom.md): autorização docente e turma persistida em SQLite. O usuário confirmou o [fluxo central da prova mínima](../classroom.md) em pt-BR; os demais casos de cliente seguem discriminados no roteiro.
2. Completar verificações manuais específicas ainda abertas: drops nesta entrega, cancelamento/reembolso da fabricação e abertura dos projetos no Blockbench. Multiplayer, circuitos e crafts foram aprovados pelo usuário em 23/10/2026; consultar o [registro](../validation.md).
3. Usar `benchmarkCircuit`, `benchmarkServer` e `/logicenergy` para acompanhar circuitos maiores. Invalidação por região depende de evidência de custo e continua fora desta correção.
4. Acrescentar outras línguas seguindo o contrato existente. **en-US e pt-BR já estão implementados**, com validação de chaves, parâmetros e referências atuais. Traduzir futuras telas ao introduzi-las.
5. Implementar [degraus de cabos](vertical-connections.md) em incremento próprio, após validar o protótipo físico/visual e suas regras de apoio.

Não há placeholders Java para esses incrementos. Serviços, persistência e UI devem entrar junto com uma funcionalidade testável, mantendo o domínio separado da API do jogo.

## Entregas e avaliação

A ideia inicial considerava entregar uma área como prefab para o professor colar e avaliar, ou informar coordenadas de uma construção. Essas continuam alternativas, não contratos definidos. Antes de implementar, decidir como fixar uma revisão da entrega, quais construções pertencem ao aluno e se a correção será manual, automática ou ambas.

Para avaliação lógica automática, preferir snapshot e casos de tabela verdade sem acionar alavancas no mundo ao vivo. Aceitar circuitos equivalentes quando o objetivo for comportamento; limitações de número/tipo de portas precisam ser critérios separados. Seção indisponível ou revisão alterada é erro de avaliação/contexto, não resposta lógica incorreta do aluno.
