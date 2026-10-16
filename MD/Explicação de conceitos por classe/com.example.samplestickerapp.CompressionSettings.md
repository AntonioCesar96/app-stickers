# CompressionSettings: validar antes de interromper

Última atualização: 2026-10-16_10-40 (America/Cuiaba).

Código: `app/src/main/java/com/example/samplestickerapp/CompressionSettings.java`.

## Responsabilidade e fundamentos

Esta classe representa um ponto inicial válido da compressão. Um objeto reúne qualidade, FPS, esforço de compressão, velocidade e detalhe espacial. O construtor é a fronteira de validação: ou cria um conjunto válido, ou lança `IllegalArgumentException` explicando o problema. Isso impede enviar ao encoder um pedido inválido e interromper trabalho útil sem necessidade.

Os campos são `final`: depois da construção, as referências aos valores não mudam. Essa imutabilidade simplifica transportar os parâmetros entre leitura da interface e criação do comando. Ela não persiste os dados nem agenda conversões; essas responsabilidades pertencem a `CompressionJob` e `CompressionService`.

## Campos e regras

Qualidade varia de 0 a 100; FPS, de 1 a 60; compressão, de 0 a 6; nitidez, de 1 a 512. Velocidade deve ser finita e positiva. `Double.isFinite` é necessário porque NaN e infinito podem passar por comparações incompletas: validar apenas `speed > 0` não exclui infinito positivo.

Nitidez não é a resolução final do arquivo: controla uma redução intermediária de detalhes, seguida de ampliação para 512×512. Compressão é esforço do encoder e não equivale a qualidade visual. FPS solicitado também pode ser limitado pela taxa da fonte no serviço.

## Duração e cálculo

`validateDuration(sourceSeconds)` calcula `sourceSeconds / speed`. Se o resultado for inválido, não positivo ou maior que 10 segundos, rejeita o conjunto. A duração da fonte só fica conhecida depois da leitura da mídia; por isso não pertence ao construtor.

Exemplo: fonte de 6 segundos e velocidade 0,5 resultam em 12 segundos. O conjunto pode ter números individualmente válidos e ainda ser inadequado para a figurinha. Já velocidade 2 produz 3 segundos. Essa distinção entre validade de campos e validade combinada é importante para regras que relacionam parâmetros.

## Colaborações e alternativas

A Activity converte os textos em números e cria o objeto. O job o transforma em JSON para persistir a intenção. O serviço reconstrói e valida antes de usar. Validar só na UI não protegeria chamadas de outros pontos; validar só no serviço descobriria erros depois de interromper a tentativa anterior.

A classe não depende do Android nem de JSON, permitindo testes unitários diretos. Transformá-la em um gerenciador de arquivos misturaria regras numéricas com persistência e tornaria testes mais difíceis.

## Verificação e exercício

`CompressionSettingsTest` cobre entradas fora do domínio, NaN, velocidade zero e duração resultante. Esses testes demonstram rejeição das combinações exercitadas, não a qualidade visual de uma conversão.

**Exercício:** fonte de 8 segundos a 0,8× é permitida? Sim: 8 / 0,8 = 10 segundos, exatamente o limite. Uma velocidade ligeiramente menor pode exceder esse limite e deve ser validada novamente.
