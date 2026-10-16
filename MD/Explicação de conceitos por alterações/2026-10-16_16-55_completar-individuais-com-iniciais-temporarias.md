# Aula: preenchimento temporário e reconciliação de coleções

Data: 2026-10-16_16-55 — America/Cuiaba.

## O problema e a mudança de modelo

Antes, uma coleção podia guardar uma ou duas escolhas, mas não estava pronta para o WhatsApp, que exige mínimo de três no fluxo usado. O pedido introduziu preenchimento: reutilizar as amostras iniciais, retirando-as conforme entram escolhas reais. Não basta verificar a quantidade total: com uma escolha e duas amostras, o total já é três, mas o próximo clique precisa trocar uma amostra por uma escolha.

Por isso agora distinguimos **conteúdo permanente**, escolhido pelo usuário, e **conteúdo temporário**, inserido pelo algoritmo. O tipo não depende da aparência: até uma amostra inicial pode ser permanente se escolhida explicitamente. É uma diferença de origem e intenção, não de imagem.

## A regra matemática e seus estados

Se c é a quantidade permanente, a quantidade temporária desejada é `max(0, 3-c)`. max impede resultado negativo quando c passa de três. O total final é c + temporárias, respeitando o limite de 30 já existente.

| Escolhidas | Temporárias desejadas | Total |
|---|---|---|
| 1 | 2 | 3 |
| 2 | 1 | 3 |
| 3 | 0 | 3 |
| 4 | 0 | 4 |

Essa regra é recalculada em toda tentativa, inclusive duplicata. Uma coleção antiga com uma escolha e nenhum temporário será reparada quando se tentar adicionar aquela mesma figurinha. A deduplicação impede nova escolha repetida, mas não impede corrigir o preenchimento. A decisão de dados duplicados e a decisão de coleção completa são responsabilidades distintas.

## Fonte estável das amostras

SplashActivity.criarPastasIniciais copia Pacote 1 e Pacote 2 dos assets para a memória compartilhada. As cópias externas podem mudar ou desaparecer. InitialStickerTemplates acessa diretamente AssetManager: animadas em Pacote 1, estáticas em Pacote 2. Esses arquivos são empacotados no APK e não são alterados pelo usuário ao gerenciar pacotes.

SelectedStickers não recebe AssetManager diretamente: depende da interface Templates, com names e open. A interface expressa apenas o que o algoritmo precisa, permitindo usar assets no Android e bytes pequenos em testes JVM. Isso é inversão de dependência em escala local, sem criar uma arquitetura nova para o app inteiro. InputStream fornece bytes por leitura incremental; não exige conhecer um caminho físico dentro do APK.

## Identificar o que pode ser removido

Os temporários têm nome reservado `00_selected-filler_<sha256>.webp`. O prefixo numérico mantém compatibilidade com o scanner existente de pacotes. O hash é um resumo dos bytes. `isFiller` exige o padrão e recalcula o resumo, comparando-o com o nome. Não decide remover por ser igual visualmente a um coração ou por pertencer ao pacote inicial.

Isso resolve dois casos importantes. Primeiro, uma inicial escolhida explicitamente é copiada sob o nome normal de escolha; a cópia de preenchimento pode ser retirada, mas a permanente fica. Segundo, se o usuário modificar externamente os bytes de um temporário sem ajustar o nome, a verificação falha: o arquivo é preservado como permanente em vez de ser apagado como se ainda fosse nossa amostra original.

O nome reservado é uma convenção de propriedade local, não autenticação criptográfica. A aplicação presume que outros componentes não inventem arquivos com esse padrão para conteúdo permanente. Os hashes evitam confundir bytes modificados e permitem reconhecer duplicatas de conteúdo.

## Construir o preenchimento desejado

prepareFillers recebe as escolhas permanentes finais, incluindo a entrada nova quando houver. Calcula faltantes. Se for zero, não abre templates: nenhuma amostra é necessária após a terceira escolha. Caso contrário, calcula os hashes das escolhas e percorre os nomes de amostras ordenados.

Cada candidato é copiado para template.tmp no staging. Seu hash precisa ser novo em relação às escolhas e a outros candidatos. Se for repetido, é descartado; se for distinto, renomeia para o nome reservado e registra como desejado. Para ao reunir a quantidade necessária. Sem amostras distintas suficientes, falha antes de publicar os novos arquivos.

Exemplo: a escolha real é a primeira amostra do pacote estático. O algoritmo não usa outra cópia dela como preenchimento; pula o hash igual e usa as duas restantes. Na segunda escolha, selecionando a segunda amostra, resta apenas a terceira temporária. Na terceira, todas são escolhas reais e não há mais temporários. Os testes Android reproduzem isso com os arquivos efetivos do APK nos dois tipos.

## Reconciliar em vez de diminuir um contador

reconcile compara o que existe com o que deveria existir. Essa abordagem é mais robusta que apagar sempre “a primeira amostra” ou decrementar um contador salvo: tolera rascunhos antigos, duplicatas e amostras removidas externamente. O filesystem é a fonte do estado atual.

```text
separar permanentes e temporários existentes
preparar a nova escolha, se houver
calcular e preparar temporários desejados
publicar arquivos novos
retirar temporários que deixaram de ser desejados
incrementar versão se algo mudou
se falhar: tentar restaurar retirados e apagar novos
```

As substituições são publicadas antes de remover o excedente. Isso evita que uma operação bem-sucedida termine com menos de três. O leitor pode observar brevemente um total maior durante a troca; não existe uma troca atômica de todo o pacote para outros processos. O catálogo é reconstruído depois pela Activity.

As listas published e removed guardam o que precisa ser desfeito. Os temporários retirados ficam no staging, não são destruídos imediatamente. Se bumpVersion falhar, os arquivos voltam aos caminhos anteriores e as publicações novas são apagadas. Se essa restauração também falhar, o staging é conservado e seu caminho aparece no erro para preservar os dados recuperáveis. Isso trata exceções normais; não é um journal com recuperação automática após morte do processo.

## Contagens e interface

Result agora informa count, selectedCount e fillerCount. A Activity mostra as três informações: por exemplo, 1 escolhida, 2 iniciais temporárias, 3/30 no pacote. Assim o usuário entende por que há três itens e o que vai desaparecer. Adicionar ao WhatsApp já pode ser oferecido, mas a instalação ainda depende da confirmação e validação do destinatário.

A cópia continua em thread de trabalho e o diálogo continua na thread principal. O mínimo de três da exportação não foi removido: a nova estratégia satisfaz esse mínimo. A tolerância de leitura a rascunhos legados também permanece, permitindo alcançá-los e completá-los na próxima tentativa.

## Custos, alternativas e validação

Hashing e cópia são lineares no número de bytes lidos; buffers usam memória constante. Ler amostras do APK acrescenta I/O apenas quando faltam itens. Uma alternativa seria duplicar a mesma escolha três vezes, mas isso produziria cópias artificiais da escolha em vez de usar as amostras solicitadas. Outra seria retirar amostras pela posição: reordenar arquivos ou inserir uma escolha idêntica tornaria essa política insegura. O estado desejado com identificação explícita evita esses problemas.

Os testes JVM simulam templates pequenos e verificam quantidades, duplicatas, reversão e preservação de arquivos alterados. Os testes Android usam assets estáticos/animados reais em cache isolado, demonstrando 1+2 → 2+1 → 3+0 sem modificar os pacotes do usuário. Não confirmam uma exportação dentro do WhatsApp. O conjunto fornece evidência sobre o algoritmo e integração com assets, sem prometer aceitação por toda versão do destinatário.

**Exercício:** uma coleção tem duas escolhas, mas perdeu seu temporário. O usuário clica numa escolha repetida. O que deve acontecer? **Resposta:** não cria outra escolha, mas insere uma amostra temporária, totalizando três.

**Exercício:** por que uma amostra escolhida manualmente não pode ser removida só por ter os mesmos bytes de uma inicial? **Resposta:** seu papel agora é permanente; origem visual não substitui a intenção explícita.

[Registro](../Documentação%20das%20alterações/2026-10-16_16-55_completar-individuais-com-iniciais-temporarias.md) · [Regras](../Documentação%20de%20regras/figurinhas-selecionadas-copia-individual-pacotes-estaticos-animados-whatsapp.md)
