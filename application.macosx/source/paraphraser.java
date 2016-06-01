import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import org.jsoup.Jsoup; 
import org.jsoup.helper.Validate; 
import org.jsoup.nodes.Document; 
import org.jsoup.nodes.Element; 
import org.jsoup.select.Elements; 

import org.jsoup.*; 
import org.jsoup.examples.*; 
import org.jsoup.helper.*; 
import org.jsoup.nodes.*; 
import org.jsoup.parser.*; 
import org.jsoup.safety.*; 
import org.jsoup.select.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class paraphraser extends PApplet {







/*
Mitchell Griest
 Paraphraser
 Summer 2016
 
 mdgriest@crimson.ua.edu
 */

//A few colors to get things started
int red = 0xffF27074;
int darkRed = 0xff8E2800;
int clay = 0xffB64926;
int yellow = 0xffFFA500;
int paleYellow = 0xffFFFF9D;
int orange = 0xffFF6138;
int green = 0xff468966;
int limeGreen = 0xffBDF271;
int lightGreen = 0xffBEEB9F;
int middleGreen = 0xff79BD8F;
int blueGreen = 0xff00A388;
int blue = 0xff1E90FF;
int dullBlue = 0xff348899;
int aqua = 0xff29D9C2;
int lightBlue = 0xff99CCFF;
int darkGrayBlue = 0xff6D7889;
int lightGray = 0xffD4D4D4;
int middleGray = 0xffAAAAAA;
int darkGray = 0xff4B4747;
int white = 0xffF3F4F2;

//Positioning for text
int xTop, yTop, xBottom, yBottom;
//Determine where to split the screen horizontally
int ySplit;
//Margin on left side of screen
int initialX = 15;

//Font for paraphrased text
PFont font;
//Font for original text
PFont plainFont;
int fontSize, plainFontSize;
int maxFontSize = 200;
int minFontSize = 10;
//How quickly to increase/decrease fontSize
int fontStep = 1;
String fontName = "Helvetica-Light";
String plainFontName = "Courier";

int bgTop, bgBottom, important, plain, cursorColor;

//Used for blinking cursor
boolean cursorFlag = true;

StringBuilder input = new StringBuilder();
StringBuilder output = new StringBuilder();
StringBuilder currentWord = new StringBuilder();

ArrayList<String> resetCommands = new ArrayList<String>();
ArrayList<Character> specialCharacters = new ArrayList<Character>();

public void setup() {
  
  surface.setResizable(true);
  surface.setTitle("Paraphraser");

  fontSize = 30;
  plainFontSize = Math.round(fontSize * 0.65f);
  font = createFont(fontName, fontSize);
  plainFont = createFont(plainFontName, plainFontSize);
  textFont(font);

  ySplit = Math.round(0.75f * height);
  xBottom = xTop = initialX;
  yTop = Math.round(ySplit / 2);
  yBottom = Math.round(height - ((height - ySplit) / 2));

  bgTop = white;
  bgBottom = darkGray;
  important = blue;
  plain = middleGray;
  cursorColor = red;
  background(bgTop);

  resetCommands.add(".clear");
  resetCommands.add(".reset");
  resetCommands.add(".CLEAR");
  resetCommands.add(".RESET");
  resetCommands.add(".c");
  resetCommands.add(".r");
  resetCommands.add(".C");
  resetCommands.add(".R");

  specialCharacters.add('.');
  specialCharacters.add('?');
  specialCharacters.add('!');
  specialCharacters.add(',');
  specialCharacters.add(';');
  specialCharacters.add(',');
}

public String paraphrase(String word) {
  try {
    StringBuilder url = new StringBuilder("http://www.thesaurus.com/browse/");
    url.append(word);
    //Grab the HTML document from thesaurs.com
    Document doc = Jsoup.connect(url.toString()).get();
    //Go find the relevancy list
    Elements rList = doc.select(".relevancy-list");
    //Get an ArrayList of all the synonyms
    Elements synonyms = rList.select(".text");
    //Get the most relevant synonym from that list
    if (synonyms.size() != 0) {
      String newWord = synonyms.get(0).text();
      return newWord;
    }
    //If there are no synonyms, return the original word
    return word;
  }
  catch(HttpStatusException e) {
    //Clear the currentword
    currentWord.setLength(0);
    //And return it with the '/' removed
    return word.substring(1);
  }
  catch(IOException e) {
    System.err.println(e);
    return("Whoops!");
  }
}

public void draw() {
  background(bgTop);

  //Draw the bottom rectangle for original text input
  fill(bgBottom);
  rect(0, ySplit, width, height);

  //Draw the input
  textFont(plainFont);
  fill(plain);
  text(input.toString(), initialX, yBottom);

  //Draw the output
  textFont(font);
  fill(important);
  text(output.toString(), initialX, yTop);

  blinkCursor();
}

public void keyPressed() {
  //Capture what the user entered
  char c = key;

  //Increase font size
  if (c == ']') {
    fontSize = min(fontSize + fontStep, maxFontSize);
    plainFontSize = min(fontSize + fontStep, maxFontSize);
    font = createFont(fontName, fontSize);
    plainFont = createFont(plainFontName, fontSize);
    textFont(font);
  }
  //Decrease font size
  else if (c == '[') {
    fontSize = max(fontSize - fontStep, minFontSize);
    plainFontSize = max(fontSize - fontStep, minFontSize);
    font = createFont(fontName, fontSize);
    plainFont = createFont(plainFontName, fontSize);
    textFont(font);
  }
  //Ignore ENTER presses for now
  else if(key == '\n'){
    return;
  }
  //BACKSPACE pressed and there is something to delete
  else if ( c == '\b' && input.length() > 0) {

    //Move x back by the width of the character to delete
    xBottom -= textWidth(input.charAt(input.length() - 1));

    //And remove the character
    input.setLength(input.length() - 1);
    if (currentWord.length() >0 ) {
      currentWord.setLength((currentWord.length() - 1));
    }
  }
  //SPACEBAR pressed
  else if ( c == ' ' && currentWord.length() > 0) {
    //Add a space to both the input and the output
    input.append(" ");
    output.append(" ");

    //If the current word is a reset command
    if (resetCommands.contains(currentWord.toString())) {
      reset();
      return;
    }

    //If the current word starts with '/', paraphrase it before adding to output
    if (currentWord.charAt(0) == '/') {
      String newWord;
      //If the word to be paraphrased ends in a special character
      if (specialCharacters.contains(currentWord.charAt(currentWord.length() - 1))) {
        //Strip the special character before paraphrasing
        newWord = currentWord.substring(0, currentWord.length() - 1);
        //Add the paraphrased word
        output.append(paraphrase(newWord));
        //And the special character that was on the end of it
        output.append(currentWord.charAt(currentWord.length() - 1));
      }
      //Otherwise, just paraphrase the word and add it to the output
      else {
        output.append(paraphrase(currentWord.toString()));
      }
    }
    //Otherwise, just add it (unchanged)
    else {
      output.append(currentWord.toString());
    }

    //And clear the currentword
    currentWord.setLength(0);
  }
  //When a general input character is received (excluding SHIFT (keycode 16))
  else if (keyCode != 16) {
    //Add it to the input
    input.append(c);
    currentWord.append(c);
  }
  //Shift cursors to the right for the next character
  xBottom = Math.round(textWidth(input.toString() + " "));
  xTop = Math.round(textWidth(output.toString()));
}

//Allow drag to the left to reset the program
int prevMouseX, currMouseX;
public void mouseDragged() {
  currMouseX = mouseX;
  if (currMouseX < (prevMouseX - 20)) {
    reset();
  }
  prevMouseX = currMouseX;
}

public void reset() {
  output.setLength(0);
  input.setLength(0);
  currentWord.setLength(0);
  xBottom = xTop = initialX;
}

public void blinkCursor() {
  fill(cursorColor);
  textFont(plainFont);
  if (frameCount % 30 == 0) {
    cursorFlag = !cursorFlag;
  }
  if (cursorFlag) {
    text("|", xBottom, yBottom);
  }
}
  public void settings() {  size(displayWidth, 300); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "paraphraser" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
