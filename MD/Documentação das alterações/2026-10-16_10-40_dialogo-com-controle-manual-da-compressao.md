# Diálogo com controle manual da compressão

## Solicitação e implementação

O usuário precisa avaliar a distância até 500 KB e acelerar a busca escolhendo parâmetros, sem esperar reduções unitárias. Também solicitou informações em um balão do ícone “i” e cancelamento que mantém o diálogo aberto.

- Layout exclusivo `dialog_compression.xml` para vídeo/GIF, preservando o diálogo compartilhado usado por imagens.
- `CropVideoActivity`: tamanho e parâmetros da última tentativa concluída, parâmetros atuais, campos de qualidade/FPS/compressão/velocidade/nitidez, cópia explícita dos valores atuais, reinício, cancelamento e PopupWindow de informações. Rascunho dos campos é salvo na recriação da Activity.
- `CompressionSettings`: validação de intervalos e duração.
- `CompressionJob`: canal persistente de comandos separado do estado e novo UUID para tentar novamente após conclusão/cancelamento/erro.
- `CompressionService`: interrupção da sessão FFmpeg específica com espera pelo callback, reinício da busca com novos parâmetros, prioridade do cancelamento e metadados de tentativas concluídas.
- `AdaptiveWebpSearch`: resolução intermediária inicial configurável sem aumentar nitidez na busca posterior.
- Testes unitários de validação e busca, e teste instrumentado do fluxo completo, incluindo cancelamento com diálogo aberto e nova tentativa.

## Validação

25 testes unitários passaram; builds debug, testes instrumentados e release concluídos. O teste `CompressionControlsTest` passou no celular USB, inclusive após a revisão final do layout (65,519 segundos).

O teste verificou tamanho e configurações reais da última saída, validação de entrada inválida, revisão de comando aplicada, interrupção de uma sessão nativa ativa, geração com qualidade 20/FPS 8/nitidez 128 abaixo de 500 KB, cancelamento mantendo o diálogo aberto e outra geração após cancelar. Também verificou ações com região visível e PopupWindow aberto pelo ícone. Capturas finais revisadas em `app/build/compression-controls-cancelled.png` e `app/build/compression-controls-info.png`.

O cabeçalho e as ações Reiniciar/Cancelar ficam fixos; métricas e formulário rolam. Os botões Fechar e Minimizar são separados do cancelamento. Nenhuma figurinha do usuário foi salva, removida ou modificada pelo teste. O registro pendente foi preservado e seu cancelamento confirmado sem serviço ativo.

Release local assinada com o certificado já utilizado para atualização sem limpar dados; build com `-x checkRelease` pela restrição existente ao identificador de exemplo. APK de testes removido após a validação. Os testes não simulam morte forçada do processo durante cada ponto da gravação dos comandos nem medem qualidade perceptiva; a persistência e a ordem das operações foram revisadas no código.

Veja as [regras consolidadas](../Documentação%20de%20regras/compressao-webp-dialogo-tentativas-parametros-reinicio-cancelamento-background.md) e a [aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_10-40_dialogo-com-controle-manual-da-compressao.md).
