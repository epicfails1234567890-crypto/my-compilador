const fs = require("fs");
const readline = require("readline");

function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

async function ejecutar(codigo, vars = {}) {
  const lineas = codigo.split(/\r?\n/).map(l => l.trim()).filter(l => l);
  const etiquetas = {};
  let i = 0, pasos = 0;
  const MAX_PASOS = 10000000;

  // indexar etiquetas
  lineas.forEach((l, idx) => {
    if (l.endsWith(":")) etiquetas[l.slice(0, -1).trim()] = idx;
  });

  while (i < lineas.length && pasos < MAX_PASOS) {
    pasos++;
    let l = lineas[i];
    if (!l || l.endsWith(":")) { i++; continue; }

    // imprimir
    if (l.startsWith("imprimir")) {
      let expr = l.slice("imprimir".length).trim();
      try {
        const val = new Function("v", "with(v){ return " + expr + "}")(vars);
        console.log(val);
      } catch (e) {
        console.log("Error imprimir:", e.message);
      }
      i++; continue;
    }

    // leer
    if (l.startsWith("leer")) {
      let variable = l.slice("leer".length).trim();
      const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
      await new Promise(resolve => {
        rl.question(``, respuesta => {
          const num = Number(respuesta);
          vars[variable] = Number.isNaN(num) ? respuesta : num;
          rl.close(); resolve();
        });
      });
      i++; continue;
    }

    // ir (goto en español)
    if (l.startsWith("ir ")) {
      const resto = l.slice(3).trim();
      const partes = resto.split(/\s+si\s+/i);
      const destino = (partes[0] || "").trim();
      const condicion = (partes[1] || "").trim();
      let ok = true;
      if (condicion) {
        try { ok = !!new Function("v", "with(v){ return " + condicion + "}")(vars); }
        catch (e) { console.log("Error condición:", e.message); ok = false; }
      }
      if (ok && destino in etiquetas) { i = etiquetas[destino]; continue; }
      i++; continue;
    }

    // asignaciones (evaluar expresiones)
    if (l.startsWith("declarar ")) {
      let resto = l.slice("declarar".length).trim();
      try {
        new Function("v", "with(v){ " + resto + "}")(vars);
      } catch (e) {
        console.log("Error declarar:", e.message);
      }
      i++; continue;
    }

    // sleep
    if (l.startsWith("dormir")) {
      let ms = Number(l.slice("dormir".length).trim());
      if (Number.isNaN(ms)) ms = 1000; // valor por defecto 1 segundo
      await sleep(ms);
      i++; continue;
    }

    i++;
  }

  if (pasos >= MAX_PASOS) console.log("[Parado por límite de pasos]");
}

// --- Punto de entrada ---
(async () => {
  const [,, archivo, ...args] = process.argv;
  if (!archivo) {
    console.error("Uso: node interprete_completo.js <fichero> [args...]");
    process.exit(1);
  }

  try {
    const codigo = fs.readFileSync(archivo, "utf8");

    // convertir args en variables iniciales
    let vars = {};
    args.forEach((val, idx) => {
      const num = Number(val);
      vars["arg" + idx] = Number.isNaN(num) ? val : num;
    });

    await ejecutar(codigo, vars);
  } catch (e) {
    console.error("Error leyendo fichero:", e.message);
  }
})();
