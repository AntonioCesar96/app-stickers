# Nomear uma pasta de acordo com sua finalidade

## Escopo e significado do nome

A pasta de aulas desta tarefa foi renomeada para indicar sua unidade de organização: a alteração realizada. Uma aula por alteração ensina o raciocínio daquela tarefa; uma aula por classe ensina o funcionamento consolidado da classe afetada. Acrescentar “por alterações” torna essa diferença visível sem abrir os documentos.

## Renomear e manter referências

Renomear uma pasta preserva seus arquivos, mas altera os caminhos usados para chegar até eles. Links relativos dependem da localização do destino. Por isso os links conhecidos que apontavam para a pasta antiga também precisam mudar. Links dessas aulas para outras pastas irmãs continuam com a mesma quantidade de níveis, pois a profundidade da pasta não mudou.

A migração de referências é uma operação de manutenção, não uma consulta ao conteúdo histórico para contexto. Nesta tarefa foram usados os caminhos já conhecidos, sem pesquisa global nas aulas ou nos registros.

## Exemplo e verificação

Um link antes direcionado a `../Explicação%20de%20conceitos/arquivo.md` passa a usar `../Explicação%20de%20conceitos%20por%20alterações/arquivo.md`. Os espaços são representados por `%20` no endereço do link. O nome do arquivo não precisa mudar, porque esta solicitação trata da pasta.

**Pergunta:** a renomeação libera a leitura automática das aulas?

**Resposta:** Não. O `AGENTS.md` foi atualizado para aplicar ao novo caminho a mesma restrição anterior. Mudar o nome não altera a política de consulta.

Conferir a nova pasta verifica a mudança no sistema de arquivos. `git diff --check` ajuda a encontrar problemas de espaços, mas não prova sozinho que todos os links possíveis estão corretos. Foram atualizadas as referências conhecidas desta sequência de tarefas.

Veja o [registro desta alteração](../Documentação%20das%20alterações/2026-10-16_09-59_renomear-aulas-por-alteracoes.md).
