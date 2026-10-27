# Classroom — aceite da 1.4.0-rc.16

Candidata de polimento técnico e organização da documentação, Hytale 0.7.4, SQLite **v6**. Identidade e resultados serão registrados após a revisão final. O layout, os comandos, os critérios, as entregas, os circuitos e os dados mantêm os contratos anteriores. Não há migração nova a partir da rc.15; versões anteriores à rc.15 continuam usando a migração v5→v6 com backup.

## Instalação e regressão curta

Com mundo/servidor parado, mantenha somente `LogicGates-1.4.0-rc.16.jar` em `mods`. O ZIP é pacote de entrega e fica fora dessa pasta. Preserve banco, pasta `submissions`, configurações e mundos. Artefatos rc.15 são conservados.

1. Professor e aluno: abrir Classroom, alternar abas, buscar, conferir contagens e abrir dados já persistidos. Conferir ingresso/saída brevemente se houver contas disponíveis.
2. Professor: selecionar atividade em consulta → Editar → alterar campos → salvar rascunho/publicar. Atualização durante edição deve preservar campos; revogação deve ocultar painel e limpar rascunho local, inclusive prazo/critérios e campos de avaliação. Restaurar autorização não reabre o editor automaticamente.
3. Aluno: consultar somente a publicação vigente e entregar uma área. Professor: selecionar a revisão → Verificar circuito → conferir relatório sem alterar nota/feedback → salvar avaliação manual. Preview, colagem, edição e descarte da cópia temporária permanecem.
4. Reabrir após sair/entrar e verificar matrícula, publicação, entrega, nota e intenção das alavancas. Fazer uma regressão breve do circuito no mundo (ligar/desligar uma alavanca e conferir portas/lâmpada).

O [roteiro rc.15](classroom-rc15-acceptance.md) continua sendo a referência completa para critérios e tabela verdade, cujo fluxo ainda depende de aceite no cliente. O [relato rc.14](evidence/classroom/2026-10-26-rc14-client.md) aprova o polimento exercitado, sem discriminar matriz de resoluções/idiomas. Esta limpeza não declara automaticamente essas combinações como executadas.

## Verificação automatizada

Executar testes direcionados de Classroom, orientação/compatibilidade e depois uma única rodada final de `fullCheck`; conferir CI Windows/Linux. Conferir especialmente recursos fechados em falha de transação, rollback, snapshots concorrentes e uma conexão/até três SELECTs na tela docente de membros. A prova nativa deve preservar paridade de rotações entre mundo e snapshot, descarte e reinício real. O verificador documental confere links locais, versões e traduções; automação não comprova renderização nem capacidade de jogadores.

Nota permanece manual; logging seletivo e extensões do produto continuam no backlog. Tags, merge e publicação estável não fazem parte desta candidata.
