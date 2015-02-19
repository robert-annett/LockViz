package lockvis.model;

import java.awt.Color;
import java.awt.Shape;

/**
 * Interface so that I don't have to do switching between my (distinct) vertex types. A display adapter (for the GUI items) would be a better pattern but this is quick and simple.
 */
public interface Vertex extends ObjectProperties{
    String toFullString();
    Shape getShape();
    Color getColor();
    String getToolTip();
    boolean isInDeadLock();
    String getName();
    ThreadInfoSet getContainingEntanglement();
}