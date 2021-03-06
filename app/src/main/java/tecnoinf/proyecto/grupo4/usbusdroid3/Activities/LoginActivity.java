package tecnoinf.proyecto.grupo4.usbusdroid3.Activities;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.Image;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import tecnoinf.proyecto.grupo4.usbusdroid3.Helpers.RestCall;
import tecnoinf.proyecto.grupo4.usbusdroid3.R;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity
        implements LoaderCallbacks<Cursor>, GoogleApiClient.OnConnectionFailedListener {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "XyYrh7Uvf4EylxNc7qr6IAjJ5";
    private static final String TWITTER_SECRET = "ga160r9tq7krioNOFSCy7nD21RNr6FC8hZm2tQ8UHb0cFNj4Nd";
    private TwitterLoginButton loginButton;
    private String saved_username;
    private String saved_password;
    private String saved_usertype;
    private String saved_tenantId;

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;
    private static final String POST = "POST";
    private static String loginURL;
    private static String registerURL;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private SharedPreferences sharedPreferences;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private View mTwitterButton;
    private String external_email;
    private ImageButton settingsButton;
    private ImageButton registerButton;
    private SignInButton googleSignInButton;

    private GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        googleSignInButton = (SignInButton) findViewById(R.id.google_sign_in_button);
        assert googleSignInButton != null;
        googleSignInButton.setSize(SignInButton.SIZE_WIDE);
        googleSignInButton.setScopes(gso.getScopeArray());

        loginURL = getString(R.string.URLlogin, getString(R.string.URL_REST_API));
        registerURL =  getString(R.string.URLregister, getString(R.string.URL_REST_API));

        sharedPreferences = getSharedPreferences("USBusData", Context.MODE_PRIVATE);

        String savedServerIP = sharedPreferences.getString("serverIP", "");
        String savedPort = sharedPreferences.getString("port", "");
        saved_tenantId = sharedPreferences.getString("tenantId", "");

        if (!savedServerIP.isEmpty() && !savedPort.isEmpty()) {
            loginURL = loginURL.replace("10.0.2.2", savedServerIP).replace(":8080", ":"+savedPort);
            registerURL = registerURL.replace("10.0.2.2", savedServerIP).replace(":8080", ":"+savedPort);
        }

        saved_usertype = sharedPreferences.getString("user_type", "");
        saved_password = sharedPreferences.getString("password", "first_use");
        if(!saved_password.equalsIgnoreCase("first_use") && !saved_password.isEmpty()) {
            saved_username = sharedPreferences.getString("username", "");

            mAuthTask = new UserLoginTask(saved_username, saved_password, getApplicationContext(), saved_usertype);
            mAuthTask.execute((Void) null);
        }

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.username);
        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        registerButton = (ImageButton) findViewById(R.id.registerBtn);
        assert registerButton != null;
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registrationIntent = new Intent(getBaseContext(), RegistrationActivity.class);
                startActivity(registrationIntent);
            }
        });

        settingsButton = (ImageButton) findViewById(R.id.settingsBtn);
        assert settingsButton != null;
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        assert mEmailSignInButton != null;
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
        mTwitterButton = findViewById(R.id.twitter_login_button);

        loginButton = (TwitterLoginButton) findViewById(R.id.twitter_login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {

                showProgress(true);
                TwitterSession session = result.data;

                mAuthTask = new UserLoginTask("twitter_"+session.getUserName(), session.getAuthToken().token, getApplicationContext(), "twitter");
                mAuthTask.execute((Void) null);

            }
            @Override
            public void failure(TwitterException exception) {
                Log.d("TwitterKit", "Login with Twitter failure", exception);
            }
        });

        googleSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        // Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void googleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password, getApplicationContext(), "usbus");
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        mProgressView.setVisibility(show? View.VISIBLE : View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mTwitterButton.setVisibility(show ? View.GONE : View.VISIBLE);
            settingsButton.setVisibility(show ? View.GONE : View.VISIBLE);
            registerButton.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            googleSignInButton.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mTwitterButton.setVisibility(show ? View.GONE : View.VISIBLE);
            settingsButton.setVisibility(show ? View.GONE : View.VISIBLE);
            registerButton.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            googleSignInButton.setVisibility(show ? View.GONE : View.VISIBLE);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String username;
        private final String mPassword;
        private final String mType;
        private String token;
        private Context mCtx;

        UserLoginTask(String user, String password, Context ctx, String type) {
            username = user;
            mPassword = password;
            mCtx = ctx;
            mType = type;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            JSONObject result;
            JSONObject registerResult;
            try {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                JSONObject credentials = new JSONObject();
                credentials.put("username", username);
                if(saved_tenantId.isEmpty()) {
                    credentials.put("tenantId", mCtx.getString(R.string.tenantId));
                } else {
                    credentials.put("tenantId", saved_tenantId);
                }
                credentials.put("password", mPassword);
                System.out.println("=+=+=+=+=+=+=+=+=+=+=+=+=+=+token twitter/google: " + mPassword);

                RestCall call = new RestCall(loginURL, POST, credentials, null);
                result = call.getData();
                //String dummy = result.toString();
                System.out.println(result);
                if(result.get("result").toString().equalsIgnoreCase("OK")) {
                    //login OK
                    System.out.println("LOGIN OK...");
                    JSONObject data = new JSONObject(result.get("data").toString());
                    token = data.getString("token");

                    editor.putString("token", token);
                    editor.putString("username", username);
                    editor.putString("password", mPassword);
                    editor.putString("user_type", mType);
                    if(saved_tenantId.isEmpty()) {
                        editor.putString("tenantId", getString(R.string.tenantId));
                    } else {
                        editor.putString("tenantId", saved_tenantId);
                    }
                    editor.putString("loginURL", loginURL);
                    editor.apply();
                } else if (mType.equalsIgnoreCase("twitter") || mType.equalsIgnoreCase("google")) {
                    if(external_email == null || external_email.isEmpty()){
                        credentials.put("email", username + "@" + mType + "client.usbus");
                    } else {
                        credentials.put("email", external_email);
                    }
                    RestCall registerCall = new RestCall(registerURL, POST, credentials, null);
                    registerResult = registerCall.getData();
                    if(registerResult.get("result").toString().equalsIgnoreCase("OK")) {
                        //register OK
                        System.out.println("REGISTRO TWITTER/GOOGLE OK...");
                        credentials.remove("email");

                        call = new RestCall(loginURL, POST, credentials, null);
                        result = call.getData();
                        if(result.get("result").toString().equalsIgnoreCase("OK")) {
                            //login OK
                            System.out.println("LOGIN AFTER TWITTER/GOOGLE REGISTER OK...");
                            JSONObject data = new JSONObject(result.get("data").toString());
                            token = data.getString("token");
                            editor.putString("token", token);
                            editor.putString("username", username);
                            editor.putString("password", mPassword);
                            editor.putString("user_type", mType);
                            if(saved_tenantId.isEmpty()) {
                                editor.putString("tenantId", getString(R.string.tenantId));
                            } else {
                                editor.putString("tenantId", saved_tenantId);
                            }
                            editor.apply();
                        } else {
                            //algun error
                            System.out.println("DANGER WILL ROBINSON..." + result.get("result").toString());
                            return false;
                        }
                    }

                } else {
                    //algun error
                    System.out.println("DANGER WILL ROBINSON..." + result.get("result").toString());
                    return false;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                showProgress(true);
                Intent mainIntent = new Intent(getBaseContext(), MainClient.class);
//                mainIntent.putExtra("token", token);
//                mainIntent.putExtra("username", username);
                startActivity(mainIntent);

                finish();
            } else {
                showProgress(false);
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
//        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
//            mStatusTextView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
//            updateUI(true);
            assert acct != null;
            external_email = acct.getEmail();
            mAuthTask = new UserLoginTask("google_"+acct.getDisplayName(), acct.getId(), getApplicationContext(), "google");
            mAuthTask.execute((Void) null);

        } else {
            // Signed out, show unauthenticated UI.
//            updateUI(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }

        super.onActivityResult(requestCode, resultCode, data);
        loginButton.onActivityResult(requestCode, resultCode, data);

    }
}

