/**
 * A class to receive messages from the GCM server and take action
 * 
 * TODO Future events:
 *  getLocation
 * 
 */
package com.messaging.push;

/**
 * @author eugene
 *
 */
public class MessageActions {
	
	private enum Action {
		addEvent, removeEvent, updateEvent, startEvent, stopEvent  
	}
	
	public void takeAction(String message) {
		
		Action action = Action.valueOf(message);
		
		switch (action) {
		case addEvent :
			addEvent();
			break;
		case removeEvent :
			removeEvent();
			break;
		case updateEvent :
			updateEvent();
			break;				
		case startEvent :
			startEvent();
			break;
		case stopEvent :
			stopEvent();
		}
	}
	
	/**
	 * Add a new event to the device
	 */
	private void addEvent() {
		// Insert new record to the database based on WHMCS ticket id 
		
	}
	
	/**
	 * Remove an event from the device
	 */
	private void removeEvent() {
		// Remove a record from the device database
		
	}
	
	/**
	 * Update an existing event on the device
	 */
	private void updateEvent() {
		// 
		
	}

	/**
	 * Called when the start button is clicked for an event
	 */
	private void startEvent() {
		//  
		
	}
	
	/**
	 * Called when the stop button is clicked for an event
	 */
	private void stopEvent() {
		// 
		
	}

}
