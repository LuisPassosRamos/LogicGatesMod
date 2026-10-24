# Atividades — contrato aprovado e etapas pedagógicas

Contrato aprovado pelo usuário em 24/10/2026 para o incremento posterior ao MVP administrativo. A rc.7 documenta essas decisões; não implementa atividades, notas, entregas, APIs, tabelas ou telas. A implementação de atividades será uma entrega separada. O aceite parcial do cliente rc.6 e os testes administrativos pendentes não são aprovação das futuras telas.

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
- O vencimento indica **Prazo encerrado** e mantém a consulta disponível. Não arquiva automaticamente. Regras de recebimento de entregas após o prazo pertencem à etapa futura de entregas.
- Arquivar oculta a atividade dos alunos e conserva rascunho e versões para o professor. Restaurar reapresenta a última versão publicada sem criar uma versão; se vencida, continua indicando prazo encerrado. Uma atividade que só possuía rascunho continua privada após restaurar.
- Futuras entregas e avaliações devem referenciar a versão correspondente. Publicar outra versão não altera silenciosamente enunciado, prazo ou valor usados por uma entrega/avaliação anterior.

Arquivamento de atividade é distinto de arquivamento de turma. O primeiro faz parte deste contrato futuro; o segundo permanece fora do MVP. A rc.7 mantém o esquema SQLite v3 existente e não antecipa migração para atividades.

## Cenários de aceite da implementação posterior

Estes casos são requisitos futuros, ainda não testes executados de atividades:

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

## Entregas e avaliação posteriores

| Contrato | Identidade e conteúdo mínimo |
| --- | --- |
| Entrega | UUID, atividade e versão, aluno UUID, revisão, instante e referência a conteúdo versionado |
| Avaliação | Revisão da entrega avaliada, professor UUID, feedback e resultado referente ao valor da versão correspondente |

Lançamento de notas e feedback ficam para a etapa de avaliação; não haverá lançamento manual antecipado neste primeiro incremento de atividades. Correção deve apontar para uma revisão reproduzível, sem acompanhar alterações posteriores do aluno silenciosamente. Vínculos históricos permitem associar participação à turma sem reaproveitar um vínculo encerrado em novo ingresso.

O formato do conteúdo da entrega permanece em aberto. Coordenadas, seleção de área, snapshot e prefab exigem decisão sobre reprodução, disponibilidade de seções e experiência do professor. A implementação futura usará um adaptador de conteúdo versionado, sem acoplar Classroom ao circuito vivo. Não implementar adaptador fictício antes dessa decisão.

Sequência acordada: implementar publicação/consulta versionadas em incremento separado; decidir conteúdo, entrega e revisão com referências próprias para aluno/professor; implementar feedback manual; depois estudar avaliação automática por casos esperados e snapshots. Falha de disponibilidade de seção é falha de contexto, não resposta lógica incorreta.

Transferência docente, múltiplos responsáveis, exclusão/arquivamento de turma, proteção de áreas e retenção definitiva precisam de contratos específicos. O MVP atual conserva dados e histórico, sem implementar essas operações.
