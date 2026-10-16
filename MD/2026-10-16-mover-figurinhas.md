# Mover figurinhas entre pacotes

## Objetivo

Adicionar a ação Mover ao modo de seleção por toque longo, com painel inferior de 100% da largura da tela e listagem visual dos destinos compatíveis. A largura foi alterada de 95% para 100% conforme a instrução mais recente do usuário.

## Alterações

- `StickerPackDetailsActivity.java`, `StickerPreviewAdapter.java` e `toolbar.xml`: ação de mover ao lado de excluir; título de seleção neutro; ocultação das ações de adicionar enquanto seleciona; restauração da seleção por nome de arquivo após recriar a tela; atualização das quantidades e prévias após mover; voltar encerra a seleção.
- `MoveStickersSheet.java`, `MoveStickersViewModel.java`, `MoveStickerPacksAdapter.java`, `sheet_move_stickers.xml`, `item_move_sticker_pack.xml`, `ic_move_stickers.xml`, `move_button_background.xml` e `strings.xml`: bottom sheet com largura de 100%, altura de 75%, nome, quantidade e até quatro prévias inteiras por pacote. Selecionar destino e confirmar realiza o movimento. Há carregamento, estado vazio, mensagem de erro e prevenção de toque repetido. O ViewModel preserva destino escolhido e operação durante rotação. O botão usa fundo verde e texto branco para evitar o tema branco sobre branco do diálogo; o estado desabilitado é cinza.
- `StickerMove.java`: mantém os arquivos WebP originais, muda apenas sua localização e nome no destino, sem recomprimir. Revalida os limites de 3 na origem e 30 no destino, rejeita pacotes incompatíveis, evita colisões e caminhos fora dos pacotes, e tenta desfazer o lote em caso de falha. Incrementa os marcadores de versão dos dois pacotes.
- `ContentsJsonHelper.java` e `StickerPackListActivity.java`: sinalizam e recarregam ambos os pacotes ao retornar à listagem; atualização do adapter existente evita acumular divisores/listeners em cada atualização.
- `StickerMoveTest.java`: movimentação múltipla, preservação de bytes e nomes existentes, filtros, capacidade/mínimo, entrada ausente/caminho indevido e rollback.
- `MoveStickersTest.java` e `app/build.gradle`: teste no aparelho com pacotes temporários próprios, seleção real, cancelamento, rotação, largura, prévias, seleção múltipla e movimento único até atingir 30 no destino. Dependência `androidx.test:core:1.4.0` somente nos testes para `ActivityScenario`.
- Este registro acompanha as alterações e os resultados da tarefa.

## Regras

- Animadas são movidas para pacotes animados; estáticas, para estáticos. O pacote atual e destinos sem espaço para toda a seleção não aparecem no painel.
- A origem precisa manter pelo menos 3 figurinhas, conforme a regra já aplicada na exclusão e na validação dos pacotes.
- O movimento usa renomeação dentro do mesmo armazenamento. Uma falha tenta restaurar todos os arquivos movidos; se a restauração também falhar, a mensagem informa que parte está no destino, sem apagar esses arquivos. Não há garantia de transação atômica de múltiplos arquivos diante de desligamento abrupto do aparelho.

## Verificação

- 19 testes unitários aprovados, incluindo 5 testes novos de movimentação.
- Teste instrumentado de movimento múltiplo e único, filtros, cancelamento, recriação, preservação de bytes e capacidade de 30 aprovado. Revisão visual corrigiu o enquadramento das prévias, o contraste do ícone e do botão. Execução final com largura de 100% aprovada (`OK (1 test)`, 5,901 s), usando o ícone real da toolbar e conferindo a confirmação dentro dos limites físicos da tela.
- Captura real do painel revisada em `app/build/move-sheet.png`: largura completa, nome, quantidade, prévias inteiras, destino selecionado e botão de confirmação visível.
- Compilações debug/release com R8 e lint vital aprovadas. Release local assinada com o certificado já usado, verificada e instalada com `adb install -r`, preservando os dados. O APK auxiliar de testes foi removido.
- Após o carregamento, `StickerPackListActivity` em primeiro plano e nenhum erro de `EntryActivity` ou `AndroidRuntime` no novo processo. Catálogo final com 2 pacotes e 8 figurinhas; todos os pacotes temporários dos testes foram removidos. Os testes movimentaram somente arquivos de seus próprios pacotes temporários.
- `git diff --check` aprovado. Testes instrumentados executados na variante debug; a release foi verificada na abertura da listagem.
- A versão Material Components 1.4.0 já instalada oferece `BottomSheetBehavior.setMaxWidth`, configurado antes de exibir o painel, conforme o [código oficial dessa versão](https://github.com/material-components/material-components-android/blob/1.4.0/lib/java/com/google/android/material/bottomsheet/BottomSheetBehavior.java).
