package br.com.baixafacil;

import java.util.ArrayList;
import java.util.List;

/** CSV exportado escolhido manualmente. Suporta vírgulas, aspas e quebras de linha entre aspas. */
final class CsvRows {
    private CsvRows(){}
    static List<List<String>> parse(String data,int maxDataRows){
        if(data==null || data.length()>400_000 || maxDataRows<1)return null;
        List<List<String>> rows=new ArrayList<>();
        ArrayList<String> row=new ArrayList<>();
        StringBuilder cell=new StringBuilder();
        boolean quoted=false,endedQuote=false;
        int start=data.startsWith("\uFEFF")?1:0;
        for(int i=start;i<data.length();i++){
            char ch=data.charAt(i);
            if(quoted){
                if(ch=='"'){
                    if(i+1<data.length() && data.charAt(i+1)=='"'){cell.append('"');i++;}
                    else{quoted=false;endedQuote=true;}
                }else cell.append(ch);
            }else if(ch=='"'){
                if(cell.length()!=0 || endedQuote)return null;
                quoted=true;
            }else if(ch==','){
                row.add(cell.toString());cell.setLength(0);endedQuote=false;
                if(row.size()>32)return null;
            }else if(ch=='\n' || ch=='\r'){
                row.add(cell.toString());cell.setLength(0);endedQuote=false;
                if(row.size()>32)return null;
                if(!row.equals(java.util.Collections.singletonList(""))){
                    rows.add(row);
                    if(rows.size()>maxDataRows+1)return null;
                }
                row=new ArrayList<>();
                if(ch=='\r' && i+1<data.length() && data.charAt(i+1)=='\n')i++;
            }else{
                // Uma aspa fechada só pode ser seguida por delimitador ou fim de registro.
                if(endedQuote)return null;
                cell.append(ch);
            }
            if(cell.length()>16_384)return null;
        }
        if(quoted)return null;
        row.add(cell.toString());
        if(!row.equals(java.util.Collections.singletonList("")))rows.add(row);
        return rows.size()<=maxDataRows+1?rows:null;
    }
}
