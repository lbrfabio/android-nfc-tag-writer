package it.fabiolbr.nfctagwriter;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity to write NFC tags with own mimetype and ID
 * Based on the excellent tutorial by Jesse Chen
 * http://www.jessechen.net/blog/how-to-nfc-on-the-android-platform/
 */
public class MainActivity extends Activity {

	private NfcAdapter mNfcAdapter;
	private PendingIntent mNfcPendingIntent;
	
	// views
	private Button mBtnWrite;
	private EditText mTextMime;
	private EditText mTextValue;
	
	@SuppressWarnings("unused")
    private	AlertDialog mDialog;
	
	private OnClickListener mListenerClick = new OnClickListener() {
        
	    @Override
        public void onClick(View v) {
            //mNfcAdapter = NfcAdapter.getDefaultAdapter(MainActivity.this);
            //mNfcPendingIntent = PendingIntent.getActivity(MainActivity.this, 0,
            //    new Intent(MainActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            enableTagWriteMode();
             
            new AlertDialog.Builder(MainActivity.this).setTitle("Touch tag to write")
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        //probably not necessary. 
                        disableTagWriteMode();
                    }

                }).create().show();     
        }
    };
		
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// initialize views
		mTextMime = (EditText)findViewById(R.id.edit_mime);
		mTextValue = (EditText)findViewById(R.id.edit_value);
		mBtnWrite = (Button)findViewById(R.id.btn_write);
		
		mBtnWrite.setOnClickListener(mListenerClick);
		
		// initialize nfc stuff
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            // Stop here, we need NFC
            Toast.makeText(this, "This device doesn't support NFC",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled", Toast.LENGTH_SHORT).show();
            //TODO: make alert dialog to ask to open nfc settings or close app 
        }
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    enableTagWriteMode();
	}
	
	
    private void enableTagWriteMode() {
	    //IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
	    //IntentFilter[] mWriteTagFilters = new IntentFilter[] { tagDetected };

        Intent intent = new Intent(this, getClass());
        //intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mNfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        //filters\techLists are null = in foreground all tags as ACTION_TAG_DISCOVERED
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, null, null);
        
      //mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
	}
    
        
	
	@Override
	protected void onPause() {
	    disableTagWriteMode();
	    super.onPause();
	}

	private void disableTagWriteMode() {
		mNfcAdapter.disableForegroundDispatch(this);
	}
	
    @Override
    protected void onNewIntent(Intent intent) {
        if (areFieldsEmpty()) return;

        // Tag writing mode
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            NdefRecord mimeRecord = NdefRecord.createMime(mTextMime.getText()
                    .toString(), mTextValue.getText().toString().getBytes());
            NdefMessage message = new NdefMessage(new NdefRecord[] { mimeRecord });
            if (writeTag(message, detectedTag)) {
                Toast.makeText(this, R.string.success_tag_written, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    /**
     * Check if one or both EditText are empty
     * @return false if one or both empty, true otherwise.
     * Also, a error message is set. 
     */
    private boolean areFieldsEmpty() {
        boolean state = false;
        if(mTextMime.getText().length() == 0) {
            mTextMime.setError(getString(R.string.error_text_empty));   
            state = true;
        } else {
            mTextMime.setError(null);
        }
	    if(mTextValue.getText().length() == 0) {
	        mTextValue.setError(getString(R.string.error_text_empty));
	        state = true;
	    } else {
	        mTextValue.setError(null);
        }
	    return state;
    }

    /*
     * Writes an NdefMessage to a NFC tag
     */
    public boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(getApplicationContext(),
                            "Error: tag not writable", Toast.LENGTH_SHORT)
                            .show();
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    mTextValue.setError(String.format(getString(R.string.error_value_toobig),
                                size, ndef.getMaxSize()));
                    return false;
                }
                ndef.writeNdefMessage(message);

                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
}