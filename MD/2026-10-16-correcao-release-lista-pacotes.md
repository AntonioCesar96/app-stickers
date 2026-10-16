# Correção do erro After last row na release

## Diagnóstico

O logcat mostrou `CursorIndexOutOfBoundsException: After last row` no carregamento de pacotes da `EntryActivity`, às 07:19:45 de 16/10/2026. A consulta ao provider retornava zero linhas. O `contents.json` gerado pela release continha campos anotados, mas faltavam `identifier`, `name`, `publisher` e `stickers` dos pacotes: o R8 havia removido os campos sem nomes JSON explícitos usados apenas por reflexão do Gson. O parser rejeitava o catálogo, e o carregador lia um cursor vazio sem conferir o retorno de `moveToFirst`.

A validação anterior de instalação release conferiu Activity aberta e ausência de crash fatal, mas não conferiu se o carregamento assíncrono dos pacotes havia terminado com sucesso.

## Alterações

- `StickerPackModel.java`: nomes `@SerializedName` explícitos para identificador, nome, publicador e lista de figurinhas, preservados pelas regras de consumo do Gson no R8.
- `StickerModel.java`: nome JSON explícito para emojis, pelo mesmo motivo.
- `StickerPackLoader.java`: percorre apenas linhas existentes com `moveToNext` e fecha o cursor de metadados com `try`/`with-resources`, inclusive quando a consulta está vazia ou há falha de leitura.
- `ContentsSerializationTest.java`: regressão do contrato JSON e verificação de nomes explícitos em todos os campos persistidos dos três modelos.
- Este registro documenta diagnóstico, alterações e verificações.

## Validação

- `:app:testDebugUnitTest`: 14 testes aprovados, incluindo 2 novos testes do contrato de serialização.
- `:app:assembleRelease -x checkRelease`: compilação otimizada com R8 e lint vital aprovada.
- APK release alinhado, assinatura verificada e instalação com `adb install -r` aprovada, usando o certificado local existente e preservando os dados.
- Catálogo gerado pela própria release no aparelho conferido: 2 pacotes e 6 figurinhas; os campos `identifier`, `name`, `publisher`, `stickers` e `emojis` estão presentes.
- Após o carregamento assíncrono, a tela em primeiro plano é `StickerPackListActivity`, confirmada por `dumpsys window`. O logcat filtrado pelo novo processo não apresentou erros de `EntryActivity` nem `AndroidRuntime`.
- `git diff --check` sem erros de whitespace.
- Logs e cópia do catálogo anterior estão em `app/build/`, ignorado pelo Git. Os arquivos de figurinhas existentes no celular não foram modificados.
