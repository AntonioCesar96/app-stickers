# Figurinhas selecionadas: cópia individual e exportação

Atualização: 2026-10-16_17-04 — America/Cuiaba.

## Finalidade

Permitir adicionar escolhas individuais ao WhatsApp sem exportar todo o pacote original. A entrada é o botão **Adicionar**, abaixo da prévia ampliada. Depois de preparar a mídia e o preenchimento, abre diretamente a integração de inclusão no WhatsApp. Não há confirmação intermediária, botão Ver pacote nem pacote de individuais na listagem do app.

Os dados persistidos e o ContentProvider continuam necessários: o WhatsApp lê os metadados e arquivos por essa integração. Ocultar na interface não significa remover os arquivos que sustentam o pacote. A confirmação e aceitação finais são controladas pelo WhatsApp; não há escrita direta no banco dele nem instalação silenciosa.

## Identidade, tipo e capacidade

Os dados internos são diretórios em `00-Figurinhas/assets` com `.selected-stickers`; animadas também têm `animado.txt`. Os identificadores existentes `Selecionadas - estaticas`/`Selecionadas - animadas` e sufixos numéricos foram preservados para não quebrar referências anteriores. O marcador distingue esses dados de um pacote comum e também permite ocultar coleções já criadas nas versões anteriores.

O nome público é `Individuais - estaticas` ou `Individuais - animadas`. Ao lotar, os títulos passam a `Individuais - estaticas +1`, `+2` etc., separadamente por tipo. `SelectedStickers.displayName` converte o sufixo interno 2 em +1, 3 em +2. `ContentsJsonHelper` publica esse nome no JSON; o identificador não muda.

`StickerPackLoader.visiblePacks` filtra apenas a lista usada pelas telas do aplicativo. Assim os individuais não aparecem na tela principal, no seletor de compartilhamento nem nos destinos de movimentação. O ContentProvider mantém os dados completos disponíveis ao WhatsApp. O filtro cria outra lista e não apaga registros ou arquivos.

O helper procura coleções do mesmo tipo em ordem alfabética do nome. Primeiro verifica duplicatas entre escolhas permanentes; se não houver, usa a primeira com menos de 30 arquivos WebP. Sem vaga, cria novo nome com sufixo 2, 3 etc., sem sobrescrever diretórios existentes. A mesma mídia com bytes idênticos já escolhida em qualquer coleção do tipo não é copiada outra vez. Outra codificação visualmente igual, mas com bytes diferentes, conta como uma nova figurinha. Preenchimentos temporários não impedem uma escolha explícita dos mesmos bytes.

O nome interno combina índice numérico e hash SHA-256, mantendo a convenção do leitor do catálogo. O ícone inicial é copiado do pacote de origem. Não é feita geração de nova arte nem recodificação. A animação e transparência do WebP permanecem nos bytes originais.

## Mínimo de três com preenchimento temporário

A cada chamada de adição individual, inclusive duplicata, `SelectedStickers` calcula `faltantes = max(0, 3 - escolhidas)`. Completa apenas essa quantidade com amostras distintas do mesmo tipo, sem repetir bytes de uma escolha permanente. Com uma escolha: duas temporárias; com duas escolhas: uma temporária; com três ou mais: nenhuma temporária. A coleção bem-sucedida tem ao menos três WebP e já pode ser enviada ao WhatsApp.

`InitialStickerTemplates` lê os assets originais do APK: Pacote 1 para animadas e Pacote 2 para estáticas, os mesmos usados por `SplashActivity.criarPastasIniciais()`. Não move nem depende das cópias desses pacotes no armazenamento do usuário; apagar ou modificar os pacotes iniciais não quebra a fonte das amostras.

As cópias temporárias usam o nome reservado `00_selected-filler_<sha256>.webp`. `isFiller` exige padrão e correspondência do hash com os bytes antes de tratar um arquivo como temporário. Escolhas explícitas têm nomes numéricos comuns. Se um temporário for escolhido explicitamente pelo botão Adicionar, ganha uma cópia permanente e deixa de contar como preenchimento. Se alguém modificar externamente seus bytes mantendo o nome, o arquivo é preservado e passa a ser contado como conteúdo permanente, evitando exclusão indevida.

Coleções antigas incompletas são completadas ao receber uma adição, mesmo que duplicada. Não há migração global na Splash nem preenchimento de todos os pacotes ao iniciar. A tolerância anterior do validador a rascunhos continua existindo para leitura de dados antigos; a exportação mantém o bloqueio a menos de três. Limite máximo: 30.

Após preparar, a Activity chama `addStickerPackToWhatsApp(result.identifier, result.name)` na thread principal. Com apenas uma variante do WhatsApp instalada, abre essa variante diretamente; com Consumer e Business instalados, mostra o seletor do Android. Sem destinatário instalado, informa indisponibilidade. Repetir uma escolha também reabre a inclusão do pacote existente sem duplicar a figurinha.

Uma inclusão cancelada no WhatsApp não apaga os dados internos já preparados; eles permitem repetir a tentativa. Atualizações usam a versão do pacote, mas a leitura/atualização do cache no destino pertence ao WhatsApp. Não se promete adicionar aos favoritos nem confirmar automaticamente a tela externa.

## Persistência e processamento

`SelectedStickers.add` verifica caminhos canônicos para garantir que a origem pertence ao pacote informado. Confere existência, extensão, tamanho não nulo e marcador de tipo. A operação é synchronized para serializar chamadas ao próprio helper. A cópia usa buffer de 16 KiB e sincroniza o descritor antes da publicação.

Coleções novas são preparadas num diretório temporário irmão de assets, já com mínimo de três, e publicadas por rename. Em coleção existente, `reconcile` prepara a escolha e o preenchimento desejado fora de assets; publica os novos arquivos antes de retirar temporários excedentes para staging e incrementar a versão. Falhas normais restauram temporários retirados e removem publicações novas. Se a restauração falha, o staging é conservado e seu caminho aparece no erro. Não existe transação global entre filesystem, catálogo JSON e todos os outros gravadores do app.

`StickerPackDetailsActivity` executa cópia/preenchimento fora da thread principal, regenera contents.json, limpa cache do provedor e marca packsChanged para recarregar a lista já filtrada. Abre a inclusão externa apenas se a Activity ainda estiver ativa. Dados concluídos permanecem se a tela for recriada; não há um job para abrir o WhatsApp posteriormente sem novo clique.

## Validação e limites

Os testes unitários cobrem arquivos, preenchimento, nomes públicos e transbordo da 31ª escolha. Os testes Android verificam metadados públicos em raiz isolada, preservação do identificador, filtragem da lista sem mutar os dados completos, amostras dos dois tipos e prévia. Não confirmam instalação dentro do WhatsApp nem garantem o tempo de atualização do cache externo.

Os individuais não possuem uma tela de gerenciamento no fluxo normal do app. O preenchimento automático pertence à ação de adicionar individual, não a toda edição externa de arquivos. A origem é preservada. Após três escolhas nenhuma inicial inserida automaticamente permanece; uma amostra escolhida explicitamente continua sendo permanente.

[Registro](../Documentação%20das%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
