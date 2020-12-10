package se.kth.jabeja;

import org.apache.log4j.Logger;
import se.kth.jabeja.config.Config;
import se.kth.jabeja.config.NodeSelectionPolicy;
import se.kth.jabeja.io.FileIO;
import se.kth.jabeja.rand.RandNoGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Jabeja {
  final static Logger logger = Logger.getLogger(Jabeja.class);
  private final Config config;
  private final HashMap<Integer/*id*/, Node/*neighbors*/> entireGraph;
  private final List<Integer> nodeIds;
  private int numberOfSwaps;
  private int round;
  private float T;
  private float T_min;
  private boolean simulatedAnnealing;
  private double factor;
  private boolean resultFileCreated = false;

  //-------------------------------------------------------------------
  public Jabeja(HashMap<Integer, Node> graph, Config config) {
    this.entireGraph = graph;
    this.nodeIds = new ArrayList(entireGraph.keySet());
    this.round = 0;
    this.numberOfSwaps = 0;
    this.config = config;
    this.T = config.getTemperature();
    this.T_min = 0.00001f;  // Not very sure about this
    this.factor = config.getFactor();  // Not very sure about this
    this.simulatedAnnealing = config.getSA();
    String infoStr;
    if (simulatedAnnealing)
      infoStr = "Simulated annealing with factor " + factor;
    else infoStr = "Standard JaBeJa with initial T " + T;
    logger.info(infoStr);
  }


  //-------------------------------------------------------------------
  public void startJabeja() throws IOException {
    for (round = 0; round < config.getRounds(); round++) {
      for (int id : entireGraph.keySet()) {
        sampleAndSwap(id);
      }

      //one cycle for all nodes have completed.
      //reduce the temperature
      saCoolDown();
      // Restart
      if (round%300 == 0) {
        T = config.getTemperature();
      }
      report(true, false);  // Save, do not print
    }
    report(false, true);  // Do not save, print
  }

  /**
   * Simulated analealing cooling function
   */
  private void saCoolDown(){
    T = Math.max(T*config.getDelta(), T_min);
  }

  /**
   * Sample and swap algorith at node p
   * @param nodeId
   */
  private void sampleAndSwap(int nodeId) {
    Node partner = null;
    Node nodep = entireGraph.get(nodeId);

    boolean doRandom = false;

    if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
            || config.getNodeSelectionPolicy() == NodeSelectionPolicy.LOCAL) {
      Integer[] sampledNeighbors = getNeighbors(nodep);
      Node candidatePartner = findPartner(nodeId, sampledNeighbors);
      if (candidatePartner == null) {
        doRandom = true;
      } else {
        partner = candidatePartner;
      }
    }

    if (doRandom || config.getNodeSelectionPolicy() == NodeSelectionPolicy.RANDOM)
      partner = findPartner(nodeId, getSample(nodeId));

    // swap the colors
    if (partner != null) {
      int temp_color = nodep.getColor();
      nodep.setColor(partner.getColor());
      partner.setColor(temp_color);
      numberOfSwaps ++;
    }
  }

  public Node findPartner(int nodeId, Integer[] nodes) {
    double alpha = config.getAlpha();

    Node nodep = entireGraph.get(nodeId);
    Node bestPartner = null;
    double highestBenefit = 0;

    // Iterate all nodes and get the one that maximizes
    // utility if swapped with nodep
    for (int idq: nodes) {
      Node nodeq = entireGraph.get(idq);
      if (nodeq.getColor() == nodep.getColor())
        continue;  // Do not check for nodes with same color
      double oldB = benefit(nodep, nodeq, alpha);
      double newB = benefitSwap(nodep, nodeq, alpha);
      boolean swap;
      if (simulatedAnnealing) {
        double ap = acceptanceProbability(oldB, newB);
        swap = (oldB != newB) && (Math.random() < ap);
      } else {
        swap = (newB * T > oldB) && (newB > highestBenefit);
      }
      if (swap) {
        bestPartner = nodeq;
        highestBenefit = newB;
      }
    }
    return bestPartner;
  }

  /**
   * Probability that the swap is accepted
   */
  private double acceptanceProbability(double oldCost, double newCost) {
    return factor*Math.exp((newCost - oldCost)/T);
  }

  /**
   * Compute the current benefit of two nodes. Benefit is defined as
   * U(p, q) = d_p(p)^alpha + d_q(q)^alpha (Eq. 5 of the paper)
   * where d_p(p) is the number of neighbors of p with its same color.
   */
  private double benefit(Node nodep, Node nodeq, double alpha) {
    int dpp = getDegree(nodep, nodep.getColor());
    int dqq = getDegree(nodeq, nodeq.getColor());
    return Math.pow(dpp, alpha) + Math.pow(dqq, alpha);
  }

  /**
   * Compute the benefit of the two nodes if their color was swapped.
   */
  private double benefitSwap(Node nodep, Node nodeq, double alpha) {
    int dpq = getDegree(nodep, nodeq.getColor());
    int dqp = getDegree(nodeq, nodep.getColor());
    return Math.pow(dpq, alpha) + Math.pow(dqp, alpha);
  }

  /**
   * The the degreee on the node based on color
   * @param node
   * @param colorId
   * @return how many neighbors of the node have color == colorId
   */
  private int getDegree(Node node, int colorId){
    int degree = 0;
    for(int neighborId : node.getNeighbours()){
      Node neighbor = entireGraph.get(neighborId);
      if(neighbor.getColor() == colorId){
        degree++;
      }
    }
    return degree;
  }

  /**
   * Returns a uniformly random sample of the graph
   * @param currentNodeId
   * @return Returns a uniformly random sample of the graph
   */
  private Integer[] getSample(int currentNodeId) {
    int count = config.getUniformRandomSampleSize();
    int rndId;
    int size = entireGraph.size();
    ArrayList<Integer> rndIds = new ArrayList<Integer>();

    while (true) {
      rndId = nodeIds.get(RandNoGenerator.nextInt(size));
      if (rndId != currentNodeId && !rndIds.contains(rndId)) {
        rndIds.add(rndId);
        count--;
      }

      if (count == 0)
        break;
    }

    Integer[] ids = new Integer[rndIds.size()];
    return rndIds.toArray(ids);
  }

  /**
   * Get random neighbors. The number of random neighbors is controlled using
   * -closeByNeighbors command line argument which can be obtained from the config
   * using {@link Config#getRandomNeighborSampleSize()}
   * @param node
   * @return
   */
  private Integer[] getNeighbors(Node node) {
    ArrayList<Integer> list = node.getNeighbours();
    int count = config.getRandomNeighborSampleSize();
    int rndId;
    int index;
    int size = list.size();
    ArrayList<Integer> rndIds = new ArrayList<Integer>();

    if (size <= count)
      rndIds.addAll(list);
    else {
      while (true) {
        index = RandNoGenerator.nextInt(size);
        rndId = list.get(index);
        if (!rndIds.contains(rndId)) {
          rndIds.add(rndId);
          count--;
        }

        if (count == 0)
          break;
      }
    }

    Integer[] arr = new Integer[rndIds.size()];
    return rndIds.toArray(arr);
  }


  /**
   * Generate a report which is stored in a file in the output dir.
   *
   * @throws IOException
   */
  private void report(boolean save, boolean print) throws IOException {
    int grayLinks = 0;
    int migrations = 0; // number of nodes that have changed the initial color
    int size = entireGraph.size();

    for (int i : entireGraph.keySet()) {
      Node node = entireGraph.get(i);
      int nodeColor = node.getColor();
      ArrayList<Integer> nodeNeighbours = node.getNeighbours();

      if (nodeColor != node.getInitColor()) {
        migrations++;
      }

      if (nodeNeighbours != null) {
        for (int n : nodeNeighbours) {
          Node p = entireGraph.get(n);
          int pColor = p.getColor();

          if (nodeColor != pColor)
            grayLinks++;
        }
      }
    }

    int edgeCut = grayLinks / 2;

    if (print) {
      logger.info("round: " + round +
              ", edge cut:" + edgeCut +
              ", swaps: " + numberOfSwaps +
              ", migrations: " + migrations + "\n");
    }
    if (save)
      saveToFile(edgeCut, migrations);
  }

  private void saveToFile(int edgeCuts, int migrations) throws IOException {
    String delimiter = "\t\t";
    String outputFilePath;

    //output file name
    File inputFile = new File(config.getGraphFilePath());
    outputFilePath = config.getOutputDir() +
            File.separator +
            inputFile.getName() + "_" +
            "NS" + "_" + config.getNodeSelectionPolicy() + "_" +
            "GICP" + "_" + config.getGraphInitialColorPolicy() + "_" +
            "T" + "_" + config.getTemperature() + "_" +
            "D" + "_" + config.getDelta() + "_" +
            "RNSS" + "_" + config.getRandomNeighborSampleSize() + "_" +
            "URSS" + "_" + config.getUniformRandomSampleSize() + "_" +
            "A" + "_" + config.getAlpha() + "_" +
            "R" + "_" + config.getRounds();
    if (simulatedAnnealing) {
      outputFilePath += "_SA_" + factor;
    } else {
      outputFilePath += "_jabeja";
    }
    outputFilePath += ".txt";

    if (!resultFileCreated) {
      File outputDir = new File(config.getOutputDir());
      if (!outputDir.exists()) {
        if (!outputDir.mkdir()) {
          throw new IOException("Unable to create the output directory");
        }
      }
      // create folder and result file with header
      String header = "# Migration is number of nodes that have changed color.";
      header += "\n\nRound" + delimiter + "Edge-Cut" + delimiter + "Swaps" + delimiter + "Migrations" + delimiter + "Skipped" + "\n";
      FileIO.write(header, outputFilePath);
      resultFileCreated = true;
    }

    FileIO.append(round + delimiter + (edgeCuts) + delimiter + numberOfSwaps + delimiter + migrations + "\n", outputFilePath);
  }
}
