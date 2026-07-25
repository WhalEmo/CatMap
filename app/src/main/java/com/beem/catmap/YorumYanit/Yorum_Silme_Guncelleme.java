package com.beem.catmap.YorumYanit;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.beem.catmap.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Yorum_Silme_Guncelleme {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void yorumSilynt(String catId, String yanitID, String yorumID, List<Yanit_Model> mevcuttur, Yanit_Adapter adapter) {
        if (catId == null || catId.isEmpty()) return;

        db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumID)
                .collection("yanitlar")
                .document(yanitID)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    List<Yanit_Model> yeniYanitListesi = new ArrayList<>(mevcuttur);
                    for (int i = 0; i < yeniYanitListesi.size(); i++) {
                        if (yeniYanitListesi.get(i).getYanitId().equals(yanitID)) {
                            yeniYanitListesi.remove(i);
                            break;
                        }
                    }
                    adapter.submitList(yeniYanitListesi);
                });
    }

    public void yorumGuncellemeynt(String catId, Yanit_Model yanit, String yorumID, Context context, List<Yanit_Model> mevcuttur, Yanit_Adapter adapter) {
        if (catId == null || catId.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.ModernAlertDialog);
        builder.setTitle("Yanıtı Güncelle");

        final EditText input = new EditText(context);
        input.setText(yanit.getYaniticerik());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        input.setMaxLines(6);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setBackground(ContextCompat.getDrawable(context, R.drawable.edittext_oval_bg));
        input.setPadding(30, 30, 30, 30);
        builder.setView(input);

        builder.setPositiveButton("Güncelle", (dialog, which) -> {
            String yeniYanit = input.getText().toString().trim();
            if (!yeniYanit.isEmpty()) {
                db.collection("cats")
                        .document(catId)
                        .collection("yorumlar")
                        .document(yorumID)
                        .collection("yanitlar")
                        .document(yanit.getYanitId())
                        .update("yaniticerik", yeniYanit)
                        .addOnSuccessListener(aVoid -> {
                            List<Yanit_Model> yeniYanitListesi = new ArrayList<>(mevcuttur);
                            for (int i = 0; i < yeniYanitListesi.size(); i++) {
                                if (yeniYanitListesi.get(i).getYanitId().equals(yanit.getYanitId())) {
                                    Yanit_Model guncel = yeniYanitListesi.get(i);
                                    guncel.setYaniticerik(yeniYanit);
                                    yeniYanitListesi.set(i, guncel);
                                    break;
                                }
                            }
                            // ListAdapter için submitList çağrısı yapıyoruz
                            adapter.submitList(yeniYanitListesi);
                            Toast.makeText(context, "Yanıt güncellendi", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.modern_dialog_bg));
            }

            if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#13216E"));
            }
            if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#BF6A33"));
            }
        });
        dialog.show();
    }
}