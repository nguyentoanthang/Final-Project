package dvp.nfctest;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NFC";
    NfcAdapter adapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    Button btW,btR;
    EditText writeText;
    TextView readText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btW = (Button) findViewById(R.id.btWriter);
        readText = (TextView) findViewById(R.id.textReader);
        writeText = (EditText) findViewById(R.id.edtWriter);
        btR=(Button)findViewById(R.id.btReader);


        btW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (myTag == null) {
                        Toast.makeText(MainActivity.this, "Khong nhan duoc the Tag", Toast.LENGTH_LONG).show();
                    } else {
                        write(writeText.getText().toString(), myTag);
                        Toast.makeText(MainActivity.this, "Ghi xong", Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Khong ghi duoc", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(MainActivity.this, "Khong ghi duoc", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });

        adapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{tagDetected};

        if (adapter != null && adapter.isEnabled()) {
            Toast.makeText(this, "NFC is Available", Toast.LENGTH_SHORT).show();
        } else Toast.makeText(this, "NFC is Unavailable", Toast.LENGTH_SHORT).show();


    }


    private void write(String text, Tag tag) throws IOException, FormatException {

        NdefRecord[] records = {createRecord(text)};
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }


    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return recordNFC;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(this, "NFC Detected" + myTag.toString(), Toast.LENGTH_LONG).show();

            Parcelable[] parcelables =intent.getParcelableArrayExtra(adapter.EXTRA_NDEF_MESSAGES);
            if(parcelables!=null && parcelables.length>0){
                readTextFromMsg((NdefMessage)parcelables[0]);
            }else{
                Toast.makeText(this,"NO Msg found",Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void readTextFromMsg(NdefMessage parcelable) {
        NdefRecord[] ndefRecord=parcelable.getRecords();
        if(ndefRecord!=null&&ndefRecord.length>0){
            NdefRecord ndefRecord1 = ndefRecord[0];
            String str= getTextFromTag(ndefRecord1);
            readText.setText(str);
        }else {
            Toast.makeText(this,"NO record found",Toast.LENGTH_SHORT).show();
        }


    }

    private String getTextFromTag(NdefRecord ndefRecord){
        String tagContent=null;
        try{
            byte[] payload=ndefRecord.getPayload();
            String str=((payload[0]& 128)==0)?"UTF-8":"UTF-16";
            int langSize=payload[0]& 0063;
            tagContent=new String(payload,langSize+1,payload.length-langSize-1,str);

        }catch (UnsupportedEncodingException e){
            Log.e("getText",e.getMessage(),e);
        }

        return tagContent;
    }

    @Override
    public void onPause() {
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume() {
        super.onResume();
        WriteModeOn();
    }

    private void WriteModeOn() {
        writeMode = true;
        adapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff() {
        writeMode = false;
        adapter.disableForegroundDispatch(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
