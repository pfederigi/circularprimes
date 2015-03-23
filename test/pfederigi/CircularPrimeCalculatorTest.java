/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pfederigi;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author pablo
 */
public class CircularPrimeCalculatorTest {

  public CircularPrimeCalculatorTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of main method, of class CircularPrimeCalculator.
   */
  @Test
  public void testResult() throws Exception {

    final int[] expected = {
      2, 3, 5, 7,
      11,
      13, 31,
      17, 71,
      37, 73,
      79, 97,
      113, 131, 311,
      197, 971, 719,
      199, 919, 991,
      337, 373, 733,
      1193, 1931, 3119, 9311,
      3779, 7793, 7937, 9377,
      11939, 19391, 39119, 91193, 93911,
      19937, 37199, 71993, 93719, 99371,
      193939, 939391, 393919, 939193, 391939, 919393,
      199933, 999331, 993319, 933199, 331999, 319993
    };

    Arrays.sort(expected);

    CircularPrimeCalculator calc = new CircularPrimeCalculator();
    calc.execute();
    Set<Integer> result = calc.getResult();
    if (result == null || result.isEmpty()) {
      fail("Result is null or is empty");
    } else {
      Iterator<Integer> it = result.iterator();
      int i = 0;
      for (; it.hasNext(); i++) {
        int n = it.next();
        if (i < expected.length) {
          if (expected[i] != n) {
            fail("Expected: " + expected[i] + " - Value: " + n);
          }
        } else {
          fail("Expected: null - Value: " + n);
        }
      }
      if (i < expected.length) {
        fail("Expected: " + expected[i] + " - Value: null");
      }

      if (expected.length == result.size() && result.containsAll(result)) {
      } else {
        fail("Results fail");
      }
    }
  }

}
