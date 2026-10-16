# Reprodução automática das figurinhas

## Objetivo e alterações

- `StickerPackListAdapter.java`: as prévias da listagem principal usam um controller Fresco com reprodução automática de animações.
- `StickerPreviewAdapter.java`: a grade de detalhes também reproduz WebP animado automaticamente, sem exigir abrir a prévia ampliada.
- Nos dois adapters, os controllers são liberados ao reciclar os itens para não manter imagens antigas em memória. A própria opção de reprodução automática do Fresco inicia/paralisa a animação conforme a imagem entra/sai da tela, conforme a [documentação oficial](https://frescolib.org/docs/animations.html).
- Figurinhas estáticas continuam sendo exibidas normalmente; não é criada animação para arquivos que não a possuem.
- Este registro documenta a mudança e sua validação.

## Verificação

- Compilação release com R8 e lint vital aprovada; APK assinado e verificado, instalado com `adb install -r`, preservando os dados.
- Abertura confirmada em `StickerPackListActivity`, sem erros de `EntryActivity` ou `AndroidRuntime` no processo instalado. Capturas da listagem obtidas em instantes diferentes para conferir as prévias animadas e as estáticas.
- Comparação de regiões das capturas 2 e 3 com FFmpeg: a região das prévias animadas mudou de hash, enquanto a região das estáticas manteve o mesmo hash, confirmando movimento nas miniaturas da listagem. Artefatos em `app/build/sticker-autoplay-*.png` (ignorados pelo Git).
- `git diff --check` aprovado. Não foram criados testes unitários para esta alteração pontual de apresentação. A tela de detalhes usa a mesma configuração de autoplay; sua animação não foi medida separadamente no aparelho.
- Nenhum arquivo de figurinha é modificado.
