# Selecionar contexto pelo nome dos arquivos

## Problema e fundamento

Arquivos documentais podem crescer continuamente, mas nem todos ajudam na alteração atual. Consultar vários documentos sem relação com a tarefa aumenta o texto processado sem necessariamente melhorar a solução. Esta alteração organiza a seleção antes da leitura do conteúdo.

Ter um arquivo no disco não equivale a carregar seu conteúdo na conversa. A economia desta regra depende de evitar leituras que seriam realizadas. Não há uma quantidade de tokens economizada medida nesta tarefa.

## Nome como indicação de escopo

O nome funciona como informação resumida para decidir se vale a pena abrir o documento. `funcionamento.md` não informa o assunto; `documentacao-do-projeto-regras-de-leitura-escrita-pastas-e-nomes.md` indica claramente que o texto trata das regras documentais. A desvantagem é um caminho mais longo; a vantagem é permitir uma seleção sem abrir o conteúdo.

O nome deve representar assuntos realmente explicados. Acrescentar palavras-chave não cobertas pelo texto produziria falsos sinais de relevância. Quando a alteração envolve outro assunto, como decodificação de vídeo, esse tema de documentação não deve ser lido.

## Fluxo de decisão

1. Identificar o assunto da tarefa.
2. Listar somente os nomes dos documentos temáticos.
3. Selecionar nomes diretamente relacionados ao assunto.
4. Ler somente os selecionados e conferir o comportamento no código pertinente.
5. Se não houver correspondência, investigar os arquivos do projeto sem abrir temas alheios.

Não é necessário adicionar essa lógica ao aplicativo: são instruções de trabalho no `AGENTS.md`. A política também exige excluir histórico e aulas de pesquisas globais, pois uma busca de conteúdo nessas pastas já seria uma consulta, mesmo sem abrir cada arquivo individualmente.

## Ler para contexto e revisar o próprio trabalho

A restrição não elimina a produção de registros e aulas. O agente pode escrever com base no trabalho recém-realizado e revisar os documentos da tarefa atual. Essa exceção permite conferir o resultado sem conceder permissão para usar todo o histórico como contexto. Consultar documentos de tarefas anteriores continua dependendo de solicitação explícita do usuário.

Links preservam a possibilidade de navegação pelo usuário, mas não anulam a política de leitura do agente. Ao renomear o tema nesta tarefa, os dois links históricos conhecidos foram corrigidos usando as referências já disponíveis, sem pesquisar o conteúdo das pastas restritas.

## Exercício comentado e verificação

**Pergunta:** listar nomes e depois pesquisar uma palavra em todos os documentos temáticos respeita a regra?

**Resposta:** Não. A seleção deve ocorrer pelos nomes antes da leitura; buscar no conteúdo de todos os temas contorna essa seleção. Primeiro identifique os nomes pertinentes e limite a leitura a eles.

**Pergunta:** um documento de tema pertinente substitui a conferência do código?

**Resposta:** Não. Ele ajuda a entender as regras registradas, mas pode estar desatualizado. O código e verificações adequadas continuam necessários para confirmar o funcionamento.

`git diff --check` ajuda a detectar problemas de espaços nas alterações acompanhadas pelo Git; não mede economia de tokens nem garante que uma política será seguida em todas as tarefas futuras. Nesta tarefa foram revisadas as instruções e suas exceções.

Veja o [registro desta alteração](../Documentação%20das%20alterações/2026-10-16_09-49_restringir-leituras-e-detalhar-nomes-dos-temas.md).
