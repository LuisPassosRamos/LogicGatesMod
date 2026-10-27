# Classroom — plano aprovado e estado dos incrementos

Plano inicial confirmado em **24/10/2026**, a partir de `v1.3.3` (`d33a2b5`), ampliado para atividades, entregas e correção em 26/10/2026. Trabalho em `codex/classroom-ui`, sem merge em `main`, tag ou publicação estável automática. Os desenhos de referência orientam o fluxo; várias turmas por UUID e capacidade fixa de 30 alunos não são requisitos.

## Estado atual

A candidata **1.4.0-rc.18** mantém o núcleo de circuitos, o MVP administrativo, atividades versionadas, entregas de áreas, revisões, preview/cópias temporárias, avaliação manual e verificação assistida. SQLite **v6**, arquivos privados imutáveis e leituras agregadas permanecem. A rc.16 é manutenção do código/documentação, sem novo contrato pedagógico ou mudança de esquema.

| Incremento | Implementação e evidência |
| --- | --- |
| Prova mínima de Custom UI | Contador, evento, atualização, fechar/reabrir; confirmado na rc.1 |
| Turma persistida | Autorização, criação/edição e SQLite; persistência relatada na rc.2 |
| Ingresso e administração | Solicitação, convite, matrícula, conferência, membros e mensagem; fluxos centrais confirmados na rc.4 |
| Permissões e notificações | Eventos nativos com revalidação; multiplayer/revogação relatados após rc.7; rc.8 reduz consultas/pacotes redundantes |
| Encerramento de turma | Transação, confirmação e histórico desde rc.9; funcionamento confirmado por relato |
| Leituras agregadas | Uma conexão por consulta; até três SELECTs na lista docente e quatro na ficha de atividades do aluno (rc.17). Cenários breves rc.10 conservam seu escopo |
| Atividades, áreas e avaliação manual | Implementados na rc.11; falha de abertura corrigida na rc.12, cujo fluxo principal foi confirmado com duas contas |
| Polimento de apresentação | Prazo por seletores, criação ampla, contagem por papel e fichas compactas na rc.13; proporções e edição explícita na rc.14, aprovada por relato |
| Verificação assistida | Quantidades e tabela verdade versionadas na rc.15; automação aprovada e interação de cliente ainda pendente |
| Manutenção | rc.16 reorganiza responsabilidades, remove redundâncias comprovadas e atualiza os guias; resultados próprios e identidade do arquivo constam no registro de validação |

Consulte [validação por build](../validation.md), [guia administrativo](../classroom.md), [guia pedagógico](../classroom-learning.md) e [roteiro atual](../classroom-rc18-acceptance.md). Capturas, relatos e automação conservam seus escopos: um servidor iniciado não aprova renderização, e um relato genérico não discrimina toda a matriz de idiomas/escalas.

## Regras confirmadas

- Autorização docente pela permissão nativa `sisso.logicgates.classroom.teach`; cada operação também exige responsabilidade pela turma. Qualquer jogador no mundo pode abrir `/classroom`.
- Cada UUID possui um único vínculo ativo no servidor: professor responsável ou aluno. A autorização docente por si só não cria vínculo nem permite consultar outra turma.
- Convite individual e solicitação pela lista pública convergem para matrícula e conferência do professor. Aceitar convite não cria vínculo imediatamente.
- Matrícula textual obrigatória de 1–64 caracteres, única entre alunos ativos da turma; remover espaços nas extremidades, preservar zeros/capitalização/conteúdo interno. UUID é identidade; nome é apresentação. Matrícula não autentica o jogador.
- Pendências não ocupam turma nem reservam matrícula. A primeira aprovação ou criação de vínculo docente encerra as demais pendências do UUID.
- Convites persistem após reinício até resposta, cancelamento ou perda de elegibilidade. Nome conhecido permite convite offline.
- Saída/remoção encerra o vínculo, conservando histórico. Encerramento libera todos os participantes e cancela ingressos pendentes na mesma transação, com confirmação e revisão válida.
- Mensagem do dia aparece no painel e, quando preenchida, no chat privado após conexão; troca de mundo não repete o envio.
- Listas são paginadas em 20 registros, sem limite artificial de turma. Nome, matrícula e situação têm prioridade; UUID completo fica na ficha.

O professor encontra alunos à esquerda e detalhes à direita; o aluno consulta a própria turma, seus ingressos e atividades publicadas. Matrículas alheias, pendências de terceiros, rascunhos e atividades arquivadas não são expostos aos alunos. A contagem docente inclui atividades não arquivadas, inclusive rascunhos; a do aluno inclui somente publicadas não arquivadas, sem duplicar uma atividade com edição privada.

## Arquitetura e persistência

Páginas Hytale convertem eventos em operações de aplicação. Domínio e contratos de persistência não dependem da API do jogo; IDs recebidos pela UI nunca autorizam acesso. Textos dos usuários são literais, rótulos/erros usam en-US e pt-BR.

- Um `classroom.db` no diretório de dados do plugin é compartilhado entre mundos; SQLite JDBC **3.53.4.0** é empacotado com recursos nativos/licenças, sem incorporar Hytale.
- Criação, aprovação, correção e encerramento usam transações, revisão otimista, responsabilidade e restrições de unicidade. Leituras de tela usam snapshot transacional, reutilizando o contexto da turma.
- I/O ocorre fora do contexto do mundo. Respostas retornam ao contexto apropriado e revalidam sessão, referência, página e geração de autorização antes de publicar.
- Eventos nativos de permissão reavaliam páginas abertas; alterações sem mudança efetiva evitam atualização redundante. Cada operação mantém sua autorização própria. Não há polling nem reenvio de mensagem do dia por esse canal.
- Notificações de dados são emitidas após commit, filtradas por contexto e agrupadas. Atualizações comuns conservam campos/revisão em edição; revogação descarta o rascunho visual e invalida respostas antigas.
- Migrações identificadas até **v6** são transacionais, com backup consistente e testes de restauração/rollback/rejeição. Esquema incompatível desabilita Classroom preservando o arquivo e os circuitos.
- Conteúdo das entregas fica em arquivos privados versionados com checksum; backup exige banco, arquivos e configuração juntos. Uma revisão só fica disponível após publicação do arquivo e commit dos metadados.

Os [contratos pedagógicos](classroom-learning-contracts.md) definem rascunho/publicação, prazo futuro em Brasília, valor padrão 10, arquivamento, conteúdo fixo, revisões e nota por revisão. A verificação assistida executa explicitamente sobre a entrega salva, usando critérios da publicação correspondente; não altera construção, nota ou feedback.

## Critérios para continuar

1. Confirmar a interação de quantidades/tabela e relatório, preservando a nota manual, com a candidata atual.
2. Executar regressão proporcional às mudanças: abertura, navegação, autorização, campos em edição, prazo/contagem e um fluxo breve de entrega/preview/cópia/nota.
3. Manter separados cenários comprovados por captura, confirmados por relato e pendentes de confirmação individual. Resoluções, escalas e idiomas precisam ser registrados, sem deduzir uma matriz completa de uma imagem recortada.
4. Corrigir falhas encontradas antes do aceite. Preparar a versão estável somente mediante aprovação explícita; tags e publicação são marcos próprios.

O comparativo rc.9/rc.10 preservou o JAR de referência e mediu 25 membros, rajadas 25/100/250 e três rodadas por versão no mesmo ambiente. Esse benchmark e os ensaios da fila/áreas não certificam quantidade de jogadores. Otimizações futuras precisam de evidência de custo, preservando logs completos em disco e resumos na análise.

## Fora do escopo atual

- **Pontuação automática:** definir como resultados da verificação geram nota; circuitos equivalentes podem satisfazer comportamento sem satisfazer restrições de componentes. Falha de contexto ou simulação inconclusiva não equivale a resposta lógica errada.
- **Logging seletivo:** erros operacionais e diagnóstico temporário com volume limitado, conforme a [análise de viabilidade](../reviews/2026-10-26-logging-feasibility.md).
- **Proteção permanente das bancadas/autoria:** seleção identifica quem enviou, sem provar quem construiu. Reservas temporárias de correção já são protegidas; propriedade permanente exige outro contrato.
- **Extensões administrativas:** transferência de professor, múltiplos responsáveis, restauração de turma encerrada e retenção/exclusão definitiva.
- **Materiais especiais e conexões entre alturas:** dependem de provas de reprodução segura e topologia/visual; não relaxar silenciosamente restrições da captura.

A [referência PartyPro](../reference/partypro.md) orientou navegação, linhas e ficha, sem integrar o build. HUD, combate, mapa e funções de guilda não são requisitos. Preservar circuitos, IDs, codecs, mundos e aparência; não acrescentar serviços, tabelas ou placeholders para um incremento sem funcionalidade testável.
