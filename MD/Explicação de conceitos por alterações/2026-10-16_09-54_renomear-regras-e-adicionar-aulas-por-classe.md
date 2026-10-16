# Entender a alteração e entender a classe inteira

## Por que manter uma aula por classe

Uma aula sobre uma tarefa explica o problema e a solução daquela mudança. Uma aula por classe explica uma unidade de código que continua existindo depois da tarefa: suas responsabilidades, dados, métodos e relações. Várias tarefas podem mudar partes diferentes da mesma classe. O documento por classe deve consolidar seu funcionamento atual para não exigir que o leitor reconstrua várias aulas históricas.

Uma classe agrupa estado e comportamento. Para ensiná-la, não basta listar métodos: é preciso mostrar quem cria seus objetos, o que cada campo representa, quais operações modificam os dados e quais outras partes do programa participam. Em uma Activity Android, por exemplo, o ciclo de vida pode explicar por que determinado callback é usado; em uma classe de algoritmo, entradas, decisões e condições de parada podem ser os conceitos centrais. Esses exemplos não significam que tais classes foram alteradas nesta tarefa.

## Identidade e atualização

O nome totalmente qualificado combina pacote e nome da classe. Duas classes podem ter o mesmo nome simples em pacotes diferentes, portanto usar a identidade completa evita colisões entre documentos. A aula usa um caminho estável e guarda a última atualização no texto, em vez de criar outro documento a cada pedido.

Uma atualização deve conferir a classe atual e corrigir explicações que ficaram obsoletas. Se um método passou a delegar uma responsabilidade, a aula precisa explicar a colaboração nova, sem continuar atribuindo todo o trabalho ao método antigo. Trechos reais e simulações ajudam a conectar conceitos abstratos ao fluxo executado.

## Relação com as regras e com a política de leitura

A pasta renomeada para `Documentação de regras` continua agrupando assuntos e funcionalidades. Uma regra pode envolver diversas classes, enquanto uma classe pode participar de vários fluxos. As duas perspectivas são úteis: regras explicam o funcionamento do recurso; aulas por classe ensinam como cada unidade participa da implementação.

Escrever documentos não implica lê-los todos em tarefas futuras. A nova pasta segue a restrição de consulta das aulas. Para atualizar uma aula da classe afetada, há uma exceção pontual de leitura; ela não libera abrir aulas de outras classes para contexto. A investigação da implementação continua baseada no código pertinente.

## Exercício comentado

**Pergunta:** se uma tarefa altera três métodos de uma classe já documentada, são necessárias três aulas novas?

**Resposta:** Não. Atualize a aula consolidada dessa classe, explicando os métodos no contexto das responsabilidades e dos demais conceitos necessários para entendê-la. A aula da tarefa registra o raciocínio específico da alteração.

**Pergunta:** renomear uma pasta de documentação exige criar uma aula sobre uma classe do app?

**Resposta:** Não. Nenhuma classe foi afetada. Nesta tarefa foram criadas instruções e a pasta para as futuras aulas; inventar uma classe afetada confundiria documentação com implementação.

## Validação

A renomeação preservou o arquivo existente e as referências conhecidas foram ajustadas. A revisão de caminhos e `git diff --check` verificam aspectos documentais, sem comprovar comportamentos do aplicativo. Não houve teste ou alteração de classes nesta tarefa.

Consulte o [registro da alteração](../Documentação%20das%20alterações/2026-10-16_09-54_renomear-regras-e-adicionar-aulas-por-classe.md).
