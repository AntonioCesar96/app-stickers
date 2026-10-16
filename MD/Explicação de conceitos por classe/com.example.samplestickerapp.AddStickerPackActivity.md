# AddStickerPackActivity: a fronteira entre o app e o WhatsApp

Código: `app/src/main/java/com/example/samplestickerapp/AddStickerPackActivity.java`.
Atualização: 2026-10-16_16-38 — America/Cuiaba.

## Responsabilidade e herança

A classe abstrata herda BaseActivity e oferece uma operação protegida para telas que adicionam pacotes ao WhatsApp. Não desenha o catálogo nem edita mídia. Centralizar a integração evita que cada tela monte extras diferentes para a mesma API. ADD_PACK=200 identifica o resultado de Activity; TAG identifica os logs.

## Entrada e preparação

`addStickerPackToWhatsApp(identifier, stickerPackName)` agora começa contando arquivos .webp no diretório do pacote. Ausência de leitura ou menos de três mostra orientação e retorna antes de abrir o destinatário. Esse é o bloqueio de exportação dos rascunhos: permitir uma coleção com uma figurinha dentro do app não autoriza exportar um pacote incompleto. O teste é de quantidade física; não substitui validação de codec ou confirmação do WhatsApp.

Depois, ContentsJsonHelper.updatePackAndContentProvider atualiza versão/catálogo e invalida cache do provedor. A integração requer metadados e bytes acessíveis pelo ContentProvider. A Activity não envia todos os WebP como anexos de uma conversa: o WhatsApp lê o pacote identificado pelo provedor.

## Encontrar o destinatário e abrir Intent

WhitelistCheck verifica instalação das variantes Consumer e Business e consulta se o pacote já foi adicionado. O código existente oferece chooser quando não está adicionado em nenhuma variante; no outro ramo prioriza Consumer. Há uma condição preexistente `x || !x` nesse ramo, logicamente sempre verdadeira. Esta tarefa não corrigiu a escolha entre variantes, portanto não se deve descrever esse caminho como uma política nova e perfeita de prioridade.

`createIntentToAddStickerPack` monta a ação com.whatsapp.intent.action.ENABLE_STICKER_PACK e os extras de identificador, authority e nome. Authority identifica o ContentProvider do app; identifier identifica o pacote dentro dele. Esses dois valores juntos permitem localizar os dados. `launchIntentToAddPackToSpecificPackage` fixa o destino, enquanto `launchIntentToAddPackToChooser` deixa o Android mostrar opções. Ambos usam startActivityForResult com ADD_PACK e tratam ActivityNotFoundException com mensagem.

## Resultado e mensagens

`onActivityResult` reconhece ADD_PACK. Quando o WhatsApp cancela com validation_error, registra o erro; no debug também abre MessageDialogFragment para diagnóstico. Cancelamento sem dados abre StickerPackNotAddedMessageFragment. Isso não confirma exportação com base apenas em o Intent ter sido iniciado: abrir uma tela e ser aceito pelo destinatário são eventos diferentes.

A classe interna StickerPackNotAddedMessageFragment constrói um AlertDialog com orientação, botão de fechar e atalho para atualizar o WhatsApp. launchWhatsAppPlayStorePage verifica quais variantes estão instaladas para escolher a URL. launchPlayStoreWithUri usa ACTION_VIEW; ausência de aplicativo capaz de abrir a URL também é tratada.

DialogFragment é um componente gerenciado pelo FragmentManager, diferente de uma referência solta a Dialog. Ele permite ao framework reconstruir a mensagem no ciclo de vida. A classe não cria executor nem faz compressão; os métodos de entrada são chamados pelas telas do app.

## Limites e exercício

O mínimo de três é aplicado a todos os chamadores dessa classe. A contagem não impede que outro gravador mude o pacote depois dela; o WhatsApp ainda faz suas próprias verificações. A atualização usa os helpers atuais do projeto, sem nova transação global. Os testes desta tarefa não confirmam pacotes no WhatsApp nem enviam mensagens.

**Exercício:** por que bloquear antes de construir o Intent? **Resposta:** evita abrir um fluxo que já sabemos incompleto e fornece orientação específica, sem depender de uma mensagem de erro externa.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md)
