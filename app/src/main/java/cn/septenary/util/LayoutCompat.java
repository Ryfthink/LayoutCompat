package cn.septenary.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.TextView;

public class LayoutCompat {

    // 是否按照宽比适配
    private static boolean sBaseScaleByW = true;

    private static Map<Point, LayoutCompat> sHolders = new HashMap<Point, LayoutCompat>();

    // 屏幕尺寸
    private static volatile Point sScreenSize;

    // 1080P 预设方案
    public static final LayoutCompat L1080P = obtain(1920, 1080);

    // 宽度参照
    private int mDesignWidth;

    // 高度参照
    private int mDesignHeight;

    public static void init(Context context) {
        Point size = new Point();
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Method method = Display.class.getMethod("getRealSize", Point.class);
            method.invoke(wm.getDefaultDisplay(), size);
        } catch (Exception e) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            size.x = metrics.widthPixels;
            size.y = metrics.heightPixels;
        }
        if (size.x < size.y) {
            int temp = size.x;
            size.x = size.y;
            size.y = temp;
        }
        // 屏幕比例大于 16：9 按高比例适配 ,否则按宽比适配
        sBaseScaleByW = (1f * size.x / size.y) <= (1f * 16 / 9);
        LayoutCompat.sScreenSize = size;
    }

    public static LayoutCompat obtain(int w, int h) {
        Point key = new Point(w, h);
        if (!sHolders.containsKey(key)) {
            synchronized (LayoutCompat.class) {
                if (!sHolders.containsKey(key)) {
                    sHolders.put(key, new LayoutCompat(w, h));
                }
            }
        }
        return sHolders.get(key);
    }

    private LayoutCompat(int w, int h) {
        mDesignWidth = w;
        mDesignHeight = h;
    }

    public float w(float px) {
        if (sScreenSize != null) {
            return px * wScale();
        }
        return px;
    }

    public float h(float px) {
        if (sScreenSize != null) {
            return px * hScale();
        }
        return px;
    }

    public float h(float px, boolean useRealyScale) {
        if (sScreenSize != null) {
            return px * hScale(useRealyScale);
        }
        return px;
    }

    public int w(int px) {
        if (sScreenSize != null) {
            return (int) (px * wScale());
        }
        return px;
    }

    public int h(int px) {
        if (sScreenSize != null) {
            return (int) (px * hScale());
        }
        return px;
    }

    /**
     *
     * @param px
     * @param useRealyScale true 按真实比例适配 ，false 只按base比例适配
     * @return
     */
    public int h(int px, boolean useRealyScale) {
        if (sScreenSize != null) {
            return (int) (px * hScale(useRealyScale));
        }
        return px;
    }

    public float getFontSize(float size) {
        return w(size);
    }

    public float screenW() {
        return sScreenSize.x;
    }

    public float screenH() {
        return sScreenSize.y;
    }

    public float wScale() {
        return baseScale();
    }

    public float hScale() {
        return hScale(false);
    }

    private float baseScale() {
        return sBaseScaleByW ? (1f * sScreenSize.x / mDesignWidth) : (1f * sScreenSize.y / mDesignHeight);
    }

    /**
     *
     * @param useRealyScale true 按真实比例适配 ，false 只按base比例适配
     * @return 适配比例
     */
    public float hScale(boolean useRealyScale) {
        return useRealyScale ? (1f * sScreenSize.y / mDesignHeight) : baseScale();
    }

    public void compact(View view) {
        compactView(view);
    }

    public void compatActivity(Activity activity) {
        compactView(activity.findViewById(android.R.id.content));
    }

    public void compatDialog(Dialog dialog) {
        compactView(dialog.findViewById(android.R.id.content));
    }

    public void compactView(View view) {
        compactViewImpl(view, false);
    }

    public void compactView(View view, boolean useReallyScale) {
        compactViewImpl(view, useReallyScale);
    }

    /**
     * @param view
     * @param useReallyScale true 按真实比例适配 ，false 只按base比例适配
     */
    private void compactViewImpl(View view, boolean useReallyScale) {
        if (view == null) {
            return;
        }
        if (sScreenSize == null || sScreenSize.x == 0 || sScreenSize.y == 0) {
            init(view.getContext());
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            // width
            if (params.width > 0) {
                params.width = w(params.width);
            }
            // height
            if (params.height > 0) {
                params.height = h(params.height, useReallyScale);
            }
            // margin
            if (params instanceof MarginLayoutParams) {
                MarginLayoutParams mParams = (MarginLayoutParams) params;
                mParams.leftMargin = w(mParams.leftMargin);
                mParams.topMargin = h(mParams.topMargin, useReallyScale);
                mParams.rightMargin = w(mParams.rightMargin);
                mParams.bottomMargin = h(mParams.bottomMargin, useReallyScale);
            }
        }

        // padding
        view.setPadding(w(view.getPaddingLeft()), h(view.getPaddingTop(), useReallyScale), w(view.getPaddingRight()), h(view.getPaddingBottom(), useReallyScale));

        // size
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, w((int) tv.getTextSize()));
            tv.setCompoundDrawablePadding(w(tv.getCompoundDrawablePadding()));
            Drawable[] cds = tv.getCompoundDrawables();
            for (Drawable d : cds) {
                Rect bounds = d == null ? null : d.getBounds();
                if (bounds != null && !bounds.isEmpty()) {
                    bounds.set(w(bounds.left), h(bounds.top, useReallyScale), w(bounds.right), h(bounds.bottom, useReallyScale));
                }
            }
            tv.setCompoundDrawables(cds[0], cds[1], cds[2], cds[3]);
        }

        // 遍历
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            final int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                compactViewImpl(vg.getChildAt(i), useReallyScale);
            }
        }
    }
}
