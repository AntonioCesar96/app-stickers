# Adicionar individuais diretamente pelo fluxo do WhatsApp

Data: 2026-10-16_17-04 — America/Cuiaba.

## Problema e resultado

O comportamento anterior criava uma coleção visível no app e abria uma confirmação com Ver pacote/Adicionar ao WhatsApp. O usuário esclareceu que o clique Adicionar deve abrir diretamente a inclusão no WhatsApp, sem pacote de individuais na listagem local.

Removida essa etapa intermediária. Após preparar os bytes e o mínimo de três, `StickerPackDetailsActivity` chama a integração do WhatsApp com o identificador e nome público. O filtro de `StickerPackLoader` exclui os dados de individuais das telas/seletoras locais, mantendo o ContentProvider completo para o destinatário.

`SelectedStickers.Result` inclui nome público. `displayName` apresenta Individuais por tipo e +1/+2 no transbordo, mantendo identificadores internos anteriores. `ContentsJsonHelper` publica os mesmos títulos no catálogo e permite testar a construção dos modelos em raiz isolada. O scanner agora trata ausência de marcador de versão como versão inicial, sem acessar um array vazio.

`AddStickerPackActivity` abre diretamente a única variante instalada; com Consumer e Business, conserva a escolha do destinatário. Corrigida a condição anterior sempre verdadeira na seleção de variante. O bloqueio de mínimo de três e a confirmação externa continuam.

Os arquivos locais de suporte continuam necessários ao contrato oficial do WhatsApp; não são apagados nem renomeados. As coleções antigas com marcador também deixam de aparecer no app. Permanecem as regras de duas/uma/nenhuma inicial temporária, deduplicação, origem preservada e máximo de 30 por tipo.

## Validação

Compilações debug e release passaram. Os 39 testes unitários passaram, incluindo nomes e 31ª escolha. Os quatro testes Android passaram no celular em 4,134 s: prévia, preenchimento estático/animado, cópia e catálogo com filtro de visibilidade. Os testes usam raízes isoladas para os dados adicionais e não alteram os pacotes do usuário. Não houve envio de mensagens nem confirmação automatizada de instalação dentro do WhatsApp; a chamada direta à integração foi revisada no código.

A release foi alinhada, assinada com a chave local existente, verificada e instalada com atualização preservando os dados. O APK de testes foi removido. A geração usou `-x checkRelease` porque a verificação preexistente do projeto modelo rejeita o applicationId atual; não foram alterados identificador ou verificação. A assinatura é local, não preparação para publicação em loja.

## Limites

O app abre o fluxo oficial, não grava no banco privado do WhatsApp. Aceitação final, confirmação e atualização do cache pertencem ao destinatário. Cancelar a inclusão não remove os dados já preparados; novo clique permite repetir. Não há migração destrutiva nem alteração dos identificadores dos pacotes anteriores.

[Aula](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_17-04_adicionar-individuais-diretamente-no-whatsapp.md) · [Regras](../Documentação%20de%20regras/figurinhas-selecionadas-copia-individual-pacotes-estaticos-animados-whatsapp.md)
