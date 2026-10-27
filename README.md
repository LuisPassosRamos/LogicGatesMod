# LogicGates

Mod educacional de circuitos para **Hytale 0.7.4**, candidata **1.4.0-rc.19**. Inclui alavanca, cabo, lâmpada, portas AND, OR, NOT, NAND, NOR, XOR e XNOR, bancada de fabricação e Classroom para turmas e atividades.

## Recursos

- **Circuitos:** orientação pelo olhar, conexões visuais automáticas, indicadores de sinal e reconstrução após carregar o mundo.
- **Turmas:** convite ou solicitação com matrícula e aprovação docente, membros, mensagem do dia e encerramento com confirmação. Cada UUID tem um único vínculo ativo como professor ou aluno.
- **Atividades:** rascunhos privados, publicações versionadas, prazo de Brasília, valor, arquivamento e restauração.
- **Entregas:** seleção de áreas sem OP, revisões preservadas, preview 3D e cópia temporária protegida para correção.
- **Avaliação:** nota e feedback manuais por revisão; verificação assistida de quantidades e tabela verdade sobre a entrega salva, sem alterar a nota ou o mundo.
- **Idiomas:** en-US e pt-BR, incluindo ajuda, diagnósticos e interface.

As conexões são horizontais. Portas têm três entradas opcionais e uma saída frontal; NOT aceita somente a entrada traseira. Entradas vazias são ignoradas e, sem nenhuma entrada conectada, todas as portas ficam desligadas. Cada face conta separadamente para XOR/XNOR. Somente a intenção da alavanca é persistida; os sinais são reconstruídos ao carregar o mundo. Veja [regras e arquitetura](docs/architecture.md).

## Compilar e instalar

Requisitos: **JDK 25**, **Python 3.10+** (somente biblioteca padrão) e internet na primeira resolução das dependências. O Gradle 9.2.0 é fornecido pelo wrapper.

```powershell
.\gradlew.bat build
```

No Linux/macOS: `bash ./gradlew build`.

O comando compila, executa testes e confere versões, documentação, traduções, assets gerados e conteúdo do pacote. Produz:

- `build/libs/LogicGates-1.4.0-rc.19.jar`: mod para instalar.
- `build/releases/LogicGates-1.4.0-rc.19.zip`: JAR, SHA-256, identificação da compilação, documentação e fontes de arte.
- `build/reports/tests/test/index.html`: relatório JUnit.

Com o mundo/servidor parado, instale **somente o JAR de produção** em `mods`, substituindo o anterior para evitar duas cópias do plugin. Guarde o ZIP fora de `mods`; se recebeu apenas o ZIP, extraia dele o JAR. Reinicie para recarregar os assets. O JAR de fontes e o plugin de integração não são necessários para jogar.

Classroom usa **SQLite v6**, com backup automático antes da migração. Preserve o diretório de dados do plugin: banco, pasta privada `submissions` e configuração devem ter backup conjunto, com o servidor parado. Para voltar à rc.14, restaure também o banco v5 compatível; substituir apenas o JAR não desfaz uma migração. Mundos e circuitos são preservados. Veja [dados e recuperação](docs/classroom-learning.md#persistência-e-recuperação).

## Comandos

| Comando | Uso e permissão |
| --- | --- |
| `/logichelp` | Regras e ajuda; `--gate=XOR --inputs=3` mostra tabela verdade |
| `/logicinspect x y z` | Orientação, entradas e resultado; `sisso.logicgates.inspect` |
| `/logicenergy` | Identificação do build, totais e tempos; `sisso.logicgates.debug` |
| `/classroom` | Turma, ingressos e atividades; operações docentes exigem `sisso.logicgates.classroom.teach` e responsabilidade pela turma |
| `/classroom --probe` | Contador de diagnóstico, sem permissão especial |

No console, acrescente `--world=nome` à inspeção. O [guia Classroom](docs/classroom.md) explica a concessão de permissão e o [guia pedagógico](docs/classroom-learning.md) descreve atividades, entregas e correção.

## Desenvolvimento e validação

```powershell
.\gradlew.bat fullCheck
.\gradlew.bat runServer
```

`fullCheck` inclui o build e a integração real com reinício do servidor. Requer `Assets.zip` de uma instalação local; configure `HYTALE_ASSETS` ou `-PhytaleAssets="D:/Hytale/Assets.zip"`. Universos de integração são isolados dos mundos de desenvolvimento. `runServer` usa `devserver-074/`.

Durante a edição, execute verificações direcionadas; reserve a bateria completa para a revisão final ou uma nova alteração que a justifique. Consulte [desenvolvimento](docs/development.md) para comandos, CI Windows/Linux e identidade dos artefatos, e [desempenho](docs/performance.md) para benchmarks.

O fluxo principal de atividades, entregas e notas foi confirmado em multiplayer na rc.12; o polimento rc.14 foi aprovado por relato. Critérios e verificação assistida introduzidos na rc.15 ainda precisam de aceite de interação. O [registro de validação](docs/validation.md) separa automação, relatos, capturas e pendências; o [roteiro da candidata](docs/classroom-acceptance.md) orienta a regressão. Esta candidata não cria tag, merge ou publicação estável automaticamente.

## Documentação e próximos incrementos

Comece pelo [índice da documentação](docs/README.md). Consulte também [arte](docs/assets.md), [fabricação](docs/crafting.md), [changelog](CHANGELOG.md) e [roadmap](docs/roadmap/README.md).

Pontuação automática, logging ampliado, proteção permanente das bancadas dos alunos e conexões entre alturas permanecem fora desta candidata. A verificação assistida já implementada apresenta resultados ao professor e conserva sua decisão sobre a nota.
