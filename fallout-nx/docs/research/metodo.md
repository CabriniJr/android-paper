# O método, condensado

Resumo do `porting-guide.md` do sa2-nx e do `porting-another-game.md` do masseffect-nx, com os pontos que mais pesam
num jogo como o Fallout. Para os detalhes, vá à fonte: este arquivo diz **onde** ler.

## A ordem

1. **PC primeiro.** Depurador, rebuild rápido e a GPU emulada do SDK como referência. Tudo funciona e é conferido
   no PC; depois é medido e ajustado no console. (sa2 §0)
2. Traduzir, varrer o código que faltou, traduzir de novo, até convergir. (sa2 §3)
3. Boot no PC com a GPU emulada até menus, gameplay e cutscenes baterem com vídeo do console real. (sa2 §4)
4. Renderer nativo + biblioteca de shaders, comparados quadro a quadro com a GPU emulada. (sa2 §5–6)
5. Switch: build, camada Horizon, lançamento com title override. (sa2 §7)
6. Medir no console, otimizar uma coisa por vez, provar cada mudança. (sa2 §8–12)

## Código que o gerador não vê (sa2 §3.2)

| Classe | Sintoma | Ferramenta (em `work/sa2-nx/tools/`) |
|---|---|---|
| Funções só alcançadas por ponteiro (vtables, tabelas de init da CRT, callbacks) | "unregistered call" | `pointer_fixpoint.py`, `pointer_audit.py` |
| Jump tables truncadas | instrução ilegal, "switch case out of range" | `switch_check.py` |
| Gaps entre funções | crash em código que existe no XEX e não no C++ | `find_gaps.py`, `gap_fixpoint.py`, `runtime_gap_resolver.py` |
| `setjmp`/`longjmp` | r1/r31 corrompidos depois de uma chamada | achar pela forma; `setjmp_address` |
| Funções que não retornam | fall-through errado, funclet de EH colado no pai | `read_before_write.py --noreturn=` |

**Para o Fallout, as vtables são a maior classe [hipótese]:** a Gamebryo e a camada TES são C++ muito virtual
(`NiObject`, `TESForm`, `BSShader`). Espere muitas rodadas de `pointer_fixpoint` antes do primeiro boot.

**Atenção:** várias ferramentas do sa2-nx têm padrões do SA2 escritos no código (`app/`, faixa de código
`82120000:82970600`). Sempre passe `--gen`, `--image` e `--code` explicitamente, como faz o `pipeline.sh audit`.

## Regras que mantêm o port correto (sa2 §0, §12; ME `do-not-break.md`)

- Nenhum dado do jogo no repositório.
- Toda correção traz evidência.
- Uma chave de configuração por otimização, mais duas chaves mestras; a primeira linha do log diz o que está ligado.
- Caminhos rápidos se autoverificam (guarda: as primeiras 256 chamadas, depois 1 a cada 4096; modo de verificação
  compara todas).
- Teste de pixels em lockstep (`vblank_per_swap`): duas execuções, hashes iguais por frame. O teste precisa falhar
  quando o código testado está desligado.
- Floats exatos: `-ffp-contract=off` na compilação **e** no link; nada de FMA vetorial que o guest não tinha.

## Armadilhas que custaram dias (sa2 §14, ME platform-notes)

- Modo applet: memória insuficiente. Lance com title override ou com um forwarder de espaço de endereçamento de 39 bits.
- O código de inicialização tem de ficar no endereço 0 (cuidado com arquivos de ordenação de seções).
- Handles de thread vazam até o limite do Horizon (o `switch_reaper` do SDK fecha).
- Escrita no SD trava a thread que loga: use o writer de log do SDK.
- Timings do PC enganam: uma mudança que corta pela metade um item no profile do PC pode não fazer nada no A57.
- Estatísticas estáticas inicializadas na primeira chamada fazem a execução de sombra seguir outro caminho.

## Ferramental de medição (sa2 §8, ME `measuring.md`)

O profiler do SDK (`sdk/src/ui/switch_perf.cpp`) escreve `logs/rex/rex_profile.log` a cada 10 s: FPS, tempo de CPU
por thread (vindo do kernel), clocks, temperaturas, chamadas de IPC por segundo e amostras de pilha a cada 1 ms.
Arquive o ELF de toda NRO testada para simbolizar depois. Compare intervalos no mesmo clock.
