package br.com.baixafacil;

import java.util.ArrayList;
import java.util.Locale;

/** Regras puras para a seleção de documentos do Android (sem permissões de armazenamento). */
final class GeckoPickerPolicy {
    private GeckoPickerPolicy(){}
    static String[] mimeTypes(String[] proposed){
        ArrayList<String> result=new ArrayList<>();
        if(proposed!=null){
            for(String value:proposed){
                if(value==null || value.length()>96)continue;
                String mime=value.trim().toLowerCase(Locale.ROOT);
                // O seletor só recebe MIME real, não extensões ou caracteres de controle.
                if(!mime.matches("(?:[a-z0-9][a-z0-9!#$&^_.+*-]*|\\*)/(?:[a-z0-9][a-z0-9!#$&^_.+*-]*|\\*)"))continue;
                if(!result.contains(mime))result.add(mime);
                if(result.size()==12)break;
            }
        }
        if(result.isEmpty())result.add("*/*");
        return result.toArray(new String[0]);
    }
    static boolean safeUriScheme(String scheme){return "content".equalsIgnoreCase(scheme);}
}
