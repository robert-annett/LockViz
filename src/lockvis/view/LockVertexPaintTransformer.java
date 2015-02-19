package lockvis.view;

import java.awt.Paint;

import lockvis.model.Vertex;
import edu.uci.ics.jung.visualization.decorators.PickableVertexPaintTransformer;
import edu.uci.ics.jung.visualization.picking.PickedInfo;

public class LockVertexPaintTransformer extends PickableVertexPaintTransformer<Vertex> {

    public LockVertexPaintTransformer(PickedInfo<Vertex> pi, Paint fillPaint, Paint pickedPaint) {
        super(pi, fillPaint, pickedPaint);
    }

    public Paint transform(Vertex v) {
        if (pi.isPicked(v))
            return picked_paint;

        return v.getColor();
    }
}
