import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import org.monome.Monome; 
import netP5.*; 
import oscP5.*; 
import java.util.Arrays; 

import org.monome.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class gridBitwigThruMoss extends PApplet {

// Grid as Controller for Bitwig ( basic implementation )
// interfaces with DrivenByMoss OSC controller script for Bitwig
// this program by Cristian Vogel
// v1.0






Monome m;
boolean updateGrid, update, globalPlayStatus = false;

int[][] led, slotContent;
boolean[][] blinkingLed;
int timer;
int loop_start, loop_end;
int HIGH = 15, MEDIUM = 7, DIM = 1;
int TRACKCOUNT = 8;
int flash = 0 ;
boolean cutting;
int next_position;
int keys_held, key_last;
int prevCol, prevRow;

String [] skiplist = {"/track/selected/color",  "/time/str"};
String beat, prevBeat;
int sketchPort = 13755;
int bitwigPort = 8005;
OscP5  oscP5;
NetAddress toBitwig;
OscMessage msg;

public void setup() {
  m = new Monome(this);
  toBitwig = new NetAddress ("127.0.0.1", bitwigPort);

  //https://github.com/sojamo/oscp5/issues/6#issuecomment-310909482
  OscProperties op = new OscProperties();
  op.setListeningPort(sketchPort);
  op.setDatagramSize(5000);
  oscP5 = new OscP5(this, op);

  frameRate(20);
  
  
  stroke(255,204); rectMode(CENTER);

  led = new int[8][16]; 
  slotContent = new int[8][16];
  blinkingLed = new boolean[8][16];
 
  for (int row = 0; row<= 7; row++) {
      for (int col = 0; col<=7; col++) { 
         led [row][col] = 0;
         slotContent [row][col] = DIM;
        }
      }
  updateGrid = true;
}

public void draw() {
  if(frameCount == 1) getSlotContent();  // delayed kludge to request content of slots
  if ( updateGrid || update ) {
    background(10);
    led[7][15] = globalPlayStatus ? HIGH : DIM;
    flash = (int) map(frameCount % 6, 0, 5, DIM, MEDIUM);
    for (int row = 0; row<= 7; row++) {
      for (int col = 0; col<=15; col++) { 
        if (led[row][col] != HIGH) {
          led[row][col] = blinkingLed[row][col] ? flash : slotContent[row][col]; 
        } else {
          led[row][col] = blinkingLed[row][col] ? flash : HIGH; 
        }
        int pRow = (int) map(col, 0,15, width * 0.1f , width * 0.9f );
        int pCol = (int) map(row, 0, 7, height * 0.1f, height * 0.9f);
        fill(255, 20 * (blinkingLed[row][col] ? flash : led[row][col])  );
        rect(pRow, pCol, 10,10);
      };
    }
    // update grid
    m.refresh(led);
    updateGrid = false;
  }
}

public void getSlotContent(){
  // needed to find out hasContent of clip slots
  sendOscMessage("/track/bank/page/+");
  sendOscMessage("/track/bank/page/-");
}       

public void sendToAllTracks( String msg, int value) {
  for( int i = 1; i<=TRACKCOUNT; i++) {
     sendOscMessage("/track/"+i+msg, value);
  }
}

/** 
 * incoming OSC callback
**/
public void oscEvent(OscMessage msg) {

    String address = msg.toString();

    if( skipTheseAddresses( msg, skiplist ) ) { return; }

  // firstly, check for messages with no args 
     if(msg.checkAddrPattern("/update")) {
      updateGrid = true;
      update = !update;
    }
//  check for message  which has a string argument 
  if(msg.checkAddrPattern("/beat/str") && msg.checkTypetag("s")) {
      String beatString = msg.get(0).stringValue();
      //println (beatString);
      if (beatString.equals(prevBeat)) { globalPlayStatus = false; } else { globalPlayStatus = true; }
            prevBeat = beatString;
            return;
    }

  // then check if message has an int argument 
   int arg0 = 0;

    if(msg.checkTypetag("i")) {  
      arg0 = msg.get(0).intValue(); 
    } else {
      return;
    }   

    if(msg.checkAddrPattern("/play")) {
      globalPlayStatus = (arg0 == 1);
    }

    if(msg.checkAddrPattern("/tempo")) {
      println("tempo: "+  arg0);
    }


    // track, clip, scene launcher iterators

    if (address.indexOf("/clip/") > 0 && address.indexOf("/track/") > 0) {

      if (update) {
      for (int row = 0; row<=7; row++ ) {
        for (int col = 0; col<=7; col++) {

          String stem = "/track/"+(row+1)+"/clip/"+(col+1);

          if(msg.checkAddrPattern(stem+"/hasContent")) {
            slotContent[row][col] = MEDIUM;
          }

          if(msg.checkAddrPattern(stem+"/isPlayingQueued")) {
            blinkingLed[row][col] = (arg0 == 1); 
          }

          if(msg.checkAddrPattern(stem+"/isPlaying")) {
            led[row][col] = (arg0 == 1) ? HIGH : slotContent[row][col];    
          }
        }
      }
    }
  }  
}

public boolean skipTheseAddresses( OscMessage msg, String[] list) {
 for ( String s : list) { 
  if (msg.checkAddrPattern(s)) return true; 
 }
 return false;
}

public void key(int col, int row, int s) {  
/*
  plenty of ternary selectors for the OSC messages to send upon button presses
*/
// play and stop
    if ( row == 7 && col == 15 && s == 1) { 
        sendOscMessage( 
                    globalPlayStatus ?  "/stop" : "/play",
                    1);
    }
// launch and stop clips
    if ( s==1 && col<=8 && col >=0 && row<=8 && row>=0 ) {
        sendOscMessage( 
                    led[row][col] == 0 ? "/track/"+(row+1)+"/clip/stop" : "/track/"+(row+1)+"/clip/"+(col+1)+"/launch"  ,
                    1);
  }
  updateGrid = true;
}

 // method to send float
    public void sendOscMessage(String message, float value)
    {
        OscMessage slimeMessage = new OscMessage (message);
        slimeMessage.add((float) value);
        oscP5.send(slimeMessage, toBitwig);
        slimeMessage.clear();
    }
    // method to send int
    public void sendOscMessage(String message, int value)
    {
        OscMessage slimeMessageInt = new OscMessage (message);
        slimeMessageInt.add((int) value);
        oscP5.send(slimeMessageInt, toBitwig);
        slimeMessageInt.clear();
    }

    //method to send float array of args

    public void sendOscMessage(String message, Float [] args)
    {
        OscMessage myMessage = new OscMessage(message);
        for (int i=0; i<args.length; i++)
        {
            myMessage.add(args[i]);
        }
        oscP5.send(myMessage, toBitwig);
    }

    //method to send with no arg
    public void sendOscMessage(String message)
    {
       OscMessage slimeMessageInt = new OscMessage (message);
        oscP5.send(slimeMessageInt, toBitwig);
        slimeMessageInt.clear();
    }


    // method to quick check if anything is still queued. Might be useful.
public boolean bagCheck( boolean[][] array) {
  for (boolean[] rows : array) {
  for (boolean j : rows) {
    if (j) {return true;} 
      }
    }    
  return false;
}
  public void settings() {  size(360,140);  pixelDensity(2); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "gridBitwigThruMoss" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
