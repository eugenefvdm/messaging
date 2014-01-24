package za.co.snowball.jobtracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JsonHelper {

	private static final String TAG = "JsonHelper";
	
	public JSONObject convertStringToJSONObject(String jsonString) {
		//jsonString = "{\"customfields\":[{\"id\":\"11\",\"type\":\"support\",\"relid\":\"5\",\"fieldname\":\"High Site\",\"fieldtype\":\"dropdown\",\"description\":\"\",\"fieldoptions\":\"Bottelary,Hawekwa,Simonsberg\",\"required\":\"on\"},{\"id\":\"13\",\"type\":\"support\",\"relid\":\"5\",\"fieldname\":\"Survey Successful?\",\"fieldtype\":\"tickbox\",\"description\":\"\",\"fieldoptions\":\"\",\"required\":\"on\"}]}";
		jsonString.replace("\"", "\\\"");
		//jsonString.replace("\"", "");
		
		try {
			JSONObject obj = new JSONObject(jsonString);
			return obj;
		} catch (Throwable t) {
		    Log.e(TAG, "Could not parse malformed JSON: " + jsonString);
		}
		return null;
	}

	/**
	 * Useful to obtain specific array of values, e.g. fieldtypes (checkbox, dropdown)
	 * 
	 * private String customFields = "\"customfields\":[{\"id\":\"11\",\"type\":\"support\",\"relid\":\"5\",\"fieldname\":\"High Site\",\"fieldtype\":\"dropdown\",\"description\":\"\",\"fieldoptions\":\"Bottelary,Hawekwa,Simonsberg\",\"required\":\"on\"},{\"id\":\"13\",\"type\":\"support\",\"relid\":\"5\",\"fieldname\":\"Survey Successful?\",\"fieldtype\":\"tickbox\",\"description\":\"\",\"fieldoptions\":\"\",\"required\":\"on\"}]";
	 * @param json
	 * @return
	 */
//	public ArrayList<String> getCustomFieldsArray(JSONObject json, String field) {
//		ArrayList<String> fields = new ArrayList<String>();
//		JSONArray customfields;
//		try {
//			customfields = json.getJSONArray("customfields");
//			int len = customfields.length();
//			for (int i = 0; i < len; i++) {
//				JSONObject obj = customfields.getJSONObject(i);
//				fields.add(obj.getString("fieldtype"));
//				Log.d(TAG, "Custom fields" + obj.getString(field));				
//			}
//			return fields;
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
//	}
	
	public List<Map<String, String>> createCustomFieldMap(JSONObject json) {
		List<Map<String, String>> fields = new ArrayList<Map<String, String>>();
		JSONArray customfields;
		try {
			customfields = json.getJSONArray("customfields");
			int len = customfields.length();
			for (int i = 0; i < len; i++) {
				JSONObject obj = customfields.getJSONObject(i);				
				Map<String, String> map = new HashMap<String, String>();
				map.put("id", obj.getString("id"));
				map.put("fieldname", obj.getString("fieldname"));
				map.put("fieldtype", obj.getString("fieldtype"));
				map.put("description", obj.getString("description"));
				map.put("required", obj.getString("required"));
				map.put("fieldoptions", obj.getString("fieldoptions"));	
				map.put("value", obj.getString("value"));
				fields.add(map);				
			}
			return fields;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
}
