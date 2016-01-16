# Emotiv-JSON-API
An API for receiving Emotiv EPOC EEG events in JSON format using TCP sockets


## What:
This project is meant to take data from a raw-data enabled EPOC or EPOC+ and convert it into JSON objects. It's useless if you don't have the Emotiv Xavier Research Edition libraries for the correct OS (available from Emotiv.com).

If you have the libraries and they're in the correct location on your computer (/usr/lib/ in the case of Mac users), an EPOC/EPOC+ properly on, and some kind of client that's connected to API_Main's socket ports, you should be all set. For example, in the [MindControlledUAV project](https://github.com/AshleyDumaine/MindControlledUAV), ImprovedMain.java acts as the client, giving the server the 2 EEG events it wants updates on (and gyros as an extra event if applicable). The server responds with a stream of JSON objects, updating the client whenever the headset sends a new packet of sensor data.

Currently, this successfully sends JSON objects over a TCP socket connection to any listening clients, but can stand to use some cleaning up and inclusion of features such as no specification of EEG events needed if a client wants updates on all possible EEG events.

Eventually, this project may use an artifical neural net (ANN) or multiple support vector machines (multi-SVM) to take raw headset data and classify different EEG events, but only if it provides higher accuracy than the built-in classification currently in the Emotiv libraries. This is the main reason that this part of the thesis is still considered unfinished.


## Why:
A couple years ago, I created a [mind-controlled drone](https://www.youtube.com/watch?v=X8jemlFtgBs) using the Emotiv SDK, an A.R. Parrot Drone 2, and a Java program I wrote to manage the communication between the headset and the drone. However, it was horribly messy as there was no straightforward communication mechanism between the EPOC and other devices/programs. 

What I did was use the EmoKey program that came with the Emotiv SDK to send emulated keystrokes to a Java window that had a KeyEventListener. Based on the key registered, it would send a different command to the drone. However, this required running the Emotiv Control Panel, EmoKey, and having the Java window in focus the whole time. Not to mention configuring commands based on arbitrary keys was confusing and annoying.

I thought it would be nice to use sockets to communticate and a nice format such as JSON to transmit headset data instead of arbitrary keystrokes to an application in focus. I somewhat recently applied this change to the mind-controlled drone project and the code became a lot less messy and dare I say it even worked better than my previous implementation that relied on EmoKey. One huge difference was that this allowed me to control the drone with gyros, which is impossible to do using EmoKey.


## How to Use:
The jar containing the server logic can either be launched programmatically by the client code or by running
```
java -jar /path/to/downloaded/jar/api_main_2.0.jar
```
in the terminal or command prompt.

The client code should connect to both port 4444 and 4445. 
On port 4445, cognitive training profiles are handled including naming/loading profiles, training them, and saving them. As soon as the headset is switched on with the client code and API server running, the server will ask for the username of the training profile to be either loaded or created. After that, the client can send requests on port 4445 to train certain cognitive commands including:
- Neutral
- Push
- Pull
- Lift
- Drop
- Left
- Right
- RotateLeft
- RotateRight
- RotateClockwise
- RotateCounterclockwise
- RotateForwards
- RotateReverse
- Disappear
 

The training request can look like the following:
```
train neutral
```
The server will then communicate on the port when the training is started, if it was successful, and when it is completed. To save the training session, the client can simply send the word "save" to the server. Having a user with trained cognitive data allows the cognitive detection of the SDK to be enabled so that any active cognitive commands show up in the JSON output if the client code requests updates on cognitive events.

On port 4444, the client should send one or a comma and space separated combination of any of the following:

| Client Input       | Server Output                                                                                                                                                                     |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| *                  | all possible data                                                                                                                                                                 |
| gyros              | x and y gyrometer data                                                                                                                                                            |
| affective          | all mood data: Frustration, Instantaneous Excitement, Long-Term Excitement, Meditation, Stress, Affinity/Interest, Engagement                                                     |
| expressive         | all facial expressions: Blink, LeftWink, RightWink, LookingLeft, LookingRight, LookingDown, LookingUp, EyesOpen, Clench, Smile, EyebrowRaise                                      |
| cognitive          | active cognitive action (if any): Push, Pull, Lift, Drop, Left, Right, RotateLeft, RotateRight, RotateClockwise, RotateCounterclockwise, RotateForwards, RotateReverse, Disappear |
| Blink, Frustration | the data corresponding to the Blink and Frustration EEG event _(any other EEG event can also be used from the affective or expressive category)_                                                                                                                     |
Any incoming JSON objects should then be parsed and used.


## Sample (Random) EEG Data For '*' Client Request
```
{
  "HeadsetData": {
    "EmoState": 0,
    "IsHeadsetOnCorrectly": 1,
    "Timestamp": 1449703039564,
    "WirelessSignalStatus": "GOOD_SIGNAL",
    "BatteryChargeLevel": 3
  },
  "EmoStateData": {
    "Gyros": {
      "GyroX": 0.43117159605026245,
      "GyroY": 0.76333498954772949
    },
    "Expressive": {
      "LookingRight": 0.42345261573791504,
      "Clench": 0.7899823784828186,
      "EyebrowRaise": 0.041739881038665771,
      "LookingLeft": 0.2046046257019043,
      "LookingDown": 0.7165074348449707,
      "LookingUp": 0.17928200960159302,
      "RightWink": 0.30473947525024414,
      "Blink": 0.85040992498397827,
      "EyesOpen": 0.26875156164169312,
      "Smile": 0.51121920347213745,
      "LeftWink": 0.92426103353500366
    },
    "Affective": {
      "Meditation": 0.395449697971344,
      "EngagementBoredom": 0.62335872650146484,
      "ExcitementShortTerm": 0.808492124080658,
      "Frustration": 0.8049471378326416,
      "ExcitementLongTerm": 0.45547342300415039
    },
    "Cognitive": {}
  }
}
```
