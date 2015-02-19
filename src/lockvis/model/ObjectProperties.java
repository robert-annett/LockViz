package lockvis.model;

import java.util.Map;

/**
 * This interface exists so that GUI elements can grab a hashmap of properties to display in a GUI. You can argue that
 * this would be better implemented as a set of adapter producing the required information but this way results in a lot
 * less code...
 * @author rannett
 */
public interface ObjectProperties {
    Map<String, String> getProperties();
}
