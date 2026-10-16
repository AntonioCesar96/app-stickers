# Texto visual e identidade de um botão

Data: 2026-10-16_16-46 — America/Cuiaba.

O botão abaixo da figurinha é declarado no layout XML `app/src/main/res/layout/activity_sticker_pack_details.xml`. O atributo `android:text` define o que o usuário lê; `android:id` identifica o componente para o código. São responsabilidades diferentes.

Nesta alteração, `android:text="Adicionar às selecionadas"` passou a `android:text="Adicionar"`. O identificador continua `add_selected_sticker`, portanto o listener configurado pelo adapter encontra o mesmo botão e executa a mesma cópia para a coleção. Não é necessário mudar métodos, persistência ou navegação para apenas abreviar o rótulo.

O texto menor também ocupa menos espaço no cartão. O layout decide sua medição a partir das dimensões, estilo, fonte e conteúdo; reduzir o texto evita a quebra que o rótulo anterior apresentava no aparelho testado na tarefa anterior. Esta tarefa não exige um novo algoritmo nem uma classe adicional.

A revisão do XML confirma o novo rótulo e a preservação do identificador. Compilar os recursos verifica que o layout continua válido para o Android, mas não comprova, por si só, a aparência em todo tamanho de tela. Como a mudança só altera apresentação textual, não foram criados testes que repitam o valor literal do atributo.

[Registro](../Documentação%20das%20alterações/2026-10-16_16-46_abreviar-botao-adicionar-figurinha.md) · [Regras atuais](../Documentação%20de%20regras/figurinhas-selecionadas-copia-individual-pacotes-estaticos-animados-whatsapp.md)
