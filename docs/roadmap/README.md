# Objetivo educacional e próximos incrementos

O TCC propõe ensinar lógica de programação de forma gamificada por meio de circuitos no Hytale. A primeira meta era alavanca, cabo e lâmpada; o MVP incluía AND, OR e NOT, com símbolos reconhecíveis. O conjunto atual já contém também NAND, NOR, XOR e XNOR. A descrição inicial foi consolidada aqui para não manter um segundo briefing desatualizado.

O núcleo de circuitos e o MVP pedagógico de atividades, entregas e correção manual estão implementados. A rc.15 acrescenta verificação assistida por tabela verdade e quantidades; pontuação automática e extensões da experiência de aprendizagem continuam posteriores. Não há escopo fechado que permita um percentual confiável de conclusão do TCC.

## Prioridades propostas

1. Concluir a regressão da [candidata de acompanhamento e interface rc.17](../classroom-rc17-acceptance.md), incluindo os [critérios e verificação assistida introduzidos na rc.15](../classroom-rc15-acceptance.md). A rc.14 foi aprovada por relato, com matriz não discriminada. O [fluxo principal de atividades, áreas e avaliação rc.12](../evidence/classroom/2026-10-26-rc12-client.md) foi confirmado por capturas e relato com professor e aluno. O MVP administrativo permanece; cinco cenários breves rc.10 foram confirmados. Aceite de cliente e preparação de 1.4.0 estável continuam explícitos.
2. Completar verificações manuais específicas ainda abertas: drops nesta entrega, cancelamento/reembolso da fabricação e abertura dos projetos no Blockbench. Multiplayer, circuitos e crafts foram aprovados pelo usuário em 23/10/2026; consultar o [registro](../validation.md).
3. Usar `benchmarkCircuit`, `benchmarkServer` e `/logicenergy` para acompanhar circuitos maiores. Invalidação por região depende de evidência de custo e continua fora desta correção.
4. Acrescentar outras línguas seguindo o contrato existente. **en-US e pt-BR já estão implementados**, com validação de chaves, parâmetros e referências atuais. Traduzir futuras telas ao introduzi-las.
5. Implementar [degraus de cabos](vertical-connections.md) em incremento próprio, após validar o protótipo físico/visual e suas regras de apoio.

Não há placeholders Java para esses incrementos. Serviços, persistência e UI devem entrar junto com uma funcionalidade testável, mantendo o domínio separado da API do jogo.

## Entregas e avaliação

O contrato de [entregas de áreas e correção](classroom-learning-contracts.md) está implementado: conteúdo fixo, revisões, preview, cópia temporária e nota/feedback manuais, com fluxo principal confirmado no cliente rc.12. A verificação assistida da rc.15 analisa quantidades e tabela verdade sobre esse conteúdo preservado. Seu novo fluxo ainda exige aceite específico; pontuação automática permanece posterior. A prova nativa não substitui renderização nem comprova autoria da construção.

A verificação assistida usa snapshot e casos de tabela verdade sem acionar alavancas no mundo ao vivo. Uma futura pontuação automática deve preservar esse isolamento e definir como transformar resultados em nota. Aceitar circuitos equivalentes quando o objetivo for comportamento; limitações de número/tipo de portas precisam ser critérios separados. Seção indisponível ou revisão alterada é erro de avaliação/contexto, não resposta lógica incorreta do aluno.
