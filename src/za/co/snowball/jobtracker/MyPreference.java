package za.co.snowball.jobtracker;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class MyPreference extends ListPreference {
    private Context context;

    // a flag to control show dialog
    private boolean showDialog = false;

    public MyPreference(Context context) {
        super(context);
        this.context = context;
    }

    public MyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;       
    }

    @Override
    protected void showDialog(Bundle state) {        
        if (showDialog) {
            // show dialog 
            super.showDialog(state);
        } else {
            // if you don't want to show a dialog when click preference
            return;
        } /* end of if */
    }
}