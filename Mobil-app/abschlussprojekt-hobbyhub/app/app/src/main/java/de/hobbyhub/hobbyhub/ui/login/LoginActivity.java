package de.hobbyhub.hobbyhub.ui.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;

import org.json.JSONException;

import de.hobbyhub.hobbyhub.R;
import de.hobbyhub.hobbyhub.api.FacebookUserApi;
import de.hobbyhub.hobbyhub.database.AppDatabase;
import de.hobbyhub.hobbyhub.model.User;
import de.hobbyhub.hobbyhub.ui.map.MapActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity {

    private GoogleSignInOptions gso;
    private GoogleSignInClient gsc;
    private CallbackManager callbackManager;
    private Disposable userDisposable;
    private Disposable insertDisposable;

    @Override
    protected void onStart() {
        super.onStart();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            // user already logged in
            loadMapActivity(account.getId());
        }
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            FacebookUserApi facebookUserApi = new FacebookUserApi();
            facebookUserApi.asyncGetUserByAccessTokenWithCallback(accessToken, loadMapActivityCallback());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //button to Map, shortcut for development, remove later
        Button button = findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the new activity
                Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                startActivity(intent);
            }
        });


        // Facebook Login
        callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setPermissions(getString(R.string.facebook_key_email));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                FacebookUserApi facebookUserApi = new FacebookUserApi();
                facebookUserApi.asyncGetUserByAccessTokenWithCallback(loginResult.getAccessToken(), openMapActivityWithUserCallback());
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(@NonNull FacebookException e) {
                Toast.makeText(LoginActivity.this, getString(R.string.facebook_login_error), Toast.LENGTH_SHORT).show();
                Log.d("Facebook Login", e.getMessage());
            }
        });

        // Google Login
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);

        signInButton.setOnClickListener(view -> {
            switch (view.getId()) {
                case R.id.sign_in_button:
                    googleSignIn();
                    break;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userDisposable != null && !userDisposable.isDisposed()) {
            userDisposable.dispose();
        }
        if (insertDisposable != null && !insertDisposable.isDisposed()) {
            insertDisposable.dispose();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void googleSignIn() {
        Intent signInIntent = gsc.getSignInIntent();
        loginResultLauncher.launch(signInIntent);
    }

    private final ActivityResultLauncher<Intent> loginResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    User googleUser = new User(
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult().getDisplayName(),
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult().getEmail(),
                            GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult().getId());
                    saveUserInDBAndLoadMapActivity(googleUser);
                } else {
                    Toast.makeText(this, getString(R.string.google_signin_error), Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void saveUserInDBAndLoadMapActivity(User user) {
        userDisposable = AppDatabase.getDatabase(getApplicationContext()).userDao()
                .getUserByOriginId(user.getOriginalId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((dbUser, throwable) -> {
                    if (dbUser != null) {
                        Log.d("Database", "User " + dbUser.getDisplayName() + " already exists. Loading map activity...");
                        loadMapActivity(dbUser.getOriginalId());
                    } else {
                        Log.d("Database", "User not found");
                        insertDisposable = AppDatabase.getDatabase(getApplicationContext()).userDao()
                                .insertUser(user)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(insertedId -> {
                                    Log.d("Database", "Created new user " + user.getDisplayName() + " with id " + insertedId);
                                    loadMapActivity(user.getOriginalId());
                                });
                    }
                });
    }

    private GraphRequest.Callback openMapActivityWithUserCallback() {
        return graphResponse -> {
            if (graphResponse.getError() == null) {
                try {
                    String id = graphResponse.getJsonObject().getString(getString(R.string.facebook_key_id));
                    String name = graphResponse.getJsonObject().getString(getString(R.string.facebook_key_name));
                    String email = graphResponse.getJsonObject().getString(getString(R.string.facebook_key_email));
                    User facebookUser = new User(name, email, id);
                    saveUserInDBAndLoadMapActivity(facebookUser);
                } catch (JSONException | NullPointerException e) {
                    Toast.makeText(this, getString(R.string.facebook_process_error), Toast.LENGTH_SHORT).show();
                    LoginManager.getInstance().logOut();
                }
            }
        };
    }

    private GraphRequest.Callback loadMapActivityCallback() {
        return graphResponse -> {
            try {
                String originalId = graphResponse.getJsonObject().getString(getString(R.string.facebook_key_id));
                loadMapActivity(originalId);
            } catch (JSONException | NullPointerException e) {
                Toast.makeText(this, getString(R.string.facebook_process_error), Toast.LENGTH_SHORT).show();
                LoginManager.getInstance().logOut();
            }
        };
    }

    private void loadMapActivity(String originalId) {
        Intent mapIntent = new Intent(LoginActivity.this, MapActivity.class);
        mapIntent.putExtra(getString(R.string.original_id), originalId);
        startActivity(mapIntent);
        finish();
    }
}