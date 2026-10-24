# Referência de compatibilidade — Hytale 0.7.4

O projeto usa a dependência exata **com.hypixel.hytale:Server:0.7.4** e manifest `ServerVersion = =0.7.4`. Java 25, Gradle Wrapper 9.2.0 e ScaffoldIt 0.2.16 compõem o ambiente atual. O pacote de referências consultado em 16/10/2026 descrevia páginas web 0.7.2; isso é histórico e não altera o alvo do projeto.

## Ordem de evidência

1. Dependência efetivamente resolvida e codecs/classes do servidor 0.7.4.
2. Testes de compilação, decodificação de assets e integração real.
3. Documentação oficial correspondente ao mesmo canal/versão.
4. Notas de migração e guias comunitários, adaptados após conferir as assinaturas.

O [índice de fontes](sources.md) preserva os links úteis das referências atualizadas. Links de documentação podem mudar de versão; não copiar um exemplo só porque ele abre no navegador. Não transportar APIs de Bukkit/Forge/Fabric por semelhança de nomes.

## Contratos usados atualmente

- `JavaPlugin` e `JavaPluginInit`: entrada; registros ficam em `LogicSimPlugin.setup()`.
- `ChunkStore`, `BlockComponentSection`, `ChunkSection` e holders: acesso a componentes de blocos em seções carregadas.
- `ChunkStore.getChunkSectionReferenceAtBlock` e operações de seção substituem acessos antigos por coluna usados em tutoriais.
- `BlockOperations.setBlock`: alteração de blocos na integração. Estados visuais no mod preservam entidade/rotação pelas operações apropriadas em `BlockComponentAccess`.
- `ComponentType`, `Store`, `Ref`, `Holder` e `CommandBuffer` pertencem ao respectivo domínio ECS. Não trocar referências de EntityStore e ChunkStore.
- Componentes com codec salvam os dados autoritativos. Uma referência transitória de ECS não é um identificador persistente.
- `IncludesAssetPack` precisa estar ativo. Os itens/estados/hitboxes são decodificados também na integração, não apenas tratados como JSON sintaticamente válido.

Não reintroduzir `WorldChunk.setTicking` ou o acesso antigo a componentes de bloco por coluna. APIs preliminares com prefixo `dev_` não são uma base assumida para a futura UI.

## Thread e ciclo de vida

Estado mutável do circuito pertence à thread do mundo/Store. Não bloquear tick com leitura de arquivo, banco ou carregamento de instância; resultados assíncronos devem voltar ao contexto apropriado, revalidando mundo, seção, referência e revisão.

Eventos tradicionais e eventos ECS usam registros distintos. O índice de entidades e seções cobre mais caminhos que cliques de jogadores: ferramentas criativas, carregamento e mudanças de orientação também podem alterar a topologia. Uma tentativa cancelada de quebra não pode remover antecipadamente o nó.

Seção indisponível não é ar. Não carregar vizinhos para satisfazer uma avaliação elétrica. Após recarga, reconstruir índices e sinais em vez de confiar no visual salvo.

## Custom UI — incremento 0

A pré-versão 1.4.0-rc.1 implementa a primeira página Classroom com documentos `.ui`, `InteractiveCustomUIPage`, `UICommandBuilder`, `UIEventBuilder` e `PageManager.openCustomPage`. As assinaturas foram conferidas na dependência 0.7.4; layouts e estilos de referência foram inspecionados nos assets locais. `AbstractPlayerCommand` abre a página no contexto do mundo; `CustomPageLifetime.CanDismiss` permite dispensa e o botão usa `close()`.

O codec recebe somente a ação; o contador atualizado pelo servidor usa `TextSpans` e mensagem traduzida. Bindings bloqueiam a interação até a resposta; ações desconhecidas recebem atualização vazia. Páginas tratam interação; HUD serve à exibição. Os arquivos não são HTML/CSS/JSX.

A página ainda precisa da prova no cliente: [abrir, clicar, atualizar, fechar e reabrir](../classroom.md). Builders/codecs exercitados em teste e servidor iniciado não validam o layout. Criar os painéis completos somente após registrar essa prova.

## Mudança de versão

Atualizar manifest, conferir notas de migração/assinaturas/formatos, executar `fullCheck` e repetir o roteiro do cliente em cópia de teste do mundo. A atual exigência exata impede anunciar compatibilidade com outra versão apenas porque o Java compilou. Não há migração automática de mundos antigos nesta entrega.
