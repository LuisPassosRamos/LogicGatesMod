# Classroom rc.10 — regressão curta das leituras

**Cinco cenários breves confirmados pelo cliente em 26/10/2026; aceite complementar pendente.** O [relato e as duas capturas](evidence/classroom/2026-10-26-rc10-client.md) confirmam abas instantâneas, atualização de professor/aluno, contagem após saída, busca e preservação/salvamento da descrição durante atualização. A rc.10 agrega consultas SQLite sem mudar layout, comandos, regras de vínculo ou esquema v4. Os resultados automatizados, build ID e SHA-256 ficam no [registro de validação](validation.md#leituras-agregadas--rc10). A linha de base rc.9 permanece preservada. Instalar somente `LogicGates-1.4.0-rc.10.jar` em `mods`, substituindo o anterior com o servidor parado; guardar o ZIP fora de `mods`.

Candidata exata: **`1.4.0-rc.10+86bb2076e27f`**, SHA-256 `471c5b66fcde9146b4b18006a9ec2f7f304902f9b9033c3c3b8b2987ca0ad657`. O JAR embutido no ZIP conserva essa identidade.

O encerramento rc.9 foi confirmado por relato; os fluxos rc.4 e as capturas rc.6 são históricos. Nenhuma medição de consultas aprova renderização ou elimina as [pendências anteriores](classroom-rc9-acceptance.md#pendências-de-interação-anteriores). Não é necessário repetir toda a campanha para conferir esta alteração.

## Quando houver disponibilidade

Usar professor e aluno. Registrar build ID, idioma, resolução/escala e resultado. Começar em pt-BR; os demais idiomas/resoluções continuam no roteiro anterior.

| Caso | Resultado esperado |
| --- | --- |
| Abrir e alternar Turma/Ingressos/Convidar/Administração | Mesmo conteúdo e ações, sem erro; relatar duração da primeira troca e se o mundo engasga |
| Busca e paginação | Filtro corresponde ao nome/professor; páginas mantêm registros e seleção corretos. Para mais de 20, reutilizar a massa isolada existente |
| Acesso por papel | Professor vê somente a própria turma e seus processos; aluno vê a própria ficha, sem matrículas de colegas; sem vínculo vê descoberta e próprios ingressos |
| Alteração com ambas as páginas abertas | Solicitar/aprovar ingresso ou sair numa turma descartável: lista, contagem e ficha se atualizam sem recarga e concordam entre si |
| Permissão revogada/restaurada | Reação automática preservada e nenhuma operação docente sem autorização; conferir restauração sem recarregar quando possível |
| Persistência e campos | `00123` permanece íntegra após reabrir; atualização de dados preserva edição local, revogação descarta rascunho docente |

Preparar listas longas com o [gerador existente](classroom-rc9-acceptance.md#massa-sintética-isolada), exclusivamente em outro save/servidor com diretório de dados independente. Outro mundo no mesmo servidor não isola o banco. Sem massa/contas suficientes, marcar os casos não executados.

## Pendências preservadas

Conteúdo longo e ações fora da rolagem; en-US e combinações de resolução/escala previstas; recusa/cancelamento; mensagem vazia; revogação/restauração automática e descarte de rascunho nesta candidata; reabertura/reinício e histórico/paginação extensa; casos de encerramento não discriminados no relato anterior. Preservação da descrição durante atualização está confirmada nesta rodada; outros campos, revisão concorrente e demais contextos não foram discriminados. Busca positiva/negativa foi confirmada; não confundir esse resultado com paginação com mais de 20 registros. Os casos da tabela acima servem de referência e não precisam ser todos repetidos sem nova alteração.

Sem tag, publicação estável ou merge em `main` automático. Atividades permanecem um incremento separado com contrato aprovado.
