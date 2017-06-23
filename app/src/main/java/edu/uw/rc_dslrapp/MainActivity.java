package edu.uw.rc_dslrapp;

/* UW Tacoma Senior Project 2016
 * RC-DSLR Team
 * Lamont Franklin, Igor Gonchar, James Brewer, Jake Nasonov
 * Basic app created along side our prototype camera controller device.
 * The interface on the app controls panning and tilting of the device, record and focus controls on the camera, and displays the stream from the camera sent by a media server on the device.
*/

import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    private Socket socket;
    private TextView msgView;

	//Default local server to use for communication
    private static final int SERVERPORT = 5000;
    private static String SERVER_IP = "192.168.0.24";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String URL = "http://192.168.0.24:8090/stream/video.mjpeg";

		//Set up the message bar to prompt for a connection
        msgView = (TextView) findViewById(R.id.textView5);
        if (msgView != null) msgView.setText("Press to connect");

		//Initialize the webview for video streaming
        final WebView webview = (WebView) findViewById(R.id.webView);

		//Initialize the "refresh" button for the video stream
        Button resetVideo = (Button) findViewById(R.id.button);
        if (resetVideo != null) {
            resetVideo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (webview!=null) webview.loadUrl("http://192.168.0.24:8090/stream");
                }
            });
        }

		//Initialize the camera recording switch and listener
        final Switch recordSw = (Switch) findViewById(R.id.switch1);
        recordSw.setEnabled(false);
        recordSw.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                try {
					//On switch, print the message and status to the socket
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    out.println("record:" + checked);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });

		//Initialize all the seek bars for motion + focus controls
        final SeekBar panBar = (SeekBar) findViewById(R.id.panBar);
        panBar.setEnabled(false);
        final SeekBar tiltBar = (SeekBar) findViewById(R.id.tiltBar);
        tiltBar.setEnabled(false);
        final SeekBar focusBar = (SeekBar) findViewById(R.id.focusBar);
        focusBar.setEnabled(false);
        final SeekBar zoomBar = (SeekBar) findViewById(R.id.zoomBar);
        zoomBar.setEnabled(false);

		//Listener for pan controls
		//Value to be sent is used as velocity change for panning, e.g. 50 = still, 51-100 = clockwise, 0-49 = ccw
        panBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
					//Print the progress of the pan bar to the socket
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    out.println("pan:"+progress);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    new Thread(new ClientThread()).start();
                }
            }

			//On release, reset the bar to the midpoint (still motion)
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(50);
            }
        });

		//Listener for tilt controls
		//Value to be sent is used as percentage for position
        tiltBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
					//Print the bar progress to the socket
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    out.println("tilt:"+progress);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });
		
		//Listener for tilt controls
		//Value to be sent is used as percentage for focus steps, e.g. 50 = no change, 51-100 = focus in, 0-49 = focus out
        focusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
					//Print the bar progress to the socket
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    out.println("focus:"+progress);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

			//On release, reset the bar to the midpoint (still focus)
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(50);
            }
        });

		//Listener for zoom controls
		//Value to be sent is used as position for the zoom servo. 0->100 = fully zoomed out->fully zoomed in
        zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),
                    true);
                    out.println("zoom:"+progress);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        });

		//Modify the settings of the webview to allow zoom/scale gestures
        if (webview != null) {
            WebSettings webSet = webview.getSettings();
            //webSet.setJavaScriptEnabled(true);
            webSet.setLoadWithOverviewMode(true);
            webSet.setBuiltInZoomControls(true);
        }

		//Set controls for pressing the message bar to pick IP address and activate all features
        msgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

				//First, create an alert dialog to enter the local IP Address of the device
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Title");

                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                // On hitting OK, activate all the controls and connect to the device
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (socket == null) {	//Socket hasn't been created yet
                            SERVER_IP = input.getText().toString();
							//Activate a new websocket to the device
                            new Thread(new ClientThread()).start();
							
							//Enable all controls
                            panBar.setEnabled(true);
                            tiltBar.setEnabled(true);
                            recordSw.setEnabled(true);
                            focusBar.setEnabled(true);
                            zoomBar.setEnabled(true);
							//Refresh the webview to the new IP
                            webview.loadUrl("http://"+SERVER_IP+":8090/stream");
                            msgView.setText("RC-DSLR Connected");
                            msgView.setTextColor(Color.GREEN);

                        } else if (socket != null) {	//Socket is already active
                            try {
								//Close the old socket to connect to the new address
                                socket.close();
                            } catch (IOException e) {
								//If the socket fails to close, for prototype purposes the device and app need to be restarted
                            }
							//Complete the same process as above
                            SERVER_IP = input.getText().toString();
                            new Thread(new ClientThread()).start();
                            panBar.setEnabled(true);
                            tiltBar.setEnabled(true);
                            recordSw.setEnabled(true);
                            focusBar.setEnabled(true);
                            zoomBar.setEnabled(true);
                            webview.loadUrl("http://"+SERVER_IP+":8090/stream");
                        }


                    }
                });
				
				//Allow the user to cancel the address input
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

				//Show the dialog box and wait for input
                builder.show();
            }
        });

    }


	//This nested class runs as a socket creator, required as an extra thread to run the socket
    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

                socket = new Socket(serverAddr, SERVERPORT);

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }


    }


}
