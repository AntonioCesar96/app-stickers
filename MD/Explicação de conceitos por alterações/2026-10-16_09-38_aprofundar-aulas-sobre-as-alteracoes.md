# Da descrição de uma mudança ao entendimento da solução

## 1. O problema que esta alteração resolve

Um registro como “a conversão passou a funcionar em background” informa o resultado, mas não ensina como ele foi construído. Para compreender a implementação, é necessário entender quem executa o trabalho, quem mostra o progresso, como os dados são preservados e o que acontece quando uma tela é destruída.

A mudança desta tarefa é documental: o `AGENTS.md` agora exige esse tipo de explicação aprofundada para cada alteração. Não foi criado um novo algoritmo nem alterada a arquitetura do aplicativo nesta tarefa. Os exemplos abaixo mostram como ensinar uma implementação, sem afirmar que ela foi modificada aqui.

## 2. Conceito, lógica, algoritmo, padrão e arquitetura

Esses termos descrevem aspectos diferentes de uma solução e devem ser conectados a exemplos reais.

| Aspecto | O que descreve | Exemplo relacionado ao projeto |
| --- | --- | --- |
| Conceito | Uma ideia necessária para entender o comportamento | Estado persistente: dados gravados que continuam disponíveis após o processo terminar. |
| Lógica | Condições e regras que determinam decisões | Uma tarefa com erro não deve forçar a abertura do editor. |
| Algoritmo | Uma sequência definida de passos para produzir um resultado | Testar parâmetros de compressão, medir a saída e escolher a próxima tentativa. |
| Padrão de projeto | Uma organização recorrente para um problema conhecido, com responsabilidades e consequências | Deve ser identificado somente quando a implementação realmente o utiliza; ter um callback, sozinho, não justifica atribuir um padrão específico à solução inteira. |
| Arquitetura | A divisão das responsabilidades e a relação entre as partes | `CropVideoActivity` apresenta a interface; `CompressionService` executa a conversão; `CompressionJob` guarda dados da tarefa. |

Uma aula precisa explicar por que essas divisões existem. Por exemplo, se um trabalho depende da existência de uma tela, destruir essa tela pode interrompê-lo. Separar execução e apresentação permite tratar seus ciclos de vida de maneira diferente. Isso não garante, por si só, sobrevivência a qualquer encerramento: é necessário estudar também persistência e recuperação.

## 3. Como estudar um algoritmo passo a passo

Considere uma etapa de compressão que mantém os FPS e testa a qualidade de um ponto em um ponto. Antes de discutir o código, identifique:

- Entrada: vídeo selecionado, parâmetros atuais e limite de bytes.
- Operação: converter o vídeo e medir o arquivo gerado.
- Decisão: aceitar a saída ou escolher parâmetros menores.
- Saída: arquivo aceito ou falha depois de esgotar os limites.

Uma simulação ilustrativa ajuda a separar qualidade configurada de tamanho observado:

| Tentativa | Qualidade | Tamanho hipotético | Decisão |
| --- | --- | --- | --- |
| 1 | 55 | 530000 bytes | Ainda excede o limite de 512000 bytes. |
| 2 | 54 | 520000 bytes | Continuar. |
| 3 | 53 | 505000 bytes | Aceitar e parar. |

Esses tamanhos são exemplos didáticos, não resultados medidos. A aula de uma implementação concreta deve ligar cada decisão aos métodos reais, mostrar como a próxima tentativa é calculada e explicar o que acontece se nenhuma saída couber.

O custo também importa: se cada conversão levar dez segundos, três conversões podem consumir cerca de trinta segundos, além dos demais custos. Um algoritmo com muitas tentativas pode ser simples de escrever e caro para executar. Discutir alternativas exige analisar tanto qualidade quanto tempo, em vez de apenas contar linhas de código.

## 4. Do requisito à instrução verificável

Antes desta tarefa, o `AGENTS.md` solicitava explicar o significado e a aplicação dos conceitos. A regra era útil, mas ainda permitia uma resposta parecida com um glossário. Agora a seção `Explicação de conceitos` descreve o percurso esperado: problema, fundamentos, raciocínio, implementação, alternativas e validação.

Isso é uma especificação do trabalho documental. Uma instrução como “explique bem” é subjetiva; pedir uma simulação com entradas e estados intermediários torna mais claro o que o documento deve oferecer. Ainda assim, cumprir tópicos não garante uma boa aula: os parágrafos precisam conectar causa, decisão e consequência, com exemplos proporcionais ao problema.

Não existe obrigação de forçar todos os assuntos em toda tarefa. Nesta alteração de Markdown, por exemplo, não faria sentido inventar uma análise de concorrência ou um novo padrão arquitetural. Profundidade significa explicar completamente o que é pertinente.

## 5. Evidência e limites da verificação

Uma aula deve distinguir observação, hipótese e escolha. “O log contém timestamps negativos” é uma observação; “esses timestamps explicam o efeito visual” pode exigir evidência adicional; “vamos recodificar o preview” é uma escolha cuja eficácia deve ser validada.

Nesta tarefa, a revisão do `AGENTS.md` verifica se as exigências estão escritas. `git diff --check` procura problemas de espaços nas alterações acompanhadas pelo Git; ele não avalia qualidade didática nem comprova comportamentos do aplicativo. Por isso não se afirma que testes do app foram executados para esta mudança documental.

## 6. Exercícios com respostas comentadas

**Pergunta:** “O serviço continua rodando quando a tela fecha” é uma explicação completa de arquitetura?

**Resposta:** Não. Falta explicar como a tela se relaciona com o serviço, onde o estado fica armazenado, como o progresso chega à interface e quais encerramentos são suportados. A frase descreve um resultado, não o funcionamento.

**Pergunta:** Medir uma saída de 505000 bytes demonstra que qualquer vídeo caberá com a mesma qualidade?

**Resposta:** Não. O resultado pertence àquela entrada e àqueles parâmetros. Duração, movimento e detalhes do vídeo podem mudar o tamanho final. Uma aula deve explicitar esse limite para evitar uma generalização incorreta.

**Pergunta:** Uma tarefa que só altera documentação precisa inventar um algoritmo para cumprir a regra?

**Resposta:** Não. A instrução exige ensinar os assuntos efetivamente envolvidos. Nesta tarefa, os assuntos são a especificação das instruções, a organização da explicação e a interpretação das validações.

Consulte o [registro desta alteração](../Documentação%20das%20alterações/2026-10-16_09-38_aprofundar-aulas-sobre-as-alteracoes.md).
