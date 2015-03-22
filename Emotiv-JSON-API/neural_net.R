install.packages('neuralnet')
library("neuralnet")

#-----------NORMALIZE-------------#

#use the following equation: (x - L) / (U - L) where L is min and U is max
normalize <- function(input, baseline) {
	#set the upper and lower limits
	range <- 500
	upperLimit <- baseline + range
	lowerLimit <- baseline - range
	if (input > upperLimit) { input <- upperLimit } #ceiling
	if (input < lowerLimit) { input <- lowerLimit } #floor
	(input - lowerLimit) / (upperLimit - lowerLimit)
}

#--------------PRINT---------------#
print <- function(x, ...) {
    if (is.numeric(x)) base::print(round(x, digits=2), ...) 
    else base::print(x, ...)
}

#----------------GET TRAINING DATA--------------#
#take a csv and extract sensor values in chunks of 5 for all 14 sensors
getData <- function(inputFile, training) {
	if (training) {
		trainingdata <<- read.csv(file=inputFile, header = F)
	}
	#print(trainingdata) #debug
	#col 1 = sample counter
	#col 2 = unknown, always 0
	#col 3 = heartbeats?
	#col 4 - 17 = sensors 1 - 14
	#col 18, 19 = references?
	#col 20 - 21 = ???
	#col 22 = calculated baseline average value <--- remove this
	#col 23 - 38 = EEG event on/off
	
	#baselines calculated from avg value of headset sensors when not on head
	#TODO: get rid of MoreArgs, use derivatives for smoothing of spikes
	sensor_1 <-as.data.frame(mapply(normalize, trainingdata[,4], MoreArgs = list(trainingdata[1,22])))
	sensor_2 <-as.data.frame(mapply(normalize, trainingdata[,5], MoreArgs = list(trainingdata[2,22])))
	sensor_3 <-as.data.frame(mapply(normalize, trainingdata[,6], MoreArgs = list(trainingdata[3,22])))
	sensor_4 <-as.data.frame(mapply(normalize, trainingdata[,7], MoreArgs = list(trainingdata[4,22])))
	sensor_5 <-as.data.frame(mapply(normalize, trainingdata[,8], MoreArgs = list(trainingdata[5,22])))
	sensor_6 <-as.data.frame(mapply(normalize, trainingdata[,9], MoreArgs = list(trainingdata[6,22])))
	sensor_7 <-as.data.frame(mapply(normalize, trainingdata[,10], MoreArgs = list(trainingdata[7,22])))
	sensor_8 <-as.data.frame(mapply(normalize, trainingdata[,11], MoreArgs = list(trainingdata[8,22])))
	sensor_9 <-as.data.frame(mapply(normalize, trainingdata[,12], MoreArgs = list(trainingdata[9,22])))
	sensor_10 <-as.data.frame(mapply(normalize, trainingdata[,13], MoreArgs = list(trainingdata[10,22])))
	sensor_11 <-as.data.frame(mapply(normalize, trainingdata[,14], MoreArgs = list(trainingdata[11,22])))
	sensor_12 <-as.data.frame(mapply(normalize, trainingdata[,15], MoreArgs = list(trainingdata[12,22])))
	sensor_13 <-as.data.frame(mapply(normalize, trainingdata[,16], MoreArgs = list(trainingdata[13,22])))
	sensor_14 <-as.data.frame(mapply(normalize, trainingdata[,17], MoreArgs = list(trainingdata[14,22])))
	
	#List of sensor names: AF3, AF4, F3, F4, FC5, FC6, F7, F8, T7, T8, P7, P8, O1, O2
	max <- nrow(sensor_4)
	#oh no, globals!!
	AF3_0 <<- sensor_1[1:(max - 4),]
	AF3_1 <<- sensor_1[2:(max - 3),]
	AF3_2 <<- sensor_1[3:(max - 2),]
	AF3_3 <<- sensor_1[4:(max - 1),]
	AF3_4 <<- sensor_1[5:(max),]
	AF4_0 <<- sensor_2[1:(max - 4),]
	AF4_1 <<- sensor_2[2:(max - 3),]
	AF4_2 <<- sensor_2[3:(max - 2),]
	AF4_3 <<- sensor_2[4:(max - 1),]
	AF4_4 <<- sensor_2[5:(max),]
	F3_0 <<- sensor_3[1:(max - 4),]
	F3_1 <<- sensor_3[2:(max - 3),]
	F3_2 <<- sensor_3[3:(max - 2),]
	F3_3 <<- sensor_3[4:(max - 1),]
	F3_4 <<- sensor_3[5:(max),]
	F4_0 <<- sensor_4[1:(max - 4),]
	F4_1 <<- sensor_4[2:(max - 3),]
	F4_2 <<- sensor_4[3:(max - 2),]
	F4_3 <<- sensor_4[4:(max - 1),]
	F4_4 <<- sensor_4[5:(max),]
	FC5_0 <<- sensor_5[1:(max - 4),]
	FC5_1 <<- sensor_5[2:(max - 3),]
	FC5_2 <<- sensor_5[3:(max - 2),]
	FC5_3 <<- sensor_5[4:(max - 1),]
	FC5_4 <<- sensor_5[5:(max),]
	FC6_0 <<- sensor_6[1:(max - 4),]
	FC6_1 <<- sensor_6[2:(max - 3),]
	FC6_2 <<- sensor_6[3:(max - 2),]
	FC6_3 <<- sensor_6[4:(max - 1),]
	FC6_4 <<- sensor_7[5:(max),]
	F7_0 <<- sensor_7[1:(max - 4),]
	F7_1 <<- sensor_7[2:(max - 3),]
	F7_2 <<- sensor_7[3:(max - 2),]
	F7_3 <<- sensor_7[4:(max - 1),]
	F7_4 <<- sensor_7[5:(max),]
	F8_0 <<- sensor_8[1:(max - 4),]
	F8_1 <<- sensor_8[2:(max - 3),]
	F8_2 <<- sensor_8[3:(max - 2),]
	F8_3 <<- sensor_8[4:(max - 1),]
	F8_4 <<- sensor_8[5:(max),]
	T7_0 <<- sensor_9[1:(max - 4),]
	T7_1 <<- sensor_9[2:(max - 3),]
	T7_2 <<- sensor_9[3:(max - 2),]
	T7_3 <<- sensor_9[4:(max - 1),]
	T7_4 <<- sensor_9[5:(max),]
	T8_0 <<- sensor_10[1:(max - 4),]
	T8_1 <<- sensor_10[2:(max - 3),]
	T8_2 <<- sensor_10[3:(max - 2),]
	T8_3 <<- sensor_10[4:(max - 1),]
	T8_4 <<- sensor_10[5:(max),]
	P7_0 <<- sensor_11[1:(max - 4),]
	P7_1 <<- sensor_11[2:(max - 3),]
	P7_2 <<- sensor_11[3:(max - 2),]
	P7_3 <<- sensor_11[4:(max - 1),]
	P7_4 <<- sensor_11[5:(max),]
	P8_0 <<- sensor_12[1:(max - 4),]
	P8_1 <<- sensor_12[2:(max - 3),]
	P8_2 <<- sensor_12[3:(max - 2),]
	P8_3 <<- sensor_12[4:(max - 1),]
	P8_4 <<- sensor_12[5:(max),]
	O1_0 <<- sensor_13[1:(max - 4),]
	O1_1 <<- sensor_13[2:(max - 3),]
	O1_2 <<- sensor_13[3:(max - 2),]
	O1_3 <<- sensor_13[4:(max - 1),]
	O1_4 <<- sensor_13[5:(max),]
	O2_0 <<- sensor_14[1:(max - 4),]
	O2_1 <<- sensor_14[2:(max - 3),]
	O2_2 <<- sensor_14[3:(max - 2),]
	O2_3 <<- sensor_14[4:(max - 1),]
	O2_4 <<- sensor_14[5:(max),]
	
	if (training) { #only applicable to training data, the client will not send such values
		#expected outputs (not averaged, truncated)
		#TODO: get vector of averages for EEG events for 5 samples at a time
		Blink <<- as.vector(trainingdata[1:(max - 4),23])
		LeftWink <<- as.vector(trainingdata[1:(max - 4),24])
		RightWink <<- as.vector(trainingdata[1:(max - 4),25])
		Furrow <<- as.vector(trainingdata[1:(max - 4),26])
		RaiseBrow <<- as.vector(trainingdata[1:(max - 4),27])
		Smile <<- as.vector(trainingdata[1:(max - 4),28])
		Clench <<- as.vector(trainingdata[1:(max - 4),29])
		LookLeft <<- as.vector(trainingdata[1:(max - 4),30])
		LookRight <<- as.vector(trainingdata[1:(max - 4),31])
		Laugh <<- as.vector(trainingdata[1:(max - 4),32])
		SmirkLeft <<- as.vector(trainingdata[1:(max - 4),33])
		SmirkRight <<- as.vector(trainingdata[1:(max - 4),34])
		Cognitive_0 <<- as.vector(trainingdata[1:(max - 4),35])
		Cognitive_1 <<- as.vector(trainingdata[1:(max - 4),36])
		Cognitive_2 <<- as.vector(trainingdata[1:(max - 4),37])
		Cognitive_3 <<- as.vector(trainingdata[1:(max - 4),38])
	}
}

#------------------TRAIN NETWORK-----------------#
trainNetwork <- function() {
getData(inputFile="/Users/x/Dropbox/baseline2.csv", training=T)

	input <- data.frame(AF3_0, AF3_1, AF3_2, AF3_3, AF3_4, AF4_0, AF4_1, AF4_2, AF4_3, AF4_4, F3_0, F3_1, F3_2, F3_3, F3_4, F4_0, F4_1, F4_2, F4_3, F4_4, FC5_0, FC5_1, FC5_2, FC5_3, FC5_4, FC6_0, FC6_1, FC6_2, FC6_3, FC6_4, F7_0, F7_1, F7_2, F7_3, F7_4, F8_0, F8_1, F8_2, F8_3, F8_4, T7_0, T7_1, T7_2, T7_3, T7_4, T8_0, T8_1, T8_2, T8_3, T8_4, P7_0, P7_1, P7_2, P7_3, P7_4, P8_0, P8_1, P8_2, P8_3, P8_4, O1_0, O1_1, O1_2, O1_3, O1_4, O2_0, O2_1, O2_2, O2_3, O2_4, Blink, LeftWink, RightWink, Furrow, RaiseBrow, Smile, Clench, LookLeft, LookRight, Laugh, SmirkLeft, SmirkRight, Cognitive_0, Cognitive_1, Cognitive_2, Cognitive_3)
	
	#train the network
	net.ans <<- neuralnet(Blink+LeftWink+RightWink+Furrow+RaiseBrow+Smile+Clench+LookLeft+LookRight+Laugh+SmirkLeft+SmirkRight+Cognitive_0+Cognitive_1+Cognitive_2+Cognitive_3~AF3_0+AF3_1+AF3_2+AF3_3+AF3_4+AF4_0+AF4_1+AF4_2+AF4_3+AF4_4+F3_0+F3_1+F3_2+F3_3+F3_4+F4_0+F4_1+F4_2+F4_3+F4_4+FC5_0+FC5_1+FC5_2+FC5_3+FC5_4+FC6_0+FC6_1+FC6_2+FC6_3+FC6_4+F7_0+F7_1+F7_2+F7_3+F7_4+F8_0+F8_1+F8_2+F8_3+F8_4+T7_0+T7_1+T7_2+T7_3+T7_4+T8_0+T8_1+T8_2+T8_3+T8_4+P7_0+P7_1+P7_2+P7_3+P7_4+P8_0+P8_1+P8_2+P8_3+P8_4+O1_0+O1_1+O1_2+O1_3+O1_4+O2_0+O2_1+O2_2+O2_3+O2_4, input, hidden=43, threshold=0.01)
}

#-------------MAIN---------------#
#prints out a stream of sensor readings from a socket
server <- function(){
	print("Network training...")
	#train neural net first
	trainNetwork()
	print("Network trained!")
	con <- socketConnection(host="localhost", port=3333, blocking=TRUE, server=TRUE, open="r")
	on.exit(close(con))
	writeLines("Listening...")
	repeat{
		data <- readLines(con, 5) #read 5 samples
		if(!is.null(data)) {
			vals <- as.numeric(unlist(strsplit(data,",")))
			if (length(data) != 0) {
				#print(vals) #do something here
				#get test data directly from the socket
				#convert data to dataframe here
				#getData(inputFile=data, training=F)
				
				#test <- data.frame(AF3_0, AF3_1, AF3_2, AF3_3, AF3_4, AF4_0, AF4_1, AF4_2, AF4_3, AF4_4, F3_0, F3_1, F3_2, F3_3, F3_4, F4_0, F4_1, F4_2, F4_3, F4_4, FC5_0, FC5_1, FC5_2, FC5_3, FC5_4, FC6_0, FC6_1, FC6_2, FC6_3, FC6_4, F7_0, F7_1, F7_2, F7_3, F7_4, F8_0, F8_1, F8_2, F8_3, F8_4, T7_0, T7_1, T7_2, T7_3, T7_4, T8_0, T8_1, T8_2, T8_3, T8_4, P7_0, P7_1, P7_2, P7_3, P7_4, P8_0, P8_1, P8_2, P8_3, P8_4, O1_0, O1_1, O1_2, O1_3, O1_4, O2_0, O2_1, O2_2, O2_3, O2_4)
				
				#net.results <- compute(net.ans, test)
				
				#see what properties net.ans has
				#ls(net.results)
				 
				#see the results
				#print(net.results$net.result)
				
				#Plot the neural network
				#plot(net.ans, rep="best",radius=2, dimension = 300, fontsize = 8)
			}
		}
	}
}
server()
