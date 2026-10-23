# Objetivo educacional e próximos incrementos

O TCC propõe ensinar lógica de programação de forma gamificada por meio de circuitos no Hytale. A primeira meta era alavanca, cabo e lâmpada; o MVP incluía AND, OR e NOT, com símbolos reconhecíveis. O conjunto atual já contém também NAND, NOR, XOR e XNOR. A descrição inicial foi consolidada aqui para não manter um segundo briefing desatualizado.

O núcleo de circuitos está implementado. O produto educacional completo ainda depende da experiência de aprendizagem, criação de atividades, entrega e acompanhamento pelo professor. Não há escopo fechado que permita um percentual confiável de conclusão do TCC.

## Prioridades propostas

1. Consolidar o roteiro de cliente, especialmente dois jogadores, orientação, iluminação e colisão.
2. Validar no cliente as letras A/B/C/seta, a ajuda com tabelas verdade e a inspeção explicativa acrescentadas em 1.2.0.
3. Adicionar português e depois outras línguas, mantendo as mesmas chaves dos itens e traduzindo mensagens de interface/diagnóstico. A versão 1.2.0 já inclui en-US/pt-BR e valida igualdade de chaves e parâmetros. Outras línguas podem seguir esse contrato.
4. Prototipar [Classroom](classroom.md) e validar um exercício simples com alunos/professor antes de ampliar o painel.
5. Medir circuitos grandes e vários jogadores antes de otimizar invalidação por rede/região. A inspeção de porta e as métricas já existem em 1.2.0; aprofundar medições do servidor e multiplayer antes de otimizar.
6. Implementar [degraus de cabos](vertical-connections.md) após validar o protótipo físico/visual e suas regras de apoio.

Não há placeholders Java para esses incrementos. Serviços, persistência e UI devem entrar junto com uma funcionalidade testável, mantendo o domínio separado da API do jogo.

## Entregas e avaliação

A ideia inicial considerava entregar uma área como prefab para o professor colar e avaliar, ou informar coordenadas de uma construção. Essas continuam alternativas, não contratos definidos. Antes de implementar, decidir como fixar uma revisão da entrega, quais construções pertencem ao aluno e se a correção será manual, automática ou ambas.

Para avaliação lógica automática, preferir snapshot e casos de tabela verdade sem acionar alavancas no mundo ao vivo. Aceitar circuitos equivalentes quando o objetivo for comportamento; limitações de número/tipo de portas precisam ser critérios separados. Seção indisponível ou revisão alterada é erro de avaliação/contexto, não resposta lógica incorreta do aluno.
