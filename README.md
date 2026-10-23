# LogicGates

Mod educacional de circuitos para **Hytale 0.7.4**, versão **1.3.1**. Adiciona alavanca, cabo, lâmpada e portas AND, OR, NOT, NAND, NOR, XOR e XNOR, com orientação pelo olhar e conexões visuais automáticas.

As conexões são horizontais. As portas têm três entradas opcionais e uma saída frontal; NOT aceita somente a entrada traseira. Entradas vazias são ignoradas e, sem nenhuma entrada conectada, todas as portas ficam desligadas. Cada entrada física conta separadamente para XOR/XNOR. Somente a intenção da alavanca é persistida; os sinais são reconstruídos ao carregar o mundo.

## Compilar e instalar

Requisitos: **JDK 25**, **Python 3.10+** (somente biblioteca padrão) e internet na primeira resolução das dependências. O Gradle 9.2.0 é fornecido pelo wrapper.

```powershell
.\gradlew.bat build
```

No Linux/macOS: `bash ./gradlew build`.

O comando compila, executa os testes e confere versões, documentação, traduções, assets gerados e o conteúdo do JAR. Entrega:

- `build/libs/LogicGates-1.3.1.jar`: mod para a pasta `mods` de um servidor Hytale 0.7.4.
- `build/releases/LogicGates-1.3.1.zip`: JAR, SHA-256, identificação da compilação, documentação e fontes de arte.
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

No console, acrescente `--world=nome` à inspeção. Veja o [guia de ensino](docs/teaching.md). Ao relatar um problema, informe a identificação da compilação e descreva o circuito.

## Documentação

Comece pelo [índice](docs/README.md). Consulte [desenvolvimento e automação](docs/development.md), [regras e arquitetura](docs/architecture.md), [edição de arte](docs/assets.md), [validação no cliente](docs/validation.md) e [changelog](CHANGELOG.md).

Classroom, traduções adicionais e conexões entre alturas estão no planejamento; ainda não integram esta versão. A CI executa as verificações portáveis em Windows e Linux. A integração nativa tem um workflow manual para runner próprio com os assets do jogo.

Para medir o avaliador: `.\gradlew.bat benchmarkCircuit`. Consulte [desempenho](docs/performance.md) para limites e interpretação.

A versão 1.3.1 simplifica as texturas: uma cor suave por porta, luz e sombra discretas, cabos pretos e ícones atualizados. Corrige faces sobrepostas no AND decorativo da Bancada LogicGates. A categoria criativa e as receitas continuam disponíveis. Veja [fabricação](docs/crafting.md) e [arte editável](docs/assets.md). O ZIP inclui as fontes da bancada e os atlas.
