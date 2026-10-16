# Reunir figurinhas escolhidas individualmente

Data: 2026-10-16_16-38 — America/Cuiaba.

## Objetivo e comportamento

Ao tocar numa figurinha da tela de detalhes, a prévia ampliada passa a incluir o botão **Adicionar às selecionadas**, imediatamente abaixo da imagem. O botão copia os bytes originais para um pacote separado e mantém o arquivo no pacote de origem.

As coleções são criadas sob demanda: `Selecionadas - estaticas` e `Selecionadas - animadas`. O Android/WhatsApp não permite misturar os dois tipos no mesmo pacote. Uma coleção pode começar com uma ou duas figurinhas no app; a ação de exportação só é oferecida a partir de três. O resultado informa quantidade, faltantes e permite ver o pacote, continuar escolhendo ou adicionar ao WhatsApp quando possível.

Repetições são detectadas pelo SHA-256 dos bytes, independentemente do nome e pacote de origem. O limite é 30 por coleção: ao atingir, outra coleção numerada é criada para novas escolhas. Pacotes comuns que coincidam com o nome proposto são preservados e não são assumidos como coleções.

## Arquivos e motivo

- `SelectedStickers.java`: armazenamento independente da interface, cópia com staging, marcadores de coleção/tipo/versão, duplicatas, capacidade e reversão da nova cópia em erro de versão.
- `StickerPreviewAdapter.java` e `activity_sticker_pack_details.xml`: cartão ampliado com imagem e ação abaixo; posicionamento considera o conjunto inteiro. A ação entrega a figurinha ao callback da tela. Rolagem e toque na imagem fecham a prévia; seleção por toque longo fecha a prévia antes de entrar no modo seleção.
- `StickerPackDetailsActivity.java`: cópia fora da thread principal, atualização do catálogo/provedor, confirmação e navegação. Indica faltantes no cabeçalho de uma coleção incompleta.
- `StickerPackValidator.java`: aceita mínimo de uma figurinha apenas para coleções identificadas pelo marcador interno. As demais verificações permanecem existentes.
- `AddStickerPackActivity.java`: barreira antes de abrir o WhatsApp para qualquer pacote com menos de três arquivos WebP.
- `SelectedStickersTest.java`: seis cenários de arquivos em diretórios temporários.
- `SelectedStickersDeviceTest.java`: teste da prévia real e cópia de WebP em armazenamento isolado.

## Validação

Compilações debug e release passaram. Os 34 testes unitários passaram, incluindo os seis novos testes de arquivos. Os dois testes instrumentados passaram no aparelho (execução final: 3,944 s). A captura da prévia foi revisada visualmente após aguardar o carregamento assíncrono pelo Fresco: imagem e botão aparecem no cartão sem corte na configuração testada. A instalação do APK de testes abriu o Play Protect e foi liberada pelo usuário.

A release foi alinhada, assinada com a chave local existente, verificada e instalada por atualização, preservando dados. O APK de testes foi removido. A geração usou `-x checkRelease` porque a verificação do projeto modelo rejeita o applicationId de exemplo já usado pelo app; identificador e verificação foram preservados. Essa é assinatura local compatível com a instalação atual, não preparação de publicação em loja.

Não foram enviadas mensagens nem confirmada instalação de pacotes no WhatsApp por automação. Os testes Android verificam prévia e cópia em raiz isolada; não automatizam a confirmação completa de salvar uma seleção no catálogo real nem a exportação ao WhatsApp. O comportamento dessas etapas foi revisado no código, mantendo essa limitação explícita.

## Limites

A coleção guarda cópias, portanto ocupa espaço adicional. SHA-256 exige ler as figurinhas existentes das coleções do mesmo tipo. A sincronização impede concorrência entre chamadas deste helper, mas não constitui transação global com os outros editores nem com o JSON do catálogo. A atualização do catálogo reutiliza os helpers existentes. Encerramento abrupto pode deixar staging residual fora de assets; não foi criada recuperação transacional de processo. Remover a cópia não remove a origem, e remover a origem não remove a cópia.

[Aula](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md) · [Regras](../Documentação%20de%20regras/figurinhas-selecionadas-copia-individual-pacotes-estaticos-animados-whatsapp.md)
