# Conceitos: documentação das decisões e do aprendizado

## Instruções do projeto em AGENTS.md

O `AGENTS.md` reúne instruções para os agentes que trabalham neste projeto. Nesta alteração, ele passa a exigir uma explicação dos conceitos usados em cada tarefa, além do registro das mudanças. Isso torna o aprendizado parte do fluxo de trabalho.

## Registro de alterações e explicação de conceitos

O registro de alterações informa o que mudou, por que mudou e como foi verificado. A explicação de conceitos descreve os conhecimentos envolvidos, seu propósito e sua aplicação concreta. Por exemplo, esta tarefa registra a nova regra em uma pasta e explica aqui os conceitos de organização documental que ela utiliza.

## Organização por pastas

Subpastas agrupam documentos por finalidade. `MD/Documentação das alterações/` contém o histórico do trabalho; `MD/Explicação de conceitos/` contém as explicações relacionadas a esse trabalho. A separação facilita encontrar tanto o registro quanto o material de estudo.

## Convenção de nomes e fuso horário

Uma convenção de nomes define um formato consistente para os arquivos. O padrão `AAAA-MM-DD_HH-mm_descricao.md` identifica a data, a hora e o assunto da tarefa, sem segundos. A ordem ano-mês-dia permite organizar os nomes cronologicamente. O fuso `America/Cuiaba` mantém os horários alinhados ao contexto do usuário.

## Markdown e links relativos

Markdown é o formato de texto usado nesses documentos, com títulos, parágrafos e links. Um link relativo aponta para outro arquivo a partir da pasta do documento atual, mantendo a relação entre os textos quando o projeto é movido para outro local.

Consulte o [registro desta alteração](../Documentação%20das%20alterações/2026-10-16_09-37_adicionar-explicacao-de-conceitos.md).

## Verificação com git diff --check

O comando `git diff --check` procura problemas de espaços e formatação nas alterações acompanhadas pelo Git. Ele ajuda na revisão textual, mas não verifica a correção das explicações nem substitui a conferência dos links, realizada separadamente nesta tarefa.
