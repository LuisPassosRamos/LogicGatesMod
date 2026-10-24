# Documentação do LogicGates

Estado atual: pré-versão **1.4.0-rc.4**, Hytale **0.7.4**; base de circuitos consolidada em **1.3.3**. Classroom implementa o MVP administrativo; rc.4 corrige a desconexão de UI relatada na rc.3 e requer novo teste no cliente. O fluxo central da prova mínima de rc.1 foi confirmado em pt-BR. Este índice substitui a leitura paralela dos roteiros antigos e do pacote `docs-hytale`.

## Uso e manutenção

| Documento | Quando consultar |
| --- | --- |
| [Regras e arquitetura](architecture.md) | Entender o circuito e localizar responsabilidades no código |
| [Desenvolvimento e automação](development.md) | Compilar, testar, versionar e localizar artefatos |
| [Arte e arquivos gerados](assets.md) | Editar modelos, texturas e gerar variantes |
| [Ensino e comandos](teaching.md) | Usar ajuda, tabelas verdade, inspeção e idiomas |
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
| [Classroom](roadmap/classroom.md) | Plano aprovado; MVP administrativo implementado para validação, atividades posteriores |
| [Conexões entre alturas](roadmap/vertical-connections.md) | Avaliação de viabilidade; implementação pendente |

A estrutura de autoria permanece em `assets/`; recursos necessários ao jogo ficam em `src/main/resources/`. O código de referência legado foi removido depois de verificar que não era dependência do mod. `build/`, caches e mundos locais não são documentação nem fontes do projeto.

- [Inventário e fabricação](crafting.md): categoria criativa, bancada e receitas.
