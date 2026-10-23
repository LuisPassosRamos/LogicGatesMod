# Changelog

As versões seguem `MAJOR.MINOR.PATCH`: quebra de compatibilidade, incremento compatível e correção compatível. Pré-versões usam, por exemplo, `1.2.0-rc.1`. O manifest de produção é a fonte única da versão do mod e do alvo Hytale. Cada entrega deve atualizar a seção correspondente antes de compilar.

## [Unreleased]

Nenhuma alteração registrada após 1.2.0.

## [1.2.0] - 2026-10-23

### Ensino e localização

- Tradução completa pt-BR dos itens, ajuda e diagnósticos, com en-US como base e verificação de chaves/parâmetros entre línguas.
- Letras A/B/C e seta de saída nos sete módulos, inclusive sem cabos conectados; NOT identifica somente C e saída. Texturas anteriores preservadas.
- `/logichelp` explica entradas opcionais, paridade, limitações e gera tabelas verdade com 1-3 entradas usando o avaliador real.
- `/logicinspect x y z` informa orientação, cada entrada física, saída e motivo do resultado: lógica, ausência de entradas, seção indisponível ou realimentação indefinida. Não carrega seções.

### Manutenção e medições

- Catálogo de metadados compartilhado entre Java, geradores e auditoria do JAR; identificadores existentes preservados.
- Onze exportações originais movidas para `assets/models/`, separadas dos recursos gerados de runtime; fluxo Blockbench atualizado.
- `/logicenergy` acrescenta tempos da avaliação completa, avaliador e monitoramento; janela limitada a 256 amostras.
- `benchmarkCircuit` mede mediana, p95 e alocação em linhas, grades, cadeias das sete portas e redes independentes.
- Novos testes de inspeção, tabelas, idiomas, marcações, métricas e comandos/permissões no servidor real.
- Repositório próprio e histórico organizado por incrementos; CI portável preparada para execução remota.

Classroom e conexões verticais continuam fora deste incremento. Teste com dois jogadores fica a cargo do usuário.

## [1.1.0] - 2026-10-23

### Organização

- Documentação consolidada em um índice: arquitetura, desenvolvimento, arte, validação, referências, histórico e funcionalidades futuras.
- Removidos o exemplo legado, cópias extensas de documentação externa e placeholders sem uso `EnergyNodeComponent`, `ToggleSourceComponent` e `BlockContextAware`.
- Projeto Blockbench original de NAND transferido para a área de autoria, fora dos recursos de produção. Fontes de arte preservadas.
- `build/` passa a conter somente saídas reproduzíveis, relatórios e universos temporários; mundos de desenvolvimento permanecem fora dele.

### Build e diagnóstico

- JAR e pacote de entrega identificados pela versão, com checksum SHA-256 e `release.json`.
- Identificação por versão e impressão digital das fontes no log de inicialização e em `/logicenergy`; revisão Git e estado de alterações registrados no pacote.
- Build não regrava o manifest de origem. Plugin de integração recebe a mesma versão e dependência exata do mod.
- `build` inclui testes, conferência dos quatro geradores, traduções, links locais e auditoria do JAR. `fullCheck` acrescenta integração em servidor real e reinício.
- CI para Windows/Linux e workflow manual de integração nativa em runner próprio. Assets do jogo não são distribuídos.

### Compatibilidade

- Mantidos Hytale 0.7.4, identificadores de blocos/componentes, persistência e regras de circuitos.
- Classroom, novas línguas e transmissão vertical continuam planejados.

## [1.0.0] - Estado anterior, sem release formal

O manifest anterior declarava 1.0.0, mas as entregas locais usavam um nome fixo de JAR, sem uma release ou tag formal correspondente. Esta entrada registra o conjunto anterior, sem atribuir retroativamente uma versão às compilações históricas.

- Alavanca, cabo e lâmpada; conexões horizontais, máscaras visuais, colisão por peça e estados reconstruídos.
- AND, OR, NOT, NAND, NOR, XOR e XNOR; entradas opcionais, isolamento de faces e tratamento de ciclos sem memória.
- Modelos editáveis de XOR/XNOR e atlas metálico com contraste entre base e indicador.
- Última referência antes da organização: 733 testes JUnit e 44 grupos no servidor. Consulte o [histórico de validação](docs/history/validation.md).
