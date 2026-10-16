# Documentação do projeto: regras de leitura, escrita, pastas e nomes

Última atualização: 2026-10-16_10-03 (America/Cuiaba).

## Finalidade e regras

A documentação do projeto permite consultar o histórico das mudanças, aprender como uma solução foi construída e entender o funcionamento atual de cada assunto. Essas necessidades têm unidades de organização diferentes:

| Pasta dentro de MD | Unidade de organização | Conteúdo e regra de atualização |
| --- | --- | --- |
| Documentação das alterações | Tarefa realizada | O que mudou, motivo, validações e limitações. Nome com data e hora. |
| Explicação de conceitos por alterações | Aula relacionada à alteração | Fundamentos, lógica, algoritmos, padrões e arquitetura efetivamente usados, desenvolvidos passo a passo. Nome com data e hora. |
| Explicação de conceitos por classe | Classe afetada | Aula consolidada da classe inteira, com conceitos, algoritmos, lógica, padrões e arquitetura pertinentes. Nome totalmente qualificado da classe; atualização dentro do texto. |
| Documentação de regras | Assunto ou funcionalidade | Regras e funcionamento atual consolidados. Nome estável, sem data; atualização dentro do texto. |

As instruções gerais estão em `C:\Users\Dell\.codex\AGENTS.md`, arquivo global deste perfil do Codex. O [AGENTS.md local](../../AGENTS.md) mantém uma referência e fica reservado para regras específicas deste projeto. A documentação continua sendo produzida na pasta `MD/` do projeto trabalhado, não na pasta de configuração global.

Registros e aulas usam `AAAA-MM-DD_HH-mm_descricao.md`. Documentos temáticos usam nomes específicos, como `compressao-de-figurinhas-animadas-webp-limite-500kb-qualidade-fps.md`, desde que esses assuntos sejam cobertos pelo conteúdo. Os horários seguem America/Cuiaba.

O alcance global depende do perfil utilizado: outro computador ou outro `CODEX_HOME` precisa da própria configuração. Instruções de projeto podem complementar ou sobrepor as globais. A documentação oficial orienta iniciar uma nova execução ou sessão para reconstruir a cadeia de instruções: [instruções com AGENTS.md](https://developers.openai.com/codex/guides/agents-md).

## Regras de leitura e economia de contexto

O conteúdo dos registros de alterações e das aulas, inclusive aulas por classe, não deve ser consultado para obter contexto sem pedido explícito do usuário. Links para essas pastas não concedem permissão de leitura. A escrita continua obrigatória: os documentos da tarefa atual podem ser revisados para concluir o próprio trabalho, sem consultar registros de tarefas anteriores. Uma aula existente da classe afetada pode ser lida pontualmente para a sua atualização, sem consultar outras aulas para contexto.

Para os temas, primeiro liste apenas os nomes. Leia somente arquivos cujo nome corresponda diretamente ao assunto da alteração. Não abra todos os temas, não use busca de conteúdo para decidir sua relevância e não inclua as pastas restritas em pesquisas globais. Se nenhum nome corresponder, examine o código pertinente.

Os nomes dos temas precisam informar funcionalidade e escopo. Um nome genérico obriga a abrir o arquivo para descobrir seu assunto, frustrando a seleção por nome. Renomeie documentos quando necessário e corrija referências conhecidas, sem pesquisar o conteúdo das pastas restritas para encontrar links.

## Como atualizar um tema

A pasta dos temas se chama `MD/Documentação de regras/`. A organização por assunto e a seleção pelo nome continuam iguais.

Ao concluir uma alteração, identifique os assuntos afetados e procure documentos existentes nesta pasta. Atualize as regras e o fluxo descritos nesses arquivos. Se o assunto ainda não estiver documentado, crie um arquivo com escopo claro. Uma tarefa que altera tanto compressão quanto notificações pode afetar dois documentos; dez pedidos sobre compressão podem atualizar um único documento.

O documento temático precisa responder como o recurso funciona hoje: o que recebe, o que produz, quais regras segue, quais componentes participam e como reage a exceções. Exemplos devem representar o comportamento implementado. Ideias futuras devem ser identificadas como propostas, sem parecer funcionalidades disponíveis.

Quando uma regra mudar, reescreva a explicação vigente. Por exemplo, se o limite de um recurso mudar, não deixe dois limites contraditórios em parágrafos diferentes. O histórico da mudança pertence aos registros por tarefa; o tema apresenta a regra atual e pode apontar para o histórico por um link.

## Limites e verificação

Ao afetar uma classe, produza também sua aula em `MD/Explicação de conceitos por classe/`. O nome deve corresponder à identidade completa da classe, como `com.example.samplestickerapp.CropVideoActivity.md`. A aula explica toda a classe atual e suas colaborações, não somente a mudança da tarefa. Não é necessário criar aulas de classes em tarefas exclusivamente documentais.

Criar esta estrutura não documenta automaticamente todos os recursos existentes. Os temas devem ser criados e mantidos conforme forem envolvidos nas alterações, após conferir os arquivos e comportamentos pertinentes. Nesta tarefa, somente o tema da organização documental foi consolidado.

Na revisão, confira se as regras descritas correspondem à implementação, se os links resolvem e se a data de atualização está correta. Uma compilação bem-sucedida não valida o conteúdo da documentação; a conferência textual e a comparação com a solução são necessárias.
