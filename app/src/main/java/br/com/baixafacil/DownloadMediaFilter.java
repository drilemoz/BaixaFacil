package br.com.baixafacil;

/** Filtros de mídia exclusivamente sobre os nomes já registrados no histórico.
 * Não consulta arquivos externos, rede, MIME ou armazenamento do Android.
 */
public final class DownloadMediaFilter {
    private DownloadMediaFilter() {}
    public static final String ALL="Todos", MEDIA="Mídias", VIDEOS="Vídeos", IMAGES="Imagens", AUDIO="Áudios";

    public static boolean matches(String filter,String file){
        if(filter==null || ALL.equals(filter))return true;
        String kind=DownloadInfo.kind(file);
        if(MEDIA.equals(filter))return VIDEOS.equals(kind)||IMAGES.equals(kind)||AUDIO.equals(kind);
        return filter.equals(kind);
    }

    public static final class Counts {
        public final int media,videos,images,audio;
        Counts(int videos,int images,int audio){
            this.videos=videos;this.images=images;this.audio=audio;
            media=videos+images+audio;
        }
    }

    /** Um registro, mesmo pausado, equivale a um item. Não é varredura do armazenamento. */
    public static Counts count(Iterable<String> fileNames){
        int videos=0,images=0,audio=0;
        if(fileNames!=null)for(String name:fileNames){
            String kind=DownloadInfo.kind(name);
            if(VIDEOS.equals(kind))videos++;
            else if(IMAGES.equals(kind))images++;
            else if(AUDIO.equals(kind))audio++;
        }
        return new Counts(videos,images,audio);
    }
}
