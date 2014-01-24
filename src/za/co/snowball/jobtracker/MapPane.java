package za.co.snowball.jobtracker;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MapPane extends Activity {

	private static final String TAG = "MapPane";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		Intent i = getIntent();
		String department = i.getStringExtra("department");
		String client = i.getStringExtra("client");
		String address = i.getStringExtra("address");
		double lat = i.getDoubleExtra("lat", 0);
		double lng = i.getDoubleExtra("lng", 0);

		setTitle(department + " at " + address);

		// Get a handle to the Map Fragment
		GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

		LatLng marker = new LatLng(lat, lng);
		
		// TODO Add true exceptions		
		try {
			map.setMyLocationEnabled(true);
		} catch (Exception e) {
			Log.e(TAG, "Could not setMyLocation");
		}
		
		try {
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 13));	
		} catch (Exception e) {
			Log.e(TAG, "Could not moveCamera");
		}
		
		try {
			map.addMarker(new MarkerOptions().title(client).snippet(address).position(marker));	
		} catch (Exception e) {
			Log.e(TAG, "Could not addMarker");
		}
		

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}