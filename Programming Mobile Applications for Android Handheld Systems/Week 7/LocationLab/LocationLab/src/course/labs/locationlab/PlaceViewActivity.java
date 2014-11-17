package course.labs.locationlab;


import android.app.ListActivity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class PlaceViewActivity extends ListActivity implements LocationListener {
	private static final long FIVE_MINS = 5 * 60 * 1000;
	private static final String TAG = "Lab-Location";

	// Set to false if you don't have network access
	public static boolean sHasNetwork = false;

	private Location mLastLocationReading;
	private PlaceViewAdapter mAdapter;
	private LocationManager mLocationManager;
	private boolean mMockLocationOn = false;

	// default minimum time between new readings
	private long mMinTime = 5000;

	// default minimum distance between old and new readings.
	private float mMinDistance = 1000.0f;

	// A fake location provider used for testing
	private MockLocationProvider mMockLocationProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set up the app's user interface. This class is a ListActivity, 
		// so it has its own ListView. ListView's adapter should be a PlaceViewAdapter

		if (null == (mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE)))
			finish();

		mAdapter = new PlaceViewAdapter(getApplicationContext());
		getListView().setFooterDividersEnabled(true);

		// Add a footerView to the ListView
		// You can use footer_view.xml to define the footer
		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TextView footer = (TextView) inflater.inflate(R.layout.footer_view,	null);
		getListView().addFooterView(footer);
		getListView().setAdapter(mAdapter);

		footer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Log.i(TAG, "Entered footerView.OnClickListener.onClick()");

				if (!mAdapter.intersects(mLastLocationReading)) {
                   	Log.i(TAG, "Starting Place Download");
					new PlaceDownloaderTask(PlaceViewActivity.this, true)
							.execute(mLastLocationReading);
				} else if (mAdapter.intersects(mLastLocationReading)) {
                   	Log.i(TAG, "You already have this location badge");
					Toast.makeText(getApplicationContext(),
							"You already have this location badge.",
							Toast.LENGTH_SHORT).show();
				} else if (mLastLocationReading == null) {
                    Log.i(TAG, "Location data is not available");
					Toast.makeText(getApplicationContext(),
							"Current Location is unavailable.",
							Toast.LENGTH_SHORT).show();
				}

			}

		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		startMockLocationManager();

		// TODO - Check NETWORK_PROVIDER for an existing location reading.
		// Only keep this last reading if it is fresh - less than 5 minutes old
		
		mLastLocationReading = mLocationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		if (mLastLocationReading != null
				&& ageInMilliseconds(mLastLocationReading) > FIVE_MINS) {
			mLastLocationReading = null;
		}

		// Register to receive location updates from NETWORK_PROVIDER

		mLocationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, this);
	}

	@Override
	protected void onPause() {

		// TODO - unregister for location updates
        mLocationManager.removeUpdates(this);
		
		shutdownMockLocationManager();
		super.onPause();
	}

	// Callback method used by PlaceDownloaderTask
	public void addNewPlace(PlaceRecord place) {
		Log.i(TAG, "Entered addNewPlace()");

		// TODO - Attempt to add place to the adapter, considering the following cases

		// A PlaceBadge for this location already exists. In this case issue a Toast message
		// with the text - "You already have this location badge." Use the PlaceRecord 
		// class' intersects() method to determine whether a PlaceBadge already exists
		// for a given location. Do not add the PlaceBadge to the adapter
		
		// The place is null. In this case issue a Toast message with the text
		// "PlaceBadge could not be acquired"
		// Do not add the PlaceBadge to the adapter
		
		// The place has no country name. In this case issue a Toast message
		// with the text - "There is no country at this location". 
		// Do not add the PlaceBadge to the adapter
		
		// Otherwise - add the PlaceBadge to the adapter
		mAdapter.add(place);
		
	}

	// LocationListener methods
	@Override
	public void onLocationChanged(Location currentLocation) {

		// TODO - Update last location considering the following cases.
		// 1) If there is no last location, set the last location to the current
		// location.
		if (mLastLocationReading == null) {
			mLastLocationReading = currentLocation;
		}
		// 2) If the current location is older than the last location, ignore
		// the current location
		else if (currentLocation.getTime() < mLastLocationReading.getTime()) {
			return;
		}
		// 3) If the current location is newer than the last locations, keep the
		// current location.
		else {
			mLastLocationReading = currentLocation;
		}
		
//		mLastLocationReading = null;
		
//		if (mLastLocationReading == null
//				|| this.ageInMilliseconds(mLastLocationReading) > this.ageInMilliseconds(currentLocation)) {
//			mLastLocationReading = currentLocation;
//		}

	}

	@Override
	public void onProviderDisabled(String provider) {
		// not implemented
	}

	@Override
	public void onProviderEnabled(String provider) {
		// not implemented
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// not implemented
	}

	// Returns age of location in milliseconds
	private long ageInMilliseconds(Location location) {
		return System.currentTimeMillis() - location.getTime();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete_badges:
			mAdapter.removeAllViews();
			return true;
		case R.id.place_one:
			mMockLocationProvider.pushLocation(37.422, -122.084);
			return true;
		case R.id.place_no_country:
			mMockLocationProvider.pushLocation(0, 0);
			return true;
		case R.id.place_two:
			mMockLocationProvider.pushLocation(38.996667, -76.9275);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void shutdownMockLocationManager() {
		if (mMockLocationOn) {
			mMockLocationProvider.shutdown();
		}
	}

	private void startMockLocationManager() {
		if (!mMockLocationOn) {
			mMockLocationProvider = new MockLocationProvider(
					LocationManager.NETWORK_PROVIDER, this);
		}
	}
}
