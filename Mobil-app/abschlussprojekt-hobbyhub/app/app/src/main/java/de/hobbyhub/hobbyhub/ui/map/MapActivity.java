package de.hobbyhub.hobbyhub.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import de.hobbyhub.hobbyhub.R;
import de.hobbyhub.hobbyhub.database.AppDatabase;
import de.hobbyhub.hobbyhub.model.User;
import de.hobbyhub.hobbyhub.ui.login.LoginActivity;
import de.hobbyhub.hobbyhub.ui.profile.ProfileActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapActivity extends AppCompatActivity {

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    private MapView mapView;
    private User user;
    private Disposable userDisposable;

    @Override
    protected void onStart() {
        super.onStart();
        String originalId = getIntent().getExtras().getString(getString(R.string.original_id));
        userDisposable = AppDatabase.getDatabase(getApplicationContext()).userDao().getUserByOriginId(originalId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((dbUser, throwable) -> user = dbUser);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarMap);
        setSupportActionBar(toolbar);

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this, gso);

        loadMap();

    }

    public void loadMap(){

        // Initialize the OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        // Get a reference to the map view from the layout
        mapView = findViewById(R.id.mapview);
        // Set the tile source to the default Mapnik source
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        // Set the map view to show the user's location (requires location permissions)
        mapView.setClickable(true);
        GeoPoint startPoint = new GeoPoint(50.9482, 10.2652); //middle of germany
        mapView.getController().setCenter(startPoint);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(7.4);

        // Add a marker to the map at a specific latitude and longitude
        Marker location_1 = new Marker(mapView);
        location_1.setPosition(new GeoPoint(48.1351, 11.5820));
        location_1.setTitle("Julia");
        location_1.setSnippet("hier ist Julia");
        location_1.setIcon(ResourcesCompat.getDrawable(getResources(), R.mipmap.picture_julia, null));
        location_1.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Add an OnMarkerClickListener to the marker
        location_1.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                // Start a new activity when the user clicks on the marker
                Intent intent = new Intent(MapActivity.this, ProfileActivity.class);
                startActivity(intent);
                return true;
            }

        });

        // add the marker to the map view
        mapView.getOverlays().add(location_1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar_map, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                logout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userDisposable != null && !userDisposable.isDisposed()) {
            userDisposable.dispose();
        }
    }

    private void logout() {
        gsc.signOut();
        LoginManager.getInstance().logOut();

        startActivity(new Intent(MapActivity.this, LoginActivity.class));
        finish();
    }
}