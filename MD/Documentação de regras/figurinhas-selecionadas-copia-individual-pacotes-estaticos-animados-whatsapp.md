# Figurinhas selecionadas: cópia individual e exportação

Atualização: 2026-10-16_16-55 — America/Cuiaba.

## Finalidade

Permitir reunir escolhas de diferentes pacotes sem adicionar todo o pacote original ao WhatsApp. A entrada é o botão **Adicionar**, abaixo da prévia ampliada de uma figurinha na tela de detalhes. O efeito é copiar para uma coleção gerenciada, não mover nem recomprimir.

## Identidade, tipo e capacidade

Uma coleção é um diretório em `00-Figurinhas/assets` com o marcador `.selected-stickers`. O nome inicial é `Selecionadas - estaticas` ou `Selecionadas - animadas`; as animadas também contêm `animado.txt`. O marcador, e não apenas o nome, distingue uma coleção de um pacote comum. O usuário pode ver a coleção na listagem normal e abrir seus detalhes.

O helper procura coleções do mesmo tipo em ordem alfabética do nome. Primeiro verifica duplicatas entre escolhas permanentes; se não houver, usa a primeira com menos de 30 arquivos WebP. Sem vaga, cria novo nome com sufixo 2, 3 etc., sem sobrescrever diretórios existentes. A mesma mídia com bytes idênticos já escolhida em qualquer coleção do tipo não é copiada outra vez. Outra codificação visualmente igual, mas com bytes diferentes, conta como uma nova figurinha. Preenchimentos temporários não impedem uma escolha explícita dos mesmos bytes.

O nome interno combina índice numérico e hash SHA-256, mantendo a convenção do leitor do catálogo. O ícone inicial é copiado do pacote de origem. Não é feita geração de nova arte nem recodificação. A animação e transparência do WebP permanecem nos bytes originais.

## Mínimo de três com preenchimento temporário

A cada chamada de adição individual, inclusive duplicata, `SelectedStickers` calcula `faltantes = max(0, 3 - escolhidas)`. Completa apenas essa quantidade com amostras distintas do mesmo tipo, sem repetir bytes de uma escolha permanente. Com uma escolha: duas temporárias; com duas escolhas: uma temporária; com três ou mais: nenhuma temporária. A coleção bem-sucedida tem ao menos três WebP e já pode ser enviada ao WhatsApp.

`InitialStickerTemplates` lê os assets originais do APK: Pacote 1 para animadas e Pacote 2 para estáticas, os mesmos usados por `SplashActivity.criarPastasIniciais()`. Não move nem depende das cópias desses pacotes no armazenamento do usuário; apagar ou modificar os pacotes iniciais não quebra a fonte das amostras.

As cópias temporárias usam o nome reservado `00_selected-filler_<sha256>.webp`. `isFiller` exige padrão e correspondência do hash com os bytes antes de tratar um arquivo como temporário. Escolhas explícitas têm nomes numéricos comuns. Se um temporário for escolhido explicitamente pelo botão Adicionar, ganha uma cópia permanente e deixa de contar como preenchimento. Se alguém modificar externamente seus bytes mantendo o nome, o arquivo é preservado e passa a ser contado como conteúdo permanente, evitando exclusão indevida.

Coleções antigas incompletas são completadas ao receber uma adição, mesmo que duplicada. Não há migração global na Splash nem preenchimento de todos os pacotes ao iniciar. A tolerância anterior do validador a rascunhos continua existindo para leitura de dados antigos; a exportação mantém o bloqueio a menos de três. Limite máximo: 30.

Após escolher, a confirmação informa destino, quantidade de escolhidas, quantidade de iniciais temporárias e total físico. Oferece **Ver pacote**, **Continuar escolhendo** e **Adicionar ao WhatsApp** quando há três. Explica que as iniciais serão substituídas. Não instala automaticamente nem salva diretamente nos favoritos. A integração continua a operar por pacote e a confirmação final pertence ao WhatsApp.

## Persistência e processamento

`SelectedStickers.add` verifica caminhos canônicos para garantir que a origem pertence ao pacote informado. Confere existência, extensão, tamanho não nulo e marcador de tipo. A operação é synchronized para serializar chamadas ao próprio helper. A cópia usa buffer de 16 KiB e sincroniza o descritor antes da publicação.

Coleções novas são preparadas num diretório temporário irmão de assets, já com mínimo de três, e publicadas por rename. Em coleção existente, `reconcile` prepara a escolha e o preenchimento desejado fora de assets; publica os novos arquivos antes de retirar temporários excedentes para staging e incrementar a versão. Falhas normais restauram temporários retirados e removem publicações novas. Se a restauração falha, o staging é conservado e seu caminho aparece no erro. Não existe transação global entre filesystem, catálogo JSON e todos os outros gravadores do app.

`StickerPackDetailsActivity` executa cópia e recarga fora da thread principal. Em seguida regenera o contents.json, limpa o cache do ContentProvider e marca packsChanged para a tela principal recarregar. O resultado visual só é mostrado se a instância ainda estiver ativa. Se a tela for recriada, uma cópia já concluída continua no armazenamento; a confirmação visual não é persistida como job.

## Validação e limites

Os testes unitários cobrem origem preservada, bytes, versão, duplicação, separação por tipo, capacidade, rejeições, falhas e transição 1+2 → 2+1 → 3+0. Incluem reparação de coleção incompleta em duplicata, promoção de temporário, falha de templates e preservação de temporário modificado. Os testes Android usam um WebP sintético e os assets reais estáticos/animados em raízes isoladas, além da prévia real. Não confirmam exportação dentro do WhatsApp.

As coleções reutilizam ações existentes de exclusão/movimentação; essas ações continuam com as regras anteriores de conservar três na origem. O preenchimento automático pertence à ação de adicionar individual, não a toda edição genérica de arquivos. O helper não substitui a validação de codec; assume uma figurinha já pertencente ao catálogo do app. Os arquivos da origem nunca são apagados por essa ação. Após três escolhas nenhuma inicial inserida automaticamente permanece; uma amostra escolhida explicitamente continua sendo escolha permanente.

[Registro](../Documentação%20das%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
