# Classroom — aceite da 1.4.0-rc.14

Build entregue: **1.4.0-rc.14+5485651dd6e3**, revisão de produção `4e1722060e14`. JAR SHA-256 `5cbeaa10090437eb4192223f34cb69526ff95ef3919de5e100f48675ff78a03e`. [Verificações automatizadas](evidence/classroom/2026-10-26-rc14-server.json).

**Cliente: funcionamento/polimento aprovados por [relato do usuário](evidence/classroom/2026-10-26-rc14-client.md).** O relato não discrimina a matriz de resoluções, escalas e idiomas abaixo; combinações não informadas continuam sem execução registrada. As [capturas rc.13](evidence/classroom/2026-10-26-rc13-client.md) confirmam a apresentação docente naquela configuração e fundamentam este ajuste; não aprovam as proporções ou a edição explícita da rc.14. SQLite v5, circuitos e conteúdo das entregas permanecem.

Com o mundo parado, substitua o JAR por `LogicGates-1.4.0-rc.14.jar`, mantendo somente uma versão em `mods`. Guarde o ZIP fora de `mods`. Preserve o banco, `submissions`, configuração e mundos; não apague dados. Registre build ID/SHA-256 do arquivo entregue.

## Consulta e edição

1. Professor: selecione uma atividade publicada. Esperado: título, situação, enunciado, prazo e valor em consulta; nenhum formulário ou botão de gravação. **Editar atividade**, junto ao título, abre o formulário.
2. Altere campos sem salvar e use **Voltar à visualização**. Esperado: consulta dos dados salvos, sem gravação automática. Entrar em Editar novamente recupera os dados salvos. **Salvar rascunho** mantém o editor; **Publicar** retorna à consulta.
3. Havendo edição privada, professor vê o rascunho salvo com aviso sobre a publicação vigente. Aluno vê apenas os dados publicados, inclusive na lista. Rascunho sem publicação mostra dados ainda não definidos sem inventar prazo/valor.
4. Arquivada: consulta disponível ao professor, sem Editar; Restaurar permite edição novamente. Conferir cancelamento de edição, troca de atividade, filtro, paginação e retorno da lista de alunos. Nenhum desses caminhos deve reabrir o editor sozinho.
5. Nova atividade continua ampla, sem lista/paginação; Voltar às atividades preserva o rascunho já criado, sem salvar os campos automaticamente. Publicar retorna à consulta normal com lista.
6. Aluno e consulta de alunos/entregas nunca oferecem edição da atividade. Nota/feedback da revisão continuam com o próprio formulário de correção; não dependem do botão Editar atividade.

## Proporções e rolagem

As colunas repartem a largura por pesos 1:3, com lateral limitada a 240–440 unidades e detalhes com mínimo de 280. Botões têm 48 unidades; campos simples 46; enunciado 220 na edição/260 na criação; descrição/mensagem da turma 200; feedback 160; preview 360. Fontes de títulos, informações e campos foram ampliadas. Isso define proporções em unidades de UI, sem garantir tamanho físico em todas as escalas.

Conferir Turma, Ingressos, Convidar, Administração, lista/consulta/edição/criação de atividades e correção. Navegação, paginação e ações quebram linha; conteúdo e formulários permanecem roláveis, com ações de gravação fora da rolagem. Valores numéricos têm largura própria. Estados vazios continuam específicos, sem preencher a tela com registros inexistentes.

Usar **1280×720, 1366×768, 1920×1080 e 2560×1440**, nas escalas padrão e máxima disponível, em **pt-BR/en-US**. Registrar escala real e captura completa do viewport; marcar combinações indisponíveis como não executadas. Conferir Anterior/Próxima ao quebrar linha, textos longos/multilinha, botões de encerramento e controles sempre alcançáveis. A ocupação mínima de 90% por eixo da rc.13 continua um critério de cliente, não uma medição deduzida dos PNGs recortados.

## Regressão breve

- Selecionar/salvar/reabrir prazo de Brasília; data parcial, bissexto e prazo passado/futuro. Publicação continua exigindo prazo futuro; armazenamento UTC preservado.
- Atualização de dados durante edição mantém campos e revisão original; mudança concorrente exige recarregar antes de sobrescrever. Revogação docente descarta edição local e restauração não reabre o editor automaticamente.
- Contagem de atividades por perfil, criação/publicação/arquivamento/restauração e vínculo único. Reutilizar a [massa isolada](classroom-rc11-acceptance.md#massa-isolada-para-paginação), sem substituir o banco real.
- Com duas contas: entrega, preview ampliado, seleção de revisão, colagem/descarte e nota no aluno. Conteúdo salvo e regras da revisão continuam iguais.

Automação verifica estados, transporte e autorização; não comprova renderização. A versão estável, tags, logging ampliado e avaliação automática continuam posteriores.
