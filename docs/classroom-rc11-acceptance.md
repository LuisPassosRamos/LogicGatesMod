# Classroom — aceite da 1.4.0-rc.11

**Cliente: pendente.** A prova no servidor isolado exercita captura, serialização, pacote de preview, colagem, simulação e descarte. Ela não comprova o gesto de seleção sem OP nem a renderização do preview no cliente. Comece por uma turma/atividade e uma área pequena; não é necessário repetir toda a história das candidatas anteriores.

Com o mundo parado, instale somente o JAR rc.11 e guarde o ZIP fora de `mods`. A migração cria um backup v4. Para preservar entregas novas, faça backup do banco **junto da pasta `submissions`**, ambos com servidor parado. Consulte [uso, limites e recuperação](classroom-learning.md).

## Primeiro: prova curta com professor e aluno

| Cenário | Procedimento e resultado esperado | Estado |
| --- | --- | --- |
| Atividade | Professor abre Atividades, cria/salva rascunho; aluno não o vê. Preenche título/enunciado, prazo futuro de Brasília e valor 10, publica. Aluno vê os mesmos dados. | Pendente |
| Seleção sem OP | Aluno usa Entregar; equipa a ferramenta; esquerdo/direito escolhem dois cantos. Ao reabrir `/classroom`, dimensões e atividade correspondem à seleção. Comandos gerais de edição continuam indisponíveis. | Pendente |
| Entrega pequena | Selecione chão + alavanca ligada → cabos → lâmpada e uma porta girada. Confirme a entrega. Professor encontra atividade → aluno → revisão 1, preview e nota `—/10`. | Pendente |
| Preview | Girar, inclinar, zoom e mover; trocar de aluno/revisão não deixa o conteúdo anterior. Sem reenviar o prefab a cada atualização simples de metadados. | Pendente |
| Cópia e edição | Professor cola em espaço vazio e testa alavanca/porta/lâmpada; edita um cabo e acrescenta um bloco dentro da reserva. Aluno tenta quebrar/colocar/acionar nessa cópia e é bloqueado. | Pendente |
| Descarte | Professor descarta; todo o volume fica livre, inclusive edições. Original do aluno e revisão salva continuam intactos. | Pendente |
| Avaliação e reenvio | Professor lança 7/10 com feedback. Aluno envia revisão 2: nota ainda `—/10`; revisão 1 continua 7/10. Avaliar a revisão 1 novamente não atribui nota à 2. | Pendente |

## Depois: casos complementares

- Publicar sem campos/prazo futuro/valor positivo, com mais de duas casas; tentar nota negativa/acima do valor. Erro sem publicação/nota parcial. Zero é uma nota válida.
- Editar sem republicar mantém publicação anterior para aluno. Republicar com outro valor mantém enunciado e valor da entrega antiga. Arquivar oculta; restaurar conserva a versão; vencer mantém consulta e aceita envio/reenvio atrasado.
- Cancelar seleção, desconectar e trocar de mundo. Ferramenta antiga ou passada a outro jogador não envia. Testar expiração, limite 32 × 16 × 32, área vazia, tipo especial recusado e região indisponível.
- Tentar colagem sobre construção/fluido, em área protegida e com outra cópia ativa. Construção original intacta, sem nova reserva útil após falha. Repetir com o plugin de proteção usado no servidor, se houver.
- Deixar cópia ativa, reiniciar e carregar naturalmente a região: limpeza recuperada, entrega ainda disponível. Interromper uma colagem num mundo isolado e conferir a mesma recuperação.
- Revogar/restaurar permissão com página aberta; acesso e rascunho docente acompanham a mudança. Encerrar turma/sair/republicar durante envio deve recusar contexto antigo; saída e encerramento preservam histórico.
- Atualizar dados enquanto professor digita atividade/nota: campos preservados; gravação com revisão antiga informa conflito. Recarregar explicitamente recupera os dados salvos.
- Listas com mais de 20 atividades/alunos/revisões; seleção, Anterior/Próxima e filtros. Textos longos e multilinha; ações permanecem acessíveis fora da rolagem.
- pt-BR/en-US, 1280×720 e 1920×1080 com escala padrão. Fechar, Esc, reabrir e reconectar; regressão breve de ingresso, matrícula `00123`, mensagem do dia e persistência.

Registre build ID/SHA-256, cenário, conta/papel, data, idioma, resolução/escala e resultado. Envie capturas representativas: seleção pronta, aluno com revisão/nota, professor com preview, cópia editada e volume após descarte. Casos que exigem outra conta continuam identificados; não há aceite estável automático.

## Massa isolada para paginação

`gradlew prepareClassroomUiFixture -PclassroomFixtureTeacher=<UUID-professor> -PclassroomFixtureStudent=<UUID-aluno>` gera um diretório **novo** em `build/classroom-ui-fixtures/`, sem alterar um banco existente. A candidata inclui 25 publicações (uma arquivada), três rascunhos privados, 25 alunos com entrega e 25 revisões para a conta fornecida na atividade 01. Revisão 1 tem avaliação; a mais recente não. Inclui os registros administrativos anteriores para busca/paginação.

Use somente num servidor/save de teste separado, com seu próprio diretório de dados do plugin. Com esse servidor parado, copie o banco **e `submissions`** conforme o README gerado. Não substitua os dados do mundo usado nas validações anteriores. O gerador e a massa não entram no JAR de produção.
