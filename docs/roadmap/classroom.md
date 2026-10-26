# Classroom — plano aprovado e estado dos incrementos

Plano confirmado pelo usuário em **24/10/2026**, a partir da base `v1.3.3` (`d33a2b5`). Trabalho em `codex/classroom-ui`, sem merge em `main`. Os desenhos fornecidos orientam o fluxo; a lista de várias turmas e a capacidade de 30 alunos não são requisitos deste MVP.

## Estado real

**1.4.0-rc.11 mantém o MVP administrativo e implementa os incrementos pedagógicos como candidata.** Atividades versionadas, entregas fixas de áreas, revisões, preview/cópias temporárias e avaliação manual estão implementados; seleção e apresentação no cliente aguardam aceite. Trabalho em `codex/classroom-ui`, sem merge em `main`, tag ou publicação estável.

O SQLite passa a v5 com backup, preservando os registros administrativos e as leituras agregadas da rc.10. Conteúdo privado é separado em arquivos imutáveis; o [contrato atualizado](classroom-learning-contracts.md), a [documentação de uso](../classroom-learning.md) e o [roteiro rc.11](../classroom-rc11-acceptance.md) descrevem limites, recuperação e o que falta comprovar.

## Polimento e evidências

| Tarefa | Estado e conclusão |
| --- | --- |
| CL-01 Evidências | Fluxos centrais rc.4 confirmados em pt-BR por [capturas e relatos](../evidence/classroom/2026-10-24-rc4-client.md). As falhas de transporte rc.3 e sintaxe rc.5 foram corrigidas; a [visão geral rc.6](../evidence/classroom/2026-10-24-rc6-client.md) foi capturada. Não generalizar essa evidência para todas as telas. |
| CL-02 Legibilidade | Ações fixas, paginação legível, seleção e histórico identificados; rc.7 corrigiu orientação vazia e prioridade de mensagens. Textos longos, idiomas e demais contextos ainda têm pendências de cliente. |
| CL-03 Autorização automática | Implementada por eventos nativos; multiplayer e revogação automática relatados após rc.7. rc.8 indexa assinaturas e ignora eventos sem mudança efetiva. Restauração e edição durante atualização ainda exigem confirmação individual. |
| CL-04 Verificar e entregar | Regressões existentes preservadas. A rc.10 acrescenta contagem JDBC e snapshot concorrente, `fullCheck` final e três rodadas por versão contra o JAR rc.9 preservado. Resultados e identidade exata em [validação](../validation.md); CI Windows/Linux registrada separadamente. |
| CL-05 Aceite | Parcial. O [relato rc.9](../evidence/classroom/2026-10-25-rc9-client-report.md) confirma encerramento, sem discriminar todos os cenários. Cinco cenários da rc.10 foram [confirmados por relato e duas capturas](../evidence/classroom/2026-10-26-rc10-client.md); as [pendências complementares](../classroom-rc10-acceptance.md) permanecem. |

Os eventos de permissão não substituem a autorização de cada operação nem reenviam a mensagem do dia. A rc.9 acrescentou encerramento e migração v4. A rc.10 não altera esquema. Na rodada rc.10, o usuário confirmou resposta instantânea na troca de abas, sem cronometragem por máquina. Esse relato não atribui isoladamente o resultado ao SQL; o benchmark de banco não certifica capacidade de jogadores. Versão estável, tags e publicação exigem aceite posterior.

## Regras confirmadas

- Autorização docente pela permissão nativa `sisso.logicgates.classroom.teach`; cada operação também exige responsabilidade pela turma. `/classroom` será acessível a qualquer jogador.
- Cada UUID tem um único vínculo ativo no servidor: professor responsável ou aluno. A autorização docente por si só não cria vínculo nem permite consultar outra turma.
- Duas entradas: convite individual pelo professor e solicitação pela lista pública de turmas.
- Nos dois caminhos, o aluno informa a matrícula e o professor confirma ou corrige antes de efetivar o ingresso. Aceitar convite não cria vínculo automaticamente.
- Matrícula textual obrigatória, única entre alunos ativos da turma; preservar zeros à esquerda. UUID é identidade técnica; nome é apresentação. Matrícula não é autenticação.
- SQLite local compartilhado por todos os mundos, separado dos componentes de jogador e dos dados de circuito.

O professor abre diretamente sua turma, com lista de alunos à esquerda e detalhes à direita. Contadores e botões de atividades/entregas entram junto das respectivas funções. O aluno recebe primeiro apenas ingresso e consulta básica; o painel pedagógico completo depende de referências próprias.

## Sequência e critérios

| Incremento | Entrega | Critério |
| --- | --- | --- |
| 0 | Página mínima com contador e fechamento | Abrir, clicar, atualizar, fechar, dispensar e reabrir no cliente 0.7.4, nos dois idiomas |
| 1 | Professor cria/edita nome e descrição da turma; painel vazio e SQLite | Reinício preserva dados; segunda turma e acesso indevido são recusados |
| 2 | Lista pública, solicitações, convites, conferência de matrícula, membros e ficha básica | Dois caminhos convergem para o mesmo vínculo; concorrência e reconexão cobertas |
| 3 | Mensagem do dia, correção de matrícula, remoção/saída e refinamento visual | Permissões, navegação e persistência verificadas com professor e aluno |
| 4 | Publicação e detalhes de atividades versionadas | Implementado na rc.11; testes de domínio e aceite visual separados |
| 5 | Entrega com revisão e feedback manual; automática em etapa posterior | Áreas fixas, revisões e avaliação manual implementadas na rc.11; fluxo principal confirmado na rc.12, complementos pendentes |

A página de prova evolui para a entrada real após validação do incremento 0. Cada incremento inclui código, testes relevantes, documentação e commits por assunto. Usar pré-versões de `1.4.0`; tags são marcos explícitos de aprovação. Preservar circuitos, IDs, codecs, mundos e aparência. Novas portas e conexões verticais estão fora deste trabalho.

## Interfaces, domínio e persistência

As páginas Hytale convertem eventos em operações de aplicação; domínio e interfaces de repositório não dependem da API do jogo. Consultas privadas e alterações verificam identidade, autorização e vínculo no servidor. Receber um ID pela UI não concede acesso. Textos de usuário são literais; textos da interface e erros terão en-US e pt-BR.

O fluxo de ingresso é `solicitação com matrícula → conferência → aprovação → vínculo`. Um convite acrescenta `convite → aceite e matrícula` antes da conferência. IDs de turma, jogador, vínculo e processo de ingresso são UUIDs. Convites usam jogadores online ou já conhecidos pelo Classroom, com último nome conhecido para apresentação.

Persistência iniciada no incremento 1 e ampliada na rc.3:

- `classroom.db` em `getDataDirectory()`, um banco por servidor. Driver Xerial SQLite JDBC **3.53.4.0**, fixado e empacotado com recursos nativos/licenças; teste do JAR isolado na matriz Java 25/Windows/Linux, com resultado local distinguido da CI remota.
- Criação de turma/vínculo em transação única e edição por revisão; índices garantem vínculo ativo único entre papéis. Aprovação, matrícula e encerramento das demais pendências também são transacionais.
- Acesso ao banco em executor dedicado. Respostas de UI retornam ao contexto do mundo e revalidam sessão/referência antes de publicar.
- Esquemas v1–v5 com identificação própria, migração transacional, backup prévio consistente e testes de restauração/rollback/rejeição. Procedimento operacional no guia Classroom. Falhas tornam Classroom indisponível, preservando arquivos e circuitos.

## Padrões aceitos com o plano

- Lista pública: nome, descrição e professor. Matrículas, fichas e solicitações ficam restritas aos envolvidos.
- Pendências não ocupam uma turma; a primeira aprovação efetiva encerra os demais processos de ingresso do jogador.
- Convites sobrevivem a reinício até aceite, recusa, cancelamento ou perda de elegibilidade.
- Matrícula: 1–64 caracteres após remoção de espaços nas extremidades; preservar capitalização e conteúdo interno. A unicidade considera vínculos ativos.
- Saída/remoção encerra vínculo e preserva seu registro. A rc.9 permite ao professor encerrar a turma com confirmação, liberar participantes e cancelar ingressos pendentes, preservando histórico. Exclusão definitiva, restauração de turma, transferência de professor e múltiplos responsáveis ficam para depois.
- Mensagem do dia no painel e, quando preenchida, no chat após a conexão do membro.

## Contratos conceituais posteriores

| Contrato | Conteúdo mínimo |
| --- | --- |
| Atividade | ID, turma, versão publicada, título, enunciado, prazo e valor |
| Entrega | ID, atividade/versão, aluno, revisão, instante e referência a conteúdo versionado |
| Avaliação | Revisão avaliada, professor, feedback e resultado |

O [contrato de atividades](classroom-learning-contracts.md) define rascunho/publicação, prazo obrigatório futuro em Brasília, valor positivo padrão 10 com até duas casas, nota ainda não atribuída como —/valor, arquivamento/restauração e acesso por vínculo. Implementado na rc.11 e confirmado no fluxo principal rc.12; os testes de cliente pendentes não impedem esta especificação, mas mantêm o aceite do polimento e a versão estável em aberto.

O [contrato de áreas](classroom-learning-contracts.md) definiu seleção de dois cantos e snapshot fixo, revisões e cópias temporárias para correção. Implementado na rc.11 e confirmado em multiplayer na rc.12, sem comprovar autoria da construção. Logging e proteção permanente das bancadas continuam posteriores.

Avaliação automática virá depois do feedback manual. Deve usar snapshots/casos esperados sem acionar o circuito vivo do aluno; indisponibilidade de seção é falha de contexto, não resposta lógica incorreta. Proteção de bancadas, instâncias, arquivamento e retenção definitiva exigem seus próprios contratos futuros.

## Validação por camada

- Automatizado: autorização/responsabilidade, vínculo único entre papéis, matrícula vazia/duplicada e `00123`, processos de ingresso, persistência/reinício, migração, rollback e concorrência.
- Integração Hytale: comando, codecs, traduções, recursos no JAR e inicialização dos serviços. `check`/`build` por incremento e `fullCheck` na revisão final.
- Cliente: interação, renderização, listas, rolagem, formulários, erros, navegação, dois idiomas, professor/aluno e reconexão. Registrar build ID, cenário e resultado; iniciar o servidor nunca aprova a interface.

## Referência e continuação aprovada

A [análise PartyPro](../reference/partypro.md) orienta listas, ficha e navegação. O MVP é administrativo: incremento 2A (solicitação completa), 2B (convites/membros) e 3 (mensagem/administração), com builds e evidências separados. A sequência pedagógica foi implementada na rc.11; avaliação automática continua posterior.

## Encerramento administrativo — rc.9

Solicitado após a rc.8, ainda sem reteste daquela candidata. Administração permite encerrar a própria turma com confirmação/cancelamento; a transação libera todos os vínculos, cancela ingressos pendentes e conserva histórico. Migração v4 com backup substitui a unicidade permanente do professor pela de turma aberta. Regras de autorização e transporte incremental permanecem. O [roteiro rc.9](../classroom-rc9-acceptance.md) mantém as pendências anteriores e acrescenta encerramento/recriação; atividades continuam sem implementação.

## Otimização de leituras — rc.10 e sequência

A meta medida na tela docente de membros é passar de cinco conexões/sete SELECTs para uma conexão e no máximo três SELECTs. O comparativo usa o JAR rc.9 preservado e a candidata no mesmo ambiente, com 25 membros, rajadas 25/100/250 e três rodadas por versão. Os logs completos ficam em disco; evidências resumidas registram builds e checksums. Testes cobrem papéis, busca/paginação, autorização, fechamento/rollback e consistência com aprovação, saída ou encerramento concorrente. Nenhuma estimativa de capacidade real é deduzida desses números.

A rc.11 implementa a sequência aprovada **prova técnica → atividades versionadas → entregas/revisões → avaliação manual**. A prova nativa verifica reprodução no servidor; o aceite de seleção, preview e correção continua no cliente. Avaliação automática, logging seletivo e proteção permanente das bancadas permanecem no backlog, assim como a limpeza dos três wrappers sem consumidores. As pendências administrativas anteriores continuam registradas; versão estável só após aceite explícito.

A abertura de Atividades na rc.11 foi bloqueada no cliente pelo comando `#ScoreField.Disabled`. A rc.12 remove os comandos incompatíveis dos campos e acrescenta regressões de atualizações de tela, gravação e erros. O [roteiro de reteste](../classroom-rc12-acceptance.md) começa pela abertura e pelo salvamento; o [relato e as capturas rc.12](../evidence/classroom/2026-10-26-rc12-client.md) confirmam o fluxo principal de áreas e avaliação, com complementos pendentes.


## Polimento — rc.13

Margem externa de 16 unidades e ações com quebra de linha; criação ampla sem lateral; fichas compactas; prazo em cinco dropdowns nativos de Brasília; contagem privada por papel no snapshot existente; ações próprias de encerramento. SQLite v5, entregas e circuitos preservados. O [roteiro rc.13](../classroom-rc13-acceptance.md) mede ocupação e mantém a matriz de cliente pendente. Aprovação estável permanece explícita.


## Proporções e consulta explícita — rc.14

As [capturas rc.13](../evidence/classroom/2026-10-26-rc13-client.md) demonstram o desequilíbrio entre o painel ampliado e os componentes. A rc.14 usa colunas flexíveis, tipografia/campos/preview maiores e seleção em modo de consulta. Editar atividade é uma ação docente explícita; criação continua ampla e a privacidade das publicações/rascunhos é preservada. O [aceite rc.14](../classroom-rc14-acceptance.md) permanece separado dessas capturas.
