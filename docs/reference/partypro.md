# PartyPro 1.0.36 — referência técnica para Classroom

Análise estática em 24/10/2026 da extração local externa `LogicGatesMod-references/partypro-1.0.36`. Inventário: 138 classes compiladas, 18 documentos UI, 76 imagens e sete idiomas (350 chaves correspondentes por idioma). As classes relevantes foram inspecionadas por bytecode/decompilação; o mod não foi executado. O manifest declara `>=0.7.0 <0.8.0`, sem comprovar nosso cliente exato.

A [visão geral oficial](https://pro-hytale-mods.gitbook.io/pro-hytale-mods-docs/getting-started/quickstart) foi consultada. O índice llms.txt e capítulos adicionais da API não ficaram acessíveis. A referência não integra o build ou a distribuição do LogicGates.

## Padrões úteis

- `PartyBrowserNewGui` usa a mesma família InteractiveCustomUIPage, builders, codecs e eventos da nossa prova; navegação lateral e áreas visíveis por seção.
- `MyPartyMemberRow.ui` demonstra linha reutilizável e clicável; `PartyBrowserNew.ui` separa lista e ficha, com rolagem independente.
- `PlayerNameTracker` associa UUID ao último nome conhecido; nomes são apresentação, sem substituir identidade técnica.
- `PartySnapshot` copia listas e fornece uma visão imutável. Eventos de alteração inspiram notificações internas posteriores ao commit.

## Diferenças que orientam nossa implementação

- Convites ficam em mapa por destinatário, expiram e podem efetivar ingresso imediato. Classroom persiste um processo por turma/aluno, exige matrícula e conferência docente e não expira convites automaticamente.
- Algumas ações usam posição do membro na lista. Classroom identifica processo/vínculo por UUID e revalida revisão e responsabilidade no servidor.
- PartyStorage grava JSON por arquivo temporário/substituição, periodicamente quando dirty. Falha de leitura pode retornar coleção vazia. Classroom conserva SQLite transacional e recusa banco incompatível sem substituí-lo.
- As regras do PartyPro estão distribuídas entre comando, UI e manager; a checagem de vínculo existente difere entre esses caminhos. Classroom concentra política nos serviços e invariantes no banco.
- Remoção por ausência e promoção automática são comportamentos configuráveis de grupos de jogo. Turmas conservam responsável e histórico após desconexão.
- A UI clássica e a nova compartilham implementação por herança, com muitas funções de HUD/RPG. Classroom mantém páginas e apresentadores pequenos por responsabilidade.
- Avatares usam serviço HTTP e cache. O MVP usa apresentação nativa simples, sem dependência de rede.
- A tradução do PartyPro usa JSON e configuração própria. LogicGates mantém i18n nativa en-US/pt-BR com auditoria das referências UI/Java.

HUD, pings, mapa, combate, teleporte, XP e integrações não são requisitos administrativos. A experiência de entrega/avaliação precisa de contrato próprio e não deriva dessas funções.
