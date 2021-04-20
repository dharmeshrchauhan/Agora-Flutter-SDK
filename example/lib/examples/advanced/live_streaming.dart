import 'dart:developer';

import 'package:agora_rtc_engine/rtc_engine.dart';
import 'package:agora_rtc_engine/rtc_local_view.dart' as RtcLocalView;
import 'package:agora_rtc_engine/rtc_remote_view.dart' as RtcRemoteView;
import 'package:agora_rtc_engine_example/config/agora.config.dart' as config;
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:simple_permissions/simple_permissions.dart';

/// LiveStreaming Example
class LiveStreaming extends StatefulWidget {
  RtcEngine _engine = null;

  @override
  State<StatefulWidget> createState() => _State();
}

class _State extends State<LiveStreaming> {
  String renderChannelId;
  bool isJoined = false;
  ClientRole role;
  int remoteUid;
  bool isLowAudio = true;

  @override
  void initState() {
    super.initState();
    SchedulerBinding.instance.addPostFrameCallback((timeStamp) {
      _showMyDialog();
    });
  }

  @override
  void dispose() {
    super.dispose();
    widget._engine?.destroy();
  }

  Future<void> _showMyDialog() async {
    return showDialog(
      context: context,
      barrierDismissible: false, // user must tap button!
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('AlertDialog Title'),
          content: SingleChildScrollView(
            child: ListBody(
              children: <Widget>[Text('Please choose role')],
            ),
          ),
          actions: <Widget>[
            TextButton(
              child: Text('Broadcaster'),
              onPressed: () {
                this.setState(() {
                  role = ClientRole.Broadcaster;
                  Navigator.of(context).pop();
                });
              },
            ),
            TextButton(
              child: Text('Audience'),
              onPressed: () {
                this.setState(() {
                  role = ClientRole.Audience;
                  Navigator.of(context).pop();
                });
              },
            ),
          ],
        );
      },
    );
  }

  _initEngine() async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      await SimplePermissions.requestPermission(Permission.RecordAudio);
      //await Permission.microphone.request();
    }
    widget._engine =
        await RtcEngine.createWithConfig(RtcEngineConfig(config.appId));

    this._addListener();

    // enable video module and set up video encoding configs
    await widget._engine?.enableVideo();

    // make this room live broadcasting room
    await widget._engine?.setChannelProfile(ChannelProfile.LiveBroadcasting);
    await this._updateClientRole(role);

    // Set audio route to speaker
    await widget._engine?.setDefaultAudioRoutetoSpeakerphone(true);

    // start joining channel
    // 1. Users can only see each other after they join the
    // same channel successfully using the same app id.
    // 2. If app certificate is turned on at dashboard, token is needed
    // when joining channel. The channel name and uid used to calculate
    // the token has to match the ones used for channel join
    await widget._engine
        ?.joinChannel(config.token, config.channelId, null, 0, null);
  }

  _addListener() {
    widget._engine
        ?.setEventHandler(RtcEngineEventHandler(warning: (warningCode) {
      log('Warning ${warningCode}');
    }, error: (errorCode) {
      log('Warning ${errorCode}');
    }, joinChannelSuccess: (channel, uid, elapsed) {
      log('joinChannelSuccess ${channel} ${uid} ${elapsed}');
      setState(() {
        isJoined = true;
      });
    }, userJoined: (uid, elapsed) {
      log('userJoined $uid $elapsed');
      this.setState(() {
        remoteUid = uid;
      });
    }, userOffline: (uid, reason) {
      log('userOffline $uid $reason');
      this.setState(() {
        remoteUid = null;
      });
    }));
  }

  _updateClientRole(ClientRole role) async {
    var option;
    if (role == ClientRole.Broadcaster) {
      await widget._engine?.setVideoEncoderConfiguration(
          VideoEncoderConfiguration(
              dimensions: VideoDimensions(640, 360),
              frameRate: VideoFrameRate.Fps30,
              orientationMode: VideoOutputOrientationMode.Adaptative));
      // enable camera/mic, this will bring up permission dialog for first time
      await widget._engine?.enableLocalAudio(true);
      await widget._engine?.enableLocalVideo(true);
    } else {
      // You have to provide client role options if set to audience
      option = ClientRoleOptions(isLowAudio
          ? AudienceLatencyLevelType.LowLatency
          : AudienceLatencyLevelType.UltraLowLatency);
    }
    await widget._engine?.setClientRole(role, option);
  }

  _onPressToggleRole() {
    this.setState(() {
      role = role == ClientRole.Audience
          ? ClientRole.Broadcaster
          : ClientRole.Audience;
      _updateClientRole(role);
    });
  }

  _onPressToggleLatencyLevel(value) {
    this.setState(() {
      isLowAudio = !isLowAudio;
      widget._engine?.setClientRole(
          ClientRole.Audience,
          ClientRoleOptions(isLowAudio
              ? AudienceLatencyLevelType.LowLatency
              : AudienceLatencyLevelType.UltraLowLatency));
    });
  }

  _renderToolBar() {
    return Positioned(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ElevatedButton(
            child: Text('Toggle Role'),
            onPressed: _onPressToggleRole,
          ),
          Container(
            color: Colors.white,
            child: Row(mainAxisSize: MainAxisSize.min, children: [
              Text('Toggle Audience Latency Level'),
              Switch(
                value: isLowAudio,
                onChanged: _onPressToggleLatencyLevel,
                activeTrackColor: Colors.grey[350],
                activeColor: Colors.white,
              ),
            ]),
          )
        ],
      ),
      left: 10,
      bottom: 10,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Column(
          children: [
            Row(
              children: [
                if (!isJoined)
                  Expanded(
                    flex: 1,
                    child: RaisedButton(
                      onPressed: _initEngine,
                      child: Text('Join channel'),
                    ),
                  )
              ],
            ),
            if (isJoined) _renderVideo(),
          ],
        ),
        if (isJoined) _renderToolBar(),
      ],
    );
  }

  _renderVideo() {
    return Expanded(
      child: Stack(
        children: [
          role == ClientRole.Broadcaster
              ? RtcLocalView.SurfaceView(
                  channelId: renderChannelId,
                )
              : remoteUid != null
                  ? RtcRemoteView.SurfaceView(
                      uid: remoteUid,
                    )
                  : Container()
        ],
      ),
    );
  }
}
