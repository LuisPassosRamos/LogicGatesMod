# Classroom — relato de multiplayer e primeira troca de aba

Em 24/10/2026, após a entrega da rc.7, o usuário informou que o Classroom funcionou em multiplayer com outro jogador e que a perda de permissão atualizou a tela automaticamente. Não houve captura, build ID/checksum explícito ou descrição de todos os fluxos executados. A última candidata entregue era `1.4.0-rc.7+5294934c944b`; isso identifica a entrega, sem comprovar o arquivo instalado por hash.

O usuário relatou uma carga diferente com “Carregando” na primeira troca de aba de cada personagem. No PC que hospedava o mundo, durou alguns segundos, menos de dez; no notebook do segundo jogador, cerca de um a dois segundos. Não há medição de FPS, duração de tick, rede ou tempo entre clique e resposta para determinar a origem exata.

Confirmações por relato: funcionamento com dois clientes e revogação docente refletida sem ação manual na interface. Restauração automática, preservação de campos, todos os casos de ingresso e idiomas/resoluções não foram confirmados separadamente nesta mensagem. As [capturas rc.6](2026-10-24-rc6-client.md) e os relatos anteriores mantêm seu escopo histórico.

A investigação encontrou custo evitável em notificações e transporte e acrescentou um [benchmark isolado](2026-10-24-rc7-performance.json). A comparação e seus limites constam no [guia de desempenho](../../performance.md#classroom--consultas-notificações-e-transporte). Os ensaios não reproduzem a demora visual do cliente nem equivalem a mil jogadores conectados. A rc.8 otimiza esses caminhos e exige reteste da primeira interação, seleção e atualização de permissões.
