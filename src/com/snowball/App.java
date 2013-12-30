package com.snowball;

import java.util.ArrayList;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    
	private static int pendingNotificationsCount = 0;
    
    /**
	 * List of all messages received used by InboxStyle notification builder
	 */
	private static ArrayList<String> pendingMessages = new ArrayList<String>();

    @Override
    public void onCreate() {
        super.onCreate();
    }
    
    public void init(Context ctx) {
    	
    }

    public static int getPendingNotificationsCount() {
        return pendingNotificationsCount;
    }

    public static void setPendingNotificationsCount(int pendingNotifications) {
        pendingNotificationsCount = pendingNotifications;
    }
    
    public static void addMessage(String message) {
    	pendingMessages.add(message);
    }
    
    public static ArrayList<String> getMessages() {
    	return pendingMessages;    	
    }
    
    public static void clearMessages() {
    	pendingMessages = new ArrayList<String>();
    }
}