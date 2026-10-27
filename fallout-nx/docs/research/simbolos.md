# Dar nome às funções do `default.xex`

Um XEX de varejo não tem símbolos: o gerador chama tudo de `sub_82XXXXXX`. Nomear as funções quentes e as que
travam é o que torna os logs, os crashes e os profiles legíveis. Modelo deste documento:
`work/refs/masseffect-nx/docs/external-function-maps.md`.

## Política de proveniência (decisão D2)

Entram neste repositório nomes e layouts vindos **apenas** de:

1. strings e estruturas do **seu próprio** executável (RTTI, nomes de classe da Gamebryo, tabelas, mensagens de log);
2. projetos públicos de código aberto (xNVSE, FOSE, patches do Xenia, wikis de formato);
3. a nossa própria análise.

**Não entram** material de devkits, builds de desenvolvimento ou PDBs que circulam por aí. **[relatado]** Em 2025
apareceram protótipos do FNV de devkits do 360 com PDBs, e há relatos de um banco IDA "completo" do FO3/NV feito a
partir deles. É material proprietário não publicado: usá-lo contamina a proveniência do projeto. Se alguém sugerir,
a resposta é não, e nomes derivados dele não entram nem em comentários.

O mesmo vale para o Skyrim do Switch: só observação externa do jogo rodando. Nada de dump do cartucho.

## Fontes, por ordem de retorno

| # | Fonte | O que dá | Como chega aos endereços do 360 | Retorno |
|---|---|---|---|---|
| 1 | **RTTI do MSVC no próprio XEX** | Nomes de classe (`.?AVNiNode@@`) ligados aos *Complete Object Locators* e, a partir deles, às vtables | String → type descriptor → COL → vtable → cada slot é um método da classe | Alto, se o RTTI estiver ligado (P-08) |
| 2 | **NiRTTI da Gamebryo** | Cada classe `Ni*` registra o próprio nome numa `NiRTTI` estática | xrefs para a string → construtor estático → `GetRTTI` virtual → vtable | Alto |
| 3 | **Tabela de comandos de script** | Nome, opcode e handler de cada comando de console/script (`GetPos`, `SetAV`...) | Achar a tabela por uma string conhecida e ler os ponteiros dos handlers | Alto e barato: centenas de nomes de uma vez |
| 4 | **xNVSE** (`work/refs/xnvse/nvse/nvse/`) | Layouts de classe (`GameForms.h`, `GameObjects.h`, `NiNodes.h`), lista de RTTI (`GameRTTI_1_4_0_525.inc`), semântica dos comandos | Não bate byte a byte (x86, outra build). Serve de **dicionário de nomes e de ordem de membros** para reconhecer offsets de `this` no PPC | Alto para o FNV, médio para o FO3 |
| 5 | **FOSE** (o script extender do FO3, da mesma equipe do NVSE) | O mesmo para o FO3 PC | idem | Médio (P-09: achar o código-fonte público) |
| 6 | **Strings de assert, de log e de settings** | `Gamebryo` e a Bethesda deixam nomes de arquivo `.cpp` e nomes de settings (`iGridsToLoad`, `fLODDist...`) | xrefs → a função que lê o setting é a que ele controla | Médio-alto. Os settings dão as chaves de qualidade do Marco 5 |
| 7 | **Patches do Xenia para Oblivion e Skyrim** (`work/refs/xenia-game-patches/`) | Endereços confirmados de limitador de FPS, resolução e MSAA nos irmãos de engine | Não transferem os endereços; transferem **o padrão** de código a procurar no FO3 | Baixo-médio |
| 8 | **UESP, GECK wiki** | Formatos (BSA, ESM, NIF), semântica de forms e records | Reconhecer os parsers pelas constantes de formato (`'TES4'`, versões de BSA) | Médio, na fase de carregamento |

## Uma etapa da pipeline para isso (Marco 1)

`./pipeline.sh symbols <jogo>` (a implementar):

1. ler `work/<jogo>/default_image.bin` (escrito pelo `codegen`);
2. extrair os type descriptors de RTTI, as strings de NiRTTI e a tabela de comandos;
3. gerar `games/<jogo>/overrides_names.toml` com `[functions] 0x82... = { name = "NiNode::UpdateWorldData" }` e um
   comentário de evidência por linha ("vtable de NiNode em 0x82..., slot 12; RTTI em 0x82...");
4. incluir esse arquivo no manifesto. Nomes não mudam o código gerado, só os símbolos: dá para regenerar sem risco.

O nome nunca é "certeza" só porque veio do xNVSE: a evidência tem de estar no XEX (RTTI, string ou xref).
