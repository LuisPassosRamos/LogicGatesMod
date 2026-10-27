# Atividades — contrato aprovado e etapas pedagógicas

Contrato inicial aprovado em 24/10/2026, ampliado pelo plano de áreas e correção em 26/10/2026. **Implementado como candidata rc.11**, com SQLite v5 e arquivos privados. O [uso atual](../classroom-learning.md) e o [roteiro de aceite](../classroom-rc11-acceptance.md) distinguem o que a automação comprova da seleção/renderização que exige cliente. O texto abaixo conserva as decisões de publicação; entregas e avaliação agora têm implementação.

## Primeiro incremento: publicar e consultar atividades

Somente o professor responsável administra atividades da própria turma, com permissão docente efetiva revalidada no servidor em cada operação. Alunos com vínculo ativo consultam publicações da sua turma. Convites, solicitações e vínculos encerrados não concedem acesso. IDs recebidos da interface nunca autorizam consultas ou alterações.

| Informação | Contrato |
| --- | --- |
| Identidade | UUID da atividade e UUID da turma; nome/matrícula não identificam a atividade ou o aluno |
| Conteúdo publicado | Título, enunciado, prazo e valor, associados à versão publicada |
| Prazo | Obrigatório ao publicar; data/hora informada em `America/Sao_Paulo` (Brasília), com fuso identificado na interface; armazenar o instante em UTC |
| Valor | Positivo, padrão 10 para nova atividade; até duas casas decimais, sem arredondamento silencioso; editável no rascunho |
| Nota apresentada | `—/valor` enquanto não houver avaliação; por exemplo, `—/10`. Futuramente `7/10` indica nota 7 de valor 10, e `0/10` indica zero atribuído |

Rascunhos são privados e podem ficar incompletos. Publicar exige título e enunciado preenchidos, prazo estritamente posterior ao instante atual do servidor e valor válido. Texto de usuário permanece literal; rótulos, erros e estados terão en-US/pt-BR. Não criar nota inicial zero para representar ausência de avaliação. A ficha geral do professor mostra o valor da atividade; nota/valor se refere a um aluno, sem inventar média da turma.

## Publicação, versões e arquivamento

- Criar e salvar rascunho não publica. Publicar é uma ação explícita do professor.
- Cada publicação conserva uma versão imutável de título, enunciado, prazo e valor. Alterar uma publicação prepara um novo rascunho, mantendo a versão atual dos alunos até publicar novamente.
- Rascunho e publicação atual podem coexistir. O rascunho de edição parte da versão publicada; o padrão 10 vale para atividades novas, sem substituir o valor já publicado.
- A consulta inicial do aluno usa a última versão publicada de atividades não arquivadas da turma atual. A data de ingresso não elimina publicações atuais da lista.
- O vencimento indica **Prazo encerrado** e mantém a consulta disponível. Não arquiva automaticamente. **Envio inicial e reenvios atrasados são aceitos e identificados como atrasados**, conforme a decisão posterior.
- Arquivar oculta a atividade dos alunos e conserva rascunho e versões para o professor. Restaurar reapresenta a última versão publicada sem criar uma versão; se vencida, continua indicando prazo encerrado. Uma atividade que só possuía rascunho continua privada após restaurar.
- Futuras entregas e avaliações devem referenciar a versão correspondente. Publicar outra versão não altera silenciosamente enunciado, prazo ou valor usados por uma entrega/avaliação anterior.

Arquivamento/restauração de atividade é distinto do encerramento administrativo de turma implementado na rc.9. Restaurar ou excluir definitivamente uma turma continua fora do MVP. O contrato foi documentado na rc.7 (v3); rc.9/rc.10 usavam v4. A implementação rc.11 acrescenta v5 com backup, sem alterar IDs ou vínculos anteriores.

## Cenários de aceite da implementação posterior

Estes casos orientam regressões automatizadas e aceite no cliente; execução por cenário fica registrada no roteiro da candidata:

| Cenário | Resultado esperado |
| --- | --- |
| Salvar rascunho incompleto | Permitido ao responsável; invisível para alunos |
| Publicar sem título/enunciado, sem prazo ou com valor inválido | Recusado; nenhuma versão publicada parcial |
| Publicar com prazo igual/anterior ao instante atual | Recusado usando o relógio do servidor |
| Publicar e consultar em en-US/pt-BR | Mesmo instante UTC, horário de Brasília identificado e conteúdo literal preservado |
| Editar publicação sem republicar | Alunos continuam vendo a versão atual |
| Republicar com outro prazo/valor | Nova versão; anteriores mantêm seus conteúdos e valores |
| Consultar sem avaliação | `—/valor`; nenhum zero ou registro de nota fictício |
| Vencer, arquivar e restaurar | Vencida continua consultável; arquivada fica privada; restauração conserva versão e prazo |
| Tentar acesso de outra turma, candidato ou ex-membro | Acesso privado recusado no servidor |
| Revogar permissão docente | Administração bloqueada sem transferir responsabilidade ou alterar dados |

## Áreas, revisões e correção — contrato ampliado

- Aluno seleciona dois cantos dentro do fluxo da atividade, sem OP; reabre `/classroom` e confirma dimensões. Sessão temporária pertence à conexão/mundo, expira em 15 minutos e termina ao cancelar, desconectar ou trocar de mundo.
- Servidor captura conteúdo fixo, apenas de regiões carregadas, com construção e circuitos, orientação e intenção das alavancas. Sem inventários, NPCs ou itens soltos; tipos especiais sem reprodução confiável são recusados.
- Cada revisão referencia UUID de aluno e vínculo, atividade/publicação, instante do servidor, arquivo versionado e checksum. Metadados só aparecem após publicação do arquivo. Reenvio preserva anteriores, aceita atraso e começa sem nota.
- Professor consulta atividade → aluno → revisão. Preview 3D é visual; testar/alterar o circuito exige cópia temporária no mundo escolhido. Espaço vazio, proteção por eventos nativos, uma reserva persistente por professor; descarte remove o volume editado, preservando a entrega.
- Nota entre zero e valor **daquela publicação**, feedback e revisão explícitos. Corrigir uma revisão antiga não avalia a mais recente. Histórico de alterações da avaliação é preservado.
- Limites padrão: 32 × 16 × 32 incluindo ar, 8 MiB descomprimidos por revisão, 1 GiB por turma, uma operação pesada + oito aguardando. Listas apenas de metadados; conteúdo só da revisão selecionada. Esses valores não certificam capacidade de jogadores.

A prova no servidor isolado é complementada pelo teste no cliente de seleção, preview, edição e descarte. A seleção atribui o envio a uma conta, sem provar autoria. Transferência docente, múltiplos responsáveis, proteção permanente das bancadas e retenção/exclusão definitiva exigem contratos futuros. Logging seletivo e avaliação automática continuam no backlog.

## Verificação assistida — contrato rc.15

Aprovado em 26/10/2026: quantidades exatas, campo vazio livre e zero proibido; alavancas são entradas e lâmpadas saídas com tabela esperada. Regras pertencem ao rascunho e à publicação correspondente. Nota continua manual. O primeiro incremento usa ordem por coordenadas locais X/Y/Z, tabela completa de até 6 entradas/4 saídas, limite de 2048 componentes para simulação e execução explícita na fila pesada existente. Não bloqueia entregas divergentes. Não há pontuação automática, execução contínua ou histórico persistido dos relatórios. Consulte [uso](../classroom-learning.md#critérios-e-verificação-assistida--rc15) e [aceite](../classroom-rc15-acceptance.md).
