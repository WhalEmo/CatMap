package com.emrullah.catmap;

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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class Yorum_Silme_Guncelleme {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public void yorumSil(String yorumID, ArrayList<Yorum_Model>yorumlar,Yorum_Adapter adapter) {
        // Önce alt yanıtları sil
        db.collection("cats")
                .document(MapsActivity.kediID)
                .collection("yorumlar")
                .document(yorumID)
                .collection("yanitlar")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    db.collection("cats")
                            .document(MapsActivity.kediID)
                            .collection("yorumlar")
                            .document(yorumID)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Silinen yorum adapter'dan bulunup çıkarılır
                                for (int i = 0; i < yorumlar.size(); i++) {
                                    if (yorumlar.get(i).getYorumID().equals(yorumID)) {
                                        yorumlar.remove(i);
                                        adapter.notifyItemRemoved(i);
                                        adapter.notifyItemRangeChanged(i, yorumlar.size());
                                        break;
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("YorumSil", "Yorum silme hatası: ", e);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("YanitSil", "Yanıtları alma/silme hatası: ", e);
                });
    }
    public void yorumGuncelleme(Yorum_Model yorum,Context context,ArrayList<Yorum_Model>yorumList,Yorum_Adapter adapter){
            AlertDialog.Builder builder = new AlertDialog.Builder(context,R.style.ModernAlertDialog);
            builder.setTitle("Yorumu Güncelle");

            final EditText input = new EditText(context);
            input.setText(yorum.getYorumicerik());
            builder.setView(input);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            input.setMinLines(3);
            input.setMaxLines(6);
            input.setGravity(Gravity.TOP | Gravity.START);
            input.setBackground(ContextCompat.getDrawable(context, R.drawable.edittext_oval_bg));
            input.setPadding(30, 30, 30, 30);

            builder.setPositiveButton("Güncelle", (dialog, which) -> {
                String yeniYorum = input.getText().toString().trim();
                db.collection("cats")
                        .document(MapsActivity.kediID)
                        .collection("yorumlar")
                        .document(yorum.getYorumID())
                        .update("icerik", yeniYorum)
                        .addOnSuccessListener(aVoid -> {
                            for (int i = 0; i < yorumList.size(); i++) {
                                if (yorumList.get(i).getYorumID().equals(yorum.getYorumID())) {
                                    yorum.setYorumicerik(yeniYorum);
                                    yorumList.set(i, yorum);
                                    adapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                            Toast.makeText(context, "Yorum güncellendi", Toast.LENGTH_SHORT).show();
                        });
            });

            builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());

            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(dlg -> {
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.modern_dialog_bg));
                }

                // Buton stilleri
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#13216E"));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#BF6A33"));
            });
            dialog.show();
    }
    public void yorumSilynt(String yanitID,String yorumID,ArrayList<Yanit_Model>yanitlar,Yanit_Adapter adapter) {
        db.collection("cats")
                .document(MapsActivity.kediID)
                .collection("yorumlar")
                .document(yorumID)
                .collection("yanitlar")
                .document(yanitID)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Silinen yorum adapter'dan bulunup çıkarılır
                    for (int i = 0; i < yanitlar.size(); i++) {
                        if (yanitlar.get(i).getYanitId().equals(yanitID)) {
                            yanitlar.remove(i);
                            adapter.notifyItemRemoved(i);
                            adapter.notifyItemRangeChanged(i, yanitlar.size());
                            break;
                        }
                    }
                });
    }

    public void yorumGuncellemeynt(Yanit_Model yanit,String yorumID,Context context,ArrayList<Yanit_Model>yanitlar,Yanit_Adapter adapter){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Yanıtı Güncelle");

        final EditText input = new EditText(context);
        input.setText(yanit.getYaniticerik());
        builder.setView(input);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        input.setMaxLines(6);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setBackground(ContextCompat.getDrawable(context, R.drawable.edittext_oval_bg));
        input.setPadding(30, 30, 30, 30);

        builder.setPositiveButton("Güncelle", (dialog, which) -> {
            String yeniYanit = input.getText().toString().trim();
            db.collection("cats")
                    .document(MapsActivity.kediID)
                    .collection("yorumlar")
                    .document(yorumID)
                    .collection("yanitlar")
                    .document(yanit.getYanitId())
                    .update("yaniticerik", yeniYanit)
                    .addOnSuccessListener(aVoid -> {
                        for (int i = 0; i < yanitlar.size(); i++) {
                            if (yanitlar.get(i).getYanitId().equals(yanit.getYanitId())) {
                                yanit.setYaniticerik(yeniYanit);
                                yanitlar.set(i, yanit);
                                adapter.notifyItemChanged(i);
                                break;
                            }
                        }
                        Toast.makeText(context, "Yorum güncellendi", Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.modern_dialog_bg));
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#13216E"));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#BF6A33"));
        });
        dialog.show();
    }


}
