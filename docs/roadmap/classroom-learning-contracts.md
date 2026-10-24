# Contratos pedagógicos posteriores ao MVP administrativo

Estes são contratos conceituais, não uma API publicada. Não existem tabelas ou telas antecipadas. O MVP atual organiza a turma e permite usar os circuitos consolidados em aulas; uma avaliação de aprendizagem exige seu próprio roteiro pedagógico.

| Contrato | Identidade e conteúdo mínimo |
| --- | --- |
| Atividade | UUID, turma, versão publicada, título e enunciado |
| Entrega | UUID, atividade e versão, aluno UUID, revisão, instante e referência a conteúdo versionado |
| Avaliação | Revisão da entrega avaliada, professor UUID, feedback e resultado |

Publicação deve conservar a versão à qual a entrega responde. Correção deve apontar para uma revisão reproduzível, sem acompanhar alterações posteriores do aluno silenciosamente. Matrícula e nome são apresentação; UUID identifica a pessoa. Vínculos históricos preservados pelo MVP permitem associar a participação à turma sem reaproveitar um vínculo encerrado em um novo ingresso.

O conteúdo será fornecido por um adaptador futuro. Coordenadas, seleção de área, snapshot e prefab ainda exigem decisão sobre reprodução, disponibilidade de seções e experiência do professor. A tela administrativa e seu banco não dependem do circuito vivo. Não escolher o formato nem implementar um adapter fictício antes dessa decisão.

Sequência: definir publicação e seus estados; implementar atividade versionada; decidir entrega e revisão com referências próprias para aluno/professor; implementar feedback manual; depois avaliar correção automática por casos esperados e snapshots. Falha de disponibilidade de seção é falha de contexto, não resposta lógica incorreta.

Transferência docente, múltiplos responsáveis, exclusão/arquivamento de turma, proteção de áreas e retenção definitiva precisam de contratos específicos. O MVP conserva dados e histórico, sem implementar essas operações.
