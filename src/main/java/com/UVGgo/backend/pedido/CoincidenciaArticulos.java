package com.UVGgo.backend.pedido;

import com.UVGgo.backend.domain.Articulo;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

// Busca en el catálogo el artículo que corresponde a un material escrito en texto libre (p. ej. leído de un PDF).
// Va por orden de confianza: código de parte exacto (74LS08N) y, si no hay, parecido de palabras y trigramas.
// Ante la duda no se enlaza: es mejor que el material siga como "del PDF" a cotizar el artículo equivocado.
final class CoincidenciaArticulos {
    // Desde este puntaje el ítem se enlaza solo; entre POSIBLE y SEGURA solo se sugiere al administrador
    static final double SEGURA=0.8, POSIBLE=0.5;
    private static final Set<String> VACIAS=Set.of("de","del","la","el","los","las","y","o","para","con","en","un","una","por","al","x","tipo","su");
    // Código de parte: mezcla letras y números (74LS08N, NE555N, MB102)
    private static final Pattern CODIGO=Pattern.compile("(?=[a-z0-9-]*\\d)(?=[a-z0-9-]*[a-z])[a-z0-9-]{4,}");
    // Valores con unidad (100nf, 22awg, 5v) parecen códigos, pero describen muchos artículos distintos
    private static final Pattern VALOR=Pattern.compile("\\d+(k|m|u|n|p)?(ohm|f|h|v|a|w|mm|cm|awg|hz|mah)?");

    record Resultado(Articulo articulo, boolean segura) {}

    private record Indexado(Articulo articulo, String compacto, Set<String> palabras, Set<String> trigramas) {
        boolean tiene(String palabra) {
            if (palabras.contains(palabra)) return true;
            if (conDigito(palabra) || palabra.length()<4) return false;
            return palabras.stream().anyMatch(a -> !conDigito(a) && a.length()>=4 && (a.startsWith(palabra) || palabra.startsWith(a)));
        }
    }

    private final List<Indexado> catalogo;

    CoincidenciaArticulos(List<Articulo> articulos) {
        catalogo=articulos.stream().filter(a -> a.id()!=null).map(a -> {
            String nombre=normalizar(a.nombre()), completo=normalizar(a.nombre()+" "+a.descripcion());
            return new Indexado(a,compacto(a.nombre()+" "+a.descripcion()),palabras(completo),trigramas(nombre));
        }).toList();
    }

    Optional<Resultado> buscar(String texto,String codigo,String termino) {
        String consulta=normalizar(termino==null || termino.isBlank() ? texto : termino);
        Set<String> palabras=palabras(consulta), trigramas=trigramas(consulta);
        // 1) Código de parte: si algún artículo lo contiene es el mismo componente; entre varios gana el más parecido
        Set<String> codigos=new LinkedHashSet<>();
        if (codigo!=null && compacto(codigo).length()>=3) codigos.add(compacto(codigo));
        codigos.addAll(codigosDe(texto));
        for (String c:codigos) {
            Optional<Indexado> mejor=catalogo.stream().filter(a -> a.compacto().contains(c))
                    .max(Comparator.comparingDouble(a -> puntaje(palabras,trigramas,a)));
            if (mejor.isPresent()) return Optional.of(new Resultado(mejor.get().articulo(),true));
        }
        // 2) Parecido general
        if (palabras.isEmpty()) return Optional.empty();
        Indexado mejor=null; double mejorPuntaje=0;
        for (Indexado a:catalogo) {
            double p=puntaje(palabras,trigramas,a);
            if (p>mejorPuntaje) { mejor=a; mejorPuntaje=p; }
        }
        if (mejor==null || mejorPuntaje<POSIBLE) return Optional.empty();
        return Optional.of(new Resultado(mejor.articulo(),mejorPuntaje>=SEGURA));
    }

    private static double puntaje(Set<String> palabras,Set<String> trigramas,Indexado a) {
        if (palabras.isEmpty()) return 0;
        long encontradas=palabras.stream().filter(a::tiene).count();
        double p=0.7*encontradas/palabras.size()+0.3*jaccard(trigramas,a.trigramas());
        // Si falta un número (10k frente a 100k) no puede ser el mismo componente
        boolean faltaNumero=palabras.stream().anyMatch(w -> conDigito(w) && !a.palabras().contains(w));
        return faltaNumero ? p*0.6 : p;
    }

    private static double jaccard(Set<String> a,Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> comunes=new HashSet<>(a); comunes.retainAll(b);
        return (double) comunes.size()/(a.size()+b.size()-comunes.size());
    }

    private static boolean conDigito(String s) { return s.chars().anyMatch(Character::isDigit); }

    // Minúsculas sin tildes; Ω es "ohm", µ es "u" y los números se separan de su unidad (10kΩ = 10 k ohm)
    static String normalizar(String s) {
        String t=sinTildes(s).replace("ω"," ohm ").replace("µ","u").replace("μ","u").replace("×"," x ");
        return t.replaceAll("[^a-z0-9]+"," ").replaceAll("(?<=\\d)(?=[a-z])|(?<=[a-z])(?=\\d)"," ").replaceAll("\\s+"," ").strip();
    }
    private static String sinTildes(String s) {
        return Normalizer.normalize(s==null ? "" : s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
    }
    private static String compacto(String s) { return sinTildes(s).replaceAll("[^a-z0-9]",""); }

    private static Set<String> palabras(String normalizado) {
        Set<String> r=new LinkedHashSet<>();
        for (String p:normalizado.split(" ")) if (!p.isEmpty() && !VACIAS.contains(p)) r.add(p);
        return r;
    }
    private static Set<String> trigramas(String normalizado) {
        String t=" "+normalizado+" ";
        Set<String> r=new HashSet<>();
        for (int i=0;i+3<=t.length();i++) r.add(t.substring(i,i+3));
        return r;
    }
    private static Set<String> codigosDe(String texto) {
        Set<String> r=new LinkedHashSet<>();
        Matcher m=CODIGO.matcher(sinTildes(texto).replaceAll("[^a-z0-9-]+"," "));
        while (m.find()) {
            String c=m.group().replace("-","");
            if (c.length()>=4 && !VALOR.matcher(c).matches()) r.add(c);
        }
        return r;
    }
}
