# fallout-nx

Ports nativos de **Fallout 3** e **Fallout: New Vegas** (Xbox 360) para o **Nintendo Switch** (homebrew), por
recompilação estática. Não é um emulador: o PowerPC do `default.xex` é traduzido para C++ antes da execução, compilado
para AArch64 e desenhado por um renderer Vulkan nativo (NVK).

Projeto de estudo: o objetivo é tirar o máximo de desempenho do Tegra X1 e aprender, no caminho, como chips gráficos
funcionam (Xenos vs. Maxwell). O plano está em [docs/ROADMAP.md](docs/ROADMAP.md), e a trilha de estudo de GPU em
[docs/gpu-study.md](docs/gpu-study.md).

> [!IMPORTANT]
> Você precisa do **seu próprio disco** do jogo. Nada derivado dele (arquivos extraídos, C++ gerado, dumps, shaders) é
> versionado: tudo vai para `work/`, que o git ignora. O projeto não contém chaves nem ferramentas de DRM; a
> descriptografia do XEX é feita pelo gerador de código do SDK, como em todos os ports ReXGlue.

## A base

A pipeline usa o mesmo toolchain dos ports que já rodam no Switch (nfsmw-nx, masseffect-nx, sa2-nx, sc5p2-nx):

| Camada | Ferramenta | Origem |
|---|---|---|
| PowerPC → C++ | `rexglue codegen` | [ReXGlue](https://github.com/rexglue/rexglue-sdk) v0.10.0, fork do [sa2-nx](https://github.com/ChanseyIsTheBest/sa2-nx) (`sdk/`) |
| Kernel/XAM do 360 | runtime do SDK + camada Horizon | idem |
| Ferramentas de análise | `find_gaps`, `switch_check`, `pointer_fixpoint`... | sa2-nx `tools/` |
| Shaders Xenos → SPIR-V | XenosRecomp + DXC | sa2-nx `shaders/` |
| GPU | renderer nativo (anel PM4 → Vulkan) | sa2-nx `app/src/me` (de masseffect-nx) |
| Driver Vulkan no Switch | NVK/Mesa | [mesa-switch](https://github.com/danfromtico/mesa-switch), build do [nfsmw-nx](https://github.com/StevensND/nfsmw-nx) |
| Switch | devkitA64 + libnx | [devkitPro](https://devkitpro.org) |

As versões ficam fixadas em [`deps.lock`](deps.lock). O guia de referência do método é o
`docs/porting-guide.md` do sa2-nx (fica em `work/sa2-nx/docs/` depois do `deps`).

## A pipeline

```text
seu disco (.iso)
   │ extract      work/assets/<jogo>/game_root/        (sa2-nx tools/extract_iso.py, só lê o .iso)
   ▼
default.xex
   │ identify     work/<jogo>/identity.txt             (SHA-256 + cabeçalho XEX2 vs. games/<jogo>/versions.toml)
   ▼
   │ codegen      work/<jogo>/generated/               (rexglue codegen games/<jogo>/<jogo>_manifest.toml)
   ▼              work/<jogo>/default_image.bin        (imagem descomprimida, para as ferramentas de análise)
C++ do jogo
   │ audit        work/<jogo>/audit/                   (gaps, jump tables → correções em overrides.toml)
   ▼
[próximos marcos] build PC → renderer nativo → shaders → build Switch (.nro) → otimização
```

```sh
./pipeline.sh doctor                      # confere as ferramentas
./pipeline.sh deps                        # baixa o toolchain fixado em deps.lock para work/
./pipeline.sh sdk                         # compila o gerador de código (rexglue)
./pipeline.sh extract fo3 ~/Fallout3.iso  # extrai seu disco
./pipeline.sh identify fo3                # identifica a build do executável
./pipeline.sh codegen fo3                 # traduz para C++
./pipeline.sh audit fo3                   # procura código que o gerador não viu
./pipeline.sh status                      # o que já foi feito
```

Cada etapa pode ser rodada de novo sozinha. O jogo é `fo3` ou `fnv`.

### Pré-requisitos

- Linux ou macOS, **clang 20+**, CMake 3.25+, Ninja, Python 3.11+, git.
- Para o Switch (a partir do marco 4): devkitPro (devkitA64, libnx, switch-tools), NVK compilado conforme o nfsmw-nx,
  DXC, e um Switch que rode homebrew.

## Estrutura

| Caminho | Conteúdo |
|---|---|
| `pipeline.sh` | as etapas |
| `deps.lock` | repositórios e commits fixados |
| `tools/identify.py` | identifica o `default.xex` (SHA-256, title id, versão, base, entry point) |
| `games/<jogo>/<jogo>_manifest.toml` | manifesto do gerador de código |
| `games/<jogo>/overrides.toml` | correções feitas à mão, cada uma com sua evidência |
| `games/<jogo>/codegen_flavor.toml` | opções de geração; vazio até o jogo rodar certo no PC |
| `games/<jogo>/versions.toml` | builds conhecidas do executável |
| `docs/` | roadmap e trilha de estudo |
| `work/` | tudo o que é gerado (ignorado pelo git) |

## Regras do projeto

Herdadas dos ports anteriores (sa2-nx `docs/porting-guide.md`, seção 0):

1. Nenhum dado do jogo no repositório.
2. Toda correção no manifesto traz a evidência (endereço, quem referencia, qual log ou ferramenta achou).
3. Uma mudança por vez, cada otimização atrás da sua própria chave de configuração.
4. Toda otimização prova que está certa (verificação contra o código original, teste de pixels em lockstep).
5. Quem decide é o console: só fica a otimização que o profile no Switch confirma.

## Licença

GPL-3.0, a mesma do sa2-nx, sobre o qual o port é construído. Fallout é marca da Bethesda Softworks/ZeniMax. Este é um
projeto de fãs, sem afiliação nenhuma.
