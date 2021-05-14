package com.ku.sca

import breeze.linalg._
import breeze.math._
import breeze.numerics._
import breeze.stats.distributions._

object IScaStuff {

  // Update the position of solutions with respect to destination
  def updatePosDes(searchAgentNum: Int, x: Array[Double], Des_pos: Array[Double], r1: Double, r2: Double, r3: Double, r4: Double) = {
    
    var newPositions = Array.fill(x.length)(0.0)
    
    // Eq. (3.3)
    if (r4 < 0.5) {
      // Eq. (3.1) X(i,j)= X(i,j)+(r1*sin(r2)*abs(r3*Destination_position(j)-X(i,j)));
      newPositions = (abs((Des_pos.map( x=> x*r3) - x)).map( y=> y*r1*sin(r2)) zip x).map( x=> x._1 + x._2 )
      
    } else {
      // Eq. (3.2) X(i,j)= X(i,j)+(r1*cos(r2)*abs(r3*Destination_position(j)-X(i,j)));
      newPositions = (abs((Des_pos.map( x=> x*r3) - x)).map( y=> y*r1*cos(r2)) zip x).map( x=> x._1 + x._2 )
    }
    
    (searchAgentNum, newPositions)
  }
  
  def updatePartitionDes(x: Array[Double], Des_pos: Array[Double], r1: Double, r2: Double, r3: Double, r4: Double) = {
    
    var newPositions = Array.fill(x.length)(0.0)

    // Eq. (3.3)
    if (r4 < 0.5) {
      // Eq. (3.1) X(i,j)= X(i,j)+(r1*sin(r2)*abs(r3*Destination_position(j)-X(i,j)));
      newPositions = (abs((Des_pos.map( y=> y*r3) - x)).map( y=> y*r1*sin(r2)) zip x).map( y=> y._1 + y._2 )
      
    } else {
      // Eq. (3.2) X(i,j)= X(i,j)+(r1*cos(r2)*abs(r3*Destination_position(j)-X(i,j)));
      newPositions = (abs((Des_pos.map( y=> y*r3) - x)).map( y=> y*r1*cos(r2)) zip x).map( y=> y._1 + y._2 )
    }
    
    newPositions
  }
  
  def updatePositions( searchAgent: Iterator[(Int, Array[Double])] ) = {

    searchAgent.map{ positions =>
      
      // here make sure there is no need to clip
      val clippedPos = clip(positions._2, MapSCA.lb, MapSCA.ub)

      /* Calculate objective function for each search agent
     			* 
     			* It will return (fitness, searchAgent)
     			*/
           val fitness = BenchmarksFun.f1(clippedPos)
           
      (positions._1, clippedPos, fitness)
    }
  }
  
  def updatePartitionPos( searchAgent: Iterator[Array[Double]] ) = {

    searchAgent.map{ positions =>

      // here make sure there is no need to clip
      val clippedPos = clip(positions, MapSCA.lb, MapSCA.ub)
      
      /* Calculate objective function for each search agent
     			* 
     			* It will return (fitness, searchAgent)
     			*/
           val fitness = BenchmarksFun.f1(clippedPos)
           
      (fitness, clippedPos)
    }
  }
  
  def calFitness( searchAgent: Array[Double] ) = {

    // here make sure there is no need to clip
      val clippedPos = clip(searchAgent, MapSCA.lb, MapSCA.ub)

      /* Calculate objective function for each search agent
     			* 
     			* It will return (fitness, searchAgent)
     			*/
           val fitness = BenchmarksFun.f1(clippedPos)
           
      (fitness, clippedPos)
  }
}