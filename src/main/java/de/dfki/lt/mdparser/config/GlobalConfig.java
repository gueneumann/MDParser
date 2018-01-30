package de.dfki.lt.mdparser.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton wrapper around {@link PropertiesConfiguration} with the global MDP configuration.
 * Configuration is loaded from "mdp.conf" in the classpath.
 *
 * @author Jörg Steffen, DFKI
 */
public final class GlobalConfig {

  public static final Path SPLIT_ALPHA_FOLDER =
      getModelBuildFolder().resolve("split_alphas"); // splitA

  public static final Path FEATURE_VECTORS_FOLDER =
      getModelBuildFolder().resolve("1_initial_feature_vectors"); //splitO
  public static final Path SPLIT_INITIAL_FOLDER =
      getModelBuildFolder().resolve("2_initial_splits"); //splitF
  public static final Path SPLIT_ADJUST_FOLDER =
      getModelBuildFolder().resolve("3_adjusted_splits"); //split
  public static final Path SPLIT_COMPACT_FOLDER =
      getModelBuildFolder().resolve("4_compacted_splits"); // splitC
  public static final Path SPLIT_MODELS_FOLDER =
      getModelBuildFolder().resolve("split_models"); // splitModels

  public static final Path SPLIT_FILE =
      getModelBuildFolder().resolve("split.txt"); // temp/split.txt
  public static final Path ALPHA_FILE =
      getModelBuildFolder().resolve("alpha.txt"); // temp/alphaParser.txt

  private static final Logger logger = LoggerFactory.getLogger(GlobalConfig.class);
  private static PropertiesConfiguration instance;


  private GlobalConfig() {

    // private constructor to enforce noninstantiability
  }


  public static PropertiesConfiguration getInstance() {

    if (null == instance) {
      Parameters params = new Parameters();
      FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
          new FileBasedConfigurationBuilder<PropertiesConfiguration>(PropertiesConfiguration.class)
              .configure(params.properties()
                  .setFileName("mdp.conf")
                  .setEncoding("UTF-8")
                  .setListDelimiterHandler(new DefaultListDelimiterHandler(',')));
      try {
        instance = builder.getConfiguration();
      } catch (ConfigurationException e) {
        logger.error(e.getLocalizedMessage(), e);
      }
    }

    return instance;
  }


  /**
   * Convenience method to retrieve the model build folder from config.
   *
   * @return model build folder
   */
  public static Path getModelBuildFolder() {

    return Paths.get(getInstance().getString(ConfigKeys.MODEL_BUILD_FOLDER));
  }


  /**
   * @param key
   *          the config key
   * @return path associated with the given key
   */
  public static Path getPath(String key) {

    return Paths.get(getInstance().getString(key));
  }


  /**
   * @param key
   *          the config key
   * @return path list associated with the given key
   */
  public static List<Path> getPathList(String key) {

    List<Path> pathList = new ArrayList<>();
    List<String> stringList = getInstance().getList(String.class, key);
    for (String oneString : stringList) {
      pathList.add(Paths.get(oneString));
    }
    return pathList;
  }


  /**
   * Convenience method to retrieve string values from config.
   *
   * @param key
   *          the config key
   * @return the key value
   */
  public static String getString(String key) {

    return getInstance().getString(key);
  }


  /**
   * Convenience method to retrieve int values from config.
   *
   * @param key
   *          the config key
   * @return the key value
   */
  public static int getInt(String key) {

    return getInstance().getInt(key);
  }


  /**
   * Convenience method to retrieve boolean values from config.
   *
   * @param key
   *          the config key
   * @return the key value
   */
  public static boolean getBoolean(String key) {

    return getInstance().getBoolean(key);
  }


  /**
   * Convenience method to retrieve double values from config.
   *
   * @param key
   *          the config key
   * @return the key value
   */
  public static double getDouble(String key) {

    return getInstance().getDouble(key);
  }
}
