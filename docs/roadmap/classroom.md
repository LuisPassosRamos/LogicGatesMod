# Classroom — plano aprovado e estado dos incrementos

Plano confirmado pelo usuário em **24/10/2026**, a partir da base `v1.3.3` (`d33a2b5`). Trabalho em `codex/classroom-ui`, sem merge em `main`. Os desenhos fornecidos orientam o fluxo; a lista de várias turmas e a capacidade de 30 alunos não são requisitos deste MVP.

## Estado real

**1.4.0-rc.4 contém o MVP administrativo (incrementos 0–3) para validação no cliente:** prova, turma, ingresso por solicitação/convite, matrícula, membros e mensagem. O usuário confirmou os fluxos centrais de rc.1/rc.2 em pt-BR. A abertura de rc.3 falhou por seletor de âncora incompatível; rc.4 corrige esse transporte. As telas e a experiência com duas contas aguardam o [roteiro de rc.4](../classroom.md). Implementação não equivale a aprovação manual.

O núcleo é independente da API Hytale, com esquema v3 e migração preservando a turma da v1. Atividades, entregas e avaliação continuam conceituais, sem tabelas/telas antecipadas. Veja [contratos futuros](classroom-learning-contracts.md).

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
| 4 | Publicação e detalhes de atividades versionadas | Definir contrato de publicação antes de implementar |
| 5 | Entrega com revisão e feedback manual; automática em etapa posterior | Escolher formato da entrega e fluxo de correção antes de implementar |

A página de prova evolui para a entrada real após validação do incremento 0. Cada incremento inclui código, testes relevantes, documentação e commits por assunto. Usar pré-versões de `1.4.0`; tags são marcos explícitos de aprovação. Preservar circuitos, IDs, codecs, mundos e aparência. Novas portas e conexões verticais estão fora deste trabalho.

## Interfaces, domínio e persistência

As páginas Hytale convertem eventos em operações de aplicação; domínio e interfaces de repositório não dependem da API do jogo. Consultas privadas e alterações verificam identidade, autorização e vínculo no servidor. Receber um ID pela UI não concede acesso. Textos de usuário são literais; textos da interface e erros terão en-US e pt-BR.

O fluxo de ingresso é `solicitação com matrícula → conferência → aprovação → vínculo`. Um convite acrescenta `convite → aceite e matrícula` antes da conferência. IDs de turma, jogador, vínculo e processo de ingresso são UUIDs. Convites usam jogadores online ou já conhecidos pelo Classroom, com último nome conhecido para apresentação.

Persistência iniciada no incremento 1 e ampliada na rc.3:

- `classroom.db` em `getDataDirectory()`, um banco por servidor. Driver Xerial SQLite JDBC **3.53.4.0**, fixado e empacotado com recursos nativos/licenças; teste do JAR isolado na matriz Java 25/Windows/Linux, com resultado local distinguido da CI remota.
- Criação de turma/vínculo em transação única e edição por revisão; índices garantem vínculo ativo único entre papéis. Aprovação, matrícula e encerramento das demais pendências também são transacionais.
- Acesso ao banco em executor dedicado. Respostas de UI retornam ao contexto do mundo e revalidam sessão/referência antes de publicar.
- Esquemas v1–v3 com identificação própria, migração transacional, backup prévio consistente e testes de restauração/rollback/rejeição. Procedimento operacional no guia Classroom. Falhas tornam Classroom indisponível, preservando arquivos e circuitos.

## Padrões aceitos com o plano

- Lista pública: nome, descrição e professor. Matrículas, fichas e solicitações ficam restritas aos envolvidos.
- Pendências não ocupam uma turma; a primeira aprovação efetiva encerra os demais processos de ingresso do jogador.
- Convites sobrevivem a reinício até aceite, recusa, cancelamento ou perda de elegibilidade.
- Matrícula: 1–64 caracteres após remoção de espaços nas extremidades; preservar capitalização e conteúdo interno. A unicidade considera vínculos ativos.
- Saída/remoção encerra vínculo e preserva seu registro. Exclusão de turma, transferência de professor e múltiplos responsáveis ficam para depois.
- Mensagem do dia no painel e, quando preenchida, no chat após a conexão do membro.

## Contratos conceituais posteriores

| Contrato | Conteúdo mínimo |
| --- | --- |
| Atividade | ID, turma, versão publicada, título e enunciado |
| Entrega | ID, atividade/versão, aluno, revisão, instante e referência a conteúdo versionado |
| Avaliação | Revisão avaliada, professor, feedback e resultado |

Esses contratos não definem ainda coordenadas, seleção de área ou snapshot/prefab. A escolha precisa garantir uma revisão reproduzível e a experiência do professor. A implementação futura usará um adaptador para conteúdo versionado, sem acoplar Classroom ao circuito vivo. Não criar tabelas, botões ou telas de entrega antes dessa decisão.

Avaliação automática virá depois do feedback manual. Deve usar snapshots/casos esperados sem acionar o circuito vivo do aluno; indisponibilidade de seção é falha de contexto, não resposta lógica incorreta. Proteção de bancadas, instâncias, arquivamento e retenção definitiva exigem seus próprios contratos futuros.

## Validação por camada

- Automatizado: autorização/responsabilidade, vínculo único entre papéis, matrícula vazia/duplicada e `00123`, processos de ingresso, persistência/reinício, migração, rollback e concorrência.
- Integração Hytale: comando, codecs, traduções, recursos no JAR e inicialização dos serviços. `check`/`build` por incremento e `fullCheck` na revisão final.
- Cliente: interação, renderização, listas, rolagem, formulários, erros, navegação, dois idiomas, professor/aluno e reconexão. Registrar build ID, cenário e resultado; iniciar o servidor nunca aprova a interface.

## Referência e continuação aprovada

A [análise PartyPro](../reference/partypro.md) orienta listas, ficha e navegação. O MVP é administrativo: incremento 2A (solicitação completa), 2B (convites/membros) e 3 (mensagem/administração), com builds e evidências separados. O painel pedagógico completo e entregas ficam para depois.
