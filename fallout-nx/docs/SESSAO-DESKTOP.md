# Roteiro da sessão desktop

O que fazer na primeira sessão com acesso aos discos e a uma máquina que compila. Objetivo: **fechar o Marco 0 e
responder P-01, P-04, P-08 e P-10.** O resto do roadmap depende dessas respostas.

## Máquina

- **Linux** (nativo ou WSL2) ou **macOS**. O `pipeline.sh` é bash. As ferramentas `.ps1` do sa2-nx são para Windows
  puro e não são usadas aqui.
- Disco: ~10 GB para `work/` (toolchain, ~150 MB do sa2-nx, terceiros do SDK, imagem extraída do jogo e C++ gerado,
  que passa de 1 GB num AAA).
- RAM: 16 GB ou mais. Compilar o C++ gerado de um AAA é pesado.

Instalação no Ubuntu/Debian (clang 20 vem do apt.llvm.org):

```sh
sudo apt install cmake ninja-build python3 python3-pip git libgtk-3-dev
# clang 20: siga https://apt.llvm.org (script llvm.sh, versão 20), depois:
export CC=clang-20 CXX=clang++-20
pip install xxhash
```

## Permissões que a sessão vai pedir

Diga sim conscientemente a estas, porque cada uma executa código de terceiros fixado em `deps.lock`:

| Etapa | O que executa |
|---|---|
| `./pipeline.sh deps` | `work/sa2-nx/tools/fetch_thirdparty.py` (clona o ReXGlue v0.10.0 com submódulos) |
| `./pipeline.sh sdk` | CMake + clang compilando o SDK do sa2-nx |
| `./pipeline.sh extract` | `work/sa2-nx/tools/extract_iso.py` (só lê o `.iso`) |
| `./pipeline.sh codegen` | o `rexglue` recém-compilado |
| `./pipeline.sh audit` | `find_gaps.py` e `switch_check.py` do sa2-nx |

`refs` só clona repositórios (já testado em nuvem: ok). `identify` é código nosso.

## Passo a passo

```sh
cd fallout-nx
./pipeline.sh doctor                         # 1. tudo [x] em "host tools"
./pipeline.sh deps && ./pipeline.sh refs     # 2. toolchain e referências
./pipeline.sh sdk                            # 3. anote o tempo de build no ROADMAP (Marco 0)
./pipeline.sh extract fo3 /caminho/FO3.iso   # 4. o .iso fica FORA do repositório
./pipeline.sh identify fo3                   # 5. P-01: cole o [[build]] em games/fo3/versions.toml
./pipeline.sh codegen fo3                    # 6. P-04: espere erros na primeira vez
./pipeline.sh audit fo3                      # 7. relatórios em work/fo3/audit/
```

Se o seu dump não for `.iso` (GOD, CCI, ZAR), converta para `.iso` antes (o `extract_iso.py` só lê ISO). Se você já
tiver os arquivos extraídos, copie-os para `work/assets/fo3/game_root/` e pule o passo 4.

## O que fazer com cada resultado

- **identify:** confira o `title_id` (esperado `425307D5` no FO3, `425307E0` no FNV). Registre a build e mude a
  marcação em `docs/research/alvo-fallout.md` para [verificado].
- **codegen com erros:** cada "target not in any function" vira uma entrada `[functions]` em
  `games/fo3/overrides.toml`, com evidência. Agrupe por classe (sa2 porting-guide §3.2) e ataque a maior primeiro.
  `FNX_CODEGEN_FLAGS=--force ./pipeline.sh codegen fo3` gera mesmo com erros, para destravar o `audit`.
- **Faixa de código:** o log do codegen mostra as seções do XEX. Ponha `code_range = "lo:hi"` em
  `games/fo3/versions.toml` e rode `audit` de novo para ligar o `switch_check`.
- **P-08 (RTTI):** `grep -c -a '\.?AV' work/fo3/default_image.bin`. Se for muito maior que zero, a etapa `symbols`
  (`docs/research/simbolos.md`) é o próximo código a escrever.
- **P-10 (tabela de comandos):** `grep -a -b -o 'ToggleGodMode' work/fo3/default_image.bin` dá o offset da string;
  a partir dele, ache as xrefs.

## O que versionar no fim da sessão

Só texto escrito por nós: `versions.toml`, `overrides.toml` (com evidência), as respostas em `perguntas.md`, as
marcações atualizadas em `docs/research/` e o ROADMAP. **Confira o `git status` antes do commit:** nada de `work/`.

## Se travar

- `sdk` falha no configure: veja `work/logs/sdk_build.log`. Primeira suspeita: versão do clang (`CC`/`CXX`) ou o
  `fetch_thirdparty` incompleto (apague `work/sa2-nx/sdk/thirdparty/.fnx_fetched` e rode `deps` de novo).
- `codegen` falha ao carregar o XEX: confira o `identify` (é mesmo um XEX2?) e se o arquivo é o `default.xex` do
  disco 1.
- Qualquer outra coisa: registre a saída exata em `perguntas.md`, como uma pergunta nova.
