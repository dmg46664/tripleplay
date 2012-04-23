package tripleplay.ui.layout;

import java.util.ArrayList;
import java.util.List;

import pythagoras.f.Dimension;
import pythagoras.f.IDimension;
import tripleplay.ui.Element;
import tripleplay.ui.Elements;
import tripleplay.ui.Layout;
import tripleplay.ui.Style;

/**
 * Lays out elements in horizontal rows, breaking rows when the width limit is reached.
 */
public class FlowLayout extends Layout
{
    /** The default gap between rows and elements in a row. */
    public static final float DEFAULT_GAP = 5;

    /**
     * Sets the gap, in pixels, to use between rows and between elements within a row.
     */
    public FlowLayout gaps (float gap) {
        _hgap = _vgap = gap;
        return this;
    }

    /**
     * Sets the gap, in pixels, to use between rows and between elements within a row.
     * @param hgap the gap to use between elements in a row
     * @param vgap the gap to use between rows
     */
    public FlowLayout gaps (float hgap, float vgap) {
        _hgap = hgap;
        _vgap = vgap;
        return this;
    }

    /**
     * Sets the alignment used for positioning elements within their row. By default, elements
     * are centered vertically: {@link Style.VAlign#CENTER}.
     */
    public FlowLayout align (Style.VAlign align)
    {
        _valign = align;
        return this;
    }

    @Override public Dimension computeSize (Elements<?> elems, float hintX, float hintY) {
        Metrics m = computeMetrics(elems, hintX, hintY);
        return m.size;
    }

    @Override public void layout (Elements<?> elems,
                                  float left, float top, float width, float height) {
        Style.HAlign halign = resolveStyle(elems, Style.HALIGN);
        Metrics m = computeMetrics(elems, width, height);
        float y = top + resolveStyle(elems, Style.VALIGN).offset(m.size.height, height);
        for (int elemIdx = 0, row = 0, size = m.rowBreaks.size(); row < size; ++row) {
            Dimension rowSize = m.rows.get(row);
            float x = left + halign.offset(rowSize.width, width);
            for (; elemIdx < m.rowBreaks.get(row).intValue(); ++elemIdx) {
                Element<?> elem = elems.childAt(elemIdx);
                IDimension esize = preferredSize(elem, width, height);
                setBounds(elem, x, y + _valign.offset(esize.height(), rowSize.height()),
                    esize.width(), esize.height());
                x += esize.width() + _hgap;
            }
            y += _vgap + rowSize.height;
        }
    }

    protected Metrics computeMetrics (Elements<?> elems, float width, float height) {
        Metrics m = new Metrics();

        // fill in components horizontally, breaking rows as needed
        Dimension rowSize = new Dimension();
        for (int ii = 0, ll = elems.childCount(); ii < ll; ++ii) {
            Element<?> elem = elems.childAt(ii);
            if (!elem.isVisible()) continue;
            IDimension esize = preferredSize(elem, width, height);
            if (rowSize.width > 0 && width > 0 && rowSize.width + _hgap + esize.width() > width) {
                m.addBreak(ii, rowSize);
                rowSize = new Dimension(esize);
            } else {
                rowSize.width += (rowSize.width > 0 ? _hgap : 0) + esize.width();
                rowSize.height = Math.max(esize.height(), rowSize.height);
            }
        }
        m.addBreak(elems.childCount(), rowSize);
        return m;
    }

    public class Metrics
    {
        public Dimension size = new Dimension();
        public List<Dimension> rows = new ArrayList<Dimension>();
        public List<Integer> rowBreaks = new ArrayList<Integer>();

        protected void addBreak (int idx, Dimension lastRowSize) {
            if (lastRowSize.height == 0 && lastRowSize.width == 0) return;
            rowBreaks.add(idx);
            rows.add(lastRowSize);
            size.height += (size.height > 0 ? _vgap : 0) + lastRowSize.height;
            size.width = Math.max(size.width, lastRowSize.width);
        }
    }

    protected float _hgap = DEFAULT_GAP, _vgap = DEFAULT_GAP;
    protected Style.VAlign _valign = Style.VAlign.CENTER;
    protected static final float LARGE_FLOAT = 1e10f;
}