package com.beem.catmap.maps;


import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.beem.catmap.BottomSheetController;
import com.beem.catmap.data.model.UserModel;
import com.beem.catmap.ui.badge.BadgeFragment;
import com.beem.catmap.ui.onboarding.OnboardingFragment;
import com.beem.catmap.ui.profile.block.UserBlockFragment;
import com.beem.catmap.ui.profile.common.ProfileFragment;
import com.beem.catmap.ui.commentreply.CommentsBottomSheetFragment;
import com.beem.catmap.data.local.CacheHelperPostLike;


import com.beem.catmap.R;
import com.beem.catmap.data.session.CurrentUserManager;
import com.beem.catmap.databinding.ActivityMapsBinding;

import com.beem.catmap.ui.auth.AuthFragment;
import com.beem.catmap.ui.auth.ProfileSetupFragment;
import com.beem.catmap.ui.camera.CameraFragment;
import com.beem.catmap.ui.chatlist.ChatFragment;
import com.beem.catmap.ui.message.MessageFragment;
import com.beem.catmap.ui.manager.CatMapToastEngine;
import com.beem.catmap.ui.manager.UiMessageManager;
import com.beem.catmap.ui.manager.UiMessageState;
import com.beem.catmap.ui.map.CatMapFragment;
import com.beem.catmap.ui.navigation.CatMapNavigationEngine;
import com.beem.catmap.ui.navigation.CatMapNavigationRenderer;
import com.beem.catmap.ui.navigation.FragmentProvider;
import com.beem.catmap.ui.navigation.Screen;
import com.beem.catmap.ui.navigation.SmartNavigationEngine;
import com.beem.catmap.ui.upload.UploadFragment;
import com.beem.catmap.ui.profile.edit.EditProfileFragment;
import com.beem.catmap.ui.profile.follow.fragment.FollowFragment;
import com.beem.catmap.ui.profile.post.PostDetailFragment;
import com.beem.catmap.utils.NetworkObserver;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.squareup.picasso.Target;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import kotlin.Unit;

public class MapsActivity extends AppCompatActivity implements BottomSheetController {
    private ActivityMapsBinding binding;
    private Boolean lastNetworkStatus = null;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private NetworkObserver networkObserver;

    private BottomSheetDialog bottomSheetDialog;
    private long sonTiklamaZamani = 0;

    Map<String, Bitmap> fotoCache = new HashMap<>();
    List<Target> targetListesi = new ArrayList<>();
    public Marker sonTiklananMarker;

    private String gosterilecekKediID;
    private FrameLayout rightSlidingPanel;
    private boolean isPanelVisible = false;
    private ImageButton btnClose;
    private int screenWidth;

    private CatMapNavigationEngine navigationEngine;
    private CatMapNavigationRenderer navigationRenderer;
    private CurrentUserManager currentUserManager;
    private double latitude;
    private double longitude;
    private boolean bittimi = true;



    private final FragmentProvider fragmentProvider = new FragmentProvider() {
        @Nullable
        @Override
        public Fragment createFragment(@NonNull String tag) {
            Screen screen = Screen.Companion.fromTag(tag);
            return switch (screen) {
                case MAP -> new CatMapFragment();
                case UPLOAD -> new UploadFragment();
                case OTHER_PROFILE -> new ProfileFragment();
                case CAMERA -> new CameraFragment();
                case CHAT -> new ChatFragment();
                case PROFILE -> new ProfileFragment();
                case MESSAGE -> new MessageFragment();
                case EDIT_PROFILE -> new EditProfileFragment();
                case BLOCKED_USERS -> new UserBlockFragment();
                case FOLLOWERS -> new FollowFragment();
                case POST -> new PostDetailFragment();
                case MESSAGE_PHOTO_PREVIEW -> null;
                case AUTH -> new AuthFragment();
                case PROFILE_SETUP -> new ProfileSetupFragment();
                case ONBOARDING -> new OnboardingFragment();
                case BADGE -> new BadgeFragment();
            };
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("ActivityLifecycle", "📌 onCreate: Activity oluşturuldu.");

        currentUserManager = CurrentUserManager.Companion.getInstance(getApplicationContext());
        networkObserver = new NetworkObserver(this);

        networkObserver.observeJava(isConnected -> {
            if (lastNetworkStatus != null && lastNetworkStatus == isConnected) {
                return;
            }
            if (lastNetworkStatus == null) {
                lastNetworkStatus = isConnected;

                if (!isConnected) {
                    UiMessageManager.INSTANCE.emitMessage(
                            new UiMessageState.Info("İnternet bağlantınız yok!")
                    );
                }
                return;
            }

            lastNetworkStatus = isConnected;

            if (!isConnected) {
                UiMessageManager.INSTANCE.emitMessage(
                        new UiMessageState.Error("İnternet bağlantınız kesildi!", 1500)
                );
            } else {
                UiMessageManager.INSTANCE.emitMessage(
                        new UiMessageState.Info("Yeniden bağlandınız")
                );
            }
        });

        setTheme(R.style.Theme_CatMap);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("CRASH_DETECTOR", "Hata oluştu: ", throwable);
            Process.killProcess(Process.myPid());
            System.exit(10);
        });

        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.catmap_background));

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
        }

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navigationEngine = new CatMapNavigationEngine(this, binding);
        navigationRenderer = new CatMapNavigationRenderer(this, R.id.fragment_container, fragmentProvider);

        SmartNavigationEngine.registerActivityCallbacks(
                () -> {
                    if (rightSlidingPanel != null && rightSlidingPanel.getTranslationX() == 0) {
                        rightSlidingPanel.animate()
                                .translationX(screenWidth)
                                .setDuration(300)
                                .start();
                        return true;
                    }
                    return false;
                },
                () -> {
                    showSystemExitDialog();
                    return Unit.INSTANCE;
                }
        );

        if (currentUserManager.isUserLoggedIn()) {
            BegenileriCek();
            SmartNavigationEngine.init(navigationEngine, Screen.MAP);
        } else {
            SmartNavigationEngine.init(navigationEngine, Screen.AUTH);
        }

        uiMessageManagerObserver();

        rightSlidingPanel = findViewById(R.id.rightSlidingPanel);
        btnClose = findViewById(R.id.btnClosePanel);
        TextView tvCatFactSliding = findViewById(R.id.tvCatFactSliding);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;

        AdView adView = findViewById(R.id.adView);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> rightSlidingPanel.animate()
                    .translationX(screenWidth)
                    .setDuration(300)
                    .withEndAction(() -> {
                        if (tvCatFactSliding != null) tvCatFactSliding.setText("");
                        isPanelVisible = false;
                    })
                    .start());
        }

        konumizni();

        gosterilecekKediID = getIntent().getStringExtra("kediId");
        if (gosterilecekKediID != null && !gosterilecekKediID.isEmpty()) {
            yorumlarBottomSheetGoster(gosterilecekKediID);
        }

        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }


    public void yorumlarBottomSheetGoster(String catId) {
        if (SystemClock.elapsedRealtime() - sonTiklamaZamani < 600) {
            return;
        }
        sonTiklamaZamani = SystemClock.elapsedRealtime();
        Fragment existing = getSupportFragmentManager().findFragmentByTag(CommentsBottomSheetFragment.TAG);
        if (existing != null && (existing.isAdded() || existing.isVisible())) {
            return;
        }

        CommentsBottomSheetFragment bottomSheet = CommentsBottomSheetFragment.newInstance(catId);
        bottomSheet.show(getSupportFragmentManager(), CommentsBottomSheetFragment.TAG);
    }

    public void sonTiklananMarkeriSil() {
        if (sonTiklananMarker != null) {
            sonTiklananMarker.remove();
            sonTiklananMarker = null;
        }
    }
    private void BegenileriCek() {
        String userId = currentUserManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) return;

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (documentSnapshot.exists()) {
                        List<String> liste = (List<String>) documentSnapshot.get("begendigiGonderiler");
                        if (liste != null) {
                            CacheHelperPostLike.getInstance().setBegeniList(new HashSet<>(liste));
                        } else {
                            CacheHelperPostLike.getInstance().setBegeniList(new HashSet<>());
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("BegeniYukleme", "Beğeniler çekilemedi: " + e.getMessage()));
    }

    private void konumizni() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Konum izni verildi.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Konum izni reddedildi!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("ActivityLifecycle", "💀 onDestroy: Activity yok ediliyor.");
        bittimi = false;

        if (currentUserManager != null && currentUserManager.isUserLoggedIn()) {
            UserModel userModel = currentUserManager.getCurrentUser();
            if (userModel != null) {
                userModel.latitude=latitude;
                userModel.longitude=longitude;
            }
        }
        LocationEngine.INSTANCE.stopTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ActivityLifecycle", "⚡ onResume: Activity etkileşime açık, ön planda!");
        if (currentUserManager.isUserLoggedIn()) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ActivityLifecycle", "🙈 onStop: Activity arka plana geçti, görünmez.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("ActivityLifecycle", "⏸️ onPause: Activity odak kaybetti.");
    }

    @Override
    public void hideBottomSheet() {
        if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
            bottomSheetDialog.hide();
        }
    }

    @Override
    public void showBottomSheet() {
        if (bottomSheetDialog != null && !bottomSheetDialog.isShowing()) {
            bottomSheetDialog.show();
        }
    }

    private void uiMessageManagerObserver() {
        UiMessageManager.INSTANCE.getMessageEvent().observe(this, message -> {
            if (message != null) {
                String msgText = "";
                int iconRes = R.drawable.ic_check;
                int strokeColor = getResources().getColor(R.color.catmap_success);
                int durationMs = 3500;

                if (message instanceof UiMessageState.Success) {
                    msgText = ((UiMessageState.Success) message).getMessage();
                    durationMs = ((UiMessageState.Success) message).getDurationMs();
                    iconRes = R.drawable.ic_check;
                    strokeColor = getResources().getColor(R.color.catmap_success);
                } else if (message instanceof UiMessageState.Error) {
                    msgText = ((UiMessageState.Error) message).getMessage();
                    durationMs = 5000;
                    iconRes = R.drawable.ic_close;
                    strokeColor = getResources().getColor(R.color.catmap_error);
                } else if (message instanceof UiMessageState.Info) {
                    msgText = ((UiMessageState.Info) message).getMessage();
                    durationMs = 4000;
                    iconRes = R.drawable.ic_gallery;
                    strokeColor = getResources().getColor(R.color.catmap_text_muted);
                }

                CatMapToastEngine.show(this, msgText, iconRes, strokeColor, durationMs);
                UiMessageManager.INSTANCE.clear();
            }
        });
    }

    private void showSystemExitDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_exit_app);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancelExit);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirmExit);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity();
        });

        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("ActivityLifecycle", "👁️ onStart: Activity görünür oldu.");
    }



    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("ActivityLifecycle", "🔄 onRestart: Activity arka plandan geri döndü.");
    }

}