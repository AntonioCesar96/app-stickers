# Compartilhamento Android: arquivos, pacotes e editores

Atualização: 2026-10-16_11-46 — America/Cuiaba.

## Contrato de entrada

O Android encontra `ShareMediaActivity` pelo filtro ACTION_SEND, categoria DEFAULT e tipos image/* ou video/*. Ela é exportada porque o remetente pertence a outro aplicativo. Isso torna a validação dos dados obrigatória: o filtro ajuda o seletor, mas não impede chamadas explícitas inválidas.

`SharedMediaImport.receive` exige ACTION_SEND e URI content. Prefere EXTRA_STREAM; se estiver ausente, aceita uma URI em ClipData com um único item. O MIME do provedor tem prioridade quando identifica imagem/vídeo; o MIME da Intent é uma alternativa. Texto, caminhos e links HTTP são recusados. Não há suporte a lote.

O nome de exibição é apenas informativo, nunca um caminho de saída. A importação usa UUID em files/shared_imports e copia em blocos de 64 KiB. Arquivos vazios, falhas de leitura e cancelamento interrompem a operação; o arquivo parcial é removido. GIF87a/GIF89a identifica GIF mesmo com nome externo incorreto. Outros formatos dependem da decodificação existente; declarar MIME não garante conteúdo válido.

## Escolha e encaminhamento

`ShareMediaActivity.refreshPacks` consulta o catálogo com `StickerPackLoader`. Fotos comuns usam pacotes estáticos; vídeos e GIFs usam animados. O pacote precisa ter menos de 30 figurinhas. O seletor reutiliza `ContatoSpinnerAdapter`, com nome e imagem do pacote.

Sem permissão para o catálogo, a tela oferece a solicitação existente. Sem pacote compatível, permite abrir o gerenciamento e orienta voltar à entrada de compartilhamento. As ações permanecem desativadas até haver mídia importada e destino disponível.

Fotos vão a `CropImageActivity` com file_path e sticker_pack. Vídeos e GIFs passam por `FileExplorerHelper.extracted`, escolhendo `CustomVideoRangeActivity` para Aparar ou `CropVideoActivity` para Cortar. As regras anteriores do helper continuam válidas, incluindo preparação de GIF e encaminhamento de vídeos longos para aparar. A entrada não inicia uma nova estratégia de compressão.

## Estado e concorrência

A cópia e a consulta do catálogo usam um executor de uma thread, mantendo a interface responsiva. Mudanças visuais retornam à thread principal e ignoram Activities encerradas. Bundle conserva caminho privado, tipo, nome e identificador do pacote. Após recriação, o arquivo privado existente é reutilizado; não se pressupõe acesso contínuo à URI original.

`StickerApplication` não faz recuperação automática de uma compressão antiga sobre essa tela. Ao sair sem encaminhar ao editor, a cópia é apagada, exceto se for fonte de um job pendente. Arquivos encaminhados são conservados para não quebrar editores e serviços. A retenção não é uma garantia de limpeza automática posterior.

## Como verificar

`ShareMediaTest` verifica os MIME registrados e rejeição de entrada inválida; publica arquivos próprios no MediaStore, compartilha, abre os editores e remove as amostras. O cenário de vídeo apaga o original antes da recriação para provar o uso da cópia privada. O teste GIF cobre a assinatura e o fallback ClipData, sem testar todo o encoder. Esses testes não garantem o comportamento de cada provedor de terceiros nem suporte a todo codec declarado como video/*.

[Registro da implementação](../Documentação%20das%20alterações/2026-10-16_11-46_receber-compartilhamento-de-midias-do-android.md)
