package pfederigi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Proceso para calcular los números primos circulares menores a 1 millón. El
 * proceso utiliza el algoritmo denominado <i>Criba de Eratóstenes</i> que
 * permite hallar todos los números primos menores que un número natural dado
 * (definido por <code>CALC_LIMIT</code>). En una primera etapa se encuentran
 * los números primos y en la segunda etapa se buscan aquellos primos que sean
 * circulares. La tarea de ambas etapas se distribuye en varios hilos definido
 * por <code>THREAD_COUNT</code>.
 *
 * @author Federigi Pablo
 * @version 1.0 22/03/2015
 */
public class CircularPrimeCalculator {

  /**
   * Límite superior hasta el que se buscan los números primos circulares
   */
  private static final int CALC_LIMIT = 1000000;
  /**
   * Cantidad de hilos que se usarán para dividir el proceso
   */
  private static final int THREAD_COUNT = 4;

  private final Object lock = new Object();
  private final Thread[] ths;
  private final List<Integer> circularPrimes;
  private final BlockingQueue<Integer> queue;
  private final CyclicBarrier barrier;

  private final AtomicInteger totalHit = new AtomicInteger();

  public CircularPrimeCalculator() {
    ths = new Thread[THREAD_COUNT];
    circularPrimes = Collections.synchronizedList(new ArrayList());
    queue = new ArrayBlockingQueue(THREAD_COUNT, true);
    barrier = new CyclicBarrier(THREAD_COUNT);
  }

  /**
   * Inicia el proceso de calculo y búsqueda
   *
   * @throws InterruptedException
   */
  public void execute() throws InterruptedException {

    /**
     * Arreglo para marcar los números compuestos. 
     * Todo número impar que quede sin marcar en este arreglo será primo. 
     * Dicha marca es true si es compuesto y false si es primo.
     */
    final boolean[] nums = new boolean[CALC_LIMIT + 1];

    /* 
      Creación de los hilos (Threads)
    */
    for (int tid = 0; tid < THREAD_COUNT; tid++) {
      final Thread t = new Thread(new Worker(tid, nums));
      t.setName("Worker-Thread-" + tid);
      ths[tid] = t;
      t.start();
    }

    /* 
      Distribución de números a procesar.
      Se tienen en cuenta únicamente los impares mayores o igual que 3
    */
    int root = (int) Math.ceil(Math.sqrt((double) CALC_LIMIT));
    for (int i = 3; i <= root; i += 2) {
      if (!nums[i]) {
        if (i - 3 < THREAD_COUNT) {
          synchronized (lock) {
            queue.add(i);
            lock.wait();
          }
        } else {
          queue.put(i);
        }
      }
    }

    /*
      Cota de fin de cálculos para los subprocesos (-1).
    */
    for (int tid = 0; tid < THREAD_COUNT; tid++) {
      queue.put(-1);
    }

    /*
      Se agrega el número 2 como parte de los números primos circulares
    */
    if (CALC_LIMIT > 2) {
      circularPrimes.add(2);
    }

    /*
      Esperar a que finalicen los hilos.
    */
    for (int tid = 0; tid < THREAD_COUNT; tid++) {
      ths[tid].join();
    }

  }

  /**
   * Devuelve el resultado del proceso iniciado por <code>execute</code>.
   * @return los números primos circulares calculados por el método
   * <code>execute</code>.
   */
  public Set<Integer> getResult() {
    Set<Integer> result = new TreeSet(circularPrimes);
    return result;
  }

  /**
   * Imprime por pantalla el resultado del proceso iniciado por 
   * <code>execute</code>.
   * Imprime por consola la cantidad y el listado de los números primos
   * circulares.
   */
  public void report() {
    System.out.printf("Cantidad calculados: %d\n", totalHit.intValue());
    System.out.printf("\n*** Resultado ***\n");
    System.out.printf("Cantidad de primos circulares: %d\n", circularPrimes.size());
    System.out.printf("Listado: \n");
    Iterator<Integer> it = getResult().iterator();
    while (it.hasNext()) {
      System.out.println(it.next());
    }
  }

  public static void main(String[] args) throws InterruptedException {
    CircularPrimeCalculator pu = new CircularPrimeCalculator();
    pu.execute();
    pu.report();
  }

  /**
   * Subproceso para marcar múltiplos y buscar primos circulares
   */
  private class Worker implements Runnable {

    private final int TID;
    private final boolean[] nums;
    private long startTime;
    private long ssTime1;
    private long ssTime2;

    private int numbersTaken = 0;

    public Worker(int TID, boolean[] nums) {
      this.TID = TID;
      this.nums = nums;
    }

    @Override
    public void run() {

      //Marcar múltiplos
      while (true) {
        if (processNextStep()) {
          break;
        }
      }

      //Esperar a que terminen los demás hilos
      waitSStep();

      //Buscar primos circulares
      searchForCircularNumbers();

      //Imprimir reporte del subproceso
      report();
    }

    /**
     * Imprime por consola estadísticas simples del proceso.
     * Imprime el tiempo de cálculo que costó el calculo de los múltiplos,
     * la búsqueda de los primos circulares y la cantidad de números tomados
     * de la cola.
     */
    private void report() {
      System.out.printf("TID[%d] Times: %.2f ms. - %.2f ms. - %.2f ms.| Count %d\n",
              TID, ssTime1 / 1000000.0, ssTime2 / 1000000.0, (ssTime1 + ssTime2) / 1000000.0, numbersTaken);

      totalHit.addAndGet(numbersTaken);
    }

    /**
     * Espera que todos los hilos alcancen la barrera <code>barrier</code>.
     */
    private void waitSStep() {
      try {
        barrier.await();
      } catch (BrokenBarrierException | InterruptedException ex) {
        Logger.getLogger(CircularPrimeCalculator.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    /**
     * Busca los primos circulares dentro del conjunto de los números primos.
     */
    private void searchForCircularNumbers() {
      startTime = System.nanoTime();
      for (int i = 3 + (2 * (TID)); i < CALC_LIMIT; i += (2 * THREAD_COUNT)) {
        if (!nums[i] && filter(i)) {
          int orbit = i;
          boolean prime = true;
          do {
            orbit = orbit(orbit);
            if (orbit == i) {
              break;
            }
            prime = !nums[orbit];
          } while (prime);

          if (prime) {
            circularPrimes.add(i);
          }
        }
      }
      ssTime2 = System.nanoTime() - startTime;
    }

    /**
     * Busca un número en la cola y marca sus múltiplos.
     * @return indica si hay más números en la cola. <code>True</code> indica
     * que sigue habiendo números en la cola. <code>False</code> si no lo hay.
     */
    private boolean processNextStep() {
      try {
        int step = queue.take();
        if (step == -1) {
          return true;
        }
        startTime = System.nanoTime();
        numbersTaken++;
        for (int j = step; j <= CALC_LIMIT / step; j++) {
          nums[step * j] = true;
          if (step - 3 <= THREAD_COUNT && j == step + 1) {
            synchronized (lock) {
              lock.notify();
            }
          }
        }
        ssTime1 += System.nanoTime() - startTime;
      } catch (InterruptedException ex) {
        Logger.getLogger(CircularPrimeCalculator.class.getName()).log(Level.SEVERE, null, ex);
      }
      return false;
    }

    /**
     * Indica si se debe o no procesar el número n. 
     * No es necesario procesar todos los números enteros. Los números
     * que contengan 0, 2, 4, 5, 6 u 8 como dígito no serán primos en al menos
     * alguna de sus rotaciones, debido a que dicho número será par, múltiplo
     * de 5 o múltiplo de 10.
     * @param n número que se desea verificar si se debe o no procesar
     * @return indica si se debe procesar el número n. <code>True</code> se debe
     * procesar, en otro caso <code>False</code>.
     */
    private boolean filter(int n) {
      if (n < 10) {
        return true;
      }
      int o = n;
      do {
        int d = o % 10;
        if (d == 1 || d == 3 || d == 7 || d == 9) {
        } else {
          return false;
        }
        o /= 10;
      } while (o > 0);
      return true;
    }

    /**
     * Realiza la rotación de un número
     * @param n número que se desea rotar
     * @return resultado de la rotación de <code>n<code>
     */
    private int orbit(int n) {
      int n_10 = n / 10;
      int orbit = n % 10;
      n = n_10;
      while (n != 0) {
        orbit *= 10;
        n /= 10;
      }
      orbit += n_10;
      return orbit;
    }

  }

}
