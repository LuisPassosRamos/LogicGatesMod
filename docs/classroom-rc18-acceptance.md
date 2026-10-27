# Classroom — aceite da 1.4.0-rc.18

Candidata de correção para Hytale **0.7.4**, SQLite **v6**. Corrige exclusivamente o alinhamento inválido que impedia carregar a UI da rc.17 e amplia o verificador correspondente. Mantém progresso, formulários, tabela visual, entregas e notas existentes. Artefatos anteriores são preservados. Não há migração nova, tag, merge ou publicação estável nesta entrega.

## Diagnóstico e correção

O [log enviado pelo usuário](evidence/classroom/2026-10-27-rc17-client.md) identifica `Pages/LogicGates/ClassroomRow.ui (19:141)` e falha ao resolver HorizontalAlignment como LabelAlignment no build **1.4.0-rc.17+1ee2a26cc4dc**. Substituído `Right` por `End`, conforme os documentos nativos 0.7.4. O verificador agora rejeita alinhamentos literais fora de Start/Center/End nas fontes e no JAR; ele não substitui o parser completo do cliente. O JAR rc.17 original foi recusado por essa nova regressão.

## Reteste no cliente — pendente

1. Com mundo/servidor fechado, substituir o JAR rc.17 pelo **LogicGates-1.4.0-rc.18.jar**, conservando somente uma versão do mod em mods. Não apagar mundo, banco, submissions ou configurações. ZIP fica fora de mods.
2. Entrar no mesmo mundo no computador do trabalho; confirmar que não aparece Failed to load CustomUI documents. Informar o build exibido. Em caso de erro, enviar o novo trecho de CustomUI do log.
3. Abrir Classroom como professor: ver resumo e contador junto ao aluno, selecionar a ficha, abrir atividade, consultar e editar. Aluno: conferir catálogo, enunciado e entrega. Confirmar que tabelas/dropdowns e notas continuam funcionando.
4. Continuar o [roteiro funcional rc.17](classroom-rc17-acceptance.md#roteiro-no-cliente--pendente), incluindo resoluções/escalas e tabela visual. O erro de entrada corrigido não constitui aceite desses fluxos.

## Verificação automatizada

Os testes direcionados de ferramentas passaram: seis casos de UI e treze de empacotamento. A nova verificação rejeitou o ClassroomRow.ui do JAR rc.17 preservado e aprovou os seis documentos de origem corrigidos. A rodada final de fullCheck, identidade do JAR e CI serão registradas após execução. Renderização e nova entrada no cliente continuam pendentes.
