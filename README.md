# LogicGates

Mod educacional de circuitos para **Hytale 0.7.4**, pré-versão **1.4.0-rc.2**. Adiciona alavanca, cabo, lâmpada e portas AND, OR, NOT, NAND, NOR, XOR e XNOR, com orientação pelo olhar e conexões visuais automáticas. Classroom permite criar/editar a turma do professor com persistência SQLite; o novo formulário aguarda validação no cliente.

As conexões são horizontais. As portas têm três entradas opcionais e uma saída frontal; NOT aceita somente a entrada traseira. Entradas vazias são ignoradas e, sem nenhuma entrada conectada, todas as portas ficam desligadas. Cada entrada física conta separadamente para XOR/XNOR. Somente a intenção da alavanca é persistida; os sinais são reconstruídos ao carregar o mundo.

## Compilar e instalar

Requisitos: **JDK 25**, **Python 3.10+** (somente biblioteca padrão) e internet na primeira resolução das dependências. O Gradle 9.2.0 é fornecido pelo wrapper.

```powershell
.\gradlew.bat build
```

No Linux/macOS: `bash ./gradlew build`.

O comando compila, executa os testes e confere versões, documentação, traduções, assets gerados e o conteúdo do JAR. Entrega:

- `build/libs/LogicGates-1.4.0-rc.2.jar`: mod para a pasta `mods` de um servidor Hytale 0.7.4.
- `build/releases/LogicGates-1.4.0-rc.2.zip`: JAR, SHA-256, identificação da compilação, documentação e fontes de arte.
- `build/reports/tests/test/index.html`: resultados JUnit.

Instale apenas o JAR de produção, substituindo o anterior para evitar duas cópias do mesmo plugin. Reinicie o servidor/cliente para recarregar os assets. O JAR de fontes e o plugin de integração não são necessários para jogar.

## Desenvolvimento e testes

```powershell
.\gradlew.bat fullCheck
.\gradlew.bat runServer
```

`fullCheck` acrescenta a integração real: criação, atualizações, descarga/retorno de seções e reinício do servidor. Requer `Assets.zip` de uma instalação local; aceita `HYTALE_ASSETS` ou `-PhytaleAssets="D:/Hytale/Assets.zip"`. Os mundos temporários são isolados dos mundos de desenvolvimento. `runServer` usa `devserver-074/`.

Os itens, a ajuda e os diagnósticos possuem traduções **en-US e pt-BR**. As portas mostram letras A/B/C e uma seta na saída; NOT usa somente C e saída.

- `/logichelp`: explica as regras; `/logichelp --gate=XOR --inputs=3` mostra uma tabela verdade.
- `/logicinspect x y z`: orientação, entradas, resultado e motivo do estado da porta. Permissão: `sisso.logicgates.inspect`.
- `/logicenergy`: identificação da compilação, totais e tempos medidos. Permissão: `sisso.logicgates.debug`.
- `/classroom`: criar/editar a própria turma; requer `sisso.logicgates.classroom.teach` para operações docentes. `/classroom --probe` mantém o contador de diagnóstico sem permissão especial. Consulte [autorização e roteiro Classroom](docs/classroom.md).

No console, acrescente `--world=nome` à inspeção. Veja o [guia de ensino](docs/teaching.md). Ao relatar um problema, informe a identificação da compilação e descreva o circuito.

## Documentação

Comece pelo [índice](docs/README.md). Consulte [desenvolvimento e automação](docs/development.md), [regras e arquitetura](docs/architecture.md), [edição de arte](docs/assets.md), [validação no cliente](docs/validation.md) e [changelog](CHANGELOG.md).

Classroom está no incremento 1: uma turma por professor, nome/descrição, vínculo e SQLite compartilhados entre mundos. O fluxo central da prova mínima foi confirmado em pt-BR no build `1.4.0-rc.1+43804c498938`; o novo formulário requer sua própria rodada de cliente. O [registro](docs/classroom.md) separa esses casos. O [plano aprovado](docs/roadmap/classroom.md) prevê ingresso/membros e mensagem do dia nas próximas entregas. Conexões entre alturas continuam no planejamento. A CI executa verificações portáveis em Windows/Linux; a integração nativa tem workflow manual com assets locais.

Para medir o avaliador: `.\gradlew.bat benchmarkCircuit`. Consulte [desempenho](docs/performance.md) para limites e interpretação.

A etapa consolidada em 1.3.3 inclui circuitos, fabricação e multiplayer aprovados pelo usuário. Esta pré-versão acrescenta a prova de UI e preserva os recursos existentes. Consulte o [estado de validação](docs/validation.md) para distinguir testes automatizados, aprovação manual e pendências.
