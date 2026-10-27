# Perguntas abertas

Cada pergunta tem um método e um lugar para a resposta. Ao responder, escreva a data, a evidência (comando, log,
endereço) e atualize a marcação correspondente nas outras páginas. Não apague perguntas respondidas: elas são o
histórico do projeto.

Status: `aberta` · `em andamento` · `respondida` · `descartada`

## Marco 0–1: o executável

**P-01: Quais builds temos?** `aberta`
Método: `./pipeline.sh extract` + `identify` nos discos de FO3 e FNV (e no disco 2 da GOTY/Ultimate, se houver).
Resposta em: `games/*/versions.toml` (`[[build]]`), e a confirmação dos title ids em `alvo-fallout.md`.

**P-02: Há title update? Usamos com ou sem?** `aberta`
Método: verificar se os TUs (`.xexp`) estão disponíveis na sua cópia; comparar `version` antes e depois.
Decisão: recomendo a build com o TU mais recente (é a que os jogadores usavam), fixada até o Marco 4.

**P-03: O que o Xenia Canary faz com esta build?** `aberta`
Método: rodar o disco no Xenia Canary (Windows/Linux), anotar até onde vai, e consultar o rastreador de
compatibilidade. Procurar especificamente o loop em `KeReleaseSemaphore` relatado.
Resposta em: `alvo-fallout.md`, seção Xenia. Esse comportamento é o nosso primeiro alvo de boot.

**P-04: O `codegen` converge?** `aberta`
Método: `./pipeline.sh codegen` sem `--force`; contar os erros de validação por classe; rodar `audit`.
Resposta em: o log em `work/logs/` (não versionado) e um resumo com os números neste arquivo.

**P-07: Como são os BSAs do 360?** `aberta`
Método: abrir um `.bsa` do seu disco com um leitor que suporte as flags do 360; conferir as flags `0x40`/`0x200`,
a ordem dos bytes, e se as texturas têm tiling ou byte-swap.
Resposta em: `alvo-fallout.md`, seção Formatos.

**P-08: O XEX tem RTTI do MSVC?** `aberta`
Método: procurar `.?AV` no `default_image.bin` (`grep -c -a '\.?AV'`). Se houver, seguir um type descriptor até a
vtable e validar contra uma vtable conhecida (ex.: `NiNode`).
Resposta em: `simbolos.md`, fonte #1.

**P-09: Onde está o código-fonte público do FOSE?** `aberta`
Método: pesquisa (silverlock.org, GitHub). Fixar em `refs.lock` se for público e tiver licença clara.

**P-10: Onde está a tabela de comandos de script no XEX?** `aberta`
Método: procurar strings de comando conhecidas (`GetPos`, `SetActorValue`, `ToggleGodMode`), seguir as xrefs até a
tabela de structs e ler o layout comparando com `CommandInfo` do xNVSE (`CommandTable.h`).

## Marco 2: boot

**P-11: sa2-nx ou masseffect-nx como base do SDK?** `aberta`
Método: `diff -r` dos dois `sdk/` (ambos sobre ReXGlue `c94f5eb`); listar o que cada um tem que o outro não tem
(I/O, disco, memória, D3D). Decisão D1 em `precedentes.md`.

**P-12: Como o jogo carrega os DLCs?** `aberta`
Método: imports `XamContent*`; strings dos `.esm` dos DLCs; ver `docs/dlc.md` do masseffect-nx e `sa2_dlc.cpp`.

**P-13: Quais módulos extras de código existem?** `aberta`
Método: listar todos os `.xex`/`.xexp` do disco; procurar `XexLoadImage` nos imports.

## Marco 4–5: desempenho

**P-05: Quanto custa a thread principal?** `aberta`
Método: profiler do SDK no console, clock padrão, cena fixa (P-06). Métrica: core-ms por frame por thread.
Comparar com o ME1 (thread principal ~30 ms; frame 100–142 core-ms).

**P-06: Qual é a cena de teste?** `aberta`
Proposta: um save na saída do Vault 101 (exterior aberto, streaming de células) e outro dentro de Megaton (muitos
atores). Rota fixa repetível. No FNV: Goodsprings → Primm.

**P-14: Quais settings de qualidade existem no XEX?** `aberta`
Método: strings `i*`, `f*`, `b*` de settings (`iGridsToLoad`, `fShadowLODDist`...); valores padrão do 360; comparar
com os `.ini` do PC.
Resposta em: uma tabela nova em `alvo-fallout.md`. Vira a lista de chaves do Marco 5.
