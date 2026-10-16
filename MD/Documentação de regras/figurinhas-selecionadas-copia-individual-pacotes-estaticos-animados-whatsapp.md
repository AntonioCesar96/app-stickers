# Figurinhas selecionadas: cópia individual e exportação

Atualização: 2026-10-16_16-46 — America/Cuiaba.

## Finalidade

Permitir reunir escolhas de diferentes pacotes sem adicionar todo o pacote original ao WhatsApp. A entrada é o botão **Adicionar**, abaixo da prévia ampliada de uma figurinha na tela de detalhes. O efeito é copiar para uma coleção gerenciada, não mover nem recomprimir.

## Identidade, tipo e capacidade

Uma coleção é um diretório em `00-Figurinhas/assets` com o marcador `.selected-stickers`. O nome inicial é `Selecionadas - estaticas` ou `Selecionadas - animadas`; as animadas também contêm `animado.txt`. O marcador, e não apenas o nome, distingue uma coleção de um pacote comum. O usuário pode ver a coleção na listagem normal e abrir seus detalhes.

O helper procura coleções do mesmo tipo em ordem alfabética do nome. Primeiro verifica duplicatas em todas; se não houver, usa a primeira com menos de 30 arquivos WebP. Sem vaga, cria novo nome com sufixo 2, 3 etc., sem sobrescrever diretórios existentes. A mesma mídia com bytes idênticos já selecionada em qualquer coleção do tipo não é copiada outra vez. Outra codificação visualmente igual, mas com bytes diferentes, conta como uma nova figurinha.

O nome interno combina índice numérico e hash SHA-256, mantendo a convenção do leitor do catálogo. O ícone inicial é copiado do pacote de origem. Não é feita geração de nova arte nem recodificação. A animação e transparência do WebP permanecem nos bytes originais.

## Rascunho e WhatsApp

Uma coleção com uma ou duas figurinhas é um rascunho válido para navegação no aplicativo. O validador permite mínimo de uma apenas com o marcador; pacotes comuns mantêm mínimo de três. A exportação em `AddStickerPackActivity` sempre exige ao menos três arquivos WebP antes de chamar a integração do WhatsApp. Limite máximo: 30.

Após escolher, a confirmação informa o destino e quantidade. Oferece **Ver pacote** e **Continuar escolhendo**; com pelo menos três, também **Adicionar ao WhatsApp**. Não instala automaticamente nem salva diretamente nos favoritos. A integração continua a operar por pacote e a confirmação final pertence ao WhatsApp.

## Persistência e processamento

`SelectedStickers.add` verifica caminhos canônicos para garantir que a origem pertence ao pacote informado. Confere existência, extensão, tamanho não nulo e marcador de tipo. A operação é synchronized para serializar chamadas ao próprio helper. A cópia usa buffer de 16 KiB e sincroniza o descritor antes da publicação.

Coleções novas são preparadas num diretório temporário irmão de assets e publicadas por rename, evitando que o scanner encontre um pacote pela metade. Em coleção existente, um arquivo .tmp é copiado e renomeado para WebP; depois a versão é incrementada. Se a atualização da versão falha, a nova cópia é removida. Não existe transação global entre filesystem, catálogo JSON e todos os outros gravadores do app.

`StickerPackDetailsActivity` executa cópia e recarga fora da thread principal. Em seguida regenera o contents.json, limpa o cache do ContentProvider e marca packsChanged para a tela principal recarregar. O resultado visual só é mostrado se a instância ainda estiver ativa. Se a tela for recriada, uma cópia já concluída continua no armazenamento; a confirmação visual não é persistida como job.

## Validação e limites

Os testes unitários cobrem origem preservada, bytes copiados, versão, duplicação entre nomes/pacotes, separação por tipo, colisão de nomes, transbordo de capacidade, rejeições e limpeza em falha. O teste Android usa um WebP sintético e a tela de detalhes real sem clicar na exportação ao WhatsApp. O resultado final de exportação depende da instalação e validação do WhatsApp.

As coleções reutilizam ações existentes de exclusão/movimentação; essas ações continuam com as regras anteriores de conservar três na origem, mesmo que uma coleção ainda possa existir como rascunho. A tarefa não redesenhou essas regras. O helper não substitui a validação de codec; assume uma figurinha já pertencente ao catálogo do app. Os arquivos da origem nunca são apagados por essa ação.

[Registro](../Documentação%20das%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
