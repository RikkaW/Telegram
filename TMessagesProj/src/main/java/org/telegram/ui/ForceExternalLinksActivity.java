package org.telegram.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Rikka on 2016/10/26.
 */

public class ForceExternalLinksActivity extends BaseFragment {

    private Adapter adapter;

    private SharedPreferences preferences;
    private List<String> urls;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ExternalLinks", R.string.ExternalLinks));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        fragmentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fragmentView.setBackgroundColor(ContextCompat.getColor(context, R.color.card_background));

        preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        urls = new ArrayList<>();
        urls.addAll(Arrays.asList(preferences.getString("force_external_urls", getDefaultUrlsString()).split(",")));

        adapter = new Adapter();

        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        int padding = (int) (context.getResources().getDimension(R.dimen.list_padding) + AndroidUtilities.dp(24 + 16));
        recyclerView.addItemDecoration(new DividerDecoration(context, LocaleController.isRTL ? 0 : padding, LocaleController.isRTL ? padding : 0));
        ((FrameLayout) fragmentView).addView(recyclerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private void showAddDialog() {
        FrameLayout frameLayout = new FrameLayout(getParentActivity());
        frameLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), 0);

        final EditText editText = new EditText(getParentActivity());
        editText.setMaxLines(1);
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        editText.post(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getParentActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        showDialog(new AlertDialog.Builder(getParentActivity())
                .setTitle(LocaleController.getString("ExternalLinksAddDomain", R.string.ExternalLinksAddDomain))
                .setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (!TextUtils.isEmpty(editText.getText().toString())) {
                            urls.add(editText.getText().toString().replace(",", "").toLowerCase());
                            adapter.notifyItemInserted(adapter.getItemCount() - 1);
                        }
                    }
                })
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .setView(frameLayout)
                .create());
    }

    public static String getDefaultUrlsString() {
        return "youtube.com" + "," +
                "youtu.be" + "," +
                "play.google.com" + "," +
                "bilibili.com";
    }

    public void save() {
        StringBuilder sb = new StringBuilder();
        for (String url : urls) {
            sb.append(url).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);

        preferences.edit().putString("force_external_urls", sb.toString()).apply();
    }

    private class Adapter extends RecyclerView.Adapter {

        @Override
        public int getItemViewType(int position) {
            return position == getItemCount() - 1 ? 1 : 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case 0:
                    return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_force_external_item_layout, parent, false));
                case 1:
                    return new AddViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_force_external_add_layout, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == 1) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showAddDialog();
                    }
                });

                ((AddViewHolder) holder).text.setText(LocaleController.getString("ExternalLinksAddDomain", R.string.ExternalLinksAddDomain));
            } else {
                ((ItemViewHolder) holder).text.setText(urls.get(position));
                ((ItemViewHolder) holder).delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position = holder.getAdapterPosition();
                        urls.remove(position);
                        notifyItemRemoved(position);

                        save();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return urls.size() + 1;
        }
    }

    private static class ItemViewHolder extends RecyclerView.ViewHolder {

        public TextView text;
        public ImageView delete;

        public ItemViewHolder(View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(android.R.id.text1);
            delete = (ImageView) itemView.findViewById(android.R.id.button1);
        }
    }

    private static class AddViewHolder extends RecyclerView.ViewHolder {

        public TextView text;

        public AddViewHolder(View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    private static class DividerDecoration extends RecyclerView.ItemDecoration {

        private Drawable drawable;

        private int left, right;

        public DividerDecoration(Context context, int extra_left, int extra_right) {
            drawable = context.getDrawable(R.drawable.divider);
            left = extra_left;
            right = extra_right;
        }

        public int getHeight() {
            return drawable.getIntrinsicHeight();
        }

        public boolean canDraw(RecyclerView parent, int position) {
            return position != parent.getAdapter().getItemCount() - 1;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft() + this.left;
            int right = parent.getWidth() - parent.getPaddingRight() - this.right;

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (!canDraw(parent, i)) {
                    continue;
                }

                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + getHeight();

                drawable.setBounds(left, top, right, bottom);
                drawable.draw(c);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (!canDraw(parent, position)) {
                return;
            }

            outRect.top = getHeight();
        }
    }
}
