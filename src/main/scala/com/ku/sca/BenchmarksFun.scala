package com.ku.sca

import breeze.linalg._
import breeze.math._
import breeze.numerics._
import breeze.stats.distributions._

object BenchmarksFun {
  
  val dim = 30

  def getBoundary( benchmark: String ) : (Double, Double) = {
    benchmark match {
    case "F1"|"F3"|"F4"|"F6" => return (-100.0, 100.0)
    case "F2"                => return (-10.0, 10.0)
    case "F5"                => return (-30.0, 30.0)
    case "F7"                => return (-1.28, 1.28)
    case "F9"                => return (-5.12, 5.12)
    case "F10"               => return (-32.0, 32.0)
    case "F11"               => return (-600.0, 600.0)
    case "F16"|"F17"|"F18"   => return (-5.0, 5.0)
    
    }
  }

  // Sphere Unimodal
  def f1( searchAgent: Array[Double] ) = {

    sum(searchAgent.map(agent => pow(agent, 2)))
  }

  // Schwefel 2.22 Unimodal
  def f2( searchAgent: Array[Double] ) = {

    sum( searchAgent.map { i => i.abs } ) + searchAgent.map { i => i.abs }.product
  }

  // Schwefel 1.2 Unimodal composite
  def f3( searchAgent: Array[Double] ) = {

    val dim = searchAgent.length

    var result = 0.0
    for (i <- 1 to  dim) {
      result = result + pow(sum(searchAgent.slice(0, i)), 2)
    }

    result
  }

  // Schwefel 2.21 Unimodal
  def f4( searchAgent: Array[Double] ) = {

    max( searchAgent.map { i => i.abs } )
  }

  // Rosenbrock Unimodal composite
  def f5( searchAgent: Array[Double] ) = {

    val dim = searchAgent.length

    val tmp = searchAgent.slice(0, dim-1)

    sum(pow((searchAgent.slice(1, dim) - pow(tmp, 2)), 2).map(_*100.0).zip(pow(tmp.map(_-1), 2)).map { case (a, b) => a + b })
  }

  // Step Unimodal composite
  def f6( searchAgent: Array[Double] ) = {
    sum(searchAgent.map(agent => pow((agent+0.5).abs, 2)))
  }

  // Quartic Unimodal composite
  def f7( searchAgent: Array[Double] ) = {
    sum(searchAgent.zipWithIndex.map { case (agent, idx) => (idx+1)*pow(agent, 4)}) + scala.util.Random.nextDouble()
  }

  // Rastrigin Multimodal F9 in the paper
  def f9(x: Array[Double]) = {

    val dim = x.length

    sum(x.map(i=>pow(i, 2)-10.0*cos(2.0*i*(constants.Pi))+10.0))
  }

  // Ackley Multimodal composite F10 in the paper
  def f10(x: Array[Double]) = {

    val dim = x.length

    -20.0*exp(-0.2*sqrt(sum(x.map(pow(_, 2)))/dim)) - exp(sum(x.map(i=>cos(2.0*i*(constants.Pi))))/dim)+20.0+exp(1.0)
  }

  // Griewank Multimodal
  def f11(searchAgent: Array[Double]) = {

    (sum(searchAgent.map(agent => pow(agent, 2)))/4000) - searchAgent.zipWithIndex.map { case (agent, idx) => cos(agent/sqrt(idx+1.0)) }.product + 1.0
  }

  // F16 (CF3)
  def f16(searchAgent: Array[Double]) = {

    4*(pow(searchAgent(0), 2))-2.1*(pow(searchAgent(0), 4))+(pow(searchAgent(0), 6))/3+searchAgent(0)*searchAgent(1)-4*(pow(searchAgent(1), 2))+4*(pow(searchAgent(1), 4))
  }

  // F17 (CF4)
  def f17(searchAgent: Array[Double]) = {

    pow((searchAgent(1)-(pow(searchAgent(0), 2))*5.1/(4*(pow(constants.Pi, 2)))+5/constants.Pi*searchAgent(0)-6), 2)+10*(1-1/(8*constants.Pi))*cos(searchAgent(0))+10;
  }

  // F18 (CF5)
  def f18(searchAgent: Array[Double]) = {

    (1+pow((searchAgent(0)+searchAgent(1)+1), 2)*(19-14*searchAgent(0)+3*(pow(searchAgent(0), 2))-14*searchAgent(1)+6*searchAgent(0)*searchAgent(1)+3*pow(searchAgent(1), 2)))*(30+pow((2*searchAgent(0)-3*searchAgent(1)), 2)*(18-32*searchAgent(0)+12*(pow(searchAgent(0), 2))+48*searchAgent(1)-36*searchAgent(0)*searchAgent(1)+27*(pow(searchAgent(1), 2))));
  }
}
