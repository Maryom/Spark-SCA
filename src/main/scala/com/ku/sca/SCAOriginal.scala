package com.ku.sca

import org.apache.log4j._
import org.apache.spark._
import org.apache.spark.mllib.rdd.RDDFunctions._
import scala.io.Source
import breeze.linalg._
import breeze.math._
import breeze.numerics._
import breeze.stats.distributions._
import org.apache.spark.rdd.RDD

object SCAOriginal {
 
  /** Our main function where the action happens */
  def main(args: Array[String]) {
    
    val dim            = BenchmarksFun.dim
    val (lb, ub)       = BenchmarksFun.getBoundary("F1")
    val PopulationSize = 32
    val Max_iter       = 300
    var t              = 0 // Loop counter
    var myArray        = Array.fill(PopulationSize)(0,Array.fill(dim)(0.0))
 
    // initialize score for the leader
    var Destination_position = Array.fill(dim)(0.0)
    var Destination_fitness: Double = Double.PositiveInfinity
    
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using every core of the cluster
    val conf = new SparkConf()
    conf.setAppName("SCAOriginal")
    
    val sc: SparkContext = new SparkContext("local[*]", "SCA")
    
    var avgTime    = List.empty[Double]
    var avgLeader  = List.empty[Double]
    var leader4run = List.empty[Double]
    var n = 0 // for averaging results
    
    while( n < 31 ) {
      val t1 = System.nanoTime
      
      /* Initialize the positions of search agents
    *
    * scala.util.Random.nextDouble() = numpy.random.uniform(0,1)
    * List.fill(PopulationSize)(List.fill(dim) = (SearchAgents_no,dim)
    */
    var Positions = Array.fill(PopulationSize)(Array.fill(dim)((scala.util.Random.nextDouble()*(ub-lb)+lb)))
    
    // Calculate the fitness of the first set and find the best one
    var Objective_values = Double.PositiveInfinity
  
    for((pos, idx) <- Positions.zipWithIndex) {
      
      Objective_values = BenchmarksFun.f1(pos)
      if (idx == 0) {
        Destination_position = pos
        Destination_fitness  = Objective_values
      } else if (Objective_values < Destination_fitness) {
        Destination_position = pos
        Destination_fitness  = Objective_values
      }
    }
      
      leader4run :+= Destination_fitness
    
    // Main loop
    t = 2 // start from the second iteration since the first iteration was dedicated to calculating the fitness
    while( t <= Max_iter ) {
      // Eq. (3.4)
      val a = 2
      val r1=a-t*((a)/Max_iter); // r1 decreases linearly from a to 0
      
      // Update the position of solutions with respect to destination
      var SearchAgents_no = 0
      while( SearchAgents_no < PopulationSize ) {
        for( index <- 0 until dim ) {
          // Update r2, r3, and r4 for Eq. (3.3)
          val r2 = (2*constants.Pi)*(scala.util.Random.nextDouble())
          val r3 = 2*(scala.util.Random.nextDouble())
          val r4 = scala.util.Random.nextDouble()

          val pos = Positions(SearchAgents_no)(index)
          
          // Eq. (3.3)
          if (r4 < 0.5) {
            // Eq. (3.1) X(i,j)= X(i,j)+(r1*sin(r2)*abs(r3*Destination_position(j)-X(i,j)));
            Positions(SearchAgents_no)(index) = pos+(r1*sin(r2)*abs(r3*Destination_position(index)-pos))
          } else {
            // Eq. (3.2) X(i,j)= X(i,j)+(r1*cos(r2)*abs(r3*Destination_position(j)-X(i,j)));
            Positions(SearchAgents_no)(index) = pos+(r1*cos(r2)*abs(r3*Destination_position(index)-pos))
          }
        }
        SearchAgents_no = SearchAgents_no + 1
      }
      
      // Check if solutions go outside the search spaceand bring them back
      SearchAgents_no = 0
      while( SearchAgents_no < PopulationSize ) {
        Positions(SearchAgents_no) = clip(Positions(SearchAgents_no), lb, ub)
        
        // Calculate the objective values
        Objective_values = BenchmarksFun.f1(Positions(SearchAgents_no))
        
        // Update the destination if there is a better solution
        if (Objective_values < Destination_fitness) {
          Destination_position = Positions(SearchAgents_no)
          Destination_fitness  = Objective_values         
        }
        
        SearchAgents_no = SearchAgents_no + 1
      }
      leader4run :+= Destination_fitness
      t=t+1
    }
    
    // As it is a nano second we need to divide it by 1000000000. in 1e9d "d" stands for double
    val duration = (System.nanoTime - t1) / 1e9d
      
      avgTime :+= duration
      avgLeader :+= Destination_fitness
      println("n")
      println(n)
    
      val avgLeader4run = (leader4run.sum) / leader4run.size
    
      println("avgLeader4run")
    
      println("Max. Leader4run")
    
      println("Min. Leader4run")
    
      println(avgLeader4run)
    
      println(leader4run.max)
    
      println(leader4run.min)
      
      n = n + 1
      t = 0
      leader4run = List.empty[Double]
    }
    
    avgTime.foreach(x=> println(f"$x%1.4f"))
    val myavgTime  = avgTime.drop(1)
    val theAvgTime = (myavgTime.sum) / myavgTime.size
    
    println("theAvgTime")
    
    println("Max. time")
    
    println("Min. time")
    
    println(theAvgTime)
    
    println(avgTime.max)
    
    println(avgTime.min)

    avgLeader.foreach(x=> println(x))
    val myavgLeader  = avgLeader.drop(1)
    val theAvgLeader = (myavgLeader.sum) / myavgLeader.size
    
    println("theAvgLeader")
    
    println("Max. Leader")
    
    println("Min. Leader")
    
    println(theAvgLeader)
    
    println(avgLeader.max)
    
    println(avgLeader.min)
    
    sc.stop()
  }
}
