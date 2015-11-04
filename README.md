# Emotiv-JSON-API
An API for receiving Emotiv EPOC EEG events in JSON format using a TCP socket
##This is part of an unfinished thesis, use at your own discretion.

##What:
This project is meant to take data from a raw-data enabled EPOC or EPOC+ and convert it into JSON objects. It's useless if you don't have the Emotiv Xavier Research Edition libraries.

If you have the libraries and they're in the correct location on your computer (/usr/lib/ in the case of Mac users), an EPOC/EPOC+ properly on, and some kind of client that's connected to API_Main's socket port, you should be all set. For example, in the [MindControlledUAV project](https://github.com/AshleyDumaine/MindControlledUAV), ImprovedMain.java acts as the client, giving the server the 2 EEG events it wants updates on (and gyros as an extra event if applicable). The server responds with a stream of JSON objects, updating the client whenever the headset sends a new packet of sensor data.

Currently, this successfully sends JSON objects over a TCP socket connection to any listening clients, but can stand to use some cleaning up and inclusion of features such as no specification of EEG events needed if a client wants updates on all possible EEG events.

Eventually, this project may use an artifical neural net (ANN) or multiple support vector machines (multi-SVM) to take raw headset data and classify different EEG events, but only if it provides higher accuracy than the built-in classification currently in the Emotiv libraries. This is the main reason that this part of the thesis is still considered unfinished.

##Why:
A couple years ago, I created a [mind-controlled drone](https://www.youtube.com/watch?v=X8jemlFtgBs) using the Emotiv SDK, an A.R. Parrot Drone 2, and a Java program I wrote to manage the communication between the headset and the drone. However, it was horribly messy as there was no straightforward communication mechanism between the EPOC and other devices/programs. 

What I did was use the EmoKey program that came with the Emotiv SDK to send emulated keystrokes to a Java window that had a KeyEventListener. Based on the key registered, it would send a different command to the drone. However, this required running the Emotiv Control Panel, EmoKey, and having the Java window in focus the whole time. Not to mention configuring commands based on arbitrary keys was confusing and annoying.

I thought it would be nice to use sockets to communticate and a nice format such as JSON to transmit headset data instead of arbitrary keystrokes to an application in focus. I somewhat recently applied this change to the mind-controlled drone project and the code became a lot less messy and dare I say it even worked better than my previous implementation that relied on EmoKey. One huge difference was that this allowed me to control the drone with gyros, which is impossible to do using EmoKey.
