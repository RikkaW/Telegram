package org.telegram.ui.Components;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Rikka on 2016/11/10.
 */

public class AutoMarqueeTextView extends TextView implements View.OnClickListener {

    private boolean mMarquee;

    public AutoMarqueeTextView(Context context) {
        super(context);

        setMarqueeRepeatLimit(-1);
        setMarquee(false);
        setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        setMarquee(!mMarquee);
    }

    public void setMarquee(boolean marquee) {
        mMarquee = marquee;

        if (mMarquee) {
            setSelected(true);
            setHorizontallyScrolling(true);
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
        } else {
            setSelected(false);
            setHorizontallyScrolling(false);
            setEllipsize(TextUtils.TruncateAt.END);
        }
    }
}
