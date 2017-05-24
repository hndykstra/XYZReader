package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.SearchIterator;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int ARTICLE_DETAIL_LOADER_ID = 893;
    private static final String BUNDLE_ITEM_ID = "itemId";
    private static final String TAG = ArticleDetailActivity.class.getSimpleName();

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private DateFormat outputFormat = DateFormat.getDateInstance(DateFormat.SHORT);
    // Most time functions can only handle 1902 - 2037
    private Cursor mCursor;

    private CollapsingToolbarLayout collapsibleToolbar;
    private ImageView titleImage;
    private Toolbar toolbar;
    private TextView bylineView;
    private TextView bodyView;
    private FloatingActionButton shareFab;

    private long itemId;
    private boolean isPaused = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // this used a ViewPager that would have supported switching articles by swipe or something, but
        // there was none of that enabled in the code to navigate a cursor by swipe, so I replaced it
        // with a simple NestedScrollView
        /* Collapsing tool bar doesn't seem to like this.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        */
        setContentView(R.layout.activity_article_detail);

        collapsibleToolbar = (CollapsingToolbarLayout)findViewById(R.id.detail_collapsing_toolbar);
        titleImage = (ImageView)findViewById(R.id.detail_image);
        bylineView = (TextView)findViewById(R.id.article_byline);
        bodyView = (TextView)findViewById(R.id.article_body);
        shareFab = (FloatingActionButton)findViewById(R.id.share_fab);
        toolbar = (Toolbar)findViewById(R.id.detail_toolbar);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        shareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        if (savedInstanceState == null) {
            Log.d(TAG, "No saved instance state");
            this.itemId = 0L;
            if (getIntent() != null && getIntent().getData() != null) {
                itemId = ItemsContract.Items.getItemId(getIntent().getData());
                Log.d(TAG, "Item id " + itemId);
            }
        } else {
            Log.d(TAG, "Restore saved instance state");
            restoreInstanceState(savedInstanceState);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "Save instance state");
        outState.putLong(BUNDLE_ITEM_ID, itemId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Pause");
        isPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "Resume");
        isPaused = false;
        if (this.itemId != 0L) {
            Log.d(TAG, "Init loader");
            getSupportLoaderManager().initLoader(ARTICLE_DETAIL_LOADER_ID, null, this);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Stop");
    }

    private void restoreInstanceState(Bundle savedState) {
        this.itemId = savedState.getLong(BUNDLE_ITEM_ID, 0L);
        Log.d(TAG, "Restore item id " + itemId);
    }

    private void bindArticle() {
        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        Date publishedDate = parsePublishedDate(mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE));
        String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
        String body = mCursor.getString(ArticleLoader.Query.BODY);
        collapsibleToolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
        bylineView.setText(DisplayUtil.buildBylineHTML(publishedDate, outputFormat, author));
        bodyView.setText(body);
        ImageLoaderHelper.getInstance(this).getImageLoader()
                .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                    @Override
                    public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                        Bitmap bitmap = imageContainer.getBitmap();
                        if (bitmap != null) {
                            titleImage.setImageBitmap(imageContainer.getBitmap());
                        }
                    }

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
    }

    private Date parsePublishedDate(String dateString) {
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "Create loader " + id);
        return ArticleLoader.newInstanceForItemId(this, itemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG, "Load finished " + cursorLoader.getId());
        mCursor = cursor;

        // Select the start ID
        if (!isPaused) {
            mCursor.moveToFirst();
            bindArticle();
            cursorLoader.cancelLoad();
        }

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }
}
