# StickerPackValidator: validade local e contrato de exportação

Código: `app/src/main/java/com/example/samplestickerapp/StickerPackValidator.java`.
Atualização: 2026-10-16_16-38 — America/Cuiaba.

## Fundamentos e responsabilidade

Validação verifica se dados obedecem aos limites antes de serem utilizados. Esta classe tem constantes e métodos estáticos, sem estado de instância. Usa Context para carregar bytes pelo ContentProvider, APIs Android para texto/URLs/ícones e WebPImage do Fresco para inspecionar figurinhas.

As constantes codificam o contrato usado pelo app: 512×512, máximo de 100 KiB para estáticas e 500 KiB para animadas, até 30 por pacote, até três emojis, duração animada até 10 s e frame mínimo de 8 ms. O ícone tem limite de 50 KiB e dimensões entre 24 e 512 nesta implementação. Não confundir esses limites internos com a garantia de aceitação por qualquer versão do WhatsApp.

## Percurso de verifyStickerPackValidity

Primeiro confere identifier, publisher, name e trayImageFile, incluindo existência textual e comprimentos. checkStringValidity limita o identificador a caracteres aceitos e rejeita `..`. Campos opcionais de sites são verificados quando presentes; links de lojas também precisam do domínio esperado. Email usa Patterns.EMAIL_ADDRESS.

Carrega o ícone por StickerPackLoader.fetchStickerAsset, verifica bytes e decodifica Bitmap para medir dimensões. Então verifica quantidade de figurinhas e percorre cada Sticker por validateSticker. Não há execução paralela nesta classe: quem a chama decide a thread. Carregar todos os bytes de cada arquivo usa memória proporcional ao arquivo em validação, além dos buffers de decodificação.

A mudança desta tarefa está na quantidade mínima: um diretório com `.selected-stickers` aceita uma; pacotes comuns continuam exigindo STICKER_SIZE_MIN=3. O máximo continua 30 e nenhuma validação de imagem é desativada. É uma regra de rascunho local. AddStickerPackActivity mantém o mínimo de três para exportar, separando “coleção guardada no app” de “pacote pronto para o WhatsApp”.

## Validar cada imagem

validateSticker verifica quantidade de emojis, nome do arquivo e texto de acessibilidade. isInvalidAccessibilityText usa limites diferentes para estático (125) e animado (255), aceitando null. validateStickerFile lê bytes, aplica limite de tamanho e cria WebPImage. Mede altura/largura e, se animado, exige mais de um frame, frames com pelo menos 8 ms e duração total até 10.000 ms. Num pacote estático, mais de um frame é erro.

checkFrameDurationsForAnimatedSticker percorre todas as durações, custo O(f) para f frames. Um frame de 5 ms reprova mesmo se a média for boa. Um WebP 512×512 com 20 frames de 100 ms soma 2 s e passa esses limites de tempo; ainda precisa respeitar os outros requisitos.

Erros são representados por IllegalStateException com contexto do pacote/arquivo. A implementação existente remove a figurinha em alguns erros de tamanho/dimensões/animação por ContentsJsonHelper antes de lançar a exceção; portanto essa validação não é inteiramente sem efeitos. A tarefa não alterou esse comportamento. IOException e erro de interpretação do WebP são convertidos para falhas de validade com causa.

## URLs, limites e exercícios

isValidWebsiteUrl primeiro tenta construir URL e depois restringe a HTTP/HTTPS. isURLInCorrectDomain compara host com a loja esperada. Esses testes validam estrutura/domínio, não disponibilidade de rede nem conteúdo da página. O marcador da coleção também não autentica origem: é apenas uma convenção local do armazenamento.

Os testes de armazenamento demonstram que o helper cria o marcador e preserva bytes. O teste Android usa WebP real; a validação completa do WhatsApp permanece externa. Para testar rascunhos manualmente, abra uma coleção com uma imagem válida: ela deve ser navegável, mas o envio deve pedir completar três.

**Exercício:** ao permitir uma figurinha no rascunho, também se permite WebP de 700 KiB? **Resposta:** não. Apenas o mínimo da quantidade mudou para coleções gerenciadas; o limite de bytes continua igual.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
