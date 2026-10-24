# Ensino, idiomas e comandos

Este guia descreve a ajuda e os comandos atuais. A pré-versão 1.4.0-rc.1 acrescenta somente uma página de prova Classroom; turmas e armazenamento de alunos ainda não existem.

## Orientação e leitura

As letras são relativas ao próprio módulo: A/B nas laterais, C atrás e seta na saída +X do modelo. A rotação transforma todas essas faces juntas. NOT apresenta somente C e saída. As letras/seta aparecem mesmo sem cabos; o indicador superior continua representando exclusivamente o resultado lógico.

Entradas vazias são ignoradas, enquanto entrada conectada sem energia conta como 0. Sem entrada conectada, todas as portas ficam desligadas. XOR conta cada face, mesmo quando duas faces recebem a mesma rede; XNOR de três entradas é paridade par, não a condição “todas iguais”.

## Ajuda e tabelas

`/logichelp` está disponível sem permissão especial e explica as regras, as faces e os limites. Exemplos:

```text
/logichelp --gate=AND --inputs=2
/logichelp --gate=XOR --inputs=3
/logichelp --gate=NOT
```

O padrão é duas entradas para portas de três entradas e uma para NOT. A tabela com uma entrada usa A; com duas, A/B; com três, A/B/C. NOT usa C. Entradas restantes estão vazias. As linhas são calculadas pelo mesmo avaliador do jogo, sem alterar nenhuma alavanca do mundo.

## Inspeção de uma porta

```text
/logicinspect 12 64 20
/logicinspect 12 64 20 --world=nome-do-mundo
```

Requer `sisso.logicgates.inspect`. No console, informar o mundo; no cliente, a resolução nativa pode usar o mundo do jogador. As coordenadas são as do bloco da porta, não a posição da câmera.

A resposta apresenta orientação, valor da saída, cada entrada física, posição do vizinho e número da avaliação. Estados possíveis:

| Entrada | Significado |
| --- | --- |
| Vazia | Nenhum componente compatível; ignorada na regra |
| Conectada em 0/1 | Face válida, com valor conhecido |
| Face incompatível | Há componente, mas suas funções/orientações não conectam |
| Seção indisponível | Não foi possível verificar; não equivale a vazio |

A razão da porta distingue lógica normal, ausência de entradas, seção indisponível e realimentação indefinida. Energia externa no cabo de saída não altera o indicador. A inspeção usa o snapshot concluído, atualiza avaliação suja e nunca força carregamento de seções.

## Diagnóstico e idiomas

`/classroom`, disponível a jogadores sem permissão especial, abre a página de prova com contador e fechamento. Cada abertura começa em zero. Consulte o [roteiro Classroom](classroom.md); sua aprovação exige teste no cliente, separado da validação do circuito.

`/logicenergy`, com `sisso.logicgates.debug`, informa versão/ID, totais por mundo e tempos em milissegundos. O monitoramento e a avaliação têm janelas limitadas; p95 não significa tempo máximo. Consulte [desempenho](performance.md).

Itens, ajuda, diagnósticos e a página Classroom usam o mecanismo nativo de tradução, com en-US e pt-BR. Selecione o idioma correspondente no cliente e reinicie/reconecte se necessário para recarregar traduções. Na validação manual, conferir termos e acentos nas duas línguas. Logs técnicos não são a interface do aluno.
