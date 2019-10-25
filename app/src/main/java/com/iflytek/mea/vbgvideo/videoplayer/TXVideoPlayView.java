package com.iflytek.mea.vbgvideo.videoplayer;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayConfig;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

/**
 * Created by N551 on 2017/2/4.
 */

public class TXVideoPlayView extends LinearLayout implements ITXLivePlayListener {

    private static final String TAG = TXVideoPlayView.class.getSimpleName();

    private TXLivePlayer mLivePlayer = null;
    private boolean          mVideoPlay;
    private TXCloudVideoView mPlayerView;
    private ImageView mLoadingView;
    private boolean          mHWDecode   = false;

    private Button mBtnLog;
    private Button           mBtnPlay;
    private Button           mBtnRenderRotation;
    private Button           mBtnRenderMode;
    private Button           mBtnHWDecode;
    private ScrollView mScrollView;
    private SeekBar mSeekBar;
    private TextView mTextDuration;
    private TextView         mTextStart;

    private static final int  CACHE_STRATEGY_FAST  = 1;  //极速
    private static final int  CACHE_STRATEGY_SMOOTH = 2;  //流畅
    private static final int  CACHE_STRATEGY_AUTO = 3;  //自动

    private static final float  CACHE_TIME_FAST = 1.0f;
    private static final float  CACHE_TIME_SMOOTH = 5.0f;

    private static final float  CACHE_TIME_AUTO_MIN = 5.0f;
    private static final float  CACHE_TIME_AUTO_MAX = 10.0f;

    private int              mCacheStrategy = 0;
    private Button           mBtnCacheStrategy;
    private Button           mRatioFast;
    private Button           mRatioSmooth;
    private Button           mRatioAuto;
    private Button           mBtnStop;
    private LinearLayout     mLayoutCacheStrategy;

    private int              mCurrentRenderMode;
    private int              mCurrentRenderRotation;

    private long             mTrackingTouchTS = 0;
    private boolean          mStartSeek = false;
    private boolean          mVideoPause = false;
    private int              mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;
    private TXLivePlayConfig mPlayConfig;
    private long             mStartPlayTS = 0;


    public TXVideoPlayView(Context context){
        super(context);
    }
    public TXVideoPlayView(Context context, AttributeSet attrs){
        super(context, attrs);
    }
    public TXVideoPlayView(Context context,AttributeSet attrs, int defattr){
        super(context, attrs, defattr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    private void initView(){
        mCurrentRenderMode     = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
        mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_PORTRAIT;

        mPlayConfig = new TXLivePlayConfig();
        if (mLivePlayer == null){
            mLivePlayer = new TXLivePlayer(getContext());
        }

        mPlayerView = (TXCloudVideoView) findViewById(R.id.video_view);
        mPlayerView.disableLog(true);
        //mLoadingView = (ImageView) findViewById(R.id.loadingImageView);
        mBtnPlay = (Button) findViewById(R.id.btnPlay);
        mBtnPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click playbtn isplay:" + mVideoPlay+" ispause:"+mVideoPause+" playtype:"+mPlayType);
                if (mVideoPlay) {
                    if (mPlayType == TXLivePlayer.PLAY_TYPE_VOD_FLV || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_HLS || mPlayType == TXLivePlayer.PLAY_TYPE_VOD_MP4) {
                        if (mVideoPause) {
                            mLivePlayer.resume();
                            mBtnPlay.setBackgroundResource(R.drawable.play_pause);
                        } else {
                            mLivePlayer.pause();
                            mBtnPlay.setBackgroundResource(R.drawable.play_start);
                        }
                        mVideoPause = !mVideoPause;

                    } else {
                        stopPlayRtmp();
                        mVideoPlay = !mVideoPlay;
                    }

                } else {
                    if (startPlayRtmp()) {
                        mVideoPlay = !mVideoPlay;
                    }
                }
            }
        });

        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean bFromUser) {
                mTextStart.setText(String.format("%02d:%02d",progress/60, progress%60));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mStartSeek = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if ( mLivePlayer != null) {
                    mLivePlayer.seek(seekBar.getProgress());
                }
                mTrackingTouchTS = System.currentTimeMillis();
                mStartSeek = false;
            }
        });

        mTextDuration = (TextView) findViewById(R.id.duration);
        mTextStart = (TextView) findViewById(R.id.play_start);
        mTextDuration.setTextColor(Color.rgb(255, 255, 255));
        mTextStart.setTextColor(Color.rgb(255, 255, 255));

        setCacheStrategy(CACHE_STRATEGY_AUTO);
    }

    public void setCacheStrategy(int nCacheStrategy) {
        if (mCacheStrategy == nCacheStrategy)   return;
        mCacheStrategy = nCacheStrategy;

        switch (nCacheStrategy) {
            case CACHE_STRATEGY_FAST:
                mPlayConfig.setAutoAdjustCacheTime(true);
                mPlayConfig.setMaxAutoAdjustCacheTime(CACHE_TIME_FAST);
                mPlayConfig.setMinAutoAdjustCacheTime(CACHE_TIME_FAST);
                mLivePlayer.setConfig(mPlayConfig);
                break;

            case CACHE_STRATEGY_SMOOTH:
                mPlayConfig.setAutoAdjustCacheTime(false);
                mPlayConfig.setCacheTime(CACHE_TIME_SMOOTH);
                mLivePlayer.setConfig(mPlayConfig);
                break;

            case CACHE_STRATEGY_AUTO:
                mPlayConfig.setAutoAdjustCacheTime(true);
                mPlayConfig.setMaxAutoAdjustCacheTime(CACHE_TIME_SMOOTH);
                mPlayConfig.setMinAutoAdjustCacheTime(CACHE_TIME_FAST);
                mLivePlayer.setConfig(mPlayConfig);
                break;

            default:
                break;
        }
    }

    private boolean checkPlayUrl(final String playUrl) {
        if (TextUtils.isEmpty(playUrl) || (!playUrl.startsWith("http://") && !playUrl.startsWith("https://") && !playUrl.startsWith("rtmp://"))) {
            Toast.makeText(getContext(), "播放地址不合法，目前仅支持rtmp,flv,hls,mp4播放方式!", Toast.LENGTH_SHORT).show();
            return false;
        }

      /*  switch (mActivityType) {
            case RTMPBaseActivity.ACTIVITY_TYPE_LIVE_PLAY:
            {
                if (playUrl.startsWith("rtmp://")) {
                    mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;
                } else if ((playUrl.startsWith("http://") || playUrl.startsWith("https://"))&& playUrl.contains(".flv")) {
                    mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_FLV;
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "播放地址不合法，直播目前仅支持rtmp,flv播放方式!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            break;
            case RTMPBaseActivity.ACTIVITY_TYPE_VOD_PLAY:
            {
                if (playUrl.startsWith("http://") || playUrl.startsWith("https://")) {
                    if (playUrl.contains(".flv")) {
                        mPlayType = TXLivePlayer.PLAY_TYPE_VOD_FLV;
                    } else if (playUrl.contains(".m3u8")) {
                        mPlayType = TXLivePlayer.PLAY_TYPE_VOD_HLS;
                    } else if (playUrl.toLowerCase().contains(".mp4")) {
                        mPlayType = TXLivePlayer.PLAY_TYPE_VOD_MP4;
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(), "播放地址不合法，点播目前仅支持flv,hls,mp4播放方式!", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "播放地址不合法，点播目前仅支持flv,hls,mp4播放方式!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            break;
            default:
                Toast.makeText(getActivity().getApplicationContext(), "播放地址不合法，目前仅支持rtmp,flv,hls,mp4播放方式!", Toast.LENGTH_SHORT).show();
                return false;
        }*/
        return true;
    }

    private boolean startPlayRtmp() {
        String playUrl = "http://61.191.24.229:4025/resource/meav/template/may_love/preview.mp4?1";
                //mRtmpUrlView.getText().toString();

//        //由于iOS AppStore要求新上架的app必须使用https,所以后续腾讯云的视频连接会支持https,但https会有一定的性能损耗,所以android将统一替换会http
//        if (playUrl.startsWith("https://")) {
//            playUrl = "http://" + playUrl.substring(8);
//        }

        if (!checkPlayUrl(playUrl)) {
            return false;
        }

        //clearLog();


        mBtnPlay.setBackgroundResource(R.drawable.play_pause);

        mLivePlayer.setPlayerView(mPlayerView);
        mLivePlayer.setPlayListener(this);

        // 硬件加速在1080p解码场景下效果显著，但细节之处并不如想象的那么美好：
        // (1) 只有 4.3 以上android系统才支持
        // (2) 兼容性我们目前还仅过了小米华为等常见机型，故这里的返回值您先不要太当真
        mLivePlayer.enableHardwareDecode(mHWDecode);
        mLivePlayer.setRenderRotation(mCurrentRenderRotation);
        mLivePlayer.setRenderMode(mCurrentRenderMode);
        //设置播放器缓存策略
        //这里将播放器的策略设置为自动调整，调整的范围设定为1到4s，您也可以通过setCacheTime将播放器策略设置为采用
        //固定缓存时间。如果您什么都不调用，播放器将采用默认的策略（默认策略为自动调整，调整范围为1到4s）
        //mLivePlayer.setCacheTime(5);
        mLivePlayer.setConfig(mPlayConfig);

        int result = mLivePlayer.startPlay(playUrl,mPlayType); // result返回值：0 success;  -1 empty url; -2 invalid url; -3 invalid playType;
        if (result == -2) {
            Toast.makeText(getContext(), "非腾讯云链接地址，若要放开限制，请联系腾讯云商务团队", Toast.LENGTH_SHORT).show();
        }
        if (result != 0) {
            mBtnPlay.setBackgroundResource(R.drawable.play_start);
            return false;
        }

        mStartPlayTS = System.currentTimeMillis();
        return true;
    }

    private  void stopPlayRtmp() {
        //enableQRCodeBtn(true);
        mBtnPlay.setBackgroundResource(R.drawable.play_start);
       // stopLoadingAnimation();
        if (mLivePlayer != null) {
            mLivePlayer.setPlayListener(null);
            mLivePlayer.stopPlay(true);
        }
    }
    @Override
    public void onPlayEvent(int event, Bundle param) {
        if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {

            Log.d("AutoMonitor", "PlayFirstRender,cost=" +(System.currentTimeMillis()-mStartPlayTS));
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_PROGRESS ) {
            if (mStartSeek) {
                return;
            }
            int progress = param.getInt(TXLiveConstants.EVT_PLAY_PROGRESS);
            int duration = param.getInt(TXLiveConstants.EVT_PLAY_DURATION);
            long curTS = System.currentTimeMillis();

            // 避免滑动进度条松开的瞬间可能出现滑动条瞬间跳到上一个位置
            if (Math.abs(curTS - mTrackingTouchTS) < 500) {
                return;
            }
            mTrackingTouchTS = curTS;

            if (mSeekBar != null) {
                mSeekBar.setProgress(progress);
            }
            if (mTextStart != null) {
                mTextStart.setText(String.format("%02d:%02d",progress/60,progress%60));
            }
            if (mTextDuration != null) {
                mTextDuration.setText(String.format("%02d:%02d",duration/60,duration%60));
            }
            if (mSeekBar != null) {
                mSeekBar.setMax(duration);
            }
            return;
        } else if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT || event == TXLiveConstants.PLAY_EVT_PLAY_END) {
            stopPlayRtmp();
            mVideoPlay = false;
            mVideoPause = false;
            if (mTextStart != null) {
                mTextStart.setText("00:00");
            }
            if (mSeekBar != null) {
                mSeekBar.setProgress(0);
            }
        } else if (event == TXLiveConstants.PLAY_EVT_PLAY_LOADING){

        }

        String msg = param.getString(TXLiveConstants.EVT_DESCRIPTION);


//        if(mLivePlayer != null){
//            mLivePlayer.onLogRecord("[event:"+event+"]"+msg+"\n");
//        }


         if (event == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
            //stopLoadingAnimation();
        }
    }

    @Override
    public void onNetStatus(Bundle status) {

        //Log.d(TAG, "Current status: " + status.toString());
//        if (mLivePlayer != null){
//            mLivePlayer.onLogRecord("[net state]:\n"+str+"\n");
//        }
    }
}
