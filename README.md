# LayoutCompat 多分辨率适配工具


1.美工通常会按照某一特定分辨率进行UI设计，以 **1920x1080** 的标注图为例，在 **res/layout** 文件夹下创建UI视图，参照标注图的尺寸，所有UI视图都按照 **像素px** 为单位配置 （ TextView 的 textSize也不例外）。

2.写好了所有的 **layout.xml** 布局后，要介绍一下 `LayoutCompat` 这个工具类的使用。`LayoutCompat` 首先按照当前设备的分辨率 与 **1920x1080** 比较计算出一个缩放因子 `scaleFactor`，然后遍历根视图下所有子View，按照 `scaleFactor` 重新计算子View的 `LayoutParams`, `padding`, `margin`, `textSize` 等等，然后重新配置给子View。你只需要在 `Activity` 中调用 `setContent()` 后执行下面语句，就完成了所有设备的适配工作。

```java
LayoutCompat.init(this);
LayoutCompat.L1080P.compat(this);
```

其中静态常量 `L1080P` 是 `LayoutComat` 预设好的设计方案，如果是按照其他分辨率的标准设计，以 **1280x720** 为例，可以这样写：

    LayoutCompat.init(this);
    LayoutCompat.obtain(1280,720).compat(this);

3.介绍一下 `LayoutCompat` 工具类部分核心代码

计算缩放因子 
```java
private float baseScale() {
    return sBaseScaleByW ? (1f * sScreenSize.x / mDesignWidth) : (1f * sScreenSize.y / mDesignHeight);
}
```

适配Activity
```java
public void compatActivity(Activity activity) {
    // 找到Activity 的 rootView进行适配
    compatViewImpl(activity.findViewById(android.R.id.content));
}
```

适配View
```java
public void compatViewImpl(View view){
    if (view == null) {
         return;
    }
    if (sScreenSize == null || sScreenSize.x == 0 || sScreenSize.y == 0) {
        init(view.getContext());
    }
    ViewGroup.LayoutParams params = view.getLayoutParams();
    // 计算 width ，height ，margin
    if (params != null) {
        if (params.width > 0) {
            params.width = w(params.width);
        }
        if (params.height > 0) {
            params.height = h(params.height);
        }
        if (params instanceof MarginLayoutParams) {
            MarginLayoutParams mParams = (MarginLayoutParams) params;
            mParams.leftMargin = w(mParams.leftMargin);
            ...
        }
    }
    // 计算 padding
    view.setPadding(w(view.getPaddingLeft()), h(view.getPaddingTop()), w(view.getPaddingRight()), h(view.getPaddingBottom()));

    // 计算 TextView 的 size
    if (view instanceof TextView) {
        TextView tv = (TextView) view;
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, w((int) tv.getTextSize()));
        ...
    }
    // 遍历子View 并适配
    if (view instanceof ViewGroup) {
        ViewGroup vg = (ViewGroup) view;
        final int count = vg.getChildCount();
        for (int i = 0; i < count; i++) {
            compactViewImpl(vg.getChildAt(i));
        }
    }
}
```

4.优缺点
- 优点：轻量级适配，代码量少，方便快捷 
- 缺点：不能适配 res/drawable 下的资源
