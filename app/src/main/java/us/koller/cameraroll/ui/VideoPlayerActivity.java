package us.koller.cameraroll.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import us.koller.cameraroll.R;

public class VideoPlayerActivity extends ThemeableActivity {

    public static final String POSITION = "POSITION";
    public static final String PLAY_WHEN_READY = "PLAY_WHEN_READY";

    private SimpleExoPlayer player;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        Uri videoUri = intent.getData();

        if (videoUri == null) {
            return;
        }

        // 1. Create a default TrackSelector
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(null);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, getString(R.string.app_name)), null);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        MediaSource videoSource = new ExtractorMediaSource(videoUri,
                dataSourceFactory, extractorsFactory, null, null);

        // Loops the video indefinitely.
        LoopingMediaSource loopingSource = new LoopingMediaSource(videoSource);

        // 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        // 3. Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

        // Bind the player to the view.
        SimpleExoPlayerView simpleExoPlayerView
                = (SimpleExoPlayerView) findViewById(R.id.simpleExoPlayerView);
        simpleExoPlayerView.setPlayer(player);

        // Prepare the player with the source.
        player.prepare(loopingSource);

        if (savedInstanceState != null) {
            player.seekTo(savedInstanceState.getLong(POSITION));
            player.setPlayWhenReady(savedInstanceState.getBoolean(PLAY_WHEN_READY));
        } else {
            player.setPlayWhenReady(true);
        }

        //needed to achieve transparent navBar
        showOrHideSystemUi(true);

        //init Play pause button
        final ImageButton playPause = (ImageButton) findViewById(R.id.play_pause);
        playPause.setImageResource(R.drawable.pause_to_play_avd);
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.setPlayWhenReady(!player.getPlayWhenReady());

            }
        });

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(null);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setWindowInsets();

        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object o) {
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
            }

            @Override
            public void onLoadingChanged(boolean b) {
            }

            @Override
            public void onPlayerStateChanged(boolean b, int i) {
                //update PlayPause-Button
                if (player.getPlayWhenReady()) {
                    playPause.setImageResource(R.drawable.play_to_pause_avd);
                } else {
                    playPause.setImageResource(R.drawable.pause_to_play_avd);
                }

                Drawable d = playPause.getDrawable();
                if (d instanceof Animatable) {
                    ((Animatable) d).start();
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException e) {
            }

            @Override
            public void onPositionDiscontinuity() {
            }
        });

        //hide & show Nav-/StatusBar together with controls
        final PlaybackControlView playbackControlView = (PlaybackControlView)
                findViewById(R.id.playback_control_view).getParent();
        final View bottomBarControls = findViewById(R.id.controls);
        playbackControlView.setVisibilityListener(
                new PlaybackControlView.VisibilityListener() {
                    @Override
                    public void onVisibilityChange(final int i) {
                        //animate Toolbar & controls
                        if (i != View.VISIBLE) {
                            //make view visible again, so the Animation is visible
                            playbackControlView.setVisibility(View.VISIBLE);
                        }

                        float toolbar_translationY = i == View.VISIBLE ? 0
                                : -(toolbar.getHeight());
                        toolbar.animate()
                                .translationY(toolbar_translationY)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        playbackControlView.setVisibility(i);
                                    }
                                })
                                .start();

                        float controls_translationY = i == View.VISIBLE ? 0
                                : bottomBarControls.getHeight();
                        bottomBarControls.animate()
                                .translationY(controls_translationY)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .start();

                        //show/hide Nav-/StatusBar
                        showOrHideSystemUi(i == View.VISIBLE);
                    }
                });
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setWindowInsets();
    }

    public void setWindowInsets() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final View bottomBarControls = findViewById(R.id.controls);
        final ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {

                    toolbar.setPadding(insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(), 0);

                    bottomBarControls.setPadding(insets.getSystemWindowInsetLeft(),
                            0, insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom());

                    // clear this listener so insets aren't re-applied
                    rootView.setOnApplyWindowInsetsListener(null);
                    return insets.consumeSystemWindowInsets();
                }
            });
        } else {
            rootView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    //hacky way of getting window insets on pre-Lollipop
                                    int[] screenSize = us.koller.cameraroll.util.Util
                                            .getScreenSize(VideoPlayerActivity.this);

                                    int[] windowInsets = new int[]{
                                            Math.abs(screenSize[0] - rootView.getLeft()),
                                            Math.abs(screenSize[1] - rootView.getTop()),
                                            Math.abs(screenSize[2] - rootView.getRight()),
                                            Math.abs(screenSize[3] - rootView.getBottom())};

                                    toolbar.setPadding(windowInsets[0], windowInsets[1],
                                            windowInsets[2], 0);

                                    bottomBarControls.setPadding(windowInsets[0], 0,
                                            windowInsets[2], windowInsets[3]);

                                    rootView.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                }
                            });
        }
    }

    public void showOrHideSystemUi(boolean show) {
        if (show) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(POSITION, player.getCurrentPosition());
        outState.putBoolean(PLAY_WHEN_READY, player.getPlayWhenReady());
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (player != null) {
            player.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (player != null) {
            player.release();
        }
    }

    @Override
    public int getThemeRes(int style) {
        if (style == DARK) {
            return R.style.Theme_CameraRoll_VideoPlayer;
        } else {
            return R.style.Theme_CameraRoll_Light_VideoPlayer;
        }
    }

    @Override
    public void onThemeApplied(int theme) {
        if (theme == LIGHT) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

            int white = ContextCompat.getColor(this, R.color.white);

            Drawable d = toolbar.getNavigationIcon();
            DrawableCompat.wrap(d);
            DrawableCompat.setTint(d.mutate(), white);
            toolbar.setNavigationIcon(d);

            toolbar.setTitleTextColor(white);

            us.koller.cameraroll.util.Util.colorToolbarOverflowMenuIcon(toolbar, white);
        }
    }
}
