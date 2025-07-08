package com.emrullah.catmap.mesaj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;

import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MesajFotoGonderYonetici {
    private static MesajFotoGonderYonetici yonetici;

    private StorageReference ref = FirebaseStorage.getInstance().getReference("mesajFotolar");

    private ArrayList<Uri> fotografUrileri = new ArrayList<>();
    private ArrayList<String> fotoUrlleri = new ArrayList<>();
    private HashMap<String , Bitmap> mesajlasmaFotolari = new HashMap<>();
    private HashMap<String, Target> targetlar = new HashMap<>();

    public static MesajFotoGonderYonetici getInstance(){
        if(yonetici == null){
            yonetici = new MesajFotoGonderYonetici();
        }
        return yonetici;
    }

    public void UriEkle(Uri foto){
        fotografUrileri.add(foto);
    }

    public void GondericiStart(){
        FotolariStorageKaydet();
    }

    private void FotolariStorageKaydet(){
        for (int i=0; i<fotografUrileri.size(); i++){
            Uri foto = fotografUrileri.get(i);
            String dosyaadi = "foto_"+System.currentTimeMillis()+"_"+i+".jpg";
            StorageReference yeniRef = ref.child(dosyaadi);
            yeniRef.putFile(foto).addOnSuccessListener(task->{
                        yeniRef.getDownloadUrl().addOnSuccessListener(uri->{
                            fotoUrlleri.add(uri.toString());

                            if(fotoUrlleri.size() == fotografUrileri.size()){
                                FotoMesajGonder();
                            }
                        });
                    }
            );
        }
    }

    private void FotoMesajGonder(){
        String sohbetID = MesajlasmaYonetici.getInstance().getSohbetID();
        DatabaseReference mesajRef = FirebaseDatabase.getInstance().getReference()
                .child("mesajlar").child(sohbetID).child("anaMesaj").push();
        Map<String, Object> veri = new HashMap<>();
        veri.put("fotoUrlleri",fotoUrlleri);
        veri.put("zaman",System.currentTimeMillis());
        veri.put("gonderen", MainActivity.kullanici.getID());
        veri.put("goruldu",false);
        veri.put("tur","foto");
        mesajRef.setValue(veri);
        fotoUrlleri.clear();
        fotografUrileri.clear();
    }

    public void FotoMesaj(boolean Kim, MesajAdapter.MesajViewHolder holder, Mesaj mesaj, Context context){
        GridLayout layout = Kim ? holder.sagFotoLayout : holder.solFotoLayout;
        layout.setVisibility(View.VISIBLE);

        int count = mesaj.getUrller().size();
        if (count == 1) {
            View mesajFoto = LayoutInflater.from(context).inflate(R.layout.mesaj_fotolar, null);
            ImageView resim = mesajFoto.findViewById(R.id.fotoImage);
            Picasso.get().load(mesaj.getUrller().get(0)).into(resim);
            layout.addView(mesajFoto);
        } else {
            for (String url : mesaj.getUrller()) {
                View mesajFoto = LayoutInflater.from(context).inflate(R.layout.mesaj_fotolar, null);
                ImageView resim = mesajFoto.findViewById(R.id.fotoImage);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 200;
                params.height = 200;
                params.setMargins(6, 6, 6, 6);
                mesajFoto.setLayoutParams(params);
                Picasso.get().load(url).into(resim);
                layout.addView(mesajFoto);
            }
        }
    }

    private Target FotoTarget(String url, Mesaj mesaj, ImageView resim){
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                mesajlasmaFotolari.put(url,bitmap);
                resim.setImageBitmap(bitmap);
                targetlar.remove(url);
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
        targetlar.put(url,target);
        return target;
    }


}
