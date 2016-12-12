package com.kaltura.playkitdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.kaltura.playkit.AudioTrackInfo;
import com.kaltura.playkit.BaseTrackInfo;
import com.kaltura.playkit.MediaEntryProvider;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKEvent;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKTracks;
import com.kaltura.playkit.PlayKitManager;
import com.kaltura.playkit.Player;
import com.kaltura.playkit.PlayerConfig;
import com.kaltura.playkit.PlayerEvent;
import com.kaltura.playkit.TextTrackInfo;
import com.kaltura.playkit.VideoTrackInfo;
import com.kaltura.playkit.ads.PKAdInfo;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.backend.ovp.OvpSessionProvider;
import com.kaltura.playkit.backend.ovp.data.PrimitiveResult;
import com.kaltura.playkit.backend.phoenix.OttSessionProvider;
import com.kaltura.playkit.backend.phoenix.PhoenixMediaProvider;
import com.kaltura.playkit.connect.ResultElement;
import com.kaltura.playkit.plugins.SamplePlugin;
import com.kaltura.playkit.plugins.ads.AdEvent;
import com.kaltura.playkit.plugins.ads.ima.IMAConfig;
import com.kaltura.playkit.plugins.ads.ima.IMAPlugin;
import com.kaltura.playkit.utils.Consts;

import java.util.ArrayList;
import java.util.List;

import static com.kaltura.playkitdemo.MockParams.Format;
import static com.kaltura.playkitdemo.MockParams.MediaId;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {


    public static final boolean AUTO_PLAY_ON_RESUME = true;

    private static final PKLog log = PKLog.get("MainActivity");

    private Player player;
    private MediaEntryProvider mediaProvider;
    private PlaybackControlsView controlsView;
    private boolean nowPlaying;
    ProgressBar progressBar;

    private Spinner videoSpinner, audioSpinner, textSpinner;

    private void registerPlugins() {

        PlayKitManager.registerPlugins(SamplePlugin.factory);
        PlayKitManager.registerPlugins(IMAPlugin.factory);
        //PlayKitManager.registerPlugins(KalturaStatsPlugin.factory, PhoenixAnalyticsPlugin.factory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        log.i("PlayKitManager: " + PlayKitManager.CLIENT_TAG);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        registerPlugins();

        //mediaProvider = new MockMediaProvider("mock/entries.playkit.json", this, "dash");

        //startOvpMediaLoading();
        startOttMediaLoading();

    }

    private void startOttMediaLoading() {
        final OttSessionProvider ottSessionProvider = new OttSessionProvider(MockParams.PhoenixBaseUrl, MockParams.OttPartnerId);
        /* start anonymous session:
        ottSessionProvider.startAnonymousSession(MockParams.OttPartnerId, null, new OnCompletion<PrimitiveResult>() {
        OR
        start user session:    */
        MockParams.UserFactory.UserLogin user = MockParams.UserFactory.getUser(MockParams.UserType.Ott);
        ottSessionProvider.startSession(user.username, user.password, null, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if(response.error == null) {
                    mediaProvider = new PhoenixMediaProvider().setSessionProvider(ottSessionProvider).setAssetId(MediaId).setReferenceType(MockParams.MediaType).setFormats(Format);

                    mediaProvider.load(new OnMediaLoadCompletion() {
                        @Override
                        public void onComplete(final ResultElement<PKMediaEntry> response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (response.isSuccess()) {
                                        onMediaLoaded(response.getResponse());
                                    } else {

                                        Toast.makeText(MainActivity.this, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""), Toast.LENGTH_LONG).show();
                                        log.e("failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""));
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void startOvpMediaLoading() {
        final OvpSessionProvider ovpSessionProvider = new OvpSessionProvider(MockParams.KalturaOvpBaseUrl);
        //ovpSessionProvider.startAnonymousSession(MockParams.OvpPartnerId, new OnCompletion<PrimitiveResult>() {
        MockParams.UserFactory.UserLogin user = MockParams.UserFactory.getUser(MockParams.UserType.Ovp);
        ovpSessionProvider.startSession(user.username, user.password, MockParams.OvpPartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if(response.error == null) {
                    mediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ovpSessionProvider).setEntryId(MockParams.EntryId);

                    mediaProvider.load(new OnMediaLoadCompletion() {
                        @Override
                        public void onComplete(final ResultElement<PKMediaEntry> response) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (response.isSuccess()) {
                                        onMediaLoaded(response.getResponse());
                                    } else {

                                        Toast.makeText(MainActivity.this, "failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""), Toast.LENGTH_LONG).show();
                                        log.e("failed to fetch media data: " + (response.getError() != null ? response.getError().getMessage() : ""));
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void onMediaLoaded(PKMediaEntry mediaEntry) {

        PlayerConfig config = new PlayerConfig();

        config.media.setMediaEntry(mediaEntry).setStartPosition(30000);

        if (player == null) {

            configurePlugins(config.plugins);

            player = PlayKitManager.loadPlayer(config, this);

            log.d("Player: " + player.getClass());
            addPlayerListeners(progressBar);

            LinearLayout layout = (LinearLayout) findViewById(R.id.player_root);
            layout.addView(player.getView());

            controlsView = (PlaybackControlsView) this.findViewById(R.id.playerControls);
            controlsView.setPlayer(player);
            initSpinners();
        }
        player.prepare(config.media);
    }

    private void initSpinners() {
        videoSpinner = (Spinner) this.findViewById(R.id.videoSpinner);
        audioSpinner = (Spinner) this.findViewById(R.id.audioSpinner);
        textSpinner = (Spinner) this.findViewById(R.id.subtitleSpinner);

        textSpinner.setOnItemSelectedListener(this);
        audioSpinner.setOnItemSelectedListener(this);
        videoSpinner.setOnItemSelectedListener(this);
    }

    private void configurePlugins(PlayerConfig.Plugins config) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("delay", 4200);
        config.setPluginConfig("Sample", jsonObject);
        addIMAPluginConfig(config);
        //config.setPluginConfig("IMASimplePlugin", jsonObject);
        //config.setPluginConfig("KalturaStatistics", jsonObject);
        //config.setPluginConfig("PhoenixAnalytics", jsonObject);
        //config.setPluginConfig("Youbora", jsonObject);

    }

    private void addIMAPluginConfig(PlayerConfig.Plugins config) {
        String adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator=";
        //"https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/3274935/preroll&impl=s&gdfp_req=1&env=vp&output=xml_vast2&unviewed_position_start=1&url=[referrer_url]&description_url=[description_url]&correlator=[timestamp]";

        List<String> videoMimeTypes = new ArrayList<>();
        //videoMimeTypes.add(MimeTypes.APPLICATION_MP4);
        //videoMimeTypes.add(MimeTypes.APPLICATION_M3U8);
        //Map<Double, String> tagTimesMap = new HashMap<>();
        //tagTimesMap.put(2.0,"GILAD");

        IMAConfig adsConfig = new IMAConfig("en", false, true, 60000, videoMimeTypes, adTagUrl,false, false);
        config.setPluginConfig(IMAPlugin.factory.getName(), adsConfig.toJSONObject());

    }
    @Override
    protected void onPause() {
        super.onPause();
        if (controlsView != null) {
            controlsView.release();
        }
        if (player != null) {
            player.onApplicationPaused();
        }
    }

    private void addPlayerListeners(final ProgressBar appProgressBar) {
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("AD_CONTENT_PAUSE_REQUESTED");
                PKAdInfo adInfo = player.getAdInfo();
                appProgressBar.setVisibility(View.VISIBLE);
            }
        }, AdEvent.Type.CONTENT_PAUSE_REQUESTED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("AD_STARTED");
                appProgressBar.setVisibility(View.INVISIBLE);
            }
        }, AdEvent.Type.STARTED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("Ad Event AD_RESUMED");
                nowPlaying = true;
                appProgressBar.setVisibility(View.INVISIBLE);
            }
        }, AdEvent.Type.RESUMED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                log.d("Ad Event AD_ALL_ADS_COMPLETED");
                appProgressBar.setVisibility(View.INVISIBLE);
            }
        }, AdEvent.Type.ALL_ADS_COMPLETED);
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                nowPlaying = true;
            }
        }, PlayerEvent.Type.PLAY);

        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                nowPlaying = false;
            }
        }, PlayerEvent.Type.PAUSE);

        player.addStateChangeListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                if (event instanceof PlayerEvent.StateChanged) {
                    PlayerEvent.StateChanged stateChanged = (PlayerEvent.StateChanged) event;
                    log.d("State changed from " + stateChanged.oldState + " to " + stateChanged.newState);

                    if(controlsView != null){
                        controlsView.setPlayerState(stateChanged.newState);
                    }
                }
            }
        });
        player.addEventListener(new PKEvent.Listener() {
            @Override
            public void onEvent(PKEvent event) {
                //When the track data available, this event occurs. It brings the info object with it.
                PlayerEvent.TracksAvailable tracksAvailable = (PlayerEvent.TracksAvailable) event;
                populateSpinnersWithTrackInfo(tracksAvailable.getPKTracks());

            }
        }, PlayerEvent.Type.TRACKS_AVAILABLE);
    }


    @Override
    protected void onResume() {
        log.d("Ad Event onResume");
        super.onResume();
        if (player != null) {
            player.onApplicationResumed();
            if (nowPlaying && AUTO_PLAY_ON_RESUME) {
                player.play();
            }
        }
        if (controlsView != null) {
            controlsView.resume();
        }
    }

    /**
     * populating spinners with track info.
     *
     * @param tracksInfo - the track info.
     */
    private void populateSpinnersWithTrackInfo(PKTracks tracksInfo) {

        //Retrieve info that describes available tracks.(video/audio/subtitle).
        TrackItem[] videoTrackItems = obtainRelevantTrackInfo(Consts.TRACK_TYPE_VIDEO, tracksInfo.getVideoTracksInfo());
        //populate spinner with this info.
        applyAdapterOnSpinner(videoSpinner, videoTrackItems);

        TrackItem[] audioTrackItems = obtainRelevantTrackInfo(Consts.TRACK_TYPE_AUDIO, tracksInfo.getAudioTracksInfo());
        applyAdapterOnSpinner(audioSpinner, audioTrackItems);


        TrackItem[] subtitlesTrackItems = obtainRelevantTrackInfo(Consts.TRACK_TYPE_TEXT, tracksInfo.getTextTracksInfo());
        applyAdapterOnSpinner(textSpinner, subtitlesTrackItems);
    }


    /**
     * Obtain info that user is interested in.
     * For example if user want to display in UI bitrate of the available tracks,
     * he can do it, by obtaining the tackType of video, and getting the getBitrate() from videoTrackInfo.
     *
     * @param trackType  - tyoe of the track you are interested in.
     * @param trackInfos - all availables tracks.
     * @return
     */
    private TrackItem[] obtainRelevantTrackInfo(int trackType, List<BaseTrackInfo> trackInfos) {
        TrackItem[] trackItems = new TrackItem[trackInfos.size()];
        switch (trackType) {
            case Consts.TRACK_TYPE_VIDEO:
                TextView tvVideo = (TextView) this.findViewById(R.id.tvVideo);
                changeSpinnerVisibility(videoSpinner, tvVideo, trackInfos);

                for (int i = 0; i < trackInfos.size(); i++) {
                        VideoTrackInfo videoTrackInfo = (VideoTrackInfo) trackInfos.get(i);
                        if(videoTrackInfo.isAdaptive()){
                            trackItems[i] = new TrackItem("Auto", videoTrackInfo.getUniqueId());
                        }else{
                            trackItems[i] = new TrackItem(String.valueOf(videoTrackInfo.getBitrate()), videoTrackInfo.getUniqueId());
                        }
                }

                break;
            case Consts.TRACK_TYPE_AUDIO:
                TextView tvAudio = (TextView) this.findViewById(R.id.tvAudio);
                changeSpinnerVisibility(audioSpinner, tvAudio, trackInfos);

                for (int i = 0; i < trackInfos.size(); i++) {
                    AudioTrackInfo audioTrackInfo = (AudioTrackInfo) trackInfos.get(i);
                    if(audioTrackInfo.isAdaptive()){
                        trackItems[i] = new TrackItem(audioTrackInfo.getLanguage() + " Auto", audioTrackInfo.getUniqueId());
                    }else{
                        trackItems[i] = new TrackItem(audioTrackInfo.getLanguage() + " " + String.valueOf(audioTrackInfo.getBitrate()), audioTrackInfo.getUniqueId());
                    }
                }
                break;
            case Consts.TRACK_TYPE_TEXT:
                TextView tvSubtitle = (TextView) this.findViewById(R.id.tvText);
                changeSpinnerVisibility(textSpinner, tvSubtitle, trackInfos);

                for (int i = 0; i < trackInfos.size(); i++) {

                    TextTrackInfo textTrackInfo = (TextTrackInfo) trackInfos.get(i);
                    trackItems[i] = new TrackItem(String.valueOf(textTrackInfo.getLanguage()), textTrackInfo.getUniqueId());
                }
                break;
        }
        return trackItems;
    }

    private void changeSpinnerVisibility(Spinner spinner, TextView textView, List<BaseTrackInfo> trackInfos) {
        //hide spinner if no data available.
        if (trackInfos.isEmpty()) {
            textView.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.VISIBLE);
        }
    }

    private void applyAdapterOnSpinner(Spinner spinner, TrackItem[] trackInfo) {
        TrackItemAdapter trackItemAdapter = new TrackItemAdapter(this, R.layout.track_items_list_row, trackInfo);
        spinner.setAdapter(trackItemAdapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            TrackItem trackItem = (TrackItem) parent.getItemAtPosition(position);
            //tell to the player, to switch track based on the user selection.
            player.changeTrack(trackItem.getUniqueId());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
