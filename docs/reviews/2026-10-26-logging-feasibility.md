# Diagnóstico do mod — viabilidade e custo

Análise em 26/10/2026 sobre a rc.10, revisão de produção `e0acb9ba79e9`, sem mudança de runtime, configuração, banco ou artefatos distribuídos. O relato e as capturas de cliente desta rodada ficam em [evidência própria](../evidence/classroom/2026-10-26-rc10-client.md). Esta revisão recomenda uma implementação futura; não acrescenta logging detalhado agora.

## Conclusão

É viável e útil ampliar o diagnóstico usando o logger nativo já existente. O desenho recomendado registra falhas inesperadas e avisos operacionais com limitação de repetição. Detalhes de ações e tempos ficam em modo temporário de diagnóstico, desligado por padrão. O custo esperado é baixo com volume controlado; não foi medido um percentual, pois a implementação proposta ainda não existe.

Registrar cada clique, atualização, SELECT, tick, jogador ou bloco permanentemente teria volume proporcional à atividade e poderia competir por CPU, memória e disco. Logging não deve acrescentar consultas ou gravações ao SQLite, invalidar dados/permissões ou disparar atualização de página. Não precisa de outro executor/arquivo de produção para a primeira entrega.

## O que existe e o que falta

- [LogicSimPlugin.setup](https://github.com/LuisPassosRamos/LogicGatesMod/blob/e0acb9ba79e9/src/main/java/sisso/logicsim/LogicSimPlugin.java) registra build/Hytale e caminho do banco. O callback de falhas usa `atSevere().withCause(...)` e uma mensagem fixa de armazenamento indisponível.
- [ClassroomRuntime](https://github.com/LuisPassosRamos/LogicGatesMod/blob/e0acb9ba79e9/src/main/java/sisso/logicsim/classroom/ClassroomRuntime.java) informa falhas de inicialização, operações com `ClassroomException.UNAVAILABLE`, exceções de callbacks e demora no shutdown. O mesmo callback recebe origens diferentes; a mensagem fixa pode atribuir um erro de listener ao armazenamento indevidamente.
- `submit` propaga exceções inesperadas fora de `ClassroomException` pelo Future sem registro explícito nessa fronteira. Rejeição de fila é convertida em indisponibilidade sem distinguir saturação de encerramento no diagnóstico. Serviço não inicializado também retorna indisponibilidade; uma falha inicial já foi registrada, portanto evitar repeti-la por cada pedido posterior.
- [SqliteClassroomRepository.read](https://github.com/LuisPassosRamos/LogicGatesMod/blob/e0acb9ba79e9/src/main/java/sisso/logicsim/classroom/persistence/SqliteClassroomRepository.java) preserva a causa das falhas operacionais ao convertê-las em indisponibilidade. A infraestrutura existente permite acrescentar contexto sem registrar os parâmetros SQL ou o conteúdo dos formulários.
- [ClassroomPage](https://github.com/LuisPassosRamos/LogicGatesMod/blob/e0acb9ba79e9/src/main/java/sisso/logicsim/classroom/ui/ClassroomPage.java) traduz falhas para o usuário; o ramo de erro desconhecido vira mensagem genérica de indisponibilidade. Fechar mundo/página é tratado como descarte de sessão. [ClassroomPresence](https://github.com/LuisPassosRamos/LogicGatesMod/blob/e0acb9ba79e9/src/main/java/sisso/logicsim/classroom/ClassroomPresence.java) suprime o envio da mensagem do dia quando a cadeia falha; as falhas de armazenamento dependem do registro no runtime.

Os achados são delimitados às fronteiras de Classroom e à inicialização do plugin, com pesquisa de chamadas de logging em `src/main/java/sisso/logicsim`. Não são uma auditoria de todas as exceções possíveis do Hytale nem uma coleta dos logs do mundo real.

## Transporte nativo conferido no SDK 0.7.4

Inspeção por `javap -p -c` no `Server-0.7.4.jar` usado pelo build, sem executar um servidor ou alterar UserData. Saída integral preservada em `build/tmp/logging-review/`.

- `HytaleLogger.at(Level)` confere nível habilitado e retorna `NoOp` quando desabilitado. Argumentos construídos antes da chamada ainda teriam custo; usar guarda de nível/configuração antes de montar diagnósticos extensos.
- `HytaleFileHandler` e `HytaleConsole` possuem threads próprias e filas `LinkedBlockingQueue` criadas sem capacidade explícita. Com as threads ativas, as mensagens são enfileiradas; a escrita/formatação de saída acontece no consumidor. Se a thread correspondente não estiver ativa, há caminho direto de publicação.
- O backend também pode encaminhar exceções a um handler Sentry configurado e a assinantes do servidor. Usar o logger nativo não torna a chamada inteira gratuita nem garante um custo fixo independente da configuração.
- Filas sem limite prático explícito no construtor reforçam a necessidade de limitar volume na origem. Um erro repetido em muitas contas pode acumular mensagens mesmo que a escrita seja assíncrona. Não presumir que rotação/retenção global resolva essa pressão; conferir a configuração operacional do servidor.

Essas observações são do binário local 0.7.4, não uma promessa sobre futuras versões ou sobre a configuração do mundo do usuário.

## Incremento recomendado

| Modo | Conteúdo | Controle de custo |
| --- | --- | --- |
| Normal | Falha inesperada, banco indisponível, fila saturada e falha relevante de transporte/callback; build, subsistema e operação | Primeiro erro com causa, repetidos agrupados e limitados; sem registrar operações bem-sucedidas continuamente |
| Diagnóstico temporário | Operação, resultado, espera na fila e execução; identificação técnica mínima para correlação | Desligado por padrão, prazo de expiração, limite/amostragem e resumo; sem texto de formulário ou parâmetros SQL |
| Tráfego normal inválido | Matrícula vazia, revisão antiga, tentativa sem permissão e sessão fechada | Continuar respondendo ao jogador, sem stack trace por tentativa; falha de contexto esperada não é erro de servidor |

Agrupamento deve ter estado limitado por categorias estáveis (subsistema/operação/tipo de falha), sem criar uma entrada permanente por UUID ou mensagem arbitrária. Registrar na fronteira responsável uma vez, evitando repetir a mesma causa no repositório, runtime e UI. Distinguir fila cheia durante operação de shutdown esperado. O próprio diagnóstico não pode derrubar o fluxo que está tentando observar.

Identificadores de correlação não precisam incluir matrícula, nome, descrição ou mensagem do dia. Logs de servidor não mostram cortes de texto, FPS, renderização incorreta ou todos os erros de markup do cliente; os logs do cliente e capturas continuam necessários nesses casos. Um diagnóstico de operações não é uma gravação de tudo que ocorre no mundo nem uma trilha completa de auditoria.

## Verificação antes de entregar código

Testar falhas inesperadas e causas, ausência de ruído nas recusas esperadas, agrupamento sob repetição, estado limitado e modo desligado/expirado. Conferir que não surgem SELECTs, notificações ou pacotes de UI adicionais. Comparar o harness existente no mesmo ambiente com diagnóstico desligado e ligado sob amostragem, preservando a identidade do JAR; medir rajadas e excesso de erros em ambiente isolado. Só então quantificar impacto. Não é necessário repetir `fullCheck` ou benchmarks nesta análise documental, que não alterou o código.
