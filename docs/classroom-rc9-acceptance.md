# Classroom rc.9 — roteiro de aceite

**Pendente no cliente.** O usuário ainda não testou a rc.8. Esta candidata acrescenta encerramento de turma pelo professor e preserva as otimizações anteriores; os relatos/capturas da rc.4/rc.6/rc.7 continuam históricos. Atividades permanecem somente especificadas.

Instale somente o JAR rc.9, com o mundo/servidor parado, substituindo o anterior. Guarde o ZIP fora de `mods`. Preserve banco e mundos; há migração v4 com backup automático. Voltar a uma versão antiga exige restaurar também o banco correspondente. O resultado automatizado e a identidade exata da candidata ficam no [registro de validação](validation.md).

## Primeiro teste, quando houver disponibilidade

Use uma turma descartável com professor e aluno. Não é necessário desfazer a turma real para aprovar o recurso.

1. Abra Administração, digite uma alteração sem salvar e selecione Encerrar turma. Confirme que aparece o aviso e o botão de confirmação. Cancele: o texto digitado e os vínculos devem continuar.
2. Deixe o painel do aluno aberto, com outro jogador mantendo solicitação/convite pendente se disponível. Confirme o encerramento: professor volta à criação, aluno à descoberta, turma desaparece da lista e processos pendentes ficam cancelados. Não exigir recarga manual.
3. Professor cria outra turma; aluno solicita novo ingresso com `00123` e recebe aprovação. Reinicie o mundo/servidor para confirmar que só os novos vínculos ficam ativos. Histórico anterior continua consultável, sem sugerir vínculo ativo.

Se não houver conta adicional para manter pendência junto de aluno ativo, registre essa parte como não executada; a transação tem cobertura automatizada. Não solicitar nova aprovação estável com base apenas nesse teste breve.

## Encerramento: cenários complementares

| Caso | Esperado | Evidência |
| --- | --- | --- |
| Confirmação/cancelamento em pt-BR/en-US | Ações fora da rolagem, aviso legível, cancelamento sem alterar turma ou rascunho | Captura da confirmação; resultado relatado |
| Permissão revogada antes de confirmar | Painel bloqueado automaticamente; nenhum encerramento sem autorização | Relato ou captura se falhar |
| Outra página altera dados da turma antes de confirmar | Revisão antiga recusada; recarregar antes de repetir conscientemente | Relato |
| Histórico e banco migrado | Vínculos e conteúdo da turma anterior preservados; nova turma com identidade própria | Relato de reinício e novo ingresso |

## Pendências de interação anteriores

Executar em pt-BR/en-US, 1280×720 e 1920×1080 com escala padrão. Registrar build ID, idioma, resolução, escala e resultado por cenário. As verificações automatizadas não comprovam renderização.

| Caso | O que conferir no cliente | Evidência mínima |
| --- | --- | --- |
| Primeira troca de aba | Comparar demora no host e segundo jogador; informar se o mundo engasga junto | Relato e duração aproximada |
| Linhas e bindings conservados | Selecionar alunos/ingressos, trocar de abas e selecionar novamente; cada botão atua no registro exibido, sem duplicar ações | Relato com professor/aluno |
| Campo local inicialmente vazio | Digitar em descrição/mensagem antes vazia, recarregar para descartar; repetir com revogação/restauração; o texto não salvo não reaparece | Relato; captura se falhar |
| Listas vazias | Rodapé orienta membros, ingressos do professor, histórico do aluno, descoberta e busca de jogadores; não pede seleção inexistente | Captura representativa; demais contextos podem ser relatados |
| Prioridade de mensagens | Confirmação, gravação, erros e resultado da operação não são substituídos pela orientação vazia | Relato; captura se falhar |
| Página posterior esvaziada | Informa página vazia, mantendo Anterior acessível; não afirma que toda a turma ficou vazia | Relato na massa isolada |
| Professor: lista, busca e seleção | Buscar/Anterior/Próxima completos; contador separado; marca de seleção corresponde à ficha | Uma ficha selecionada com controles visíveis |
| Administração e texto extenso | Nome, descrição e mensagem editáveis; rolagem só do conteúdo; Salvar permanece acessível | Editor rolado com Salvar visível |
| Ficha do aluno e confirmação | Matrícula íntegra, mensagem e descrição legíveis; Sair/Confirmar/Cancelar acessíveis | Confirmação e resultado |
| Revogar e restaurar, sem clicar na página | Bloqueio automático, rascunho docente descartado, nenhuma gravação indevida; restauração automática do conteúdo salvo | Tela bloqueada e painel restaurado |
| Permissão efetiva inalterada | Remover OP mantendo permissão docente direta preserva acesso e o rascunho | Relato de acesso e texto preservado |
| Atualização durante edição | Enquanto professor digita, aluno solicita ou sai; atualização não apaga o campo nem adota revisão alheia silenciosamente | Relato; captura em caso de falha |
| Ingresso e erros | Solicitação/convite ainda exigem aprovação; vazia recusada e `00123` preservada; repetir brevemente após novo layout | Painel final e erro representativo |
| Recusa/cancelamento | Recusar convite, cancelar convite e cancelar solicitação encerram só o processo correspondente | Histórico com tipo, situação e instante |
| Mensagem vazia | Some do painel e não envia no próximo login; preenchida envia uma vez por conexão, sem repetir ao trocar de mundo | Relato e chat relevante |
| Lista com mais de 20 | Segunda página acessível, controles coerentes, registro selecionado mantém seu UUID; histórico não sugere múltiplos vínculos ativos | Segunda página e uma ficha |
| Idioma | Rótulos e erros traduzidos, sem chaves cruas; conteúdo do usuário permanece literal | Professor e aluno em en-US |
| Fechar/Esc/reabrir e reconectar | Nenhuma resposta da página anterior reaparece; vínculos e conteúdo persistem | Relato, com captura se falhar |

Matrícula duplicada e disputa entre duas turmas possuem testes automatizados; registrar separadamente se não houver contas suficientes para reteste no cliente.

## Massa sintética isolada

O gerador fica somente nos fontes de teste. Não há comando de carga no mod de produção. Pelo checkout, obtenha os UUIDs das duas contas com `/uuid` e execute, substituindo os valores entre sinais `< >`:

```powershell
.\gradlew.bat prepareClassroomUiFixture -PclassroomFixtureTeacher=<UUID_PROFESSOR> -PclassroomFixtureStudent=<UUID_ALUNO>
```

Cada execução cria um diretório novo em `build/classroom-ui-fixtures/<UUID>/`, com `classroom.db` e instruções. São 25 alunos ativos, 25 solicitações pendentes, mais 25 perfis conhecidos sem vínculo e histórico extenso da conta do aluno. Descrição e mensagem têm 2048 caracteres. O programa usa os serviços de produção e recusa um destino existente.

Para conferir no cliente:

1. Criar um save/servidor exclusivo de teste, com esta versão do plugin. Outro mundo no mesmo servidor compartilha o banco e não é isolamento.
2. Iniciar e encerrar esse ambiente. Identificar no log seu `Classroom database: <caminho>` e confirmar que é diferente do ambiente atual.
3. Com o ambiente de teste parado, colocar o banco sintético somente naquele diretório de dados. Preservar qualquer dado de teste anterior antes da substituição. Nunca usar o diretório da turma real.
4. Iniciar, conceder `sisso.logicgates.classroom.teach` ao UUID docente e conectar as duas contas. Nomes autenticados substituem os nomes fictícios dessas contas.
5. Professor confere Turma/Ingressos/Convidar e página 2. Aluno consulta a própria ficha e sai da turma para abrir o histórico paginado.

Não é necessário apagar a turma real ou alterar os papéis das contas nela. O ZIP de entrega não inclui bancos, mundos ou dados sintéticos. A geração já foi exercitada por teste automatizado; a apresentação da massa no cliente ainda exige esta rodada.
