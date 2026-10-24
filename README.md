# LogicGates

Mod educacional de circuitos para **Hytale 0.7.4**, pré-versão **1.4.0-rc.9**. Adiciona alavanca, cabo, lâmpada e portas AND, OR, NOT, NAND, NOR, XOR e XNOR, com orientação pelo olhar e conexões visuais automáticas. Classroom implementa o MVP administrativo com turmas, ingresso, matrícula, membros e mensagem do dia em SQLite; a rc.9 acrescenta encerramento de turma e mantém a redução de atualizações redundantes da rc.8. A abertura foi capturada na rc.6 e multiplayer/revogação automática foram relatados após a rc.7; a otimização da candidata exige reteste no cliente.

As conexões são horizontais. As portas têm três entradas opcionais e uma saída frontal; NOT aceita somente a entrada traseira. Entradas vazias são ignoradas e, sem nenhuma entrada conectada, todas as portas ficam desligadas. Cada entrada física conta separadamente para XOR/XNOR. Somente a intenção da alavanca é persistida; os sinais são reconstruídos ao carregar o mundo.

## Compilar e instalar

Requisitos: **JDK 25**, **Python 3.10+** (somente biblioteca padrão) e internet na primeira resolução das dependências. O Gradle 9.2.0 é fornecido pelo wrapper.

```powershell
.\gradlew.bat build
```

No Linux/macOS: `bash ./gradlew build`.

O comando compila, executa os testes e confere versões, documentação, traduções, assets gerados e o conteúdo do JAR. Entrega:

- `build/libs/LogicGates-1.4.0-rc.9.jar`: mod para a pasta `mods` de um servidor Hytale 0.7.4.
- `build/releases/LogicGates-1.4.0-rc.9.zip`: JAR, SHA-256, identificação da compilação, documentação e fontes de arte.
- `build/reports/tests/test/index.html`: resultados JUnit.

Com o mundo/servidor parado, instale apenas o JAR de produção, substituindo o anterior para evitar duas cópias do mesmo plugin. O ZIP é um pacote de entrega: guarde-o fora de `mods`; se recebeu somente o ZIP, extraia dele apenas o JAR de produção para instalar. Reinicie o servidor/cliente para recarregar os assets. O JAR de fontes e o plugin de integração não são necessários para jogar. A rc.9 migra Classroom para SQLite v4 com backup automático, preservando mundos e registros. Para voltar a versões antigas, restaure também o banco compatível.

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
- `/classroom`: acessar turma, ingressos e administração; requer `sisso.logicgates.classroom.teach` para operações docentes. `/classroom --probe` mantém o contador de diagnóstico sem permissão especial. Consulte [autorização e roteiro Classroom](docs/classroom.md).

No console, acrescente `--world=nome` à inspeção. Veja o [guia de ensino](docs/teaching.md). Ao relatar um problema, informe a identificação da compilação e descreva o circuito.

## Documentação

Comece pelo [índice](docs/README.md). Consulte [desenvolvimento e automação](docs/development.md), [regras e arquitetura](docs/architecture.md), [edição de arte](docs/assets.md), [validação no cliente](docs/validation.md) e [changelog](CHANGELOG.md).

Classroom tem uma turma ativa por UUID, como professor ou aluno. Solicitação e convite exigem matrícula e aprovação docente. O MVP inclui administração de membros e mensagem do dia; os fluxos centrais foram confirmados em pt-BR na rc.4, e a candidata rc.9 exige a rodada de cliente descrita no [registro](docs/classroom.md). O [plano aprovado](docs/roadmap/classroom.md) mantém atividades/entregas posteriores. A CI verifica Windows/Linux; integração nativa usa assets locais. Conexões entre alturas continuam fora deste trabalho.

Para medir o avaliador: `.\gradlew.bat benchmarkCircuit`. Consulte [desempenho](docs/performance.md) para limites e interpretação.

A etapa consolidada em 1.3.3 inclui circuitos, fabricação e multiplayer aprovados pelo usuário. Esta pré-versão acrescenta Classroom e preserva os recursos existentes. Consulte o [estado de validação](docs/validation.md) para distinguir testes automatizados, aprovação manual e pendências.
