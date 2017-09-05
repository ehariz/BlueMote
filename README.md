# BlueMote
An Android app to control your computer from your phone, using bluetooth. 
Based on google's android bluetoothchat sample, available here : https://github.com/googlesamples/android-BluetoothChat

BlueMote includes a Java server app to run on your computer, and an Android App to control your computer from your phone. The server
app uses the Bluecove API to read incoming bluetooth data, and a dll built with JNI (Java Native Interface) to simulate media keys using native C++ code.
The Android app allows you to switch windows on your computer,control the volume, move the mouse and click from your phone, and provides media controls(next media,previous media,play,pause).
