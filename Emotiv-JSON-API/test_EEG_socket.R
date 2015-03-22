#prints out a stream of sensor readings from a socket
server <- function(){
    writeLines("Listening...")
    con <- socketConnection(host="localhost", port=3333, blocking=TRUE, server=TRUE, open="r")
    on.exit(close(con))
    repeat{
    	data <- readLines(con, 1)
    	if(!is.null(data)) {
    		vals <- as.numeric(unlist(strsplit(data,",")))
    		if (length(data) != 0) print(vals)
    	}
    }
}
server()
