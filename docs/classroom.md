# Classroom — prova mínima da interface

Disponível em **1.4.0-rc.1**, Hytale **0.7.4**. Este é o incremento 0: uma página para comprovar o caminho cliente → servidor → atualização da página. O [plano aprovado](roadmap/classroom.md) descreve as próximas entregas; turmas, membros, mensagem do dia e banco ainda não existem nesta versão.

## Instalação e funcionamento

Compile com `.\gradlew.bat build`. O pacote é `build/releases/LogicGates-1.4.0-rc.1.zip` e o mod é `build/libs/LogicGates-1.4.0-rc.1.jar`. Instale somente esse JAR de produção na pasta `mods` de um ambiente de testes 0.7.4, substituindo a versão anterior, e reinicie para recarregar os assets. Preserve os mundos de desenvolvimento.

No jogo, execute `/classroom`. Qualquer jogador no mundo pode abrir; não exige OP nem permissão docente. No console, o comando nativo informa que é necessário um jogador.

A página mostra descrição, contador, botão **Incrementar contador**, botão **Fechar** e ID da compilação. Cada clique envia somente a ação; o servidor incrementa e atualiza o rótulo existente. O contador pertence à abertura atual, começa em zero e não é salvo. Fechar/dispensar encerra essa página; reabrir cria outra. Jogadores diferentes têm páginas independentes. O idioma acompanha a tradução nativa do cliente, en-US ou pt-BR.

## Roteiro obrigatório no cliente

**Estado: pendente de execução no cliente.** Este roteiro não foi aprovado por compilação, JUnit ou inicialização do servidor. Não expandir as telas antes de registrar a prova mínima.

| Caso | Procedimento | Esperado |
| --- | --- | --- |
| Abrir | Entrar com jogador sem OP e executar `/classroom` | Página e dois botões legíveis; contador 0; build ID visível |
| Evento/atualização | Clicar cinco vezes em Incrementar contador | 1, 2, 3, 4 e 5; botões continuam respondendo após cada atualização |
| Fechar | Usar Fechar e movimentar o jogador | Página some e o controle do jogo retorna |
| Reabrir | Executar `/classroom` novamente | Nova página com contador 0 |
| Dispensar | Pressionar Esc e reabrir pelo comando | Jogo volta ao controle; nova página responde normalmente |
| Substituir página | Com a página aberta, executar o comando novamente se o cliente permitir | Página atual funcional, sem atualização de uma abertura antiga |
| Reconectar | Desconectar com a página aberta, entrar e usar `/classroom` | Nenhuma página residual; novo contador 0 e eventos funcionais |
| Idiomas | Repetir abrir/clicar/fechar em en-US e pt-BR | Título, instrução, contador e botões traduzidos; parâmetros e acentos corretos |
| Layout | Conferir na resolução e escala de UI usadas no piloto | Texto, rodapé e botões cabem; sem sobreposição ou corte |
| Dois jogadores | Abrir nos dois clientes e clicar apenas em um | Somente o contador da página desse jogador muda |

Registrar por rodada: data, Hytale 0.7.4, versão/build ID mostrado na página, SHA-256 do JAR quando necessário, idioma, resolução/escala, casos executados, esperado/observado e evidência da falha. Obter o checksum em `build/libs/LogicGates-1.4.0-rc.1.jar.sha256` e a identidade em `build/release-metadata/release.json`.

## O que a automação comprova

- JUnit usa builders, codec de evento e codec de mensagem reais da dependência 0.7.4. A saída para o transporte é capturada: não há cliente simulado nem renderização.
- Confere documento empacotado, alvos dos bindings, ações, atualizações parciais, contador independente, valor enviado pelo cliente ignorado e eventos posteriores ao encerramento ignorados.
- Ações desconhecidas ou ausentes são reconhecidas sem alterar o contador, liberando o bloqueio de interação do cliente.
- Python verifica chaves/parâmetros dos dois idiomas, referências `%server.logicgates.*` em `.ui` e presença dos documentos no JAR distribuído.
- A integração nativa confere carregamento das traduções e registro de `/classroom` pelo caminho exclusivo de jogadores, antes/depois de reiniciar. Não abre a página em um cliente.

Após o roteiro aprovado, registrar a identidade do JAR e iniciar o incremento 1: turma persistida e autorização docente. O plano completo permanece na branch, com seus critérios e pendências explícitos.
