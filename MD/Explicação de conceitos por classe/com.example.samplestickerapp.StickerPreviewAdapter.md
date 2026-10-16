# StickerPreviewAdapter: células recicladas, seleção e prévia ampliada

Código: `app/src/main/java/com/example/samplestickerapp/StickerPreviewAdapter.java`.
Atualização: 2026-10-16_16-38 — America/Cuiaba.

## Papel e estado

RecyclerView.Adapter conecta os dados StickerPack às células StickerPreviewViewHolder. RecyclerView reutiliza células que saem da tela; portanto onBindViewHolder precisa reaplicar imagem, seleção e ouvintes, sem presumir que a View é nova. O adapter mantém dimensões/padding, recurso de erro, inflater, referência à lista e o pacote atual.

Há dois estados visuais diferentes. selectionMode e selectedPositions representam seleção múltipla para mover/excluir. expandedStickerPreview, expandedPreviewContainer e clickedStickerPreview representam a ampliação de uma única figurinha. A opacidade da lista vai de 1 a 0,2 quando a prévia aparece. O conjunto ampliado contém a imagem e o botão; sua visibilidade é controlada no container, não individualmente na imagem.

## Criação, binding e ciclo da célula

onCreateViewHolder infla sticker_image_item e aplica largura, altura e padding à imagem. onBindViewHolder usa a URI do ContentProvider montada por StickerPackLoader, controlador Fresco com autoplay e reaproveitamento do controlador antigo. Mostra overlay/check quando a posição está selecionada. onViewRecycled limpa o controlador, liberando a associação com conteúdo fora de uso.

Toque longo inicia seleção e fecha a prévia. Se já selecionando, alterna o item. Toque curto também alterna seleção nesse modo; fora dele, abre/fecha ampliação. toggleSelection mantém o Set sem duplicatas e avisa listeners sobre quantidade e entrada/saída do modo. Quando o conjunto fica vazio, selectionMode volta a false.

selectedFiles percorre os stickers na ordem do pacote e transforma posições em nomes; restoreSelection faz o caminho inverso. Isso permite a Activity salvar nomes em Bundle em vez de posições suscetíveis a mudanças. replacePack fecha prévia, substitui dados e limpa seleção. O contrato dos ouvintes é síncrono na thread de interface: a Activity atualiza a toolbar, o adapter não conhece seus MenuItems.

## Exclusão existente

handleDelete verifica se a origem conservará pelo menos três e pede confirmação. deleteSelectedItems ordena posições de forma decrescente para remover sem deslocar os índices ainda pendentes. Primeiro chama ContentsJsonHelper para apagar arquivos/atualizar catálogo; depois remove itens da lista, notifica o RecyclerView, recalcula tamanho e limpa seleção. Essa lógica existente não ganhou uma exceção específica para rascunhos de selecionadas nesta tarefa.

## Geometria da prévia

Ao anexar o adapter, onAttachedToRecyclerView guarda a lista e registra um ouvinte de rolagem; ao desanexar remove o ouvinte. Qualquer deslocamento fecha a prévia, pois a célula de referência mudou de posição.

positionExpandedStickerPreview encontra o ViewHolder clicado, calcula seu centro e subtrai metade das dimensões do cartão. Depois limita as bordas usando tamanho da lista e suas margens. O ajuste atual usa o container inteiro; antes, a imagem era toda a prévia. Se só a altura de 188 dp da imagem fosse usada, o botão abaixo poderia ficar cortado. As posições finais são limitadas a zero para não ficarem negativas.

expandPreview fecha uma ampliação já aberta ou prepara a nova URI e controlador com autoplay. Torna o cartão visível, reduz opacidade da lista e configura o botão. setOnAddSelected fornece um Consumer<Sticker>; ao clicar, o adapter captura o Sticker, fecha a prévia e chama o Consumer. Essa é uma colaboração por callback: a Activity executa o efeito persistente, evitando I/O dentro do adapter.

hideExpandedStickerPreview restaura a célula e opacidade e esconde o conjunto. isStickerPreviewExpanded consulta visibilidade do container. getItemCount retorna o tamanho do pacote, com a infraestrutura existente de cellLimit (neste construtor, zero significa sem limite).

## Validação e limites

O teste instrumentado abre detalhes, toca a primeira célula e verifica botão abaixo da imagem e cartão dentro da lista; também fecha pela imagem. O tamanho fixo de 240 dp atende ao layout atual, mas telas muito estreitas/fontes extremas precisam de avaliação específica. A prévia não é persistida ao recriar a tela; os nomes de seleção múltipla são persistidos pela Activity.

**Exercício:** por que não colocar a cópia do arquivo em onBindViewHolder? **Resposta:** binding ocorre várias vezes pela reciclagem e não representa uma ação explícita de salvar; além disso I/O bloquearia a interface e misturaria responsabilidades.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
