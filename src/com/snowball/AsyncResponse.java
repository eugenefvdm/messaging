/*
 * Interface for HTTPTask
 * See:
 * http://stackoverflow.com/questions/12575068/how-to-get-the-result-of-onpostexecute-to-main-activity-because-asynctask-is-a
 */


package com.snowball;

public interface AsyncResponse {
	void asyncProcessFinish(String output);
}
