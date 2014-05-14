package com.greatnowhere.iradar;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class AboutActivity extends Activity {

	private Button btnOk;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        
        btnOk = (Button) findViewById(R.id.aboutIdButtonOK);
        btnOk.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
    }

}
