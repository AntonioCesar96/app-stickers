# Aula: coleções de figurinhas escolhidas uma a uma

Data: 2026-10-16_16-38 — America/Cuiaba.

## Do objetivo ao modelo

O objetivo era escolher uma figurinha e levá-la ao WhatsApp sem exportar seu pacote inteiro. A integração documentada trabalha com pacotes e exige mínimo de três. A solução implementada aceita escolhas individuais no aplicativo e acumula essas escolhas em coleções exportáveis. Não tenta simular uma API de favoritos que não existe no fluxo utilizado.

Há duas representações importantes: arquivo WebP é o conteúdo da figurinha; pacote é a organização que descreve várias figurinhas, tipo e ícone. Separar conteúdo e organização permite copiar o arquivo sem recodificá-lo. Copiar mantém qualidade, frames e transparência porque os bytes não mudam. Em contrapartida, há consumo adicional de disco.

## O que muda na interface

A prévia anterior era apenas um SimpleDraweeView. Agora ele está dentro de um LinearLayout vertical com um botão abaixo. O RecyclerView continua desenhando as células; o cartão é uma camada sobreposta no FrameLayout. O adapter mantém o carregamento de animações pelo Fresco e controla quando o conjunto aparece.

Antes, calcular a posição com a altura da imagem bastava. Agora a área ocupada inclui imagem, margem, botão e padding. O cálculo usa expandedPreviewContainer.getWidth/getHeight para limitar o cartão à área da lista. Isso evita deixar o botão fora da tela ao selecionar uma célula perto da borda.

Um callback conecta o adapter à Activity. O adapter sabe qual Sticker foi clicado e avisa `onAddSelected.accept(selected)`; a Activity sabe salvar, recarregar e navegar. Esse uso de callback mantém regras de armazenamento fora da camada que recicla células. Não foi criado um framework de eventos nem uma arquitetura nova para o aplicativo todo.

## Escolher não significa mover

`SelectedStickers.add(assets, sourceId, name, animated)` recebe a raiz, pacote, arquivo e tipo. A primeira parte transforma caminhos em canônicos: `..` e ligações são resolvidos antes da comparação. O pai do arquivo precisa ser o pacote, e o pai do pacote precisa ser assets. Isso impede uma solicitação de copiar um arquivo arbitrário fora da área esperada.

Depois confere arquivo existente/não vazio, extensão e tipo do diretório. Essas verificações tratam estado que pode ter mudado desde o clique; não são uma validação completa de WebP. O catálogo já fornece a mídia de entrada, e o validador existente continua conferindo dimensões, duração e outros limites.

## Hash: reconhecer repetição sem depender do nome

SHA-256 transforma os bytes em um resumo de 256 bits. Dois arquivos idênticos geram o mesmo resumo, mesmo que tenham nomes diferentes. A solução calcula o hash da entrada e compara com os arquivos das coleções do mesmo tipo. Assim, copiar novamente a mesma figurinha de outro pacote não cria outra cópia.

Exemplo: pacote A contém 01_riso.webp; pacote B contém 09_copia.webp, com os mesmos bytes. A primeira escolha cria a entrada na coleção. A segunda encontra o mesmo hash e retorna duplicate=true, sem aumentar a contagem nem a versão. Se a figurinha for recodificada, seus bytes e hash podem mudar, mesmo que visualmente pareça a mesma. Não há comparação perceptual de imagens.

Para n bytes de entrada e s bytes somados das coleções examinadas, o custo de leitura é O(n+s). O buffer de 16 KiB é constante, mas a lista de arquivos ocupa memória proporcional à quantidade. Hashes não são mantidos em um banco nesta implementação: isso evita índices obsoletos após edições externas, ao custo de reler arquivos.

## Distribuição entre coleções

O algoritmo examina todos os diretórios gerenciados compatíveis, buscando uma repetição e lembrando a primeira vaga. A verificação de repetição não para quando encontra uma vaga: a cópia pode estar em outro pacote cheio. Só após terminar a busca escolhe onde inserir.

```text
validar origem; calcular hash
destinoComVaga = nenhum
para cada coleção do mesmo tipo:
    se algum arquivo tem o mesmo hash: retornar duplicata
    se tem menos de 30 e ainda não há destino: guardar coleção
se não há destino: preparar e publicar coleção nova
senão: copiar, publicar arquivo e aumentar versão
retornar nome, quantidade e duplicate=false
```

Simulação: a primeira coleção tem 30, a segunda tem 4. Escolher algo novo insere na segunda, totalizando 5. Escolher algo idêntico ao primeiro pacote retorna esse primeiro com contagem 30. Se todas estiverem cheias, o laço de nomes procura um nome livre. Um diretório comum chamado Selecionadas - estaticas não é reutilizado sem o marcador, e a nova coleção recebe sufixo.

## Publicar dados completos

Uma falha no meio da cópia pode produzir bytes incompletos. Para uma coleção existente, copia-se para .tmp, sincroniza-se e só depois renomeia-se para .webp. O scanner reconhece apenas WebP, portanto não considera o temporário uma figurinha. Para uma coleção nova, prepara-se um diretório fora de assets com figurinha, ícone e marcadores; só então o diretório entra em assets por rename.

O `finally` remove temporários em falhas normais. A variável committed indica que cópia e atualização de versão terminaram; se não, remove apenas a cópia recém-criada. A origem nunca é renomeada ou apagada. Essa organização oferece recuperação local de falhas, não uma transação durável com journal: queda abrupta ou outro gravador do aplicativo podem produzir situações que exigem recarga. `synchronized` serializa chamadas ao helper no processo, não bloqueia o mundo externo.

Os marcadores v_N.txt são a convenção existente de image_data_version. Incrementá-los permite ao catálogo anunciar mudança ao WhatsApp. A alteração reutiliza ContentsJsonHelper para reconstruir JSON e atualizar o ContentProvider, respeitando a organização atual do app. Esse helper tem suas próprias limitações de escrita e tratamento de erros; a tarefa não substituiu o catálogo por um banco transacional.

## Rascunhos e barreira de exportação

Não faria sentido exigir três escolhas para salvar a primeira. Por isso um pacote identificado por .selected-stickers aceita mínimo de uma no validador interno. Esse marcador é uma convenção local de funcionalidade, não uma credencial de segurança. O restante das validações continua ativo.

Permitir rascunho no aplicativo não muda o contrato do destinatário. `AddStickerPackActivity.addStickerPackToWhatsApp` conta os WebP e retorna com orientação se houver menos de três. Assim separam-se duas decisões: “posso guardar esta coleção?” e “posso exportá-la agora?”. A confirmação após a escolha só oferece exportação quando a quantidade é suficiente.

## Threads, ciclo de vida e testes

A Activity executa hash/cópia/recarga em uma Thread, pois I/O na thread principal bloquearia animações e toques. `savingSelected` bloqueia repetição enquanto a instância salva. A sincronização do helper e deduplicação protegem a chamada repetida inclusive de outra instância. `runOnUiThread` apresenta o resultado apenas se a tela não acabou. A gravação não vira um serviço em background nem persiste uma confirmação de diálogo após morte de processo.

Os seis testes unitários usam diretórios temporários e bytes pequenos para exercitar lógica de arquivos. Verificam resultado e efeitos, inclusive preservação da origem e reversão após versão no limite. Não provam validade visual desses bytes. O teste instrumentado complementa com um WebP real e interação na tela Android, sem enviar mensagens ou instalar pacotes no WhatsApp.

**Exercício:** por que não usar só o nome da imagem para impedir repetição? **Resposta:** dois pacotes podem ter o mesmo nome para imagens diferentes, ou nomes diferentes para a mesma imagem.

**Exercício:** com 2 estáticas e 2 animadas, já há pacote exportável? **Resposta:** não; cada tipo pertence a uma coleção separada e cada uma ainda precisa de uma escolha.

**Exercício:** uma falha de versão deve apagar a figurinha original? **Resposta:** não. A operação é cópia; a recuperação remove apenas o novo destino que ainda não foi confirmado.

[Registro](../Documentação%20das%20alterações/2026-10-16_16-38_reunir-figurinhas-escolhidas-em-pacotes-selecionadas.md) · [Regras consolidadas](../Documentação%20de%20regras/figurinhas-selecionadas-copia-individual-pacotes-estaticos-animados-whatsapp.md)
