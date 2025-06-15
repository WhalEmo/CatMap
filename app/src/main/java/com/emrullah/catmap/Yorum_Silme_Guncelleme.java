package com.emrullah.catmap;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class Yorum_Silme_Guncelleme {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public void yorumSil(String yorumID, int position, ArrayList<Yorum_Model>yorumlar) {
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
                                yorumlar.remove(position);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("YorumSil", "Yorum silme hatası: ", e);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("YanitSil", "Yanıtları alma/silme hatası: ", e);
                });
    }
    public void yorumGuncelleme(Yorum_Model yorum, Context context){
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Yorumu Güncelle");

            final EditText input = new EditText(context);
            input.setText(yorum.getYorumicerik());
            builder.setView(input);

            builder.setPositiveButton("Güncelle", (dialog, which) -> {
                String yeniYorum = input.getText().toString().trim();
                db.collection("cats")
                        .document(MapsActivity.kediID)
                        .collection("yorumlar")
                        .document(yorum.getYorumID())
                        .update("icerik", yeniYorum)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Yorum güncellendi", Toast.LENGTH_SHORT).show();
                        });
            });

            builder.setNegativeButton("İptal", (dialog, which) -> dialog.cancel());
            builder.show();

    }



}
