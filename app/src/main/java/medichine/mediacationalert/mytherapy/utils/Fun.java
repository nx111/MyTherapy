package medichine.mediacationalert.mytherapy.utils;


import static android.content.Context.CLIPBOARD_SERVICE;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.File;

import medichine.mediacationalert.mytherapy.BuildConfig;
import medichine.mediacationalert.mytherapy.R;


public class Fun {

    public static Context context;
    public static String appurl = "https://play.google.com/store/apps/details?id=com.kodi.setup.configurator";
    public static String moreApp = "https://play.google.com/store/apps/dev?id=8697094179003576981";
    private static int count = 0;
    private static int countfc = 0;
    public static Activity activity;
    private static com.facebook.ads.InterstitialAd interstitialAd;
    private static InterstitialAd mInterstitialAd;
    private static int divider = 3;
    private static int fc = 2;
    private static int admobon = 0;
    public static int nc = 15;
    private FrameLayout adContainerView;

    public static boolean removeAds = false;
    public static AdView adView;

    public Fun(Activity context) {
        this.context = context;
        this.activity = context;
        checkAds();
    }

    private static void checkAds() {
        if (!BuildConfig.ADS_ENABLED) {
            removeAds = true;
            return;
        }
        Prefs prefs = new Prefs(context);
        removeAds = prefs.isRemoveAd();
    }

    public static void showBanner(Activity activity, FrameLayout adContainerView) {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
            adContainerView.setVisibility(View.GONE);
        }
        if (!BuildConfig.ADS_ENABLED || removeAds || adContainerView == null) {
            return;
        }

    }

    private static void loadBanner(Activity activity) {
        // Create an ad request. Check your logcat output for the hashed device ID
        // to get test ads on a physical device, e.g.,
        // "Use AdRequest.Builder.addTestDevice("ABCDE0123") to get test ads on this
        // device."
        AdRequest adRequest =
                new AdRequest.Builder()
                        .build();

        AdSize adSize = getAdSize(activity);
        // Step 4 - Set the adaptive ad size on the ad view.
        adView.setAdSize(adSize);


        // Step 5 - Start loading the ad in the background.
        adView.loadAd(adRequest);
    }

    private static AdSize getAdSize(Activity activity) {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        int adWidth = (int) (widthPixels / density);

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }


    private static BroadcastReceiver attachmentDownloadCompleteReceive;

    public static boolean checkInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if (info != null && info.isConnected()) {
            return true;
        } else {
            return false;

        }
    }


    private static int ac = 0;


    public static void addShowAdmob() {

        if (!BuildConfig.ADS_ENABLED || removeAds) {
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, context.getString(R.string.interstitial_key), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {

                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        if (mInterstitialAd != null) {
                            mInterstitialAd.show(activity);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {

                        Log.i("MainActivity", loadAdError.getMessage());
                        mInterstitialAd = null;
                    }

                });

    }

    public static void addShow() {

        if (!BuildConfig.ADS_ENABLED || removeAds) {
            return;
        }
        count++;
        addShowAdmob();

    }

    public static void copyItem(String s) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        ClipData clipe = ClipData.newPlainText("LINK", s);

        if (clipboard != null) {
            clipboard.setPrimaryClip(clipe);
            Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
        }
    }


    /*  public static void addShowFb() {

          if (removeAds) {

          } else {
              interstitialAd = new com.facebook.ads.InterstitialAd(context, context.getString(R.string.fb_insta_id));
              // Create listeners for the Interstitial Ad
              InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
                  @Override
                  public void onInterstitialDisplayed(Ad ad) {
                      // Interstitial ad displayed callback

                  }

                  @Override
                  public void onInterstitialDismissed(Ad ad) {
                      // Interstitial dismissed callback

                  }

                  @Override
                  public void onError(Ad ad, AdError adError) {
                      // Ad error callback

                  }

                  @Override
                  public void onAdLoaded(Ad ad) {
                      // Interstitial ad is loaded and ready to be displayed

                      // Show the ad
                      interstitialAd.show();
                  }

                  @Override
                  public void onAdClicked(Ad ad) {
                      // Ad clicked callback

                  }

                  @Override
                  public void onLoggingImpression(Ad ad) {
                      // Ad impression logged callback

                  }
              };

              // For auto play video ads, it's recommended to load the ad
              // at least 30 seconds before it is shown
              interstitialAd.loadAd(
                      interstitialAd.buildLoadAdConfig()
                              .withAdListener(interstitialAdListener)
                              .build());

          }
      }
  */
    public static boolean haveStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //  Log.e("Permission error", "You have permission");
                return true;
            } else {

                //  Log.e("Permission error", "You have asked for permission");
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                Toast.makeText(context, R.string.download_permission_needed, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else { //you dont need to worry about these stuff below api level 23
            //  Log.e("Permission error", "You already have the permission");
            return true;
        }
    }

    protected static boolean need2Download(String fileName) {

        File basePath = new File("BOOK_STORE_PATH");

        File fullPath = new File(basePath, fileName);

        if (fullPath.exists())
            return false;
        return true;
    }

    public static void downLoadFromLink(String url) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        if (haveStoragePermission() && checkInternet()) {
            if (need2Download(fileName)) {

                try {
                    if (url != null && !url.isEmpty()) {
                        Uri uri = Uri.parse(url);
                        context.registerReceiver(attachmentDownloadCompleteReceive, new IntentFilter(
                                DownloadManager.ACTION_DOWNLOAD_COMPLETE));

                        DownloadManager.Request request = new DownloadManager.Request(uri);
                        request.setMimeType(getMimeType(uri));
                        request.setTitle(fileName);
                        request.setDescription("Downloading attachment..");
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setAllowedOverMetered(true)
                                .setAllowedOverRoaming(false)
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName + "zip");
                        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                        Toast.makeText(context, R.string.download_complete_soon, Toast.LENGTH_SHORT).show();

                    }

                } catch (Exception e) {

                }
            }

        } else
            Toast.makeText(context, R.string.check_internet, Toast.LENGTH_SHORT).show();

    }

    public static String getMimeType(Uri uri) {
        String mimeType = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(uri);
        } else {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri
                    .toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    fileExtension.toLowerCase());
        }
        return mimeType;
    }

    public static int dpToPx(int dp) {
        Resources r = context.getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    public static void openPlayer() {

        boolean isAppInstalled = appInstalledOrNot("org.xbmc.kodi");

        if (isAppInstalled) {
            Intent LaunchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage("org.xbmc.kodi");
            context.startActivity(LaunchIntent);
        } else {
            Toast.makeText(context, R.string.kodi_not_found, Toast.LENGTH_SHORT).show();

        }
    }

    public static boolean appInstalledOrNot(String uri) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }
}
