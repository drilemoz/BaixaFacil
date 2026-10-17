package br.com.baixafacil;

/** Formata metadados realmente lidos de arquivos locais. */
public final class MediaPreviewFormat {
    private MediaPreviewFormat() {}
    public static String duration(long millis) {
        if(millis<0)return "";
        long seconds=millis/1000;
        long hours=seconds/3600,minutes=(seconds%3600)/60,remaining=seconds%60;
        if(hours>0)return String.format(java.util.Locale.ROOT,"%d:%02d:%02d",hours,minutes,remaining);
        return String.format(java.util.Locale.ROOT,"%02d:%02d",minutes,remaining);
    }
    public static String description(int width,int height,long durationMillis){
        String resolution=(width>0&&height>0)?width+"×"+height:"";
        String time=duration(durationMillis);
        if(!resolution.isEmpty()&&!time.isEmpty())return resolution+" • "+time;
        return !resolution.isEmpty()?resolution:time;
    }
}
