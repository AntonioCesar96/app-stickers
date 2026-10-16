# Como as instruções globais e locais se combinam

## 1. O problema e os escopos

Uma preferência guardada somente no repositório precisa ser repetida nos demais projetos. O usuário pediu que documentação, aulas, leitura seletiva e sugestão de commit fossem preferências de todo o seu trabalho com o Codex.

Escopo é o conjunto de lugares onde uma instrução se aplica. O escopo global pertence ao perfil do Codex; o escopo local pertence a um projeto ou subpasta. Mover as regras para o global elimina a necessidade de copiá-las manualmente para cada repositório desse perfil.

## 2. Perfil e descoberta dos arquivos

O diretório de configuração padrão é `~/.codex`. Neste ambiente ele corresponde a `C:\Users\Dell\.codex`. A variável `CODEX_HOME` pode indicar outro diretório. Um perfil diferente ou outro computador não passa a usar automaticamente o arquivo gravado aqui.

Conforme a [documentação oficial](https://developers.openai.com/codex/guides/agents-md), a descoberta global considera `AGENTS.override.md` antes de `AGENTS.md`; arquivos de projeto são acrescentados na cadeia e podem sobrepor instruções globais. Portanto o global fornece a base reutilizável, sem impedir exceções locais.

Nesta tarefa, a inspeção encontrou um global vazio, nenhuma substituição global e nenhuma definição de CODEX_HOME nos locais consultados. Isso permitiu migrar sem substituir instruções globais anteriores.

## 3. Migração, conferência e duplicação

A operação ocorreu em etapas: ler as regras do projeto, gerar o texto global preservando as seções, gravar o global, conferir sua igualdade com o texto esperado e somente então reduzir o arquivo local a uma referência. Essa ordem evita retirar a origem antes de verificar o destino.

O título mudou para indicar escopo global. Uma introdução esclarece que `MD/` continua sendo relativo ao projeto trabalhado. Sem isso, seria possível interpretar incorretamente que todos os documentos devem ir para a pasta de configuração do Codex.

Manter duas cópias integrais das mesmas instruções criaria repetição e risco de divergência: uma edição futura poderia atualizar somente uma cópia. A referência local preserva a indicação para quem abre este repositório, deixando as preferências completas em um único lugar.

## 4. Validação e limites

A comparação exata demonstra que nenhuma regra foi omitida na transferência, além das transformações declaradas. Ela não demonstra que qualquer instrução local futura será compatível nem que outros ambientes usarão este perfil.

O arquivo global ficou abaixo do limite padrão de 32 KiB para o conteúdo combinado das instruções, mas outros arquivos podem aumentar esse total. A documentação orienta iniciar nova execução ou sessão quando for necessário reconstruir a cadeia. Nesta tarefa o ambiente também apresentou as instruções globais atualizadas, evidência adicional de sua descoberta.

`git diff --check` verifica problemas de espaços nas alterações acompanhadas pelo Git do projeto. Ele não audita o arquivo externo global, que foi conferido separadamente por leitura e comparação. Não houve mudança em classes do aplicativo, por isso não se criou uma aula por classe.

## 5. Exercícios comentados

**Pergunta:** copiar o global para este computador configura automaticamente o Codex em outro computador?

**Resposta:** Não. O arquivo pertence ao perfil local. O outro ambiente precisa receber a configuração no seu próprio diretório do Codex.

**Pergunta:** se o global exige documentação, os arquivos MD devem ser escritos dentro de `.codex`?

**Resposta:** Não. O global guarda a instrução, mas ela determina que os documentos sejam criados no projeto ou na pasta trabalhada.

**Pergunta:** por que conferir o destino antes de retirar as regras do arquivo local?

**Resposta:** Porque a gravação pode falhar ou produzir um texto incompleto. Conferir primeiro mantém a origem disponível até existir evidência de que a transferência foi concluída.

Veja o [registro da alteração](../Documentação%20das%20alterações/2026-10-16_10-03_migrar-regras-para-agents-global.md).
