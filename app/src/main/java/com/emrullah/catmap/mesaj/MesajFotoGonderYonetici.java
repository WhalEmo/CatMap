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
import android.widget.TextView;

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
    private ArrayList<String> mesajlasmaFotoUrl = new ArrayList<>();
    private HashMap<String, Target> targetlar = new HashMap<>();
    private final int BITMAP_ADET = 80;

    public static MesajFotoGonderYonetici getInstance(){
        if(yonetici == null){
            yonetici = new MesajFotoGonderYonetici();
        }
        return yonetici;
    }

    public void UriEkle(Uri foto){
        fotografUrileri.add(foto);
    }

    public void GondericiStart(MesajAdapter adapter){
        FotolariStorageKaydet(adapter);
    }

    private void FotolariStorageKaydet(MesajAdapter adapter){
        for (int i=0; i<fotografUrileri.size(); i++){
            Uri foto = fotografUrileri.get(i);
            String dosyaadi = "foto_"+System.currentTimeMillis()+"_"+i+".jpg";
            StorageReference yeniRef = ref.child(dosyaadi);
            yeniRef.putFile(foto).addOnSuccessListener(task->{
                        yeniRef.getDownloadUrl().addOnSuccessListener(uri->{
                            fotoUrlleri.add(uri.toString());

                            if(fotoUrlleri.size() == fotografUrileri.size()){
                                FotoMesajGonder(adapter);
                            }
                        });
                    }
            );
        }
    }

    private void FotoMesajGonder(MesajAdapter adapter){
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
        Mesaj yeniMesaj = new Mesaj(MainActivity.kullanici.getID(),fotoUrlleri,System.currentTimeMillis(),mesajRef.getKey(),false);
        yeniMesaj.setTur("foto");
        adapter.getMesajArrayList().add(yeniMesaj);
        adapter.notifyItemInserted(adapter.getItemCount() - 1);
        fotoUrlleri.clear();
        fotografUrileri.clear();
    }

    public void FotoMesaj(boolean Kim, MesajAdapter.MesajViewHolder holder, Mesaj mesaj, Context context){
        GridLayout layout = Kim ? holder.sagFotoLayout : holder.solFotoLayout;
        layout.removeAllViews();
        System.out.println("girildi fotomesaj");
        layout.setVisibility(View.VISIBLE);
        // burda hata var
        View mesajFoto = LayoutInflater.from(context).inflate(R.layout.mesaj_fotolar, null);
        ImageView resim = mesajFoto.findViewById(R.id.fotoImage);
        ImageView placeholder = mesajFoto.findViewById(R.id.placeholderImage);
        TextView fazlası = mesajFoto.findViewById(R.id.fazlaFotoSayisi);
        int count = mesaj.getUrller().size();
        String url;
        if (count == 1) {
            url = mesaj.getUrller().get(0);
            layout.addView(mesajFoto);
            if(mesajlasmaFotolari.containsKey(url)) {
                resim.setImageBitmap(mesajlasmaFotolari.get(url));
            }
            else {
                Picasso.get().load(url).into(FotoTarget(url, resim, placeholder));
            }
        } else if(count > 1) {
            url = mesaj.getUrller().get(0);
            fazlası.setVisibility(View.VISIBLE);
            fazlası.setText("+"+String.valueOf(count - 1));
            layout.addView(mesajFoto);

            if(mesajlasmaFotolari.containsKey(url)) {
                resim.setImageBitmap(mesajlasmaFotolari.get(url));
            }
            else {
                Picasso.get().load(url).into(FotoTarget(url, resim,placeholder));
            }
        }
    }

    private Target FotoTarget(String url, ImageView resim, ImageView placeholder){
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                FotoYerAc();
                mesajlasmaFotolari.put(url,bitmap);
                placeholder.setVisibility(View.GONE);
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

    private void FotoYerAc(){
        if(mesajlasmaFotoUrl.size() >= BITMAP_ADET){
            mesajlasmaFotolari.remove(mesajlasmaFotoUrl.get(0));
        }
    }


}
