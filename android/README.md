# Anima-me (app Android)

Editor de desenho e animação 2D para Android, inspirado em Ibis Paint X, FlipaClip,
RoughAnimator e (numa direção mais ousada) Procreate Dreams — mas com motor de
desenho, pincéis e formato de dados **próprios e originais**.

## Arquitetura

- `MainActivity.kt` — tela principal: canvas, barra de ferramentas, timeline de frames.
- `SettingsActivity.kt` / `ToolOptionsActivity.kt` — tema e opções de pincel (tamanho,
  opacidade, flow, espaçamento, suavização, pressão, rotação, cor), persistidas em
  `BrushToolState`.
- `editor/BrushEngine.kt` — motor procedural de traço: pressão, jitter, dispersão,
  rotação, textura (ruído procedural, sem bitmap), água, ar, fade, estabilização.
- `editor/BrushCatalog.kt` + `editor/BrushLibraryV2.kt` — catálogo de pincéis: uma
  lista curada (nomes/categorias) e uma matriz procedural combinatória adicional.
  `editor/BrushDefaults.forNamedBrush()` deriva o comportamento real (material,
  textura, água, dispersão, forma de carimbo) a partir da categoria + palavras-chave
  do nome — é o que faz cada pincel ter, de fato, uma característica própria em vez
  de todos caírem no mesmo pincel de tinta genérico.
- `editor/StampShapes.kt` — silhuetas reais (estrela, coração, losango, floco de
  neve, pegada, folha, gota, cruz) para pincéis de carimbo/decoração, desenhadas
  proceduralmente com `Path` — sem nenhuma imagem importada.
- `editor/LiquifyEngine.kt`, `editor/FillBucketEngine.kt`, `editor/AnimationModel.kt`,
  `editor/AnimationCameraController.kt` — liquify, balde de tinta, modelo de frames/
  camadas, controlador de câmera para playback.
- `editor/BrushQrImporter.kt` + `editor/BrushQrCodec.kt` — importação de pincel via
  QR code: formato nativo Anima-me (texto, com checksum) e leitura da *estrutura de
  contêiner* do formato IPBZ (magic bytes, chunks, deflate) para compatibilidade —
  sem decodificar os parâmetros internos proprietários do pincel.

## Limite deliberado sobre o Ibis Paint X

O app busca paridade de **funcionalidade** com apps de referência, não cópia de
**ativos**. Isso significa: não importamos texturas/bitmaps de pincel de terceiros,
não replicamos o layout binário proprietário do formato de pincel de nenhum app, e
o catálogo de nomes de pincéis (`BrushCatalog.kt`) usa nomenclatura própria — ainda
que várias entradas históricas do catálogo estejam com nomes muito próximos aos de
um app concorrente e precisem de uma passada de renomeação (ver "Pendências").

## Pendências conhecidas

- Renomear as entradas do `BrushCatalog.kt` que ainda espelham de forma muito
  próxima o catálogo de nomes de outro app (a função/categoria pode ficar igual,
  só o texto do nome precisa mudar).
- "Desenhar dentro" (clipar o traço ao conteúdo já pintado) está persistido em
  `BrushToolState.drawsInside` mas ainda não afeta a renderização — precisa de
  acesso ao raster da camada atual para recorte por pixel.
- Decodificação real dos parâmetros do formato IPBZ (não apenas a estrutura do
  contêiner) — deliberadamente não implementada por ser o miolo proprietário do
  formato de outro app.
- Rotação real de câmera/cena ao estilo RoughAnimator/FlipaClip (hoje o app já
  gira/zoom/pan o viewport a dois dedos; falta persistir "posições de câmera" por
  frame como palavras-chave de animação de câmera).
