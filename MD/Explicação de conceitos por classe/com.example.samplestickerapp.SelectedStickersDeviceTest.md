# SelectedStickersDeviceTest: prévia e WebP real no Android

Código: `app/src/androidTest/java/com/example/samplestickerapp/SelectedStickersDeviceTest.java`.
Atualização: 2026-10-16_16-38 — America/Cuiaba.

## Instrumentação e thread principal

Instrumentation executa testes dentro do ambiente Android e fornece o Context do app. É necessária para abrir uma Activity, observar Views reais, usar Bitmap e capturar a tela. runOnMainSync executa toques e verificações de layout na thread de interface. O teste não injeta gestos no sistema: usa performClick nos componentes do próprio aplicativo.

## Prévia real

previewShowsSelectionButtonBelowStickerAndImageDismissesIt lê um pacote do catálogo e abre StickerPackDetailsActivity com a mesma informação que a navegação normal utiliza. Aguarda o primeiro layout, obtém a célula por findViewHolderForAdapterPosition(0) e toca na imagem.

Depois exige container visível, botão apresentado, topo do botão abaixo da imagem e conjunto dentro da área da lista. Uma pausa adicional permite o carregamento assíncrono da animação pelo Fresco antes de takeScreenshot. A captura vai para cache/selected-sticker-preview.png para inspeção visual. Finalmente toca na imagem e verifica que o container ficou invisível. O finally termina a Activity mesmo se alguma asserção falhar.

As coordenadas relativas são suficientes porque imagem e botão pertencem ao mesmo LinearLayout. O teste cobre a primeira célula na configuração atual do aparelho, não todas as combinações de fonte, rotação e dimensões de tela. Não clica em salvar a figurinha desse pacote, preservando o catálogo do usuário.

## Arquivos reais, armazenamento isolado

collectionCopiesRealWebpIntoIsolatedStorageWithoutChangingSource cria um diretório UUID no cache do app, com assets/source. Gera uma imagem verde 512×512 com Bitmap, codifica WebP e cria ícone PNG 96×96. Chama o helper de produção duas vezes para a mesma entrada. Confere primeira contagem, duplicata, origem existente, marcador e dimensão do arquivo copiado decodificado.

O finally recicla o Bitmap e remove somente a raiz de teste. Essa execução complementa os bytes fictícios da suíte JVM, mas não testa todo o ciclo de catálogo e exportação. O teste utiliza a API de WebP do Android para uma amostra estática; preservação de bytes no helper também é testada na JVM. Não adiciona pacote ao WhatsApp nem envia mensagem.

## Limites e exercício

Os dois testes passaram no aparelho conectado. Eles precisam de catálogo acessível para a verificação de prévia; a cópia isolada não depende de um destino real. Pausas usadas para layout/imagem não são um benchmark nem garantia de prontidão em qualquer hardware. As asserções de estado complementam a captura, que permite observar aspectos que coordenadas não revelam.

**Exercício:** por que a captura não substitui assertTrue(button.isShown())? **Resposta:** imagem exige inspeção humana; a asserção falha automaticamente quando a estrutura funcional não aparece. Os dois mecanismos verificam aspectos diferentes.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
