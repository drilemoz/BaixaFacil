package br.com.baixafacil;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Diálogos de download alinhados ao visual geral do aplicativo. */
public final class DownloadDialogs {
    private DownloadDialogs() {}

    public static AlertDialog confirm(Activity host, String title, String subtitle,
                                      String primaryLabel, String primaryValue,
                                      String secondaryLabel, String secondaryValue,
                                      String tertiaryLabel, String tertiaryValue,
                                      String note, boolean insecure, Runnable confirmAction) {
        Ui.Palette p = new Ui.Palette(host);
        int d = Ui.dp(host, 1);
        LinearLayout root = Ui.vertical(host);
        root.setPadding(d * 18, d * 18, d * 18, d * 14);
        root.setBackground(Ui.shape(host, p.surface, 24));

        LinearLayout head = Ui.horizontal(host);
        FrameLayout iconBox = new FrameLayout(host);
        iconBox.setBackground(Ui.shape(host, p.soft, 16));
        iconBox.addView(Ui.icon(host, "download", p.teal, 22), new FrameLayout.LayoutParams(d * 24, d * 24, Gravity.CENTER));
        head.addView(iconBox, new LinearLayout.LayoutParams(d * 48, d * 48));
        LinearLayout texts = Ui.vertical(host);
        texts.addView(Ui.text(host, title, 19, p.text, true));
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = Ui.text(host, subtitle, 12, p.muted, false);
            sub.setMaxLines(3);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(sub, Ui.lp(host, -1, -2, 3));
        }
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
        tp.leftMargin = d * 11;
        head.addView(texts, tp);
        root.addView(head);

        ScrollView scroll = new ScrollView(host);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = Ui.vertical(host);
        content.setPadding(0, d * 10, 0, 0);
        content.addView(infoRow(host, p, "globe", primaryLabel, primaryValue), Ui.lp(host, -1, -2, 0));
        if (secondaryValue != null && !secondaryValue.isEmpty()) {
            content.addView(infoRow(host, p, "file", secondaryLabel, secondaryValue), Ui.lp(host, -1, -2, 8));
        }
        if (tertiaryValue != null && !tertiaryValue.isEmpty()) {
            content.addView(infoRow(host, p, "folder", tertiaryLabel, tertiaryValue), Ui.lp(host, -1, -2, 8));
        }
        if (note != null && !note.isEmpty()) {
            TextView text = Ui.text(host, note, 12, p.muted, false);
            text.setPadding(d * 8, d * 12, d * 8, d * 6);
            content.addView(text);
        }
        if (insecure) {
            LinearLayout warning = Ui.horizontal(host);
            warning.setPadding(d * 12, d * 10, d * 12, d * 10);
            warning.setBackground(Ui.shape(host, p.soft, 16));
            warning.addView(Ui.icon(host, "alert", p.warn, 20), new LinearLayout.LayoutParams(d * 20, d * 20));
            TextView danger = Ui.text(host, "Conexão HTTP sem criptografia. Só prossiga se confiar no servidor.", 12, p.warn, true);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(0, -2, 1);
            dp.leftMargin = d * 10;
            warning.addView(danger, dp);
            content.addView(warning, Ui.lp(host, -1, -2, 8));
        }
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, Ui.lp(host, -1, -2, 6));

        LinearLayout actions = Ui.horizontal(host);
        actions.setGravity(Gravity.END);
        TextView cancel = Ui.pill(host, "Cancelar", p.text, p.soft);
        TextView confirm = Ui.pill(host, "Baixar", Color.WHITE, p.teal);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, d * 44);
        actions.addView(cancel, cp);
        LinearLayout.LayoutParams okp = new LinearLayout.LayoutParams(-2, d * 44);
        okp.leftMargin = d * 10;
        actions.addView(confirm, okp);
        root.addView(actions, Ui.lp(host, -1, -2, 10));

        AlertDialog dialog = new AlertDialog.Builder(host).setView(root).create();
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (confirmAction != null) confirmAction.run();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(host.getResources().getDisplayMetrics().widthPixels * 92 / 100, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    public static void info(Activity host, String title, String message) {
        new AlertDialog.Builder(host).setTitle(title).setMessage(message).setPositiveButton("Entendi", null).show();
    }

    private static LinearLayout infoRow(Activity host, Ui.Palette p, String icon, String label, String value) {
        int d = Ui.dp(host, 1);
        LinearLayout row = Ui.horizontal(host);
        row.setPadding(d * 12, d * 11, d * 12, d * 11);
        row.setBackground(Ui.shape(host, p.soft, 16));
        FrameLayout box = new FrameLayout(host);
        box.setBackground(Ui.shape(host, p.surface, 12));
        box.addView(Ui.icon(host, icon, p.text, 18), new FrameLayout.LayoutParams(d * 20, d * 20, Gravity.CENTER));
        row.addView(box, new LinearLayout.LayoutParams(d * 40, d * 40));
        LinearLayout text = Ui.vertical(host);
        text.addView(Ui.text(host, label, 12, p.muted, true));
        TextView valueView = Ui.text(host, value == null ? "-" : value, 14, p.text, true);
        valueView.setMaxLines(2);
        valueView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.addView(valueView, Ui.lp(host, -1, -2, 2));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
        tp.leftMargin = d * 11;
        row.addView(text, tp);
        return row;
    }
}
