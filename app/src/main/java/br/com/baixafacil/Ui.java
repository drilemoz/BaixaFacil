package br.com.baixafacil;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Identidade visual local, sem bibliotecas de UI nem recursos de rede adicionais. */
public final class Ui {
    public static final int TEAL = Color.rgb(18, 111, 99);
    private Ui() {}

    public static int dp(Context c, float d) { return (int) (c.getResources().getDisplayMetrics().density * d + .5f); }
    public static int mode(Context c) { return c.getSharedPreferences("appearance", Context.MODE_PRIVATE).getInt("mode", 0); }
    public static boolean dark(Context c) {
        int m = mode(c);
        return m == 2 || (m == 0 && (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
    }
    public static void setMode(Context c, int m) { c.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putInt("mode", m).apply(); }
    public static void selectTheme(Activity a) { a.setTheme(dark(a) ? R.style.AppThemeDark : R.style.AppTheme); }
    public static void systemBars(Activity a, Palette p) {
        a.getWindow().setStatusBarColor(p.background);
        a.getWindow().setNavigationBarColor(p.background);
        int flags = 0;
        if (!p.dark) flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        a.getWindow().getDecorView().setSystemUiVisibility(flags);
        if (Build.VERSION.SDK_INT >= 29) a.getWindow().setNavigationBarContrastEnforced(false);
    }
    public static final class Palette {
        public final boolean dark;
        public final int background, surface, soft, text, muted, border, teal, warn, danger;
        public Palette(Context c) {
            dark = Ui.dark(c);
            background = dark ? 0xFF000000 : 0xFFF4F6F5;
            surface = dark ? 0xFF151A19 : 0xFFFFFFFF;
            soft = dark ? 0xFF242B29 : 0xFFF0F2F1;
            text = dark ? 0xFFF3F7F5 : 0xFF182E29;
            muted = dark ? 0xFFADBAB6 : 0xFF67716E;
            border = dark ? 0xFF303A36 : 0xFFE7ECE9;
            teal = dark ? 0xFF36C3AA : TEAL;
            warn = dark ? 0xFFFFC877 : 0xFF9B6917;
            danger = dark ? 0xFFFF918E : 0xFFB13D3D;
        }
    }
    public static GradientDrawable shape(Context c, int fill, float radius) {
        GradientDrawable bg = new GradientDrawable(); bg.setColor(fill); bg.setCornerRadius(dp(c,radius)); return bg;
    }
    public static GradientDrawable outline(Context c, int fill, float radius, int border) {
        GradientDrawable bg = shape(c,fill,radius); bg.setStroke(dp(c,1),border); return bg;
    }
    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL); t.setIncludeFontPadding(true); return t;
    }
    public static LinearLayout vertical(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    public static LinearLayout horizontal(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    public static LinearLayout.LayoutParams lp(Context c, int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w < 0 ? w : dp(c,w), h < 0 ? h : dp(c,h)); p.topMargin = dp(c,top); return p;
    }
    public static TextView pill(Context c, String s, int fg, int bg) {
        TextView t = text(c,s,13,fg,true); t.setGravity(Gravity.CENTER);
        t.setBackground(shape(c,bg,50)); t.setPadding(dp(c,18),0,dp(c,18),0); t.setMinHeight(dp(c,44)); return t;
    }
    public static LinearLayout action(Context c, String name, String icon, int fg, int bg, float radius) {
        LinearLayout row = horizontal(c); row.setGravity(Gravity.CENTER); row.setBackground(shape(c,bg,radius));
        row.setClickable(true); row.setFocusable(true); row.setContentDescription(name);
        Icon glyph = new Icon(c,icon,fg,21); row.addView(glyph,new LinearLayout.LayoutParams(dp(c,22),dp(c,22)));
        TextView t = text(c,name,14,fg,true); LinearLayout.LayoutParams p = lp(c,-2,-2,0); p.leftMargin=dp(c,9); row.addView(t,p);
        row.setMinimumHeight(dp(c,48)); return row;
    }
    public static Icon icon(Context c, String kind, int color, int size) { return new Icon(c,kind,color,size); }

    /** Pequenos ícones vetoriais desenhados em Canvas; escala em dp e não requer fontes externas. */
    public static final class Icon extends View {
        private final String kind;
        private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int color, nominalSize;
        public Icon(Context c, String kind, int color, int size) { super(c); this.kind=kind; this.color=color; this.nominalSize=size; setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onMeasure(int w,int h) {
            int target=dp(getContext(),nominalSize); setMeasuredDimension(resolveSize(target,w),resolveSize(target,h));
        }
        private void lines(Canvas c,float... p) { Path path = new Path(); path.moveTo(p[0],p[1]); for (int i=2;i<p.length;i+=2)path.lineTo(p[i],p[i+1]); c.drawPath(path,pen); }
        private void rect(Canvas c,float a,float b,float d,float e,float r) { c.drawRoundRect(new RectF(a,b,d,e),r,r,pen); }
        private void circle(Canvas c,float x,float y,float r) { c.drawCircle(x,y,r,pen); }
        @Override protected void onDraw(Canvas target) {
            super.onDraw(target); int save=target.save(); float scale=Math.min(getWidth(),getHeight())/24f;
            target.translate((getWidth()-24*scale)/2,(getHeight()-24*scale)/2); target.scale(scale,scale);
            pen.setColor(color);pen.setStyle(Paint.Style.STROKE);pen.setStrokeWidth(2);pen.setStrokeCap(Paint.Cap.ROUND);pen.setStrokeJoin(Paint.Join.ROUND);
            switch(kind) {
                case "download": lines(target,12,3,12,16); lines(target,7,11,12,16,17,11); lines(target,4,17,4,20,20,20,20,17); break;
                case "link": circle(target,8,15,0.1f); lines(target,10,14,14,10); target.drawArc(new RectF(3,9,13,21),40,245,false,pen); target.drawArc(new RectF(11,3,21,15),215,245,false,pen); break;
                case "paste": rect(target,5,5,19,22,2); rect(target,9,2,15,7,1); lines(target,9,14,11,16,16,11); break;
                case "globe": circle(target,12,12,9); lines(target,3,12,21,12); target.drawOval(new RectF(8,3,16,21),pen); break;
                case "webdown": circle(target,9,10,6); lines(target,3,10,15,10); target.drawOval(new RectF(6,4,12,16),pen); lines(target,17,11,17,18); lines(target,14,15,17,18,20,15); lines(target,12,20,22,20); break;
                case "mic": target.drawRoundRect(new RectF(9,3,15,15),3,3,pen);target.drawArc(new RectF(6,8,18,20),0,180,false,pen);lines(target,12,20,12,23);break;
                case "qr": lines(target,3,10,3,3,10,3);lines(target,14,3,21,3,21,10);lines(target,3,14,3,21,10,21);lines(target,14,21,21,21,21,14);rect(target,8,8,16,16,0);break;
                case "settings": lines(target,4,6,20,6); lines(target,4,12,20,12); lines(target,4,18,20,18); circle(target,9,6,2.5f); circle(target,16,12,2.5f); circle(target,10,18,2.5f); break;
                case "search": circle(target,10,10,6);lines(target,14.5f,14.5f,21,21); break;
                case "filter": lines(target,3,5,21,5,14,13,14,19,10,21,10,13,3,5); break;
                case "folder": lines(target,3,8,3,19,21,19,21,8,12,8,10,5,3,5,3,8); lines(target,12,10,12,16); lines(target,9,13,12,16,15,13); break;
                case "file": lines(target,6,3,15,3,20,8,20,21,6,21,6,3); lines(target,15,3,15,8,20,8); break;
                case "archive": rect(target,4,3,20,21,2);lines(target,4,9,20,9);lines(target,10,13,14,13); break;
                case "pause": circle(target,12,12,9);lines(target,9,8,9,16);lines(target,15,8,15,16); break;
                case "play": circle(target,12,12,9); lines(target,10,8,16,12,10,16,10,8); break;
                case "x":lines(target,5,5,19,19);lines(target,19,5,5,19);break;
                case "check":lines(target,4,12,10,18,20,6);break;
                case "back":lines(target,19,12,5,12,11,6);lines(target,5,12,11,18);break;
                case "forward":lines(target,5,12,19,12,13,6);lines(target,19,12,13,18);break;
                case "reload":target.drawArc(new RectF(4,4,20,20),-65,300,false,pen);lines(target,16,3,20,4,19,9);break;
                case "home":lines(target,3,11,12,4,21,11);lines(target,5,10,5,20,19,20,19,10);lines(target,10,20,10,14,14,14,14,20);break;
                case "trash":lines(target,4,7,20,7);lines(target,9,4,15,4);lines(target,6,7,7,21,17,21,18,7);lines(target,10,11,10,17);lines(target,14,11,14,17);break;
                case "lock":rect(target,5,10,19,21,2);target.drawArc(new RectF(8,3,16,15),180,180,false,pen);break;
                case "alert":lines(target,12,4,3,20,21,20,12,4);lines(target,12,10,12,14);circle(target,12,17,0.7f);break;
                case "shield":lines(target,12,2,20,5,20,12,17,19,12,22,7,19,4,12,4,5,12,2);lines(target,8,12,11,15,16,9);break;
                case "copy": rect(target,8,5,19,16,2); rect(target,5,8,16,19,2); break;
                case "bookmark": lines(target,6,3,18,3,18,21,12,17,6,21,6,3); break;
                case "chevron": lines(target,6,9,12,15,18,9); break;
                case "more": pen.setStyle(Paint.Style.FILL);circle(target,12,5,2);circle(target,12,12,2);circle(target,12,19,2);break;
                case "menu":lines(target,4,6,20,6);lines(target,4,12,20,12);lines(target,4,18,20,18);break;
                default: circle(target,12,12,8);
            }
            target.restoreToCount(save);
        }
    }
}
