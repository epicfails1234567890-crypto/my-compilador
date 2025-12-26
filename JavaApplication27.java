package javaapplication27;

import java.io.IOException;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.Scanner;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaApplication27 {

    // Variables escalares
    private static final HashMap<String, BigDecimal> vars1 = new HashMap<>();
    // Arrays: nombre -> (indice entero -> valor BigDecimal)
    private static final HashMap<String, HashMap<Integer, BigDecimal>> arrays = new HashMap<>();

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java -jar JavaApplication25.jar <archivo.txt>");
            return;
        }

        String codigo;
        try {
            // Normaliza saltos y espacios
            codigo = Files.readString(Paths.get(args[0])).replace("\r", "");
        } catch (IOException ex) {
            Logger.getLogger(JavaApplication27.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        // Limpia estado por ejecución
        vars1.clear();
        arrays.clear();

        int precision = 100;
        Random rand = new Random();
        Scanner scanner = new Scanner(System.in);

        // Divide por ';' y limpia
        String[] partesRaw = codigo.split(";");
        String[] partes = java.util.Arrays.stream(partesRaw)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        // Etiquetas
        HashMap<String, Integer> etiquetas = new HashMap<>();
        for (int i = 0; i < partes.length; i++) {
            String s = partes[i];
            if (s.startsWith(":")) {
                String[] partesEtiqueta = s.substring(1).trim().split("\\s+", 2);
                String nombreEtiqueta = partesEtiqueta[0];
                etiquetas.put(nombreEtiqueta, i);
                partes[i] = (partesEtiqueta.length > 1) ? partesEtiqueta[1] : "";
            }
        }

        // PC loop
        int pc = 0;
        while (pc < partes.length) {
            String s = partes[pc].trim();
            if (s.isEmpty()) { pc++; continue; }

            if (s.startsWith("ajustar ")) {
                precision = parseIntSafe(s.substring(8).trim(), precision);
            }

            else if (s.startsWith("escribir ")) {
                System.out.print(s.substring(9));
            }
            
            else if (s.startsWith("dormir ")) {
                String tiempo = s.substring(7).trim();
                try {
                    int ms = Integer.parseInt(tiempo); // más directo que evalInt
                    Thread.sleep(ms);
                } catch (NumberFormatException e) {
                    System.err.println("Error: dormir requiere un número entero → " + tiempo);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // declarar nombre valor X | declarar arr[idx] valor X
            else if (s.startsWith("declarar ")) {
                String[] trozos = s.split("\\s+valor\\s+");
                if (trozos.length == 2) {
                    String destino = trozos[0].substring(9).trim();
                    BigDecimal valor = evalBD(trozos[1].trim(), precision);

                    if (isArrayAccess(destino)) {
                        String nombreArray = arrayName(destino);
                        int indice = evalInt(arrayIndex(destino), precision);
                        arrays.computeIfAbsent(nombreArray, k -> new HashMap<>()).put(indice, valor);
                    } else {
                        vars1.put(destino, valor);
                    }
                }
            }

            // mostrar nombre | mostrar arr[idx]
            else if (s.startsWith("mostrar ")) {
                String token = s.substring(8).trim();
                if (isArrayAccess(token)) {
                    String nombreArray = arrayName(token);
                    int indice = evalInt(arrayIndex(token), precision);
                    HashMap<Integer, BigDecimal> arr = arrays.get(nombreArray);
                    BigDecimal v = (arr != null) ? arr.get(indice) : null;
                    if (v != null) System.out.print(toPlain(v));
                } else {
                    BigDecimal v = evalBD(token, precision);
                    System.out.print(toPlain(v));
                }
            }

            // leer DEST (DEST puede ser variable escalar o arr[idx])
            else if (s.startsWith("leer ")) {
                String dest = s.substring(5).trim();
                String texto = scanner.nextLine();

                // Permite números, expresiones y también cadenas literales entre comillas
                BigDecimal valor;
                String t = texto.trim();
                if (t.startsWith("\"") && t.endsWith("\"")) {
                    // Si tu lenguaje soporta strings, guarda aparte; si no, intenta numérico
                    // Aquí intentamos numérico por compatibilidad:
                    valor = evalBD(t.substring(1, t.length() - 1), precision);
                } else {
                    valor = evalBD(t, precision);
                }

                if (isArrayAccess(dest)) {
                    String nombreArray = arrayName(dest);
                    int indice = evalInt(arrayIndex(dest), precision);
                    arrays.computeIfAbsent(nombreArray, k -> new HashMap<>())
                          .put(indice, valor);
                } else {
                    vars1.put(dest, valor);
                }
            }

            // truncar DEST (DEST puede ser variable escalar o arr[idx])
            else if (s.startsWith("truncar ")) {
                String dest = s.substring(8).trim();

                if (isArrayAccess(dest)) {
                    String nombreArray = arrayName(dest);
                    int indice = evalInt(arrayIndex(dest), precision);
                    HashMap<Integer, BigDecimal> arr = arrays.get(nombreArray);
                    if (arr != null) {
                        BigDecimal v = arr.get(indice);
                        if (v != null) {
                            arr.put(indice, v.setScale(0, RoundingMode.DOWN));
                        }
                    }
                } else {
                    BigDecimal v = vars1.get(dest);
                    if (v != null) {
                        vars1.put(dest, v.setScale(0, RoundingMode.DOWN));
                    }
                }
            }


            // aleatorizar variable escalar
            else if (s.startsWith("aleatorizar ")) {
                String destino = s.substring(12).trim();
                BigDecimal valorAleatorio = BigDecimal.valueOf(rand.nextDouble());

                if (isArrayAccess(destino)) {
                    String nombreArray = arrayName(destino);
                    int indice = evalInt(arrayIndex(destino), precision);
                    arrays.computeIfAbsent(nombreArray, k -> new HashMap<>()).put(indice, valorAleatorio);
                } else {
                    vars1.put(destino, valorAleatorio);
                }
            }

            // incrementar nombre | incrementar arr[idx]
            else if (s.startsWith("incrementar ")) {
                String destino = s.substring(12).trim();
                if (isArrayAccess(destino)) {
                    String nombreArray = arrayName(destino);
                    int indice = evalInt(arrayIndex(destino), precision);
                    HashMap<Integer, BigDecimal> arr = arrays.computeIfAbsent(nombreArray, k -> new HashMap<>());
                    BigDecimal val = arr.getOrDefault(indice, BigDecimal.ZERO);
                    arr.put(indice, val.add(BigDecimal.ONE));
                } else {
                    BigDecimal inc = vars1.getOrDefault(destino, BigDecimal.ZERO);
                    vars1.put(destino, inc.add(BigDecimal.ONE));
                }
            }

            // decrementar nombre | decrementar arr[idx]
            else if (s.startsWith("decrementar ")) {
                String destino = s.substring(12).trim();
                if (isArrayAccess(destino)) {
                    String nombreArray = arrayName(destino);
                    int indice = evalInt(arrayIndex(destino), precision);
                    HashMap<Integer, BigDecimal> arr = arrays.computeIfAbsent(nombreArray, k -> new HashMap<>());
                    BigDecimal val = arr.getOrDefault(indice, BigDecimal.ZERO);
                    arr.put(indice, val.subtract(BigDecimal.ONE));
                } else {
                    BigDecimal dec = vars1.getOrDefault(destino, BigDecimal.ZERO);
                    vars1.put(destino, dec.subtract(BigDecimal.ONE));
                }
            }
            // sumar DEST mas EXPR  (DEST puede ser var o arr[idx])
            else if (s.startsWith("sumar ")) {
                String[] p = s.substring(6).split("\\s+mas\\s+");
                if (p.length == 2) {
                    String dest = p[0].trim();
                    BigDecimal rhs = evalBD(p[1].trim(), precision);

                    if (isArrayAccess(dest)) {
                        String nombreArray = arrayName(dest);
                        int indice = evalInt(arrayIndex(dest), precision);
                        HashMap<Integer, BigDecimal> arr = arrays.computeIfAbsent(nombreArray, k -> new HashMap<>());
                        BigDecimal cur = arr.getOrDefault(indice, BigDecimal.ZERO);
                        arr.put(indice, cur.add(rhs));
                    } else {
                        BigDecimal cur = vars1.getOrDefault(dest, BigDecimal.ZERO);
                        vars1.put(dest, cur.add(rhs));
                    }
                }
            }

            // restar DEST menos EXPR  (DEST puede ser var o arr[idx])
            else if (s.startsWith("restar ")) {
                String[] p = s.substring(7).split("\\s+menos\\s+");
                if (p.length == 2) {
                    String dest = p[0].trim();
                    BigDecimal rhs = evalBD(p[1].trim(), precision);

                    if (isArrayAccess(dest)) {
                        String nombreArray = arrayName(dest);
                        int indice = evalInt(arrayIndex(dest), precision);
                        HashMap<Integer, BigDecimal> arr = arrays.computeIfAbsent(nombreArray, k -> new HashMap<>());
                        BigDecimal cur = arr.getOrDefault(indice, BigDecimal.ZERO);
                        arr.put(indice, cur.subtract(rhs));
                    } else {
                        BigDecimal cur = vars1.getOrDefault(dest, BigDecimal.ZERO);
                        vars1.put(dest, cur.subtract(rhs));
                    }
                }
            }

            // multiplicar DEST por EXPR  (DEST puede ser var o arr[idx])
            else if (s.startsWith("multiplicar ")) {
                String[] p = s.substring(12).split("\\s+por\\s+");
                if (p.length == 2) {
                    String dest = p[0].trim();
                    BigDecimal rhs = evalBD(p[1].trim(), precision);

                    if (isArrayAccess(dest)) {
                        String nombreArray = arrayName(dest);
                        int indice = evalInt(arrayIndex(dest), precision);
                        HashMap<Integer, BigDecimal> arr = arrays.computeIfAbsent(nombreArray, k -> new HashMap<>());
                        BigDecimal cur = arr.getOrDefault(indice, BigDecimal.ZERO);
                        arr.put(indice, cur.multiply(rhs, new MathContext(precision)));
                    } else {
                        BigDecimal cur = vars1.getOrDefault(dest, BigDecimal.ZERO);
                        vars1.put(dest, cur.multiply(rhs, new MathContext(precision)));
                    }
                }
            }

            // dividir DEST entre EXPR  (DEST puede ser var o arr[idx])
            else if (s.startsWith("dividir ")) {
                String[] p = s.substring(8).split("\\s+entre\\s+");
                if (p.length == 2) {
                    String dest = p[0].trim();
                    BigDecimal rhs = evalBD(p[1].trim(), precision);

                    if (isArrayAccess(dest)) {
                        String nombreArray = arrayName(dest);
                        int indice = evalInt(arrayIndex(dest), precision);
                        HashMap<Integer, BigDecimal> arr = arrays.computeIfAbsent(nombreArray, k -> new HashMap<>());
                        BigDecimal cur = arr.getOrDefault(indice, BigDecimal.ZERO);
                        arr.put(indice, cur.divide(rhs, new MathContext(precision)));
                    } else {
                        BigDecimal cur = vars1.getOrDefault(dest, BigDecimal.ZERO);
                        vars1.put(dest, cur.divide(rhs, new MathContext(precision)));
                    }
                }
            }


            // resto de DEST entre EXPR  (DEST puede ser variable escalar o arr[idx])
            else if (s.startsWith("resto de ")) {
                String[] p = s.substring(9).split("\\s+entre\\s+");
                if (p.length == 2) {
                    String dest = p[0].trim();
                    BigDecimal rhs = evalBD(p[1].trim(), precision);

                    // Evitar remainder con 0
                    if (rhs.compareTo(BigDecimal.ZERO) == 0) {
                        // Opcional: mantener el valor sin cambios o registrar error
                        // System.err.println("Intento de 'resto' con divisor 0: " + s);
                    } else if (isArrayAccess(dest)) {
                        String nombreArray = arrayName(dest);
                        int indice = evalInt(arrayIndex(dest), precision);
                        HashMap<Integer, BigDecimal> arr = arrays.computeIfAbsent(nombreArray, k -> new HashMap<>());
                        BigDecimal cur = arr.getOrDefault(indice, BigDecimal.ZERO);

                        BigDecimal res = cur.remainder(rhs, new MathContext(precision));
                        arr.put(indice, res);
                    } else {
                        BigDecimal cur = vars1.getOrDefault(dest, BigDecimal.ZERO);
                        BigDecimal res = cur.remainder(rhs, new MathContext(precision));
                        vars1.put(dest, res);
                    }
                }
            }


            // ir etiqueta
            else if (s.startsWith("ir ")) {
                String nombreEtiqueta = s.substring(3).trim();
                Integer destino = etiquetas.get(nombreEtiqueta);
                if (destino != null) { pc = destino; continue; }
            }

            // si <var|arr[idx]> <op> <valor|var|arr[idx]> ir <etiqueta>
            // si CONDICION ir ETIQUETA
            else if (s.startsWith("si ")) {
                String resto = s.substring(3).trim();
                String[] partesIf = resto.split("\\s+ir\\s+");
                if (partesIf.length == 2) {
                    String condicion = partesIf[0].trim();
                    String destinoEtiqueta = partesIf[1].trim();

                    // Permite expresiones tipo "a < b", "pos[x] == 3", etc.
                    String[] tokens = condicion.split("\\s+");
                    if (tokens.length == 3) {
                        BigDecimal izq = evalBD(tokens[0], precision);
                        String op = tokens[1];
                        BigDecimal der = evalBD(tokens[2], precision);

                        if (compara(izq, op, der)) {
                            Integer destino = etiquetas.get(destinoEtiqueta);
                            if (destino != null) {
                                pc = destino;
                                continue;
                            }
                        }
                    } else {
                        // Opcional: log de error si la condición no tiene formato válido
                        // System.err.println("Condición mal formada en 'si': " + condicion);
                    }
                }
            }


            pc++;
        }

        System.out.println(); // salto final
    }

    // -------- Utilidades --------

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    // Eval BD: variable, literal, o arr[idx]
    private static BigDecimal evalBD(String token, int precision) {
        token = token.trim();
        if (isArrayAccess(token)) {
            String nombreArray = arrayName(token);
            int indice = evalInt(arrayIndex(token), precision);
            HashMap<Integer, BigDecimal> arr = arrays.get(nombreArray);
            BigDecimal v = (arr != null) ? arr.get(indice) : null;
            return (v != null) ? v : BigDecimal.ZERO;
        }
        BigDecimal var = vars1.get(token);
        if (var != null) return var;
        try { return new BigDecimal(token); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    // Eval índice como entero robusto
    private static int evalInt(String token, int precision) {
        BigDecimal v = evalBD(token, precision);
        try {
            return v.intValueExact(); // fuerza entero exacto
        } catch (ArithmeticException ex) {
            return v.setScale(0, RoundingMode.DOWN).intValue(); // fallback
        }
    }

    private static boolean isArrayAccess(String s) {
        int i = s.indexOf('['), j = s.indexOf(']');
        return i >= 0 && j > i;
    }

    private static String arrayName(String s) {
        int i = s.indexOf('[');
        return (i >= 0) ? s.substring(0, i).trim() : s.trim();
    }

    private static String arrayIndex(String s) {
        int i = s.indexOf('['), j = s.indexOf(']');
        return s.substring(i + 1, j).trim();
    }

    private static boolean compara(BigDecimal a, String op, BigDecimal b) {
        int cmp = a.compareTo(b);
        switch (op) {
            case "<":  return cmp < 0;
            case ">":  return cmp > 0;
            case "==": return cmp == 0;
            case "!=": return cmp != 0;
            case ">=": return cmp >= 0;
            case "<=": return cmp <= 0;
            default:   return false;
        }
    }

    private static String toPlain(BigDecimal v) {
        BigDecimal t = v.stripTrailingZeros();
        return t.toPlainString();
    }
}
