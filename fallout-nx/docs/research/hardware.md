# Hardware: de onde vem o código, para onde vai

## Origem e destino

| | Xbox 360 | Nintendo Switch (Erista/Mariko, clock padrão) |
|---|---|---|
| CPU | Xenon: 3 núcleos PowerPC *in-order* a 3,2 GHz, 2 threads por núcleo (6 threads), VMX128 | 4× Cortex-A57 a **1020 MHz**; o jogo usa **3** (o 4º é do sistema), NEON, L2 de 2 MB, I-cache de 48 KB **[verificado]** platform-notes (ME), porting-guide (SA2) |
| GPU | Xenos: 48 ALUs unificadas, ~500 MHz, **10 MB de eDRAM** com ROPs | GM20B (Maxwell, 256 núcleos): 307,2 MHz portátil (o ME pede o perfil oficial de 460,8), 768 MHz na dock **[verificado]** platform-notes |
| Memória | 512 MB GDDR3 unificada | 4 GB LPDDR4: 1331,2 MHz portátil, 1600 MHz na dock. O processo recebe **~3189 MB** com title takeover e ~400 MB em modo applet **[verificado]** platform-notes |
| Endianness | big-endian | little-endian (o código gerado faz byte-swap nos acessos à memória do guest) |

## Fatos do Horizon/NVK que mudam o design

Todos **[verificado]** em `work/refs/masseffect-nx/docs/platform-notes.md` (medidos no console ou herdados dos ports anteriores):

- **Prioridade `0x3B` é a única com fatia de tempo** (10 ms). As outras são cooperativas: uma thread que nunca
  bloqueia fica com o núcleo. O jogo recompilado faz *spin* o tempo todo, então as threads do jogo rodam em `0x3B`
  e as threads de serviço do host ficam acima delas.
- Sem memória esparsa, sem `mmap` de arquivo e com número limitado de mapeamentos. A memória do guest é comprometida
  em grânulos de 2 MB, e a física só quando tocada.
- Exceções chegam por um único handler, numa única pilha, sem syscalls e sem exceções C++. Se o kernel preempta uma
  thread dentro do handler, ela perde metade dos registradores vetoriais.
- **Nunca ler `cntvct_el0`** (dá falha). **Nunca usar o boost oficial de CPU** (a GPU cai para 76,8 MHz).
- `thread_local` vira uma chamada de função (`__aarch64_read_tp`): manter fora de loops (porting-guide 7.4).
- Milhares de arquivos pequenos no SD são lentos de abrir. Por isso o asset pack (`.nxpack`) junta tudo num arquivo.

## O que isso significa para o Fallout [hipótese]

1. **CPU:** 6 threads de hardware viram 3 núcleos. A thread principal da engine (IA, scripts, física, culling) mais
   a thread do anel do renderer competem pelos mesmos núcleos. O ME1 mostrou que 100 core-ms por frame é o teto
   para 30 FPS. A primeira medida útil é o custo da thread principal em core-ms (P-05).
2. **Memória:** 3189 MB contra os 512 MB originais. Há folga para caches (pipelines, texturas convertidas, blocos de
   I/O). O risco não é falta de memória, é **fragmentação** em sessões longas (ME1: `bad_alloc` depois de 70 min).
   Um jogo de 100 horas precisa de teste de sessão longa desde o Marco 4.
3. **GPU:** o FO3 supõe que blend, Z e MSAA na eDRAM são baratos. No Switch, esse tráfego vai para a LPDDR4
   (~21–25 GB/s). Resolução interna, MSAA desligado e resolves aliasados são os controles (porting-guide 11;
   masseffect-nx `do-not-break.md`: nunca reduza o front buffer, só a cena).
4. **Armazenamento:** os BSAs do FO3 são grandes e lidos durante o jogo (streaming de células). O cache de blocos do
   ME1 (`streaming-io.md`) é o ponto de partida.
