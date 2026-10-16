# CompressionSuggestion: uma heurística pura para a próxima tentativa

Código: `app/src/main/java/com/example/samplestickerapp/CompressionSuggestion.java`.
Atualização: 2026-10-16_17-23 — America/Cuiaba.

## Responsabilidade e entradas

Esta classe final e visível no pacote não conhece Views, JSON, jobs ou FFmpeg. Seu único método suggest recebe parâmetros da tentativa medida, parâmetros atuais, tamanho concluído e modo. Retorna outro CompressionSettings válido sem alterar as entradas. Essa separação permite testar a decisão matemática na JVM e deixar o gerenciamento da interface na Activity.

LIMIT define 500×1024 bytes, o alvo do app. O enum Mode associa nomes semânticos a fatores numéricos: AGGRESSIVE=1,35, MODERATE=0,85 e GENTLE=0,40. Enum evita números soltos em comparações espalhadas e torna explícito o conjunto de estratégias. A ordem corresponde ao Spinner, configurado pela Activity.

## Condições de parada e base

Bytes não positivos são rejeitados porque não representam um resultado concluído utilizável. Se bytes já está no limite ou abaixo, retorna current sem degradação. A Activity preserva também as edições manuais nesse caso, sem preencher o formulário de novo.

Para reduzir, toma o menor entre measured/current em qualidade, FPS e detalhe. Assim uma medição anterior Q75 não faz sugerir aumentar uma conversão que já está em Q20. Não estima exatamente quantos bytes essa Q20 produzirá: o tamanho pertence ao registro measured, por isso o resultado continua heurístico.

## Fórmulas e significado

`excess = 1 - LIMIT/(double)bytes` calcula a parte dos bytes atuais que precisa desaparecer. O cast evita divisão inteira, que perderia frações. `severity = min(0,85, excess*strength)` define o grau de intervenção, com teto para tamanho extremo.

Qualidade cai por um termo discreto e um termo proporcional à qualidade e severidade, arredondados para cima. FPS cai proporcionalmente, com piso12 quando a base já era pelo menos12; para valores inferiores, o piso é o próprio valor existente. Detalhe usa raiz quadrada de um fator de área, arredondado para baixo e limitado em1. Esses arredondamentos convertem propostas contínuas em parâmetros inteiros aceitos pelo encoder.

Esforço de compressão usa o maior dos dois registros mais um, até6. Reduzir esse parâmetro significaria menos esforço, não necessariamente menos bytes. Velocidade permanece current.speed, evitando acelerar um vídeo como efeito colateral da sugestão. O construtor CompressionSettings valida novamente o vetor final.

Com Q75/FPS30/detalhe512 e tamanho dobrado em relação ao alvo, as propostas de qualidade são65/54/41 para Suave/Moderado/Agressivo. A fórmula distribui perdas entre variáveis, mas não contém um modelo exato do conteúdo visual. Com limites saturados, diferentes modos podem coincidir em algum parâmetro. Não há benchmark que sustente promessa de alcançar500KB em uma rodada.

## Estado, custos e alternativas

A função usa um número fixo de operações, portanto tempo e memória adicionais são O(1). Não lê o arquivo inteiro nem inicia uma sessão. Igual evidência gera igual saída, evitando compor reduções de sugestões que nunca foram convertidas. Seu estado é apenas as constantes; não mantém histórico entre chamadas.

Uma alternativa de subtrair sempre10 pontos ignoraria se o arquivo está2% ou300% acima do alvo. Outra seria prever bytes por regra de três da qualidade, assumindo uma linearidade que WebP não garante. Esta heurística responde ao excesso, respeita limites e deixa o vetor editável. O algoritmo automático de busca permanece separado e só passa a usar os valores após reinício explícito.

## Validação e exercício

CompressionSuggestionTest cobre seis cenários: intensidades num caso representativo, excesso crescente, parâmetros atuais menores, limites extremos, ausência de degradação quando cabe/idempotência e bytes inválidos. O teste Android verifica o preenchimento e a ausência de reinício ao aplicar. Não se testa a qualidade perceptiva nem o tamanho de todas as saídas possíveis.

**Exercício:** por que o método não recebe os EditTexts? **Resposta:** ele precisa de dados numéricos/evidência, não de componentes Android. A Activity traduz estados e mostra a proposta, preservando a testabilidade e a responsabilidade de cada parte.

[Aula da alteração](../Explicação%20de%20conceitos%20por%20alterações/2026-10-16_17-23_sugerir-reducao-de-compressao-por-intensidade.md)
