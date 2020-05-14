package com.example.libraryagent_v3;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.content.Context;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.UserStateDetails;
import com.ubtechinc.cruzr.sdk.face.CruzrFaceApi;
import com.ubtechinc.cruzr.sdk.ros.RosConstant;
import com.ubtechinc.cruzr.sdk.ros.RosRobotApi;
import com.ubtechinc.cruzr.sdk.speech.SpeechConstant;
import com.ubtechinc.cruzr.sdk.speech.SpeechRobotApi;
import com.ubtechinc.cruzr.serverlibutil.interfaces.InitListener;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.lex.interactionkit.InteractionClient;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.continuations.LexServiceContinuation;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.AudioPlaybackListener;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.InteractionListener;
import com.amazonaws.mobileconnectors.lex.interactionkit.listeners.MicrophoneListener;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;
import com.amazonaws.regions.Regions;
import com.ubtechinc.cruzr.serverlibutil.interfaces.RemoteWarnListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements InteractiveVoiceView.InteractiveVoiceListener {

    public InteractiveVoiceView.InteractiveVoiceListener voiceListener;
    //public AudioPlaybackListener audioPlaybackListener;
    public InteractionListener interactionListener;
    public MicrophoneListener microphoneListener;

    public String intentName;
    public String responseText;

    final AudioPlaybackListener audioPlaybackListener = new AudioPlaybackListener() {
        @Override
        public void onAudioPlaybackStarted() {
            Log.i("ix", "audio started");
        }

        @Override
        public void onAudioPlayBackCompleted() {
            Log.i("ix", "audio finished");
        }

        @Override
        public void onAudioPlaybackError(Exception e) {

        }
    };

    private static final int KEY_DETECT = 210;
    private static final int DETECT_OBJECT_IN = 1;
    private static final int DETECT_OBJECT_OUT = 2;

    private boolean inConversation = false;

    //private InteractiveVoiceView voiceView = (InteractiveVoiceView) findViewById(R.id.voiceInterface);
    public InteractiveVoiceView voiceView;

    //public static String SHOW_BAR = "com.ubt.cruzr.showbar";
    public static String HIDE_BAR = "com.ubt.cruzr.hidebar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        voiceView = findViewById(R.id.voiceInterface);

        // ONBOARD AUDIO - setup
        final MediaPlayer mp_firstapproach = MediaPlayer.create(this, R.raw.speechfirstapproach);

        // ROS API
        RosRobotApi.get().initializ(this, new InitListener() {
            @Override
            public void onInit() {
                super.onInit();
                Log.i("INFO", "Ros initialised");
                RosRobotApi.get().ledSetOnOff(true);
                RosRobotApi.get().ledSetEffect(RosConstant.Effect.LED_QUIET, 255, RosConstant.Color.PURPLE);
                //waveArms();
                RosRobotApi.get().run("pose1");
            }
        });

        // ROS object detection and response
        RosRobotApi.get().registerWarnCallback(new RemoteWarnListener() {
            @Override
            public void onResult(String message, int key, int value, int data) {
                if (key == KEY_DETECT) {
                    switch (value) {
                        case DETECT_OBJECT_IN:
                            personDetected(mp_firstapproach);
                            break;
                        case DETECT_OBJECT_OUT:
                            personLeft();
                            break;
                    }
                }
            }
        });

        // CRUZR API Setup
        CruzrFaceApi.initCruzrFace(this);
        CruzrFaceApi.setCruzrFace(null,"face_smile", true, true);
        SpeechRobotApi.get().enableWakeup(SpeechConstant.PROHIBIT_WAKE_UP_TYPE_SPEECH,false);

        // LEX listeners - not working at the moment
//        audioPlaybackListener = new AudioPlaybackListener() {
//            @Override
//            public void onAudioPlaybackStarted() {
//                Log.i("ix", "audio started");
//            }
//
//            @Override
//            public void onAudioPlayBackCompleted() {
//                Log.i("ix", "audio finished");
//            }
//
//            @Override
//            public void onAudioPlaybackError(Exception e) {
//
//            }
//        };

        interactionListener = new InteractionListener() {
            @Override
            public void onReadyForFulfillment(Response response) {
                inConversation = false;
                Log.i("ix", "!inconversation");
            }

            @Override
            public void promptUserToRespond(Response response, LexServiceContinuation continuation) {
                Log.i("ix", "promptusertorespond");
            }

            @Override
            public void onInteractionError(Response response, Exception e) {

            }
        };

        voiceListener = new InteractiveVoiceView.InteractiveVoiceListener() {
            @Override
            public void dialogReadyForFulfillment(Map<String, String> slots, String intent) {

            }

            @Override
            public void onResponse(Response response) {

            }

            @Override
            public void onError(String responseText, Exception e) {

            }
        };

        microphoneListener = new MicrophoneListener() {
            @Override
            public void readyForRecording() {

            }

            @Override
            public void startedRecording() {
                inConversation = true;
                Log.i("ix", "inconversation");
            }

            @Override
            public void onRecordingEnd() {

            }

            @Override
            public void onSoundLevelChanged(double soundLevel) {

            }

            @Override
            public void onMicrophoneError(Exception e) {

            }
        };

        // Initialize the Amazon Cognito credentials provider for Lex bot libraryAgent
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-west-2:63c46f9d-1e9a-4f25-bd82-95d447a9b94d", // Identity pool ID
                Regions.US_WEST_2 // Region
        );

        InteractionClient lexInteractionClient = new InteractionClient(getApplicationContext(),
                credentialsProvider,
                Regions.US_WEST_2,
                "libraryAgent",
                "libraryAgentFirst");
        lexInteractionClient.setAudioPlaybackListener(audioPlaybackListener);
        lexInteractionClient.setInteractionListener(interactionListener);

        init();
    }

    private void init() {
        Context appContext = getApplicationContext();
        //InteractiveVoiceView voiceView = (InteractiveVoiceView) findViewById(R.id.voiceInterface);
        voiceView.setInteractiveVoiceListener(this);
        CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(
                "us-west-2:63c46f9d-1e9a-4f25-bd82-95d447a9b94d",
                Regions.fromName("us-west-2"));
        voiceView.getViewAdapter().setCredentialProvider(credentialsProvider);
        voiceView.getViewAdapter().setInteractionConfig(
                new InteractionConfig("libraryAgent",  "libraryAgentFirst"));
        voiceView.getViewAdapter().setAwsRegion("us-west-2");

//        AWSMobileClient.getInstance().initialize(this, new Callback<UserStateDetails>() {
//            @Override
//            public void onResult(UserStateDetails result) {
//                Log.d("ix", "onResult: ");
//                voiceView.getViewAdapter().setCredentialProvider(AWSMobileClient.getInstance());
//                AWSMobileClient.getInstance().getCredentials();
//
//                String identityId = AWSMobileClient.getInstance().getIdentityId();
//                String botName = null;
//                String botAlias = null;
//                String botRegion = null;
//                JSONObject lexConfig;
//                try {
//                    lexConfig = AWSMobileClient.getInstance().getConfiguration().optJsonObject("Lex");
//                    lexConfig = lexConfig.getJSONObject(lexConfig.keys().next());
//
//                    botName = lexConfig.getString("libraryAgent");
//                    botAlias = lexConfig.getString("LibraryAgentFirst");
//                    botRegion = lexConfig.getString("us-west-2");
//                } catch (JSONException e) {
//                    Log.e("ix", "onResult: Failed to read configuration", e);
//                }
//
//                InteractionConfig lexInteractionConfig = new InteractionConfig(
//                        botName,
//                        botAlias,
//                        identityId);
//                voiceView.getViewAdapter().setInteractionConfig(lexInteractionConfig);
//                voiceView.getViewAdapter().setAwsRegion(botRegion);
//            }
//
//            @Override
//            public void onError(Exception e) {
//                Log.e("ix", "onError: ", e);
//            }
//        });
    }

    @Override
    public void dialogReadyForFulfillment(Map<String, String> slots, String intent) {
        Log.i("ix", "dialogreadyforfulfillment " + intent);
        voiceView.performClick();
    }

    @Override
    public void onResponse(Response response) {
        Log.i("ix", "response received " + response.getTextResponse());
        Log.i("ix", "response intent name " + response.getIntentName());
        Log.i("ix", "response session attributes" + response.getSessionAttributes());

        intentName = response.getIntentName();
        responseText = response.getTextResponse();

        robotActions();
    }

    @Override
    public void onError(String responseText, Exception e) {

    }

    private void personDetected(MediaPlayer mp_firstapproach) {
        // new approach
        if (!inConversation) {
            Log.i("ix", "object detected not in conversation");
            inConversation = true;
            //voiceView.performClick();
            if (mp_firstapproach.isPlaying()==false) {
                mp_firstapproach.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        firstResponseCompleted();
                    }
                });
                mp_firstapproach.start();
                RosRobotApi.get().run("talk2");
                CruzrFaceApi.setCruzrFace(null,"face_happy", true, true);
            }
        } else {
            // same person still present
            Log.i("ix", "object detected but already in conversation");
        }
    }

    private void firstResponseCompleted() {
        CruzrFaceApi.setCruzrFace(null,"face_smile", true, true);
        voiceView.performClick();
    }

    private void personLeft() {
        // reset to be ready for new approach
        Log.i("ix", "object left");
        inConversation = false;
        CruzrFaceApi.setCruzrFace(null,"techface_standby", true, true);
    }

    private void robotActions() {

        if (intentName==null)
        {
            intentIsNull();
        } else {
            switch (intentName) {
                case "hello":
                    //findBathroom();
                    speakAndAct("shy", "techface_smile");
                    break;
//                case "findBathroom":
//                    //findBathroom();
//                    speakAndAct("guideleft", "face_happy");
//                    break;
//                case "findKitchen":
//                    //findKitchen();
//                    speakAndAct("guideright", "face_happy");
//                    break;
//                case "findPhotocopier":
//                    speakAndAct("guideleft", "face_happy");
//                    break;
                case "findSomething":
                    findSomethingResponse();
                    break;
                case "foodAndDrink":
                    //findBathroom();
                    speakAndAct("guideright", "face_proud");
                    break;
                case "findPhotocopier":
                    findPhotocopier();
                    break;
                case "humanHelpWifi":
                    //humanHelpWifi();
                    speakAndAct("talk7", "face_default");
                    break;
                case "privacyQuestions":
                    speakAndAct("talk1", "face_speak");
                    break;
                case "robotName":
                    speakAndAct("talk7", "face_shy");
                    break;
                case "robotRole":
                    speakAndAct("talk4", "face_speak");
                    break;
                case "uqFacts":
                    speakAndAct("talk5", "face_proud");
                    break;
                case "whereAreYouFrom":
                    speakAndAct("talk8", "face_speak");
                    break;
                default:
                    //RosRobotApi.get().run("searching");
                    //CruzrFaceApi.setCruzrFace(null,"techface_doubt", true, true);
                    speakAndAct("searching", "techface_doubt");
                    break;

            }
        }


    }

    private void findSomethingResponse() {
        String responseTextLC = responseText.toLowerCase();
        if (responseTextLC.contains("scan") || responseTextLC.contains("copier") || responseTextLC.contains("print")) {
            speakAndAct("guideleft", "face_happy");
        } else if (responseTextLC.contains("kitchen") || responseTextLC.contains("micro") || responseTextLC.contains("water")) {
            speakAndAct("guideright", "face_happy");
        } else if (responseTextLC.contains("toilet") || responseTextLC.contains("bath") || responseTextLC.contains("restroom")) {
            speakAndAct("guideleft", "face_happy");
        }
    }

    private void speakAndAct(String action, String face) {
        RosRobotApi.get().run(action);
        CruzrFaceApi.setCruzrFace(null,face, true, true);
    }

    private void findBathroom() {
        RosRobotApi.get().run("guideright");
        CruzrFaceApi.setCruzrFace(null,"face_happy", true, true);
        //voiceView.performClick();
    }

    private void findKitchen() {
        RosRobotApi.get().run("guideleft");
        CruzrFaceApi.setCruzrFace(null,"face_happy", true, true);
        //voiceView.performClick();
    }

    private void findPhotocopier() {
        RosRobotApi.get().run("guideright");
        CruzrFaceApi.setCruzrFace(null, "face_happy", true, true);
    }

    private void humanHelpWifi() {
        RosRobotApi.get().run("talk7");
        CruzrFaceApi.setCruzrFace(null,"face_default", true, true);
        //voiceView.performClick();
    }

    private void intentIsNull() {
        RosRobotApi.get().run("searching");
        CruzrFaceApi.setCruzrFace(null,"techface_default", true, true);
    }
}
