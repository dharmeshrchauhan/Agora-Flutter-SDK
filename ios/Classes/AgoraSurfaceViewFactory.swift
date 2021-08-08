//
//  AgoraSurfaceViewFactory.swift
//  agora_rtc_engine
//
//  Created by LXH on 2020/6/28.
//

import Foundation
import DeepAR

class AgoraSurfaceViewFactory: NSObject, FlutterPlatformViewFactory {
    private final weak var messager: FlutterBinaryMessenger?
    private final weak var rtcEnginePlugin: SwiftAgoraRtcEnginePlugin?
    private final weak var rtcChannelPlugin: AgoraRtcChannelPlugin?

    init(_ messager: FlutterBinaryMessenger, _ rtcEnginePlugin: SwiftAgoraRtcEnginePlugin, _ rtcChannelPlugin: AgoraRtcChannelPlugin) {
        self.messager = messager
        self.rtcEnginePlugin = rtcEnginePlugin
        self.rtcChannelPlugin = rtcChannelPlugin
    }

    func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
        FlutterStandardMessageCodec.sharedInstance()
    }

    func create(withFrame frame: CGRect, viewIdentifier viewId: Int64, arguments args: Any?) -> FlutterPlatformView {
        return AgoraSurfaceView(messager!, frame, viewId, args as? Dictionary<String, Any?>, rtcEnginePlugin!, rtcChannelPlugin!)
    }
}

class AgoraSurfaceView: NSObject, FlutterPlatformView {
    private final weak var rtcEnginePlugin: SwiftAgoraRtcEnginePlugin?
    private final weak var rtcChannelPlugin: AgoraRtcChannelPlugin?
    private var _localView: ARCameraView!
    private var _view: RtcSurfaceView!
    private let channel: FlutterMethodChannel

    init(_ messager: FlutterBinaryMessenger, _ frame: CGRect, _ viewId: Int64, _ args: Dictionary<String, Any?>?, _ rtcEnginePlugin: SwiftAgoraRtcEnginePlugin, _ rtcChannelPlugin: AgoraRtcChannelPlugin) {
        let deepArLicenseKey = args?["deepArLicenseKey"] as! String
        let isLocalView = deepArLicenseKey.count > 0
        //let isLocalView = false
        self.rtcEnginePlugin = rtcEnginePlugin
        self.rtcChannelPlugin = rtcChannelPlugin
        if isLocalView  {
            self._localView = ARCameraView()
        }
        else {
            self._view = RtcSurfaceView(frame: frame)
        }
        self.channel = FlutterMethodChannel(name: "agora_rtc_engine/surface_view_\(viewId)", binaryMessenger: messager)
        super.init()
        if isLocalView  {
            engine?.setExternalVideoSource(true, useTexture: true, pushMode: true);
            engine?.setVideoEncoderConfiguration(AgoraVideoEncoderConfiguration(size: AgoraVideoDimension1280x720,
                                                                                frameRate: .fps15,
                                                                                         bitrate: AgoraVideoBitrateStandard,
                                                                                         orientationMode: .adaptative))
            self._localView.deepARDelegate = self
            self._localView.setupDeepAR(deepArLicenseKey: deepArLicenseKey)
            self._localView.setupARCamera()
        }
        else {
            if let map = args {
                setData(map["data"] as! NSDictionary)
                setRenderMode((map["renderMode"] as! NSNumber).uintValue)
                setMirrorMode((map["mirrorMode"] as! NSNumber).uintValue)
            }
        }
        
        channel.setMethodCallHandler { [weak self] (call, result) in
            var args = [String: Any?]()
            if let arguments = call.arguments {
                args = arguments as! Dictionary<String, Any?>
            }
            switch call.method {
            case "setData":
                self?.setData(args["data"] as! NSDictionary)
            case "setRenderMode":
                self?.setRenderMode((args["renderMode"] as! NSNumber).uintValue)
            case "setMirrorMode":
                self?.setMirrorMode((args["mirrorMode"] as! NSNumber).uintValue)
            case "setNextARFilter":
                self?.setNextARFilter()
            case "endDeepAR":
                self?.endDeepAR()
            case "switchCamera":
                self?.switchCamera()
            default:
                result(FlutterMethodNotImplemented)
            }
        }
    }

    func view() -> UIView {
        return _localView == nil ? _view : _localView;
    }

    deinit {
        channel.setMethodCallHandler(nil)
    }

    func setData(_ data: NSDictionary) {
        var channel: AgoraRtcChannel? = nil
        if let channelId = data["channelId"] as? String {
            channel = getChannel(channelId)
        }
        if let `engine` = engine {
            _view?.setData(engine, channel, (data["uid"] as! NSNumber).uintValue)
        }
    }

    func setRenderMode(_ renderMode: UInt) {
        if let `engine` = engine {
            _view?.setRenderMode(engine, renderMode)
        }
    }

    func setMirrorMode(_ mirrorMode: UInt) {
        if let `engine` = engine {
            _view?.setMirrorMode(engine, mirrorMode)
        }
    }
    
    func setNextARFilter() {
        _localView.setNextARFilter();
    }
    
    func endDeepAR() {
        _localView.endDeepAR();
        //engine?.setExternalVideoSource(false, useTexture: false, pushMode: false)
    }
    
    public func switchCamera() {
        _localView.switchCamera();
    }

    private var engine: AgoraRtcEngineKit? {
        return rtcEnginePlugin?.engine
    }

    private func getChannel(_ channelId: String?) -> AgoraRtcChannel? {
        guard let `channelId` = channelId else {
            return nil
        }
        return rtcChannelPlugin?.channel(channelId)
    }
}

extension AgoraSurfaceView: DeepARDelegate {
    func didFinishPreparingForVideoRecording() {}
    
    func frameAvailable(_ sampleBuffer: CMSampleBuffer!) {
        
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            print("*** NO BUFFER ERROR")
            return
        }

        let time = CMSampleBufferGetPresentationTimeStamp(sampleBuffer)

        let videoFrame = AgoraVideoFrame()
        videoFrame.format = 12
        videoFrame.time = time
        videoFrame.textureBuf = pixelBuffer
        videoFrame.rotation = 0
        
        self.rtcEnginePlugin?.engine?.pushExternalVideoFrame(videoFrame)
    }
    
    func didFinishVideoRecording(_ videoFilePath: String!) {
        print("didFinishVideoRecording")
    }
    
    func recordingFailedWithError(_ error: Error!) {
        print("recordingFailedWithError")
    }
    
    func didTakeScreenshot(_ screenshot: UIImage!) {
        print("didTakeScreenshot")
    }
    
    func didInitialize() {
        print("didInitialize")
    }
    
    func faceVisiblityDidChange(_ faceVisible: Bool) {
        print("faceVisiblityDidChange")
    }
}

