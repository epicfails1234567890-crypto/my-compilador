// ejecutor.js
const fs = require("fs");
const prompt = require("prompt-sync")();

// Traductor extendido de "JS en español"
function traducir(codigo) {
  return codigo
    // Variables y constantes
    .replace(/\bvariable\b/g, "let")
    .replace(/\bconstante\b/g, "const")

    // Control de flujo con condiciones sin paréntesis
    .replace(/\bsino si\b/g, "else if")
    .replace(/\bsino\b/g, "else")
    .replace(/\bsi\s+([^{]+)\{/g, "if ($1){")
    .replace(/\bmientras\s+([^{]+)\{/g, "while ($1){")
    .replace(/\bpara\s+([^{]+)\{/g, "for ($1){")
    .replace(/\bcontinuar\b/g, "continue")
    .replace(/\bfin\b/g, "break")

    // Funciones
    .replace(/\bfuncion\b/g, "function")
    .replace(/\bretornar\b/g, "return")

    // Booleanos
    .replace(/\bverdadero\b/g, "true")
    .replace(/\bfalso\b/g, "false")

    // Entrada/salida
    .replace(/\bpregunta\((.*?)\)/g, "prompt($1)")
    .replace(/\bimprimir\((.*?)\)/g, "console.log($1)")

    // Comparaciones
    .replace(/\bes igual a\b/g, "==")
    .replace(/\bes distinto de\b/g, "!=")
    .replace(/\bmayor que\b/g, ">")
    .replace(/\bmenor que\b/g, "<")
    .replace(/\bmayor o igual que\b/g, ">=")
    .replace(/\bmenor o igual que\b/g, "<=")

    // Operadores lógicos
    .replace(/\by\b/g, "&&")
    .replace(/\bo\b/g, "||")
    .replace(/\bno\b/g, "!")

    // Tipos y conversiones
    .replace(/\bnumero\b/g, "Number")
    .replace(/\btexto\b/g, "String")
    .replace(/\bbooleano\b/g, "Boolean")

    // Estructuras
    .replace(/\barreglo\b/g, "Array")
    .replace(/\bdiccionario\b/g, "Object");
}

// Ejecuta código traducido
function ejecutar(codigoEsp) {
  const codigoJS = traducir(codigoEsp);
  try {
    return eval(codigoJS);
  } catch (err) {
    console.error("⚠️ Error en tu código:", err.message);
  }
}

// Si hay archivo, lo ejecuta primero
const archivo = process.argv[2];
if (archivo) {
  const codigoEsp = fs.readFileSync(archivo, "utf8");
  //console.log("=== Código traducido del archivo ===\n" + traducir(codigoEsp));
  ejecutar(codigoEsp);
}

// Prompt interactivo
while (true) {
  const linea = prompt("JS-ES> ");
  if (!linea) break;
  const resultado = ejecutar(linea);
  if (resultado !== undefined) console.log("=>", resultado);
}
