# Base de pesquisa

O que já se sabe (e o que ainda não se sabe) para portar Fallout 3 e New Vegas do Xbox 360 para o Switch. Escrita em
27/10/2026 a partir de pesquisa na web e da leitura dos repositórios fixados em [`refs.lock`](../../refs.lock)
(`./pipeline.sh refs` baixa todos para `work/refs/`).

## Legenda

Cada afirmação traz o seu grau de confiança:

- **[verificado]**: lido na fonte primária (código, documentação do projeto, cabeçalho do arquivo), com a fonte citada.
- **[relatado]**: dito por terceiros (imprensa, wiki, fórum), sem confirmação nossa.
- **[hipótese]**: dedução nossa; tem uma pergunta correspondente em [perguntas.md](perguntas.md).

Quando uma hipótese for confirmada ou derrubada, atualize a marcação aqui e registre a evidência em `perguntas.md`.

## Páginas

| Página | Conteúdo |
|---|---|
| [precedentes.md](precedentes.md) | Os ports 360 → Switch que existem, o que cada um ensina, números de desempenho, qual base usar |
| [alvo-fallout.md](alvo-fallout.md) | Fallout 3 e New Vegas no 360: edições, IDs, DLC, engine, formatos, estado no Xenia |
| [simbolos.md](simbolos.md) | Como dar nome às funções do `default.xex`: fontes públicas, o que transfere, política de proveniência |
| [hardware.md](hardware.md) | Xenon/Xenos vs. Tegra X1, e os fatos medidos do Horizon/NVK que limitam o port |
| [metodo.md](metodo.md) | O método dos ports anteriores, condensado, com as armadilhas que custaram dias |
| [perguntas.md](perguntas.md) | Perguntas abertas numeradas, cada uma com o método para responder e onde registrar |

A leitura obrigatória fora deste repositório, na ordem:

1. `work/sa2-nx/docs/porting-guide.md`: o método completo, do `default.xex` ao NRO.
2. `work/refs/masseffect-nx/docs/porting-another-game.md`, depois `platform-notes.md` e `performance-history.md`:
   o precedente mais parecido com o nosso (AAA de 2007, em disco, com áreas abertas e streaming).
3. `work/refs/masseffect-nx/docs/external-function-maps.md`: o modelo para o nosso [simbolos.md](simbolos.md).
4. `work/refs/rexglue-sdk/` (wiki em https://github.com/rexglue/rexglue-sdk/wiki): CLI e formato do manifesto.
