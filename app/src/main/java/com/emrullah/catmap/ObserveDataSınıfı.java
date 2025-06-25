package com.emrullah.catmap;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class ObserveDataSınıfı {

    public static <T> void observeOnce(LiveData<T> liveData, LifecycleOwner owner, Observer<T> observer) {
        liveData.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(T t) {
                observer.onChanged(t); // senin işlemin çalışır
                liveData.removeObserver(this); // sonra gözlemci kaldırılır
            }
        });
    }
}
//bu sınıfı yazma seebebeim eger observe butona basınca calısması gerekiyorsa ve butona surekli basılırsa her seferınde
//dinleyici ekler lıve data bu yuzden silmek gerekir
//ancak eger surekli guncellenecek aktif kalacak bir ui varsa mesela takip takipci sayısı o zaman silmeye gerek yok
//cunku surekli dinlemesi gerekli
//Sayı değiştikçe UI güncellenecekse	observe()	Canlı güncellemeler için gereklidir.
//Sadece bir kez güncelleme yapılacaksa	observeOnce()	Gereksiz gözlemci birikimini önler.