/*****************************************************************************
 * VideoPlayerActivity.java
 *****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlc.gui.video;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.PictureInPictureParams;
import android.app.Presentation;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ViewStubCompat;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.util.Rational;
import android.view.Display;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.AndroidUtil;
import org.videolan.medialibrary.Medialibrary;
import org.videolan.medialibrary.Tools;
import org.videolan.medialibrary.media.MediaWrapper;
import org.videolan.vlc.BuildConfig;
import org.videolan.vlc.PlaybackService;
import org.videolan.vlc.R;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.gui.MainActivity;
import org.videolan.vlc.gui.PlaybackServiceActivity;
import org.videolan.vlc.gui.audio.PlaylistAdapter;
import org.videolan.vlc.gui.browser.FilePickerActivity;
import org.videolan.vlc.gui.browser.FilePickerFragment;
import org.videolan.vlc.gui.dialogs.AdvOptionsDialog;
import org.videolan.vlc.gui.dialogs.DictionaryDialog;
import org.videolan.vlc.gui.dialogs.SubtitleSelectorDialog;
import org.videolan.vlc.gui.helpers.OnRepeatListener;
import org.videolan.vlc.gui.helpers.SwipeDragItemTouchHelperCallback;
import org.videolan.vlc.gui.helpers.UiTools;
import org.videolan.vlc.gui.preferences.PreferencesActivity;
import org.videolan.vlc.gui.tv.audioplayer.AudioPlayerActivity;
import org.videolan.vlc.interfaces.IPlaybackSettingsController;
import org.videolan.vlc.media.MediaDatabase;
import org.videolan.vlc.media.MediaUtils;
import org.videolan.vlc.subs.Caption;
import org.videolan.vlc.subs.SubtitleParser;
import org.videolan.vlc.subs.TimedTextObject;
import org.videolan.vlc.util.AndroidDevices;
import org.videolan.vlc.util.FileUtils;
import org.videolan.vlc.util.Permissions;
import org.videolan.vlc.util.Strings;
import org.videolan.vlc.util.SubtitlesDownloader;
import org.videolan.vlc.util.VLCInstance;
import org.videolan.vlc.widget.StrokedRobotoTextView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;



public class VideoPlayerActivity extends AppCompatActivity implements IVLCVout.Callback, IVLCVout.OnNewVideoLayoutListener,
        IPlaybackSettingsController, PlaybackService.Client.Callback, PlaybackService.Callback,
        PlaylistAdapter.IPlayer, OnClickListener, View.OnLongClickListener, ScaleGestureDetector.OnScaleGestureListener,SubtitleParser.ISubtitleParserListener {

    public final static String TAG = "VLC/VideoPlayerActivity";

    // Internal intent identifier to distinguish between internal launch and
    // external intent.
    public final static String PLAY_FROM_VIDEOGRID = Strings.buildPkgString("gui.video.PLAY_FROM_VIDEOGRID");
    public final static String PLAY_FROM_SERVICE = Strings.buildPkgString("gui.video.PLAY_FROM_SERVICE");
    public final static String EXIT_PLAYER = Strings.buildPkgString("gui.video.EXIT_PLAYER");

    public final static String PLAY_EXTRA_ITEM_LOCATION = "item_location";
    public final static String PLAY_EXTRA_SUBTITLES_LOCATION = "subtitles_location";
    public final static String PLAY_EXTRA_ITEM_TITLE = "title";
    public final static String PLAY_EXTRA_FROM_START = "from_start";
    public final static String PLAY_EXTRA_START_TIME = "position";
    public final static String PLAY_EXTRA_OPENED_POSITION = "opened_position";
    public final static String PLAY_DISABLE_HARDWARE = "disable_hardware";

    public final static String ACTION_RESULT = Strings.buildPkgString("player.result");
    public final static String EXTRA_POSITION = "extra_position";
    public final static String EXTRA_DURATION = "extra_duration";
    public final static String EXTRA_URI = "extra_uri";
    public final static int RESULT_CONNECTION_FAILED = RESULT_FIRST_USER + 1;
    public final static int RESULT_PLAYBACK_ERROR = RESULT_FIRST_USER + 2;
    public final static int RESULT_VIDEO_TRACK_LOST = RESULT_FIRST_USER + 3;
    private static final float DEFAULT_FOV = 80f;
    public static final float MIN_FOV = 20f;
    public static final float MAX_FOV = 150f;

    private final PlaybackServiceActivity.Helper mHelper = new PlaybackServiceActivity.Helper(this, this);
    protected PlaybackService mService;
    private Medialibrary mMedialibrary;
    private SurfaceView mSurfaceView = null;
    private StrokedRobotoTextView mSubtitleView = null;
    private View mRootView;
    private FrameLayout mSurfaceFrame;
    protected MediaRouter mMediaRouter;
    private MediaRouter.SimpleCallback mMediaRouterCallback;
    private SecondaryDisplay mPresentation;
    private int mPresentationDisplayId = -1;
    private Uri mUri;
    private boolean mAskResume = true;
    private boolean mIsRtl;
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mDetector = null;

    private ImageView mPlaylistToggle;
    private RecyclerView mPlaylist;
    private PlaylistAdapter mPlaylistAdapter;
    private ImageView mPlaylistNext;
    private ImageView mPlaylistPrevious;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_SCREEN = 1;
    private static final int SURFACE_FILL = 2;
    private static final int SURFACE_16_9 = 3;
    private static final int SURFACE_4_3 = 4;
    private static final int SURFACE_ORIGINAL = 5;
    private int mCurrentSize;

    private SharedPreferences mSettings;
    /*
        0 => disable in both lock and unlock mode.
        1 => enable just in unlock mode
        2 => enable just in lock mode
        3 => enable in both lock mode and unlock mode
    */
    private int mTouchVolumeControl = 0;
    private int mTouchBrightnessControl = 0;
    private int mTouchSeekControl = 0;
    private int mDoubleTapControl = 0;
    private int mEdgeSeekControl = 0;

    /** Overlay */
    private ActionBar mActionBar;
    private ViewGroup mActionBarView;
    private View mOverlayProgress;
    private View mOverlayBackground;
    private View mOverlayButtons;
    private static final int OVERLAY_TIMEOUT = 4000;
    private static final int OVERLAY_INFINITE = -1;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int FADE_OUT_INFO = 3;
    private static final int START_PLAYBACK = 4;
    private static final int AUDIO_SERVICE_CONNECTION_FAILED = 5;
    private static final int RESET_BACK_LOCK = 6;
    private static final int CHECK_VIDEO_TRACKS = 7;
    private static final int LOADING_ANIMATION = 8;
    private static final int SHOW_INFO = 9;
    private static final int HIDE_INFO = 10;

    private static final int LOADING_ANIMATION_DELAY = 1000;

    private boolean mDragging;
    private boolean mShowing;
    private boolean mShowingDialog;
    private DelayState mPlaybackSetting = DelayState.OFF;
    private SeekBar mSeekbar;
    private TextView mTitle;
    private TextView mSysTime;
    private TextView mBattery;
    private TextView mTime;
    private TextView mLength;
    private TextView mInfo;
    private LinearLayout mEditDelayLayout;
    private EditText mEditDelayText;
    private View mOverlayInfo;
    private ImageView mOverlayInfoIcon;
    private boolean mIsLoading;
    private boolean mIsPlaying = false;
    private ImageView mLoading;
    private ImageView mTipsBackground;
    private ImageView mPlayPause;
    private ImageView mTracks;
    private ImageView mNavMenu;
    private ImageView mRewind;
    private ImageView mForward;
    private ImageView mAdvOptions;
    private ImageView mPlaybackSettingPlus;
    private ImageView mPlaybackSettingMinus;
    private ImageView mPlaybackSettingSyncDone;
    private ImageView mCaptionSettingsNext;
    private ImageView mCaptionSettingsPrev;
    private ImageView mCaptionSettingsSync;
    private View mObjectFocused;
    protected boolean mEnableCloneMode;
    private boolean mDisplayRemainingTime;
    private int mScreenOrientation;
    private int mScreenOrientationLock;
    private int mCurrentScreenOrientation;
    private ImageView mLock;
    private ImageView mUnlock;
    private ImageView mSize;
    private String KEY_REMAINING_TIME_DISPLAY = "remaining_time_display";
    private String KEY_BLUETOOTH_DELAY = "key_bluetooth_delay";
    private long mSubtitleDelay = 0L;
    private long mAudioDelay = 0L;
    private boolean mRateHasChanged = false;
    private int mCurrentAudioTrack = -2;

    private boolean mIsLocked = false;
    /* -1 is a valid track (Disable) */
    private int mLastAudioTrack = -2;
    private int mOverlayTimeout = 0;
    private boolean mLockBackButton = false;
    boolean mWasPaused = false;
    private long mSavedTime = -1;
    private float mSavedRate = 1.f;

    /**
     * For uninterrupted switching between audio and video mode
     */
    private boolean mSwitchingView;
    private boolean mSwitchToPopup;
    private boolean mHasSubItems = false;

    // size of the video
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;

    //Volume
    private AudioManager mAudioManager;
    private int mAudioMax;
    private boolean audioBoostEnabled;
    private boolean mMute = false;
    private int mVolSave;
    private float mVol;
    private float mOriginalVol;
    private Toast warningToast;

    //Touch Events
    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_VOLUME = 1;
    private static final int TOUCH_BRIGHTNESS = 2;
    private static final int TOUCH_MOVE = 3;
    private static final int TOUCH_SEEK = 4;
    private int mTouchAction = TOUCH_NONE;
    private int mSurfaceYDisplayRange, mSurfaceXDisplayRange;
    private float mFov;
    private float mInitTouchY, mTouchY =-1f, mTouchX=-1f;

    //stick event
    private static final int JOYSTICK_INPUT_DELAY = 300;
    private long mLastMove;

    // Brightness
    private boolean mIsFirstBrightnessGesture = true;
    private float mRestoreAutoBrightness = -1f;

    // Tracks & Subtitles
    private MediaPlayer.TrackDescription[] mAudioTracksList;

    private volatile ArrayList<String> mSubtitleFiles = new ArrayList<>();
    private volatile Map<String,Long> mSubtitleFileMapDelay = new HashMap<String,Long>();
    private TimedTextObject mSubs;
    private String mCurrentSubtitlePath = null;
    private Caption mLastCaption = null;
    private Caption mSyncCaption = null;
    private int mLastSubIndex = 0;

    //subtitles styles
    private int mSubtitleColor;

    /**
     * Flag to indicate whether the media should be paused once loaded
     * (e.g. lock screen, or to restore the pause state)
     */
    private boolean mPlaybackStarted = false;

    // Tips
    private View mOverlayTips;
    private static final String PREF_TIPS_SHOWN = "video_player_tips_shown";

    // Navigation handling (DVD, Blu-Ray...)
    private int mMenuIdx = -1;
    private boolean mIsNavMenu = false;

    /* for getTime and seek */
    private long mForcedTime = -1;
    private long mLastTime = -1;

    private OnLayoutChangeListener mOnLayoutChangeListener;
    private AlertDialog mAlertDialog;

    /*detect was medaiaplayer playing before opening dialog*/
    private boolean mWasPlaying;

    private boolean enableCaptionControls = true;
    private boolean enableTouchSub = true;

    DisplayMetrics mScreen = new DisplayMetrics();

    protected boolean mIsBenchmark = false;

    private static LibVLC LibVLC() {
        return VLCInstance.get();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!VLCInstance.testCompatibleCPU(this)) {
            exit(RESULT_CANCELED);
            return;
        }

        if (AndroidUtil.isJellyBeanMR1OrLater) {
            // Get the media router service (Miracast)
            mMediaRouter = (MediaRouter) getApplicationContext().getSystemService(Context.MEDIA_ROUTER_SERVICE);
            Log.d(TAG, "MediaRouter information : " + mMediaRouter  .toString());
        }

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        if (!VLCApplication.showTvUi()) {

            mTouchVolumeControl = (mSettings.getBoolean("enable_volume_gesture", true) ? 1 : 0)
                    + (mSettings.getBoolean("enable_volume_gesture_lock", false) ? 2 : 0);

            mTouchBrightnessControl = (mSettings.getBoolean("enable_brightness_gesture", true) ? 1 : 0)
                    + (mSettings.getBoolean("enable_brightness_gesture_lock", false) ? 2 : 0);

            mTouchSeekControl = (mSettings.getBoolean("enable_seek_gesture", true) ? 1 : 0)
                    + (mSettings.getBoolean("enable_seek_gesture_lock", false) ? 2 : 0);

            mDoubleTapControl = (mSettings.getBoolean("enable_doubletap_gesture", true) ? 1 : 0)
                    + (mSettings.getBoolean("enable_doubletap_gesture_lock", true) ? 2 : 0);

            mEdgeSeekControl = (mSettings.getBoolean("enable_edge_seek", true) ? 1 : 0)
                    + (mSettings.getBoolean("enable_edge_seek_lock", true) ? 2 : 0);

        }

        /* Services and miscellaneous */
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
        mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioBoostEnabled = mSettings.getBoolean("audio_boost", true);

        mEnableCloneMode = mSettings.getBoolean("enable_clone_mode", false);
        createPresentation();
        setContentView(mPresentation == null ? R.layout.player : R.layout.player_remote_control);

        /** initialize Views an their Events */
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setBackgroundDrawable(null);
        mActionBar.setDisplayShowCustomEnabled(true);
        /** add back button to action bar */
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setCustomView(R.layout.player_action_bar);

        mRootView = findViewById(R.id.player_root);
        mActionBarView = (ViewGroup) mActionBar.getCustomView();

        mTitle = (TextView) mActionBarView.findViewById(R.id.player_overlay_title);
        if (!AndroidUtil.isJellyBeanOrLater) {
            mSysTime = (TextView) findViewById(R.id.player_overlay_systime);
            mBattery = (TextView) findViewById(R.id.player_overlay_battery);
        }

        mPlaylistToggle = (ImageView) findViewById(R.id.playlist_toggle);
        mPlaylist = (RecyclerView) findViewById(R.id.video_playlist);


        mUnlock = (ImageView) findViewById(R.id.unlock_overlay_button);
        mUnlock.setOnClickListener(this);



        mScreenOrientation = Integer.valueOf(
                mSettings.getString("screen_orientation", "99" /*SCREEN ORIENTATION SENSOR*/));

        enableCaptionControls =  mSettings.getBoolean("enable_caption_controls",true);
        enableTouchSub =  mSettings.getBoolean("enable_touch_sub",true);

        mSurfaceView = (SurfaceView) findViewById(R.id.player_surface);
        mSubtitleView = (StrokedRobotoTextView) findViewById(R.id.subtitles_surface);

        mSurfaceFrame = (FrameLayout) findViewById(R.id.player_surface_frame);

        /* Loading view */
        mLoading = (ImageView) findViewById(R.id.player_overlay_loading);
        if (mPresentation != null)
            mTipsBackground = (ImageView) findViewById(R.id.player_remote_tips_background);
        dimStatusBar(true);
        mHandler.sendEmptyMessageDelayed(LOADING_ANIMATION, LOADING_ANIMATION_DELAY);

        mSwitchingView = false;

        mAskResume = mSettings.getBoolean("dialog_confirm_resume", true);
        mDisplayRemainingTime = mSettings.getBoolean(KEY_REMAINING_TIME_DISPLAY, false);
        // Clear the resume time, since it is only used for resumes in external
        // videos.
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putLong(PreferencesActivity.VIDEO_RESUME_TIME, -1);

        // Paused flag - per session too, like the subs list.
        editor.remove(PreferencesActivity.VIDEO_PAUSED);
        editor.apply();

        IntentFilter filter = new IntentFilter();
        if (mBattery != null)
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(VLCApplication.SLEEP_INTENT);
        registerReceiver(mReceiver, filter);

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // 100 is the value for screen_orientation_start_lock
        setRequestedOrientation(getScreenOrientation(mScreenOrientation));
        // Extra initialization when no secondary display is detected
        if (mPresentation == null) {
            // Orientation
            // Tips
            if (!BuildConfig.DEBUG && !VLCApplication.showTvUi() && !mSettings.getBoolean(PREF_TIPS_SHOWN, false)) {
                ((ViewStubCompat) findViewById(R.id.player_overlay_tips)).inflate();
                mOverlayTips = findViewById(R.id.overlay_tips_layout);
            }

            //Set margins for TV overscan
            if (VLCApplication.showTvUi()) {
                int hm = getResources().getDimensionPixelSize(R.dimen.tv_overscan_horizontal);
                int vm = getResources().getDimensionPixelSize(R.dimen.tv_overscan_vertical);

                RelativeLayout uiContainer = (RelativeLayout) findViewById(R.id.player_ui_container);
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) uiContainer.getLayoutParams();
                lp.setMargins(hm, 0, hm, vm);
                uiContainer.setLayoutParams(lp);

                LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams) mTitle.getLayoutParams();
                titleParams.setMargins(0, vm, 0, 0);
                mTitle.setLayoutParams(titleParams);
            }
        }

        getWindowManager().getDefaultDisplay().getMetrics(mScreen);
        mSurfaceYDisplayRange = Math.min(mScreen.widthPixels, mScreen.heightPixels);
        mSurfaceXDisplayRange = Math.max(mScreen.widthPixels, mScreen.heightPixels);
        mCurrentScreenOrientation = getResources().getConfiguration().orientation;
        if (mIsBenchmark) {
            mCurrentSize = SURFACE_FIT_SCREEN;
        } else {
            mCurrentSize = mSettings.getInt(PreferencesActivity.VIDEO_RATIO, SURFACE_BEST_FIT);
        }
        mMedialibrary = VLCApplication.getMLInstance();
        mIsRtl = AndroidUtil.isJellyBeanMR1OrLater && TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;

        //config subtitle view
        mSubtitleColor = Color.parseColor(mSettings.getString("subtitles_color","#ffffff"));

        mSubtitleView.setTextColor(mSubtitleColor);
        mSubtitleView.setTextSize(Integer.parseInt(mSettings.getString("subtitles_size","30")));
        mSubtitleView.setStrokeColor(Color.parseColor(mSettings.getString("subtitles_outline_color","#000000")));
        mSubtitleView.setStrokeWidth(TypedValue.COMPLEX_UNIT_DIP,Integer.parseInt(mSettings.getString("subtitles_outline_width","3")));
        mSubtitleView.setMovementMethod(LinkMovementMethod.getInstance());

        //set layout_marginBottom
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)mSubtitleView.getLayoutParams();
        layoutParams.bottomMargin = Integer.parseInt(mSettings.getString("subtitles_bottom_margins","25"));
        mSubtitleView.setLayoutParams(layoutParams);
        boolean background = mSettings.getBoolean("subtitles_background",false);
        if(background)
            mSubtitleView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.grey900transparent));
    }
    @Override
    public boolean onSupportNavigateUp(){
        onBackPressed();
        return true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        mShowingDialog = false;
        /*
         * Set listeners here to avoid NPE when activity is closing
         */
        setHudClickListeners(true);

        if (mIsLocked && mScreenOrientation == 99)
            setRequestedOrientation(mScreenOrientationLock);
    }

    private void setHudClickListeners(boolean enabled) {
        if (mSeekbar != null)
            mSeekbar.setOnSeekBarChangeListener(enabled ? mSeekListener : null);
        if (mLock != null)
            mLock.setOnClickListener(enabled ? this : null);
        if (mPlayPause != null)
            mPlayPause.setOnClickListener(enabled ? this : null);
        if (mPlayPause != null)
            mPlayPause.setOnLongClickListener(enabled ? this : null);
        if (mLength != null)
            mLength.setOnClickListener(enabled ? this : null);
        if (mTime != null)
            mTime.setOnClickListener(enabled ? this : null);
        if (mSize != null)
            mSize.setOnClickListener(enabled ? this : null);
        if (mNavMenu != null)
            mNavMenu.setOnClickListener(enabled ? this : null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Permissions.PERMISSION_STORAGE_TAG: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadMedia();
                } else {
                    Permissions.showStoragePermissionDialog(this, true);
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (mPlaybackStarted && mService.getCurrentMediaWrapper() != null) {
            Uri uri = intent.hasExtra(PLAY_EXTRA_ITEM_LOCATION) ?
                    (Uri) intent.getExtras().getParcelable(PLAY_EXTRA_ITEM_LOCATION) : intent.getData();
            if (uri == null || uri.equals(mUri))
                return;
            if (TextUtils.equals("file", uri.getScheme()) && uri.getPath().startsWith("/sdcard")) {
                Uri convertedUri = FileUtils.convertLocalUri(uri);
                if (convertedUri == null || convertedUri.equals(mUri))
                    return;
                else
                    uri = convertedUri;
            }
            mUri = uri;
            mTitle.setText(mService.getCurrentMediaWrapper().getTitle());
            if (mPlaylist.getVisibility() == View.VISIBLE) {
                mPlaylistAdapter.setCurrentIndex(mService.getCurrentMediaPosition());
                mPlaylist.setVisibility(View.GONE);
            }
            showTitle();
            initUI();
            setPlaybackParameters();
            mForcedTime = mLastTime = -1;
            setOverlayProgress();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();
        hideOverlay(true);
        setHudClickListeners(false);
        /* Stop the earliest possible to avoid vout error */

        if (!isInPictureInPictureMode()) {
            if (isFinishing() ||
                    (AndroidUtil.isNougatOrLater && !AndroidUtil.isOOrLater //Video on background on Nougat Android TVs
                            && AndroidDevices.isAndroidTv && !requestVisibleBehind(true)))
                stopPlayback();
            else if (!isFinishing() && !mShowingDialog && "2".equals(mSettings.getString(PreferencesActivity.KEY_VIDEO_APP_SWITCH, "0")) && isInteractive()) {
                switchToPopup();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void switchToPopup() {
        if (AndroidDevices.hasPiP) {
            if (AndroidUtil.isOOrLater)
                enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(mVideoWidth, mVideoHeight)).build());
            else
                //noinspection deprecation
                enterPictureInPictureMode();
        } else {
            if (Permissions.canDrawOverlays(this)) {
                mSwitchingView = true;
                mSwitchToPopup = true;
                if (mService != null && !mService.isPlaying())
                    mService.getCurrentMediaWrapper().addFlags(MediaWrapper.MEDIA_PAUSED);
                cleanUI();
                exitOK();
            } else
                Permissions.checkDrawOverlaysPermission(this);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private boolean isInteractive() {
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        return AndroidUtil.isLolliPopOrLater ? pm.isInteractive() : pm.isScreenOn();
    }

    @Override
    public void onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled();
        stopPlayback();
        exitOK();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (!AndroidUtil.isHoneycombOrLater)
            changeSurfaceLayout();
        super.onConfigurationChanged(newConfig);
        getWindowManager().getDefaultDisplay().getMetrics(mScreen);
        mCurrentScreenOrientation = newConfig.orientation;
        mSurfaceYDisplayRange = Math.min(mScreen.widthPixels, mScreen.heightPixels);
        mSurfaceXDisplayRange = Math.max(mScreen.widthPixels, mScreen.heightPixels);
        resetHudLayout();
    }

    public void resetHudLayout() {
        if (mOverlayButtons == null)
            return;
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)mOverlayButtons.getLayoutParams();
        int orientation = getScreenOrientation(100);
        boolean portrait = orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        if (portrait) {
            layoutParams.addRule(RelativeLayout.BELOW, R.id.player_overlay_length);
            layoutParams.addRule(RelativeLayout.RIGHT_OF, 0);
            layoutParams.addRule(RelativeLayout.LEFT_OF, 0);
        } else {
            layoutParams.addRule(RelativeLayout.BELOW, R.id.player_overlay_seekbar);
            layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.player_overlay_time);
            layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.player_overlay_length);
        }
        mOverlayButtons.setLayoutParams(layoutParams);
    }


    @Override
    protected void onStart() {
        super.onStart();
        mMedialibrary.pauseBackgroundOperations();
        mHelper.onStart();
        if (mSettings.getBoolean("save_brightness", false)) {
            float brightness = mSettings.getFloat("brightness_value", -1f);
            if (brightness != -1f)
                setWindowBrightness(brightness);
        }
        IntentFilter filter = new IntentFilter(PLAY_FROM_SERVICE);
        filter.addAction(EXIT_PLAYER);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mServiceReceiver, filter);
        if (mBtReceiver != null) {
            IntentFilter btFilter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            btFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            registerReceiver(mBtReceiver, btFilter);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onStop() {
        super.onStop();
        mMedialibrary.resumeBackgroundOperations();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceReceiver);

        if (mBtReceiver != null)
            unregisterReceiver(mBtReceiver);
        if (mAlertDialog != null && mAlertDialog.isShowing())
            mAlertDialog.dismiss();
        if (!isFinishing() && mService != null && mService.isPlaying() &&
                "1".equals(mSettings.getString(PreferencesActivity.KEY_VIDEO_APP_SWITCH, "0"))) {
            switchToAudioMode(false);
        }

        cleanUI();
        stopPlayback();

        SharedPreferences.Editor editor = mSettings.edit();
        if (mSavedTime != -1)
            editor.putLong(PreferencesActivity.VIDEO_RESUME_TIME, mSavedTime);

        editor.putFloat(PreferencesActivity.VIDEO_RATE, mSavedRate);

        editor.apply();

        restoreBrightness();

        if (mSubtitlesGetTask != null)
            mSubtitlesGetTask.cancel(true);

        if (mService != null)
            mService.removeCallback(this);
        mHelper.onStop();
    }

    private void restoreBrightness() {
        if (mRestoreAutoBrightness != -1f) {
            int brightness = (int) (mRestoreAutoBrightness*255f);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightness);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }
        // Save brightness if user wants to
        if (mSettings.getBoolean("save_brightness", false)) {
            float brightness = getWindow().getAttributes().screenBrightness;
            if (brightness != -1f) {
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putFloat("brightness_value", brightness);
                editor.apply();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null)
            unregisterReceiver(mReceiver);

        // Dismiss the presentation when the activity is not visible.
        if (mPresentation != null) {
            Log.i(TAG, "Dismissing presentation because the activity is no longer visible.");
            mPresentation.dismiss();
            mPresentation = null;
        }

        mAudioManager = null;
    }

    /**
     * Add or remove MediaRouter callbacks. This is provided for version targeting.
     *
     * @param add true to add, false to remove
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void mediaRouterAddCallback(boolean add) {
        if (!AndroidUtil.isJellyBeanMR1OrLater || mMediaRouter == null
                || add == (mMediaRouterCallback != null))
            return;

        if(add) {
            mMediaRouterCallback = new MediaRouter.SimpleCallback() {
                @Override
                public void onRoutePresentationDisplayChanged(
                        MediaRouter router, MediaRouter.RouteInfo info) {
                    Log.d(TAG, "onRoutePresentationDisplayChanged: info=" + info);
                    final Display presentationDisplay = info.getPresentationDisplay();
                    final int newDisplayId = presentationDisplay != null ? presentationDisplay.getDisplayId() : -1;
                    if (newDisplayId != mPresentationDisplayId)
                        removePresentation();
                }
            };
            mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouterCallback);
        }
        else {
            mMediaRouter.removeCallback(mMediaRouterCallback);
            mMediaRouterCallback = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void surfaceFrameAddLayoutListener(boolean add) {
        if (!AndroidUtil.isHoneycombOrLater || mSurfaceFrame == null
                || add == (mOnLayoutChangeListener != null))
            return;

        if (add) {
            mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
                private final Runnable mRunnable = new Runnable() {
                    @Override
                    public void run() {
                        changeSurfaceLayout();
                    }
                };
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                        /* changeSurfaceLayout need to be called after the layout changed */
                        mHandler.removeCallbacks(mRunnable);
                        mHandler.post(mRunnable);
                    }
                }
            };
            mSurfaceFrame.addOnLayoutChangeListener(mOnLayoutChangeListener);
            changeSurfaceLayout();
        }
        else {
            mSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
            mOnLayoutChangeListener = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void startPlayback() {
        /* start playback only when audio service and both surfaces are ready */
        if (mPlaybackStarted || mService == null)
            return;

        mSavedRate = 1.0f;
        mSavedTime = -1;
        mPlaybackStarted = true;

        final IVLCVout vlcVout = mService.getVLCVout();
        if (vlcVout.areViewsAttached()) {
            if (mService.isPlayingPopup())
                mService.stop();
            vlcVout.detachViews();
        }
        if (mPresentation == null) {
            vlcVout.setVideoView(mSurfaceView);
//            vlcVout.setSubtitlesView(mSubtitleView);
        } else {
            vlcVout.setVideoView(mPresentation.mSurfaceView);
//            vlcVout.setSubtitlesView(mPresentation.mSubtitleView);
        }
        vlcVout.addCallback(this);
        vlcVout.attachViews(this);
        mService.setVideoTrackEnabled(true);

        initUI();

        loadMedia();
        boolean ratePref = mSettings.getBoolean(PreferencesActivity.KEY_AUDIO_PLAYBACK_SPEED_PERSIST, true);
        mService.setRate(ratePref || mRateHasChanged ? mSettings.getFloat(PreferencesActivity.VIDEO_RATE, 1.0f) : 1.0F, false);
    }

    private void initPlaylistUi() {
        if (mService.hasPlaylist()) {
            mPlaylistPrevious = (ImageView) findViewById(R.id.playlist_previous);
            mPlaylistNext = (ImageView) findViewById(R.id.playlist_next);
            mPlaylistAdapter = new PlaylistAdapter(this);
            mPlaylistAdapter.setService(mService);
            final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mPlaylist.setLayoutManager(layoutManager);
            mPlaylistToggle.setVisibility(View.VISIBLE);
            mPlaylistPrevious.setVisibility(View.VISIBLE);
            mPlaylistNext.setVisibility(View.VISIBLE);
            mPlaylistToggle.setOnClickListener(VideoPlayerActivity.this);
            mPlaylistPrevious.setOnClickListener(VideoPlayerActivity.this);
            mPlaylistNext.setOnClickListener(VideoPlayerActivity.this);

            ItemTouchHelper.Callback callback =  new SwipeDragItemTouchHelperCallback(mPlaylistAdapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(mPlaylist);
            if (mIsRtl) {
                mPlaylistPrevious.setImageResource(R.drawable.ic_playlist_next_circle);
                mPlaylistNext.setImageResource(R.drawable.ic_playlist_previous_circle);
            }
        }
    }

    private void initUI() {

        cleanUI();

        /* Dispatch ActionBar touch events to the Activity */
        mActionBarView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                onTouchEvent(event);
                return true;
            }
        });

        surfaceFrameAddLayoutListener(true);

        /* Listen for changes to media routes. */
        mediaRouterAddCallback(true);

        if (mRootView != null)
            mRootView.setKeepScreenOn(true);
    }

    private void setPlaybackParameters() {
        if (mAudioDelay != 0L && mAudioDelay != mService.getAudioDelay())
            mService.setAudioDelay(mAudioDelay);
        else if (mBtReceiver != null && (mAudioManager.isBluetoothA2dpOn() || mAudioManager.isBluetoothScoOn()))
            toggleBtDelay(true);
        if (mCurrentAudioTrack != -2)
            mService.setAudioTrack(mCurrentAudioTrack);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void stopPlayback() {
        if (!mPlaybackStarted)
            return;

        mWasPaused = !mService.isPlaying();
        if (!isFinishing()) {
            mCurrentAudioTrack = mService.getAudioTrack();
        }

        mService.saveMediaMeta();

        if (mMute)
            mute(false);

        mPlaybackStarted = false;

        mService.setVideoTrackEnabled(false);
        mService.removeCallback(this);

        mHandler.removeCallbacksAndMessages(null);
        final IVLCVout vlcVout = mService.getVLCVout();
        vlcVout.removeCallback(this);
        vlcVout.detachViews();

        if (mService.hasMedia() && mSwitchingView && mService != null) {
            Log.d(TAG, "mLocation = \"" + mUri + "\"");
            if (mSwitchToPopup)
                mService.switchToPopup(mService.getCurrentMediaPosition());
            else {
                mService.getCurrentMediaWrapper().addFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
                mService.showWithoutParse(mService.getCurrentMediaPosition());
            }
            return;
        }

        if (mService.isSeekable()) {
            mSavedTime = getTime();
            long length = mService.getLength();
            //remove saved position if in the last 5 seconds
            if (length - mSavedTime < 5000)
                mSavedTime = 0;
            else
                mSavedTime -= 2000; // go back 2 seconds, to compensate loading time
        }

        mSavedRate = mService.getRate();
        mRateHasChanged = mSavedRate != 1.0f;

        mService.setRate(1.0f, false);
        mService.stop();

        //clear handlerThread
        mProgressSubtitleCaptionThread = null;
    }

    private void cleanUI() {

        if (mRootView != null)
            mRootView.setKeepScreenOn(false);

        if (mDetector != null) {
            mDetector.setOnDoubleTapListener(null);
            mDetector = null;
        }

        /* Stop listening for changes to media routes. */
        mediaRouterAddCallback(false);

        surfaceFrameAddLayoutListener(false);

        if (AndroidUtil.isICSOrLater)
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);

        mActionBarView.setOnTouchListener(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if(mWasPlaying)
            play();

        if(data == null){
            Log.d(TAG, "Subtitle selection dialog was cancelled");
            return;
        }

        if(data.hasExtra(FilePickerFragment.EXTRA_MRL)) {
            final Uri subLocation = Uri.parse(data.getStringExtra(FilePickerFragment.EXTRA_MRL));
            addAndSaveSubtitle(subLocation,false);
            if(mCurrentSubtitlePath != null) {
                if (!mCurrentSubtitlePath.contains(subLocation.getPath()))
                    showConfirmReplaceSubtitleDialog(subLocation.getPath());
                else if (mCurrentSubtitlePath.equals(subLocation.getPath()))
                    //maybe content of file changed!!
                    parseSubtitle(subLocation.getPath());
            } else {
                parseSubtitle(subLocation.getPath());
            }
        } else
            Log.d(TAG, "Subtitle selection dialog was cancelled");

    }

    public static void start(Context context, Uri uri) {
        start(context, uri, null, false, -1);
    }

    public static void start(Context context, Uri uri, boolean fromStart) {
        start(context, uri, null, fromStart, -1);
    }

    public static void start(Context context, Uri uri, String title) {
        start(context, uri, title, false, -1);
    }
    public static void startOpened(Context context, Uri uri, int openedPosition) {
        start(context, uri, null, false, openedPosition);
    }

    private static void start(Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        Intent intent = getIntent(context, uri, title, fromStart, openedPosition);

        context.startActivity(intent);
    }

    public static Intent getIntent(String action, MediaWrapper mw, boolean fromStart, int openedPosition) {
        return getIntent(action, VLCApplication.getAppContext(), mw.getUri(), mw.getTitle(), fromStart, openedPosition);
    }

    @NonNull
    public static Intent getIntent(Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        return getIntent(PLAY_FROM_VIDEOGRID, context, uri, title, fromStart, openedPosition);
    }

    @NonNull
    public static Intent getIntent(String action, Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.setAction(action);
        intent.putExtra(PLAY_EXTRA_ITEM_LOCATION, uri);
        intent.putExtra(PLAY_EXTRA_ITEM_TITLE, title);
        intent.putExtra(PLAY_EXTRA_FROM_START, fromStart);

        if (openedPosition != -1 || !(context instanceof Activity)) {
            if (openedPosition != -1)
                intent.putExtra(PLAY_EXTRA_OPENED_POSITION, openedPosition);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(Intent.ACTION_BATTERY_CHANGED)) {
                if (mBattery == null)
                    return;
                int batteryLevel = intent.getIntExtra("level", 0);
                if (batteryLevel >= 50)
                    mBattery.setTextColor(Color.GREEN);
                else if (batteryLevel >= 30)
                    mBattery.setTextColor(Color.YELLOW);
                else
                    mBattery.setTextColor(Color.RED);
                mBattery.setText(String.format("%d%%", batteryLevel));
            }
            else if (action.equalsIgnoreCase(VLCApplication.SLEEP_INTENT)) {
                exitOK();
            }
        }
    };

    protected void exit(int resultCode){
        if (isFinishing())
            return;
        Intent resultIntent = new Intent(ACTION_RESULT);
        if (mUri != null && mService != null) {
            if (AndroidUtil.isNougatOrLater)
                resultIntent.putExtra(EXTRA_URI, mUri.toString());
            else
                resultIntent.setData(mUri);
            resultIntent.putExtra(EXTRA_POSITION, mService.getTime());
            resultIntent.putExtra(EXTRA_DURATION, mService.getLength());
        }
        setResult(resultCode, resultIntent);
        finish();
    }

    private void exitOK() {
        exit(RESULT_OK);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (mIsLoading)
            return false;
        showOverlay();
        return true;
    }

    @TargetApi(12) //only active for Android 3.1+
    public boolean dispatchGenericMotionEvent(MotionEvent event){
        if (mIsLoading)
            return  false;
        //Check for a joystick event
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) !=
                InputDevice.SOURCE_JOYSTICK ||
                event.getAction() != MotionEvent.ACTION_MOVE)
            return false;

        InputDevice mInputDevice = event.getDevice();

        float dpadx = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float dpady = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        if (mInputDevice == null || Math.abs(dpadx) == 1.0f || Math.abs(dpady) == 1.0f)
            return false;

        float x = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_X);
        float y = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Y);
        float rz = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_RZ);

        if (System.currentTimeMillis() - mLastMove > JOYSTICK_INPUT_DELAY){
            if (Math.abs(x) > 0.3){
                if (VLCApplication.showTvUi()) {
                    navigateDvdMenu(x > 0.0f ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT);
                } else
                    seekDelta(x > 0.0f ? 10000 : -10000);
            } else if (Math.abs(y) > 0.3){
                if (VLCApplication.showTvUi())
                    navigateDvdMenu(x > 0.0f ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
                else {
                    if (mIsFirstBrightnessGesture)
                        initBrightnessTouch();
                    changeBrightness(-y / 10f);
                }
            } else if (Math.abs(rz) > 0.3){
                mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int delta = -(int) ((rz / 7) * mAudioMax);
                int vol = (int) Math.min(Math.max(mVol + delta, 0), mAudioMax);
                setAudioVolume(vol);
            }
            mLastMove = System.currentTimeMillis();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mLockBackButton) {
            mLockBackButton = false;
            mHandler.sendEmptyMessageDelayed(RESET_BACK_LOCK, 2000);
            Toast.makeText(getApplicationContext(), getString(R.string.back_quit_lock), Toast.LENGTH_SHORT).show();
        } else if(mPlaylist.getVisibility() == View.VISIBLE) {
            togglePlaylist();
        } else if (mPlaybackSetting != DelayState.OFF){
            endPlaybackSetting();
        } else if (VLCApplication.showTvUi() && mShowing && !mIsLocked) {
            hideOverlay(true);
        } else {
            exitOK();
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B)
            return super.onKeyDown(keyCode, event);
        if (mPlaybackSetting != DelayState.OFF)
            return false;
        if (mIsLoading) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_S:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    exitOK();
                    return true;
            }
            return false;
        }
        //Handle playlist d-pad navigation
        if (mPlaylist.hasFocus()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mPlaylistAdapter.setCurrentIndex(mPlaylistAdapter.getCurrentIndex() - 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mPlaylistAdapter.setCurrentIndex(mPlaylistAdapter.getCurrentIndex() + 1);
                    break;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_BUTTON_A:
                    mService.playIndex(mPlaylistAdapter.getCurrentIndex());
                    break;
            }
            return true;
        }
        if (mShowing || (mFov == 0f && keyCode == KeyEvent.KEYCODE_DPAD_DOWN))
            showOverlayTimeout(OVERLAY_TIMEOUT);
        switch (keyCode) {
        case KeyEvent.KEYCODE_F:
        case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            seekDelta(10000);
            return true;
        case KeyEvent.KEYCODE_R:
        case KeyEvent.KEYCODE_MEDIA_REWIND:
            seekDelta(-10000);
            return true;
        case KeyEvent.KEYCODE_BUTTON_R1:
            seekDelta(60000);
            return true;
        case KeyEvent.KEYCODE_BUTTON_L1:
            seekDelta(-60000);
            return true;
        case KeyEvent.KEYCODE_BUTTON_A:
            if (mOverlayProgress != null && mOverlayProgress.getVisibility() == View.VISIBLE)
                return false;
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
        case KeyEvent.KEYCODE_MEDIA_PLAY:
        case KeyEvent.KEYCODE_MEDIA_PAUSE:
        case KeyEvent.KEYCODE_SPACE:
            if (mIsNavMenu)
                return navigateDvdMenu(keyCode);
            else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) //prevent conflict with remote control
                return super.onKeyDown(keyCode, event);
            else
                doPlayPause();
            return true;
        case KeyEvent.KEYCODE_O:
        case KeyEvent.KEYCODE_BUTTON_Y:
        case KeyEvent.KEYCODE_MENU:
            showAdvancedOptions();
            return true;
        case KeyEvent.KEYCODE_V:
        case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK:
        case KeyEvent.KEYCODE_BUTTON_X:
            onAudioSubClick(mTracks);
            return true;
        case KeyEvent.KEYCODE_N:
            showNavMenu();
            return true;
        case KeyEvent.KEYCODE_A:
            resizeVideo();
            return true;
        case KeyEvent.KEYCODE_M:
        case KeyEvent.KEYCODE_VOLUME_MUTE:
            updateMute();
            return true;
        case KeyEvent.KEYCODE_S:
        case KeyEvent.KEYCODE_MEDIA_STOP:
            exitOK();
            return true;
        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (!mShowing) {
                if (mFov == 0f)
                    seekDelta(-10000);
                else
                    mService.updateViewpoint(-5f, 0f, 0f, 0f, false);
                return true;
            }
        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (!mShowing) {
                if (mFov == 0f)
                    seekDelta(10000);
                else
                    mService.updateViewpoint(5f, 0f, 0f, 0f, false);
                return true;
            }
        case KeyEvent.KEYCODE_DPAD_UP:
            if (!mShowing) {
                if (mFov == 0f)
                    showAdvancedOptions();
                else
                    mService.updateViewpoint(0f, -5f, 0f, 0f, false);
                return true;
            }
        case KeyEvent.KEYCODE_DPAD_DOWN:
            if (!mShowing && mFov != 0f) {
                mService.updateViewpoint(0f, 5f, 0f, 0f, false);
                return true;
            }
        case KeyEvent.KEYCODE_DPAD_CENTER:
            if (!mShowing) {
                doPlayPause();
                return true;
            }
        case KeyEvent.KEYCODE_ENTER:
            if (mIsNavMenu)
                return navigateDvdMenu(keyCode);
            else
                return super.onKeyDown(keyCode, event);
        case KeyEvent.KEYCODE_J:
            delayAudioDelta(-50000l);
            return true;
        case KeyEvent.KEYCODE_K:
            delayAudioDelta(50000l);
            return true;
        case KeyEvent.KEYCODE_G:
            delaySubsDelta(-50000l);
            return true;
        case KeyEvent.KEYCODE_H:
            delaySubsDelta(50000l);
            return true;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            if (mMute) {
                updateMute();
            } else {
                int vol;
                if (mService.getVolume() > 100)
                    vol = Math.round(((float)mService.getVolume())*mAudioMax/100 - 1);
                else
                    vol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) - 1;
                vol = Math.min(Math.max(vol, 0), mAudioMax * (audioBoostEnabled ? 2 : 1));
                mOriginalVol = vol;
                setAudioVolume(vol);
            }
            return true;
        case KeyEvent.KEYCODE_VOLUME_UP:
            if (mMute) {
                updateMute();
            } else {
                int vol;
                if (mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) < mAudioMax)
                    vol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + 1;
                else
                    vol = Math.round(((float)mService.getVolume())*mAudioMax/100 + 1);
                vol = Math.min(Math.max(vol, 0), mAudioMax * (audioBoostEnabled ? 2 : 1));
                setAudioVolume(vol);
            }
            return true;
        case KeyEvent.KEYCODE_CAPTIONS:
            selectSubtitles();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean navigateDvdMenu(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                mService.navigate(MediaPlayer.Navigate.Up);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mService.navigate(MediaPlayer.Navigate.Down);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mService.navigate(MediaPlayer.Navigate.Left);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mService.navigate(MediaPlayer.Navigate.Right);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_A:
                mService.navigate(MediaPlayer.Navigate.Activate);
                return true;
            default:
                return false;
        }
    }

    @Override
    public DelayState getPlaybackSetting() {
        return mPlaybackSetting;
    }

    @Override
    public void showAudioDelaySetting() {
        mPlaybackSetting = DelayState.AUDIO;
        showDelayControls();
    }

    @Override
    public boolean isTouchSubEnabled(){
        return enableTouchSub;
    }
    @Override
    public boolean isCaptionControllerEnabled(){
        return enableCaptionControls;
    }

    @Override
    public void enableDisableTouchSub(){
        SharedPreferences.Editor editor = mSettings.edit();
        if(enableTouchSub) {
            enableTouchSub = false;
            editor.putBoolean("enable_touch_sub", false);
        }
        else {
            enableTouchSub = true;
            editor.putBoolean("enable_touch_sub", true);
        }
        editor.apply();
        //to load caption again with new enableTouchSub option
        hideSubtitleCaption();
        if(mProgressSubtitleCaptionThread != null && mProgressSubtitleCaptionThread.mProgressSubtitleHanlder != null)
            mProgressSubtitleCaptionThread.progessCaption();

    }

    @Override
    public void enableDisableCaptionControls(){
        SharedPreferences.Editor editor = mSettings.edit();
        if(enableCaptionControls) {
            enableCaptionControls = false;
            if(mPlaybackSetting != DelayState.SUBS){
                  hideCaptionControls();
            }
            editor.putBoolean("enable_caption_controls", false);
        }
        else {
            enableCaptionControls = true;
            if(!mService.isPlaying())
                showCaptionControls();

            editor.putBoolean("enable_caption_controls", true);
        }

        editor.apply();
    }

    @Override
    public void showSubsDelaySetting() {
        mPlaybackSetting = DelayState.SUBS;
        showDelayControls();
    }

    private void syncWithCurrentCaption(){
        long currentVideoTime = getTime();
        long delay = 0;
        if(mSyncCaption != null) {
            delay = currentVideoTime - mSyncCaption.start.getMilliseconds();
            mSyncCaption = null;
            delaySubs(delay);
        }

    }

    public void showDelayControls(){
        mTouchAction = TOUCH_NONE;
        if (mPresentation != null)
            showOverlayTimeout(OVERLAY_INFINITE);
        ViewStubCompat vsc = (ViewStubCompat) findViewById(R.id.player_overlay_settings_stub);
        if (vsc != null) {
            vsc.inflate();
            mPlaybackSettingPlus = (ImageView) findViewById(R.id.player_delay_plus);
            mPlaybackSettingMinus = (ImageView) findViewById(R.id.player_delay_minus);
            mPlaybackSettingSyncDone = (ImageView) findViewById(R.id.player_sync_done);

        }
        mPlaybackSettingMinus.setOnClickListener(this);
        mPlaybackSettingPlus.setOnClickListener(this);
        mPlaybackSettingSyncDone.setOnClickListener(this);
        mPlaybackSettingMinus.setOnTouchListener(new OnRepeatListener(this));
        mPlaybackSettingPlus.setOnTouchListener(new OnRepeatListener(this));
        mPlaybackSettingMinus.setVisibility(View.VISIBLE);
        mPlaybackSettingPlus.setVisibility(View.VISIBLE);
        mPlaybackSettingSyncDone.setVisibility(View.VISIBLE);
        mPlaybackSettingPlus.requestFocus();
        if(mPlaybackSetting != DelayState.SUBS)
            if(mCaptionSettingsSync != null)
                mCaptionSettingsSync.setVisibility(View.GONE);

        if(!mService.isPlaying() && mPlaybackSetting == DelayState.SUBS )
            showCaptionControls();

        initPlaybackSettingInfo();
    }
    private void hideCaptionControls(){
        if(mCaptionSettingsNext !=null)
            mCaptionSettingsNext.setVisibility(View.INVISIBLE);
        if(mCaptionSettingsPrev != null)
            mCaptionSettingsPrev.setVisibility(View.INVISIBLE);
        if(mCaptionSettingsSync != null)
            mCaptionSettingsSync.setVisibility(View.GONE);

    }

    private void showCaptionControls() {
        if(mSubs == null)
            return;
        else if(!enableCaptionControls)
            if(mPlaybackSetting != DelayState.SUBS)
                return;

        ViewStubCompat vsc = (ViewStubCompat) findViewById(R.id.player_overlay_next_prev_subcaption_stub);
        if (vsc != null) {
            vsc.inflate();
            mCaptionSettingsPrev = (ImageView) findViewById(R.id.player_prev_caption);
            mCaptionSettingsNext = (ImageView) findViewById(R.id.player_next_caption);
            mCaptionSettingsSync = (ImageView) findViewById(R.id.player_sync_caption);
        }

        mCaptionSettingsNext.setOnLongClickListener(this);
        if (mCaptionSettingsNext != null){
            mCaptionSettingsPrev.setOnClickListener(this);
            mCaptionSettingsNext.setOnClickListener(this);
            mCaptionSettingsNext.setVisibility(View.VISIBLE);
        }
        if(mCaptionSettingsPrev != null) {
            mCaptionSettingsSync.setOnClickListener(this);
            mCaptionSettingsPrev.setOnLongClickListener(this);
            mCaptionSettingsPrev.setVisibility(View.VISIBLE);
        }
        if(mCaptionSettingsSync !=null && mPlaybackSetting == DelayState.SUBS){
            mCaptionSettingsSync.setOnClickListener(this);
            mCaptionSettingsSync.setVisibility(View.VISIBLE);
        }

        //hack:to force show this icons on screen (some times they are not shown until
        //show next subtitle caption or tap the screen

        dimStatusBar(false);

    }


    private void initPlaybackSettingInfo() {
        initInfoOverlay();
        UiTools.setViewVisibility(mOverlayInfoIcon, View.GONE);
        UiTools.setViewVisibility(mOverlayInfo, View.VISIBLE);
        UiTools.setViewVisibility(mEditDelayLayout, View.VISIBLE);
        String text = "";
        if (mPlaybackSetting == DelayState.AUDIO) {
            text += getString(R.string.audio_delay);
            mEditDelayText.setText(Long.toString(mService.getAudioDelay() / 1000l));
        } else if (mPlaybackSetting == DelayState.SUBS) {
            text += getString(R.string.spu_delay);
            mEditDelayText.setText(Long.toString(getSubtitleDelay()));
        } else
            text += "0";
        mInfo.setText(text);
    }

    @Override
    public void endPlaybackSetting() {
        //hide soft keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if(inputMethodManager != null && mEditDelayText != null)
            inputMethodManager.hideSoftInputFromWindow(mEditDelayText.getWindowToken(), 0);
        mTouchAction = TOUCH_NONE;
        mService.saveMediaMeta();
        if (mBtReceiver != null && mPlaybackSetting == DelayState.AUDIO
                && (mAudioManager.isBluetoothA2dpOn() || mAudioManager.isBluetoothScoOn())) {
            String msg = getString(R.string.audio_delay) + "\n"
                    + mService.getAudioDelay() / 1000l
                    + " ms";
            Snackbar sb = Snackbar.make(mInfo, msg, Snackbar.LENGTH_LONG);
            sb.setAction(R.string.save_bluetooth_delay, mBtSaveListener);
            sb.show();
        }

        if (mPlaybackSetting == DelayState.SUBS)
            saveSubtitleDelay(Uri.parse(mCurrentSubtitlePath), mSubtitleDelay);

        mPlaybackSetting = DelayState.OFF;
        if (mPlaybackSettingMinus != null) {
            mPlaybackSettingMinus.setOnClickListener(null);
            mPlaybackSettingMinus.setVisibility(View.INVISIBLE);
        }
        if (mPlaybackSettingPlus != null) {
            mPlaybackSettingPlus.setOnClickListener(null);
            mPlaybackSettingPlus.setVisibility(View.INVISIBLE);
        }
        if(mPlaybackSettingSyncDone != null){
            mPlaybackSettingSyncDone.setOnClickListener(null);
            mPlaybackSettingSyncDone.setVisibility(View.INVISIBLE);
        }
        if(!enableCaptionControls || mSubs == null)
            hideCaptionControls();

        mSyncCaption = null;

        UiTools.setViewVisibility(mOverlayInfo, View.INVISIBLE);
        UiTools.setViewVisibility(mEditDelayLayout, View.GONE);
        if(mInfo != null)
            mInfo.setText("");
        if(mEditDelayText != null)
            mEditDelayText.setText("");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mCaptionSettingsSync != null)
                    mCaptionSettingsSync.setVisibility(View.GONE);
            }
        });

        if (mPlayPause != null)
            mPlayPause.requestFocus();
    }

    public void delayAudioDelta(long delta){
        long delay = mService.getAudioDelay()+delta;
        delayAudio(delay);
    }
    public void delayAudio(long delay) {
        initInfoOverlay();
        mService.setAudioDelay(delay);
        mInfo.setText(getString(R.string.audio_delay));
        mEditDelayText.setText(Long.toString(delay/1000l));
        mAudioDelay = delay;
        if (mPlaybackSetting == DelayState.OFF) {
            mPlaybackSetting = DelayState.AUDIO;
            initPlaybackSettingInfo();
        }
    }

    public long getSubtitleDelay(){
        return mSubtitleDelay;
    }
    public String getCurrentSubtitlePath(){
        return mCurrentSubtitlePath;
    }

    public void delaySubsDelta(long delta){
        long delay = getSubtitleDelay()+delta;
        delaySubs(delay);
    }
    public void delaySubs(long delay) {
        initInfoOverlay();
        mInfo.setText(getString(R.string.spu_delay));
        mEditDelayText.setText(Long.toString(delay));
        mSubtitleDelay = delay;
        if (mPlaybackSetting == DelayState.OFF) {
            mPlaybackSetting = DelayState.SUBS;
            initPlaybackSettingInfo();
        }

        if(mProgressSubtitleCaptionThread != null && mProgressSubtitleCaptionThread.mProgressSubtitleHanlder != null)
            mProgressSubtitleCaptionThread.progessCaption();
    }


    /**
     * Lock screen rotation
     */
    private void lockScreen() {
        if (mScreenOrientation != 100) {
            mScreenOrientationLock = getRequestedOrientation();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            else
                setRequestedOrientation(getScreenOrientation(100));
        }
        showInfo(R.string.locked, 1000);
        mTime.setEnabled(false);
        mSeekbar.setEnabled(false);
        mLength.setEnabled(false);
        mSize.setEnabled(false);
        if (mPlaylistNext != null)
            mPlaylistNext.setEnabled(false);
        if (mPlaylistPrevious != null)
            mPlaylistPrevious.setEnabled(false);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) mOverlayProgress.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,0);
        mOverlayProgress.setLayoutParams(layoutParams);
        mOverlayProgress.getBackground().setAlpha(0);

        hideOverlay(true);
        mLockBackButton = true;
        mIsLocked = true;
    }

    /**
     * Remove screen lock
     */
    private void unlockScreen() {
        if(mScreenOrientation != 100)
            setRequestedOrientation(mScreenOrientationLock);
        showInfo(R.string.unlocked, 1000);
        mUnlock.setVisibility(View.INVISIBLE);
        mTime.setEnabled(true);
        mSeekbar.setEnabled(mService == null || mService.isSeekable());
        mLength.setEnabled(true);
        mSize.setEnabled(true);
        if (mPlaylistNext != null)
            mPlaylistNext.setEnabled(true);
        if (mPlaylistPrevious != null)
            mPlaylistPrevious.setEnabled(true);
        mShowing = false;
        mIsLocked = false;

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) mOverlayProgress.getLayoutParams();
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP,0);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
        mOverlayProgress.setLayoutParams(layoutParams);
        mOverlayProgress.getBackground().setAlpha(255);

        showOverlay();
        mLockBackButton = false;
    }

    /**
     * Show text in the info view and vertical progress bar for "duration" milliseconds
     * @param resource
     * @param duration
     * @param barNewValue new volume/brightness value (range: 0 - 15)
     */
    private  void showInfoWithIcon(int resource, int duration, int barNewValue) {
        showInfo(Integer.toString(barNewValue), duration);
        mOverlayInfoIcon.setImageResource(resource);
        mOverlayInfoIcon.setColorFilter(ContextCompat.getColor(this,R.color.white));
        mOverlayInfoIcon.setVisibility(View.VISIBLE);
    }

    /**
     * Show text in the info view for "duration" milliseconds
     * @param text
     * @param duration
     */
    private void showInfo(String text, int duration) {
        initInfoOverlay();
        UiTools.setViewVisibility(mOverlayInfoIcon, View.GONE);
        UiTools.setViewVisibility(mOverlayInfo, View.VISIBLE);
        UiTools.setViewVisibility(mEditDelayLayout, View.GONE);
        mInfo.setText(text);
        mHandler.removeMessages(FADE_OUT_INFO);
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration);
    }

    private void initInfoOverlay() {
        ViewStubCompat vsc = (ViewStubCompat) findViewById(R.id.player_info_stub);
        if (vsc != null) {
            vsc.inflate();
            // the info textView is not on the overlay
            mInfo = (TextView) findViewById(R.id.player_overlay_textinfo);
            mOverlayInfo = findViewById(R.id.player_overlay_info);
            mOverlayInfoIcon = (ImageView) findViewById(R.id.overlay_info_icon);
            mEditDelayLayout = (LinearLayout) findViewById(R.id.player_overlay_delay);
            mEditDelayText = (EditText) findViewById(R.id.player_overlay_edit_delay);
            mEditDelayText.setOnEditorActionListener(new TextView.OnEditorActionListener(){
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        long delay = Long.parseLong(mEditDelayText.getText().toString());
                        if (mPlaybackSetting == DelayState.AUDIO)
                            delayAudio(delay * 1000l);
                        else if (mPlaybackSetting == DelayState.SUBS)
                            delaySubs(delay);
                    }
                    return false;
                }
            });
//=======
//            mVerticalBar = findViewById(R.id.verticalbar);
//            mVerticalBarProgress = findViewById(R.id.verticalbar_progress);
//            mVerticalBarBoostProgress = findViewById(R.id.verticalbar_boost_progress);
//>>>>>>> f5e2403776f36ed276353f7ad3c0652b590396a0
        }
    }

    private void showInfo(int textid, int duration) {
        initInfoOverlay();
        UiTools.setViewVisibility(mOverlayInfoIcon, View.GONE);
        UiTools.setViewVisibility(mOverlayInfo, View.VISIBLE);
        UiTools.setViewVisibility(mEditDelayLayout, View.GONE);
        mInfo.setText(textid);
        mHandler.removeMessages(FADE_OUT_INFO);
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration);
    }

    /**
     * hide the info view with "delay" milliseconds delay
     * @param delay
     */
    private void hideInfo(int delay) {
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, delay);
    }

    /**
     * hide the info view
     */
    private void hideInfo() {
        hideInfo(0);
    }

    private void fadeOutInfo() {
        if(mPlaybackSetting != DelayState.OFF) {
            initPlaybackSettingInfo();
            return;
        }
        if (mOverlayInfo != null && mOverlayInfo.getVisibility() == View.VISIBLE) {
            mOverlayInfo.startAnimation(AnimationUtils.loadAnimation(
                    VideoPlayerActivity.this, android.R.anim.fade_out));
            UiTools.setViewVisibility(mOverlayInfo, View.INVISIBLE);
        }
    }

    /* PlaybackService.Callback */

    @Override
    public void update() {
        updateList();
    }

    @Override
    public void updateProgress() {
    }

    @Override
    public void onMediaEvent(Media.Event event) {
        switch (event.type) {
            case Media.Event.ParsedChanged:
                updateNavStatus();
                break;
            case Media.Event.MetaChanged:
                break;
            case Media.Event.SubItemTreeAdded:
                mHasSubItems = true;
                break;
        }
    }

    @Override
    public void onMediaPlayerEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Opening:
                mHasSubItems = false;
                break;
            case MediaPlayer.Event.Playing:
                onPlaying();
                hideCaptionControls();
                break;
            case MediaPlayer.Event.Paused:
                updateOverlayPausePlay();
                showCaptionControls();
                break;
            case MediaPlayer.Event.Stopped:
                exitOK();
                break;
            case MediaPlayer.Event.EndReached:
                /* Don't end the activity if the media has subitems since the next child will be
                 * loaded by the PlaybackService */
                if (!mHasSubItems)
                    endReached();
                break;
            case MediaPlayer.Event.EncounteredError:
                encounteredError();
                break;
            case MediaPlayer.Event.Vout:
                updateNavStatus();
                if (mMenuIdx == -1)
                    handleVout(event.getVoutCount());
                break;
            case MediaPlayer.Event.ESAdded:
                if (mMenuIdx == -1) {
                    MediaWrapper media = mMedialibrary.findMedia(mService.getCurrentMediaWrapper());
                    if (media == null)
                        return;
                    if (event.getEsChangedType() == Media.Track.Type.Audio) {
                        setESTrackLists();
                        int audioTrack = (int) media.getMetaLong(MediaWrapper.META_AUDIOTRACK);
                        if (audioTrack != 0 || mCurrentAudioTrack != -2)
                            mService.setAudioTrack(media.getId() == 0L ? mCurrentAudioTrack : audioTrack);
                    } else if (event.getEsChangedType() == Media.Track.Type.Text) {
                        setESTrackLists();
                    }
                }
            case MediaPlayer.Event.ESDeleted:
                if (mMenuIdx == -1 && event.getEsChangedType() == Media.Track.Type.Video) {
                    mHandler.removeMessages(CHECK_VIDEO_TRACKS);
                    mHandler.sendEmptyMessageDelayed(CHECK_VIDEO_TRACKS, 1000);
                }
                invalidateESTracks(event.getEsChangedType());
                break;
            case MediaPlayer.Event.ESSelected:
                if (event.getEsChangedType() == Media.VideoTrack.Type.Video) {
                    Media.VideoTrack vt = mService.getCurrentVideoTrack();
                    changeSurfaceLayout();
                    if (vt != null)
                        mFov = vt.projection == Media.VideoTrack.Projection.Rectangular ? 0f : DEFAULT_FOV;
                }
                break;
            case MediaPlayer.Event.SeekableChanged:
                updateSeekable(event.getSeekable());
                break;
            case MediaPlayer.Event.PausableChanged:
                updatePausable(event.getPausable());
                break;
            case MediaPlayer.Event.Buffering:
                if (!mIsPlaying)
                    break;
                if (event.getBuffering() == 100f)
                    stopLoading();
                else if (!mHandler.hasMessages(LOADING_ANIMATION) && !mIsLoading
                        && mTouchAction != TOUCH_SEEK && !mDragging)
                    mHandler.sendEmptyMessageDelayed(LOADING_ANIMATION, LOADING_ANIMATION_DELAY);
                break;
            case MediaPlayer.Event.TimeChanged:
            case MediaPlayer.Event.PositionChanged:
                if(mProgressSubtitleCaptionThread != null && mProgressSubtitleCaptionThread.mProgressSubtitleHanlder != null)
                    mProgressSubtitleCaptionThread.progessCaption();
                break;
        }
    }

    /**
     * Handle resize of the surface and the overlay
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mService == null)
                return true;

            switch (msg.what) {
                case FADE_OUT:
                    hideOverlay(false);
                    break;
                case SHOW_PROGRESS:
                    int pos = setOverlayProgress();
                    if (pos >= 0 && canShowProgress()) {
                        msg = mHandler.obtainMessage(SHOW_PROGRESS);
                        mHandler.sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case FADE_OUT_INFO:
                    fadeOutInfo();
                    break;
                case START_PLAYBACK:
                    startPlayback();
                    break;
                case AUDIO_SERVICE_CONNECTION_FAILED:
                    exit(RESULT_CONNECTION_FAILED);
                    break;
                case RESET_BACK_LOCK:
                    mLockBackButton = true;
                    break;
                case CHECK_VIDEO_TRACKS:
                    if (mService.getVideoTracksCount() < 1 && mService.getAudioTracksCount() > 0) {
                        Log.i(TAG, "No video track, open in audio mode");
                        switchToAudioMode(true);
                    }
                    break;
                case LOADING_ANIMATION:
                    startLoading();
                    break;
                case HIDE_INFO:
                    hideOverlay(true);
                    break;
                case SHOW_INFO:
                    showOverlay();
                    break;
            }
            return true;
        }
    });

    private boolean canShowProgress() {
        return !mDragging && mShowing && mService != null &&  mService.isPlaying();
    }

    private void onPlaying() {
        mIsPlaying = true;
        setPlaybackParameters();
        stopLoading();
        updateOverlayPausePlay();
        updateNavStatus();
        if (!mService.getCurrentMediaWrapper().hasFlag(MediaWrapper.MEDIA_PAUSED))
            mHandler.sendEmptyMessageDelayed(FADE_OUT, OVERLAY_TIMEOUT);
        else {
            mService.getCurrentMediaWrapper().removeFlags(MediaWrapper.MEDIA_PAUSED);
            mWasPaused = false;
        }

        setESTracks();
    }

    private void endReached() {
        if (mService == null)
            return;
        if (mService.getRepeatType() == PlaybackService.REPEAT_ONE){
            seek(0);
            return;
        }
        if (mService.expand(false) == 0) {
            mHandler.removeMessages(LOADING_ANIMATION);
            mHandler.sendEmptyMessageDelayed(LOADING_ANIMATION, LOADING_ANIMATION_DELAY);
            Log.d(TAG, "Found a video playlist, expanding it");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    loadMedia();
                }
            });
        }
        //Ignore repeat 
        if (mService.getRepeatType() == PlaybackService.REPEAT_ALL && mService.getMediaListSize() == 1)
            exitOK();
    }

    private void encounteredError() {
        if (isFinishing())
            return;
        //We may not have the permission to access files
        if (AndroidUtil.isMarshMallowOrLater && mUri != null &&
                TextUtils.equals(mUri.getScheme(), "file") &&
                !Permissions.canReadStorage()) {
            Permissions.checkReadStoragePermission(this, true);
            return;
        }
        /* Encountered Error, exit player with a message */
        mAlertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                exit(RESULT_PLAYBACK_ERROR);
            }
        })
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                exit(RESULT_PLAYBACK_ERROR);
            }
        })
        .setTitle(R.string.encountered_error_title)
        .setMessage(R.string.encountered_error_message)
        .create();
        mAlertDialog.show();
    }

    private final Runnable mSwitchAudioRunnable = new Runnable() {
        @Override
        public void run() {
            if (mService.hasMedia()) {
                Log.i(TAG, "Video track lost, switching to audio");
                mSwitchingView = true;
            }
            exit(RESULT_VIDEO_TRACK_LOST);
        }
    };

    private void handleVout(int voutCount) {
        mHandler.removeCallbacks(mSwitchAudioRunnable);

        final IVLCVout vlcVout = mService.getVLCVout();
        if (vlcVout.areViewsAttached() && voutCount == 0) {
            mHandler.postDelayed(mSwitchAudioRunnable, 4000);
        }
    }

    public void switchToAudioMode(boolean showUI) {
        if (mService == null)
            return;
        mSwitchingView = true;
        // Show the MainActivity if it is not in background.
        if (showUI) {
            Intent i = new Intent(this, VLCApplication.showTvUi() ? AudioPlayerActivity.class : MainActivity.class);
            startActivity(i);
        } else
            mSettings.edit().putBoolean(PreferencesActivity.VIDEO_RESTORE, true).apply();
        exitOK();
    }

    private void changeMediaPlayerLayout(int displayW, int displayH) {
        /* Change the video placement using MediaPlayer API */
        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                mService.setVideoAspectRatio(null);
                mService.setVideoScale(0);
                break;
            case SURFACE_FIT_SCREEN:
            case SURFACE_FILL: {
                Media.VideoTrack vtrack = mService.getCurrentVideoTrack();
                if (vtrack == null)
                    return;
                final boolean videoSwapped = vtrack.orientation == Media.VideoTrack.Orientation.LeftBottom
                        || vtrack.orientation == Media.VideoTrack.Orientation.RightTop;
                if (mCurrentSize == SURFACE_FIT_SCREEN) {
                    int videoW = vtrack.width;
                    int videoH = vtrack.height;

                    if (videoSwapped) {
                        int swap = videoW;
                        videoW = videoH;
                        videoH = swap;
                    }
                    if (vtrack.sarNum != vtrack.sarDen)
                        videoW = videoW * vtrack.sarNum / vtrack.sarDen;

                    float ar = videoW / (float) videoH;
                    float dar = displayW / (float) displayH;

                    float scale;
                    if (dar >= ar)
                        scale = displayW / (float) videoW; /* horizontal */
                    else
                        scale = displayH / (float) videoH; /* vertical */
                    mService.setVideoScale(scale);
                    mService.setVideoAspectRatio(null);
                } else {
                    mService.setVideoScale(0);
                    mService.setVideoAspectRatio(!videoSwapped ? ""+displayW+":"+displayH
                                                               : ""+displayH+":"+displayW);
                }
                break;
            }
            case SURFACE_16_9:
                mService.setVideoAspectRatio("16:9");
                mService.setVideoScale(0);
                break;
            case SURFACE_4_3:
                mService.setVideoAspectRatio("4:3");
                mService.setVideoScale(0);
                break;
            case SURFACE_ORIGINAL:
                mService.setVideoAspectRatio(null);
                mService.setVideoScale(1);
                break;
        }
    }

    @Override
    public boolean isInPictureInPictureMode() {
        return AndroidUtil.isNougatOrLater && super.isInPictureInPictureMode();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        changeSurfaceLayout();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void changeSurfaceLayout() {
        int sw;
        int sh;

        // get screen size
        if (mPresentation == null) {
            sw = getWindow().getDecorView().getWidth();
            sh = getWindow().getDecorView().getHeight();
        } else {
            sw = mPresentation.getWindow().getDecorView().getWidth();
            sh = mPresentation.getWindow().getDecorView().getHeight();
        }

        // sanity check
        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }

        if (mService != null) {
            final IVLCVout vlcVout = mService.getVLCVout();
            vlcVout.setWindowSize(sw, sh);
        }

        SurfaceView surface;
//        SurfaceView subtitlesSurface;
        FrameLayout surfaceFrame;
        if (mPresentation == null) {
            surface = mSurfaceView;
//            subtitlesSurface = mSubtitleView;
            surfaceFrame = mSurfaceFrame;
        } else {
            surface = mPresentation.mSurfaceView;
//            subtitlesSurface = mPresentation.mSubtitlesSurfaceView;
            surfaceFrame = mPresentation.mSurfaceFrame;
        }
        LayoutParams lp = surface.getLayoutParams();

        if (mVideoWidth * mVideoHeight == 0 || isInPictureInPictureMode()) {
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */
            lp.width  = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.MATCH_PARENT;
            surface.setLayoutParams(lp);
            lp = surfaceFrame.getLayoutParams();
            lp.width  = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.MATCH_PARENT;
            surfaceFrame.setLayoutParams(lp);
            if (mService != null && mVideoWidth * mVideoHeight == 0)
                changeMediaPlayerLayout(sw, sh);
            return;
        }

        if (mService != null && lp.width == lp.height && lp.width == LayoutParams.MATCH_PARENT) {
            /* We handle the placement of the video using Android View LayoutParams */
            mService.setVideoAspectRatio(null);
            mService.setVideoScale(0);
        }

        double dw = sw, dh = sh;
        boolean isPortrait;

        if (mPresentation == null) {
            // getWindow().getDecorView() doesn't always take orientation into account, we have to correct the values
            isPortrait = mCurrentScreenOrientation == Configuration.ORIENTATION_PORTRAIT;
        } else {
            isPortrait = false;
        }

        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh;
            dh = sw;
        }

        // compute the aspect ratio
        double ar, vw;
        if (mSarDen == mSarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth;
            ar = (double)mVideoVisibleWidth / (double)mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * (double)mSarNum / mSarDen;
            ar = vw / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double dar = dw / dh;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_FIT_SCREEN:
                if (dar >= ar)
                    dh = dw / ar; /* horizontal */
                else
                    dw = dh * ar; /* vertical */
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_ORIGINAL:
                dh = mVideoVisibleHeight;
                dw = vw;
                break;
        }

        // set display size
        lp.width  = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
        surface.setLayoutParams(lp);
//        subtitlesSurface.setLayoutParams(lp);

        // set frame size (crop if necessary)
        lp = surfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        surfaceFrame.setLayoutParams(lp);

        surface.invalidate();
    }

    private void sendMouseEvent(int action, int button, int x, int y) {
        if (mService == null)
            return;
        final IVLCVout vlcVout = mService.getVLCVout();
        vlcVout.sendMouseEvent(action, button, x, y);
    }

    /**
     * show/hide the overlay
     */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mService == null)
            return false;
        if (mDetector == null) {
            mDetector = new GestureDetectorCompat(this, mGestureListener);
            mDetector.setOnDoubleTapListener(mGestureListener);
        }
        if (mFov != 0f && mScaleGestureDetector == null)
            mScaleGestureDetector = new ScaleGestureDetector(this, this);
        if (mPlaylist.getVisibility() == View.VISIBLE) {
            togglePlaylist();
            return true;
        }
        if (mFov != 0f && mScaleGestureDetector != null)
            mScaleGestureDetector.onTouchEvent(event);
        if ((mScaleGestureDetector != null && mScaleGestureDetector.isInProgress()) ||
                (mDetector != null && mDetector.onTouchEvent(event)))
            return true;

        float x_changed, y_changed;
        if (mTouchX != -1f && mTouchY != -1f) {
            y_changed = event.getRawY() - mTouchY;
            x_changed = event.getRawX() - mTouchX;
        } else {
            x_changed = 0f;
            y_changed = 0f;
        }

        // coef is the gradient's move to determine a neutral zone
        float coef = Math.abs (y_changed / x_changed);
        float xgesturesize = ((x_changed / mScreen.xdpi) * 2.54f);
        float delta_y = Math.max(1f, (Math.abs(mInitTouchY - event.getRawY()) / mScreen.xdpi + 0.5f) * 2f);

        int xTouch = Math.round(event.getRawX());
        int yTouch = Math.round(event.getRawY());

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                // Audio
                mTouchY = mInitTouchY = event.getRawY();
                if (mService.getVolume() <= 100) {
                    mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    mOriginalVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                else {
                    mVol = ((float)mService.getVolume()) * mAudioMax / 100;
                }
                mTouchAction = TOUCH_NONE;
                // Seek
                mTouchX = event.getRawX();
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_DOWN, 0, xTouch, yTouch);
                break;

            case MotionEvent.ACTION_MOVE:
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_MOVE, 0, xTouch, yTouch);

                if (mFov == 0f) {
                    // No volume/brightness action if coef < 2 or a secondary display is connected
                    //TODO : Volume action when a secondary display is connected
                    if (mTouchAction != TOUCH_SEEK && coef > 2 && mPresentation == null) {
                        if (Math.abs(y_changed/mSurfaceYDisplayRange) < 0.05)
                            return false;
                        mTouchY = event.getRawY();
                        mTouchX = event.getRawX();
                        // (Up or Down - Right side) LTR: Volume RTL: Brightness
                        if ((int)mTouchX > (4 * mScreen.widthPixels / 7f)){
                            if(!mIsLocked && mTouchVolumeControl % 2 == 0 /* == 2 || == 0 */)
                                return true;
                            if(mIsLocked && mTouchVolumeControl < 2)
                                return true;
                            if (!mIsRtl)
                                doVolumeTouch(y_changed);
                            else
                                doBrightnessTouch(y_changed);
                            hideOverlay(true);
                        }
                        // (Up or Down - Left side) LTR: Brightness RTL: Volume
                        if ((int)mTouchX < (3 * mScreen.widthPixels / 7f)){
                            if(!mIsLocked && mTouchBrightnessControl % 2 == 0 /* == 2 || == 0 */)
                                return true;
                            if(mIsLocked && mTouchBrightnessControl < 2)
                                return true;
                            if(!mIsRtl)
                                doBrightnessTouch(y_changed);
                            else
                                doVolumeTouch(y_changed);
                            hideOverlay(true);
                        }
                    } else {
                        // Seek (Right or Left move)
                        if(!mIsLocked && mTouchSeekControl % 2 == 0 /* == 2 || == 0 */)
                            return true;
                        if(mIsLocked && mTouchSeekControl < 2)
                            return true;
                        doSeekTouch(Math.round(delta_y), mIsRtl ? -xgesturesize : xgesturesize , false);
                    }
                } else {
                    mTouchY = event.getRawY();
                    mTouchX = event.getRawX();
                    mTouchAction = TOUCH_MOVE;
                    float yaw = mFov * -x_changed/(float)mSurfaceXDisplayRange;
                    float pitch = mFov * -y_changed/(float)mSurfaceXDisplayRange;
                    mService.updateViewpoint(yaw, pitch, 0, 0, false);
                }
                break;

            case MotionEvent.ACTION_UP:
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_UP, 0, xTouch, yTouch);
                // Seek
                if (mTouchAction == TOUCH_SEEK)
                    doSeekTouch(Math.round(delta_y), mIsRtl ? -xgesturesize : xgesturesize , true);
                mTouchX = -1f;
                mTouchY = -1f;
                break;
        }
        return mTouchAction != TOUCH_NONE;
    }

    private void doSeekTouch(int coef, float gesturesize, boolean seek) {
        if (coef == 0)
            coef = 1;
        // No seek action if coef > 0.5 and gesturesize < 1cm
        if (Math.abs(gesturesize) < 1 || !mService.isSeekable())
            return;

        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_SEEK)
            return;
        mTouchAction = TOUCH_SEEK;

        long length = mService.getLength();
        long time = getTime();

        // Size of the jump, 10 minutes max (600000), with a bi-cubic progression, for a 8cm gesture
        int jump = (int) ((Math.signum(gesturesize) * ((600000 * Math.pow((gesturesize / 8), 4)) + 3000)) / coef);

        // Adjust the jump
        if ((jump > 0) && ((time + jump) > length))
            jump = (int) (length - time);
        if ((jump < 0) && ((time + jump) < 0))
            jump = (int) -time;

        //Jump !
        if (seek && length > 0)
            seek(time + jump, length,true);

        if (length > 0)
            //Show the jump's size
            showInfo(String.format("%s%s (%s)%s",
                    jump >= 0 ? "+" : "",
                    Tools.millisToString(jump),
                    Tools.millisToString(time + jump),
                    coef > 1 ? String.format(" x%.1g", 1.0/coef) : ""), 50);
        else
            showInfo(R.string.unseekable_stream, 1000);
    }

    private void doVolumeTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_VOLUME)
            return;
        float delta = - ((y_changed / (float) mScreen.heightPixels) * mAudioMax);
        mVol += delta;
        int vol = (int) Math.min(Math.max(mVol, 0), mAudioMax * (audioBoostEnabled ? 2 : 1));
        if (delta < 0)
            mOriginalVol = vol;
        if (delta != 0f) {
            if (vol > mAudioMax) {
                if (audioBoostEnabled) {
                    if (mOriginalVol < mAudioMax) {
                        displayWarningToast();
                        setAudioVolume(mAudioMax);
                    } else {
                        setAudioVolume(vol);
                    }
                }
            } else {
                setAudioVolume(vol);
            }
        }
    }

    //Toast that appears only once
    public void displayWarningToast() {
        if(warningToast != null)
            warningToast.cancel();
        warningToast = Toast.makeText(getApplication(), R.string.audio_boost_warning, Toast.LENGTH_SHORT);
        warningToast.show();
    }

    private void setAudioVolume(int vol) {
        if (vol <= 0 && AndroidUtil.isNougatOrLater)
            return; //Android N+ throws "SecurityException: Not allowed to change Do Not Disturb state"

        /* Since android 4.3, the safe volume warning dialog is displayed only with the FLAG_SHOW_UI flag.
         * We don't want to always show the default UI volume, so show it only when volume is not set. */
        if (vol <= mAudioMax) {
            mService.setVolume(100);
            int newVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (vol != newVol)
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
            vol = Math.round(vol * 100 / mAudioMax);
        } else {
            vol = Math.round(vol * 100 / mAudioMax);
            mService.setVolume(Math.round(vol));
        }
        mTouchAction = TOUCH_VOLUME;
        showInfoWithIcon(R.drawable.ic_volume_up_black_24dp, 1000, vol);
    }

    private void mute(boolean mute) {
        mMute = mute;
        if (mMute)
            mVolSave = mService.getVolume();
        mService.setVolume(mMute ? 0 : mVolSave);
    }

    private void updateMute () {
        mute(!mMute);
        showInfo(mMute ? R.string.sound_off : R.string.sound_on, 1000);
    }

    private void initBrightnessTouch() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float brightnesstemp = lp.screenBrightness != -1f ? lp.screenBrightness : 0.6f;
        // Initialize the layoutParams screen brightness
        try {
            if (Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            if (!Permissions.canWriteSettings(this)) {
                Permissions.checkWriteSettingsPermission(this, Permissions.PERMISSION_SYSTEM_BRIGHTNESS);
                return;
            }
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                mRestoreAutoBrightness = android.provider.Settings.System.getInt(getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            } else if (brightnesstemp == 0.6f) {
                brightnesstemp = android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            }
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        lp.screenBrightness = brightnesstemp;
        getWindow().setAttributes(lp);
        mIsFirstBrightnessGesture = false;
    }

    private void doBrightnessTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_BRIGHTNESS)
            return;
        if (mIsFirstBrightnessGesture) initBrightnessTouch();
            mTouchAction = TOUCH_BRIGHTNESS;

        // Set delta : 2f is arbitrary for now, it possibly will change in the future
        float delta = - y_changed / mSurfaceYDisplayRange;

        changeBrightness(delta);
    }

    private void changeBrightness(float delta) {
        // Estimate and adjust Brightness
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float brightness =  Math.min(Math.max(lp.screenBrightness + delta, 0.01f), 1f);
        setWindowBrightness(brightness);
        brightness = Math.round(brightness * 100);
        showInfoWithIcon(R.drawable.ic_brightness_high_black_24dp, 1000, (int) brightness);
    }

    private void setWindowBrightness(float brightness) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness =  brightness;
        // Set Brightness
        getWindow().setAttributes(lp);
    }

    /**
     * handle changes of the seekbar (slicer)
     */
    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
            showOverlayTimeout(OVERLAY_INFINITE);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            showOverlay(true);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!isFinishing() && fromUser && mService.isSeekable()) {
                seek(progress);
                setOverlayProgress();
                mTime.setText(Tools.millisToString(progress));
                showInfo(Tools.millisToString(progress), 1000);
            }
        }
    };

    public void onAudioSubClick(View anchor){
        if (anchor == null) {
            initOverlay();
            anchor = mTracks;
        }
        final AppCompatActivity context = this;
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.audiosub_tracks, popupMenu.getMenu());
        popupMenu.getMenu().findItem(R.id.video_menu_audio_track).setEnabled(mService.getAudioTracksCount() > 0);
        popupMenu.getMenu().findItem(R.id.video_menu_subtitles).setEnabled(mSubtitleFiles.size() > 0);
        //FIXME network subs cannot be enabled & screen cast display is broken with picker
        popupMenu.getMenu().findItem(R.id.video_menu_subtitles_picker).setEnabled(mPresentation == null);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.video_menu_audio_track) {
                    selectAudioTrack();
                    return true;
                } else if (item.getItemId() == R.id.video_menu_subtitles) {
                    selectSubtitles();
                    return true;
                } else if (item.getItemId() == R.id.video_menu_subtitles_picker) {
                    if (mUri == null)
                        return false;
                    mWasPlaying = mService.isPlaying();
                    pause();
                    mShowingDialog = true;
                    Intent filePickerIntent = new Intent(context, FilePickerActivity.class);
                    filePickerIntent.setData(Uri.parse(FileUtils.getParent(mUri.toString())));
                    context.startActivityForResult(filePickerIntent, 0);
                    return true;
                } else if (item.getItemId() == R.id.video_menu_subtitles_download) {
                    if (mUri == null)
                        return false;
                    MediaUtils.getSubs(VideoPlayerActivity.this, mService.getCurrentMediaWrapper(), new SubtitlesDownloader.Callback() {
                        @Override
                        public void onRequestEnded(boolean success) {
                            if (success)
                                getSubtitles();
                        }
                    });
                }
                hideOverlay(true);
                return false;
            }
        });
        popupMenu.show();
        showOverlay();
    }

    @Override
    public void onPopupMenu(View anchor, final int position) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.audio_player, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.audio_player_mini_remove) {
                    if (mService != null) {
                        mPlaylistAdapter.remove(position);
                        mService.remove(position);
                        return true;
                    }
                }
                return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void updateList() {
        if (mService == null || mPlaylistAdapter == null)
            return;

        mPlaylistAdapter.update(mService.getMedias());
    }

    @Override
    public void onSelectionSet(int position) {
        mPlaylist.scrollToPosition(position);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.player_overlay_play:
                doPlayPause();
                break;
            case R.id.playlist_toggle:
                togglePlaylist();
                break;
            case R.id.playlist_next:
                mService.next();
                break;
            case R.id.playlist_previous:
                mService.previous(false);
                break;
            case R.id.player_overlay_forward:
                seekDelta(10000);
                break;
            case R.id.player_overlay_rewind:
                seekDelta(-10000);
                break;
            case R.id.lock_overlay_button:
                lockScreen();
                break;
            case R.id.unlock_overlay_button:
                unlockScreen();
                break;
            case R.id.player_overlay_size:
                resizeVideo();
                break;
            case R.id.player_overlay_navmenu:
                showNavMenu();
                break;
            case R.id.player_overlay_length:
            case R.id.player_overlay_time:
                mDisplayRemainingTime = !mDisplayRemainingTime;
                showOverlay();
                mSettings.edit().putBoolean(KEY_REMAINING_TIME_DISPLAY, mDisplayRemainingTime).apply();
                break;
            case R.id.player_delay_minus:
                if (mPlaybackSetting == DelayState.AUDIO)
                    delayAudioDelta(-50000);
                else if (mPlaybackSetting == DelayState.SUBS)
                    delaySubsDelta(-100);
                break;
            case R.id.player_delay_plus:
                if (mPlaybackSetting == DelayState.AUDIO)
                    delayAudioDelta(50000);
                else if (mPlaybackSetting == DelayState.SUBS)
                    delaySubsDelta(100);
                break;
            case R.id.player_overlay_adv_function:
                showAdvancedOptions();
                break;
            case R.id.player_overlay_tracks:
                onAudioSubClick(v);
                break;
            case R.id.player_prev_caption:
                showPrevSubtitleCaption();
                break;
            case R.id.player_next_caption:
                showNextSubtitleCaption();
                break;
            case R.id.player_sync_done:
                if (mPlaybackSetting != DelayState.OFF)
                    endPlaybackSetting();
                break;
            case R.id.player_sync_caption:
                syncWithCurrentCaption();
                break;
        }
    }

    public boolean onLongClick(View v) {
        switch (v.getId()){
            case R.id.player_overlay_play:
                if (mService == null)
                    return false;
                if (mService.getRepeatType() == PlaybackService.REPEAT_ONE) {
                    showInfo(getString(R.string.repeat), 1000);
                    mService.setRepeatType(PlaybackService.REPEAT_NONE);
                } else {
                    mService.setRepeatType(PlaybackService.REPEAT_ONE);
                    showInfo(getString(R.string.repeat_single), 1000);
                }
                return true;
            case R.id.player_next_caption:
                seekNextSubtitleCaption();
                return true;
            case R.id.player_prev_caption:
                seekPrevSubtitleCaption();
                return true;
            default:
                return false;
        }
    }
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float diff = DEFAULT_FOV * (1 - detector.getScaleFactor());
        if (mService.updateViewpoint(0, 0, 0, diff, false)) {
            mFov = Math.min(Math.max(MIN_FOV, mFov + diff), MAX_FOV);
            return true;
        }
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return mSurfaceXDisplayRange!= 0 && mFov != 0f;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {}


    private interface TrackSelectedListener {
        boolean onTrackSelected(int trackID);
    }

    private void selectTrack(final MediaPlayer.TrackDescription[] tracks, int currentTrack, int titleId,
                             final TrackSelectedListener listener) {
        if (listener == null)
            throw new IllegalArgumentException("listener must not be null");
        if (tracks == null)
            return;
        final String[] nameList = new String[tracks.length];
        final int[] idList = new int[tracks.length];
        int i = 0;
        int listPosition = 0;
        for (MediaPlayer.TrackDescription track : tracks) {
            idList[i] = track.id;
            nameList[i] = track.name;
            // map the track position to the list position
            if (track.id == currentTrack)
                listPosition = i;
            i++;
        }

        if (!isFinishing()) {
            mAlertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
                    .setTitle(titleId)
                    .setSingleChoiceItems(nameList, listPosition, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int listPosition) {
                            int trackID = -1;
                            // Reverse map search...
                            for (MediaPlayer.TrackDescription track : tracks) {
                                if (idList[listPosition] == track.id) {
                                    trackID = track.id;
                                    break;
                                }
                            }
                            listener.onTrackSelected(trackID);
                            dialog.dismiss();
                        }
                    })
                    .create();
            mAlertDialog.setCanceledOnTouchOutside(true);
            mAlertDialog.setOwnerActivity(VideoPlayerActivity.this);
            mAlertDialog.show();
        }
    }


    private void selectAudioTrack() {
        setESTrackLists();
        selectTrack(mAudioTracksList, mService.getAudioTrack(), R.string.track_audio,
                new TrackSelectedListener() {
                    @Override
                    public boolean onTrackSelected(int trackID) {
                        if (trackID < -1 || mService == null)
                            return false;
                        if(trackID == -1 && mPlaybackSetting == DelayState.AUDIO){
                            endPlaybackSetting();
                        }
                        mService.setAudioTrack(trackID);
                        MediaWrapper mw = mMedialibrary.findMedia(mService.getCurrentMediaWrapper());
                        if (mw != null && mw.getId() != 0L)
                            mw.setLongMeta(MediaWrapper.META_AUDIOTRACK, trackID);
                        return true;
                    }
                });
    }


    @Override
    public synchronized void onSubtitleParseCompleted(boolean isSuccessful, TimedTextObject subtitleCaptions, String subtitleFilePath) {
        removeCurrentSubtitle();
        if((subtitleCaptions == null) || (subtitleCaptions.captions == null ) || (subtitleCaptions.captions.size() == 0)){
            Toast.makeText(this, R.string.subtitle_parse_fail, Toast.LENGTH_LONG).show();
            return;
        }


        mSubs = subtitleCaptions;
        mCurrentSubtitlePath = subtitleFilePath;

        Boolean enableSaveSubDelay = mSettings.getBoolean("save_individual_subtitle_delay",true);
        mSubtitleDelay = 0;
        if(enableSaveSubDelay && mSubtitleFileMapDelay.containsKey(subtitleFilePath))
            mSubtitleDelay = mSubtitleFileMapDelay.get(subtitleFilePath);

        //usage of below :
        // A- when video is paused and user loads the subtitle
        // B- to change the current caption when user switches the subtitle
        if(mProgressSubtitleCaptionThread != null && mProgressSubtitleCaptionThread.mProgressSubtitleHanlder != null)
            mProgressSubtitleCaptionThread.progessCaption();
        if(mService != null && !mService.isPlaying())
            showCaptionControls();
        saveLastUsedSubtitle(mCurrentSubtitlePath);

    }

    private void parseSubtitle(String path){
        prepareSubtitleThreadClass();
        removeCurrentSubtitle();
        SubtitleParser mSubtitleParser = SubtitleParser.getInstance();
        mSubtitleParser.setSubtitleParserListener(VideoPlayerActivity.this);
        String manualEncoding = mSettings.getString("subtitle_text_encoding","");
        mSubtitleParser.parseSubtitle(path,null, manualEncoding);


    }

    private void selectSubtitles() {
        if(mSubtitleFiles == null || mSubtitleFiles.size()==0)
            return;
        FragmentManager fm = getSupportFragmentManager();
        int trackPosition = -1;
        int tempPosition = 0;
        for (String path : mSubtitleFiles){
            if(path.equals(mCurrentSubtitlePath)){
                trackPosition = tempPosition;
                break;
            }
            tempPosition++;
        }

        final SubtitleSelectorDialog subtitleSelectorDialog = SubtitleSelectorDialog.newInstance(mSubtitleFiles, mEncodedSubtitles, trackPosition);
        subtitleSelectorDialog.setOnTrackClickListener(new SubtitleSelectorDialog.OnTrackClickListener() {
            @Override
            public void onTrackClick(boolean isChecked, String path) {
                //not unselected the subtitle track
                if(isChecked){
                    parseSubtitle(path);
                }
                else{
                    removeCurrentSubtitle();
                    mSubtitleDelay = 0;
                    hideCaptionControls();
                }

            }

            @Override
            public void onTrackDelete(final String deletedPath, ArrayList<String> newTracksList) {
                VLCApplication.runBackground(new Runnable() {
                    @Override
                    public void run() {
                        MediaDatabase.getInstance().deleteSubtitle(deletedPath, getmediaUniqueName());
                    }
                });
                if(newTracksList.size() == 0)
                    //close the dialogFragment when user deletes all subTracks
                    getSupportFragmentManager().beginTransaction().remove(subtitleSelectorDialog).commit();

                mSubtitleFiles = newTracksList;
                if(mCurrentSubtitlePath!=null && mCurrentSubtitlePath.equals(deletedPath)){
                    removeCurrentSubtitle();
                    mSubtitleDelay = 0;
                    hideCaptionControls();
                }

            }

        });
        subtitleSelectorDialog.show(fm,"select_subtitles");
    }

    private void removeCurrentSubtitle(){
        if(mPlaybackSetting == DelayState.SUBS) {
            mSubtitleDelay = 0;
            endPlaybackSetting();
        }

        mCurrentSubtitlePath = null;
        mSubs = null;
        mLastCaption = null;
        hideSubtitleCaption();
        hideCaptionControls();
        saveLastUsedSubtitle(mCurrentSubtitlePath);

    }
    private void hideSubtitleCaption(){
        mSubtitleView.setVisibility(View.INVISIBLE);

        //I should set it to "". consider this situation
        // 1:1->1:10 "Hello" and 1:11->1:15 no caption and 1:16->1:20 "Hello"
        //if I don't the second "Hello" is equal to first one and will not shown
        mSubtitleView.setText("");

    }


    private void progressSubtitleCaption() {
        if ( mService != null && mSubs != null) {
            Collection<Caption> subtitles = mSubs.captions.values();
            long currentTime = getTime() - mSubtitleDelay;
            if (mLastCaption != null && currentTime >= mLastCaption.start.getMilliseconds() && currentTime <= mLastCaption.end.getMilliseconds()) {
                showTimedCaptionText(mLastCaption);
            } else {
                int index = 0;
                for (Caption caption : subtitles) {
                    if (currentTime >= caption.start.getMilliseconds() && currentTime <= caption.end.getMilliseconds()) {
                        mLastCaption = caption;
                        mLastSubIndex = index;
                        showTimedCaptionText(caption);
                        return;
                    } else if (currentTime > caption.end.getMilliseconds()) {
                        mLastCaption = null;
                        showTimedCaptionText(null);
                    }
                    else if(caption.start.getMilliseconds() > currentTime ) {
                        //this should be here to set mLastSubIndex to correct one
                        //when there is no caption for a while and user uses nex and prev buttons
                        mLastSubIndex = index-1;

                        //for begin of the video when there is no caption yet and user use next button
                        //then plays again so I should hide the caption
                        mLastCaption = null;
                        showTimedCaptionText(null);
                        //  we don't need to check other captions
                        return;
                    }
                    index++;
                }
                //reached to end of captions and didn't find the caption
                mLastSubIndex = index-1;
            }
        }
    }

    private Caption showNextSubtitleCaption(){
        if(mSubs == null)
            return null;
        Collection<Caption> subtitles = mSubs.captions.values();
        Caption caption = null;
        if(mLastSubIndex < subtitles.size()-1) {
            caption = (subtitles.toArray(new Caption[subtitles.size()]))[++mLastSubIndex];
            mLastCaption = caption;
            showTimedCaptionText(caption);
        }
        if(mPlaybackSetting == DelayState.SUBS)
            mSyncCaption = caption;
        return caption;


    }

    private Caption showPrevSubtitleCaption(){
        if(mSubs == null)
            return null;

        Collection<Caption> subtitles = mSubs.captions.values();
        Caption caption = null;
        if(mLastSubIndex > 0) {
            if(mLastCaption == null){
                //there is no dialog at current position so I should show latest
                caption = (subtitles.toArray(new Caption[subtitles.size()]))[mLastSubIndex];
            }
            else
                caption = (subtitles.toArray(new Caption[subtitles.size()]))[--mLastSubIndex];

            mLastCaption = caption;


            showTimedCaptionText(caption);
        }

        if(mPlaybackSetting == DelayState.SUBS)
            mSyncCaption = caption;

        return caption;
    }

    private void seekNextSubtitleCaption(){
        Caption caption = showNextSubtitleCaption();
        if(caption !=null)
            seekWithOutCaption(caption.start.getMilliseconds());
    }

    private void seekPrevSubtitleCaption(){
        Caption caption = showPrevSubtitleCaption();
        if(caption != null)
            seekWithOutCaption(caption.start.getMilliseconds());
    }

    private SpannableStringBuilder makeClickable(String text){
        SpannableStringBuilder ss = new SpannableStringBuilder("");
        String pattern = "[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+]";
        Pattern r = Pattern.compile(pattern);
        String[] words = text.split("(?=[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+])|(?<=[-!$%^&*()_+|~=`{}\\[\\]:\\\";'<>?,.\\/\\s+])");
        int start,end;
        start = end =  0;
        int index = 0;
        for(final String word : words){
            index++;
            ss.append(word);
            end = start + word.length();

            if(!r.matcher(word).find()) {
                ClickableSpan clickableSpan = new SubTouchSpan(word, text);
                ss.setSpan(clickableSpan, start, end, 0);
            }

            start = end;
        }

        return ss;
    }

    private class SubTouchSpan extends ClickableSpan {
        private final String mText;
        //TODO: Fix this currentyl if I use showNextSubtitleCaption and showPrevSubtitleCaption
        //mLastCaption is not set so I can't use mLastCaption and I added mDialog property here
        private final String mDialog;
        private SubTouchSpan(final String text, final String dialog) {
            mText = text;
            mDialog = dialog;
        }
        @Override
        public void onClick(final View widget) {
            mWasPlaying = mService.isPlaying();
            pause();
            FragmentManager fm = getSupportFragmentManager();
            final int dictionaryState = mSettings.getInt("dictionary_status", 0);
            switch (dictionaryState){
                case 3 : /*OK*/
                    final DictionaryDialog dictionaryDialog = DictionaryDialog.newInstance(mDialog, mText);
                    if(dictionaryDialog != null)
                        dictionaryDialog.show(fm,"sub_Touch");
                    return;

                case 0 : /*not ready yet*/
                    Toast.makeText(getApplicationContext(),getString(R.string.dictionary_is_not_ready),Toast.LENGTH_LONG).show();
                    return;
                case 2 : /*not enough storage error */
                    Toast.makeText(getApplicationContext(),getString(R.string.dictionary_not_enough_space),Toast.LENGTH_LONG).show();
                    return;
                case 1 : /*unknown error*/
                    Toast.makeText(getApplicationContext(),getString(R.string.dictionary_error),Toast.LENGTH_LONG).show();
                    return;
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.linkColor = mSubtitleColor;
            ds.setUnderlineText(false);
        }
    }

    public void onDictionaryDialogDismissed(){
        if(mWasPlaying){
            play();
        }
    }

    protected void showTimedCaptionText(final Caption text) {
        if (text == null) {
            if (mSubtitleView.getText().length() > 0) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        hideSubtitleCaption();
                    }
                });
            }
            return;
        }
        SpannableStringBuilder styledString = (SpannableStringBuilder) Html.fromHtml(text.content);


        if (!mSubtitleView.getText().toString().equals(styledString.toString())) {
            if(enableTouchSub) {
//               /*currently I use styledString.toString for clickable sub and I don't use the tags in it */
//                ForegroundColorSpan[] toRemoveSpans = styledString.getSpans(0, styledString.length(), ForegroundColorSpan.class);
//                for (ForegroundColorSpan remove : toRemoveSpans) {
//                    styledString.removeSpan(remove);
//                }

                final SpannableStringBuilder clickableString = makeClickable(styledString.toString());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mSubtitleView.setVisibility(View.VISIBLE);
                        mSubtitleView.setText(clickableString);
                    }
                });
            }
            else {
                final SpannableStringBuilder regularStyledString = styledString;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mSubtitleView.setVisibility(View.VISIBLE);
                        mSubtitleView.setText(regularStyledString);
                    }
                });
            }
        }
    }

    public ProgressSubtitleCaptionThread mProgressSubtitleCaptionThread;
    private void prepareSubtitleThreadClass(){
        if (mProgressSubtitleCaptionThread != null)
            return;
        mProgressSubtitleCaptionThread = new ProgressSubtitleCaptionThread("ProgressSubtitleCaptionThread");
        mProgressSubtitleCaptionThread.start();
        mProgressSubtitleCaptionThread.prepareHanlder();
    }

    public class ProgressSubtitleCaptionThread extends HandlerThread implements Handler.Callback {
        private Handler mProgressSubtitleHanlder;
        private SpannableStringBuilder styledString;
        public static final int PROGRESSCAPTION = 0;

        public ProgressSubtitleCaptionThread(String name) {
            super(name);
        }

        public void progessCaption(){
            mProgressSubtitleHanlder.sendMessage(mProgressSubtitleHanlder.obtainMessage(PROGRESSCAPTION));
        }

        public void prepareHanlder() {
            mProgressSubtitleHanlder = new Handler(getLooper(), this);
        }

        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case PROGRESSCAPTION:
                    progressSubtitleCaption();
                    break;

            }
            return false;
        }
    }

    private void showNavMenu() {
        if (mMenuIdx >= 0)
            mService.setTitleIdx(mMenuIdx);
    }

    private void updateSeekable(boolean seekable) {
        if (mRewind != null) {
            mRewind.setEnabled(seekable);
            mRewind.setImageResource(seekable
                    ? R.drawable.ic_rewind_circle
                    : R.drawable.ic_rewind_circle_disable_o);
        }
        if (mForward != null){
            mForward.setEnabled(seekable);
            mForward.setImageResource(seekable
                    ? R.drawable.ic_forward_circle
                    : R.drawable.ic_forward_circle_disable_o);
        }
        if (!mIsLocked && mSeekbar != null)
            mSeekbar.setEnabled(seekable);
    }

    private void updatePausable(boolean pausable) {
        if (mPlayPause == null)
            return;
        mPlayPause.setEnabled(pausable);
        if (!pausable)
            mPlayPause.setImageResource(R.drawable.ic_play_circle_disable_o);
    }

    private void doPlayPause() {
        if (!mService.isPausable())
            return;

        if (mService.isPlaying()) {
            showOverlayTimeout(OVERLAY_INFINITE);
            pause();
        } else {
            hideOverlay(true);
            play();
        }
    }


    private long getTime() {
        long time = mService.getTime();
        if (mForcedTime != -1 && mLastTime != -1) {
            /* XXX: After a seek, mService.getTime can return the position before or after
             * the seek position. Therefore we return mForcedTime in order to avoid the seekBar
             * to move between seek position and the actual position.
             * We have to wait for a valid position (that is after the seek position).
             * to re-init mLastTime and mForcedTime to -1 and return the actual position.
             */
            if (mLastTime > mForcedTime) {
                if (time <= mLastTime && time > mForcedTime || time > mLastTime)
                    mLastTime = mForcedTime = -1;
            } else {
                if (time > mForcedTime)
                    mLastTime = mForcedTime = -1;
            }
        } else if (time == 0)
            time = (int) mService.getCurrentMediaWrapper().getTime();
        return mForcedTime == -1 ? time : mForcedTime;
    }

    protected void seek(long position) {
        seek(position, mService.getLength(),true);
    }

    private void seekWithOutCaption(long position){
        seek(position, mService.getLength(),false);

    }

    private void seek(long position, long length, boolean showCaption) {
        mForcedTime = position;
        mLastTime = mService.getTime();
        mService.seek(position, length);
        //update seekbar , mLength and mTime
        setOverlayProgress((int) position, (int) length);

        //when video is paused and user seeks
        if(!mService.isPlaying() && showCaption)
            if(mProgressSubtitleCaptionThread != null && mProgressSubtitleCaptionThread.mProgressSubtitleHanlder != null)
                mProgressSubtitleCaptionThread.progessCaption();
    }

    private void seekDelta(int delta) {
        // unseekable stream
        if(mService.getLength() <= 0 || !mService.isSeekable()) return;

        long position = getTime() + delta;
        if (position < 0) position = 0;
            seek(position);
        StringBuilder sb = new StringBuilder();
        if (delta > 0f)
            sb.append('+');
        sb.append((int)(delta/1000f))
                .append("s (")
                .append(Tools.millisToString(mService.getTime()))
                .append(')');
        showInfo(sb.toString(), 1000);
    }

    private void initSeekButton() {
        mRewind = (ImageView) findViewById(R.id.player_overlay_rewind);
        mForward = (ImageView) findViewById(R.id.player_overlay_forward);
        mRewind.setVisibility(View.VISIBLE);
        mForward.setVisibility(View.VISIBLE);
        mRewind.setOnClickListener(this);
        mForward.setOnClickListener(this);
        mRewind.setOnTouchListener(new OnRepeatListener(this));
        mForward.setOnTouchListener(new OnRepeatListener(this));
    }

    private void resizeVideo() {
        if (mCurrentSize < SURFACE_ORIGINAL) {
            mCurrentSize++;
        } else {
            mCurrentSize = 0;
        }
        changeSurfaceLayout();
        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                showInfo(R.string.surface_best_fit, 1000);
                break;
            case SURFACE_FIT_SCREEN:
                showInfo(R.string.surface_fit_screen, 1000);
                break;
            case SURFACE_FILL:
                showInfo(R.string.surface_fill, 1000);
                break;
            case SURFACE_16_9:
                showInfo("16:9", 1000);
                break;
            case SURFACE_4_3:
                showInfo("4:3", 1000);
                break;
            case SURFACE_ORIGINAL:
                showInfo(R.string.surface_original, 1000);
                break;
        }
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt(PreferencesActivity.VIDEO_RATIO, mCurrentSize);
        editor.apply();
        showOverlay();
    }

    /**
     * show overlay
     * @param forceCheck: adjust the timeout in function of playing state
     */
    private void showOverlay(boolean forceCheck) {
        if (forceCheck)
            mOverlayTimeout = 0;
        showOverlayTimeout(0);
    }

    /**
     * show overlay with the previous timeout value
     */
    private void showOverlay() {
        showOverlay(false);
    }

    /**
     * show overlay
     */
    private void showOverlayTimeout(int timeout) {
        if (mService == null)
            return;
        initOverlay();
        if (timeout != 0)
            mOverlayTimeout = timeout;
        else
            mOverlayTimeout = mService.isPlaying() ? OVERLAY_TIMEOUT : OVERLAY_INFINITE;
        if (mIsNavMenu){
            mShowing = true;
            return;
        }

        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        if (!mShowing) {
            mShowing = true;
            if (!mIsLocked) {
                mOverlayProgress.setVisibility(View.VISIBLE);

                if(mOverlayProgress !=null){
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSubtitleView.getLayoutParams();
                    params.addRule(RelativeLayout.ABOVE, R.id.progress_overlay);
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,0); // remove the ALIGN_PARENT_BOTTOM rule
                    mSubtitleView.setLayoutParams(params);
                }
            }
            else{
                mUnlock.setVisibility(View.VISIBLE);
            }
            if (!AndroidDevices.isChromeBook)
                dimStatusBar(false);
//            mOverlayProgress.setVisibility(View.VISIBLE);
            if (mPresentation != null)
                mOverlayBackground.setVisibility(View.VISIBLE);
            updateOverlayPausePlay();
        }
        mHandler.removeMessages(FADE_OUT);
        if (mOverlayTimeout != OVERLAY_INFINITE)
            mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), mOverlayTimeout);
    }

    private void initOverlay() {
        ViewStubCompat vsc = (ViewStubCompat) findViewById(R.id.player_hud_stub);
        if (vsc != null) {
            final boolean seekButtons = mSettings.getBoolean("enable_seek_buttons", false);
            vsc.inflate();
            mOverlayProgress = findViewById(R.id.progress_overlay);
            RelativeLayout.LayoutParams layoutParams =
                    (RelativeLayout.LayoutParams)mOverlayProgress.getLayoutParams();
            if (AndroidDevices.isPhone || !AndroidDevices.hasNavBar)
                layoutParams.width = LayoutParams.MATCH_PARENT;
            else
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            mOverlayProgress.setLayoutParams(layoutParams);
            mOverlayBackground = findViewById(R.id.player_overlay_background);
            mOverlayButtons =  findViewById(R.id.player_overlay_buttons);
            // Position and remaining time
            final boolean rtl = AndroidUtil.isJellyBeanMR1OrLater && TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;
            mTime = (TextView) findViewById(rtl ? R.id.player_overlay_length : R.id.player_overlay_time);
            mLength = (TextView) findViewById(rtl ? R.id.player_overlay_time : R.id.player_overlay_length);
            mPlayPause = (ImageView) findViewById(R.id.player_overlay_play);
            mTracks = (ImageView) findViewById(R.id.player_overlay_tracks);
            mTracks.setOnClickListener(this);
            mAdvOptions = (ImageView) findViewById(R.id.player_overlay_adv_function);
            mAdvOptions.setOnClickListener(this);
            mLock = (ImageView) findViewById(R.id.lock_overlay_button);
            mSize = (ImageView) findViewById(R.id.player_overlay_size);
            mNavMenu = (ImageView) findViewById(R.id.player_overlay_navmenu);
            if (seekButtons)
                initSeekButton();
            mSeekbar = (SeekBar) findViewById(R.id.player_overlay_seekbar);
            resetHudLayout();
            updateOverlayPausePlay();
            updateSeekable(mService.isSeekable());
            updatePausable(mService.isPausable());
            updateNavStatus();
            setHudClickListeners(true);
            initPlaylistUi();
        }
    }


    /**
     * hider overlay
     */
    private void hideOverlay(boolean fromUser) {
        if (mShowing) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.removeMessages(SHOW_PROGRESS);
            Log.i(TAG, "remove View!");
            UiTools.setViewVisibility(mOverlayTips, View.INVISIBLE);

            if(mOverlayProgress !=null && !mIsLocked){
                if(mSubtitleView !=null){
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mSubtitleView.getLayoutParams();
                    params.addRule(RelativeLayout.ABOVE, 0); //remove layout_above rule
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    mSubtitleView.setLayoutParams(params);
                }
            }

            if (!fromUser && !mIsLocked) {
                mOverlayProgress.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mPlayPause.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mTracks != null)
                    mTracks.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mAdvOptions !=null)
                    mAdvOptions.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mRewind != null)
                    mRewind.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mForward != null)
                    mForward.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mPlaylistNext != null)
                    mPlaylistNext.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mPlaylistPrevious != null)
                    mPlaylistPrevious.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mSize.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            }
            if (mPresentation != null) {
                mOverlayBackground.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mOverlayBackground.setVisibility(View.INVISIBLE);
            }
            mOverlayProgress.setVisibility(View.INVISIBLE);

            UiTools.setViewVisibility(mUnlock, View.INVISIBLE);
            mShowing = false;
            if (!AndroidDevices.isChromeBook)
                dimStatusBar(true);
        } else if (!fromUser) {
            /*
             * Try to hide the Nav Bar again.
             * It seems that you can't hide the Nav Bar if you previously
             * showed it in the last 1-2 seconds.
             */
            dimStatusBar(true);
        }
    }

    /**
     * Dim the status bar and/or navigation icons when needed on Android 3.x.
     * Hide it on Android 4.0 and later
     */
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void dimStatusBar(boolean dim) {
        if (dim || mIsLocked)
            mActionBar.hide();
        else
            mActionBar.show();
        if (!AndroidUtil.isHoneycombOrLater || mIsNavMenu)
            return;
        int visibility = 0;
        int navbar = 0;

        if (AndroidUtil.isJellyBeanOrLater) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            navbar = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        if (dim || mIsLocked) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (AndroidUtil.isICSOrLater)
                navbar |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            else
                visibility |= View.STATUS_BAR_HIDDEN;
            if (!AndroidDevices.hasCombBar) {
                navbar |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (AndroidUtil.isKitKatOrLater)
                    visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE;
                if (AndroidUtil.isJellyBeanOrLater)
                    visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            }
        } else {
            mActionBar.show();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (AndroidUtil.isICSOrLater)
                visibility |= View.SYSTEM_UI_FLAG_VISIBLE;
            else
                visibility |= View.STATUS_BAR_VISIBLE;
        }

        if (AndroidDevices.hasNavBar)
            visibility |= navbar;
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }

    private void showTitle() {
        if (!AndroidUtil.isHoneycombOrLater || mIsNavMenu)
            return;
        int visibility = 0;
        int navbar = 0;
        mActionBar.show();

        if (AndroidUtil.isJellyBeanOrLater) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            navbar = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        if (AndroidUtil.isICSOrLater)
            navbar |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        if (AndroidDevices.hasNavBar)
            visibility |= navbar;
        getWindow().getDecorView().setSystemUiVisibility(visibility);

    }

    private void updateOverlayPausePlay() {
        if (mService == null || mPlayPause == null)
            return;
        if (mService.isPausable())
            mPlayPause.setImageResource(mService.isPlaying() ? R.drawable.ic_pause_circle
                    : R.drawable.ic_play_circle);
        mPlayPause.requestFocus();
    }

    /**
     * update the overlay
     */
    private int setOverlayProgress() {
        if (mService == null)
            return -1;
        int time = (int) getTime();
        int length = (int) mService.getLength();
        return setOverlayProgress(time, length);
    }

    private int setOverlayProgress(int time, int length) {
        if (time == -1 || length == -1)
            return -1;
        // Update all view elements
        if (mSeekbar != null) {
            mSeekbar.setMax(length);
            mSeekbar.setProgress(time);
        }
        if (mSysTime != null)
            mSysTime.setText(DateFormat.getTimeFormat(this).format(new Date(System.currentTimeMillis())));
        if (mTime != null &&  time >= 0) mTime.setText(Tools.millisToString(time));
        if (mLength != null &&  length >= 0) mLength.setText(mDisplayRemainingTime && length > 0
                ? "-" + '\u00A0' + Tools.millisToString(length - time)
                : Tools.millisToString(length));

        return time;
    }

    private void invalidateESTracks(int type) {
        switch (type) {
            case Media.Track.Type.Audio:
                mAudioTracksList = null;
                break;
            case Media.Track.Type.Text:
//                mSubtitleTracksList = null;
                break;
        }
    }

    private void setESTracks() {
        if (mLastAudioTrack >= -1) {
            mService.setAudioTrack(mLastAudioTrack);
            mLastAudioTrack = -2;
        }
        mService.setSpuTrack(-1);
    }

    private MediaPlayer.TrackDescription [] mSubtitleTracksList;
    private ArrayList<String> mEncodedSubtitles = new ArrayList<>();
    private Queue<EncodingSub> mEncodingSubQueue = new LinkedList<>();
    private class EncodingSub{
        String path;
        int id;
        public EncodingSub(String path,int id){
            this.path = path;
            this.id = id;
        }
    }
    private void setESTrackLists() {
        if (mAudioTracksList == null && mService.getAudioTracksCount() > 0)
            mAudioTracksList = mService.getAudioTracks();

        if (mSubtitleTracksList == null && mService.getSpuTracksCount() > 0) {
            mSubtitleTracksList = mService.getSpuTracks();

            long lastModified = new File(Uri.parse(mService.getCurrentMediaLocation()).getPath()).lastModified();
            File movieDirectory = FileUtils.createMovieEncodedSubtitleDirectory(getApplicationContext(), Uri.parse(mService.getCurrentMediaLocation()).getPath(), lastModified);
            if (movieDirectory == null) {
                return;
            }
            for (MediaPlayer.TrackDescription trackDescription : mSubtitleTracksList) {
                if(trackDescription.id == -1)
                    continue;
                String srtFileName = (trackDescription.id + trackDescription.name).replaceAll("[^a-zA-Z0-9.-]", "_")+".srt";
                File srtFile = new File(movieDirectory, srtFileName); //srtFile
                mEncodedSubtitles.add(srtFile.getAbsolutePath());

                if(!srtFile.exists()) {
                    EncodingSub encodingSub = new EncodingSub(srtFile.getAbsolutePath(), trackDescription.id);
                    mEncodingSubQueue.add(encodingSub);
                }
            }
            if(!mEncodingSubQueue.isEmpty())
                Toast.makeText(getApplicationContext(),R.string.preparing_embeded_subs, Toast.LENGTH_SHORT).show();

            generateSubtitles();
        }
    }

    /**
     *
     */
    private void play() {
        mService.play();
        if (mRootView != null)
            mRootView.setKeepScreenOn(true);
    }

    /**
     *
     */
    private void pause() {
        mService.pause();
        if (mRootView != null)
            mRootView.setKeepScreenOn(false);
    }

    /*
     * Additionnal method to prevent alert dialog to pop up
     */
    @SuppressWarnings({ "unchecked" })
    private void loadMedia(boolean fromStart) {
        mAskResume = false;
        getIntent().putExtra(PLAY_EXTRA_FROM_START, fromStart);
        loadMedia();
    }

    /**
     * External extras:
     * - position (long) - position of the video to start with (in ms)
     * - subtitles_location (String) - location of a subtitles file to load
     * - from_start (boolean) - Whether playback should start from start or from resume point
     * - title (String) - video title, will be guessed from file if not set.
     */
    @TargetApi(12)
    @SuppressWarnings({ "unchecked" })
    protected void loadMedia() {
        if (mService == null)
            return;
        mUri = null;
        mIsPlaying = false;
        String title = null;
        boolean fromStart = false;
        String itemTitle = null;
        int positionInPlaylist = -1;
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        long savedTime = extras != null ? extras.getLong(PLAY_EXTRA_START_TIME) : 0L; // position passed in by intent (ms)
        if (extras != null && savedTime == 0L)
            savedTime = extras.getInt(PLAY_EXTRA_START_TIME);
        /*
         * If the activity has been paused by pressing the power button, then
         * pressing it again will show the lock screen.
         * But onResume will also be called, even if vlc-android is still in
         * the background.
         * To workaround this, pause playback if the lockscreen is displayed.
         */
        final KeyguardManager km = (KeyguardManager) getApplicationContext().getSystemService(KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode())
            mWasPaused = true;
        if (mWasPaused)
            Log.d(TAG, "Video was previously paused, resuming in paused mode");

        if (intent.getData() != null)
            mUri = intent.getData();
        if (extras != null) {
            if (intent.hasExtra(PLAY_EXTRA_ITEM_LOCATION))
                mUri = extras.getParcelable(PLAY_EXTRA_ITEM_LOCATION);
            fromStart = extras.getBoolean(PLAY_EXTRA_FROM_START, false);
            mAskResume &= !fromStart;
            positionInPlaylist = extras.getInt(PLAY_EXTRA_OPENED_POSITION, -1);
        }

        if (intent.hasExtra(PLAY_EXTRA_SUBTITLES_LOCATION)) {
            final Uri subLocation = Uri.parse(extras.getString(PLAY_EXTRA_SUBTITLES_LOCATION));
            addAndSaveSubtitle(subLocation, false);
        }
        if (intent.hasExtra(PLAY_EXTRA_ITEM_TITLE))
            itemTitle = extras.getString(PLAY_EXTRA_ITEM_TITLE);

        MediaWrapper openedMedia = null;
        if (positionInPlaylist != -1 && mService.hasMedia() && positionInPlaylist < mService.getMedias().size()) {
            // Provided externally from AudioService
            Log.d(TAG, "Continuing playback from PlaybackService at index " + positionInPlaylist);
            openedMedia = mService.getMedias().get(positionInPlaylist);
            if (openedMedia == null) {
                encounteredError();
                return;
            }
            mUri = openedMedia.getUri();
            itemTitle = openedMedia.getTitle();
            updateSeekable(mService.isSeekable());
            updatePausable(mService.isPausable());

            mService.flush();
        }

        if (mUri != null) {
            if (mService.hasMedia() && !mUri.equals(mService.getCurrentMediaWrapper().getUri()))
                mService.stop();
            // restore last position
            MediaWrapper media;
            if (openedMedia == null || openedMedia.getId() <= 0L) {
                Medialibrary ml = mMedialibrary;
                media = ml.getMedia(mUri);
                if (media == null && TextUtils.equals(mUri.getScheme(), "file") &&
                        mUri.getPath() != null && mUri.getPath().startsWith("/sdcard")) {
                    mUri = FileUtils.convertLocalUri(mUri);
                    media = ml.getMedia(mUri);
                }
                if (media != null && media.getId() != 0L && media.getTime() == 0L)
                    media.setTime((long) (media.getMetaLong(MediaWrapper.META_PROGRESS) * (double) media.getLength())/100L);
            } else
                media = openedMedia;
            if (media != null) {
                // in media library
                if (media.getTime() > 0 && !fromStart && positionInPlaylist == -1) {
                    if (mAskResume) {
                        showConfirmResumeDialog();
                        return;
                    }
                }
                // Consume fromStart option after first use to prevent
                // restarting again when playback is paused.
                intent.putExtra(PLAY_EXTRA_FROM_START, false);
                if (fromStart || mService.isPlaying())
                    media.setTime(0L);
                else if (savedTime <= 0L)
                    savedTime = media.getTime();

                mLastAudioTrack = media.getAudioTrack();
            } else {
                // not in media library

                if (savedTime > 0L && mAskResume) {
                    showConfirmResumeDialog();
                    return;
                } else {
                    long rTime = mSettings.getLong(PreferencesActivity.VIDEO_RESUME_TIME, -1);
                    if (rTime > 0 && !fromStart) {
                        if (mAskResume) {
                            showConfirmResumeDialog();
                            return;
                        } else {
                            Editor editor = mSettings.edit();
                            editor.putLong(PreferencesActivity.VIDEO_RESUME_TIME, -1);
                            editor.apply();
                            savedTime = rTime;
                        }
                    }
                }
            }

            // Start playback & seek
            mService.addCallback(this);
            /* prepare playback */
            final boolean hasMedia = mService.hasMedia();
            final boolean medialoaded = media != null;
            if (!medialoaded) {
                if (hasMedia)
                    media = mService.getCurrentMediaWrapper();
                else
                    media = new MediaWrapper(mUri);
            }
            if (mWasPaused)
                media.addFlags(MediaWrapper.MEDIA_PAUSED);
            if (intent.hasExtra(PLAY_DISABLE_HARDWARE))
                media.addFlags(MediaWrapper.MEDIA_NO_HWACCEL);
            media.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
            media.addFlags(MediaWrapper.MEDIA_VIDEO);

            if (savedTime <= 0L && media.getTime() > 0L)
                savedTime = media.getTime();
            if (savedTime > 0L && !mService.isPlaying())
                mService.saveTimeToSeek(savedTime);

            // Handle playback
            if (!hasMedia) {
                if (!medialoaded && positionInPlaylist != -1)
                    mService.loadLastPlaylist(PlaybackService.TYPE_VIDEO);
                else
                    mService.load(media);
            } else if (!mService.isPlaying())
                mService.playIndex(positionInPlaylist);
            else
                onPlaying();

            // Get possible subtitles
            getSubtitles();

            // Get the title
            if (itemTitle == null && !TextUtils.equals(mUri.getScheme(), "content"))
                title = mUri.getLastPathSegment();
        } else {
            mService.addCallback(this);
            mService.loadLastPlaylist(PlaybackService.TYPE_VIDEO);
            MediaWrapper mw = mService.getCurrentMediaWrapper();
            if (mw == null) {
                finish();
                return;
            }
            mUri = mService.getCurrentMediaWrapper().getUri();
        }
        if (itemTitle != null)
            title = itemTitle;
        mTitle.setText(title);

        if (mWasPaused) {
            // XXX: Workaround to update the seekbar position
            mForcedTime = savedTime;
            setOverlayProgress();
            mForcedTime = -1;

            showOverlay(true);
        }
    }

    private void saveSubtitleDelay(final Uri subLocation, final long delay, final String mediaName) {
        VLCApplication.runBackground(new Runnable() {
            @Override
            public void run() {
                MediaDatabase.getInstance().saveSubtitle(subLocation.getPath(), mediaName, delay);
            }
        });
    }
    private void saveSubtitleDelay(final Uri subLocation,long delay){
        saveSubtitleDelay(subLocation, delay, getmediaUniqueName());
    }
    private void addAndSaveSubtitle(final Uri subLocation, boolean addTobegin){
        if(mSubtitleFiles.contains(subLocation.getPath()))
            return;
        if(addTobegin)
            mSubtitleFiles.add(0,subLocation.getPath());
        else
            mSubtitleFiles.add(subLocation.getPath());
        mSubtitleFileMapDelay.put(subLocation.getPath(),0L);
        saveSubtitleDelay(subLocation,0L);

    }
    private void saveLastUsedSubtitle(final String subLocation){
        VLCApplication.runBackground(new Runnable() {
            @Override
            public void run() {
                MediaDatabase.getInstance().saveLastUsedSubtitle(subLocation , getmediaUniqueName());
            }
        });
    }

    private String mMediaUniqueName = null;
    private String getmediaUniqueName(){
         if(mMediaUniqueName == null) {
             String path =  mService.getCurrentMediaLocation();
             if (path == null)
                 return null;
             long lastModified = new File(Uri.parse(path).getPath()).lastModified();
             mMediaUniqueName = FileUtils.generateMediaUniqueName(Uri.parse(path).getPath()/*To prevent converting '[' , ']' , ... to %xxxx*/, lastModified);
         }
        return mMediaUniqueName;
    }
    private SubtitlesGetTask mSubtitlesGetTask = null;
    private class SubtitlesGetTask extends AsyncTask<String, Void, SubtitlesGetTask.Wrapper> {

        public class Wrapper{
            public String lastUsedSubtitle;
            public ArrayList<MediaDatabase.SubtitleWithDelay> prefsList;
        }

        String mediaUniqueName = null;
        @Override
        protected void onPreExecute(){
            //This can be called from MainThread so I added it in onPreExecute
            mediaUniqueName = getmediaUniqueName();
        }
        @Override
        protected Wrapper doInBackground(String... strings) {
            ArrayList<MediaDatabase.SubtitleWithDelay> prefsList = new ArrayList<>();
            String lastUsedSubtitle = null;
            if(mediaUniqueName != null && !mediaUniqueName.isEmpty()) {
                lastUsedSubtitle = MediaDatabase.getInstance().getLastUsedSubtitle(mediaUniqueName);
                if (!TextUtils.equals(mUri.getScheme(), "content")) {
                    prefsList.addAll(MediaDatabase.getInstance().getSubtitles(mediaUniqueName));
                }
            }

            Wrapper wrapper = new Wrapper();
            wrapper.lastUsedSubtitle = lastUsedSubtitle;
            wrapper.prefsList = prefsList;

            return wrapper;
        }

        @Override
        protected void onPostExecute(Wrapper w) {
            if(w.lastUsedSubtitle != null)
                parseSubtitle(w.lastUsedSubtitle);
            // Add any selected subtitle file from the file picker
            if (w.prefsList.size() > 0) {
                for (MediaDatabase.SubtitleWithDelay fileWithDelay : w.prefsList) {
                    String url = fileWithDelay.subtitleUrl;
                    if (!mSubtitleFiles.contains(url)) {
                        mSubtitleFiles.add(url);
                        mSubtitleFileMapDelay.put(url,fileWithDelay.subtitleDelay);
                    }
                }
            }
            mSubtitlesGetTask = null;
        }

        @Override
        protected void onCancelled() {
            mSubtitlesGetTask = null;
        }
    }

    private void generateSubtitles(){
        if(mEncodingSubQueue.isEmpty())
            return;
        EncodingSub encodedSubData = mEncodingSubQueue.poll();
        final String srtFilePath = encodedSubData.path;
        final int  i = encodedSubData.id;

        FFmpeg ffmpeg = FFmpeg.getInstance(getApplicationContext());
        try {
            ffmpeg.execute(new String[]{"-i",Uri.parse(mService.getCurrentMediaLocation()).getPath(),"-map","0:"+i,srtFilePath,"-y",}, new ExecuteBinaryResponseHandler() {
                @Override
                public void onStart() {
                }

                @Override
                public void onProgress(String message) {
                    Log.d(TAG,"ffmpeg execute: "+ "onProgress"+message);
                }

                @Override
                public void onFailure(String message) {
                    Log.d(TAG,"ffmpeg execute: "+ "onFailure"+message);
                }

                @Override
                public void onSuccess(String message) {
                    addAndSaveSubtitle(Uri.parse(srtFilePath), true);
                    if(mCurrentSubtitlePath == null)
                        parseSubtitle(srtFilePath);
                }

                @Override
                public void onFinish() {
                    Log.d(TAG,"ffmpeg execute: "+ "onFinish");
                    generateSubtitles();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            Log.d(TAG,"ffmpeg execute: " + "alreadyRunning");
        }
    }

    public void getSubtitles() {
        if (mSubtitlesGetTask != null || mService == null)
            return;

        mSubtitlesGetTask = new SubtitlesGetTask();
        mSubtitlesGetTask.execute();


    }

    @SuppressWarnings("deprecation")
    private int getScreenRotation(){
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        try {
            Method m = display.getClass().getDeclaredMethod("getRotation");
            return (Integer) m.invoke(display);
        } catch (Exception e) {
            return Surface.ROTATION_0;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private int getScreenOrientation(int mode){
        switch(mode) {
            case 99: //screen orientation user
                return ActivityInfo.SCREEN_ORIENTATION_USER;
            case 101: //screen orientation landscape
                return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            case 102: //screen orientation portrait
                return ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        }
        /*
         mScreenOrientation = 100, we lock screen at its current orientation
         */
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int rot = getScreenRotation();
        /*
         * Since getRotation() returns the screen's "natural" orientation,
         * which is not guaranteed to be SCREEN_ORIENTATION_PORTRAIT,
         * we have to invert the SCREEN_ORIENTATION value if it is "naturally"
         * landscape.
         */
        @SuppressWarnings("deprecation")
        boolean defaultWide = display.getWidth() > display.getHeight();
        if(rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270)
            defaultWide = !defaultWide;
        if(defaultWide) {
            switch (rot) {
            case Surface.ROTATION_0:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case Surface.ROTATION_90:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            case Surface.ROTATION_180:
                // SCREEN_ORIENTATION_REVERSE_PORTRAIT only available since API
                // Level 9+
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            case Surface.ROTATION_270:
                // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                // Level 9+
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            default:
                return 0;
            }
        } else {
            switch (rot) {
            case Surface.ROTATION_0:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            case Surface.ROTATION_90:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case Surface.ROTATION_180:
                // SCREEN_ORIENTATION_REVERSE_PORTRAIT only available since API
                // Level 9+
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            case Surface.ROTATION_270:
                // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                // Level 9+
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            default:
                return 0;
            }
        }
    }

    public void showConfirmResumeDialog() {
        if (isFinishing())
            return;
        mService.pause();
        /* Encountered Error, exit player with a message */
        mAlertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
                .setMessage(R.string.confirm_resume)
                .setPositiveButton(R.string.resume_from_position, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        loadMedia(false);
                    }
                })
                .setNegativeButton(R.string.play_from_start, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        loadMedia(true);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener(){
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        onBackPressed();
                    }
                })
                .create();
        mAlertDialog.setCancelable(true);
        mAlertDialog.show();
    }

    public void showConfirmReplaceSubtitleDialog(final String path){
        if (isFinishing())
            return;

        mWasPlaying = mService.isPlaying();
        pause();
        /* Encountered Error, exit player with a message */
        mAlertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
                .setMessage(R.string.replace_subtitle)
                .setPositiveButton(R.string.use_and_add_subtitle, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        parseSubtitle(path);
                        if(mWasPlaying)
                            play();
                    }
                })
                .setNegativeButton(R.string.just_add_to_list, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(mWasPlaying)
                            play();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener(){
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        play();
                    }
                })
                .create();
        mAlertDialog.setCancelable(true);
        mAlertDialog.show();
    }

    public void showAdvancedOptions() {
        FragmentManager fm = getSupportFragmentManager();
        AdvOptionsDialog advOptionsDialog = new AdvOptionsDialog();
        advOptionsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dimStatusBar(true);
            }
        });
        advOptionsDialog.show(fm, "fragment_adv_options");
        hideOverlay(false);
    }

    private void togglePlaylist() {
        if (mPlaylist.getVisibility() == View.VISIBLE) {
            mPlaylist.setVisibility(View.GONE);
            mPlaylist.setOnClickListener(null);
            return;
        }
        hideOverlay(true);
        mPlaylist.setVisibility(View.VISIBLE);
        mPlaylist.setAdapter(mPlaylistAdapter);
        updateList();
    }

    private BroadcastReceiver mBtReceiver = AndroidUtil.isICSOrLater ? new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                    long savedDelay = mSettings.getLong(KEY_BLUETOOTH_DELAY, 0l);
                    long currentDelay = mService.getAudioDelay();
                    if (savedDelay != 0l) {
                        boolean connected = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1) == BluetoothA2dp.STATE_CONNECTED;
                        if (connected && currentDelay == 0l)
                            toggleBtDelay(true);
                        else if (!connected && savedDelay == currentDelay)
                            toggleBtDelay(false);
                    }
            }
        }
    } : null;

    private void toggleBtDelay(boolean connected) {
        mService.setAudioDelay(connected ? mSettings.getLong(KEY_BLUETOOTH_DELAY, 0) : 0l);
    }

    private OnClickListener mBtSaveListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            mSettings.edit().putLong(KEY_BLUETOOTH_DELAY, mService.getAudioDelay()).apply();
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void createPresentation() {
        if (mMediaRouter == null || mEnableCloneMode)
            return;

        // Get the current route and its presentation display.
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute(
            MediaRouter.ROUTE_TYPE_LIVE_VIDEO);

        Display presentationDisplay = route != null ? route.getPresentationDisplay() : null;

        if (presentationDisplay != null) {
            // Show a new presentation if possible.
            Log.i(TAG, "Showing presentation on display: " + presentationDisplay);
            mPresentation = new SecondaryDisplay(this, LibVLC(), presentationDisplay);
            mPresentation.setOnDismissListener(mOnDismissListener);
            try {
                mPresentation.show();
                mPresentationDisplayId = presentationDisplay.getDisplayId();
            } catch (WindowManager.InvalidDisplayException ex) {
                Log.w(TAG, "Couldn't show presentation!  Display was removed in "
                        + "the meantime.", ex);
                mPresentation = null;
            }
        } else
            Log.i(TAG, "No secondary display detected");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void removePresentation() {
        if (mMediaRouter == null)
            return;

        // Dismiss the current presentation if the display has changed.
        Log.i(TAG, "Dismissing presentation because the current route no longer "
                + "has a presentation display.");
        if (mPresentation != null) mPresentation.dismiss();
        mPresentation = null;
        mPresentationDisplayId = -1;
        stopPlayback();

        recreate();
    }

    /**
     * Listens for when presentations are dismissed.
     */
    private final DialogInterface.OnDismissListener mOnDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            if (dialog == mPresentation) {
                Log.i(TAG, "Presentation was dismissed.");
                mPresentation = null;
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static final class SecondaryDisplay extends Presentation {
        public final static String TAG = "VLC/SecondaryDisplay";

        private SurfaceView mSurfaceView;
        private SurfaceView mSubtitlesSurfaceView;
        private FrameLayout mSurfaceFrame;

        public SecondaryDisplay(Context context, LibVLC libVLC, Display display) {
            super(context, display);
            if (context instanceof AppCompatActivity) {
                setOwnerActivity((AppCompatActivity) context);
            }
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.player_remote);

            mSurfaceView = (SurfaceView) findViewById(R.id.remote_player_surface);
            mSubtitlesSurfaceView = (SurfaceView) findViewById(R.id.remote_subtitles_surface);
            mSurfaceFrame = (FrameLayout) findViewById(R.id.remote_player_surface_frame);

            mSubtitlesSurfaceView.setZOrderMediaOverlay(true);
            mSubtitlesSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            VideoPlayerActivity activity = (VideoPlayerActivity)getOwnerActivity();
            if (activity == null) {
                Log.e(TAG, "Failed to get the VideoPlayerActivity instance, secondary display won't work");
                return;
            }

            Log.i(TAG, "Secondary display created");
        }
    }

    /**
     * Start the video loading animation.
     */
    private void startLoading() {
        if (mIsLoading)
            return;
        mIsLoading = true;
        AnimationSet anim = new AnimationSet(true);
        RotateAnimation rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);
        rotate.setInterpolator(new DecelerateInterpolator());
        rotate.setRepeatCount(RotateAnimation.INFINITE);
        anim.addAnimation(rotate);
        mLoading.setVisibility(View.VISIBLE);
        mLoading.startAnimation(anim);
    }

    /**
     * Stop the video loading animation.
     */
    private void stopLoading() {
        mHandler.removeMessages(LOADING_ANIMATION);
        UiTools.setViewVisibility(mTipsBackground, View.VISIBLE);
        if (!mIsLoading)
            return;
        mIsLoading = false;
        mLoading.setVisibility(View.INVISIBLE);
        mLoading.clearAnimation();
    }

    public void onClickOverlayTips(View v) {
        UiTools.setViewVisibility(mOverlayTips, View.GONE);
    }

    public void onClickDismissTips(View v) {
        UiTools.setViewVisibility(mOverlayTips, View.GONE);
        Editor editor = mSettings.edit();
        editor.putBoolean(PREF_TIPS_SHOWN, true);
        editor.apply();
    }

    private void updateNavStatus() {
        mIsNavMenu = false;
        mMenuIdx = -1;

        final MediaPlayer.Title[] titles = mService.getTitles();
        if (titles != null) {
            final int currentIdx = mService.getTitleIdx();
            for (int i = 0; i < titles.length; ++i) {
                final MediaPlayer.Title title = titles[i];
                if (title.isMenu()) {
                    mMenuIdx = i;
                    break;
                }
            }
            mIsNavMenu = mMenuIdx == currentIdx;
        }

        if (mIsNavMenu) {
            /*
             * Keep the overlay hidden in order to have touch events directly
             * transmitted to navigation handling.
             */
            hideOverlay(false);
        }
        else if (mMenuIdx != -1)
            setESTracks();

        UiTools.setViewVisibility(mNavMenu, mMenuIdx >= 0 && mNavMenu != null ? View.VISIBLE : View.GONE);
        supportInvalidateOptionsMenu();
    }

    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            mHandler.sendEmptyMessageDelayed(mShowing ? HIDE_INFO : SHOW_INFO, 200);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mHandler.removeMessages(HIDE_INFO);
            mHandler.removeMessages(SHOW_INFO);
            float range = mCurrentScreenOrientation == Configuration.ORIENTATION_LANDSCAPE ? mSurfaceXDisplayRange : mSurfaceYDisplayRange;
            if (mService == null)
                return false;

            //seek if it's on edge else play/pause
            float x = e.getX();
            if (x < range/10f /*range * 0.1*/ ) {
                if((!mIsLocked && mEdgeSeekControl % 2 == 1) ||(mIsLocked && mEdgeSeekControl >= 2) ) {
                    seekDelta(-10000);
                    return true;
                }
            }
            else if (x > range * 0.90f){
                if((!mIsLocked && mEdgeSeekControl % 2 == 1 ) || (mIsLocked && mEdgeSeekControl >= 2 )){
                    seekDelta(10000);
                    return true;
                }
            }

            if ((!mIsLocked && mDoubleTapControl % 2 == 1 ) || (mIsLocked && mDoubleTapControl >= 2)){
                doPlayPause();
                return true;
            }
            return false;
        }
    };

    public PlaybackServiceActivity.Helper getHelper() {
        return mHelper;
    }

    @Override
    public void onConnected(PlaybackService service) {
        mService = service;
        if (!mSwitchingView)
            mHandler.sendEmptyMessage(START_PLAYBACK);
        mSwitchingView = false;
        mSettings.edit().putBoolean(PreferencesActivity.VIDEO_RESTORE, false).apply();
        if (mService.getVolume() > 100 && !audioBoostEnabled)
            mService.setVolume(100);
    }

    @Override
    public void onDisconnected() {
        mService = null;
        mHandler.sendEmptyMessage(AUDIO_SERVICE_CONNECTION_FAILED);
    }

    @Override
    public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth  = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mSarNum = sarNum;
        mSarDen = sarDen;
        changeSurfaceLayout();
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
    }

    private BroadcastReceiver mServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), PLAY_FROM_SERVICE))
                onNewIntent(intent);
            else if (TextUtils.equals(intent.getAction(), EXIT_PLAYER))
                exitOK();
        }
    };
}
