package com.ghovo.c_find_.activities;


import static com.ghovo.c_find_.utilities.Constants.IMAGE_HEIGHT;
import static com.ghovo.c_find_.utilities.Constants.IMAGE_WIDTH;
import static com.ghovo.c_find_.utilities.Constants.KEY_ACTIVITY_FOR_SEARCH;
import static com.ghovo.c_find_.utilities.Constants.KEY_COLLECTION_USERS;
import static com.ghovo.c_find_.utilities.Constants.KEY_DISTANCE;
import static com.ghovo.c_find_.utilities.Constants.KEY_EMAIL;
import static com.ghovo.c_find_.utilities.Constants.KEY_IMAGE;
import static com.ghovo.c_find_.utilities.Constants.KEY_LATITUDE;
import static com.ghovo.c_find_.utilities.Constants.KEY_LONGITUDE;
import static com.ghovo.c_find_.utilities.Constants.KEY_NUMBER;
import static com.ghovo.c_find_.utilities.Constants.KEY_USER_ID;
import static com.ghovo.c_find_.utilities.Constants.KEY_USER_NAME;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ghovo.c_find_.R;
import com.ghovo.c_find_.databinding.ActivitySignUpBinding;
import com.ghovo.c_find_.utilities.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class SignUpActivity extends AppCompatActivity {

    private double latitude;
    private double longitude;
    private String encodedImage;
    private FirebaseAuth firebaseAuth;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firebaseFirestore;
    private ActivitySignUpBinding activitySignUpBinding;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 10000;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {

                    if (result.getData() != null) {

                        Uri imageUri = result.getData().getData();

                        try {

                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                            activitySignUpBinding.imageProfile.setImageBitmap(bitmap);
                            activitySignUpBinding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodedImage(bitmap);

                        } catch (FileNotFoundException e) {

                            e.printStackTrace();

                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(getApplicationContext());
        activitySignUpBinding = ActivitySignUpBinding.inflate(getLayoutInflater());
        changeStatusBarColor();

        setContentView(activitySignUpBinding.getRoot());

        setListeners();
    }


    private void setListeners() {

        activitySignUpBinding.textSignIn.setOnClickListener(v -> onBackPressed());

        activitySignUpBinding.buttonSignUp.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signUp();

            }
        });

        activitySignUpBinding.layoutImage.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);

        });

        activitySignUpBinding.inputUserName.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (encodedImage == null) {

                    activitySignUpBinding.imageProfile.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_incorrect_input
                    ));

                    activitySignUpBinding.textAddImage.setText(R.string.add_image);

                } else {

                    activitySignUpBinding.imageProfile.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_correct_input
                    ));

                    activitySignUpBinding.textAddImage.setText("");

                }

                if (charSequence.length() > 3) {

                    activitySignUpBinding.inputUserName.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_correct_input
                    ));

                    activitySignUpBinding.textUserName.setText("");

                } else {

                    activitySignUpBinding.inputUserName.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_incorrect_input
                    ));

                    activitySignUpBinding.textUserName.setText(R.string.short_username);

                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

        });

        activitySignUpBinding.inputEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (encodedImage == null) {

                    activitySignUpBinding.imageProfile.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_incorrect_input
                    ));

                    activitySignUpBinding.textAddImage.setText(R.string.add_image);

                } else {

                    activitySignUpBinding.imageProfile.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_correct_input
                    ));

                    activitySignUpBinding.textAddImage.setText("");

                }

                if (Patterns.EMAIL_ADDRESS.matcher(charSequence.toString()).matches()) {

                    activitySignUpBinding.inputEmail.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_correct_input
                    ));

                    activitySignUpBinding.textEmail.setText("");

                } else {

                    activitySignUpBinding.inputEmail.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_incorrect_input
                    ));

                    activitySignUpBinding.textEmail.setText(R.string.unknown_email);

                }


            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

        });

        activitySignUpBinding.inputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (encodedImage == null) {

                    activitySignUpBinding.imageProfile.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_incorrect_input
                    ));

                    activitySignUpBinding.textAddImage.setText(R.string.add_image);

                } else {

                    activitySignUpBinding.imageProfile.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_correct_input
                    ));

                    activitySignUpBinding.textAddImage.setText("");

                }

                if (charSequence.length() < 6) {

                    activitySignUpBinding.inputPassword.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_incorrect_input
                    ));

                    activitySignUpBinding.textPassword.setText(R.string.poor_password);

                } else if (charSequence.length() <= 8) {

                    activitySignUpBinding.inputPassword.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_password_input
                    ));

                    activitySignUpBinding.textPassword.setText(R.string.fair_password);

                } else {

                    activitySignUpBinding.inputPassword.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_correct_input
                    ));

                    activitySignUpBinding.textPassword.setText("");

                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

        });



        activitySignUpBinding.inputConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (encodedImage == null) {

                    activitySignUpBinding.imageProfile.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_incorrect_input
                    ));

                    activitySignUpBinding.textAddImage.setText(R.string.add_image);

                } else {

                    activitySignUpBinding.imageProfile.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_correct_input
                    ));

                    activitySignUpBinding.textAddImage.setText("");

                }

                if (activitySignUpBinding.inputPassword.getText().toString().
                        equals(activitySignUpBinding.inputConfirmPassword.getText().toString())) {

                    activitySignUpBinding.inputConfirmPassword.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_correct_input
                    ));

                    activitySignUpBinding.inputPassword.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_correct_input
                    ));

                    activitySignUpBinding.textPassword.setText("");
                    activitySignUpBinding.textConfirmPassword.setText("");

                } else {

                    activitySignUpBinding.inputPassword.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_incorrect_input
                    ));

                    activitySignUpBinding.inputConfirmPassword.setBackground(AppCompatResources.getDrawable(
                            getApplicationContext(), R.drawable.background_incorrect_input
                    ));

                    activitySignUpBinding.textPassword.setText(R.string.match_password);
                    activitySignUpBinding.textConfirmPassword.setText(R.string.match_password);

                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

        });

    }

    private void showToast(String message) {

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

    }

    private Boolean isValidSignUpDetails() {

        if (encodedImage == null) {

            showToast("Select profile image");
            return false;

        } else if (activitySignUpBinding.inputUserName.getText().toString().trim().isEmpty()) {

            showToast("Enter username");
            return false;

        } else if (activitySignUpBinding.inputEmail.getText().toString().trim().isEmpty()) {

            showToast("Enter email");
            return false;

        } else if (!Patterns.EMAIL_ADDRESS.matcher(activitySignUpBinding.inputEmail.getText().toString()).matches()) {

            showToast("Enter valid email!");
            return false;

        } else if (activitySignUpBinding.inputPassword.getText().toString().trim().isEmpty()) {

            showToast("Enter password");
            return false;

        } else if (activitySignUpBinding.inputConfirmPassword.getText().toString().trim().isEmpty()) {

            showToast("Confirm password");
            return false;

        } else if (!activitySignUpBinding.inputPassword.getText().toString().
                equals(activitySignUpBinding.inputConfirmPassword.getText().toString())) {

            showToast("Passwords doesn't matching");
            return false;

        }
        else {

            return true;

        }

    }

    private void loading(Boolean isLoading) {

        if (isLoading) {

            activitySignUpBinding.buttonSignUp.setVisibility(View.INVISIBLE);
            activitySignUpBinding.progressBar.setVisibility(View.VISIBLE);

        } else {

            activitySignUpBinding.buttonSignUp.setVisibility(View.VISIBLE);
            activitySignUpBinding.progressBar.setVisibility(View.INVISIBLE);

        }

    }

    private String encodedImage(Bitmap bitmap) {

        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.WEBP, 95, byteArrayOutputStream);

        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);

    }

    private void changeStatusBarColor() {

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.white, getTheme()));

    }

    private void inputErrorVisualisation(int code) {

        if (code == 1) {

            activitySignUpBinding.inputUserName.setBackground(AppCompatResources.getDrawable(
                    getApplicationContext(), R.drawable.background_incorrect_input
            ));

            activitySignUpBinding.inputEmail.setBackground(AppCompatResources.getDrawable(
                    getApplicationContext(), R.drawable.background_correct_input
            ));

            activitySignUpBinding.textEmail.setText("");
            activitySignUpBinding.textUserName.setText(R.string.used_username);

        } else if (code == 2) {

            activitySignUpBinding.inputEmail.setBackground(AppCompatResources.getDrawable(
                    getApplicationContext(), R.drawable.background_incorrect_input
            ));

            activitySignUpBinding.inputUserName.setBackground(AppCompatResources.getDrawable(
                    getApplicationContext(), R.drawable.background_correct_input
            ));

            activitySignUpBinding.textEmail.setText(R.string.used_email);
            activitySignUpBinding.textUserName.setText("");

        }

    }

    private void signUp() {

        loading(true);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        firebaseFirestore.collection(KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(v -> {

                    AtomicBoolean flag = new AtomicBoolean(true);

                    for (QueryDocumentSnapshot queryDocumentSnapshot : v.getResult()) {

                        if (activitySignUpBinding.inputUserName.getText().toString().trim()
                                .equals(queryDocumentSnapshot.getString(KEY_USER_NAME))) {

                            flag.set(false);
                            break;

                        }

                    }

                    if (flag.get()) {

                        firebaseAuth.createUserWithEmailAndPassword(activitySignUpBinding.inputEmail.getText().toString(),
                                        activitySignUpBinding.inputPassword.getText().toString())
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {

                                        HashMap<String, Object> userData = new HashMap<>();

                                        userData.put(KEY_USER_NAME, activitySignUpBinding.inputUserName.getText().toString().trim().toLowerCase());
                                        userData.put(KEY_EMAIL, activitySignUpBinding.inputEmail.getText().toString());
                                        userData.put(KEY_IMAGE, encodedImage);
                                        userData.put(KEY_ACTIVITY_FOR_SEARCH, true);
                                        userData.put(KEY_LATITUDE, "0.0");
                                        userData.put(KEY_LONGITUDE, "0.0");
                                        userData.put(KEY_DISTANCE, "1000");

                                        firebaseFirestore.collection(KEY_COLLECTION_USERS)
                                                .add(userData)
                                                .addOnSuccessListener(documentReference -> {
                                                    preferenceManager.putString(KEY_USER_ID, documentReference.getId());
                                                    preferenceManager.putString(KEY_USER_NAME, activitySignUpBinding.inputUserName.getText().toString().trim());
                                                    preferenceManager.putString(KEY_IMAGE, encodedImage);


                                                    showToast("Sign up is successful\n" +
                                                            "Verify your account");

                                                    Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                                                    startActivity(intent);
                                                    finish();

                                                    loading(false);

                                                })
                                                .addOnFailureListener(e -> {

                                                    showToast(e.getMessage());
                                                    Objects.requireNonNull(firebaseAuth.getCurrentUser()).delete();

                                                });


                                    } else if (Objects.equals(
                                            Objects.requireNonNull(task.getException()).getClass(),
                                            FirebaseAuthUserCollisionException.class)) {
                                        loading(false);

                                        inputErrorVisualisation(2);
                                        showToast("Email already used");

                                    } else if (Objects.equals(
                                            Objects.requireNonNull(task.getException()).getClass(),
                                            FirebaseAuthWeakPasswordException.class)) {

                                        loading(false);
                                        showToast("Password is short");

                                    } else {

                                        showToast("Sign up isn't successful");
                                        loading(false);

                                    }

                                });

                    } else {

                        inputErrorVisualisation(1);
                        showToast("This username is already used");
                        loading(false);
                    }
                });
    }
}