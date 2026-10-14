## Formatting

This project uses Prettier for JSON and GLSL-related shader files. Install the
JavaScript dependencies once, then use the formatting scripts:

```sh
npm install
npm run format
```

The VS Code Prettier extension uses the local configuration and
`prettier-plugin-glsl` automatically. Shader files use the `glsl` language mode
for `.glsl`, `.fsh`, and `.vsh` extensions.

The project formatter uses `format.mjs` to temporarily replace Minecraft's
namespace include syntax (``#include <namespace:file.glsl>``) with parser-safe
placeholders. It applies the same approach to GLSL array constructors such as
`vec3[](`, which the plugin does not parse. It restores the original syntax
after formatting, so the files remain compatible with Minecraft.