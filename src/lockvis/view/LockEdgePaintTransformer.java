package lockvis.view;

import java.awt.Color;
import java.awt.Paint;

import lockvis.model.MutexAction;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.visualization.picking.PickedInfo;

public class LockEdgePaintTransformer implements Transformer<MutexAction, Paint> {
    protected PickedInfo<MutexAction> pi;
    protected Paint draw_paint;
    protected Paint picked_paint;

    public LockEdgePaintTransformer(PickedInfo<MutexAction> pi, Paint draw_paint, Paint picked_paint) {
        if (pi == null)
            throw new IllegalArgumentException("PickedInfo instance must be non-null");
        this.pi = pi;
        this.draw_paint = draw_paint;
        this.picked_paint = picked_paint;
    }

    public Paint transform(MutexAction e) {
        if (pi.isPicked(e)) {
            return picked_paint;
        }

        if (e.getState().endsWith("waiting")) {
            return Color.RED;
        }
        if (e.getState().endsWith("locked")) {
            return Color.GREEN;
        }
        if (e.getState().endsWith("parking")) {
            return Color.ORANGE;
        }
        return draw_paint;
    }
}
