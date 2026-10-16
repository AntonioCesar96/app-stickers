# SelectedStickersTest: verificar efeitos persistentes em isolamento

Código: `app/src/test/java/com/example/samplestickerapp/SelectedStickersTest.java`.
Atualização: 2026-10-16_16-55 — America/Cuiaba.

## Ambiente e responsabilidade

Esta classe JUnit roda na JVM do computador. TemporaryFolder cria uma raiz exclusiva por teste e remove seus arquivos ao terminar. O helper de produção recebe essa raiz explicitamente, portanto nenhum cenário precisa tocar nos pacotes do celular. Os arquivos WebP de teste têm bytes pequenos e intencionalmente não são imagens: aqui o objetivo é verificar regras de cópia, identidade e recuperação, não o decoder.

source cria diretório, ícone fictício e, quando solicitado, marcador animado. sticker grava três bytes com valor variável para produzir conteúdos diferentes. expectFailure executa uma Operation que pode lançar IOException e exige falha com mensagem; não considera que qualquer exceção inesperada seja sucesso.

## Fonte simulada e contagens

A interface Templates é implementada no teste com três nomes e ByteArrayInputStream de conteúdos distintos. Isso reproduz leitura de bytes das iniciais sem AssetManager ou arquivos de instalação. O método auxiliar add injeta essa fonte no helper de produção. Os resultados distinguem count físico, selectedCount e fillerCount: uma escolha tem count=3, selectedCount=1, fillerCount=2.

## Os dez cenários

1. copiesOriginalBytesPreservesSourceAndBuildsDraftThenReadyPack adiciona três entradas em sequência. Apesar do nome histórico do método, agora cada estado tem total três: 1+2, 2+1, 3+0. Verifica origem, cópia, ícone, marcador e v_3 sem v_1.
2. deduplicatesIdenticalBytesAcrossSourcesAndNames usa nomes e pacotes diferentes com bytes iguais. Exige mesmo destino, duplicate=true e contagem inalterada.
3. separatesTypesAndNeverReusesUnmanagedNameCollision prepara um diretório comum com o nome padrão. Exige novo nome numerado e coleção animada distinta com animado.txt.
4. startsNextPackAtThirtyAndFindsDuplicatesInFullPack cria 31 escolhas diferentes. Confere 30 no primeiro e uma escolha mais duas temporárias no segundo, depois encontra duplicata no primeiro cheio.
5. rejectsMissingOrEscapingSourceAndLeavesNoPartialPack exercita entradas indevidas, tipo inconsistente, arquivo ausente e falta de ícone. Confere que não surgiu coleção incompleta nem staging residual em falha normal.
6. failedVersionUpdateRollsBackOnlyNewCopy injeta versão máxima, tenta nova escolha e confere os três arquivos anteriores preservados, sem publicação parcial e com origem intacta. Testa a reversão após cópia e retirada de temporários.
7. duplicateAlsoRepairsOldIncompleteCollection remove os preenchimentos para simular uma coleção antiga e tenta adicionar uma escolha repetida. Exige duplicate=true, uma permanente e duas temporárias: deduplicação não dispensa reparo.
8. explicitlyChoosingAnInitialPromotesItAndDoesNotDeduplicateAgainstFiller escolhe uma das amostras como permanente e depois clica nos temporários restantes. Eles viram escolhas e a transição termina em três permanentes sem temporários.
9. templateFailureRollsBackAdditionAndKeepsPreviousFillers injeta uma fonte vazia ao tentar adicionar a segunda escolha. Exige falha, três arquivos anteriores e v_1 preservados. Preparar antes de publicar evita estado incompleto.
10. modifiedFillerIsPreservedAsARealChoice altera os bytes de uma cópia temporária sem atualizar o nome. Ao adicionar outra escolha, exige que o conteúdo modificado permaneça e seja contado como permanente, não removido pelo algoritmo.

## O que os resultados significam

Os testes verificam conteúdo e efeitos observáveis, sem apenas espelhar todos os métodos privados. Não demonstram validade WebP, comportamento do ContentProvider, interface ou exportação ao WhatsApp. Os dez testes passaram no computador; a validação Android complementa com assets reais dos dois tipos. Os cenários usam estruturas temporárias e não alteram os pacotes do usuário.

**Exercício:** por que testar 31 itens em vez de só testar o número 30 numa função? **Resposta:** o cenário demonstra criação, preenchimento, transbordo e reconhecimento de duplicata no pacote anterior, incluindo efeitos de arquivo que uma condição isolada não prova.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
