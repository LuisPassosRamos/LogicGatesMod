# Classroom — turma persistida e validação da interface

Disponível em **1.4.0-rc.2**, Hytale **0.7.4**. O incremento 1 permite ao professor criar sua única turma e editar nome/descrição, com vínculo e SQLite compartilhados pelo servidor. O fluxo central da prova mínima em pt-BR foi confirmado em rc.1, conforme o registro abaixo. **Fluxo central de rc.2 confirmado pelo usuário em pt-BR**, conforme o registro abaixo. Ingresso de alunos, convites e mensagem do dia entram nos incrementos seguintes do [plano aprovado](roadmap/classroom.md).

## Instalação e funcionamento

Compile com `.\gradlew.bat build`. O pacote é `build/releases/LogicGates-1.4.0-rc.2.zip` e o mod é `build/libs/LogicGates-1.4.0-rc.2.jar`. Instale somente esse JAR de produção na pasta `mods` de um ambiente de testes 0.7.4, substituindo a versão anterior, e reinicie para recarregar os assets. Preserve os mundos de desenvolvimento. O driver SQLite e suas licenças já estão no JAR; não é preciso instalar um banco externo.

Qualquer jogador no mundo pode abrir `/classroom`. Para criar, consultar e editar a própria turma como professor, o servidor verifica `sisso.logicgates.classroom.teach` em cada operação, inclusive depois de a página estar aberta. Contas sem autorização recebem uma mensagem de acesso; não recebem os dados privados da turma. No console, `/classroom` exige um jogador.

O administrador pode conceder/remover a permissão pelo console nativo 0.7.4, substituindo `<UUID>` pela identidade técnica do jogador:

```text
perm user add <UUID> sisso.logicgates.classroom.teach
perm user remove <UUID> sisso.logicgates.classroom.teach
```

Com permissão, `/classroom` mostra um formulário para criar a turma ou editar a existente. Nome obrigatório de 1–64 caracteres; descrição opcional de até 2048, com quebras de linha. O servidor valida os campos e preserva o conteúdo como texto literal. **Recarregar** busca os dados atuais e substitui o rascunho. Uma edição desatualizada recebe erro e conserva os campos para revisão.

Fechar/dispensar não cancela uma gravação já aceita. A operação pode concluir e será vista ao reabrir; sua resposta não atualiza uma página encerrada ou substituída. O idioma acompanha a tradução nativa do cliente, en-US ou pt-BR. O rodapé identifica a compilação.

## Dados e backup

O plugin registra no log `Classroom database: <caminho absoluto>`. Esse é o `classroom.db` de `getDataDirectory()`, compartilhado entre mundos. Esquema v1 com `application_id` próprio: turma, professor e vínculo ativo são gravados na mesma transação. Índices garantem vínculo único por jogador e professor responsável único. Revisões impedem sobrescrever uma edição concorrente.

O banco usa conexões curtas, journal padrão DELETE, gravações serializadas e I/O fora do tick. Um banco incompatível, corrompido ou com esquema incompleto torna Classroom indisponível e preserva o arquivo; o plugin mantém os sistemas de circuitos registrados. Nenhuma recuperação apaga/recria dados automaticamente.

Para backup, encerre o servidor normalmente e copie o diretório de dados do plugin para uma pasta externa a `build/`. Para restaurar, mantenha o servidor parado, preserve uma cópia do diretório atual e restaure o backup completo no mesmo local. Use backup de encerramento limpo; não copie apenas o arquivo principal de um banco ativo. Migrações futuras exigem backup prévio, versão explícita e teste de rollback. rc.1 não tinha banco, portanto rc.2 apenas cria o esquema inicial.

Os comandos de testes e `runServer` habilitam acesso nativo da JVM. Em execução direta de servidor Java 25, use `--enable-native-access=ALL-UNNAMED` antes de `-jar`.

## Roteiro no cliente — incremento 1

**Fluxo central confirmado em 24/10/2026**, build `1.4.0-rc.2+a04a304d984f`: turma existente preservada após sair/entrar no mundo e usar Recarregar; contador incrementa e reinicia ao fechar/reabrir. O relato não confirma separadamente edição, revogação, en-US ou encerramento completo do processo do servidor. Os demais casos abaixo permanecem pendentes.

![Turma persistida em rc.2](evidence/classroom/2026-10-24-classroom-rc2-pt-BR.png)

| Caso | Procedimento | Esperado |
| --- | --- | --- |
| Sem autorização | Abrir com conta sem OP e sem permissão docente | Mensagem de acesso; dados privados e formulário indisponíveis |
| Criar | Conceder a permissão, recarregar e criar nome/descrição | Turma salva; mesmo formulário passa a Salvar alterações |
| Validar | Enviar nome vazio/espaços | Erro traduzido, sem criar ou alterar dados |
| Texto literal | Usar acentos, apóstrofo, `%server.fake`, `<b>texto</b>` e descrição em várias linhas | Conteúdo preservado como texto, sem interpretação como tradução ou layout |
| Editar | Alterar campos, salvar, fechar e reabrir | Mesma turma, com valores atualizados |
| Reiniciar | Encerrar completamente o servidor, iniciar e abrir de novo | Mesma turma e vínculo, sem criar outra |
| Revogar | Retirar permissão com a página aberta e tentar salvar | Operação negada; formulário ocultado; dados preservados |
| Página encerrada | Salvar e fechar/reabrir durante a operação | Dados salvos aparecem ao reabrir; resposta antiga não altera outra página |
| Idiomas/layout | Conferir campos, descrição com rolagem, botões e erros em en-US e pt-BR | Conteúdo legível, sem cortes ou sobreposição na escala usada |
| Diagnóstico | Executar `/classroom --probe` | Contador independente, com o fluxo já conhecido |

Registrar build ID, data, idioma, resolução/escala e resultado por caso. O checksum fica em `build/libs/LogicGates-1.4.0-rc.2.jar.sha256` e a identidade em `build/release-metadata/release.json`.

## Prova mínima — registro histórico de rc.1

A página de contador permanece em `/classroom --probe`: envia somente a ação, mantém o contador por abertura e reinicia em zero ao reabrir. Em rc.1, o comando era `/classroom` sem flag.

### Roteiro de regressão da prova mínima

**Fluxo central confirmado pelo usuário em 24/10/2026**, no build `1.4.0-rc.1+43804c498938`, Hytale 0.7.4, pt-BR: abriu `/classroom`, clicou dez vezes com atualização em todas, fechou, reabriu pelo comando e confirmou o contador resetado. A captura abaixo mostra contador 10, rótulos legíveis e o mesmo build ID. Isso permite iniciar o incremento 1; Esc, reconexão, en-US, jogador sem OP e dois jogadores continuam sem confirmação específica. A resolução/escala completa não foi informada.

![Classroom em pt-BR com contador 10 e build ID visível](evidence/classroom/2026-10-24-probe-pt-BR.png)

| Caso | Procedimento | Esperado |
| --- | --- | --- |
| Abrir | Entrar com jogador sem OP e executar `/classroom --probe` | Página e dois botões legíveis; contador 0; build ID visível |
| Evento/atualização | Clicar cinco vezes em Incrementar contador | 1, 2, 3, 4 e 5; botões continuam respondendo após cada atualização |
| Fechar | Usar Fechar e movimentar o jogador | Página some e o controle do jogo retorna |
| Reabrir | Executar `/classroom --probe` novamente | Nova página com contador 0 |
| Dispensar | Pressionar Esc e reabrir pelo comando | Jogo volta ao controle; nova página responde normalmente |
| Substituir página | Com a página aberta, executar o comando novamente se o cliente permitir | Página atual funcional, sem atualização de uma abertura antiga |
| Reconectar | Desconectar com a página aberta, entrar e usar `/classroom --probe` | Nenhuma página residual; novo contador 0 e eventos funcionais |
| Idiomas | Repetir abrir/clicar/fechar em en-US e pt-BR | Título, instrução, contador e botões traduzidos; parâmetros e acentos corretos |
| Layout | Conferir na resolução e escala de UI usadas no piloto | Texto, rodapé e botões cabem; sem sobreposição ou corte |
| Dois jogadores | Abrir nos dois clientes e clicar apenas em um | Somente o contador da página desse jogador muda |

O registro acima corresponde exclusivamente ao build de rc.1 indicado na captura. Nas regressões posteriores, informar a identidade da nova compilação.

## O que a automação comprova

- JUnit usa builders, codec de evento e codec de mensagem reais da dependência 0.7.4. A saída para o transporte é capturada: não há cliente simulado nem renderização.
- Confere documento empacotado, alvos dos bindings, ações, atualizações parciais, contador independente, valor enviado pelo cliente ignorado e eventos posteriores ao encerramento ignorados.
- Ações desconhecidas ou ausentes são reconhecidas sem alterar o contador, liberando o bloqueio de interação do cliente.
- Python verifica chaves/parâmetros dos dois idiomas, referências `%server.logicgates.*` em `.ui` e presença dos documentos no JAR distribuído.
- A integração nativa confere carregamento das traduções e registro de `/classroom` pelo caminho exclusivo de jogadores, antes/depois de reiniciar. Não abre a página em um cliente.
- O incremento 1 acrescenta testes de domínio, SQLite, rollback, esquema incompatível, concorrência e revogação; um teste isolado carrega o driver apenas do JAR de produção. A matriz CI executa esse teste em Windows/Linux; a execução local não equivale a uma nova aprovação remota de Linux.
- A integração usa o serviço do plugin de produção, a permissão nativa e o driver empacotado para criar/editar e conferir a mesma turma após reinício. Os campos e a renderização do novo formulário dependem do roteiro do cliente.

Após validar o formulário do incremento 1, avançar ao incremento 2: ingresso, convites e membros. O plano completo permanece na branch, com seus critérios e pendências explícitos.
