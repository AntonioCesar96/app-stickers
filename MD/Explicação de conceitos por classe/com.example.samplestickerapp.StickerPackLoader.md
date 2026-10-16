# StickerPackLoader: ler o provedor e construir a lista visual

Código: `app/src/main/java/com/example/samplestickerapp/StickerPackLoader.java`.
Atualização: 2026-10-16_17-04 — America/Cuiaba.

## Responsabilidade e fundamentos

ContentProvider expõe dados por URIs. Um Cursor é uma tabela de resultados cujo ponteiro precisa avançar antes de ler uma linha. Esta classe estática, visível no pacote, transforma essas linhas em StickerPack/Sticker para as telas do app. Não desenha Views nem grava o catálogo. Context fornece ContentResolver para consultas; as constantes de coluna vêm do contrato de StickerContentProvider.

## Fluxo principal

fetchStickerPacks consulta AUTHORITY_URI e falha se não houver Cursor. Em try-with-resources, fetchFromContentProvider percorre `while (cursor.moveToNext())`, lê campos por nome e constrói os modelos. A consulta é fechada inclusive em erro. Depois visiblePacks cria uma nova ArrayList excluindo diretórios com .selected-stickers. Essa é uma política da UI; o provider conserva todas as entradas para o WhatsApp.

Os identificadores visíveis são checados por HashSet para evitar duplicações. O código mantém o erro existente quando a lista fica vazia. Para cada pacote visível, getStickersForPack carrega figurinhas, define a lista e chama StickerPackValidator. A filtragem anterior à leitura de bytes evita processar individuais só para uma listagem que não deve exibi-los.

O filtro é compartilhado por tela principal, seleção de destino do compartilhamento e movimentação. Não precisa espalhar uma condição de ocultação por cada Activity. Não equivale a autorização de acesso ou privacidade: o marcador é uma convenção local, os arquivos continuam acessíveis ao provider e no armazenamento usado pelo app.

## Montagem de metadados e imagens

fetchFromContentProvider lê identificador, título, editor, ícone, links, versão e flags de animação/cache. getColumnIndexOrThrow detecta colunas ausentes em vez de interpretar uma posição arbitrária. A construção do objeto não valida por si só todos os arquivos.

fetchFromContentProviderForStickers consulta a URI stickers/identifier. Quando há linhas, moveToFirst e percorre com do/while. Lê nome, emojis separados por vírgula e texto de acessibilidade. getStickersForPack então chama fetchStickerAsset para cada arquivo, rejeita bytes vazios e registra tamanho para cálculo do pacote. Erros de I/O ou caminho tornam-se IllegalStateException com contexto.

fetchStickerAsset abre um InputStream pelo ContentResolver e acumula bytes em ByteArrayOutputStream com blocos de 16 KiB. É adequado para arquivos pequenos sujeitos aos limites de sticker, mas usa memória proporcional ao tamanho final. Não carrega um vídeo original inteiro nem faz decodificação por FFmpeg.

getStickerListUri e getStickerAssetUri usam Uri.Builder com authority do BuildConfig e segmentos separados. appendPath evita montar caminhos por concatenação manual. Esses endereços também são usados pelo Fresco para mostrar figurinhas.

## Custos, estado e erros

A classe não mantém cache próprio; o provider pode ter cache, invalidado por ContentsJsonHelper após alterações. visiblePacks faz O(p) verificações de marcador e aloca O(p) referências no pior caso. Carregar as figurinhas custa proporcionalmente aos bytes dos pacotes visíveis, além da validação/decodificação. O chamador deve executar essa leitura fora da thread principal quando ela puder demorar.

Cursor é recurso fechado após a leitura. Ausência de dados, arquivo inexistente, metadado repetido e validação inválida são erros distintos propagados para o chamador. Filtrar individuais não os apaga nem valida para o WhatsApp: esse destinatário lê diretamente do provider e aplica suas próprias exigências.

## Validação e exercício

O teste Android de visibilidade monta metadados em raiz temporária e verifica exclusão dos internos sem alterar a lista completa. Os outros testes abrem a prévia de um pacote normal usando o caminho real. Não certificam todos os estados possíveis do provider nem uma lista sem nenhum pacote normal.

**Exercício:** por que filtrar aqui, e não em StickerContentProvider? **Resposta:** o objetivo é ocultar apenas das telas locais; o WhatsApp continua precisando dos dados completos do provider.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_17-04_adicionar-individuais-diretamente-no-whatsapp.md)
