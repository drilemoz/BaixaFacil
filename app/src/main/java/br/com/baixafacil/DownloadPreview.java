package br.com.baixafacil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/** Consulta opcional HEAD. Não transfere o corpo do arquivo nem guarda cookies/sessões. */
public final class DownloadPreview {
    private DownloadPreview() {}

    public static final class Result {
        public final String name, type, message;
        public final long length;
        public final boolean known;
        Result(String name,String type,long length,boolean known,String message){
            this.name=name;this.type=type;this.length=length;this.known=known;this.message=message;
        }
    }

    public static Result probe(String original) {
        if(!TransferHttp.isWebUrl(original))return new Result("arquivo","",-1,false,"Endereço não suportado");
        String url=original;
        for(int i=0;i<5;i++) {
            HttpURLConnection connection=null;
            try {
                connection=(HttpURLConnection)new URL(url).openConnection();
                connection.setRequestMethod("HEAD");
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(4000);
                connection.setReadTimeout(4000);
                connection.setRequestProperty("Accept-Encoding","identity");
                connection.setRequestProperty("User-Agent","BaixaFacil/Preview");
                int status=connection.getResponseCode();
                if(TransferHttp.isRedirect(status)) {
                    String location=connection.getHeaderField("Location");
                    if(location==null)return unavailable(original,"Redirecionamento sem destino");
                    String target=new URL(new URL(url),location).toExternalForm();
                    if(!TransferHttp.isWebUrl(target)||TransferHttp.isDowngrade(url,target))
                        return unavailable(original,"Destino inseguro ou não suportado");
                    url=target;
                    continue;
                }
                if(status<200 || status>=300) {
                    return unavailable(original,"Servidor não disponibilizou prévia (HTTP "+status+")");
                }
                String type=connection.getContentType();
                if(type==null)type="";
                String disposition=connection.getHeaderField("Content-Disposition");
                long length=connection.getContentLengthLong();
                // Transfer-Encoding/Content-Encoding podem invalidar uma estimativa de tamanho.
                String encoding=connection.getHeaderField("Content-Encoding");
                if(encoding!=null&&!encoding.equalsIgnoreCase("identity")) length=-1;
                String name=FileNames.choose(disposition,url,type);
                return new Result(name,type,length,true,"Informações fornecidas pelo servidor (podem mudar no download)");
            }catch(IOException|IllegalArgumentException e){
                return unavailable(original,"Não foi possível consultar os metadados");
            } finally {if(connection!=null)connection.disconnect();}
        }
        return unavailable(original,"Muitos redirecionamentos");
    }
    private static Result unavailable(String url,String message) {
        String name=FileNames.choose(null,url,null);
        return new Result(name,"",-1,false,message);
    }
}
