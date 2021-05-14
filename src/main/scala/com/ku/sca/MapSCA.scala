package com.ku.sca

import org.apache.log4j._
import org.apache.spark._
import org.apache.spark.mllib.rdd.RDDFunctions._
import breeze.linalg._
import breeze.math._
import breeze.numerics._
import breeze.stats.distributions._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object MapSCA {
  
  val SearchAgents_no = 96
  val Max_iter        = 2
  val inner_loop      = 50
  val inner_stop      = 49
  val dim             = BenchmarksFun.dim
  val (lb, ub)        = BenchmarksFun.getBoundary("F1")
  val a               = 2
  def updateRs(t: Int) : (Double, Double, Double, Double) = {
      
      // Eq. (3.4)
      // r1=a-t*((a)/Max_iteration); % r1 decreases linearly from a to 0
      val r1 = a - t *((a)/Max_iter)
      
      // Update r2, r3, and r4 for Eq. (3.3)
      // r2=(2*pi)*rand();
      val r2 = (2*constants.Pi)*(scala.util.Random.nextDouble())
      
      // r3=2*rand;
      val r3 = 2*(scala.util.Random.nextDouble())
      
      // r4=rand();
      val r4 = scala.util.Random.nextDouble()
    
      return (r1, r2, r3, r4)
    }

  // initialize score for the leader
  var destination_fitness_pos = (Double.PositiveInfinity, Array.fill(dim)(0.0))
  
  /** Our main function where the action happens */
  def main(args: Array[String]) {

    var t = 0 // Loop counter 
    var n = 0 // for averaging results
    var avgTime    = List.empty[Double]
    var avgLeader  = List.empty[Double]
    var leader4run = List.empty[Double]
    var avgIteration = List.empty[Int]
    
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)
    
    // Create a SparkContext using every core of the cluster
    val conf = new SparkConf()
             .setMaster("local[*]")
             .setAppName("SCA")
             
    val sc = new SparkContext(conf)

    var duration = 0.0
    var t1 = System.nanoTime
    while( n < 31 ) {

    // To determine # of partitions in an RDD, rdd.partitions.size
    println("The minimum number of partition", sc.defaultParallelism)
    
    val partitionsNum = sc.defaultParallelism

    t1 = System.nanoTime

    var fitness_bc = 0.0
    /* Initialize the positions of search agents
    * 
    *   X(:,i)=rand(SearchAgents_no,1).*(ub_i-lb_i)+lb_i;
    */ 
    val pos_fitnessTmp = sc.parallelize(List.range(0, SearchAgents_no), partitionsNum).map(index => Array.fill(dim)(scala.util.Random.nextDouble()*(ub-lb)+lb)).persist
    val pos_fitness    = pos_fitnessTmp.mapPartitions(IScaStuff.updatePartitionPos).persist
    
    destination_fitness_pos = pos_fitness.glom().map((value:Array[(Double, Array[Double])]) => value.minBy(_._1)).reduce((acc,value) => { 
    if(acc._1 > value._1) value else acc})
 
    var fitness_desPosBC = sc.broadcast(destination_fitness_pos)
    
    fitness_bc  = fitness_desPosBC.value._1
    var min_pop = fitness_desPosBC.value._2
    leader4run :+= fitness_bc

    val (r1, r2, r3, r4) = updateRs(t)
      
      t = t + 1
      
      // Apply a function to update the Position of search agents (each element of RDD) : Array[(Int, Array[Double])]
      var posArray = pos_fitness.mapPartitions { agent =>
        agent.map{ x =>
          IScaStuff.updatePartitionDes(x._2, min_pop, r1, r2, r3, r4)
        }
      }.persist

      t = t + 1
      
    while( t <= Max_iter ) {

      val pos_fitness  = posArray.mapPartitions(IScaStuff.updatePartitionPos)
 
      val pos_fitness_rdd = pos_fitness.mapPartitions {

        var positions = Array[(Double, Array[Double])]()
        var myarray   = Array[(Double, Array[Double])]()
        var localFitness  = fitness_bc
        var local_min_pop = min_pop
        var m = 0
        (iterator) => {
          while( m < inner_loop ) {
       
            if (m == 0) { myarray = iterator.toArray }

            myarray.foreach { e =>
                if (e._1 < localFitness) {
                  localFitness  = e._1
                  local_min_pop = e._2
                }
              } 
            
            val (r1, r2, r3, r4) = updateRs(m)
            
            // Apply a function to update the Position of search agents (each element of RDD partition)
            myarray = myarray.map(x=> IScaStuff.updatePartitionDes(x._2, local_min_pop, r1, r2, r3, r4)).map(x=>IScaStuff.calFitness(x))
            
            if (m == inner_stop) {
              if (localFitness < fitness_bc) {
                fitness_bc = localFitness
              }

              positions = myarray
            }
            m = m + 1 
          }
         }
        positions.iterator
     }.persist

     val checkIt = pos_fitness_rdd.glom().map((value:Array[(Double, Array[Double])]) => value.minBy(_._1))
     
     val tmp_destination_fitness_pos = checkIt.reduce((acc,value) => { 
  if(acc._1 > value._1) value else acc})
           
      posArray = pos_fitness_rdd.map(_._2)

      if (tmp_destination_fitness_pos._1 < fitness_bc) {
        fitness_desPosBC.destroy
        fitness_desPosBC = sc.broadcast(tmp_destination_fitness_pos)
        fitness_bc       = fitness_desPosBC.value._1
        min_pop          = fitness_desPosBC.value._2
      }

      leader4run :+= fitness_bc
              
      t = t + 1
    }
    
    // As it is a nano second we need to divide it by 1000000000. in 1e9d "d" stands for double
    duration = (System.nanoTime - t1) / 1e9d
    
    avgTime :+= duration
    avgLeader :+= fitness_bc
    println("n")
    println(n)
    
    leader4run.foreach(x=> println(f"$x%1.15f"))
    
    val avgLeader4run = (leader4run.sum) / leader4run.size
    
    println("avgLeader4run")
    
    println("Max. avgLeader4run")
    
    println("Min. avgLeader4run")
    
    println(avgLeader4run)
    
    println(leader4run.max)
    
    println(leader4run.min)
    
    n = n + 1
    t = 0
    leader4run = List.empty[Double]
    fitness_desPosBC.destroy()
    fitness_bc = 0.0
    duration = 0.0
    }
    
    avgTime.foreach(x=> println(f"$x%1.4f"))
    val myavgTime = avgTime.drop(1)
    val theAvgTime = (myavgTime.sum) / myavgTime.size
    
    println("theAvgTime")
    
    println("Max. time")
    
    println("Min. time")
    
    println(theAvgTime)
    
    println(avgTime.max)
    
    println(avgTime.min)
    println("theAvgLeader")
    avgLeader.foreach(x=> println(f"$x%1.11f"))
    val myavgLeader = avgLeader.drop(1)
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