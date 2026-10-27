# Classroom — aceite da 1.4.0-rc.16

Candidata de polimento técnico e organização da documentação, Hytale 0.7.4, SQLite **v6**. Build **1.4.0-rc.16+922db3324774**, revisão de produção `dd67acbe89ff`, compilação limpa. JAR SHA-256 `0cc156676d12971a749bd68241ecb02bb763fdf509309cbb91114ed1ac83ef08`. A [evidência automatizada](evidence/classroom/2026-10-27-rc16-server.json) registra 910 testes Java, 38 Python e 90 verificações nativas aprovados (63 criação + 27 após reinício real).

O layout, os comandos, os critérios, as entregas, os circuitos e os dados mantêm os contratos anteriores. Não há migração nova a partir da rc.15; versões anteriores à rc.15 continuam usando a migração v5→v6 com backup. Automação não comprova renderização; a regressão de cliente abaixo está pendente.

## Instalação e regressão curta

Com mundo/servidor parado, mantenha somente `LogicGates-1.4.0-rc.16.jar` em `mods`. O ZIP é pacote de entrega e fica fora dessa pasta. Preserve banco, pasta `submissions`, configurações e mundos. Artefatos rc.15 são conservados.

1. Professor e aluno: abrir Classroom, alternar abas, buscar, conferir contagens e abrir dados já persistidos. Conferir ingresso/saída brevemente se houver contas disponíveis.
2. Professor: selecionar atividade em consulta → Editar → alterar campos → salvar rascunho/publicar. Atualização durante edição deve preservar campos; revogação deve ocultar painel e limpar rascunho local, inclusive prazo/critérios e campos de avaliação. Restaurar autorização não reabre o editor automaticamente.
3. Aluno: consultar somente a publicação vigente e entregar uma área. Professor: selecionar a revisão → Verificar circuito → conferir relatório sem alterar nota/feedback → salvar avaliação manual. Preview, colagem, edição e descarte da cópia temporária permanecem.
4. Reabrir após sair/entrar e verificar matrícula, publicação, entrega, nota e intenção das alavancas. Fazer uma regressão breve do circuito no mundo (ligar/desligar uma alavanca e conferir portas/lâmpada).

O [roteiro rc.15](classroom-rc15-acceptance.md) continua sendo a referência completa para critérios e tabela verdade, cujo fluxo ainda depende de aceite no cliente. O [relato rc.14](evidence/classroom/2026-10-26-rc14-client.md) aprova o polimento exercitado, sem discriminar matriz de resoluções/idiomas. Esta limpeza não declara automaticamente essas combinações como executadas.

## Verificação automatizada

Os 167 testes direcionados de Classroom, orientação/compatibilidade passaram; depois, uma única rodada final de `fullCheck` passou em 3 min 23 s. Logs completos: `build/tmp/rc16-targeted.log` e `build/tmp/rc16-fullCheck.log`. Conferidos recursos fechados em falha de transação, rollback, snapshots concorrentes e uma conexão/até três SELECTs na tela docente de membros. A prova nativa preservou paridade de rotações entre mundo e snapshot, recusa de inclinação, descarte e reinício real. O verificador documental aprovou links locais, versões, 296 chaves por idioma e quatro documentos UI. Esses testes não certificam capacidade de jogadores.

CI Windows/Linux aprovada no commit `dd67acbe89ff639765e8e1be496e26eca79606f7`: [execução](https://github.com/LuisPassosRamos/LogicGatesMod/actions/runs/40444170877). Evidências/documentação posteriores são incorporadas ao ZIP preservando os bytes do JAR testado; não exigem repetir o servidor.

Nota permanece manual; logging seletivo e extensões do produto continuam no backlog. Tags, merge e publicação estável não fazem parte desta candidata.
