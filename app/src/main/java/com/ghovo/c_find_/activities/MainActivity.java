package com.ghovo.c_find_.activities;



import static com.ghovo.c_find_.utilities.Constants.KEY_ACTIVITY_FOR_SEARCH;
import static com.ghovo.c_find_.utilities.Constants.KEY_COLLECTION_HISTORY;
import static com.ghovo.c_find_.utilities.Constants.KEY_COLLECTION_REQUEST;
import static com.ghovo.c_find_.utilities.Constants.KEY_COLLECTION_USERS;
import static com.ghovo.c_find_.utilities.Constants.KEY_DISTANCE;
import static com.ghovo.c_find_.utilities.Constants.KEY_EMAIL;
import static com.ghovo.c_find_.utilities.Constants.KEY_FCM_TOKEN;
import static com.ghovo.c_find_.utilities.Constants.KEY_IMAGE;
import static com.ghovo.c_find_.utilities.Constants.KEY_IS_LIKED;
import static com.ghovo.c_find_.utilities.Constants.KEY_LATITUDE;
import static com.ghovo.c_find_.utilities.Constants.KEY_LONGITUDE;
import static com.ghovo.c_find_.utilities.Constants.KEY_NUMBER;
import static com.ghovo.c_find_.utilities.Constants.KEY_RECEIVER_EMAIL;
import static com.ghovo.c_find_.utilities.Constants.KEY_RECEIVER_ID;
import static com.ghovo.c_find_.utilities.Constants.KEY_RECEIVER_IMAGE;
import static com.ghovo.c_find_.utilities.Constants.KEY_RECEIVER_USER_NAME;
import static com.ghovo.c_find_.utilities.Constants.KEY_SENDER_EMAIL;
import static com.ghovo.c_find_.utilities.Constants.KEY_SENDER_ID;
import static com.ghovo.c_find_.utilities.Constants.KEY_SENDER_IMAGE;
import static com.ghovo.c_find_.utilities.Constants.KEY_SENDER_NUMBER;
import static com.ghovo.c_find_.utilities.Constants.KEY_SENDER_USER_NAME;
import static com.ghovo.c_find_.utilities.Constants.KEY_USER_ID;
import static com.ghovo.c_find_.utilities.Constants.KEY_USER_NAME;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ghovo.c_find_.R;
import com.ghovo.c_find_.adapters.UsersAdapter;
import com.ghovo.c_find_.databinding.ActivityMainBinding;
import com.ghovo.c_find_.dialogs.UserInfoDialog;
import com.ghovo.c_find_.listeners.DialogListener;
import com.ghovo.c_find_.listeners.UserListener;
import com.ghovo.c_find_.models.User;
import com.ghovo.c_find_.utilities.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends BaseActivity implements UserListener, DialogListener {

    private Boolean isLiked;
    private double latitude;
    private double longitude;
    private FusedLocationProviderClient fusedLocationClient;
    private User receiverUser;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore firebaseFirestore;
    private PreferenceManager preferenceManager;
    private ActivityMainBinding activityMainBinding;
    private LocationCallback locationCallback;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Handler handler;
    private Runnable runnable;
    private static final int DELAY_DURATION = 3000; // 3 seconds
    private FirebaseFirestore firestore;
    private Query query;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        preferenceManager = new PreferenceManager(getApplicationContext());
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        changeStatusBarColor();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    assert location != null;
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    updateDatabase();
                }
            }
        };

        checkLocationPermission();

        setContentView(activityMainBinding.getRoot());

        loadUserDetails();
        getToken();

        getUsers();

        setListeners();
        activityMainBinding.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                activityMainBinding.swipeRefresh.setRefreshing(false);
                getUsers();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }


    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            requestLocationUpdates();
        }
    }
    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    private void setListeners() {

        activityMainBinding.imageSignOut.setOnClickListener(v -> signOut());

        activityMainBinding.searchImage.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), HistoryActivity.class)));

        activityMainBinding.accountImage.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), AccountActivity.class)));

        activityMainBinding.likesImage.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), LikesActivity.class)));

        activityMainBinding.toMeetings.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), NotificationsActivity.class)));

    }

    private void loadUserDetails() {

        activityMainBinding.accountImage.setImageBitmap(getResizedBitmap(getReceiverUserImage(
                preferenceManager.getString(KEY_IMAGE)
        )));

        activityMainBinding.homeImage.setImageResource(R.drawable.ic_home_filled);
        activityMainBinding.searchImage.setImageResource(R.drawable.ic_search);

    }

    private final OnCompleteListener<QuerySnapshot> historyOnCompleteListener = task -> {

        if (!task.isSuccessful() || task.getResult() == null || task.getResult().getDocuments().size() == 0) {

            HashMap<String, Object> usersData = new HashMap<>();

            usersData.put(KEY_SENDER_ID, preferenceManager.getString(KEY_USER_ID));
            usersData.put(KEY_RECEIVER_ID, receiverUser.id);
            usersData.put(KEY_RECEIVER_IMAGE, receiverUser.image);
            usersData.put(KEY_RECEIVER_USER_NAME, receiverUser.userName);
            usersData.put(KEY_RECEIVER_EMAIL, receiverUser.email);
            usersData.put(KEY_IS_LIKED, isLiked);

            firebaseFirestore.collection(KEY_COLLECTION_HISTORY)
                    .add(usersData);

        } else {

            DocumentReference documentReferenceStatus = firebaseFirestore.collection(KEY_COLLECTION_HISTORY)
                    .document(task.getResult().getDocuments().get(0).getId());

            documentReferenceStatus.update(KEY_IS_LIKED, isLiked);

        }

    };

    private final OnCompleteListener<QuerySnapshot> requestOnCompleteListener = task -> {

        if (!task.isSuccessful() || task.getResult() == null || task.getResult().getDocuments().size() == 0) {

            HashMap<String, Object> usersData = new HashMap<>();

            usersData.put(KEY_SENDER_ID, preferenceManager.getString(KEY_USER_ID));
            usersData.put(KEY_SENDER_IMAGE, preferenceManager.getString(KEY_IMAGE));
            usersData.put(KEY_SENDER_USER_NAME, preferenceManager.getString(KEY_USER_NAME));
            usersData.put(KEY_SENDER_EMAIL, preferenceManager.getString(KEY_EMAIL));
            usersData.put(KEY_SENDER_NUMBER,preferenceManager.getString(KEY_NUMBER));
            usersData.put(KEY_RECEIVER_ID, receiverUser.id);

            firebaseFirestore.collection(KEY_COLLECTION_REQUEST)
                    .add(usersData);

        }

    };

    private void getUsers() {
        //loading(true);

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection(KEY_COLLECTION_USERS).whereEqualTo(KEY_ACTIVITY_FOR_SEARCH,true)
                .get()
                .addOnCompleteListener(task -> {

                    loading(false);
                    String currentUserId = preferenceManager.getString(KEY_USER_ID);

                    if (task.isSuccessful() && task.getResult() != null) {

                        List<User> userList = new ArrayList<>();

                        for (QueryDocumentSnapshot queryDocumentSnapshot: task.getResult()) {


//                            double latitudeInput = Double.parseDouble(Objects.requireNonNull(queryDocumentSnapshot.getString(KEY_LATITUDE)));
//                            double longitudeInput = Double.parseDouble(Objects.requireNonNull(queryDocumentSnapshot.getString(KEY_LONGITUDE)));

                            if (!currentUserId.equals(queryDocumentSnapshot.getId())) {
                                            User user = new User();
                                            user.userName = queryDocumentSnapshot.getString(KEY_USER_NAME);
                                            user.email = queryDocumentSnapshot.getString(KEY_EMAIL);
                                            user.image = queryDocumentSnapshot.getString(KEY_IMAGE);
                                            user.token = queryDocumentSnapshot.getString(KEY_FCM_TOKEN);
                                            user.id = queryDocumentSnapshot.getId();
                                            userList.add(user);


                            }

//                            Log.d("HELLO", "ISDistanceOk: " + ifDistanceIsOk(latitudeInput, longitudeInput));
//                            Log.d("HELLO", "LAT: " + String.valueOf(latitudeCurrent));
//                            Log.d("HELLO", "LNG: " + String.valueOf(longitudeCurrent));
//                            Log.d("HELLO", "LAT1: " + String.valueOf(latitudeInput));
//                            Log.d("HELLO", "LNG1: " + String.valueOf(longitudeInput));

                        }

                            if (userList.size() > 0) {

                                UsersAdapter usersAdapter = new UsersAdapter(userList, this);
                                activityMainBinding.usersRecyclerView.setAdapter(usersAdapter);

                                activityMainBinding.usersRecyclerView.setVisibility(View.VISIBLE);

                            } else {

                                showErrorMessage();

                            }

                    } else {

                        showErrorMessage();

                    }
                });

    }

    private void getToken() {

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(this::updateToken)
                .addOnFailureListener(v -> {

                    if (firebaseUser != null) {

                        firebaseUser.delete();

                    }

                    preferenceManager.clear();

                    showToast("Please Sign in again\n " +
                            "Your account will be deleted\n" +
                            "    Or connection Error    ");

                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();

                });

    }

    private void updateToken(String token) {

        preferenceManager.putString(KEY_FCM_TOKEN, token);

        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                firebaseFirestore.collection(KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(KEY_USER_ID)
                );

        documentReference.update(KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> {

                    if (firebaseUser != null) {

                        firebaseUser.delete();

                    }

                    preferenceManager.clear();

                    showToast("Please Sign in again\n " +
                            "Your account will be deleted\n" +
                            "    Or connection Error    ");

                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();

                });

    }

    private void signOut() {

        showToast("Signing out...");
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();

        DocumentReference documentReference =
                firebaseFirestore.collection(KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(KEY_USER_ID)
                );

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(KEY_FCM_TOKEN, FieldValue.delete());

        documentReference.update(updates)
                .addOnSuccessListener(unused -> {

                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();

                })
                .addOnFailureListener(e -> {

                    showToast("Unable to sign out");

                    if (firebaseUser != null) {

                        firebaseUser.delete();

                    }

                    preferenceManager.clear();

                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();

                });

    }

    private void showToast(String message) {

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

    }

    private void showErrorMessage() {

        activityMainBinding.textErrorMessage.setText(String.format("%s", "No User available"));
        activityMainBinding.textErrorMessage.setVisibility(View.VISIBLE);

    }

    private void loading(Boolean isLoading) {

        if (isLoading) {

            activityMainBinding.progressBar.setVisibility(View.VISIBLE);

        } else {

            activityMainBinding.progressBar.setVisibility(View.INVISIBLE);

        }
    }

    private void showDialog(String encodedImage) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        UserInfoDialog userInfoDialog = new UserInfoDialog(getReceiverUserImage(encodedImage), this);

        userInfoDialog.show(fragmentManager, "dialog");

    }

    private Bitmap getReceiverUserImage(String encodedImage) {

        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

    }

    private void changeStatusBarColor() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.white, getTheme()));
    }

    private Bitmap getResizedBitmap(Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) 400) / width;
        float scaleHeight = ((float) 400) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, width, height, matrix, false);
        bitmap.recycle();

        return resizedBitmap;

    }

    private void checkForHistory(String senderId, String receiverId) {

        firebaseFirestore.collection(KEY_COLLECTION_HISTORY)
                .whereEqualTo(KEY_SENDER_ID, senderId)
                .whereEqualTo(KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(historyOnCompleteListener);

    }

    private void checkForRequest(String senderId, String receiverId) {

        firebaseFirestore.collection(KEY_COLLECTION_REQUEST)
                .whereEqualTo(KEY_SENDER_ID, senderId)
                .whereEqualTo(KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(requestOnCompleteListener);

    }

    @Override
    public void onUserClicked(User user) {

        preferenceManager.putString(KEY_RECEIVER_IMAGE, user.image);
        preferenceManager.putString(KEY_RECEIVER_ID, user.id);
        preferenceManager.putString(KEY_RECEIVER_USER_NAME, user.userName);

        this.receiverUser = user;

        showDialog(user.image);

    }

    @Override
    public void onButtonClicked(Boolean flag) {

        isLiked = flag;

        if (flag) {

            checkForRequest(preferenceManager.getString(KEY_USER_ID), receiverUser.id);
            checkForHistory(preferenceManager.getString(KEY_USER_ID), receiverUser.id);

        } else {

            checkForHistory(preferenceManager.getString(KEY_USER_ID), receiverUser.id);

        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                // Permission denied, handle accordingly
            }
        }
    }


    private void updateDatabase(){
        DocumentReference documentReferenceStatus = firebaseFirestore.collection(KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(KEY_USER_ID));
        documentReferenceStatus.update(KEY_LATITUDE,String.valueOf(latitude));
        documentReferenceStatus.update(KEY_LONGITUDE,String.valueOf(longitude));
    }

    private boolean ifDistanceIsOk(double latitudeInput, double longitudeInput, double chosenDistance) {
        int earthRadius = 6371;

        double latDistance = Math.toRadians(latitude - latitudeInput);
        double lonDistance = Math.toRadians(longitude - longitudeInput);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latitudeInput)) * Math.cos(Math.toRadians(latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadius * c * 1000;

//        Log.d("hello", "Lat: " + latitudeInput);
//        Log.d("hello", "Long: " + longitudeInput);
//
//        Log.d("hello", "LAT: " + latitude);
//        Log.d("hello", "LONG: " + longitude);
//        Log.d("hello", "ifDistanceIsOk: " + distance);
        return distance<=chosenDistance;
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}