# Documentação do LogicGates

Estado atual: pré-versão **1.4.0-rc.12**, Hytale **0.7.4**; circuitos consolidados em **1.3.3**. O MVP administrativo permanece, com os cinco cenários breves da rc.10 confirmados. A candidata acrescenta atividades versionadas, entregas de áreas, preview/cópias temporárias e avaliação manual. [Uso e persistência](classroom-learning.md); [aceite rc.12](classroom-rc12-acceptance.md). A rc.12 corrige a desconexão na abertura de Atividades da rc.11; o reteste está pendente. A automação e a prova nativa não aprovam seleção/renderização no cliente. Evidências anteriores conservam sua versão e escopo.

## Uso e manutenção

| Documento | Quando consultar |
| --- | --- |
| [Regras e arquitetura](architecture.md) | Entender o circuito e localizar responsabilidades no código |
| [Desenvolvimento e automação](development.md) | Compilar, testar, versionar e localizar artefatos |
| [Arte e arquivos gerados](assets.md) | Editar modelos, texturas e gerar variantes |
| [Ensino e comandos](teaching.md) | Usar ajuda, tabelas verdade, inspeção e idiomas |
| [Atividades e entregas](classroom-learning.md) | Publicação, seleção, revisões, preview, cópias, notas e backup conjunto |
| [Classroom](classroom.md) | Turmas, ingresso, matrícula, membros, mensagem, backup e validação |
| [Desempenho](performance.md) | Reproduzir benchmarks e interpretar os tempos |
| [Validação](validation.md) | Reproduzir testes e conferir aparência, colisão e multiplayer |
| [Compatibilidade Hytale](reference/hytale-074.md) | Adaptar código e avaliar tutoriais de outras versões |
| [Fontes externas](reference/sources.md) | Encontrar documentação primária e guias contextualizados |
| [Changelog](../CHANGELOG.md) | Identificar o que mudou em cada entrega |
| [Histórico de validação](history/validation.md) | Consultar evidências anteriores sem confundir builds antigas com a atual |

## Planejamento

| Documento | Estado |
| --- | --- |
| [Objetivo educacional e prioridades](roadmap/README.md) | Direção do TCC; não é lista de funcionalidades implementadas |
| [Classroom](roadmap/classroom.md) | Plano aprovado; atividades/entregas/avaliação candidatas na rc.11, com aceite de cliente pendente |
| [Conexões entre alturas](roadmap/vertical-connections.md) | Avaliação de viabilidade; implementação pendente |

A estrutura de autoria permanece em `assets/`; recursos necessários ao jogo ficam em `src/main/resources/`. O código de referência legado foi removido depois de verificar que não era dependência do mod. `build/`, caches e mundos locais não são documentação nem fontes do projeto.

- [Inventário e fabricação](crafting.md): categoria criativa, bancada e receitas.

A rc.9 permite ao professor encerrar a turma com confirmação, preservando histórico e liberando participantes. Inclui migração SQLite v4 com backup. Encerramento confirmado por relato, com aceite parcial. A rc.10 preservava o esquema v4; a rc.11 acrescenta v5 e arquivos de conteúdo. [Roteiro atual](classroom-rc15-acceptance.md).

A [revisão de otimização e testes de 25/10/2026](reviews/2026-10-25-optimization-and-verification.md) distingue ajustes de runtime, código sem chamadas internas e execução proporcional das verificações. O [relato rc.9](evidence/classroom/2026-10-25-rc9-client-report.md) confirma o encerramento da turma, mantendo pendências sem confirmação individual.

A [validação breve rc.10](evidence/classroom/2026-10-26-rc10-client.md) distingue as duas capturas dos cinco resultados relatados. A [análise de logging](reviews/2026-10-26-logging-feasibility.md) recomenda ampliar falhas operacionais com volume limitado e diagnóstico temporário, ainda sem implementação.

A rc.15 acrescenta critérios versionados de quantidade/tabela verdade e verificação assistida, mantendo nota manual. [Uso](classroom-learning.md#critérios-e-verificação-assistida--rc15) e [aceite](classroom-rc15-acceptance.md). SQLite v6 migra critérios com backup, preservando dados pedagógicos v5.
