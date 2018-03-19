package driver_api.sensor

import java.time.Instant


object SensingApi {

  trait NamedEntity {
    val name: String
    val description: String
  }

  class UnitOfMeasure private (
    val name: String,
    val symbol: String,
    val description: String
  ) extends NamedEntity {
    override lazy val toString = s"$name[$symbol]"
  }

  object UnitOfMeasure {

    def apply(name: String, symbol: String, description: String = ""): UnitOfMeasure =
      new UnitOfMeasure(name, symbol, description)

    implicit class AugmentedString(name: String) {
      def |(symbol: String): UnitOfMeasure = UnitOfMeasure(name, symbol)
    }
  }

  class PropertyObserver private (
    val procedure: () => Observation,
    val observedProperty: ObservedProperty
  )

  object PropertyObserver {
    def apply(procedure: () => Observation, observedProperty: ObservedProperty): PropertyObserver = {
      new PropertyObserver(procedure, observedProperty)
    }
  }

  case class ObservedProperty(
    name: String,
    description: String,
    unitOfMeasure: UnitOfMeasure,
  ) extends NamedEntity

  class Observation private (
    val phenomenonTime: Instant, //or interval?
    val resultTime: Instant,
    val result: String
  ) {
    override lazy val toString =
      s"[$phenomenonTime] ~> $result"
  }
  object Observation {

    def apply(phenomenonTime: Instant, resultTime: Instant, result: String): Observation =
      new Observation(phenomenonTime, resultTime, result)

    def now(result: String): Observation =
      new Observation(Instant.now, Instant.now, result)

  }
}
