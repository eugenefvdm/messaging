package com.messaging.push._backup;

public class Event {
	  private long id;
	  private String event;

	  public long getId() {
	    return id;
	  }

	  public void setId(long id) {
	    this.id = id;
	  }

	  public String getEvent() {
	    return event;
	  }

	  public void setEvent(String Event) {
	    this.event = Event;
	  }

	  // Will be used by the ArrayAdapter in the ListView
	  @Override
	  public String toString() {
	    return event;
	  }
	} 