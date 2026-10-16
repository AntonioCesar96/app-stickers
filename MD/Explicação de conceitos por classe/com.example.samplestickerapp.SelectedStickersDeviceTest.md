# SelectedStickersDeviceTest: prévia e WebP real no Android

Código: `app/src/androidTest/java/com/example/samplestickerapp/SelectedStickersDeviceTest.java`.
Atualização: 2026-10-16_17-04 — America/Cuiaba.

## Instrumentação e thread principal

Instrumentation executa testes dentro do ambiente Android e fornece o Context do app. É necessária para abrir uma Activity, observar Views reais, usar Bitmap e capturar a tela. runOnMainSync executa toques e verificações de layout na thread de interface. O teste não injeta gestos no sistema: usa performClick nos componentes do próprio aplicativo.

## Prévia real

previewShowsSelectionButtonBelowStickerAndImageDismissesIt lê um pacote do catálogo e abre StickerPackDetailsActivity com a mesma informação que a navegação normal utiliza. Aguarda o primeiro layout, obtém a célula por findViewHolderForAdapterPosition(0) e toca na imagem.

Depois exige container visível, botão apresentado, topo do botão abaixo da imagem e conjunto dentro da área da lista. Uma pausa adicional permite o carregamento assíncrono da animação pelo Fresco antes de takeScreenshot. A captura vai para cache/selected-sticker-preview.png para inspeção visual. Finalmente toca na imagem e verifica que o container ficou invisível. O finally termina a Activity mesmo se alguma asserção falhar.

As coordenadas relativas são suficientes porque imagem e botão pertencem ao mesmo LinearLayout. O teste cobre a primeira célula na configuração atual do aparelho, não todas as combinações de fonte, rotação e dimensões de tela. Não clica em salvar a figurinha desse pacote, preservando o catálogo do usuário.

## Arquivos reais, armazenamento isolado

collectionCopiesRealWebpIntoIsolatedStorageWithoutChangingSource cria um diretório UUID no cache do app, com assets/source. Gera uma imagem verde 512×512, codifica WebP e cria ícone PNG 96×96. Chama o helper duas vezes com InitialStickerTemplates real. Exige três arquivos, uma escolha, duas temporárias, duplicata sem aumento, origem existente, marcador e dimensão de um WebP copiado.

O finally recicla o Bitmap e remove somente a raiz de teste. Essa execução complementa os bytes fictícios da suíte JVM, mas não testa todo o ciclo de catálogo e exportação. Não adiciona pacote ao WhatsApp nem envia mensagem.

## Amostras iniciais reais: estáticas e animadas

bundledStaticAndAnimatedSamplesFillThenDisappearAsChoicesArrive cria outra raiz isolada e executa o mesmo algoritmo para false e true. A fonte é InitialStickerTemplates(context.getAssets()), de modo que realmente lê Pacote 2/Pacote 1 do APK. Para cada tipo, copia três amostras para uma origem de teste e as escolhe progressivamente. Na primeira seleção, a amostra escolhida não pode aparecer também como preenchimento; as outras duas completam. Na segunda resta uma temporária; na terceira nenhuma.

As asserções exigem total três, i+1 escolhas, 2-i temporárias, ausência de duplicate nas novas escolhas e marcador animado coerente. Percorre os WebP do destino e chama isFiller para confirmar a classificação persistida. Todos os originais continuam existindo. A raiz é apagada no finally, sem tocar nos diretórios reais do usuário. Isso verifica fonte, distinção por tipo e transição completa, não apenas um mock de contador.

## Limites e exercício

Os quatro testes passaram no aparelho conectado (4,134 s na alteração de fluxo direto). Precisam de catálogo acessível para a prévia; cópias e metadados adicionais usam raízes isoladas. Pausas de layout/imagem não são benchmark nem garantia universal de prontidão. Não automatizam confirmação no WhatsApp nem morte abrupta do processo.

## Dados completos e projeção visível

individualMetadataRemainsAvailableButAppListHidesIt cria em cache um diretório comum e outro com marcador de individuais. A sobrecarga de ContentsJsonHelper.getStickerPacks(File) gera os dois modelos, com o título Individuais - estaticas +1 e o identificador antigo preservado. A ausência de marcador de versão nas pastas também exercita o tratamento de array vazio.

Converte os modelos em StickerPack e passa a lista completa a StickerPackLoader.visiblePacks. Exige apenas o pacote normal na saída e os dois ainda presentes na entrada. Isso prova que o filtro de interface não muta os metadados completos. Não faz consulta do WhatsApp nem confirma uma exportação externa: é uma verificação da projeção de dados usada pela aplicação.

**Exercício:** por que a captura não substitui assertTrue(button.isShown())? **Resposta:** imagem exige inspeção humana; a asserção falha automaticamente quando a estrutura funcional não aparece. Os dois mecanismos verificam aspectos diferentes.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
