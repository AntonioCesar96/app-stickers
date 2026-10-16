# Aula: sugerir parâmetros com base no excesso de tamanho

Data: 2026-10-16_17-23 — America/Cuiaba.

## Do problema ao desenho

O algoritmo automático pode fazer muitas tentativas. O usuário quer intervir com uma redução maior, mas mantendo a liberdade de revisar os campos antes de reiniciar. Antes havia campos manuais e um diálogo Edit separado; agora o Edit foi retirado e o formulário da compressão ganhou um seletor de intensidade.

Há três ações distintas: escolher a intensidade, receber valores sugeridos e reiniciar o encoder. Apenas a terceira deve cancelar/recomeçar processamento. Essa separação preserva a intenção do usuário: uma sugestão é um rascunho editável, não um comando de execução.

## Evidência: tamanho e parâmetros precisam pertencer à mesma tentativa

CompressionJob registra currentAttempt e lastAttempt. O primeiro descreve o que está rodando; o segundo inclui bytes de uma conversão concluída. Não se deve associar bytes parciais à qualidade atual, pois o arquivo ainda pode crescer ou estar inválido.

applyCompressionSuggestion carrega o job no clique, exige lastAttempt com bytes positivos e lê os parâmetros desse registro. Lê também currentAttempt, quando presente, para não sugerir subir qualidade/FPS/nitidez acima de reduções que o serviço já aplicou. Usa o menor desses parâmetros como base. A velocidade vem da tentativa atual e o esforço de compressão considera o maior esforço já usado. Quando o registro atual não existe, lastAttempt é a referência.

Os inputs digitados não são a evidência do cálculo: ainda não produziram o arquivo medido. Mudar de modo os substitui explicitamente por uma nova sugestão. O usuário pode editar depois. Repetir Sugerir com o mesmo estado produz a mesma proposta, sem reduzir sucessivamente os próprios campos como se fossem novas medições.

## Excesso relativo e intensidade

O alvo é 500×1024 = 512000 bytes. A fração a retirar do arquivo atual é `e = 1 - alvo/bytes`. Um arquivo de 1024000 bytes tem e=0,5: metade do tamanho atual precisa desaparecer. Essa fração não é igual ao percentual acima do alvo, que nesse caso é 100%. A interface mostra o percentual acima do limite; o algoritmo usa a fração do arquivo atual.

Cada modo define um fator: Suave 0,40, Moderado 0,85, Agressivo 1,35. A severidade é `s = min(0,85, e*fator)`. Assim arquivos muito maiores geram propostas mais fortes e o fator diferencia as opções. O teto evita levar tudo diretamente ao mínimo por um tamanho extremo. Em limites/arrendondamentos, alguns parâmetros podem coincidir entre modos; não há garantia de três vetores distintos para qualquer entrada, por exemplo quando qualidade já é zero.

## Distribuir a redução entre parâmetros

Qualidade: subtrai `ceil(2*fator + qualidade*0,60*s)` e limita em zero. ceil arredonda para cima, garantindo um passo discreto mesmo perto do alvo. O termo pequeno por fator diferencia ajustes próximos do limite; o termo proporcional acompanha o excesso e o valor de qualidade disponível.

FPS: subtrai `ceil(fps*0,50*s)`. Se a base tem pelo menos 12, mantém mínimo 12. Se já era menor, não a aumenta nem reduz mais por esta heurística. Isso conserva a preferência de fluidez atual e deixa reduções extremas manuais possíveis no formulário.

Nitidez: calcula `floor(detalhe * sqrt(1 - 0,70*s))`, com mínimo 1. A raiz aparece porque detalhe é o lado de um quadrado intermediário e a área varia com lado². É uma aproximação de distribuição espacial, não uma previsão exata de bytes. O resultado final do app continua 512×512 após ampliação; o parâmetro determina detalhe intermediário.

Compressão: é esforço do encoder de 0 a 6, não qualidade visual. Subtrair esse número poderia diminuir o esforço, contrariando a intenção de compactar. Por isso a sugestão aumenta um nível do maior esforço observado, até 6. Isso pode aumentar tempo por tentativa; não promete economizar CPU. Velocidade permanece a atual para não mudar duração/movimento como efeito secundário da sugestão.

## Simulação com valores concretos

Considere Q75, 30 FPS, C4, detalhe512 e arquivo de 1024000 bytes. Os cálculos produzem aproximadamente:

| Modo | Qualidade | FPS | Compressão | Detalhe |
|---|---:|---:|---:|---:|
| Suave | 65 | 27 | 5 | 474 |
| Moderado | 54 | 23 | 5 | 429 |
| Agressivo | 41 | 19 | 5 | 371 |

São parâmetros propostos, não tamanhos previstos. Uma cena estática e uma cena com ruído podem gerar tamanhos diferentes com o mesmo vetor. No cenário visual Android, 680000 bytes com Q48/12FPS/detalhe512 geram Q35 no modo Agressivo, mantendo os 12 FPS. Esses bytes do teste são sintéticos para testar o formulário, não medição de desempenho.

## Interface e estado

O Spinner tem três entradas e inicia em Moderado. Seu listener compara a posição com suggestionMode antes de aplicar: o callback inicial do Android não deve apagar campos restaurados só porque o componente foi ligado. Ao trocar a opção, aplica. Sugerir oferece uma forma explícita de recalcular a mesma opção após terminar outra tentativa.

suggestionMode e retryDraft são preservados no Bundle. O polling continua atualizando métricas, sem preencher campos a cada 750 ms. A mensagem de sugestão usa o canal persistente de status do formulário para não desaparecer no poll seguinte. A parte central continua rolável; cabeçalho e ações de reinício/cancelamento permanecem fixos.

applyCompressionSuggestion só altera EditTexts e estado de UI. Não chama job.request, CompressionJob.restart ou CompressionService.start. Essas chamadas continuam em retryCompression, depois de ler/validar os campos e do clique explícito. Esse limite é a garantia arquitetural de que selecionar Agressivo não interrompe uma sessão em andamento.

## Casos de borda e testes

Sem bytes válidos: informa para aguardar e mantém os campos. Arquivo que já cabe: não reduz. Qualidade zero/detalhe1/esforço6: respeita os limites. FPS8: não sobe para12. Serviço já em Q20/detalhe180: a sugestão não volta para Q75/512 de uma medição antiga. Um valor manual continua sob controle do usuário até outra ação explícita substituir ou reiniciar.

Os seis testes JVM verificam intensidade ordenada num caso representativo, proporcionalidade ao excesso, limites, tempo preservado, idempotência da mesma evidência e falta de medição. O teste Android usa Activity e diálogo reais, confirma três opções, remoção de Edit e job cancelado permanecendo cancelado após sugerir. Não mede o tamanho final de todas as sugestões: isso dependeria de benchmarks com conteúdo variado.

**Exercício:** por que “50% acima do alvo” não significa cortar 50% do arquivo? **Resposta:** 750 KB está 50% acima de 500 KB, mas precisa perder 250/750, cerca de 33,3% dos bytes atuais.

**Exercício:** selecionar um modo deve cancelar FFmpeg? **Resposta:** não; apenas preenche rascunho. O comando nasce no clique Reiniciar.

**Exercício:** uma versão mais agressiva garante 500 KB? **Resposta:** não. A compressão depende do conteúdo e os parâmetros não têm relação linear com bytes. A proposta é um salto editável para reduzir tentativas pequenas.

[Registro](../Documentação%20das%20alterações/2026-10-16_17-23_sugerir-reducao-de-compressao-por-intensidade.md) · [Regras](../Documentação%20de%20regras/compressao-webp-dialogo-tentativas-parametros-reinicio-cancelamento-background.md)
