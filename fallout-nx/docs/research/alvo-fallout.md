# O alvo: Fallout 3 e Fallout: New Vegas no Xbox 360

## Identidade

| | Fallout 3 | Fallout: New Vegas |
|---|---|---|
| Lançamento 360 | 2008 (Bethesda Game Studios) | 2010 (Obsidian, publicado pela Bethesda) |
| Title ID | `425307D5` **[relatado]** (No-Intro: "Fallout 3 (World) (425307D5) (Addon)") | `425307E0` **[relatado]** (URL do Marketplace termina em `d802425307e0`) |
| Edição completa | Game of the Year (2009, 2 discos com os 5 DLCs) | Ultimate Edition (2012, 2 discos com os DLCs) |
| Irmãos de engine no 360 | Oblivion (`425307D1`), Skyrim (`425307E6`) | idem |

`./pipeline.sh identify` confirma o title id contra o cabeçalho XEX2 do seu disco. Registre o resultado em
`games/*/versions.toml` e troque a marcação acima para **[verificado]**.

DLCs do FO3: Operation Anchorage, The Pitt, Broken Steel, Point Lookout, Mothership Zeta. DLCs do FNV: Dead Money,
Honest Hearts, Old World Blues, Lonesome Road, Gun Runners' Arsenal, Courier's Stash.

## O que precisa ser descoberto sobre o disco

**[hipótese]** até o primeiro `extract`:

- `default.xex` na raiz e dados em `Data/` (`*.esm` e arquivos `*.bsa`), como no PC.
- Na GOTY/Ultimate, o disco 2 traz os DLCs, provavelmente como conteúdo instalável (pacotes STFS) e não como arquivos
  soltos. O masseffect-nx (`docs/disc-swap.md`) mostra como investigar: imports `XamContent*`, `XamSwapDisc`, strings.
- Title updates (TUs) existem para os dois jogos. No ReXGlue, um TU entra como `patch_file_path` (`.xexp`), aplicado
  antes da análise. **Escolha uma build (com ou sem TU) e fique nela até o Marco 4.** Cada override vale para uma build só.

## A engine

- Gamebryo (NetImmerse) com as camadas da Bethesda por cima: `TESForm`/`TESObjectREFR` (dados do mundo),
  `NiNode`/`NiAVObject` (grafo de cena), `BSShader*` (renderer). Os nomes vêm do código do xNVSE (`GameForms.h`,
  `NiNodes.h`, `GameRTTI_1_4_0_525.inc`, com 1.751 linhas de RTTI do FNV PC). **[verificado]** em `work/refs/xnvse`.
- Física: Havok (middleware compilado estaticamente). Vai ser recompilado junto, com muito VMX128. **[relatado]**
- Vídeo: Bink. O FFmpeg do SDK tem decoder de Bink 1. **[hipótese]**: confirmar que os `.bik` do 360 são Bink 1.
- Áudio: XMA no 360. O SDK já traz decoder de XMA e mixer (porting-guide 7.2). **[verificado]**
- Script: o ObScript da Bethesda, interpretado pela engine, com uma tabela de comandos (nome → handler). No PC o
  xNVSE documenta essa tabela (`CommandTable.h`; comandos de extensão a partir do opcode `0x1400`). **[verificado]**
  No 360 a tabela deve existir igual, e ela nomeia centenas de funções de uma vez ([simbolos.md](simbolos.md)).

## Formatos

- **BSA v104 (0x68)**: usado por FO3 e FNV (o Oblivion usa v103). Flag `0x40` = arquivo de Xbox 360, flag `0x200` =
  codec XMem (compressão exclusiva do 360, só junto com a flag de compressão). Números e hashes depois do cabeçalho
  ficam em big-endian. **[relatado]** UESP, "Archive File Format": a página se contradiz sobre qual bit controla a
  ordem dos bytes. Confirmar no código de um leitor (OpenMW, BSArch) ou no próprio XEX (P-07).
- Texturas: DDS (DXT1/3/5, 8888) no PC. **[hipótese]**: no 360 vêm com tiling do Xenos e possivelmente com
  byte-swap, o que afeta o renderer (untile ao carregar) e o asset pack.
- Malhas: NIF; dados de jogo: ESM/ESP (os mesmos formatos do PC, documentados na UESP e no GECK wiki).

## Estado no Xenia (a referência de comportamento)

- **[relatado]** Um artigo de jan/2026 sobre o Xenia no Mac lista o FO3 (`425307D5`) como "não inicia: loop infinito
  em `KeReleaseSemaphore`". A fonte é fraca (o Xenia não suporta Mac oficialmente).
- **[relatado]** Um protótipo do FO3 (27/03/2009, Hidden Palace) "inicia no Xenia Canary, com algumas texturas deformadas".
- Não há patches do Xenia Canary para FO3 nem para FNV em `game-patches`. **[verificado]**: só para Oblivion e
  Skyrim, que dão endereços de configurações irmãs (60 FPS, resolução 960×540, desligar MSAA).
- Consultar o rastreador de compatibilidade do Xenia Canary com o título exato (P-03). O ReXGlue herda o kernel do
  Xenia: se o Xenia trava em `KeReleaseSemaphore`, o nosso boot pode travar no mesmo lugar, e esse vira o primeiro bug.

## Desempenho no próprio 360

**[relatado]**: os dois jogos tinham quedas abaixo de 30 FPS em áreas abertas e travadas de streaming ao atravessar
células. A meta "30 FPS estáveis" é, portanto, mais alta que o original em algumas cenas. Defina a cena de teste
(P-06) antes de medir qualquer coisa.
