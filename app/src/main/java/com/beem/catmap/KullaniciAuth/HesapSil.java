package com.beem.catmap.KullaniciAuth;

import android.support.annotation.NonNull;

import com.beem.catmap.MainActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HesapSil {
   private FirebaseFirestore db = FirebaseFirestore.getInstance();
   private DatabaseReference ref = FirebaseDatabase.getInstance().getReference("mesajlar");

    private ArrayList<String> takipEdilenler = new ArrayList<>();
    private ArrayList<String> takipciler = new ArrayList<>();
    private ArrayList<Map<String, Object>> yuklenenKediler;

    public void HesapSilmeBaslat(Runnable onFinish) {
        Task<Void> takipEdilenlerTask = db.collection("users")
                .document(MainActivity.kullanici.getID())
                .collection("takipEdilenler")
                .get()
                .continueWithTask(task ->{
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            takipEdilenler.add(doc.getId());
                        }
                        return TakipEdilenlerCikartma();
                    }
                    return null;
                });

        Task<Void> takipcilerTask = db.collection("users")
                .document(MainActivity.kullanici.getID())
                .collection("takipciler")
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            takipciler.add(doc.getId());
                        }
                        return TakipcilerCikartma();
                    }
                    return Tasks.forResult(null);
                });

        Task<Void> kedilerTask = db.collection("users")
                .document(MainActivity.kullanici.getID())
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        yuklenenKediler = task.getResult().contains("GonderilenKediler")
                                ? (ArrayList<Map<String, Object>>) task.getResult().get("GonderilenKediler")
                                : new ArrayList<>();
                        return KedilerSilme();
                    }
                    return Tasks.forResult(null);
                });

        Tasks.whenAllComplete(takipEdilenlerTask, takipcilerTask, kedilerTask)
                .addOnSuccessListener(tasks -> {
                    MesajlariSil(() -> {
                        HesabiSil(() -> {
                            onFinish.run();
                        });
                    });
                });
    }



    private Task<Void> TakipEdilenlerCikartma() {
        List<Task<Void>> silinecekler = new ArrayList<>();
        for (String takipEdilen : takipEdilenler) {
            silinecekler.add(db.collection("users")
                    .document(takipEdilen)
                    .collection("takipciler")
                    .document(MainActivity.kullanici.getID())
                    .delete());
        }
        return Tasks.whenAll(silinecekler);
    }

    private Task<Void> TakipcilerCikartma() {
        List<Task<Void>> silinecekler = new ArrayList<>();
        for (String takipci : takipciler) {
            silinecekler.add(db.collection("users")
                    .document(takipci)
                    .collection("takipEdilenler")
                    .document(MainActivity.kullanici.getID())
                    .delete());
        }
        return Tasks.whenAll(silinecekler);
    }

    private Task<Void> KedilerSilme() {
        List<Task<Void>> silinecekler = new ArrayList<>();
        for (Map<String, Object> gonderi : yuklenenKediler) {
            String kediID = (String) gonderi.get("kediID");
            silinecekler.add(db.collection("cats").document(kediID).delete());
        }
        return Tasks.whenAll(silinecekler);
    }

    private void MesajlariSil(Runnable onFinish) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (child.getKey() != null) {
                        String[] idler = child.getKey().split("_");
                        if (idler.length == 2) {
                            String id1 = idler[0];
                            String id2 = idler[1];
                            if (id1.equals(MainActivity.kullanici.getID()) || id2.equals(MainActivity.kullanici.getID())) {
                                ref.child(child.getKey()).removeValue();
                            }
                        }
                    }
                }
                onFinish.run();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
    private void HesabiSil(Runnable onFinish){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        AuthCredential credential = EmailAuthProvider.getCredential(MainActivity.kullanici.getEmail(), MainActivity.kullanici.getSifre());

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.delete().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        db.collection("users").document(MainActivity.kullanici.getID()).delete().addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                onFinish.run();
                            }
                        });
                    }
                }
                );
            }
        });
    }
}
