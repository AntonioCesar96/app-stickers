# Instruções do projeto

## Leitura seletiva para economizar contexto

- Não leia nem pesquise o conteúdo de `MD/Documentação das alterações/`, `MD/Explicação de conceitos por alterações/` ou `MD/Explicação de conceitos por classe/` para obter contexto, salvo quando o usuário solicitar explicitamente a consulta. Não siga links para essas pastas automaticamente.
- A restrição de consulta não remove a obrigação de escrever a documentação. Crie os registros e aulas da tarefa com base no trabalho realizado. É permitido ler somente os arquivos criados ou atualizados na tarefa atual para revisá-los e concluí-los; isso não autoriza consultar documentos de tarefas anteriores.
- Em `MD/Documentação de regras/`, liste primeiro somente os nomes dos arquivos. Leia um documento apenas se seu nome indicar relação direta com o assunto que será alterado. Não leia todos os temas preventivamente nem abra um arquivo apenas para descobrir se é relevante.
- Se nenhum nome corresponder ao assunto, investigue o código e os arquivos pertinentes à tarefa, sem consultar temas alheios. Não faça buscas globais de conteúdo que incluam as pastas restritas; limite os caminhos ou exclua essas pastas explicitamente.

## Documentação das alterações

- Crie a pasta `MD/Documentação das alterações/` na raiz do projeto caso ela não exista.
- Documente nessa subpasta todas as alterações realizadas no projeto, incluindo código, configurações, recursos e documentação.
- Use arquivos Markdown com nomes descritivos e data e hora sem segundos, no formato `AAAA-MM-DD_HH-mm_descricao.md`. Separe data, hora e descrição com underscores, mantendo hífens entre as palavras da descrição. Use o fuso horário `America/Cuiaba`; por exemplo: `2026-10-16_09-30_ajustar-formato-data-hora.md`.
- Registre o que foi alterado, o motivo e as validações realizadas. Informe limitações ou verificações pendentes quando houver.
- Atualize o documento da tarefa ao concluir o trabalho para refletir o resultado final.

## Explicação de conceitos por alterações

- Crie também a pasta `MD/Explicação de conceitos por alterações/` caso ela não exista.
- A cada alteração realizada no projeto, crie ou atualize nessa pasta um documento explicando todos os conceitos aplicados ou utilizados na alteração mais recente.
- Use o mesmo padrão de nomes da documentação de alterações: `AAAA-MM-DD_HH-mm_descricao.md`, no fuso `America/Cuiaba`.
- Explique cada conceito em linguagem clara: o que é, por que foi utilizado e como aparece na alteração. Inclua exemplos relacionados ao código ou aos arquivos alterados quando ajudarem a compreensão.
- Cubra os conceitos efetivamente envolvidos na tarefa, incluindo os de código, arquitetura, ferramentas, configuração ou documentação, conforme aplicável. Evite acrescentar conceitos sem relação com a alteração.
- Escreva como uma aula aprofundada para o usuário aprender o que foi feito, e não apenas como um resumo técnico, glossário ou lista de definições. O objetivo é permitir que ele compreenda o raciocínio e consiga reproduzir ou adaptar a solução.
- Desenvolva os pré-requisitos necessários e avance dos fundamentos até a implementação concreta. Explique os termos antes de usá-los e não presuma conhecimento prévio sem apresentá-lo.
- Ensine os conceitos, algoritmos, lógicas, padrões de projeto e decisões de arquitetura efetivamente envolvidos. Explique como as partes se conectam, quais responsabilidades possuem e como os dados e o controle percorrem a solução.
- Apresente o problema original, o comportamento anterior, a causa identificada e a construção da solução passo a passo. Diferencie fatos verificados de hipóteses e decisões de implementação.
- Para algoritmos e regras, mostre entradas, saídas, estados intermediários, condições de parada e casos de borda. Use exemplos concretos, simulações manuais e pseudocódigo quando ajudarem; explique custos de tempo, memória e processamento quando relevantes.
- Analise trechos reais dos arquivos alterados, explicando os blocos importantes, o motivo de cada decisão e seus efeitos. Relacione a explicação aos caminhos dos arquivos e aos métodos envolvidos; não substitua a aula por uma cópia do código.
- Discuta alternativas, vantagens, desvantagens, limitações e por que a abordagem escolhida atende ao problema. Não invente padrões, algoritmos ou camadas que não foram utilizados, nem force temas sem relação com a tarefa.
- Explique como validar a solução, o que cada teste ou verificação demonstra, o que não demonstra e quais erros comuns podem ocorrer. Inclua perguntas ou pequenos exercícios com respostas comentadas para consolidar os pontos centrais quando houver conteúdo técnico que justifique isso.
- Use diagramas, tabelas ou exemplos visuais quando tornarem o funcionamento mais claro. Priorize profundidade, precisão e progressão didática; tarefas simples devem ser explicadas com rigor sem conteúdo artificial para aumentar o tamanho.
- Relacione o documento de conceitos ao registro correspondente em `MD/Documentação das alterações/` por meio de links relativos e mantenha ambos atualizados com o resultado final.

## Explicação de conceitos por classe

- Crie também a pasta `MD/Explicação de conceitos por classe/` caso ela não exista.
- Para cada classe criada ou afetada pela alteração, crie ou atualize uma aula própria explicando todos os conceitos efetivamente utilizados naquela classe, incluindo as partes existentes necessárias para entendê-la, e não apenas as linhas modificadas.
- Mantenha um documento consolidado por classe, atualizado quando ela mudar, sem criar um arquivo por pedido. Use o nome totalmente qualificado da classe para evitar ambiguidades, como `com.example.samplestickerapp.CropVideoActivity.md`. Registre no texto o caminho do código e a data e hora da última atualização no formato `AAAA-MM-DD_HH-mm`, no fuso `America/Cuiaba`.
- Aplique a mesma profundidade didática exigida em “Explicação de conceitos por alterações”: fundamentos, responsabilidades, campos e estado, métodos, fluxo de dados e controle, algoritmos, lógica, padrões e arquitetura realmente presentes, dependências e colaboração com outras classes. Explique também ciclo de vida, concorrência, persistência e tratamento de erros quando utilizados.
- Analise trechos reais da classe, construa exemplos e simulações passo a passo, explique decisões e alternativas e apresente validações, limites e exercícios comentados quando pertinentes. Baseie a aula na leitura da classe atual; não invente conceitos ou comportamentos.
- Esta aula por classe complementa a aula da alteração e a documentação de regras; não substitui nenhuma delas. Use links relativos para relacionar os documentos e evite duplicações extensas desnecessárias.
- Respeite a restrição de leitura para contexto. Para manter uma aula de uma classe afetada na tarefa atual, é permitida somente a leitura pontual necessária para atualizar esse documento, sem consultar aulas de outras classes ou tarefas. Se a alteração não afetar classes, não crie uma aula de classe fictícia.

## Documentação de regras

- Crie também a pasta `MD/Documentação de regras/` caso ela não exista.
- Mantenha nessa pasta uma documentação consolidada dos assuntos tratados nas alterações, agrupada por tema ou funcionalidade, e não por pedido do chat. Exemplos de temas: compressão de figurinhas, importação de GIF, reprodução e crop, processamento em background e organização de pacotes.
- Antes de criar um arquivo, procure pelo nome um documento do mesmo tema e atualize-o, respeitando a leitura seletiva acima. Crie outro arquivo somente quando houver um assunto distinto que justifique documentação própria. Uma alteração pode atualizar vários temas; vários pedidos podem atualizar o mesmo arquivo.
- Use nomes estáveis, específicos e informativos, sem data no nome, que permitam reconhecer o assunto e o escopo sem abrir o documento. Prefira palavras-chave da funcionalidade e dos mecanismos abordados, como `compressao-de-figurinhas-animadas-webp-limite-500kb-qualidade-fps.md`, evitando nomes genéricos como `regras.md` ou `funcionamento.md`. Não inclua no nome assuntos que o documento não cobre. Quando um nome existente não representar bem o conteúdo, renomeie o arquivo e atualize os links conhecidos; não leia as pastas restritas apenas para procurar referências.
- O nome sem data dos temas é uma exceção intencional ao padrão com data das pastas de registros e aulas. Registre a data e hora da última atualização dentro do documento, no fuso `America/Cuiaba` e formato `AAAA-MM-DD_HH-mm`.
- Explique como o tema funciona no estado atual do projeto: finalidade, regras de negócio, limites, entradas e saídas, fluxo de funcionamento, componentes responsáveis, algoritmos e decisões relevantes, exemplos, exceções e tratamento de erros. Desenvolva os tópicos aplicáveis com profundidade e linguagem didática, sem inventar comportamentos não verificados.
- Atualize ou substitua explicações que ficaram obsoletas quando o comportamento mudar; não apenas acrescente relatos cronológicos ou copie o histórico do chat. Diferencie comportamento implementado, limitações conhecidas e propostas ainda não implementadas.
- Relacione os temas aos arquivos e métodos pertinentes e, quando útil, aos registros de alterações e às aulas por meio de links relativos. Mantenha os documentos coerentes entre si, evitando repetir explicações extensas que possam ser referenciadas.
- Ao concluir cada alteração, revise todos os temas afetados e atualize a documentação correspondente. Esta documentação complementa os registros por tarefa e as aulas; não os substitui.

## Mensagem de commit

- Ao final de todas as respostas finais ao usuário, sugira uma mensagem de commit em português mais detalhada e explicativa, coerente com o trabalho realizado. Não limite a sugestão a uma frase curta ou genérica.
- A mensagem deve explicar as principais alterações, as partes afetadas e o motivo ou efeito prático das mudanças. Inclua validações relevantes quando realizadas, sem afirmar verificações que não ocorreram.
- Apresente a sugestão com o prefixo “Commit sugerido:”, seguido de um bloco de código com um título descritivo e um corpo explicativo. Dimensione o detalhamento à tarefa e mantenha o texto pronto para uso como mensagem de commit.
- Não execute commits automaticamente; somente quando o usuário solicitar.
