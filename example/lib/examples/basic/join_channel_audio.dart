import 'dart:developer';

import 'package:agora_rtc_engine/rtc_engine.dart';
import 'package:agora_rtc_engine_example/config/agora.config.dart' as config;
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:simple_permissions/simple_permissions.dart';

/// JoinChannelAudio Example
class JoinChannelAudio extends StatefulWidget {
  RtcEngine _engine = null;

  @override
  State<StatefulWidget> createState() => _State();
}

class _State extends State<JoinChannelAudio> {
  String channelId = config.channelId;
  bool isJoined = false,
      openMicrophone = true,
      enableSpeakerphone = true,
      playEffect = false;
  bool _enableInEarMonitoring = false;
  double _recordingVolume = 0, _playbackVolume = 0, _inEarMonitoringVolume = 0;
  TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: channelId);
    this._initEngine();
  }

  @override
  void dispose() {
    super.dispose();
    widget._engine?.destroy();
  }

  _initEngine() async {
    widget._engine =
        await RtcEngine.createWithConfig(RtcEngineConfig(config.appId));
    this._addListeners();

    await widget._engine.enableAudio();
    await widget._engine.setChannelProfile(ChannelProfile.LiveBroadcasting);
    await widget._engine.setClientRole(ClientRole.Broadcaster);
  }

  _addListeners() {
    widget._engine?.setEventHandler(RtcEngineEventHandler(
      joinChannelSuccess: (channel, uid, elapsed) {
        log('joinChannelSuccess ${channel} ${uid} ${elapsed}');
        setState(() {
          isJoined = true;
        });
      },
      leaveChannel: (stats) async {
        log('leaveChannel ${stats.toJson()}');
        setState(() {
          isJoined = false;
        });
        await widget._engine
            ?.joinChannel(config.token, config.channelId, null, config.uid)
            ?.catchError((err) {
          print('error ${err.toString()}');
        });
      },
    ));
  }

  _joinChannel() async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      await SimplePermissions.requestPermission(Permission.RecordAudio);
      //await Permission.microphone.request();
    }

    await widget._engine
        ?.joinChannel(config.token, config.channelId, null, config.uid)
        ?.catchError((onError) {
      print('error ${onError.toString()}');
    });
  }

  _leaveChannel() async {
    await widget._engine?.leaveChannel();
  }

  _switchMicrophone() {
    widget._engine?.enableLocalAudio(!openMicrophone)?.then((value) {
      setState(() {
        openMicrophone = !openMicrophone;
      });
    })?.catchError((err) {
      log('enableLocalAudio $err');
    });
  }

  _switchSpeakerphone() {
    widget._engine?.setEnableSpeakerphone(!enableSpeakerphone)?.then((value) {
      setState(() {
        enableSpeakerphone = !enableSpeakerphone;
      });
    })?.catchError((err) {
      log('setEnableSpeakerphone $err');
    });
  }

  _switchEffect() async {
    if (playEffect) {
      widget._engine?.stopEffect(1)?.then((value) {
        setState(() {
          playEffect = false;
        });
      })?.catchError((err) {
        log('stopEffect $err');
      });
    } else {
      widget._engine
          ?.playEffect(
              1,
              await RtcEngineExtension.getAssetAbsolutePath(
                  "assets/Sound_Horizon.mp3"),
              -1,
              1,
              1,
              100,
              true)
          ?.then((value) {
        setState(() {
          playEffect = true;
        });
      })?.catchError((err) {
        log('playEffect $err');
      });
    }
  }

  _onChangeInEarMonitoringVolume(double value) {
    setState(() {
      _inEarMonitoringVolume = value;
    });
    widget._engine?.setInEarMonitoringVolume(value.toInt());
  }

  _toggleInEarMonitoring(value) {
    setState(() {
      _enableInEarMonitoring = value;
    });
    widget._engine?.enableInEarMonitoring(value);
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Column(
          children: [
            TextField(
              controller: _controller,
              decoration: InputDecoration(hintText: 'Channel ID'),
              onChanged: (text) {
                setState(() {
                  channelId = text;
                });
              },
            ),
            Row(
              children: [
                Expanded(
                  flex: 1,
                  child: RaisedButton(
                    onPressed:
                        isJoined ? this._leaveChannel : this._joinChannel,
                    child: Text('${isJoined ? 'Leave' : 'Join'} channel'),
                  ),
                )
              ],
            ),
          ],
        ),
        Align(
            alignment: Alignment.bottomRight,
            child: Padding(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  RaisedButton(
                    onPressed: this._switchMicrophone,
                    child: Text('Microphone ${openMicrophone ? 'on' : 'off'}'),
                  ),
                  RaisedButton(
                    onPressed: this._switchSpeakerphone,
                    child:
                        Text(enableSpeakerphone ? 'Speakerphone' : 'Earpiece'),
                  ),
                  RaisedButton(
                    onPressed: this._switchEffect,
                    child: Text('${playEffect ? 'Stop' : 'Play'} effect'),
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Text('RecordingVolume:'),
                      Slider(
                        value: _recordingVolume,
                        min: 0,
                        max: 400,
                        divisions: 5,
                        label: 'RecordingVolume',
                        onChanged: (double value) {
                          setState(() {
                            _recordingVolume = value;
                          });
                          widget._engine
                              ?.adjustRecordingSignalVolume(value.toInt());
                        },
                      )
                    ],
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Text('PlaybackVolume:'),
                      Slider(
                        value: _playbackVolume,
                        min: 0,
                        max: 400,
                        divisions: 5,
                        label: 'PlaybackVolume',
                        onChanged: (double value) {
                          setState(() {
                            _playbackVolume = value;
                          });
                          widget._engine
                              ?.adjustPlaybackSignalVolume(value.toInt());
                        },
                      )
                    ],
                  ),
                  Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Row(mainAxisSize: MainAxisSize.min, children: [
                        Text('InEar Monitoring Volume:'),
                        Switch(
                          value: _enableInEarMonitoring,
                          onChanged: _toggleInEarMonitoring,
                          activeTrackColor: Colors.grey[350],
                          activeColor: Colors.white,
                        )
                      ]),
                      if (_enableInEarMonitoring)
                        Container(
                            width: 300,
                            child: Slider(
                              value: _inEarMonitoringVolume,
                              min: 0,
                              max: 100,
                              divisions: 5,
                              label: 'InEar Monitoring Volume',
                              onChanged: _onChangeInEarMonitoringVolume,
                            ))
                    ],
                  ),
                ],
              ),
              padding: EdgeInsets.symmetric(vertical: 20, horizontal: 0),
            ))
      ],
    );
  }
}
