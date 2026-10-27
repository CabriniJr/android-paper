# fallout-nx: instruções para sessões do Claude

Port por recompilação estática de Fallout 3 e Fallout: New Vegas (Xbox 360) para o Nintendo Switch (homebrew).
Projeto de estudo do usuário, que é engenheiro de plataforma e observabilidade: explique o *porquê* das decisões de
hardware e compilador, e trate medição como cidadã de primeira classe (tempo de frame como SLO).

## Antes de qualquer trabalho

1. Leia `docs/SESSAO-DESKTOP.md` (o roteiro da sessão), `docs/ROADMAP.md` (onde estamos) e
   `docs/research/perguntas.md` (o que está aberto).
2. `./pipeline.sh status` mostra o que já existe em `work/`.
3. Fatos sobre o jogo, o hardware e o método estão em `docs/research/`, marcados como [verificado], [relatado]
   ou [hipótese]. Não trate uma hipótese como fato. Ao confirmar ou derrubar uma, atualize a marcação e registre a
   evidência em `perguntas.md`.

## Regras invioláveis

- **Nenhum dado do jogo no git.** Arquivos extraídos, C++ gerado, dumps de imagem, shaders, logs com conteúdo do jogo:
  tudo em `work/` (ignorado). Antes de cada commit, rode `git status` e confira se nada de `work/`, `*.xex`, `*.bin`
  ou `generated/` foi adicionado.
- **Proveniência (decisão D2, `docs/research/simbolos.md`):** nomes e layouts só do executável do próprio usuário,
  de projetos públicos e da nossa análise. Nada de PDBs de devkit, builds vazadas ou bancos IDA derivados delas.
  Nada de dump do Skyrim do Switch.
- **Nenhuma chave nem ferramenta de DRM** no repositório. A descriptografia do XEX é do gerador do SDK.
- **Toda correção em `games/*/overrides.toml` traz a evidência** num comentário: endereço, quem referencia, qual
  ferramenta ou linha de log encontrou.
- **Uma build por jogo** (fixada em `versions.toml`) até o Marco 4. Overrides não valem entre builds.
- Relate os resultados com fidelidade, inclusive o que não foi verificado e o que falhou.

## Mapa

| Caminho | O quê |
|---|---|
| `pipeline.sh` | etapas: doctor, deps, refs, sdk, extract, identify, codegen, audit, status |
| `deps.lock` | o toolchain fixado (sa2-nx: fork do SDK ReXGlue 0.10.0, ferramentas, renderer) |
| `refs.lock` | repositórios de referência só para leitura (masseffect-nx, nfsmw-nx, sc5p2-nx, xNVSE, patches do Xenia, ReXGlue) |
| `games/<fo3,fnv>/` | manifesto do gerador, overrides, flavour do codegen, builds conhecidas |
| `tools/` | as nossas ferramentas (Python 3.11+, sem dependências) |
| `docs/research/` | a base de pesquisa |
| `work/` | tudo o que é gerado ou baixado (ignorado) |

As ferramentas do sa2-nx (`work/sa2-nx/tools/`) têm padrões do SA2 escritos no código (`app/`, faixa de código):
sempre passe `--gen`, `--image` e `--code` explicitamente.

## Estilo

- Documentação e mensagens ao usuário em português. Código, comentários de código e commits em inglês, no estilo dos
  arquivos vizinhos.
- Scripts de shell em bash com `set -euo pipefail`; ferramentas novas em Python sem dependências externas quando
  possível.
- Uma mudança por commit, com o porquê na mensagem.
