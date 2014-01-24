/*
 * Build a custom layout for edit text, checkbox, and dropdown using JSON
 * 
 * customfields":[{"id":"11","type":"support","relid":"5","fieldname":"High Site","fieldtype":"dropdown","description":"","fieldoptions":"Bottelary,Hawekwa,Simonsberg","required":"on"},{"id":"13","type":"support","relid":"5","fieldname":"Survey Successful?","fieldtype":"tickbox","description":"","fieldoptions":"","required":"on"}]
 * 
 */

package za.co.snowball.jobtracker;

import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

import za.co.snowball.jobtracker.db.MyContentProvider;
import za.co.snowball.jobtracker.db.Table;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

public class BuildCustomLayout extends Activity {

	protected static final String TAG = "BuildCustomLayout";
	private Dialog dialog;
	
	private Uri mJobUri;	
	private String mCustomFields;
	
	private CheckBox[] mCheckBox;
	private EditText[] mEditText;
	
	private List<Map<String, String>> mFieldTypes;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Create a root LinearLayout to hold all the built up items
		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

		// SavedInstance or bundle retrieves JSON
		Bundle extras = getIntent().getExtras();		
		if (savedInstanceState != null) {			
			mJobUri = (Uri) savedInstanceState.getParcelable(MyContentProvider.CONTENT_ITEM_TYPE);
			mCustomFields = (String) savedInstanceState.getString(Table.COLUMN_JOB_CUSTOM_FIELDS);
		} else {
			mJobUri = extras.getParcelable(MyContentProvider.CONTENT_ITEM_TYPE);
			mCustomFields = extras.getString(Table.COLUMN_JOB_CUSTOM_FIELDS);			
		}
		Log.d(TAG, "jobUri: " + mJobUri);
		Log.d(TAG, "mCustomFields: " + mCustomFields);
		
		JsonHelper json = new JsonHelper();
		JSONObject customFieldsJson = json.convertStringToJSONObject(mCustomFields);
		if (customFieldsJson == null) {
			//throw Exception;
		}
		
		mFieldTypes = json.createCustomFieldMap(customFieldsJson);

		mCheckBox = new CheckBox[20];			
		mEditText = new EditText[20];
		
		int i = 0;
		
		for (Map<String,String> cursor : mFieldTypes) {
			String fieldtype = cursor.get("fieldtype");
			String fieldname = cursor.get("fieldname");
			String fieldoptions = cursor.get("fieldoptions");
			Log.d(TAG, "fieldoptions are " + fieldoptions);
			String value = cursor.get("value");
			Log.d(TAG, "value " + value);
			// Create tick boxes
			if (fieldtype.equals("tickbox")) {
				Log.i(TAG, "Creating tickbox");
				mCheckBox[i] = new CheckBox(this);
				mCheckBox[i].setText(fieldname);
				if (value.equals("checked")) {
					mCheckBox[i].setChecked(true);
				}
				LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				mCheckBox[i].setLayoutParams(cblp);
				linearLayout.addView(mCheckBox[i]);
			}
			// Create text fields
			if (fieldtype.equals("text")) {
				Log.i(TAG, "Creating EditText");				
				mEditText[i] = new EditText(this);
				mEditText[i].setHint(fieldname);
				mEditText[i].setText(value);
				LinearLayout.LayoutParams etlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				mEditText[i].setLayoutParams(etlp);
				linearLayout.addView(mEditText[i]);	
			}
			// Create drop downs but create buttons but create a dialog referenced by the button first
			// TODO If empty field options are sent it would be useless to build a drop down (and app might crash)
			if (fieldtype.equals("dropdown")) {				
				Log.i(TAG, "Creating dialog");				
				dialog = new Dialog(this);		
				
				String[] stringArray = fieldoptions.split(",");
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(fieldname);
				builder.setItems(stringArray, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	String itemString = String.valueOf(item);
				    	Log.d(TAG, itemString);
				         // Do something with the selection
				    }
				});
				dialog = builder.create();				
				Log.i(TAG, "Creating button that will activate dialog");
				// Create a button
				final Button b = new Button(this);
				b.setText(fieldname);
				b.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.show();
					}
				});				
				LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				b.setLayoutParams(blp);
				linearLayout.addView(b);
			}			
			i = i + 1;
		} // end for loop

		setContentView(linearLayout, llp);
	}
	
	@Override 
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);		
		outState.putParcelable(MyContentProvider.CONTENT_ITEM_TYPE, mJobUri);
		outState.putString(Table.COLUMN_JOB_CUSTOM_FIELDS, mCustomFields);			
	}
	
	// onPause mirrors onCreate but is used to store the values back into the map and update the database
	@Override	
	protected void onPause() {
		int i = 0;
		JSONArray mJSONArray = new JSONArray();
		for (Map<String,String> cursor : mFieldTypes) {			
			String fieldtype = cursor.get("fieldtype");
			if (fieldtype.equals("tickbox")) {
				String checkValue = (mCheckBox[i].isChecked()) ? "checked" : "unchecked"; 
				cursor.put("value", checkValue);
			}
			if (fieldtype.equals("text")) {
				cursor.put("value", mEditText[i].getText().toString());
			}
			if (fieldtype.equals("dropdown")) {
				cursor.put("value", "high site");
			}
			i=i+1;
			JSONObject json = new JSONObject(cursor);
			mJSONArray.put(json);
		}		
		String JSONString = "{\"customfields\":" + mJSONArray.toString() + "}";
		Log.d(TAG, "The JSON to store is " + JSONString);
		// Store value in database
		ContentValues values = new ContentValues();		
		values.put(Table.COLUMN_JOB_CUSTOM_FIELDS, JSONString);
		getContentResolver().update(mJobUri, values, null, null);		
		// Save in method variable so that when onSaveInstance is called the new values are populated
		mCustomFields = JSONString;
		super.onPause();
	}

}
