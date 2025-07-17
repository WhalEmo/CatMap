package com.beem.catmap;

import android.widget.ImageView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

public class URLye_Ulasma {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public void IDdenUrlyeUlasma(String id, ImageView view){
        db.collection("users")
                .document(id)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String url = documentSnapshot.getString("profilFotoUrl");
                        Picasso.get()
                                .load(url)
                                .fit()
                                .centerCrop()
                                .placeholder(R.drawable.kullanici)
                                .into(view);
                    }
                });
    }

}
