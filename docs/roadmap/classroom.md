# Classroom — proposta, ainda não implementada

## Visão do usuário

Classroom será a interface de turmas dentro do jogo. A comparação é uma guilda: o professor cria/administra a turma; alunos são membros. O professor terá acesso às atividades que publica, poderá definir a mensagem do dia exibida ao aluno ao entrar e abrir a ficha de cada aluno, com matrícula e entregas. O ingresso deve admitir convite ou entrada pela interface; regras exatas ainda precisam ser definidas.

O projeto já oferece simulação independente da API e testes suficientes para servir como base técnica. A UI e o domínio educacional ainda precisam ser construídos. A existência das classes Custom UI torna a proposta viável, mas não equivale a um painel pronto ou a um fluxo comprovado no cliente.

## Fluxos propostos

- Professor: criar turma → configurar nome/mensagem → convidar ou aprovar alunos → publicar atividades → abrir aluno/matrícula → consultar entregas e feedback.
- Aluno: ver turmas/convites → solicitar entrada ou aceitar convite → ler mensagem/atividades → montar circuito → entregar → consultar retorno.
- Administração: autorizar quem pode criar turmas e manter acesso/backup dos dados.

Permissão técnica de professor e vínculo com a turma são verificações do servidor. Esconder um botão não substitui autorização. Um aluno não deve promover a si mesmo alterando dados enviados pela UI.

## Domínio e persistência sugeridos

| Entidade | Dados iniciais |
| --- | --- |
| Turma | ID, nome, professor responsável, mensagem do dia, configuração de ingresso |
| Matrícula/vínculo | Turma, UUID do jogador, matrícula textual, papel e estado do vínculo |
| Convite/solicitação | Turma, destinatário/solicitante, estado e validade |
| Atividade | ID, turma, versão, título, enunciado e critérios |
| Entrega | Atividade, aluno, revisão, data, referência/snapshot e estado |
| Avaliação | Entrega/revisão, feedback, casos esperados/observados e autor |

UUID é identidade técnica; nome é apresentação. Matrícula deve ser texto, preservando zeros à esquerda. Definir unicidade por instituição/turma ou globalmente, além de quem pode atribuí-la. Não usar a matrícula como segredo de autenticação.

Turmas/atividades pertencem a um repositório compartilhado do servidor, não apenas ao componente do jogador. Recomenda-se separar serviço de domínio, interface de repositório, persistência e páginas Hytale. SQLite é uma opção a avaliar para um servidor; JSON pode servir a um protótipo pequeno com gravação segura. Nenhuma escolha/dependência foi implementada. O esquema deve ser versionado, com backups e migrações antes de receber dados reais.

A mensagem do dia pode ser uma página/aviso na entrada, com HUD apenas para exibição breve se necessário. Definir se aparece a cada conexão, por turma ativa ou somente quando a mensagem muda. Evitar armazenar dados pessoais além do necessário ao vínculo pedagógico.

## Sequência de implementação sugerida

1. Página mínima: abrir pelo jogo, receber evento, atualizar texto e fechar; validar em 0.7.4.
2. Turmas/vínculos persistidos: criar, entrar por convite/aprovação, listar membros e mensagem do dia; testes de autorização e reinício.
3. Atividades e ficha de aluno: matrícula, lista de atividades e estados de entrega.
4. Primeira entrega simples com revisão e feedback manual. Escolher coordenadas, área congelada ou snapshot/prefab antes de programar o contrato.
5. Avaliação automática de um exercício pequeno usando o avaliador puro, sem mudar o circuito vivo do aluno.

O incremento inicial não precisa de NPC tutor, mundo gerado ou instâncias. Bancadas delimitadas no mesmo mundo podem servir ao piloto; isolamento, carregamento assíncrono e retorno do jogador precisam de projeto próprio se forem introduzidas instâncias.

## Decisões ainda abertas

Quem pode ser professor; número de turmas por professor/aluno; convite versus ingresso por código/aprovação; escopo da matrícula; tratamento de alunos offline; formato/versionamento das entregas; avaliação manual/automática; proteção das bancadas; arquivamento de turma e retenção dos dados.

Essas decisões não bloqueiam a prova mínima de Custom UI. Elas devem ser resolvidas antes dos respectivos fluxos, sem criar implementações fictícias no núcleo de circuitos.
