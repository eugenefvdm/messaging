/*
 * Build Layout using this JSON
 * 
 * customfields":[{"id":"11","type":"support","relid":"5","fieldname":"High Site","fieldtype":"dropdown","description":"","fieldoptions":"Bottelary,Hawekwa,Simonsberg","required":"on"},{"id":"13","type":"support","relid":"5","fieldname":"Survey Successful?","fieldtype":"tickbox","description":"","fieldoptions":"","required":"on"}]
 * 
 */

package com.snowball;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

public class BuildCustomLayout extends Activity {

	protected static final String TAG = "BuildCustomLayout";
	private Dialog dialog;
	
	//private String customFields = "\"customfields\":[{\"id\":\"11\",\"type\":\"support\",\"relid\":\"5\",\"fieldname\":\"High Site\",\"fieldtype\":\"dropdown\",\"description\":\"\",\"fieldoptions\":\"Bottelary,Hawekwa,Simonsberg\",\"required\":\"on\"},{\"id\":\"13\",\"type\":\"support\",\"relid\":\"5\",\"fieldname\":\"Survey Successful?\",\"fieldtype\":\"tickbox\",\"description\":\"\",\"fieldoptions\":\"\",\"required\":\"on\"}]";
	
	
	private String customFields = "{\"customfields\":[{\"id\":\"11\",\"type\":\"support\",\"relid\":\"5\",\"fieldname\":\"High Site\",\"fieldtype\":\"dropdown\",\"description\":\"\",\"fieldoptions\":\"Bottelary,Hawekwa,Simonsberg\",\"required\":\"on\"},{\"id\":\"13\",\"type\":\"support\",\"relid\":\"5\",\"fieldname\":\"Survey Successful?\",\"fieldtype\":\"tickbox\",\"description\":\"\",\"fieldoptions\":\"\",\"required\":\"on\"},{\"id\":\"14\",\"type\":\"support\",\"relid\":\"5\",\"fieldname\":\"Need ladder?\",\"fieldtype\":\"tickbox\",\"description\":\"\",\"fieldoptions\":\"\",\"required\":\"on\"}]}";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Grabbing the Application context in case we do Toasts or other context sensitive stuff (in anonymous inner classes)
		final Context context = getApplication();
		
		// Create a root LinearLayout to hold the check box
		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		
		JsonHelper json = new JsonHelper();
		JSONObject customFieldsJson = json.convertStringToJSONObject(customFields);
		//ArrayList<String> fieldTypes = json.getCustomFieldsArray(customFieldsJson, "fieldtype");
		
		Map<String, String> fieldTypes = json.createCustomFieldHashMap(customFieldsJson);
		
		for (Map.Entry<String, String> cursor : fieldTypes.entrySet()) {
			Log.d(TAG, "iteration over " + cursor.getKey() + " value " + cursor.getValue());
			if (cursor.getValue().equals("tickbox")) {
				Log.w(TAG, "Creating tickbox");				
				final CheckBox cb = new CheckBox(this);
				cb.setText(cursor.getKey());
				cb.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// Do store value in configuration object, e.g.:
						//new Object o
						//o.addKey, addValue
						// Need identifier!
					}
				});				
				LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				cb.setLayoutParams(cblp);
				linearLayout.addView(cb);
			}
			//System.out.println("Key = " + cursor.getKey() + ", Value = " + cursor.getValue());
		}
		
//		for (String type : fieldTypes) {
//			if (type.equals("dropdown")) {
//				Log.w(TAG, "Creating dropdown");
//			} else if (type.equals("tickbox")) {
//				
//				
//			}
//		}
	
	
//		// Create a root LinearLayout to hold the check box
//		LinearLayout linearLayout = new LinearLayout(this);
//		linearLayout.setOrientation(LinearLayout.VERTICAL);
//		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

		dialog = new Dialog(this);		
		String[] stringArray = new String[] { "Bright Mode", "Normal Mode" };
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Choose High Site");
		builder.setItems(stringArray, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	String itemString = String.valueOf(item);
		    	Log.d(TAG, itemString);
		         // Do something with the selection
		    }
		});
		dialog = builder.create();
				
//		final CheckBox cb = new CheckBox(this);
//		cb.setText("Survey Successful?");
//		cb.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				dialog.show();
//			}
//		});

		// Layout parameters for the CheckBox
//		LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//		cb.setLayoutParams(cblp);
//		linearLayout.addView(cb);

		setContentView(linearLayout, llp);

	}

}
