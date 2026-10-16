# InitialStickerTemplates: ler amostras originais do APK

Código: `app/src/main/java/com/example/samplestickerapp/InitialStickerTemplates.java`.
Atualização: 2026-10-16_16-55 — America/Cuiaba.

## Problema e responsabilidade

SplashActivity.criarPastasIniciais usa assets para iniciar os pacotes externos. O preenchimento das coleções precisa dessas mesmas amostras mesmo quando o usuário apagou os pacotes iniciais. Esta classe lê diretamente os arquivos empacotados no APK por AssetManager; não modifica Splash nem recria os pacotes externos.

Asset é um recurso incluído na aplicação. Seu acesso fornece um InputStream, não exige um caminho de arquivo comum e funciona com o empacotamento do Android. O campo final assets recebe AssetManager no construtor. A referência é conservada para as leituras; não há Activity, View ou estado de transferência guardado.

## Interface e métodos

A classe implementa SelectedStickers.Templates. Esse contrato contém names(boolean) e open(boolean, String), permitindo que o algoritmo use uma fonte abstrata. No app a fonte é AssetManager; nos testes unitários é ByteArrayInputStream. Isso mantém a lógica de contagens e troca de arquivos testável sem Android.

directory escolhe Pacote 1 para animated=true e Pacote 2 para false. O mapeamento foi verificado na Splash e no conteúdo dos assets: Pacote 1 contém animado.txt. Não se descobre o tipo pelo nome visual de uma figurinha nem pelo estado editável do armazenamento compartilhado.

names chama assets.list e filtra apenas extensões WebP com Locale.ROOT, ignorando ícone e marcadores. Retorna um array novo; o helper o ordena para seleção determinística. Uma lista ausente resulta em array vazio e o algoritmo produz erro quando não consegue preencher, sem inventar um arquivo.

open rejeita separadores de caminho no nome e abre `directory + "/" + name`. Os nomes normalmente vêm de names; a verificação mantém o contrato limitado a um arquivo da pasta. IOException propaga para o helper, que prepara candidatos antes de publicar e pode reverter uma tentativa. Quem consome o InputStream deve fechá-lo; prepareFillers usa try-with-resources.

## Fluxo e custos

Na primeira escolha estática, a Activity cria a fonte e passa ao helper. Ele chama names(false), ignora uma amostra se seus bytes já forem a escolha e abre as necessárias para completar duas. Na terceira escolha, prepareFillers calcula zero e nem consulta a fonte. O custo da listagem é proporcional à quantidade de nomes na pasta; leitura é proporcional aos bytes das amostras utilizadas. Não há cache próprio ou download.

O mapeamento de nomes depende da estrutura atual do APK: se os pacotes embarcados forem reorganizados, esta classe e a Splash precisam continuar coerentes. Os arquivos são copiados sem recompressão; a classe não valida codec, tamanho nem frame rate. Sua responsabilidade é fornecer as amostras já embarcadas e separadas por tipo.

## Validação e exercício

O teste Android bundledStaticAndAnimatedSamplesFillThenDisappearAsChoicesArrive usa a implementação real para ambos os tipos e verifica 1+2, 2+1 e 3+0. Os arquivos são gravados em cache isolado; os pacotes do usuário não são alterados. Testes JVM exercitam falha de fonte usando outra implementação do mesmo contrato.

**Exercício:** por que não ler `00-Figurinhas/assets/Pacote 1` diretamente? **Resposta:** essa cópia externa pertence ao usuário e pode ter sido apagada ou alterada. Os assets do APK são a fonte original estável solicitada para preenchimento.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-55_completar-individuais-com-iniciais-temporarias.md)
