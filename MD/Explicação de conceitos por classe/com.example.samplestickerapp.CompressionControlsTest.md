# CompressionControlsTest: integrar diálogo, serviço e FFmpeg

Última atualização: 2026-10-16_11-04 (America/Cuiaba).

Código: `app/src/androidTest/java/com/example/samplestickerapp/CompressionControlsTest.java`.

## O que a instrumentação acrescenta

`nativeEncoderPublishesMeasuredIntermediatePercentage` gera um vídeo de teste, inicia o serviço real e aguarda um percentual estritamente entre 0 e 99, depois cancela de forma coordenada. Isso verifica o caminho nativo de produção do progresso, sem precisar concluir um encode longo. O cenário visual também prepara 42% sintéticos e verifica barra determinada e texto iguais, capturando a apresentação. Os 42% da captura não são uma amostra da conversão nativa: são uma verificação separada da interface.

`organizedDialogUses95PercentWidthAndKeepsFieldsVisible` acrescenta um cenário exclusivamente visual: gera um pequeno preview, prepara metadados sintéticos de tarefa cancelada e abre o diálogo. Mede o decor em 95% da largura da tela e verifica região global visível para os cinco campos e as ações. Captura `compression-dialog-organized.png` para revisão. Os 664,1 KB mostrados nesse cenário são dados de apresentação, não resultado de um novo benchmark de conversão.

Um teste instrumentado roda no Android e pode interagir com Activities e o codec nativo. Aqui ele verifica o contrato entre formulário, comando persistido, interrupção e resultado. Testes unitários não mostram se um botão realmente mantém o diálogo aberto ou se o FFmpeg interrompido terminou antes da nova saída.

Os campos `instrumentation` e `context` fornecem acesso à aplicação de teste e à aplicação alvo. A fixture é gerada no cache com `testsrc2`, evitando depender de um vídeo pessoal ou de uma rede disponível.

## Espera por condição e thread principal

`waitFor` consulta o registro por ID até encontrar estado e metadados esperados, com prazo máximo. Falha técnica publicada como error interrompe a espera com o JSON no diagnóstico. Isso reduz a dependência de sleeps fixos para conversões de duração variável.

`values` preenche os campos do diálogo dentro de `runOnMainSync`, pois Views devem ser modificadas na thread da interface. O teste não depende de injeção de toque pelo adb, que pode ser bloqueada pelo dispositivo. Usa os listeners reais por `performClick`.

## Fluxo exercitado

O cenário espera uma primeira tentativa real exceder 500 KB, confirma que a UI mostra tamanho e configurações e verifica rejeição de qualidade 101. Depois pede qualidade 20, FPS 8 e detalhe 128 durante a busca. Confere revisão aplicada, interrupção de sessão ativa, parâmetros do arquivo concluído e igualdade entre bytes registrados e tamanho real do arquivo.

Em seguida reinicia a partir de done com parâmetros mais pesados e cancela. O estado deve ser cancelled, a recuperação automática deve estar desabilitada e o diálogo deve continuar visível. As ações fixas precisam ter região global visível, não apenas existir na hierarquia.

O teste captura a tela, abre o PopupWindow pelo ícone e confirma sua exibição. Por fim, reinicia depois de cancelar e espera um novo resultado aceito. Uma nova identidade em reinícios terminais é verificada para evitar reuso de lifecycle antigo.

## Preservação e limpeza

Antes de começar, verifica que nenhum serviço de compressão do usuário está ativo. Um registro pendente sem serviço pode ser preservado temporariamente enquanto o teste usa suas próprias tarefas. IDs, preferências de retorno e arquivo de fixture são restaurados ou removidos em finally.

O teste não salva suas saídas no pacote do usuário. Cancelar serviço e consumir somente os próprios IDs evita misturar validação com publicação de figurinhas reais.

## Evidência, limites e exercício

As capturas permitem avaliar layout, mas uma captura isolada não demonstra todas as dimensões de tela ou acessibilidade. O contador de interrupções e os metadados demonstram o fluxo exercitado, sem prometer tempo fixo para cancelamento. A qualidade visual do vídeo não é comparada por métrica perceptiva.

**Exercício:** por que comparar bytes registrados com `result.length()`? Porque uma UI poderia mostrar um tamanho antigo ou inventado apesar de a conversão produzir um arquivo diferente. Cruzar estado e artefato confere que a telemetria pertence ao resultado observado.
