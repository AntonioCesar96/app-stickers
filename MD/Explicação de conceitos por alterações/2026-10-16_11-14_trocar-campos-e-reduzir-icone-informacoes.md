# Posição de campos e tamanho visual de um ícone

## Apresentação e identidade

Um campo tem uma posição na hierarquia do layout e uma identidade usada pelo código. Nesta alteração, a posição de velocidade foi trocada com a de nitidez, mas os IDs `retry_speed` e `retry_detail` continuam identificando seus respectivos valores. O código que lê parâmetros não depende da ordem visual, e sim desses IDs.

O rótulo usa `labelFor` para associar seu significado ao campo correspondente. Ao mover os controles, é necessário mover também essa associação e o tipo de entrada: velocidade aceita decimal, nitidez aceita inteiro. Trocar somente os textos deixaria a interface enganosa, mesmo que parecesse correta.

## Vetor, dp e área de toque

O drawable novo descreve o contorno e a letra i com caminhos vetoriais num viewport de 24×24. O tamanho de exibição é de 20 dp. O viewport define as coordenadas do desenho; dp define sua dimensão lógica na tela, convertida pela densidade do aparelho.

O botão mantém 48 dp e recebe 14 dp de padding em cada lado. Assim, sobram 20 dp para o desenho. `fitCenter` centraliza e escala o conteúdo dentro dessa área. A diferença entre área visível e área tocável permite deixar o ícone discreto sem dificultar a abertura da ajuda.

O preenchimento evenOdd cria a região vazada do círculo a partir dos contornos exterior e interior. A cor cinza reduz a competição visual com o botão principal verde. O listener permanece no mesmo botão: o comportamento da ajuda não muda.

## Verificação e exercício

A validação confere XML, ordem dos campos, IDs e tipos, além da compilação dos recursos. Isso não mede preferência estética de todos os usuários; o objetivo concreto é cumprir a nova posição e reduzir o desenho sem alterar o algoritmo.

**Exercício:** diminuir o botão inteiro de 48 para 20 dp produziria o mesmo resultado? Não. O ícone poderia parecer igualmente pequeno, mas a área de toque também seria reduzida. O padding permite separar essas duas decisões.

Veja o [registro](../Documentação%20das%20alterações/2026-10-16_11-14_trocar-campos-e-reduzir-icone-informacoes.md).
