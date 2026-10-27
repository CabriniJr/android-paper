# Roadmap

Fallout 3 primeiro; New Vegas depois, reaproveitando tudo o que for da engine (Gamebryo/"Fallout engine" de 2008–2010).
Cada marco tem um critério de saída verificável. Não se avança com o anterior pela metade.

## Marco 0: pipeline (este commit)

- [x] Etapas `doctor`, `deps`, `sdk`, `extract`, `identify`, `codegen`, `audit`, `status`
- [x] Toolchain fixado (`deps.lock`: sa2-nx @ `adf3a4e`, ReXGlue 0.10.0)
- [x] Manifestos de FO3 e FNV no formato do sa2-nx, sem dados do jogo
- [x] Base de pesquisa (`docs/research/`) e repositórios de referência (`refs.lock`, etapa `refs` testada)
- [ ] Rodar `deps` + `sdk` numa máquina com clang 20 e registrar os tempos aqui
- [ ] `identify` nos seus discos; registrar as builds em `games/*/versions.toml` (P-01)
- Roteiro: [SESSAO-DESKTOP.md](SESSAO-DESKTOP.md)

**Saída:** `./pipeline.sh status fo3` com tudo marcado até `codegen`.

## Marco 1: tradução limpa (PC, sem rodar)

- `codegen` sem erros de validação: cada "target not in any function" vira entrada em `[functions]` com evidência
- Achar `setjmp`/`longjmp` pela forma (sa2-nx porting-guide 3.2), registrar `setjmp_address`/`longjmp_address`
- Varreduras: `find_gaps`, `switch_check`, `pointer_fixpoint` até convergir
- Procurar módulos extras (`.xex` dentro dos dados carregados com `XexLoadImage`) → `[[modules]]`
- Anotar a versão do XDK/Direct3D (decide o layout do device, porting-guide 10)
- Title update: o XEX patch (`.xexp`) aplicado antes da análise, se houver

**Saída:** codegen sem `--force`; relatório de gaps estável entre duas passadas.

## Marco 2: primeiro boot no PC (GPU emulada)

- App PC (subclasse `ReXApp`, CMake) adaptada do `app/` do sa2-nx, como o sc5p2-nx fez com `app_sc5p2/`
- Montar o disco 2 e os DLCs (GOTY: Operation Anchorage, The Pitt, Broken Steel, Point Lookout, Mothership Zeta)
- Triagem de crashes: unregistered call → gap/ponteiro; access violation → `llvm-symbolizer`
- Saves funcionando cedo

**Saída:** do boot ao menu, carregar um save, andar no Wasteland, comparado com vídeo de um console real.

## Marco 3: renderer nativo no PC

- Achar as funções do Direct3D do jogo (`d3d_match.py`, `listing_diff.py`) e ligar os hooks (shader load, Draw*, Create*Shader, flip)
- Biblioteca de shaders: dump → XenosRecomp → DXC → SPIR-V, até `0 unidentified loads`
- Comparar quadro a quadro com a GPU emulada (lockstep `vblank_per_swap`)

**Saída:** nenhuma diferença visível entre os renderers em menus, interior, exterior, VATS e Pip-Boy.

## Marco 4: Switch

- Build devkitA64 + NVK; `check_switch_build.py` a cada build
- Asset pack (o FO3 tem milhares de arquivos dentro dos BSAs; medir a abertura no SD)
- Lançar com title override (o modo applet não tem memória suficiente)

**Saída:** jogável no console, qualquer FPS, sem crash por 30 minutos.

## Marco 5: desempenho

Na ordem que pagou nos ports anteriores (porting-guide 9–11), cada item atrás de uma chave e medido no console:

1. Flavour do codegen (registradores como locais, chamadas diretas, sem `volatile`)
2. `-O3`, LTO, `-fipa-pta`, funções quentes primeiro, PGO a partir de uma sessão real
3. Nativos verificados para as funções mais quentes (matemática de matriz/vetor da engine, memcpy/memset em NEON)
4. Custo de CPU do renderer (deferred recording, fingerprints CRC32)
5. GPU: menos quebras de render pass, resolves aliasados, constantes de especialização, resolução interna e
   sombras ajustáveis

Alvo inicial: **30 FPS estáveis no clock padrão** (1020 MHz de CPU). Para comparar, no próprio 360 o FO3 caía abaixo
de 30 em áreas abertas. Instrumentação desde o começo: o profiler do SDK (`switch_perf`) e o tempo de frame tratado
como um SLO (p99 ≤ 33,3 ms).

## Marco 6: New Vegas

- Mesma pipeline com `fnv`; portar as correções da engine identificando funções equivalentes por hash e forma
- Separar o que é da engine (compartilhado) do que é específico de cada jogo
