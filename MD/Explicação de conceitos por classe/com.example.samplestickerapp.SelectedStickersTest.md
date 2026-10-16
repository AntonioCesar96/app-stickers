# SelectedStickersTest: verificar efeitos persistentes em isolamento

Código: `app/src/test/java/com/example/samplestickerapp/SelectedStickersTest.java`.
Atualização: 2026-10-16_16-38 — America/Cuiaba.

## Ambiente e responsabilidade

Esta classe JUnit roda na JVM do computador. TemporaryFolder cria uma raiz exclusiva por teste e remove seus arquivos ao terminar. O helper de produção recebe essa raiz explicitamente, portanto nenhum cenário precisa tocar nos pacotes do celular. Os arquivos WebP de teste têm bytes pequenos e intencionalmente não são imagens: aqui o objetivo é verificar regras de cópia, identidade e recuperação, não o decoder.

source cria diretório, ícone fictício e, quando solicitado, marcador animado. sticker grava três bytes com valor variável para produzir conteúdos diferentes. expectFailure executa uma Operation que pode lançar IOException e exige falha com mensagem; não considera que qualquer exceção inesperada seja sucesso.

## Os seis cenários

1. copiesOriginalBytesPreservesSourceAndBuildsDraftThenReadyPack adiciona três entradas em sequência. Confere contagens 1, 2 e 3, existência dos originais, bytes de uma cópia e do ícone, marcador de coleção e evolução para v_3 sem v_1. Verifica que o estado final corresponde à operação persistida.
2. deduplicatesIdenticalBytesAcrossSourcesAndNames usa nomes e pacotes diferentes com bytes iguais. Exige mesmo destino, duplicate=true e contagem inalterada.
3. separatesTypesAndNeverReusesUnmanagedNameCollision prepara um diretório comum com o nome padrão. Exige novo nome numerado e coleção animada distinta com animado.txt.
4. startsNextPackAtThirtyAndFindsDuplicatesInFullPack cria 31 entradas diferentes. Confere 30 no primeiro e uma no segundo, depois retorna uma duplicata pertencente ao primeiro cheio. Isso testa a ordem de busca, além da comparação do limite.
5. rejectsMissingOrEscapingSourceAndLeavesNoPartialPack exercita entradas indevidas, tipo inconsistente, arquivo ausente e falta de ícone. Confere que não surgiu coleção incompleta nem staging residual em falha normal.
6. failedVersionUpdateRollsBackOnlyNewCopy injeta versão máxima, tenta nova escolha e confere uma única cópia anterior, nenhum .tmp e origem preservada. Isso verifica tratamento de erro num ponto posterior à cópia.

## O que os resultados significam

Os testes verificam conteúdo e efeitos observáveis, sem apenas espelhar todos os métodos privados. Não demonstram validade WebP, comportamento do ContentProvider, interface ou exportação ao WhatsApp. A regra de mínimo de três na exportação é responsabilidade Android separada. Os seis testes passaram no computador; a validação em dispositivo complementa com mídia real e interface.

**Exercício:** por que testar 31 itens em vez de só testar o número 30 numa função? **Resposta:** o cenário demonstra criação, preenchimento, transbordo e reconhecimento de duplicata no pacote anterior, incluindo efeitos de arquivo que uma condição isolada não prova.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
