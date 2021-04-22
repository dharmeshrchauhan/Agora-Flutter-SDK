//
//  RtcSurfaceView.swift
//  RCTAgora
//
//  Created by LXH on 2020/4/15.
//  Copyright © 2020 Syan. All rights reserved.
//

import UIKit
import DeepAR
import AgoraRtcKit

class ARCameraView: UIView {
    private var arView: ARView!
    private var deepAr: DeepAR!
    public var deepARDelegate: DeepARDelegate!
    private var cameraController: CameraController!
    private var btn: UIButton!
    
    public func setupDeepAR() {
        self.deepAr = DeepAR()
        self.deepAr.setLicenseKey("8b7dffa9177cca4a8a908338cae6c885ca75cb02a38dd087618fe9001e042a66fa2b653c30affdf1")
        self.deepAr.delegate = deepARDelegate
    }
    
    @objc
    private func toggleBeautyFilter(_ sender: UIButton) {
        self.deepAr.switchEffect(withSlot: "masks", path:
                                    Bundle(identifier: "org.cocoapods.agora-rtc-engine")?.path(forResource: "beauty_without_eyelashes", ofType: nil))
        self.btn.setTitle("Remove Beauty Filter", for: .normal)
        self.btn.isHidden = true
        //self.btn.setTitle("Set Beauty Filter", for: .normal)
    }
    
    public func setupARCamera() {
        //let rect = CGRect(x: 0, y: 0, width: 720, height: 1280)
        let arviewFrame = self.bounds.width > 0 ? self.bounds : CGRect(x: 0, y: 0, width: 300, height: 300)
        self.arView = (self.deepAr.createARView(withFrame: arviewFrame) as! ARView)
        self.addSubview(self.arView)
//        self.btn = UIButton(frame: CGRect(x: 20, y: 30, width: 150, height: 30));
//        self.btn.setTitle("Set Beauty Filter", for: .normal)
//        self.btn.addTarget(self, action: #selector(toggleBeautyFilter(_:)), for: .touchUpInside)
//        self.addSubview(self.btn)
        self.cameraController = CameraController()
        self.cameraController.deepAR = self.deepAr
        self.cameraController.startCamera()

        DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(1)) {
            self.deepAr.switchEffect(withSlot: "masks", path:
                                        Bundle(identifier: "org.cocoapods.agora-rtc-engine")?.path(forResource: "beauty_without_eyelashes", ofType: nil))
            self.deepAr.startCapture(withOutputWidth: 720, outputHeight: 1280, subframe: CGRect(x: 0.0, y: 0.0, width: 1.0, height: 1.0))
        }
    }
    
    override var bounds: CGRect {
        didSet {
            if self.arView != nil {
                self.arView.frame = self.bounds;
            }
        }
    }
    
    override var frame: CGRect {
        didSet {
            if self.arView != nil {
                self.arView.frame = self.bounds;
            }
        }
    }
}
