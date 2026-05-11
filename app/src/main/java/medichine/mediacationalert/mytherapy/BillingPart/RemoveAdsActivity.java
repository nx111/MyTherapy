package medichine.mediacationalert.mytherapy.BillingPart;

import static medichine.mediacationalert.mytherapy.utils.Fun.showBanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import medichine.mediacationalert.mytherapy.BuildConfig;
import medichine.mediacationalert.mytherapy.utils.ItemClickListener;
import medichine.mediacationalert.mytherapy.activity.MainActivity;
import medichine.mediacationalert.mytherapy.R;
import medichine.mediacationalert.mytherapy.utils.Fun;
import medichine.mediacationalert.mytherapy.utils.Prefs;

public class RemoveAdsActivity extends AppCompatActivity implements ItemClickListener {

    Activity activity;
    Prefs prefs;
    private BillingClient billingClient;
    List<ProductDetails> productDetailsList;
    ProgressBar loadProducts;
    RecyclerView recyclerView;
    Handler handler;
    AdView mAdView;
    AdRequest adRequest;
    private InterstitialAd mInterstitialAd;
    private FrameLayout adContainerView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_remove_ads);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        if (!BuildConfig.ADS_ENABLED) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        initViews();
        setTitle(R.string.remove_ads);
        //Initialize a BillingClient with PurchasesUpdatedListener onCreate method
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(
                        (billingResult, list) -> {
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                                for (Purchase purchase : list) {
                                    handlePurchase(purchase);
                                }
                            }
                        }
                ).build();

        //start the connection after initializing the billing client
        establishConnection();
    /*    //restore purchases
        btn_restore_fab.setOnClickListener(v -> {
            restorePurchases();
        });*/

        //Checks if the user has a removeAd if not then show ads.
        //Checks if the user has a premium/subscription if not then show ads.
        if (prefs.getPremium() == 0) {
            if (!prefs.isRemoveAd()) {
                Log.d("RemoveAds", "Remove ads off");
            } else {
                Log.d("RemoveAds", "Remove ads On");
                //    mAdView.setVisibility(View.GONE);
            }
        }

        new Fun(this);
        FrameLayout adContainerView = findViewById(R.id.ad_view_container);
        showBanner(this, adContainerView);
    }

    void establishConnection() {

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    showProducts();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                establishConnection();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    void showProducts() {
        ImmutableList productList = ImmutableList.of(
                //Product 1 = index is 0

                //Product 2 = index is 1


                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("ks2")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),

                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("ks3")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),

                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("ksw")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(
                params,
                (billingResult, prodDetailsList) -> {
                    // Process the result
                    productDetailsList.clear();
                    handler.postDelayed(() -> {
                        loadProducts.setVisibility(View.INVISIBLE);
                        productDetailsList.addAll(prodDetailsList);
                        RemoveAdsAdapter adapter = new RemoveAdsAdapter(getApplicationContext(), productDetailsList, RemoveAdsActivity.this, this);
                        recyclerView.setHasFixedSize(true);
                        recyclerView.setLayoutManager(new LinearLayoutManager(RemoveAdsActivity.this, LinearLayoutManager.VERTICAL, false));
                        recyclerView.setAdapter(adapter);
                    }, 2000);

                }
        );

    }

    void launchPurchaseFlow(ProductDetails productDetails) {
        assert productDetails.getSubscriptionOfferDetails() != null;
        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken())
                                .build()
                );
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        billingClient.launchBillingFlow(activity, billingFlowParams);
    }

    void handlePurchase(Purchase purchases) {
        if (!purchases.isAcknowledged()) {
            billingClient.acknowledgePurchase(AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchases.getPurchaseToken())
                    .build(), billingResult -> {

                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    //Setting setIsRemoveAd to true
                    // true - No ads
                    // false - showing ads.
                    prefs.setIsRemoveAd(true);
                    reloadScreen();
                }
            });
        }
    }

    private void reloadScreen() {
        //Reload the screen to activate the removeAd and remove the actual Ad off the screen.
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }

    protected void onResume() {
        super.onResume();
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        for (Purchase purchase : list) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                                handlePurchase(purchase);
                            }
                        }
                    }
                }
        );
    }

    void restorePurchases() {
        billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener((billingResult, list) -> {
        }).build();
        final BillingClient finalBillingClient = billingClient;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                establishConnection();
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    finalBillingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (billingResult1, list) -> {
                                if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    if (list.size() > 0) {
                                        prefs.setIsRemoveAd(true); // set true to activate remove ad feature
                                    } else {
                                        prefs.setIsRemoveAd(false); // set false to de-activate remove ad feature
                                    }
                                }
                            });
                }
            }
        });
    }

    private void initViews() {

        activity = this;
        handler = new Handler();
        prefs = new Prefs(this);
        productDetailsList = new ArrayList<>();
        recyclerView = findViewById(R.id.recycleViewID);

        loadProducts = findViewById(R.id.progressID);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void showSnackbar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void clickListener(int pos) {
        launchPurchaseFlow(productDetailsList.get(pos));

    }
}
