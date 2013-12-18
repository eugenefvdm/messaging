/*
 * GCMIntentService.java
 * 
 * Handles all incoming messages and takes appropriate action
 */

package com.messaging.push;

import static com.messaging.push.CommonUtilities.SENDER_ID;
import static com.messaging.push.CommonUtilities.displayMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.messaging.push.contentprovider.MyTaskContentProvider;

public class GCMIntentService extends GCMBaseIntentService {

	private static final String TAG = "GCMIntentService";

    public GCMIntentService() {
        super(SENDER_ID);
    }

    /**
     * Method called on device registered
     **/
    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(TAG, "Device registered: regId = " + registrationId);
        displayMessage(context, "Your device registred with GCM");
        Log.d("NAME", MainActivity.name);
        ServerUtilities.register(context, MainActivity.name, MainActivity.email, registrationId);
    }

    /**
     * Method called on device unregistered
     * */
    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.i(TAG, "Device unregistered");
        displayMessage(context, getString(R.string.gcm_unregistered));
        ServerUtilities.unregister(context, registrationId);
    }

    /**
     * Method called on Receiving a new message
     * */
    @Override
    protected void onMessage(Context context, Intent intent) {
    	ContentValues values;
    	String messageType = "";
        String message = intent.getExtras().getString("price");
        Log.i(TAG, "Received message '" + message + "' and now examining content...");
        String department = null;
        int ticket_id = 0;
		// TODO The AndroidHive demo return NULL first time you register, this has to be researched
        if (message == null) {
        	messageType = "First time registration! Welcome!";
        	Log.e(TAG, "Message is NULL!");
        } else {
        	String action = MyTaskContentProvider.getAction(message);
        	if (action != null) {
        		values = MyTaskContentProvider.convertMessageToContentValues(action, message);
        		department = values.getAsString("department");
        		if (action.equals("insert")) {			
        			getContentResolver().insert(MyTaskContentProvider.CONTENT_URI, values);
        			messageType = "New " + department;
        		} else if (action.equals("update")) {
        			ticket_id = values.getAsInteger("ticket_id");
        			Uri todoUri = Uri.parse(MyTaskContentProvider.CONTENT_URI + "/ticket" + "/" + ticket_id);        			
        			getContentResolver().update(todoUri, values, null, null);
        			messageType = "Updated " + department;
        		} else if (action.equals("delete")) {
        			ticket_id = values.getAsInteger("ticket_id");
        			Uri todoUri = Uri.parse(MyTaskContentProvider.CONTENT_URI + "/ticket" + "/" + ticket_id);        			
        			getContentResolver().delete(todoUri, null, null);
        			messageType = "Deleted " + department;
        		}
        	} else {
        		messageType = "System Message: " + message;
        	}
        }
        displayMessage(context, messageType);
        generateNotification(context, messageType);                    
    }

    /**
     * Method called on receiving a deleted message
     * */
    @Override
    protected void onDeletedMessages(Context context, int total) {
        Log.i(TAG, "Received deleted messages notification");
        String message = getString(R.string.gcm_deleted, total);
        displayMessage(context, message);
        // notifies user
        generateNotification(context, message);
    }

    /**
     * Method called on Error
     * */
    @Override
    public void onError(Context context, String errorId) {
        Log.i(TAG, "Received error: " + errorId);
        displayMessage(context, getString(R.string.gcm_error, errorId));
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
        Log.i(TAG, "Received recoverable error: " + errorId);
        displayMessage(context, getString(R.string.gcm_recoverable_error,
                errorId));
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    private static void generateNotification(Context context, String message) {
        int icon = R.drawable.snowball;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        
        String title = context.getString(R.string.app_name);
        
        Intent notificationIntent = new Intent(context, MainActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        
        // Play default notification sound
        notification.defaults |= Notification.DEFAULT_SOUND;
        
        //notification.sound = Uri.parse("android.resource://" + context.getPackageName() + "your_sound_file_name.mp3");
        
        // Vibrate if vibrate is enabled
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notificationManager.notify(0, notification);      

    }

}
