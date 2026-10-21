package br.com.baixafacil;

import java.util.Locale;

/** Classificação e estimativas locais: nenhuma dessas informações é fingida como medição. */
public final class DownloadInfo {
    private DownloadInfo() {}

    public static String kind(String file) {
        if (file == null) return "Outros";
        String name=file.toLowerCase(Locale.ROOT);
        int q=name.indexOf('?'); if(q>=0) name=name.substring(0,q);
        if(ends(name,".apk",".apks",".xapk",".aab")) return "Aplicativos";
        if(ends(name,".mp4",".mkv",".avi",".mov",".webm",".3gp",".m4v")) return "Vídeos";
        if(ends(name,".mp3",".m4a",".aac",".ogg",".flac",".wav",".opus")) return "Áudios";
        if(ends(name,".jpg",".jpeg",".jfif",".png",".apng",".webp",".gif",".bmp",".heic",".heif",".avif",".svg")) return "Imagens";
        if(ends(name,".pdf",".txt",".doc",".docx",".xls",".xlsx",".ppt",".pptx",".csv",".epub",".odt")) return "Documentos";
        if(ends(name,".zip",".rar",".7z",".tar",".gz",".xz",".bz2",".zst")) return "Compactados";
        return "Outros";
    }
    private static boolean ends(String name,String... extensions) {
        for(String ext:extensions) if(name.endsWith(ext))return true;
        return false;
    }
    /** -1 significa que ainda não há dados suficientes para estimar. */
    public static long etaSeconds(long total,long done,long bytesPerSecond) {
        if(total<=0 || done<0 || done>=total || bytesPerSecond<=0)return -1;
        long rest=total-done;
        return Math.max(1, (rest+bytesPerSecond-1)/bytesPerSecond);
    }
    public static String etaLabel(long seconds) {
        if(seconds<0)return "";
        if(seconds<60)return "~"+seconds+" s";
        long minutes=(seconds+59)/60;
        if(minutes<60)return "~"+minutes+" min";
        if(seconds<86400)return "~"+((seconds+3599)/3600)+" h";
        return "mais de 1 dia";
    }
}
