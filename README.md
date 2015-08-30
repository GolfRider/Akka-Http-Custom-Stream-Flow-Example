# Akka-Http-Stream-Flow-Custom-Example
Akka Http with custom stream flow processing

  1. Run the app via sbt: sbt run
  2. Access the app from url: http://localhost:9000/count?id=<n> // n is some integer
  3. The services process the custom stream flow and gives the output

  This example showcases Akka Http with Akka Custom Stream Flow. 
  Every element of the stream flow is 
  executed by a seperate actor. Enjoy :-)
