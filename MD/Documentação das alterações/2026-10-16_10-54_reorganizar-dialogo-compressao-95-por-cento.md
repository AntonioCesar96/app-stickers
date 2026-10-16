# Reorganizar diálogo de compressão

## Alterações

- `dialog_compression.xml`: cabeçalho compacto, cartão destacado para tamanho da última tentativa, grade alinhada com os cinco parâmetros, botão principal verde e cancelamento separado. Tipografia explícita e espaçamento regular, sem rótulos herdando tamanhos excessivos do tema.
- `CropVideoActivity`: resumo atual mais curto, separação de tamanho e metadados, remoção de status repetidos, barra oculta em estados terminais, fundo arredondado próprio sem insets e largura real de 95%.
- `CompressionControlsTest`: cenário visual específico que verifica largura, campos e ações visíveis sem precisar esperar por uma compressão longa. Ajustadas as asserções de apresentação do cenário anterior para os novos IDs.
- Atualizadas regras e aulas das classes afetadas. Algoritmo, comandos de reinício e cancelamento permanecem os mesmos.

## Validação

25 testes unitários passaram. Teste instrumentado de apresentação passou em 4,398 segundos no Android USB, com escala de fonte do usuário em 1,25. Captura `app/build/compression-dialog-organized.png` revisada visualmente: cinco campos e ações visíveis, textos alinhados e superfície ocupando 95% da tela. O cenário usa metadados sintéticos e não mede compressão. A primeira tentativa de layout deixou campos abaixo da área visível; a grade foi ajustada e validada novamente.

Build release concluído e APK instalado por atualização, preservando dados e usando o certificado local existente. Build manteve `-x checkRelease` pela restrição do identificador de exemplo. Pacote de testes removido. Documentação da [aula](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_10-54_reorganizar-dialogo-compressao-95-por-cento.md).
