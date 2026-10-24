# Desenvolvimento, versões e automação

## Requisitos e comandos

Use **JDK 25**, **Python 3.10+**, Gradle Wrapper 9.2.0 e ScaffoldIt 0.2.16. Os scripts Python usam somente a biblioteca padrão. A dependência `com.hypixel.hytale:Server:0.7.4` é resolvida pelo build; o teste de servidor também exige os assets de uma instalação local do jogo.

| Comando no Windows | Resultado |
| --- | --- |
| `.\gradlew.bat build` | Compila, executa JUnit e testes das ferramentas, confere projeto/assets/JAR e monta o ZIP |
| `.\gradlew.bat check` | Testes e conferências, sem montar o ZIP de entrega |
| `.\gradlew.bat verifyGeneratedAssets` | Confere os sete geradores sem reescrever fontes |
| `.\gradlew.bat serverTestJar` | Empacota somente o plugin isolado de integração |
| `.\gradlew.bat benchmarkCircuit` | Mede avaliador em 12 cenários; grava JSON sem limiar artificial de aprovação |
| `.\gradlew.bat benchmarkServer` | Mede monitoramento e publicação em seções reais; requer os assets locais |
| `.\gradlew.bat fullCheck` | Build completo e integração real com reinício |
| `.\gradlew.bat runServer` | Abre o ambiente de desenvolvimento em `devserver-074/` |
| `.\gradlew.bat clean` | Remove as saídas em `build/` |

No Linux/macOS, use `bash ./gradlew` no lugar de `.\gradlew.bat`. Para selecionar outro Python: `-PpythonExecutable="C:/Python313/python.exe"`. Os caminhos de executáveis são argumentos separados, inclusive com espaços.

Para a integração, configure `HYTALE_ASSETS` ou execute:

```powershell
.\gradlew.bat fullCheck -PhytaleAssets="D:/Hytale/Assets.zip"
```

Depois de `build serverTestJar`, também é possível chamar `python tools/run_server_tests.py --assets "D:/Hytale/Assets.zip"`. `--keep-world` preserva o universo de uma execução aprovada; falhas sempre o preservam para investigação. `--server-jar` seleciona um JAR local, cuja versão é conferida antes de iniciar.

`runServer` mantém a configuração de assets do launcher do ScaffoldIt. O Devtools antigo foi excluído do runtime por incompatibilidade com 0.7.4; recompile e reinicie após alterações Java. O build não regrava o manifest de origem.

## Fonte única de versão

Edite `Version` em `src/main/resources/manifest.json` e registre a mesma versão em [CHANGELOG.md](../CHANGELOG.md). `ServerVersion` é também a fonte única do alvo Hytale, consumida pelo settings Gradle, ferramentas e plugin de integração. Não há uma segunda versão do mod em `gradle.properties`.

- PATCH: correção compatível, por exemplo `1.1.1`.
- MINOR: funcionalidade compatível, por exemplo `1.2.0`.
- MAJOR: alteração incompatível de contrato, dados ou uso; documentar migração.
- Pré-versão: `1.2.0-rc.1` para testes de um incremento ainda não consolidado.

O JAR recebe `logicgates-build.properties`: versão, alvo Hytale, revisão Git, indicador de alterações locais e SHA-256 das fontes/configuração de produção. O ID curto usa `versão+12 caracteres da impressão digital` e aparece na inicialização e em `/logicenergy`. O fingerprint não inclui docs, testes ou mundos. Ele identifica conteúdo de produção; o SHA-256 do JAR identifica o arquivo exato. Revisão/estado Git também podem alterar os bytes do pacote.

`build/release-metadata/release.json` registra esses dados, tamanho e checksum. Mesmo duas compilações ainda chamadas 1.1.0 podem ser distinguidas. Para relatar um teste, informe ID, versão do jogo, cenário e resultado; envie o checksum quando precisar comparar arquivos.

## Pastas e limpeza

| Pasta | Conteúdo e política |
| --- | --- |
| `src/main/` | Fontes Java e recursos necessários ao runtime; os caminhos dos assets são contratos |
| `src/test/` | JUnit, verificações de assets/API e plugin de integração separado |
| `tools/` | Geradores, identidade de build, verificadores e runner nativo |
| `assets/` | Projetos de autoria e atlas fonte; não são saída descartável |
| `docs/` | Documentação mantida e planos explicitamente separados |
| `.github/workflows/` | Automação portável e integração nativa opcional |
| `build/libs/` | JAR versionado, JAR de fontes e checksum |
| `build/releases/` | ZIP de entrega verificado |
| `build/reports/` e `build/test-results/` | Relatórios atuais, regeneráveis |
| `build/tmp/server-integration/` | Universos descartáveis, separados de relatórios |
| `devserver/`, `devserver-074/` | Mundos/configurações locais: preservar; não versionar nem limpar com `clean` |
| `.gradle/`, `.idea/` | Cache/configuração local, ignorados no Git |

`build/` inteiro é descartável. Não guardar nele artes originais, única cópia de mundos, decisões de projeto ou evidências históricas indispensáveis. O runner remove somente seu universo temporário aprovado e mantém logs/resultados em `build/reports/server-integration/<execução>/`. `clean` remove também esses relatórios; exporte evidências que precisem durar.

A limpeza inicial de 23/10/2026 tem uma cópia externa em `../LogicGatesMod-backups/20261023-polish-before.zip`, fora de `build/` e do repositório. É um resgate local, não uma dependência de build ou um artefato para distribuir.

## CI e entrega

O repositório próprio é [LuisPassosRamos/LogicGatesMod](https://github.com/LuisPassosRamos/LogicGatesMod), privado. O remote `origin` aponta para ele; `template` preserva a origem do scaffold.

O workflow `ci.yml` roda em push, pull request e disparo manual, em Windows e Linux, com Java 25/Python 3.13. Executa `build benchmarkCircuit`, publica o ZIP aprovado e conserva relatórios. O pacote inclui a documentação; seus links locais e checksum também são conferidos. As actions estão fixadas por commit. A resolução inicial das dependências requer rede.

`server-integration.yml` executa `fullCheck benchmarkServer`, preserva relatórios funcionais e de carga; é manual e requer um runner próprio com o rótulo `hytale-0.7.4`, Java/Python, acesso às dependências e `HYTALE_ASSETS` configurado para o arquivo licenciado. Ele executa `fullCheck` e publica somente resultados/entrega, sem copiar assets ou o servidor do jogo para os artifacts. Não executar automaticamente código de pull requests externos nesse runner.

O push dos commits dispara a CI portável no GitHub. A integração nativa continua dependendo de runner próprio com os assets; `fullCheck` pode ser executado localmente sem instalar um runner. Tags identificam marcos de fonte; o build não cria tags ou releases remotas automaticamente.

Antes de distribuir: atualizar versão/changelog, executar `fullCheck`, realizar o roteiro de cliente e compartilhar o ZIP correspondente. Registre commits por incremento e crie a tag `vX.Y.Z` quando a versão tiver sido aprovada; nenhuma tag é criada automaticamente pelo build.

## Acrescentar outra língua

Copiar `Server/Languages/en-US/server.lang` para a pasta do novo locale e traduzir valores mantendo todas as chaves e parâmetros `{nome}`. `verifyProject` rejeita chaves faltantes/extras, duplicadas, valores vazios e parâmetros divergentes; verifica referências literais em Java, nomes/descrições dos itens e referências `%server.logicgates.*` nos documentos `.ui`. Atualmente são en-US e pt-BR. As mensagens usam a tradução nativa do Hytale, com resolução de idioma pelo cliente/servidor e língua base; textos de engenharia nos logs/scripts permanecem em inglês.

Os documentos Custom UI ficam em `src/main/resources/Common/UI/Custom/`. A auditoria de distribuição exige sua presença no JAR. Testes de builders, eventos e codecs não interpretam o layout no cliente: executar também o [roteiro Classroom](classroom.md) antes de expandir as telas. A prova central de rc.1 foi confirmada em pt-BR; rc.2 acrescenta o formulário e SQLite, com validação própria pendente.

SQLite JDBC 3.53.4.0 está fixado no Gradle e incorporado por `classroomRuntime`, com licenças e recursos nativos, sem empacotar Hytale. `test` depende do JAR e inclui uma execução isolada do driver no pacote final, também na matriz Windows/Linux. Os testes, o runner nativo e `runServer` habilitam `--enable-native-access=ALL-UNNAMED` no Java 25. Banco, backup e recuperação são descritos no [guia Classroom](classroom.md); não guardar a única cópia de dados em `build/`.

O [guia de uso e ensino](teaching.md) descreve comandos e permissões. O [guia de desempenho](performance.md) separa medições do avaliador e do servidor real.

Os sete geradores incluem bancada, categoria e ícones. O ZIP contém fontes de arte e documentação; o JAR permanece exclusivo de runtime. Para editar Java ou regenerar arte, use o checkout do repositório: o ZIP de entrega não inclui Gradle, Java e ferramentas. Veja [arte](assets.md) e [fabricação](crafting.md).

## Contratos para contribuições

- Alterar fontes em `assets/` e regenerar; não corrigir manualmente variantes ou ícones. Não apagar exportações originais ou mundos locais para reduzir o projeto.
- Componente novo exige catálogo lógico (quando elétrico), catálogo da oficina, categoria, ícone, receita e traduções completas. O build rejeita registro incompleto.
- Preservar IDs e codecs existentes; documentar migração antes de alterar dados persistidos. Não persistir resultados derivados dos circuitos.
- Mudanças no motor devem cobrir entrada removida/0/1, multiplicidade física, orientação, ciclos, descarregamento e retorno de seção vazia. A integração deve continuar sem forçar carregamento durante a simulação.
- Medir antes de otimizar. Comparar mesmo cenário, JVM, heap e máquina; publicar mediana/p95 e alocações, sem converter benchmark sem clientes em promessa de alunos suportados.
- Fazer commits por assunto: código/arte e suas regressões, depois documentação/versão. Executar `fullCheck` na revisão final e conservar a identidade do JAR entregue. Criar tags apenas como marcos explícitos; uma tag não substitui o registro de pendências manuais.
