import org.scalatest.funsuite.AnyFunSuite

class CalcSpec extends AnyFunSuite {
  test("adds") { assert(Calc.add(2, 3) == 5) }
  test("multiplies") { assert(Calc.multiply(4, 3) == 12) }
  test("subtracts") { assert(Calc.subtract(7, 3) == 4) }
  test("divides") { assert(Calc.divide(12, 4) == 3) }
}
