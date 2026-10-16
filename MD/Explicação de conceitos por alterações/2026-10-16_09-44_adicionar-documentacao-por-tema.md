# Como organizar conhecimento por tema

## Do histórico à consulta do funcionamento

Um histórico responde “o que mudou nesta tarefa?”. Uma referência temática responde “como esta funcionalidade funciona agora?”. Essa diferença importa quando vários pedidos alteram a mesma funcionalidade: consultar apenas o histórico exige reconstruir a sequência de mudanças e decidir quais regras ainda valem.

A alteração no `AGENTS.md`, na seção `Documentação por tema`, exige uma referência consolidada para evitar esse trabalho. Ela complementa as aulas, que explicam o raciocínio de uma implementação, e os registros, que preservam as mudanças e validações de cada tarefa.

## Identidade estável e agrupamento

Um arquivo temático tem identidade ligada ao assunto, não ao momento em que foi criado. Por isso usa um nome como `compressao-de-figurinhas.md` e guarda a data de atualização dentro do texto. Seu caminho pode continuar sendo usado em links mesmo depois de novas mudanças.

A decisão de organização segue um raciocínio simples: identificar o assunto, procurar um documento existente e atualizar esse documento. Somente um assunto distinto justifica outro arquivo. Isso evita um documento por mensagem e reduz a dispersão das regras. Não é um novo algoritmo do aplicativo nem um padrão arquitetural implementado; é uma regra de manutenção documental.

## Atualização e consistência

Imagine, como exemplo hipotético, que uma regra mude de “gerar automaticamente” para “aguardar uma ação”. O registro da tarefa explica essa mudança; o documento do tema precisa apresentar a ação como condição atual para gerar. Acrescentar a regra nova sem remover ou corrigir a antiga criaria contradição.

Links relativos conectam os documentos sem copiar explicações extensas. O leitor pode consultar a regra atual no tema e seguir para uma aula para entender a implementação. A vantagem é reduzir repetições; a responsabilidade adicional é conferir os links e manter os textos coerentes.

## Exercício comentado

**Situação:** três pedidos alteram os parâmetros de compressão e um quarto altera o destino das notificações. Quantos documentos temáticos devem existir?

**Resposta:** a quantidade depende dos assuntos, não do número de pedidos. Os três primeiros podem atualizar o mesmo documento de compressão. O quarto pode atualizar um tema existente de processamento em background ou notificações. Se ambos participarem do fluxo alterado, mais de um tema pode precisar de atualização. O critério é facilitar a consulta sem fragmentar ou misturar assuntos sem relação.

## Verificação e limites

Revisar a estrutura e os links comprova a organização documental, mas não a correção de regras técnicas ainda não estudadas. Nesta tarefa não se produziram descrições de todos os recursos do app nem se alterou seu código. A regra passa a orientar as próximas tarefas e este documento explica seu funcionamento.

Veja o [registro da alteração](../Documentação%20das%20alterações/2026-10-16_09-44_adicionar-documentacao-por-tema.md) e o tema [organização da documentação](../Documentação%20de%20regras/documentacao-do-projeto-regras-de-leitura-escrita-pastas-e-nomes.md).
