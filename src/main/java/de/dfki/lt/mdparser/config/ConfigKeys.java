package de.dfki.lt.mdparser.config;

/**
 * Defines all configuration keys as constants.
 *
 * @author Jörg Steffen, DFKI
 */
public final class ConfigKeys {

  public static final String MODEL_BUILD_FOLDER = "model.build.folder";
  public static final String MODEL_OUTPUT_FOLDER = "model.output.folder";
  public static final String TRAINING_THREADS = "training.threads";
  public static final String PARSING_THREADS = "parsing.threads";
  public static final String ALGORITHM = "algorithm";


  private ConfigKeys() {

    // private constructor to enforce noninstantiability
  }
}
