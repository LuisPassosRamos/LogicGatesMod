# Documentação do LogicGates

Mod para **Hytale 0.7.4**, candidata **1.4.0-rc.18**, com Classroom em **SQLite v6**. Circuitos, administração de turmas, atividades versionadas, entregas de áreas e avaliação manual estão implementados. A verificação assistida compara quantidades e tabela verdade sobre a entrega salva; **não atribui nota automaticamente**.

## Por onde começar

| Necessidade | Documento |
| --- | --- |
| Instalar e conhecer o mod | [README do projeto](../README.md) |
| Criar turma, autorizar professor e administrar alunos | [Classroom administrativo](classroom.md) |
| Publicar atividades, entregar áreas e corrigir | [Atividades e entregas](classroom-learning.md) |
| Usar portas, ajuda, tabelas verdade e inspeção | [Ensino e comandos](teaching.md) |
| Encontrar componentes e fabricar na bancada | [Inventário e fabricação](crafting.md) |
| Conferir o estado de aceite e os testes necessários | [Validação](validation.md) e [roteiro da candidata](classroom-rc18-acceptance.md) |

O fluxo principal de atividades/entregas foi confirmado em multiplayer na rc.12; o polimento rc.14 foi aprovado por relato, sem discriminar toda a matriz de idiomas/resoluções. Os novos critérios e o relatório da rc.15 ainda exigem validação de interação. A rc.17 acrescenta acompanhamento por aluno e polimento da interface: execução automatizada e aceite no cliente são registrados separadamente, sem inferir aprovação estável. A rc.18 teve [reabertura e apresentação docente pt-BR documentadas](evidence/classroom/2026-10-27-rc18-ptbr-client.md) em 1280×720 relatados, com ressalvas visuais e escala não informada. Aluno, inglês, outras dimensões e comportamentos não demonstrados continuam pendentes. Consulte [resultados e evidências](validation.md).

## Manutenção e contribuição

| Documento | Quando consultar |
| --- | --- |
| [Regras e arquitetura](architecture.md) | Entender responsabilidades, simulação, persistência e autorização |
| [Desenvolvimento e automação](development.md) | Compilar, testar proporcionalmente à mudança e identificar o arquivo entregue |
| [Arte e arquivos gerados](assets.md) | Editar fontes de arte e regenerar recursos |
| [Desempenho](performance.md) | Reproduzir benchmarks e interpretar seus limites |
| [Compatibilidade Hytale](reference/hytale-074.md) | Conferir contratos da API 0.7.4 e limitações de Custom UI |
| [Fontes externas](reference/sources.md) | Localizar documentação primária e referências contextualizadas |
| [Changelog](../CHANGELOG.md) | Identificar mudanças por versão |

## Planejamento e registros

| Documento | Finalidade |
| --- | --- |
| [Objetivo educacional e prioridades](roadmap/README.md) | Separar o que existe das extensões futuras |
| [Plano Classroom](roadmap/classroom.md) | Regras aprovadas, incrementos e limites de escopo |
| [Contratos pedagógicos](roadmap/classroom-learning-contracts.md) | Publicações, entregas, correção e verificação assistida |
| [Conexões entre alturas](roadmap/vertical-connections.md) | Proposta ainda não implementada |
| [Revisão de otimização e testes](reviews/2026-10-25-optimization-and-verification.md) | Decisões anteriores sobre custo e verificações proporcionais |
| [Polimento técnico e documental](reviews/2026-10-27-project-polish.md) | Escopo da limpeza rc.16, remoções justificadas e contratos preservados |
| [Viabilidade de logging](reviews/2026-10-26-logging-feasibility.md) | Diagnóstico seletivo proposto, ainda no backlog |
| [Registros de validação](validation.md) e [histórico anterior](history/validation.md) | Resultados por build, com evidências e pendências preservadas |

Guias descrevem o uso atual; planos definem contratos; roteiros e evidências identificam versões específicas. Não interpretar uma pendência histórica como estado atual nem uma aprovação antiga como prova de uma compilação nova. Fontes de autoria ficam em `assets/`, recursos do jogo em `src/main/resources/`; `build/`, caches e mundos locais não são fontes ou documentação mantida.
