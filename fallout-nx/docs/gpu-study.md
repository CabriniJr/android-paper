# Trilha de estudo: chips gráficos

Cada tópico está ligado ao marco em que ele aparece na prática. A ideia é ler pouco antes de precisar e escrever
uma nota curta (aqui ou em `docs/notes/`) com o que foi medido, não só com o que foi lido.

## Os dois chips

| | Xenos (Xbox 360, 2005) | Maxwell GM20B (Tegra X1, Switch) |
|---|---|---|
| Shaders | 48 ALUs unificadas (vec4+escalar), ~500 MHz | 256 núcleos CUDA (2 SMs), 307–768 MHz |
| Framebuffer | **10 MB de eDRAM** com ROPs embutidos (blend, Z, MSAA "de graça") | sem eDRAM; L2 + rasterização em tiles + compressão de cor |
| Memória | 512 MB GDDR3 unificada, ~22 GB/s | 4 GB LPDDR4 unificada, ~21–25 GB/s |
| Saída do framebuffer | *resolve*: cópia explícita da eDRAM para textura | render target é só memória |
| Shaders | microcódigo Xenos (dentro de "containers" D3D) | SASS Maxwell (via NAK/NVK a partir de SPIR-V) |

Números aproximados; confirme cada um na fonte primária antes de usar em medições.

**A pergunta central do projeto:** o FO3 foi feito supondo que blend, profundidade e MSAA na eDRAM custam quase nada.
No Switch, todo esse tráfego passa pela LPDDR4. Onde isso estoura a banda, e o que o Maxwell (tiled caching, delta
color compression, ZCULL) recupera?

## Marco 0–1: o pipeline gráfico

- Fabian Giesen (ryg), *A trip through the Graphics Pipeline 2011* (série de blog): do driver ao ROP.
- *Real-Time Rendering*, 4ª ed.: caps. 2–3 (pipeline e GPU) e 23 (arquitetura de hardware).

## Marco 2–3: Xenos por dentro

- Código do Xenia: `src/xenia/gpu/` (registradores, PM4, formatos de textura e tiling). Os comentários do render
  target cache sobre a eDRAM são a melhor documentação pública dela.
- sa2-nx `docs/direct-renderer.md`, `docs/api-renderer.md`: como o anel PM4 vira Vulkan.
- XenosRecomp (sa2-nx `shaders/`): ler o HLSL gerado lado a lado com a desmontagem do microcódigo.
- Exercício: capturar um frame do FO3 e listar cada render target, cada resolve e o custo estimado em bytes.

## Marco 4–5: Maxwell e o Tegra X1

- Whitepaper do NVIDIA Tegra X1 e o *GeForce GTX 980 whitepaper* (arquitetura Maxwell).
- envytools/nouveau: documentação aberta dos registradores e do formato de comandos das GPUs NVIDIA.
- Mesa NVK/NAK: como o SPIR-V vira SASS; o patch do nfsmw-nx (ZCULL, custo de draw) como estudo de caso.
- deko3d: a API de baixo nível do homebrew para essa GPU (um triângulo e depois um cubo, como aquecimento).
- sa2-nx `docs/gpu-optimisation-candidates.md`: 800 linhas de medições reais nesse chip.
- Exercício: o mesmo frame do marco 3, agora medido com os timestamps de GPU do renderer, por categoria de passe.

## Hábito

Toda sessão de estudo termina com uma pergunta medível ("quantos MB por frame o resolve do HDR move?") e, na próxima
sessão, com a resposta medida.
