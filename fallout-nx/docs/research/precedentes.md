# Precedentes: ports Xbox 360 → Switch por recompilação estática

## A família

Todos usam ReXGlue v0.10.0 (commit `c94f5eb`) com uma camada Horizon, renderer Vulkan nativo sobre NVK e assets
fornecidos pelo usuário. Todos são GPL-3.0. **[verificado]** nos READMEs e em `patches/README.md` de cada um.

| Projeto | Jogo (ano, engine) | O que trouxe para a família | Desempenho publicado |
|---|---|---|---|
| [nfsmw-nx](https://github.com/StevensND/nfsmw-nx) | Need for Speed: Most Wanted (2005) | A camada Horizon do SDK, o build do NVK (patch de ZCULL e custo de draw), PGO | ver `docs/performance-history.md` |
| [masseffect-nx](https://github.com/nebadasSwifty/masseffect-nx) | Mass Effect (2007, UE3) | O renderer nativo (anel PM4 → Vulkan), cache de I/O, discos múltiplos, instalador para `.iso` | **~25 FPS no clock padrão** a 960×544 (20 em Eden Prime com folhagem, 30+ em interiores) **[verificado]** README |
| [sa2-nx](https://github.com/ChanseyIsTheBest/sa2-nx) | Sonic Adventure 2 (XBLA) | Fork do SDK mais completo, ferramentas de análise, guia do método, nativos verificados, camada D3D nativa | 60 FPS no clock padrão **[verificado]** porting-guide |
| [sc5p2-nx](https://github.com/BiroGrafx/sc5p2-nx) | Space Channel 5 Part 2 (XBLA) | Prova de que o sa2-nx serve de base para outro jogo (`app_sc5p2/` ao lado de `app/`) | 30 FPS com CPU a 1785 MHz; cai no clock padrão **[verificado]** README |
| Banjo N&B, Sonic & All-Stars Racing Transformed, Mushihimesama Futari, Peggle 2 | | Correções compartilhadas | citados no porting-guide do sa2-nx |

## A lição do Mass Effect (o caso mais parecido com o nosso)

Do `README.md`, de `docs/cpu-cost-analysis.md` e de `docs/toolchain.md` do masseffect-nx **[verificado]**:

- **Quem limita é a CPU.** A thread principal precisa de ~30 ms por frame no clock padrão. O frame inteiro custa de
  100 a 142 core-ms em 3 núcleos, sendo que 30 FPS permitem ~100.
- As opções que pagaram mudam **o que o código gerado faz** (registradores como locais, stores eliminados, valores
  passados direto entre funções): a thread principal caiu de 32,4 para ~29 ms.
- **LTO, PGO, `-Os` e ordenação de funções não deram ganho mensurável** no ME1. No SA2 o PGO deu ~15%. Ou seja, o
  ganho depende do jogo: medir antes de adotar.
- Não use o modo de boost oficial de CPU: com ele, o sistema derruba a GPU para 76,8 MHz e o jogo vai a 3–4 FPS.
- Streaming de texturas e de níveis é ponto fraco conhecido (`docs/streaming-io.md`). A memória fragmenta em sessões
  longas, e o primeiro `bad_alloc` apareceu depois de 70 min (`docs/memory-growth.md`).

**Implicação para o Fallout [hipótese]:** o FO3 é um AAA da mesma época, com mundo aberto, IA de muitos atores e
física Havok. Espere a mesma situação: CPU como gargalo, alvo realista de 20–25 FPS no clock padrão antes da
otimização específica e 30 FPS como meta difícil. A pergunta P-05 mede isso.

## Decisão D1: qual fork do SDK usar

| | sa2-nx | masseffect-nx |
|---|---|---|
| Guia do método | o mais completo (`porting-guide.md`, 627 linhas) | `porting-another-game.md` |
| Base para um segundo jogo | provada (sc5p2-nx) | não provada |
| Disco `.iso`, vários discos, streaming de jogo grande | não é o caso dele (XBLA) | é exatamente o caso dele |
| Ferramentas (gaps, ponteiros, jump tables) | sim, com SA2 nos padrões | sim, com ME1 nos padrões |

**Escolha atual:** sa2-nx em `deps.lock`, porque o porting-guide recomenda começar dele e porque o sc5p2-nx mostra
como encaixar outro jogo. O masseffect-nx entra como referência (`refs.lock`) para disco, streaming e memória.
**Reavaliar no Marco 2:** se o FO3 bater nos mesmos problemas de I/O e memória que o ME1, portar do masseffect-nx o
`block_cache` e as correções de `memory-growth.md`. A pergunta P-11 compara os dois SDKs.

## Fora da família ReXGlue, mas relevantes

- **Fallout 4 no Switch 2** (oficial, 2026): mostra o que a Bethesda corta numa plataforma Tegra, mas é outro
  hardware e outra geração de engine. **[relatado]**
- **Skyrim no Switch** (oficial, 2017): a mesma linhagem de engine no mesmo hardware. Serve de referência para os
  cortes de qualidade, observando o jogo rodar, sem dump (ver [simbolos.md](simbolos.md), política). **[relatado]**
- **fallout2-ce-switch / Fallout CE Switch**: Fallout 1 e 2 já no Switch, com SDL2. Servem para aprender o toolchain do
  Switch, não para a engine 3D.
